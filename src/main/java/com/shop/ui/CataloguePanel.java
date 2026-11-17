
package com.shop.ui;

import com.shop.model.Category;
import com.shop.model.Product;
import com.shop.model.User;
import com.shop.service.CatalogService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class CataloguePanel extends JPanel {

    private final MainFrame frame;
    private final CatalogService service = new CatalogService();

    private final JTextField searchField = new JTextField(15);
    private final JTextField minPriceField = new JTextField(7);
    private final JTextField maxPriceField = new JTextField(7);

    private final JComboBox<Category> categoryBox = new JComboBox<>();

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Product Name", "Category", "Price", "Stock"}, 0) {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable table = new JTable(model);

    private List<Product> current = List.of();
    private User user;

    public CataloguePanel(MainFrame frame) {

        this.frame = frame;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Title
        JLabel title = new JLabel("Product Catalogue");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        add(title, BorderLayout.NORTH);

        // Search and filter section
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        filterPanel.add(new JLabel("Search:"));
        filterPanel.add(searchField);

        filterPanel.add(new JLabel("Category:"));
        filterPanel.add(categoryBox);

        filterPanel.add(new JLabel("Min Price:"));
        filterPanel.add(minPriceField);

        filterPanel.add(new JLabel("Max Price:"));
        filterPanel.add(maxPriceField);

        JButton searchButton = new JButton("Search / Filter");
        JButton resetButton = new JButton("Reset");

        filterPanel.add(searchButton);
        filterPanel.add(resetButton);

        // Product table
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(25);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 10));
        centerPanel.add(filterPanel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton detailsButton = new JButton("View Details");
        JButton backButton = new JButton("Back");

        buttonPanel.add(detailsButton);
        buttonPanel.add(backButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // Button actions
        searchButton.addActionListener(e -> searchProducts());

        resetButton.addActionListener(e -> {
            searchField.setText("");
            minPriceField.setText("");
            maxPriceField.setText("");
            if (categoryBox.getItemCount() > 0) {
                categoryBox.setSelectedIndex(0);
            }
            searchProducts();
        });

        detailsButton.addActionListener(e -> viewDetails());

        backButton.addActionListener(e -> frame.show("dashboard"));

        // Search when Enter is pressed
        searchField.addActionListener(e -> searchProducts());
    }

    // Load user and catalogue data
    public void setUser(User user) {

        this.user = user;

        loadCategories();
        searchProducts();
    }

    // Load categories into dropdown
    private void loadCategories() {

        categoryBox.removeAllItems();
        categoryBox.addItem(null);

        try {

            List<Category> categories = service.categories();

            for (Category category : categories) {
                categoryBox.addItem(category);
            }

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to load categories: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }

        categoryBox.setRenderer(new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                if (value == null) {
                    setText("All Categories");
                } else {
                    setText(value.toString());
                }

                return this;
            }
        });
    }

    // Search and filter products
    private void searchProducts() {

        try {

            String keyword = searchField.getText().trim();

            Category selectedCategory =
                    (Category) categoryBox.getSelectedItem();

            Integer categoryId = selectedCategory == null
                    ? null : selectedCategory.getId();

            BigDecimal minPrice = null;
            BigDecimal maxPrice = null;

            if (!minPriceField.getText().trim().isEmpty()) {
                minPrice = new BigDecimal(
                        minPriceField.getText().trim());
            }

            if (!maxPriceField.getText().trim().isEmpty()) {
                maxPrice = new BigDecimal(
                        maxPriceField.getText().trim());
            }

            if (minPrice != null && minPrice.signum() < 0) {
                throw new IllegalArgumentException(
                        "Minimum price cannot be negative.");
            }

            if (maxPrice != null && maxPrice.signum() < 0) {
                throw new IllegalArgumentException(
                        "Maximum price cannot be negative.");
            }

            if (minPrice != null && maxPrice != null
                    && minPrice.compareTo(maxPrice) > 0) {

                throw new IllegalArgumentException(
                        "Minimum price cannot be greater than maximum price.");
            }

            current = service.searchProducts(
                    keyword, categoryId, minPrice, maxPrice);

            model.setRowCount(0);

            for (Product p : current) {

                model.addRow(new Object[]{
                        p.getId(),
                        p.getName(),
                        p.getCategoryName() == null
                                ? "-" : p.getCategoryName(),
                        "₹" + p.getPrice(),
                        p.getStockQuantity()
                });
            }

            if (current.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "No products found.",
                        "Catalogue",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please enter valid numbers for price.",
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE
            );

        } catch (IllegalArgumentException e) {

            JOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    "Invalid Filter",
                    JOptionPane.ERROR_MESSAGE
            );

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Database error: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // Display complete details of selected product
    private void viewDetails() {

        int row = table.getSelectedRow();

        if (row < 0) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please select a product first."
            );

            return;
        }

        Product p = current.get(row);

        String details = """
                Product Name: %s

                Category: %s

                Description: %s

                SKU: %s

                Price: ₹%s

                Available Stock: %d

                """.formatted(
                p.getName(),
                p.getCategoryName() == null
                        ? "-" : p.getCategoryName(),
                p.getDescription() == null
                        ? "No description available" : p.getDescription(),
                p.getSku() == null ? "-" : p.getSku(),
                p.getPrice(),
                p.getStockQuantity()
        );

        JOptionPane.showMessageDialog(
                this,
                details,
                "Product Details",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}
