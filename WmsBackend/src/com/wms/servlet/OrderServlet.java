package com.wms.servlet;

import java.io.BufferedReader;
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

public class OrderServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // 1. GET: 获取订单列表 (支持角色分流)
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String roleParam = request.getParameter("role");
        String workerIdParam = request.getParameter("workerId");
        
        int role = (roleParam != null) ? Integer.parseInt(roleParam) : 1;
        int workerId = (workerIdParam != null && !workerIdParam.isEmpty()) ? Integer.parseInt(workerIdParam) : -1;

        try (Connection conn = DBUtil.getConnection()) {
            String sql;
            if (role == 0) {
                // 管理员：获取全部订单，用于管理和终审
                sql = "SELECT o.*, b.name AS item_name, b.code AS item_code_large, b.location "
                    + "FROM orders o "
                    + "JOIN big_warehouse b ON o.big_warehouse_id = b.id "
                    + "ORDER BY o.id DESC";
            } else if (workerId != -1) {
                // 工人专属进行中：获取该工人名下的进行中订单 (1-已接单, 2-已取货, 3-已送达)
                sql = "SELECT o.*, b.name AS item_name, b.code AS item_code_large, b.location "
                    + "FROM orders o "
                    + "JOIN big_warehouse b ON o.big_warehouse_id = b.id "
                    + "WHERE o.worker_id = " + workerId + " AND o.status IN (1, 2, 3) "
                    + "ORDER BY o.id DESC";
            } else {
                // 工人抢单大厅：获取当前无人领取的待接单 (0-待接单, 5-已拒绝)
                sql = "SELECT o.*, b.name AS item_name, b.code AS item_code_large, b.location "
                    + "FROM orders o "
                    + "JOIN big_warehouse b ON o.big_warehouse_id = b.id "
                    + "WHERE o.status IN (0, 5) "
                    + "ORDER BY o.id ASC";
            }
            
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
    // 2. PUT: 管理员修改订单 (运费和截止日期)
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String body = JsonUtil.getRequestBody(request);
        try {
            int orderId = Integer.parseInt(JsonUtil.getJsonValue(body, "orderId"));
            double totalPrice = Double.parseDouble(JsonUtil.getJsonValue(body, "totalPrice"));
            String deadline = JsonUtil.getJsonValue(body, "deadline");

            try (Connection conn = DBUtil.getConnection()) {
                String sql = "UPDATE orders SET total_price = ?, deadline = ? WHERE id = ? AND status IN (0, 5)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setDouble(1, totalPrice);
                    ps.setString(2, deadline);
                    ps.setInt(3, orderId);
                    int rows = ps.executeUpdate();
                    if (rows > 0) {
                        out.print("{\"code\":200,\"message\":\"订单修改成功！\"}");
                    } else {
                        response.setStatus(400);
                        out.print("{\"code\":400,\"message\":\"订单状态已改变，无法修改\"}");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"code\":500,\"message\":\"修改发生异常\"}");
        }
        out.flush();
    }

    // 3. DELETE: 管理员删除订单 (回滚扣除的大仓库存)
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        int orderId = Integer.parseInt(request.getParameter("orderId"));

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false); // 启用事务控制

            // A. 查询出库数量和大仓货物关联 ID
            String selectSql = "SELECT big_warehouse_id, quantity, status FROM orders WHERE id = ? FOR UPDATE";
            int bigWarehouseId = 0;
            int quantity = 0;
            
            try (PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
                psSelect.setInt(1, orderId);
                try (ResultSet rs = psSelect.executeQuery()) {
                    if (rs.next()) {
                        int status = rs.getInt("status");
                        if (status != 0 && status != 5) {
                            // 已经被接单或完成的订单，严禁删除
                            conn.rollback();
                            response.setStatus(400);
                            out.print("{\"code\":400,\"message\":\"该任务已被工人接受，无法撤销！\"}");
                            return;
                        }
                        bigWarehouseId = rs.getInt("big_warehouse_id");
                        quantity = rs.getInt("quantity");
                    } else {
                        conn.rollback();
                        response.setStatus(404);
                        out.print("{\"code\":404,\"message\":\"订单不存在\"}");
                        return;
                    }
                }
            }

            // B. 退回大仓可用库存
            String refundSql = "UPDATE big_warehouse SET quantity = quantity + ? WHERE id = ?";
            try (PreparedStatement psRefund = conn.prepareStatement(sqlInvQuery(refundSql))) {
                psRefund.setInt(1, quantity);
                psRefund.setInt(2, bigWarehouseId);
                psRefund.executeUpdate();
            }

            // C. 安全物理删除订单
            String deleteSql = "DELETE FROM orders WHERE id = ?";
            try (PreparedStatement psDel = conn.prepareStatement(deleteSql)) {
                psDel.setInt(1, orderId);
                psDel.executeUpdate();
            }

            conn.commit(); // 提交事务
            out.print("{\"code\":200,\"message\":\"订单撤销成功，扣除的大仓库存已安全退回！\"}");

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"code\":500,\"message\":\"撤销删除失败\"}");
        }
        out.flush();
    }

    private String sqlInvQuery(String sql) {
        return sql; // 辅助转换
    }
}