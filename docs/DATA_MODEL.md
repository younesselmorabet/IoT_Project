# Modèle de données InfluxDB (T6)

Base : InfluxDB 2.x, organisation `iot`, bucket `iot_data`, rétention 30 jours (développement).

## Structure d'un point

| Élément | Valeur | Origine |
|---|---|---|
| measurement | `temperature` ou `humidity` | dernier niveau du topic (`metric`) |
| tag `zone` | `salon` | topic |
| tag `device_id` | `capteur01` | topic (identique au champ `deviceId`) |
| tag `unit` | `C` ou `%` | champ `unit` du message |
| field `value` | `27.5` (float) | champ `value` du message |
| timestamp | instant du message | champ `timestamp` (ISO 8601 UTC) |

Les tags servent à filtrer et regrouper (peu de valeurs possibles). La valeur mesurée est toujours un champ.

## Exemple (line protocol)

```
temperature,zone=salon,device_id=capteur01,unit=C value=27.5 1794221400000000000
```

## Requêtes Flux utiles

Dernière température de chaque capteur :

```flux
from(bucket: "iot_data")
  |> range(start: -1h)
  |> filter(fn: (r) => r._measurement == "temperature" and r._field == "value")
  |> last()
```

Moyenne par 5 minutes pour un capteur :

```flux
from(bucket: "iot_data")
  |> range(start: -24h)
  |> filter(fn: (r) => r._measurement == "temperature" and r.device_id == "capteur01")
  |> aggregateWindow(every: 5m, fn: mean, createEmpty: false)
```

## Choix et limites

- Un measurement par grandeur : simple à interroger. Ajouter une grandeur ne demande aucune migration.
- `status` n'est pas stocké dans InfluxDB : l'état online/offline sera dans PostgreSQL ou dans l'API (sprint 2, T8).
- Rétention de 30 jours : à revoir avant le déploiement cloud.
