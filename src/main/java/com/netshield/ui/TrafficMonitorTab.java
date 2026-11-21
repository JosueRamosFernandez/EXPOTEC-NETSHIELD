package com.netshield.ui;

import com.netshield.model.*;
import com.netshield.service.ThreatDetectorService;
import com.netshield.service.TrafficMonitorService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Tab 2: Monitoreo de Tráfico en Tiempo Real, Ranking de Puertos y Detector de Saturación.
 */
public class TrafficMonitorTab {

    private final Tab tab;
    private final TrafficMonitorService trafficService = new TrafficMonitorService();

    private final ObservableList<TrafficConnection> connectionsList = FXCollections.observableArrayList();
    private final ObservableList<PortTrafficSummary> portSummaryList = FXCollections.observableArrayList();
    private FilteredList<TrafficConnection> filteredConnections;

    // Tarjetas KPI
    private final Label lblActiveConns = new Label("0");
    private final Label lblActiveConnsSub = new Label("Established: 0 | Listening: 0");

    private final Label lblDownRate = new Label("0.0 KB/s");
    private final Label lblDownRateSub = new Label("Tasa de descarga");

    private final Label lblUpRate = new Label("0.0 KB/s");
    private final Label lblUpRateSub = new Label("Tasa de subida");

    private final Label lblActivePorts = new Label("0");
    private final Label lblActivePortsSub = new Label("Sin puertos saturados");

    // Banner de diagnóstico de saturación
    private final Label lblSaturationBanner = new Label("Estado de Red: Esperando inicio de monitoreo...");

    // Gráfico de Ancho de Banda en Vivo
    private final XYChart.Series<Number, Number> downSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> upSeries = new XYChart.Series<>();
    private int timeTick = 0;

    // Controles
    private final Button btnToggleMonitor = new Button("Iniciar Monitoreo");
    private final ComboBox<String> cmbInterval = new ComboBox<>();
    private final Spinner<Integer> spnWarnThreshold = new Spinner<>(3, 100, 10, 1);
    private final TextField txtSearch = new TextField();
    private final ToggleGroup stateToggleGroup = new ToggleGroup();
    private final RadioButton rbAll = new RadioButton("Todas");
    private final RadioButton rbEstablished = new RadioButton("ESTABLISHED");
    private final RadioButton rbListening = new RadioButton("LISTENING");
    private final RadioButton rbTimeWait = new RadioButton("TIME_WAIT");
    private final Label lblCount = new Label("0 conexiones");

    private VBox rootContainer;

    public TrafficMonitorTab() {
        this.tab = new Tab("Monitoreo de Tráfico");
        this.tab.setClosable(false);

        buildUI();
        setupService();
    }

