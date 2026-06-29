package com.leadflow.leadflow_backend.rest;

import com.leadflow.leadflow_backend.dto.PasswordResetRequest;
import com.leadflow.leadflow_backend.dto.ProfileResponse;
import com.leadflow.leadflow_backend.dto.ProfileUpdateRequest;
import com.leadflow.leadflow_backend.model.User;
import com.leadflow.leadflow_backend.repos.UserRepository;
import com.leadflow.leadflow_backend.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private User extractUser(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        // Fallback to query repository if principal is not a User object
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(Authentication authentication) {
        User user = extractUser(authentication);
        ProfileResponse response = ProfileResponse.builder()
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .build();
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            Authentication authentication) {
        User currentUser = extractUser(authentication);

        // Check if new email is already taken by another user
        if (!currentUser.getEmail().equalsIgnoreCase(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Email is already in use by another account"));
        }

        // Check if new phone is already taken by another user
        if (!currentUser.getPhone().equals(request.getPhone()) &&
                userRepository.existsByPhone(request.getPhone())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Phone number is already in use by another account"));
        }

        boolean emailChanged = !currentUser.getEmail().equalsIgnoreCase(request.getEmail());

        currentUser.setName(request.getName());
        currentUser.setEmail(request.getEmail());
        currentUser.setPhone(request.getPhone());

        userRepository.save(currentUser);
        log.info("Profile updated for user: {}", currentUser.getEmail());

        String newToken = null;
        if (emailChanged) {
            newToken = jwtUtil.generateToken(currentUser.getEmail());
            log.info("Email updated. Issued new JWT token for: {}", currentUser.getEmail());
        }

        ProfileResponse response = ProfileResponse.builder()
                .name(currentUser.getName())
                .email(currentUser.getEmail())
                .phone(currentUser.getPhone())
                .token(newToken)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody PasswordResetRequest request,
            Authentication authentication) {
        User currentUser = extractUser(authentication);

        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Incorrect current password"));
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);
        log.info("Password successfully reset for user: {}", currentUser.getEmail());

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }
}
