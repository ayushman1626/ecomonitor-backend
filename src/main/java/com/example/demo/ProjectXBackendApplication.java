package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class ProjectXBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectXBackendApplication.class, args);
    }

    @PostConstruct
    public void printEnvVars() {
        System.out.println("===== ENVIRONMENT VARIABLES CHECK =====");
        System.out.println("DB_URL: " + System.getenv("DB_URL"));
        System.out.println("DB_USERNAME: " + System.getenv("DB_USERNAME"));
        System.out.println("DB_PASSWORD: " + System.getenv("DB_PASSWORD"));
        System.out.println("PORT: " + System.getenv("PORT"));
        System.out.println("MAIL_USERNAME: " + System.getenv("MAIL_USERNAME"));
        System.out.println("MAIL_PASSWORD: " + System.getenv("MAIL_PASSWORD"));
        System.out.println("=======================================");
    }
}
