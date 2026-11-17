package com.shop;

import com.shop.util.DBConnection;
import java.sql.*;

public class Main {
    public static void main(String[] args) {
        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT name, stock_quantity FROM products")) {
            System.out.println("Connected to MySQL!");
            while (rs.next()) {
                System.out.println(rs.getString("name") + " - " + rs.getInt("stock_quantity"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}