package com.netshield.ui;

import com.netshield.model.*;
import com.netshield.service.LatencyScannerService;
import com.netshield.service.NetworkDiscovery;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Tab 1: Dashboard de Control y Resultados en Vivo con Perfiles Rápidos,
 * opción de Análisis de Todos los Puertos (1 - 65,535), Banner Grabbing,
 * tarjetas de métricas en tiempo real con estadísticas discriminadas (Abiertos, Cerrados y Filtrados)
 * y barra de búsqueda interactiva.
 */
public class DashboardTab {

    private final Tab tab;
    private final ObservableList<ScanResult> tableData = FXCollections.observableArrayList();
    private FilteredList<ScanResult> filteredData;
    private final LatencyScannerService scannerService;

    // Tarjetas de Métricas en cabecera con subtítulos dinámicos
    private final Label lblOpenPorts = new Label("0");
    private final Label lblOpenSub = new Label("Cerrados: 0 | Filtrados: 0");

    private final Label lblAlerts = new Label("0");
    private final Label lblAlertsSub = new Label("Críticas: 0 | Advertencias: 0");

    private final Label lblBaseline = new Label("0.00 ms");
    private final Label lblBaselineSub = new Label("RTT Promedio: -- ms");

    private final Label lblScanned = new Label("0 / 0");
    private final Label lblScannedSub = new Label("Tiempo: 0.0s");

    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label lblStatus = new Label("Listo para iniciar escaneo");

    // Controles de configuración
    private final TextField txtTargetIp = new TextField("127.0.0.1");
    private final TextField txtStartPort = new TextField("8079");
    private final TextField txtEndPort = new TextField("8085");
    private final TextField txtTimeout = new TextField("150");
    private final TextField txtThreshold = new TextField("5.0");
    private final ComboBox<String> cmbPresets = new ComboBox<>();
    private final Button btnDetectIp = new Button("Mi IP");
    private final Button btnLocalhost = new Button("Localhost");
    private final Button btnFullRange = new Button("Todos los Puertos (1 - 65,535)");
    private final CheckBox chkHighSpeed = new CheckBox("Concurrencia Alta (150 hilos)");
    private final Button btnStart = new Button("Iniciar Escaneo");
    private final Button btnCancel = new Button("Cancelar");

    // Barra de búsqueda y filtrado rápido
    private final TextField txtSearch = new TextField();
    private final ToggleGroup filterGroup = new ToggleGroup();
    private final RadioButton rbFilterAll = new RadioButton("Todos");
    private final RadioButton rbFilterOpen = new RadioButton("Solo Abiertos");
    private final RadioButton rbFilterAlerts = new RadioButton("Con Alertas");
    private final Label lblResultCount = new Label("0 puertos");

    private Consumer<ScanResult> onResultCallback;
    private Consumer<ScanReport> onScanCompleteCallback;
    private final AtomicInteger alertCounter = new AtomicInteger(0);
    private final AtomicInteger criticalCounter = new AtomicInteger(0);
    private long scanStartTime;
    private VBox rootContainer;

    public DashboardTab(LatencyScannerService scannerService) {
        this.scannerService = scannerService;
        this.tab = new Tab("Dashboard & Escaneo");
        this.tab.setClosable(false);

        buildUI();
    }

