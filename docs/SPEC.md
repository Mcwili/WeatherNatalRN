# Entwicklungs-Prompt für Cursor

## Projektname

Tempo RN

## Ziel

Entwickle eine moderne Android-App für präzise lokale Wetter-, Regen-, Wind-, UV-, Wellen-, Radar- und Gezeitenprognosen im Grossraum Natal, Pirangi und der Küstenregion von Rio Grande do Norte, Brasilien.

Die App soll als installierbare APK bereitgestellt werden und ohne Benutzerkonto funktionieren.

Die Benutzeroberfläche muss vollständig in brasilianischem Portugiesisch umgesetzt werden.

Die App soll nicht nur Daten eines einzelnen Wetterdienstes anzeigen. Sie soll mehrere kostenlose Wettermodelle und Datenquellen abrufen, normalisieren, vergleichen und daraus eine konsolidierte Prognose mit Wahrscheinlichkeiten, Unsicherheiten und einem transparenten Vertrauenswert erzeugen.

Die App soll insbesondere folgende Fragen schnell beantworten:

- Vai chover?
- A que horas?
- Quanto pode chover?
- Qual é a confiança da previsão?
- Como estará o vento?
- Qual será o índice UV?
- Como está a maré?
- Quando será a próxima maré baixa?
- Quando será a próxima maré alta?
- Como estão as ondas?

---

# 1. Plattform und technischer Stack

Entwickle eine native Android-App.

Verwende:

- Kotlin
- Jetpack Compose
- Material 3
- Kotlin Coroutines
- Kotlin Flow
- Retrofit
- OkHttp
- Kotlinx Serialization
- Room Database
- WorkManager
- DataStore Preferences
- Hilt Dependency Injection
- MapLibre Native Android
- Coil
- Gradle Kotlin DSL
- Android Studio kompatibles Projekt
- Minimum SDK 26
- aktuelle stabile Target-SDK-Version
- Clean Architecture
- modulare Projektstruktur

Die App muss als signierbare Release-APK gebaut werden können.

Vorgeschlagene Module:

- app
- core-network
- core-database
- core-model
- core-ui
- data-weather
- data-radar
- data-marine
- data-tides
- domain-forecast
- domain-confidence
- feature-home
- feature-forecast
- feature-map
- feature-tides
- feature-model-comparison
- feature-settings

---

# 2. Geografischer Fokus

Die App ist ausschliesslich für Rio Grande do Norte und besonders für die Küstenregion um Natal und Pirangi vorgesehen.

Hauptorte:

## Natal

- Latitude: -5.7945
- Longitude: -35.2110

## Pirangi do Norte

- Latitude: -5.981
- Longitude: -35.109

## Pirangi do Sul

- Latitude: -6.035
- Longitude: -35.105

Weitere auswählbare Orte:

- Ponta Negra
- Via Costeira
- Cotovelo
- Parnamirim
- Búzios
- Tabatinga
- Nísia Floresta
- Pipa
- São Miguel do Gostoso

Standardort:

- Pirangi do Norte

Zeitzone:

- America/Fortaleza

Alle Zeitangaben müssen in lokaler Zeit dargestellt werden.

Eine weltweite Ortssuche ist nicht notwendig.

Optional darf die Standortberechtigung verwendet werden. Die App muss jedoch vollständig ohne Standortfreigabe funktionieren.

---

# 3. Datenquellen

Die erste Version soll ohne kostenpflichtige APIs und ohne zwingende Registrierung funktionieren.

## 3.1 Open-Meteo Forecast API

Verwende Open-Meteo als primäre Wetterdatenquelle.

Rufe nach Möglichkeit separate Modellprognosen ab für:

- ECMWF IFS
- ECMWF AIFS
- NOAA GFS
- DWD ICON Global
- GEM Global
- UK Met Office Global
- BOM ACCESS-G

Die Software muss fehlende oder zeitweise nicht verfügbare Modelle tolerieren.

Stündliche Wetterwerte:

- temperature_2m
- apparent_temperature
- relative_humidity_2m
- dew_point_2m
- precipitation
- rain
- showers
- precipitation_probability
- weather_code
- cloud_cover
- cloud_cover_low
- cloud_cover_mid
- cloud_cover_high
- visibility
- pressure_msl
- surface_pressure
- wind_speed_10m
- wind_direction_10m
- wind_gusts_10m
- uv_index
- uv_index_clear_sky
- sunshine_duration
- cape

Tageswerte:

