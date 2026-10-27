package com.odoalert.controller;

import com.odoalert.dto.VehicleRequest;
import com.odoalert.model.Vehicle;
import com.odoalert.repo.UserRepository;
import com.odoalert.repo.VehicleRepository;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    private final VehicleRepository vehicles;
    private final UserRepository users;

    public VehicleController(VehicleRepository vehicles, UserRepository users) {
        this.vehicles = vehicles;
        this.users = users;
    }

    private Long ownerId(Authentication auth) {
        return users.findByEmailIgnoreCase(auth.getName()).orElseThrow().getId();
    }

    @GetMapping
    public List<Map<String,Object>> list(Authentication auth) {
        return vehicles.findAllByOwnerIdOrderByIdDesc(ownerId(auth)).stream()
            .map(this::toMap).toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody VehicleRequest r, Authentication auth) {
        Long owner = ownerId(auth);
        String reg = r.registrationNumber().trim().toUpperCase();
        if (vehicles.existsByOwnerIdAndRegistrationNumberIgnoreCase(owner, reg)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "That registration number is already saved."));
        }
        var user = users.findById(owner).orElseThrow();
        Vehicle v = new Vehicle(user, r.name(), reg, r.type(), r.insuranceExpiry(),
            r.pucExpiry(), r.fitnessExpiry(), r.nextServiceDate(), r.nextServiceKm());
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(vehicles.save(v)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody VehicleRequest r,
                                   Authentication auth) {
        Long owner = ownerId(auth);
        Vehicle v = vehicles.findByIdAndOwnerId(id, owner).orElse(null);
        if (v == null) return ResponseEntity.notFound().build();

        String reg = r.registrationNumber().trim().toUpperCase();
        if (vehicles.existsByOwnerIdAndRegistrationNumberIgnoreCaseAndIdNot(owner, reg, id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "That registration number is already saved."));
        }
        v.update(r.name(), reg, r.type(), r.insuranceExpiry(), r.pucExpiry(),
            r.fitnessExpiry(), r.nextServiceDate(), r.nextServiceKm());
        return ResponseEntity.ok(toMap(vehicles.save(v)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication auth) {
        Vehicle v = vehicles.findByIdAndOwnerId(id, ownerId(auth)).orElse(null);
        if (v == null) return ResponseEntity.notFound().build();
        vehicles.delete(v);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reminders")
    public Map<String,Object> reminders(Authentication auth) {
        List<Map<String,Object>> items = vehicles.findAllByOwnerIdOrderByIdDesc(ownerId(auth))
            .stream().flatMap(v -> remindersFor(v).stream())
            .sorted(Comparator.comparingLong(m -> ((Number)m.get("daysLeft")).longValue()))
            .toList();
        long due = items.stream().filter(m -> ((Number)m.get("daysLeft")).longValue() >= 0).count();
        long expiredDocs = items.stream().filter(m ->
            "DOCUMENT".equals(m.get("kind")) &&
            ((Number)m.get("daysLeft")).longValue() < 0).count();
        long maintenance = items.stream().filter(m ->
            "SERVICE".equals(m.get("kind")) &&
            ((Number)m.get("daysLeft")).longValue() <= 5).count();
        return Map.of("items", items, "dueSoon", due, "expiredDocuments", expiredDocs,
            "maintenanceDue", maintenance);
    }

    private List<Map<String,Object>> remindersFor(Vehicle v) {
        List<Map<String,Object>> out = new ArrayList<>();
        addReminder(out, v, "Insurance", v.getInsuranceExpiry(), "DOCUMENT");
        addReminder(out, v, "PUC", v.getPucExpiry(), "DOCUMENT");
        addReminder(out, v, "Fitness / Permit", v.getFitnessExpiry(), "DOCUMENT");
        addReminder(out, v, "Next Service", v.getNextServiceDate(), "SERVICE");
        return out;
    }

    private void addReminder(List<Map<String,Object>> out, Vehicle v, String label,
                             LocalDate date, String kind) {
        if (date == null) return;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), date);
        if (days <= 5) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("vehicleId", v.getId());
            m.put("vehicleName", v.getName());
            m.put("registrationNumber", v.getRegistrationNumber());
            m.put("label", label);
            m.put("date", date.toString());
            m.put("daysLeft", days);
            m.put("kind", kind);
            out.add(m);
        }
    }

    private Map<String,Object> toMap(Vehicle v) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", v.getId());
        m.put("name", v.getName());
        m.put("number", v.getRegistrationNumber());
        m.put("type", v.getType());
        m.put("insurance", v.getInsuranceExpiry());
        m.put("puc", v.getPucExpiry());
        m.put("fitness", v.getFitnessExpiry());
        m.put("service", v.getNextServiceDate());
        m.put("serviceKm", v.getNextServiceKm());
        return m;
    }
}
