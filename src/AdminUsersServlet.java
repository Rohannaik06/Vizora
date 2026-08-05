import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/AdminUsersServlet")
public class AdminUsersServlet extends HttpServlet {

    // सर्व युझर्सची यादी फेच करण्यासाठी (GET)
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("[");

        // तुमच्या users टेबलमधील अचूक कॉलम्स् (user_id, full_name, email, mobile)
        String sql = "SELECT user_id, full_name, email, mobile FROM users ORDER BY user_id DESC";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            boolean first = true;
            while (rs.next()) {
                if (!first) {
                    jsonBuilder.append(",");
                }
                first = false;

                jsonBuilder.append("{");
                jsonBuilder.append("\"userId\":").append(rs.getInt("user_id")).append(",");
                jsonBuilder.append("\"fullName\":\"").append(escapeJson(rs.getString("full_name"))).append("\",");
                jsonBuilder.append("\"email\":\"").append(escapeJson(rs.getString("email"))).append("\",");
                jsonBuilder.append("\"mobile\":\"").append(escapeJson(rs.getString("mobile"))).append("\"");
                jsonBuilder.append("}");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        jsonBuilder.append("]");
        out.print(jsonBuilder.toString());
    }

    // युझर डिलीट करण्यासाठी (POST)
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String userIdStr = request.getParameter("userId");

        if (userIdStr != null) {
            int userId = Integer.parseInt(userIdStr);

            try (Connection con = DBConnection.getConnection()) {
                String deleteSql = "DELETE FROM users WHERE user_id = ?";
                try (PreparedStatement ps = con.prepareStatement(deleteSql)) {
                    ps.setInt(1, userId);
                    int rows = ps.executeUpdate();

                    if (rows > 0) {
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

    private String escapeJson(String data) {
        if (data == null) return "";
        return data.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}