- weather_code
- temperature_2m_max
- temperature_2m_min
- apparent_temperature_max
- apparent_temperature_min
- sunrise
- sunset
- daylight_duration
- sunshine_duration
- uv_index_max
- precipitation_sum
- rain_sum
- showers_sum
- precipitation_hours
- precipitation_probability_max
- wind_speed_10m_max
- wind_gusts_10m_max
- wind_direction_10m_dominant

Forecast-Horizont:

- Detailansicht: 96 Stunden
- Tagesansicht: 4 Tage
- interner Cache: bis 7 Tage

## 3.2 Open-Meteo Ensemble API

Verwende Ensemble-Daten für:

- precipitation
- rain
- temperature_2m
- cloud_cover
- wind_speed_10m
- wind_gusts_10m
- uv_index

Berechne daraus:

- Regenwahrscheinlichkeit
- Streuung der Regenmenge
- Temperaturunsicherheit
- Windunsicherheit
- Ensemble-Stabilität
- Prognosebandbreiten

## 3.3 Open-Meteo Historical Forecast API

Verwende historische Prognosen zur lokalen Bewertung der Modelle.

Speichere vergangene Modellprognosen und vergleiche sie später mit der besten verfügbaren Beobachtung oder Referenz.

Auswertungszeitraum:

- mindestens 30 Tage
- bevorzugt 90 Tage, sobald genügend Daten vorhanden sind

## 3.4 Open-Meteo Air Quality API

Rufe ab:

- PM2.5
- PM10
- Ozon
- Aerosole
- Staub
- Luftqualitätsindex, sofern verfügbar

Diese Daten beeinflussen nicht direkt den Wetter-Confidence-Score.

## 3.5 Open-Meteo Marine API

Basis-URL:

```text
https://marine-api.open-meteo.com/v1/marine
```

Rufe mindestens ab:

- sea_level_height_msl
- wave_height
- wave_direction
- wave_period
- wind_wave_height
- wind_wave_direction
- wind_wave_period
- swell_wave_height
- swell_wave_direction
- swell_wave_period
- ocean_current_velocity
- ocean_current_direction
- sea_surface_temperature, sofern verfügbar

Parameter:

```text
timezone=America/Fortaleza
forecast_days=7
```

Hinweis:

`sea_level_height_msl` ist kein offizieller nautischer Gezeitenwert. Er basiert auf einem modellierten Meeresspiegel und kann meteorologische Einflüsse enthalten.

## 3.6 RainViewer Weather Maps API

Metadaten-Endpoint:

```text
https://api.rainviewer.com/public/weather-maps.json
```

Verwende RainViewer für animierte Niederschlagskarten.

Funktionen:

- Radarhistorie
- Play und Pause
- Zeitleiste
- Auswahl einzelner Frames
- Transparenzregler
- Zoom
- Verschieben
- Zentrierung auf Natal oder Pirangi
- automatische Aktualisierung
- sichtbarer Zeitstempel pro Frame

Wenn keine Radarabdeckung vorhanden ist, zeige:

```text
Radar meteorológico indisponível ou com cobertura limitada para esta região.
```

RainViewer darf nicht als alleinige Grundlage für Regenprognosen verwendet werden.

## 3.7 OpenStreetMap und MapLibre

Verwende MapLibre Native Android.

Die OpenStreetMap-Attribution muss sichtbar sein:

```text
© OpenStreetMap contributors
```

Kartendienste müssen austauschbar konfiguriert werden.

Die öffentlichen OSM-Tile-Server dürfen nicht für Massendownloads oder aggressives Caching verwendet werden.

## 3.8 DHN Gezeitentabellen

Verwende offizielle Gezeitentabellen der brasilianischen Marine beziehungsweise der DHN als Referenz.

Primäre Referenzstation:

```text
Porto de Natal
```

Die Jahresdaten sollen ausserhalb der App in ein strukturiertes JSON-Format importiert werden.

Beispiel:

```json
{
  "stationId": "porto_natal",
  "stationName": "Porto de Natal",
  "source": "DHN",
  "year": 2026,
  "timezone": "America/Fortaleza",
  "events": [
    {
      "timestamp": "2026-07-31T03:42:00-03:00",
      "type": "LOW",
      "heightMeters": 0.4
    },
    {
      "timestamp": "2026-07-31T09:58:00-03:00",
      "type": "HIGH",
      "heightMeters": 2.1
    }
  ]
}
```

Keine Gezeitenzeiten erfinden.

