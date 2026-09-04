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

### 🔵 Rev MC-26.9 — NullPointer Guard su currentScreen in InventoryControls.moveToSlotItem
- **Stato**: `[APERTA]`
- **Data Rilevamento**: 2026-09-04 ore 00:59:03
- **Ambito**: Accessibilità GUI & Navigazione Griglia Inventario (Autonoma, indipendente dal Cognitive Coordinator)
- **Problema Riscontrato (Esperienza Luca)**: Durante la transizione o chiusura rapida dell'inventario verso il menu di gioco, la pressione di un tasto di navigazione slot genera un'eccezione non gestita. Il gioco non è andato in crash e non sono stati creati crash report, ma il difetto va corretto con un guard difensivo.
- **Evidenza Telemetrica / Log**:
  ```text
  Caused by: java.lang.NullPointerException: Cannot invoke "org.mcaccess.minecraftaccess.mixin.AbstractContainerScreenAccessor.getLeftPos()" because "this.currentScreen" is null
      at knot//org.mcaccess.minecraftaccess.features.inventory_controls.InventoryControls.moveToSlotItem(InventoryControls.java:1022)
      at knot//org.mcaccess.minecraftaccess.features.inventory_controls.InventoryControls.focusSlotItem(InventoryControls.java:1002)
      at knot//org.mcaccess.minecraftaccess.features.inventory_controls.InventoryControls.selectGroup(InventoryControls.java:1134)
  ```
- **Causa Radice**: In `InventoryControls.moveToSlotItem` manca il controllo preventivo `if (this.currentScreen == null) return;` prima di accedere ai metodi dell'accessor durante eventi di input concorrenti alla chiusura dello schermo.
- **Soluzione di Affinamento (PRAPI)**: Inserimento del null check difensivo `if (this.currentScreen == null) return;` in `moveToSlotItem` e `focusSlotItem`.
- **Piano Tecnico di Riferimento**: In fase di pianificazione (sessione futura dedicata a GUI/Inventari).
- **Esito Collaudo**: Aperta per lavorazione successiva.

---

### 🔵 Rev MC-26.10 — Soppressione Accovacciamento Non Intenzionale (Shift Sneak Hijack) all'Interno delle Schermate GUI
- **Stato**: `[APERTA]`
- **Data Rilevamento**: 2026-09-04 ore 01:58
- **Ambito**: Coesistenza tra Sicurezza Movimento (`SafetyMovementGuard` / `FallDetector`) e Accessibilità Interfacce (`InventoryControls` / Kuma Hotkeys `Shift+C`, `Shift+K`, `Shift+V`, `Shift+È`)
- **Problema Riscontrato (Esperienza Luca)**: Quando ci si trova all'interno di una schermata di gioco (inventario, banco di lavoro, fornace, baule, alambicco), premendo il tasto `Shift` per eseguire una combinazione di navigazione (es. `Shift+C`, `Shift+K`, `Shift+V`) o per il trasferimento rapido, si attiva contemporaneamente l'accovacciamento nel mondo di gioco (il personaggio si china ed emette il segnale sonoro `SHOVEL_FLATTEN`, rialzandosi al rilascio). Prima di Fase 3A questo effetto collaterale non era presente.
- **Evidenza Telemetrica & Diagnosi**:
  - `FallDetector.tick` invoca `resetSafetyState()` ad ogni tick quando `client.gui.screen() != null`;
  - `resetSafetyState()` chiama `movementGuard.clearSystemOverride()`, che invoca `reconcileCrouchState()`;
  - `SafetyMovementGuard` interroga GLFW tramite `RawCrouchIntentProvider.readIntent()`, che rileva la pressione fisica di `GLFW_KEY_LEFT_SHIFT`;
  - Poiché `intent.pressed()` è vero, il guard invoca `MinecraftSneakOverridePort.applyEffectiveCrouch(true)`, che chiama forzatamente `client.player.setShiftKeyDown(true)`;
  - Il cambio di postura attiva il listener `PlayerStatus`, che emette i rintocchi acustici di accovacciamento nel mondo mentre l'utente è dentro il menu.
- **Causa Radice**: `RawCrouchIntentProvider` e `SafetyMovementGuard` campionano il tasto `Shift` fisico da GLFW senza verificare se è attiva una GUI (`client.screen != null`), interpretando un tasto modificatore di navigazione dell'inventario come un comando fisico di movimento del giocatore.
- **Soluzione di Affinamento (PRAPI)**:
  1. *Guard Contestuale Schermo*: Se `client.screen != null`, `RawCrouchIntentProvider` restituisce `new CrouchIntent(false, true)` (nessun intento di accovacciamento quando si opera su una GUI);
  2. *Disimpegno Pulito in `resetSafetyState`*: Quando una GUI è aperta, `SafetyMovementGuard` rilascia qualsiasi override di sistema attivo (`sneakPort.applyEffectiveCrouch(false)`) senza imporre lo stato GLFW raw all'entità giocatore.
- **Piano Tecnico di Riferimento**: In fase di pianificazione (sessione correttiva anomalie GUI & Sicurezza).
- **Esito Collaudo**: Aperta per lavorazione.


