package com.netshield.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Modelo reactivo que representa una conexión de red activa observada
 * durante el monitoreo de tráfico en tiempo real.
 */
public class TrafficConnection {

    private final SimpleStringProperty protocol;
    private final SimpleStringProperty localAddress;
    private final SimpleIntegerProperty localPort;
    private final SimpleStringProperty remoteAddress;
    private final SimpleIntegerProperty remotePort;
    private final SimpleStringProperty state;
    private final SimpleLongProperty pid;
    private final SimpleStringProperty processName;
    private final SimpleLongProperty packetsObserved;
    private final SimpleLongProperty bytesTransferred;

    public TrafficConnection(String protocol, String localAddress, int localPort,
                             String remoteAddress, int remotePort, String state,
                             long pid, String processName) {
        this.protocol = new SimpleStringProperty(protocol);
        this.localAddress = new SimpleStringProperty(localAddress);
        this.localPort = new SimpleIntegerProperty(localPort);
        this.remoteAddress = new SimpleStringProperty(remoteAddress);
        this.remotePort = new SimpleIntegerProperty(remotePort);
        this.state = new SimpleStringProperty(state);
        this.pid = new SimpleLongProperty(pid);
        this.processName = new SimpleStringProperty(processName);
        this.packetsObserved = new SimpleLongProperty(1);
        this.bytesTransferred = new SimpleLongProperty(0);
    }

    public String getProtocol() { return protocol.get(); }
    public SimpleStringProperty protocolProperty() { return protocol; }

    public String getLocalAddress() { return localAddress.get(); }
    public SimpleStringProperty localAddressProperty() { return localAddress; }

    public int getLocalPort() { return localPort.get(); }
    public SimpleIntegerProperty localPortProperty() { return localPort; }

    public String getRemoteAddress() { return remoteAddress.get(); }
    public SimpleStringProperty remoteAddressProperty() { return remoteAddress; }

    public int getRemotePort() { return remotePort.get(); }
    public SimpleIntegerProperty remotePortProperty() { return remotePort; }

    public String getState() { return state.get(); }
    public SimpleStringProperty stateProperty() { return state; }
    public void setState(String s) { this.state.set(s); }

    public long getPid() { return pid.get(); }
    public SimpleLongProperty pidProperty() { return pid; }

    public String getProcessName() { return processName.get(); }
    public SimpleStringProperty processNameProperty() { return processName; }

    public long getPacketsObserved() { return packetsObserved.get(); }
    public SimpleLongProperty packetsObservedProperty() { return packetsObserved; }
    public void incrementPackets() { this.packetsObserved.set(this.packetsObserved.get() + 1); }

    public long getBytesTransferred() { return bytesTransferred.get(); }
    public SimpleLongProperty bytesTransferredProperty() { return bytesTransferred; }
    public void addBytes(long bytes) { this.bytesTransferred.set(this.bytesTransferred.get() + bytes); }

    public String getKey() {
        return protocol.get() + ":" + localAddress.get() + ":" + localPort.get() + "->" + remoteAddress.get() + ":" + remotePort.get();
    }
}
