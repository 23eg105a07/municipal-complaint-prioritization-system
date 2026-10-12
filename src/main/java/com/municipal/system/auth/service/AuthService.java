package com.municipal.system.auth.service;

import com.municipal.system.auth.dto.AuthResponse;
import com.municipal.system.auth.dto.LoginRequest;
import com.municipal.system.auth.dto.RegisterRequest;
import com.municipal.system.config.AppProperties;
import com.municipal.system.exception.ConflictException;
import com.municipal.system.security.JwtTokenProvider;
import com.municipal.system.security.UserPrincipal;
import com.municipal.system.user.dto.UserResponse;
import com.municipal.system.user.entity.Role;
import com.municipal.system.user.entity.User;
import com.municipal.system.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final AppProperties appProperties;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email is already registered: " + request.email());
        }

        User user = User.builder()
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .phone(request.phone() != null ? request.phone().trim() : null)
                .role(Role.CITIZEN)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new citizen with id: {}", savedUser.getId());

        UserPrincipal principal = UserPrincipal.create(savedUser);
        String token = tokenProvider.generateToken(principal);
        long expiresInSeconds = appProperties.security().jwt().expirationMinutes() * 60;

        return AuthResponse.of(token, expiresInSeconds, UserResponse.fromEntity(savedUser));
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = tokenProvider.generateToken(principal);
        long expiresInSeconds = appProperties.security().jwt().expirationMinutes() * 60;

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));

        return AuthResponse.of(token, expiresInSeconds, UserResponse.fromEntity(user));
    }
}
