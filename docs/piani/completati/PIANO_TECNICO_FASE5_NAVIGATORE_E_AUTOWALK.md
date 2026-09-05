# Piano Tecnico Formale — Fase 5: Navigatore e Auto-Walk (ASTRALIS v2.6.3)
# Autore: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity
# Framework: ASTRALIS v2.6.3 — Sotto-Fase 1A (Progettazione, Contratti & FSM)
# Repository: minecraft-access (Fabric / NeoForge, Java 25, Minecraft 26.2)
# Branch Attivo: feat/cognitive-orchestrator
# Incremento Versione Target (AVF): v26.2-1.19.0

> [!IMPORTANT]
> **Gating Semantico di Fase 1 (Regola 0)**: Questo documento costituisce il Piano Tecnico Formale di Sotto-Fase 1A. È fatto assoluto divieto di modificare classi Java, file di configurazione o bytecode prima dell'esplicita convalida e autorizzazione di Luca (*"procedi"*, *"applica"*, *"esegui"*).

---

## 🎯 1. Visione d'Insieme, Motivazione & Strategia a Due Stadi

La Fase 5 della roadmap cognitiva realizza il **completo disaccoppiamento architetturale** tra il calcolo della rotta nello spazio voxel e la sua esecuzione cinematica automatica, istituendo un **Coordinatore di Dominio Locale** (`MovementCoordinator`) dedicato al movimento e alla navigazione.

In piena conformità con i vincoli della Sezione 14 della Strategia Generale (*"Non si introducono nuove abitudini di gioco prima di avere dimostrato che le informazioni già esistenti sono ordinate, comprensibili e non intrusive"*), la pianificazione adotta la **Strategia a Due Stadi**:

### 1.1 Stadio 1: Fase 5 Stretta (Migrazione Architetturale a Parità di Comportamento 1:1)
- L'obiettivo esclusivo di questo stadio è la migrazione dell'attuale `AutoWalkController` verso il nuovo sistema a tre livelli, **senza alterare le regole del mondo di gioco o i comandi fisici esistenti**;
- Viene creato il sottogestore `MovementCoordinator` per gestire l'emissione dei messaggi e dei suoni verso il `CognitiveCoordinator` centrale;
- Il comportamento in-game resta rigorosamente fedele alle costanti cinematiche storiche:
  - Sterzata progressiva limitata a 20°/tick;
  - Rilevamento arrivo a meta entro 2 blocchi quadratici (`distToFinalGoalSq <= 2.0`);
  - Fermata davanti a porte chiuse entro 2.1 blocchi (`distToDoorSq <= 4.5`);
  - Watchdog anti-blocco con ricalcolo al 12° tick di immobilità e aborto al 24° tick (stuck);
  - Ripianificazione post-atterraggio dopo dislivelli (> 0.4 m) o salti;
  - Ricalcolo dinamico su entità bersaglio se distanziata di oltre 2 blocchi quadratici (`distSqr > 4.0`);
- Lo Human Takeover rispetta fedelmente la logica storica: rilevamento sui tasti `S`, `A`, `D`, `Shift` dopo i 10 tick di grazia iniziali (se `stopOnManualInput = true`); il tasto `W` resta escluso per evitare conflitti con il bot;
- Il paesaggio acustico storico viene congelato al 100%: suono d'arrivo con `NOTE_BLOCK_BELL` a pitch 1.2f, avanzamento nodi con `NOTE_BLOCK_HAT` a pitch 1.8f e debouncing a 200 ms;
- Tutte le preferenze utente esistenti in Cloth Config sono formalmente vincolanti;
- Riuso al 100% delle chiavi di localizzazione storiche senza stringhe hardcoded e con ordine alfabetico JSON preservato;
- La voce viene ripulita dai troncamenti vocali (speech truncation) con NVDA e subordinata agli allarmi di sicurezza.

### 1.2 Stadio 2: Sotto-Fase 5E (Estensione Funzionale Post-Convalida con Stop Gating)
- **Gating Formale Obbligatorio**: La Sotto-Fase 5E non si avvia mai in automatico al termine della Fase 5 Stretta. Richiede il collaudo in-game positivo della parità 1:1, la stesura di una specifica revisione formale (Sotto-Fase 1A delle estensioni) e il comando esplicito di Luca (*"procedi con 5E"*);
- Ambiti riservati esclusivamente alla Sotto-Fase 5E:
  - Opzione di auto-apertura porte (`autoOpenDoors`) con interazione controllata del client;
  - Enum di personalizzazione della verbosità di marcia (`progressionVerbosity`: silenzioso, solo suoni, cadenzato a 5 passi, dettagliato);
  - Esposizione di un comando da tastiera per interrogare il Navigatore (GPS manuale su rotta e distanza senza muovere il giocatore).

---

## 🏛️ 2. Architettura a Tre Livelli del Dominio Movimento (Modello Lineare NVDA)

L'architettura del movimento si articola linearmente nei tre livelli ufficiali del framework cognitivo:

- **Livello 1: Rilevatori Tecnici e Calcolo Geometrico**:
  - `AutoWalkPathfinder`: algoritmo puro di pathfinding A* nello spazio voxel; determina nodi, distanze, salti e collisioni restituendo un record `PathResult` immutabile.
- **Livello 2: Dominio Specialistico del Movimento**:
  - `RouteNavigator`: gestore dello stato della rotta, del tracciamento nodi e dell'algoritmo di ricalcolo (la mente);
  - `AutoWalkMotor`: detentore della FSM cinematica (`State`), esecutore dei comandi virtuali del client, gestione della sterzata fluida, frenata in curva, salti su gradino, nuoto, rilevamento atterraggio e watchdog anti-blocco (il braccio/corpo);
  - `MovementCoordinator`: sottogestore centrale di dominio che orchestra la rotta e il motore, gestisce il ciclo di vita multipiattaforma del client con Balm (anti-ghosting tasti) ed è **l'unico interlocutore verso il sistema cognitivo centrale**.
- **Livello 3: Sistema Cognitivo Centrale**:
  - `CognitiveCoordinator`: riceve gli eventi di movimento come `SourceDomain.MOVEMENT`, ne gestisce la priorità a fine-tick (`ClientPlayingTick.AFTER`), garantisce la precedenza assoluta alla sicurezza Fast-Path e offre la pulizia selettiva con invalidamento della cache di deduplicazione.
- **Uscita Protetta Verso l'Utente**:
  - Canale vocale `MainClass.narrate` (NVDA / Tolk) + suoni 3D posizionali di movimento conformi alle abitudini acustiche storiche.

---

## 📦 3. Contratti di Dominio & Ciclo di Vita FSM del Percorso

Per garantire la separazione delle responsabilità (Clean Architecture), i ruoli e la proprietà dei dati sono rigorosamente formalizzati.

