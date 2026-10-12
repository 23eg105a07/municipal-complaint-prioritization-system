package com.municipal.system.assignment.dto;

import jakarta.validation.constraints.NotNull;

public record AssignOfficerRequest(
        @NotNull(message = "Officer ID is required")
        Long officerId,

        String notes
) {
}