    private void buildUI() {
        rootContainer = new VBox(12);
        rootContainer.setPadding(new Insets(14));

        // 1. Tarjeta de Configuración y Control
        VBox controlCard = new VBox(10);
        controlCard.getStyleClass().add("card");

        // Fila 1: Presets y botones de acceso rápido
        HBox presetBar = new HBox(10);
        presetBar.setAlignment(Pos.CENTER_LEFT);

        cmbPresets.getItems().addAll(
            "⚡ Testbed Local (8079 - 8085)",
            "🌐 Puertos Well-Known (1 - 1024)",
            "🖥️ Servicios Web & Red (21 - 445)",
            "🗄️ Bases de Datos & Cache (3306 - 6379)",
            "🚀 Rango Completo (1 - 65,535: Todos los Puertos)"
        );
        cmbPresets.setValue("⚡ Testbed Local (8079 - 8085)");
        cmbPresets.setPrefWidth(280);
        cmbPresets.setOnAction(e -> applyPreset(cmbPresets.getValue()));

        btnFullRange.getStyleClass().add("button-purple");
        btnFullRange.setGraphic(UiUtil.icon(UiUtil.PATH_ROCKET, "#11111b", 14));
        btnFullRange.setOnAction(e -> selectFullRange());

        btnDetectIp.getStyleClass().add("button-secondary");
        btnDetectIp.setGraphic(UiUtil.icon(UiUtil.PATH_SEARCH, "#89b4fa", 13));
        btnDetectIp.setOnAction(e -> detectMyIp());

        btnLocalhost.getStyleClass().add("button-secondary");
        btnLocalhost.setOnAction(e -> {
            txtTargetIp.setText("127.0.0.1");
            lblStatus.setText("Destino configurado a Localhost (127.0.0.1).");
        });

        chkHighSpeed.setSelected(true);
        chkHighSpeed.setStyle("-fx-text-fill: #a6adc8; -fx-font-size: 11px;");

        Region spacerPreset = new Region();
        HBox.setHgrow(spacerPreset, Priority.ALWAYS);

        presetBar.getChildren().addAll(
            new Label("Perfil Rápido:"), cmbPresets,
            btnFullRange,
            btnDetectIp,
            btnLocalhost,
            spacerPreset,
            chkHighSpeed
        );

        // Fila 2: Parámetros del escaneo y botones de acción
        HBox paramBar = new HBox(10);
        paramBar.setAlignment(Pos.CENTER_LEFT);

        txtTargetIp.setPromptText("IP Objetivo");
        txtTargetIp.setPrefWidth(125);

        txtStartPort.setPromptText("Inicio");
        txtStartPort.setPrefWidth(65);

        txtEndPort.setPromptText("Fin");
        txtEndPort.setPrefWidth(70);

        txtTimeout.setPromptText("Timeout");
        txtTimeout.setPrefWidth(65);

        txtThreshold.setPromptText("Umbral");
        txtThreshold.setPrefWidth(65);

        btnStart.setGraphic(UiUtil.icon(UiUtil.PATH_PLAY, "#11111b", 13));

        btnCancel.getStyleClass().add("button-danger");
        btnCancel.setGraphic(UiUtil.icon(UiUtil.PATH_STOP, "#11111b", 12));
        btnCancel.setDisable(true);

        Region spacerParam = new Region();
        HBox.setHgrow(spacerParam, Priority.ALWAYS);

        paramBar.getChildren().addAll(
            new Label("IP:"), txtTargetIp,
            new Label("Puertos:"), txtStartPort, new Label("-"), txtEndPort,
            new Label("Timeout (ms):"), txtTimeout,
            new Label("Umbral Δ (ms):"), txtThreshold,
            spacerParam,
            btnStart, btnCancel
        );

        controlCard.getChildren().addAll(presetBar, paramBar);

        // 2. Tarjetas de Métricas Rápidas (KPIs) con subtítulos reactivos
        HBox metricsCards = new HBox(12);
        metricsCards.getChildren().addAll(
            UiUtil.metricCard("PUERTOS ABIERTOS", lblOpenPorts, lblOpenSub, "#a6e3a1",
                UiUtil.icon(UiUtil.PATH_CHECK, "#a6e3a1", 14)),
            UiUtil.metricCard("ALERTAS DE LATENCIA", lblAlerts, lblAlertsSub, "#f38ba8",
                UiUtil.icon(UiUtil.PATH_WARNING, "#f38ba8", 14)),
            UiUtil.metricCard("LÍNEA BASE (EMA)", lblBaseline, lblBaselineSub, "#89b4fa",
                UiUtil.icon(UiUtil.PATH_CHART, "#89b4fa", 14)),
            UiUtil.metricCard("PUERTOS EVALUADOS", lblScanned, lblScannedSub, "#cba6f7",
                UiUtil.icon(UiUtil.PATH_BOLT, "#cba6f7", 14))
        );

        // 3. Barra de progreso y estado
        VBox progressBox = new VBox(4);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        lblStatus.setStyle("-fx-text-fill: #a6adc8; -fx-font-size: 11px;");
        progressBox.getChildren().addAll(progressBar, lblStatus);

        // 4. Barra de Búsqueda y Filtro Rápido de la Tabla
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(2, 0, 2, 0));

