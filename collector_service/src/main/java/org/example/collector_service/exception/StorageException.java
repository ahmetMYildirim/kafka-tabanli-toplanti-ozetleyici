package org.example.collector_service.exception;

import org.springframework.http.HttpStatus;

public class StorageException extends CollectorServiceException{
    public StorageException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR");
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause, HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR");
    }
}
