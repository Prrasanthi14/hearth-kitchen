package com.hearth.kitchen;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HearthKitchenApplication {

    public static void main(String[] args) {
        // Set JVM timezone to IST so all timestamps display in local time.
        // Must run before Spring Boot starts so Hibernate and Thymeleaf both pick it up.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(HearthKitchenApplication.class, args);
    }
}
