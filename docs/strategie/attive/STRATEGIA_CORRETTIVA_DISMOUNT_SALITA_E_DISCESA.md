# Strategia Correttiva: Risoluzione Regressione Dismount Salita & Perfezionamento Discesa Verticale

> Rettifica operativa Codex del 2026-09-12: la diagnosi e la proposta sottostanti sono integrate dai contratti D30–D41 del [piano corrente](../../piani/attivi/PIANO_TECNICO_CORRETTIVO_TRANSIZIONI_VERTICALI_E_LANDING.md), in attesa di convalida Luca. Il piano distingue presa, trasferimento e stabilità, corregge l'interpretazione della velocità di fine tick e vincola l'uso di W alla geometria. Il testo precedente resta fonte storica, non istruzione implementativa autonoma.
# Progetto: Minecraft Access (Fork 26.2 / 1.21.x)
# Autore: Antigravity (AI Pair Programmer Primario) & Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA)
# Data: 2026-09-12
# Framework: ASTRALIS v3.0.4 — Protocolli 1, 2, 4 e 12 (Inner Codex Pattern)
# Riferimento Revisione RRU: docs/report/REGISTRO_REVISIONI.md (Rev MC-26.22)
# Report di Handover Master: docs/report/REPORT_HANDOVER_CODEX_PIANO_AUTOWALK_VERTICALE.md
# Piano Tecnico Corrente (1A, attesa convalida): docs/piani/attivi/PIANO_TECNICO_CORRETTIVO_TRANSIZIONI_VERTICALI_E_LANDING.md
# Piano precedente superato: docs/piani/superati/MC-26.22/PIANO_TECNICO_CORRETTIVO_DISCESA_AUTOWALK.md

---

## 🎯 1. OBIETTIVO STRATEGICO & CONTESTO

A seguito del 6° collaudo in-game (ore 00:33..00:35 del 2026-09-12) eseguito da Luca con il JAR contenente le modifiche D23..D29 di GPT Codex (SHA-256 `56284E5478719016DB25E9F4699D18038A3C98ECB8EDF5A3B5956D6D6B249217`), è emersa un'acuta regressione in salita:
- Il personaggio avvia regolarmente la scalata (`MOUNT -> TRANSIT`), sale lungo i pioli fino alla sommità (`Y=83.88`), ma ad un passo dall'accesso al tetto del Belvedere si arresta, scivola indietro e vocalizza:
  *"Movimento bloccato sulla scala"*, *"Percorso ostruito, marcia arrestata"*.
- L'obiettivo di questa strategia è documentare in modo analitico e definitivo le cause radice isolate a livello cinematico, geometrico e volumetrico, definire le contromisure correttive per la salita e prevenire le criticità concorrenti identificate per la discesa (sia in AutoWalk, sia con Climb Assistant `Alt+S`, sia in movimento manuale da tastiera).

---

## 🔍 2. EVIDENZE FORENSI & ANALISI INCROCIATA (LOG, DATI GIOCATORE E MAPPA)

### 2.1 Posizione del Personaggio e Topologia del Belvedere
- **Salvataggio giocatore**: `players/data/e48e6275-dac3-40de-8d53-17ec4b51515e.dat`.
- **Coordinate reali**: `X=-59.417, Y=81.000, Z=-41.488`, `Yaw=4.8` (Nord), `Pitch=0.0`. Luca si trova sulla balconata, ai piedi della scala della Torre Belvedere.
- **Topologia Voxel**:
  - Colonna scala a pioli: `X=-60, Z=-42`, estesa da `Y=82` a `Y=84` (`LadderBlock.FACING=NORTH`, supporto solido sul lato Sud a `Z=-41`).
  - Balconata inferiore: calpestabile a `Y=81.0`.
  - Tetto sommitale: calpestabile a `Y=85.0`, con apertura/foro scala a `(-60, 85, -42)` e piano di sbarco/landing a `(-60, 85, -41)`.

### 2.2 Telemetria Reale dei Log di Gioco (`latest.log` ore 00:33:59..00:35:25)
Nelle prove registrate per le rotte 40, 41, 42, 43, 44, la sequenza telemetrica tick per tick mostra una dinamica deterministica e invariante:

