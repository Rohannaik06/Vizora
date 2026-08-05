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

@WebServlet("/AdminAnalyticsServlet")
public class AdminAnalyticsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        double totalRevenue = 0;
        int totalOrders = 0;
        
        // महिन्यानिहाय रेव्हेन्यू आणि ऑर्डर्स (जानेवारी ते डिसेंबर - १२ महिने)
        double[] monthlyRevenue = new double[12];
        int[] monthlyOrders = new int[12];
        
        // पेमेंट मोड्स (COD, UPI, Card)
        int codCount = 0, upiCount = 0, cardCount = 0;

        StringBuilder productsJson = new StringBuilder();
        productsJson.append("[");

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement()) {

            // 1. Fetch total orders and overall stats (excluding cancelled orders)
            String statsSql = "SELECT SUM(total_amount) AS revenue, COUNT(*) AS total_count FROM orders WHERE order_status != 'CANCELLED'";
            try (ResultSet rs = st.executeQuery(statsSql)) {
                if (rs.next()) {
                    totalRevenue = rs.getDouble("revenue");
                    totalOrders = rs.getInt("total_count");
                }
            }

            // 2. Fetch monthly breakdown from database based on order_date
            String monthlySql = "SELECT MONTH(order_date) AS m, SUM(total_amount) AS rev, COUNT(*) as cnt FROM orders WHERE order_status != 'CANCELLED' GROUP BY MONTH(order_date)";
            try (ResultSet rsMon = st.executeQuery(monthlySql)) {
                while (rsMon.next()) {
                    int month = rsMon.getInt("m"); // 1 to 12
                    if (month >= 1 && month <= 12) {
                        monthlyRevenue[month - 1] = rsMon.getDouble("rev");
                        monthlyOrders[month - 1] = rsMon.getInt("cnt");
                    }
                }
            }

            // 3. Fetch payment methods distribution
            String paySql = "SELECT payment_method, COUNT(*) AS cnt FROM orders GROUP BY payment_method";
            try (ResultSet rsPay = st.executeQuery(paySql)) {
                while (rsPay.next()) {
                    String method = (rsPay.getString("payment_method") != null) ? rsPay.getString("payment_method").toUpperCase() : "";
                    int count = rsPay.getInt("cnt");
                    if (method.contains("COD")) codCount += count;
                    else if (method.contains("UPI")) upiCount += count;
                    else if (method.contains("CARD")) cardCount += count;
                }
            }

            // 4. Fetch Products stock for live inventory line graph
            String prodListSql = "SELECT product_name, stock FROM products LIMIT 10";
            try (ResultSet rsList = st.executeQuery(prodListSql)) {
                boolean first = true;
                while (rsList.next()) {
                    if (!first) productsJson.append(",");
                    first = false;

                    productsJson.append("{");
                    productsJson.append("\"productName\":\"").append(escapeJson(rsList.getString("product_name"))).append("\",");
                    productsJson.append("\"stock\":").append(rsList.getInt("stock"));
                    productsJson.append("}");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        productsJson.append("]");

        // Arrays to JSON conversion helper
        String monthlyRevStr = java.util.Arrays.toString(monthlyRevenue);
        String monthlyOrdStr = java.util.Arrays.toString(monthlyOrders);

        // Final JSON Response Package
        String finalJson = String.format(java.util.Locale.US,
            "{\"totalRevenue\":%.2f, \"totalOrders\":%d, \"monthlyRevenue\":%s, \"monthlyOrders\":%s, \"codCount\":%d, \"upiCount\":%d, \"cardCount\":%d, \"productsStock\":%s}",
            totalRevenue, totalOrders, monthlyRevStr, monthlyOrdStr, codCount, upiCount, cardCount, productsJson.toString()
        );

        out.print(finalJson);
    }

    private String escapeJson(String data) {
        if (data == null) return "";
        return data.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}