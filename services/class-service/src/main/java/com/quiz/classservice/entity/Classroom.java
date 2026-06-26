package com.quiz.classservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Entity
@Table(name = "classes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = PRIVATE)
public class Classroom {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    String id;

    @Column(nullable = false, length = 255)
    String name;

    @Column(name = "teacher_id", nullable = false, length = 36)
    String teacherId;

    /** Short code for students to join (Azota-style); unique when set. */
    @Column(name = "join_code", length = 16, unique = true)
    String joinCode;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
