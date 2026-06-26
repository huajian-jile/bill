package org.example.bill.test;

import java.sql.*;

public class DBConnectionTester {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("用法: java DBConnectionTester <dbUrl> <dbUser> <dbPassword>");
            System.exit(1);
        }

        String dbUrl = args[0];
        String dbUser = args[1];
        String dbPassword = args[2];

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            DatabaseMetaData metaData = conn.getMetaData();
            System.out.println("数据库连接成功!");
            System.out.println("数据库: " + metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion());
            System.out.println("URL: " + metaData.getURL());
            System.out.println("用户: " + metaData.getUserName());

            // 测试查询
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT version()")) {
                if (rs.next()) {
                    System.out.println("PostgreSQL 版本: " + rs.getString(1));
                }
            }

        } catch (SQLException e) {
            System.err.println("数据库连接失败: " + e.getMessage());
            System.exit(1);
        }
    }
}