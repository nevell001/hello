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
}
