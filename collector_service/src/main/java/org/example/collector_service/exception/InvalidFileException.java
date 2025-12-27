package org.example.collector_service.exception;

import org.springframework.http.HttpStatus;

public class InvalidFileException extends CollectorServiceException{
    public InvalidFileException(String message){
        super(message, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "INVALID_FILE_TYPE");
    }
}
