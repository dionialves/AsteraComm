package com.dionialves.AsteraComm.report.costpercircuit;

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
import java.util.List;

@RequiredArgsConstructor
@Service
public class CostPerCircuitService {

    private final CostPerCircuitRepository costPerCircuitRepository;

    public List<CallCostReportDTO> getReport(int month, int year, boolean onlyWithCost) {
        return costPerCircuitRepository.findCallCostByPeriod(month, year).stream()
                .filter(row -> !onlyWithCost || row.totalCost().compareTo(BigDecimal.ZERO) > 0)
                .map(row -> new CallCostReportDTO(
                        row.customerName(),
                        row.circuitNumber(),
                        row.callCount(),
                        row.totalBillSeconds() / 60,
                        row.totalCost()
                ))
                .toList();
    }

    public CostPerCircuitResponseDTO getCostPerCircuit(int month, int year, boolean onlyWithCost) {
        List<CallCostReportDTO> data = getReport(month, year, onlyWithCost);
        long totalCalls   = data.stream().mapToLong(CallCostReportDTO::callCount).sum();
        long totalMinutes = data.stream().mapToLong(CallCostReportDTO::totalMinutes).sum();
        BigDecimal totalCost = data.stream()
                .map(CallCostReportDTO::totalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        CostPerCircuitSummaryDTO summary = new CostPerCircuitSummaryDTO(
                data.size(), totalCalls, totalMinutes, totalCost);
        return new CostPerCircuitResponseDTO(summary, data);
    }

    public byte[] generateCostPerCircuitPdf(int month, int year, boolean onlyWithCost) {
        List<CallCostReportDTO> data = getReport(month, year, onlyWithCost);

        String[] meses = {"Janeiro","Fevereiro","Março","Abril","Maio","Junho",
                          "Julho","Agosto","Setembro","Outubro","Novembro","Dezembro"};
        String mesNome = (month >= 1 && month <= 12) ? meses[month - 1] : String.valueOf(month);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont  = new Font(Font.HELVETICA, 14, Font.BOLD, Color.WHITE);
            Font headerFont = new Font(Font.HELVETICA, 9,  Font.BOLD, Color.WHITE);
            Font bodyFont   = new Font(Font.HELVETICA, 9,  Font.NORMAL, new Color(55, 65, 81));
            Font footFont   = new Font(Font.HELVETICA, 9,  Font.BOLD,   new Color(55, 65, 81));

            // ── Título ──────────────────────────────────────────────────────
            PdfPTable header = new PdfPTable(1);
            header.setWidthPercentage(100);
            PdfPCell titleCell = new PdfPCell(new Phrase(
                    "Relatório — Custo de Ligações por Circuito   |   " + mesNome + " / " + year, titleFont));
            titleCell.setBackgroundColor(new Color(39, 39, 42));
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setPadding(10);
            header.addCell(titleCell);
            doc.add(header);
            doc.add(new Paragraph(" "));

            // ── Tabela de dados ─────────────────────────────────────────────
            PdfPTable table = new PdfPTable(new float[]{3f, 2f, 1f, 1f, 1.5f});
            table.setWidthPercentage(100);

            Color headerBg = new Color(39, 39, 42);
            for (String col : new String[]{"Cliente", "Circuito", "Ligações", "Minutos", "Custo (R$)"}) {
                PdfPCell cell = new PdfPCell(new Phrase(col, headerFont));
                cell.setBackgroundColor(headerBg);
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setPadding(6);
                cell.setHorizontalAlignment(col.equals("Ligações") || col.equals("Minutos") || col.equals("Custo (R$)")
                        ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                table.addCell(cell);
            }

            long sumCalls = 0, sumMinutes = 0;
            BigDecimal sumCost = BigDecimal.ZERO;
            Color rowAlt = new Color(249, 250, 251);

            for (int i = 0; i < data.size(); i++) {
                CallCostReportDTO row = data.get(i);
                sumCalls   += row.callCount();
                sumMinutes += row.totalMinutes();
                sumCost     = sumCost.add(row.totalCost());
                Color bg = (i % 2 == 1) ? rowAlt : Color.WHITE;

                addCell(table, row.customerName(),  bodyFont, Element.ALIGN_LEFT,  bg);
                addCell(table, row.circuitName(),   bodyFont, Element.ALIGN_LEFT,  bg);
                addCell(table, String.valueOf(row.callCount()),    bodyFont, Element.ALIGN_RIGHT, bg);
                addCell(table, String.valueOf(row.totalMinutes()), bodyFont, Element.ALIGN_RIGHT, bg);
                addCell(table, formatCurrency(row.totalCost()),   bodyFont, Element.ALIGN_RIGHT, bg);
            }

            // Rodapé totalizador
            Color footBg = new Color(243, 244, 246);
            PdfPCell footLabel = new PdfPCell(new Phrase(data.size() + " circuito(s)", footFont));
            footLabel.setColspan(2);
            footLabel.setBackgroundColor(footBg);
            footLabel.setBorder(Rectangle.TOP);
            footLabel.setPadding(6);
            table.addCell(footLabel);
            addCell(table, String.valueOf(sumCalls),   footFont, Element.ALIGN_RIGHT, footBg);
            addCell(table, String.valueOf(sumMinutes), footFont, Element.ALIGN_RIGHT, footBg);
            addCell(table, formatCurrency(sumCost),   footFont, Element.ALIGN_RIGHT, footBg);

            doc.add(table);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF", e);
        }
    }

    private void addCell(PdfPTable table, String text, Font font, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.TOP);
        cell.setPadding(6);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private String formatCurrency(BigDecimal value) {
        return "R$ " + String.format("%,.2f", value).replace(",", "X").replace(".", ",").replace("X", ".");
    }
}
