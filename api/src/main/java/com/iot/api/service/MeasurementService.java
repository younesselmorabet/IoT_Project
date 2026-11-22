
package com.iot.api.service;

import com.iot.api.dto.MeasurementResponse;
import com.iot.api.repository.MeasurementRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class MeasurementService {

    private final MeasurementRepository measurementRepository;

    public MeasurementService(MeasurementRepository measurementRepository) {
        this.measurementRepository = measurementRepository;
    }

    public List<MeasurementResponse> getAllMeasurements() {
        return measurementRepository.findAll();
    }

    public List<MeasurementResponse> getMeasurementsByDevice(String deviceId) {
        return measurementRepository.findByDeviceId(deviceId);
    }

    public List<MeasurementResponse> getMeasurementsByFilters(
            String zone,
            String metric,
            Instant from,
            Instant to,
            Integer limit
    ) {
        return measurementRepository.findByFilters(
                zone, metric, from, to, limit
        );
    }

    public Optional<MeasurementResponse> getLatestMeasurementByDevice(String deviceId) {
        return measurementRepository.findLatestByDeviceId(deviceId);
    }

    public List<MeasurementResponse> getLatestMeasurements() {
        return measurementRepository.findLatest();
    }
}
