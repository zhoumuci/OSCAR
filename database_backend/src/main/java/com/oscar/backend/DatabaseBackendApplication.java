package com.oscar.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DatabaseBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatabaseBackendApplication.class, args);
    }

}
