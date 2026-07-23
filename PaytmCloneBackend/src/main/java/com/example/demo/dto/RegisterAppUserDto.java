package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterAppUserDto {
	
	@NotBlank(message = "Name is required")
	private String name;
	
	@Email
	@NotBlank(message = "invalid email")
	private String email;
	
	@NotBlank
    @Pattern(
        regexp = "^[6-9]\\d{9}$",
        message = "Invalid mobile number"
    )
	private String mobile;
	
	@NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must be at least 8 characters long and include uppercase, lowercase, number, and special character"
    )
    private String password;
}
