import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/CartActionServlet")
public class CartActionServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        String action = request.getParameter("action");
        String cartIdParam = request.getParameter("cartId");

        // Validate incoming parameters to prevent NumberFormatException crashes
        if (action == null || cartIdParam == null) {
            out.print("error");
            return;
        }

        try {
            int cartId = Integer.parseInt(cartIdParam);

            try (Connection con = DBConnection.getConnection()) {
                
                if ("update".equals(action)) {
                    String changeParam = request.getParameter("change");
                    if (changeParam == null) {
                        out.print("error");
                        return;
                    }
                    int change = Integer.parseInt(changeParam);
                    
                    // 1. First update the quantity safely
                    String updateSql = "UPDATE cart SET quantity = quantity + ? WHERE cart_id = ?";
                    try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                        ps.setInt(1, change);
                        ps.setInt(2, cartId);
                        ps.executeUpdate();
                    }
                    
                    // 2. Prevent negative quantities and automatically remove if <= 0
                    String deleteSql = "DELETE FROM cart WHERE cart_id = ? AND quantity <= 0";
                    try (PreparedStatement ps = con.prepareStatement(deleteSql)) {
                        ps.setInt(1, cartId);
                        ps.executeUpdate();
                    }
                    
                    out.print("success");

                } 
                else if ("remove".equals(action)) {
                    String deleteSql = "DELETE FROM cart WHERE cart_id = ?";
                    try (PreparedStatement ps = con.prepareStatement(deleteSql)) {
                        ps.setInt(1, cartId);
                        ps.executeUpdate();
                    }
                    out.print("success");
                } 
                else {
                    out.print("error");
                }

            }
        } catch (NumberFormatException e) {
            // Handles invalid parameter formats gracefully
            e.printStackTrace();
            out.print("error");
        } catch (Exception e) {
            e.printStackTrace();
            out.print("error");
        }
    }
}