    private void buildUI() {
        rootContainer = new VBox(10);
        rootContainer.setPadding(new Insets(12));

        // 1. Barra de Control Superior
        VBox controlCard = new VBox(8);
        controlCard.getStyleClass().add("card");
        controlCard.setPadding(new Insets(10, 14, 10, 14));

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label("Inspector de Tráfico & Detector de Puertos Saturados");
        lblTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #89b4fa;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblThresh = new Label("Umbral Saturación:");
        lblThresh.setStyle("-fx-text-fill: #a6adc8; -fx-font-size: 11px;");
        spnWarnThreshold.setPrefWidth(72);
        spnWarnThreshold.setEditable(true);
        spnWarnThreshold.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                trafficService.setSaturationThresholds(newV, newV * 2 + 5);
            }
        });

        cmbInterval.getItems().addAll("Intervalo: 1 seg", "Intervalo: 2 seg", "Intervalo: 5 seg");
        cmbInterval.setValue("Intervalo: 1 seg");
        cmbInterval.setPrefWidth(130);

        btnToggleMonitor.getStyleClass().add("button-success");
        btnToggleMonitor.setGraphic(UiUtil.icon(UiUtil.PATH_PLAY, "#11111b", 12));
        btnToggleMonitor.setOnAction(e -> toggleMonitoring());

        headerRow.getChildren().addAll(
            UiUtil.icon(UiUtil.PATH_SPEED, "#89b4fa", 16),
            lblTitle,
            spacer,
            lblThresh, spnWarnThreshold,
            cmbInterval,
            btnToggleMonitor
        );

        HBox bannerRow = new HBox(8);
        bannerRow.setAlignment(Pos.CENTER_LEFT);
        lblSaturationBanner.setStyle("-fx-text-fill: #a6adc8; -fx-font-size: 11px; -fx-font-weight: bold;");
        bannerRow.getChildren().addAll(UiUtil.icon(UiUtil.PATH_WARNING, "#89b4fa", 12), lblSaturationBanner);

        controlCard.getChildren().addAll(headerRow, bannerRow);

        // 2. Tarjetas KPI
        HBox kpiCards = new HBox(10);
        kpiCards.getChildren().addAll(
            UiUtil.metricCard("CONEXIONES ACTIVAS", lblActiveConns, lblActiveConnsSub, "#a6e3a1",
                UiUtil.icon(UiUtil.PATH_NETWORK, "#a6e3a1", 14)),
            UiUtil.metricCard("VELOCIDAD DE BAJADA", lblDownRate, lblDownRateSub, "#89b4fa",
                UiUtil.icon(UiUtil.PATH_SPEED, "#89b4fa", 14)),
            UiUtil.metricCard("VELOCIDAD DE SUBIDA", lblUpRate, lblUpRateSub, "#cba6f7",
                UiUtil.icon(UiUtil.PATH_ROCKET, "#cba6f7", 14)),
            UiUtil.metricCard("PUERTOS ACTIVOS", lblActivePorts, lblActivePortsSub, "#fab387",
                UiUtil.icon(UiUtil.PATH_BOLT, "#fab387", 14))
        );

        // 3. Gráfica en Vivo de Ancho de Banda (KB/s)
        VBox chartCard = new VBox(4);
        chartCard.getStyleClass().add("card");
        chartCard.setPadding(new Insets(8));
        chartCard.setPrefHeight(150);
        chartCard.setMinHeight(140);

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Muestras (s)");
        xAxis.setAutoRanging(true);
        xAxis.setAnimated(false);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("KB/s");
        yAxis.setAutoRanging(true);
        yAxis.setAnimated(false);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Telemetría de Ancho de Banda en Vivo (KB/s)");
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setPrefHeight(140);

        downSeries.setName("Descarga (KB/s)");
        upSeries.setName("Subida (KB/s)");
        chart.getData().addAll(downSeries, upSeries);
        chartCard.getChildren().add(chart);

        // 4. Panel Dividido: Izquierda (Conexiones Activas) | Derecha (Ranking de Puertos Saturados + Alertas)
        HBox splitArea = new HBox(12);
        VBox.setVgrow(splitArea, Priority.ALWAYS);

        // Izquierda: Conexiones Activas
        VBox leftPane = new VBox(8);
        HBox.setHgrow(leftPane, Priority.ALWAYS);
        leftPane.setPrefWidth(680);

        HBox filterBar = new HBox(8);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        txtSearch.setPromptText("Filtrar IP, puerto, proceso o estado...");
        txtSearch.setPrefWidth(210);
        txtSearch.textProperty().addListener((obs, oldV, newV) -> applyFilter());

        rbAll.setToggleGroup(stateToggleGroup);
        rbAll.setSelected(true);
        rbEstablished.setToggleGroup(stateToggleGroup);
        rbListening.setToggleGroup(stateToggleGroup);
        rbTimeWait.setToggleGroup(stateToggleGroup);

        rbAll.setOnAction(e -> applyFilter());
        rbEstablished.setOnAction(e -> applyFilter());
        rbListening.setOnAction(e -> applyFilter());
        rbTimeWait.setOnAction(e -> applyFilter());

        Region filterSpacer = new Region();
        HBox.setHgrow(filterSpacer, Priority.ALWAYS);
        lblCount.setStyle("-fx-text-fill: #89b4fa; -fx-font-weight: bold; -fx-font-size: 11px;");

        filterBar.getChildren().addAll(
            UiUtil.icon(UiUtil.PATH_FILTER, "#89b4fa", 13),
            txtSearch,
            rbAll, rbEstablished, rbListening, rbTimeWait,
            filterSpacer, lblCount
        );

        TableView<TrafficConnection> table = buildConnectionsTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        leftPane.getChildren().addAll(filterBar, table);

        // Derecha: Ranking de Puertos Saturados + Historial de Alertas
        VBox rightPane = new VBox(10);
        rightPane.setPrefWidth(460);
        rightPane.setMinWidth(400);

        VBox portRankingCard = new VBox(6);
        portRankingCard.getStyleClass().add("card");
        portRankingCard.setPadding(new Insets(10));
        VBox.setVgrow(portRankingCard, Priority.ALWAYS);

        Label lblPortTitle = new Label("Ranking de Puertos por Carga (Detector de Saturación)");
        lblPortTitle.setStyle("-fx-text-fill: #fab387; -fx-font-weight: bold; -fx-font-size: 12px;");

        TableView<PortTrafficSummary> portTable = buildPortSummaryTable();
        VBox.setVgrow(portTable, Priority.ALWAYS);
        portRankingCard.getChildren().addAll(lblPortTitle, portTable);

        VBox alertsCard = new VBox(6);
        alertsCard.getStyleClass().add("card");
        alertsCard.setPadding(new Insets(10));
        alertsCard.setPrefHeight(165);

        HBox alertsHeader = new HBox(8);
        alertsHeader.setAlignment(Pos.CENTER_LEFT);
        Label lblAlertsTitle = new Label("Alertas Recientes de Saturación y Anomalías");
        lblAlertsTitle.setStyle("-fx-text-fill: #f38ba8; -fx-font-weight: bold; -fx-font-size: 12px;");
        Region alertSpacer = new Region();
        HBox.setHgrow(alertSpacer, Priority.ALWAYS);
        Button btnClearAlerts = new Button("Limpiar");
        btnClearAlerts.getStyleClass().add("button-secondary");
        btnClearAlerts.setStyle("-fx-padding: 3 8; -fx-font-size: 10px;");
        btnClearAlerts.setOnAction(e -> ThreatDetectorService.getInstance().clearAlerts());
        alertsHeader.getChildren().addAll(lblAlertsTitle, alertSpacer, btnClearAlerts);

        ListView<ThreatAlert> alertsList = buildAlertsList();
        VBox.setVgrow(alertsList, Priority.ALWAYS);
        alertsCard.getChildren().addAll(alertsHeader, alertsList);

        rightPane.getChildren().addAll(portRankingCard, alertsCard);
        splitArea.getChildren().addAll(leftPane, rightPane);

        rootContainer.getChildren().addAll(controlCard, kpiCards, chartCard, splitArea);
        tab.setContent(rootContainer);
    }

    private void setupService() {
        trafficService.setOnTrafficUpdate(snapshot -> Platform.runLater(() -> {
            // 1. Actualizar KPIs
            lblActiveConns.setText(String.valueOf(snapshot.totalConnections()));
            lblActiveConnsSub.setText(String.format("Estab: %d | Listen: %d | Wait: %d",
                    snapshot.establishedCount(), snapshot.listeningCount(), snapshot.timeWaitCount()));

            lblDownRate.setText(String.format("%.1f KB/s", snapshot.downloadRateKbps()));
            lblUpRate.setText(String.format("%.1f KB/s", snapshot.uploadRateKbps()));

            lblActivePorts.setText(snapshot.portSummaries().size() + " puertos");
            long satCount = snapshot.saturatedPortsCount();

            Optional<PortTrafficSummary> topPort = snapshot.highestLoadPort();
            if (satCount > 0 && topPort.isPresent()) {
                PortTrafficSummary top = topPort.get();
                lblActivePortsSub.setText(String.format("⚠ Saturado: Puerto %d (%d conex.)", top.getPort(), top.getTotalConnections()));
                lblSaturationBanner.setText(String.format(
                        "ALERTA DE SATURACIÓN: El puerto %d (%s) concentra %d conexiones activas/en espera (%.1f%% del tráfico).",
                        top.getPort(), top.getServiceName(), top.getTotalConnections(), top.getConcentrationPercent()));
                lblSaturationBanner.setStyle(top.getSaturationLevel() == AlertLevel.CRITICAL
                        ? "-fx-text-fill: #f38ba8; -fx-font-size: 11px; -fx-font-weight: bold;"
                        : "-fx-text-fill: #fab387; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else {
                String topPortsStr = snapshot.portSummaries().stream()
                        .limit(3)
                        .map(p -> ":" + p.getPort())
                        .collect(Collectors.joining(", "));
                lblActivePortsSub.setText(topPortsStr.isEmpty() ? "Sin tráfico activo" : "Top: " + topPortsStr);
                lblSaturationBanner.setText("Estado de Red: Tráfico distribuido dentro de parámetros normales (sin puertos saturados).");
                lblSaturationBanner.setStyle("-fx-text-fill: #a6e3a1; -fx-font-size: 11px; -fx-font-weight: bold;");
            }

            // 2. Actualizar Gráfica
            timeTick++;
            downSeries.getData().add(new XYChart.Data<>(timeTick, snapshot.downloadRateKbps()));
            upSeries.getData().add(new XYChart.Data<>(timeTick, snapshot.uploadRateKbps()));

            if (downSeries.getData().size() > 25) {
                downSeries.getData().remove(0);
                upSeries.getData().remove(0);
            }

            // 3. Actualizar Tablas
            connectionsList.setAll(snapshot.connections());
            portSummaryList.setAll(snapshot.portSummaries());
            applyFilter();
        }));

        trafficService.setOnError(ex -> Platform.runLater(() ->
            lblActiveConnsSub.setText("Aviso: " + ex.getMessage())
        ));
    }

    private void toggleMonitoring() {
        if (!trafficService.isMonitoring()) {
            int interval = 1;
            String sel = cmbInterval.getValue();
            if (sel != null && sel.contains("2")) interval = 2;
            else if (sel != null && sel.contains("5")) interval = 5;

            trafficService.startMonitoring(interval);
            btnToggleMonitor.setText("Detener Monitoreo");
            btnToggleMonitor.getStyleClass().clear();
            btnToggleMonitor.getStyleClass().addAll("button", "button-danger");
            btnToggleMonitor.setGraphic(UiUtil.icon(UiUtil.PATH_STOP, "#11111b", 12));
        } else {
            trafficService.stopMonitoring();
            btnToggleMonitor.setText("Iniciar Monitoreo");
            btnToggleMonitor.getStyleClass().clear();
            btnToggleMonitor.getStyleClass().addAll("button", "button-success");
            btnToggleMonitor.setGraphic(UiUtil.icon(UiUtil.PATH_PLAY, "#11111b", 12));
            lblSaturationBanner.setText("Estado de Red: Monitoreo detenido.");
            lblSaturationBanner.setStyle("-fx-text-fill: #a6adc8; -fx-font-size: 11px; -fx-font-weight: bold;");
        }
    }

    private void applyFilter() {
        if (filteredConnections == null) return;
        String query = txtSearch.getText().toLowerCase().trim();
        boolean onlyEstablished = rbEstablished.isSelected();
        boolean onlyListening = rbListening.isSelected();
        boolean onlyTimeWait = rbTimeWait.isSelected();

        filteredConnections.setPredicate(conn -> {
            String state = conn.getState().toUpperCase();
            if (onlyEstablished && !state.contains("ESTAB")) return false;
            if (onlyListening && !state.contains("LIST")) return false;
            if (onlyTimeWait && !state.contains("WAIT")) return false;

            if (query.isEmpty()) return true;

            String local = conn.getLocalAddress() + ":" + conn.getLocalPort();
            String remote = conn.getRemoteAddress() + ":" + conn.getRemotePort();
            String proc = conn.getProcessName().toLowerCase();

            return local.contains(query) || remote.contains(query) || proc.contains(query) || state.toLowerCase().contains(query);
        });

        lblCount.setText(String.format("%d / %d", filteredConnections.size(), connectionsList.size()));
    }

    @SuppressWarnings("unchecked")
    private TableView<TrafficConnection> buildConnectionsTable() {
        filteredConnections = new FilteredList<>(connectionsList, p -> true);
        SortedList<TrafficConnection> sorted = new SortedList<>(filteredConnections);

        TableView<TrafficConnection> table = new TableView<>(sorted);
        sorted.comparatorProperty().bind(table.comparatorProperty());

        Label placeholder = new Label("Presione 'Iniciar Monitoreo' para auditar conexiones y localizar puertos saturados.");
        placeholder.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 12px;");
        table.setPlaceholder(placeholder);

        TableColumn<TrafficConnection, String> colLocal = new TableColumn<>("IP Local");
        colLocal.setCellValueFactory(c -> c.getValue().localAddressProperty());
        colLocal.setPrefWidth(115);

        TableColumn<TrafficConnection, Number> colLocalPort = new TableColumn<>("P. Local");
        colLocalPort.setCellValueFactory(c -> c.getValue().localPortProperty());
        colLocalPort.setPrefWidth(65);

        TableColumn<TrafficConnection, String> colRemote = new TableColumn<>("IP Remota");
        colRemote.setCellValueFactory(c -> c.getValue().remoteAddressProperty());
        colRemote.setPrefWidth(115);

        TableColumn<TrafficConnection, Number> colRemotePort = new TableColumn<>("P. Remoto");
        colRemotePort.setCellValueFactory(c -> c.getValue().remotePortProperty());
        colRemotePort.setPrefWidth(70);

        TableColumn<TrafficConnection, String> colState = new TableColumn<>("Estado");
        colState.setCellValueFactory(c -> c.getValue().stateProperty());
        colState.setPrefWidth(115);
        colState.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setText(null);
                    String s = item.toUpperCase();
                    if (s.contains("ESTAB")) {
                        setGraphic(UiUtil.badge("● ESTABLISHED", "open"));
                    } else if (s.contains("LIST")) {
                        setGraphic(UiUtil.badge("○ LISTENING", "normal"));
                    } else if (s.contains("WAIT")) {
                        setGraphic(UiUtil.badge("◐ TIME_WAIT", "warning"));
                    } else {
                        setGraphic(UiUtil.badge("● " + item, "closed"));
                    }
                }
            }
        });

        TableColumn<TrafficConnection, String> colProc = new TableColumn<>("Proceso");
        colProc.setCellValueFactory(c -> c.getValue().processNameProperty());
        colProc.setPrefWidth(120);

        table.getColumns().addAll(colLocal, colLocalPort, colRemote, colRemotePort, colState, colProc);
        return table;
    }

    @SuppressWarnings("unchecked")
    private TableView<PortTrafficSummary> buildPortSummaryTable() {
        TableView<PortTrafficSummary> table = new TableView<>(portSummaryList);
        Label placeholder = new Label("Sin actividad activa por puerto aún.");
        placeholder.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 11px;");
        table.setPlaceholder(placeholder);

        TableColumn<PortTrafficSummary, Number> colPort = new TableColumn<>("Puerto");
        colPort.setCellValueFactory(c -> c.getValue().portProperty());
        colPort.setPrefWidth(65);

        TableColumn<PortTrafficSummary, String> colSvc = new TableColumn<>("Servicio");
        colSvc.setCellValueFactory(c -> c.getValue().serviceNameProperty());
        colSvc.setPrefWidth(95);

        TableColumn<PortTrafficSummary, Number> colConns = new TableColumn<>("Conex.");
        colConns.setCellValueFactory(c -> c.getValue().totalConnectionsProperty());
        colConns.setPrefWidth(60);

        TableColumn<PortTrafficSummary, Number> colPct = new TableColumn<>("% Carga");
        colPct.setCellValueFactory(c -> c.getValue().concentrationPercentProperty());
        colPct.setPrefWidth(70);
        colPct.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.1f%%", item.doubleValue()));
            }
        });

        TableColumn<PortTrafficSummary, AlertLevel> colLevel = new TableColumn<>("Estado");
        colLevel.setCellValueFactory(c -> c.getValue().saturationLevelProperty());
        colLevel.setPrefWidth(115);
        colLevel.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(AlertLevel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setText(null);
                    switch (item) {
                        case CRITICAL -> setGraphic(UiUtil.badge("● SATURADO", "critical"));
                        case WARNING -> setGraphic(UiUtil.badge("▲ ELEVADO", "warning"));
                        default -> setGraphic(UiUtil.badge("● Normal", "normal"));
                    }
                }
            }
        });

        table.getColumns().addAll(colPort, colSvc, colConns, colPct, colLevel);
        return table;
    }

    private ListView<ThreatAlert> buildAlertsList() {
        ListView<ThreatAlert> list = new ListView<>(ThreatDetectorService.getInstance().getAlerts());
        list.setStyle("-fx-background-color: #181825; -fx-border-color: #313244; -fx-border-radius: 6;");
        list.setPlaceholder(new Label("Sin alertas de anomalía registradas."));
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ThreatAlert item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    String color = switch (item.severity()) {
                        case CRITICAL -> "#f38ba8";
                        case WARNING -> "#fab387";
                        default -> "#a6e3a1";
                    };
                    Label lbl = new Label(String.format("[%s] %s (P:%d) — %s",
                            item.getFormattedTimestamp().substring(11),
                            item.threatType(),
                            item.targetPort(),
                            item.description()));
                    lbl.setWrapText(true);
                    lbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 10.5px;");
                    setGraphic(lbl);
                    setStyle("-fx-background-color: #1e1e2e; -fx-padding: 4 6;");
                }
            }
        });
        return list;
    }

    public void shutdown() {
        trafficService.stopMonitoring();
    }

    public Tab getTab() { return tab; }
    public VBox getRoot() { return rootContainer; }
}
