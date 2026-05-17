package com.skybooker.auth.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Random;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.skybooker.auth.config.RabbitMQConfig;
import com.skybooker.auth.dto.AuthResponse;
import com.skybooker.auth.dto.ForgotPasswordRequest;
import com.skybooker.auth.dto.GoogleAuthRequest;
import com.skybooker.auth.dto.LoginRequest;
import com.skybooker.auth.dto.ProfileResponse;
import com.skybooker.auth.dto.RegisterRequest;
import com.skybooker.auth.dto.ResetPasswordRequest;
import com.skybooker.auth.dto.UpdateProfileRequest;
import com.skybooker.auth.dto.VerifyResetOtpRequest;
import com.skybooker.auth.entity.User;
import com.skybooker.auth.event.NotificationEvent;
import com.skybooker.auth.repository.UserRepository;
import com.skybooker.auth.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.admin.secret-key}")
    private String adminSecretKey;

    @Value("${app.staff.secret-key}")
    private String staffSecretKey;

    @Value("${google.client-id}")
    private String googleClientId;

    @Override
    public AuthResponse register(RegisterRequest request) {
        log.info("Register attempt — email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        String requestedRole = request.getRole() != null
                ? request.getRole().toUpperCase().trim()
                : "PASSENGER";

        String role;

        switch (requestedRole) {
            case "ADMIN" -> {
                if (request.getAdminSecretKey() == null || request.getAdminSecretKey().isBlank())
                    throw new RuntimeException("Admin registration requires the admin secret key.");
                if (!request.getAdminSecretKey().equals(adminSecretKey))
                    throw new RuntimeException("Invalid admin secret key. Access denied.");
                if (userRepository.countByRole("ADMIN") >= 4)
                    throw new RuntimeException("Maximum 4 admin accounts already exist.");
                role = "ADMIN";
            }
            case "AIRLINE_STAFF" -> {
                if (request.getStaffSecretKey() == null || request.getStaffSecretKey().isBlank())
                    throw new RuntimeException("Staff registration requires the staff secret key.");
                if (!request.getStaffSecretKey().equals(staffSecretKey))
                    throw new RuntimeException("Invalid staff secret key.");
                role = "AIRLINE_STAFF";
            }
            case "PASSENGER" -> role = "PASSENGER";
            default -> throw new RuntimeException("Invalid role: " + requestedRole);
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone() != null ? request.getPhone() : "NOT_PROVIDED");
        user.setRole(role);
        user.setActive(true);
        user.setVerified(false);
        user.setProvider("LOCAL");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setGender(request.getGender() != null ? request.getGender() : "NOT_SPECIFIED");
        user.setNationality(request.getNationality() != null ? request.getNationality() : "NOT_SPECIFIED");
        user.setPassportNumber(request.getPassportNumber() != null ? request.getPassportNumber() : "NOT_PROVIDED");

        userRepository.save(user);
        log.info("User registered — email: {}, role: {}", request.getEmail(), role);

        // Publish notification event
        try {
            NotificationEvent event = new NotificationEvent();
            event.setType("USER_REGISTERED");
            event.setToEmail(request.getEmail());
            event.setUserName(request.getFullName());
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.USER_REGISTERED_KEY, event);
        } catch (Exception e) {
            log.warn("Failed to publish USER_REGISTERED event: {}", e.getMessage());
        }

        return AuthResponse.successMessage("Registration successful! Role assigned: " + role);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt — email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("No account found with email: " + request.getEmail()));

        if (!user.isActive())
            throw new RuntimeException("Account deactivated. Contact support.");

        if ("GOOGLE".equals(user.getProvider()))
            throw new RuntimeException("This account uses Google Sign-In. Please use the 'Sign in with Google' button.");

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new RuntimeException("Incorrect password. Please try again.");

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        log.info("Login successful — email: {}", user.getEmail());
        return AuthResponse.token(token);
    }

    @Override
    public AuthResponse forgotPassword(ForgotPasswordRequest request) {
        log.info("Forgot password request — email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("No account found with email: " + request.getEmail()));

        if ("GOOGLE".equals(user.getProvider()))
            throw new RuntimeException("Google login users cannot reset password here.");

        String otp = generateOtp();
        user.setResetOtp(otp);
        user.setResetOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        try {
            NotificationEvent event = new NotificationEvent();
            event.setType("PASSWORD_RESET_OTP");
            event.setToEmail(user.getEmail());
            event.setUserName(user.getFullName());
            event.setOtp(otp);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.PASSWORD_RESET_OTP_KEY, event);
        } catch (Exception e) {
            log.warn("Failed to publish PASSWORD_RESET_OTP event: {}", e.getMessage());
        }

        return AuthResponse.successMessage("OTP sent to your registered email address.");
    }

    @Override
    public AuthResponse verifyResetOtp(VerifyResetOtpRequest request) {
        log.info("Verify reset OTP — email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("No account found with email: " + request.getEmail()));

        if ("GOOGLE".equals(user.getProvider()))
            throw new RuntimeException("Google login users cannot reset password here.");

        if (user.getResetOtp() == null || !user.getResetOtp().equals(request.getOtp()))
            throw new RuntimeException("Invalid OTP. Please check the code sent to your email.");

        if (user.getResetOtpExpiresAt() == null || user.getResetOtpExpiresAt().isBefore(LocalDateTime.now()))
            throw new RuntimeException("OTP expired. Please request a new code.");

        return AuthResponse.successMessage("OTP verified. You can now enter your new password.");
    }

    @Override
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        log.info("Reset password attempt — email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("No account found with email: " + request.getEmail()));

        if ("GOOGLE".equals(user.getProvider()))
            throw new RuntimeException("Google login users cannot reset password here.");

        if (user.getResetOtp() == null || !user.getResetOtp().equals(request.getOtp()))
            throw new RuntimeException("Invalid OTP. Please check the code sent to your email.");

        if (user.getResetOtpExpiresAt() == null || user.getResetOtpExpiresAt().isBefore(LocalDateTime.now()))
            throw new RuntimeException("OTP expired. Please request a new code.");

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetOtp(null);
        user.setResetOtpExpiresAt(null);
        userRepository.save(user);

        return AuthResponse.successMessage("Password reset successful. You can now log in with your new password.");
    }

    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

    private String generateOtp() {
        return String.format("%06d", RANDOM.nextInt(900000) + 100000);
    }

    @Override
    public AuthResponse googleLogin(GoogleAuthRequest request) {
        GoogleIdToken.Payload payload = verifyGoogleToken(request.getCredential());
        String email    = payload.getEmail();
        String fullName = (String) payload.get("name");

        Optional<User> existing = userRepository.findByEmail(email);
        boolean isNewUser = existing.isEmpty();
        User user;

        if (isNewUser) {
            user = new User();
            user.setFullName(fullName != null ? fullName : email.split("@")[0]);
            user.setEmail(email);
            user.setPassword(null);
            user.setPhone("NOT_PROVIDED");
            user.setRole("PASSENGER");
            user.setProvider("GOOGLE");
            user.setActive(true);
            user.setVerified(true);
            user.setGender("NOT_SPECIFIED");
            user.setNationality("NOT_SPECIFIED");
            user.setPassportNumber("NOT_PROVIDED");
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            // Publish welcome notification for new Google users
            try {
                NotificationEvent event = new NotificationEvent();
                event.setType("USER_REGISTERED");
                event.setToEmail(email);
                event.setUserName(user.getFullName());
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.USER_REGISTERED_KEY, event);
            } catch (Exception e) {
                log.warn("Failed to publish USER_REGISTERED event for Google user: {}", e.getMessage());
            }
        } else {
            user = existing.get();
            if (!user.isActive())
                throw new RuntimeException("Account deactivated. Contact support.");
            if (!"GOOGLE".equals(user.getProvider())) {
                user.setProvider("GOOGLE");
                user.setVerified(true);
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
            }
        }

        String jwtToken = jwtUtil.generateToken(user.getEmail(), user.getRole());
        return AuthResponse.googleLogin(jwtToken, user.getEmail(), user.getFullName(), user.getRole(), isNewUser);
    }

    @Override
    public ProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        return toProfileResponse(user);
    }

    @Override
    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        if (request.getFullName()       != null) user.setFullName(request.getFullName());
        if (request.getPhone()          != null) user.setPhone(request.getPhone());
        if (request.getGender()         != null) user.setGender(request.getGender());
        if (request.getNationality()    != null) user.setNationality(request.getNationality());
        if (request.getPassportNumber() != null) user.setPassportNumber(request.getPassportNumber());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Profile updated — email: {}", email);
        return toProfileResponse(user);
    }

    private ProfileResponse toProfileResponse(User user) {
        ProfileResponse res = new ProfileResponse();
        res.setId(user.getId());
        res.setFullName(user.getFullName());
        res.setEmail(user.getEmail());
        res.setPhone(user.getPhone());
        res.setGender(user.getGender());
        res.setNationality(user.getNationality());
        res.setPassportNumber(user.getPassportNumber());
        res.setRole(user.getRole());
        res.setProvider(user.getProvider());
        return res;
    }

    private GoogleIdToken.Payload verifyGoogleToken(String credential) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken == null)
                throw new RuntimeException("Google token verification failed. Please try signing in again.");
            return idToken.getPayload();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Google token verification error: " + e.getMessage());
        }
    }
}
