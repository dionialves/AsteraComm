package com.dionialves.AsteraComm.report.audit;

import com.dionialves.AsteraComm.call.Call;
import com.dionialves.AsteraComm.call.CallCostingService;
import com.dionialves.AsteraComm.call.CallDirection;
import com.dionialves.AsteraComm.call.CallRepository;
import com.dionialves.AsteraComm.call.CallType;
import com.dionialves.AsteraComm.circuit.Circuit;
import com.dionialves.AsteraComm.circuit.CircuitRepository;
import com.dionialves.AsteraComm.exception.NotFoundException;
import com.dionialves.AsteraComm.plan.PackageType;
import com.dionialves.AsteraComm.plan.Plan;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class AuditService {

    private final CircuitRepository circuitRepository;
    private final CallRepository    callRepository;

    public AuditResultDTO simulate(String circuitNumber, int month, int year) {
        return simulate(circuitNumber, month, year, false);
    }

    public AuditResultDTO simulate(String circuitNumber, int month, int year, boolean onlyOutgoing) {
        Circuit circuit = circuitRepository.findByNumber(circuitNumber)
                .orElseThrow(() -> new NotFoundException("Circuito não encontrado: " + circuitNumber));

        Plan plan = circuit.getPlan();
        if (plan == null) {
            throw new NotFoundException("Circuito " + circuitNumber + " não possui plano vinculado");
        }

        List<Call> calls = callRepository.findByCircuitNumberAndPeriod(circuitNumber, month, year);

        return buildResult(circuit, plan, month, year, calls, onlyOutgoing);
    }

    private AuditResultDTO buildResult(Circuit circuit, Plan plan, int month, int year, List<Call> calls, boolean onlyOutgoing) {
        List<AuditCallLineDTO> lines = new ArrayList<>();

        // Acumuladores de quota (independentes por tipo em PER_CATEGORY)
        BigDecimal quotaAccumulated = BigDecimal.ZERO;
        BigDecimal unifiedRemaining = resolveUnifiedQuota(plan);
        Map<CallType, BigDecimal> perCategoryRemaining = resolvePerCategoryQuota(plan);

        // Totalizadores para o resumo
        BigDecimal totalMinutes     = BigDecimal.ZERO;
        BigDecimal quotaMinutesUsed = BigDecimal.ZERO;
        BigDecimal totalCost        = BigDecimal.ZERO;

        for (Call call : calls) {
            int billSeconds        = call.getBillSeconds();
            CallType callType      = call.getCallType();
            BigDecimal rate        = resolveRate(plan, callType);
            BigDecimal durationMinutes = BigDecimal.valueOf(Math.ceil(billSeconds / 30.0))
                    .divide(BigDecimal.valueOf(2), 1, RoundingMode.UNNECESSARY);

            BigDecimal cost;
            BigDecimal quotaUsedThisCall;

            if (billSeconds <= 3) {
                cost              = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
                quotaUsedThisCall = BigDecimal.ZERO;
            } else if (plan.getPackageType() == PackageType.UNIFIED) {
                quotaUsedThisCall = applyQuota(unifiedRemaining, durationMinutes);
                cost              = decodeCost(billSeconds, rate, durationMinutes, unifiedRemaining);
                unifiedRemaining  = unifiedRemaining.subtract(quotaUsedThisCall);
            } else if (plan.getPackageType() == PackageType.PER_CATEGORY) {
                BigDecimal categoryRemaining = perCategoryRemaining.getOrDefault(callType, BigDecimal.ZERO);
                quotaUsedThisCall = applyQuota(categoryRemaining, durationMinutes);
                cost              = decodeCost(billSeconds, rate, durationMinutes, categoryRemaining);
                perCategoryRemaining.merge(callType, quotaUsedThisCall.negate(), BigDecimal::add);
            } else {
                // PackageType.NONE
                quotaUsedThisCall = BigDecimal.ZERO;
                cost              = CallCostingService.calculateFractionCost(billSeconds, rate);
            }

            quotaAccumulated = quotaAccumulated.add(quotaUsedThisCall);
            if (billSeconds > 3) totalMinutes = totalMinutes.add(durationMinutes);
            quotaMinutesUsed = quotaMinutesUsed.add(quotaUsedThisCall);
            totalCost        = totalCost.add(cost);

            lines.add(new AuditCallLineDTO(
                    call.getUniqueId(),
                    call.getCallDate(),
                    call.getDst(),
                    callType,
                    billSeconds,
                    rate,
                    quotaUsedThisCall,
                    quotaAccumulated,
                    cost,
                    call.getDirection()
            ));
        }

        // Filter outgoing only if requested
        if (onlyOutgoing) {
            lines = lines.stream()
                    .filter(l -> l.direction() == CallDirection.OUTBOUND)
                    .toList();
        }

        BigDecimal excessMinutes = totalMinutes.subtract(quotaMinutesUsed);

        AuditSummaryDTO summary = new AuditSummaryDTO(
                lines.size(),
                totalMinutes,
                quotaMinutesUsed,
                excessMinutes,
                totalCost
        );

        return new AuditResultDTO(circuit.getNumber(), plan.getName(), month, year, lines, summary);
    }

    private static BigDecimal decodeCost(int billSeconds, BigDecimal rate,
                                         BigDecimal durationMinutes, BigDecimal remaining) {
        if (remaining.compareTo(durationMinutes) >= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
        } else if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            int billableSeconds = billSeconds - remaining.multiply(BigDecimal.valueOf(60)).intValue();
            return CallCostingService.calculateFractionCost(billableSeconds, rate);
        } else {
            return CallCostingService.calculateFractionCost(billSeconds, rate);
        }
    }

    private static BigDecimal applyQuota(BigDecimal remaining, BigDecimal durationMinutes) {
        if (remaining.compareTo(durationMinutes) >= 0) return durationMinutes;
        if (remaining.compareTo(BigDecimal.ZERO) > 0)  return remaining;
        return BigDecimal.ZERO;
    }

    private BigDecimal resolveUnifiedQuota(Plan plan) {
        if (plan.getPackageType() != PackageType.UNIFIED) return BigDecimal.ZERO;
        return plan.getPackageTotalMinutes() != null
                ? BigDecimal.valueOf(plan.getPackageTotalMinutes())
                : BigDecimal.ZERO;
    }

    private Map<CallType, BigDecimal> resolvePerCategoryQuota(Plan plan) {
        Map<CallType, BigDecimal> map = new EnumMap<>(CallType.class);
        if (plan.getPackageType() != PackageType.PER_CATEGORY) return map;
        putIfNotNull(map, CallType.FIXED_LOCAL,          plan.getPackageFixedLocal());
        putIfNotNull(map, CallType.FIXED_LONG_DISTANCE,  plan.getPackageFixedLongDistance());
        putIfNotNull(map, CallType.MOBILE_LOCAL,         plan.getPackageMobileLocal());
        putIfNotNull(map, CallType.MOBILE_LONG_DISTANCE, plan.getPackageMobileLongDistance());
        return map;
    }

    private void putIfNotNull(Map<CallType, BigDecimal> map, CallType type, Integer value) {
        if (value != null && value > 0) map.put(type, BigDecimal.valueOf(value));
    }

    public byte[] generatePdf(String circuitNumber, int month, int year, boolean onlyRelevant) {
        return generatePdf(circuitNumber, month, year, onlyRelevant, false);
    }

    public byte[] generatePdf(String circuitNumber, int month, int year, boolean onlyRelevant, boolean onlyOutgoing) {
        AuditResultDTO result = simulate(circuitNumber, month, year, onlyOutgoing);
        String[] meses = {"Janeiro","Fevereiro","Março","Abril","Maio","Junho",
                          "Julho","Agosto","Setembro","Outubro","Novembro","Dezembro"};
        String mesNome = (month >= 1 && month <= 12) ? meses[month - 1] : String.valueOf(month);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 36, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont  = new Font(Font.HELVETICA, 13, Font.BOLD, Color.WHITE);
            Font subFont    = new Font(Font.HELVETICA, 9,  Font.NORMAL, new Color(100, 100, 100));
            Font headerFont = new Font(Font.HELVETICA, 8,  Font.BOLD, Color.WHITE);
            Font bodyFont   = new Font(Font.HELVETICA, 8,  Font.NORMAL, new Color(55, 65, 81));
            Font footFont   = new Font(Font.HELVETICA, 8,  Font.BOLD,   new Color(55, 65, 81));

            // Título
            PdfPTable titleTable = new PdfPTable(1);
            titleTable.setWidthPercentage(100);
            PdfPCell titleCell = new PdfPCell(new Phrase(
                    "Auditoria de Custeio   |   Circuito " + result.circuitNumber() + "   |   " + mesNome + " / " + year, titleFont));
            titleCell.setBackgroundColor(new Color(39, 39, 42));
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setPadding(10);
            titleTable.addCell(titleCell);
            doc.add(titleTable);

            // Subtítulo com plano
            doc.add(new Paragraph("Plano: " + result.planName(), subFont));
            doc.add(new Paragraph(" "));

            // Resumo
            AuditSummaryDTO s = result.summary();
            PdfPTable summaryTable = new PdfPTable(new float[]{1f, 1f, 1f, 1f, 1f});
            summaryTable.setWidthPercentage(100);
            Color sumBg = new Color(243, 244, 246);
            for (String col : new String[]{"Ligações", "Minutos totais", "Franquia usada", "Excedente", "Custo total"}) {
                PdfPCell c = new PdfPCell(new Phrase(col, new Font(Font.HELVETICA, 8, Font.BOLD, new Color(100, 100, 100))));
                c.setBackgroundColor(sumBg);
                c.setBorder(Rectangle.BOX);
                c.setPadding(5);
                summaryTable.addCell(c);
            }
            for (String val : new String[]{
                    String.valueOf(s.totalCalls()),
                    s.totalMinutes() + " min",
                    s.quotaMinutesUsed() + " min",
                    s.excessMinutes() + " min",
                    "R$ " + String.format("%,.2f", s.totalCost()).replace(",","X").replace(".",",").replace("X",".")}) {
                PdfPCell c = new PdfPCell(new Phrase(val, footFont));
                c.setBackgroundColor(Color.WHITE);
                c.setBorder(Rectangle.BOX);
                c.setPadding(5);
                summaryTable.addCell(c);
            }
            doc.add(summaryTable);
            doc.add(new Paragraph(" "));

            // Tabela detalhada
            PdfPTable table = new PdfPTable(new float[]{1.4f, 1.5f, 0.9f, 0.8f, 0.8f, 0.8f, 0.9f, 0.9f});
            table.setWidthPercentage(100);
            Color headerBg = new Color(39, 39, 42);
            for (String col : new String[]{"Data/Hora", "Destino", "Tipo", "Seg. fat.", "Franquia", "Acumulado", "Tarifa/min", "Custo"}) {
                PdfPCell c = new PdfPCell(new Phrase(col, headerFont));
                c.setBackgroundColor(headerBg);
                c.setBorder(Rectangle.NO_BORDER);
                c.setPadding(5);
                table.addCell(c);
            }
            Color rowAlt = new Color(249, 250, 251);
            List<AuditCallLineDTO> lines = onlyRelevant
                    ? result.lines().stream()
                        .filter(l -> l.quotaUsedThisCall().signum() > 0 || l.cost().signum() > 0)
                        .toList()
                    : result.lines();
            for (int i = 0; i < lines.size(); i++) {
                AuditCallLineDTO line = lines.get(i);
                Color bg = (i % 2 == 1) ? rowAlt : Color.WHITE;
                String tipo = switch (line.callType()) {
                    case FIXED_LOCAL -> "Fixo Local";
                    case MOBILE_LOCAL -> "Móvel Local";
                    case FIXED_LONG_DISTANCE -> "Fixo LD";
                    case MOBILE_LONG_DISTANCE -> "Móvel LD";
                    default -> "—";
                };
                String custo = "R$ " + String.format("%,.2f", line.cost()).replace(",","X").replace(".",",").replace("X",".");
                String tarifa = "R$ " + String.format("%,.4f", line.ratePerMinute()).replace(",","X").replace(".",",").replace("X",".");
                addPdfCell(table, line.callDate().format(dtf),              bodyFont, Element.ALIGN_LEFT,  bg);
                addPdfCell(table, line.dst(),                               bodyFont, Element.ALIGN_LEFT,  bg);
                addPdfCell(table, tipo,                                     bodyFont, Element.ALIGN_LEFT,  bg);
                addPdfCell(table, String.valueOf(line.billSeconds()),        bodyFont, Element.ALIGN_RIGHT, bg);
                addPdfCell(table, String.valueOf(line.quotaUsedThisCall()), bodyFont, Element.ALIGN_RIGHT, bg);
                addPdfCell(table, String.valueOf(line.quotaAccumulated()),  bodyFont, Element.ALIGN_RIGHT, bg);
                addPdfCell(table, tarifa,                                   bodyFont, Element.ALIGN_RIGHT, bg);
                addPdfCell(table, custo,                                    bodyFont, Element.ALIGN_RIGHT, bg);
            }
            // Linha de totais
            BigDecimal totalCostLines = lines.stream()
                    .map(AuditCallLineDTO::cost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int totalBillSec           = lines.stream().mapToInt(AuditCallLineDTO::billSeconds).sum();
            BigDecimal totalQuota      = lines.stream().map(AuditCallLineDTO::quotaUsedThisCall)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal lastAccum       = lines.isEmpty() ? BigDecimal.ZERO
                    : lines.get(lines.size() - 1).quotaAccumulated();
            String totalCustoStr = "R$ " + String.format("%,.2f", totalCostLines)
                    .replace(",", "X").replace(".", ",").replace("X", ".");
            Color totalBg = new Color(243, 244, 246);
            addPdfCell(table, "TOTAL",                           footFont, Element.ALIGN_LEFT,  totalBg);
            addPdfCell(table, "",                                footFont, Element.ALIGN_LEFT,  totalBg);
            addPdfCell(table, "",                                footFont, Element.ALIGN_LEFT,  totalBg);
            addPdfCell(table, String.valueOf(totalBillSec),      footFont, Element.ALIGN_RIGHT, totalBg);
            addPdfCell(table, totalQuota.toPlainString(),        footFont, Element.ALIGN_RIGHT, totalBg);
            addPdfCell(table, lastAccum.toPlainString(),         footFont, Element.ALIGN_RIGHT, totalBg);
            addPdfCell(table, "",                                footFont, Element.ALIGN_RIGHT, totalBg);
            addPdfCell(table, totalCustoStr,                     footFont, Element.ALIGN_RIGHT, totalBg);

            doc.add(table);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF de auditoria", e);
        }
    }

    private void addPdfCell(PdfPTable table, String text, Font font, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.TOP);
        cell.setPadding(5);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private static BigDecimal resolveRate(Plan plan, CallType callType) {
        return switch (callType) {
            case FIXED_LOCAL          -> plan.getFixedLocal();
            case FIXED_LONG_DISTANCE  -> plan.getFixedLongDistance();
            case MOBILE_LOCAL         -> plan.getMobileLocal();
            case MOBILE_LONG_DISTANCE -> plan.getMobileLongDistance();
            default                   -> BigDecimal.ZERO;
        };
    }
}
