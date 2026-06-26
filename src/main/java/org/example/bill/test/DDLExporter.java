package org.example.bill.test;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DDLExporter {

    // 综合查询 SQL
    private static final String COMPREHENSIVE_QUERY = """
            WITH user_objects AS (
                -- 用户自定义函数
                SELECT 
                    n.nspname AS schema_name,
                    p.proname AS object_name,
                    'FUNCTION' AS object_type,
                    pg_get_functiondef(p.oid) AS ddl,
                    p.oid AS object_oid
                FROM 
                    pg_proc p
                    JOIN pg_namespace n ON n.oid = p.pronamespace
                    LEFT JOIN pg_depend d ON d.objid = p.oid AND d.deptype = 'e'
                    LEFT JOIN pg_extension e ON e.oid = d.refobjid
                WHERE 
                    n.nspname NOT IN ('pg_catalog', 'information_schema')
                    AND p.prokind = 'f'
                    AND e.oid IS NULL
                    AND p.oid >= 16384
                
                UNION ALL
                
                -- 用户自定义视图
                SELECT 
                    v.schemaname AS schema_name,
                    v.viewname AS object_name,
                    'VIEW' AS object_type,
                    'CREATE VIEW ' || v.schemaname || '.' || v.viewname || ' AS ' || v.definition AS ddl,
                    (v.schemaname || '.' || v.viewname)::regclass AS object_oid
                FROM 
                    pg_views v
                    LEFT JOIN pg_depend d ON d.objid = (v.schemaname || '.' || v.viewname)::regclass AND d.deptype = 'e'
                    LEFT JOIN pg_extension e ON e.oid = d.refobjid
                WHERE 
                    v.schemaname NOT IN ('pg_catalog', 'information_schema')
                    AND e.oid IS NULL
                    AND (v.schemaname || '.' || v.viewname)::regclass >= 16384
                
                UNION ALL
                
                -- 用户自定义物化视图
                SELECT 
                    m.schemaname AS schema_name,
                    m.matviewname AS object_name,
                    'MATERIALIZED VIEW' AS object_type,
                    'CREATE MATERIALIZED VIEW ' || m.schemaname || '.' || m.matviewname || ' AS ' || m.definition AS ddl,
                    (m.schemaname || '.' || m.matviewname)::regclass AS object_oid
                FROM 
                    pg_matviews m
                    LEFT JOIN pg_depend d ON d.objid = (m.schemaname || '.' || m.matviewname)::regclass AND d.deptype = 'e'
                    LEFT JOIN pg_extension e ON e.oid = d.refobjid
                WHERE 
                    m.schemaname NOT IN ('pg_catalog', 'information_schema')
                    AND e.oid IS NULL
            )
            SELECT 
                schema_name,
                object_name,
                object_type,
                ddl
            FROM 
                user_objects
            ORDER BY 
                object_type, schema_name, object_name
            """;

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("用法: java DDLExporter <dbUrl> <dbUser> <dbPassword> <outputDir>");
            System.exit(1);
        }

        String dbUrl = args[0];
        String dbUser = args[1];
        String dbPassword = args[2];
        String outputDir = args[3];

        try {
            exportDDL(dbUrl, dbUser, dbPassword, outputDir);
        } catch (Exception e) {
            System.err.println("导出失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void exportDDL(String dbUrl, String dbUser, String dbPassword, String outputDir)
            throws SQLException, IOException {

        // 确保输出目录存在
        Files.createDirectories(Paths.get(outputDir, "functions"));
        Files.createDirectories(Paths.get(outputDir, "views"));
        Files.createDirectories(Paths.get(outputDir, "materialized-views"));

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(COMPREHENSIVE_QUERY)) {

            int count = 0;
            while (rs.next()) {
                String schemaName = rs.getString("schema_name");
                String objectName = rs.getString("object_name");
                String objectType = rs.getString("object_type");
                String ddl = rs.getString("ddl");

                // 根据对象类型确定子目录
                String subDir = getSubDirectory(objectType);
                String fileName = generateReadableFileName(schemaName, objectName, objectType);
                String filePath = Paths.get(outputDir, subDir, fileName).toString();

                // 写入文件
                writeDDLToFile(filePath, ddl, schemaName, objectName, objectType);
                count++;

                System.out.printf("导出: %s.%s (%s) -> %s%n",
                        schemaName, objectName, objectType, fileName);
            }

            System.out.printf("成功导出 %d 个 DDL 文件到 %s%n", count, outputDir);
        }
    }

    private static String getSubDirectory(String objectType) {
        switch (objectType.toUpperCase()) {
            case "FUNCTION":
                return "functions";
            case "VIEW":
                return "views";
            case "MATERIALIZED VIEW":
                return "materialized-views";
            default:
                return "others";
        }
    }

    /**
     * 生成易读的文件名，支持中文
     */
    private static String generateReadableFileName(String schemaName, String objectName, String objectType) {
        // 清理文件名，但保留中文等Unicode字符
        String cleanSchema = sanitizeFileName(schemaName);
        String cleanObjectName = sanitizeFileName(objectName);
        String prefix = getFilePrefix(objectType);

        //return String.format("%s%s.%s.sql", prefix, cleanSchema, cleanObjectName);
        return String.format("%s.%s.sql",  cleanSchema, cleanObjectName);
    }

    /**
     * 清理文件名，移除非法字符，但保留Unicode字符（包括中文）
     */
    private static String sanitizeFileName(String name) {
        if (name == null) return "null";

        // 移除文件系统不允许的字符，但保留中文等Unicode字符
        return name.replaceAll("[\\\\/:*?\"<>|]", "_")  // 移除Windows非法字符
                .replaceAll("^[\\s.]+|[\\s.]+$", "")  // 移除首尾空格和点
                .replaceAll("\\s+", "_")             // 将空格替换为下划线
                .trim();
    }

    private static String getFilePrefix(String objectType) {
        switch (objectType.toUpperCase()) {
            case "FUNCTION":
                return "func_";
            case "VIEW":
                return "view_";
            case "MATERIALIZED VIEW":
                return "mv_";
            default:
                return "obj_";
        }
    }

    private static void writeDDLToFile(String filePath, String ddl, String schemaName, String objectName, String objectType) throws IOException {
        try (FileWriter writer = new FileWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write("-- Auto-generated DDL file\n");
            //writer.write("-- Generated at: " + new java.util.Date() + "\n");
            writer.write("-- Schema: " + schemaName + "\n");
            writer.write("-- Object: " + objectName + " (" + objectType + ")\n");
            writer.write("-- File: " + new File(filePath).getName() + "\n\n");
            writer.write(ddl);

            // 如果是函数，添加分号作为结束符
            if ("FUNCTION".equalsIgnoreCase(objectType) && !ddl.trim().endsWith(";")) {
                writer.write(";\n");
            } else {
                writer.write("\n");
            }
        }
    }
}