package org.example.collector_service.exception;

import org.springframework.http.HttpStatus;

public class FileUploadException extends CollectorServiceException{
    public FileUploadException(String message){
        super(message, HttpStatus.BAD_REQUEST,"FILE_UPLOAD_ERROR");
    }

    public FileUploadException(String message, Throwable cause){
        super(message, cause, HttpStatus.BAD_REQUEST,"FILE_UPLOAD_ERROR");

    }
}
