# Release & Signierung

## Build

```bash
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk (signiert, installierbar)
```

CI (`.github/workflows/android-build.yml`) baut bei jedem Push auf den Entwicklungsbranch:
Unit-Tests → Release-APK → Upload als Workflow-Artefakt **und** Veröffentlichung als
`TempoRN-v1.0.0.apk` auf dem Branch `apk-dist`.

## Signierung

Die APK wird mit dem eingecheckten Keystore `signing/temporn-release.keystore` signiert
(Alias `temporn`, Store-/Key-Passwort `temporn-release`, RSA 2048, gültig 30 Jahre).

**Bewusste Entscheidung für v1:** Der Keystore liegt im Repository, damit jeder Build —
lokal wie CI — dieselbe Signatur trägt und Updates ohne Neuinstallation möglich sind.
Das ist für eine private, nicht über einen Store vertriebene App akzeptabel, aber
**nicht Play-Store-tauglich**. Für eine Veröffentlichung: neuen, geheimen Keystore erzeugen,
als CI-Secret hinterlegen und die `signingConfig` auf Umgebungsvariablen umstellen.
API-Keys enthält die App keine (Spez. §4 bleibt erfüllt — das Signierzertifikat ist kein
Zugangsgeheimnis zu Diensten).

## Installation auf dem Gerät

1. APK aufs Telefon übertragen (Download/USB/Cloud)
2. "Installation aus unbekannten Quellen" für den verwendeten Dateimanager/Browser erlauben
3. APK antippen und installieren — die App startet ohne Login mit Pirangi do Norte
4. Updates: neue APK einfach darüber installieren (gleiche Signatur)

## Versionierung

`versionName`/`versionCode` in `app/build.gradle.kts`. Für jede Auslieferung
`versionCode` erhöhen, sonst verweigert Android das Update.
