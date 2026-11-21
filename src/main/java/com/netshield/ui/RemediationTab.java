package com.netshield.ui;

import com.netshield.model.ScanReport;
import com.netshield.model.VulnerabilityInfo;
import com.netshield.service.DataExporter;
import com.netshield.service.ReportExporter;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab 3: Plan de Remediación con resumen por severidad y exportación Multi-Formato (PDF, CSV, JSON).
 */
public class RemediationTab {

    private final Tab tab;
    private final VBox vulnContainer = new VBox(10);
    private final Label lblSummary = new Label("Realice un escaneo para generar el informe de vulnerabilidades.");
    private final Label lblRiskBreakdown = new Label("Sin auditorías activas.");
    private final Button btnExportPdf = new Button("Exportar PDF");
    private final Button btnExportCsv = new Button("Exportar CSV");
    private final Button btnExportJson = new Button("Exportar JSON");
    private ScanReport currentReport;
    private VBox rootContainer;

    public RemediationTab() {
        this.tab = new Tab("Plan de Remediación & Reportes");
        this.tab.setClosable(false);

        buildUI();
    }

    private void buildUI() {
        rootContainer = new VBox(12);
        rootContainer.setPadding(new Insets(14));

        // Cabecera con resumen y acciones
        VBox headerCard = new VBox(10);
        headerCard.getStyleClass().add("card");

        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        btnExportPdf.getStyleClass().add("button-danger");
        btnExportPdf.setGraphic(UiUtil.icon(UiUtil.PATH_DOWNLOAD, "#11111b", 13));
        btnExportPdf.setDisable(true);
        btnExportPdf.setOnAction(e -> exportPdf());

        btnExportCsv.getStyleClass().add("button-success");
        btnExportCsv.setGraphic(UiUtil.icon(UiUtil.PATH_CLIPBOARD, "#11111b", 13));
        btnExportCsv.setDisable(true);
        btnExportCsv.setOnAction(e -> exportCsv());

        btnExportJson.setGraphic(UiUtil.icon(UiUtil.PATH_DOWNLOAD, "#11111b", 13));
        btnExportJson.setDisable(true);
        btnExportJson.setOnAction(e -> exportJson());

        lblSummary.setStyle("-fx-text-fill: #cdd6f4; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actionRow.getChildren().addAll(btnExportPdf, btnExportCsv, btnExportJson, spacer, lblSummary);

        lblRiskBreakdown.setStyle("-fx-text-fill: #f9e2af; -fx-font-size: 11px; -fx-font-weight: bold;");
        headerCard.getChildren().addAll(actionRow, lblRiskBreakdown);

        // Contenedor scrolleable
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        vulnContainer.setPadding(new Insets(5));
        scrollPane.setContent(vulnContainer);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        rootContainer.getChildren().addAll(headerCard, scrollPane);
        tab.setContent(rootContainer);
    }

    public void updateReport(ScanReport report) {
        this.currentReport = report;
        Platform.runLater(() -> {
            vulnContainer.getChildren().clear();
            btnExportPdf.setDisable(false);
            btnExportCsv.setDisable(false);
            btnExportJson.setDisable(false);

            String scopeBadge = report.isFullRangeScan() ? " [RANGO COMPLETO: 65,535 PUERTOS]" : "";
            lblSummary.setText(String.format("Objetivo: %s%s | Puertos Abiertos: %d | Vulnerabilidades: %d | Alertas Latencia: %d",
                report.getTargetIp(), scopeBadge, report.getOpenPorts(), report.getVulnerabilities().size(), report.getAlertCount()));

            long high = report.getVulnerabilities().stream().filter(v -> "ALTO".equalsIgnoreCase(v.riskLevel())).count();
            long medium = report.getVulnerabilities().stream().filter(v -> "MEDIO".equalsIgnoreCase(v.riskLevel())).count();
            long low = report.getVulnerabilities().stream().filter(v -> "BAJO".equalsIgnoreCase(v.riskLevel())).count();

            lblRiskBreakdown.setText(String.format("🔴 Riesgo Alto: %d   |   🟠 Riesgo Medio: %d   |   🟢 Riesgo Bajo: %d", high, medium, low));

            if (report.getVulnerabilities().isEmpty()) {
                VBox emptyCard = new VBox(8);
                emptyCard.getStyleClass().add("card");
                emptyCard.setAlignment(Pos.CENTER);
                emptyCard.setPadding(new Insets(30));

                Label lblCheck = new Label("Excelente: No se identificaron vulnerabilidades conocidas en los puertos evaluados.");
                lblCheck.setStyle("-fx-text-fill: #a6e3a1; -fx-font-size: 13px; -fx-font-weight: bold;");
                emptyCard.getChildren().addAll(UiUtil.icon(UiUtil.PATH_CHECK, "#a6e3a1", 28), lblCheck);
                vulnContainer.getChildren().add(emptyCard);
                return;
            }

            List<VulnerabilityInfo> sorted = report.getVulnerabilities().stream()
                .sorted(Comparator.comparingInt(v -> switch (v.riskLevel().toUpperCase()) {
                    case "ALTO" -> 0;
                    case "MEDIO" -> 1;
                    default -> 2;
                }))
                .collect(Collectors.toList());

            for (VulnerabilityInfo v : sorted) {
                VBox card = new VBox(8);
                card.getStyleClass().add("card");

                HBox titleBar = new HBox(10);
                titleBar.setAlignment(Pos.CENTER_LEFT);

                Label lblPort = new Label(String.format("Puerto %d / TCP — %s", v.port(), v.serviceName()));
                lblPort.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #89b4fa;");

                String riskType = switch (v.riskLevel().toUpperCase()) {
                    case "ALTO" -> "critical";
                    case "MEDIO" -> "warning";
                    default -> "open";
                };
                Label lblBadge = UiUtil.badge("RIESGO " + v.riskLevel().toUpperCase(), riskType);

                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);

                Label lblRef = new Label("CVE: " + v.cveReference());
                lblRef.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 11px; -fx-font-family: Consolas, monospace;");

                titleBar.getChildren().addAll(lblPort, lblBadge, sp, lblRef);

                Label lblDesc = new Label("Diagnóstico: " + v.description());
                lblDesc.setStyle("-fx-text-fill: #bac2de; -fx-font-size: 12px;");
                lblDesc.setWrapText(true);

                HBox remBox = new HBox(6);
                remBox.setAlignment(Pos.CENTER_LEFT);
                Label lblRem = new Label("Plan de Mitigación: " + v.remediation());
                lblRem.setStyle("-fx-text-fill: #a6e3a1; -fx-font-weight: bold; -fx-font-size: 12px;");
                lblRem.setWrapText(true);
                remBox.getChildren().addAll(UiUtil.icon(UiUtil.PATH_CHECK, "#a6e3a1", 13), lblRem);

                card.getChildren().addAll(titleBar, lblDesc, remBox);
                vulnContainer.getChildren().add(card);
            }
        });
    }

