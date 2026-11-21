package com.netshield.ui;

import com.netshield.service.NetworkDiscovery;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Tab 4: Descubrimiento de Subred Local (/24) y Mapeo de Hosts Activos.
 */
public class NetworkDiscoveryTab {

    public static class DiscoveredHost {
        private final StringProperty ip;
        private final StringProperty hostname;
        private final StringProperty status;
        private final StringProperty pingTime;

        public DiscoveredHost(String ip, String hostname, String status, String pingTime) {
            this.ip = new SimpleStringProperty(ip);
            this.hostname = new SimpleStringProperty(hostname);
            this.status = new SimpleStringProperty(status);
            this.pingTime = new SimpleStringProperty(pingTime);
        }

        public String getIp() { return ip.get(); }
        public StringProperty ipProperty() { return ip; }
        public StringProperty hostnameProperty() { return hostname; }
        public StringProperty statusProperty() { return status; }
        public StringProperty pingTimeProperty() { return pingTime; }
    }

    private final Tab tab;
    private final ObservableList<DiscoveredHost> hostList = FXCollections.observableArrayList();
    private final ComboBox<String> cmbInterfaces = new ComboBox<>();
    private final TextField txtSubnetPrefix = new TextField("192.168.1");
    private final Button btnScan = new Button("Escanear Subred (/24)");
    private final Button btnSendToScanner = new Button("Enviar a Escáner");
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label lblStatus = new Label("Seleccione una interfaz o ingrese el prefijo /24 para descubrir hosts.");

    private Consumer<String> onHostSelectedForScan;
    private VBox rootContainer;

    public NetworkDiscoveryTab() {
        this.tab = new Tab("Descubrimiento de Red");
        this.tab.setClosable(false);

        buildUI();
        initInterfaces();
    }

