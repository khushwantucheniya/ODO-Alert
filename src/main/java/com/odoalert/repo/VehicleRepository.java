package com.odoalert.repo;

import com.odoalert.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findAllByOwnerIdOrderByIdDesc(Long ownerId);
    Optional<Vehicle> findByIdAndOwnerId(Long id, Long ownerId);
    boolean existsByOwnerIdAndRegistrationNumberIgnoreCase(Long ownerId, String registrationNumber);
    boolean existsByOwnerIdAndRegistrationNumberIgnoreCaseAndIdNot(Long ownerId, String registrationNumber, Long id);
}
