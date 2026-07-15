import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@WebServlet("/ProductDetailsServlet")
public class ProductDetailsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            out.print("{}"); // Return empty JSON if no ID is provided
            return;
        }

        try {
            int productId = Integer.parseInt(idParam);

            try (Connection con = DBConnection.getConnection()) {
                String sql = "SELECT * FROM products WHERE product_id = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, productId);
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            // Calculate discount percentage safely
                            double original = rs.getDouble("original_price");
                            double selling = rs.getDouble("selling_price");
                            int discount = 0;
                            if (original > selling) {
                                discount = (int) Math.round(((original - selling) / original) * 100);
                            }

                            // Fetch the extra images safely
                            String img1 = rs.getString("image_1") != null ? rs.getString("image_1") : "";
                            String img2 = rs.getString("image_2") != null ? rs.getString("image_2") : "";
                            String img3 = rs.getString("image_3") != null ? rs.getString("image_3") : "";
                            String status = rs.getString("status") != null ? rs.getString("status") : "";

                            // Build JSON string with Locale.US to ensure decimals use dots (.) instead of commas (,)
                            String json = String.format(java.util.Locale.US,
                                "{\"id\":%d, \"name\":\"%s\", \"brand\":\"%s\", \"category\":\"%s\", \"gender\":\"%s\", \"description\":\"%s\", \"originalPrice\":%.2f, \"sellingPrice\":%.2f, \"discount\":%d, \"stock\":%d, \"thumbnail\":\"%s\", \"image1\":\"%s\", \"image2\":\"%s\", \"image3\":\"%s\", \"status\":\"%s\"}",
                                rs.getInt("product_id"),
                                escapeJson(rs.getString("product_name")),
                                escapeJson(rs.getString("brand")),
                                escapeJson(rs.getString("category")),
                                escapeJson(rs.getString("gender")),
                                escapeJson(rs.getString("description")),
                                original,
                                selling,
                                discount,
                                rs.getInt("stock"),
                                escapeJson(rs.getString("thumbnail")),
                                escapeJson(img1),
                                escapeJson(img2),
                                escapeJson(img3),
                                escapeJson(status)
                            );

                            out.print(json);
                        } else {
                            out.print("{}"); // Product not found
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("{}");
        }
    }

    // Helper method to prevent JSON parsing errors if descriptions have quotes or newlines
    private String escapeJson(String data) {
        if (data == null) return "";
        return data.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "");
    }
}