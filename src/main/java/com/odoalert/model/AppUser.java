package com.odoalert.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 190)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private LocalDate createdAt = LocalDate.now();

    protected AppUser() {}

    public AppUser(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email.toLowerCase().trim();
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getPasswordHash() { return passwordHash; }
}
