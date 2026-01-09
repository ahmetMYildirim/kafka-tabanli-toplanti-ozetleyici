package org.example.ai_service.exception;

public class TaskExtractionException extends RuntimeException {

    public TaskExtractionException(String message) {
        super(message);
    }

    public TaskExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
