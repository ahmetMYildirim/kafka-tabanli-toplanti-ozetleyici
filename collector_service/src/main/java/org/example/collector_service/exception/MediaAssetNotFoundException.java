package org.example.collector_service.exception;

import org.springframework.http.HttpStatus;

public class MediaAssetNotFoundException extends CollectorServiceException{
    public MediaAssetNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "MEDIA_ASSET_NOT_FOUND");
    }
}