        txtSearch.setPromptText("Buscar por puerto, servicio, banner o estado...");
        txtSearch.setPrefWidth(300);
        txtSearch.textProperty().addListener((obs, oldV, newV) -> updateFilter());

        rbFilterAll.setToggleGroup(filterGroup);
        rbFilterAll.setSelected(true);
        rbFilterOpen.setToggleGroup(filterGroup);
        rbFilterAlerts.setToggleGroup(filterGroup);

        rbFilterAll.setOnAction(e -> updateFilter());
        rbFilterOpen.setOnAction(e -> updateFilter());
        rbFilterAlerts.setOnAction(e -> updateFilter());

        Region filterSpacer = new Region();
        HBox.setHgrow(filterSpacer, Priority.ALWAYS);

        lblResultCount.setStyle("-fx-text-fill: #89b4fa; -fx-font-size: 11px; -fx-font-weight: bold;");

        filterBar.getChildren().addAll(
            UiUtil.icon(UiUtil.PATH_FILTER, "#89b4fa", 14),
            txtSearch,
            rbFilterAll, rbFilterOpen, rbFilterAlerts,
            filterSpacer,
            lblResultCount
        );

        // 5. Tabla de Resultados
        TableView<ScanResult> table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        rootContainer.getChildren().addAll(controlCard, metricsCards, progressBox, filterBar, table);
        tab.setContent(rootContainer);

