import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/SendOtpServlet")
public class SendOtpServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        String email = request.getParameter("email");

        if (email == null || email.trim().isEmpty()) {
            out.print("Please enter email");
            return;
        }

        email = email.trim().toLowerCase();

        SecureRandom random = new SecureRandom();
        String otpString = String.valueOf(100000 + random.nextInt(900000));
        Timestamp expiryTime = new Timestamp(System.currentTimeMillis() + (5 * 60 * 1000));

        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                out.print("Database connection failed");
                return;
            }

            // १. ईमेल डेटाबेसमध्ये आधीपासून आहे का तपासा
            PreparedStatement checkStmt = conn.prepareStatement("SELECT user_id FROM users WHERE LOWER(TRIM(email)) = ?");
            checkStmt.setString(1, email);
            ResultSet rs = checkStmt.executeQuery();

            int userId = 0;
            if (rs.next()) {
                // **जुना युजर:** फक्त user_id घ्या (त्याचे नाव बदलण्याची गरज नाही)
                userId = rs.getInt("user_id");
            } else {
                // **नवीन युजर:** ईमेलच्या @ च्या आधीचा भाग त्याचे डीफॉल्ट नाव म्हणून सेट करणे
                String defaultName = email.substring(0, email.indexOf('@'));
                // पहिल्या अक्षराला कॅपिटल करणे
                if (defaultName.length() > 0) {
                    defaultName = defaultName.substring(0, 1).toUpperCase() + defaultName.substring(1);
                }

                PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO users (full_name, email, otp, otp_expiry) VALUES (?, ?, ?, ?)", 
                    PreparedStatement.RETURN_GENERATED_KEYS
                );
                insertStmt.setString(1, defaultName);
                insertStmt.setString(2, email);
                insertStmt.setString(3, otpString);
                insertStmt.setTimestamp(4, expiryTime);
                insertStmt.executeUpdate();

                ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    userId = generatedKeys.getInt(1);
                }
            }

            // २. युजरचा ओटीपी अपडेट करणे
            if (userId > 0) {
                PreparedStatement updateStmt = conn.prepareStatement("UPDATE users SET otp = ?, otp_expiry = ? WHERE user_id = ?");
                updateStmt.setString(1, otpString);
                updateStmt.setTimestamp(2, expiryTime);
                updateStmt.setInt(3, userId);
                updateStmt.executeUpdate();
            }

            // ३. थ्रेडच्या आत वापरण्यासाठी व्हेरिएबल्स 'final' करणे
            final String recipientEmail = email;
            final String finalOtp = otpString;

            // ४. ईमेल पाठवणे बॅकग्राउंड थ्रेडमध्ये (सुपरफास्ट रिस्पॉन्ससाठी)
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Properties properties = new Properties();
                        properties.put("mail.smtp.auth", "true");
                        properties.put("mail.smtp.starttls.enable", "true");
                        properties.put("mail.smtp.host", "smtp.gmail.com");
                        properties.put("mail.smtp.port", "587");

                        Session mailSession = Session.getInstance(properties, new Authenticator() {
                            @Override
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(EmailConfig.EMAIL, EmailConfig.PASSWORD);
                            }
                        });

                        Message message = new MimeMessage(mailSession);
                        message.setFrom(new InternetAddress(EmailConfig.EMAIL));
                        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                        message.setSubject("Vizora Login OTP");
                        message.setText("Your Vizora Login OTP is: " + finalOtp + "\nValid for 5 minutes.");

                        Transport.send(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            // ५. युजरला लगेच इन्स्टंट रिस्पॉन्स पाठवणे
            out.print("OTP sent successfully");

        } catch (Exception e) {
            e.printStackTrace();
            out.print("Server error: " + e.getMessage());
        }
    }
}