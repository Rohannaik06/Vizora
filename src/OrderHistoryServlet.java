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
import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/OrderHistoryServlet")
public class OrderHistoryServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String userIdStr = request.getParameter("userId");
        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            out.print("[]");
            return;
        }

        JSONArray ordersArray = new JSONArray();

        try (Connection con = DBConnection.getConnection()) {
            // तुमच्या products टेबलमधील 'thumbnail', 'product_name', 'brand' हे अचूक कॉल्युम्स वापरले आहेत
            String sql = "SELECT o.order_id, o.total_amount, o.order_status, o.order_date, o.payment_method, o.payment_id, " +
                         "p.product_id, p.product_name, p.brand, p.selling_price, p.thumbnail, oi.quantity " +
                         "FROM orders o " +
                         "JOIN order_items oi ON o.order_id = oi.order_id " +
                         "JOIN products p ON oi.product_id = p.product_id " +
                         "WHERE o.user_id = ? ORDER BY o.order_date DESC";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, Integer.parseInt(userIdStr));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        JSONObject orderObj = new JSONObject();
                        orderObj.put("orderId", rs.getInt("order_id"));
                        orderObj.put("total", rs.getDouble("total_amount"));
                        orderObj.put("status", rs.getString("order_status") != null ? rs.getString("order_status") : "CONFIRMED");
                        orderObj.put("date", rs.getString("order_date") != null ? rs.getString("order_date") : "");
                        
                        // पेमेंट मेथड (ONLINE किंवा COD)
                        String dbPaymentMethod = rs.getString("payment_method");
                        orderObj.put("paymentMethod", dbPaymentMethod != null ? dbPaymentMethod.trim().toUpperCase() : "COD");
                        
                        orderObj.put("paymentId", rs.getString("payment_id") != null ? rs.getString("payment_id") : "N/A");
                        orderObj.put("productId", rs.getInt("product_id"));
                        orderObj.put("name", rs.getString("product_name")); 
                        orderObj.put("brand", rs.getString("brand") != null ? rs.getString("brand") : "Vizora"); 
                        
                        // प्रॉडक्टचा अचूक थंबनेल फोटो लोड करणे
                        String thumb = rs.getString("thumbnail");
                        orderObj.put("thumbnail", (thumb != null && !thumb.trim().isEmpty()) ? thumb.trim() : "default.jpg");
                        
                        orderObj.put("quantity", rs.getInt("quantity"));

                        ordersArray.put(orderObj);
                    }
                }
            }
            out.print(ordersArray.toString());

        } catch (Exception e) {
            e.printStackTrace();
            out.print("[]");
        }
    }
}