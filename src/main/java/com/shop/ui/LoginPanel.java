package com.shop.ui;

import com.shop.model.User;
import com.shop.service.AuthService;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class LoginPanel extends JPanel {
    private final JTextField emailField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final AuthService auth = new AuthService();

    public LoginPanel(MainFrame frame) {
        setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Login", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        add(title, g);

        g.gridwidth = 1; g.gridy = 1; add(new JLabel("Email:"), g);
        g.gridx = 1; add(emailField, g);
        g.gridx = 0; g.gridy = 2; add(new JLabel("Password:"), g);
        g.gridx = 1; add(passwordField, g);

        JButton loginBtn = new JButton("Login");
        g.gridx = 0; g.gridy = 3; g.gridwidth = 2;
        add(loginBtn, g);

        JButton registerBtn = new JButton("Create a new account");
        g.gridy = 4;
        add(registerBtn, g);

        loginBtn.addActionListener(e -> {
            String email = emailField.getText();
            String password = new String(passwordField.getPassword());
            if (email.isBlank() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter email and password.",
                        "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                User user = auth.login(email, password);
                if (user == null) {
                    JOptionPane.showMessageDialog(this, "Invalid email or password.",
                            "Login failed", JOptionPane.ERROR_MESSAGE);
                } else {
                    passwordField.setText("");
                    frame.showDashboard(user);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        registerBtn.addActionListener(e -> frame.show("register"));
    }
}
