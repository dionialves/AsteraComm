package com.dionialves.AsteraComm.report.callhistory;

import com.dionialves.AsteraComm.call.Call;
import com.dionialves.AsteraComm.call.CallRepository;
import com.dionialves.AsteraComm.call.CallDirection;
import com.dionialves.AsteraComm.circuit.Circuit;
import com.dionialves.AsteraComm.circuit.CircuitRepository;
import com.dionialves.AsteraComm.exception.NotFoundException;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
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
import java.util.List;

@RequiredArgsConstructor
@Service
public class CallHistoryService {

    private final CircuitRepository circuitRepository;
    private final CallRepository    callRepository;

    public CallHistoryResultDTO getHistory(String circuitNumber, int month, int year) {
        Circuit circuit = circuitRepository.findByNumber(circuitNumber)
                .orElseThrow(() -> new NotFoundException("Circuito não encontrado: " + circuitNumber));

        List<Call> calls = callRepository.findByCircuitNumberAndPeriod(circuitNumber, month, year);

        List<CallHistoryLineDTO> lines = calls.stream()
                .map(this::toLineDTO)
                .toList();

        int totalCalls = lines.size();
        int totalBillSec = lines.stream().mapToInt(CallHistoryLineDTO::billSeconds).sum();
        BigDecimal totalMinutes = BigDecimal.valueOf(Math.ceil(totalBillSec / 30.0))
                .divide(BigDecimal.valueOf(2), 1, RoundingMode.UNNECESSARY);

        return new CallHistoryResultDTO(circuitNumber, month, year, totalCalls, totalMinutes, lines);
    }

    private CallHistoryLineDTO toLineDTO(Call call) {
        int billSec = call.getBillSeconds();
        Integer durationSec = billSec > 3 ? call.getDurationSeconds() : null;
        return new CallHistoryLineDTO(
                call.getUniqueId(),
                call.getCallDate(),
                call.getCallerNumber(),
                call.getDst(),
                translateDisposition(call.getDisposition()),
                call.getCallType(),
                call.getDirection(),
                call.getDirection() == CallDirection.OUTBOUND ? "Efetuada" : "Recebida",
                billSec,
                durationSec
        );
    }

    private String translateDisposition(String disposition) {
        if (disposition == null) return "—";
        return switch (disposition.trim().toUpperCase()) {
            case "ANSWERED"  -> "Atendida";
            case "NO ANSWER" -> "Não Atendeu";
            case "BUSY"      -> "Ocupado";
            case "FAILED"    -> "Falhou";
            default          -> disposition;
        };
    }

    public byte[] generatePdf(String circuitNumber, int month, int year) {
        CallHistoryResultDTO result = getHistory(circuitNumber, month, year);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

        String[] meses = {"Janeiro","Fevereiro","Março","Abril","Maio","Junho",
                          "Julho","Agosto","Setembro","Outubro","Novembro","Dezembro"};
        String mesNome = (month >= 1 && month <= 12) ? meses[month - 1] : String.valueOf(month);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont  = new Font(Font.HELVETICA, 14, Font.BOLD, Color.WHITE);
            Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            Font bodyFont   = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(55, 65, 81));
            Font footFont   = new Font(Font.HELVETICA, 9, Font.BOLD,   new Color(55, 65, 81));

            PdfPTable header = new PdfPTable(1);
            header.setWidthPercentage(100);
            PdfPCell titleCell = new PdfPCell(new Phrase(
                    "Histórico de Ligações   |   Circuito: " + circuitNumber + "   |   " + mesNome + " / " + year, titleFont));
            titleCell.setBackgroundColor(new Color(39, 39, 42));
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setPadding(10);
            header.addCell(titleCell);
            doc.add(header);
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{2f, 1.5f, 1.5f, 1.2f, 1f, 1f, 1.2f});
            table.setWidthPercentage(100);
            Color headerBg = new Color(39, 39, 42);
            String[] cols = {"Data/Hora", "Origem", "Destino", "Tipo", "Direção", "Duração", "Status"};
            for (String col : cols) {
                PdfPCell cell = new PdfPCell(new Phrase(col, headerFont));
                cell.setBackgroundColor(headerBg);
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(cell);
            }

            Color rowAlt = new Color(249, 250, 251);
            for (int i = 0; i < result.lines().size(); i++) {
                CallHistoryLineDTO line = result.lines().get(i);
                Color bg = (i % 2 == 1) ? rowAlt : Color.WHITE;
                String tipo = switch (line.callType()) {
                    case FIXED_LOCAL          -> "Fixo Local";
                    case MOBILE_LOCAL         -> "Móvel Local";
                    case FIXED_LONG_DISTANCE  -> "Fixo LD";
                    case MOBILE_LONG_DISTANCE -> "Móvel LD";
                    default -> "—";
                };

                int sec = line.billSeconds();
                int h = sec / 3600; int m = (sec % 3600) / 60; int s = sec % 60;
                String duration = (h > 0 ? h + ":" : "") + (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;

                addPdfCell(table, line.callDate().format(dtf), bodyFont, Element.ALIGN_LEFT, bg);
                addPdfCell(table, line.callerNumber() != null ? line.callerNumber() : "—", bodyFont, Element.ALIGN_LEFT, bg);
                addPdfCell(table, line.dst(), bodyFont, Element.ALIGN_LEFT, bg);
                addPdfCell(table, tipo, bodyFont, Element.ALIGN_LEFT, bg);
                addPdfCell(table, line.directionLabel(), bodyFont, Element.ALIGN_LEFT, bg);
                addPdfCell(table, duration, bodyFont, Element.ALIGN_RIGHT, bg);
                addPdfCell(table, line.dispositionLabel(), bodyFont, Element.ALIGN_LEFT, bg);
            }
            doc.add(table);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF de histórico de ligações", e);
        }
    }

    private void addPdfCell(PdfPTable table, String text, Font font, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.TOP);
        cell.setPadding(6);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }
}