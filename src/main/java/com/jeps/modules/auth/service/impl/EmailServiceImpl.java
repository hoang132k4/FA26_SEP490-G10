package com.jeps.modules.auth.service.impl;

import com.jeps.modules.auth.service.EmailService;
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

    @Value("${spring.mail.password:}")
    private String configuredPassword;

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

    @Override
    public void sendEmailChangeVerificationEmail(String newEmail, String fullName, String verificationLink) {
        log.info("================================================================================");
        log.info("📧 [EMAIL VERIFICATION] Link generated for new email: {}", newEmail);
        log.info("👉 URL: {}", verificationLink);
        log.info("================================================================================");

        if (mailSender == null || configuredUsername == null || configuredUsername.trim().isEmpty()
                || configuredPassword == null || configuredPassword.trim().isEmpty()) {
            log.warn("JavaMailSender / SMTP credentials not configured (SPRING_MAIL_USERNAME or SPRING_MAIL_PASSWORD is empty). Running in local simulation mode. Email verification link is printed above.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String sender = configuredUsername.trim();
            String displayName = (fullName != null && !fullName.isBlank()) ? fullName.trim() : "bạn";
            helper.setFrom(sender, "JEPS Support");
            helper.setTo(newEmail);
            helper.setSubject("[JEPS] Xác nhận thay đổi địa chỉ email của bạn");

            String htmlContent = "<div style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #1C2E3D; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #E8E4DA; border-radius: 8px;\">"
                    + "<div style=\"text-align: center; margin-bottom: 20px;\">"
                    + "<h2 style=\"color: #173B57; margin: 0;\">JEPS - Japanese Exam Preparation</h2>"
                    + "</div>"
                    + "<p>Xin chào <strong>" + displayName + "</strong>,</p>"
                    + "<p>Chúng tôi nhận được yêu cầu cập nhật địa chỉ email tài khoản JEPS sang địa chỉ mới: <strong>" + newEmail + "</strong>.</p>"
                    + "<p>Vui lòng bấm vào nút bên dưới để xác nhận địa chỉ email mới này:</p>"
                    + "<div style=\"text-align: center; margin: 25px 0 15px 0;\">"
                    + "<a href=\"" + verificationLink + "\" style=\"background-color: #315C78; color: #FFFFFF; text-decoration: none; padding: 12px 28px; border-radius: 6px; font-weight: bold; display: inline-block;\">Xác nhận địa chỉ Email mới</a>"
                    + "</div>"
                    + "<p style=\"font-size: 0.88em; color: #475569; margin-bottom: 6px;\">Hoặc sao chép liên kết bên dưới và dán vào thanh địa chỉ trình duyệt:</p>"
                    + "<div style=\"background-color: #F1F5F9; padding: 10px 14px; border-radius: 6px; word-break: break-all; font-family: monospace; font-size: 0.85em; margin-bottom: 20px;\">"
                    + "<a href=\"" + verificationLink + "\" style=\"color: #2563EB; text-decoration: underline;\">" + verificationLink + "</a>"
                    + "</div>"
                    + "<p style=\"font-size: 0.82em; color: #B45309; background-color: #FEF3C7; padding: 8px 12px; border-radius: 6px; margin-bottom: 20px;\">"
                    + "💡 <strong>Mẹo:</strong> Nếu thư nằm trong mục <em>Thư rác (Spam)</em>, hãy đánh dấu <strong>\"Không phải thư rác\"</strong> hoặc dán liên kết trên trực tiếp vào trình duyệt."
                    + "</p>"
                    + "<p style=\"font-size: 0.9em; color: #64748B;\">Liên kết này có hiệu lực trong vòng <strong>15 phút</strong> và chỉ sử dụng được 01 lần.</p>"
                    + "<p style=\"font-size: 0.9em; color: #64748B;\">Nếu bạn không yêu cầu thay đổi email, vui lòng bỏ qua email này. Địa chỉ email hiện tại của tài khoản sẽ không bị thay đổi.</p>"
                    + "<hr style=\"border: none; border-top: 1px solid #E8E4DA; margin: 20px 0;\">"
                    + "<p style=\"font-size: 0.8em; color: #94A3B8; text-align: center;\">JEPS Japanese Learning Platform &copy; 2026</p>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email change verification email sent successfully via SMTP to: {}", newEmail);
        } catch (Exception e) {
            log.error("Failed to dispatch email change verification email to {}: {}", newEmail, e.getMessage(), e);
        }
    }

    @Override
    public void sendRegistrationVerificationEmail(String toEmail, String fullName, String verificationLink) {
        log.info("================================================================================");
        log.info("📧 [REGISTER VERIFICATION] Link generated for new account: {}", toEmail);
        log.info("👉 URL: {}", verificationLink);
        log.info("================================================================================");

        if (mailSender == null || configuredUsername == null || configuredUsername.trim().isEmpty()
                || configuredPassword == null || configuredPassword.trim().isEmpty()) {
            log.warn("JavaMailSender / SMTP credentials not configured (SPRING_MAIL_USERNAME or SPRING_MAIL_PASSWORD is empty). Running in local simulation mode. Registration verification link is printed above.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String sender = configuredUsername.trim();
            String displayName = (fullName != null && !fullName.isBlank()) ? fullName.trim() : "bạn";
            helper.setFrom(sender, "JEPS Support");
            helper.setTo(toEmail);
            helper.setSubject("[JEPS] Xác nhận địa chỉ email để kích hoạt tài khoản");

            String htmlContent = "<div style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #1C2E3D; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #E8E4DA; border-radius: 8px;\">"
                    + "<div style=\"text-align: center; margin-bottom: 20px;\">"
                    + "<h2 style=\"color: #173B57; margin: 0;\">JEPS - Japanese Exam Preparation</h2>"
                    + "</div>"
                    + "<p>Xin chào <strong>" + displayName + "</strong>,</p>"
                    + "<p>Cảm ơn bạn đã đăng ký tài khoản tại <strong>JEPS</strong> với địa chỉ email: <strong>" + toEmail + "</strong>.</p>"
                    + "<p>Vui lòng bấm vào nút bên dưới để xác nhận email và kích hoạt tài khoản của bạn:</p>"
                    + "<div style=\"text-align: center; margin: 25px 0 15px 0;\">"
                    + "<a href=\"" + verificationLink + "\" style=\"background-color: #315C78; color: #FFFFFF; text-decoration: none; padding: 12px 28px; border-radius: 6px; font-weight: bold; display: inline-block;\">Xác nhận Email & Kích hoạt tài khoản</a>"
                    + "</div>"
                    + "<p style=\"font-size: 0.88em; color: #475569; margin-bottom: 6px;\">Hoặc sao chép liên kết bên dưới và dán vào thanh địa chỉ trình duyệt:</p>"
                    + "<div style=\"background-color: #F1F5F9; padding: 10px 14px; border-radius: 6px; word-break: break-all; font-family: monospace; font-size: 0.85em; margin-bottom: 20px;\">"
                    + "<a href=\"" + verificationLink + "\" style=\"color: #2563EB; text-decoration: underline;\">" + verificationLink + "</a>"
                    + "</div>"
                    + "<p style=\"font-size: 0.82em; color: #B45309; background-color: #FEF3C7; padding: 8px 12px; border-radius: 6px; margin-bottom: 20px;\">"
                    + "💡 <strong>Mẹo:</strong> Nếu thư nằm trong mục <em>Thư rác (Spam)</em>, hãy đánh dấu <strong>\"Không phải thư rác\"</strong> hoặc dán liên kết trên trực tiếp vào trình duyệt."
                    + "</p>"
                    + "<p style=\"font-size: 0.9em; color: #64748B;\">Liên kết này có hiệu lực trong vòng <strong>15 phút</strong> và chỉ sử dụng được 01 lần.</p>"
                    + "<p style=\"font-size: 0.9em; color: #64748B;\">Nếu bạn không thực hiện đăng ký tài khoản này, vui lòng bỏ qua email này.</p>"
                    + "<hr style=\"border: none; border-top: 1px solid #E8E4DA; margin: 20px 0;\">"
                    + "<p style=\"font-size: 0.8em; color: #94A3B8; text-align: center;\">JEPS Japanese Learning Platform &copy; 2026</p>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Registration verification email sent successfully via SMTP to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to dispatch registration verification email to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}
