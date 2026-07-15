import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@WebServlet("/ViewWishlistServlet")
public class ViewWishlistServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            int userId = Integer.parseInt(request.getParameter("userId"));
            StringBuilder json = new StringBuilder("[");

            try (Connection con = DBConnection.getConnection()) {
                // विशलिस्ट आणि प्रोडक्ट्स टेबल JOIN करत आहोत
                String sql = "SELECT p.product_id, p.product_name, p.brand, p.selling_price, p.original_price, p.thumbnail " +
                             "FROM wishlist w JOIN products p ON w.product_id = p.product_id " +
                             "WHERE w.user_id = ? ORDER BY w.added_at DESC";

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        boolean first = true;
                        while (rs.next()) {
                            if (!first) json.append(",");
                            first = false;

                            json.append("{")
                                .append("\"id\":").append(rs.getInt("product_id")).append(",")
                                .append("\"name\":\"").append(escapeJson(rs.getString("product_name"))).append("\",")
                                .append("\"brand\":\"").append(escapeJson(rs.getString("brand"))).append("\",")
                                .append("\"sellingPrice\":").append(rs.getDouble("selling_price")).append(",")
                                .append("\"originalPrice\":").append(rs.getDouble("original_price")).append(",")
                                .append("\"thumbnail\":\"").append(escapeJson(rs.getString("thumbnail"))).append("\"")
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