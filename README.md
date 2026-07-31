# Tempo RN

Previsão local de tempo, chuva, vento, UV, ondas e maré para o litoral do Rio Grande do Norte
(Natal, Pirangi e região) — como app Android nativo.

**Tempo RN não mostra apenas um serviço meteorológico:** o app consulta vários modelos gratuitos
(ECMWF IFS/AIFS, ICON, GFS, UKMO, GEM, ACCESS-G via Open-Meteo), normaliza, compara e produz uma
previsão consolidada com probabilidades, faixas de incerteza e um índice de confiança transparente.

## Kernfunktionen (v1)

- **Início** — Vai chover? Wahrscheinlichkeit, Zeitfenster, Menge, Confidence-Chip; Gezeiten-Countdown;
  24h-Stundenleiste; 4-Tage-Übersicht
- **Previsão** — 96-h-Diagramme (Chuva/Temperatura/Vento/Nuvens/UV) mit Bandbreite (p25–p75) und
  Confidence-Streifen, Stunden-Detail per Slider
- **Mapas** — MapLibre + OpenStreetMap, RainViewer-Radar mit Zeitleiste (Play/Scrub, Zeitstempel,
  10-min-Auto-Refresh), ehrlicher Abdeckungshinweis
- **Maré** — Gezeitenphase, nächste Ebbe/Flut mit Countdown, 24-h-Kurve mit Nachtband und Sonnenzeiten,
  Wellen/Wassertemperatur, Sicherheits- und Qualitätshinweise
- **Modelos** — Regenmenge pro Modell und Stunde, konsolidierte Prognose, Modellgewichte und lokale
  Trefferquote (transparentes, tägliches Backtesting)
- **Offline** — Room-Cache; die App zeigt immer die letzten Daten mit Datenalter an

## Architektur (Kurzfassung)

Single-Module-App (`:app`) mit Clean-Architecture-Paketen — Details in
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md):

```
core/config     zentrale Schwellenwerte (Spez.-Paragraphen §6–§11, §20)
core/model      Domänenmodelle (ConsolidatedHour, TideState, …)
core/network    Retrofit-APIs + DTOs (Open-Meteo Forecast/Ensemble/Marine, RainViewer)
core/database   Room (Modell-, Ensemble-, Marine-, Gezeiten-, Gewichts-, Archiv-Tabellen)
domain          pure Logik: Konsolidierung, Confidence, Gezeitenanalyse, Gewichts-Update
data            Repositories + WorkManager-Worker (60 min / 3 h / täglich)
ui, feature     Compose: Theme "Horizontlinie", 5 Screens
```

Prognosealgorithmus: [`docs/FORECAST_ALGORITHM.md`](docs/FORECAST_ALGORITHM.md) ·
Gezeitenlogik: [`docs/TIDE_ALGORITHM.md`](docs/TIDE_ALGORITHM.md) ·
Datenquellen und Limits: [`docs/API_SOURCES.md`](docs/API_SOURCES.md) ·
Datenschutz: [`docs/PRIVACY.md`](docs/PRIVACY.md) ·
Tests: [`docs/TESTING.md`](docs/TESTING.md) ·
Release/Signierung: [`docs/RELEASE.md`](docs/RELEASE.md) ·
Vollständige Spezifikation: [`docs/SPEC.md`](docs/SPEC.md) ·
Designvorschlag: [`docs/design/design-proposal.html`](docs/design/design-proposal.html)

## Build

Voraussetzungen: JDK 17, Android SDK (compileSdk 35). Dann:

```bash
./gradlew test              # Unit-Tests (Prognose- und Gezeitenlogik)
./gradlew assembleRelease   # signierte Release-APK
# Ergebnis: app/build/outputs/apk/release/app-release.apk
```

CI: `.github/workflows/android-build.yml` baut bei jedem Push Tests + Release-APK und
veröffentlicht die APK auf dem Branch `apk-dist` sowie als Workflow-Artefakt.

## Bekannte Einschränkungen (v1)

- **DHN-Gezeitentabellen sind noch nicht importiert.** Die App läuft im klar gekennzeichneten
  Modus `MODELLED_SEA_LEVEL` (Open-Meteo `sea_level_height_msl`, Zelle `cell_selection=sea`).
  Es werden keine Gezeitenzeiten erfunden; das Import-Format ist in `docs/TIDE_ALGORITHM.md`
  beschrieben (Tabelle `tide_event`).
- **Radarabdeckung** im RN-Küstenraum ist real begrenzt; der Mapas-Screen kommuniziert das offen.
- **Kartenlayer** v1: Radar. Die Layer Chuva prevista/Vento/Nuvens aus der Spezifikation folgen.
- **Air-Quality-API** ist noch nicht angebunden (kein Akzeptanzkriterium der v1).
- **Ensemble** nutzt ECMWF-IFS-, GFS- und ICON-Ensembles; UV ist dort nicht verfügbar und kommt
  aus dem Best-Match-Abruf.
- **Sichtbare Texte** sind vollständig pt-BR, aber teilweise als Literale im Code statt in
  String-Resources (Spez. §30) — Refactoring geplant.
- **Lokale Verifikation** startet mit ESTIMATED-Referenz (Best-Match-Kurzfrist); INMET-Stationsdaten
  als OBSERVED-Quelle folgen.
- R8/Shrinking ist bewusst deaktiviert (Zuverlässigkeit vor Grösse); Aktivierung nach On-Device-QA.

## Lizenz- und Attributionshinweise

- Wetterdaten: [Open-Meteo](https://open-meteo.com) (CC BY 4.0, nicht-kommerzielle Nutzung)
- Radar: [RainViewer](https://www.rainviewer.com/api.html)
- Karten: © OpenStreetMap contributors — Attribution wird in der App angezeigt
- Kartenrenderer: [MapLibre](https://maplibre.org) (BSD)

Die App gibt keine Sicherheitsgarantien für Meer oder Navigation. Consulte as autoridades locais.
