package com.shop.service;

import com.shop.dao.UserDAO;
import com.shop.model.User;
import com.shop.util.PasswordUtil;

import java.sql.SQLException;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();

    /** Returns an error message, or null if registration succeeded. */
    public String register(String name, String email, String password) {
        name = name.trim();
        email = email.trim().toLowerCase();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            return "All fields are required.";
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$")) {
            return "Please enter a valid email address.";
        }
        if (password.length() < 6) {
            return "Password must be at least 6 characters.";
        }
        try {
            if (userDAO.findByEmail(email) != null) {
                return "This email is already registered.";
            }
            userDAO.insert(name, email, PasswordUtil.hash(password));
            return null;
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }

    /** Returns the user, or null if the email or password is wrong. */
    public User login(String email, String password) throws SQLException {
        User user = userDAO.findByEmail(email.trim().toLowerCase());
        if (user != null && PasswordUtil.verify(password, user.getPasswordHash())) {
            return user;
        }
        return null;
    }
}