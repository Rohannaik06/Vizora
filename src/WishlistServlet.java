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

@WebServlet("/WishlistServlet")
public class WishlistServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        try {
            int userId = Integer.parseInt(request.getParameter("userId"));
            int productId = Integer.parseInt(request.getParameter("productId"));

            try (Connection con = DBConnection.getConnection()) {
                
                // १. चेक करा प्रोडक्ट आधीच विशलिस्ट मध्ये आहे का?
                String checkSql = "SELECT wishlist_id FROM wishlist WHERE user_id=? AND product_id=?";
                try (PreparedStatement check = con.prepareStatement(checkSql)) {
                    check.setInt(1, userId);
                    check.setInt(2, productId);
                    
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next()) {
                            // २. जर आधीच असेल, तर डिलीट करा (Toggle Off)
                            String deleteSql = "DELETE FROM wishlist WHERE user_id=? AND product_id=?";
                            try (PreparedStatement delete = con.prepareStatement(deleteSql)) {
                                delete.setInt(1, userId);
                                delete.setInt(2, productId);
                                delete.executeUpdate();
                            }
                            out.print("removed");
                        } else {
                            // ३. जर नसेल, तर इन्सर्ट करा (Toggle On)
                            String insertSql = "INSERT INTO wishlist(user_id, product_id) VALUES(?,?)";
                            try (PreparedStatement insert = con.prepareStatement(insertSql)) {
                                insert.setInt(1, userId);
                                insert.setInt(2, productId);
                                insert.executeUpdate();
                            }
                            out.print("added");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("error");
        }
    }
}