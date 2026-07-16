package com.campus.trade.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false)
    private String password; // BCrypt 哈希

    @Column(length = 30)
    private String nickname;

    @Column(length = 60)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 50)
    private String campus;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
