package com.shop.dao;

import com.shop.model.Product;
import com.shop.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductDAO {

    private static final String BASE_SELECT = """
            SELECT p.id, p.category_id, c.name AS category_name,
                   p.name, p.description, p.sku, p.price,
                   p.stock_quantity, p.low_stock_threshold
            FROM products p
            LEFT JOIN categories c ON p.category_id = c.id
            WHERE p.is_active = TRUE
            """;

    // Display all active products
    public List<Product> findAllActive() throws SQLException {
        return searchProducts("", null, null, null);
    }

    // Fetch a single active product by ID (used for live details & cart stock checks)
    public Optional<Product> findById(int id) throws SQLException {
        String sql = BASE_SELECT + " AND p.id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    // Search products by name/SKU/description and filter by category and price range
    public List<Product> searchProducts(
            String keyword,
            Integer categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice) throws SQLException {

        List<Product> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT);

        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (hasKeyword) {
            sql.append(" AND (p.name LIKE ? ESCAPE '\\\\' OR p.sku LIKE ? ESCAPE '\\\\' OR p.description LIKE ? ESCAPE '\\\\')");
        }
        if (categoryId != null) {
            sql.append(" AND p.category_id = ?");
        }
        if (minPrice != null) {
            sql.append(" AND p.price >= ?");
        }
        if (maxPrice != null) {
            sql.append(" AND p.price <= ?");
        }
        sql.append(" ORDER BY p.name ASC");

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {

            int index = 1;
            if (hasKeyword) {
                String pattern = "%" + escapeLike(keyword.trim()) + "%";
                ps.setString(index++, pattern);
                ps.setString(index++, pattern);
                ps.setString(index++, pattern);
            }
            if (categoryId != null) {
                ps.setInt(index++, categoryId);
            }
            if (minPrice != null) {
                ps.setBigDecimal(index++, minPrice);
            }
            if (maxPrice != null) {
                ps.setBigDecimal(index++, maxPrice);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    // Add a new product (Member 2)
    public void insert(Integer categoryId, String name, String description,
                       String sku, BigDecimal price, int stock,
                       int threshold) throws SQLException {
        String sql = """
                INSERT INTO products (category_id, name, description, sku,
                                      price, stock_quantity, low_stock_threshold)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, categoryId, name, description, sku, price, stock, threshold);
            ps.executeUpdate();
        }
    }

    // Update an existing product (Member 2)
    public void update(int id, Integer categoryId, String name,
                       String description, String sku, BigDecimal price,
                       int stock, int threshold) throws SQLException {
        String sql = """
                UPDATE products
                SET category_id = ?, name = ?, description = ?, sku = ?,
                    price = ?, stock_quantity = ?, low_stock_threshold = ?
                WHERE id = ?
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, categoryId, name, description, sku, price, stock, threshold);
            ps.setInt(8, id);
            ps.executeUpdate();
        }
    }

    // Soft delete a product (Member 2)
    public void softDelete(int id) throws SQLException {
        String sql = "UPDATE products SET is_active = FALSE WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        int catId = rs.getInt("category_id");
        Integer actualCategoryId = rs.wasNull() ? null : catId;

        return new Product(
                rs.getInt("id"),
                actualCategoryId,
                rs.getString("category_name"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("sku"),
                rs.getBigDecimal("price"),
                rs.getInt("stock_quantity"),
                rs.getInt("low_stock_threshold")
        );
    }

    private String escapeLike(String input) {
        return input.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private void bind(PreparedStatement ps, Integer categoryId,
                      String name, String description, String sku,
                      BigDecimal price, int stock, int threshold)
            throws SQLException {
        if (categoryId == null) {
            ps.setNull(1, Types.INTEGER);
        } else {
            ps.setInt(1, categoryId);
        }
        ps.setString(2, name);
        ps.setString(3, description);
        if (sku == null || sku.isBlank()) {
            ps.setNull(4, Types.VARCHAR);
        } else {
            ps.setString(4, sku);
        }
        ps.setBigDecimal(5, price);
        ps.setInt(6, stock);
        ps.setInt(7, threshold);
    }
}