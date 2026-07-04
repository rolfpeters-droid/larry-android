# Larry — Android app (Fase 1 / POC)

Native Android-app (Kotlin + Jetpack Compose) die via Tailscale verbindt met Larry's backend
op de Mac mini. Aparte codebase, niet gemengd met de Alto-app.

## Status: Fase 1

Scope bewust verkleind t.o.v. de oorspronkelijke opdracht na API-inventarisatie -- zie
`scope.md` in `1-Projects/Prive/PKA/Android-App/` in de Obsidian-vault voor de volledige
analyse en openstaande vragen. Kort samengevat, **nog niet aanwezig** (Fase 2, wacht op
backend-uitbreiding):

- Streaming responses (backend is synchroon/blokkerend)
- Server-side history-pull (backend heeft geen sessiegeheugen)
- Model-override (Opus/Sonnet/Haiku) en multi-agent-switching
- Cost-tracker

**Wel aanwezig in Fase 1:**

- Chat-interface met lokale geschiedenis (Room), volledig client-side beheerd
- Spraakinvoer (Android SpeechRecognizer) en TTS voor Larry's antwoorden (toggleable)
- Instelbare endpoint (geen hardcoded IP) + optioneel bearer-token
- RP Design System-styling, consistent met Larry's web-UI en de AIOS Mobile PWA

## Build-stappen

Dit project heeft **geen ingecheckte Gradle-wrapper** (bewuste keuze -- geschreven op een
ARM64 Linux-machine zonder Android-toolchain, dus de wrapper-jar kon niet lokaal
gegenereerd/geverifieerd worden). Twee manieren om te bouwen:

### Optie 1 — Android Studio (aanbevolen voor lokaal ontwikkelen)

1. Open dit project in Android Studio (Hedgehog/2023.1 of nieuwer)
2. Android Studio biedt automatisch aan de Gradle-wrapper te genereren/synchroniseren -- accepteer dat
3. Build → Build APK(s)

### Optie 2 — GitHub Actions (aanbevolen voor snel een installeerbare APK)

1. Push deze repo naar GitHub
2. De workflow in `.github/workflows/build.yml` draait automatisch op elke push naar `main`
3. Download de APK als build-artifact (`larry-debug-apk` of `larry-release-apk-unsigned`)
   via de Actions-tab van de run

De release-APK is **ongesigned** -- voor sideload-gebruik op eigen toestel is dat geen
probleem (Android accepteert unsigned debug-builds; voor een unsigned release-APK moet je
'm zelf signen met een debug-keystore, of gewoon de debug-APK gebruiken voor persoonlijk gebruik).

### Optie 3 — lokaal met handmatig geinstalleerde Gradle

```bash
gradle :app:assembleDebug
```

(vereist een lokale Gradle 8.9+ installatie plus Android SDK met `ANDROID_HOME` ingesteld)

## Tailscale-setup checklist (voor gebruik op de Z Fold 7)

- [ ] Tailscale-app geinstalleerd en ingelogd op de Z Fold 7 (zelfde tailnet als de Mac mini)
- [ ] Tailscale-verbinding **actief** voor je Larry opent (de app doet geen fallback naar
      publiek internet -- Tailscale-only per ontwerp)
- [ ] Larry-app geinstalleerd (debug-APK sideloaden: Instellingen → Beveiliging →
      "Onbekende bronnen toestaan" voor de bestandsbeheerder/browser waarmee je installeert)
- [ ] Bij eerste start: ga naar Instellingen (tandwiel-icoon rechtsboven) en vul het endpoint in:
      - Tailscale-hostname: `http://mac-mini-van-rolf:5050/larry`
      - Of het 100.x-adres: `http://100.87.1.16:5050/larry`
      - **Geen slash aan het eind**
- [ ] Test met een kort bericht -- bij een netwerkfout geeft de app een duidelijke melding
      ("Kan Larry niet bereiken. Controleer of Tailscale actief is...")

## Architectuur (kort)

```
MainActivity
  └─ NavHost (chat / settings)
       ├─ ChatScreen + ChatViewModel
       │    ├─ Room (ChatDatabase/ChatDao) -- lokale geschiedenis
       │    └─ LarryApiClient -- OkHttp POST naar {endpoint}/chat
       └─ SettingsScreen
            └─ SettingsStore (DataStore Preferences) -- endpoint, token, TTS-voorkeur
```

Geen dependency-injection framework (Hilt/Koin) -- handmatige lazy-init in
`LarryApplication`, conform "geen overengineering" uit de opdracht.

## Open vragen (zie scope.md voor detail)

1. Streaming: backend-wijziging akkoord, of blijft de niet-streamende ervaring voor de POC?
2. History-pull: is de `/dagboek`-samenvatting (SOP-002) voldoende als "geheugen", of komt
   er een dedicated history-endpoint?
3. Model-override / agent-switching: bestaat dit al ergens, of is dit nieuwe backend-functionaliteit?
4. Cost-tracker: client-side schatting oke, of moet de backend token-usage teruggeven?
