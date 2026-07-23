import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/SignupServlet")
public class SignupServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String name = request.getParameter("full_name");
        String email = request.getParameter("email");
        String mobile = request.getParameter("mobile");
        String password = request.getParameter("password");

        try {

            Connection con = DBConnection.getConnection();

            String sql = "INSERT INTO users(full_name, email, mobile, password) VALUES(?,?,?,?)";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, mobile);
            ps.setString(4, password);

            int row = ps.executeUpdate();

            ps.close();
            con.close();

            if (row > 0) {
                response.sendRedirect("/VIZORA/webapp/login.html");
            } else {
                response.getWriter().println("Signup Failed!");
            }

        } catch (Exception e) {

            response.setContentType("text/html");

            response.getWriter().println("<h2>Signup Failed!</h2>");
            response.getWriter().println("<p>" + e.getMessage() + "</p>");

            e.printStackTrace();

        }

    }

}