package com.iot.api.dto;

import java.time.Instant;

public record MeasurementResponse(
        String deviceId,
        String zone,
        String metric,
        double value,
        String unit,
        Instant timestamp
) {
}