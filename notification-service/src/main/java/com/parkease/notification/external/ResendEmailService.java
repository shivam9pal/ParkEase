package com.parkease.notification.external;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ResendEmailService {

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from-email}")
    private String fromEmail;

    @Value("${resend.from-name}")
    private String fromName;

    public void sendEmail(String toEmail, String subject, String body) {
        try {
            Resend resend = new Resend(apiKey);

            CreateEmailOptions emailRequest = CreateEmailOptions.builder()
                    .from(fromName + " <" + fromEmail + ">")
                    .to(List.of(toEmail))
                    .subject(subject)
                    .html(buildHtmlEmail(subject, body))
                    .build();

            CreateEmailResponse response = resend.emails().send(emailRequest);
            log.info("Email sent via Resend: id={}, to={}", response.getId(), toEmail);

        } catch (ResendException e) {
            // Log + swallow — email failure must NOT fail the notification DB save
            log.error("Failed to send email via Resend to={}: {}", toEmail, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending email to={}: {}", toEmail, e.getMessage());
        }
    }

    private String buildHtmlEmail(String subject, String body) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background-color: #1a73e8; color: white; padding: 20px; border-radius: 8px 8px 0 0;">
                    <h1 style="margin: 0; font-size: 24px;">🅿️ ParkEase</h1>
                    <p style="margin: 5px 0 0; opacity: 0.9;">Smart Parking Management</p>
                </div>
                <div style="background: #ffffff; border: 1px solid #e0e0e0; padding: 30px; border-radius: 0 0 8px 8px;">
                    <h2 style="color: #1a73e8;">%s</h2>
                    <p style="color: #333; line-height: 1.6;">%s</p>
                    <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;">
                    <p style="color: #888; font-size: 12px;">
                        This is an automated message from ParkEase. Please do not reply.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(subject, body);
    }
}