package com.skybooker.auth.dto;

import lombok.Data;

@Data
public class ProfileResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String gender;
    private String nationality;
    private String passportNumber;
    private String role;
    private String provider;
}
