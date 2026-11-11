package com.example.catalog.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record ProductRequest(
        @NotBlank @Size(max = 30) String supplierProductId,
        ProductAction action,
        @Size(max = 60) String brand,
        @Valid CategoryRequest category,
        @Valid Map<String, String> names,
        @Valid Map<String, String> descriptions,
        @Valid List<@NotNull @Valid AttributeRequest> attributes,
        @Valid List<@NotNull @Valid VariantRequest> variants) {
}
