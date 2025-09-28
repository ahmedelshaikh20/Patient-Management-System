package com.example.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequestDto {
  @NotBlank(message = "Email is Required")
  @Email(message = "Email should be valid email address")
  private String email;
  @NotBlank(message = "Message is Required")
  @Size(min = 8 , message = "Password Should be at least 8 characters long")
  private String password;

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}

