package com.wms.util;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * [CLASS] DBUtil
 * Attributes:
 *   - URL : String = "jdbc:mysql://localhost:3306/softwareproject"
 *   - USER : String = "root"
 *   - PASS : String = "123456"
 * Operations:
 *   + getConnection() : Connection
 *   Description: 数据库持久层连接公用工具类，加载 8.0/5.7 驱动。
 */
public class DBUtil {
    private static final String URL = "jdbc:mysql://localhost:3306/softwareproject?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai";
    private static final String USER = "root";
    private static final String PASS = "root"; // ⚠️ 请根据实际机房密码修改

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.mysql.jdbc.Driver"); // 兼容旧版驱动
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}