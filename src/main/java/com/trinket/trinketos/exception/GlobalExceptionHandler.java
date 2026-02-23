package com.trinket.trinketos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach((error) -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });
    return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put("error", "Bad Request");
    errorResponse.put("message", ex.getMessage());
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, String>> handleAccessDeniedException(AccessDeniedException ex) {
    return new ResponseEntity<>(Map.of("error", "Access denied: " + ex.getMessage()), HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<Map<String, String>> handleBadCredentialsException(BadCredentialsException ex) {
    return new ResponseEntity<>(Map.of("error", "Invalid email or password"), HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {
    // Check for AI Quota Exceeded (Gemini)
    if (ex.getMessage() != null && (ex.getMessage().contains("Quota exceeded")
        || ex.getMessage().contains("Too Many Requests")
        || ex.getMessage().contains("429"))) {
      return new ResponseEntity<>(
          Map.of("error", "Cota de IA excedida (Free Tier). Tente novamente em alguns minutos."),
          HttpStatus.TOO_MANY_REQUESTS);
    }

    return new ResponseEntity<>(Map.of("error", ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler({ org.springframework.dao.DataIntegrityViolationException.class,
      org.hibernate.exception.ConstraintViolationException.class })
  public ResponseEntity<Map<String, String>> handleDataIntegrityViolationException(Exception ex) {
    String message = "Database error: " + ex.getMessage();
    String detail = ex.getMessage();

    // Sometimes the detail is deeper
    if (ex.getCause() != null) {
      detail += " " + ex.getCause().getMessage();
      if (ex.getCause().getCause() != null) {
        detail += " " + ex.getCause().getCause().getMessage();
      }
    }

    if (detail.contains("Key (slug)")) {
      message = "Este identificador (slug) já está em uso por outra organização.";
    } else if (detail.contains("Key (email)")) {
      message = "Este email já está cadastrado.";
    }

    return new ResponseEntity<>(Map.of("error", message), HttpStatus.CONFLICT);
  }
}
