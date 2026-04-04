package com.parkease.notification.external;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TwilioSmsService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromPhone;

    @Value("${twilio.enabled:true}")
    private boolean enabled;

    @PostConstruct
    public void init() {
        if (enabled) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio SMS service initialized. From: {}", fromPhone);
        } else {
            log.warn("Twilio SMS is DISABLED (twilio.enabled=false). SMS will be logged only.");
        }
    }

    public void sendSms(String toPhone, String body) {
        if (!enabled) {
            log.info("[SMS STUB] To: {} | Body: {}", toPhone, body);
            return;
        }

        if (toPhone == null || toPhone.isBlank()) {
            log.warn("Skipping SMS — recipient phone is null or blank");
            return;
        }

        try {
            Message message = Message.creator(
                    new PhoneNumber(toPhone),    // E.164 format: +91XXXXXXXXXX
                    new PhoneNumber(fromPhone),
                    body
            ).create();

            log.info("SMS sent via Twilio: sid={}, to={}", message.getSid(), toPhone);

        } catch (ApiException e) {
            // Log + swallow — SMS failure must NOT fail the notification DB save
            log.error("Failed to send SMS via Twilio to={}: {}", toPhone, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending SMS to={}: {}", toPhone, e.getMessage());
        }
    }
}