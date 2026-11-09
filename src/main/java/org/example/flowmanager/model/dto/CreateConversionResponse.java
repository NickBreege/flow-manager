package org.example.flowmanager.model.dto;

import org.example.flowmanager.model.status.ConversionStatus;

public record CreateConversionResponse(
        Long id,
        ConversionStatus status
) {
}
