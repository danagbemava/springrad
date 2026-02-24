package dev.springrad.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringRadExceptionTest {

    @Test
    void initializrTimeoutExceptionHasUserMessage() {
        InitializrTimeoutException exception = new InitializrTimeoutException("timeout", new RuntimeException("cause"));
        assertEquals("Could not reach start.spring.io. Check your network connection.", exception.getUserMessage());
    }

    @Test
    void initializrApiExceptionWith400FormatsUserMessage() {
        InitializrApiException exception = new InitializrApiException(400, "Unknown dependency id 'x'");
        assertTrue(exception.getUserMessage().contains("Initializr rejected the request: Unknown dependency id 'x'"));
    }
}
