package com.example.catalog.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record VariantRequest(
        @NotBlank @Size(max = 30) String supplierVariantId,
        @NotNull @Valid PackSizeRequest packSize,
        @NotNull @Positive BigDecimal mrp,
        @NotNull @Positive BigDecimal sellingPrice,
        @NotNull Boolean priceIncludesTax,
        @NotNull @AllowedGst Integer gstPercent,
        String barcode,
        @NotNull Integer stock) {
}
