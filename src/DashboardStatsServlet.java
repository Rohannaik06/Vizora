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
        int totalProducts = 0;
        int totalUsers = 0;

        StringBuilder ordersJson = new StringBuilder();
        ordersJson.append("[");

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement()) {

            // १. टोटल रेव्हेन्यू आणि ऑर्डर्सची संख्या मोजणे
            ResultSet rsOrders = st.executeQuery("SELECT SUM(total_amount) AS revenue, COUNT(*) AS count FROM orders");
            if (rsOrders.next()) {
                totalRevenue = rsOrders.getDouble("revenue");
                totalOrders = rsOrders.getInt("count");
            }
            rsOrders.close();

            // २. प्रॉडक्ट्सची संख्या मोजणे
            ResultSet rsProd = st.executeQuery("SELECT COUNT(*) AS count FROM products");
            if (rsProd.next()) {
                totalProducts = rsProd.getInt("count");
            }
            rsProd.close();

            // ३. युजर्सची संख्या मोजणे (जर तुमचे युजर्स टेबल असेल तर, नसल्यास 0 ठेवू शकता)
            try {
                ResultSet rsUsers = st.executeQuery("SELECT COUNT(*) AS count FROM users");
                if (rsUsers.next()) {
                    totalUsers = rsUsers.getInt("count");
                }
                rsUsers.close();
            } catch (Exception e) {
                totalUsers = 0; // युजर टेबल नसले तरी एरर येणार नाही
            }

            // ४. डॅशबोर्ड टेबलसाठी शेवटच्या ५ ऑर्डर्स फेच करणे
            ResultSet rsList = st.executeQuery("SELECT order_id, full_name, total_amount, order_status FROM orders ORDER BY order_date DESC LIMIT 5");
            boolean first = true;
            while (rsList.next()) {
                if (!first) {
                    ordersJson.append(",");
                }
                first = false;

                ordersJson.append("{");
                ordersJson.append("\"orderId\":").append(rsList.getInt("order_id")).append(",");
                ordersJson.append("\"customer\":\"").append(escapeJson(rsList.getString("full_name"))).append("\",");
                ordersJson.append("\"amount\":").append(rsList.getDouble("total_amount")).append(",");
                ordersJson.append("\"status\":\"").append(escapeJson(rsList.getString("order_status"))).append("\"");
                ordersJson.append("}");
            }
            rsList.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        ordersJson.append("]");

        // ५. मुख्य JSON पॅकेज तयार करून पाठवणे
        String finalJson = String.format(java.util.Locale.US,
            "{\"totalRevenue\":%.2f, \"totalOrders\":%d, \"totalProducts\":%d, \"totalUsers\":%d, \"recentOrders\":%s}",
            totalRevenue, totalOrders, totalProducts, totalUsers, ordersJson.toString()
        );

        out.print(finalJson);
    }

    private String escapeJson(String data) {
        if (data == null) return "";
        return data.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}