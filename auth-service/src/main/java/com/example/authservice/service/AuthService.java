package com.example.authservice.service;

import com.example.authservice.dto.LoginRequestDto;
import com.example.authservice.jwt.JwtUtil;
import com.example.authservice.model.User;
import com.example.authservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

  private UserService userService;
  private PasswordEncoder passwordEncoder;
  private JwtUtil jwtUtil;
  public AuthService(UserService userService ,  PasswordEncoder passwordEncoder , JwtUtil jwtUtil) {
    this.userService = userService;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
  }
  public Optional<String> authenticate(LoginRequestDto loginRequestDto) {

    Optional<String> token = userService.findByEmail(loginRequestDto.getEmail())
      .filter(u-> passwordEncoder.matches(loginRequestDto.getPassword(), u.getPassword()))
      .map(u-> jwtUtil.generateToken(u.getEmail() , u.getRole()));
    return token;
  }


    public boolean validateToken(String token) {
    try {
      jwtUtil.validateToken(token);
      return true;
    }catch (Exception e){
      return false;
    }



    }


}
