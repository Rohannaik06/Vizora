import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/AdminOrdersServlet")
public class AdminOrdersServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("[");

        String sql = 
            "SELECT o.order_id, o.full_name, o.mobile, o.address, o.city, o.state, o.pincode, o.payment_method, o.total_amount, o.order_status, " +
            "p.product_name, p.brand, oi.quantity " +
            "FROM orders o " +
            "LEFT JOIN order_items oi ON o.order_id = oi.order_id " +
            "LEFT JOIN products p ON oi.product_id = p.product_id " +
            "ORDER BY o.order_date DESC";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            boolean first = true;
            while (rs.next()) {
                if (!first) {
                    jsonBuilder.append(",");
                }
                first = false;

                String fullAddress = (rs.getString("address") != null ? rs.getString("address") : "") + ", " + 
                                     (rs.getString("city") != null ? rs.getString("city") : "") + ", " + 
                                     (rs.getString("state") != null ? rs.getString("state") : "") + " - " + 
                                     (rs.getString("pincode") != null ? rs.getString("pincode") : "");

                jsonBuilder.append("{");
                jsonBuilder.append("\"orderId\":").append(rs.getInt("order_id")).append(",");
                jsonBuilder.append("\"customer\":\"").append(escapeJson(rs.getString("full_name"))).append("\",");
                jsonBuilder.append("\"mobile\":\"").append(escapeJson(rs.getString("mobile"))).append("\",");
                jsonBuilder.append("\"address\":\"").append(escapeJson(fullAddress)).append("\",");
                jsonBuilder.append("\"productName\":\"").append(escapeJson(rs.getString("product_name"))).append("\",");
                jsonBuilder.append("\"brand\":\"").append(escapeJson(rs.getString("brand"))).append("\",");
                jsonBuilder.append("\"quantity\":").append(rs.getInt("quantity")).append(",");
                jsonBuilder.append("\"paymentMethod\":\"").append(escapeJson(rs.getString("payment_method"))).append("\",");
                jsonBuilder.append("\"amount\":").append(rs.getDouble("total_amount")).append(",");
                jsonBuilder.append("\"status\":\"").append(escapeJson(rs.getString("order_status"))).append("\"");
                jsonBuilder.append("}");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        jsonBuilder.append("]");
        out.print(jsonBuilder.toString());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String orderIdStr = request.getParameter("orderId");
        String newStatus = request.getParameter("status");

        if (orderIdStr != null && newStatus != null) {
            int orderId = Integer.parseInt(orderIdStr);

            try (Connection con = DBConnection.getConnection()) {
                String updateSql = "UPDATE orders SET order_status = ? WHERE order_id = ?";
                try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                    ps.setString(1, newStatus);
                    ps.setInt(2, orderId);
                    int rows = ps.executeUpdate();

                    if (rows > 0) {
                        response.getWriter().print("success");
                    } else {
                        response.getWriter().print("failed");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.getWriter().print("error");
            }
        }
    }

    private String escapeJson(String data) {
        if (data == null) return "";
        return data.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}