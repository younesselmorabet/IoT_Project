
package com.iot.api.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.iot.api.dto.MeasurementResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class InfluxMeasurementRepository implements MeasurementRepository {

    private final QueryApi queryApi;
    private final String bucket;

    public InfluxMeasurementRepository(
            InfluxDBClient influxDBClient,
            @Value("${influxdb.bucket}") String bucket
    ) {
        this.queryApi = influxDBClient.getQueryApi();
        this.bucket = bucket;
    }

    // Execute a Flux query and convert the results into Java objects
    private List<MeasurementResponse> executeQuery(String flux) {

        List<FluxTable> tables = queryApi.query(flux);

        return tables.stream()
                .flatMap(table -> table.getRecords().stream())
                .map(this::mapRecord)
                .toList();
    }

    // Convert one InfluxDB record into our API DTO
    private MeasurementResponse mapRecord(FluxRecord record) {

        return new MeasurementResponse(
                Objects.toString(record.getValueByKey("device_id")),
                Objects.toString(record.getValueByKey("zone")),
                record.getMeasurement(),
                ((Number) record.getValue()).doubleValue(),
                Objects.toString(record.getValueByKey("unit")),
                record.getTime()
        );
    }

    // Escape strings inserted into Flux queries
    private String fluxString(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"") + "\"";
    }

    @Override
    public List<MeasurementResponse> findAll() {

        String flux = """
                from(bucket: "%s")
                  |> range(start: -30d)
                  |> filter(fn: (r) => r._field == "value")
                """.formatted(bucket);

        return executeQuery(flux);
    }

    @Override
    public List<MeasurementResponse> findByDeviceId(String deviceId) {

        String flux = """
                from(bucket: "%s")
                  |> range(start: -30d)
                  |> filter(fn: (r) =>
                      r._field == "value" and
                      r.device_id == %s
                  )
                """.formatted(bucket, fluxString(deviceId));

        return executeQuery(flux);
    }

    @Override
    public List<MeasurementResponse> findByFilters(
            String zone,
            String metric,
            Instant from,
            Instant to,
            Integer limit
    ) {

        if (from != null && to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException(
                    "'from' must be earlier than 'to'"
            );
        }

        String start = from != null
                ? "time(v: " + fluxString(from.toString()) + ")"
                : "-30d";

        StringBuilder flux = new StringBuilder("""
                from(bucket: "%s")
                  |> range(start: %s
                """.formatted(bucket, start));

        if (to != null) {
            flux.append(", stop: time(v: ")
                    .append(fluxString(to.toString()))
                    .append(")");
        }

        flux.append("""
                )
                  |> filter(fn: (r) => r._field == "value")
                """);

        if (zone != null && !zone.isBlank()) {
            flux.append("""
                      |> filter(fn: (r) => r.zone == %s)
                    """.formatted(fluxString(zone)));
        }

        if (metric != null && !metric.isBlank()) {
            flux.append("""
                      |> filter(fn: (r) => r._measurement == %s)
                    """.formatted(fluxString(metric)));
        }

        if (limit != null) {
            if (limit < 1) {
                throw new IllegalArgumentException(
                        "'limit' must be greater than zero"
                );
            }

            flux.append("  |> limit(n: ").append(limit).append(")\n");
        }

        return executeQuery(flux.toString());
    }

    @Override
    public Optional<MeasurementResponse> findLatestByDeviceId(
            String deviceId
    ) {

        String flux = """
                from(bucket: "%s")
                  |> range(start: -30d)
                  |> filter(fn: (r) =>
                      r._field == "value" and
                      r.device_id == %s
                  )
                  |> sort(columns: ["_time"], desc: true)
                  |> limit(n: 1)
                """.formatted(bucket, fluxString(deviceId));

        return executeQuery(flux).stream().findFirst();
    }

    @Override
    public List<MeasurementResponse> findLatest() {

        String flux = """
                from(bucket: "%s")
                  |> range(start: -30d)
                  |> filter(fn: (r) => r._field == "value")
                  |> group(columns: ["device_id", "_measurement"])
                  |> last()
                """.formatted(bucket);

        return executeQuery(flux);
    }
}
