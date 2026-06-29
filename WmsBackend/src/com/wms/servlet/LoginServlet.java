package com.wms.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.wms.util.DBUtil;
import com.wms.util.JsonUtil;

/**
 * [CLASS] LoginServlet
 * BaseClass: HttpServlet
 * Operations:
 *   # doPost(request : HttpServletRequest, response : HttpServletResponse) : void
 *   Description: 处理多角色登录逻辑，校验 users 数据，返回完整用户信息及角色代码。
 */
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String body = JsonUtil.getRequestBody(request);
        String username = JsonUtil.getJsonValue(body, "username");
        String password = JsonUtil.getJsonValue(body, "password");

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT * FROM users WHERE username = ? AND password = ? AND status = 1";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, password);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        out.print("{"
                            + "\"code\":200,"
                            + "\"message\":\"登录成功\","
                            + "\"data\":{"
                            + "\"id\":" + rs.getInt("id") + ","
                            + "\"username\":\"" + rs.getString("username") + "\","
                            + "\"role\":" + rs.getInt("role") + ","
                            + "\"realName\":\"" + rs.getString("real_name") + "\","
                            + "\"balance\":" + rs.getDouble("balance")
                            + "}"
                            + "}");
                    } else {
                        response.setStatus(401);
                        out.print("{\"code\":401,\"message\":\"用户名或密码错误或账号已被禁用\"}");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"code\":500,\"message\":\"服务器内部异常\"}");
        }
        out.flush();
    }
}