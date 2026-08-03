import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/DashboardStatsServlet")
public class DashboardStatsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        double totalRevenue = 0;
        int totalOrders = 0;
        int confirmedOrders = 0;
        int cancelledOrders = 0;
        int totalProducts = 0;
        int totalUsers = 0;

        StringBuilder ordersJson = new StringBuilder();
        ordersJson.append("[");

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement()) {

            // १. एकाच SQL क्वेरीमध्ये Revenue आणि Orders (Total, Confirmed, Cancelled) चे सर्व कॅल्क्युलेशन करणे (Professional Approach)
            String statsSql = "SELECT " +
                              "SUM(CASE WHEN order_status != 'CANCELLED' THEN total_amount ELSE 0 END) AS revenue, " +
                              "COUNT(*) AS total_count, " +
                              "SUM(CASE WHEN order_status = 'CONFIRMED' OR order_status = 'DELIVERED' THEN 1 ELSE 0 END) AS confirmed_count, " +
                              "SUM(CASE WHEN order_status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled_count " +
                              "FROM orders";
                              
            ResultSet rsOrders = st.executeQuery(statsSql);
            if (rsOrders.next()) {
                totalRevenue = rsOrders.getDouble("revenue");
                totalOrders = rsOrders.getInt("total_count");
                confirmedOrders = rsOrders.getInt("confirmed_count");
                cancelledOrders = rsOrders.getInt("cancelled_count");
            }
            rsOrders.close();

            // २. प्रॉडक्ट्सची एकूण संख्या
            ResultSet rsProd = st.executeQuery("SELECT COUNT(*) AS count FROM products");
            if (rsProd.next()) {
                totalProducts = rsProd.getInt("count");
            }
            rsProd.close();

            // ३. युजर्सची एकूण संख्या
            try {
                ResultSet rsUsers = st.executeQuery("SELECT COUNT(*) AS count FROM users");
                if (rsUsers.next()) {
                    totalUsers = rsUsers.getInt("count");
                }
                rsUsers.close();
            } catch (Exception e) {
                totalUsers = 0; 
            }

            // ४. डॅशबोर्ड टेबलसाठी फक्त टॉप १० अलीकडील ऑर्डर्स फेच करणे (LIMIT 10)
            String detailedOrdersQuery = 
                "SELECT o.order_id, o.full_name, o.mobile, o.payment_method, o.total_amount, o.order_status, " +
                "p.product_name, p.brand, oi.quantity " +
                "FROM orders o " +
                "LEFT JOIN order_items oi ON o.order_id = oi.order_id " +
                "LEFT JOIN products p ON oi.product_id = p.product_id " +
                "ORDER BY o.order_date DESC LIMIT 10";

            ResultSet rsList = st.executeQuery(detailedOrdersQuery);
            boolean first = true;
            while (rsList.next()) {
                if (!first) {
                    ordersJson.append(",");
                }
                first = false;

                ordersJson.append("{");
                ordersJson.append("\"orderId\":").append(rsList.getInt("order_id")).append(",");
                ordersJson.append("\"customer\":\"").append(escapeJson(rsList.getString("full_name"))).append("\",");
                ordersJson.append("\"mobile\":\"").append(escapeJson(rsList.getString("mobile"))).append("\",");
                ordersJson.append("\"productName\":\"").append(escapeJson(rsList.getString("product_name"))).append("\",");
                ordersJson.append("\"brand\":\"").append(escapeJson(rsList.getString("brand"))).append("\",");
                ordersJson.append("\"quantity\":").append(rsList.getInt("quantity")).append(",");
                ordersJson.append("\"paymentMethod\":\"").append(escapeJson(rsList.getString("payment_method"))).append("\",");
                ordersJson.append("\"amount\":").append(rsList.getDouble("total_amount")).append(",");
                ordersJson.append("\"status\":\"").append(escapeJson(rsList.getString("order_status"))).append("\"");
                ordersJson.append("}");
            }
            rsList.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        ordersJson.append("]");

        // ५. JSON पॅकेजमध्ये नवीन variables ॲड करणे
        String finalJson = String.format(java.util.Locale.US,
            "{\"totalRevenue\":%.2f, \"totalOrders\":%d, \"confirmedOrders\":%d, \"cancelledOrders\":%d, \"totalProducts\":%d, \"totalUsers\":%d, \"recentOrders\":%s}",
            totalRevenue, totalOrders, confirmedOrders, cancelledOrders, totalProducts, totalUsers, ordersJson.toString()
        );

        out.print(finalJson);
    }

    private String escapeJson(String data) {
        if (data == null) return "";
        return data.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}