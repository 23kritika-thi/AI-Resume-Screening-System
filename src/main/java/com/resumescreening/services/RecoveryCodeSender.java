package com.resumescreening.services;

/**
 * Delivers one-time password recovery codes without persisting the code.
 */
public interface RecoveryCodeSender {

    boolean isConfigured();

    default void validateConfiguration() throws EmailConfigurationException {
        if (!isConfigured()) {
            throw new EmailConfigurationException(SmtpRecoveryCodeSender.CONFIGURATION_MESSAGE);
        }
    }

    void sendVerificationCode(String email, String code)
            throws EmailConfigurationException, EmailDeliveryException;

    class EmailConfigurationException extends Exception {
        public EmailConfigurationException(String message) {
            super(message);
        }

        public EmailConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    class EmailDeliveryException extends Exception {
        public EmailDeliveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
