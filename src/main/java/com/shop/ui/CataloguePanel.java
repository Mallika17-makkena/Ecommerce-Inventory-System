package com.shop.ui;

import com.shop.model.Category;
import com.shop.model.Product;
import com.shop.model.User;
import com.shop.service.CatalogService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class CataloguePanel extends JPanel {

    private final MainFrame frame;
    private final CatalogService service = new CatalogService();

    private final JTextField searchField = new JTextField(14);
    private final JTextField minPriceField = new JTextField(7);
    private final JTextField maxPriceField = new JTextField(7);
    private final JComboBox<Category> categoryBox = new JComboBox<>();
    private final JLabel statusLabel = new JLabel(" ");

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "SKU", "Product Name", "Category", "Price", "Stock", "Availability"}, 0) {
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

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Product Catalogue");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        statusLabel.setForeground(new Color(80, 80, 80));
        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(statusLabel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Search and filter bar
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
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

        // Table setup with sorting & double-click details
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(25);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(45);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(190);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    viewDetails();
                }
            }
        });

        JPanel centerPanel = new JPanel(new BorderLayout(5, 10));
        centerPanel.add(filterPanel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Bottom action bar
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton detailsButton = new JButton("View Details");
        JButton backButton = new JButton("Back to Dashboard");

        buttonPanel.add(detailsButton);
        buttonPanel.add(backButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Event listeners
        searchButton.addActionListener(e -> searchProducts());
        searchField.addActionListener(e -> searchProducts());
        minPriceField.addActionListener(e -> searchProducts());
        maxPriceField.addActionListener(e -> searchProducts());
        categoryBox.addActionListener(e -> {
            if (categoryBox.isFocusOwner()) {
                searchProducts();
            }
        });

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
    }

    public void setUser(User user) {
        this.user = user;
        loadCategories();
        searchProducts();
    }

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
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value == null ? "All Categories" : value.toString());
                return this;
            }
        });
    }

    private void searchProducts() {
        try {
            String keyword = searchField.getText().trim();
            Category selectedCategory = (Category) categoryBox.getSelectedItem();
            Integer categoryId = selectedCategory == null ? null : selectedCategory.getId();

            BigDecimal minPrice = minPriceField.getText().trim().isEmpty()
                    ? null : new BigDecimal(minPriceField.getText().trim());
            BigDecimal maxPrice = maxPriceField.getText().trim().isEmpty()
                    ? null : new BigDecimal(maxPriceField.getText().trim());

            current = service.searchProducts(keyword, categoryId, minPrice, maxPrice);
            model.setRowCount(0);

            for (Product p : current) {
                String availability = p.getStockQuantity() <= 0
                        ? "Out of Stock"
                        : (p.isLowStock() ? "Low Stock" : "In Stock");

                model.addRow(new Object[]{
                        p.getId(),
                        p.getSku() == null ? "-" : p.getSku(),
                        p.getName(),
                        p.getCategoryName() == null ? "-" : p.getCategoryName(),
                        "₹" + p.getPrice(),
                        p.getStockQuantity(),
                        availability
                });
            }

            statusLabel.setText(current.isEmpty()
                    ? "No matching products found."
                    : "Showing " + current.size() + " product(s)");

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

    private void viewDetails() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a product first.");
            return;
        }

        int modelRow = table.convertRowIndexToModel(viewRow);
        Product selected = current.get(modelRow);

        // Fetch latest stock/price from DB before showing details
        Product p = selected;
        try {
            p = service.getProductById(selected.getId()).orElse(selected);
        } catch (SQLException ignored) {
        }

        String status = p.getStockQuantity() <= 0
                ? "Out of Stock"
                : (p.isLowStock() ? "Low Stock (" + p.getStockQuantity() + " left)" : "In Stock");

        String details = """
                Product Name: %s
                SKU: %s
                Category: %s
                Price: ₹%s
                Availability: %s (%d units)

                Description:
                %s
                """.formatted(
                p.getName(),
                p.getSku() == null || p.getSku().isBlank() ? "-" : p.getSku(),
                p.getCategoryName() == null ? "-" : p.getCategoryName(),
                p.getPrice(),
                status,
                p.getStockQuantity(),
                (p.getDescription() == null || p.getDescription().isBlank())
                        ? "No description available." : p.getDescription()
        );

        JOptionPane.showMessageDialog(
                this,
                details,
                "Product Details - " + p.getName(),
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}