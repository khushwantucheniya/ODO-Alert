package com.odoalert.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record VehicleRequest(
    @NotBlank @Size(max=70) String name,
    @NotBlank @Size(max=20) String registrationNumber,
    @NotBlank @Pattern(regexp="Car|Bike|Truck|Bus|Other") String type,
    LocalDate insuranceExpiry,
    LocalDate pucExpiry,
    LocalDate fitnessExpiry,
    LocalDate nextServiceDate,
    @PositiveOrZero Long nextServiceKm
) {}
