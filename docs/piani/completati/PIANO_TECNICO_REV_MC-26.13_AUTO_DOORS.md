# Piano Tecnico: Rev MC-26.13 — Gestione Intelligente Auto-Apri e Auto-Chiudi Porte e Varchi (AutoOpen & AutoClose Doors)

- **Autori**: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity (Senior AI Pair Programmer)
- **Ramo Git di Riferimento**: `feat/cognitive-orchestrator`
- **Piattaforma Target**: Minecraft 1.26.2 (Fabric / Java 25)
- **Framework di Governance**: ASTRALIS v3.0.2 (Fase 3 — Chiusura Tecnica & Living Documentation)
- **Stato**: `[COMPLETATO, COLLAUDATO AL 100% E CONVALIDATO DA LUCA IN-GAME]`
- **Data di Redazione**: 2026-09-07
- **Documenti Correlati**:
  * [`docs/strategie/attive/STRATEGIA_COGNITIVA_GESTIONE_PORTE_E_VARCHI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/attive/STRATEGIA_COGNITIVA_GESTIONE_PORTE_E_VARCHI.md)
  * [`docs/report/REGISTRO_REVISIONI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/REGISTRO_REVISIONI.md)
  * [`src/main/java/org/mcaccess/minecraftaccess/Config.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/Config.java)
  * [`src/main/java/org/mcaccess/minecraftaccess/features/door/DoorInteractionHelper.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/door/DoorInteractionHelper.java)
  * [`src/main/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkMotor.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkMotor.java)

---

## 🎯 1. Obiettivo Tecnico & Scenari di Accessibilità (NVDA)

Implementare e consolidare un sottosistema modulare, robusto e configurabile per:
1. **AutoOpen (Apertura Automatica)**: Aprire istantaneamente le porte, i cancelletti e le botole chiuse quando il giocatore vi si avvicina camminando in avanti (sia manualmente con `W`, sia durante la navigazione guidata di `AutoWalk`), o mentre sale una rampa di scale o una scala a pioli (`LadderBlock`/vines/scaffolding) verso una botola a soffitto, azzerando gli urti e la frustrazione del dover mirare manualmente lo stipite per sbloccare il passaggio;
2. **AutoClose (Chiusura Automatica Unificata a Zero Disorientamento della Visuale)**: Richiudere automaticamente la porta alle proprie spalle non appena il personaggio ha superato fisicamente la soglia e liberato la sagoma del battente, con la medesima architettura sia a piedi sia in AutoWalk:
   - *Nel Movimento Manuale (WASD)*: chiusura programmatica istantanea a **Zero Disorientamento della Visuale** (l'orientamento Yaw/Pitch dello sguardo dell'utente rimane inviolato al 100%, preservando la bussola mentale e la direzione di marcia). La chiusura scatta non appena il giocatore posa entrambi i piedi nel blocco adiacente alla porta ($d \ge 0.90\text{ m}$ dal centro del blocco canonico `LOWER`), emettendo il suono 3D della porta alle spalle (volume di sicurezza $0.75\text{f}$) e la notifica vocale univoca *"Porta chiusa alle spalle"*. Per le botole a soffitto su scale a pioli, la chiusura automatica scatta non appena il giocatore emerge completamente sul pavimento solido adiacente al foro $1\times 1$ ($d_{XZ} \ge 0.85\text{ m}$ dal centro del pozzo) con notifica *"Botola chiusa alle spalle"*;
   - *Nella Navigazione Automatica (AutoWalk)*: chiusura unificata tramite delega a `DoorInteractionManager` (Zero Disorientamento della Visuale, Zero Pausa Cinetica, Zero Oscillazione 180°). All'apertura della porta, AutoWalk registra la sessione di varco in `DoorInteractionManager`; il personaggio continua ad avanzare dritto lungo il percorso calcolato. Non appena oltrepassa la soglia nel blocco successivo ($d \ge 0.90\text{ m}$), il gestore richiude la porta alle spalle con il comando diretto, emette il suono 3D alle spalle e la voce *"Porta chiusa alle spalle"*, senza alcuna rotazione della testa né dipendenza dal clock/framerate del processore.

---

## 🛡️ 2. Audit Preventivo sui 6 Cancelli dell'Inner Codex (Protocollo 12)

1. **Cancello 1 — Rifiuto del Patching Euristico & Eliminazione delle Macchine a Stati Frammentate**:
   - Nessun timer cieco slegato dallo stato fisico e nessuna catena di tick fragili (Tick 1 stop, Tick 2 ruota 180°, Tick 3 riallinea) soggetta a fluttuazioni del TPS/FPS: l'attraversamento è verificato puramente dalla geometria del varco (`isPlayerAcrossDoor`), e la chiusura è un'azione atomica a visuale fissa gestita in modo centralizzato da `DoorInteractionManager`.
2. **Cancello 2 — Purezza dell'Intento Fisico nei Sistemi Ibridi (`Hardware Grounding`)**:
   - Durante la marcia in AutoWalk, qualsiasi pressione manuale di tasti fisici reali (rilevata via polling GLFW / `crouchIntent.pressed()`) attiva il Takeover istantaneo a $0\text{ ms}$, revocando l'automatismo e restituendo il pieno controllo all'utente.
3. **Cancello 3 — Integrità della Hitbox e Volumetria Continua (Soglia Calibrata 0.90m)**:
   - La chiusura automatica scatta quando la bounding box del giocatore ($0.6\text{ m}$ di larghezza, raggio $0.3\text{ m}$) ha completamente liberato il blocco 1x1 della porta ($0.5\text{ m}$ dal centro) e si trova stabilmente nel blocco adiacente ($d \ge 0.90\text{ m}$ dal centro del blocco inferiore canonico), impedendo che il battente spinga o incastri il personaggio.
4. **Cancello 4 — Disciplina dei Contratti Denominati e Chiusi**:
   - Tutte le modifiche sono articolate nei contratti formali atomici **D0..D5**, con precondizioni, postcondizioni, pulizia esplicita del codice obsoleto e invarianti anti-regressione.
5. **Cancello 5 — Determinismo Headless e Time-Seam a 0 ms**:
   - Tutte le logiche di calcolo geometrico, FSM e watchdog di timeout espongono un clock virtuale testabile a 0 ms in JUnit 5 senza `Thread.sleep`.
6. **Cancello 6 — Custode del Budget Token & Anti-Bloat**:
   - Il router `GEMINI.md` locale rimane compatto; la teoria e i dettagli risiedono unicamente nelle schede `knowledge/` e nei report di sessione.

---

## 🧪 3. Matrice di Simulazione a 3 Livelli (Validazione Preventiva)

1. **Livello 1 — Scenari Comuni (Happy Path)**:
   - *Caso 1.1 (Manuale)*: Giocatore cammina con `W` verso porta di legno chiusa $\to$ la porta si apre a $1.5\text{ m}$, il giocatore attraversa, superata la soglia di $0.90\text{ m}$ nel blocco adiacente la porta si richiude alle spalle con suono 3D OpenAL posizionato sul blocco ed annuncio vocale *"Porta chiusa alle spalle"*. Visuale fissa al 100%;
   - *Caso 1.2 (AutoWalk)*: AutoWalk attivo verso un waypoint $\to$ incontra porta chiusa $\to$ stop a $1.8\text{ m}$, vocalizza *"Porta chiusa, apertura automatica"*, apre la porta e registra la sessione in `DoorInteractionManager` $\to$ riprende la marcia in avanti sul nodo successivo $\to$ non appena supera la soglia a $0.90\text{ m}$, `DoorInteractionManager` chiude istantaneamente la porta alle spalle, riproducendo il suono 3D dietro il giocatore e la notifica *"Porta chiusa alle spalle"*, mentre il giocatore procede fluido senza interruzioni né oscillazioni dello sguardo.
2. **Livello 2 — Scenari Meno Comuni (Alternative Paths & Concorrenza)**:
   - *Caso 2.1*: La porta viene aperta dall'utente a mano con `]` o click destro prima dell'intervento automatico $\to$ AutoOpen rileva che la porta è già aperta e non invia un doppio click (evita l'apri/chiudi istantaneo);
   - *Caso 2.2*: Cancelletti di staccionata o botole orizzontali $\to$ se `includeGatesAndTrapdoors == true`, subiscono la medesima logica; se `false`, vengono ignorati;
   - *Caso 2.3*: Svolta immediata a 90° dopo la porta $\to$ il calcolo proietta il movimento sull'asse di marcia iniziale, garantendo la chiusura corretta anche in caso di ingresso in stanze a L.
3. **Livello 3 — Casi Limite (Corner Cases & Fail-Safe)**:
   - *Caso 3.1 (Porte di Ferro Meccaniche)*: Se il blocco è `Blocks.IRON_DOOR` o `Blocks.IRON_TRAPDOOR`, l'auto-apertura a mano viene bloccata a monte; AutoWalk si arresta ed enuncia *"Porta di ferro, richiede interruttore o piastra"*;
   - *Caso 3.2 (Esitazione / Retromarcia Giocatore)*: Il giocatore apre la porta, si attarda nel vano $\to$ il *Passage Renewal* mantiene vivo il timer; se indietreggia e si allontana senza varcare la soglia, trascorsi $6000\text{ ms}$ (Watchdog Timeout), la sessione di varco decade in sicurezza;
   - *Caso 3.3 (Pericolo / Burrone oltre la Porta)*: Se oltre la porta è presente lava o un precipizio non protetto, i moduli `FallDetector` e `SafetyMovementGuard` intervengono prioritariamente arrestando la marcia prima del vuoto.

---

## 📋 4. I 6 Contratti di Consegna (D0..D5)

### Contratto D0: Bonifica Preventiva & Pulizia del Codice Obsoleto (Clean Sweep)
- **File Coinvolti**:
  * `src/main/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkMotor.java`
- **Specifica di Bonifica**:
  1. *Rimozione Campi e Getter Obsoleti*:
     - Eliminazione di `doorToClosePos` e `doorCloseManeuverTicks` con relative annotazioni `@Getter` (righe 101-105);
  2. *Rimozione Reset di Stato*:
     - Eliminazione dei reset di `doorToClosePos` e `doorCloseManeuverTicks` nei metodi `start(...)` (righe 146-147) e `stop(...)` (righe 163-164);
  3. *Estirpazione Manovra Cinetica a 180°*:
     - Rimozione integrale del blocco `// 4.5. Manovra Cinetica di Chiusura Porta` (righe 257-296), eliminando arresti intermedi, manovre a 3 tick, rotazioni dello sguardo a 180° e dipendenze dal clock/framerate;
  4. *Pulizia in processDoorWait*:
     - Rimozione delle assegnazioni locali a `this.doorToClosePos` e `this.doorCloseManeuverTicks` (righe 738-741).

