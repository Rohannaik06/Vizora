import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/ProductServlet")
public class ProductServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        StringBuilder json = new StringBuilder();
        json.append("[");

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT product_id, product_name, brand, category, gender, description, " +
                     "original_price, selling_price, stock, thumbnail, " +
                     "image_1, image_2, image_3, image_4, status " +
                     "FROM products WHERE status='ACTIVE' ORDER BY product_id DESC")) {

            boolean first = true;

            while (rs.next()) {
                if (!first) {
                    json.append(",");
                }
                first = false;

                // डिस्काउंट कॅल्क्युलेशन
                double origPrice = rs.getDouble("original_price");
                double sellPrice = rs.getDouble("selling_price");
                int discount = 0;
                if (origPrice > sellPrice && origPrice > 0) {
                    discount = (int) Math.round(((origPrice - sellPrice) / origPrice) * 100);
                }

                json.append("{");
                json.append("\"id\":").append(rs.getInt("product_id")).append(",");
                json.append("\"name\":\"").append(escapeJson(rs.getString("product_name"))).append("\",");
                json.append("\"brand\":\"").append(escapeJson(rs.getString("brand"))).append("\",");
                json.append("\"category\":\"").append(escapeJson(rs.getString("category"))).append("\",");
                json.append("\"gender\":\"").append(escapeJson(rs.getString("gender"))).append("\",");
                json.append("\"description\":\"").append(escapeJson(rs.getString("description"))).append("\",");
                json.append("\"originalPrice\":").append(origPrice).append(",");
                json.append("\"sellingPrice\":").append(sellPrice).append(",");
                json.append("\"discount\":").append(discount).append(",");
                json.append("\"stock\":").append(rs.getInt("stock")).append(",");
                json.append("\"thumbnail\":\"").append(escapeJson(rs.getString("thumbnail"))).append("\",");
                json.append("\"image1\":\"").append(escapeJson(rs.getString("image_1"))).append("\",");
                json.append("\"image2\":\"").append(escapeJson(rs.getString("image_2"))).append("\",");
                json.append("\"image3\":\"").append(escapeJson(rs.getString("image_3"))).append("\",");
                json.append("\"image4\":\"").append(escapeJson(rs.getString("image_4"))).append("\"");
                json.append("}");
            }

            json.append("]");
            response.getWriter().print(json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().print("[]"); 
        }
    }

    private String escapeJson(String data) {
        if (data == null) {
            return "";
        }
        return data.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r");
    }
}