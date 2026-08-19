import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/CreateRazorpayOrderServlet")
public class CreateRazorpayOrderServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String KEY_ID = "rzp_test_TPAYx2yWXe0F1D";
    private static final String KEY_SECRET = "zHv3yj4hhP75wkRzYk8aWUI1";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try {
            int userId = Integer.parseInt(request.getParameter("userId"));
            String checkoutType = request.getParameter("checkoutType");
            String productIdStr = request.getParameter("productId");

            double totalAmount = 0.0;

            try (Connection con = DBConnection.getConnection()) {
                if ("cart".equals(checkoutType)) {
                    String cartSql = "SELECT c.quantity, p.selling_price FROM cart c JOIN products p ON c.product_id = p.product_id WHERE c.user_id = ?";
                    try (PreparedStatement ps = con.prepareStatement(cartSql)) {
                        ps.setInt(1, userId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                totalAmount += rs.getDouble("selling_price") * rs.getInt("quantity");
                            }
                        }
                    }
                } else if (productIdStr != null && !productIdStr.isEmpty()) {
                    int productId = Integer.parseInt(productIdStr);
                    String prodSql = "SELECT selling_price FROM products WHERE product_id = ?";
                    try (PreparedStatement ps = con.prepareStatement(prodSql)) {
                        ps.setInt(1, productId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                totalAmount = rs.getDouble("selling_price");
                            }
                        }
                    }
                }
            }

            if (totalAmount <= 0) {
                out.print("{\"status\":\"error\", \"message\":\"Invalid order amount\"}");
                return;
            }

            long amountInPaise = Math.round(totalAmount * 100);

            // Java 20+ साठी युनिव्हर्सल आणि आधुनिक पध्दत (Deprecation Fix)
            URL url = java.net.URI.create("https://api.razorpay.com/v1/orders").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            String auth = KEY_ID + ":" + KEY_SECRET;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

            String jsonInputString = "{\"amount\":" + amountInPaise + ", \"currency\":\"INR\", \"receipt\":\"txn_" + System.currentTimeMillis() + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            StringBuilder responseString = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    responseString.append(responseLine.trim());
                }
            }

            if (responseCode == 200) {
                String jsonResp = responseString.toString();
                String orderId = extractJsonValue(jsonResp, "id");

                out.print("{\"status\":\"success\", \"orderId\":\"" + orderId + "\", \"amount\":" + amountInPaise + ", \"key\":\"" + KEY_ID + "\"}");
            } else {
                out.print("{\"status\":\"error\", \"message\":\"Razorpay Error: " + responseString.toString().replace("\"", "'") + "\"}");
            }

        } catch (Throwable e) {
            e.printStackTrace();
            String errorMsg = e.getMessage() != null ? e.getMessage().replace("\"", "'") : "Unknown error";
            out.print("{\"status\":\"error\", \"message\":\"" + errorMsg + "\"}");
        }
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) return "";
        startIndex += searchKey.length();
        int endIndex = json.indexOf("\"", startIndex);
        return json.substring(startIndex, endIndex);
    }
}