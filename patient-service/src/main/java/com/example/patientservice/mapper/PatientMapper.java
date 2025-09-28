package com.example.patientservice.mapper;

import com.example.patientservice.dto.PatientRequestDto;
import com.example.patientservice.dto.PatientResponseDto;
import com.example.patientservice.model.Patient;

import java.time.LocalDate;

public class PatientMapper {

  public static PatientResponseDto toDto(Patient patient){
    PatientResponseDto patientResponseDto = new PatientResponseDto();
    patientResponseDto.setAddress(patient.getAddress());
    patientResponseDto.setDateOfBirth(patient.getBirthDate().toString());
    patientResponseDto.setEmail(patient.getEmail());
    patientResponseDto.setId(patient.getId().toString());
    patientResponseDto.setName(patient.getName());

    return patientResponseDto;
  }


  public static Patient toModel(PatientRequestDto patientRequestDto){
    Patient patient = new Patient();
    patient.setName(patientRequestDto.getName());
    patient.setAddress(patientRequestDto.getAddress());
    patient.setEmail(patientRequestDto.getEmail());
    patient.setBirthDate(LocalDate.parse(patientRequestDto.getDateOfBirth()));
    patient.setRegistrationDate(LocalDate.parse(patientRequestDto.getRegisteredDate()));
    return patient;

  }


}
