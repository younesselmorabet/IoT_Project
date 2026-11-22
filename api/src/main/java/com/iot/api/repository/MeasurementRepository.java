package com.iot.api.repository;

import com.iot.api.dto.MeasurementResponse;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MeasurementRepository {

    List<MeasurementResponse> findAll();

    List<MeasurementResponse> findByDeviceId(String deviceId);

    List<MeasurementResponse> findByFilters(
            String zone,
            String metric,
            Instant from,
            Instant to,
            Integer limit
    );

    Optional<MeasurementResponse> findLatestByDeviceId(String deviceId);

    List<MeasurementResponse> findLatest();
}