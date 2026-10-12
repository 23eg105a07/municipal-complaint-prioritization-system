package com.municipal.system.complaint.dto;

import com.municipal.system.complaint.entity.ComplaintCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateComplaintRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title cannot exceed 200 characters")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Category is required")
        ComplaintCategory category,

        @NotNull(message = "Severity is required")
        @Min(value = 1, message = "Severity must be between 1 and 5")
        @Max(value = 5, message = "Severity must be between 1 and 5")
        Integer severity,

        @NotNull(message = "Zone ID is required")
        Integer zoneId,

        BigDecimal latitude,
        BigDecimal longitude,
        String address,
        String photoUrl
) {
}
