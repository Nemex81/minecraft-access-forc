# Piano Tecnico Formale — Rev MC-26.19: Quiete Sensoriale AutoWalk & Navigatore, Verbosità di Progressione & Silenziamento Mirino (ASTRALIS v3.0.4)

- **Tipologia**: IMPLEMENTATIVO / CORRETTIVO (Rev MC-26.19)
- **Autore**: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity (Senior AI Pair Programmer)
- **Revisori**: Luca / Antigravity
- **Data e Ora**: 2026-09-08T17:05:00+02:00
- **Ramo Git di Riferimento**: `feat/cognitive-orchestrator`
- **Incremento Versione Target (AVF)**: `26.2-1.19.3`
- **Stato Operativo**: `[PIANO TECNICO COMPLETATO E COLLAUDATO CON SUCCESSO DA LUCA AL 100%]`
- **Documenti Correlati**:
  * [`docs/strategie/archiviate/STRATEGIA_QUIETE_SENSORIALE_AUTOWALK_E_NAVIGATORE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/archiviate/STRATEGIA_QUIETE_SENSORIALE_AUTOWALK_E_NAVIGATORE.md)
  * [`docs/report/archivio/REPORT_SESSIONE_REV_MC-26.19_QUIETE_SENSORIALE_AUTOWALK_E_NAVIGATORE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_REV_MC-26.19_QUIETE_SENSORIALE_AUTOWALK_E_NAVIGATORE.md)
  * [`docs/report/ARCHIVIO_REVISIONI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/ARCHIVIO_REVISIONI.md)
  * [`src/main/java/org/mcaccess/minecraftaccess/Config.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/Config.java)
  * [`src/main/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkMotor.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkMotor.java)
  * [`src/main/java/org/mcaccess/minecraftaccess/features/autowalk/MovementCoordinator.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/autowalk/MovementCoordinator.java)
  * [`src/main/java/org/mcaccess/minecraftaccess/features/NarrateCrosshair.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/NarrateCrosshair.java)
  * [`src/main/java/org/mcaccess/minecraftaccess/features/ObstacleDetector.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/ObstacleDetector.java)
  * [`src/main/java/org/mcaccess/minecraftaccess/features/safety/fall/ProximityFallDetector.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/safety/fall/ProximityFallDetector.java)

---

## 🎯 1. Visione d'Insieme & Obiettivo Tecnico

Questo piano traduce in contratti software atomici e rigorosi la **Strategia di Quiete Sensoriale per l'AutoWalk e il Navigatore (Rev MC-26.19)**. L'obiettivo è conseguire un'esperienza di deambulazione automatica pulita, ordinata ed elegante per lo screen reader NVDA, analogamente a quanto realizzato per il rilevatore di cadute duale (`CentralFallSafetyManager` nella Rev MC-26.18), preservando al contempo le abitudini di telemetria vocale amate da Luca.

### 1.1 Sintesi degli Interventi
1. **Verbosità di Progressione Regolabile**: Disaccoppiare la notifica dei passi rimanenti da `narrateHints` globale e incapsularla in un enum dedicato `ProgressionFeedbackMode` in `Config.AutoWalk` (con default `SOUND_AND_VOICE` fedele all'esperienza storica di Luca);
2. **Silenziamento dell'Arpa di Quota del Mirino**: Sopprimere `playRelativePositionSoundCue` in `NarrateCrosshair` durante la marcia se `silenceCrosshairDuringWalk` è abilitato, evitando arpeggi continui durante la sterzata in curva;
3. **Tutela Inviolabile dell'Interruttore Master**: Demarcazione formale tra Livello 0 (`enabled` di modulo) e Livello 1 (filtri contestuali di marcia);
4. **Armonizzazione Ostacoli**: Verifica della quiete per gli ostacoli superabili ordinari e mantenimento della reattività su stallo e comandi manuali (`U`);
5. **Silenziamento del Cue Acustico Anticaduta (Incudine)**: Soppressione del pre-freno acustico d'emergenza (`ANVIL_LAND`) in `ProximityFallDetector` durante l'AutoWalk quando `silenceFallWarningsDuringWalk` è abilitato, evitando falsi allarmi su cigli sicuri e scarpate costeggiate dalla rotta A*.

---

## 📦 2. Contratti Denominati di Implementazione (Inner Codex Pattern — Cancello 4)

### Contratto D0 — Presidio Inviolabile degli Interruttori Master di Modulo (Livello 0)
- **Oggetto**: Tutela degli switch `config.*.enabled`.
- **Invariante**:
  - Se `config.fallDetector.enabled == false`, il sottosistema cadute è completamente inattivo;
  - Se `config.obstacleDetector.enabled == false`, il rilevatore ostacoli è completamente inattivo;
  - Se `config.autoWalk.enabled == false`, l'AutoWalk è completamente inattivo.
- **Regola di Non-Interferenza**: Nessun flag di marcia (`silenceCrosshairDuringWalk`, `silenceObstaclesDuringWalk`, `silenceFallWarningsDuringWalk`) può essere letto o applicato se l'interruttore master del rispettivo modulo è disabilitato.

### Contratto D1 — Enum `ProgressionFeedbackMode`, Configurazione & Rigore I18N
- **File Coinvolti**:
  * [`src/main/java/org/mcaccess/minecraftaccess/Config.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/Config.java)
  * [`src/main/resources/assets/minecraft_access/lang/it_it.json`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/resources/assets/minecraft_access/lang/it_it.json)
  * [`src/main/resources/assets/minecraft_access/lang/en_us.json`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/resources/assets/minecraft_access/lang/en_us.json)
