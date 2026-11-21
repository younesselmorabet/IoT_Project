"""Écriture des mesures validées dans InfluxDB (modèle : docs/DATA_MODEL.md)."""
import os
from pathlib import Path

from dotenv import load_dotenv
from influxdb_client import InfluxDBClient, Point, WritePrecision
from influxdb_client.client.write_api import SYNCHRONOUS

load_dotenv(Path(__file__).resolve().parent.parent / ".env")

URL = os.getenv("INFLUXDB_URL", "http://localhost:8086")
TOKEN = os.environ["INFLUXDB_TOKEN"]
ORG = os.getenv("INFLUXDB_ORG", "iot")
BUCKET = os.getenv("INFLUXDB_BUCKET", "iot_data")

_client = InfluxDBClient(url=URL, token=TOKEN, org=ORG)
_write_api = _client.write_api(write_options=SYNCHRONOUS)


def write_measurement(zone: str, metric: str, m) -> None:
    point = (
        Point(metric)
        .tag("zone", zone)
        .tag("device_id", m.deviceId)
        .tag("unit", m.unit)
        .field("value", m.value)
        .time(m.timestamp, WritePrecision.S)
    )
    _write_api.write(bucket=BUCKET, record=point)