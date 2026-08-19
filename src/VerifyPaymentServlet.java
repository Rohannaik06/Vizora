import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/VerifyPaymentServlet")
public class VerifyPaymentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String KEY_SECRET = "zHv3yj4hhP75wkRzYk8aWUI1";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        try {
            String razorpayPaymentId = request.getParameter("razorpay_payment_id");
            String razorpayOrderId = request.getParameter("razorpay_order_id");
            String razorpaySignature = request.getParameter("razorpay_signature");

            int userId = Integer.parseInt(request.getParameter("userId"));
            String fullName = request.getParameter("fullName");
            String mobile = request.getParameter("mobile");
            String pincode = request.getParameter("pincode");
            String locality = request.getParameter("locality");
            String address = request.getParameter("address");
            String city = request.getParameter("city");
            String state = request.getParameter("state");
            String checkoutType = request.getParameter("checkoutType");
            String productIdStr = request.getParameter("productId");

            // 1. HMAC-SHA256 वापरून Razorpay सिग्नेचर व्हेरिफाय करणे
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            String generatedSignature = calculateHmacSha256(payload, KEY_SECRET);

            if (!generatedSignature.equals(razorpaySignature)) {
                out.print("error: payment signature verification failed");
                return;
            }

            // 2. सिग्नेचर मॅच झाली, डेटाबेसमध्ये ऑर्डर 'CONFIRMED' आणि 'ONLINE' म्हणून सेव्ह करणे
            try (Connection con = DBConnection.getConnection()) {
                
                if ("cart".equals(checkoutType)) {
                    String cartSql = "SELECT c.product_id, c.quantity, p.selling_price FROM cart c JOIN products p ON c.product_id = p.product_id WHERE c.user_id = ?";
                    double totalAmount = 0.0;

                    try (PreparedStatement ps = con.prepareStatement(cartSql)) {
                        ps.setInt(1, userId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                totalAmount += rs.getDouble("selling_price") * rs.getInt("quantity");
                            }
                        }
                    }

                    // 'PAID' ऐवजी 'CONFIRMED' केले जेणेकरून युजरला सुंदर दिसेल
                    String orderSql = "INSERT INTO orders (user_id, full_name, mobile, pincode, locality, address, city, state, payment_method, total_amount, order_status, payment_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ONLINE', ?, 'CONFIRMED', ?)";
                    int newOrderId = 0;

                    try (PreparedStatement orderPs = con.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                        orderPs.setInt(1, userId);
                        orderPs.setString(2, fullName);
                        orderPs.setString(3, mobile);
                        orderPs.setString(4, pincode);
                        orderPs.setString(5, locality);
                        orderPs.setString(6, address);
                        orderPs.setString(7, city);
                        orderPs.setString(8, state);
                        orderPs.setDouble(9, totalAmount);
                        orderPs.setString(10, razorpayPaymentId);

                        if (orderPs.executeUpdate() > 0) {
                            try (ResultSet keys = orderPs.getGeneratedKeys()) {
                                if (keys.next()) newOrderId = keys.getInt(1);
                            }
                        }
                    }

                    if (newOrderId > 0) {
                        try (PreparedStatement fetchCart = con.prepareStatement("SELECT c.product_id, c.quantity, p.selling_price FROM cart c JOIN products p ON c.product_id = p.product_id WHERE c.user_id = ?")) {
                            fetchCart.setInt(1, userId);
                            try (ResultSet rs = fetchCart.executeQuery()) {
                                while (rs.next()) {
                                    int prodId = rs.getInt("product_id");
                                    int qty = rs.getInt("quantity");
                                    double price = rs.getDouble("selling_price");

                                    try (PreparedStatement itemPs = con.prepareStatement("INSERT INTO order_items (order_id, product_id, price, quantity) VALUES (?, ?, ?, ?)")) {
                                        itemPs.setInt(1, newOrderId);
                                        itemPs.setInt(2, prodId);
                                        itemPs.setDouble(3, price);
                                        itemPs.setInt(4, qty);
                                        itemPs.executeUpdate();
                                    }

                                    try (PreparedStatement stockPs = con.prepareStatement("UPDATE products SET stock = stock - ? WHERE product_id = ?")) {
                                        stockPs.setInt(1, qty);
                                        stockPs.setInt(2, prodId);
                                        stockPs.executeUpdate();
                                    }
                                }
                            }
                        }

                        try (PreparedStatement clearCart = con.prepareStatement("DELETE FROM cart WHERE user_id = ?")) {
                            clearCart.setInt(1, userId);
                            clearCart.executeUpdate();
                        }

                        out.print("success");
                    } else {
                        out.print("error: failed to save cart order");
                    }

                } else {
                    int productId = Integer.parseInt(productIdStr);
                    double realPrice = 0.0;

                    try (PreparedStatement ps = con.prepareStatement("SELECT selling_price FROM products WHERE product_id = ?")) {
                        ps.setInt(1, productId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) realPrice = rs.getDouble("selling_price");
                        }
                    }

                    // 'PAID' ऐवजी 'CONFIRMED' केले
                    String orderSql = "INSERT INTO orders (user_id, full_name, mobile, pincode, locality, address, city, state, payment_method, total_amount, order_status, payment_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ONLINE', ?, 'CONFIRMED', ?)";
                    
                    try (PreparedStatement orderPs = con.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                        orderPs.setInt(1, userId);
                        orderPs.setString(2, fullName);
                        orderPs.setString(3, mobile);
                        orderPs.setString(4, pincode);
                        orderPs.setString(5, locality);
                        orderPs.setString(6, address);
                        orderPs.setString(7, city);
                        orderPs.setString(8, state);
                        orderPs.setDouble(9, realPrice);
                        orderPs.setString(10, razorpayPaymentId);

                        if (orderPs.executeUpdate() > 0) {
                            try (ResultSet keys = orderPs.getGeneratedKeys()) {
                                if (keys.next()) {
                                    int newOrderId = keys.getInt(1);

                                    try (PreparedStatement itemPs = con.prepareStatement("INSERT INTO order_items (order_id, product_id, price, quantity) VALUES (?, ?, ?, 1)")) {
                                        itemPs.setInt(1, newOrderId);
                                        itemPs.setInt(2, productId);
                                        itemPs.setDouble(3, realPrice);
                                        itemPs.executeUpdate();
                                    }

                                    try (PreparedStatement stockPs = con.prepareStatement("UPDATE products SET stock = stock - 1 WHERE product_id = ?")) {
                                        stockPs.setInt(1, productId);
                                        stockPs.executeUpdate();
                                    }

                                    out.print("success");
                                }
                            }
                        } else {
                            out.print("error: failed to save direct order");
                        }
                    }
                }
            }

        } catch (Throwable e) {
            e.printStackTrace();
            out.print("error: " + e.getMessage());
        }
    }

    private String calculateHmacSha256(String data, String secret) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to calculate hmac-sha256", e);
        }
    }
}