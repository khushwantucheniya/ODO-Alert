package com.odoalert.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "vehicles", indexes = @Index(name = "idx_vehicle_owner", columnList = "owner_id"))
public class Vehicle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @Column(nullable = false, length = 70)
    private String name;

    @Column(nullable = false, length = 20)
    private String registrationNumber;

    @Column(nullable = false, length = 20)
    private String type;

    private LocalDate insuranceExpiry;
    private LocalDate pucExpiry;
    private LocalDate fitnessExpiry;
    private LocalDate nextServiceDate;
    private Long nextServiceKm;

    @Column(nullable = false)
    private LocalDate createdAt = LocalDate.now();

    protected Vehicle() {}

    public Vehicle(AppUser owner, String name, String registrationNumber, String type,
                   LocalDate insuranceExpiry, LocalDate pucExpiry, LocalDate fitnessExpiry,
                   LocalDate nextServiceDate, Long nextServiceKm) {
        this.owner = owner;
        update(name, registrationNumber, type, insuranceExpiry, pucExpiry,
               fitnessExpiry, nextServiceDate, nextServiceKm);
    }

    public void update(String name, String registrationNumber, String type,
                       LocalDate insuranceExpiry, LocalDate pucExpiry, LocalDate fitnessExpiry,
                       LocalDate nextServiceDate, Long nextServiceKm) {
        this.name = name.trim();
        this.registrationNumber = registrationNumber.trim().toUpperCase();
        this.type = type;
        this.insuranceExpiry = insuranceExpiry;
        this.pucExpiry = pucExpiry;
        this.fitnessExpiry = fitnessExpiry;
        this.nextServiceDate = nextServiceDate;
        this.nextServiceKm = nextServiceKm;
    }

    public Long getId() { return id; }
    public AppUser getOwner() { return owner; }
    public String getName() { return name; }
    public String getRegistrationNumber() { return registrationNumber; }
    public String getType() { return type; }
    public LocalDate getInsuranceExpiry() { return insuranceExpiry; }
    public LocalDate getPucExpiry() { return pucExpiry; }
    public LocalDate getFitnessExpiry() { return fitnessExpiry; }
    public LocalDate getNextServiceDate() { return nextServiceDate; }
    public Long getNextServiceKm() { return nextServiceKm; }
}
