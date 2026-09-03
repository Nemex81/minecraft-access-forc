# Piano Tecnico Implementativo — Fase 3: Migrazione Pilota Dominio Sicurezza (3A FallDetector & 3B ObstacleDetector)

# Incremento Versione Target (AVF): v26.2-1.19.0-dev (Fase 3: Migrazione Pilota Dominio Sicurezza)
**Autore:** Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity  
**Riferimento Standard:** ASTRALIS Framework v2.5.5  
**Ramo di Lavoro:** `feat/cognitive-orchestrator`  
**Stato:** `[FASE 3 — COMPLETATA E COLLAUDATA CON SUCCESSO SUL CAMPO — ARCHIVIATA]`  
**Documenti di Riferimento:**
- `docs/report/REPORT_PASSAGGIO_CONSEGNE_FASE3_SICUREZZA.md`
- `docs/report/RAPPORTO_CHIUSURA_FASE2_E_INDIRIZZO_FASE3_SICUREZZA.md`
- `docs/report/RAPPORTO_CHIUSURA_FASE1_E_INDIRIZZO_FASE2_COGNITIVE_COORDINATOR.md`

---

## 0. Revisione di convalida Codex — correzioni vincolanti

Il piano è coerente nell'impianto, ma viene convalidato solo con le seguenti correzioni, che evitano regressioni percettive e test puramente nominali:

1. Un evento `SOUND_ONLY` conserva un testo localizzato non vuoto nel contratto dati, ma il coordinatore non lo vocalizza per effetto dell'`OutputType`. Non usare stringhe vuote come rappresentazione semantica dell'assenza di voce.
2. Il debounce `SOUND_ONLY` nel fast-path deve aggiornare `recentEvents` dopo una qualunque emissione effettiva (audio o voce), senza cambiare l'ordine storico dell'audio né il micro-burst dei critici vocali.
3. Le suite pure devono verificare un mapper/factory realmente invocato dai rilevatori e un dispatcher con seam controllabili; la sola sottomissione manuale di eventi al coordinatore non verifica il percorso di `FallDetector` o `ObstacleDetector`.
4. A coordinatore attivo, non è sufficiente omettere `CrosshairFeedbackManager.onObstacleDetected(...)` né aggiornare soltanto `lastNarrationTime`: `processCrosshairTick(...)` può narrare subito una **mutazione del bersaglio** durante il movimento prima del ramo di debounce della distanza. Occorre una barriera temporanea, esplicita e stretta per il solo feedback automatico del mirino in movimento. Durante la finestra, il manager deve assorbire silenziosamente sia mutazione sia variazione di distanza, aggiornando il proprio stato per evitare una voce tardiva alla scadenza. Letture manuali, `Alt + V`, e feedback del mirino da fermi restano sempre disponibili.
5. Il testo cognitivo dell'ostacolo deve coincidere con il **testo finale** storicamente pronunciato da `CrosshairFeedbackManager.onObstacleDetected(...)`, non con il solo testo grezzo di `ObstacleDetectionUtils.getNarrationMessage(...)`. Il messaggio storico può infatti aggiungere distanza e contesto del mirino. La composizione sarà estratta in un componente puro, alimentato da una piccola istantanea in sola lettura del contesto del mirino: nessun nuovo produttore vocale deve rimanere nel rilevatore.
6. Non promettere reset su morte, respawn o cambio dimensione: gli hook corrispondenti non sono parte del perimetro della Fase 3. Restano obbligatori soltanto TTL e assenza di riferimenti nulli.
7. Il rollback non può usare `git reset --hard`. Si usa un revert mirato del solo commit di sotto-blocco, previa autorizzazione di Luca.

---

## 🎯 0. Visione d'Insieme & Obiettivi della Fase 3

La Fase 3 costituisce il **primo collaudo verticale e pilota su eventi reali** del Cognitive Coordinator.  
Nelle Fasi 1 e 2 il coordinatore è stato costruito, verificato (22 test unitari a 0 ms) e integrato in Cloth Config senza produttori collegati (buffer vuoto a zero overhead).  
La Fase 3 ha lo scopo di collegare il **Dominio Sicurezza (`SourceDomain.SAFETY`)**, procedendo con la massima cautela attraverso **due sotto-blocchi sequenziali disaccoppiati da un Gate Interno**:

1. **Sotto-Blocco 3A — `FallDetector` (Soli avvisi automatici di caduta imminente) — [STATO: COMPLETATO E CONVALIDATO]**:
   - Migrazione degli avvisi automatici di nuova caduta e dell'edge-bump sul ciglio verso il canale `CognitiveCoordinator.submitEvent(...)` completata nel commit `b25a4eb0345492222e62c90f236ba227f6550d60`;
   - Conservazione rigorosa dell'urgenza storica: priorità `CRITICAL`, Fast-Path a 0 ms, interrupt immediato, soppressione rumori di fondo tramite Scudo Critico (1500 ms);
   - Preservazione del debounce storico a 1500 ms sull'edge-bump verificata tramite test;
   - Fallback legacy deterministico a coordinate dirette quando `cognitiveCoordinatorEnabled = false` verificato.

2. **Sotto-Blocco 3B — `ObstacleDetector` (Soli avvisi automatici di ostacoli sul cammino) — [STATO: TARGET ATTIVO]**:
   - Avvio autorizzato a seguito del collaudo positivo della 3A;
   - Migrazione degli avvisi automatici di ostacolo durante il movimento;
   - Disaccoppiamento controllato da `CrosshairFeedbackManager`: un solo produttore vocale a coordinatore attivo, percorso legacy integrale a coordinatore disattivo;
   - Preservazione assoluta dell'arbitro del mirino (`NarrateCrosshair` NON viene migrato in Fase 3, appartiene alla Fase 4).

### Vincolo Assoluto di Non-Regressione e Conservatività
- Nessuna alterazione delle risposte immediate a comandi espliciti dell'utente da tastiera;
- Nessuna introduzione di messaggi vocali non richiesti (es. divieto di messaggi "Percorso libero" o conferme di uscita dal pericolo in questo pilota);
- Nessuna modifica a mixin, comandi di chat, GUI o altri domini (Mirino, AutoWalk, Waypoint, Mentore rimangono intatti nei loro percorsi storici).

---

## 🧭 1. Analisi dei Componenti Coinvolti & Confini Rigorosi