        // Eventos
        btnStart.setOnAction(e -> startScan());
        btnCancel.setOnAction(e -> cancelScan());
    }

    private void updateFilter() {
        if (filteredData == null) return;
        String query = txtSearch.getText().toLowerCase().trim();
        boolean onlyOpen = rbFilterOpen.isSelected();
        boolean onlyAlerts = rbFilterAlerts.isSelected();

        filteredData.setPredicate(res -> {
            if (onlyOpen && res.getStatus() != PortStatus.OPEN) return false;
            if (onlyAlerts && res.getAlertLevel() == AlertLevel.NORMAL) return false;

            if (query.isEmpty()) return true;

            String portStr = String.valueOf(res.getPort());
            String servStr = res.getServiceName() != null ? res.getServiceName().toLowerCase() : "";
            String bannerStr = res.getBanner() != null ? res.getBanner().toLowerCase() : "";
            String ipStr = res.getIpAddress() != null ? res.getIpAddress().toLowerCase() : "";
            String statusStr = res.getStatus() != null ? res.getStatus().name().toLowerCase() : "";

            return portStr.contains(query) || servStr.contains(query) || bannerStr.contains(query) || ipStr.contains(query) || statusStr.contains(query);
        });

        lblResultCount.setText("Resultados visibles: " + filteredData.size() + " / " + tableData.size());
    }

    public void selectFullRange() {
        txtStartPort.setText("1");
        txtEndPort.setText("65535");
        txtTimeout.setText("80");
        txtThreshold.setText("5.0");
        chkHighSpeed.setSelected(true);
        cmbPresets.setValue("🚀 Rango Completo (1 - 65,535: Todos los Puertos)");
        lblStatus.setText("Modo Rango Total activado: 65,535 puertos TCP configurados con alta concurrencia.");
    }

    private void applyPreset(String preset) {
        if (preset == null) return;
        if (preset.contains("Testbed")) {
            txtTargetIp.setText("127.0.0.1");
            txtStartPort.setText("8079");
            txtEndPort.setText("8085");
            txtTimeout.setText("150");
            txtThreshold.setText("5.0");
        } else if (preset.contains("Well-Known")) {
            txtStartPort.setText("1");
            txtEndPort.setText("1024");
            txtTimeout.setText("120");
            txtThreshold.setText("5.0");
        } else if (preset.contains("Servicios Web")) {
            txtStartPort.setText("21");
            txtEndPort.setText("445");
            txtTimeout.setText("150");
            txtThreshold.setText("5.0");
        } else if (preset.contains("Bases de Datos")) {
            txtStartPort.setText("3306");
            txtEndPort.setText("6379");
            txtTimeout.setText("180");
            txtThreshold.setText("5.0");
        } else if (preset.contains("Rango Completo")) {
            selectFullRange();
        }
    }

    public void setTargetIp(String ip) {
        txtTargetIp.setText(ip);
    }

    public void configureTarget(String ip, int start, int end) {
        txtTargetIp.setText(ip);
        txtStartPort.setText(String.valueOf(start));
        txtEndPort.setText(String.valueOf(end));
        txtTimeout.setText("150");
        txtThreshold.setText("5.0");
        cmbPresets.setValue("⚡ Testbed Local (8079 - 8085)");
        lblStatus.setText(String.format("Objetivo configurado: %s (Puertos %d a %d)", ip, start, end));
    }

    private void detectMyIp() {
        List<String> ips = NetworkDiscovery.getLocalIPv4Addresses();
        if (!ips.isEmpty()) {
            txtTargetIp.setText(ips.get(0));
            lblStatus.setText("IP local asignada: " + ips.get(0) + " (Subred: " + NetworkDiscovery.getSubnetPrefix(ips.get(0)) + ".0/24)");
        }
    }

    @SuppressWarnings("unchecked")
    private TableView<ScanResult> buildTable() {
        filteredData = new FilteredList<>(tableData, p -> true);
        SortedList<ScanResult> sortedData = new SortedList<>(filteredData);

        TableView<ScanResult> table = new TableView<>(sortedData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());

        Label placeholder = new Label("No hay resultados de escaneo aún. Inicie un escaneo para visualizar.");
        placeholder.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 12px;");
        table.setPlaceholder(placeholder);

        TableColumn<ScanResult, String> colIp = new TableColumn<>("IP");
        colIp.setCellValueFactory(c -> c.getValue().ipAddressProperty());
        colIp.setPrefWidth(100);

        TableColumn<ScanResult, Integer> colPort = new TableColumn<>("Puerto");
        colPort.setCellValueFactory(cell -> cell.getValue().portProperty().asObject());
        colPort.setPrefWidth(75);

        TableColumn<ScanResult, String> colService = new TableColumn<>("Servicio");
        colService.setCellValueFactory(cell -> cell.getValue().serviceNameProperty());
        colService.setPrefWidth(120);

        TableColumn<ScanResult, String> colBanner = new TableColumn<>("Banner / Fingerprint");
        colBanner.setCellValueFactory(cell -> cell.getValue().bannerProperty());
        colBanner.setPrefWidth(170);
        colBanner.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || "N/A".equals(item)) {
                    setText(empty ? null : "—");
                    setGraphic(null);
                    setStyle("-fx-text-fill: #6c7086;");
                } else {
                    setText(item);
                    setGraphic(null);
                    setStyle("-fx-text-fill: #94e2d5; -fx-font-family: Consolas, monospace;");
                }
            }
        });

        TableColumn<ScanResult, PortStatus> colStatus = new TableColumn<>("Estado");
        colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());
        colStatus.setPrefWidth(95);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(PortStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(null);
                    if (item == PortStatus.OPEN) {
                        setGraphic(UiUtil.badge("● ABIERTO", "open"));
                    } else if (item == PortStatus.CLOSED) {
                        setGraphic(UiUtil.badge("○ CERRADO", "closed"));
                    } else {
                        setGraphic(UiUtil.badge("◐ FILTRADO", "warning"));
                    }
                }
            }
        });

        TableColumn<ScanResult, Number> colRtt = new TableColumn<>("RTT (ms)");
        colRtt.setCellValueFactory(cell -> cell.getValue().rttMillisProperty());
        colRtt.setPrefWidth(90);
        colRtt.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.3f", item.doubleValue()));
                    setStyle("-fx-font-family: Consolas, monospace;");
                }
            }
        });

        TableColumn<ScanResult, Number> colJitter = new TableColumn<>("Jitter (ms)");
        colJitter.setCellValueFactory(cell -> cell.getValue().jitterMillisProperty());
        colJitter.setPrefWidth(95);
        colJitter.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    double val = item.doubleValue();
                    setText(String.format("%+.3f", val));
                    if (val >= 5.0) {
                        setStyle("-fx-text-fill: #f38ba8; -fx-font-weight: bold; -fx-font-family: Consolas, monospace;");
                    } else if (val >= 2.0) {
                        setStyle("-fx-text-fill: #fab387; -fx-font-family: Consolas, monospace;");
                    } else {
                        setStyle("-fx-text-fill: #a6adc8; -fx-font-family: Consolas, monospace;");
                    }
                }
            }
        });

        TableColumn<ScanResult, AlertLevel> colAlert = new TableColumn<>("Alerta Latencia");
        colAlert.setCellValueFactory(cell -> cell.getValue().alertLevelProperty());
        colAlert.setPrefWidth(125);
        colAlert.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(AlertLevel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(null);
                    if (item == AlertLevel.CRITICAL) {
                        setGraphic(UiUtil.badge("● CRÍTICO", "critical"));
                    } else if (item == AlertLevel.WARNING) {
                        setGraphic(UiUtil.badge("▲ ADVERTENCIA", "warning"));
                    } else {
                        setGraphic(UiUtil.badge("● Normal", "normal"));
                    }
                }
            }
        });

        TableColumn<ScanResult, String> colRemediation = new TableColumn<>("Remediación Recomendada");
        colRemediation.setCellValueFactory(cell -> cell.getValue().remediationProperty());
        colRemediation.setPrefWidth(320);

        table.getColumns().addAll(colIp, colPort, colService, colBanner, colStatus, colRtt, colJitter, colAlert, colRemediation);
        return table;
    }

    private void startScan() {
        try {
            String ip = txtTargetIp.getText().trim();
            int start = Integer.parseInt(txtStartPort.getText().trim());
            int end = Integer.parseInt(txtEndPort.getText().trim());
            int timeout = Integer.parseInt(txtTimeout.getText().trim());
            double threshold = Double.parseDouble(txtThreshold.getText().trim());

            if (start < 1 || end > 65535 || start > end) {
                throw new IllegalArgumentException("Rango de puertos inválido (debe ser entre 1 y 65535).");
            }

            int threads = chkHighSpeed.isSelected() ? 150 : 30;
            HostScanConfig config = new HostScanConfig(ip, start, end, timeout, threads, threshold);

            tableData.clear();
            alertCounter.set(0);
            criticalCounter.set(0);
            lblOpenPorts.setText("0");
            lblOpenSub.setText("Cerrados: 0 | Filtrados: 0");
            lblAlerts.setText("0");
            lblAlertsSub.setText("Críticas: 0 | Advertencias: 0");
            lblBaseline.setText("0.00 ms");
            lblBaselineSub.setText("RTT Promedio: -- ms");
            lblScanned.setText("0 / " + config.totalPorts());
            lblScannedSub.setText("Tiempo: 0.0s");
            progressBar.setProgress(0);
            btnStart.setDisable(true);
            btnCancel.setDisable(false);
            scanStartTime = System.currentTimeMillis();

            // Callback para puertos abiertos o con alerta: se agregan de inmediato a la tabla
            scannerService.setOnResult((result, bl) -> Platform.runLater(() -> {
                tableData.add(result);
                updateFilter();
                if (result.getStatus() == PortStatus.OPEN) {
                    lblOpenPorts.setText(String.valueOf(tableData.stream().filter(r -> r.getStatus() == PortStatus.OPEN).count()));
                }
                if (result.getAlertLevel() == AlertLevel.CRITICAL) {
                    criticalCounter.incrementAndGet();
                }
                if (result.getAlertLevel() != AlertLevel.NORMAL) {
                    int totalAlerts = alertCounter.incrementAndGet();
                    lblAlerts.setText(String.valueOf(totalAlerts));
                    lblAlertsSub.setText(String.format("Críticas: %d | Advertencias: %d",
                        criticalCounter.get(), totalAlerts - criticalCounter.get()));
                }
                if (bl != null && bl.getEma() > 0) {
                    lblBaseline.setText(String.format("%.2f ms", bl.getEma()));
                }
                if (onResultCallback != null) {
                    onResultCallback.accept(result);
                }
            }));

            // Callback detallado adaptativo por lotes (telemetría completa)
            scannerService.setOnDetailedProgress((scanned, total, open, closed, filtered, ema, percent) -> Platform.runLater(() -> {
                progressBar.setProgress(percent);
                lblScanned.setText(scanned + " / " + total);
                lblOpenPorts.setText(String.valueOf(open));
                lblOpenSub.setText(String.format("Cerrados: %,d | Filtrados: %,d", closed, filtered));
                if (ema > 0) {
                    lblBaseline.setText(String.format("%.2f ms", ema));
                }
                double elapsed = (System.currentTimeMillis() - scanStartTime) / 1000.0;
                lblScannedSub.setText(String.format("%.1fs transcurridos", elapsed));
                lblStatus.setText(String.format("Escaneando %s... Progreso: %.1f%% (%d de %d) | Abiertos: %d | Cerrados: %,d | Filtrados: %,d",
                    ip, percent * 100.0, scanned, total, open, closed, filtered));
            }));

            // Finalización del escaneo
            scannerService.setOnComplete(report -> Platform.runLater(() -> {
                btnStart.setDisable(false);
                btnCancel.setDisable(true);
                progressBar.setProgress(1.0);
                double elapsed = report.getScanDurationSeconds();
                lblScannedSub.setText(String.format("Completado en %.1fs", elapsed));
                lblBaselineSub.setText(String.format("RTT Promedio: %.2f ms", report.getAverageRtt()));
                lblAlertsSub.setText(String.format("Críticas: %d | Advertencias: %d",
                    report.getCriticalCount(), report.getAlertCount() - report.getCriticalCount()));
                lblStatus.setText(String.format("✓ Escaneo completado en %.1fs | Evaluados: %,d | Abiertos: %d | Cerrados: %,d | Filtrados: %,d | RTT Medio: %.2f ms",
                    elapsed, report.getTotalScanned(), report.getOpenPorts(), report.getClosedPorts(), report.getFilteredPorts(), report.getAverageRtt()));
                if (onScanCompleteCallback != null) {
                    onScanCompleteCallback.accept(report);
                }
            }));

            scannerService.setOnError(ex -> Platform.runLater(() -> {
                btnStart.setDisable(false);
                btnCancel.setDisable(true);
                lblStatus.setText("Error durante el escaneo: " + ex.getMessage());
            }));

            scannerService.startScan(config);

        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error en los parámetros de entrada: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    private void cancelScan() {
        scannerService.cancel();
        btnStart.setDisable(false);
        btnCancel.setDisable(true);
        lblStatus.setText("Escaneo cancelado por el usuario.");
    }

    public Tab getTab() { return tab; }
    public VBox getRoot() { return rootContainer; }
    public void setOnResultCallback(Consumer<ScanResult> cb) { this.onResultCallback = cb; }
    public void setOnScanCompleteCallback(Consumer<ScanReport> cb) { this.onScanCompleteCallback = cb; }
}
