package com.shop.ui;

import com.shop.service.AuthService;

import javax.swing.*;
import java.awt.*;

public class RegisterPanel extends JPanel {
    private final JTextField nameField = new JTextField(20);
    private final JTextField emailField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final AuthService auth = new AuthService();

    public RegisterPanel(MainFrame frame) {
        setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Register", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        add(title, g);

        g.gridwidth = 1; g.gridy = 1; add(new JLabel("Full name:"), g);
        g.gridx = 1; add(nameField, g);
        g.gridx = 0; g.gridy = 2; add(new JLabel("Email:"), g);
        g.gridx = 1; add(emailField, g);
        g.gridx = 0; g.gridy = 3; add(new JLabel("Password:"), g);
        g.gridx = 1; add(passwordField, g);

        JButton registerBtn = new JButton("Register");
        g.gridx = 0; g.gridy = 4; g.gridwidth = 2;
        add(registerBtn, g);

        JButton backBtn = new JButton("Back to login");
        g.gridy = 5;
        add(backBtn, g);

        registerBtn.addActionListener(e -> {
            String error = auth.register(nameField.getText(), emailField.getText(),
                    new String(passwordField.getPassword()));
            if (error != null) {
                JOptionPane.showMessageDialog(this, error, "Registration failed",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Registration successful! Please log in.");
                nameField.setText("");
                emailField.setText("");
                passwordField.setText("");
                frame.show("login");
            }
        });

        backBtn.addActionListener(e -> frame.show("login"));
    }
}