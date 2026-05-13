package com.parkease.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.parkease.auth.exception.EmailServiceException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from-email}")
    private String fromEmail;

    @Value("${resend.from-name}")
    private String fromName;

    // ── Public send method ─────────────────────────────────────────────────────
    public void sendOtpEmail(String toEmail, String otpCode, boolean isRegistration) {
        String subject = isRegistration
                ? "Verify your ParkEase account"
                : "Reset your ParkEase password";

        String htmlBody = buildOtpEmailHtml(otpCode, isRegistration);

        send(toEmail, subject, htmlBody);
    }

    public void sendUserDeactivationEmail(String toEmail, String fullName) {
        String subject = "Account Deactivation Confirmation";
        String htmlBody = buildDeactivationEmailHtml(fullName);
        send(toEmail, subject, htmlBody);
    }

    public void sendUserReactivationEmail(String toEmail, String fullName) {
        String subject = "Your ParkEase Account Is Active Again";
        String htmlBody = buildReactivationEmailHtml(fullName);
        send(toEmail, subject, htmlBody);
    }

    // ── Core Resend dispatch ───────────────────────────────────────────────────
    // A failed email is a hard failure here — unlike notification-service,
    // the user CANNOT proceed without receiving the OTP.
    private void send(String toEmail, String subject, String htmlBody) {
        try {
            Resend resend = new Resend(apiKey);

            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(fromName + " <" + fromEmail + ">")
                    .to(toEmail)
                    .subject(subject)
                    .html(htmlBody)
                    .build();

            resend.emails().send(options);
            log.info("OTP email sent successfully to: {}", toEmail);

        } catch (ResendException e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown email service error";

            // ── Parse SMTP error codes from Resend exception ────────────────────────
            EmailErrorType errorType = parseEmailError(errorMessage, toEmail);

            // ── Log specific error details for debugging ───────────────────────────
            log.error("Failed to send OTP email to {}: [{}] {}",
                    toEmail, errorType.getErrorCode(), errorMessage);

            // ── Throw EmailServiceException with specific error code ───────────────
            throw new EmailServiceException(
                    errorType.getUserMessage() + " (to: " + maskEmail(toEmail) + ")",
                    e
            );
        }
    }

    // ── Helper: Determine email error type from exception message ────────────────
    private EmailErrorType parseEmailError(String errorMessage, String toEmail) {
        if (errorMessage == null) {
            return EmailErrorType.UNKNOWN;
        }

        String lowerMessage = errorMessage.toLowerCase();

        // ── Permanent bounce errors (550-553 range) ──────────────────────────────
        if (errorMessage.contains("550") || lowerMessage.contains("mailbox unavailable")) {
            log.warn("Permanent bounce: Mailbox unavailable for {}", toEmail);
            return EmailErrorType.BOUNCE_MAILBOX_UNAVAILABLE;
        }

        if (errorMessage.contains("551") || lowerMessage.contains("user not local")) {
            log.warn("Permanent bounce: User not local for {}", toEmail);
            return EmailErrorType.BOUNCE_USER_NOT_LOCAL;
        }

        if (errorMessage.contains("552") || lowerMessage.contains("exceeded storage")) {
            log.warn("Permanent bounce: Exceeded storage for {}", toEmail);
            return EmailErrorType.BOUNCE_EXCEEDED_STORAGE;
        }

        if (errorMessage.contains("553") || lowerMessage.contains("mailbox name not allowed")) {
            log.warn("Permanent bounce: Invalid mailbox name for {}", toEmail);
            return EmailErrorType.BOUNCE_INVALID_MAILBOX;
        }

        // ── Temporary errors (4xx codes) ───────────────────────────────────────────
        if (errorMessage.contains("450") || errorMessage.contains("451")) {
            log.warn("Temporary email error for {}: {}", toEmail, errorMessage);
            return EmailErrorType.TEMPORARY_ERROR;
        }

        // ── Invalid email format ──────────────────────────────────────────────────
        if (lowerMessage.contains("invalid") && lowerMessage.contains("email")) {
            log.warn("Invalid email format: {}", maskEmail(toEmail));
            return EmailErrorType.INVALID_EMAIL_FORMAT;
        }

        // ── Rate limiting/quota exceeded ──────────────────────────────────────────
        if (errorMessage.contains("429") || lowerMessage.contains("rate limit")
                || lowerMessage.contains("quota")) {
            log.warn("Rate limit/quota exceeded for email service");
            return EmailErrorType.RATE_LIMIT_EXCEEDED;
        }

        // ── Default: Unknown error ────────────────────────────────────────────────
        log.error("Unclassified email error for {}: {}", toEmail, errorMessage);
        return EmailErrorType.UNKNOWN;
    }

    // ── Helper: Mask email for logging ────────────────────────────────────────
    private String maskEmail(String email) {
        if (email == null || email.length() < 5) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***@" + email.substring(atIndex + 1);
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    // ── Enum: Email error types with user-friendly messages ──────────────────────
    private enum EmailErrorType {
        BOUNCE_MAILBOX_UNAVAILABLE(
                "EMAIL_BOUNCE_MAILBOX_UNAVAILABLE",
                "This email address does not exist or is no longer active. Please verify your email."),
        BOUNCE_USER_NOT_LOCAL(
                "EMAIL_BOUNCE_USER_NOT_FOUND",
                "This email address was not found. Please double-check and try again."),
        BOUNCE_EXCEEDED_STORAGE(
                "EMAIL_BOUNCE_STORAGE_FULL",
                "The recipient's mailbox is full. Please ask them to clear space and try again."),
        BOUNCE_INVALID_MAILBOX(
                "EMAIL_BOUNCE_INVALID",
                "This email address is invalid. Please verify and try again."),
        INVALID_EMAIL_FORMAT(
                "EMAIL_INVALID_FORMAT",
                "The email address format is invalid. Please enter a valid email."),
        TEMPORARY_ERROR(
                "EMAIL_TEMPORARY_ERROR",
                "Email service is temporarily unavailable. Please try again in a few moments."),
        RATE_LIMIT_EXCEEDED(
                "EMAIL_RATE_LIMIT",
                "Too many email requests. Please try again later."),
        UNKNOWN(
                "EMAIL_SERVICE_ERROR",
                "Failed to send email. Please try again.");

        private final String errorCode;
        private final String userMessage;

        EmailErrorType(String errorCode, String userMessage) {
            this.errorCode = errorCode;
            this.userMessage = userMessage;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getUserMessage() {
            return userMessage;
        }
    }

    // ── HTML Template ──────────────────────────────────────────────────────────
    private String buildOtpEmailHtml(String otpCode, boolean isRegistration) {
        String heading = isRegistration
                ? "Verify Your Email"
                : "Reset Your Password";

        String subtext = isRegistration
                ? "Use the OTP below to complete your ParkEase registration."
                : "Use the OTP below to reset your ParkEase account password.";

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
            </head>

            <body style="margin:0; padding:0; background-color:#f4f6f8; font-family: Arial, sans-serif;">

              <table width="100%%" cellpadding="0" cellspacing="0" style="padding: 20px 0;">
                <tr>
                  <td align="center">

                    <!-- Main Card -->
                    <table width="100%%" cellpadding="0" cellspacing="0"
                           style="max-width: 500px; background:#ffffff; border-radius:12px;
                                  padding:32px; box-shadow:0 4px 12px rgba(0,0,0,0.08);">

                      <!-- Header -->
                      <tr>
                        <td align="center" style="padding-bottom: 20px;">
                          <h2 style="margin:0; color:#111827; font-size:22px;">
                            %s
                          </h2>
                        </td>
                      </tr>

                      <!-- Subtext -->
                      <tr>
                        <td align="center">
                          <p style="margin:0; color:#6b7280; font-size:14px; line-height:1.5;">
                            %s
                          </p>
                        </td>
                      </tr>

                      <!-- OTP Box -->
                      <tr>
                        <td align="center" style="padding:30px 0;">
                          <div style="display:inline-block;
                                      font-size:32px;
                                      font-weight:bold;
                                      letter-spacing:8px;
                                      color:#2563eb;
                                      background:linear-gradient(135deg,#eff6ff,#dbeafe);
                                      padding:16px 28px;
                                      border-radius:10px;">
                            %s
                          </div>
                        </td>
                      </tr>

                      <!-- Info -->
                      <tr>
                        <td align="center">
                          <p style="margin:0; font-size:13px; color:#4b5563;">
                            This OTP is valid for <strong>10 minutes</strong>.<br/>
                            Do not share it with anyone.
                          </p>
                        </td>
                      </tr>

                      <!-- Divider -->
                      <tr>
                        <td style="padding:25px 0;">
                          <hr style="border:none; border-top:1px solid #e5e7eb;"/>
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td align="center">
                          <p style="margin:0; font-size:12px; color:#9ca3af; line-height:1.6;">
                            If you didn’t request this, you can safely ignore this email.<br/>
                            <strong>— ParkEase Team</strong>
                          </p>
                        </td>
                      </tr>

                    </table>

                  </td>
                </tr>
              </table>

            </body>
            </html>
            """.formatted(heading, subtext, otpCode);
    }

    // ── User Account Status Email Templates ────────────────────────────────────
    private String buildDeactivationEmailHtml(String fullName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
            </head>

            <body style="margin:0; padding:0; background-color:#f4f6f8; font-family: Arial, sans-serif;">

              <table width="100%%" cellpadding="0" cellspacing="0" style="padding: 20px 0;">
                <tr>
                  <td align="center">

                    <!-- Main Card -->
                    <table width="100%%" cellpadding="0" cellspacing="0"
                           style="max-width: 500px; background:#ffffff; border-radius:12px;
                                  padding:32px; box-shadow:0 4px 12px rgba(0,0,0,0.08);">

                      <!-- Header -->
                      <tr>
                        <td align="center" style="padding-bottom: 20px;">
                          <h2 style="margin:0; color:#111827; font-size:22px;">
                            Account Deactivated
                          </h2>
                        </td>
                      </tr>

                      <!-- Name greeting -->
                      <tr>
                        <td align="center">
                          <p style="margin:0; color:#6b7280; font-size:14px; line-height:1.5;">
                            Hi %s,<br/>
                            Your ParkEase account has been successfully deactivated.
                          </p>
                        </td>
                      </tr>

                      <!-- Info -->
                      <tr>
                        <td align="center" style="padding:25px 0;">
                          <p style="margin:0; font-size:13px; color:#4b5563; line-height:1.6;">
                            You will no longer have access to your parking reservations and account features.<br/>
                            If you change your mind, you can reactivate your account at any time.
                          </p>
                        </td>
                      </tr>

                      <!-- Divider -->
                      <tr>
                        <td style="padding:25px 0;">
                          <hr style="border:none; border-top:1px solid #e5e7eb;"/>
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td align="center">
                          <p style="margin:0; font-size:12px; color:#9ca3af; line-height:1.6;">
                            We're sorry to see you go.<br/>
                            <strong>— ParkEase Team</strong>
                          </p>
                        </td>
                      </tr>

                    </table>

                  </td>
                </tr>
              </table>

            </body>
            </html>
            """.formatted(fullName);
    }

    private String buildReactivationEmailHtml(String fullName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
            </head>

            <body style="margin:0; padding:0; background-color:#f4f6f8; font-family: Arial, sans-serif;">

              <table width="100%%" cellpadding="0" cellspacing="0" style="padding: 20px 0;">
                <tr>
                  <td align="center">

                    <!-- Main Card -->
                    <table width="100%%" cellpadding="0" cellspacing="0"
                           style="max-width: 500px; background:#ffffff; border-radius:12px;
                                  padding:32px; box-shadow:0 4px 12px rgba(0,0,0,0.08);">

                      <!-- Header -->
                      <tr>
                        <td align="center" style="padding-bottom: 20px;">
                          <h2 style="margin:0; color:#111827; font-size:22px;">
                            Welcome Back!
                          </h2>
                        </td>
                      </tr>

                      <!-- Name greeting -->
                      <tr>
                        <td align="center">
                          <p style="margin:0; color:#6b7280; font-size:14px; line-height:1.5;">
                            Hi %s,<br/>
                            Your ParkEase account has been successfully reactivated.
                          </p>
                        </td>
                      </tr>

                      <!-- Info -->
                      <tr>
                        <td align="center" style="padding:25px 0;">
                          <p style="margin:0; font-size:13px; color:#4b5563; line-height:1.6;">
                            You now have full access to your parking reservations and account features.<br/>
                            Start booking your parking spots today!
                          </p>
                        </td>
                      </tr>

                      <!-- Divider -->
                      <tr>
                        <td style="padding:25px 0;">
                          <hr style="border:none; border-top:1px solid #e5e7eb;"/>
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td align="center">
                          <p style="margin:0; font-size:12px; color:#9ca3af; line-height:1.6;">
                            Glad to have you back!<br/>
                            <strong>— ParkEase Team</strong>
                          </p>
                        </td>
                      </tr>

                    </table>

                  </td>
                </tr>
              </table>

            </body>
            </html>
            """.formatted(fullName);
    }
}
