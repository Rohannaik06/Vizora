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

@WebServlet("/AddressServlet")
public class AddressServlet extends HttpServlet {

    // ================= FETCH ADDRESS DATA =================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            int userId = Integer.parseInt(request.getParameter("userId"));
            
            try (Connection con = DBConnection.getConnection()) {
                String sql = "SELECT full_name, mobile, pincode, locality, address, city, state FROM addresses WHERE user_id = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String json = String.format(
                                "{\"full_name\":\"%s\", \"mobile\":\"%s\", \"pincode\":\"%s\", \"locality\":\"%s\", \"address\":\"%s\", \"city\":\"%s\", \"state\":\"%s\"}",
                                escapeJson(rs.getString("full_name")),
                                escapeJson(rs.getString("mobile")),
                                escapeJson(rs.getString("pincode")),
                                escapeJson(rs.getString("locality")),
                                escapeJson(rs.getString("address")),
                                escapeJson(rs.getString("city")),
                                escapeJson(rs.getString("state"))
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

    // ================= UPDATE OR INSERT ADDRESS DATA =================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
            
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        try {
            int userId = Integer.parseInt(request.getParameter("userId"));
            String fullName = request.getParameter("fullName");
            String mobile = request.getParameter("mobile");
            String pincode = request.getParameter("pincode");
            String locality = request.getParameter("locality");
            String address = request.getParameter("address");
            String city = request.getParameter("city");
            String state = request.getParameter("state");

            try (Connection con = DBConnection.getConnection()) {
                String checkSql = "SELECT COUNT(*) FROM addresses WHERE user_id = ?";
                try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
                    checkPs.setInt(1, userId);
                    try (ResultSet rs = checkPs.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            // Update existing address
                            String updateSql = "UPDATE addresses SET full_name=?, mobile=?, pincode=?, locality=?, address=?, city=?, state=? WHERE user_id=?";
                            try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                                ps.setString(1, fullName);
                                ps.setString(2, mobile);
                                ps.setString(3, pincode);
                                ps.setString(4, locality);
                                ps.setString(5, address);
                                ps.setString(6, city);
                                ps.setString(7, state);
                                ps.setInt(8, userId);

                                int updated = ps.executeUpdate();
                                out.print(updated > 0 ? "success" : "failed");
                            }
                        } else {
                            // Insert new address if none exists
                            String insertSql = "INSERT INTO addresses (user_id, full_name, mobile, pincode, locality, address, city, state) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                            try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                                ps.setInt(1, userId);
                                ps.setString(2, fullName);
                                ps.setString(3, mobile);
                                ps.setString(4, pincode);
                                ps.setString(5, locality);
                                ps.setString(6, address);
                                ps.setString(7, city);
                                ps.setString(8, state);

                                int inserted = ps.executeUpdate();
                                out.print(inserted > 0 ? "success" : "failed");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("error");
        }
    }

    private String escapeJson(String data) {
        return (data == null) ? "" : data.replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }
}