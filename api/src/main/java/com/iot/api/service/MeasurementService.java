
package com.iot.api.service;

import com.iot.api.dto.MeasurementResponse;
import com.iot.api.repository.MeasurementRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class MeasurementService {

    private final MeasurementRepository measurementRepository;

    public MeasurementService(MeasurementRepository measurementRepository) {
        this.measurementRepository = measurementRepository;
    }

    public List<MeasurementResponse> getAllMeasurements(Integer limit) {
        return measurementRepository.findAll(limit);
    }

    public List<MeasurementResponse> getMeasurementsByDevice(String deviceId, Integer limit) {
        return measurementRepository.findByDeviceId(deviceId, limit);
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

    public List<MeasurementResponse> getLatestMeasurementsByDevice(String deviceId) {
        return measurementRepository.findLatestByDeviceId(deviceId);
    }

    public List<MeasurementResponse> getLatestMeasurements() {
        return measurementRepository.findLatest();
    }
}
