# Architektur

## Überblick

Tempo RN ist eine Single-Module-Compose-App mit Clean-Architecture-Schichten als Pakete.
Die Spezifikation (docs/SPEC.md §1) schlägt eine Multi-Modul-Struktur vor; für v1 wurde bewusst
ein Modul gewählt (schnellere Builds, weniger Konfigurationsfläche). Die Paketgrenzen entsprechen
den vorgeschlagenen Modulen und erlauben eine spätere Extraktion ohne Umbau der Abhängigkeiten.

```
com.tempo.rn
├── core
│   ├── config        ForecastConfig: alle fachlichen Schwellenwerte, zentral (§7–§11, §20)
│   ├── model         reine Datenklassen/Enums, keine Android-Abhängigkeiten
│   ├── network       Retrofit-Interfaces, DTOs, NetworkModule (4 Basis-URLs)
│   └── database      Room: Entities, DAOs, TempoDatabase, DatabaseModule
├── domain            pure Kotlin-Logik, vollständig unit-getestet:
│   │                 ForecastConsolidator, ConfidenceCalculator, RainClassifier,
│   │                 WeightedStats, TideAnalyzer, ModelWeightUpdater
├── data              Repositories (Weather/Marine/Tide/Radar/Location), TimeUtil,
│   │                 SyncScheduler + WorkManager-Worker
├── ui
│   ├── theme         Farbwelt "Horizontlinie" (M3 ColorScheme + TempoColors), Typo
│   ├── icons         ImageVector-Icons (Navigation + mehrfarbige Wetterglyphen)
│   ├── components    TempoCard, ConfidenceChip, Charts (Canvas), OfflineBanner
│   └── nav           TempoApp: Scaffold + NavigationBar + NavHost
└── feature           home / forecast / map / tide / models (je Screen + ViewModel)
```

## Datenfluss

```
API-Daten → Validierung/Parsing (DTOs, tolerant) → Normalisierung (TimeUtil, Einheiten)
→ Room (Quelle der Wahrheit) → Flows → ForecastConsolidator (Modellvergleich, Gewichtung,
Ensemble, Unsicherheit) → ConsolidatedForecast → ViewModel (StateFlow) → Compose
```

- **Room ist die einzige Quelle der Wahrheit.** UI liest nie direkt aus dem Netz.
- Jede Datenquelle wird isoliert abgerufen; Ausfälle einzelner Modelle/Endpunkte
  degradieren die Prognose, statt sie zu verhindern (§33).
- Konsolidierung geschieht lazy im Flow-Combine — Gewichts-Updates oder neue Daten
  triggern automatisch eine Neuberechnung.

## Hintergrundarbeit (WorkManager, §20)

| Worker | Rhythmus | Aufgabe |
|---|---|---|
| ForecastSyncWorker | 60 min + on-demand | Best-Match, 7 Einzelmodelle, Ensemble |
| MarineSyncWorker | 3 h + on-demand | Marine-/Meeresspiegel-Daten |
| VerificationWorker | 24 h | Backtesting: Archiv vs. Kurzfrist-Referenz, Gewichts-Update |

Fehler: `Result.retry()` mit exponentiellem Backoff, max. 3 Versuche, danach `failure`
(bestehende Daten bleiben sichtbar, Datenalter wird angezeigt).

## Backtesting & Gewichte (§6.3, §12, §13)

Bei jedem Abruf werden Prognosen für die Leads 6/12/24/48 h in `forecast_archive` festgehalten.
Der tägliche VerificationWorker vergleicht fällige Archiv-Zeilen mit dem besten verfügbaren
Kurzfristwert (Qualität ESTIMATED → halbe Anpassungsstärke), aktualisiert Trefferquoten und
passt Gewichte an: max. ±5 %/Tag, Grenzen 0.40–1.20, Anpassung erst nach 7 Auswertungstagen.
Alles einsehbar im Modelos-Screen — keine Blackbox.

## Bewusste Abweichungen von der Spezifikation (v1)

| Punkt | Spez. | v1 | Grund |
|---|---|---|---|
| Module | 16 Gradle-Module | 1 Modul, gleiche Paketstruktur | Buildstabilität, Tempo |
| Strings | alle in Resources | teils Literale (pt-BR) | Refactoring geplant |
| Karten-Layer | 5 Layer | Radar | RainViewer zuerst; Rest folgt |
| Air Quality | §3.4 | nicht angebunden | kein v1-Akzeptanzkriterium |
| Beobachtung | INMET etc. | Best-Match (ESTIMATED) | §13-Prioritätenliste, Stufe 5 |
