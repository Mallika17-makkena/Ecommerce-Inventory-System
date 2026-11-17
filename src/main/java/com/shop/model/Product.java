package com.shop.model;

import java.math.BigDecimal;

public class Product {
    private final int id;
    private final Integer categoryId;
    private final String categoryName;
    private final String name;
    private final String description;
    private final String sku;
    private final BigDecimal price;
    private final int stockQuantity;
    private final int lowStockThreshold;

    public Product(int id, Integer categoryId, String categoryName, String name,
                   String description, String sku, BigDecimal price,
                   int stockQuantity, int lowStockThreshold) {
        this.id = id;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.name = name;
        this.description = description;
        this.sku = sku;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.lowStockThreshold = lowStockThreshold;
    }

    public int getId() { return id; }
    public Integer getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getSku() { return sku; }
    public BigDecimal getPrice() { return price; }
    public int getStockQuantity() { return stockQuantity; }
    public int getLowStockThreshold() { return lowStockThreshold; }
    public boolean isLowStock() { return stockQuantity <= lowStockThreshold; }
}