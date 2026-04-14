package tn.esprit.tic.civiAgora.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${civox.mail.from:}")
    private String configuredSenderAddress;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean smtpAuth;

    @Value("${civox.mail.sender-name:Civox}")
    private String senderName;

    @Value("${civox.mail.require-smtp:true}")
    private boolean requireSmtp;

    public void sendHtmlMessage(String to, String subject, String htmlContent) throws Exception {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Email recipient is required");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Email subject is required");
        }
        if (htmlContent == null || htmlContent.isBlank()) {
            throw new IllegalArgumentException("Email HTML content is required");
        }

        requireRealSmtpConfiguration();
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());

        String senderAddress = resolveSenderAddress();
        InternetAddress sender = new InternetAddress(senderAddress, resolveSenderName(), StandardCharsets.UTF_8.name());
        InternetAddress recipient = new InternetAddress(to.trim(), true);
        helper.setFrom(sender);
        helper.setReplyTo(sender);
        helper.setTo(recipient);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        log.info("Sending email '{}' to {} through SMTP {}:{}", subject, recipient.getAddress(), mailHost, mailPort);
        mailSender.send(message);
        log.info("Email '{}' delivered to SMTP provider for {}", subject, recipient.getAddress());
    }

    private void requireRealSmtpConfiguration() {
        if (!requireSmtp) {
            return;
        }

        List<String> missing = new ArrayList<>();
        if (isBlank(mailHost)) {
            missing.add("CIVOX_MAIL_HOST / spring.mail.host");
        }
        if (isBlank(resolveSenderAddressOrNull())) {
            missing.add("CIVOX_MAIL_FROM or CIVOX_MAIL_USERNAME");
        }
        if (smtpAuth && isBlank(mailUsername)) {
            missing.add("CIVOX_MAIL_USERNAME / spring.mail.username");
        }
        if (smtpAuth && isBlank(mailPassword)) {
            missing.add("CIVOX_MAIL_PASSWORD / spring.mail.password");
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Real SMTP mail delivery is not configured. Missing: " + String.join(", ", missing)
            );
        }

        if (isLocalSmtpHost(mailHost)) {
            throw new IllegalStateException(
                    "Real SMTP mail delivery is configured to use a local SMTP host (" + mailHost + "). " +
                            "Set CIVOX_MAIL_HOST to your SMTP provider host for real inbox delivery."
            );
        }
    }

    private String resolveSenderAddress() {
        String senderAddress = resolveSenderAddressOrNull();
        if (!isBlank(senderAddress)) {
            return senderAddress;
        }
        throw new IllegalStateException("Mail sender address is not configured");
    }

    private String resolveSenderAddressOrNull() {
        if (!isBlank(configuredSenderAddress)) {
            return configuredSenderAddress.trim();
        }
        if (!isBlank(mailUsername)) {
            return mailUsername.trim();
        }
        return null;
    }

    private String resolveSenderName() {
        return isBlank(senderName) ? "Civox" : senderName.trim();
    }

    private boolean isLocalSmtpHost(String host) {
        if (host == null) {
            return false;
        }

        String normalized = host.trim().toLowerCase();
        return normalized.equals("localhost")
                || normalized.equals("127.0.0.1")
                || normalized.equals("0.0.0.0")
                || normalized.equals("::1");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
