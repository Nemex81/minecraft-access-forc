# Piano Tecnico Implementativo — Silenziamento Sensoriale Selettivo durante la Navigazione Automatica (AutoWalk Sensory Quieting)

- **Tipologia:** EVOLUTIVO / ACCESSING & DENOISING
- **Autore:** Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity (Senior AI Pair Programmer)
- **Revisori:** Luca / Antigravity
- **Data e Ora:** 2026-09-07
- **Stato Operativo:** [COMPLETATO E CONVALIDATO DA LUCA POST-COLLAUDO CON SUCCESSO]
- **Incremento Versione Target (AVF):** `v26.2-1.19.0-dev` (incluso nella release cumulativa del branch `feat/cognitive-orchestrator`)
- **Piani & Documenti Correlati:**
  * [`docs/strategie/archivio/STRATEGIA_COGNITIVA_SILENZIAMENTO_SENSORIALE_AUTOWALK.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/archivio/STRATEGIA_COGNITIVA_SILENZIAMENTO_SENSORIALE_AUTOWALK.md)
  * [`docs/strategie/STRATEGIA_COGNITIVE_COORDINATOR.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/STRATEGIA_COGNITIVE_COORDINATOR.md)
  * [`docs/report/REGISTRO_REVISIONI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/REGISTRO_REVISIONI.md)
- **Conformità ai 5 Cancelli (Protocollo 12):** Conforme al 100% (Rifiuto patching euristico, Hardware grounding, Hitbox continua, Contratti denominati D1..D6, Determinismo headless a 0 ms)

---

## 🗺️ Sommario Operativo & Registro di Avanzamento (Checklist con Gating di Convalida)

> **Regola Aurea di Avanzamento (Matrice a 3 Stati)**:
> - `- [ ] [DA AVVIARE]`: Attività pianificata ma non ancora iniziata.
> - `- [/] [IMPLEMENTATO — IN ATTESA DI CONVALIDA]`: Codice scritto o intervento completato, ma in attesa di test o collaudo formale (spunta parziale).
> - `- [x] [CONVALIDATO CON SUCCESSO]`: Spunta definitiva concessa **esclusivamente POST-CONVALIDA** (approvazione di Luca per la 1A, test suite 100% verde per la 1B, collaudo pratico in-game di Luca per la Fase 2).

- [x] **1. Contratto D1 — Estensione Configurazione `Config.AutoWalk` & Normalizzazione I18n** [CONVALIDATO CON SUCCESSO]
- [x] **2. Contratto D2 — Query di Stato `isAutoWalkActive()` in `MovementCoordinator`** [CONVALIDATO CON SUCCESSO]
- [x] **3. Contratto D3 — Silenziamento Mirino in `CrosshairFeedbackManager`** [CONVALIDATO CON SUCCESSO]
- [x] **4. Contratto D4 — Silenziamento Ostacoli in `ObstacleDetector`** [CONVALIDATO CON SUCCESSO]
- [x] **5. Contratto D5 — Silenziamento Avvisi Vocali Ciglio in `FallDetector` (Preservazione Fisica e CRITICAL)** [CONVALIDATO CON SUCCESSO]
- [x] **6. Contratto D6 — Suite di Test Unitari Headless (`AutoWalkSensoryQuietingTest`)** [CONVALIDATO CON SUCCESSO]
- [x] **7. Compilazione `shadowJar`, Verifica 0 Regressioni e Deploy Proattivo** [CONVALIDATO CON SUCCESSO]
- [x] **8. Risoluzione Conflitto Tasti Kuma e Guardie Esclusive `ModifierUtils` per `Ctrl+Alt+W` (Toggle Sprint)** [CONVALIDATO CON SUCCESSO]
- [x] **9. Collaudo In-Game di Luca con Esito Positivo e Chiusura Fase 3** [CONVALIDATO CON SUCCESSO]

---

## 0. Decisione Architetturale e Perimetro

Durante la marcia guidata dal pilota automatico (`AutoWalkMotor`), la rotazione automatica dello sguardo e il movimento costante provocano l'attivazione a raffica di tre distinti flussi informativi non richiesti:
1. Feed continuo del mirino che legge ogni blocco intersecato dalla rotazione;
2. Notifiche di ostacoli saltabili o muri che l'AutoWalk sta già affrontando con auto-jump o aggiramento;
3. Notifiche vocali di ciglio o discesa sicura mentre si costeggiano dislivelli.

Questo piano definisce la realizzazione del **Silenziamento Sensoriale Selettivo** a monte, articolato in tre interruttori dedicati (tutti attivi di default a `true`), salvaguardando incondizionatamente:
- L'auto-accovacciamento fisico fail-safe (`SafetyMovementGuard`);
- Il Fast-Path immediato a latenza zero per pericoli letali `CRITICAL` (lava, fuoco, caduta nel vuoto);
- L'interrogazione manuale esplicita tramite tasto `B`, centramento `5`/`M` e lock POI `X` (`DirectInteractionShield`).

---

## 1. Contratti Tecnici D1..D6

### Contratto D1 — Estensione Configurazione `Config.AutoWalk` & Normalizzazione I18n
1. In `src/main/java/org/mcaccess/minecraftaccess/Config.java` all'interno della classe `AutoWalk`:
   ```java
   public boolean silenceCrosshairDuringWalk = true;
   public boolean silenceObstaclesDuringWalk = true;
   public boolean silenceFallWarningsDuringWalk = true;
   ```
2. Normalizzazione descrittiva e tooltip per l'opzione storica `voiceFeedback`:
   - Rinominata per NVDA in *"Feedback vocale all'arrivo"* con tooltip descrittivo;
3. Aggiornamento in ordine alfabetico crescente dei file di lingua:
   - `src/main/resources/assets/minecraft_access/lang/it_it.json`
   - `src/main/resources/assets/minecraft_access/lang/en_us.json`
   - Conformità rigorosa al linter CI (`jq -e "keys != keys_unsorted"`).

### Contratto D2 — Query di Stato `isAutoWalkActive()` in `MovementCoordinator`
1. In `MovementCoordinator.java`, esposizione di un metodo statico pulito in sola lettura:
   ```java
   private static @Nullable MovementCoordinator activeInstance = null;
   private static @Nullable Boolean testAutoWalkActive = null;

   public static boolean isAutoWalkActive() {
       if (testAutoWalkActive != null) return testAutoWalkActive;
       return activeInstance != null && activeInstance.isActive();
   }

   public static void setTestAutoWalkActive(@Nullable Boolean active) {
       testAutoWalkActive = active;
   }
   ```
2. Registrazione dell'istanza attiva nel costruttore o in `start()` / `cancel()` per garantire consistenza deterministica a costo zero (zero iterazioni, zero locking).

### Contratto D3 — Silenziamento Mirino in `CrosshairFeedbackManager`
1. Introduzione della funzione pura di decisione:
   ```java
   public static boolean shouldSilenceCrosshair(boolean isAutoWalkActive, boolean silenceConfig) {
       return isAutoWalkActive && silenceConfig;
   }
   ```
2. In `CrosshairFeedbackManager.processCrosshairTick`:
   - All'inizio del tick del mirino, se `shouldSilenceCrosshair(MovementCoordinator.isAutoWalkActive(), config.silenceCrosshairDuringWalk)` è vero:
     - Aggiorna i puntatori di stato interni (`currentTarget`, `currentNarration`) per evitare salti alla ripresa;
     - Esegue `return;` immediato prima di qualsiasi formattazione vocale o sottomissione al `CognitiveCoordinator`.
   - **Tutela Assoluta Comandi Manuali**: I metodi `narrateTarget()` (tasto `B`) e `centerHorizonAndTarget()` bypassano questo controllo grazie al `DirectInteractionShield`, mantenendo vocalizzazione istantanea.

### Contratto D4 — Silenziamento Ostacoli in `ObstacleDetector`
1. Introduzione della funzione pura di decisione:
   ```java
   public static boolean shouldSilenceObstacles(boolean isAutoWalkActive, boolean silenceConfig) {
       return isAutoWalkActive && silenceConfig;
   }
   ```
2. In `ObstacleDetector.tick()` / `checkObstacles()`:
   - Se `shouldSilenceObstacles(MovementCoordinator.isAutoWalkActive(), config.silenceObstaclesDuringWalk)` è vero:
     - Sopprime la generazione e l'emissione vocale e acustica ordinaria degli ostacoli;
     - L'ispezione panoramica manuale (`Alt+V`) rimane pienamente accessibile su richiesta esplicita.
3. **Tutela Watchdog AutoWalk**: Se il bot sbatte su un ostacolo imprevisto e si ferma per oltre 3 secondi (60 tick), `AutoWalkMotor` emette il proprio evento specifico `autowalk:stuck` (*"Percorso ostruito, marcia arrestata"*), garantendo che Luca sia sempre informato del blocco reale.

### Contratto D5 — Silenziamento Avvisi Vocali Ciglio in `FallDetector`
1. Introduzione della funzione pura di decisione:
   ```java
   public static boolean shouldSilenceFallVoiceWarnings(boolean isAutoWalkActive, boolean silenceConfig) {
       return isAutoWalkActive && silenceConfig;
   }
   ```
2. In `FallDetector.tick()`:
   - Se `shouldSilenceFallVoiceWarnings(MovementCoordinator.isAutoWalkActive(), config.silenceFallWarningsDuringWalk)` è vero:
     - Sopprime unicamente l'annuncio vocale del ciglio (`edgeBump`) e la vocalizzazione della discesa sicura.
3. **INVARIANTI INVIOLABILI DI SICUREZZA FISICA**:
   - `SafetyMovementGuard.engageFallProtection()` (l'auto-sneak fisico) rimane pienamente vigile ed operativo: se un blocco crolla sotto i piedi, il bot si accovaccia immediatamente;
   - Gli allarmi `CRITICAL` (lava, fuoco, burrone profondo con caduta in corso) viaggiano sul Fast-Path a latenza zero (< 1 ms) e NON vengono mai silenziati.

### Contratto D6 — Test Unitari Headless a 0 ms (`AutoWalkSensoryQuietingTest`)
1. Creazione di una nuova classe di test JUnit `src/test/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkSensoryQuietingTest.java`:
   - Test 1: `testCrosshairSilencedWhenAutoWalkActiveAndConfigTrue()`
   - Test 2: `testCrosshairActiveWhenAutoWalkActiveAndConfigFalse()`
   - Test 3: `testCrosshairActiveWhenAutoWalkInactive()`
   - Test 4: `testObstaclesSilencedWhenAutoWalkActiveAndConfigTrue()`
   - Test 5: `testFallVoiceWarningsSilencedWhenAutoWalkActive()`
   - Test 6: `testPhysicalSafetyCrouchPreservedDuringAutoWalk()`
   - Test 7: `testCriticalSafetyAlarmsBypassQuieting()`
   - Test 8: `testDefaultConfigValuesAreAllTrue()`

---

## 2. Inventario dei File Coinvolti

### 2.1 File da Modificare
- `src/main/java/org/mcaccess/minecraftaccess/Config.java`: aggiunta dei 3 campi booleani in `AutoWalk` con default `true`;
- `src/main/resources/assets/minecraft_access/lang/it_it.json`: aggiunta etichette e tooltip in ordine alfabetico;
- `src/main/resources/assets/minecraft_access/lang/en_us.json`: aggiunta etichette e tooltip in ordine alfabetico;
- `src/main/java/org/mcaccess/minecraftaccess/features/autowalk/MovementCoordinator.java`: gestione `isAutoWalkActive()` e tracciamento istanza;
- `src/main/java/org/mcaccess/minecraftaccess/features/crosshair/CrosshairFeedbackManager.java`: integrazione filtro `shouldSilenceCrosshair`;
- `src/main/java/org/mcaccess/minecraftaccess/features/ObstacleDetector.java`: integrazione filtro `shouldSilenceObstacles`;
- `src/main/java/org/mcaccess/minecraftaccess/features/FallDetector.java`: integrazione filtro `shouldSilenceFallVoiceWarnings`.

### 2.2 Nuovi File
- `src/test/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkSensoryQuietingTest.java`: suite di test unitari a 0 ms.

### 2.3 File Rigorosamente CONGELATI (Zero Modifiche)
- `AutoWalkPathfinder.java`: il motore di ricerca topologica A* e la geometria voxel rimangono intoccati;
- `SafetyMovementGuard.java`: l'attuatore fisico dello sneak rimane intoccato e protetto al 100%;
- `CognitiveCoordinator.java`: la macchina di arbitraggio centralizzata e la coda a fine tick rimangono inalterate.

---

## 3. Piano di Test e Verifica

1. **Test Automatici (`gradlew test`)**:
   - Esecuzione della nuova suite `AutoWalkSensoryQuietingTest` (8 test nuovi);
   - Esecuzione dell'intera suite del progetto (tutti i 299 test pregressi devono restare verdi, totale atteso: 307 test verdi, 0 fallimenti).
2. **Build Pulita (`shadowJar`)**:
   - `.\gradlew.bat --no-daemon shadowJar`;
   - Verifica assenza di warning o errori di checkstyle e I18n.
3. **Deploy Proattivo**:
   - Copia automatica del `.jar` compilato nelle istanze attive di PrismLauncher prima del collaudo in-game.
4. **Collaudo Manuale con NVDA**:
   - Avvio di AutoWalk verso un waypoint distante nella Tenuta:
     - Verificare che il mirino non parli durante la marcia;
     - Verificare che salendo gradini non vengano annunciati ostacoli;
     - Verificare che i passi e i nodi sonori siano nitidi e puliti;
     - Premere `B` durante la marcia per confermare la risposta manuale immediata;
     - All'arrivo alla meta, verificare la corretta vocalizzazione *"Arrivato a destinazione"*.

---

## 4. Criteri di Accettazione Finali

- La marcia in AutoWalk risulta silenziosa e priva di chatter verbale;
- I 3 interruttori in Cloth Config risultano attivi di default e perfettamente commutabili;
- L'opzione `voiceFeedback` è chiara e non ambigua;
- La protezione fisica anticaduta interviene regolarmente in presenza di burroni;
- Tutta la suite di test è verde al 100% (`0 failure, 0 error`).

---

## 5. Checkpoint Vincolante (Regola 0)

Questo documento costituisce il **Piano Tecnico Formale (Sotto-Fase 1A)**.
In conformità al Principio del Dialogo a 2 Tempi e al Gating Semantico, **nessun file sorgente Java o file di build è stato modificato**.
La Sotto-Fase 1B (scrittura codice e test) inizierà esclusivamente dopo l'approvazione esplicita di Luca.
