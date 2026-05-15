package com.skybooker.auth.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String phone;
    private String gender;
    private String nationality;
    private String passportNumber;
}
