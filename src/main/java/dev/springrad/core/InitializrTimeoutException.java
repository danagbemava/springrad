package dev.springrad.core;

public final class InitializrTimeoutException extends SpringRadException {
    public InitializrTimeoutException(String message, Throwable cause) {
        super(message, "Could not reach start.spring.io. Check your network connection.", cause);
    }
}
