package com.netshield.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Resumen de actividad y saturación agrupado por puerto de red.
 * Permite identificar qué puertos concentran mayor volumen de conexiones
 * o estados anómalos (TIME_WAIT, ráfagas concurrentes).
 */
public class PortTrafficSummary {

    private final SimpleIntegerProperty port;
    private final SimpleStringProperty serviceName;
    private final SimpleIntegerProperty totalConnections;
    private final SimpleIntegerProperty establishedCount;
    private final SimpleIntegerProperty timeWaitCount;
    private final SimpleDoubleProperty concentrationPercent;
    private final SimpleObjectProperty<AlertLevel> saturationLevel;

    public PortTrafficSummary(int port, String serviceName, int totalConnections,
                              int establishedCount, int timeWaitCount,
                              double concentrationPercent, AlertLevel saturationLevel) {
        this.port = new SimpleIntegerProperty(port);
        this.serviceName = new SimpleStringProperty(serviceName);
        this.totalConnections = new SimpleIntegerProperty(totalConnections);
        this.establishedCount = new SimpleIntegerProperty(establishedCount);
        this.timeWaitCount = new SimpleIntegerProperty(timeWaitCount);
        this.concentrationPercent = new SimpleDoubleProperty(concentrationPercent);
        this.saturationLevel = new SimpleObjectProperty<>(saturationLevel);
    }

    public int getPort() { return port.get(); }
    public SimpleIntegerProperty portProperty() { return port; }

    public String getServiceName() { return serviceName.get(); }
    public SimpleStringProperty serviceNameProperty() { return serviceName; }

    public int getTotalConnections() { return totalConnections.get(); }
    public SimpleIntegerProperty totalConnectionsProperty() { return totalConnections; }

    public int getEstablishedCount() { return establishedCount.get(); }
    public SimpleIntegerProperty establishedCountProperty() { return establishedCount; }

    public int getTimeWaitCount() { return timeWaitCount.get(); }
    public SimpleIntegerProperty timeWaitCountProperty() { return timeWaitCount; }

    public double getConcentrationPercent() { return concentrationPercent.get(); }
    public SimpleDoubleProperty concentrationPercentProperty() { return concentrationPercent; }

    public AlertLevel getSaturationLevel() { return saturationLevel.get(); }
    public SimpleObjectProperty<AlertLevel> saturationLevelProperty() { return saturationLevel; }
}