- **Definizione Enum**:
  ```java
  public enum ProgressionFeedbackMode {
      SOUND_AND_VOICE,
      SOUND_ONLY,
      VOICE_ONLY,
      OFF
  }
  ```
- **Aggiunta in `Config.AutoWalk`**:
  ```java
  @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
  public ProgressionFeedbackMode progressionFeedbackMode = ProgressionFeedbackMode.SOUND_AND_VOICE;
  ```
- **Rigore I18N**: Le chiavi di traduzione e i relativi tooltip vengono inseriti sia in `it_it.json` sia in `en_us.json`, rispettando rigorosamente l'**ordine alfabetico crescente delle chiavi JSON** per garantire il superamento della CI GitHub:
  - `text.autoconfig.minecraft-access.option.autoWalk.progressionFeedbackMode`: "Feedback di progressione" / "Progression feedback"
  - `text.autoconfig.minecraft-access.option.autoWalk.progressionFeedbackMode.@Tooltip`: spiegazione lineare dei 4 stati.

### Contratto D2 — De-accoppiamento del Flusso di Progressione in `AutoWalkMotor` e `MovementCoordinator`
- **File Coinvolti**:
  * [`src/main/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkMotor.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkMotor.java)
  * [`src/main/java/org/mcaccess/minecraftaccess/features/autowalk/MovementCoordinator.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/autowalk/MovementCoordinator.java)
- **Logica in `AutoWalkMotor` (Avanzamento Waypoint)**:
  - Il metodo riceve la modalità `progressionFeedbackMode`;
  - *Click Nodo*: Scatta se `config.playNodeSoundCue` è abilitato AND (`progressionFeedbackMode == SOUND_AND_VOICE || progressionFeedbackMode == SOUND_ONLY`);
  - *Annuncio Vocale Passi*: Scatta se `remainingSteps > 0 && remainingSteps % 5 == 0 && currentIndex != lastAnnouncedStepIndex` AND (`progressionFeedbackMode == SOUND_AND_VOICE || progressionFeedbackMode == VOICE_ONLY`);
  - **Svincolo Definitivo**: Eliminata la dipendenza da `narrateHints` per il conteggio dei passi.
- **Seam Determinista Package-Private**:
  - `static boolean shouldPlayNodeSound(boolean playNodeSoundCue, ProgressionFeedbackMode mode)`
  - `static boolean shouldNarrateStepProgression(int remainingSteps, int currentIndex, int lastAnnouncedIndex, ProgressionFeedbackMode mode)`

