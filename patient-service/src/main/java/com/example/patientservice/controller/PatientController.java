package com.example.patientservice.controller;

import com.example.patientservice.dto.PatientRequestDto;
import com.example.patientservice.dto.PatientResponseDto;
import com.example.patientservice.dto.validators.CreatePatientValidationGroup;
import com.example.patientservice.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients")
@Tag(name = "Patient" , description = "Description for our PatientController")
public class PatientController {

  private static final Logger log = LoggerFactory.getLogger(PatientController.class);
  private final PatientService patientService;

  public PatientController(PatientService patientService) {
    this.patientService = patientService;
  }


  @GetMapping
  @Operation(summary = "Get All Patients")
  public ResponseEntity<List<PatientResponseDto>> patients() {

    return ResponseEntity.ok().body(patientService.getPatients());

  }

  @PostMapping
  @Operation(summary = "Create a new Patient")
  public ResponseEntity<PatientResponseDto> createPatient(@Validated({Default.class, CreatePatientValidationGroup.class}) @RequestBody PatientRequestDto patientRequestDto) {
    return ResponseEntity.ok().body(patientService.createPatient(patientRequestDto));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update already existing Patient")
  public ResponseEntity<PatientResponseDto> updatePatient(@PathVariable UUID id, @Validated({Default.class}) @RequestBody PatientRequestDto patientRequestDto) {

    return ResponseEntity.ok().body(patientService.updatePatient(id, patientRequestDto));

  }


  @DeleteMapping("/{id}")
  @Operation(summary = "Delete Patient by ID")
  public ResponseEntity<Void> deletePatient(@PathVariable UUID id) {
    patientService.deletePatient(id);
    return ResponseEntity.noContent().build();
  }


}
