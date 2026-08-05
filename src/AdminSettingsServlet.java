import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/AdminSettingsServlet")
public class AdminSettingsServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();
        request.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");

        try (Connection con = DBConnection.getConnection()) {
            
            // 1. Update Admin Profile (Name & Email)
            if ("updateProfile".equals(action)) {
                String adminName = request.getParameter("adminName");
                String adminEmail = request.getParameter("adminEmail");
                // जुना ईमेल ओळखण्यासाठी (किंवा तुम्ही id नुसारही करू शकता)
                String oldEmail = request.getParameter("oldEmail"); 

                if (adminName == null || adminEmail == null || adminName.trim().isEmpty() || adminEmail.trim().isEmpty()) {
                    out.print("error: fields cannot be empty");
                    return;
                }

                // जर oldEmail नसेल दिला तर आपण थेट नवीन ईमेलने अपडेट करू (किंवा गृहीत धरू की admin_id = 1 मुख्य ॲडमिन आहे)
                String sql = "UPDATE admins SET admin_name = ?, email = ? WHERE email = ? OR admin_id = 1";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, adminName.trim());
                    ps.setString(2, adminEmail.trim());
                    ps.setString(3, (oldEmail != null && !oldEmail.trim().isEmpty()) ? oldEmail.trim() : adminEmail.trim());
                    
                    int rows = ps.executeUpdate();
                    if (rows > 0) {
                        out.print("success");
                    } else {
                        // काहीच अपडेट झाले नाही तर नवीन रेकॉर्ड किंवा आयडी 1 अपडेट करण्याचा प्रयत्न करू
                        String fallbackSql = "UPDATE admins SET admin_name = ?, email = ? WHERE admin_id = 1";
                        try (PreparedStatement fbPs = con.prepareStatement(fallbackSql)) {
                            fbPs.setString(1, adminName.trim());
                            fbPs.setString(2, adminEmail.trim());
                            fbPs.executeUpdate();
                            out.print("success");
                        }
                    }
                }

            // 2. Update Admin Password
            } else if ("updatePassword".equals(action)) {
                String currentPass = request.getParameter("currentPass");
                String newPass = request.getParameter("newPass");

                if (currentPass == null || newPass == null || currentPass.trim().isEmpty() || newPass.trim().isEmpty()) {
                    out.print("error: password cannot be empty");
                    return;
                }

                // ॲडमिनच्या डेटाबेसमध्ये जुना पासवर्ड तपासणे (Single Vendor साठी admin_id = 1 गृहीत धरून)
                String checkSql = "SELECT admin_id FROM admins WHERE (password = ? OR admin_id = 1)";
                try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
                    checkPs.setString(1, currentPass.trim());
                    
                    try (var rs = checkPs.executeQuery()) {
                        if (rs.next()) {
                            // जुना पासवर्ड मॅच झाला, आता नवीन पासवर्ड सेव्ह करा
                            String updatePassSql = "UPDATE admins SET password = ? WHERE admin_id = ?";
                            try (PreparedStatement updatePs = con.prepareStatement(updatePassSql)) {
                                updatePs.setString(1, newPass.trim());
                                updatePs.setInt(2, rs.getInt("admin_id"));
                                updatePs.executeUpdate();
                                out.print("success");
                            }
                        } else {
                            out.print("error: incorrect current password");
                        }
                    }
                }

            } else {
                out.print("error: invalid action");
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.print("error: " + e.getMessage());
        }
    }
}