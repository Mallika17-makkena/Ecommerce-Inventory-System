package com.shop.dao;

import com.shop.model.Product;
import com.shop.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    public List<Product> findAllActive() throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = """
                SELECT p.id, p.category_id, c.name AS category_name, p.name, p.description,
                       p.sku, p.price, p.stock_quantity, p.low_stock_threshold
                FROM products p
                LEFT JOIN categories c ON p.category_id = c.id
                WHERE p.is_active = TRUE
                ORDER BY p.id
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int catId = rs.getInt("category_id");
                Integer categoryId = rs.wasNull() ? null : catId;
                list.add(new Product(rs.getInt("id"), categoryId,
                        rs.getString("category_name"), rs.getString("name"),
                        rs.getString("description"), rs.getString("sku"),
                        rs.getBigDecimal("price"), rs.getInt("stock_quantity"),
                        rs.getInt("low_stock_threshold")));
            }
        }
        return list;
    }

    public void insert(Integer categoryId, String name, String description, String sku,
                       BigDecimal price, int stock, int threshold) throws SQLException {
        String sql = """
                INSERT INTO products (category_id, name, description, sku, price,
                                      stock_quantity, low_stock_threshold)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, categoryId, name, description, sku, price, stock, threshold);
            ps.executeUpdate();
        }
    }

    public void update(int id, Integer categoryId, String name, String description, String sku,
                       BigDecimal price, int stock, int threshold) throws SQLException {
        String sql = """
                UPDATE products SET category_id = ?, name = ?, description = ?, sku = ?,
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

    /** Soft delete: old orders still refer to the product. */
    public void softDelete(int id) throws SQLException {
        String sql = "UPDATE products SET is_active = FALSE WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void bind(PreparedStatement ps, Integer categoryId, String name, String description,
                      String sku, BigDecimal price, int stock, int threshold) throws SQLException {
        if (categoryId == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, categoryId);
        ps.setString(2, name);
        ps.setString(3, description);
        if (sku == null || sku.isBlank()) ps.setNull(4, Types.VARCHAR); else ps.setString(4, sku);
        ps.setBigDecimal(5, price);
        ps.setInt(6, stock);
        ps.setInt(7, threshold);
    }
}