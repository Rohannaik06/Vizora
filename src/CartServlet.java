import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/CartServlet")
public class CartServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        try {
            int userId = Integer.parseInt(request.getParameter("userId"));
            int productId = Integer.parseInt(request.getParameter("productId"));

            try (Connection con = DBConnection.getConnection()) {
                // चेक करा प्रोडक्ट आधीच आहे का
                String checkSql = "SELECT quantity FROM cart WHERE user_id=? AND product_id=?";
                try (PreparedStatement check = con.prepareStatement(checkSql)) {
                    check.setInt(1, userId);
                    check.setInt(2, productId);
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next()) {
                            // जर असेल तर quantity वाढवा
                            String updateSql = "UPDATE cart SET quantity = quantity + 1 WHERE user_id=? AND product_id=?";
                            try (PreparedStatement update = con.prepareStatement(updateSql)) {
                                update.setInt(1, userId);
                                update.setInt(2, productId);
                                update.executeUpdate();
                            }
                        } else {
                            // नसेल तर नवीन एन्ट्री करा
                            String insertSql = "INSERT INTO cart(user_id, product_id, quantity) VALUES(?,?,1)";
                            try (PreparedStatement insert = con.prepareStatement(insertSql)) {
                                insert.setInt(1, userId);
                                insert.setInt(2, productId);
                                insert.executeUpdate();
                            }
                        }
                    }
                }
                out.print("success");
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("error");
        }
    }
}