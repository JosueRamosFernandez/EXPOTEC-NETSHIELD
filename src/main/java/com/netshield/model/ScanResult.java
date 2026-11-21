package com.netshield.model;

import javafx.beans.property.*;

/**
 * Modelo observable (JavaFX Properties) que representa el resultado del
 * escaneo de un puerto TCP individual, incluyendo Banner Grab / Fingerprinting.
 */
public class ScanResult {

    // ── Propiedades observables ──────────────────────────────────────────
    private final StringProperty  ipAddress;
    private final IntegerProperty port;
    private final ObjectProperty<PortStatus> status;
    private final StringProperty  serviceName;
    private final DoubleProperty  rttMillis;        // Round-Trip Time en ms
    private final DoubleProperty  jitterMillis;     // |RTT − baseline| en ms
    private final ObjectProperty<AlertLevel> alertLevel;
    private final StringProperty  remediation;      // Recomendación técnica
    private final StringProperty  banner;           // Banner / Versión detectada

    // ── Constructor completo ────────────────────────────────────────────
    public ScanResult(String ipAddress, int port, PortStatus status,
                      String serviceName, double rttMillis, double jitterMillis,
                      AlertLevel alertLevel, String remediation, String banner) {
        this.ipAddress    = new SimpleStringProperty(ipAddress);
        this.port         = new SimpleIntegerProperty(port);
        this.status       = new SimpleObjectProperty<>(status);
        this.serviceName  = new SimpleStringProperty(serviceName);
        this.rttMillis    = new SimpleDoubleProperty(rttMillis);
        this.jitterMillis = new SimpleDoubleProperty(jitterMillis);
        this.alertLevel   = new SimpleObjectProperty<>(alertLevel);
        this.remediation  = new SimpleStringProperty(remediation);
        this.banner       = new SimpleStringProperty(banner != null ? banner : "N/A");
    }

    public ScanResult(String ipAddress, int port, PortStatus status,
                      String serviceName, double rttMillis, double jitterMillis,
                      AlertLevel alertLevel, String remediation) {
        this(ipAddress, port, status, serviceName, rttMillis, jitterMillis, alertLevel, remediation, "N/A");
    }

    // ── Getters de valor ────────────────────────────────────────────────
    public String     getIpAddress()    { return ipAddress.get();    }
    public int        getPort()         { return port.get();         }
    public PortStatus getStatus()       { return status.get();       }
    public String     getServiceName()  { return serviceName.get();  }
    public double     getRttMillis()    { return rttMillis.get();    }
    public double     getJitterMillis() { return jitterMillis.get(); }
    public AlertLevel getAlertLevel()   { return alertLevel.get();   }
    public String     getRemediation()  { return remediation.get();  }
    public String     getBanner()       { return banner.get();       }

    // ── Setters ─────────────────────────────────────────────────────────
    public void setAlertLevel(AlertLevel level) { this.alertLevel.set(level);   }
    public void setRemediation(String rem)      { this.remediation.set(rem);    }
    public void setJitterMillis(double j)       { this.jitterMillis.set(j);     }
    public void setBanner(String b)             { this.banner.set(b != null ? b : "N/A"); }

    // ── Properties (para TableView cell-value factories) ────────────────
    public StringProperty  ipAddressProperty()    { return ipAddress;    }
    public IntegerProperty portProperty()         { return port;         }
    public ObjectProperty<PortStatus> statusProperty() { return status;  }
    public StringProperty  serviceNameProperty()  { return serviceName;  }
    public DoubleProperty  rttMillisProperty()    { return rttMillis;    }
    public DoubleProperty  jitterMillisProperty() { return jitterMillis; }
    public ObjectProperty<AlertLevel> alertLevelProperty() { return alertLevel; }
    public StringProperty  remediationProperty()  { return remediation;  }
    public StringProperty  bannerProperty()       { return banner;       }

    // ── toString ────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return String.format("[%s] %s:%d  %-8s  RTT=%.3f ms  Jitter=%.3f ms  Banner=[%s]  %s",
                alertLevel.get(), ipAddress.get(), port.get(),
                status.get(), rttMillis.get(), jitterMillis.get(),
                banner.get(), serviceName.get());
    }
}
