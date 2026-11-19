package com.subtrack.service.impl;

import com.subtrack.service.EmailService;
import com.subtrack.service.RenewalReminderEmailItem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.mail.internet.MimeMessage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SendGridEmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Override
    public void sendVerificationEmail(String toEmail, String token) {
        if (!StringUtils.hasText(fromAddress)) {
            throw new IllegalStateException("MAIL_FROM must be set to a verified sender email address");
        }
        String verificationUrl = frontendUrl.replaceAll("/+$", "") + "/verify-email?token=" + token;

        String text = "Hello,\n\nPlease use the following link to verify your email and activate your account:\n"
                + verificationUrl + "\n\nThis link expires in 24 hours.";
        String html = emailTemplate(
                "Verify your SubTrack account",
                "One quick step to get started",
                "Please verify your email address to activate your SubTrack account.",
                "Verify email",
                verificationUrl,
                "This link expires in 24 hours."
        );

        sendHtmlEmail(toEmail, "Verify your SubTrack account", text, html);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String token) {
        if (!StringUtils.hasText(fromAddress)) {
            throw new IllegalStateException("MAIL_FROM must be set to a verified sender email address");
        }
        String resetUrl = frontendUrl.replaceAll("/+$", "") + "/reset-password?token=" + token;

        String text = "Hello,\n\nUse the following link to reset your SubTrack password:\n"
                + resetUrl + "\n\nThis link expires in 1 hour.";
        String html = emailTemplate(
                "Reset your SubTrack password",
                "Reset your password",
                "We received a request to reset the password for your SubTrack account.",
                "Reset password",
                resetUrl,
                "This link expires in 1 hour. If you did not request this, you can ignore this email."
        );

        sendHtmlEmail(toEmail, "Reset your SubTrack password", text, html);
    }

    @Override
    public void sendRenewalDigestEmail(String toEmail, List<RenewalReminderEmailItem> renewals) {
        if (renewals.isEmpty()) {
            return;
        }
        if (!StringUtils.hasText(fromAddress)) {
            throw new IllegalStateException("MAIL_FROM must be set to a verified sender email address");
        }
        StringBuilder text = new StringBuilder("Hello,\n\nThese subscriptions are coming up for renewal:\n");
        StringBuilder htmlItems = new StringBuilder("<ul>");
        for (RenewalReminderEmailItem renewal : renewals) {
            String safeName = renewal.subscriptionName().replaceAll("[\\r\\n]", " ").trim();
            String amount = renewal.price().toPlainString() + " " + renewal.currency();
            text.append("\n- ").append(safeName).append(" — ").append(amount)
                    .append(" on ").append(renewal.billingDate());
            htmlItems.append("<li><strong>").append(htmlEscape(safeName)).append("</strong> — ")
                    .append(htmlEscape(amount)).append(" on ").append(renewal.billingDate()).append("</li>");
        }
        htmlItems.append("</ul>");
        String html = emailTemplate(
                "Upcoming subscription renewals",
                renewals.size() == 1 ? "A payment is coming up" : "Your upcoming payments",
                htmlItems.toString(),
                "Open SubTrack",
                frontendUrl.replaceAll("/+$", "") + "/account",
                "You received this digest because these subscriptions are active in your SubTrack account."
        );
        sendHtmlEmail(toEmail, "Your upcoming subscription renewals", text.toString(), html);
    }

    private void sendHtmlEmail(String toEmail, String subject, String text, String html) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(text, html);
            mailSender.send(mimeMessage);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not send email", exception);
        }
    }

    private String emailTemplate(
            String title,
            String heading,
            String message,
            String buttonText,
            String buttonUrl,
            String footer) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                  <body style="margin:0;padding:32px 12px;background:#f4f7f6;font-family:Arial,sans-serif;color:#1d2925;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #e3e9e6;">
                      <tr>
                        <td style="padding:28px 36px;background:#173f35;">
                          <div style="color:#ffffff;font-size:22px;font-weight:700;">subtrack</div>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:36px;">
                          <div style="font-size:12px;color:#6d8179;letter-spacing:1.5px;text-transform:uppercase;">SUBTRACK</div>
                          <h1 style="margin:10px 0 14px;font-size:26px;line-height:34px;color:#173f35;">%s</h1>
                          <p style="margin:0 0 26px;font-size:16px;line-height:24px;color:#4a5d56;">%s</p>
                          <a href="%s" style="display:inline-block;padding:14px 22px;border-radius:8px;background:#2f6e5d;color:#ffffff;text-decoration:none;font-size:15px;font-weight:700;">%s</a>
                          <p style="margin:28px 0 0;font-size:13px;line-height:20px;color:#7a8983;">%s</p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:18px 36px;border-top:1px solid #edf1ef;color:#899790;font-size:12px;text-align:center;">%s</td>
                      </tr>
                    </table>
                  </body>
                </html>
                """.formatted(title, heading, buttonUrl, buttonText, message, footer);
    }

    private String htmlEscape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}