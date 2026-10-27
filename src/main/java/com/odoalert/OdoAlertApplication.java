package com.odoalert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OdoAlertApplication {
    public static void main(String[] args) {
        SpringApplication.run(OdoAlertApplication.class, args);
    }
}