    private void buildUI() {
        rootContainer = new VBox(12);
        rootContainer.setPadding(new Insets(14));

        // Panel de Control
        VBox controlCard = new VBox(10);
        controlCard.getStyleClass().add("card");

        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Label lblTitle = new Label("Explorador de Subred Local (/24)");
        lblTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #89b4fa;");
        headerRow.getChildren().add(lblTitle);

        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        cmbInterfaces.setPrefWidth(170);
        cmbInterfaces.setOnAction(e -> {
            String selected = cmbInterfaces.getValue();
            if (selected != null) {
                txtSubnetPrefix.setText(NetworkDiscovery.getSubnetPrefix(selected));
            }
        });

        txtSubnetPrefix.setPrefWidth(120);
        txtSubnetPrefix.setPromptText("ej. 192.168.1");

        btnScan.setGraphic(UiUtil.icon(UiUtil.PATH_NETWORK, "#11111b", 13));

        btnSendToScanner.getStyleClass().add("button-success");
        btnSendToScanner.setGraphic(UiUtil.icon(UiUtil.PATH_ROCKET, "#11111b", 13));
        btnSendToScanner.setDisable(true);
        btnSendToScanner.setOnAction(e -> sendSelectedHost());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actionRow.getChildren().addAll(
            new Label("Interfaz IP:"), cmbInterfaces,
            new Label("Prefijo /24:"), txtSubnetPrefix,
            btnScan,
            spacer,
            btnSendToScanner
        );

        controlCard.getChildren().addAll(headerRow, actionRow);

        // Barra de estado
        VBox progressBox = new VBox(4);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        lblStatus.setStyle("-fx-text-fill: #a6adc8; -fx-font-size: 11px;");
        progressBox.getChildren().addAll(progressBar, lblStatus);

        // Tabla de Hosts
        TableView<DiscoveredHost> table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            btnSendToScanner.setDisable(newV == null);
        });

        rootContainer.getChildren().addAll(controlCard, progressBox, table);
        tab.setContent(rootContainer);

        btnScan.setOnAction(e -> startDiscovery());
    }

    private void initInterfaces() {
        List<String> ips = NetworkDiscovery.getLocalIPv4Addresses();
        cmbInterfaces.getItems().addAll(ips);
        if (!ips.isEmpty()) {
            cmbInterfaces.setValue(ips.get(0));
            txtSubnetPrefix.setText(NetworkDiscovery.getSubnetPrefix(ips.get(0)));
        }
    }

    private TableView<DiscoveredHost> buildTable() {
        TableView<DiscoveredHost> table = new TableView<>(hostList);
        Label placeholder = new Label("No se han escaneado subredes aún. Presione 'Escanear Subred (/24)'.");
        placeholder.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 12px;");
        table.setPlaceholder(placeholder);

        TableColumn<DiscoveredHost, String> colIp = new TableColumn<>("Dirección IPv4");
        colIp.setCellValueFactory(c -> c.getValue().ipProperty());
        colIp.setPrefWidth(160);
        colIp.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-family: Consolas, monospace; -fx-text-fill: #cdd6f4;");
                }
            }
        });

        TableColumn<DiscoveredHost, String> colHost = new TableColumn<>("Nombre de Host / FQDN");
        colHost.setCellValueFactory(c -> c.getValue().hostnameProperty());
        colHost.setPrefWidth(280);

        TableColumn<DiscoveredHost, String> colStatus = new TableColumn<>("Estado");
        colStatus.setCellValueFactory(c -> c.getValue().statusProperty());
        colStatus.setPrefWidth(130);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(null);
                    setGraphic(UiUtil.badge("● " + item, "open"));
                }
            }
        });

        TableColumn<DiscoveredHost, String> colPing = new TableColumn<>("Respuesta ICMP");
        colPing.setCellValueFactory(c -> c.getValue().pingTimeProperty());
        colPing.setPrefWidth(150);
        colPing.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-family: Consolas, monospace; -fx-text-fill: #94e2d5;");
                }
            }
        });

        table.getColumns().addAll(colIp, colHost, colStatus, colPing);
        return table;
    }

    private void startDiscovery() {
        String prefix = txtSubnetPrefix.getText().trim();
        if (prefix.isEmpty()) {
            lblStatus.setText("Por favor ingrese un prefijo válido (ej. 192.168.1).");
            return;
        }

        hostList.clear();
        btnScan.setDisable(true);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        lblStatus.setText("Escaneando segmento " + prefix + ".1 - " + prefix + ".254 (ICMP Ping)...");

        CompletableFuture.runAsync(() -> {
            try {
                for (int i = 1; i <= 254; i++) {
                    String testIp = prefix + "." + i;
                    final int idx = i;

                    CompletableFuture.runAsync(() -> {
                        try {
                            InetAddress addr = InetAddress.getByName(testIp);
                            long start = System.currentTimeMillis();
                            if (addr.isReachable(300)) {
                                long ping = System.currentTimeMillis() - start;
                                String hostname = addr.getCanonicalHostName();
                                Platform.runLater(() -> hostList.add(new DiscoveredHost(testIp, hostname, "ACTIVO", ping + " ms")));
                            }
                        } catch (Exception ignored) {
                        }
                    });

                    if (i % 25 == 0) {
                        final double prog = i / 254.0;
                        Platform.runLater(() -> progressBar.setProgress(prog));
                    }
                }

                Thread.sleep(1200);

                Platform.runLater(() -> {
                    progressBar.setProgress(1.0);
                    lblStatus.setText(String.format("Descubrimiento completado: %d host(s) activos encontrados en %s.0/24",
                        hostList.size(), prefix));
                    btnScan.setDisable(false);
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al escanear subred: " + ex.getMessage());
                    btnScan.setDisable(false);
                    progressBar.setProgress(0);
                });
            }
        });
    }

    private void sendSelectedHost() {
        DiscoveredHost selected = hostList.stream()
            .filter(h -> h != null && h.getIp() != null)
            .findFirst()
            .orElse(null);

        if (selected != null && onHostSelectedForScan != null) {
            onHostSelectedForScan.accept(selected.getIp());
        }
    }

    public void setOnHostSelectedForScan(Consumer<String> cb) {
        this.onHostSelectedForScan = cb;
    }

    public Tab getTab() { return tab; }
    public VBox getRoot() { return rootContainer; }
}
