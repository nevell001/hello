package com.cashier.dao;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAO 工厂
 * 提供 DAO 实例的管理和依赖注入支持
 */
public class DAOFactory {
    private static final DAOFactory INSTANCE = new DAOFactory();
    private final Map<Class<?>, Object> daoMap = new ConcurrentHashMap<>();

    private DAOFactory() {
        // 初始化默认 DAO 实例
        registerDefaults();
    }

    public static DAOFactory getInstance() {
        return INSTANCE;
    }

    /**
     * 注册默认 DAO 实例
     */
    private void registerDefaults() {
        // 注册新的重构版 DAO
        register(ProductDAORefactored.class, new ProductDAORefactored());
        register(UnitDAORefactored.class, new UnitDAORefactored());
        register(CategoryDAORefactored.class, new CategoryDAORefactored());
        register(SupplierDAORefactored.class, new SupplierDAORefactored());
        register(ShiftDAORefactored.class, new ShiftDAORefactored());
        register(RechargeRecordDAORefactored.class, new RechargeRecordDAORefactored());
        register(PromotionDAORefactored.class, new PromotionDAORefactored());
        register(UserDAORefactored.class, new UserDAORefactored());
        register(OperationLogDAORefactored.class, new OperationLogDAORefactored());
        register(TransactionDAORefactored.class, new TransactionDAORefactored());
        register(PaymentDAORefactored.class, new PaymentDAORefactored());
        register(PurchaseOrderDAORefactored.class, new PurchaseOrderDAORefactored());
        register(MemberDAORefactored.class, new MemberDAORefactored());
        register(ReturnOrderDAORefactored.class, new ReturnOrderDAORefactored());
        register(InvoiceDAORefactored.class, new InvoiceDAORefactored());
    }

    /**
     * 注册 DAO 实例
     * @param clazz DAO 类
     * @param instance DAO 实例
     * @param <T> DAO 类型
     */
    public <T> void register(Class<T> clazz, T instance) {
        daoMap.put(clazz, instance);
    }

    /**
     * 获取 DAO 实例
     * @param clazz DAO 类
     * @param <T> DAO 类型
     * @return DAO 实例
     */
    @SuppressWarnings("unchecked")
    public <T> T getDAO(Class<T> clazz) {
        T instance = (T) daoMap.get(clazz);
        if (instance == null) {
            throw new IllegalStateException("DAO not registered: " + clazz.getName());
        }
        return instance;
    }

    /**
     * 获取商品 DAO（重构版）
     * @return ProductDAORefactored 实例
     */
    public ProductDAORefactored getProductDAO() {
        return getDAO(ProductDAORefactored.class);
    }

    /**
     * 获取单位 DAO（重构版）
     * @return UnitDAORefactored 实例
     */
    public UnitDAORefactored getUnitDAO() {
        return getDAO(UnitDAORefactored.class);
    }

    /**
     * 获取分类 DAO（重构版）
     * @return CategoryDAORefactored 实例
     */
    public CategoryDAORefactored getCategoryDAO() {
        return getDAO(CategoryDAORefactored.class);
    }

    /**
     * 获取供应商 DAO（重构版）
     * @return SupplierDAORefactored 实例
     */
    public SupplierDAORefactored getSupplierDAO() {
        return getDAO(SupplierDAORefactored.class);
    }

    /**
     * 获取班次 DAO（重构版）
     * @return ShiftDAORefactored 实例
     */
    public ShiftDAORefactored getShiftDAO() {
        return getDAO(ShiftDAORefactored.class);
    }

    /**
     * 获取充值记录 DAO（重构版）
     * @return RechargeRecordDAORefactored 实例
     */
    public RechargeRecordDAORefactored getRechargeRecordDAO() {
        return getDAO(RechargeRecordDAORefactored.class);
    }

    /**
     * 获取促销 DAO（重构版）
     * @return PromotionDAORefactored 实例
     */
    public PromotionDAORefactored getPromotionDAO() {
        return getDAO(PromotionDAORefactored.class);
    }

    /**
     * 获取用户 DAO（重构版）
     * @return UserDAORefactored 实例
     */
    public UserDAORefactored getUserDAO() {
        return getDAO(UserDAORefactored.class);
    }

    /**
     * 获取操作日志 DAO（重构版）
     * @return OperationLogDAORefactored 实例
     */
    public OperationLogDAORefactored getOperationLogDAO() {
        return getDAO(OperationLogDAORefactored.class);
    }

    /**
     * 获取交易 DAO（重构版）
     * @return TransactionDAORefactored 实例
     */
    public TransactionDAORefactored getTransactionDAO() {
        return getDAO(TransactionDAORefactored.class);
    }

    /**
     * 获取支付 DAO（重构版）
     * @return PaymentDAORefactored 实例
     */
    public PaymentDAORefactored getPaymentDAO() {
        return getDAO(PaymentDAORefactored.class);
    }

    /**
     * 获取采购订单 DAO（重构版）
     * @return PurchaseOrderDAORefactored 实例
     */
    public PurchaseOrderDAORefactored getPurchaseOrderDAO() {
        return getDAO(PurchaseOrderDAORefactored.class);
    }

    /**
     * 获取会员 DAO（重构版）
     * @return MemberDAORefactored 实例
     */
    public MemberDAORefactored getMemberDAO() {
        return getDAO(MemberDAORefactored.class);
    }

    /**
     * 获取退货订单 DAO（重构版）
     * @return ReturnOrderDAORefactored 实例
     */
    public ReturnOrderDAORefactored getReturnOrderDAO() {
        return getDAO(ReturnOrderDAORefactored.class);
    }

    /**
     * 获取发票 DAO（重构版）
     * @return InvoiceDAORefactored 实例
     */
    public InvoiceDAORefactored getInvoiceDAO() {
        return getDAO(InvoiceDAORefactored.class);
    }
}
