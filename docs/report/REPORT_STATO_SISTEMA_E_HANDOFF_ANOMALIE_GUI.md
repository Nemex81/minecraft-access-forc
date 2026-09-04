# Rapporto di Stato Attuale del Sistema & Handoff Anomalie GUI

- **Progetto**: Minecraft Access (Fork 26.2 / 1.21.x)
- **Data e Ora**: 2026-09-04 — 02:10
- **Autori**: Luca (Sviluppatore & Collaudatore), Antigravity (Senior AI Pair Programmer) & ChatGPT (Senior Architectural Reviewer)
- **Branch Git Attivo**: `feat/cognitive-orchestrator`
- **Versione Locale AVF**: `v26.2-1.19.0-dev`
- **Livello Framework ASTRALIS**: `v2.6.1` (100% Sincronizzato)
- **Stato Documentale Cartella `docs/report/`**:
  - `docs/report/archivio/`: 15 report storici archiviati con successo.
  - `docs/report/REGISTRO_REVISIONI.md`: Registro snello attivo con 3 voci aperte (`Rev MC-26.7`, `Rev MC-26.9`, `Rev MC-26.10`).
  - `docs/report/ARCHIVIO_REVISIONI.md`: 8 macro-revisioni collaudate e chiuse.

---

## 🧭 1. STATO ATTUALE DELL'IMPLEMENTAZIONE DEL SISTEMA COGNITIVO CENTRALE

L'epica del **Cognitive Orchestrator** ha completato con successo le prime tre macro-fasi, con stabilità dimostrata sia nella suite di test headless deterministici (185/185 test verdi) sia nel collaudo in-game prolungato (> 1h 12m multiplayer e singleplayer):

1. **Fase 1 (Architettura di Base & Cognitive Coordinator — Completata)**:
   - Modello ad eventi immutabili `CognitiveEvent`, 4 priorità gerarchiche scalari (`CRITICAL(4)`, `OPERATIONAL(3)`, `CONTEXTUAL(2)`, `PASSIVE(1)`), canali `OutputType` e bus atomico `CognitiveCoordinator`.
   - Disaccoppiamento test seams per esecuzione headless a 0 ms senza client grafico.
2. **Fase 2 (Migrazione Dominio 1 — Mirino & Orientamento — Completata)**:
   - Migrazione di `CrosshairFeedbackManager` con eliminazione dei troncamenti vocali tramite il pattern *Token Composition & Ordering Enum*.
3. **Fase 3 (Migrazione Dominio 2 — Sicurezza Voxel & Movimento — Completata)**:
   - Sotto-Fase 3A: `FallDetector` con `SafetyMovementGuard`, modello anticaduta a 2 zone, discesa assistita su scale e botole, e neutralizzazione salto su ciglio (`cancelJumpWhenAutoSneakActive`).
   - Sotto-Fase 3B: `ObstacleDetector` con `ObstacleNarrationComposer`, parità legacy per le 4 modalità direzionali e pattern *"Silent Commit"* per prevenire i lag mutation alerts post-soppressione.
4. **Fase 4 Macro-Piano (Migrazione Dominio 3 — Prossimi Canali Percettivi — In Attesa)**:
   - Sospesa temporaneamente in modo concordato per consentire la bonifica prioritaria e chirurgica delle due anomalie GUI aperte.

---

## 🔍 2. SPECIFICA TECNICA DEFINITIVA DELLE ANOMALIE GUI APERTE

---

### A. Rev MC-26.9 — NullPointer Guard & Anti-Ghost Narration su `currentScreen` in `InventoryControls`
- **Sintomo**: Durante la navigazione dell'inventario o la chiusura rapida con `Esc`, compare nei log un'eccezione non bloccante:
  `NullPointerException: Cannot invoke "AbstractContainerScreenAccessor.getLeftPos()" because "this.currentScreen" is null`.
- **Causa Radice**: In `InventoryControls.java` (righe 1022 e 1039), i metodi `moveToSlotItem(SlotItem slotItem)` e `moveToSlotItem(SlotItem slotItem, int delay)` calcolano la posizione reale del cursore del mouse tramite `currentScreen.getLeftPos()` e `currentScreen.getTopPos()`. Se l'evento differito o la pressione di un tasto arriva quando la schermata si sta chiudendo (`currentScreen` già impostato a `null`), l'invocazione fallisce.
- **Strategia Risolutiva Rafforzata (Dual Guard)**:
  1. *Guard all'ingresso dei gestori di navigazione*: All'inizio di `changeGroup`, `selectGroup`, `focusSlotItem` e negli handler Kuma di `InventoryControls`:
     ```java
     if (currentScreen == null) return; // o return false per handler Kuma
     ```
     impedendo l'elaborazione di comandi zombie e azzerando narrazioni vocali di dati ormai obsoleti.
  2. *Guard difensivo nei metodi del mouse*:
     ```java
     if (slotItem == null || currentScreen == null) return;
     ```
     in `moveToSlotItem(SlotItem)` e `moveToSlotItem(SlotItem, int)`.

---

