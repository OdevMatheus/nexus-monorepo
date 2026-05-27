package com.matheushenrique.nexum.services.impl;

import com.matheushenrique.nexum.dtos.request.*;
import com.matheushenrique.nexum.dtos.response.*;
import com.matheushenrique.nexum.entities.User;
import com.matheushenrique.nexum.repositories.UserRepository;
import com.matheushenrique.nexum.security.EmailService;
import com.matheushenrique.nexum.security.JwtService;
import com.matheushenrique.nexum.security.exceptions.EmailAlreadyInUseException;
import com.matheushenrique.nexum.security.exceptions.EmailNotVerifiedException;
import com.matheushenrique.nexum.security.exceptions.InvalidCredentialsException;
import com.matheushenrique.nexum.security.exceptions.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Transactional
    public MessageResponse register(RegisterRequest request) {
        var existingUser = userRepository.findByEmail(request.email());

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.isEmailVerified()) {
                throw new EmailAlreadyInUseException("Email already registered");
            }

            String newToken = UUID.randomUUID().toString();
            user.setEmailVerificationToken(newToken);
            user.setEmailTokenExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
            userRepository.save(user);
            emailService.sendVerificationEmail(user.getEmail(), user.getName(), newToken);

            return new MessageResponse("A new verification email has been sent. Check your inbox.");
        }

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .emailVerified(false)
                .emailVerificationToken(verificationToken)
                .emailTokenExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();

        userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), user.getName(), verificationToken);

        return new MessageResponse("Registration successful. Check your email to verify your account.");
    }

    @Transactional
    public MessageResponse verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired verification token"));

        if (Instant.now().isAfter(user.getEmailTokenExpiresAt())) {
            throw new InvalidTokenException("Verification token has expired");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailTokenExpiresAt(null);
        userRepository.save(user);

        return new MessageResponse("Email verified successfully. You can now log in.");
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Please verify your email before logging in");
        }

        String accessToken  = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        userRepository.save(user);

        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                accessToken,
                refreshToken
        );
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String token = request.refreshToken();

        if (!jwtService.isTokenValid(token)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        UUID userId = jwtService.extractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("User not found"));

        if (!token.equals(user.getRefreshToken()) ||
                Instant.now().isAfter(user.getRefreshTokenExpiresAt())) {
            throw new InvalidTokenException("Refresh token expired or already used");
        }

        String newAccessToken  = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        user.setRefreshToken(newRefreshToken);
        user.setRefreshTokenExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        userRepository.save(user);

        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                newAccessToken,
                newRefreshToken
        );
    }

    @Transactional
    public MessageResponse logout(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRefreshToken(null);
        user.setRefreshTokenExpiresAt(null);
        userRepository.save(user);

        return new MessageResponse("Logged out successfully");
    }
}