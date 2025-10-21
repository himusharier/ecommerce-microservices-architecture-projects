package com.himusharier.auth.advice;

import com.himusharier.auth.util.ApiResponse;
import com.himusharier.auth.exception.RefreshTokenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApplicationExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleAllExceptions(Exception exception) {
        ApiResponse<String> response = new ApiResponse<>(
                false,
                exception.getMessage()
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<String>> handleRuntimeExceptions(Exception exception) {
        ApiResponse<String> response = new ApiResponse<>(
                false,
                exception.getMessage()
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(RefreshTokenException.class)
    public ResponseEntity<ApiResponse<String>> handleRefreshTokenException(RefreshTokenException exception) {
        ApiResponse<String> response = new ApiResponse<>(
                false,
                exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleValidationErrors(MethodArgumentNotValidException exception){
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String errorMessage = (fieldError != null) ? fieldError.getDefaultMessage() : "Invalid request.";

        ApiResponse<String> response = new ApiResponse<>(
                false,
                errorMessage
        );
        return ResponseEntity.badRequest().body(response);
    }

}

