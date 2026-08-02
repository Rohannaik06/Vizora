import java.io.BufferedReader;
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

    // १. प्रॉडक्ट्स डेटाबेसवरून फेच करण्यासाठी (GET)
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("[");

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

    // २. स्मार्ट ॲड प्रॉडक्ट (जर आधीपासून असेल तर स्टॉक प्लस होणार, अन्यथा नवीन इन्सर्ट) (POST)
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String name = request.getParameter("product_name");
        String brand = request.getParameter("brand");
        int newStock = Integer.parseInt(request.getParameter("stock"));

        try (Connection con = DBConnection.getConnection()) {
            
            // प्रॉडक्ट आधीपासून आहे का चेक करणे
            String checkSql = "SELECT product_id FROM products WHERE product_name = ? AND brand = ?";
            try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
                checkPs.setString(1, name);
                checkPs.setString(2, brand);
                
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        // प्रॉडक्ट उपलब्ध आहे -> फक्त स्टॉक अपडेट करणे
                        int existingId = rs.getInt("product_id");
                        String updateStockSql = "UPDATE products SET stock = stock + ? WHERE product_id = ?";
                        try (PreparedStatement updatePs = con.prepareStatement(updateStockSql)) {
                            updatePs.setInt(1, newStock);
                            updatePs.setInt(2, existingId);
                            updatePs.executeUpdate();
                        }
                    } else {
                        // नवीन प्रॉडक्ट आहे -> नवीन इन्सर्ट करणे
                        String insertSql = "INSERT INTO products (product_name, brand, category, gender, description, original_price, selling_price, stock, status, thumbnail, image_1, image_2, image_3, image_4) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement insertPs = con.prepareStatement(insertSql)) {
                            insertPs.setString(1, name);
                            insertPs.setString(2, brand);
                            insertPs.setString(3, request.getParameter("category"));
                            insertPs.setString(4, request.getParameter("gender"));
                            insertPs.setString(5, request.getParameter("description"));
                            insertPs.setDouble(6, Double.parseDouble(request.getParameter("original_price")));
                            insertPs.setDouble(7, Double.parseDouble(request.getParameter("selling_price")));
                            insertPs.setInt(8, newStock);
                            insertPs.setString(9, request.getParameter("status"));
                            insertPs.setString(10, request.getParameter("thumbnail"));
                            insertPs.setString(11, request.getParameter("image_1"));
                            insertPs.setString(12, request.getParameter("image_2"));
                            insertPs.setString(13, request.getParameter("image_3"));
                            insertPs.setString(14, request.getParameter("image_4"));
                            
                            insertPs.executeUpdate();
                        }
                    }
                }
            }
            response.sendRedirect("AdminProducts.html");

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().println("Error: " + e.getMessage());
        }
    }

    // ३. टेबलमधील स्टॉक (+1 किंवा -1) अपडेट करण्यासाठी (PUT)
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        BufferedReader reader = request.getReader();
        String line = reader.readLine();
        int productId = 0;
        int change = 0;

        if (line != null) {
            String[] pairs = line.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length > 1) {
                    if (keyValue[0].equals("productId")) productId = Integer.parseInt(keyValue[1]);
                    if (keyValue[0].equals("change")) change = Integer.parseInt(keyValue[1]);
                }
            }
        }

        try (Connection con = DBConnection.getConnection()) {
            // स्टॉक झिरोच्या खाली जाऊ नये म्हणून GREATEST वापरले आहे
            String sql = "UPDATE products SET stock = GREATEST(0, stock + ?) WHERE product_id = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, change);
                ps.setInt(2, productId);
                ps.executeUpdate();
                response.getWriter().print("success");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().print("error");
        }
    }

    // ४. प्रॉडक्ट पूर्णपणे डिलीट करण्यासाठी (DELETE)
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String productIdStr = request.getParameter("productId");
        if (productIdStr != null) {
            int productId = Integer.parseInt(productIdStr);

            try (Connection con = DBConnection.getConnection()) {
                String sql = "DELETE FROM products WHERE product_id = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, productId);
                    ps.executeUpdate();
                    response.getWriter().print("success");
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