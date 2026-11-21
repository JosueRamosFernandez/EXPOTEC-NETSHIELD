package com.netshield.ui;

import com.netshield.model.AlertLevel;
import com.netshield.model.PortStatus;
import com.netshield.model.ScanResult;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Tab 2: Métricas en Vivo - Visualización gráfica de RTT, Línea Base EMA y Picos de Anomalía,
 * enriquecida con tarjetas de telemetría estadística en tiempo real.
 */
public class MetricsTab {

    private final Tab tab;
    private final XYChart.Series<Number, Number> rttSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> anomalySeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> baselineSeries = new XYChart.Series<>();

    private final Label lblLiveStat = new Label("Gráfico en espera de datos de escaneo.");
    private final Label lblSamplesVal = new Label("0");
    private final Label lblMinVal = new Label("--");
    private final Label lblMaxVal = new Label("--");
    private final Label lblEmaVal = new Label("--");

    private double minRtt = Double.MAX_VALUE;
    private double maxRtt = 0.0;
    private double currentEma = 0.0;
    private int sampleCount = 0;
    private VBox rootContainer;

    public MetricsTab() {
        this.tab = new Tab("Métricas & Latencia");
        this.tab.setClosable(false);

        buildUI();
    }

    @SuppressWarnings("unchecked")
    private void buildUI() {
        rootContainer = new VBox(12);
        rootContainer.setPadding(new Insets(14));

        // 1. Tarjetas Estadísticas KPI en cabecera
        HBox statsCards = new HBox(12);
        statsCards.getChildren().addAll(
            UiUtil.metricCard("MUESTRAS REGISTRADAS", lblSamplesVal, "Puertos abiertos con telemetría", "#89b4fa",
                UiUtil.icon(UiUtil.PATH_BOLT, "#89b4fa", 14)),
            UiUtil.metricCard("LATENCIA MÍNIMA (RTT)", lblMinVal, "Mejor tiempo de respuesta", "#a6e3a1",
                UiUtil.icon(UiUtil.PATH_CHECK, "#a6e3a1", 14)),
            UiUtil.metricCard("LATENCIA MÁXIMA (RTT)", lblMaxVal, "Pico más alto detectado", "#f38ba8",
                UiUtil.icon(UiUtil.PATH_WARNING, "#f38ba8", 14)),
            UiUtil.metricCard("LÍNEA BASE ACTUAL (EMA)", lblEmaVal, "Filtro adaptativo en curso", "#cba6f7",
                UiUtil.icon(UiUtil.PATH_CHART, "#cba6f7", 14))
        );

        // 2. Barra de acciones y estado
        HBox actionBar = new HBox(15);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        actionBar.getStyleClass().add("card");
        actionBar.setPadding(new Insets(8, 14, 8, 14));

        Button btnClear = new Button("Limpiar Gráfico");
        btnClear.getStyleClass().add("button-secondary");
        btnClear.setGraphic(UiUtil.icon(UiUtil.PATH_BROOM, "#89b4fa", 13));
        btnClear.setOnAction(e -> clearChart());

        lblLiveStat.setStyle("-fx-text-fill: #a6adc8; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label legendInfo = new Label("● Azul: RTT Normal   |   ● Verde: Filtro EMA   |   ▲ Rojo: Anomalía (Δ >= 5ms)");
        legendInfo.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 11px;");

        actionBar.getChildren().addAll(btnClear, lblLiveStat, spacer, legendInfo);

        // 3. Configuración de ejes numéricos
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Número de Puerto TCP");
        xAxis.setAutoRanging(true);
        xAxis.setAnimated(false);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Latencia / RTT (ms)");
        yAxis.setAutoRanging(true);
        yAxis.setAnimated(false);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Perfil de Latencia TCP (RTT) vs Línea Base Dinámica (EMA)");
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setLegendVisible(true);

        rttSeries.setName("RTT Normal (ms)");
        baselineSeries.setName("Línea Base EMA (ms)");
        anomalySeries.setName("Anomalía / Pico (Δ >= 5ms)");

        chart.getData().addAll(rttSeries, baselineSeries, anomalySeries);
        VBox.setVgrow(chart, Priority.ALWAYS);

        rootContainer.getChildren().addAll(statsCards, actionBar, chart);
        tab.setContent(rootContainer);
    }

    public void addScanPoint(ScanResult result) {
        if (result.getStatus() != PortStatus.OPEN) return;

        Platform.runLater(() -> {
            int port = result.getPort();
            double rtt = result.getRttMillis();
            sampleCount++;

            if (rtt < minRtt) minRtt = rtt;
            if (rtt > maxRtt) maxRtt = rtt;

            // EMA incremental para visualización
            if (sampleCount == 1) {
                currentEma = rtt;
            } else {
                double alpha = 2.0 / (20 + 1);
                currentEma = alpha * rtt + (1 - alpha) * currentEma;
            }

            // Trazar el punto de la baseline
            baselineSeries.getData().add(new XYChart.Data<>(port, currentEma));

            // Trazar el punto normal o la anomalía
            XYChart.Data<Number, Number> point = new XYChart.Data<>(port, rtt);
            if (result.getAlertLevel() != AlertLevel.NORMAL) {
                anomalySeries.getData().add(point);
                lblLiveStat.setText(String.format("⚠️ ¡Pico anómalo en puerto %d! RTT: %.3f ms (Δ: %+.3f ms)",
                    port, rtt, result.getJitterMillis()));
            } else {
                rttSeries.getData().add(point);
                lblLiveStat.setText(String.format("Puerto %d evaluado | RTT: %.3f ms", port, rtt));
            }

            lblSamplesVal.setText(String.valueOf(sampleCount));
            lblMinVal.setText(String.format("%.3f ms", minRtt));
            lblMaxVal.setText(String.format("%.3f ms", maxRtt));
            lblEmaVal.setText(String.format("%.3f ms", currentEma));
        });
    }

    public void clearChart() {
        rttSeries.getData().clear();
        anomalySeries.getData().clear();
        baselineSeries.getData().clear();
        minRtt = Double.MAX_VALUE;
        maxRtt = 0.0;
        currentEma = 0.0;
        sampleCount = 0;
        lblLiveStat.setText("Gráfico reiniciado.");
        lblSamplesVal.setText("0");
        lblMinVal.setText("--");
        lblMaxVal.setText("--");
        lblEmaVal.setText("--");
    }

    public Tab getTab() { return tab; }
    public VBox getRoot() { return rootContainer; }
}