---

### Contratto D1: Configurazione Cloth Config & Localizzazione I18N
- **File Coinvolti**:
  * `src/main/java/org/mcaccess/minecraftaccess/Config.java`
  * `src/main/resources/assets/minecraft_access/lang/it_it.json`
  * `src/main/resources/assets/minecraft_access/lang/en_us.json`
- **Specifica**:
  - Presenza e conformità della categoria `doorInteraction` in `Config.java` (`autoOpenDoors`, `autoCloseDoors`, `includeGatesAndTrapdoors`, `doorNarration`);
  - Presenza delle chiavi di traduzione bilingue nei file JSON ordinate alfabeticamente per la conformità con `linting.yml`:
    * `minecraft_access.door.auto_opening`: *"Porta chiusa, apertura automatica"* / *"Closed door, auto-opening"*
    * `minecraft_access.door.auto_closed`: *"Porta chiusa alle spalle"* / *"Door closed behind you"*
    * `minecraft_access.door.auto_closed_trapdoor`: *"Botola chiusa alle spalle"* / *"Trapdoor closed behind you"*
    * `minecraft_access.door.iron_door_requires_switch`: *"Porta di ferro, richiede interruttore"* / *"Iron door, requires switch"*

---

### Contratto D2: Helper Voxel, Predicati Geometrici & Sonificazione 3D (`DoorInteractionHelper.java`)
- **File Coinvolto**:
  * `src/main/java/org/mcaccess/minecraftaccess/features/door/DoorInteractionHelper.java`
