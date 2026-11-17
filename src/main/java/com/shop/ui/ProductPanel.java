package com.shop.ui;

import com.shop.model.Category;
import com.shop.model.Product;
import com.shop.model.User;
import com.shop.service.CatalogService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class ProductPanel extends JPanel {
    private final MainFrame frame;
    private final CatalogService service = new CatalogService();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Name", "Category", "SKU", "Price", "Stock", "Alert"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private List<Product> current = List.of();
    private User user;

    public ProductPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Manage Products");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        add(title, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = new JButton("Add");
        JButton edit = new JButton("Edit");
        JButton delete = new JButton("Delete");
        JButton back = new JButton("Back");
        buttons.add(add); buttons.add(edit); buttons.add(delete); buttons.add(back);
        add(buttons, BorderLayout.SOUTH);

        add.addActionListener(e -> openForm(null));
        edit.addActionListener(e -> {
            Product p = selected();
            if (p != null) openForm(p);
        });
        delete.addActionListener(e -> {
            Product p = selected();
            if (p == null) return;
            int ok = JOptionPane.showConfirmDialog(this, "Delete \"" + p.getName() + "\"?",
                    "Confirm", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                String err = service.deleteProduct(user, p.getId());
                if (err != null) JOptionPane.showMessageDialog(this, err, "Error",
                        JOptionPane.ERROR_MESSAGE);
                reload();
            }
        });
        back.addActionListener(e -> frame.show("dashboard"));
    }

    public void setUser(User user) {
        this.user = user;
        reload();
    }

    private Product selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a product first.");
            return null;
        }
        return current.get(row);
    }

    private void reload() {
        model.setRowCount(0);
        try {
            current = service.products();
            for (Product p : current) {
                model.addRow(new Object[]{p.getId(), p.getName(),
                        p.getCategoryName() == null ? "-" : p.getCategoryName(),
                        p.getSku() == null ? "-" : p.getSku(), p.getPrice(),
                        p.getStockQuantity(), p.isLowStock() ? "LOW STOCK" : ""});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openForm(Product p) {
        List<Category> cats;
        try {
            cats = service.categories();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
            return;
        }
        JTextField name = new JTextField(p == null ? "" : p.getName(), 20);
        JTextField desc = new JTextField(p == null || p.getDescription() == null ? "" : p.getDescription(), 20);
        JTextField sku = new JTextField(p == null || p.getSku() == null ? "" : p.getSku(), 20);
        JTextField price = new JTextField(p == null ? "" : p.getPrice().toPlainString(), 20);
        JTextField stock = new JTextField(p == null ? "" : String.valueOf(p.getStockQuantity()), 20);
        JTextField threshold = new JTextField(p == null ? "5" : String.valueOf(p.getLowStockThreshold()), 20);
        JComboBox<Category> catBox = new JComboBox<>(cats.toArray(new Category[0]));
        if (p != null && p.getCategoryId() != null) {
            for (Category c : cats) if (c.getId() == p.getCategoryId()) catBox.setSelectedItem(c);
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Name:")); form.add(name);
        form.add(new JLabel("Description:")); form.add(desc);
        form.add(new JLabel("SKU:")); form.add(sku);
        form.add(new JLabel("Category:")); form.add(catBox);
        form.add(new JLabel("Price:")); form.add(price);
        form.add(new JLabel("Stock:")); form.add(stock);
        form.add(new JLabel("Low-stock level:")); form.add(threshold);

        int result = JOptionPane.showConfirmDialog(this, form,
                p == null ? "Add Product" : "Edit Product", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        String err = service.saveProduct(user, p == null ? null : p.getId(),
                (Category) catBox.getSelectedItem(), name.getText(), desc.getText(),
                sku.getText(), price.getText(), stock.getText(), threshold.getText());
        if (err != null) {
            JOptionPane.showMessageDialog(this, err, "Validation", JOptionPane.ERROR_MESSAGE);
        }
        reload();
    }
}
