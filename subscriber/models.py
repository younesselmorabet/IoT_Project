"""Validation des mesures selon CONTRAT.md."""
from datetime import datetime

from pydantic import BaseModel

RANGES = {"temperature": (-40, 85), "humidity": (0, 100)}
UNITS = {"temperature": "C", "humidity": "%"}


class Measurement(BaseModel):
    deviceId: str
    value: float
    unit: str
    timestamp: datetime


def validate_measurement(metric: str, topic_device_id: str, raw: str) -> Measurement:
    """Retourne la mesure validée, ou lève ValueError avec la raison du rejet."""
    if metric not in RANGES:
        raise ValueError(f"métrique inconnue : {metric}")

    m = Measurement.model_validate_json(raw)

    if m.deviceId != topic_device_id:
        raise ValueError(f"deviceId {m.deviceId} différent du topic ({topic_device_id})")
    if m.unit != UNITS[metric]:
        raise ValueError(f"unité {m.unit} invalide pour {metric}")
    if m.timestamp.tzinfo is None:
        raise ValueError("timestamp sans fuseau (UTC attendu)")

    low, high = RANGES[metric]
    if not low <= m.value <= high:
        raise ValueError(f"valeur {m.value} hors plage [{low}, {high}]")
    return m