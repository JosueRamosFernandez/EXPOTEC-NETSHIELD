package com.netshield.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Modelo que agrega los resultados de un escaneo completo para
 * la generación del reporte PDF/CSV/JSON, el panel de remediación y el Dashboard.
 */
public class ScanReport {

    private final String           targetIp;
    private final LocalDateTime    timestamp;
    private final HostScanConfig   config;
    private final LatencyBaseline  baseline;
    private final List<ScanResult> results;
    private final List<VulnerabilityInfo> vulnerabilities;

    // ── Estadísticas calculadas ─────────────────────────────────────────
    private int openPorts;
    private int closedPorts;
    private int filteredPorts;
    private int alertCount;
    private int criticalCount;
    private double scanDurationSeconds;

    // ── Constructor ─────────────────────────────────────────────────────
    public ScanReport(HostScanConfig config, LatencyBaseline baseline) {
        this.targetIp        = config.targetIp();
        this.timestamp       = LocalDateTime.now();
        this.config          = config;
        this.baseline        = baseline;
        this.results         = Collections.synchronizedList(new ArrayList<>());
        this.vulnerabilities = Collections.synchronizedList(new ArrayList<>());
    }

    // ── Registrar resultados (Sincronizado para concurrencia masiva) ────
    public synchronized void addResult(ScanResult result) {
        results.add(result);

        switch (result.getStatus()) {
            case OPEN     -> openPorts++;
            case CLOSED   -> closedPorts++;
            case FILTERED -> filteredPorts++;
        }
        if (result.getAlertLevel() == AlertLevel.WARNING)  alertCount++;
        if (result.getAlertLevel() == AlertLevel.CRITICAL) { alertCount++; criticalCount++; }
    }

    public synchronized void addVulnerability(VulnerabilityInfo vuln) {
        vulnerabilities.add(vuln);
    }

    // ── Getters y Métricas Estadísticas ─────────────────────────────────
    public String           getTargetIp()        { return targetIp;       }
    public LocalDateTime    getTimestamp()       { return timestamp;      }
    public HostScanConfig   getConfig()          { return config;         }
    public LatencyBaseline  getBaseline()        { return baseline;       }

    public synchronized List<ScanResult>        getResults()         { return new ArrayList<>(results); }
    public synchronized List<VulnerabilityInfo> getVulnerabilities() { return new ArrayList<>(vulnerabilities); }

    public synchronized int getOpenPorts()     { return openPorts;     }
    public synchronized int getClosedPorts()   { return closedPorts;   }
    public synchronized int getFilteredPorts() { return filteredPorts; }
    public synchronized int getAlertCount()    { return alertCount;    }
    public synchronized int getCriticalCount() { return criticalCount; }
    public synchronized int getTotalScanned()  { return results.size(); }

    public synchronized double getScanDurationSeconds() {
        return scanDurationSeconds;
    }

    public synchronized void setScanDurationSeconds(double duration) {
        this.scanDurationSeconds = duration;
    }

    /**
     * Calcula la latencia RTT promedio de los puertos abiertos detectados.
     */
    public synchronized double getAverageRtt() {
        return results.stream()
                .filter(r -> r.getStatus() == PortStatus.OPEN && r.getRttMillis() > 0)
                .mapToDouble(ScanResult::getRttMillis)
                .average()
                .orElse(0.0);
    }

    /** Indica si fue un escaneo completo de todo el rango de puertos (1 - 65,535). */
    public boolean isFullRangeScan() {
        return config.isFullRangeScan();
    }

    /** Timestamp formateado para encabezados de reportes. */
    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /** Resumen ejecutivo en texto. */
    public synchronized String getSummary() {
        String scopeText = isFullRangeScan() ? "ESCANEO COMPLETO (65,535 Puertos TCP)" : "Rango Personalizado";
        return String.format("""
                ═══════════════════════════════════════════
                  NETSHIELD UI — Resumen de Auditoría
                ═══════════════════════════════════════════
                  Objetivo     : %s
                  Alcance      : %s (%d - %d)
                  Fecha/Hora   : %s
                  Duración     : %.2f s
                  Puertos evaluados : %d
                  Puertos abiertos  : %d
                  Puertos cerrados  : %d
                  Puertos filtrados : %d
                  RTT Promedio (Open): %.3f ms
                  Alertas latencia  : %d  (Críticas: %d)
                  Vulnerabilidades  : %d
                  Baseline EMA      : %.3f ms
                ═══════════════════════════════════════════
                """,
                targetIp, scopeText, config.startPort(), config.endPort(),
                getFormattedTimestamp(), scanDurationSeconds,
                getTotalScanned(),
                openPorts, closedPorts, filteredPorts,
                getAverageRtt(),
                alertCount, criticalCount,
                vulnerabilities.size(),
                baseline.getEma());
    }
}
