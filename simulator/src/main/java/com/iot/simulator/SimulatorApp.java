package com.iot.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class SimulatorApp {

    private static final String BROKER_URL = env("BROKER_URL", "tcp://localhost:1883");
    private static final String ZONE = env("ZONE", "salon");
    private static final String DEVICE_ID = env("DEVICE_ID", "capteur01");
    private static final long INTERVAL_MS = Long.parseLong(env("INTERVAL_MS", "5000"));
    private static final String CLIENT_ID = "iot-simulator-" + DEVICE_ID;

    private static final String STATUS_TOPIC =
            "iot/" + ZONE + "/" + DEVICE_ID + "/status";

    private static final String TEMPERATURE_TOPIC =
            "iot/" + ZONE + "/" + DEVICE_ID + "/temperature";

    private static final String HUMIDITY_TOPIC =
            "iot/" + ZONE + "/" + DEVICE_ID + "/humidity";

    private static MqttClient client;
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {

        try {
            connect();
            publishStatus("online");

            // Handle normal application shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (client != null && client.isConnected()) {
                        publishStatus("offline");
                        client.disconnect();
                        client.close();
                        System.out.println("Simulator disconnected.");
                    }
                } catch (MqttException e) {
                    System.err.println("Shutdown error: " + e.getMessage());
                }
            }));

            System.out.println("IoT Simulator started: " + ZONE + "/" + DEVICE_ID);

            // Publish measurements at a fixed interval
            while (true) {
                double temperature = generateValue(18, 30);
                double humidity = generateValue(30, 70);

                publishMeasurement(
                        TEMPERATURE_TOPIC, temperature, "C"
                );

                publishMeasurement(
                        HUMIDITY_TOPIC, humidity, "%"
                );

                Thread.sleep(INTERVAL_MS);
            }

        } catch (Exception e) {
            System.err.println("Simulator error: " + e.getMessage());
        }
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private static void connect() throws MqttException {

        client = new MqttClient(BROKER_URL, CLIENT_ID);

        MqttConnectOptions options = new MqttConnectOptions();

        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);

        // Last Will: published if connection is lost unexpectedly
        options.setWill(
                STATUS_TOPIC,
                "offline".getBytes(),
                1,
                true
        );

        client.connect(options);

        System.out.println("Connected to MQTT Broker: " + BROKER_URL);
    }

    private static void publishStatus(String status)
            throws MqttException {

        client.publish(
                STATUS_TOPIC,
                status.getBytes(),
                1,
                true
        );

        System.out.println("Status published: " + status);
    }

    private static void publishMeasurement(
            String topic, double value, String unit)
            throws Exception {

        Map<String, Object> message = Map.of(
                "deviceId", DEVICE_ID,
                "value", value,
                "unit", unit,
                "timestamp", Instant.now().toString()
        );

        String json = mapper.writeValueAsString(message);

        client.publish(
                topic,
                json.getBytes(),
                1,
                false
        );

        System.out.println(topic + " -> " + json);
    }

    private static double generateValue(double min, double max) {

        double value = ThreadLocalRandom.current()
                .nextDouble(min, max);

        return Math.round(value * 10.0) / 10.0;
    }
}