1. **Aggancio Iniziale (ore 00:35:23)**:
   `[Climb FSM] routeRevision=44, columnId=wall_mounted:-60,82..84,-42, phase=MOUNT->TRANSIT, pos=(-59.42, 81.42, -41.49), velocityY=0.333, yaw=0.0, keyUp=true, keyJump=false`
   - *Analisi*: Il salto iniziale aggancia la scala; la transizione a `TRANSIT` avviene regolarmente.
2. **Raggiungimento della Cima e Transizione a Dismount (ore 00:35:24)**:
   `[Climb FSM] routeRevision=44, columnId=wall_mounted:-60,82..84,-42, phase=TRANSIT->DISMOUNT, pos=(-59.42, 83.88, -41.49), velocityY=0.118, yaw=0.0, keyUp=true, rung=BlockPos{x=-60, y=84, z=-42}, landing=BlockPos{x=-60, y=85, z=-41}`
   - *Analisi*: Il giocatore supera la soglia `Y >= 84.0 - 0.15 = 83.85`. Essendo l'ultimo piolo transit, `evaluateTransit` promuove la fase a `DISMOUNT` e fa avanzare il waypoint al landing del tetto.
3. **Scivolamento Gravitazionale e Aborto Watchdog (ore 00:35:25, esattamente 15 tick dopo)**:
   `[Climb FSM] routeRevision=44, columnId=wall_mounted:-60,82..84,-42, phase=DISMOUNT->DISMOUNT, pos=(-59.42, 82.49, -41.49), velocityY=-0.225, yaw=current, keyUp=false, lease=RELEASE, reason=OUTSIDE_COLUMN_ABORT, outcome=STUCK_ABORT`
   - *Analisi*: La quota Y scende da `83.88` a `82.49`, la velocità Y diventa fortemente negativa (`-0.225`), `keyUp` è `false`. Scattano 15 tick consecutivi di stallo e il watchdog chiude con `OUTSIDE_COLUMN_ABORT` e `STUCK_ABORT`.
4. **Oscillazione Ciclica alla Base (Rotta 37, ore 00:33:41..00:33:46)**:
   - Posizione giocatore a `(-59.00, 81.00, -41.49)`. A 1 metro di distanza orizzontale da `X=-60`, il salto raggiunge `81.42` entrando in `TRANSIT` per progresso metrico, ma senza contatto scala reale (`onClimbable=false`). Il personaggio ricade a terra a `81.00`, generando un rimbalzo `TRANSIT -> MOUNT -> TRANSIT` ogni 15 tick con esito `REMOUNT_RETRY`.

---

## 🔬 3. DIAGNOSI TECNICA DEL CODICE SORGENTE

### 3.1 La Causa Radice del Dismount in Salita (`ClimbKinematics.java` e `ClimbLandingProbe.java`)
Nel modulo `ClimbKinematics.java`, la routine `evaluateDismount` (linee 562–630) è stata riscritta da GPT Codex assumendo implicitamente che ogni dismount sia una discesa o un arrivo su superficie complanare:

1. **Assenza Totale di Consapevolezza Direzionale**: `evaluateDismount` non interroga mai `snapshot.isAscent()`.
2. **Il Blocco di `transferSafe` in Quota**:
   - `evaluateDismount` assegna l'input virtuale di marcia: `keyUp = snapshot.landingResult().transferSafe()`.
   - In `ClimbLandingProbe.java` (linee 50–51 e 66–88), `transferSafe` delega a `isFootprintSupported`, che a sua volta invoca `hasSupportAt(..., playerBox.minY, landingPos)`.
   - `hasSupportAt` esige che la superficie del blocco di supporto coincida con la quota piedi corrente entro `HEIGHT_EPSILON` (0.08 m):
     `Math.abs(world.maxY - feetY) <= 0.08`.
   - Tuttavia, in salita alla sommità della scala, `feetY` è `83.88` (il giocatore è ancora aggrappato ai pioli), mentre il pavimento del tetto è a quota `Y=85.0`.
   - Il dislivello di `1.12 m` fa fallire sistematicamente il controllo: `hasSupportAt` restituisce `false`, `transferSafe` diventa `false` e `keyUp` viene forzato a `false`.
