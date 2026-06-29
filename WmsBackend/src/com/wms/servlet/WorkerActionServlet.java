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
 * [CLASS] WorkerActionServlet
 * BaseClass: HttpServlet
 * Operations:
 *   # doPost(request : HttpServletRequest, response : HttpServletResponse) : void
 *   Description: 处理工人抢单逻辑。控制阶段1：每个工人进行中任务上限为3。阶段2与阶段3的状态切换。
 */
public class WorkerActionServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String action = request.getParameter("action"); // "accept", "pickup", "deliver"
        int orderId = Integer.parseInt(request.getParameter("orderId"));
        int workerId = Integer.parseInt(request.getParameter("workerId"));

        try (Connection conn = DBUtil.getConnection()) {
            if ("accept".equals(action)) {
                // 阶段一：抢单（需要校验该工人的活跃订单数不能 >= 3）
                String checkActive = "SELECT COUNT(*) FROM orders WHERE worker_id = ? AND status IN (1, 2, 3)";
                try (PreparedStatement psCheck = conn.prepareStatement(checkActive)) {
                    psCheck.setInt(1, workerId);
                    try (ResultSet rs = psCheck.executeQuery()) {
                        rs.next();
                        int activeCount = rs.getInt(1);
                        if (activeCount >= 3) {
                            response.setStatus(400);
                            out.print("{\"code\":400,\"message\":\"您当前已有3个进行中的任务，完成后才能继续接单！\"}");
                            return;
                        }
                    }
                }

                // 抢单并绑定 worker_id 并修改状态为 1-已接单
                String update = "UPDATE orders SET worker_id = ?, status = 1 WHERE id = ? AND status IN (0, 5)";
                try (PreparedStatement psUp = conn.prepareStatement(update)) {
                    psUp.setInt(1, workerId);
                    psUp.setInt(2, orderId);
                    int rows = psUp.executeUpdate();
                    if (rows > 0) {
                        out.print("{\"code\":200,\"message\":\"接单成功，请尽快取货！\"}");
                    } else {
                        response.setStatus(400);
                        out.print("{\"code\":400,\"message\":\"接单失败，该订单已被领取\"}");
                    }
                }
            } 
            else if ("pickup".equals(action)) {
                // 阶段二：确认已取货 (1 变为 2)
                String update = "UPDATE orders SET status = 2 WHERE id = ? AND worker_id = ? AND status = 1";
                try (PreparedStatement psUp = conn.prepareStatement(update)) {
                    psUp.setInt(1, orderId);
                    psUp.setInt(2, workerId);
                    if (psUp.executeUpdate() > 0) {
                        out.print("{\"code\":200,\"message\":\"确认取货成功\"}");
                    } else {
                        response.setStatus(400);
                        out.print("{\"code\":400,\"message\":\"非法操作\"}");
                    }
                }
            } 
            else if ("deliver".equals(action)) {
                // 阶段三：确认送达 (2 变为 3)
                String update = "UPDATE orders SET status = 3 WHERE id = ? AND worker_id = ? AND status = 2";
                try (PreparedStatement psUp = conn.prepareStatement(update)) {
                    psUp.setInt(1, orderId);
                    psUp.setInt(2, workerId);
                    if (psUp.executeUpdate() > 0) {
                        out.print("{\"code\":200,\"message\":\"确认送达成功，请等待管理员终审\"}");
                    } else {
                        response.setStatus(400);
                        out.print("{\"code\":400,\"message\":\"异常错误\"}");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"code\":500,\"message\":\"服务器响应异常\"}");
        }
        out.flush();
    }
}