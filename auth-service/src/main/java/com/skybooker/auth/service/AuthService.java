package com.skybooker.auth.service;

import com.skybooker.auth.dto.AuthResponse;
import com.skybooker.auth.dto.ForgotPasswordRequest;
import com.skybooker.auth.dto.GoogleAuthRequest;
import com.skybooker.auth.dto.LoginRequest;
import com.skybooker.auth.dto.ProfileResponse;
import com.skybooker.auth.dto.RegisterRequest;
import com.skybooker.auth.dto.ResetPasswordRequest;
import com.skybooker.auth.dto.UpdateProfileRequest;
import com.skybooker.auth.dto.VerifyResetOtpRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse forgotPassword(ForgotPasswordRequest request);

    AuthResponse verifyResetOtp(VerifyResetOtpRequest request);

    AuthResponse resetPassword(ResetPasswordRequest request);

    AuthResponse googleLogin(GoogleAuthRequest request);

    ProfileResponse getProfile(String email);

    ProfileResponse updateProfile(String email, UpdateProfileRequest request);
}
