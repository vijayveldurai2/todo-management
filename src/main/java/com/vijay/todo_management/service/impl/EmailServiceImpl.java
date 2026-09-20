package com.vijay.todo_management.service.impl;

import com.vijay.todo_management.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.verification.base-url}")
    private String verificationBaseUrl;

    public EmailServiceImpl(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    @Override
    public void sendVerificationEmail(String toEmail, String rawToken) {
        String link = buildVerificationLink(rawToken);

        if (!mailEnabled) {
            log.info("Mail disabled (app.mail.enabled=false). Verification link for {}: {}", toEmail, link);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Mail enabled but JavaMailSender is not available. Verification link for {}: {}", toEmail, link);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Verify your Todo Management account");
        message.setText(
                "Thanks for signing up.\n\n"
                        + "Open this link to verify your email:\n"
                        + link
                        + "\n\nIf you did not create an account, you can ignore this message."
        );

        mailSender.send(message);
        log.info("Verification email sent to {}", toEmail);
    }

    private String buildVerificationLink(String rawToken) {
        String base = verificationBaseUrl;
        String separator = base.contains("?") ? "&" : "?";
        return base + separator + "token=" + rawToken;
    }
}
