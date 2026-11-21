"""Subscriber MQTT (T5) : reçoit et affiche les messages définis dans CONTRAT.md.

Variables d'environnement : BROKER_HOST (défaut localhost), BROKER_PORT (défaut 1883).
La validation (Pydantic) et l'écriture en base arrivent au sprint 2 (T7).
"""
import json
import os

import paho.mqtt.client as mqtt

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
        data = json.loads(payload)
        print(f"[{zone}/{device_id}] {kind} = {data['value']} {data['unit']} ({data['timestamp']})")
    except (json.JSONDecodeError, KeyError):
        print(f"Message invalide sur {msg.topic} : {payload}")


def main():
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    client.on_connect = on_connect
    client.on_message = on_message
    client.connect(BROKER_HOST, BROKER_PORT, keepalive=60)
    client.loop_forever()


if __name__ == "__main__":
    main()