Wenn offizielle Daten fehlen, zeige:

```text
Os horários oficiais da DHN não estão disponíveis para este período. Exibindo uma estimativa baseada em modelo.
```

---

# 4. API-Registrierung und API-Key-Strategie

Die kostenlose Standardversion soll ohne API-Key funktionieren.

Vorkonfiguration:

```text
Open-Meteo Forecast
requiresRegistration = false
requiresApiKey = false
enabled = true

Open-Meteo Ensemble
requiresRegistration = false
requiresApiKey = false
enabled = true

Open-Meteo Marine
requiresRegistration = false
requiresApiKey = false
enabled = true

Open-Meteo Air Quality
requiresRegistration = false
requiresApiKey = false
enabled = true

RainViewer
requiresRegistration = false
requiresApiKey = false
enabled = true

WorldTides
requiresRegistration = true
requiresApiKey = true
enabled = false
```

WorldTides darf nur als optionale spätere Provider-Integration vorgesehen werden.

API-Keys dürfen niemals direkt in der APK gespeichert werden.

Für spätere geschützte APIs muss folgende Architektur möglich sein:

```text
Android App
→ eigener Backend-Proxy
→ externer API-Anbieter
```

Erstelle eine zentrale Konfiguration:

```kotlin
data class ApiProviderConfig(
    val providerId: String,
    val baseUrl: String,
    val requiresRegistration: Boolean,
    val requiresApiKey: Boolean,
    val enabled: Boolean,
    val commercialUseAllowed: Boolean?,
    val requestLimitDescription: String?,
    val attributionText: String?,
    val privacyPolicyUrl: String?,
    val termsUrl: String?
)
```

---

# 5. Datenverarbeitung

Verarbeitungskette:

```text
API-Daten
→ Validierung
→ Normalisierung
→ zeitliche Ausrichtung
→ Modellvergleich
→ historische Modellgewichtung
→ Ensemble-Auswertung
→ Unsicherheitsanalyse
→ konsolidierte Prognose
→ Speicherung
→ Frontend
```

Gemeinsames internes Modell:

```kotlin
data class NormalizedHourlyForecast(
    val locationId: String,
    val modelId: String,
    val timestamp: Instant,
    val temperatureC: Double?,
    val apparentTemperatureC: Double?,
    val precipitationMm: Double?,
    val rainMm: Double?,
    val precipitationProbability: Double?,
    val cloudCoverPercent: Double?,
    val windSpeedKmh: Double?,
    val windDirectionDegrees: Double?,
    val windGustKmh: Double?,
    val uvIndex: Double?,
    val cape: Double?,
    val fetchedAt: Instant,
    val modelRunTime: Instant?
)
```

---

# 6. Wettermodell-Konsolidierung

Verwende keinen einfachen Durchschnitt.

Die Prognose besteht aus drei Ebenen:

## 6.1 Modellübereinstimmung

Berechne pro Stunde:

- Anzahl Modelle mit Regen
- Abweichung der Regenmenge
- Abweichung beim Regenbeginn
- Temperaturspanne
- Windspanne
- Wolkenbedeckungsspanne

## 6.2 Ensemble-Streuung

Geringe Ensemble-Streuung bedeutet höhere Sicherheit.

Hohe Ensemble-Streuung bedeutet tiefere Sicherheit.

## 6.3 Lokale historische Genauigkeit

Jedes Modell erhält separate Gewichte für:

- Regen ja oder nein
- Regenbeginn
- Regenmenge
- Temperatur
- Wind
- Wolken

Verwende keine globale Einheitsbewertung.

Startgewichtung:

- ECMWF IFS: 1.00
- ECMWF AIFS: 0.95
- ICON Global: 0.90
- GFS: 0.85
- GEM Global: 0.80
- UKMO Global: 0.85
- ACCESS-G: 0.75

Nach mindestens sieben Tagen soll die App die Gewichte schrittweise anpassen.

Regeln:

- maximale tägliche Gewichtsänderung: 5 Prozent
- Mindestgewicht: 0.40
- Maximalgewicht: 1.20

---

# 7. Regendefinition

Ein Modell signalisiert Regen, wenn mindestens eine Bedingung erfüllt ist:

- precipitation mindestens 0.1 mm pro Stunde
- rain mindestens 0.1 mm pro Stunde
- showers mindestens 0.1 mm pro Stunde
- precipitation_probability mindestens 40 Prozent und precipitation grösser als 0

Klassen:

