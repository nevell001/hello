package com.cashier.controller;

import com.cashier.CashierSystemFXApplication;
import com.cashier.dao.CategoryDAO;
import com.cashier.dao.DAOFactory;
import com.cashier.dao.MemberDAO;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.i18n.I18nManager;
import com.cashier.model.CartItem;
import com.cashier.model.Category;
import com.cashier.model.Member;
import com.cashier.model.Product;
import com.cashier.model.Transaction;
import com.cashier.model.User;
import com.cashier.printer.PrintUtil;
import com.cashier.service.TransactionService;
import com.cashier.util.CurrencyUtil;
import com.cashier.util.DateTimeFormats;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.StatusBarManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.cashier.model.PaymentOrder;
import com.cashier.service.PaymentService;
import com.cashier.util.QrCodeImageUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 触屏版收银控制器
 *
 * <p>三栏布局(分类导航 + 商品卡片 + 购物车/摘要/支付)的触屏收银台。
 * 自管 {@link ObservableList}&lt;{@link CartItem}&gt; 与库存快照 {@code inventoryMap}，
 * 直接复用主项目 DAO/Service 层完成核心收银闭环。</p>
 *
 * <p>首版范围:分类、商品卡片、购物车增删改、4 种支付、会员查询折扣、结账、打印。
 * 不含挂单、促销、交接班(交接班由 PosModeView 底栏处理)。</p>
 */
public class TouchCartController implements CartViewHost {
    private static final Logger logger = LoggerFactoryUtil.getLogger(TouchCartController.class);
    private static final I18nManager i18n = I18nManager.getInstance();
    private static final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();

    @FXML private VBox categoryBox;
    @FXML private TextField searchField;
    @FXML private FlowPane productGrid;
    @FXML private Button clearBtn;
    @FXML private VBox cartList;
    @FXML private TextField memberPhoneField;
    @FXML private Label memberInfoLabel;
    @FXML private Label totalQtyLabel;
    @FXML private Label totalAmountLabel;
    @FXML private Label discountLabel;
    @FXML private Label finalAmountLabel;
    @FXML private Button checkoutBtn;

    private CashierSystemFXApplication application;
    private User currentUser;

    private final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    /** 库存快照,key = Product.name({@code TransactionService.executeTransaction} 契约要求) */
    private final Map<String, Product> inventoryMap = new HashMap<>();
    private Member currentMember;
    /** 当前选中的分类名,null 表示"全部" */
    private String currentCategoryName = null;
    /** 当前搜索关键字,null 或空表示无搜索 */
    private String currentKeyword = null;
    /** 支付进行中标志,防止重复结算 */
    private boolean paymentInProgress = false;

    @FXML
    private void initialize() {
        logger.info("触屏版收银视图初始化");
        loadCategories();
        loadProducts(null);
        refreshCartView();
        updateSummary();
        setupShortcuts();
    }

    // ===== 快捷键 =====

    private void setupShortcuts() {
        Platform.runLater(() -> {
            Scene scene = searchField.getScene();
            if (scene != null) {
                bindShortcuts(scene);
            } else {
                searchField.sceneProperty().addListener((obs, o, n) -> {
                    if (n != null) {
                        bindShortcuts(n);
                    }
                });
            }
        });
    }

