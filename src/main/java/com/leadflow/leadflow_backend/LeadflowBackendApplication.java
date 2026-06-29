package com.leadflow.leadflow_backend;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class LeadflowBackendApplication {

    public static void main(final String[] args) {
        SpringApplication.run(LeadflowBackendApplication.class, args);
        System.out.println("Application started");
    }

}


