# Gezeitenlogik

Implementiert in `domain/TideAnalyzer.kt` und `data/TideRepository.kt` (Spez. §14–§18).
Grundsatz: **Es werden niemals Gezeitenzeiten erfunden.**

## Datenquellen und Prioritäten

1. **OFFICIAL_TIDE_TABLE** — offizielle DHN-Jahrestabelle Porto de Natal, importiert als
   Zeilen der Room-Tabelle `tide_event` (siehe Importformat unten). Confiança alta.
2. **MODELLED_SEA_LEVEL** — Fallback der v1: Open-Meteo Marine `sea_level_height_msl`
   (`cell_selection=sea`). Sichtbar gekennzeichnet, Confiança moderada, mit Hinweistext:
   *"Os horários oficiais da DHN não estão disponíveis para este período. Exibindo uma
   estimativa baseada em modelo."*

## Erkennung von Ebbe und Flut (§16)

1. Zeitreihe sortieren, deduplizieren, nicht-endliche Werte verwerfen
2. bei < 6 Punkten: Status UNKNOWN ("Situação da maré indisponível")
3. vorsichtige Glättung (gleitendes Mittel über 3 Punkte)
4. Vorzeichenwechsel der ersten Ableitung → lokale Minima (mögliche Ebbe) und
   Maxima (mögliche Flut)
5. Mindestabstand 3 h zwischen Extremen; bei Verstoss bzw. gleichem Typ in Folge
   wird das extremere Ereignis behalten
6. Richtung: RISING/FALLING aus der Kurve um "jetzt"; innerhalb ±45 min um ein Extrem
   NEAR_HIGH/NEAR_LOW

## Anzeige (§14, §17)

Maré-Screen: Phase + Datenqualität (Chip), nächste Ebbe/Flut mit Countdown und Höhe,
24-h-Kurve (Canvas) mit Nachtbändern (Sonnenauf-/-untergang aus Best-Match-Daily),
markierten Extremen, "Agora"-Marker; Ereignisliste der nächsten Tage; Wellen, Swell,
Wassertemperatur; Sicherheitshinweis (§19/§40). Startseite: Kompaktkurve 12 h + Countdown.

## Lokale Korrektur (§15)

Porto de Natal ist Referenz, nicht Pirangi. Die Struktur `LocalTideCorrection`
(Zeit-Offsets, Höhenfaktor, Confidence UNKNOWN) ist vorgesehen; v1 wendet bewusst
keine willkürlichen Korrekturen an (Offsets 0, Faktor 1.0).

## DHN-Importformat

Jahresdaten ausserhalb der App in JSON wandeln und als `tide_event`-Zeilen importieren
(stationId `porto_natal`):

```json
{
  "stationId": "porto_natal",
  "stationName": "Porto de Natal",
  "source": "DHN",
  "year": 2026,
  "timezone": "America/Fortaleza",
  "events": [
    { "timestamp": "2026-07-31T03:42:00-03:00", "type": "LOW",  "heightMeters": 0.4 },
    { "timestamp": "2026-07-31T09:58:00-03:00", "type": "HIGH", "heightMeters": 2.1 }
  ]
}
```

Sobald Ereignisse vorhanden sind, schaltet `TideRepository` automatisch auf
OFFICIAL_TIDE_TABLE um (Ereignisse aus der Tabelle, Kurve nur noch zur Darstellung).
