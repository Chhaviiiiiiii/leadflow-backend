package com.leadflow.leadflow_backend.domain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
@Document(collection = "leads")
@Data
@Getter
@Setter
public class Lead {

    @Id
    private String id;

    @Size(max = 255)
    private String userId;

    @NotNull
    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String email;

    @NotBlank(message = "Phone number is mandatory")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number: Must be a valid phone number (10-15 digits, optional +)")
    private String phone;
    private String source;
    private LeadStatus status;
    private String notes;

    private String createdBy;
    private LocalDateTime lastContacted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Scheduler fields
    private LocalDateTime lastReminderSent;

    private LocalDateTime lastFollowupSent;



}
