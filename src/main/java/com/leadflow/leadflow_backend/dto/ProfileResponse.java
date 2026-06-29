package com.leadflow.leadflow_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    private String name;
    private String email;
    private String phone;
    private String token; // optional, populated only if email is changed
}
