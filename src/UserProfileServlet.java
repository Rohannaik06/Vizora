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

@WebServlet("/UserProfileServlet")
public class UserProfileServlet extends HttpServlet {

    // ================= FETCH USER DATA (When page loads) =================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            int userId = Integer.parseInt(request.getParameter("userId"));
            
            try (Connection con = DBConnection.getConnection()) {
                // Corrected: using full_name and user_id
                String sql = "SELECT full_name AS name, email, mobile, gender FROM users WHERE user_id = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String name = rs.getString("name");
                            // Split name into first and last name safely
                            String[] nameParts = name != null ? name.split(" ", 2) : new String[]{"", ""};
                            String firstName = nameParts[0];
                            String lastName = nameParts.length > 1 ? nameParts[1] : "";

                            String json = String.format(
                                "{\"firstName\":\"%s\", \"lastName\":\"%s\", \"email\":\"%s\", \"mobile\":\"%s\", \"gender\":\"%s\"}",
                                escapeJson(firstName),
                                escapeJson(lastName),
                                escapeJson(rs.getString("email")),
                                escapeJson(rs.getString("mobile") != null ? rs.getString("mobile") : ""),
                                escapeJson(rs.getString("gender") != null ? rs.getString("gender") : "")
                            );
                            out.print(json);
                        } else {
                            out.print("{}");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("{}");
        }
    }

    // ================= UPDATE USER DATA (When form is saved) =================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
            
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        try {
            int userId = Integer.parseInt(request.getParameter("userId"));
            String fullName = request.getParameter("firstName") + " " + request.getParameter("lastName");
            String email = request.getParameter("email");
            String mobile = request.getParameter("mobile");
            String gender = request.getParameter("gender");

            try (Connection con = DBConnection.getConnection()) {
                // Corrected: using full_name and user_id
                String sql = "UPDATE users SET full_name=?, email=?, mobile=?, gender=? WHERE user_id=?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, fullName);
                    ps.setString(2, email);
                    ps.setString(3, mobile);
                    ps.setString(4, gender);
                    ps.setInt(5, userId);
                    
                    int updated = ps.executeUpdate();
                    if(updated > 0) {
                        out.print("success");
                    } else {
                        out.print("failed");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("error");
        }
    }

    private String escapeJson(String data) {
        return (data == null) ? "" : data.replace("\"", "\\\"");
    }
}