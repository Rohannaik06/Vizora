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

@WebServlet("/AdminProductServlet")
public class AdminProductServlet extends HttpServlet {

    // प्रॉडक्ट्स डेटाबेसवरून फेच करण्यासाठी (GET)
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("[");

        // स्टॉकनुसार चढत्या क्रमाने (Min to Max) सॉर्ट करून डेटा फेच करणे
        String sql = "SELECT * FROM products ORDER BY stock ASC";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            boolean first = true;
            while (rs.next()) {
                if (!first) {
                    jsonBuilder.append(",");
                }
                first = false;

                jsonBuilder.append("{");
                jsonBuilder.append("\"id\":").append(rs.getInt("product_id")).append(",");
                jsonBuilder.append("\"name\":\"").append(escapeJson(rs.getString("product_name"))).append("\",");
                jsonBuilder.append("\"brand\":\"").append(escapeJson(rs.getString("brand"))).append("\",");
                jsonBuilder.append("\"category\":\"").append(escapeJson(rs.getString("category"))).append("\",");
                jsonBuilder.append("\"gender\":\"").append(escapeJson(rs.getString("gender"))).append("\",");
                jsonBuilder.append("\"origPrice\":").append(rs.getDouble("original_price")).append(",");
                jsonBuilder.append("\"sellPrice\":").append(rs.getDouble("selling_price")).append(",");
                jsonBuilder.append("\"stock\":").append(rs.getInt("stock")).append(",");
                jsonBuilder.append("\"status\":\"").append(escapeJson(rs.getString("status"))).append("\",");
                jsonBuilder.append("\"thumb\":\"").append(escapeJson(rs.getString("thumbnail"))).append("\",");
                jsonBuilder.append("\"img1\":\"").append(escapeJson(rs.getString("image_1"))).append("\",");
                jsonBuilder.append("\"img2\":\"").append(escapeJson(rs.getString("image_2"))).append("\",");
                jsonBuilder.append("\"img3\":\"").append(escapeJson(rs.getString("image_3"))).append("\",");
                jsonBuilder.append("\"img4\":\"").append(escapeJson(rs.getString("image_4"))).append("\"");
                jsonBuilder.append("}");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        jsonBuilder.append("]");
        out.print(jsonBuilder.toString());
    }

    // नवीन प्रॉडक्ट डेटाबेसमध्ये ॲड करण्यासाठी (POST)
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String name = request.getParameter("product_name");
        String brand = request.getParameter("brand");
        String category = request.getParameter("category");
        String gender = request.getParameter("gender");
        String description = request.getParameter("description");
        double origPrice = Double.parseDouble(request.getParameter("original_price"));
        double sellPrice = Double.parseDouble(request.getParameter("selling_price"));
        int stock = Integer.parseInt(request.getParameter("stock"));
        String status = request.getParameter("status");
        String thumbnail = request.getParameter("thumbnail");
        String img1 = request.getParameter("image_1");
        String img2 = request.getParameter("image_2");
        String img3 = request.getParameter("image_3");
        String img4 = request.getParameter("image_4");

        String sql = "INSERT INTO products (product_name, brand, category, gender, description, original_price, selling_price, stock, status, thumbnail, image_1, image_2, image_3, image_4) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, brand);
            ps.setString(3, category);
            ps.setString(4, gender);
            ps.setString(5, description);
            ps.setDouble(6, origPrice);
            ps.setDouble(7, sellPrice);
            ps.setInt(8, stock);
            ps.setString(9, status);
            ps.setString(10, thumbnail);
            ps.setString(11, img1);
            ps.setString(12, img2);
            ps.setString(13, img3);
            ps.setString(14, img4);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                response.sendRedirect("AdminProducts.html"); // यशस्वीरीत्या ॲड झाल्यावर पुन्हा पेजवर येणे
            } else {
                response.getWriter().println("Failed to add product.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().println("Error: " + e.getMessage());
        }
    }

    private String escapeJson(String data) {
        if (data == null) return "";
        return data.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}