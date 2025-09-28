package com.example.patientservice.service;

import com.example.patientservice.PatientServiceApplication;
import com.example.patientservice.dto.PatientRequestDto;
import com.example.patientservice.dto.PatientResponseDto;
import com.example.patientservice.exception.EmailAlreadyExistException;
import com.example.patientservice.exception.PatientNotFoundException;
import com.example.patientservice.grpc.BillingServiceGrpcClient;
import com.example.patientservice.kafka.KafkaProducer;
import com.example.patientservice.mapper.PatientMapper;
import com.example.patientservice.model.Patient;
import com.example.patientservice.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {
  private static final Logger log = LoggerFactory.getLogger(PatientService.class);
  private PatientRepository patientRepository;
  private BillingServiceGrpcClient billingServiceGrpcClient;
  private KafkaProducer kafkaProducer;

  public PatientService(PatientRepository patientRepository, BillingServiceGrpcClient billingServiceGrpcClient, KafkaProducer kafkaProducer) {
    this.patientRepository = patientRepository;
    this.billingServiceGrpcClient = billingServiceGrpcClient;
    this.kafkaProducer = kafkaProducer;
  }


  public List<PatientResponseDto> getPatients() {
    List<Patient> patients = patientRepository.findAll();
    return patients.stream().map(PatientMapper::toDto).toList();
  }

  public PatientResponseDto createPatient(PatientRequestDto patientRequestDto) {
    if (patientRepository.existsPatientByEmail(patientRequestDto.getEmail())) {
      throw new EmailAlreadyExistException("Email Already Exist");
    }
    Patient newPatient = patientRepository.save(PatientMapper.toModel(patientRequestDto));
    billingServiceGrpcClient.createBillingAccount(newPatient.getId().toString(), newPatient.getName(), newPatient.getEmail());
    kafkaProducer.sendMessage(newPatient);
    return PatientMapper.toDto(newPatient);

  }


  public PatientResponseDto updatePatient(UUID id, PatientRequestDto patientRequestDto) {

    Patient patient = patientRepository.getPatientById(id);
    if (patient == null) {
      throw new PatientNotFoundException("Patient Not found with id: ", id);
    }

    if (patientRepository.existsPatientByEmailAndIdNot(patientRequestDto.getEmail(), id)) {
      throw new EmailAlreadyExistException("Email Already Exist");
    }

    patient.setName(patientRequestDto.getName());
    patient.setEmail(patientRequestDto.getEmail());
    patient.setAddress(patientRequestDto.getAddress());
    patient.setBirthDate(LocalDate.parse(patientRequestDto.getDateOfBirth()));

    patientRepository.save(patient);
    return PatientMapper.toDto(patient);

  }


  public void deletePatient(UUID id) {
    patientRepository.deleteById(id);
  }


}
