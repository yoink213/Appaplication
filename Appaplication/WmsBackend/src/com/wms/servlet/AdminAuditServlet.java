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

public class AdminAuditServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        int orderId = Integer.parseInt(request.getParameter("orderId"));
        String auditAction = request.getParameter("action"); // "ACCEPT" (确认同意), "REJECT" (拒绝驳回)

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false); // 启用事务锁机制

            String query = "SELECT worker_id, total_price, status FROM orders WHERE id = ? FOR UPDATE";
            try (PreparedStatement psQuery = conn.prepareStatement(query)) {
                psQuery.setInt(1, orderId);
                try (ResultSet rs = psQuery.executeQuery()) {
                    if (rs.next() && rs.getInt("status") == 3) {
                        int workerId = rs.getInt("worker_id");
                        double price = rs.getDouble("total_price");

                        if ("ACCEPT".equals(auditAction)) {
                            // 同意：更新状态为 4-已完成
                            String updateOrder = "UPDATE orders SET status = 4 WHERE id = ?";
                            try (PreparedStatement psUp = conn.prepareStatement(updateOrder)) {
                                psUp.setInt(1, orderId);
                                psUp.executeUpdate();
                            }

                            // 将运费金额添加到工人账户余额中
                            String payWorker = "UPDATE users SET balance = balance + ? WHERE id = ?";
                            try (PreparedStatement psPay = conn.prepareStatement(payWorker)) {
                                psPay.setDouble(1, price);
                                psPay.setInt(2, workerId);
                                psPay.executeUpdate();
                            }

                            conn.commit();
                            out.print("{\"code\":200,\"message\":\"审核通过，资金已到账！\"}");
                        } else {
                            // 🔍【修改】：拒绝未完成，状态重置回 1-已接单 (保留原有 worker_id，退回到该工人的账户中重新配送)
                            String rejectOrder = "UPDATE orders SET status = 1 WHERE id = ?";
                            try (PreparedStatement psRe = conn.prepareStatement(rejectOrder)) {
                                psRe.setInt(1, orderId);
                                psRe.executeUpdate();
                            }
                            conn.commit();
                            out.print("{\"code\":200,\"message\":\"已驳回！任务已原路返回至对应工人名下\"}");
                        }
                    } else {
                        conn.rollback();
                        response.setStatus(400);
                        out.print("{\"code\":400,\"message\":\"订单不可审核\"}");
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