### Contratto D3 — Silenziamento del Cue Sonoro Arpa in `NarrateCrosshair`
- **File Coinvolti**:
  * [`src/main/java/org/mcaccess/minecraftaccess/features/NarrateCrosshair.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/NarrateCrosshair.java)
- **Logica di Soppressione**:
  - All'interno di `tick(Minecraft client, Player player, Level level)`, prima di eseguire il blocco `playRelativePositionSoundCue`:
  ```java
  boolean autoWalkActive = org.mcaccess.minecraftaccess.features.autowalk.MovementCoordinator.isAutoWalkActive();
  Config mainConfig = Config.getInstance();
  boolean silenceCrosshair = mainConfig != null && mainConfig.autoWalk != null && mainConfig.autoWalk.silenceCrosshairDuringWalk;
  if (!shouldSilenceCrosshairHarp(autoWalkActive, silenceCrosshair)) {
      // emissione ordinaria suono arpa
  }
  ```
- **Seam Package-Private**:
  - `public static boolean shouldSilenceCrosshairHarp(boolean isAutoWalkActive, boolean silenceConfig)`: restituisce `isAutoWalkActive && silenceConfig`.

### Contratto D4 — Armonizzazione `ObstacleDetector` & Preservazione Ispezione Manuale
- **File Coinvolti**:
  * [`src/main/java/org/mcaccess/minecraftaccess/features/ObstacleDetector.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/ObstacleDetector.java)
- **Garanzie Operative**:
  - Se `autoWalkActive && silenceObstaclesDuringWalk`: `ObstacleDetector.tick` esce subito (`return;`), garantendo zero chatter passivo su gradini ordinari (`STEP_CLIMBABLE`) o pareti;
  - Se il giocatore preme il tasto manuale `U` (`inspectObstacle`), il metodo `inspectObstacle()` viene eseguito normalmente a prescindere dallo stato di marcia, poiché si tratta di un comando manuale esplicito.

### Contratto D4bis — Silenziamento del Cue Acustico Anticaduta (Incudine) in `ProximityFallDetector`
- **File Coinvolti**:
  * [`src/main/java/org/mcaccess/minecraftaccess/features/safety/fall/ProximityFallDetector.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/safety/fall/ProximityFallDetector.java)
- **Logica Operativa**:
  - In `ProximityFallDetector.checkProximityWarning`:
    ```java
    // Durante AutoWalk la voce e i cue sonori anticaduta (xilofono E incudine) sono zittiti a monte
    boolean fallVoiceSilenced = autoWalkActive && silenceFallVoiceWarnings;
    boolean audioSilencedInAutoWalk = shouldSilenceFallAudioInAutoWalk(autoWalkActive, silenceFallVoiceWarnings);
    ```
  - La condizione `audioSilencedInAutoWalk` si applica sia all'allarme di prossimità (Zone 1, 2A, 2B) sia al debounced Edge Bump;
  - **Invariante di Sicurezza Fisica**: L'auto-sneak (`autoSneakActive`) e la protezione fisica del movimento (`getMovementGuard().engageFallProtection()`) restano operativi qualora la fisica del giocatore entri in collisione con un ciglio reale;
  - **Ripristino Istantaneo**: Se l'AutoWalk si interrompe (Watchdog di stallo, Human Takeover da tasti movimento `keySneak` / WASD o arrivo a meta), `autoWalkActive` diventa `false` e l'allarme acustico dell'incudine torna pienamente attivo a latenza 0 ms.
- **Seam Package-Private per Test**:
  - `public static boolean shouldSilenceFallAudioInAutoWalk(boolean autoWalkActive, boolean silenceFallWarnings)`: restituisce `autoWalkActive && silenceFallWarnings`.

### Contratto D5 — Suite di Test Unitari Headless (Determinismo a 0 ms — Cancello 5)
- **File di Test**:
  * [`src/test/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkSensoryQuietingRefinementTest.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/test/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkSensoryQuietingRefinementTest.java)
- **Casi di Test Unitari da Coprire**:
  1. *Matrice Booleana `ProgressionFeedbackMode` per Suono Nodi*: verifica dei 4 stati (True per `SOUND_AND_VOICE` e `SOUND_ONLY`, False per gli altri);
  2. *Matrice Booleana `ProgressionFeedbackMode` per Voce Passi*: verifica dei 4 stati (True per `SOUND_AND_VOICE` e `VOICE_ONLY`, False per gli altri);
  3. *Debouncing e Cadenza a 5 Passi*: verifica che a 24 passi non parli, a 20 parli, a 19 non parli;
  4. *Matrice 4 Stati `shouldSilenceCrosshairHarp`*: `(true, true) -> true`, tutte le altre combinazioni `false`;
  5. *Matrice 4 Stati `shouldSilenceFallAudioInAutoWalk`*: `(true, true) -> true`, tutte le altre combinazioni `false`;
  6. *Indipendenza dall'Interruttore Master*: verifica che con `autoWalk.enabled = false` o `fallDetector.enabled = false` i moduli siano inerti.

