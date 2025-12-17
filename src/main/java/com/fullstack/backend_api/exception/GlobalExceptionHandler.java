package com.fullstack.backend_api.exception;

import com.fullstack.backend_api.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice  // 모든 @Controller의 예외를 처리하는 전역 핸들러
public class GlobalExceptionHandler {

    /**
     * IllegalStateException 처리 (우리가 PostService에서 던진 필수 필드 누락 예외)
     * HTTP Status: 400 Bad Request
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("message", ex.getMessage());
        error.put("errorType", "BadRequest");

        // 400 Bad Request 반환
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * @Valid 유효성 검사 실패 처리 (DTO의 @NotBlank 등이 실패했을 때)
     * HTTP Status: 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });

        Map<String, String> response = new HashMap<>();
        response.put("message", "입력 유효성 검사 실패");
        response.put("details", errors.toString());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * IllegalArgumentException 처리 (PostService에서 데이터 없음 예외)
     * HTTP Status: 404 Not Found
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        error.put("errorType", "NotFound");

        // 💡 404 Not Found 반환
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * SecurityException 처리 (수정/삭제 권한 없음 예외)
     * HTTP Status: 403 Forbidden
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurityException(SecurityException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        error.put("errorType", "Forbidden");

        // 💡 403 Forbidden 반환
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    // 403 Forbidden 처리
    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ErrorResponse> handlePermissionDeniedException(PermissionDeniedException e) {

        HttpStatus status = HttpStatus.FORBIDDEN;

        // 사용자 정의 DTO의 생성자 사용
        ErrorResponse errorResponse = new ErrorResponse(
                e.getMessage(),
                status.getReasonPhrase()
        );

        return new ResponseEntity<>(errorResponse, status); // 👈 403 Forbidden
    }

     // (선택) 404 Not Found 처리 (ResourceNotFoundException)
     @ExceptionHandler(ResourceNotFoundException.class)
     public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException e) {
         ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), "Forbidden");
         return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND); // 404 반환
     }

    // ... (다른 예외 처리 로직)

}
