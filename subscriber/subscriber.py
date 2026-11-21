"""Subscriber MQTT : valide les mesures de CONTRAT.md et les écrit dans InfluxDB.

Variables d'environnement : BROKER_HOST (défaut localhost), BROKER_PORT (défaut 1883),
INFLUXDB_* (lues dans le .env à la racine, voir .env.example).
"""
import os

import paho.mqtt.client as mqtt

from influx_writer import write_measurement
from models import validate_measurement

BROKER_HOST = os.getenv("BROKER_HOST", "localhost")
BROKER_PORT = int(os.getenv("BROKER_PORT", "1883"))
TOPIC = "iot/+/+/+"  # iot/{zone}/{deviceId}/{metric|status}


def on_connect(client, userdata, flags, reason_code, properties):
    if reason_code.is_failure:
        print(f"Connexion refusée : {reason_code}")
        return
    print(f"Connecté à {BROKER_HOST}:{BROKER_PORT}, abonnement à {TOPIC}")
    client.subscribe(TOPIC, qos=1)


def on_message(client, userdata, msg):
    zone, device_id, kind = msg.topic.split("/")[1:4]
    payload = msg.payload.decode("utf-8")

    if kind == "status":
        print(f"[{zone}/{device_id}] état : {payload}")
        return

    try:
        m = validate_measurement(kind, device_id, payload)
    except ValueError as e:
        print(f"Rejeté sur {msg.topic} : {e}")
        return

    try:
        write_measurement(zone, kind, m)
    except Exception as e:
        print(f"Écriture InfluxDB échouée pour {msg.topic} : {e}")
        return

    print(f"[{zone}/{device_id}] {kind} = {m.value} {m.unit} ({m.timestamp.isoformat()}) -> InfluxDB")


def main():
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    client.on_connect = on_connect
    client.on_message = on_message
    client.connect(BROKER_HOST, BROKER_PORT, keepalive=60)
    client.loop_forever()


if __name__ == "__main__":
    main()