package com.example.authservice.controller;

import com.example.authservice.dto.LoginRequestDto;
import com.example.authservice.dto.LoginResponseDto;
import com.example.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto login) {


    Optional<String> tokenOptional = authService.authenticate(login);
    if (tokenOptional.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    return ResponseEntity.ok(new LoginResponseDto(tokenOptional.get()));
  }


  @Operation(summary = "Validate Token")
  @GetMapping("/validate")
  public ResponseEntity<Void> validateToken(
    @RequestHeader("Authorization") String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    return authService.validateToken(authHeader.substring(7))
      ? ResponseEntity.ok().build()
      : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }


}
