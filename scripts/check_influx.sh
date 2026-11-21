#!/usr/bin/env bash
# Vérifie qu'InfluxDB répond et que le bucket existe (T6).
set -euo pipefail
source "$(dirname "$0")/../.env"

echo "1. Santé du serveur"
curl -fsS "${INFLUXDB_URL}/health"; echo

echo "2. Écriture d'un point de test"
curl -fsS -X POST "${INFLUXDB_URL}/api/v2/write?org=${INFLUXDB_ORG}&bucket=${INFLUXDB_BUCKET}&precision=s" \
  -H "Authorization: Token ${INFLUXDB_TOKEN}" \
  --data-binary "temperature,zone=test,device_id=check,unit=C value=21.5 $(date +%s)"
echo "OK"

echo "3. Lecture du point de test"
curl -fsS -X POST "${INFLUXDB_URL}/api/v2/query?org=${INFLUXDB_ORG}" \
  -H "Authorization: Token ${INFLUXDB_TOKEN}" \
  -H "Content-Type: application/vnd.flux" \
  -H "Accept: application/csv" \
  --data "from(bucket: \"${INFLUXDB_BUCKET}\") |> range(start: -5m) |> filter(fn: (r) => r.zone == \"test\")"
