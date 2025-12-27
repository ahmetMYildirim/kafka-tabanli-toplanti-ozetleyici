package org.example.collector_service.exception;

import org.springframework.http.HttpStatus;

public class OutboxEventPublisherException extends CollectorServiceException{
    public OutboxEventPublisherException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "OUTBOX_EVENT_PUBLISHER_ERROR");
    }

    public OutboxEventPublisherException(String message, Throwable cause) {
        super(message, cause, HttpStatus.INTERNAL_SERVER_ERROR, "OUTBOX_EVENT_PUBLISHER_ERROR");
    }
}
