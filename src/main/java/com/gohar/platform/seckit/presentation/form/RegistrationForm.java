package com.gohar.platform.seckit.presentation.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form backing bean for Thymeleaf user registration input.
 * Enforces presentation-layer Bean Validation constraints.
 */
public class RegistrationForm {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Email address is required")
    @Email(message = "Please enter a valid email address")
    @Size(max = 254, message = "Email address cannot exceed 254 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be at least 8 characters")
    private String password;

    public RegistrationForm() {
    }

    public RegistrationForm(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public void clearPassword() {
        this.password = "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