3. **Conseguenza Meccanica Vanilla**:
   - In Minecraft, su una ladder senza `W` premuto e senza sneak, il giocatore non sale e non resta fermo: scivola verso il basso per gravità.
   - Il personaggio scende da `83.88` a `82.49`.
   - Trascorsa la finestra del watchdog (15 tick = 0.75 s) senza aver raggiunto `onGround` sul landing, `evaluateDismount` ritorna `STUCK_ABORT` con codice `OUTSIDE_COLUMN_ABORT`.
4. **Perché i Test Unitari Precedenti Erano Verdi**:
   - In `ClimbKinematicsTest.java`, i test di dismount simulavano il giocatore già collocato a quota landing (`Vec3.atBottomCenterOf(landing)`) oppure fornivano un mock statico `new ClimbLandingProbe.Result(true, true, true, true)`. Nessun test eseguiva la transizione dinamica reale dal piolo `Y=83.88` al tetto `Y=85.0`.

---

### 3.2 Analisi Preventiva della Discesa con AutoWalk (`CAPTURE_WAIT`)
Nella cinematica di discesa dall'alto (`evaluateDescentMount`, linee 321–338):
1. Quando il giocatore si avvicina al foro della scala sul tetto (`Z <= -41.45`), `centerReachedCaptureBand` attiva la sotto-fase `CAPTURE_WAIT`.
2. In `CAPTURE_WAIT`, `keyUp` viene forzato immediatamente a `false`.
3. Se l'inerzia residua del passo non è sufficiente a vincere l'attrito del blocco di pietra prima che la AABB cada nel vuoto del varco, il personaggio si ferma immobile sul ciglio del tetto.
4. Rimanendo fermo con `keyUp=false`, dopo 15 tick il watchdog scatta con `OUTSIDE_COLUMN_ABORT`, abortendo la discesa prima ancora di agganciare la scala.

---

### 3.3 Analisi della Discesa con Climb Assistant (`Alt+S`)
- In `ClimbAssistantController.java`, la selezione contestuale della direzione invoca `ClimbRouteAssembler.assembleClimbRoute` e avvia la marcia tattica tramite `coordinator.startTacticalRoute`.
- Poiché la sessione tattica è eseguita dallo stesso identico motore (`AutoWalkMotor`) e dallo stesso modulo cinematico (`ClimbKinematics`), il Climb Assistant presenta al 100% la stessa vulnerabilità di stallo sul landing in salita e la stessa sensibilità in `CAPTURE_WAIT` in discesa.

---

### 3.4 Analisi della Discesa in Movimento Manuale (Tastiera WASD)
- Nel movimento manuale, la protezione anticaduta è gestita in tempo reale da `ProximityFallDetector.java` e `TraversalSafetyAnalyzer.java`.
- Se il giocatore muove fisicamente `W` verso il foro della scala:
  - `moveDir` non è nullo;
  - `TraversalSafetyAnalyzer.analyzeTraversal` rileva la scala sottostante ed emette `SAFE_DESCENT_AVAILABLE`;
  - `SafetyMovementGuard.allowValidatedDescent` concede il passaggio senza applicare lo sneak forzato.
- **Punto Critico Rilevato**: Se il giocatore rilascia i tasti (`moveDir == null`) mentre si trova sul bordo prima di agganciare la scala, la riga 194 di `ProximityFallDetector` invoca `revokeValidatedDescent()`, e `isStandingOnDangerousEdge` inserisce lo sneak meccanico di emergenza (`engageFallProtection`), frenando il giocatore. Questo presidio è corretto per il gameplay manuale, ma conferma la necessità che le routine automatiche mantengano una lease attiva non revocabile dal detector generico.

---

## 📐 4. MODELLO LOGICO "SE... ALLORA" (ACCESSIBILITÀ COGNITIVA NVDA)

1. **Se** il percorso è in salita (`isAscent == true`) ed entra in `DISMOUNT` alla sommità della scala:
   - **Allora** il landing (`Y=85.0`) si trova al di sopra dei piedi (`Y=83.88`) e `ClimbLandingProbe` fallisce il test complanare;
   - **Allora** `evaluateDismount` disattiva `keyUp`;
   - **Allora** il personaggio scivola all'indietro per gravità e dopo 15 tick il watchdog interrompe la marcia con *"Percorso ostruito, marcia arrestata"*.

