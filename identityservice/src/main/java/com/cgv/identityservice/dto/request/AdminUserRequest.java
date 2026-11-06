package com.cgv.identityservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUserRequest(@NotBlank String fullName, @Email @NotBlank String email,
                               @Size(max = 20) String phone, String username, String password, String role) {}