- Sem chuva: unter 0.1 mm
- Chuva fraca: 0.1 bis unter 1.0 mm pro Stunde
- Chuva moderada: 1.0 bis unter 5.0 mm pro Stunde
- Chuva forte: 5.0 bis unter 15.0 mm pro Stunde
- Chuva muito forte: ab 15.0 mm pro Stunde

Alle Grenzwerte müssen zentral konfigurierbar sein.

---

# 8. Regenwahrscheinlichkeit

## 8.1 Modellbasierte Wahrscheinlichkeit

```text
P_models =
Summe aus Modellgewicht × Regenindikator
geteilt durch
Summe aller gültigen Modellgewichte
```

Wenn ein Modell eine eigene Regenwahrscheinlichkeit liefert, verwende diese.

Wenn nicht:

- Regen mindestens 0.1 mm: 1
- kein Regen: 0

## 8.2 Ensemble-Wahrscheinlichkeit

```text
P_ensemble =
Anzahl Ensemble-Mitglieder mit Regen
geteilt durch
Anzahl gültiger Ensemble-Mitglieder
```

## 8.3 Finale Wahrscheinlichkeit

Wenn beide Werte vorhanden sind:

```text
P_final =
0.60 × P_models
+
0.40 × P_ensemble
```

Wenn nur Modelldaten vorhanden sind:

```text
P_final = P_models
```

Begrenze das Resultat auf 0 bis 100 Prozent.

---

# 9. Regenmenge

Verwende einen gewichteten Median.

Zeige:

- Mediana prevista
- Faixa provável
- Menor previsão
- Maior previsão

Verwende bevorzugt Ensemble-Quantile.

Falls nicht verfügbar:

- 25. Perzentil
- 75. Perzentil

Beispiel:

```text
Chuva prevista: 1,8 mm
Faixa provável: 0,6 a 3,4 mm
```

---

# 10. Confidence Score

Berechne pro Stunde einen Score von 0 bis 100.

Gewichtung:

- Modellübereinstimmung: 40 Prozent
- Ensemble-Stabilität: 30 Prozent
- historische Modellqualität: 20 Prozent
- Aktualität der Daten: 10 Prozent

Formel:

```text
confidence =
0.40 × modelAgreement
+
0.30 × ensembleStability
+
0.20 × historicalSkill
+
0.10 × freshness
```

Kategorien:

- 80 bis 100: Confiança alta
- 60 bis 79: Confiança moderada
- 40 bis 59: Confiança baixa
- 0 bis 39: Confiança muito baixa

Erklärung im Frontend:

```text
A confiança indica o nível de concordância entre os modelos, não uma garantia de que a previsão irá ocorrer.
```

---

# 11. Modellabweichungen

Warnungen bei deutlichen Differenzen:

```text
Os modelos divergem sobre a chuva entre 14h e 17h.
```

```text
3 de 7 modelos indicam chuva.
```

```text
Há grande variação na quantidade prevista.
```

Schwellenwerte:

- Temperaturspanne grösser als 3 °C
- Regenmengenspanne grösser als 5 mm pro Stunde
- Windspanne grösser als 15 km/h
- Regenbeginn weicht um mehr als 2 Stunden ab
- Modellübereinstimmung unter 65 Prozent

---

# 12. Lokales Lernen und Backtesting

Keine frei trainierende Blackbox-KI verwenden.

Verwende transparente statistische Kalibrierung.

Speichere:

- Modell
- Modelllauf
- Abrufzeit
- Zielzeit
- Prognosewerte
- konsolidierte Prognose

Metriken:

## Regen

- Brier Score
- Trefferquote
- False Positive Rate
- False Negative Rate
- Precision
- Recall

## Regenmenge

- MAE
- RMSE
- Median Absolute Error

## Temperatur

- MAE
- Bias
- RMSE

## Wind

- MAE
- Bias

Forecast-Horizonte getrennt auswerten:

- 0 bis 6 Stunden
- 7 bis 12 Stunden
- 13 bis 24 Stunden
- 25 bis 48 Stunden
- 49 bis 96 Stunden

---

# 13. Beobachtungsquellen

Priorität:

1. lokale Wetterstation
2. offizielle INMET-Stationsdaten
3. Radar
4. kurzfristige Reanalyse
5. Historical Forecast oder Reanalyse als Ersatz

Qualitätskennzeichnung:

```kotlin
enum class VerificationQuality {
    OBSERVED,
    ESTIMATED
}
```

Bei geschätzten Beobachtungen dürfen Modellgewichte nur halb so stark angepasst werden.

