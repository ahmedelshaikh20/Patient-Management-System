package com.example.patientservice.repository;

import com.example.patientservice.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

//This tells Spring that this interface is a JPA Repository
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
  boolean existsPatientByEmail(String email);

  Patient getPatientById(UUID id);

  boolean existsPatientByEmailAndIdNot(String email, UUID id);
}
