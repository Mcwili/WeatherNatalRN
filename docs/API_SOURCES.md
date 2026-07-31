# Datenquellen

Alle Quellen der v1 sind kostenlos, ohne Registrierung und ohne API-Key (Spez. §4).
Es liegen keine Schlüssel im Code oder in der APK.

| Provider | Basis-URL | Zweck | Registrierung/Key | Kommerziell |
|---|---|---|---|---|
| Open-Meteo Forecast | https://api.open-meteo.com/v1/forecast | Best-Match + 7 Einzelmodelle | nein/nein | nein (CC BY 4.0, non-commercial free tier) |
| Open-Meteo Ensemble | https://ensemble-api.open-meteo.com/v1/ensemble | Wahrscheinlichkeiten, Quantile | nein/nein | nein |
| Open-Meteo Marine | https://marine-api.open-meteo.com/v1/marine | Wellen, Meeresspiegel (`cell_selection=sea`) | nein/nein | nein |
| RainViewer | https://api.rainviewer.com/public/weather-maps.json | Radar-Frames (Vergangenheit + Nowcast) | nein/nein | eingeschränkt |
| OpenStreetMap | https://tile.openstreetmap.org | Basiskarte (Raster) | nein/nein | Tile-Usage-Policy beachten |
| DHN (Marinha do Brasil) | Jahres-Gezeitentabellen, Import ausserhalb der App | offizielle Gezeiten Porto de Natal | — | öffentlich |
| WorldTides (optional, deaktiviert) | — | spätere offizielle Gezeiten-API | ja/ja | kostenpflichtig |

## Abgerufene Variablen

- **Best-Match stündlich:** temperature_2m, apparent_temperature, relative_humidity_2m,
  precipitation, precipitation_probability, weather_code, cloud_cover, wind_speed_10m,
  wind_direction_10m, wind_gusts_10m, uv_index
- **Best-Match täglich:** weather_code, t_max/min, precipitation_sum, precipitation_probability_max,
  uv_index_max, wind max/gusts, sunrise, sunset
- **Pro Modell stündlich** (ecmwf_ifs025, ecmwf_aifs025, icon_seamless, gfs_seamless,
  ukmo_seamless, gem_seamless, bom_access_global): temperature_2m, precipitation,
  wind_speed_10m, wind_gusts_10m, wind_direction_10m, cloud_cover
- **Ensemble** (ecmwf_ifs025, gfs_seamless, icon_seamless): precipitation, temperature_2m,
  wind_speed_10m — Member-Spalten werden zu p25/p50/p75/min/max + Regenanteil verdichtet
- **Marine:** sea_level_height_msl, wave_height/direction/period, wind_wave_height,
  swell_wave_height/direction/period, sea_surface_temperature

## Schutzmechanismen (Spez. §32)

- User-Agent `TempoRN/1.0.0`, Timeouts 20/30 s, WorkManager-Backoff (max. 3 Retries)
- Room-Cache: UI funktioniert vollständig offline mit letzten Daten
- Zeitzone aller Abrufe: `America/Fortaleza`
- OSM-Tiles: nur Live-Ansicht im Mapas-Screen, kein Massen-Prefetch
- Fehlende Modelle/Felder werden toleriert (nullable DTOs, `ignoreUnknownKeys`)

## Hinweis zu `sea_level_height_msl`

Kein offizieller nautischer Gezeitenwert: modellierter Meeresspiegel inkl. meteorologischer
Anteile. Die App kennzeichnet diesen Modus sichtbar (`MODELLED_SEA_LEVEL`, Confiança moderada)
und zeigt den DHN-Hinweistext, solange keine offiziellen Tabellen importiert sind.