---

# 14. Gezeitenmodul

Zeige pro Tag:

- nächste Ebbe
- nächste Flut
- Wasserhöhe
- aktuelle Gezeitenphase
- steigendes oder fallendes Wasser
- Zeit bis zur nächsten Ebbe oder Flut
- grafische Wasserstandskurve
- Datenqualität
- Referenzstation
- Aktualisierungszeit

Portugiesische Begriffe:

- Maré
- Maré baixa
- Maré alta
- Maré subindo
- Maré descendo
- Próxima maré baixa
- Próxima maré alta
- Altura prevista
- Nível do mar
- Curva da maré
- Dados de referência
- Porto de Natal

Datenarten:

```kotlin
enum class TideDataType {
    OFFICIAL_TIDE_TABLE,
    MODELLED_TIDE,
    MODELLED_SEA_LEVEL,
    ESTIMATED_LOCAL_TIDE
}
```

---

# 15. Lokale Gezeitenkorrektur

Porto de Natal ist die Referenzstation, liegt aber nicht direkt in Pirangi.

Plane konfigurierbare lokale Korrekturen vor:

```kotlin
data class LocalTideCorrection(
    val locationId: String,
    val referenceStationId: String,
    val timeOffsetMinutesLowTide: Int,
    val timeOffsetMinutesHighTide: Int,
    val heightFactor: Double,
    val confidence: CorrectionConfidence,
    val sourceDescription: String
)
```

Startwerte:

```text
timeOffsetMinutesLowTide = 0
timeOffsetMinutesHighTide = 0
heightFactor = 1.0
confidence = UNKNOWN
```

Keine willkürlichen Korrekturen einführen.

---

# 16. Erkennung von Ebbe und Flut

Bei vorhandener Wasserstandskurve:

1. Zeitreihe sortieren
2. Werte validieren
3. kurze Lücken interpolieren
4. vorsichtig glätten
5. Vorzeichenwechsel der ersten Ableitung erkennen
6. lokale Minima und Maxima bestimmen
7. Mindestabstände prüfen
8. mit DHN-Ereignissen vergleichen

Lokales Maximum:

- mögliche Flut

Lokales Minimum:

- mögliche Ebbe

Status:

```kotlin
enum class TideDirection {
    RISING,
    FALLING,
    NEAR_HIGH,
    NEAR_LOW,
    UNKNOWN
}
```

Texte:

- RISING = Maré subindo
- FALLING = Maré descendo
- NEAR_HIGH = Próxima da maré alta
- NEAR_LOW = Próxima da maré baixa
- UNKNOWN = Situação da maré indisponível

---

# 17. Gezeitengrafik

Erstelle eine hochwertige interaktive Kurve.

Elemente:

- 24- oder 48-Stunden-Wasserstandskurve
- Zeitachse
- Höhenachse in Metern
- aktuelle Uhrzeit
- aktueller Punkt
- markierte Hochwasserpunkte
- markierte Niedrigwasserpunkte
- Sonnenaufgang
- Sonnenuntergang
- Tag- und Nachtbereich
- aktuelle Gezeitenrichtung
- Touch-Anzeige exakter Werte
- horizontales Scrollen
- Animation beim Tageswechsel

Beschriftungen:

- Agora
- Maré baixa
- Maré alta
- Altura
- Horário

Die Kurve muss auf echten Daten beruhen.

---

# 18. Datenqualität Gezeiten

```kotlin
enum class TideConfidence {
    HIGH,
    MODERATE,
    LOW,
    UNKNOWN
}
```

Beispiele:

```text
Confiança alta
Horários oficiais disponíveis e boa concordância com o modelo.
```

```text
Confiança moderada
Previsão baseada parcialmente em dados modelados.
```

```text
Confiança baixa
Os dados apresentam diferenças ou cobertura limitada para Pirangi.
```

---

# 19. Küstenstatus

Zeige eine vorsichtige Zusammenfassung:

- Condições tranquilas
- Atenção ao vento
- Ondulação elevada
- Maré muito baixa
- Maré alta
- Dados insuficientes

Berücksichtige:

- Gezeitenphase
- Wasserhöhe
- Wellenhöhe
- Wellenperiode
- Windgeschwindigkeit
- Böen
- Regen
- Datenqualität

Sicherheitshinweis:

```text
As condições podem mudar rapidamente. Consulte as autoridades locais e observe o mar antes de entrar na água.
```