### 1.1 Componenti Coinvolti nella Fase 3A
- `src/main/java/org/mcaccess/minecraftaccess/features/FallDetector.java`:
  - Punto di emissione automatica: `handleDangerDetected(Player, BlockPos, int, double)`;
  - Filtro a monte invariato: rispetto delle preferenze `config.enabled`, `config.voiceWarning`, `config.playAudioCues`, `config.edgeBumpFeedbackMode`, `config.volume`.
- `src/main/java/org/mcaccess/minecraftaccess/features/cognitive/CognitiveEvent.java`:
  - Introduzione di una factory esplicita e flessibile per la creazione di alert di sicurezza con `OutputType` configurabile (`createSafetyAlert`), evitando stringhe semantiche vuote o output errati per il caso `SOUND_ONLY`.
- `src/main/java/org/mcaccess/minecraftaccess/features/cognitive/CognitiveCoordinator.java`:
  - Micro-perfezionamento nel Fast-Path: registrazione in `recentEvents` della deduplicazione sia per eventi con voce sia per eventi `SOUND_ONLY`, per assicurare il debounce sonoro a 1500 ms anche con voce disattivata.

### 1.2 Componenti Coinvolti nella Fase 3B
- `src/main/java/org/mcaccess/minecraftaccess/features/ObstacleDetector.java`:
  - Punto di emissione automatica nel metodo `tick(...)`;
  - Calcolo geometrico invariato (`ObstacleDetectionUtils.scan(...)`);
  - Emissione condizionale: se coordinatore attivo, costruzione di un evento tramite factory pura e invio a `CognitiveCoordinator.submitEvent(...)`; se disattivo, invocazione dello storico `CrosshairFeedbackManager.onObstacleDetected(...)`.
  - Il messaggio dell'evento attivo conserva la composizione vocale legacy, attraverso un contesto del mirino in sola lettura e senza chiamare un metodo che narri.
- `src/main/java/org/mcaccess/minecraftaccess/features/crosshair/CrosshairFeedbackManager.java`:
  - Trattato come dipendenza di compatibilità: il suo percorso storico resta intatto a coordinatore spento.
  - A coordinatore attivo espone esclusivamente: una istantanea immutabile del contesto utile alla composizione del messaggio e un hook di soppressione del **solo** feed automatico in movimento. Non narra e non entra nell'arbitraggio cognitivo.

### 1.3 Flussi Manuali Rigorosamente Esclusi dalla Migrazione (ZERO CognitiveEvent)
I comandi espliciti e le interazioni dirette dell'utente restano al 100% su `MainClass.narrate(..., true)` e sui cue audio diretti:
1. **Ispezione cadute `Alt + F`** (`FallDetector.inspectNearbyFalls`):
   - Narrazione di buca trovata o nessuna buca vicina;
   - Suono `SoundEvents.NOTE_BLOCK_BELL`;
   - Resta su percorso storico diretto.
2. **Toggle auto-sneak `Ctrl + Alt + F`** (`FallDetector.toggleAutoSneak`):
   - Conferma vocale di attivazione/disattivazione;
   - Suono `SoundEvents.NOTE_BLOCK_PLING` o `NOTE_BLOCK_BASS`;
   - Resta su percorso storico diretto.
3. **Ispezione panoramica ostacoli ed orientamento `Alt + V`** (`ObstacleDetector.inspectObstacle`):
   - Scansione panoramica a 360 gradi, rotazione dello sguardo opzionale e narrazione riassuntiva;
   - Resta su percorso storico diretto.
4. **Scansione ambientale passiva di cadute** (`FallDetector.searchNearbyPositions`):
   - Rintocco periodico discreto `playOnFall` per buche attorno al giocatore;
   - Resta su percorso storico diretto.
5. **Uscita dal pericolo** (`FallDetector.handleDangerCleared`):
   - Ripristino dello sprint e rilascio dello sneak;
   - Zero narrazioni vocali aggiuntive in questo pilota (nessuna frase "Percorso libero").
6. **GUI, menu, schermate, chat e controlli Numpad/tastiera**:
   - Completamente estranei alla Fase 3.

---

## 📊 2. Matrice di Configurazione, Priorità e Output

Il Cognitive Coordinator non decide se una feature debba parlare o suonare: il filtro risiede per intero a monte nei singoli rilevatori.  
La tabella seguente specifica il comportamento deterministico per ogni combinazione:

### 2.1 Matrice di Produzione Segnali
- Se Voce attiva E Cue attivi:
  - `OutputType`: `VOICE_AND_SOUND`
  - Emissione: Testo localizzato storico e `SoundCue` con suono, volume e pitch storici.
- Se Voce attiva E Cue disattivi:
  - `OutputType`: `VOICE_ONLY`
  - Emissione: Testo localizzato storico, nessun cue sonoro emesso.
- Se Voce disattiva E Cue attivi:
  - `OutputType`: `SOUND_ONLY`
  - Emissione: `SoundCue` storico emesso; il testo localizzato resta disponibile nel record ma non viene mai sintetizzato.
- Se Voce disattiva E Cue disattivi (oppure rilevatore disabilitato in config):
  - `OutputType`: Nessun evento generato
  - Emissione: Zero emissioni, zero overhead, nessun oggetto inviato al coordinatore.

### 2.2 Tabella Contrattuale dei Punti Emessi

#### Punto 3A.1 — Nuova Caduta Imminente (`FallDetector.handleDangerDetected`)
- **Condizione Scatenante:** `isNewDanger == true` (nuova posizione di pericolo rilevata sul vettore di cammino).
- **Dominio:** `SourceDomain.SAFETY`
- **Priorità:** `CognitivePriority.CRITICAL` (Fast-Path immediato a 0 ms, interrupt = true).
- **Chiave Semantica:** `"safety.fall.warning"`
- **Firma di Stato (`StateSignature`):**
  - `distanceBucket`: `(int) Math.round(distance)`
  - `severityLevel`: `depth` (profondità della caduta in blocchi)
  - `targetId`: `"fall:warning"`
- **Testo Narrato:** Storico `I18n.get("minecraft_access.fall_detector.warning", relPos, depth)`
- **Target Position:** `dangerPos`
- **Distanza:** `distance`
- **Direzione Spaziale:** `SpatialDirection.FORWARD`
- **Sound Cue:** `SoundCue.of(SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, dangerPos, config.volume, 1.0f)`
- **TTL:** 2000 ms
- **Can Chain:** `false` (i pericoli critici non vengono mai concatenati)

