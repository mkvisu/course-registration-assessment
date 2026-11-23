package com.example.enrollment;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import jakarta.validation.ConstraintViolationException;
import java.util.*;
@RestControllerAdvice class ErrorHandler {
 record ApiError(String error){}
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<ApiError> status(ResponseStatusException ex){return ResponseEntity.status(ex.getStatusCode()).body(new ApiError(ex.getReason()));}
 @ExceptionHandler({MethodArgumentNotValidException.class,ConstraintViolationException.class,org.springframework.http.converter.HttpMessageNotReadableException.class}) ResponseEntity<ApiError> invalid(Exception ex){return ResponseEntity.badRequest().body(new ApiError("Invalid request: check required fields, email format and numeric values"));}
}