2. **Se** il percorso è in discesa dall'alto (`isAscent == false`) ed entra in `CAPTURE_WAIT`:
   - **Allora** `keyUp` viene forzato a `false` prima dell'attraversamento verticale;
   - **Allora** l'attrito superficiale può arrestare il personaggio sul ciglio;
   - **Allora** dopo 15 tick scatta il timeout di `OUTSIDE_COLUMN_ABORT`.

3. **Se** il personaggio alla base della scala tenta il Mount da oltre 0.8 m di distanza orizzontale:
   - **Allora** il salto non raggiunge la bounding box dei pioli;
   - **Allora** la mancanza di aderenza produce un loop ciclico `MOUNT <-> TRANSIT` con `REMOUNT_RETRY`.

---

## 🛠️ 5. PIANO DI AZIONE & SPECIFICHE DELLA CORREZIONE

La correzione ingegneristica si articola in 4 pilastri:

### Pilastro 1: Differenziazione Bidirezionale in `evaluateDismount`
- Introdurre la biforcazione esplicita basata su `snapshot.isAscent()`:
  - **In Salita (`isAscent == true`)**:
    1. Il motore deve mantenere attivi `keyUp = true` e l'orientamento verso il supporto/landing finché il giocatore non è fisicamente a terra sul landing (`playerOnGround == true` con quota piedi compatibile con `landingPos.getY()`);
    2. Durante il transito tra l'ultimo piolo e il landing, il watchdog deve misurare il progresso verticale e la riduzione della distanza euclidea verso il centro del blocco di landing, evitando di dichiarare aborto se il giocatore sta avanzando regolarmente;
    3. Il completamento (`Outcome.COMPLETED`) richiede due tick consecutivi stabili a terra sul landing (`landingResult.stable()`).
  - **In Discesa (`isAscent == false`)**:
    1. Mantenere la verifica di trasferimento protetto e lo spegnimento di `keyUp` su landing già sostenuto per prevenire cadute oltre il parapetto.

### Pilastro 2: Tolleranza Quota in `ClimbLandingProbe` per la Salita
- In `ClimbLandingProbe.evaluate`:
  - Se il controllo viene eseguito durante una salita, la ricerca del supporto (`hasSupportAt`) non deve vincolare `feetY` alla quota corrente dei piedi se il giocatore è ancora sotto la superficie del landing, bensì verificare che il blocco di destinazione sia effettivamente solido e libero da ostacoli alla quota target (`landingPos.getY()`).

### Pilastro 3: Fluidità di Ingresso in Discesa (`CAPTURE_WAIT`)
- In `evaluateDescentMount`:
  - Garantire che l'azzeramento di `keyUp` in `CAPTURE_WAIT` avvenga solo dopo che la swept AABB o il baricentro hanno iniziato l'attraversamento del bordo verso la colonna, evitando che il personaggio rimanga bloccato per attrito sul pavimento prima del foro.

### Pilastro 4: Suite di Test Headless a 0 ms (`ClimbKinematicsTest`)
- Aggiungere test deterministici specifici:
  1. `testAscentDismountMaintainsKeyUpUntilGroundedOnLanding`: simula la salita dal piolo sommitale (`Y=83.88`) fino a `Y=85.0`, verificando che `keyUp` rimanga `true` e che il dismount si completi solo a terra sul tetto.
  2. `testAscentDismountDoesNotAbortWhileProgressingTowardsLanding`: verifica che il watchdog non scatti durante la transizione ascensionale.
  3. `testBelvedereDescentEntersCaptureWithoutEdgeFrictionStall`: verifica l'ingresso in discesa dal tetto.

---

## 🔗 6. RETE DOCUMENTALE & POINTER DRY
- **Report di Handover Primario**: [`docs/report/REPORT_HANDOVER_CODEX_PIANO_AUTOWALK_VERTICALE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/REPORT_HANDOVER_CODEX_PIANO_AUTOWALK_VERTICALE.md) (Appendice 4.6).
- **Registro Revisioni RRU**: [`docs/report/REGISTRO_REVISIONI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/REGISTRO_REVISIONI.md) (Rev MC-26.22).
- **Piano Tecnico Formale**: [`docs/piani/superati/MC-26.22/PIANO_TECNICO_CORRETTIVO_DISCESA_AUTOWALK.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/superati/MC-26.22/PIANO_TECNICO_CORRETTIVO_DISCESA_AUTOWALK.md).