#### Punto 3A.2 — Edge-Bump Ripetuto sul Ciglio (`FallDetector.handleDangerDetected`)
- **Condizione Scatenante:** `!isNewDanger && autoSneakActive && (now - lastEdgeBumpTime >= 1500)`
- **Dominio:** `SourceDomain.SAFETY`
- **Priorità:** `CognitivePriority.CRITICAL` (Fast-Path immediato a 0 ms, interrupt = true).
- **Chiave Semantica:** `"safety.fall.edge_bump"`
- **Firma di Stato (`StateSignature`):**
  - `distanceBucket`: `0` (sul ciglio immediato)
  - `severityLevel`: `depth`
  - `targetId`: `"fall:edge_bump"`
- **Testo Narrato:** Storico `I18n.get("minecraft_access.fall_detector.edge_bump", relPos, depth)`
- **Target Position:** `dangerPos`
- **Distanza:** `distance`
- **Direzione Spaziale:** `SpatialDirection.FORWARD`
- **Sound Cue:** `SoundCue.of(SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, dangerPos, config.volume, 1.0f)`
- **TTL:** 2000 ms
- **Can Chain:** `false`

#### Punto 3B.1 — Ostacolo Superabile (`ObstacleDetector.tick` con `state == STEP_CLIMBABLE`)
- **Condizione Scatenante:** `shouldWarn == true` e `result.state() == ObstacleState.STEP_CLIMBABLE`
- **Dominio:** `SourceDomain.SAFETY`
- **Priorità:** `CognitivePriority.CONTEXTUAL` (arbitrato a fine tick, rank 2).
- **Chiave Semantica:** `"safety.obstacle.step_climbable"`
- **Firma di Stato (`StateSignature`):**
  - `distanceBucket`: calcolato in blocchi
  - `severityLevel`: `1` (gradino superabile)
  - `targetId`: ID del blocco primario se non nullo; altrimenti fallback semantico stabile basato su `result.state().name()`. Il testo localizzato non è mai usato come firma.
- **Testo Narrato:** Il testo finale storico: messaggio grezzo `ObstacleDetectionUtils.getNarrationMessage(...)` composto dal mapper puro con distanza e, se previsto, contesto del mirino, secondo le stesse regole di `CrosshairFeedbackManager.onObstacleDetected(...)`.
- **Target Position:** `result.targetFootPos()`
- **Distanza:** distanza calcolata
- **Direzione Spaziale:** calcolata da `relAngleForNarration` (FORWARD, RIGHT, BACK, LEFT)
- **Sound Cue:** `SoundCue.of(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, soundPos, config.volume, 1.5f)`
- **TTL:** 2500 ms
- **Can Chain:** `false` in Fase 3 (verrà abilitato in Fase 4 per la fusione col mirino)

#### Punto 3B.2 — Ostacolo Bloccante (`ObstacleDetector.tick` con `WALL`, `HEAD_OBSTACLE`, `LOW_CEILING`)
- **Condizione Scatenante:** `shouldWarn == true` e ostacolo bloccante
- **Dominio:** `SourceDomain.SAFETY`
- **Priorità:** `CognitivePriority.CONTEXTUAL` (arbitrato a fine tick, interrupt = false).
- **Chiave Semantica:** `"safety.obstacle.barrier"`
- **Firma di Stato (`StateSignature`):**
  - `distanceBucket`: calcolato in blocchi
  - `severityLevel`: `2` per `LOW_CEILING`, `3` per `HEAD_OBSTACLE`, `4` per `WALL`
  - `targetId`: ID del blocco primario se non nullo; altrimenti fallback semantico stabile basato su `result.state().name()`.
- **Testo Narrato:** Il testo finale storico: messaggio grezzo `ObstacleDetectionUtils.getNarrationMessage(...)` composto dal mapper puro con distanza e, se previsto, contesto del mirino, secondo le stesse regole di `CrosshairFeedbackManager.onObstacleDetected(...)`.
- **Target Position:** `result.lookAtPos() != null ? result.lookAtPos() : result.targetFootPos()`
- **Distanza:** distanza calcolata
- **Direzione Spaziale:** calcolata da `relAngleForNarration`
- **Sound Cue:** `SoundCue.of(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, soundPos, config.volume, 0.6f)`
- **TTL:** 2500 ms
- **Can Chain:** `false`

---

## 🛠️ 3. Elenco Dettagliato File-per-File e Modifiche Previste

### 3.1 `src/main/java/org/mcaccess/minecraftaccess/features/cognitive/CognitiveEvent.java`
- **Responsabilità:** Contratto immutabile degli eventi cognitivi.
- **Modifica Proposta:**
  - Aggiunta del metodo factory `createSafetyAlert`:
    ```java
    import java.util.Objects;

    public static CognitiveEvent createSafetyAlert(
            String semanticKey,
            CognitivePriority priority,
            StateSignature signature,
            String text,
            @Nullable BlockPos targetPos,
            double distance,
            SpatialDirection direction,
            OutputType outputType,
            @Nullable SoundCue soundCue,
            long ttlMillis,
            long timestamp
    ) {
        return new CognitiveEvent(
                SourceDomain.SAFETY,
                priority,
                semanticKey,
                signature,
                Objects.requireNonNull(text, "text"),
                targetPos,
                distance,
                direction,
                outputType,
                soundCue,
                ttlMillis,
                false, // canChain = false per il dominio sicurezza pilota
                timestamp
        );
    }
    ```
  - **Invariante:** Nessuna rottura delle factory storiche esistenti (`createCritical`, `createOperational`, etc.).

### 3.2 `src/main/java/org/mcaccess/minecraftaccess/features/cognitive/CognitiveCoordinator.java`
- **Responsabilità:** Arbitraggio, deduplicazione e dispatching eventi.
- **Modifica Proposta:**
  - Nel metodo `handleCriticalFastPath`: assicurare che `recentEvents.put(key, now)` venga registrato una sola volta quando l'evento non duplicato emette voce **oppure** suono, garantendo il debounce a 1500 ms anche con `SOUND_ONLY`. Il piano esecutivo deve mantenere: calcolo della deduplicazione prima dell'output, audio storico emesso prima della voce, primo critico con `interrupt=true`, secondo critico vocale con `interrupt=false`.
  ```java
  boolean emittedVoice = false;
  if (event.isVoiceEnabled() && !event.narrationText().isBlank()) {
      if (!isDuplicate) {
          if (criticalCountInTick == 0) {
              narrationConsumer.accept(event.narrationText(), true);
          } else {
              narrationConsumer.accept(event.narrationText(), false);
          }
          criticalCountInTick++;
          emittedVoice = true;
      }
  }
  if (!isDuplicate && (emittedVoice || (event.isSoundEnabled() && event.soundCue() != null))) {
      recentEvents.put(key, now);
  }
  ```
  - Aggiungere un test specifico in `CognitiveCoordinatorTest`: due critici `SOUND_ONLY` identici entro finestra producono un solo cue; dopo la finestra il cue è nuovamente emesso.
  - **Invariante:** Correzione puramente logica, retrocompatibile e trasparente.

