package com.quiz.auth.dto;

import com.quiz.auth.entity.UserRole;
import java.time.Instant;
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
public class UserProfileResponse {
    String id;
    String username;
    String email;
    UserRole role;
    Instant createdAt;
}
