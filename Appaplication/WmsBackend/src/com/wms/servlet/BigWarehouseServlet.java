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
 *   Description: �����ѯ����ڻ���Լ�����Ա�½��ɵ���С�ַ���������������
 */
public class BigWarehouseServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // GET: ��ȡ��ֿ������л����б�
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT * FROM big_warehouse ORDER BY code ASC";
            //// ����Ԥ���������󣬲�ִ�в�ѯ��ȡ�����
            // try-with-resources �Զ��ر� ps �� rs
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

    // POST: ����Ա�Ӵ�ַ���С�֣���ȫ�Զ����ɵ������
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        //// ���������л�ȡ JSON �ַ���
        String body = JsonUtil.getRequestBody(request);
        try {
            int bigWarehouseId = Integer.parseInt(JsonUtil.getJsonValue(body, "bigWarehouseId"));
            int quantity = Integer.parseInt(JsonUtil.getJsonValue(body, "quantity"));
            double totalPrice = Double.parseDouble(JsonUtil.getJsonValue(body, "totalPrice"));
            String deadline = JsonUtil.getJsonValue(body, "deadline");
            int adminId = Integer.parseInt(JsonUtil.getJsonValue(body, "adminId"));

            try (Connection conn = DBUtil.getConnection()) {
                conn.setAutoCommit(false); // �����Զ��ύ�������ֶ��������

                // 1. ����У���ֿ��,FOR UPDATE �������м�¼����ֹ�����޸�
                String stockSql = "SELECT quantity FROM big_warehouse WHERE id = ? FOR UPDATE";
                try (PreparedStatement psStock = conn.prepareStatement(stockSql)) {
                	// ���ò�������ƷID
                    psStock.setInt(1, bigWarehouseId);
                    try (ResultSet rsStock = psStock.executeQuery()) {
                    	// �����Ʒ�����ڻ��治��
                        if (!rsStock.next() || rsStock.getInt("quantity") < quantity) {
                            conn.rollback();
                            response.setStatus(400);
                            out.print("{\"code\":400,\"message\":\"�����Ʒ��治��\"}");
                            return;
                        }
                    }
                }

                // 2. �۳���ֿ��
                String updateStock = "UPDATE big_warehouse SET quantity = quantity - ? WHERE id = ?";
                try (PreparedStatement psUp = conn.prepareStatement(updateStock)) {
                    psUp.setInt(1, quantity);
                    psUp.setInt(2, bigWarehouseId);
                    psUp.executeUpdate();
                }

                // 3. ��ȡ�����������Ӳ��������ɶ�Ӧ���
                int orderNoSeq = getNextSequence(conn, "ORDER_NO");
                int smallItemSeq = getNextSequence(conn, "SMALL_ITEM");

                String orderNo = String.format("ORD-%03d", orderNoSeq);
                String smallItemCode = String.format("%03d", smallItemSeq);

                // 4. ����С���¶�����Ĭ��Ϊ״̬ 0-���ӵ�
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

                conn.commit(); // �ύ����
                out.print("{\"code\":200,\"message\":\"�ɵ�������С�ֳɹ������� " + orderNo + "\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"code\":500,\"message\":\"�ɷ��������쳣\"}");
        }
        out.flush();
    }

    //��ȡ���к�
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