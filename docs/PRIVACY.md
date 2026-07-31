# Datenschutz

Tempo RN ist bewusst datensparsam (Spez. §31):

- **kein Benutzerkonto**, kein Login
- **keine Werbe-ID**, keine Tracking- oder Analytics-SDKs
- **keine persönlichen Daten** werden erhoben oder extern gespeichert
- **kein Standortzugriff** in v1 — die Ortswahl erfolgt aus einer festen Liste
  (Standard: Pirangi do Norte); eine optionale Standortberechtigung wäre ein
  späteres, rein optionales Feature
- Netzverkehr ausschliesslich zu den in `docs/API_SOURCES.md` genannten Wetter-,
  Radar- und Kartendiensten; übertragen werden nur die Koordinaten des gewählten
  Ortes (feste Liste öffentlicher Orte) und ein generischer User-Agent
- alle Prognose- und Gezeitendaten liegen lokal in einer Room-Datenbank auf dem Gerät
- keine API-Keys in der App (v1 nutzt nur key-freie Dienste)
