# Rapporto di Stato Attuale del Sistema & Handoff Anomalie GUI

- **Progetto**: Minecraft Access (Fork 26.2 / 1.21.x)
- **Data e Ora**: 2026-09-04 — 02:00
- **Autori**: Luca (Sviluppatore & Collaudatore) & Antigravity (Senior AI Pair Programmer)
- **Branch Git Attivo**: `feat/cognitive-orchestrator`
- **Versione Locale AVF**: `v26.2-1.19.0-dev`
- **Livello Framework ASTRALIS**: `v2.6.1` (100% Sincronizzato)
- **Stato Documentale Cartella `docs/report/`**:
  - `docs/report/archivio/`: 15 report storici archiviati con successo.
  - `docs/report/REGISTRO_REVISIONI.md`: Registro snello attivo con 3 voci aperte (`Rev MC-26.7`, `Rev MC-26.9`, `Rev MC-26.10`).
  - `docs/report/ARCHIVIO_REVISIONI.md`: 8 macro-revisioni collaudate e chiuse.

---

## 🧭 1. STATO ATTUALE DELL'IMPLEMENTAZIONE DEL SISTEMA COGNITIVO CENTRALE

L'epica del **Cognitive Orchestrator** ha completato con successo le sue prime tre macro-fasi, raggiungendo una stabilità eccezionale sia nei test headless deterministici (185/185 test verdi) sia nel collaudo in-game prolungato (> 1h 12m multiplayer e singleplayer):

1. **Fase 1 (Architettura di Base & Cognitive Coordinator — Completata)**:
   - Modello ad eventi immutabili `CognitiveEvent`, priorità scalari (`CRITICAL`, `SAFETY`, `OPERATIONAL`, `ENVIRONMENT`, `BACKGROUND`), canali `OutputType` e bus atomico `CognitiveCoordinator`.
   - Disaccoppiamento test seams per esecuzione headless a 0 ms senza client grafico.
2. **Fase 2 (Migrazione Dominio 1 — Mirino & Orientamento — Completata)**:
   - Migrazione di `CrosshairFeedbackManager` con eliminazione dei troncamenti vocali tramite il pattern *Token Composition & Ordering Enum*.
3. **Fase 3 (Migrazione Dominio 2 — Sicurezza Voxel & Movimento — Completata)**:
   - Sotto-Fase 3A: `FallDetector` con `SafetyMovementGuard`, modello anticaduta a 2 zone, discesa assistita su scale e botole, e neutralizzazione salto su ciglio (`cancelJumpWhenAutoSneakActive`).
   - Sotto-Fase 3B: `ObstacleDetector` con `ObstacleNarrationComposer`, parità legacy per le 4 modalità direzionali e pattern *"Silent Commit"* per prevenire i lag mutation alerts post-soppressione.
4. **Fase 4 Macro-Piano (Migrazione Dominio 3 — Prossimi Canali Percettivi — In Attesa)**:
   - Sospesa temporaneamente per consentire la bonifica prioritaria delle anomalie aperte nell'accessibilità delle interfacce GUI.

---

## 🔍 2. DIAGNOSI DETTAGLIATA DELLE ANOMALIE GUI APERTE

Prima di riprendere le espansioni del sistema cognitivo, la nuova chat si focalizzerà sul PRAPI dedicato a due anomalie correlate all'esperienza nelle interfacce:

---

### A. Rev MC-26.9 — NullPointer su `currentScreen` in `InventoryControls.moveToSlotItem`
- **Sintomo**: Durante la navigazione dell'inventario o la chiusura rapida con `Esc`, compare nei log un'eccezione non bloccante:
  `NullPointerException: Cannot invoke "AbstractContainerScreenAccessor.getLeftPos()" because "this.currentScreen" is null`.
- **Causa Radice**: In `InventoryControls.java` (righe 1022 e 1039), i metodi `moveToSlotItem(SlotItem slotItem)` e `moveToSlotItem(SlotItem slotItem, int delay)` calcolano la posizione reale del cursore del mouse tramite `currentScreen.getLeftPos()` e `currentScreen.getTopPos()`. Se l'evento differito o la pressione di un tasto arriva quando la schermata si sta chiudendo (`currentScreen` già impostato a `null`), l'invocazione fallisce.
- **Strategia Risolutiva**:
  Inserire il guard difensivo atomico all'inizio di entrambi i metodi:
  ```java
  if (slotItem == null || currentScreen == null) return;
  ```
  e analogamente in `focusSlotItem`.

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
- **Strategia Risolutiva Sistemica a 2 Barriere**:
  1. *Barriera Contestuale (`RawCrouchIntentProvider`)*:
     - Se `client.screen != null` (qualsiasi interfaccia, menu o chat attiva), il tasto `Shift` appartiene all'ambiente GUI (modificatore tastiera o quick-move). `RawCrouchIntentProvider.readIntent()` deve restituire immediatamente `new CrouchIntent(false, true)`.
  2. *Barriera Posturale (`FallDetector.resetSafetyState`)*:
     - Durante la permanenza in una schermata (`client.gui.screen() != null`), `FallDetector` si limita a rilasciare l'override di sistema senza sincronizzare la postura del giocatore con il tasto Shift fisico.

---

## 🎯 3. PIANO D'AZIONE PER LA NUOVA SESSIONE (READY-TO-EXECUTE)

All'apertura della nuova chat con Antigravity, l'assistente leggerà questo rapporto e procederà immediatamente con:
1. **Applicazione della correzione per `Rev MC-26.9`** su `InventoryControls.java`;
2. **Applicazione della correzione per `Rev MC-26.10`** su `RawCrouchIntentProvider.java` e `FallDetector.java`;
3. **Esecuzione suite test headless** (`.\gradlew.bat --no-daemon test`);
4. **Compilazione & Deploy automatico** (`.\gradlew.bat --no-daemon shadowJar`) nelle istanze PrismLauncher per il test in-game di Luca;
5. **Chiusura e migrazione delle revisioni collaudate** in `docs/report/ARCHIVIO_REVISIONI.md`.