### 3.3 `src/main/java/org/mcaccess/minecraftaccess/features/FallDetector.java` (Sotto-Blocco 3A)
- **Responsabilità:** Rilevatore automatico di burroni, buche e dislivelli mortali sul vettore di cammino.
- **Modifiche in `handleDangerDetected`:**
  - Creazione del metodo di supporto privato `emitDangerAlert(BlockPos dangerPos, int depth, double distance, boolean isEdgeBump, long now)`;
  - Valutazione della guardia `CognitiveCoordinator.isCoordinatorEnabled()`:
    - **Se `false` (Percorso Legacy Diretto):**
      Esecuzione del codice storico identico:
      - `MainClass.narrate(msg, true)` se voce abilitata;
      - `Minecraft.getInstance().level.playLocalSound(...)` se cue abilitato.
    - **Se `true` (Percorso Cognitivo):**
      - Risoluzione dell'`OutputType` (`VOICE_AND_SOUND`, `VOICE_ONLY`, `SOUND_ONLY`);
      - Creazione di `SoundCue` se suoni attivi;
      - Invocazione di `CognitiveCoordinator.submitEvent(event, now)`.
  - **Invarianti Intoccate:**
    - Logica fisica dei vettori di movimento e raycast look-ahead intatta;
    - Logica di rallentamento corsa (`autoSlowdown`) e accovacciamento forzato (`autoSneakOnEdge`) intatta;
    - Metodi `inspectNearbyFalls`, `toggleAutoSneak`, `searchNearbyPositions` e `handleDangerCleared` intatti e diretti.

### 3.4 `src/main/java/org/mcaccess/minecraftaccess/features/ObstacleDetector.java` (Sotto-Blocco 3B)
- **Responsabilità:** Rilevatore automatico di gradini, muri, soffitti bassi e ostacoli alla testa.
- **Seam di Test Package-Private:**
  - `static java.util.function.Consumer<CognitiveEvent> cognitiveEventConsumer = CognitiveCoordinator::submitEvent;`
  - `@FunctionalInterface interface LegacyObstacleVoiceSink { void accept(ObstacleScanResult result, String message, double relativeAngle); }`
  - `static LegacyObstacleVoiceSink legacyVoiceConsumer = CrosshairFeedbackManager::onObstacleDetected;`
  - `@FunctionalInterface interface LegacyObstacleAudioSink { void accept(Level level, SoundCue cue); }`
  - `static LegacyObstacleAudioSink legacyAudioConsumer = (level, cue) -> { if (cue.soundEvent() != null && cue.position() != null) { level.playLocalSound(cue.position(), cue.soundEvent(), cue.soundSource(), cue.volume(), cue.pitch(), true); } };`
  - **Contratto del cue:** `ObstacleSafetyEventFactory.createSoundCue(...)` restituisce per gli ostacoli un cue con posizione non nulla (`lookAtPos`, altrimenti `targetFootPos`). Il medesimo cue viene consegnato al coordinatore oppure riprodotto sullo stesso `Level` ricevuto dal tick, senza interrogare `Minecraft.getInstance()` nel fallback.
  - `static void resetTestSeams()` per ripristinare i delegati di produzione in `@AfterEach`.
- **Modifiche in `tick`:**
  - Nel blocco `if (shouldWarn)`:
    - Valutazione della guardia `CognitiveCoordinator.isCoordinatorEnabled()`:
      - **Se `false` (Percorso Legacy Diretto):**
        - Esecuzione storica identica tramite seam legacy:
          - `if (config.voiceWarning) legacyVoiceConsumer.accept(result, msg, relAngleForNarration);`
          - `if (config.playAudioCues) { SoundCue cue = ObstacleSafetyEventFactory.createSoundCue(result, config.volume); legacyAudioConsumer.accept(level, cue); }`
      - **Se `true` (Percorso Cognitivo):**
        - Calcolo del messaggio grezzo tramite `ObstacleDetectionUtils.getNarrationMessage(...)`;
        - Calcolo adattatore della distanza intera storica per l'ostacolo: `Math.max(1, (int) Math.round(Math.sqrt(player.distanceToSqr(Vec3.atCenterOf(result.targetFootPos())))))`;
        - Acquisizione dello snapshot immutabile `ObstacleNarrationContext` da `CrosshairFeedbackManager.getNarrationContextSnapshot()`;
        - Invocazione di `ObstacleNarrationComposer.composeFinalNarration(rawMsg, isFrontal, obstacleDistance, context)`: compositore puro privo di riferimenti a Minecraft;
        - Costruzione dell'evento tramite `ObstacleSafetyEventFactory.createObstacleEvent(...)` con mappatura rigorosa di `SpatialDirection` e `OutputType`;
        - Se e solo se l'evento ha il canale voce abilitato (`event.isVoiceEnabled()`), invocazione preventiva di `CrosshairFeedbackManager.suppressAutomaticMovementFeedback(100)`; se `SOUND_ONLY`, nessuna soppressione del mirino;
        - Invio a `cognitiveEventConsumer.accept(event)`;
        - `CrosshairFeedbackManager.onObstacleDetected` **NON viene chiamato**: la voce è prodotta unicamente dal coordinatore.
  - **Invarianti Intoccate:**
    - Metodo `inspectObstacle` (`Alt + V`) intatto su percorso diretto;
    - Metodi geometrici `ObstacleDetectionUtils` intatti;
    - Rilevamento input tastiera e calcolo angoli intatti.

### 3.5 `src/main/java/org/mcaccess/minecraftaccess/features/crosshair/CrosshairFeedbackManager.java` (Sotto-Blocco 3B)
- **Responsabilità:** Coordinatore del mirino e orientamento visuale.
- **Unica Fonte di Verità per la Composizione del Testo:**
  - Estrazione della composizione testuale in `ObstacleNarrationComposer` puro;
  - `CrosshairFeedbackManager.onObstacleDetected(...)` (percorso legacy a coordinatore spento) viene preservato intatto nell'API e delega la costruzione della stringa allo stesso `ObstacleNarrationComposer.composeFinalNarration(...)`, garantendo identità al 100% tra legacy e cognitivo a zero duplicazione.
