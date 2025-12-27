package org.example.collector_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CollectorServiceException extends RuntimeException{
    private final HttpStatus status;
    private final String errorCode;

    public CollectorServiceException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public CollectorServiceException(String message, Throwable cause, HttpStatus status, String errorCode) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }
}
