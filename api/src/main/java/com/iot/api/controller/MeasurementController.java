
package com.iot.api.controller;

import com.iot.api.dto.MeasurementResponse;
import com.iot.api.service.MeasurementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/measurements")
public class MeasurementController {

    private final MeasurementService measurementService;

    public MeasurementController(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    // GET /api/measurements
    @GetMapping
    public ResponseEntity<List<MeasurementResponse>> getAllMeasurements() {
        return ResponseEntity.ok(
                measurementService.getAllMeasurements()
        );
    }

    // GET /api/measurements/device/{deviceId}
    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<MeasurementResponse>> getMeasurementsByDevice(
            @PathVariable String deviceId
    ) {
        return ResponseEntity.ok(
                measurementService.getMeasurementsByDevice(deviceId)
        );
    }

    // GET /api/measurements/search
    @GetMapping("/search")
    public ResponseEntity<List<MeasurementResponse>> searchMeasurements(
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) String metric,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(
                measurementService.getMeasurementsByFilters(
                        zone, metric, from, to, limit
                )
        );
    }

    // GET /api/measurements/latest
    @GetMapping("/latest")
    public ResponseEntity<List<MeasurementResponse>> getLatestMeasurements() {
        return ResponseEntity.ok(
                measurementService.getLatestMeasurements()
        );
    }

    // GET /api/measurements/latest/{deviceId}
    @GetMapping("/latest/{deviceId}")
    public ResponseEntity<MeasurementResponse> getLatestMeasurementByDevice(
            @PathVariable String deviceId
    ) {
        return measurementService.getLatestMeasurementByDevice(deviceId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
