package com.quiz.classservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserClassId implements Serializable {

    @Column(name = "user_id", nullable = false, length = 36)
    String userId;

    @Column(name = "class_id", nullable = false, length = 36)
    String classId;
}