### B. Rev MC-26.10 — Soppressione Accovacciamento Non Intenzionale (`Shift Sneak Hijack`) in GUI
- **Sintomo Segnalato da Luca**: Quando ci si trova all'interno di una schermata GUI (inventario, tavolo da lavoro, fornace, cassa, alambicco), premendo il tasto `Shift` per eseguire una combinazione di navigazione (es. `Shift+C`, `Shift+K`, `Shift+V`) o per il trasferimento rapido, si attiva contemporaneamente l'accovacciamento nel mondo di gioco (il personaggio si china ed emette il segnale sonoro `SHOVEL_FLATTEN` pitch 0.5f, e al rilascio di Shift si rialza con pitch 0.9f). Prima di Fase 3A questo non accadeva.
- **Causa Radice Accertata**:
  1. Ad ogni tick del client di gioco, `FallDetector.tick` verifica se è presente uno schermo aperto (`client.gui.screen() != null`);
  2. Rilevando lo schermo, invoca `resetSafetyState()`, che chiama `movementGuard.clearSystemOverride()`;
  3. `SafetyMovementGuard.clearSystemOverride()` invoca internamente `reconcileCrouchState()`, il quale legge lo stato grezzo dei tasti da GLFW tramite `RawCrouchIntentProvider.readIntent()`;
  4. Poiché Luca tiene premuto `Shift` per azionare la scorciatoia dell'inventario, GLFW riporta `GLFW_KEY_LEFT_SHIFT = true`;
  5. `SafetyMovementGuard` valuta `systemOverrideActive || intent.pressed()` ($0 \lor 1 = 1$) e invoca forzatamente `MinecraftSneakOverridePort.applyEffectiveCrouch(true)`;
  6. L'adapter impone `client.player.setShiftKeyDown(true)`, modificando la postura fisica dell'entità nel mondo;
  7. Il detector `PlayerStatus` rileva la transizione di `player.isCrouching()` ed emette i suoni di accovacciamento/alzata nelle orecchie del giocatore mentre naviga nei menu.
- **Strategia Risolutiva Sistemica (Separazione Rigorosa di Responsabilità — Review ChatGPT)**:
  1. *`RawCrouchIntentProvider` (Invariato — Pure Hardware Truth)*:
     - Rimane un probe hardware puro verso GLFW. Non deve essere inquinato da logiche di interfaccia, preservando il Single Responsibility Principle (SRP).
  2. *`SafetyMovementGuard` (Nuovo Metodo di Dominio `suspendForGui()`)*:
     - Introduce il metodo esplicito:
       ```java
       public void suspendForGui() {
           currentAllowedDescentId = null;
           systemOverrideActive = false;
           if (Boolean.TRUE.equals(lastAppliedCrouch)) {
               sneakPort.applyEffectiveCrouch(false);
               lastAppliedCrouch = false;
           }
       }
       ```
     - Questo metodo revoca i token di sistema e rilascia l'accovacciamento sintetico se attivo, ma **NON invoca `reconcileCrouchState()` e NON tocca la postura del giocatore**.
  3. *`FallDetector.tick` (Routing Esplicito GUI)*:
     - Se `client.gui.screen() != null`, invoca `getMovementGuard().suspendForGui()`, azzera gli allarmi e termina il tick senza invocare `reconcileCrouchState()`.
  4. *Ripresa Naturale Post-GUI*:
     - Alla chiusura della GUI (`client.gui.screen() == null`), il tick successivo riprende il normale ciclo: se il giocatore tiene premuto `Shift` nel mondo, `reconcileCrouchState()` rileva l'intento manuale e accovaccia regolarmente il personaggio.

---

## 🧪 3. MATRICE DEI TEST DI VERIFICA (AUTOMATED & MANUAL)

La nuova sessione implementerà e certificherà 5 test dedicati:

1. **Chiusura GUI Concorrente (`InventoryControlsTest`)**:
   - Chiusura schermo tra evento e callback differita $\rightarrow$ zero eccezioni NPE, zero scritture mouse a coordinate invalide, zero narrazioni fantasma.
2. **Shift in GUI (`SafetyMovementGuardTest`)**:
   - `intentProbe` con Shift fisico premuto durante `suspendForGui()` $\rightarrow$ zero invocazioni a `sneakPort.applyEffectiveCrouch(true)`.
3. **Apertura GUI su Ciglio (`SafetyMovementGuardTest`)**:
   - Transizione in GUI mentre `systemOverrideActive == true` $\rightarrow$ rilascio pulito del solo token di sistema (`applyEffectiveCrouch(false)`).
4. **Uscita da GUI con Shift Premuto (`SafetyMovementGuardTest`)**:
   - Transizione da `suspendForGui()` a `reconcileCrouchState()` mantenendo Shift premuto $\rightarrow$ ripristino istantaneo dell'accovacciamento manuale nel mondo.
5. **Non-Regressione Voxel Totale**:
   - Suite completa dei 185 test headless (`.\gradlew.bat --no-daemon test`) per garantire che discesa assistita su scale e botole restino perfette.

---

## 🚀 4. ISTRUZIONI DI AVVIO PER LA NUOVA CHAT

All'apertura della nuova conversazione con Antigravity, l'assistente leggerà questo rapporto e sarà immediatamente pronto a:
1. Redigere il piano tecnico formale (Protocollo 1A) per `Rev MC-26.9` e `Rev MC-26.10`;
2. Applicare le modifiche su `InventoryControls.java`, `SafetyMovementGuard.java` e `FallDetector.java`;
3. Compilare, eseguire i test automatici e distribuire l'artefatto nelle istanze di collaudo.
