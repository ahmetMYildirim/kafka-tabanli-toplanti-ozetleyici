import org.example.collector_service.exception.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Unit Tests")
public class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/media/upload");
    }

    @Nested
    @DisplayName("CollectorServiceException handling")
    class CollectorServiceExceptionTests {

        @Test
        @DisplayName("Should handle InvalidFileException")
        void handleCollectorServiceException_InvalidFile_ShouldReturnUnsupportedMediaType() {
            InvalidFileException exception = new InvalidFileException("File is empty");

            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCollectorServiceException(exception, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("File is empty");
            assertThat(response.getBody().getPath()).isEqualTo("/api/v1/media/upload");
        }

        @Test
        @DisplayName("Should handle FileSizeExceededException")
        void handleCollectorServiceException_FileSize_ShouldReturnPayloadTooLarge() {
            FileSizeExceededException exception = new FileSizeExceededException("File size exceeds 500MB");

            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCollectorServiceException(exception, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
            assertThat(response.getBody().getMessage()).contains("500MB");
        }

        @Test
        @DisplayName("Should handle MediaAssetNotFoundException")
        void handleCollectorServiceException_NotFound_ShouldReturnNotFound() {
            MediaAssetNotFoundException exception = new MediaAssetNotFoundException("Media asset not found: xyz123");

            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCollectorServiceException(exception, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().getMessage()).contains("xyz123");
        }

        @Test
        @DisplayName("Should handle DuplicateFileException")
        void handleCollectorServiceException_Duplicate_ShouldReturnConflict() {
            DuplicateFileException exception = new DuplicateFileException("File already exists");

            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCollectorServiceException(exception, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().getMessage()).isEqualTo("File already exists");
        }

        @Test
        @DisplayName("Should handle StorageException")
        void handleCollectorServiceException_Storage_ShouldReturnInternalError() {
            StorageException exception = new StorageException("Failed to store file", new RuntimeException());

            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCollectorServiceException(exception, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("Validation exception handling")
    class ValidationExceptionTests {

        @Test
        @DisplayName("Should handle MethodArgumentNotValidException with field errors")
        void handleValidationException_WithFieldErrors_ShouldReturnBadRequest() {
            MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);

            FieldError fieldError1 = new FieldError("request", "meetingId", "Meeting ID is required");
            FieldError fieldError2 = new FieldError("request", "platform", "Platform is required");

            when(exception.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationException(exception, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrorCode()).isEqualTo("VALIDATION_ERROR");
            assertThat(response.getBody().getValidationErrors()).hasSize(2);
            assertThat(response.getBody().getValidationErrors().get(0).getField()).isEqualTo("meetingId");
        }

        @Test
        @DisplayName("Should handle validation exception with empty field errors")
        void handleValidationException_EmptyErrors_ShouldReturnBadRequest() {
            MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);

            when(exception.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(List.of());

            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationException(exception, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getValidationErrors()).isEmpty();
        }
    }

    @Nested
    @DisplayName("MaxUploadSizeExceededException handling")
    class MaxUploadSizeTests {

        @Test
        @DisplayName("Should handle max upload size exceeded")
        void handleMaxUploadSizeException_ShouldReturnPayloadTooLarge() {
            MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(500 * 1024 * 1024);

            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMaxUploadSizeException(exception, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
            assertThat(response.getBody().getErrorCode()).isEqualTo("FILE_SIZE_EXCEEDED");
        }
    }

    @Nested
    @DisplayName("IllegalArgumentException handling")
    class IllegalArgumentTests {

        @Test
        @DisplayName("Should handle illegal argument exception")
        void handleIllegalArgumentException_ShouldReturnBadRequest() {
            IllegalArgumentException exception = new IllegalArgumentException("Invalid parameter value");

            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIllegalArgumentException(exception, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getErrorCode()).isEqualTo("ILLEGAL_ARGUMENT");
            assertThat(response.getBody().getMessage()).isEqualTo("Invalid parameter value");
        }
    }

    @Nested
    @DisplayName("Generic exception handling")
    class GenericExceptionTests {

        @Test
        @DisplayName("Should handle generic exception")
        void handleGenericException_ShouldReturnInternalServerError() {
            Exception exception = new RuntimeException("Unexpected error occurred");

            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGenericException(exception, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().getErrorCode()).isEqualTo("INTERNAL_SERVER_ERROR");
            assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        }

        @Test
        @DisplayName("Should not expose internal details in generic error")
        void handleGenericException_ShouldNotExposeDetails() {
            Exception exception = new NullPointerException("Sensitive internal details");

            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGenericException(exception, request);

            assertThat(response.getBody().getMessage()).doesNotContain("Sensitive");
            assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        }
    }
}

