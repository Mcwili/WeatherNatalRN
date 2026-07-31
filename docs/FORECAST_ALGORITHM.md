# Prognosealgorithmus

Implementiert in `domain/ForecastConsolidator.kt`, `ConfidenceCalculator.kt`,
`RainClassifier.kt`, `WeightedStats.kt`. Alle Schwellenwerte zentral in
`core/config/ForecastConfig.kt`. Kein einfacher Durchschnitt (Spez. §6).

## 1. Regendefinition (§7)

Ein Modell signalisiert Regen, wenn precipitation ≥ 0.1 mm/h — oder wenn die modell-eigene
Wahrscheinlichkeit ≥ 40 % beträgt und precipitation > 0 ist.

Klassen: Sem chuva < 0.1 · Chuva fraca 0.1–1 · moderada 1–5 · forte 5–15 · muito forte ≥ 15 mm/h.

## 2. Regenwahrscheinlichkeit (§8)

```
P_models   = Σ(Gewicht_m × Regenindikator_m) / Σ(Gewicht_m)         (pro Stunde)
P_ensemble = Members_mit_Regen / Members_gültig
P_final    = 0.60 × P_models + 0.40 × P_ensemble    (wenn beide vorhanden)
           = P_models bzw. P_ensemble                (wenn nur eine Quelle)
           = Dienst-Wahrscheinlichkeit des Best-Match (letzter Fallback)
```

Begrenzt auf 0–100 %.

## 3. Regenmenge (§9)

- **Mediana prevista:** gewichteter Median über die Modellwerte (Gewichte s. u.).
  Ein Ausreisser-Modell zieht die Menge nicht hoch (getestet).
- **Faixa provável:** bevorzugt Ensemble-Quantile p25–p75; sonst Quantile über die Modellwerte.
- Zusätzlich Menor/Maior previsão (min/max).

## 4. Confidence Score (§10)

```
confidence = 0.40 × modelAgreement      (Anteil der gewichteten Mehrheitsseite Regen/kein Regen)
           + 0.30 × ensembleStability   (1 / (1 + 2·spread/(p50+1)), spread = p75−p25)
           + 0.20 × historicalSkill     (mittlere lokale Trefferquote; 0.7 solange keine Daten)
           + 0.10 × freshness           (1.0 bis 90 min Datenalter, linear auf 0 bei 360 min)
```

Ohne Ensemble wird dessen Anteil auf modelAgreement umgelegt (fehlende Daten drücken die
Skala nicht künstlich). Kategorien: ≥80 alta · ≥60 moderada · ≥40 baixa · sonst muito baixa.

## 5. Divergenzwarnungen (§11)

Im 24-h-Fenster, Schwellen aus ForecastConfig: Temperaturspanne > 3 °C, Regenmenge > 5 mm/h,
Wind > 15 km/h, Übereinstimmung < 65 %. Meldungen als pt-BR-Texte
("Os modelos divergem sobre a chuva…", "3 de 7 modelos indicam chuva.").

## 6. Modellgewichtung (§6.3) und Lernen (§12/§13)

Startgewichte: ECMWF 1.00 · AIFS 0.95 · ICON 0.90 · GFS 0.85 · UKMO 0.85 · GEM 0.80 · ACCESS-G 0.75.

Täglicher VerificationWorker:
1. Beim Abruf werden Prognosen für Leads 6/12/24/48 h archiviert (`forecast_archive`).
2. Fällige Zeilen werden gegen den besten verfügbaren Kurzfristwert geprüft
   (Best-Match zur Zielstunde; Qualität **ESTIMATED** → halbe Anpassungsstärke, §13).
3. Treffer = Regen-ja/nein stimmt überein. Trefferquote pro Modell wird fortgeschrieben.
4. `ModelWeightUpdater`: Richtung = (Trefferquote − 0.5) × 2; Schritt = ±5 % × Richtung
   (×0.5 bei ESTIMATED); Grenzen 0.40–1.20; Anpassung erst ab 7 Auswertungstagen.

Alle Werte (Gewicht, Trefferquote, Anzahl Auswertungen) sind im Modelos-Screen sichtbar.

## Grenzen

- Verifikationsreferenz ist v1 eine Schätzung (§13-Stufe 5); INMET-Beobachtungen (OBSERVED)
  sind der nächste Ausbauschritt.
- Metriken Brier-Score/MAE/RMSE je Horizontklasse (§12) sind über `forecast_archive`
  berechenbar, aber noch nicht im UI.
