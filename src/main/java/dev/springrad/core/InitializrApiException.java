package dev.springrad.core;

public final class InitializrApiException extends SpringRadException {
    private final int statusCode;
    private final String responseBody;

    public InitializrApiException(int statusCode, String responseBody) {
        super(
                "Initializr API request failed with status " + statusCode,
                toUserMessage(statusCode, responseBody)
        );
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    private static String toUserMessage(int statusCode, String responseBody) {
        if (statusCode == 400) {
            return "Initializr rejected the request: " + responseBody;
        }
        return "Initializr API request failed (" + statusCode + "): " + responseBody;
    }
}
