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
        int cartId = Integer.parseInt(request.getParameter("cartId"));

        try (Connection con = DBConnection.getConnection()) {
            
            // १. जर ॲक्शन "update" असेल (Quantity कमी/जास्त करण्यासाठी)
            if ("update".equals(action)) {
                int change = Integer.parseInt(request.getParameter("change"));
                
                // आधी क्वांटिटी अपडेट करा (+1 किंवा -1)
                String updateSql = "UPDATE cart SET quantity = quantity + ? WHERE cart_id = ?";
                try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                    ps.setInt(1, change);
                    ps.setInt(2, cartId);
                    ps.executeUpdate();
                }
                
                // प्रो-टीप: जर क्वांटिटी 0 झाली, तर तो आयटम कार्टमधून आपोआप डिलीट करा
                String deleteSql = "DELETE FROM cart WHERE cart_id = ? AND quantity <= 0";
                try (PreparedStatement ps = con.prepareStatement(deleteSql)) {
                    ps.setInt(1, cartId);
                    ps.executeUpdate();
                }
                out.print("success");

            } 
            // २. जर ॲक्शन "remove" असेल (आयटम पूर्णपणे काढण्यासाठी)
            else if ("remove".equals(action)) {
                String deleteSql = "DELETE FROM cart WHERE cart_id = ?";
                try (PreparedStatement ps = con.prepareStatement(deleteSql)) {
                    ps.setInt(1, cartId);
                    ps.executeUpdate();
                }
                out.print("success");
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.print("error");
        }
    }
}