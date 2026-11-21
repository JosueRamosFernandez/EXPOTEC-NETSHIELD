package com.netshield.service;

import com.netshield.model.ScanReport;
import com.netshield.model.ScanResult;
import com.netshield.model.VulnerabilityInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * <h2>Exportador de Datos a Formatos CSV y JSON</h2>
 * <p>
 * Permite la interoperabilidad del informe de auditoría con hojas de cálculo (Excel/Calc)
 * y plataformas SIEM / SOC (Splunk, Elastic, Graylog).
 * </p>
 */
public class DataExporter {

    /**
     * Exporta los resultados a formato CSV delimitado por comas.
     */
    public static void exportToCsv(ScanReport report, File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            writer.println("IP,Puerto,Servicio,Banner,Estado,RTT_ms,Jitter_ms,Alerta,Remediacion");

            for (ScanResult r : report.getResults()) {
                writer.printf("\"%s\",%d,\"%s\",\"%s\",\"%s\",%.3f,%.3f,\"%s\",\"%s\"%n",
                    escapeCsv(r.getIpAddress()),
                    r.getPort(),
                    escapeCsv(r.getServiceName()),
                    escapeCsv(r.getBanner()),
                    r.getStatus().name(),
                    r.getRttMillis(),
                    r.getJitterMillis(),
                    r.getAlertLevel().name(),
                    escapeCsv(r.getRemediation())
                );
            }
        }
    }

    /**
     * Exporta los resultados a formato JSON estructurado.
     */
    public static void exportToJson(ScanReport report, File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            writer.println("{");
            writer.printf("  \"targetIp\": \"%s\",%n", report.getTargetIp());
            writer.printf("  \"timestamp\": \"%s\",%n", report.getFormattedTimestamp());
            writer.println("  \"summary\": {");
            writer.printf("    \"totalScanned\": %d,%n", report.getTotalScanned());
            writer.printf("    \"openPorts\": %d,%n", report.getOpenPorts());
            writer.printf("    \"closedPorts\": %d,%n", report.getClosedPorts());
            writer.printf("    \"filteredPorts\": %d,%n", report.getFilteredPorts());
            writer.printf("    \"alertCount\": %d,%n", report.getAlertCount());
            writer.printf("    \"criticalCount\": %d,%n", report.getCriticalCount());
            writer.printf("    \"baselineEma\": %.3f%n", report.getBaseline().getEma());
            writer.println("  },");

            // Puertos
            writer.println("  \"ports\": [");
            for (int i = 0; i < report.getResults().size(); i++) {
                ScanResult r = report.getResults().get(i);
                writer.println("    {");
                writer.printf("      \"ip\": \"%s\",%n", escapeJson(r.getIpAddress()));
                writer.printf("      \"port\": %d,%n", r.getPort());
                writer.printf("      \"service\": \"%s\",%n", escapeJson(r.getServiceName()));
                writer.printf("      \"banner\": \"%s\",%n", escapeJson(r.getBanner()));
                writer.printf("      \"status\": \"%s\",%n", r.getStatus().name());
                writer.printf("      \"rttMs\": %.3f,%n", r.getRttMillis());
                writer.printf("      \"jitterMs\": %.3f,%n", r.getJitterMillis());
                writer.printf("      \"alert\": \"%s\",%n", r.getAlertLevel().name());
                writer.printf("      \"remediation\": \"%s\"%n", escapeJson(r.getRemediation()));
                writer.print("    }");
                if (i < report.getResults().size() - 1) writer.print(",");
                writer.println();
            }
            writer.println("  ],");

            // Vulnerabilidades
            writer.println("  \"vulnerabilities\": [");
            for (int i = 0; i < report.getVulnerabilities().size(); i++) {
                VulnerabilityInfo v = report.getVulnerabilities().get(i);
                writer.println("    {");
                writer.printf("      \"port\": %d,%n", v.port());
                writer.printf("      \"service\": \"%s\",%n", escapeJson(v.serviceName()));
                writer.printf("      \"risk\": \"%s\",%n", escapeJson(v.riskLevel()));
                writer.printf("      \"cve\": \"%s\",%n", escapeJson(v.cveReference()));
                writer.printf("      \"description\": \"%s\",%n", escapeJson(v.description()));
                writer.printf("      \"remediation\": \"%s\"%n", escapeJson(v.remediation()));
                writer.print("    }");
                if (i < report.getVulnerabilities().size() - 1) writer.print(",");
                writer.println();
            }
            writer.println("  ]");
            writer.println("}");
        }
    }

    private static String escapeCsv(String str) {
        if (str == null) return "";
        return str.replace("\"", "\"\"");
    }

    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
