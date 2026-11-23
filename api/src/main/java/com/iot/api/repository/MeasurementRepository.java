package com.iot.api.repository;

import com.iot.api.dto.MeasurementResponse;

import java.time.Instant;
import java.util.List;

public interface MeasurementRepository {

    List<MeasurementResponse> findAll();

    List<MeasurementResponse> findByDeviceId(String deviceId);

    List<MeasurementResponse> findByDeviceId(String deviceId, Integer limit);

    List<MeasurementResponse> findAll(Integer limit);

    List<MeasurementResponse> findByFilters(
            String zone,
            String metric,
            Instant from,
            Instant to,
            Integer limit
    );

    List<MeasurementResponse> findLatestByDeviceId(String deviceId);

    List<MeasurementResponse> findLatest();
}