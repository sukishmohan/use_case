package com.example.catalog.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

public record FeedRequest(
        @NotBlank @Pattern(regexp = "FD-[A-Z]+-\\d{6}") String feedId,
        @NotNull @Positive Long sequenceNo,
        @NotNull FeedMode mode,
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING) OffsetDateTime generatedAt,
        @NotEmpty @Size(max = 1000) @Valid List<@NotNull @Valid ProductRequest> products) {
}
