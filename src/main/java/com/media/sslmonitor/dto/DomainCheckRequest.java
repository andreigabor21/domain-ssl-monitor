package com.media.sslmonitor.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DomainCheckRequest {

    @NotEmpty(message = "Domains list cannot be empty")
    private List<String> domains;
}