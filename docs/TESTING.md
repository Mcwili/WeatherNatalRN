# Tests

## Unit-Tests (`app/src/test`, `./gradlew test`)

Getestet wird die komplette fachliche Logik (Spez. §35):

| Testklasse | Abdeckung |
|---|---|
| RainClassifierTest | Regenklassen-Grenzen (0.1/1/5/15 mm), Regenindikator inkl. Wahrscheinlichkeits-Gate |
| WeightedStatsTest | gewichteter Median (inkl. Gewichts-Dominanz), Quantile, Leerfälle |
| ConfidenceCalculatorTest | Formelgewichte 40/30/20/10, Fallback ohne Ensemble, Freshness-Verlauf, Ensemble-Stabilität |
| ForecastConsolidatorTest | gewichtete Wahrscheinlichkeit, 60/40-Kombination mit Ensemble, Median vs. Ausreisser, fehlende Modelle, Divergenzwarnung, Confidence-Abfall bei alten Daten |
| TideAnalyzerTest | Extrema-Erkennung (alternierend, Mindestabstand), Richtung steigend/fallend, nächste Ebbe/Flut, UNKNOWN bei zu wenig Daten, Interpolation |
| ModelWeightUpdaterTest | 7-Tage-Sperre, ±5 %-Limit, halbe Stärke bei ESTIMATED, Grenzen 0.40/1.20 |

CI führt `./gradlew test` vor jedem APK-Build aus; bei Fehlschlag werden die
Test-Reports als Artefakt hochgeladen.

## Noch offen (geplant)

- Integrationstests der API-Parser mit aufgezeichneten JSON-Antworten (MockWebServer)
- Compose-UI-Tests der fünf Screens
- On-Device-QA auf realem Gerät (Akzeptanzkriterium 22 — kann nur manuell erfolgen)
