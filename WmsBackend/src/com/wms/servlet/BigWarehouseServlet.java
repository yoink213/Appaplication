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
 * [CLASS] BigWarehouseServlet
 * BaseClass: HttpServlet
 * Operations:
 *   # doGet(request : HttpServletRequest, response : HttpServletResponse) : void
 *   # doPost(request : HttpServletRequest, response : HttpServletResponse) : void
 *   Description: 负责查询大仓内货物，以及管理员新建派单向小仓分配货物的联动事务。
 */
public class BigWarehouseServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // GET: 获取大仓库内所有货物列表
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT * FROM big_warehouse ORDER BY code ASC";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    json.append("{")
                        .append("\"id\":").append(rs.getInt("id")).append(",")
                        .append("\"code\":\"").append(rs.getString("code")).append("\",")
                        .append("\"name\":\"").append(rs.getString("name")).append("\",")
                        .append("\"category\":\"").append(rs.getString("category")).append("\",")
                        .append("\"quantity\":").append(rs.getInt("quantity")).append(",")
                        .append("\"price\":").append(rs.getDouble("price")).append(",")
                        .append("\"location\":\"").append(rs.getString("location")).append("\",")
                        .append("\"description\":\"").append(rs.getString("description")).append("\"")
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

    // POST: 管理员从大仓发往小仓，并全自动生成递增编号
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String body = JsonUtil.getRequestBody(request);
        try {
            int bigWarehouseId = Integer.parseInt(JsonUtil.getJsonValue(body, "bigWarehouseId"));
            int quantity = Integer.parseInt(JsonUtil.getJsonValue(body, "quantity"));
            double totalPrice = Double.parseDouble(JsonUtil.getJsonValue(body, "totalPrice"));
            String deadline = JsonUtil.getJsonValue(body, "deadline");
            int adminId = Integer.parseInt(JsonUtil.getJsonValue(body, "adminId"));

            try (Connection conn = DBUtil.getConnection()) {
                conn.setAutoCommit(false); // 启动事务控制

                // 1. 锁行校验大仓库存
                String stockSql = "SELECT quantity FROM big_warehouse WHERE id = ? FOR UPDATE";
                try (PreparedStatement psStock = conn.prepareStatement(stockSql)) {
                    psStock.setInt(1, bigWarehouseId);
                    try (ResultSet rsStock = psStock.executeQuery()) {
                        if (!rsStock.next() || rsStock.getInt("quantity") < quantity) {
                            conn.rollback();
                            response.setStatus(400);
                            out.print("{\"code\":400,\"message\":\"大仓商品库存不足\"}");
                            return;
                        }
                    }
                }

                // 2. 扣除大仓库存
                String updateStock = "UPDATE big_warehouse SET quantity = quantity - ? WHERE id = ?";
                try (PreparedStatement psUp = conn.prepareStatement(updateStock)) {
                    psUp.setInt(1, quantity);
                    psUp.setInt(2, bigWarehouseId);
                    psUp.executeUpdate();
                }

                // 3. 获取序列自增种子并计算生成对应编号
                int orderNoSeq = getNextSequence(conn, "ORDER_NO");
                int smallItemSeq = getNextSequence(conn, "SMALL_ITEM");

                String orderNo = String.format("ORD-%03d", orderNoSeq);
                String smallItemCode = String.format("%03d", smallItemSeq);

                // 4. 创建小仓新订单，默认为状态 0-待接单
                String insertOrder = "INSERT INTO orders (order_no, small_item_code, big_warehouse_id, quantity, total_price, deadline, status, admin_id) VALUES (?, ?, ?, ?, ?, ?, 0, ?)";
                try (PreparedStatement psIn = conn.prepareStatement(insertOrder)) {
                    psIn.setString(1, orderNo);
                    psIn.setString(2, smallItemCode);
                    psIn.setInt(3, bigWarehouseId);
                    psIn.setInt(4, quantity);
                    psIn.setDouble(5, totalPrice);
                    psIn.setString(6, deadline);
                    psIn.setInt(7, adminId);
                    psIn.executeUpdate();
                }

                conn.commit(); // 提交事务
                out.print("{\"code\":200,\"message\":\"派单并存入小仓成功，单号 " + orderNo + "\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"code\":500,\"message\":\"派发任务处理异常\"}");
        }
        out.flush();
    }

    private int getNextSequence(Connection conn, String seqName) throws Exception {
        String query = "SELECT current_val FROM sys_sequence WHERE seq_name = ? FOR UPDATE";
        int currentVal = 0;
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, seqName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    currentVal = rs.getInt("current_val") + 1;
                    String update = "UPDATE sys_sequence SET current_val = ? WHERE seq_name = ?";
                    try (PreparedStatement psUp = conn.prepareStatement(update)) {
                        psUp.setInt(1, currentVal);
                        psUp.setString(2, seqName);
                        psUp.executeUpdate();
                    }
                }
            }
        }
        return currentVal;
    }
}