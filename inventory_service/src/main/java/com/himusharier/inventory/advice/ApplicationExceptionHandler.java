package com.himusharier.inventory.advice;

import com.himusharier.inventory.exception.ProductSubmissionException;
import com.himusharier.inventory.exception.ResourceNotFoundException;
import com.himusharier.inventory.util.ApiResponse;
import org.springframework.http.ResponseEntity;
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
        return ResponseEntity.status(500).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleValidationErrors(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String errorMessage = (fieldError != null) ? fieldError.getDefaultMessage() : "Invalid request.";

        ApiResponse<String> response = new ApiResponse<>(
                false,
                errorMessage
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleNotFound(ResourceNotFoundException exception) {
        ApiResponse<String> response = new ApiResponse<>(
                false,
                exception.getMessage()
        );
        return ResponseEntity.status(404).body(response);
    }

    @ExceptionHandler(ProductSubmissionException.class)
    public ResponseEntity<ApiResponse<String>> handleProductSubmissionError(ProductSubmissionException exception) {
        ApiResponse<String> response = new ApiResponse<>(
                false,
                exception.getMessage()
        );
        return ResponseEntity.status(400).body(response);
    }

}
