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

    @Value("${spring.mail.username:}")
    private String configuredUsername;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        log.info("================================================================================");
        log.info("🔑 [PASSWORD RESET] Link generated for email: {}", toEmail);
        log.info("👉 URL: {}", resetLink);
        log.info("================================================================================");

        // If real SMTP credentials are not configured, run in simulation mode
        if (mailSender == null || configuredUsername == null || configuredUsername.trim().isEmpty()) {
            log.warn("JavaMailSender / SMTP credentials not configured (SPRING_MAIL_USERNAME is empty). Running in local simulation mode. Password reset link is printed above.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String sender = configuredUsername.trim();
            helper.setFrom(sender, "JEPS Support");
            helper.setTo(toEmail);
            helper.setSubject("[JEPS] Hướng dẫn đặt lại mật khẩu của bạn");

            String htmlContent = "<div style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #1C2E3D; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #E8E4DA; border-radius: 8px;\">"
                    + "<div style=\"text-align: center; margin-bottom: 20px;\">"
                    + "<h2 style=\"color: #173B57; margin: 0;\">JEPS - Japanese Exam Preparation</h2>"
                    + "</div>"
                    + "<p>Xin chào,</p>"
                    + "<p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản JEPS liên kết với địa chỉ email: <strong>" + toEmail + "</strong>.</p>"
                    + "<p>Vui lòng bấm vào nút bên dưới để tiến hành đặt mật khẩu mới:</p>"
                    + "<div style=\"text-align: center; margin: 25px 0 15px 0;\">"
                    + "<a href=\"" + resetLink + "\" style=\"background-color: #315C78; color: #FFFFFF; text-decoration: none; padding: 12px 28px; border-radius: 6px; font-weight: bold; display: inline-block;\">Đặt lại mật khẩu</a>"
                    + "</div>"
                    + "<p style=\"font-size: 0.88em; color: #475569; margin-bottom: 6px;\">Hoặc sao chép liên kết bên dưới và dán vào thanh địa chỉ Chrome:</p>"
                    + "<div style=\"background-color: #F1F5F9; padding: 10px 14px; border-radius: 6px; word-break: break-all; font-family: monospace; font-size: 0.85em; margin-bottom: 20px;\">"
                    + "<a href=\"" + resetLink + "\" style=\"color: #2563EB; text-decoration: underline;\">" + resetLink + "</a>"
                    + "</div>"
                    + "<p style=\"font-size: 0.82em; color: #B45309; background-color: #FEF3C7; padding: 8px 12px; border-radius: 6px; margin-bottom: 20px;\">"
                    + "💡 <strong>Mẹo:</strong> Nếu thư nằm trong mục <em>Thư rác (Spam)</em>, Google sẽ tạm khóa nút bấm. Bạn hãy bấm nút <strong>\"Báo cáo không phải thư rác\"</strong> ở đầu thư để mở khóa, hoặc dán liên kết trên vào Chrome."
                    + "</p>"
                    + "<p style=\"font-size: 0.9em; color: #64748B;\">Liên kết này có hiệu lực trong vòng <strong>15 phút</strong> và chỉ sử dụng được 01 lần.</p>"
                    + "<p style=\"font-size: 0.9em; color: #64748B;\">Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này. Tài khoản của bạn vẫn được bảo mật an toàn.</p>"
                    + "<hr style=\"border: none; border-top: 1px solid #E8E4DA; margin: 20px 0;\">"
                    + "<p style=\"font-size: 0.8em; color: #94A3B8; text-align: center;\">JEPS Japanese Learning Platform &copy; 2026</p>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Password reset email sent successfully via SMTP to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to dispatch password reset email to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}