### 3.1 `RouteNavigator` (La Mente: Stato, Geometria e Query della Rotta)
- **Proprietà dei Dati**: È l'unico detentore dell'elenco dei nodi (`List<BlockPos> currentPath`), dell'indice corrente (`currentPathIndex`), dell'obiettivo iniziale (`currentGoalPos`) e del target (`targetObject`).
- **Contratti Matematici e Costanti di Ripianificazione (Parità 1:1)**:
  - *Soglia ricalcolo su entità in movimento*: `targetObject instanceof Entity entity` e `entity.blockPosition().distSqr(currentGoalPos) > 4.0` (innesca `repath` se l'entità si sposta di oltre 2.0 blocchi quadratici);
  - *Vincolo verticale su avanzamento nodo*: un nodo è considerato raggiunto orizzontalmente solo se `distH < advanceThreshold` (0.45 m a passo, 0.70 m in sprint) **E** `Math.abs(deltaY) < 1.0` (differenziale verticale assoluto inferiore a 1 blocco, sia verso l'alto che verso il basso);
  - *Rilevamento traguardo finale*: `rawTargetPos != null && player.blockPosition().distSqr(rawTargetPos) <= 2.0 && currentPathIndex >= currentPath.size() - 1`;
  - *Gestione del caso "Già alla Meta" (ALREADY_AT_TARGET — Opzione A Convalidata)*: se al momento dell'avvio della rotta il giocatore si trova già entro la distanza di arrivo ($\le 2.0$ blocchi dal target), il pathfinder restituisce `ALREADY_AT_TARGET`. Il flusso instrada l'evento direttamente nella pipeline ufficiale di arrivo (`AUTOWALK_ARRIVED`), rispettando coerentemente le opzioni `voiceFeedback` (generando `SOUND_ONLY` se disabilitata) e `lookAtTargetOnArrival`.
- **Metodi di Modifica dello Stato**:
  - `startRoute(Object target, int maxRange)`: invoca il pathfinder, imposta il percorso iniziale e azzera l'indice;
  - `advanceWaypoint()`: incrementa di 1 l'indice del nodo se non ha raggiunto il traguardo;
  - `repath(Level level, LocalPlayer player, int maxRange)`: ricalcola la rotta se l'entità bersaglio si è spostata, dopo un atterraggio o se il motore segnala un blocco;
  - `clearRoute()`: svuota il percorso e azzera l'indice su arrivo, annullamento o reset di sessione.
- **Metodi di Query in Sola Lettura**:
  - `boolean hasActiveRoute()`: verifica se esiste un percorso valido;
  - `boolean isAtFinalGoal(LocalPlayer player)`: verifica la prossimità finale quadratica <= 2.0;
  - `BlockPos getCurrentNodePos()`: restituisce il nodo attualmente da raggiungere;
  - `int getRemainingSteps()`: restituisce quanti nodi mancano al traguardo;
  - `double getRemainingDistance(LocalPlayer player)`: calcola la distanza rimanente lungo i nodi della rotta.

### 3.2 `AutoWalkMotor` (Il Braccio: Cinematica, FSM e Controlli Virtuali)
- **Proprietà dello Stato FSM del Corpo**:
  - `State state`: enum cinematico (`WALKING`, `JUMPING`, `SWIMMING`, `ARRIVED`, `CANCELLED`, `IDLE`);
  - `boolean wasInAir`: flag per rilevare il passaggio da aria a terra;
  - `double lastGroundY`: quota Y dell'ultimo punto solido calpestato;
  - `int jumpHoldingTicks`: contatore di mantenimento tasto salto (4 tick);
  - `int sprintCooldownTicks`: contatore di isteresi sprint (20 tick post-curva > 15°);
  - `int stuckTicks`: contatore di immobilità del watchdog;
  - `BlockPos waitingClosedDoorPos`: posizione della porta chiusa davanti a cui si è in attesa;
  - `int startupGraceTicks`: contatore di grazia iniziale (10 tick).
- **Rilevamento Atterraggio Post-Dislivello**:
  - Nel tick del client, se `wasInAir && onGround && (Math.abs(currentY - lastGroundY) > 0.4 || state == State.JUMPING)`: aggiorna `lastGroundY = currentY` e richiede esplicitamente `navigator.repath(level, player, maxRange)` per riallineare la rotta da terra.
- **Responsabilità nel Tick del Client**:
  - Riceve il nodo corrente da `RouteNavigator`;
  - *Sterzata progressiva*: differenziale di Yaw con rate limit a 20.0° per tick per prevenire strappi visivi;
  - *Frenata in curva stretta*: se `Math.abs(yawDiff) > 55.0f && distH > 0.6`, rilascia temporaneamente `keyUp` per consentire la rotazione sul posto prima di ripartire;
  - *Isteresi di sprint*: se `Math.abs(yawDiff) > 15.0f`, imposta `sprintCooldownTicks = 20`;
  - *Iniezione marcia*: imposta `client.options.keyUp.setDown(true)` se non in frenata da curva o attesa porta;
  - *Salto assistito su dislivello (Step-Up)*: se `(distH <= 1.25 || player.horizontalCollision) && deltaY > 0.30 && deltaY <= 1.25 && onGround && config.autoJump`, attiva `keyJump.setDown(true)` per 4 tick (`jumpHoldingTicks = 4`);
  - *Nuoto assistito (Auto-Swim)*: se `player.isInWater() || player.isInLiquid()` e `config.autoSwim`, mantiene `keyJump.setDown(true)`;
  - *Avanzamento nodi*: verifica la soglia `advanceThreshold` (0.45 m / 0.70 m) e `Math.abs(deltaY) < 1.0`, invoca `navigator.advanceWaypoint()` e notifica il passaggio al coordinatore;
  - *Arresto a meta*: quando `navigator.isAtFinalGoal()` è vero, invoca `resetMovement()`, orienta lo sguardo al target se `lookAtTargetOnArrival == true` e notifica l'arrivo al coordinatore.
- **Gestione Porte Chiuse e Riapertura (Comportamento Storico 1:1)**:
  - Rileva se il nodo corrente o quello sovrastante contengono una porta (in legno o ferro), cancello o botola chiusa (`isDoorOrGateClosed`);
  - Se il giocatore si trova entro 2.1 blocchi (`distToDoorSq <= 4.5`), rilascia `keyUp`, disattiva lo sprint, allinea lo sguardo alla porta e passa in attesa, memorizzando `waitingClosedDoorPos = doorCheckPos` ed emettendo `AUTOWALK_DOOR_WAIT` una sola volta;
  - Se `waitingClosedDoorPos != null` e il blocco a quella posizione risulta ora aperto (aperto da Luca o da meccanismo): azzera `waitingClosedDoorPos`, riprende la marcia in avanti ed emette `AUTOWALK_DOOR_OPENED`.
- **Watchdog Anti-Blocco (Stuck Detector a Due Soglie)**:
  - Se il giocatore è a terra e si muove meno di 0.04 m per 12 tick consecutivi: richiede un `repath` a `RouteNavigator`;
  - Se l'immobilità persiste per 24 tick consecutivi: abortisce la marcia, rilascia i tasti virtuali ed emette `AUTOWALK_STUCK`.
- **Human Takeover Fedele al Codice Esistente**:
  - Se `config.stopOnManualInput == false`, il takeover da tasti è disattivato: la marcia automatica continua;
  - Se `config.stopOnManualInput == true`:
    - Per i primi 10 tick dall'avvio (`startupGraceTicks = 10`), il controllo è protetto per impedire al rilascio di `Alt+W` di causare auto-abort;
    - Trascorsi i 10 tick di grazia, se `client.options.keyDown.isDown()`, `keyLeft.isDown()`, `keyRight.isDown()` o `keyShift.isDown()` risultano premuti, il motore si arresta all'istante a latenza 0 ms, rilascia i comandi virtuali ed emette `AUTOWALK_CANCELLED`;
    - *Esclusione del Tasto W*: il comando in avanti `keyUp` (W) è escluso dalla verifica perché è il tasto virtuale tenuto premuto dal bot stesso.

### 3.3 Contrattualizzazione Delle Preferenze Utente (Cloth Config)
Tutte le impostazioni di configurazione sono vincolanti e governano il comportamento runtime:
- `config.stopOnManualInput`: se `false`, ignora la pressione dei tasti manuali `S`, `A`, `D`, `Shift` durante la marcia;
- `config.voiceFeedback`: se `false`, genera un evento con `OutputType.SOUND_ONLY` per l'arrivo (`AUTOWALK_ARRIVED`), sopprimendo al 100% la voce ma preservando il cue sonoro `NOTE_BLOCK_BELL` a 1.2f;
- `config.playNodeSoundCue`: se `false`, sopprime il cue acustico `NOTE_BLOCK_HAT` al passaggio sui nodi intermedi;
- `speechSettings.narrateHints`: se `false`, sopprime sia la vocalizzazione progressiva a 5 passi (`AUTOWALK_PROGRESS`), sia gli avvisi vocali relativi alle porte (`AUTOWALK_DOOR_WAIT` e `AUTOWALK_DOOR_OPENED`);
- `config.lookAtTargetOnArrival`: se `true`, orienta lo sguardo del giocatore verso il centro del bersaglio all'arrivo (incluso il caso `ALREADY_AT_TARGET`); se `false`, lascia inalterata la direzione visiva corrente;
- `config.sprint`: abilita lo sprint solo se congiuntamente `config.sprint == true`, `sprintCooldownTicks == 0`, `!player.isShiftKeyDown()` e livello fame `foodLevel > 6.0f`.

### 3.4 Rigore I18N: Riuso Integrale delle Chiavi Esistenti
Nella Fase 5 Stretta è fatto assoluto divieto di inserire stringhe hardcoded nel codice sorgente Java o di creare nuove chiavi nei file di lingua. Il catalogo eventi impiega **esclusivamente** le 12 chiavi già presenti nei file `it_it.json` ed `en_us.json`:
- `minecraft_access.autowalk.start`: *"Navigazione verso %s, distanza %s metri, %s passi"*
- `minecraft_access.autowalk.arrived`: *"Arrivato a destinazione: %s"*
- `minecraft_access.autowalk.cancelled`: *"Navigazione automatica annullata"*
- `minecraft_access.autowalk.stuck`: *"Percorso ostruito, marcia arrestata"*
- `minecraft_access.autowalk.no_path`: *"Nessun percorso sicuro trovato per %s"*
- `minecraft_access.autowalk.out_of_range`: *"Bersaglio oltre il raggio di navigazione: %s metri"*
- `minecraft_access.autowalk.disabled`: *"Navigatore disabilitato nelle impostazioni"*
- `minecraft_access.autowalk.sprint_enabled`: *"Navigazione: corsa abilitata"*
- `minecraft_access.autowalk.sprint_disabled`: *"Navigazione: camminata abilitata"*
- `minecraft_access.autowalk.step_door_closed`: *"Porta chiusa davanti a te. Premi Tasto Destro per aprire"*
- `minecraft_access.autowalk.step_door_opened`: *"Porta aperta. Procedi verso %s"*
- `minecraft_access.autowalk.step_progression`: *"Ancora %s passi"*

Tutti i parametri numerici vengono formattati attraverso `NarrationUtils.narrateNumber(...)` per preservare la corretta vocalizzazione in sintesi vocale.

### 3.5 `MovementCoordinator` (Sottogestore di Dominio & Ciclo di Vita Balm)
- **Ruolo Centrale**: Riceve le richieste di avvio/stop da `AutoWalkManager`, orchestra `RouteNavigator` e `AutoWalkMotor`, e gestisce la comunicazione bidirezionale con `CognitiveCoordinator`.
- **Factory di Eventi con Supporto `OutputType` Esplicito**:
  - `MovementCoordinator` espone metodi factory dedicati capaci di impostare l'`OutputType` esatto:
    - Se `voiceFeedback = true`: output `OutputType.VOICE_AND_SOUND` (voce e campana);
    - Se `voiceFeedback = false`: output `OutputType.SOUND_ONLY` (campana attiva, voce completamente muta per contratto).
- **Ciclo di Vita Multipiattaforma con Balm (Anti-Ghosting Tasti)**:
  - Si registra alle callback multipiattaforma condivise (Balm):
    - `ClientLifecycleCallback.ConnectedToServer`: azzera lo stato e cancella qualsiasi rotta pendente;
    - `ClientLifecycleCallback.DisconnectedFromServer`: azzera lo stato, esegue `resetMovement()` e invoca `clearDomainEvents(SourceDomain.MOVEMENT)`.
  - Nel tick client (`ClientPlayingTick.AFTER`), monitora la morte/respawn del giocatore (`player.isDeadOrDying()`, cambio `player.getId()`) e il cambio dimensione (`level != lastLevel`):
    - Su ciascuna di queste condizioni esegue immediatamente `AutoWalkMotor.resetMovement()` forzando `setDown(false)` su `keyUp`, `keyJump` e sprint, azzera la rotta e ripulisce gli eventi di movimento pendenti.
- **Catalogo Lineare Ufficiale degli Eventi Generati (SourceDomain.MOVEMENT)**:
  - Evento 1: `AUTOWALK_START`
    - Condizione: percorso calcolato e marcia avviata con successo;
    - Priorità: `OPERATIONAL`;
    - Output: `OutputType.VOICE_AND_SOUND` (`NOTE_BLOCK_PLING` a pitch 1.2f, volume configurato);
    - Testo: `I18n.get("minecraft_access.autowalk.start", targetName, distStr, stepsStr)`.
  - Evento 2: `AUTOWALK_ARRIVED`
    - Condizione: traguardo raggiunto (`distToFinalGoalSq <= 2.0`) o caso iniziale `ALREADY_AT_TARGET`;
    - Priorità: `OPERATIONAL`;
    - Output: `OutputType.VOICE_AND_SOUND` se `voiceFeedback = true`; `OutputType.SOUND_ONLY` se `voiceFeedback = false` (`NOTE_BLOCK_BELL` a pitch 1.2f, volume 0.8f);
    - Testo: `I18n.get("minecraft_access.autowalk.arrived", targetName)`.
  - Evento 3: `AUTOWALK_NO_PATH`
    - Condizione: nessun percorso calcolabile verso il bersaglio;
    - Priorità: `OPERATIONAL`;
    - Output: `OutputType.VOICE_AND_SOUND` (`NOTE_BLOCK_BASS` a pitch 0.6f, volume configurato);
    - Testo: `I18n.get("minecraft_access.autowalk.no_path", targetName)`.
  - Evento 4: `AUTOWALK_OUT_OF_RANGE`
    - Condizione: bersaglio oltre la distanza massima configurata;
    - Priorità: `OPERATIONAL`;
    - Output: `OutputType.VOICE_ONLY`;
    - Testo: `I18n.get("minecraft_access.autowalk.out_of_range", distStr)`.
  - Evento 5: `AUTOWALK_CANCELLED`
    - Condizione: ripresa manuale tramite tasti di movimento (S, A, D, Shift), annullamento esplicito da comando o bersaglio non più valido;
    - Priorità: `OPERATIONAL`;
    - Output: `OutputType.VOICE_AND_SOUND` (`NOTE_BLOCK_HAT` a pitch 0.5f, volume configurato);
    - Testo: `I18n.get("minecraft_access.autowalk.cancelled")`.
  - Evento 6: `AUTOWALK_STUCK`
    - Condizione: blocco insormontabile oltre 24 tick;
    - Priorità: `OPERATIONAL`;
    - Output: `OutputType.VOICE_AND_SOUND` (`NOTE_BLOCK_BASS` a pitch 0.5f, volume configurato);
    - Testo: `I18n.get("minecraft_access.autowalk.stuck")`.
  - Evento 7: `AUTOWALK_DOOR_WAIT`
    - Condizione: fermo a 2.1 blocchi davanti a una porta o cancello chiuso;
    - Priorità: `OPERATIONAL`;
    - Output: `OutputType.VOICE_ONLY` (attiva solo se `narrateHints = true`);
    - Testo: `I18n.get("minecraft_access.autowalk.step_door_closed")`.
  - Evento 8: `AUTOWALK_DOOR_OPENED`
    - Condizione: la porta davanti a cui il bot era in attesa risulta aperta;
    - Priorità: `OPERATIONAL`;
    - Output: `OutputType.VOICE_ONLY` (attiva solo se `narrateHints = true`);
    - Testo: `I18n.get("minecraft_access.autowalk.step_door_opened", targetName)`.
  - Evento 9: `AUTOWALK_PROGRESS`
    - Condizione: avanzamento ogni 5 passi (se `narrateHints = true`);
    - Priorità: `CONTEXTUAL`;
    - Output: `OutputType.VOICE_ONLY` (accodata in assenza di eventi superiori);
    - Testo: `I18n.get("minecraft_access.autowalk.step_progression", stepsStr)`.
  - Evento 10: `AUTOWALK_STEP_NODE`
    - Condizione: passaggio sopra un nodo intermedio della rotta (se `playNodeSoundCue = true`);
    - Priorità: `PASSIVE`;
    - Output: `OutputType.SOUND_ONLY` (`NOTE_BLOCK_HAT` a pitch 1.8f, volume `config.audioCueVolume * 0.5f`, debouncing rigoroso a 200 ms);
    - Testo: Vuoto.

---

## 🛡️ 4. Modifica di Supporto a `CognitiveCoordinator`: Cancellazione Selettiva & Invalida Cache

Per garantire la pulizia immediata della marcia senza code residue e senza sopprimere partenze ravvicinate:

1. **Firma e Sincronizzazione del Metodo**:
   - Viene implementato in `CognitiveCoordinator`:
     `public static synchronized void clearDomainEvents(SourceDomain domain)`
2. **Operazioni Atomiche a Tre Livelli**:
   - *Livello 1 (Tick Buffer)*: `tickBuffer.removeIf(event -> event.domain() == domain);`
   - *Livello 2 (Short Queue)*: `shortQueue.removeIf(event -> event.domain() == domain);`
   - *Livello 3 (Cache Deduplicazione)*: `recentEvents.keySet().removeIf(key -> key.domain() == domain);`
3. **Motivazione Tecnica del Livello 3**:
   - La finestra di deduplicazione di 1.5 secondi (`recentEvents`) serve a silenziare ripetizioni identiche.
   - Se Luca interrompe la marcia ed esegue subito un nuovo avvio entro 1.5 secondi, la cancellazione dal Livello 3 impedisce che il nuovo evento `AUTOWALK_START` venga soppresso come duplicato silenzioso.
4. **Isolamento delle Sessioni di Rotta**:
   - Gli eventi di navigazione integrano nella loro `StateSignature` l'identificativo univoco del bersaglio o della sessione di rotta (`targetId`), prevenendo confusioni tra tragitti diversi.

---

## 🔒 5. Difesa in Profondità per le Soppressioni Storiche

Per evitare qualsiasi rischio di regressione durante il collaudo in-game:
- In `finishArrival`, il codice **mantiene temporaneamente attive** le chiamate:
  - `NarrateCrosshair.suppressNarration(1500);`
  - `ObstacleDetector.suppressWarnings(1500);`
- Queste chiamate agiranno come cintura di sicurezza passiva insieme al nuovo evento `OPERATIONAL`. Verranno formalmente rimosse o deprecate solo a valle del collaudo in-game, quando sarà provato sul campo che lo scudo operativo del `CognitiveCoordinator` copre l'arrivo al 100%.

---

## 🚫 6. Delimitazione Negativa Esplicita (Cosa NON Toccare)

In conformità alle direttive ASTRALIS, i seguenti componenti sono dichiarati intoccabili in questa fase:

1. **Scanner Direzionale (`DirectionalPathScanner` / `Pagina Su` / `Pagina Giù`)**:
   - Rimane un modulo storico indipendente; è fatto assoluto divieto di modificarlo o intercettarne i tasti.
2. **Mentore e Accademia (`ContextualMentor`, `AcademyManager`)**:
   - Rinviati formalmente alla Fase 6 (Didattica). Nessuna riga di codice didattico viene toccata.
3. **Moduli di Sicurezza di Fase 3 (`FallDetector`, `ObstacleDetector`, `SafetyMovementGuard`)**:
   - Rimangono intatti nei loro contratti e preservano il bypass scale (`Rev MC-26.8`).
4. **Moduli di Esplorazione di Fase 4 (`CrosshairExplorationEventFactory`, `DirectInteractionShield`)**:
   - Rimangono congelati nei loro contratti operativi.
5. **Keybinding di Sistema**:
   - Il comando `Ctrl+Alt+W` rimane assegnato esclusivamente all'abilitazione sprint di AutoWalk (`toggleSprint`), senza sovrapposizioni.

---

## 🛡️ 7. Matrice delle Invarianti Anti-Regressione

- **Invariante 1 (Precedenza Assoluta della Sicurezza Fast-Path)**:
  - Nessun calcolo o evento del navigatore può ritardare un allarme di burrone (`FallDetector`) o lava. Il Fast-Path a 0 ms taglia all'istante qualsiasi voce di movimento.
- **Invariante 2 (Rilascio Totale Tasti Virtuali su Qualsiasi Arresto)**:
  - Qualsiasi uscita dall'Auto-Walk (arrivo, cancellazione, takeover, morte, disconnessione, cambio dimensione, stuck) deve forzare `resetMovement`, garantendo che `keyUp` e `keyJump` non rimangano mai impostati a `true`.
- **Invariante 3 (Trasparenza del Fallback Legacy con Interrupt Vocale)**:
  - Se `cognitiveCoordinatorEnabled = false`, il sistema ricade direttamente sulle chiamate storiche `MainClass.narrate`, mantenendo tassativamente `interrupt = true` per gli eventi operativi di avvio (`AUTOWALK_START`), annullamento (`AUTOWALK_CANCELLED`), arrivo (`AUTOWALK_ARRIVED`) e blocco (`AUTOWALK_STUCK`), garantendo identica reattività uditiva.
- **Invariante 4 (Volumi di Sicurezza ASTRALIS)**:
  - Tutti i cue acustici 3D generati dal movimento rispettano il volume massimo di sicurezza <= 0.8f e i volumi utente configurati in Cloth Config.

---

## 🧪 8. Matrice di Simulazione Scenari a 3 Livelli

### Livello 1: Scenari Comuni (Happy Path)
- **Scenario 1.1: Avvio marcia verso Waypoint**
  - Condizione: bersaglio valido selezionato con `X`, pressione `Alt+W`;
  - Esito: `RouteNavigator` calcola la rotta, `MovementCoordinator` emette `AUTOWALK_START` a priorità `OPERATIONAL`. `AutoWalkMotor` inizia la marcia fluida; il mirino ambientale tace.
- **Scenario 1.2: Avanzamento regolare e arrivo alla meta**
  - Condizione: tragitto senza ostacoli;
  - Esito: al passaggio su ogni nodo suona il click 3D (`NOTE_BLOCK_HAT` a pitch 1.8f); arrivato a meta (`distToFinalGoalSq <= 2.0`), il motore si ferma, orienta lo sguardo al target se `lookAtTargetOnArrival = true` ed emette `AUTOWALK_ARRIVED` con suono `NOTE_BLOCK_BELL` a pitch 1.2f (e voce se `voiceFeedback = true`). Nessun troncamento vocale.
- **Scenario 1.3: Avvio già alla meta (ALREADY_AT_TARGET)**
  - Condizione: pressione `Alt+W` quando il giocatore si trova già entro 2 blocchi dal bersaglio;
  - Esito: emissione immediata di `AUTOWALK_ARRIVED` (rispettando `voiceFeedback` e `lookAtTargetOnArrival`), reset del movimento e transizione a stato `ARRIVED`.

### Livello 2: Scenari Meno Comuni & Concorrenza (Alternative Paths)
- **Scenario 2.1: Human Takeover immediato**
  - Condizione: durante la marcia, Luca preme il tasto `S` (indietro);
  - Esito: se `stopOnManualInput = true`, al tick corrente `AutoWalkMotor` rileva la pressione, rilascia i tasti virtuali ed emette `AUTOWALK_CANCELLED`, eseguendo `clearDomainEvents(MOVEMENT)`. Controllo manuale ripreso al 100%.
- **Scenario 2.2: Porta chiusa e successiva apertura manuale**
  - Condizione: una porta o cancello (in legno o ferro) o una botola è chiusa lungo il tragitto;
  - Esito: a 2.1 blocchi il motore si ferma, allinea lo sguardo alla porta ed emette `AUTOWALK_DOOR_WAIT`. Appena la porta viene aperta dall'utente o dal gioco, il motore emette `AUTOWALK_DOOR_OPENED` e riprende la marcia in avanti in piena autonomia.
- **Scenario 2.3: Bersaglio fuori portata**
  - Condizione: obiettivo a 100 blocchi con `maxRange = 64`;
  - Esito: risposta immediata `AUTOWALK_OUT_OF_RANGE`, nessun movimento impresso.

### Livello 3: Casi Limite & Corner Cases (Boundary, Zero, Null, Error)
- **Scenario 3.1: Morte, respawn, disconnessione o portale dimensionale**
  - Condizione: cambio dimensione o morte mentre il bot è in cammino;
  - Esito: le callback Balm e il tick client in `MovementCoordinator` forzano il rilascio immediato dei tasti virtuali e azzerano la rotta. Zero annunci fantasma, zero tasti incastrati, zero corse automatiche post-respawn.
- **Scenario 3.2: Blocco da ostacolo mobile (Stuck Watchdog)**
  - Condizione: un mob o un blocco ostruisce il cammino per 24 tick;
  - Esito: al 12° tick repath silenzioso; al 24° tick arresto con evento `AUTOWALK_STUCK`.
- **Scenario 3.3: Bersaglio nullo all'avvio**
  - Condizione: pressione `Alt+W` senza target selezionato;
  - Esito: risposta istantanea *"Nessun punto di interesse selezionato"*, nessun evento di marcia creato.

---

## 🔬 9. Suite di Test Unitari Obbligatori (JUnit Headless a 0 ms)

La convalida tecnica automatica richiede l'esecuzione della suite con il comando anti-lock e senza watch del filesystem:  
`.\gradlew.bat --no-daemon --no-watch-fs test`

I test minimi vincolanti da implementare nella nuova classe di test sono:
1. `testHumanTakeoverOnMovementKeys`: verifica arresto immediato su `S`, `A`, `D` e `Shift` quando `stopOnManualInput = true`;
2. `testManualInputIgnoredWhenSettingDisabled`: verifica che con `stopOnManualInput = false` la marcia prosegua senza takeover;
3. `testStartupGraceTicksImmunity`: verifica che nei primi 10 tick i comandi di avvio non causino auto-abort;
4. `testStuckWatchdogTwoThresholds`: ricalcolo al 12° tick e abort al 24° tick con rilascio tasti virtuali;
5. `testClosedDoorStopsAndFacesDoor`: arresto a 2.1 blocchi da porte in legno, ferro o botole chiuse;
6. `testDoorOpenedResumesWalkingAndEmitsEvent`: verifica emissione di `AUTOWALK_DOOR_OPENED` e ripresa marcia quando la porta si apre;
7. `testSelectiveDomainEventClear`: verifica che `clearDomainEvents(MOVEMENT)` pulisca buffer, queue e cache dedup di MOVEMENT senza toccare gli allarmi `SAFETY`;
8. `testRouteProgressionAndArrival`: avanzamento indice su `distH < threshold` e `Math.abs(deltaY) < 1.0` (sia sopra che sotto), e transizione ad `ARRIVED` su `distSqr <= 2.0`;
9. `testTargetNullGracefulHandling`: gestione sicura di target null senza eccezioni;
10. `testLegacyFallbackUsesInterruptTrueForOperationalEvents`: verifica che il fallback legacy chiami `MainClass.narrate` con `interrupt = true` per gli eventi operativi di movimento;
11. `testLifecycleResetReleasesKeys`: verifica che le callback Balm e gli hook di ciclo di vita rilascino fisicamente tutti i tasti di movimento;
12. `testDynamicEntityRepathOnMovement`: verifica ripianificazione quando l'entità bersaglio si sposta con `distSqr > 4.0`;
13. `testPostLandingRepath`: verifica ripianificazione post-atterraggio da dislivello (`|currentY - lastGroundY| > 0.4` o `state == JUMPING`);
14. `testSprintHysteresisAndTurnBrake`: verifica cooldown di 20 tick dello sprint post-sterzata > 15° e rilascio `keyUp` in curva > 55°;
15. `testVoiceFeedbackDisabledProducesSoundOnly`: verifica che `voiceFeedback = false` produca `OutputType.SOUND_ONLY` (campana attiva, voce muta);
16. `testNarrateHintsDisabledSuppressesProgressionAndDoors`: verifica che `narrateHints = false` sopprima sia il conteggio passi a 5 a 5 sia gli avvisi porta;
17. `testLookAtTargetOnArrivalDisabled`: verifica che con `lookAtTargetOnArrival = false` non venga ruotato lo sguardo del giocatore all'arrivo;
18. `testAutoJumpAutoSwimAndVolumeSettings`: verifica rispetto di `autoJump`, `autoSwim` e volume `config.audioCueVolume`;
19. `testAlreadyAtTargetHandling`: verifica gestione coerente del caso `ALREADY_AT_TARGET` all'avvio secondo l'Opzione A convalidata;
20. `testOperationalEventVsCriticalFastPath`: verifica che un allarme `SAFETY` (burrone/lava) prevalga istantaneamente a 0 ms interrompendo qualsiasi evento `OPERATIONAL`.

---

## 📋 10. Piano Esecutivo delle Sotto-Fasi

- [x] **Sotto-Fase 5A — Disaccoppiamento di Dominio (`RouteNavigator` & `AutoWalkMotor`)**:
  - [x] Creazione di `RouteNavigator.java` (proprietà rotta, avanzamento nodi, vincolo `Math.abs(deltaY) < 1.0`, caso `ALREADY_AT_TARGET`, repath e tracking dinamico entità);
  - [x] Creazione di `AutoWalkMotor.java` (proprietà stato FSM `State`, cinematica, sterzata 20°/tick, isteresi sprint 20 tick, frenata curve > 55°, porte chiuse/aperte, stuck watchdog, human takeover con rispetto opzioni config);
  - [x] Test unitari headless su rotta, avanzamento, stati FSM e contratti configurazione (20 test dedicati eseguiti, 228 totali verdi).
- [x] **Sotto-Fase 5B — Sottogestore `MovementCoordinator` & Supporto a `CognitiveCoordinator`**:
  - [x] Implementazione di `clearDomainEvents(SourceDomain domain)` in `CognitiveCoordinator` con pulizia di `tickBuffer`, `shortQueue` e invalidazione di `recentEvents`;
  - [x] Creazione di `MovementCoordinator.java` con factory pura di eventi `SourceDomain.MOVEMENT`, supporto esplicito `OutputType.SOUND_ONLY`, integrazione hook Balm (`ClientLifecycleCallback`) e monitoraggio tick client per morte/cambio dimensione;
  - [x] Test unitari sulla cancellazione selettiva, invalidamento deduplicazione e arbitraggio tra `OPERATIONAL` e `SAFETY` Fast-Path (16 nuovi test, 244 totali verdi).
- [x] **Sotto-Fase 5C — Armonizzazione `AutoWalkManager`, Compilazione & Test**:
  - [x] Collegamento di `AutoWalkManager` al nuovo `MovementCoordinator`;
  - [x] Conservazione della facciata legacy (con `interrupt = true` sui messaggi vocali diretti) e delle opzioni Cloth Config esistenti;
  - [x] Risoluzione e convalida micro-correzione sensoriale: helper unico `emitLegacySound`, ripristino cue acustico HAT a 0.5f su disabilitazione marcia attiva e fixture con 7 asserzioni vincolanti nel test 8;
  - [x] Compilazione con `.\gradlew.bat --no-daemon --no-watch-fs shadowJar` (build riuscita in 41s);
  - [x] Esecuzione suite di test completa (10 test di armonizzazione convalidati, 254 test verdi totali, 0 errori, 0 fallimenti).
- [x] **Sotto-Fase 5D — Deploy Proattivo, Collaudo In-Game & Revisione Correttiva Voxel (Implementazione Conclusa; Telemetria PRAPI Post-Implementazione in Corso)**:
  - [x] Deploy automatico del jar nell'istanza attiva di PrismLauncher (`*26.2*Access*`) eseguito con successo;
  - [x] Collaudo in-game Sessione 1 (ore 20:00–20:05):
    - [x] Rilevamento e risoluzione del disallineamento pitch dello sguardo su porte chiuse (forzamento `player.setXRot(0.0f)` all'altezza occhi in `AutoWalkMotor.java` e `AutoWalkMotorTest.java`, 255 test verdi, convalidato sul campo da Luca);
    - [x] Rilevamento deadlock a tick 0 su porta chiusa all'avvio da fermo e collisione su scale a soffitto basso;
  - [x] **Revisione Correttiva Condivisa Antigravity + Codex (Two-Pass Pathfinding & Headroom a 3 Volumi)**:
    - [x] **Asse 1: Two-Pass Pathfinding Deterministico e Policy Porte Chiuse (`AutoWalkPathfinder`)**:
      - Centralizzazione del metodo geometrico puro `isDoorOrGateClosed(Level level, BlockPos pos)` in `AutoWalkPathfinder`;
      - Propagazione a cascata del parametro di policy `boolean allowClosedDoors` in tutti i predicati geometrici: `isStandable`, `isPassable(level, pos, allowClosedDoors)`, vicini ortogonali e anti-snagging diagonale;
      - Controllo simmetrico di entrambi i volumi di transito: piedi (`targetPos`) e testa (`targetPos.above()`);
      - Rilevamento univoco del varco per le due metà della porta (`DoorBlock.HALF = LOWER / UPPER`), prevenendo costi duplicati o doppi annunci vocali;
      - Costante reale del budget: `MAX_EXPLORED_NODES = 2500` nodi esplorati;
      - Introduzione dello stato distinto `PathStatus.SEARCH_BUDGET_EXHAUSTED` in `PathResult` per separare l'esaurimento dei 2500 nodi da un reale `NO_PATH` (open set svuotato);
      - *Passaggio 1 (Strict Path)*: A* cerca la rotta con `allowClosedDoors = false`. Se esiste una via aperta all'esterno o libera, viene selezionata al primo colpo e l'AutoWalk parte subito senza alcun blocco a tick 0;
      - *Passaggio 2 (Fallback Path)*: Eseguito **esclusivamente** se il Passaggio 1 restituisce un reale `NO_PATH` (open set vuoto, certezza matematica di assenza di vie aperte). Con `allowClosedDoors = true`, le porte chiuse diventano transitabili applicando una singola penalità `CLOSED_DOOR_PENALTY = 30.0` per varco in `calculateStepCost`, minimizzando le porte chiuse necessarie per accedere a stanze interne;
      - *Politica di Protezione su Budget Esaurito*: Se il Passaggio 1 restituisce `SEARCH_BUDGET_EXHAUSTED`, il sistema non dichiara arbitrariamente necessaria una porta chiusa né esegue il fallback acritico, ma segnala l'impossibilità di pianificare entro il budget di sicurezza;
      - *Flusso completo di `SEARCH_BUDGET_EXHAUSTED`*: `RouteNavigator` pulisce la rotta sia in `startRoute(...)` sia in `repath(...)`; `AutoWalkMotor` arresta in sicurezza l'eventuale repath senza avviare l'attesa porta; `MovementCoordinator` inoltra il feedback terminale senza mai dichiarare che una porta sia inevitabile;
      - *Contratto I18N invariato*: nessuna nuova chiave di lingua. `SEARCH_BUDGET_EXHAUSTED` usa il messaggio storico `minecraft_access.autowalk.no_path`, semanticamente corretto perché nessuna rotta è stata trovata entro il limite sicuro. La distinzione tra budget esaurito e reale `NO_PATH` resta nel risultato di dominio, nei log diagnostici e nei test;
    - [x] **Asse 2: Headroom Geometrico e Clearance Salto Assistito a 3 Volumi (`hasJumpArcClearance`)**:
      - Implementazione del predicato geometrico `hasJumpArcClearance(Level level, BlockPos from, BlockPos targetStep)` in `AutoWalkPathfinder`;
      - Divieto tassativo di utilizzare `isPassable(...)` per il controllo del soffitto (in quanto ammette intenzionalmente botole e porte);
      - Verifica rigorosa della collisione reale: `state.getCollisionShape(level, pos).isEmpty() && !isHazard(level, pos)`;
      - Tre volumi verticali consecutivi privi di collisione solida a partire dalla pedata di atterraggio:
        1. `targetStep` (spazio piedi);
        2. `targetStep.above()` (spazio busto/testa);
        3. `targetStep.above(2)` (culmine della parabola di salto assistito);
      - Spazio di stacco sopra la testa di partenza: `from.above(2)` privo di collisione solida;
      - Per una salita diagonale, verifica equivalente dell'arco di salto sulla cella di arrivo e su entrambi i corridoi ortogonali intermedi (`ortho1` e `ortho2`): se uno dei volumi superiori necessari contiene collisione o pericolo, il candidato diagonale viene scartato;
      - Rigetto preventivo di scale o gradini in cunicoli con soffitto basso che impediscono il salto;
    - [x] **Asse 3: Separazione dei Ruoli e Controllo Fisico (`AutoWalkMotor`)**:
      - `AutoWalkMotor` delega la determinazione dello stato delle porte a `AutoWalkPathfinder.isDoorOrGateClosed`;
      - Il motore conserva la sola responsabilità fisica e sensoriale:
        - Arresto controllato a $\le 2.1$ blocchi da porte chiuse selezionate come inevitabili dal Passaggio 2;
        - Allineamento orizzontale dello sguardo con pitch a 0.0° (`Dritto`);
        - Singola richiesta vocale di apertura con debouncing;
        - Ripartenza automatica fluida non appena il blocco porta passa allo stato aperto;
        - Divieto assoluto di manovre di retromarcia forzata o arretramenti ciechi;
    - [x] **Asse 4: Suite di Test Unitari Headless (7 Scenari Vincolanti)**:
      - [x] 1. Test Strict Pass: via esterna libera selezionata aggirando porte chiuse vicine senza attese al tick 0;
      - [x] 2. Test Fallback Pass: attraversamento con penalità 30.0 attivato solo su reale `NO_PATH` (porta inevitabile);
      - [x] 3. Test Budget Esaurito: esito `SEARCH_BUDGET_EXHAUSTED` distinto da `NO_PATH`, senza falsa attestazione di necessità della porta. Il pathfinder espone esclusivamente a livello package-private un budget di esplorazione controllabile dal test; in produzione continua a usare senza eccezioni `MAX_EXPLORED_NODES = 2500`;
      - [x] 4. Test Copertura Varchi: gestione univoca di porta inferiore/superiore, cancelli e botole (aperti vs chiusi);
      - [x] 5. Test Clearance Headroom: gradini e scale con soffitto basso scartati, rampe con 3 volumi liberi accettate;
      - [x] 6. Test Flusso Terminale Budget: `RouteNavigator`, `AutoWalkMotor` e `MovementCoordinator` gestiscono `SEARCH_BUDGET_EXHAUSTED` con pulizia della rotta, arresto sicuro e messaggio storico `minecraft_access.autowalk.no_path`, senza attesa porta né fallback;
      - [x] 7. Test Regressione Suite: conservazione del 100% verde sui 255 test esistenti + 8 nuovi scenari in `AutoWalkPathfinderTest.java` (totale 263 test verdi, 0 fallimenti, 0 errori);
    - [x] Compilazione pulita con `.\gradlew.bat --no-daemon --no-watch-fs shadowJar` (build riuscita in 27s);
    - [x] Esecuzione suite automatica con `.\gradlew.bat --no-daemon --no-watch-fs test` (263 test superati con successo in 52s);
    - [x] Deploy proattivo del JAR in PrismLauncher (`Minecraft 26.2 Access 1.12.0` e `Minecraft 26.2 Access - Server Tenuta`) eseguito con successo;
    - [x] Prova in-game Sessione 2 con Luca nella tenuta (rilevamento anomalia lock visuale a 20 Hz su porta chiusa e asimmetria di costo su partenza da varco chiuso);
    - [x] **Revisione Correttiva 5D.2 — Sblocco Visuale & Normalizzazione Canonica Varchi Chiusi (Antigravity + Codex)**:
      - [x] **Asse 1: Sblocco Sguardo e Rimozione del Lock della Visuale a 20 Hz (`AutoWalkMotor`)**:
        - Spostamento di `player.lookAt(...)` e `player.setXRot(0.0f)` all'interno della guardia di primo ingresso nello stato di attesa porta (`waitingClosedDoorPos == null || !waitingClosedDoorPos.equals(doorCheckPos)`);
        - Nei tick successivi, finché la stessa porta resta chiusa, il motore mantiene fermo il movimento virtuale (`keyUp = false`, `sprinting = false`), ma non modifica più in alcun modo yaw e pitch del giocatore;
        - Controllo della telecamera garantito al 100% per Luca tramite tastiera per esplorare l'ambiente circostante;
        - Se lungo la rotta viene incontrata una diversa porta chiusa, la transizione di sguardo una tantum si ripete per la nuova coordinata;
      - [x] **Asse 2: Normalizzazione Canonica del Varco e Penalità alla Partenza (`AutoWalkPathfinder`)**:
        - Introduzione dell'helper di normalizzazione canonica: ogni porta a due blocchi viene mappata alla sua posizione inferiore (`DoorBlock.HALF == UPPER ? pos.below() : pos`); cancelli e botole mantengono la propria posizione;
        - In `calculateStepCost`, per il solo nodo radice di partenza (`current.parent == null`), verifica se la cella voxel del nodo corrente appartiene a un varco chiuso di partenza (`exitsClosedDoor`); se Luca è soltanto davanti al varco, continua invece ad applicarsi la già esistente penalità sul nodo di arrivo (`entersClosedDoor`);
        - Se `exitsClosedDoor` è vero ed è un varco distinto dal varco di arrivo (`entersClosedDoor`), il primo passo sconta la penalità `CLOSED_DOOR_PENALTY = 30.0` se il movimento attraversa il pannello della porta (bloccato nel Passaggio 1, penalizzato nel Passaggio 2), mentre allontanarsi dal varco tornando nella stanza è completamente libero da penalità;
        - Se nel primo passo vengono attraversati due varchi chiusi distinti, ciascuno riceve la propria penalità;
        - Se sorgente e destinazione appartengono alla stessa porta a due blocchi (metà inferiore/superiore), la penalità resta una sola;
      - [x] **Asse 3: Suite di Test di Accettazione Obbligatori (6 Scenari Vincolanti)**:
        - [x] 1. Test LookAt One-Shot (`testDoorWaitLookAtOneShotAndFreedomOfRotation` in `AutoWalkMotorTest`): porta chiusa in attesa causa un solo orientamento sguardo e livellamento pitch; avanzamento rilasciato e rotazione libera nei tick successivi;
        - [x] 2. Test Penalità Partenza (`testRootNodeExitingClosedDoorGetsPenalty` in `AutoWalkPathfinderTest`): uscita iniziale da varco chiuso riceve penalità 30.0 attraverso il pannello e 0.0 tornando indietro nella stanza;
        - [x] 3. Test Doppi Varchi Distinti (`testTwoDistinctClosedDoorsGetDoublePenalty` in `AutoWalkPathfinderTest`): due varchi chiusi distinti attraversati nel primo passo ricevono due penalità distinte (60.0);
        - [x] 4. Test Normalizzazione Porta (`testCanonicalDoorNormalizationHelper` in `AutoWalkPathfinderTest`): l'helper canonico restituisce il medesimo identificatore per la metà inferiore e superiore della stessa porta e quindi una sola penalità;
        - [x] 5. Test Fixture Tenuta (`testFixtureTenutaPrefersOpenDoubleDoorAtTickZero` in `AutoWalkPathfinderTest`): fixture headless con partenza da cella porta chiusa a `(0, 64, 0)` e varco aperto alternativo a `(0, 64, 4)`; la rotta preferisce l'uscita aperta al tick 0;
        - [x] 6. Test Porta Inevitabile (`testInevitableClosedDoorMaintainsHistoricSafeStop` in `AutoWalkPathfinderTest`): porta chiusa inevitabile conserva lo status FOUND in fallback, attraversa il varco e garantisce l'avviso storico e l'arresto sicuro con visuale libera.
    - [x] Compilazione ed esecuzione suite: 269 test verdi totali (0 errori, 0 fallimenti);
    - [x] Deploy proattivo eseguito nelle istanze PrismLauncher (`Minecraft 26.2 Access 1.12.0` e `Minecraft 26.2 Access - Server Tenuta`).

---

## 🛑 STOP OBBLIGATORIO PRE-5E (Gating Semantico ASTRALIS)
Il completamento implementativo della Sotto-Fase 5D, incluse le revisioni 5D.1, 5D.2 e 5D.3, sancisce la chiusura formale della **Fase 5 Stretta (Parità 1:1)**. Le osservazioni del collaudo telemetrico sono gestite come revisioni PRAPI post-implementazione e non riaprono le sotto-fasi già concluse.
È fatto assoluto divieto di avviare la Sotto-Fase 5E (Estensioni Funzionali) senza la previa stesura della Sotto-Fase 1A dedicata alle estensioni e l'esplicito comando di autorizzazione di Luca.

---

- [ ] **Sotto-Fase 5E (Post-Convalida) — Estensioni Funzionali**:
  - Introduzione di `autoOpenDoors` (apertura automatica porte durante l'autowalk);
  - Introduzione dell'enum `AutoWalkVerbosity` (silenzioso, solo suoni, cadenzato, dettagliato);
  - Esposizione comando tastiera per interrogazione navigatore (GPS manuale).

---

## 🎮 STATO FINALE: COLLAUDATO E CONVALIDATO CON SUCCESSO AL 100% (Archiviato)
In conformità al **Protocollo 3 (Deploy Proattivo)**, al **Protocollo 4 (Telemetria Live)** e al **Protocollo 6 (Chiusura Tecnica)**:
1. La Fase 5 (5A, 5B, 5C, 5D con tutte le relative revisioni correttive 5D.1 - 5D.7-R3) è completata, convalidata con successo empirico al 100% da Luca in-game e archiviata;
2. Suite completa convalidata al 100% verde (299/299 test unitari superati con successo);
3. Il JAR `minecraft-access-1.12.0-SNAPSHOT.jar` (7.43 MB) è operativo in entrambe le istanze di gioco.
