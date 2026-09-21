package com.example.bookingsystem.service;

import com.example.bookingsystem.dto.LoginRequest;
import com.example.bookingsystem.dto.LoginResponse;
import com.example.bookingsystem.entity.User;
import com.example.bookingsystem.exception.ResourceNotFoundException;
import com.example.bookingsystem.repository.UserRepository;
import com.example.bookingsystem.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UserDetails principal = (UserDetails) authentication.getPrincipal();

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUsername()));

        String token = jwtUtil.generateToken(principal, Map.of("role", user.getRole().name()));

        return LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .username(user.getUsername())
                .role(user.getRole().name())
                .expiresInMs(jwtUtil.getExpirationMs())
                .build();
    }
}