Keine Sicherheitsgarantie ausgeben.

---

# 20. Aktualisierung

Verwende WorkManager.

Rhythmus:

- Forecast: alle 60 Minuten
- RainViewer: alle 10 Minuten bei geöffneter Kartenansicht
- Air Quality: alle 3 Stunden
- Marine-Daten: alle 3 Stunden
- historische Bewertung: täglich
- Modellgewichtung: täglich
- DHN-Daten: jährlich aktualisieren

Bei Fehlern:

- Exponential Backoff
- maximal 3 Wiederholungen
- keine Endlosschleifen
- bestehende Daten weiter anzeigen
- Datenalter anzeigen

Beispiele:

```text
Atualizado às 13:42
```

```text
Dados de 2 horas atrás
```

---

# 21. Offline-Modus

Speichere:

- 96 Stunden Forecast
- 4 Tagesprognosen
- letzte Radar-Metadaten
- Marine-Daten
- Gezeitendaten
- Modellgewichte
- Modellvergleich
- Kartenposition
- gewählter Ort
- letzte Aktualisierungszeit

Offline-Hinweis:

```text
Sem conexão. Exibindo a última previsão disponível.
```

---

# 22. Navigation

Bottom Navigation:

1. Início
2. Previsão
3. Mapas
4. Maré
5. Modelos

Falls fünf Einträge zu eng sind, verschiebe Modelos in ein Untermenü.

---

# 23. Startseite

Zeige:

## Kopfbereich

- Ort
- Datum
- Aktualisierungszeit
- Wettersymbol
- Temperatur
- gefühlte Temperatur
- Beschreibung

Beispiel:

```text
Pirangi do Norte
Parcialmente nublado
27 °C
Sensação de 30 °C
```

## Regenkarte

```text
Vai chover?
```

Beispiel:

```text
62% de chance de chuva nas próximas 3 horas
Confiança moderada
Possível início entre 15h e 16h
```

Bei Unsicherheit:

```text
Previsão incerta. Os modelos apresentam divergência.
```

## Gezeitenkarte

Beispiel:

```text
Maré em Pirangi
Descendo
Próxima maré baixa às 15:48
Altura prevista: 0,5 m
Faltam 1h 36min
```

Zeige eine kompakte 12-Stunden-Kurve.

## Stundenleiste

Nächste 24 Stunden:

- Uhrzeit
- Symbol
- Temperatur
- Regenwahrscheinlichkeit
- Regenmenge
- Wind

## Tageskarten

4 Tage:

- Minimum
- Maximum
- Regenwahrscheinlichkeit
- Regenmenge
- UV
- Wind
- Confidence Score
- Ebbe und Flutzeiten
- Wellenhöhe

---

# 24. Bildschirm Previsão

Zeige 96 Stunden.

Filter:

- Temperatura
- Chuva
- Vento
- Nuvens
- UV

Diagramme:

- Temperatur
- Regenwahrscheinlichkeit
- Regenmenge
- Wind
- Wolken
- UV

Beispiel:

```text
Chance de chuva: 70%
Quantidade prevista: 1,2 mm
Faixa provável: 0,4 a 2,8 mm
```

---

# 25. Bildschirm Mapas

Layer:

- Radar
- Chuva prevista
- Nuvens
- Vento
- Temperatura

Regenkarte:

- Raster für Natal und Pirangi
- ungefähr Latitude -6.30 bis -5.55
- ungefähr Longitude -35.45 bis -34.95
- Rasterabstand ungefähr 0.05 Grad
- gebündelte Requests
- keine hunderte Einzelrequests
- Zeitschritte für 24 Stunden

Windkarte:

- Windpfeile
- Windgeschwindigkeit
- Böen
- Richtung

Jeder Layer braucht eine portugiesische Legende.

---

# 26. Bildschirm Maré

Zeige:

- aktuelle Gezeitenphase
- nächste Ebbe
- nächste Flut
- 24- oder 48-Stunden-Kurve
- 4-Tagesübersicht
- Wellenhöhe
- Wellenperiode
- Wellenrichtung
- Wassertemperatur
- Küstenwind
- Datenquelle
- Datenqualität

---

# 27. Bildschirm Modelos

Zeige pro Stunde:

- ECMWF
- AIFS
- ICON
- GFS
- GEM
- weitere verfügbare Modelle
- konsolidierte Prognose

Vergleich:

- Regenwahrscheinlichkeit
- Regenmenge
- Temperatur
- Wind
- Wolken

Beispiel:

