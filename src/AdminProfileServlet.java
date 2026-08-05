import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/AdminProfileServlet")
public class AdminProfileServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String adminName = "Super Admin";
        String adminEmail = "admin@vizora.com";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT admin_name, email FROM admins ORDER BY admin_id ASC LIMIT 1")) {

            if (rs.next()) {
                adminName = rs.getString("admin_name");
                adminEmail = rs.getString("email");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // JSON फॉरमॅटमध्ये माहिती पाठवणे
        String json = String.format("{\"adminName\":\"%s\", \"adminEmail\":\"%s\"}", 
            adminName != null ? adminName : "Super Admin", 
            adminEmail != null ? adminEmail : "admin@vizora.com"
        );

        out.print(json);
    }
}