package com.cashier.controller;

import com.cashier.dao.OperationLogDAO;
import com.cashier.i18n.I18nManager;
import com.cashier.model.OperationLog;
import com.cashier.util.LoggerFactoryUtil;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AuditLogController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(AuditLogController.class);
    private final ObservableList<OperationLog> allLogs = FXCollections.observableArrayList();

    @FXML private TableView<OperationLog> auditTable;
    @FXML private TableColumn<OperationLog, String> timeColumn;
    @FXML private TableColumn<OperationLog, String> usernameColumn;
    @FXML private TableColumn<OperationLog, String> categoryColumn;
    @FXML private TableColumn<OperationLog, String> operationColumn;
    @FXML private TableColumn<OperationLog, String> resultColumn;
    @FXML private TableColumn<OperationLog, String> detailsColumn;
    @FXML private TableColumn<OperationLog, Number> affectedColumn;
    @FXML private TextField usernameField;
    @FXML private TextField keywordField;
    @FXML private ComboBox<String> categoryBox;
    @FXML private ComboBox<String> resultBox;
    @FXML private Label countLabel;

    @FXML
    private void initialize() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        timeColumn.setCellValueFactory(data -> new SimpleStringProperty(format.format(data.getValue().timestamp)));
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().username == null ? "-" : data.getValue().username));
        categoryColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().category));
        operationColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().operation));
        resultColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().result));
        detailsColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().details));
        affectedColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().affectedRecords));

        categoryBox.setItems(FXCollections.observableArrayList(
            "ALL", "AUTH", "TRANSACTION", "REFUND", "INVENTORY", "PURCHASE", "MEMBER", "USER", "SETTINGS", "SYSTEM"));
        resultBox.setItems(FXCollections.observableArrayList("ALL", "SUCCESS", "FAILURE"));
        categoryBox.getSelectionModel().selectFirst();
        resultBox.getSelectionModel().selectFirst();
        handleRefresh();
    }

    @FXML
    private void handleRefresh() {
        try {
            List<OperationLog> logs = OperationLogDAO.findAll();
            allLogs.setAll(logs);
            handleSearch();
        } catch (Exception e) {
            logger.error("加载审计日志失败", e);
            countLabel.setText(I18nManager.getInstance().get("audit.load_failed"));
        }
    }

    @FXML
    private void handleSearch() {
        String username = usernameField.getText().trim().toLowerCase(Locale.ROOT);
        String keyword = keywordField.getText().trim().toLowerCase(Locale.ROOT);
        String category = categoryBox.getValue();
        String result = resultBox.getValue();

        List<OperationLog> filtered = allLogs.stream().filter(log ->
            (username.isEmpty() || safe(log.username).toLowerCase(Locale.ROOT).contains(username)) &&
            ("ALL".equals(category) || category.equals(log.category)) &&
            ("ALL".equals(result) || result.equals(log.result)) &&
            (keyword.isEmpty() || (safe(log.operation) + " " + safe(log.details)).toLowerCase(Locale.ROOT).contains(keyword))
        ).toList();
        auditTable.setItems(FXCollections.observableArrayList(filtered));
        countLabel.setText(I18nManager.getInstance().get("audit.count", filtered.size()));
    }

    @FXML
    private void handleClear() {
        usernameField.clear();
        keywordField.clear();
        categoryBox.getSelectionModel().selectFirst();
        resultBox.getSelectionModel().selectFirst();
        handleSearch();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