    /**
     * 绑定快捷键。使用 addEventFilter(capture 阶段)优先于 PosModeController 在 scene 上
     * 注册的 F8 handler(它仅 consume 不动作),确保 F8 等键由触屏视图处理而不被拦截。
     */
    private void bindShortcuts(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();
            if (code == KeyCode.F1) {
                event.consume();
                focusSearchField();
                searchField.selectAll();
            } else if (code == KeyCode.F2) {
                event.consume();
                handleCheckout();
            } else if (code == KeyCode.F4) {
                event.consume();
                if (memberPhoneField != null) {
                    memberPhoneField.requestFocus();
                }
            } else if (code == KeyCode.F5) {
                event.consume();
                handleClear();
            } else if (code == KeyCode.F8) {
                event.consume();
                handleCashPayment();
            } else if (event.isControlDown() && code == KeyCode.DIGIT1) {
                event.consume();
                handleWechatPayment();
            } else if (event.isControlDown() && code == KeyCode.DIGIT2) {
                event.consume();
                handleAlipayPayment();
            } else if (event.isControlDown() && code == KeyCode.DIGIT3) {
                event.consume();
                handleCardPayment();
            } else if (code == KeyCode.DELETE) {
                event.consume();
                if (!cartItems.isEmpty()) {
                    removeItem(cartItems.get(cartItems.size() - 1));
                }
            }
        });
    }

    public void setApplication(CashierSystemFXApplication application) {
        this.application = application;
    }

    // ===== CartViewHost 契约 =====
    @Override
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    @Override
    public boolean isCartEmpty() {
        return cartItems == null || cartItems.isEmpty();
    }

    @Override
    public void focusSearchField() {
        if (searchField != null) {
            searchField.requestFocus();
        }
    }

    // ===== 分类导航 =====

    private void loadCategories() {
        try {
            List<Category> cats = CategoryDAO.findAll();
            categoryBox.getChildren().clear();
            ToggleGroup group = new ToggleGroup();
            categoryBox.getChildren().add(buildCategoryButton(i18n.get("tpos.all_categories"), null, group));
            for (Category c : cats) {
                categoryBox.getChildren().add(buildCategoryButton(c.name, c.name, group));
            }
            group.selectedToggleProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    onCategorySelected((String) n.getUserData());
                }
            });
            if (!group.getToggles().isEmpty()) {
                group.selectToggle(group.getToggles().get(0));
            }
        } catch (SQLException e) {
            logger.error("加载分类失败", e);
            StatusBarManager.updateError(i18n.get("label.error") + ": " + e.getMessage());
        }
    }

    private ToggleButton buildCategoryButton(String label, String categoryName, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(label);
        btn.getStyleClass().add("tpos-category-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setUserData(categoryName); // null = 全部
        btn.setToggleGroup(group);
        return btn;
    }

    private void onCategorySelected(String categoryName) {
        this.currentCategoryName = categoryName;
        this.currentKeyword = null;
        if (searchField != null) {
            searchField.clear();
        }
        loadProducts(categoryName);
    }

    // ===== 商品加载与卡片 =====

    private void loadProducts(String categoryName) {
        try {
            List<Product> products;
            if (currentKeyword != null && !currentKeyword.isBlank()) {
                products = filterByKeyword(productDAO.findAll(), currentKeyword);
            } else if (categoryName == null) {
                products = productDAO.findAll();
            } else {
                products = productDAO.findByCategory(categoryName);
            }
            // 覆盖更新库存快照(保留不在当前列表中的购物车商品条目)
            for (Product p : products) {
                inventoryMap.put(p.name, p);
            }
            refreshProductGrid(products);
        } catch (SQLException e) {
            logger.error("加载商品失败", e);
            StatusBarManager.updateError(i18n.get("label.error") + ": " + e.getMessage());
            refreshProductGrid(new ArrayList<>());
        }
    }

    private List<Product> filterByKeyword(List<Product> all, String keyword) {
        String k = keyword.trim().toLowerCase();
        if (k.isEmpty()) {
            return all;
        }
        List<Product> result = new ArrayList<>();
        for (Product p : all) {
            if (containsIgnoreCase(p.name, k)
                || containsIgnoreCase(p.barcode, k)
                || containsIgnoreCase(p.productCode, k)) {
                result.add(p);
            }
        }
        return result;
    }

    private static boolean containsIgnoreCase(String text, String keyword) {
        return text != null && text.toLowerCase().contains(keyword);
    }

    private void refreshProductGrid(List<Product> products) {
        productGrid.getChildren().clear();
        for (Product p : products) {
            productGrid.getChildren().add(buildProductCard(p));
        }
    }

    /** 商品卡片:首字符色块 + 名称 + 库存 + 价格(无图标字段,用首字符代替) */
    private VBox buildProductCard(Product p) {
        VBox card = new VBox(6);
        card.getStyleClass().add("tpos-product-card");
        card.setUserData(p);

        StackPane initialPane = new StackPane();
        initialPane.getStyleClass().add("tpos-product-card-initial");
        String firstChar = (p.name != null && !p.name.isEmpty()) ? p.name.substring(0, 1) : "?";
        Label initial = new Label(firstChar);
        initial.getStyleClass().add("tpos-product-card-initial-text"); initial.setStyle("-fx-font-size: 26; -fx-font-weight: bold;");
        initialPane.getChildren().add(initial);

        Label name = new Label(p.name);
        name.getStyleClass().add("tpos-product-card-name");

        Label meta = new Label(i18n.get("tpos.stock_label", p.quantity));
        meta.getStyleClass().add("tpos-product-card-meta");

        String unitSuffix = (p.unit != null && !p.unit.isEmpty()) ? "/" + p.unit : "";
        Label price = new Label(CurrencyUtil.format(p.getPrice().doubleValue()) + unitSuffix);
        price.getStyleClass().add("tpos-product-card-price");

        card.getChildren().addAll(initialPane, name, meta, price);

        if (p.quantity <= 0) {
            card.getStyleClass().add("tpos-product-card--out");
        } else {
            card.setOnMouseClicked(e -> addToCart(p));
        }
        return card;
    }

    // ===== 购物车操作 =====

    private void addToCart(Product product) {
        if (product == null) {
            return;
        }
        inventoryMap.putIfAbsent(product.name, product);
        int stock = currentStock(product);
        CartItem existing = findCartItem(product.id);
        int inCart = existing != null ? existing.quantity : 0;
        if (inCart + 1 > stock) {
            warn(i18n.get("tpos.out_of_stock_warn", product.name));
            return;
        }
        if (existing != null) {
            existing.addQuantity(1);
        } else {
            cartItems.add(new CartItem(product, 1));
        }
        refreshCartView();
        updateSummary();
    }

    private void incrementQty(CartItem item) {
        int stock = currentStock(item.product);
        if (item.quantity + 1 > stock) {
            warn(i18n.get("tpos.out_of_stock_warn", item.product.name));
            return;
        }
        item.addQuantity(1);
        refreshCartView();
        updateSummary();
    }

    private void decrementQty(CartItem item) {
        if (item.quantity <= 1) {
            cartItems.remove(item);
        } else {
            item.addQuantity(-1);
        }
        refreshCartView();
        updateSummary();
    }

    private void removeItem(CartItem item) {
        cartItems.remove(item);
        refreshCartView();
        updateSummary();
    }

    private CartItem findCartItem(int productId) {
        for (CartItem item : cartItems) {
            if (item.product.id == productId) {
                return item;
            }
        }
        return null;
    }

    private int currentStock(Product product) {
        Product inv = inventoryMap.get(product.name);
        return inv != null ? inv.quantity : product.quantity;
    }

    private void refreshCartView() {
        cartList.getChildren().clear();
        if (cartItems.isEmpty()) {
            Label empty = new Label(i18n.get("cart.empty"));
            empty.getStyleClass().add("tpos-cart-empty");
            cartList.getChildren().add(empty);
            return;
        }
        for (CartItem item : cartItems) {
            cartList.getChildren().add(buildCartRow(item));
        }
    }

    private HBox buildCartRow(CartItem item) {
        HBox row = new HBox(8);
        row.getStyleClass().add("tpos-cart-row");
        row.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(item.product.name);
        name.getStyleClass().add("tpos-cart-row-name");
        Label meta = new Label(CurrencyUtil.format(item.product.getPrice().doubleValue()) + " × " + item.quantity);
        meta.getStyleClass().add("tpos-cart-row-meta");
        Label subtotal = new Label(CurrencyUtil.format(item.subtotal.doubleValue()));
        subtotal.getStyleClass().add("tpos-cart-row-subtotal");
        info.getChildren().addAll(name, meta, subtotal);

        HBox stepper = new HBox(4);
        stepper.setAlignment(Pos.CENTER);
        Button minus = new Button("−");
        minus.getStyleClass().add("tpos-qty-minus");
        minus.setOnAction(e -> decrementQty(item));
        Label qty = new Label(String.valueOf(item.quantity));
        qty.getStyleClass().add("tpos-qty-val");
        Button plus = new Button("+");
        plus.getStyleClass().add("tpos-qty-plus");
        plus.setOnAction(e -> incrementQty(item));
        stepper.getChildren().addAll(minus, qty, plus);

        Button remove = new Button("×");
        remove.getStyleClass().add("tpos-remove-btn");
        remove.setOnAction(e -> removeItem(item));

        row.getChildren().addAll(info, stepper, remove);
        return row;
    }

    private void updateSummary() {
        int qty = cartItems.stream().mapToInt(i -> i.quantity).sum();
        BigDecimal total = TransactionService.calculateTotalAmount(cartItems);
        BigDecimal finalAmt = TransactionService.calculateFinalAmount(cartItems, currentMember);
        BigDecimal discount = total.subtract(finalAmt);
        totalQtyLabel.setText(String.valueOf(qty));
        totalAmountLabel.setText(CurrencyUtil.format(total.doubleValue()));
        discountLabel.setText("-" + CurrencyUtil.format(discount.doubleValue()));
        finalAmountLabel.setText(CurrencyUtil.format(finalAmt.doubleValue()));
    }

    // ===== 搜索 / 清空 / 会员 =====

    @FXML
    private void handleSearch() {
        currentKeyword = searchField.getText();
        loadProducts(currentCategoryName);
    }

    @FXML
    private void handleClear() {
        if (cartItems.isEmpty()) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            i18n.get("tpos.clear_confirm"), ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            cartItems.clear();
            currentMember = null;
            if (memberPhoneField != null) {
                memberPhoneField.clear();
            }
            if (memberInfoLabel != null) {
                memberInfoLabel.setText("");
            }
            refreshCartView();
            updateSummary();
        }
    }

    @FXML
    private void handleSearchMember() {
        String phone = memberPhoneField.getText();
        if (phone == null || phone.trim().isEmpty()) {
            currentMember = null;
            memberInfoLabel.setText("");
            updateSummary();
            return;
        }
        try {
            Member m = MemberDAO.findByPhone(phone.trim());
            if (m == null) {
                currentMember = null;
                memberInfoLabel.setText(i18n.get("tpos.member_not_found"));
                warn(i18n.get("tpos.member_not_found"));
            } else {
                currentMember = m;
                String discountStr = m.getDiscount().stripTrailingZeros().toPlainString();
                memberInfoLabel.setText(i18n.get("tpos.member_info", m.name, m.level, discountStr));
            }
            updateSummary();
        } catch (SQLException e) {
            logger.error("查询会员失败", e);
            StatusBarManager.updateError(i18n.get("label.error") + ": " + e.getMessage());
        }
    }

    // ===== 支付前置校验 =====

    private boolean preCheck() {
        if (paymentInProgress) {
            return false;
        }
        if (cartItems.isEmpty()) {
            warn(i18n.get("runtime.cart_empty_payment"));
            return false;
        }
        if (!com.cashier.service.DataService.hasActiveShift()) {
            warn(i18n.get("runtime.no_active_shift"));
            return false;
        }
        return true;
    }

    // ===== 现金支付 =====

    @FXML
    private void handleCashPayment() {
        if (!preCheck()) {
            return;
        }
        final BigDecimal finalAmount = TransactionService.calculateFinalAmount(cartItems, currentMember);

        Dialog<BigDecimal> dialog = new Dialog<>();
        dialog.setTitle(i18n.get("cart.cash_payment"));
        dialog.setHeaderText(null);

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        Label dueLabel = new Label(i18n.get("runtime.amount_due", CurrencyUtil.format(finalAmount.doubleValue())));
        dueLabel.setStyle("-fx-font-size: 22; -fx-font-weight: bold;");

        TextField receivedField = new TextField();
        receivedField.setPromptText(i18n.get("runtime.payment_amount_hint"));
        receivedField.setPrefHeight(42);
        receivedField.setStyle("-fx-font-size: 18;");

        Label changeLabel = new Label(i18n.get("runtime.change_amount", CurrencyUtil.format(0)));
        changeLabel.getStyleClass().setAll("tpos-change-ok"); changeLabel.setStyle("-fx-font-size: 18;");

        String symbol = CurrencyUtil.getSymbol();
        HBox quickBtns = new HBox(8);
        for (int amt : new int[]{100, 50, 20, 10, 5}) {
            Button b = new Button(symbol + amt);
            b.setPrefSize(88, 50);
            b.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
            b.setOnAction(e -> {
                receivedField.setText(String.valueOf(amt));
                receivedField.requestFocus();
            });
            quickBtns.getChildren().add(b);
        }

        receivedField.textProperty().addListener((o, ov, nv) -> {
            try {
                BigDecimal received = new BigDecimal(nv.trim());
                BigDecimal diff = received.subtract(finalAmount);
                if (diff.compareTo(BigDecimal.ZERO) >= 0) {
                    changeLabel.setText(i18n.get("runtime.change_amount", CurrencyUtil.format(diff.doubleValue())));
                    changeLabel.getStyleClass().setAll("tpos-change-ok"); changeLabel.setStyle("-fx-font-size: 18;");
                } else {
                    changeLabel.setText(i18n.get("tpos.cash_short"));
                    changeLabel.getStyleClass().setAll("tpos-change-short"); changeLabel.setStyle("-fx-font-size: 18;");
                }
            } catch (NumberFormatException e) {
                changeLabel.setText(i18n.get("runtime.change_amount", CurrencyUtil.format(0)));
                changeLabel.getStyleClass().setAll("tpos-change-ok"); changeLabel.setStyle("-fx-font-size: 18;");
            }
        });

        content.getChildren().addAll(dueLabel, receivedField, quickBtns, changeLabel);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    BigDecimal received = new BigDecimal(receivedField.getText().trim());
                    if (received.compareTo(BigDecimal.ZERO) <= 0) {
                        return null;
                    }
                    return received;
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        BigDecimal received = dialog.showAndWait().orElse(null);
        if (received == null) {
            return;
        }
        if (received.compareTo(finalAmount) < 0) {
            warn(i18n.get("tpos.cash_short"));
            return;
        }
        BigDecimal change = received.subtract(finalAmount);
        executePayment("现金", received, change);
    }

    // ===== 银行卡支付 =====

    @FXML
    private void handleCardPayment() {
        handleGenericPayment("银行卡");
    }

    private void handleGenericPayment(String paymentMethod) {
        if (!preCheck()) {
            return;
        }
        BigDecimal finalAmount = TransactionService.calculateFinalAmount(cartItems, currentMember);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText(null);
        alert.setContentText(i18n.get("runtime.payment_confirm",
            paymentMethod, CurrencyUtil.format(finalAmount.doubleValue())));
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            executePayment(paymentMethod, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    // ===== 结账大按钮(默认走现金支付流程) =====

    @FXML
    private void handleCheckout() {
        handleCashPayment();
    }

    // ===== 电子支付(微信/支付宝) =====

    @FXML
    private void handleWechatPayment() {
        startElectronicPayment(PaymentOrder.PaymentChannel.WECHAT, "微信");
    }

    @FXML
    private void handleAlipayPayment() {
        startElectronicPayment(PaymentOrder.PaymentChannel.ALIPAY, "支付宝");
    }

    private void startElectronicPayment(PaymentOrder.PaymentChannel channel, String paymentMethod) {
        if (!preCheck()) {
            return;
        }
        if (!PaymentService.isChannelAvailable(channel)) {
            warn(i18n.get("payment.channel.unavailable") + ": "
                + PaymentService.getChannelUnavailableReason(channel));
            return;
        }
        try {
            Transaction transaction = createTransaction(paymentMethod);
            String terminalId = currentUser != null ? currentUser.username : "desktop";
            PaymentOrder paymentOrder = PaymentService.createPaymentOrder(
                transaction.transactionId, transaction.finalAmount, channel, terminalId);
            showElectronicPaymentDialog(paymentOrder, transaction, paymentMethod);
        } catch (Exception e) {
            logger.error("创建电子支付订单失败", e);
            warn(i18n.get("payment.create.failed") + ": " + e.getMessage());
        }
    }

    /**
     * 电子支付二维码对话框 + 异步轮询(照搬 CartController 范式,确保不阻塞 UI 线程)。
     */
    private void showElectronicPaymentDialog(PaymentOrder paymentOrder, Transaction transaction,
                                             String paymentMethod) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(i18n.get("payment.scan.title"));
        dialog.setHeaderText(i18n.get("payment.scan.header",
            paymentMethod, CurrencyUtil.format(transaction.finalAmount.doubleValue())));
        if (productGrid.getScene() != null) {
            dialog.initOwner(productGrid.getScene().getWindow());
            dialog.getDialogPane().getStylesheets().addAll(productGrid.getScene().getStylesheets());
        }

        ImageView qrView = new ImageView(QrCodeImageUtil.create(paymentOrder.qrCodeContent, 260));
        Label status = new Label(i18n.get("payment.waiting"));
        Label orderLabel = new Label(paymentOrder.merchantOrderNo);
        orderLabel.getStyleClass().add("tpos-muted"); orderLabel.setStyle("-fx-font-size: 12;");
        VBox content = new VBox(12, qrView, status, orderLabel);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        AtomicBoolean settled = new AtomicBoolean(false);
        AtomicBoolean queryRunning = new AtomicBoolean(false);
        Timeline poller = new Timeline();
        poller.getKeyFrames().add(new KeyFrame(Duration.seconds(2), event -> {
            if (!queryRunning.compareAndSet(false, true)) {
                return;
            }
            CompletableFuture.supplyAsync(() -> {
                try {
                    return PaymentService.queryPaymentStatus(paymentOrder.paymentId);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).whenComplete((latest, error) -> javafx.application.Platform.runLater(() -> {
                queryRunning.set(false);
                if (error != null) {
                    status.setText(i18n.get("payment.query.retrying"));
                    return;
                }
                if (latest == null) {
                    return;
                }
                status.setText(latest.status.getDisplayName());
                if (latest.status == PaymentOrder.PaymentStatus.SUCCESS
                        && settled.compareAndSet(false, true)) {
                    poller.stop();
                    dialog.close();
                    paymentInProgress = false;
                    completeTransaction(transaction, paymentMethod, BigDecimal.ZERO, BigDecimal.ZERO);
                } else if (latest.status.isFinal() && latest.status != PaymentOrder.PaymentStatus.SUCCESS) {
                    poller.stop();
                    dialog.close();
                    paymentInProgress = false;
                    warn(i18n.get("payment.not_completed") + ": " + latest.status.getDisplayName());
                }
            }));
        }));
        poller.setCycleCount(Timeline.INDEFINITE);

        dialog.setOnHidden(event -> {
            poller.stop();
            paymentInProgress = false;
            if (!settled.get()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        PaymentService.cancelPaymentOrder(paymentOrder.paymentId);
                    } catch (SQLException e) {
                        logger.warn("取消支付订单失败: {}", paymentOrder.paymentId, e);
                    }
                });
            }
        });

        paymentInProgress = true;
        poller.play();
        dialog.show();
    }

    // ===== 结账事务 =====

    private void executePayment(String paymentMethod, BigDecimal receivedAmount, BigDecimal changeAmount) {
        try {
            Transaction transaction = createTransaction(paymentMethod);
            completeTransaction(transaction, paymentMethod, receivedAmount, changeAmount);
        } catch (Exception e) {
            logger.error("交易失败", e);
            warn(i18n.get("runtime.transaction_failed") + ": " + e.getMessage());
        }
    }

    private Transaction createTransaction(String paymentMethod) {
        Transaction tx = new Transaction();
        tx.transactionId = TransactionService.generateOrderNumber();
        tx.timestamp = DateTimeFormats.formatStandard(LocalDateTime.now(ZoneId.systemDefault()));
        tx.items = new ArrayList<>();

        // 合并同 id 商品
        Map<Integer, Product> productMap = new LinkedHashMap<>();
        for (CartItem item : cartItems) {
            Product src = item.product;
            Product existing = productMap.get(src.id);
            if (existing != null) {
                existing.quantity += item.quantity;
            } else {
                Product np = new Product();
                np.id = src.id;
                np.productCode = src.productCode;
                np.barcode = src.barcode;
                np.name = src.name;
                np.price = src.price;
                np.quantity = item.quantity;
                np.category = src.category;
                np.unit = src.unit;
                np.cost = src.cost;
                productMap.put(src.id, np);
            }
        }
        tx.items.addAll(productMap.values());

        tx.totalAmount = TransactionService.calculateTotalAmount(cartItems);
        Map<String, String> settings = com.cashier.service.DataService.loadSettings();
        double taxRate = 0.0;
        try {
            taxRate = Double.parseDouble(settings.getOrDefault("taxRate", "0.0"));
        } catch (NumberFormatException ignored) {
            // 税率解析失败按 0 处理
        }
        tx.tax = tx.totalAmount.multiply(BigDecimal.valueOf(taxRate))
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        tx.finalAmount = TransactionService.calculateFinalAmount(cartItems, currentMember);
        tx.paymentMethod = paymentMethod;
        if (currentMember != null) {
            tx.memberPhone = currentMember.phone;
        }
        if (currentUser != null) {
            tx.operatorUsername = currentUser.username;
            tx.operatorName = currentUser.name;
        }
        return tx;
    }

    private void completeTransaction(Transaction transaction, String paymentMethod,
                                     BigDecimal receivedAmount, BigDecimal changeAmount) {
        try {
            // 兜底:保证购物车所有商品都在 inventoryMap,避免 executeTransaction 内 inventory.get(name) 返回 null
            for (CartItem ci : cartItems) {
                inventoryMap.computeIfAbsent(ci.product.name, n -> {
                    try {
                        return productDAO.findById(ci.product.id);
                    } catch (SQLException ex) {
                        logger.warn("结账前补查库存失败: {}", ci.product.name, ex);
                        return null;
                    }
                });
            }

            TransactionService.TransactionResult result = TransactionService.executeTransaction(
                cartItems, currentMember, transaction, inventoryMap, null);

            if (!result.isSuccess() || result.getTransaction() == null) {
                warn(result.getMessage() != null ? result.getMessage() : i18n.get("runtime.transaction_failed"));
                return;
            }

            logger.info("触屏版交易成功,交易ID: {}", result.getTransaction().transactionId);
            printReceipt(result.getTransaction(), paymentMethod, receivedAmount, changeAmount);
            showPaymentSuccess(paymentMethod, changeAmount);
            resetAfterPayment();
        } catch (Exception e) {
            logger.error("交易失败", e);
            warn(i18n.get("runtime.transaction_failed") + ": " + e.getMessage());
        }
    }

    private void printReceipt(Transaction tx, String paymentMethod, BigDecimal received, BigDecimal change) {
        StringBuilder items = new StringBuilder();
        int totalQty = 0;
        for (CartItem ci : cartItems) {
            items.append(ci.product.name)
                .append(" x").append(ci.quantity)
                .append("  ").append(String.format("%.2f", ci.subtotal.doubleValue()))
                .append("\n");
            totalQty += ci.quantity;
        }
        Map<String, String> settings = com.cashier.service.DataService.loadSettings();
        String storeName = settings.getOrDefault("storeName", "狸算收银");
        String cashierName = currentUser != null ? currentUser.name : "";
        String memberInfo = currentMember != null
            ? (currentMember.name + "(" + currentMember.phone + ") " + currentMember.level) : null;
        BigDecimal total = TransactionService.calculateTotalAmount(cartItems);
        BigDecimal discount = total.subtract(tx.finalAmount);
        boolean ok = PrintUtil.printReceipt(
            tx.transactionId, storeName, cashierName, items.toString(), totalQty,
            total.doubleValue(), discount.doubleValue(), tx.finalAmount.doubleValue(),
            received.doubleValue(), change.doubleValue(), paymentMethod, memberInfo);
        if (!ok) {
            logger.info("小票打印未完成(可能未连接打印机),交易仍已成功");
        }
    }

    private void showPaymentSuccess(String paymentMethod, BigDecimal change) {
        String changeText = change.compareTo(BigDecimal.ZERO) > 0
            ? "  " + i18n.get("runtime.change_amount", CurrencyUtil.format(change.doubleValue())) : "";
        String msg = i18n.get("service.transaction_success") + changeText;
        StatusBarManager.updateSuccess(msg);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void resetAfterPayment() {
        cartItems.clear();
        currentMember = null;
        if (memberPhoneField != null) {
            memberPhoneField.clear();
        }
        if (memberInfoLabel != null) {
            memberInfoLabel.setText("");
        }
        refreshCartView();
        updateSummary();
        loadProducts(currentCategoryName); // 刷新库存显示(库存已扣减)
    }

    // ===== 工具 =====

    private void warn(String msg) {
        StatusBarManager.updateWarning(msg);
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
