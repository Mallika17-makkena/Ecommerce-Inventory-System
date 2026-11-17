package com.shop.ui;

import com.shop.model.User;

import javax.swing.*;
import java.awt.*;

public class DashboardPanel extends JPanel {
    private final JLabel welcome = new JLabel("", SwingConstants.CENTER);
    private final JLabel role = new JLabel("", SwingConstants.CENTER);
    private final JButton manageProducts = new JButton("Manage Products");
    private final JButton manageCategories = new JButton("Manage Categories");
    private User user;

    public DashboardPanel(MainFrame frame) {
        setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 10, 10, 10);
        g.gridx = 0;
        g.fill = GridBagConstraints.HORIZONTAL;

        welcome.setFont(new Font("Arial", Font.BOLD, 22));
        g.gridy = 0; add(welcome, g);
        g.gridy = 1; add(role, g);
        g.gridy = 2; add(manageProducts, g);
        g.gridy = 3; add(manageCategories, g);

        JButton logout = new JButton("Logout");
        g.gridy = 4; add(logout, g);

        manageProducts.addActionListener(e -> frame.showProducts(user));
        manageCategories.addActionListener(e -> frame.showCategories(user));
        logout.addActionListener(e -> {
            user = null;
            frame.show("login");
        });
    }

    public void setUser(User user) {
        this.user = user;
        welcome.setText("Welcome, " + user.getFullName() + "!");
        role.setText("Role: " + user.getRole());
        manageProducts.setVisible(user.isAdmin());
        manageCategories.setVisible(user.isAdmin());
    }
}