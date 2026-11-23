package com.iot.api.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.iot.api.dto.MeasurementResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Repository
public class InfluxMeasurementRepository implements MeasurementRepository {

    private final QueryApi queryApi;
    private final String bucket;
    private final Duration defaultRange;
    private final Duration latestRange;
    private final int defaultLimit;
    private final int maxLimit;

    public InfluxMeasurementRepository(
            InfluxDBClient influxDBClient,
            @Value("${influxdb.bucket}") String bucket,
            @Value("${api.default-range:PT1H}") Duration defaultRange,
            @Value("${api.latest-range:P30D}") Duration latestRange,
            @Value("${api.default-limit:1000}") int defaultLimit,
            @Value("${api.max-limit:5000}") int maxLimit
    ) {
        this.queryApi = influxDBClient.getQueryApi();
        this.bucket = bucket;
        this.defaultRange = defaultRange;
        this.latestRange = latestRange;
        this.defaultLimit = defaultLimit;
        this.maxLimit = maxLimit;
    }

    private List<MeasurementResponse> executeQuery(String flux) {
        return queryApi.query(flux).stream()
                .flatMap(table -> table.getRecords().stream())
                .map(this::mapRecord)
                .filter(Objects::nonNull)
                .toList();
    }

    private MeasurementResponse mapRecord(FluxRecord record) {
        if (!(record.getValue() instanceof Number number)) {
            return null;
        }
        return new MeasurementResponse(
                tag(record, "device_id"),
                tag(record, "zone"),
                record.getMeasurement(),
                number.doubleValue(),
                tag(record, "unit"),
                record.getTime()
        );
    }

    private static String tag(FluxRecord record, String key) {
        Object value = record.getValueByKey(key);
        return value == null ? null : value.toString();
    }

    private static String fluxString(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("${", "\\${")
                .replace("\n", "\\n")
                .replace("\r", "\\r") + "\"";
    }

    private int resolveLimit(Integer limit) {
        if (limit == null) {
            return defaultLimit;
        }
        if (limit < 1) {
            throw new IllegalArgumentException("'limit' must be greater than zero");
        }
        if (limit > maxLimit) {
            throw new IllegalArgumentException("'limit' must not exceed " + maxLimit);
        }
        return limit;
    }

    private static void appendFilter(StringBuilder flux, String column, String value) {
        if (value != null && !value.isBlank()) {
            flux.append("  |> filter(fn: (r) => r.").append(column)
                    .append(" == ").append(fluxString(value)).append(")\n");
        }
    }



    private List<MeasurementResponse> search(
            String deviceId, String zone, String metric,
            Instant from, Instant to, Integer limit
    ) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException("'from' must be earlier than 'to'");
        }
        int effectiveLimit = resolveLimit(limit);

        Instant start = from != null
                ? from
                : (to != null ? to : Instant.now()).minus(defaultRange);

        StringBuilder flux = new StringBuilder();
        flux.append("from(bucket: ").append(fluxString(bucket)).append(")\n");
        flux.append("  |> range(start: ").append(start);
        if (to != null) {
            flux.append(", stop: ").append(to);
        }
        flux.append(")\n");
        flux.append("  |> filter(fn: (r) => r._field == \"value\")\n");

        appendFilter(flux, "device_id", deviceId);
        appendFilter(flux, "zone", zone);
        appendFilter(flux, "_measurement", metric);

        flux.append("  |> group()\n");
        flux.append("  |> sort(columns: [\"_time\"], desc: true)\n");
        flux.append("  |> limit(n: ").append(effectiveLimit).append(")\n");

        return executeQuery(flux.toString());
    }

    @Override
    public List<MeasurementResponse> findAll() {
        return findAll(defaultLimit);
    }

    @Override
    public List<MeasurementResponse> findAll(Integer limit) {
        return search(null, null, null, null, null, limit);
    }

    @Override
    public List<MeasurementResponse> findByDeviceId(String deviceId) {
        return findByDeviceId(deviceId, defaultLimit);
    }

    @Override
    public List<MeasurementResponse> findByDeviceId(String deviceId, Integer limit) {
        return search(deviceId, null, null, null, null, limit);
    }

    @Override
    public List<MeasurementResponse> findByFilters(
            String zone, String metric, Instant from, Instant to, Integer limit
    ) {
        return search(null, zone, metric, from, to, limit);
    }

    @Override
    public List<MeasurementResponse> findLatestByDeviceId(String deviceId) {
        Instant start = Instant.now().minus(latestRange);

        String flux = """
                from(bucket: %s)
                  |> range(start: %s)
                  |> filter(fn: (r) => r._field == "value" and r.device_id == %s)
                  |> group(columns: ["zone", "_measurement"])
                  |> top(n: 1, columns: ["_time"])
                """.formatted(fluxString(bucket), start, fluxString(deviceId));

        return executeQuery(flux);
    }

    @Override
    public List<MeasurementResponse> findLatest() {
        Instant start = Instant.now().minus(latestRange);

        String flux = """
                from(bucket: %s)
                  |> range(start: %s)
                  |> filter(fn: (r) => r._field == "value")
                  |> group(columns: ["zone", "device_id", "_measurement"])
                  |> top(n: 1, columns: ["_time"])
                """.formatted(fluxString(bucket), start);

        return executeQuery(flux);
    }
}