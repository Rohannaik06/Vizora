import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@WebServlet("/OrderHistoryServlet")
public class OrderHistoryServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String userIdParam = request.getParameter("userId");
        if (userIdParam == null || userIdParam.trim().isEmpty()) {
            out.print("[]");
            return;
        }

        try {
            int userId = Integer.parseInt(userIdParam);

            try (Connection con = DBConnection.getConnection()) {
                // Join the 3 tables to get order details + product images and names
                String sql = "SELECT o.order_id, DATE_FORMAT(o.order_date, '%d %b %Y') AS order_date, " +
                             "o.total_amount, o.order_status, p.product_name, p.brand, p.thumbnail " +
                             "FROM orders o " +
                             "JOIN order_items oi ON o.order_id = oi.order_id " +
                             "JOIN products p ON oi.product_id = p.product_id " +
                             "WHERE o.user_id = ? ORDER BY o.order_date DESC";

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        
                        out.print("["); // Start JSON Array
                        boolean isFirst = true;

                        while (rs.next()) {
                            if (!isFirst) {
                                out.print(",");
                            }
                            
                            String jsonObj = String.format(Locale.US,
                                "{\"orderId\":%d, \"date\":\"%s\", \"total\":%.2f, \"status\":\"%s\", \"name\":\"%s\", \"brand\":\"%s\", \"thumbnail\":\"%s\"}",
                                rs.getInt("order_id"),
                                escapeJson(rs.getString("order_date")),
                                rs.getDouble("total_amount"),
                                escapeJson(rs.getString("order_status")),
                                escapeJson(rs.getString("product_name")),
                                escapeJson(rs.getString("brand")),
                                escapeJson(rs.getString("thumbnail"))
                            );
                            out.print(jsonObj);
                            isFirst = false;
                        }
                        out.print("]"); // End JSON Array
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("[]"); // Return empty array on error
        }
    }

    private String escapeJson(String data) {
        if (data == null) return "";
        return data.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}