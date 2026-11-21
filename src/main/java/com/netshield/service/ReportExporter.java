package com.netshield.service;

import com.netshield.model.PortStatus;
import com.netshield.model.ScanReport;
import com.netshield.model.ScanResult;
import com.netshield.model.VulnerabilityInfo;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio encargado de generar informes ejecutivos y técnicos en PDF
 * a partir de un ScanReport utilizando Apache PDFBox 3.0 con soporte multi-página.
 */
public class ReportExporter {

    private static final float MARGIN = 45;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float BOTTOM_LIMIT = 65;

    public static void exportToPdf(ScanReport report, File destinationFile) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            int[] pageCounter = { 0 };
            float[] y = { PAGE_HEIGHT - MARGIN };

            PDPage[] curPage = { new PDPage(PDRectangle.A4) };
            doc.addPage(curPage[0]);
            PDPageContentStream[] cs = { new PDPageContentStream(doc, curPage[0]) };

            // === TÍTULO ===
            writeLine(cs[0], MARGIN, y, fontBold(), 17, "NetShield UI - Informe de Auditoría y Seguridad de Red");
            y[0] -= 4;
            writeLine(cs[0], MARGIN, y, font(), 10.5f, "Auditoría de Subred, Fingerprinting y Detección de Anomalías por Latencia");
            y[0] -= 6;
            drawLine(cs[0], MARGIN, y[0], PAGE_WIDTH - MARGIN);
            y[0] -= 16;

            // === 1. RESUMEN EJECUTIVO ===
            writeLine(cs[0], MARGIN, y, fontBold(), 12.5f, "1. Resumen Ejecutivo de la Auditoría");
            y[0] -= 4;

            String scopeText = report.isFullRangeScan()
                ? "ESCANEO COMPLETO DE TODOS LOS PUERTOS (1 - 65,535 TCP)"
                : report.getTotalScanned() + " puertos (" + report.getConfig().startPort() + " - " + report.getConfig().endPort() + ")";

            String[] summary = {
                "IP Objetivo: " + report.getTargetIp(),
                "Fecha y Hora: " + report.getFormattedTimestamp(),
                "Alcance de la Auditoría: " + scopeText,
                "Puertos Abiertos: " + report.getOpenPorts() + " | Cerrados: " + report.getClosedPorts() + " | Filtrados: " + report.getFilteredPorts(),
                "Alertas de Latencia (>= " + report.getConfig().anomalyThresholdMs() + " ms): " + report.getAlertCount() + " (Críticas: " + report.getCriticalCount() + ")",
                String.format("Línea Base de Latencia (EMA): %.3f ms", report.getBaseline().getEma()),
                String.format("Rango RTT Observado: Min = %.3f ms | Max = %.3f ms", report.getBaseline().getMinRtt(), report.getBaseline().getMaxRtt())
            };

            for (String line : summary) {
                writeLine(cs[0], MARGIN + 10, y, font(), 9, line);
            }
            y[0] -= 10;

            // === 2. TABLA DE PUERTOS ABIERTOS Y BANNERS ===
            List<ScanResult> openPorts = report.getResults().stream()
                .filter(r -> r.getStatus() == PortStatus.OPEN)
                .sorted(Comparator.comparingInt(ScanResult::getPort))
                .collect(Collectors.toList());

            writeLine(cs[0], MARGIN, y, fontBold(), 12.5f, "2. Puertos Abiertos y Fingerprinting (" + openPorts.size() + ")");
            y[0] -= 4;

