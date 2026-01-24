package Vaultproject.Vaultapp.Services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // send email verification
    public void sendVerificationEmail(String toEmail, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper (message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify Your Email - Vaultapp");
            
            String verificationUrl = baseUrl + "/vaultauth-api-v1/verify-email?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

            String htmlContent = String.format("""
                <html>
                    <body style="font-family: Arial, sans-serif;">
                        <h2>Welcome to Vaultapp!</h2>
                        <p>Thank you for registering. Please verify your email address by clicking the button below:</p>
                        <div style="margin: 30px 0;">
                            <a href="%s" 
                               style="background-color: #4CAF50; 
                                      color: white; 
                                      padding: 14px 20px; 
                                      text-decoration: none; 
                                      border-radius: 4px;
                                      display: inline-block;">
                                Verify Email
                            </a>
                        </div>
                        <p>Or copy and paste this link into your browser:</p>
                        <p style="color: #666;">%s</p>
                        <p style="color: #999; font-size: 12px; margin-top: 30px;">
                            This link will expire in 24 hours. If you didn't create an account, please ignore this email.
                        </p>
                    </body>
                </html>
                """, verificationUrl, verificationUrl);

                helper.setText(htmlContent, true);
                mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper (message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Password Reset Request - Vaultapp");
            
            String passwordVerificationUrl = baseUrl + "/vault-api-v1/password-reset?token=" + URLEncoder.encode(resetToken, StandardCharsets.UTF_8);

            String htmlContent = String.format("""
                <html>
                    <body style="font-family: Arial, sans-serif;">
                        <h2>Password Reset Request</h2>
                        <p>We received a request to reset your password. Click the button below to reset it:</p>
                        <div style="margin: 30px 0;">
                            <a href="%s" 
                               style="background-color: #f44336; 
                                      color: white; 
                                      padding: 14px 20px; 
                                      text-decoration: none; 
                                      border-radius: 4px;
                                      display: inline-block;">
                                Reset Password
                            </a>
                        </div>
                        <p>If you did not request a password reset, please ignore this email.</p>
                    </body>
                </html>
                    """, passwordVerificationUrl);

                helper.setText(htmlContent, true);
                mailSender.send(message);
            } catch (MessagingException e) {
             throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}