- **Contesto Read-Only Immutabile:**
  - Introduzione del record `ObstacleNarrationContext(@Nullable String targetNarration, @Nullable Integer targetDistance)`;
  - Metodo accessor `public static ObstacleNarrationContext getNarrationContextSnapshot()`: restituisce una copia immutabile di `currentNarration` e `currentDistance`. Non legge client, non parla, non suona e non altera lo stato.
- **Finestra di Soppressione Automatica Distinta:**
  - Introduzione di un campo dedicato `private static long automaticMovementSuppressedUntil = 0;` (NON riutilizza né altera semanticamente `suppressMovementFeed`, che rimane circoscritto al solo debounce di distanza);
  - Metodo `public static void suppressAutomaticMovementFeedback(long durationMillis)` con aggiornamento monotono:
    `automaticMovementSuppressedUntil = Math.max(automaticMovementSuppressedUntil, clock.getAsLong() + durationMillis);`
- **Metodo Package-Private per Silent Commit e Test Headless:**
  - Estrazione della logica di assorbimento in un metodo package-private, deterministico e direttamente testabile. Il metodo aggiorna intenzionalmente lo stato del manager, quindi non viene qualificato come "puro":
    ```java
    static boolean absorbAutomaticMovementFeedbackIfSuppressed(
            boolean inActiveMovement,
            boolean isTargetMutation,
            boolean isDistanceProgression,
            @Nullable Object target,
            @Nullable String targetName,
            int roundedDistance,
            long now
    ) {
        if (inActiveMovement && now < automaticMovementSuppressedUntil) {
            if (isTargetMutation || isDistanceProgression) {
                currentTarget = target;
                currentNarration = targetName;
                currentDistance = roundedDistance;
                return true;
            }
        }
        return false;
    }
    ```
  - In `processCrosshairTick(...)`, la dichiarazione storica `long now = System.currentTimeMillis()` viene sostituita da `long now = clock.getAsLong()`. Subito dopo il calcolo di `isTargetMutation` e `isDistanceProgression`, e prima del return storico e del ramo di mutazione, avviene la valutazione:
    ```java
    if (absorbAutomaticMovementFeedbackIfSuppressed(
            inActiveMovement,
            isTargetMutation,
            isDistanceProgression,
            target,
            targetName,
            roundedDistance,
            now
    )) {
        return;
    }
    ```
- **Limiti Inviolabili e Protezione Comandi Manuali:**
  - La guardia opera esclusivamente se `inActiveMovement == true`. Da fermi (`inActiveMovement == false`), la soppressione non agisce mai.
  - Nessuna soppressione per `onManualCrosshairRequested` (tasto B), `onCameraRotated` o `inspectObstacle` (`Alt + V`).
- **Seam di Test Package-Private:**
  - `static java.util.function.LongSupplier clock = System::currentTimeMillis;`
  - `static void resetTestSeams()` per il ripristino in `@AfterEach`:
    - `automaticMovementSuppressedUntil = 0;`
    - `currentTarget = null;`
    - `currentNarration = null;`
    - `currentDistance = null;`
    - `lastNarrationTime = 0;`
    - `lastDistanceNarrationTime = 0;`
    - `clock = System::currentTimeMillis;`

---

## 🔬 4. Piano di Test Unitari Puri (Headless, Senza Minecraft)

Tutti i test saranno eseguiti in ambiente headless puro tramite JUnit 5, iniettando delegati per narrazione, audio e timestamp deterministici, senza istanziare client o mondi Minecraft. A questo scopo il piano deve introdurre una `ObstacleSafetyEventFactory` package-private, realmente usata dal rilevatore, e un `ObstacleNarrationComposer` puro. Essi ricevono dati già calcolati, incluso un record immutabile del contesto del mirino, e restituiscono l'evento completo oppure nessun evento; il dispatcher legacy/cognitivo e il clock del manager devono avere seam package-private ripristinati in `@AfterEach`.

### 4.1 Test Suite `SafetyEventFactoryTest.java` e `FallDetectorCognitiveDispatchTest.java` (Nuovi file)
1. **`testNewFallDangerEmitsCriticalFastPathWithSoundAndVoice`**:
   - Configurazione: Voce ON, Suono ON.
   - Costruzione dell'evento tramite il mapper effettivamente invocato dalla 3A a $t = 10000$.
   - Verifica: Narrazione emessa immediatamente (0 ms, interrupt = true), SoundCue emesso con `SoundEvents.ANVIL_HIT`, volume invariato.
2. **`testFallDangerVoiceOnlyProducesNoSoundCue`**:
   - Configurazione: Voce ON, Suono OFF (`OutputType.VOICE_ONLY`).
   - Verifica: Narrazione presente, lista suoni emessi vuota.
3. **`testFallDangerSoundOnlyProducesNoSpokenText`**:
   - Configurazione: Voce OFF, Suono ON (`OutputType.SOUND_ONLY`).
   - Verifica: SoundCue presente, nessuna narrazione emessa (lista vuota, nessuna stringa fantasma).
4. **`testEdgeBumpDebounce1500MsPreserved`**:
   - Sottomissione edge-bump a $t = 10000$: emesso.
   - Sottomissione medesimo edge-bump a $t = 10500$ (< 1500 ms): soppresso come duplicato.
   - Sottomissione medesimo edge-bump a $t = 11600$ (> 1500 ms): emesso regolarmente.
5. **`testCriticalFallSilencesConcurrentNonCriticalEvents`**:
   - Sottomissione contemporanea di caduta critica ed evento esplorativo passivo a $t = 10000$.
   - Verifica: Caduta emessa immediatamente; evento passivo soppresso e scartato; buffer libero da residui.
6. **`testCoordinatorDisabledExecutesDirectLegacyBypass`**:
   - `CognitiveCoordinator.setCoordinatorEnabled(false)`.
   - Invocazione del dispatcher della 3A: il ramo legacy chiama una sola volta i delegate storici di voce e/o cue, senza entrare nei buffer né attivare lo scudo critico.