```text
5 de 7 modelos indicam chuva às 16h.
```

Zusätzlich:

- Modellgewicht
- lokale Trefferquote
- Anzahl Auswertungen
- Modelllauf
- Datenalter

Erklärung:

```text
O aplicativo combina diferentes modelos meteorológicos. Modelos com melhor desempenho recente na região recebem um peso um pouco maior.
```

---

# 28. UV-Anzeige

Kategorien:

- 0 bis 2: Baixo
- 3 bis 5: Moderado
- 6 bis 7: Alto
- 8 bis 10: Muito alto
- 11 oder höher: Extremo

Hinweise:

- Use protetor solar.
- Evite exposição prolongada.
- Índice UV extremo.

---

# 29. Design

Das Frontend soll hochwertig, modern und attraktiv sein.

Stil:

- Material 3
- heller und dunkler Modus
- grosse Wetterinformationen
- klare Informationshierarchie
- abgerundete Karten
- dezente Animationen
- sehr gute Lesbarkeit bei Sonne
- grosszügige Abstände
- klare Icons
- hochwertige Diagramme
- maritime und tropische Anmutung
- keine kitschigen Hintergründe
- barrierearme Kontraste
- grosse Touchflächen
- responsives Layout

Die wichtigsten Informationen müssen innerhalb weniger Sekunden erfassbar sein.

---

# 30. Sprache

Alle sichtbaren Texte müssen in brasilianischem Portugiesisch sein.

Alle Texte gehören in Android String Resources.

Keine sichtbaren Strings direkt im Kotlin-Code.

---

# 31. Datenschutz

- kein Benutzerkonto
- keine Werbe-ID
- keine Tracking-SDKs
- keine extern gespeicherten persönlichen Daten
- Standortzugriff optional
- vollständige Funktion ohne Standortfreigabe

---

# 32. Rate Limiting

Implementiere:

- HTTP-Cache
- Room-Cache
- Request-Deduplizierung
- keine identischen parallelen Requests
- Timeouts
- Retry mit Backoff
- User-Agent mit App-Name und Version
- zentrale API-URLs
- austauschbare Provider
- keine geheimen Schlüssel im Code

Interface:

```kotlin
interface RateLimitManager {
    suspend fun canRequest(provider: WeatherProvider): Boolean
    suspend fun recordRequest(provider: WeatherProvider)
    suspend fun nextAllowedRequest(provider: WeatherProvider): Instant
}
```

---

# 33. Fehlerbehandlung

Die App darf nicht abstürzen, wenn:

- ein Modell fehlt
- ein API-Endpunkt ausfällt
- Werte null sind
- Zeitreihen unterschiedliche Längen haben
- Radarframes fehlen
- Daten verspätet sind
- JSON-Felder ergänzt werden
- die Verbindung abbricht

Zeige verständliche portugiesische Fehlermeldungen.

---

# 34. Datenbank

Room-Tabellen mindestens für:

- WeatherForecastEntity
- ModelForecastEntity
- ForecastRunEntity
- ModelWeightEntity
- ModelVerificationEntity
- RadarFrameEntity
- MarineForecastEntity
- TideStationEntity
- TideEventEntity
- TideHeightEntity
- TideForecastRunEntity
- LocalTideCorrectionEntity
- TideSourceMetadataEntity
- AirQualityEntity
- AppLocationEntity

---

# 35. Tests

Unit Tests:

- Regenklassifizierung
- Modellgewichtung
- gewichteter Median
- Regenwahrscheinlichkeit
- Confidence Score
- Zeitachsen-Normalisierung
- fehlende Modelle
- Ausreisser
- Backtesting
- Gezeiten-Minima und Maxima
- lokale Korrekturen
- Datenqualitätslogik

Integration Tests:

- Open-Meteo Forecast
- Open-Meteo Ensemble
- Open-Meteo Marine
- Air Quality
- RainViewer
- Room
- Offline Cache

Compose UI Tests:

- Startseite
- Ortsauswahl
- Stundenprognose
- Karten-Layer
- Gezeitenansicht
- Modellvergleich
- Offline-Modus

---

# 36. Akzeptanzkriterien

Die erste Version ist erfolgreich, wenn:

