package com.shop.ui;

import com.shop.model.User;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final DashboardPanel dashboard = new DashboardPanel(this);
    private final ProductPanel productPanel = new ProductPanel(this);
    private final CategoryPanel categoryPanel = new CategoryPanel(this);

    public MainFrame() {
        setTitle("E-Commerce & Inventory Management");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 480);
        setLocationRelativeTo(null);

        root.add(new LoginPanel(this), "login");
        root.add(new RegisterPanel(this), "register");
        root.add(dashboard, "dashboard");
        root.add(productPanel, "products");
        root.add(categoryPanel, "categories");
        add(root);
        show("login");
    }

    public void show(String name) {
        cards.show(root, name);
    }

    public void showDashboard(User user) {
        dashboard.setUser(user);
        show("dashboard");
    }

    public void showProducts(User user) {
        productPanel.setUser(user);
        show("products");
    }

    public void showCategories(User user) {
        categoryPanel.setUser(user);
        show("categories");
    }
}