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

@WebServlet("/OrderServlet")
public class OrderServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        try {
            // 1. Get all details from the frontend checkout form
            int userId = Integer.parseInt(request.getParameter("userId"));
            int productId = Integer.parseInt(request.getParameter("productId"));
            
            String fullName = request.getParameter("fullName");
            String mobile = request.getParameter("mobile");
            String pincode = request.getParameter("pincode");
            String locality = request.getParameter("locality");
            String address = request.getParameter("address");
            String city = request.getParameter("city");
            String state = request.getParameter("state");
            String paymentMethod = request.getParameter("paymentMethod");

            try (Connection con = DBConnection.getConnection()) {
                
                // 2. Securely fetch the real price and current stock from the products table
                double realPrice = 0.0;
                int currentStock = 0;
                String priceSql = "SELECT selling_price, stock FROM products WHERE product_id = ?";
                try (PreparedStatement pricePs = con.prepareStatement(priceSql)) {
                    pricePs.setInt(1, productId);
                    try (ResultSet rs = pricePs.executeQuery()) {
                        if (rs.next()) {
                            realPrice = rs.getDouble("selling_price");
                            currentStock = rs.getInt("stock");
                        } else {
                            out.print("error: product not found");
                            return;
                        }
                    }
                }

                // Check if stock is available
                if (currentStock <= 0) {
                    out.print("error: product is out of stock");
                    return;
                }

                // 3. Insert into `orders` table with explicit 'PENDING' status (Industry Standard Flow)
                String orderSql = "INSERT INTO orders (user_id, full_name, mobile, pincode, locality, address, city, state, payment_method, total_amount, order_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')";
                
                try (PreparedStatement orderPs = con.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                    orderPs.setInt(1, userId);
                    orderPs.setString(2, fullName);
                    orderPs.setString(3, mobile);
                    orderPs.setString(4, pincode);
                    orderPs.setString(5, locality);
                    orderPs.setString(6, address);
                    orderPs.setString(7, city);
                    orderPs.setString(8, state);
                    orderPs.setString(9, paymentMethod);
                    orderPs.setDouble(10, realPrice);

                    int rowsAffected = orderPs.executeUpdate();

                    if (rowsAffected > 0) {
                        // 4. Get the generated order_id
                        try (ResultSet generatedKeys = orderPs.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                int newOrderId = generatedKeys.getInt(1);

                                // 5. Insert into `order_items` table
                                String itemSql = "INSERT INTO order_items (order_id, product_id, price, quantity) VALUES (?, ?, ?, 1)";
                                try (PreparedStatement itemPs = con.prepareStatement(itemSql)) {
                                    itemPs.setInt(1, newOrderId);
                                    itemPs.setInt(2, productId);
                                    itemPs.setDouble(3, realPrice);
                                    
                                    itemPs.executeUpdate();
                                }

                                // 6. Reduce product quantity from database (stock - 1)
                                String reduceStockSql = "UPDATE products SET stock = stock - 1 WHERE product_id = ?";
                                try (PreparedStatement stockPs = con.prepareStatement(reduceStockSql)) {
                                    stockPs.setInt(1, productId);
                                    stockPs.executeUpdate();
                                }

                                out.print("success");
                            }
                        }
                    } else {
                        out.print("error: failed to create order");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("error: " + e.getMessage());
        }
    }
}