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

@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");

        PrintWriter out = response.getWriter();

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {

            Connection con = DBConnection.getConnection();

            String sql = "SELECT * FROM users WHERE email=? AND password=?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, email);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if(rs.next()){

                int userId = rs.getInt("user_id");

                String userName = rs.getString("full_name");

                out.println("<script>");
                out.println("localStorage.setItem('isLoggedIn','true');");
                out.println("localStorage.setItem('userId','"+userId+"');");
                out.println("localStorage.setItem('userName','"+userName+"');");
                // FIXED PATH BELOW:
                out.println("window.location='/VIZORA/index.html';");
                out.println("</script>");

            }
            else{

                out.println("<script>");
                out.println("alert('Invalid Email or Password');");
                out.println("window.location='/VIZORA/webapp/login.html';");
                out.println("</script>");

            }

            rs.close();
            ps.close();
            con.close();

        }
        catch(Exception e){

            e.printStackTrace();

            out.println("<h2>"+e.getMessage()+"</h2>");

        }

    }

}