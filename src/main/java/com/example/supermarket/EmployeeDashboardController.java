package com.example.supermarket;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.util.Optional;

import static com.example.supermarket.EmployeeDashboardDataModel.*;

public class EmployeeDashboardController {

    @FXML
    Button logout;
    @FXML
    private TableColumn<PurchaseData, Integer> col_customerId;
    @FXML
    private TableColumn<PurchaseData, String> col_brand;
    @FXML
    private TableColumn<PurchaseData, Double> col_price;
    @FXML
    private TableColumn<PurchaseData, String> col_productName;
    @FXML
    private TableColumn<PurchaseData, Integer> col_quantity;
    @FXML
    private Label employee_id;
    @FXML
    private Button minimizeButton;
    @FXML
    private Button purchase_addBtn;

    @FXML
    private Button purchase_clearBtn;

    @FXML
    private TextField customerId;

    @FXML
    private ComboBox<String> purchase_brand;

    @FXML
    private Button purchase_submitBtn;

    @FXML
    private ComboBox<String> purchase_productName;

    @FXML
    private Spinner<Integer> purchase_quantity;

    @FXML
    private TableView<PurchaseData> purchase_tableView;

    @FXML
    private Label purchase_total;

    @FXML
    private AnchorPane titlePane;

    private ObservableList<PurchaseData> purchaseList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setDragAndDrop();
        setMinimize();
        setPurchaseListData();
        initPurchaseBrand();
        initPurchaseQuantitySpinner();
        setPurchase_addBtn();
        setPurchase_clearBtn();
        setPurchase_submitBtn();
        setTotalValue();
    }

    private void setTotalValue() {
        double totalAmount = 0;
        for (PurchaseData purchaseData : purchaseList) {
            totalAmount += purchaseData.getQuantity()*purchaseData.getPrice();
        }
        purchase_total.setText(String.valueOf(totalAmount));
    }

    public void setEmployeeId(String employeeId) {
        employee_id.setText(employeeId);
        employee_id.setStyle("-fx-center:true");
        employee_id.setMaxWidth(Double.MAX_VALUE);
        AnchorPane.setLeftAnchor(employee_id, 0.0);
        AnchorPane.setRightAnchor(employee_id, 0.0);
        employee_id.setAlignment(Pos.CENTER);
    }

    public void setDragAndDrop() {
        titlePane.setOnMousePressed(pressEvent -> titlePane.setOnMouseDragged(dragEvent -> {
            titlePane.getScene().getWindow().setX(dragEvent.getScreenX() - pressEvent.getSceneX());
            titlePane.getScene().getWindow().setY(dragEvent.getScreenY() - pressEvent.getSceneY());
        }));
    }

    public void setMinimize() {
        minimizeButton.setOnAction(actionEvent -> ((Stage) minimizeButton.getScene().getWindow()).setIconified(true));
    }

    public void close() {
        System.exit(0);
    }

    public void logout() {
        try {
            AlertBox alertBox = new AlertBox(Alert.AlertType.CONFIRMATION, "Confirmation message", null, "Are you sure you want to logout?");
            Optional<ButtonType> option = alertBox.showAlert();

            if (option.get().equals(ButtonType.OK)) {
                ((Stage) logout.getScene().getWindow()).setScene(
                        new Scene(FXMLLoader.load(getClass().getResource("LoginDashboard.fxml")
                )));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setPurchaseListData() {
        purchaseList = loadPurchaseListDataSQL();
        col_customerId.setCellValueFactory(item -> item.getValue().customerIdProperty());
        col_brand.setCellValueFactory(item -> (item.getValue().brandProperty()));
        col_productName.setCellValueFactory(item -> (item.getValue().productNameProperty()));
        col_quantity.setCellValueFactory(item -> (item.getValue().quantityProperty()));
        col_price.setCellValueFactory(item -> (item.getValue().priceProperty()));

        purchase_tableView.setItems(purchaseList);
    }

    public void initPurchaseQuantitySpinner() {
        purchase_quantity.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0));
        purchase_quantity.setEditable(true);
        purchase_quantity.getEditor().textProperty().addListener((Observable, oldValue, newValue) -> {
            if (oldValue.matches("0")) {
                purchase_quantity.getEditor().setText(newValue.replaceAll("0", ""));
            }
            if (!newValue.matches("\\d*")) {
                purchase_quantity.getEditor().setText(newValue.replaceAll("\\D", ""));
            }
        });
        purchase_quantity.valueProperty().addListener((Observable, oldValue, newValue) -> {
            if (newValue == null) {
                purchase_quantity.getValueFactory().setValue(oldValue);
            }
        });
    }

    public void initPurchaseBrand() {
        ObservableList<String> brandList = getBrandListSQL();
        purchase_brand.setItems(brandList);
        purchase_brand.valueProperty().addListener((Observable, oldValue, newValue) -> {
            ObservableList<String> productList = searchBrandSQL(newValue);
            purchase_productName.setItems(null);
            purchase_productName.setItems(productList);
        });
    }

    public void setPurchase_addBtn() {
        purchase_addBtn.setOnAction(actionEvent -> {
            //update purchase list and table
            if (!verifyCustomerId()) {
                AlertBox alertBox = new AlertBox(Alert.AlertType.ERROR, "Error message", null, "Customer ID must be integer");
                alertBox.showAlert();
                return;
            } else if (purchase_brand.getValue() == null) {
                AlertBox alertBox = new AlertBox(Alert.AlertType.ERROR, "Error message", null, "Brand Name must not be blank");
                alertBox.showAlert();
                return;
            } else if (purchase_productName.getValue() == null) {
                AlertBox alertBox = new AlertBox(Alert.AlertType.ERROR, "Error message", null, "Product Name must not be blank");
                alertBox.showAlert();
                return;
            } else if (purchase_quantity.getValue() < 1) {
                AlertBox alertBox = new AlertBox(Alert.AlertType.ERROR, "Error message", null, "Quantity must be >0");
                alertBox.showAlert();
                return;
            }

            PurchaseData purchaseData = new PurchaseData();
            purchaseData.setCustomerId(Integer.parseInt(customerId.getText()));
            purchaseData.setBrand(purchase_brand.getValue());
            purchaseData.setProductName(purchase_productName.getValue());
            purchaseData.setQuantity(purchase_quantity.getValue());
            double productPrice = getProductPriceSQL(purchase_productName.getValue());
            purchaseData.setPrice(productPrice);
            purchaseData.setDate(new java.sql.Date(new java.util.Date().getTime()));

            purchaseList.add(purchaseData);
            purchase_tableView.setItems(purchaseList);

            //update total
            double currentTotal = Double.parseDouble(purchase_total.getText().substring(1));
            purchase_total.setText("$" + String.format("%.2f", (currentTotal + productPrice * purchase_quantity.getValue())));
        });
    }

    private boolean verifyCustomerId() {
        return customerId.getText().matches("\\d*") && !customerId.getText().isEmpty();
    }

    public void setPurchase_clearBtn() {
        purchase_clearBtn.setOnAction(actionEvent -> {
            purchaseList.clear();
            purchase_tableView.setItems(purchaseList);
            purchase_total.setText("$0.0");
        });
    }

    public void setPurchase_submitBtn() {
        //update sql table
        purchase_submitBtn.setOnAction(actionEvent -> {
            insertPurchaseDataSQL(purchaseList);
            AlertBox alertBox = new AlertBox(Alert.AlertType.INFORMATION, "Information message", null, "Successfully submitted order(s)!");
            alertBox.showAlert();
        });
    }
}