            if (openPorts.isEmpty()) {
                writeLine(cs[0], MARGIN + 10, y, fontItalic(), 9, "No se detectaron puertos abiertos en el rango evaluado.");
            } else {
                writeLine(cs[0], MARGIN + 10, y, fontBold(), 8.5f,
                    String.format("%-7s %-12s %-22s %-10s %-10s %-10s", "Puerto", "Servicio", "Banner / Firma", "RTT(ms)", "Jitter", "Alerta"));
                y[0] -= 2;
                drawLine(cs[0], MARGIN + 10, y[0], PAGE_WIDTH - MARGIN - 10);
                y[0] -= 4;

                for (ScanResult r : openPorts) {
                    if (y[0] < BOTTOM_LIMIT) {
                        writeFooter(cs[0], ++pageCounter[0]);
                        cs[0].close();
                        curPage[0] = new PDPage(PDRectangle.A4);
                        doc.addPage(curPage[0]);
                        cs[0] = new PDPageContentStream(doc, curPage[0]);
                        y[0] = PAGE_HEIGHT - MARGIN;
                    }

                    String b = r.getBanner();
                    if (b.length() > 20) b = b.substring(0, 18) + "..";

                    String row = String.format("%-7d %-12s %-22s %-10.3f %-+10.3f %-10s",
                        r.getPort(), r.getServiceName(), b, r.getRttMillis(), r.getJitterMillis(), r.getAlertLevel().name());
                    writeLine(cs[0], MARGIN + 10, y, font(), 8, row);
                }
            }
            y[0] -= 10;

            // === 3. VULNERABILIDADES Y REMEDIACIONES ===
            if (y[0] < 130) {
                writeFooter(cs[0], ++pageCounter[0]);
                cs[0].close();
                curPage[0] = new PDPage(PDRectangle.A4);
                doc.addPage(curPage[0]);
                cs[0] = new PDPageContentStream(doc, curPage[0]);
                y[0] = PAGE_HEIGHT - MARGIN;
            }

            writeLine(cs[0], MARGIN, y, fontBold(), 12.5f,
                "3. Vulnerabilidades Detectadas y Plan de Remediación (" + report.getVulnerabilities().size() + ")");
            y[0] -= 4;

            if (report.getVulnerabilities().isEmpty()) {
                writeLine(cs[0], MARGIN + 10, y, fontItalic(), 9,
                    "No se detectaron servicios de riesgo catalogados en los puertos abiertos evaluados.");
            } else {
                for (VulnerabilityInfo v : report.getVulnerabilities()) {
                    if (y[0] < 90) {
                        writeFooter(cs[0], ++pageCounter[0]);
                        cs[0].close();
                        curPage[0] = new PDPage(PDRectangle.A4);
                        doc.addPage(curPage[0]);
                        cs[0] = new PDPageContentStream(doc, curPage[0]);
                        y[0] = PAGE_HEIGHT - MARGIN;
                    }

                    writeLine(cs[0], MARGIN + 10, y, fontBold(), 9,
                        String.format("[%s] Puerto %d (%s) - Referencia: %s", v.riskLevel(), v.port(), v.serviceName(), v.cveReference()));

                    String desc = v.description();
                    if (desc.length() > 95) desc = desc.substring(0, 92) + "...";
                    writeLine(cs[0], MARGIN + 18, y, font(), 8, "Diagnóstico: " + desc);

                    String rem = v.remediation();
                    if (rem.length() > 95) rem = rem.substring(0, 92) + "...";
                    writeLine(cs[0], MARGIN + 18, y, font(), 8, "Remediación: " + rem);
                    y[0] -= 5;
                }
            }

            writeFooter(cs[0], ++pageCounter[0]);
            cs[0].close();

            doc.save(destinationFile);
        }
    }

    private static void writeLine(PDPageContentStream cs, float x, float[] y,
                                  PDType1Font f, float size, String text) throws IOException {
        cs.beginText();
        cs.setFont(f, size);
        cs.newLineAtOffset(x, y[0]);
        cs.showText(text);
        cs.endText();
        y[0] -= (size + 4.5f);
    }

    private static void drawLine(PDPageContentStream cs, float x1, float y, float x2) throws IOException {
        cs.setLineWidth(0.6f);
        cs.moveTo(x1, y);
        cs.lineTo(x2, y);
        cs.stroke();
    }

    private static void writeFooter(PDPageContentStream cs, int pageNum) throws IOException {
        cs.beginText();
        cs.setFont(fontItalic(), 8);
        cs.newLineAtOffset(MARGIN, 28);
        cs.showText("NetShield UI - Herramienta Académica de Ciberseguridad | Página " + pageNum);
        cs.endText();
    }

    private static PDType1Font font()       { return new PDType1Font(Standard14Fonts.FontName.HELVETICA); }
    private static PDType1Font fontBold()   { return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD); }
    private static PDType1Font fontItalic() { return new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE); }
}
