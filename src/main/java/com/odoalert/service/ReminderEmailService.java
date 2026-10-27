package com.odoalert.service;

import com.odoalert.model.Vehicle;
import com.odoalert.repo.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReminderEmailService {
    private static final Logger log = LoggerFactory.getLogger(ReminderEmailService.class);

    private final VehicleRepository vehicles;
    private final JavaMailSender mailSender;

    @Value("${odoalert.reminders.email-enabled:false}")
    private boolean emailEnabled;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public ReminderEmailService(VehicleRepository vehicles, JavaMailSender mailSender) {
        this.vehicles = vehicles;
        this.mailSender = mailSender;
    }

    // Runs daily at 9:00 AM India time. Sends a message exactly 5 days before expiry.
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")
    @Transactional(readOnly = true)
    public void sendExpiryReminders() {
        if (!emailEnabled || fromAddress == null || fromAddress.isBlank()) {
            log.debug("Email reminders disabled or Gmail username not configured.");
            return;
        }

        LocalDate today = LocalDate.now();
        List<Vehicle> allVehicles = vehicles.findAll();

        for (Vehicle vehicle : allVehicles) {
            String recipient = vehicle.getOwner().getEmail();
            sendIfDue(recipient, vehicle, "Insurance", vehicle.getInsuranceExpiry(), today);
            sendIfDue(recipient, vehicle, "PUC", vehicle.getPucExpiry(), today);
            sendIfDue(recipient, vehicle, "Fitness / Permit", vehicle.getFitnessExpiry(), today);
        }
    }

    private void sendIfDue(String recipient, Vehicle vehicle, String document,
                           LocalDate expiryDate, LocalDate today) {
        if (expiryDate == null || ChronoUnit.DAYS.between(today, expiryDate) != 5) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject("ODO Alert: " + document + " expires in 5 days");
        message.setText(
            "Hello,\n\nYour vehicle document is due to expire in 5 days.\n\n" +
            "Vehicle: " + vehicle.getName() + "\n" +
            "Registration: " + vehicle.getRegistrationNumber() + "\n" +
            "Document: " + document + "\n" +
            "Expiry date: " + expiryDate + "\n\n" +
            "Please renew it on time.\n\nODO Alert"
        );

        try {
            mailSender.send(message);
            log.info("Expiry reminder sent for vehicle {} ({}) document {}",
                vehicle.getRegistrationNumber(), recipient, document);
        } catch (MailException ex) {
            log.error("Could not send {} expiry reminder for vehicle {}",
                document, vehicle.getRegistrationNumber(), ex);
        }
    }
}
