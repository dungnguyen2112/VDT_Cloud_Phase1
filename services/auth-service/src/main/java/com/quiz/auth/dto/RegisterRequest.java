package com.quiz.auth.dto;

import com.quiz.auth.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = PRIVATE)
public class RegisterRequest {
    @NotBlank @Size(min = 3, max = 50) String username;
    @NotBlank @Email @Size(max = 100) String email;
    @NotBlank @Size(min = 6, max = 100) String password;
    @NotNull UserRole role;
}
