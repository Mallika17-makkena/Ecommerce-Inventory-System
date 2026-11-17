package com.shop.ui;

import com.shop.model.Category;
import com.shop.model.User;
import com.shop.service.CatalogService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class CategoryPanel extends JPanel {
    private final CatalogService service = new CatalogService();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Name", "Description"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private List<Category> current = List.of();
    private User user;

    public CategoryPanel(MainFrame frame) {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Manage Categories");
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
            Category c = selected();
            if (c != null) openForm(c);
        });
        delete.addActionListener(e -> {
            Category c = selected();
            if (c == null) return;
            int ok = JOptionPane.showConfirmDialog(this,
                    "Delete category \"" + c.getName() + "\"? Its products are kept.",
                    "Confirm", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                String err = service.deleteCategory(user, c.getId());
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

    private Category selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a category first.");
            return null;
        }
        return current.get(row);
    }

    private void reload() {
        model.setRowCount(0);
        try {
            current = service.categories();
            for (Category c : current) {
                model.addRow(new Object[]{c.getId(), c.getName(),
                        c.getDescription() == null ? "" : c.getDescription()});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openForm(Category c) {
        JTextField name = new JTextField(c == null ? "" : c.getName(), 20);
        JTextField desc = new JTextField(c == null || c.getDescription() == null ? "" : c.getDescription(), 20);
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Name:")); form.add(name);
        form.add(new JLabel("Description:")); form.add(desc);

        int result = JOptionPane.showConfirmDialog(this, form,
                c == null ? "Add Category" : "Edit Category", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        String err = service.saveCategory(user, c == null ? null : c.getId(),
                name.getText(), desc.getText());
        if (err != null) {
            JOptionPane.showMessageDialog(this, err, "Validation", JOptionPane.ERROR_MESSAGE);
        }
        reload();
    }
}