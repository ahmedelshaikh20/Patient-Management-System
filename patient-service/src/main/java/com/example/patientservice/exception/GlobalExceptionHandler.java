package com.example.patientservice.exception;


import com.sun.jdi.event.StepEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {

    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getFieldErrors().forEach((error) -> {
      errors.put(error.getField(), error.getDefaultMessage());
    });

    return ResponseEntity.badRequest().body(errors);
  }

  @ExceptionHandler(EmailAlreadyExistException.class)
  public ResponseEntity<Map<String, String>> handleConstraintViolationException(EmailAlreadyExistException ex) {

    Map<String , String> errors = new HashMap<>();
    errors.put("Error", "Email Already Exist");
    return ResponseEntity.badRequest().body(errors);
  }
  @ExceptionHandler(PatientNotFoundException.class)
  public ResponseEntity<Map<String, String>> handleConstraintPatientNotFoundException(PatientNotFoundException ex) {

    Map<String , String> errors = new HashMap<>();
    errors.put("Error", "Email Already Exist");
    return ResponseEntity.badRequest().body(errors);
  }

}
