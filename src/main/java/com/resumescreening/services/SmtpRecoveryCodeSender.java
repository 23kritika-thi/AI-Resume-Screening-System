package com.resumescreening.services;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Properties;

/**
 * Sends recovery codes over authenticated SMTP with required STARTTLS.
 * All connection and credential values are read from environment variables.
 */
public class SmtpRecoveryCodeSender implements RecoveryCodeSender {

    public static final String CONFIGURATION_MESSAGE =
        "Password recovery email is not configured. Set SMTP_HOST, SMTP_PORT, "
            + "SMTP_USERNAME, SMTP_PASSWORD, and SMTP_FROM environment variables.";

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String from;

    public SmtpRecoveryCodeSender() {
        this(System.getenv());
    }

    SmtpRecoveryCodeSender(Map<String, String> environment) {
        host = value(environment, "SMTP_HOST");
        username = value(environment, "SMTP_USERNAME");
        password = value(environment, "SMTP_PASSWORD");
        from = value(environment, "SMTP_FROM");
        int configuredPort = -1;
        try {
            configuredPort = Integer.parseInt(value(environment, "SMTP_PORT"));
        } catch (NumberFormatException ignored) {
            // Report the same actionable configuration error as other missing values.
        }
        port = configuredPort;
    }

    @Override
    public boolean isConfigured() {
        return !host.isBlank()
            && port > 0
            && port <= 65535
            && !username.isBlank()
            && !password.isBlank()
            && isValidAddress(from);
    }

    @Override
    public void validateConfiguration() throws EmailConfigurationException {
        if (!isConfigured()) {
            throw new EmailConfigurationException(CONFIGURATION_MESSAGE);
        }
        Transport transport = null;
        try {
            transport = createSession().getTransport("smtp");
            transport.connect(host, port, username, password);
            transport.close();
        } catch (MessagingException e) {
            if (transport != null && transport.isConnected()) {
                try {
                    transport.close();
                } catch (MessagingException closeError) {
                    e.addSuppressed(closeError);
                }
            }
            throw new EmailConfigurationException(
                "SMTP is configured but could not be reached or authenticated. Verify SMTP_HOST, "
                    + "SMTP_PORT, SMTP_USERNAME, and SMTP_PASSWORD.",
                e
            );
        }
    }

    @Override
    public void sendVerificationCode(String email, String code)
            throws EmailConfigurationException, EmailDeliveryException {
        if (!isConfigured()) {
            throw new EmailConfigurationException(CONFIGURATION_MESSAGE);
        }

        Session session = createSession();
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email, true));
            message.setSubject("ResumeAI password recovery", "UTF-8");
            message.setText(
                "Your ResumeAI password recovery code is: " + code
                    + "\n\nThis code expires in 10 minutes and can only be used once. "
                    + "If you did not request this, you can ignore this email.",
                "UTF-8"
            );
            Transport.send(message);
        } catch (MessagingException e) {
            throw new EmailDeliveryException("The recovery email could not be delivered.", e);
        }
    }

    private Session createSession() {
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.port", Integer.toString(port));
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.starttls.enable", "true");
        properties.setProperty("mail.smtp.starttls.required", "true");
        properties.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        properties.setProperty("mail.smtp.connectiontimeout", "10000");
        properties.setProperty("mail.smtp.timeout", "10000");
        properties.setProperty("mail.smtp.writetimeout", "10000");
        return Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    private static String value(Map<String, String> environment, String name) {
        String value = environment.get(name);
        return value == null ? "" : value.trim();
    }

    private static boolean isValidAddress(String address) {
        if (address.isBlank()) {
            return false;
        }
        try {
            InternetAddress parsed = new InternetAddress(address, true);
            parsed.validate();
            return true;
        } catch (AddressException e) {
            return false;
        }
    }
}
