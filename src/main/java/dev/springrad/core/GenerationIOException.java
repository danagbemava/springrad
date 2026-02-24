package dev.springrad.core;

public final class GenerationIOException extends SpringRadException {
    public GenerationIOException(String message, Throwable cause) {
        super(message, "Failed to write generated project files. Check disk permissions and free space.", cause);
    }

    public GenerationIOException(String message, String userMessage, Throwable cause) {
        super(message, userMessage, cause);
    }
}
