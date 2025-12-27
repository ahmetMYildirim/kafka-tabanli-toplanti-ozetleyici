package org.example.collector_service.exception;

import org.springframework.http.HttpStatus;

public class FileSizeExceededException extends CollectorServiceException{
    public FileSizeExceededException(String message) {
        super(message, HttpStatus.PAYLOAD_TOO_LARGE, "FILE_SIZE_EXCEEDED");
    }
}