    private Window getActiveWindow() {
        if (rootContainer != null && rootContainer.getScene() != null) {
            return rootContainer.getScene().getWindow();
        }
        if (tab.getTabPane() != null && tab.getTabPane().getScene() != null) {
            return tab.getTabPane().getScene().getWindow();
        }
        return null;
    }

    private void exportPdf() {
        if (currentReport == null) return;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte Técnico de Seguridad (PDF)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf"));
        fileChooser.setInitialFileName("NetShield_Reporte_" + currentReport.getTargetIp().replace(".", "_") + ".pdf");

        File file = fileChooser.showSaveDialog(getActiveWindow());
        if (file != null) {
            try {
                ReportExporter.exportToPdf(currentReport, file);
                showAlert(Alert.AlertType.INFORMATION, "Reporte PDF exportado exitosamente:\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Error al generar PDF: " + ex.getMessage());
            }
        }
    }

    private void exportCsv() {
        if (currentReport == null) return;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Datos a Formato CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo CSV (*.csv)", "*.csv"));
        fileChooser.setInitialFileName("NetShield_Datos_" + currentReport.getTargetIp().replace(".", "_") + ".csv");

        File file = fileChooser.showSaveDialog(getActiveWindow());
        if (file != null) {
            try {
                DataExporter.exportToCsv(currentReport, file);
                showAlert(Alert.AlertType.INFORMATION, "Datos CSV exportados exitosamente:\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Error al generar CSV: " + ex.getMessage());
            }
        }
    }

    private void exportJson() {
        if (currentReport == null) return;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Auditoría a Formato JSON");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo JSON (*.json)", "*.json"));
        fileChooser.setInitialFileName("NetShield_Auditoria_" + currentReport.getTargetIp().replace(".", "_") + ".json");

        File file = fileChooser.showSaveDialog(getActiveWindow());
        if (file != null) {
            try {
                DataExporter.exportToJson(currentReport, file);
                showAlert(Alert.AlertType.INFORMATION, "Auditoría JSON exportada exitosamente:\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Error al generar JSON: " + ex.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public Tab getTab() { return tab; }
    public VBox getRoot() { return rootContainer; }
}
