package dev.springrad.core;

public class SpringRadException extends RuntimeException {
    private final String userMessage;

    public SpringRadException(String message, String userMessage) {
        super(message);
        this.userMessage = userMessage;
    }

    public SpringRadException(String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.userMessage = userMessage;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
