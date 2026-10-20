package com.jlpp.modules.auth.service.impl;

import com.jlpp.modules.auth.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@jeps.edu.vn}")
    private String fromEmail;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        if (mailSender == null) {
            log.warn("JavaMailSender is not configured. Simulating email dispatch to: {}", toEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[JEPS] Hướng dẫn đặt lại mật khẩu của bạn");

            String htmlContent = "<div style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #1C2E3D; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #E8E4DA; border-radius: 8px;\">"
                    + "<div style=\"text-align: center; margin-bottom: 20px;\">"
                    + "<h2 style=\"color: #173B57; margin: 0;\">JEPS - Japanese Exam Preparation</h2>"
                    + "</div>"
                    + "<p>Xin chào,</p>"
                    + "<p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản JEPS liên kết với địa chỉ email: <strong>" + toEmail + "</strong>.</p>"
                    + "<p>Vui lòng bấm vào nút bên dưới để tiến hành đặt mật khẩu mới:</p>"
                    + "<div style=\"text-align: center; margin: 30px 0;\">"
                    + "<a href=\"" + resetLink + "\" style=\"background-color: #315C78; color: #FFFFFF; text-decoration: none; padding: 12px 28px; border-radius: 6px; font-weight: bold; display: inline-block;\">Đặt lại mật khẩu</a>"
                    + "</div>"
                    + "<p style=\"font-size: 0.9em; color: #64748B;\">Liên kết này có hiệu lực trong vòng <strong>15 phút</strong> và chỉ sử dụng được 01 lần.</p>"
                    + "<p style=\"font-size: 0.9em; color: #64748B;\">Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này. Tài khoản của bạn vẫn được bảo mật an toàn.</p>"
                    + "<hr style=\"border: none; border-top: 1px solid #E8E4DA; margin: 20px 0;\">"
                    + "<p style=\"font-size: 0.8em; color: #94A3B8; text-align: center;\">JEPS Japanese Learning Platform &copy; 2026</p>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
            // Do not rethrow in a way that exposes SMTP connection errors to external clients
        }
    }
}
