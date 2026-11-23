package com.iot.api.controller;

import com.iot.api.dto.MeasurementResponse;
import com.iot.api.service.MeasurementService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/measurements")
@Validated
public class MeasurementController {

    // Contract: lowercase, no spaces or accents
    private static final String ID_REGEX = "^[a-z0-9_-]{1,64}$";
    private static final String ID_MESSAGE =
            "must be lowercase letters, digits, '-' or '_' (max 64 chars)";

    private final MeasurementService measurementService;

    public MeasurementController(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    // GET /api/measurements?limit=100
    @GetMapping
    public ResponseEntity<List<MeasurementResponse>> getAllMeasurements(
            @RequestParam(required = false) @Min(1) Integer limit
    ) {
        return ResponseEntity.ok(measurementService.getAllMeasurements(limit));
    }

    // GET /api/measurements/device/{deviceId}?limit=100
    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<MeasurementResponse>> getMeasurementsByDevice(
            @PathVariable @Pattern(regexp = ID_REGEX, message = ID_MESSAGE) String deviceId,
            @RequestParam(required = false) @Min(1) Integer limit
    ) {
        return ResponseEntity.ok(
                measurementService.getMeasurementsByDevice(deviceId, limit)
        );
    }

    // GET /api/measurements/search?zone=&metric=&from=&to=&limit=
    // Dates: ISO-8601 with 'Z' (a '+' offset must be URL-encoded as %2B)
    @GetMapping("/search")
    public ResponseEntity<List<MeasurementResponse>> searchMeasurements(
            @RequestParam(required = false)
            @Pattern(regexp = ID_REGEX, message = ID_MESSAGE) String zone,
            @RequestParam(required = false)
            @Pattern(regexp = ID_REGEX, message = ID_MESSAGE) String metric,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) @Min(1) Integer limit
    ) {
        return ResponseEntity.ok(
                measurementService.getMeasurementsByFilters(zone, metric, from, to, limit)
        );
    }

    // GET /api/measurements/latest  (latest value of each metric, every device)
    @GetMapping("/latest")
    public ResponseEntity<List<MeasurementResponse>> getLatestMeasurements() {
        return ResponseEntity.ok(measurementService.getLatestMeasurements());
    }

    // GET /api/measurements/latest/{deviceId}  (latest value of each metric)
    @GetMapping("/latest/{deviceId}")
    public ResponseEntity<List<MeasurementResponse>> getLatestMeasurementsByDevice(
            @PathVariable @Pattern(regexp = ID_REGEX, message = ID_MESSAGE) String deviceId
    ) {
        List<MeasurementResponse> latest =
                measurementService.getLatestMeasurementsByDevice(deviceId);
        return latest.isEmpty()
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(latest);
    }
}