---

## 🧪 3. Validazione Preventiva sui 7 Assi di Qualità ASTRALIS

1. **Validità**: Pienamente conforme a Minecraft 26.2 (1.21.x), Fabric + NeoForge, Balm e Java 25.
2. **Efficacia**: Azzeramento dell'arpeggio fastidioso del mirino durante le sterzate in AutoWalk, soppressione dei falsi allarmi dell'incudine lungo i cigli calcolati e pieno controllo della verbosità dei passi.
3. **Coerenza**: Simmetria perfetta con il pattern a 4 stati di `EdgeBumpFeedbackMode` e con la disciplina a 3 zone del `CentralFallSafetyManager`.
4. **Completezza**: Coinvolge Configurazione, Motore, Coordinatore, Mirino, Ostacoli, Anticaduta di Prossimità e File di Lingua IT/EN.
5. **Precisione**: Separazione netta tra ciò che viene zittito in marcia regolare e ciò che risponde all'interrogazione manuale o allo stallo.
6. **Affidabilità e Prestazioni**: Tutti i controlli sono eseguiti a monte tramite condizioni booleane primitive, con zero garbage collection.
7. **Assenza di Regressioni**: La deambulazione manuale (WASD) e i comandi da tastiera rimangono identici; retrocompatibilità totale con i salvataggi esistenti.

---

## 🔬 4. Matrice di Simulazione a 3 Livelli

### Livello 1 — Scenari Comuni (Happy Path)
- **Scenario 1.1: Marcia regolare con default Luca (`SOUND_AND_VOICE`)**:
  - *Condizione*: AutoWalk attivo verso il Granaio a 25 passi.
  - *Comportamento*: A ogni passo suona il click del nodo (`NOTE_BLOCK_HAT`); a 20, 15, 10 e 5 passi la voce annuncia la distanza residua. Nelle curve la testa gira senza emettere arpeggi. All'arrivo suona la campana.
- **Scenario 1.2: Marcia lungo un dirupo o ciglio costeggiato (Zona 2A/2B)**:
  - *Condizione*: La rotta calcolata passa a 1.2 metri da una scarpata.
  - *Comportamento*: Nessun boato improvviso dell'incudine; l'AutoWalk prosegue sereno sul sentiero sicuro.

### Livello 2 — Scenari Meno Comuni & Concorrenza (Alternative Paths)
- **Scenario 2.1: Interrogazione manuale del mirino con tasto B in movimento**:
  - *Condizione*: Durante l'AutoWalk l'arpa è muta. Il giocatore preme `B`.
  - *Comportamento*: Il `DirectInteractionShield` intercetta l'input manuale ed emette all'istante il blocco mirato con voce a priorità alta.
- **Scenario 2.2: Interruzione della marcia per stallo o takeover sul ciglio**:
  - *Condizione*: L'AutoWalk si arresta a 1.2m dal ciglio per blocco imprevisto o pressione di Shift/S.
  - *Comportamento*: `autoWalkActive` diventa immediatamente `false`; al tick successivo scatta l'allarme pre-freno `ANVIL_LAND` per informare il giocatore del baratro circostante.

### Livello 3 — Casi Limite & Corner Cases (Boundary, Zero, Null, Error)
- **Scenario 3.1: Interruttore generale del Modulo Spento**:
  - *Condizione*: L'utente disabilita `fallDetector.enabled = false`.
  - *Comportamento*: Nessuna logica anticaduta viene eseguita; l'AutoWalk non attiva né sovrascrive il modulo disabilitato.

---

## 🛑 5. GATING SEMANTICO E PROTOCOLLO DI STOP (Regola 0)

> [!IMPORTANT]
> **STOP OBBLIGATORIO PRIMA DELLA SOTTO-FASE 1B (CODICE SORGENTE)**:
> In conformità alla Regola 0 e al Canone ASTRALIS v3.0.4, la presente Sotto-Fase 1A è formalmente conclusa e convalidata. 
> **Nessuna riga di codice Java o file JSON di configurazione verrà modificata** prima che Luca abbia esaminato il presente piano e abbia fornito l'esplicito comando di procedere (*"procedi"*, *"applica"*, *"esegui"*).
