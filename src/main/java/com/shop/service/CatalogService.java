package com.shop.service;

import com.shop.dao.CategoryDAO;
import com.shop.dao.ProductDAO;
import com.shop.model.Category;
import com.shop.model.Product;
import com.shop.model.User;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class CatalogService {
    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    public List<Product> products() throws SQLException { return productDAO.findAllActive(); }
    public List<Category> categories() throws SQLException { return categoryDAO.findAll(); }

    /** Returns an error message, or null on success. */
    public String saveProduct(User actor, Integer productId, Category category, String name,
                              String description, String sku, String priceText,
                              String stockText, String thresholdText) {
        if (actor == null || !actor.isAdmin()) return "Admin access only.";
        name = name.trim();
        if (name.isEmpty()) return "Product name is required.";

        BigDecimal price;
        int stock, threshold;
        try {
            price = new BigDecimal(priceText.trim());
            stock = Integer.parseInt(stockText.trim());
            threshold = Integer.parseInt(thresholdText.trim());
        } catch (NumberFormatException e) {
            return "Price, stock and low-stock level must be valid numbers.";
        }
        if (price.signum() < 0) return "Price cannot be negative.";
        if (stock < 0 || threshold < 0) return "Stock values cannot be negative.";

        Integer catId = category == null ? null : category.getId();
        try {
            if (productId == null) {
                productDAO.insert(catId, name, description.trim(), sku.trim(), price, stock, threshold);
            } else {
                productDAO.update(productId, catId, name, description.trim(), sku.trim(),
                        price, stock, threshold);
            }
            return null;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) return "This SKU already exists.";
            return "Database error: " + e.getMessage();
        }
    }

    public String deleteProduct(User actor, int id) {
        if (actor == null || !actor.isAdmin()) return "Admin access only.";
        try {
            productDAO.softDelete(id);
            return null;
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }

    public String saveCategory(User actor, Integer id, String name, String description) {
        if (actor == null || !actor.isAdmin()) return "Admin access only.";
        name = name.trim();
        if (name.isEmpty()) return "Category name is required.";
        try {
            if (id == null) categoryDAO.insert(name, description.trim());
            else categoryDAO.update(id, name, description.trim());
            return null;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) return "This category already exists.";
            return "Database error: " + e.getMessage();
        }
    }

    public String deleteCategory(User actor, int id) {
        if (actor == null || !actor.isAdmin()) return "Admin access only.";
        try {
            categoryDAO.delete(id);
            return null;
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }
}