package com.example.patientservice.dto;

import com.example.patientservice.dto.validators.CreatePatientValidationGroup;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PatientRequestDto {

  @NotBlank(message = "Name is Required")
  @Size(min = 1, max = 100, message = "Number of name chars should be between 1 and 100")
  private String name;

  @NotBlank(message = "Email is Required")
  @Email(message = "Email Should be Valid")
  private String email;


  @NotBlank(message = "Date of birth is required")
  private String dateOfBirth;

  @NotBlank(groups = CreatePatientValidationGroup.class, message = "Registered Date is Required")
  private String registeredDate;


  @NotNull(message = "Address shouldnot be empty")
  private String address;


  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getRegisteredDate() {
    return registeredDate;
  }

  public void setRegisteredDate(String registeredDate) {
    this.registeredDate = registeredDate;
  }

  public String getDateOfBirth() {
    return dateOfBirth;
  }

  public void setDateOfBirth(String dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }


}
