# Report di Sessione: Rev MC-26.18 — De-monolitizzazione & Architettura Duale Cadute (Prossimità 1..6 & Lungo Raggio 7..24)

- **Autori**: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity (Senior AI Pair Programmer)
- **Ramo Git**: `feat/dual-fall-safety-subsystem`
- **Data Sessione**: 2026-09-08
- **Framework di Riferimento**: ASTRALIS v3.0.2 (Protocolli 1..6, Protocollo 12 Inner Codex, RRU Disaccoppiato & AVF v26.2-1.19.2)
- **Esito Collaudo**: `[COLLAUDATO CON SUCCESSO AL 100% IN-GAME DA LUCA]`

---

## 🎯 1. Obiettivo & Causa Radice

Nel modulo di sicurezza anticaduta di Minecraft Access, la classe storica `FallDetector.java` (915 righe) risultava un monolite sovraccarico di responsabilità concorrenti: scansione orografica tick-by-tick, calcolo del ciglio, gestione discesa scale a pioli, debouncing e sonificazione diretta.
Inoltre:
1. **Assenza di percezione anticipata su voragini orografiche a lungo raggio**: non esisteva un radar orografico periodico per avvisare di burroni distanti 7..24 blocchi prima di trovarsi a ridosso del ciglio;
2. **Concorrenza acustica tra xilofono e incudine**: in fase di avvicinamento al bordo, i suoni potevano sovrapporsi degradando la chiarezza del segnale;
3. **Attenuazione e mascheramento dell'incudine pre-freno (PRAPI & PRAPI-B)**: l'uso del campione sonoro `SoundEvents.ANVIL_HIT` e del canale audio `SoundSource.BLOCKS` rendeva il suono troppo ovattato e facilmente sovrastato dal parlato della sintesi vocale NVDA durante la deambulazione.

---

## 🛠️ 2. Architettura & File Modificati

1. **Creazione Package Dedicato `features.safety.fall`**:
   - `CentralFallSafetyManager.java`: orchestratore unico su tick client (`ClientPlayingTick.AFTER`), coordinamento tra detector, gestione baluardi di sicurezza, soppressione in AutoWalk e mutua esclusione;
   - `ProximityFallDetector.java`: gestore specializzato corto raggio ($1..6\text{ m}$), articolato su 3 zone discrete:
     * *Zona 1* ($2..6\text{ m}$): pre-allerta xilofono (`NOTE_BLOCK_IRON_XYLOPHONE`) e rallentamento sprint;
     * *Zona 2A* ($1.0..1.5\text{ m}$): pre-freno acustico d'emergenza con incudine metallica (`ANVIL_LAND`) su canale `PLAYERS` e mutua esclusione acustica dallo xilofono;
     * *Zona 2B* ($\le 0.85\text{ m}$): auto-accovacciamento meccanico forzato sul ciglio (`autoSneak`) via `SafetyMovementGuard`.
   - `LongRangeFallDetector.java`: radar orografico periodico ($7..24\text{ m}$), emissione diretta ogni 3.5s con campanella 3D attenuata (`NOTE_BLOCK_BELL`), curva di decadimento lenta ($50\%$) e proiezione vettoriale OpenAL sicura ($[2.5 .. 12.0]\text{ m}$).
2. **`src/main/java/org/mcaccess/minecraftaccess/features/FallDetector.java`**:
   - Svuotato del monolite (ridotto da 915 a ~140 righe) e trasformato in facciata trasparente e retrocompatibile delegando interamente a `CentralFallSafetyManager`.
3. **`src/main/java/org/mcaccess/minecraftaccess/Config.java`**:
   - Nuove opzioni di configurazione per il lungo raggio (`longRangeRadarEnabled`, `longRangeMinDistance = 7`, `longRangeMaxDistance = 24`, `longRangeScanIntervalSeconds = 3.5f`, `longRangeVolumeMultiplier = 0.80f`) con bound discreti rigidi e getter difensivi.
4. **Localizzazioni Bilingue (`it_it.json`, `en_us.json`)**:
   - Aggiunte tutte le chiavi descrittive in rigoroso ordine alfabetico crescente.
5. **Suite di Test Unitari Headless (0 ms)**:
   - `ProximityFallDetectorTest.java`: 7 test unitari per scala a 3 zone, mutua esclusione acustica, discesa scale a pioli, escalation dinamica;
   - `LongRangeFallDetectorTest.java`: 8 test unitari per scansione temporale, raggio, calcolo volume e soppressione;
   - `FallDetectorCognitiveDispatchTest.java` e `SafetyEventFactoryTest.java`: sincronizzati e aggiornati con `ANVIL_LAND` e `SoundSource.PLAYERS`.

---

## 🧪 3. Metriche di Verifica, Telemetria & Collaudo In-Game

- **Test Unitari Headless**: `344/344` test passati (100% verdi, 0 falliti, 0 ignorati).
- **Compilazione**: `BUILD SUCCESSFUL` via `.\gradlew.bat --no-daemon --no-watch-fs shadowJar`.
- **Deploy Proattivo**: Decompresso e verificato il bytecode del `.jar` con `ANVIL_LAND`, deployato in entrambe le istanze di gioco PrismLauncher.
- **Risultati Telemetrici e Collaudo Empirico di Luca**:
  - *Sessione 1 (14:38)*: Verificata de-monolitizzazione e architettura duale;
  - *Sessione 2 (14:58)*: Verificata l'escalation dinamica e il decadimento volume sul lungo raggio;
  - *Sessione 3 (15:12 - 15:16)*: Collaudo definitivo PRAPI-B con `ANVIL_LAND` su bus `PLAYERS`:
    * L'incudine suona squillante, perentoria e nitidissima anche durante il parlato simultaneo di NVDA;
    * L'escalation informativa (6m -> 3m -> 2m xilofono -> 1m incudine -> ciglio auto-sneak) funziona con precisione millimetrica;
    * Nessun falso allarme sulle scale a pioli (discesa sicura riconosciuta);
    * Zero errori, zero ClassCastException e zero warning nei log di gioco.
