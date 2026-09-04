# Registro Attivo delle Revisioni & Affinamenti Post-Collaudo (RRU)
# Progetto: Minecraft Access (Fork 26.2 / 1.21.x)
# Autore: Luca (Sviluppatore & Collaudatore) & Antigravity (AI Pair Programmer)
# Percorso: docs/report/REGISTRO_REVISIONI.md
# Archivio Storico: docs/report/ARCHIVIO_REVISIONI.md

Questo documento costituisce il **Registro Attivo Snello** del progetto Minecraft Access. Ospita *esclusivamente* le revisioni aperte o in lavorazione. A collaudo positivo confermato da Luca, le voci vengono migrate nell'**Archivio Storico delle Revisioni** (`docs/report/ARCHIVIO_REVISIONI.md`), mantenendo questo file sempre leggero e rapido da consultare con NVDA.

---

## 📋 REVISIONI ATTIVE IN CORSO

---

### 🔵 Rev MC-26.7 — Resilienza & Fallback Traduzioni per Blocchi di Mod Terze (es. Macaw's Doors)
- **Stato**: `[APERTA]`
- **Data Rilevamento**: 2026-09-01
- **Problema Riscontrato (Esperienza Luca)**: In presenza di mod terze (es. Macaw's Doors) prive di localizzazione italiana, il mirino o il raycast vocalizzano la chiave grezza (es. *"Ostacolo di block.mcwdoors.dark_oak_barn_door a 6 blocchi"*).
- **Evidenza Telemetrica / Log**: `Narrating=block.mcwdoors.dark_oak_barn_door`.
- **Causa Radice**: La chiave non ha traduzione in `it_it.json` e il sistema vanilla restituisce la chiave non tradotta.
- **Soluzione di Affinamento (PRAPI)**:
  1. Fallback su lingua inglese (`en_us`) in `ObstacleDetectionUtils` / `WorldNarrator` quando la stringa inizia con `block.` o manca in italiano;
  2. Formattazione leggibile dall'identificatore del blocco (es. estrazione di *"dark oak barn door"* dalla chiave);
  3. Override di dizionario per le mod del modpack ufficiale in `minecraft_access/lang/it_it.json`.
- **Piano Tecnico di Riferimento**: In fase di pianificazione.
- **Esito Collaudo**: In attesa di lavorazione e collaudo.

---

### 🔵 Rev MC-26.9 — NullPointer Guard su currentScreen e Anti-Ghost in InventoryControls
- **Stato**: `[IN TELEMETRIA / PRONTO PER COLLAUDO IN-GAME]`
- **Data Rilevamento**: 2026-09-04 ore 00:59:03
- **Ambito**: Accessibilità GUI & Navigazione Griglia Inventario
- **Problema Riscontrato (Esperienza Luca)**: Durante la transizione o chiusura rapida dell'inventario verso il menu di gioco, la pressione di un tasto di navigazione slot poteva generare NPE su `currentScreen`, muovere il mouse senza contesto o produrre narrazioni residue dello slot.
- **Evidenza Telemetrica / Log**:
  ```text
  Caused by: java.lang.NullPointerException: Cannot invoke "org.mcaccess.minecraftaccess.mixin.AbstractContainerScreenAccessor.getLeftPos()" because "this.currentScreen" is null
      at knot//org.mcaccess.minecraftaccess.features.inventory_controls.InventoryControls.moveToSlotItem(InventoryControls.java:1022)
  ```
- **Soluzione Implementata (PRAPI)**:
  1. Predicato centrale `isActiveContainerScreen()` con verifica rigorosa dell'identità d'istanza (`activeScreen instanceof AbstractContainerScreen && activeScreen == currentScreen`);
  2. Sincronizzazione ciclo lifecycle in `tick()` prima del debounce dell'intervallo con `clearNavigationState()`;
  3. Guard a monte su tutti i 18 handler Kuma e su tutti i metodi di navigazione/focus (`changeGroup`, `selectGroup`, `focusSlotItemAt`, `focusSlotItem`, `changeRecipeTab`, `changeCreativeInventoryTab`, `narrateRecipeInfo`);
  4. Guard a valle in entrambi gli overload di `moveToSlotItem` (`if (slotItem == null || !isActiveContainerScreen()) return;`);
  5. Inizializzazione difensiva di `interval` con `Interval.ms(150)` e null-check su `Config.getInstance()`.
- **Piano Tecnico di Riferimento**: [`docs/piani/attivi/PIANO_TECNICO_CORRETTIVO_REV_MC-26.9_MC-26.10_GUI.md`](file:///C:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/attivi/PIANO_TECNICO_CORRETTIVO_REV_MC-26.9_MC-26.10_GUI.md)
- **Verifica Automatica**: Test unitario [`InventoryControlsLifecycleTest`](file:///C:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/test/java/org/mcaccess/minecraftaccess/features/inventory_controls/InventoryControlsLifecycleTest.java) (6 test su 6 superati al 100%).
- **Artefatto Compilato & Distribuito**: JAR SHA-256 `B3DE0E2A85F6C0E256615774ABDD1DC31861CC136C80638CE31E84EF0D20989A`.
- **Esito Collaudo**: Implementato e distribuito; in attesa del riscontro soggettivo e dei log del collaudo in-game di Luca.

---

### 🔵 Rev MC-26.10 — Soppressione Accovacciamento Non Intenzionale (Shift Sneak Hijack) all'Interno delle Schermate GUI
- **Stato**: `[IN TELEMETRIA / PRONTO PER COLLAUDO IN-GAME]`
- **Data Rilevamento**: 2026-09-04 ore 01:58
- **Ambito**: Coesistenza tra Sicurezza Movimento (`SafetyMovementGuard` / `FallDetector`) e Accessibilità Interfacce (`InventoryControls` / Kuma Hotkeys `Shift+C`, `Shift+K`, `Shift+V`, `Shift+È`)
- **Problema Riscontrato (Esperienza Luca)**: All'interno di qualsiasi interfaccia GUI (inventario, banco di lavoro, fornace, cassa), la pressione del tasto `Shift` per combinazioni di tasti o quick-move attivava contemporaneamente l'accovacciamento nel mondo con rintocchi audio `SHOVEL_FLATTEN`.
- **Soluzione Implementata (PRAPI)**:
  1. `RawCrouchIntentProvider` preservato puro al 100% come fedele lettore hardware GLFW (Single Responsibility);
  2. Metodo `suspendForGui()` in `SafetyMovementGuard` con ownership token rigoroso: rilascia il crouch con `applyIfChanged(false)` solo se `systemOverrideActive` era vero, senza toccare la postura manuale né interrogare il probe hardware;
  3. Routing esplicito in `FallDetector.tick`: se `client.gui.screen() != null`, esecuzione prioritaria di `resetSafetyStateForGui()` (che chiama `suspendForGui()`), separata dal reset ordinario nel mondo (`resetSafetyState()`);
  4. Revoca immediata di `currentAllowedDescentId` e ripresa trasparente dello Shift manuale una volta chiusa la schermata.
- **Piano Tecnico di Riferimento**: [`docs/piani/attivi/PIANO_TECNICO_CORRETTIVO_REV_MC-26.9_MC-26.10_GUI.md`](file:///C:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/attivi/PIANO_TECNICO_CORRETTIVO_REV_MC-26.9_MC-26.10_GUI.md)
- **Verifica Automatica**: 6 nuovi test di ownership e idempotenza in [`SafetyMovementGuardTest`](file:///C:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/test/java/org/mcaccess/minecraftaccess/features/safety/traversal/SafetyMovementGuardTest.java) (tutti superati al 100%).
- **Artefatto Compilato & Distribuito**: JAR SHA-256 `B3DE0E2A85F6C0E256615774ABDD1DC31861CC136C80638CE31E84EF0D20989A`.
- **Esito Collaudo**: Implementato e distribuito; in attesa del riscontro in-game di Luca.


