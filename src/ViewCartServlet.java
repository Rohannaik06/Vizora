import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/ViewCartServlet")
public class ViewCartServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            int userId = Integer.parseInt(request.getParameter("userId"));
            
            StringBuilder json = new StringBuilder("[");
            try (Connection con = DBConnection.getConnection()) {
                String sql = "SELECT c.cart_id, c.quantity, p.product_id, p.product_name, p.brand, " +
                             "p.selling_price, p.original_price, p.thumbnail " +
                             "FROM cart c JOIN products p ON c.product_id = p.product_id " +
                             "WHERE c.user_id=?";
                
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        boolean first = true;
                        while (rs.next()) {
                            if (!first) json.append(",");
                            first = false;

                            json.append("{")
                                .append("\"cartId\":").append(rs.getInt("cart_id")).append(",")
                                .append("\"productId\":").append(rs.getInt("product_id")).append(",")
                                .append("\"name\":\"").append(escapeJson(rs.getString("product_name"))).append("\",")
                                .append("\"brand\":\"").append(escapeJson(rs.getString("brand"))).append("\",")
                                .append("\"price\":").append(rs.getDouble("selling_price")).append(",")
                                .append("\"original\":").append(rs.getDouble("original_price")).append(",")
                                .append("\"quantity\":").append(rs.getInt("quantity")).append(",")
                                .append("\"image\":\"").append(escapeJson(rs.getString("thumbnail"))).append("\"")
                                .append("}");
                        }
                    }
                }
            }
            json.append("]");
            response.getWriter().write(json.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("[]");
        }
    }

    private String escapeJson(String data) {
        return (data == null) ? "" : data.replace("\"", "\\\"");
    }
}