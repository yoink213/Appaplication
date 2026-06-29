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

/**
 * [CLASS] OrderServlet
 * BaseClass: HttpServlet
 * Operations:
 *   # doGet(request : HttpServletRequest, response : HttpServletResponse) : void
 *   Description: 提供给 Android 端查询小仓内可领单任务池的接口，支持联合大仓表获取详细信息。
 */
public class OrderServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        try (Connection conn = DBUtil.getConnection()) {
            // 联合查询：获取订单基础信息，以及大仓商品的名称、货架位置和大仓编号
            String sql = "SELECT o.*, b.name AS item_name, b.code AS item_code_large, b.location "
                       + "FROM orders o "
                       + "JOIN big_warehouse b ON o.big_warehouse_id = b.id "
                       + "WHERE o.status IN (0, 5) "
                       + "ORDER BY o.id ASC";
            
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    json.append("{")
                        .append("\"id\":").append(rs.getInt("id")).append(",")
                        .append("\"orderNo\":\"").append(rs.getString("order_no")).append("\",")
                        .append("\"smallItemCode\":\"").append(rs.getString("small_item_code")).append("\",")
                        .append("\"bigWarehouseId\":").append(rs.getInt("big_warehouse_id")).append(",")
                        .append("\"quantity\":").append(rs.getInt("quantity")).append(",")
                        .append("\"totalPrice\":").append(rs.getDouble("total_price")).append(",")
                        .append("\"deadline\":\"").append(rs.getString("deadline")).append("\",")
                        .append("\"status\":").append(rs.getInt("status")).append(",")
                        .append("\"itemName\":\"").append(rs.getString("item_name")).append("\",")
                        .append("\"itemCodeLarge\":\"").append(rs.getString("item_code_large")).append("\",")
                        .append("\"location\":\"").append(rs.getString("location")).append("\"")
                        .append("}");
                    first = false;
                }
                json.append("]");
                out.print(json.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("[]");
        }
        out.flush();
    }
}