1. eine installierbare Release-APK erzeugt wird
2. die App ohne Login startet
3. Pirangi do Norte Standardort ist
4. mindestens vier Wettermodelle verglichen werden
5. eine konsolidierte 96-Stunden-Prognose angezeigt wird
6. jede Stunde eine Regenwahrscheinlichkeit hat
7. jede Stunde einen Confidence Score hat
8. Modellabweichungen sichtbar sind
9. Radarframes angezeigt werden, sofern verfügbar
10. bei fehlender Radarabdeckung eine klare Meldung erscheint
11. Wind, UV, Temperatur, Wolken und Regen dargestellt werden
12. Ebbe und Flut für mindestens vier Tage angezeigt werden
13. eine grafische Gezeitenkurve vorhanden ist
14. offizielle und modellierte Gezeitendaten unterschieden werden
15. die Referenzstation sichtbar ist
16. die Gezeiten-Datenqualität angezeigt wird
17. die App offline die letzten Daten zeigt
18. alle sichtbaren Texte brasilianisches Portugiesisch verwenden
19. keine kostenpflichtige API zwingend notwendig ist
20. keine API-Keys im Quellcode stehen
21. Unit Tests für Prognose und Gezeiten vorhanden sind
22. die App auf einem realen Android-Gerät getestet wurde
23. Wind, Wellen und Gezeiten gemeinsam dargestellt werden
24. keine Sicherheitsgarantie für Meer oder Navigation ausgegeben wird
25. eine signierbare Release-Version dokumentiert ist

---

# 37. Umsetzungsetappen

## Etappe 1

- Projektstruktur
- Gradle
- Design-System
- Navigation
- Ortsmodell
- Room
- Open-Meteo Standard-Forecast
- Startseite

## Etappe 2

- separate Wettermodelle
- Normalisierung
- Modellvergleich
- Konsolidierung
- Confidence Score

## Etappe 3

- Ensemble API
- probabilistische Prognose
- Bandbreiten
- Diagramme

## Etappe 4

- MapLibre
- RainViewer
- Karten-Layer
- Zeitleiste

## Etappe 5

- Open-Meteo Marine
- DHN-Import
- Gezeitenlogik
- Gezeitenkurve
- Wellen und Küstenstatus

## Etappe 6

- historisches Backtesting
- lokale Modellgewichtung
- Modellstatistik

## Etappe 7

- Offline-Modus
- Fehlerbehandlung
- Tests
- Performance
- Release-APK
- Dokumentation

---

# 38. Arbeitsweise für Cursor

Arbeite schrittweise und modular.

Bei jedem Schritt:

1. erkläre kurz die zu erstellenden oder zu ändernden Dateien
2. implementiere vollständigen kompilierbaren Code
3. verwende keine offenen TODO-Platzhalter
4. ergänze alle Gradle-Abhängigkeiten
5. prüfe Imports und Paketnamen
6. ergänze Tests
7. führe einen Build aus
8. behebe Buildfehler
9. dokumentiere Annahmen
10. aktualisiere die README

Beginne mit:

- Projektstruktur
- Gradle-Konfiguration
- Domain-Modelle
- API-Interfaces
- Datenbank-Entities
- Repository-Interfaces
- Forecast-Konsolidierungslogik
- Gezeitenlogik
- Unit Tests

Erstelle danach das Frontend.

---

# 39. Projektdokumentation

Erstelle:

- README.md
- ARCHITECTURE.md
- API_SOURCES.md
- FORECAST_ALGORITHM.md
- TIDE_ALGORITHM.md
- PRIVACY.md
- TESTING.md
- RELEASE.md

README-Inhalt:

- Projektziel
- Architektur
- APIs
- API-Einschränkungen
- Build-Anleitung
- APK-Erstellung
- Testanleitung
- Datenmodell
- Prognosealgorithmus
- Confidence-Berechnung
- Gezeitenlogik
- Datenschutz
- bekannte Einschränkungen
- Radar-Abdeckung
- Lizenzhinweise

---

# 40. Fachliche Einschränkungen

Die App darf niemals behaupten, dass eine Wetterprognose sicher eintritt.

Verwende nicht:

```text
Vai chover com certeza.
```

Verwende:

```text
Há alta probabilidade de chuva.
```

```text
Os modelos indicam chuva.
```

```text
A previsão ainda apresenta incerteza.
```

Radar, Satellit, Wettermodell, Ensemble, Marine-Modell und offizielle Gezeitentabelle sind unterschiedliche Datenarten und dürfen nicht gleichgesetzt werden.

Die App soll Unsicherheiten sichtbar machen und verständlich erklären.

Marine- und Gezeitendaten dürfen nicht als Navigationsgarantie oder Sicherheitsfreigabe verwendet werden.
