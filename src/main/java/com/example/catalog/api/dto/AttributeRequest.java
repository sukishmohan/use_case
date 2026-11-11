package com.example.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AttributeRequest(
        @NotBlank @Size(max = 40) String key,
        @Size(max = 200) String value) {
}