### 4.2 Test Suite `ObstacleSafetyEventFactoryTest.java` e `ObstacleDetectorCognitiveDispatchTest.java` (Nuovi file)
1. **`testClimbableStepEmitsContextualEvent`**:
   - Sottomissione ostacolo `STEP_CLIMBABLE`.
   - Verifica: Priorità `CONTEXTUAL`, suono `NOTE_BLOCK_PLING`, pitch 1.5f, interrupt = false.
2. **`testUnjumpableWallEmitsContextualEventWithBassSound`**:
   - Sottomissione ostacolo `WALL`.
   - Verifica: Priorità `CONTEXTUAL`, suono `NOTE_BLOCK_BASS`, pitch 0.6f.
3. **`testSpatialDirectionMappingAndBoundarySymmetry`**:
   - Normalizzazione preliminare in `[0°, 360°)`.
   - Verifica intervalli e confini esatti:
     - `[315°, 360°)` e `[0°, 45°)` -> `FORWARD` (test su 0°, 359°, 44.999°, 315.0°);
     - `[45°, 135°)` -> `RIGHT` (test su 45.0°, 90°, 134.999°);
     - `[135°, 225°)` -> `BACK` (test su 135.0°, 180°, 224.999°);
     - `[225°, 315°)` -> `LEFT` (test su 225.0°, 270°, 314.999°).
4. **`testObstacleNarrationMatchesLegacyComposition` (`ObstacleNarrationComposerTest.java`)**:
   - Test headless puro senza Minecraft o Player.
   - Per ostacolo frontale (`isFrontal == true`): parità esatta per distanza 1 blocco (*"Davanti: ostacolo, a 1 blocco"*) e distanza > 1 blocchi (*"Davanti: ostacolo, a %d blocchi"*);
   - Per ostacolo laterale (`isFrontal == false`): con contesto mirino non nullo (*"Sinistra: ostacolo. Davanti: Bersaglio, a X blocchi"*);
   - Per ostacolo laterale con contesto mirino vuoto o nullo: fallback deterministico al solo messaggio grezzo (*"Sinistra: ostacolo"*).
5. **`testSingleNarrationProducerWhenCoordinatorActive`**:
   - Verifica che a coordinatore attivo venga sottomesso un solo evento, `legacyVoiceConsumer` non venga chiamato e il hook di compatibilità non produca voce.
6. **`testAutomaticCrosshairMutationIsSilentlyAbsorbedDuringObstacleWindow`**:
   - Dopo l'attivazione della finestra di 100 ms tramite `suppressAutomaticMovementFeedback`, una mutazione di bersaglio con `inActiveMovement = true` non produce voce; target, narrazione e distanza correnti vengono però aggiornati silenziosamente (*silent commit* tramite `absorbAutomaticMovementFeedbackIfSuppressed`). Alla scadenza non deve comparire alcun annuncio arretrato.
7. **`testSecondSuppressionDoesNotShortenExistingDeadline`**:
   - Attivazione prima soppressione di 100 ms a $t = 1000$ (deadline = 1100).
   - Invocazione seconda soppressione di 50 ms a $t = 1020$: la deadline calcolata con $\max(1100, 1020 + 50 = 1070)$ rimane invariata a 1100.
   - A $t = 1080$ la soppressione risulta ancora attiva e non viene accorciata accidentalmente.
8. **`testManualAndStationaryCrosshairFeedbackRemainAvailableDuringObstacleWindow`**:
   - Durante la medesima finestra, una richiesta manuale del mirino (`B`), rotazione visuale o un cambio bersaglio con `inActiveMovement = false` continuano a narrare secondo il percorso storico a 0 ms.
9. **`testSoundOnlyObstacleDoesNotSuppressAutomaticCrosshairFeedback`**:
   - Con `voiceWarning = false` e cue attivo, viene generato il solo cue cognitivo `SOUND_ONLY` e non viene aperta alcuna finestra di soppressione del mirino.
10. **`testLegacyBypassWhenCoordinatorDisabledAndPlaysIdenticalSoundCue`**:
    - Con `CognitiveCoordinator.setCoordinatorEnabled(false)`, invocazione del dispatcher: `legacyVoiceConsumer` riceve `ObstacleScanResult`, messaggio e angolo; `legacyAudioConsumer` riceve esattamente lo stesso `SoundCue` (stesso suono, pitch, volume e posizione) che la factory avrebbe assegnato all'evento cognitivo.

---

## 🎧 5. Piano di Collaudo Manuale NVDA In-Game (Scenari Concreti per Non Vedenti)

Il collaudo manuale avverrà tramite screen reader NVDA e cuffie stereo, procedendo rigorosamente a due step disaccoppiati:

### 5.1 Collaudo Sotto-Blocco 3A (`FallDetector`)
1. **Scenario 3A.1 — Avvicinamento al ciglio con sprint (Zona 1 -> Zona 2)**:
   - *Azione:* Corsa in avanti verso un burrone profondo.
   - *Aspettativa NVDA:* Interruzione immediata della corsa (slowdown), narrazione chiara e tempestiva dell'avviso di caduta (*"Attenzione, caduta davanti di X blocchi"*), rintocco solenne dell'incudine in cuffia.
   - *Criterio di Successo:* Zero ritardi percepiti, prime sillabe della parola "Attenzione" perfettamente scandite e mai troncate.
2. **Scenario 3A.2 — Edge-Bump e permanenza sul ciglio (Zona 2)**:
   - *Azione:* Pressione continuata del tasto `W` contro il vuoto mentre l'auto-sneak è attivo sul bordo.
   - *Aspettativa NVDA:* Ripetizione ritmata dell'avviso sul ciglio ogni 1500 ms esatti.
   - *Criterio di Successo:* Nessun mitragliamento vocale (chatter), cadenza regolare a 1,5 secondi.
3. **Scenario 3A.3 — Verifica canali separati (Voce OFF / Suono OFF)**:
   - *Azione:* Testare con solo cue sonoro attivo e con solo voce attiva nelle opzioni di FallDetector.
   - *Aspettativa NVDA:* Con solo suono, rintocco incudine 3D perfetto e silenzio vocale assoluto. Con solo voce, avviso parlato nitido e nessun rintocco incudine.
4. **Scenario 3A.4 — Verifica comandi manuali intatti**:
   - *Azione:* Pressione di `Alt + F` (ispezione cadute) e `Ctrl + Alt + F` (toggle auto-sneak).
   - *Aspettativa NVDA:* Risposte vocali immediate con campana e pling invariate.

### 5.2 Checkpoint Interno & Gate 3A -> 3B
- Al termine dei test e del collaudo 3A, presentazione del report di collaudo a Luca.
- **Nessuna riga di codice della 3B verrà toccata prima dell'esplicita conferma di Luca.**

