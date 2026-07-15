import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
            // Only update if status is still in an early stage
            String sql = "UPDATE orders SET order_status = 'CANCELLED' " +
                         "WHERE order_id = ? AND (order_status = 'PENDING' OR order_status = 'CONFIRMED')";
            
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, orderId);
                int result = ps.executeUpdate();
                
                if (result > 0) {
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