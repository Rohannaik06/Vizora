import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/CancelOrderServlet")
public class CancelOrderServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        String orderId = request.getParameter("orderId");
        
        try (Connection con = DBConnection.getConnection()) {
            
            // 1. Only update status if it is still in an early stage
            String sql = "UPDATE orders SET order_status = 'CANCELLED' " +
                         "WHERE order_id = ? AND (order_status = 'PENDING' OR order_status = 'CONFIRMED')";
            
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, orderId);
                int result = ps.executeUpdate();
                
                if (result > 0) {
                    
                    // 2. RESTORE PRODUCT QUANTITY (STOCK INCREASE)
                    // Get product_id and quantity from order_items table for this order
                    String getItemsSql = "SELECT product_id, quantity FROM order_items WHERE order_id = ?";
                    try (PreparedStatement itemPs = con.prepareStatement(getItemsSql)) {
                        itemPs.setString(1, orderId);
                        try (ResultSet rs = itemPs.executeQuery()) {
                            
                            while (rs.next()) {
                                int productId = rs.getInt("product_id");
                                int qty = rs.getInt("quantity");
                                
                                // Add back the quantity to products table
                                String restoreStockSql = "UPDATE products SET stock = stock + ? WHERE product_id = ?";
                                try (PreparedStatement restorePs = con.prepareStatement(restoreStockSql)) {
                                    restorePs.setInt(1, qty);
                                    restorePs.setInt(2, productId);
                                    restorePs.executeUpdate();
                                }
                            }
                        }
                    }

                    response.getWriter().print("success");
                } else {
                    response.getWriter().print("failed");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().print("error");
        }
    }
}