- **Specifica**:
  - Conferma e mantenimento dei metodi statici purs testabili a 0 ms:
    * `isInteractableClosedDoorOrGate(@Nullable BlockState state, boolean includeGatesAndTrapdoors)`: riconoscimento porte chiuse interagibili a mano;
    * `isInteractableOpenDoorOrGate(@Nullable BlockState state)`: verifica se aperta per chiusura;
    * `isPlayerAcrossDoor(Vec3 entryPos, Vec3 currentPos, BlockPos doorPos, @Nullable Direction entryFacing)`: soglia geometrica calibrata $d \ge 0.90\text{ m}$ dal centro varco normalizzato `LOWER`;
    * `isPlayerInsideDoorWay(Vec3 currentPos, BlockPos doorPos)`: vano porta ($d \le 0.65\text{ m}$) per rinnovo watchdog (*Passage Renewal*);
    * `playDoorCloseSound(@Nullable Level level, @Nullable BlockPos pos, @Nullable BlockState state)`: emissione suono 3D nativo sul blocco con volume calibrato a $0.75\text{f}$;
    * `createBlockHit(BlockPos pos, Direction side)`: interazione geometrica mirata al centro.

---

### Contratto D3: Gestore FSM Centralizzato, Registrazione Sessioni & Passage Renewal (`DoorInteractionManager.java`)
- **File Coinvolto**:
  * `src/main/java/org/mcaccess/minecraftaccess/features/door/DoorInteractionManager.java`
