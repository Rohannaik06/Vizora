import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/VerifyOtpServlet")
public class VerifyOtpServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String email = request.getParameter("email");
        String enteredOtp = request.getParameter("otp");

        if (email == null || enteredOtp == null) {
            out.print("{\"status\":\"error\", \"message\":\"Invalid parameters.\"}");
            return;
        }

        email = email.trim().toLowerCase();
        enteredOtp = enteredOtp.trim();

        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                out.print("{\"status\":\"error\", \"message\":\"Database connection failed.\"}");
                return;
            }

            PreparedStatement ps = conn.prepareStatement("SELECT user_id, full_name, otp, otp_expiry FROM users WHERE LOWER(TRIM(email)) = ?");
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("user_id");
                String dbOtp = rs.getString("otp");
                Timestamp expiryTime = rs.getTimestamp("otp_expiry");
                Timestamp currentTime = new Timestamp(System.currentTimeMillis());

                if (dbOtp == null || expiryTime == null) {
                    out.print("{\"status\":\"error\", \"message\":\"OTP not found. Please request a new OTP.\"}");
                    return;
                }

                if (currentTime.after(expiryTime)) {
                    out.print("{\"status\":\"error\", \"message\":\"OTP expired. Please request a new OTP.\"}");
                    return;
                }

                if (dbOtp.trim().equals(enteredOtp)) {
                    // यशस्वी लॉगिन झाल्यावर user_id द्वारे ओटीपी क्लिअर करणे
                    PreparedStatement clearPs = conn.prepareStatement("UPDATE users SET otp = NULL, otp_expiry = NULL WHERE user_id = ?");
                    clearPs.setInt(1, userId);
                    clearPs.executeUpdate();

                    HttpSession session = request.getSession(true);
                    session.setAttribute("loggedIn", true);
                    session.setAttribute("userEmail", email);
                    session.setAttribute("userName", rs.getString("full_name"));
                    session.setAttribute("userId", userId);

                    String fullName = rs.getString("full_name");

                    out.print("{\"status\":\"success\", \"message\":\"Login successful!\", \"userName\":\"" + fullName + "\", \"userId\":" + userId + "}");
                } else {
                    out.print("{\"status\":\"error\", \"message\":\"Invalid OTP.\"}");
                }
            } else {
                out.print("{\"status\":\"error\", \"message\":\"User not found in database.\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"status\":\"error\", \"message\":\"Server error occurred.\"}");
        }
    }
}