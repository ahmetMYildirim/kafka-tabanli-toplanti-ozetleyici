package org.example.ai_service.exception;

/**
 * AiApiException - OpenAI API çağrılarında oluşan hataları temsil eden exception
 * 
 * Whisper veya GPT API'larından dönen hatalar bu exception ile wrap edilir.
 * 
 * @author Ahmet
 * @version 1.0
 */
public class AiApiException extends RuntimeException {

    public AiApiException(String message) {
        super(message);
    }

    public AiApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