- **Specifica**:
  - Hub unico e centralizzato per l'esecuzione dell'AutoClose sia per il movimento manuale sia per AutoWalk:
    * Metodo pubblico `registerSession(BlockPos doorPos, Vec3 entryPos, @Nullable Direction entryDirection, long openedTime)` per accogliere sessioni sia dal manuale sia da AutoWalk;
    * Elaborazione autonoma sul client tick di tutte le sessioni attive in `processActiveDoorSessions`:
      - Controllo *Passage Renewal*: rinnovo timestamp se il giocatore è ancora nel vano ($d \le 0.65\text{ m}$);
      - Fail-safe Watchdog: decadenza silenziosa dopo 6000 ms se il giocatore indietreggia o devia rotta;
      - Chiusura a soglia varcata: appena $d \ge 0.90\text{ m}$, invio programmatico di `interactWithDoor` sul blocco, riproduzione suono 3D diegetico alle spalle, notifica vocale *"Porta chiusa alle spalle"*, e rimozione della sessione;
      - Zero Disorientamento della Visuale: Yaw e Pitch non vengono minimamente alterati.

---

### Contratto D4: Unificazione AutoClose in AutoWalk via Delega a `DoorInteractionManager` (`AutoWalkMotor.java`)
- **File Coinvolto**:
  * `src/main/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkMotor.java`
- **Specifica**:
  - In `AutoWalkMotor.processDoorWait`, non appena l'AutoOpen apre la porta con `DoorInteractionManager.interactWithDoor(client, doorPos)`:
    * Se `autoClose` è attivo (`config.doorInteraction.autoCloseDoors == true`), registra immediatamente la sessione nel gestore unificato:
      ```java
      if (autoClose && client.player != null) {
          Direction facing = client.player.getDirection();
          DoorInteractionManager.registerSession(doorPos, client.player.position(), facing, System.currentTimeMillis());
      }
      ```
    * AutoWalk prosegue senza alcuna interruzione, senza pause cinetiche, senza allineamenti a 180° e senza toccare Yaw/Pitch;
    * La chiusura avverrà in totale trasparenza e fluidità tramite `DoorInteractionManager` non appena il giocatore supera i $0.90\text{ m}$ oltre la soglia.

---

### Contratto D5: Suite di Test Unitari Headless (`DoorInteractionManagerTest.java` & `AutoWalkMotorTest.java`)
- **File Coinvolti**:
  * `src/test/java/org/mcaccess/minecraftaccess/features/door/DoorInteractionManagerTest.java`
  * `src/test/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkMotorTest.java`
- **Specifica**:
  - Test suite JUnit 5 disaccoppiata a 0 ms che verifica:
    1. Registrazione ed evasione sessione unificata con time-seam virtuale;
    2. Riconoscimento porte chiuse interagibili vs aperte vs ferro;
    3. Transito con soglia calibrata a $0.90\text{ m}$ sui 4 assi cardinali e con svolta a 90°;
    4. Riconoscimento presenza nel vano (`isPlayerInsideDoorWay`) e *Passage Renewal*;
    5. Watchdog timeout di 6000 ms;
    6. Bonifica totale di riferimenti e campi obsoleti in `AutoWalkMotor`;
    7. Suite completa `./gradlew test --no-daemon` 100% verde (323+ test a zero regressioni).

---

## 🛑 5. STOP OBBLIGATORIO (ASTRALIS v3.0.2 — Sotto-Fase 1A)

Il presente Piano Tecnico Formale conclude la **Sotto-Fase 1A**.  
In conformità rigorosa con la **Regola 0 (Default Consultivo Permanente)**, nessun file di codice sorgente (`.java`) o di configurazione verrà toccato fino alla ricezione del tuo comando esplicito (*"procedi"*, *"applica"*, *"esegui"*).