### 5.3 Collaudo Sotto-Blocco 3B (`ObstacleDetector`)
1. **Scenario 3B.1 — Ostacolo superabile in cammino (Gradino/Slab)**:
   - *Azione:* Camminare verso un blocco rialzato di 1 blocco.
   - *Aspettativa NVDA:* Notifica contestuale del gradino saltabile con pling acuto (pitch 1.5).
2. **Scenario 3B.2 — Parete bloccante (Muro frontale)**:
   - *Azione:* Camminare verso una parete di 2 blocchi.
   - *Aspettativa NVDA:* Notifica di muro con rintocco grave di basso (pitch 0.6).
3. **Scenario 3B.3 — Verifica ASSENZA DOPPIA VOCE col Mirino**:
   - *Azione:* Puntare il mirino contro la parete mentre ci si muove verso di essa.
   - *Aspettativa NVDA:* Una e una sola voce per l'ostacolo. Nessuna sovrapposizione tra la notifica dell'ostacolo e il feedback di CrosshairFeedbackManager.
4. **Scenario 3B.4 — Nessun annuncio arretrato del mirino**:
   - *Azione:* Subito dopo un avviso ostacolo, attraversare o ruotare verso un nuovo bersaglio mentre si continua a camminare; attendere oltre 100 ms.
   - *Aspettativa NVDA:* Il nuovo bersaglio non viene narrato in ritardo. Un successivo normale cambio di bersaglio oltre la finestra torna a essere leggibile.
5. **Scenario 3B.5 — Comandi espliciti e feedback da fermi intatti**:
   - *Azione:* Durante la finestra immediata dopo un ostacolo, usare la richiesta manuale del mirino e `Alt + V`; poi fermarsi e cambiare bersaglio.
   - *Aspettativa NVDA:* Tutte e tre le azioni ricevono risposta immediata e storica.
6. **Scenario 3B.6 — Disattivazione Coordinatore da Cloth Config**:
   - *Azione:* Disabilitare `cognitiveCoordinatorEnabled` nel menu e ripetere i test.
   - *Aspettativa NVDA:* Comportamento perfettamente identico alla mod legacy pre-refactor.

---

## 🛡️ 6. Validazione Preventiva secondo i 7 Assi di Qualità ASTRALIS

1. **Validità (Asse 1):** Piena conformità alle specifiche di dominio voxel e architetturali di ASTRALIS v2.5.5; aderenza integrale al rapporto di indirizzo di Fase 3 convalidato.
2. **Efficacia (Asse 2):** Risolve alla radice la competizione vocale del dominio sicurezza, garantendo 0 ms di latenza per la vita del giocatore e preservando la leggibilità per NVDA.
3. **Coerenza (Asse 3):** Preserva al 100% le API e i flussi manuali esistenti (`Alt+F`, `Alt+V`, toggle sneak), integrandosi nei punti automatici storici.
4. **Completezza (Asse 4):** Copre tutti e quattro gli stati della matrice di output (Voce+Suono, Solo Voce, Solo Suono, Spento) e tutti i tipi di pericolo/ostacolo.
5. **Precisione (Asse 5):** Firme di stato semantiche deterministiche con bucket distanza e severità numerica senza deduzioni testuali.
6. **Affidabilità & Prestazioni (Asse 6):** Fast-Path a 0 allocazioni pesanti, zero chiamate a Minecraft nei test unitari, debounce ad anello chiuso.
7. **Assenza di Regressioni (Asse 7):** Fallback legacy a coordinatore spento identico al codice master esistente; zero modifiche a mixin o altri moduli.

---

## 🎲 7. Matrice di Simulazione a 3 Livelli (Stress-Test Logico)

### Livello 1: Scenari Comuni (Happy Path)
- **Scenario 1A:** Giocatore corre verso un precipizio.
  - *Comportamento:* Look-ahead rileva caduta a 1.5 m -> genera evento CRITICAL -> Fast-path emette voce con interrupt + incudine 3D -> scudo critico attivato per 1500 ms -> giocatore rallenta e si ferma. (ESITO: SUCCESSO)
- **Scenario 1B:** Giocatore cammina contro un muretto.
  - *Comportamento:* ObstacleDetector rileva muro a 1 blocco -> genera evento CONTEXTUAL -> flushing a fine tick emette avviso ostacolo con suono di basso -> zero duplicazioni. (ESITO: SUCCESSO)

### Livello 2: Scenari Meno Comuni (Alternative Paths & Concorrenza)
- **Scenario 2A:** Caduta critica rilevata nello stesso tick di un ostacolo laterale.
  - *Comportamento:* La caduta passa su Fast-path a 0 ms e attiva lo Scudo Critico (1500 ms). A fine tick, l'ostacolo laterale (CONTEXTUAL) viene soppresso dallo scudo critico. Nessun disturbo vocale durante l'emergenza. (ESITO: SUCCESSO)
- **Scenario 2B:** Giocatore con solo cue sonoro attivo (`voiceWarning = false`).
  - *Comportamento:* Evento generato con `OUTPUT_TYPE = SOUND_ONLY`. Il coordinatore suona l'incudine, registra l'evento in `recentEvents` ed evita la voce. Al tick successivo (< 1500 ms) il suono è debouncato. Zero stringhe vuote narrate. (ESITO: SUCCESSO)

### Livello 3: Casi Limite (Corner Cases & Boundary)
- **Scenario 3A:** Coordinatore disabilitato a runtime dall'utente in Cloth Config.
  - *Comportamento:* `coordinatorEnabled == false` -> FallDetector e ObstacleDetector eseguono il percorso storico diretto (`MainClass.narrate` e `CrosshairFeedbackManager.onObstacleDetected`). Comportamento identico al 100% alla versione precedente. (ESITO: SUCCESSO)
- **Scenario 3B:** Caduta su terreno sicuro non letale (es. scala a scendere o acqua).
  - *Comportamento:* La geometria voxel di `FallDetector.calculateDangerousDrop` calcola drop = 0 -> nessun pericolo rilevato -> nessun evento inviato al coordinatore -> zero overhead. (ESITO: SUCCESSO)
- **Scenario 3C:** Un evento automatico perde validità prima del flush o prima del suo riuso.
  - *Comportamento:* il TTL impedisce l'emissione tardiva; il piano non aggiunge né assume hook di reset su morte, respawn o cambio dimensione. (ESITO: SUCCESSO)
