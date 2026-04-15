package com.parkease.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.parkease.auth.entity.User;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private String phone;

    private User.Role role = User.Role.DRIVER;

    private String vehiclePlate;

    @JsonIgnore  // ← hides this from Swagger schema and JSON serialization
    @AssertTrue(message = "Registration as ADMIN is not allowed")
    public boolean isRoleValid() {
        return role == null || role != User.Role.ADMIN;
    }
}
