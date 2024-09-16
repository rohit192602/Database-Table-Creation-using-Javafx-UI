import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CreateTable extends Application {

    private static final String DATABASE_URL = "jdbc:mysql://localhost:3306/mynewdb?user=root&password=Shivro@1926"; // MySQL database URL
    private Connection connection;
    private TableView<TableRowData> tableView;
    private TableView<TableContentData> tableContentView;
    private List<TableRowData> rowDataList;
    private List<TableContentData> tableContentDataList;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Image logoImage = new Image("file:///E:/Incubator/CreateTables/src/core2web.png");
        ImageView logoView = new ImageView(logoImage);
        logoView.setFitHeight(60);
        logoView.setPreserveRatio(true);
        primaryStage.setTitle("Create JDBC Table");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DATABASE_URL);
            log("Loaded MySQL JDBC driver.");
        } catch (Exception e) {
            log("Error loading MySQL JDBC driver: " + e.getMessage());
            return;
        }

        rowDataList = new ArrayList<>();
        tableContentDataList = new ArrayList<>();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 20, 10, 20));

        grid.add(logoView, 0, 0, 3, 1);

        TextField tableNameField = new TextField();
        TextField columnNameField = new TextField();
        ComboBox<String> dataTypeComboBox = new ComboBox<>();
        dataTypeComboBox.getItems().addAll("INTEGER", "REAL", "TEXT", "BLOB");

        Button addColumnButton = new Button("Add Column");
        Button createTableButton = new Button("Create Table");
        Button showTableButton = new Button("Show Table");

        TextArea logArea = new TextArea();
        logArea.setEditable(false);

        tableView = new TableView<>();
        TableColumn<TableRowData, String> columnNameColumn = new TableColumn<>("Column Name");
        TableColumn<TableRowData, String> dataTypeColumn = new TableColumn<>("Data Type");
        columnNameColumn.setCellValueFactory(new PropertyValueFactory<>("columnName"));
        dataTypeColumn.setCellValueFactory(new PropertyValueFactory<>("dataType"));
        tableView.getColumns().addAll(columnNameColumn, dataTypeColumn);

        tableContentView = new TableView<>();

        grid.add(new Label("Table Name:"), 0, 1);
        grid.add(tableNameField, 1, 1);
        grid.add(new Label("Column Name:"), 0, 2);
        grid.add(columnNameField, 1, 2);
        grid.add(new Label("Data Type:"), 0, 3);
        grid.add(dataTypeComboBox, 1, 3);
        grid.add(addColumnButton, 2, 3);
        grid.add(createTableButton, 1, 4);
        grid.add(showTableButton, 2, 4);
        grid.add(logArea, 0, 5, 3, 1);
        grid.add(tableView, 0, 6, 3, 1);
        grid.add(tableContentView, 0, 7, 3, 1);

        addColumnButton.setOnAction(e -> {
            String tableName = tableNameField.getText();
            String columnName = columnNameField.getText();
            String dataType = dataTypeComboBox.getValue();
            if (!tableName.isEmpty() && !columnName.isEmpty() && dataType != null) {
                String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + dataType;
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                    logArea.appendText("Column '" + columnName + "' added to table '" + tableName + "'.\n");
                    log("Column '" + columnName + "' added to table '" + tableName + "'.");
                    columnNameField.clear();
                    rowDataList.add(new TableRowData(columnName, dataType));
                    tableView.setItems(FXCollections.observableArrayList(rowDataList));
                } catch (SQLException ex) {
                    logArea.appendText("Error adding column: " + ex.getMessage() + "\n");
                    log("Error adding column: " + ex.getMessage());
                }
            }
        });

        createTableButton.setOnAction(e -> {
            String tableName = tableNameField.getText();
            if (!tableName.isEmpty()) {
                String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (id INTEGER PRIMARY KEY)";
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                    logArea.appendText("Table '" + tableName + "' created.\n");
                    log("Table '" + tableName + "' created.");
                    rowDataList.clear();
                    tableView.setItems(FXCollections.observableArrayList(rowDataList));
                } catch (SQLException ex) {
                    log("Error creating table: " + ex.getMessage());
                }
            }
        });

        showTableButton.setOnAction(e -> {
            String tableName = tableNameField.getText();
            if (!tableName.isEmpty()) {
                try (Statement statement = connection.createStatement()) {
                    ResultSet resultSet = statement.executeQuery("SELECT * FROM " + tableName);
                    tableContentDataList.clear();
                    tableContentView.getColumns().clear();
                    while (resultSet.next()) {
                        List<String> row = new ArrayList<>();
                        for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                            row.add(resultSet.getString(i));
                        }
                        tableContentDataList.add(new TableContentData(row));
                    }
                    for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                        String columnName = resultSet.getMetaData().getColumnName(i);
                        TableColumn<TableContentData, String> column = new TableColumn<>(columnName);
                        final int columnIndex = i - 1; // Column index for PropertyValueFactory
                        column.setCellValueFactory(cellData -> cellData.getValue().getColumnData(columnIndex));
                        tableContentView.getColumns().add(column);
                    }
                    tableContentView.setItems(FXCollections.observableArrayList(tableContentDataList));
                } catch (SQLException ex) {
                    logArea.appendText("Error showing table: " + ex.getMessage() + "\n");
                    log("Error showing table: " + ex.getMessage());
                }
            }
        });

        Scene scene = new Scene(grid, 1400, 750);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void log(String message) {
        System.out.println(message);
    }

    @Override
    public void stop() {
        try {
            if (connection != null) {
                connection.close();
                log("Database connection closed.");
            }
        } catch (SQLException e) {
            log("Error closing database connection: " + e.getMessage());
        }
    }

    class TableRowData {
        private String columnName;
        private String dataType;

        public TableRowData(String columnName, String dataType) {
            this.columnName = columnName;
            this.dataType = dataType;
        }

        public String getColumnName() {
            return columnName;
        }

        public String getDataType() {
            return dataType;
        }
    }

    class TableContentData {
        private final List<StringProperty> columnData;

        public TableContentData(List<String> data) {
            columnData = new ArrayList<>();
            for (String item : data) {
                columnData.add(new SimpleStringProperty(item));
            }
        }

        public StringProperty getColumnData(int index) {
            if (index >= 0 && index < columnData.size()) {
                return columnData.get(index);
            }
            return new SimpleStringProperty("");
        }
    }
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////PROPER CODE////////////////
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CreateTable extends Application {

    private static final String DATABASE_URL = "jdbc:mysql://localhost:3306/mynewdb?user=root&password=Shivro@1926"; // MySQL database URL
    private Connection connection;
    private TableView<TableContentData> tableContentView;
    private List<TableContentData> tableContentDataList;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Image logoImage = new Image("images\\core2web.png");
        ImageView logoView = new ImageView(logoImage);
        logoView.setFitHeight(60);
        logoView.setPreserveRatio(true);
        primaryStage.setTitle("Create JDBC Table");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DATABASE_URL);
            log("Loaded MySQL JDBC driver.");
        } catch (Exception e) {
            log("Error loading MySQL JDBC driver: " + e.getMessage());
            return;
        }

        tableContentDataList = new ArrayList<>();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 20, 10, 20));

        grid.add(logoView, 0, 0, 3, 1);

        TextField tableNameField = new TextField();
        TextField columnNameField = new TextField();
        ComboBox<String> dataTypeComboBox = new ComboBox<>();
        dataTypeComboBox.getItems().addAll("INTEGER", "REAL", "TEXT", "BLOB");

        Button addColumnButton = new Button("Add Column");
        Button createTableButton = new Button("Create Table");
        Button showTableButton = new Button("Show Table");

        TextArea logArea = new TextArea();
        logArea.setEditable(false);

        // Initialize tableContentView without any initial columns
        tableContentView = new TableView<>();

        grid.add(new Label("Table Name:"), 0, 1);
        grid.add(tableNameField, 1, 1);
        grid.add(new Label("Column Name:"), 0, 2);
        grid.add(columnNameField, 1, 2);
        grid.add(new Label("Data Type:"), 0, 3);
        grid.add(dataTypeComboBox, 1, 3);
        grid.add(addColumnButton, 2, 3);
        grid.add(createTableButton, 1, 4);
        grid.add(showTableButton, 2, 4);
        grid.add(logArea, 0, 5, 3, 1);
        grid.add(tableContentView, 0, 6, 3, 1);  // Add only the tableContentView

        addColumnButton.setOnAction(e -> {
            String tableName = tableNameField.getText();
            String columnName = columnNameField.getText();
            String dataType = dataTypeComboBox.getValue();
            if (!tableName.isEmpty() && !columnName.isEmpty() && dataType != null) {
                String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + dataType;
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                    logArea.appendText("Column '" + columnName + "' added to table '" + tableName + "'.\n");
                    log("Column '" + columnName + "' added to table '" + tableName + "'.");
                    columnNameField.clear();
                } catch (SQLException ex) {
                    logArea.appendText("Error adding column: " + ex.getMessage() + "\n");
                    log("Error adding column: " + ex.getMessage());
                }
            }
        });

        createTableButton.setOnAction(e -> {
            String tableName = tableNameField.getText();
            if (!tableName.isEmpty()) {
                String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (id INTEGER PRIMARY KEY)";
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                    logArea.appendText("Table '" + tableName + "' created.\n");
                    log("Table '" + tableName + "' created.");
                } catch (SQLException ex) {
                    log("Error creating table: " + ex.getMessage());
                }
            }
        });

        showTableButton.setOnAction(e -> {
            String tableName = tableNameField.getText();
            if (!tableName.isEmpty()) {
                try (Statement statement = connection.createStatement()) {
                    ResultSet resultSet = statement.executeQuery("SELECT * FROM " + tableName);
                    tableContentDataList.clear();
                    tableContentView.getColumns().clear();
                    while (resultSet.next()) {
                        List<String> row = new ArrayList<>();
                        for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                            row.add(resultSet.getString(i));
                        }
                        tableContentDataList.add(new TableContentData(row));
                    }
                    for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                        String columnName = resultSet.getMetaData().getColumnName(i);
                        TableColumn<TableContentData, String> column = new TableColumn<>(columnName);
                        final int columnIndex = i - 1; // Column index for PropertyValueFactory
                        column.setCellValueFactory(cellData -> cellData.getValue().getColumnData(columnIndex));
                        tableContentView.getColumns().add(column);
                    }
                    tableContentView.setItems(FXCollections.observableArrayList(tableContentDataList));
                } catch (SQLException ex) {
                    logArea.appendText("Error showing table: " + ex.getMessage() + "\n");
                    log("Error showing table: " + ex.getMessage());
                }
            }
        });

        Scene scene = new Scene(grid, 1400, 750);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void log(String message) {
        System.out.println(message);
    }

    @Override
    public void stop() {
        try {
            if (connection != null) {
                connection.close();
                log("Database connection closed.");
            }
        } catch (SQLException e) {
            log("Error closing database connection: " + e.getMessage());
        }
    }

    class TableContentData {
        private final List<StringProperty> columnData;

        public TableContentData(List<String> data) {
            columnData = new ArrayList<>();
            for (String item : data) {
                columnData.add(new SimpleStringProperty(item));
            }
        }

        public StringProperty getColumnData(int index) {
            if (index >= 0 && index < columnData.size()) {
                return columnData.get(index);
            }
            return new SimpleStringProperty("");
        }
    }
}