- **Scenario 3D:** Un ostacolo vocale e una mutazione del bersaglio del mirino avvengono nello stesso tick mentre il giocatore cammina.
  - *Comportamento:* il rilevatore apre la finestra limitata del solo feedback automatico in movimento, il manager assorbe silenziosamente la mutazione aggiornando il proprio stato, e il coordinatore emette una sola voce di sicurezza. Le richieste manuali e il feedback da fermi non sono coinvolti. (ESITO: SUCCESSO)

---

## 🚦 8. Piano Esecutivo in Fasi Atomiche & Protocollo di Rollback

### Passo 1 — Sotto-Fase 1B: Implementazione e Verifica Headless della 3B
1. **Compositore Puro e Snapshot Dati:**
   - Creazione del record immutabile `ObstacleNarrationContext` e della classe pura `ObstacleNarrationComposer` (completamente disaccoppiata da Minecraft e Player);
   - Creazione della suite `ObstacleNarrationComposerTest` (parità letterale del testo per ostacoli frontali a 1 blocco, > 1 blocchi, laterali con e senza bersaglio mirino).
2. **Factory Pura Eventi di Sicurezza Ostacolo:**
   - Creazione di `ObstacleSafetyEventFactory` con normalizzazione preliminare in `[0°, 360°)` e mappatura simmetrica di `SpatialDirection` (`FORWARD`, `RIGHT`, `BACK`, `LEFT`);
   - Creazione della suite `ObstacleSafetyEventFactoryTest` (copertura bordi esatti 45°, 135°, 225°, 315°, severità, sound cue `PLING`/`BASS` e matrice `OutputType`).
3. **Adattamento di `CrosshairFeedbackManager`:**
   - Delega interna di `onObstacleDetected(...)` a `ObstacleNarrationComposer` (singola fonte di verità condivisa);
   - Aggiunta dell'accessor in sola lettura `getNarrationContextSnapshot()`;
   - Introduzione del campo dedicato `automaticMovementSuppressedUntil` e del metodo `suppressAutomaticMovementFeedback(durationMillis)` con aggiornamento monotono `Math.max(...)`;
   - Estrazione del metodo package-private `absorbAutomaticMovementFeedbackIfSuppressed(...)` per eseguire il *silent commit* (`currentTarget`, `currentNarration`, `currentDistance`) e consentire test headless puri al 100%;
   - Integrazione in `processCrosshairTick` all'inizio della valutazione;
   - Creazione della suite `CrosshairMovementSuppressionTest` (verifica soppressione in movimento, silent commit, non-accorciamento deadline con `Math.max`, zero annunci arretrati e comandi manuali/da fermi attivi a 0 ms).
4. **Migrazione `ObstacleDetector.java` e Seam di Test:**
   - Definizione delle interfacce funzionali package-private `LegacyObstacleVoiceSink` e `LegacyObstacleAudioSink`; la seconda riceve `Level` e `SoundCue`, senza riferimenti statici a campi d'istanza né a `Minecraft.getInstance()`;
   - Aggiunta dei seam package-private con reset completo in `@AfterEach`;
   - Calcolo della distanza storica intera nel livello adattatore di `ObstacleDetector` (`Math.max(1, (int) Math.round(Math.sqrt(player.distanceToSqr(Vec3.atCenterOf(result.targetFootPos()))))))`;
   - Dispatch condizionale: a coordinatore attivo, composizione pura, factory, soppressione mirino solo per eventi vocali (`event.isVoiceEnabled()`), e submit al coordinatore (nessuna chiamata a `onObstacleDetected`); a coordinatore disattivo, fallback legacy integrale con lo stesso `SoundCue` della factory;
   - Creazione della suite `ObstacleDetectorCognitiveDispatchTest` (verifica 100% percorsi cognitivo e bypass legacy, unicità del produttore vocale, parità del SoundCue riprodotto).
5. **Verifica Globale e Compilazione:**
   - Esecuzione `.\gradlew.bat --no-daemon test` (attesa suite verde a zero errori headless);
   - Compilazione con `.\gradlew.bat --no-daemon shadowJar`.

### Gate 3B-1 — Rapporto dopo Codice e Test
- Antigravity presenta a Luca il diff, l'esito completo della suite e dell'artefatto compilato.
- **Nessun deploy in PrismLauncher, collaudo NVDA o commit Git è autorizzato da questo gate.** Essi richiedono istruzioni esplicite successive di Luca.

### Passo 2 — Deploy e Collaudo NVDA, Solo dopo Nuova Autorizzazione Esplicita
- Deploy dell'artefatto autorizzato nell'istanza PrismLauncher indicata da Luca.
- Esecuzione con Luca dei sei scenari NVDA della sezione 5.3: assenza doppia voce, assenza annunci arretrati, `Alt+V` e tasto `B` reattivi, parità legacy a coordinatore spento.
- Presentazione di un rapporto di collaudo, senza creare commit.

### Gate 3B-2 — Commit, Solo dopo Collaudo Positivo e Nuova Autorizzazione Esplicita
- Soltanto dopo approvazione di Luca sul rapporto NVDA, creazione di un commit atomico limitato alla 3B su `feat/cognitive-orchestrator`.

### Protocollo di Rollback
In caso di anomalie bloccanti durante il collaudo della 3A o della 3B:
- Identificare il commit atomico del solo sotto-blocco difettoso tramite test e revisione del diff.
- Proporre un revert mirato di quel commit, previa autorizzazione esplicita di Luca; non eseguire reset distruttivi della working tree.
- Ripristinare l'istanza di prova solo tramite il flusso di deploy previsto e dopo conferma dell'artefatto da usare.

---

## 🛑 9. Stop Obbligatorio (Regola 0 — Gating Semantico)

In conformità rigorosa alla **Regola 0 (Default Consultivo Permanente & Dialogo a 2 Tempi)** del Genoma ASTRALIS v2.5.5:

- **Nessuna riga di codice sorgente Java o file di configurazione è stata modificata prima dell'autorizzazione esplicita di Luca per il Passo 1 della 3B.**
- Il presente Piano Tecnico Formale viene congelato e sottoposto alla revisione congiunta di **Luca** e di **ChatGPT**.
- Antigravity attende l'esplicito comando di Luca (es. *"Approvo il Passo 1 della Fase 3B: procedi solo con sorgenti, test e compilazione; nessun deploy o commit"*) prima di compiere qualsiasi azione di scrittura o compilazione.
