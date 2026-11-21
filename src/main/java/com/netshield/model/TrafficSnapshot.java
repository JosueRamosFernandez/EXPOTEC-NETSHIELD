package com.netshield.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Instantánea periódica de telemetría de tráfico de red, conexiones activas
 * y análisis de concentración/saturación por puerto.
 */
public record TrafficSnapshot(
        long timestamp,
        List<TrafficConnection> connections,
        double downloadRateKbps,
        double uploadRateKbps,
        int establishedCount,
        int listeningCount,
        int timeWaitCount,
        Map<Integer, Integer> topPortsActivity,
        List<PortTrafficSummary> portSummaries
) {
    public TrafficSnapshot {
        connections = Collections.unmodifiableList(connections);
        topPortsActivity = Collections.unmodifiableMap(topPortsActivity);
        portSummaries = portSummaries != null ? Collections.unmodifiableList(portSummaries) : List.of();
    }

    public int totalConnections() {
        return connections.size();
    }

    public long saturatedPortsCount() {
        return portSummaries.stream()
                .filter(p -> p.getSaturationLevel() != AlertLevel.NORMAL)
                .count();
    }

    public Optional<PortTrafficSummary> highestLoadPort() {
        return portSummaries.isEmpty() ? Optional.empty() : Optional.of(portSummaries.get(0));
    }
}
