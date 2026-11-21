# Contrat de données (T1)

Ce document est la référence commune entre le simulateur (Java) et le subscriber / l'ingestion (Python). Toute modification doit être validée par les deux membres de l'équipe.

## Topics

| Usage | Topic | Sens | QoS | Retained |
|---|---|---|---|---|
| Mesure | `iot/{zone}/{deviceId}/{metric}` | objet → broker | 1 | non |
| État de l'objet | `iot/{zone}/{deviceId}/status` | objet → broker | 1 | oui |
| Commande (sprint 3) | `iot/{zone}/{deviceId}/commande` | application → objet | 1 | non |

- `zone` : `salon`, `cuisine`, `bureau`...
- `deviceId` : `capteur01`, `capteur02`...
- `metric` : `temperature` ou `humidity`
- Tout en minuscules, sans espace ni accent.

Exemples : `iot/salon/capteur01/temperature`, `iot/salon/capteur01/status`.

## Message de mesure (JSON)

```json
{
  "deviceId": "capteur01",
  "value": 27.5,
  "unit": "C",
  "timestamp": "2026-11-09T10:30:00Z"
}
```

| Champ | Type | Règle |
|---|---|---|
| `deviceId` | string | identique au `deviceId` du topic |
| `value` | nombre | décimal avec un point |
| `unit` | string | `C` pour temperature, `%` pour humidity |
| `timestamp` | string | ISO 8601, UTC, suffixe `Z` |

## Message d'état

Texte brut : `online` ou `offline`.

- Le simulateur publie `online` (retained) à la connexion.
- Il déclare un Last Will : si la connexion est coupée brutalement, le broker publie `offline` (retained) à sa place.

## Plages de valeurs attendues (utilisées pour la validation en sprint 2)

| Métrique | Min | Max |
|---|---|---|
| temperature | -40 | 85 |
| humidity | 0 | 100 |
