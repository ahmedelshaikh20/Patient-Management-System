package com.example.patientservice.exception;

import java.util.UUID;

public class PatientNotFoundException extends RuntimeException {
  public PatientNotFoundException(String message , UUID patientId) {
    super(message + patientId.toString());
  }
}
