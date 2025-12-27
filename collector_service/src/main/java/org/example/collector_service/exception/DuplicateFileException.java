package org.example.collector_service.exception;

import org.springframework.http.HttpStatus;

public class DuplicateFileException extends CollectorServiceException{
    public DuplicateFileException(String message) {
        super(message, HttpStatus.CONFLICT, "DUPLICATE_FILE");
    }
}
