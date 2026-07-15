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

@WebServlet("/ForgotPasswordServlet")
public class ForgotPasswordServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        String email = request.getParameter("email");
        String newPassword = request.getParameter("newPassword");

        try {
            Connection con = DBConnection.getConnection();
            String checkQuery = "SELECT * FROM users WHERE email=?";
            PreparedStatement checkPs = con.prepareStatement(checkQuery);
            checkPs.setString(1, email);
            ResultSet rs = checkPs.executeQuery();

            if (rs.next()) {
                String updateQuery = "UPDATE users SET password=? WHERE email=?";
                PreparedStatement updatePs = con.prepareStatement(updateQuery);
                updatePs.setString(1, newPassword);
                updatePs.setString(2, email);

                int row = updatePs.executeUpdate();

                if (row > 0) {
                    out.println("<script>");
                    out.println("alert('Password updated successfully. Please login.');");
                    // CHANGE REQUIRED: Since login.html is in the root 'frontend/' folder, 
                    // and this request comes from the root, 'login.html' is correct.
                    out.println("window.location='../login.html';"); 
                    out.println("</script>");
                } else {
                    out.println("<script>");
                    out.println("alert('Password update failed!');");
                    out.println("history.back();");
                    out.println("</script>");
                }
                updatePs.close();
            } else {
                out.println("<script>");
                out.println("alert('Email is not registered!');");
                out.println("history.back();");
                out.println("</script>");
            }
            rs.close();
            checkPs.close();
            con.close();
        } catch(Exception e){
            e.printStackTrace();
            out.println("<script>");
            out.println("alert('Something went wrong!');");
            out.println("history.back();");
            out.println("</script>");
        }
    }
}