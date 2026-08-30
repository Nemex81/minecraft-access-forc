# Piano Tecnico di Implementazione: Modulo Numpad Controls (Minecraft Access)

Documento di specifica tecnica, analisi architetturale e validazione per il nuovo modulo di controllo tramite tastierino numerico (**Numpad Controls**) in **Minecraft Access**.

---

## 1. Executive Summary & Obiettivi di Progetto

L'obiettivo del modulo è fornire una consolle tattile integrata a 17 tasti fisici sul tastierino numerico, consentendo ai giocatori non vedenti di gestire:
1. **Controllo dello sguardo e orientamento**: sia continuo/discreto (relativo) che assoluto (punti cardinali, diagonali, nadir, zenith, snap).
2. **Scansione radar, tracciamento e lock-on**: navigazione rapida tra gruppi di punti di interesse (POI), singoli bersagli, marcature e lock-on automatico della visuale.
3. **Interazione ed emulazione mouse**: attacco/scavo continuo (`+`), uso/interazione continua (`Enter`), sblocco rapido (`-`), selezione rapida hotbar (`/`, `*`).
4. **Ispezione rapida di stato e ambiente**: stato salute/fame/armatura, equipaggiamento mano destra/sinistra, meteo, luce, bioma, coordinate e Access Menu.
5. **Configurabilità e Rimappabilità Totale**: sezione dedicata nel menu controlli, master switch, parametri fini regolabili e preset per destrorsi e mancini.

Il modulo opera in **parallelo e perfetta coesistenza** con i controlli da tastiera esistenti, lasciandoli pienamente operativi.

---

## 2. Architettura del Modulo & Integrazione nel Framework

```
+-----------------------------------------------------------------------------------+
|                              NumpadControls Module                                |
|                        (BalmClientModule + Kuma KeyMappings)                      |
+-----------------------------------------------------------------------------------+
       |                    |                       |                     |
       v                    v                       v                     v
+---------------+   +----------------+   +--------------------+   +-----------------+
| CameraControls|   | ObjectTracker  |   |    MouseUtils      |   |   HUD & Status  |
| & Orientation |   | & Locking      |   | (Press/Hold/Wheel) |   |  (PlayerStatus, |
| (Yaw/Pitch)   |   | (POI/Waypoints)|   |                    |   |  PositionUtils) |
+---------------+   +----------------+   +--------------------+   +-----------------+
```

### 2.1 Componenti Coinvolti
1. **`KeyMappingCategories.java`**:
   - Aggiunta della costante:
     ```java
     public static final String NUMPAD_CONTROLS = "key.categories.minecraft_access.numpad_controls";
     ```
2. **`Config.java`**:
   - Definizione della classe interna configurabile con annotazioni Cloth Config:
     ```java
     @ConfigEntry.Gui.CollapsibleObject
     public NumpadControls numpadControls = new NumpadControls();

     public static class NumpadControls {
         @ConfigEntry.Gui.Tooltip
         public boolean enabled = true;

         public enum HandednessPreset {
             RIGHT_HANDED,
             LEFT_HANDED
         }

         @ConfigEntry.Gui.Tooltip
         public HandednessPreset preset = HandednessPreset.RIGHT_HANDED;

         @ConfigEntry.BoundedDiscrete(min = 1, max = 90)
         @ConfigEntry.Gui.Tooltip
         public float normalRotatingAngle = 15.0f;

         @ConfigEntry.BoundedDiscrete(min = 5, max = 180)
         @ConfigEntry.Gui.Tooltip
         public float modifiedRotatingAngle = 45.0f;

         @ConfigEntry.Gui.Tooltip
         public boolean continuousRotation = true;

         @ConfigEntry.BoundedDiscrete(min = 1, max = 5)
         @ConfigEntry.Gui.Tooltip
         public float continuousRotationSpeed = 1.0f;

         @ConfigEntry.Gui.Tooltip
         public boolean invertYAxis = false;

         @ConfigEntry.Gui.Tooltip
         public boolean narrateFacingOnChange = true;

         @ConfigEntry.Gui.Tooltip
         public boolean enableContinuousHold = true;

         @ConfigEntry.BoundedDiscrete(min = 50, max = 500)
         @ConfigEntry.Gui.Tooltip
         public int scrollDelayMilliseconds = 150;

         @ConfigEntry.Gui.Tooltip
         public boolean narrateDistanceOnSelect = true;

         @ConfigEntry.Gui.Tooltip
         public boolean autoLookOnLock = true;

         @ConfigEntry.Gui.Tooltip
         public boolean playCardinalSnapSound = true;

         @ConfigEntry.BoundedDiscrete(min = 0, max = 1)
         @ConfigEntry.Gui.Tooltip
         public float audioCueVolume = 1.0f;
     }
     ```
3. **`NumpadControls.java` (`org.mcaccess.minecraftaccess.features`)**:
   - Implementa `BalmClientModule`.
   - Registra le associazioni Kuma separate per ciascun layer di modificatori (`None`, `SHIFT`, `CONTROL`, `ALT`).
   - Verifica ad ogni esecuzione il flag `Config.getInstance().numpadControls.enabled`.
   - Implementa `ClientTickCallback.AFTER` per:
     - **Rotazione continua della telecamera su tenuta prolungata (Hold)**: se `continuousRotation = true`, monitora `key.isDown()` dopo una soglia naturale di 200ms applicando una rotazione fluida progressiva ogni tick con arresto istantaneo al rilascio.
     - **Attacco e uso continui del mouse**: monitoraggio `isDown()` per `+` (tasto sinistro) ed `Enter` (tasto destro).
4. **`MainClass.java`**:
   - Registrazione del modulo `public static NumpadControls numpadControls;` nel ciclo di vita di Balm.

---

## 3. Matrice Completa dei Keybindings & Mapping Tecnico

Tutti i binding sono registrati nella categoria `KeyMappingCategories.NUMPAD_CONTROLS` e risultano rimappabili dall'utente nel menu standard di Minecraft (`Opzioni...` -> `Controlli...` -> `Assegnazione tasti...`).

### 3.1 Layer 0: Numpad Diretto (Preset Destrorso Ergonomico)

| Tasto GLFW | Modificatore | ID Binding Kuma | Azione / Metodo Chiamato (Dual-Mode: Tap Discreto / Hold Continuo) |
| :--- | :--- | :--- | :--- |
| `KEY_NUMPAD8` | Nessuno | `numpad.camera.look_up` | Guarda in alto (`normalRotatingAngle` su tap / Rotazione continua su hold) |
| `KEY_NUMPAD2` | Nessuno | `numpad.camera.look_down` | Guarda in basso (`normalRotatingAngle` su tap / Rotazione continua su hold) |
| `KEY_NUMPAD4` | Nessuno | `numpad.camera.look_left` | Ruota a sinistra (`normalRotatingAngle` su tap / Rotazione continua su hold) |
| `KEY_NUMPAD6` | Nessuno | `numpad.camera.look_right` | Ruota a destra (`normalRotatingAngle` su tap / Rotazione continua su hold) |
| `KEY_NUMPAD7` | Nessuno | `numpad.camera.look_up_left` | Diagonale Alto-Sinistra (`modifiedRotatingAngle` / Hold continuo) |
| `KEY_NUMPAD9` | Nessuno | `numpad.camera.look_up_right` | Diagonale Alto-Destra (`modifiedRotatingAngle` / Hold continuo) |
| `KEY_NUMPAD1` | Nessuno | `numpad.camera.look_down_left` | Diagonale Basso-Sinistra |
| `KEY_NUMPAD3` | Nessuno | `numpad.camera.look_down_right` | Diagonale Basso-Destra |
| `KEY_NUMPAD5` | Nessuno | `numpad.camera.center_crosshair` | Centra orizzonte (Pitch 0°, rintocco sonoro `playSnapSound` e voce opzionale) + Narra mirino |
| `KEY_NUMPAD0` | Nessuno | `numpad.mouse.left_click` | **Azione Primaria**: Attacco / Scavo (Simulazione click sinistro con hold) |
| `KEY_NUMPADENTER` | Nessuno | `numpad.mouse.right_click` | **Azione Secondaria**: Usa / Piazza / Mangia (Simulazione click destro con hold) |
| `KEY_NUMPADDECIMAL` | Nessuno | `numpad.status.player_all` | **Stato Giocatore Istantaneo**: Lettura Salute, Fame e Livello a 1 tocco |
| `KEY_NUMPADADD` | Nessuno | `numpad.mouse.middle_click` | **Seleziona Blocco**: Pick Block nel mirino (Simulazione tasto centrale) |
| `KEY_NUMPADSUBTRACT` | Nessuno | `numpad.action.unlock` | `MainClass.poiManager.lockingHandler.unlock(true, true)` |
| `KEY_NUMPADDIVIDE` | Nessuno | `numpad.hotbar.scroll_prev` | `MouseUtils.Wheel.UP.scroll()` (Slot Hotbar prec.) |
| `KEY_NUMPADMULTIPLY` | Nessuno | `numpad.hotbar.scroll_next` | `MouseUtils.Wheel.DOWN.scroll()` (Slot Hotbar succ.) |

---

### 3.2 Layer 1: `Ctrl` + Numpad (Bussola Assoluta, Punti Cardinali & Radar POI)

| Tasto GLFW | Modificatore | ID Binding Kuma | Azione / Metodo Chiamato |
| :--- | :--- | :--- | :--- |
| `KEY_NUMPAD8` | `CONTROL` | `numpad.orient.north` | Snap istantaneo a **Nord** (`Orientation.NORTH`) |
| `KEY_NUMPAD6` | `CONTROL` | `numpad.orient.east` | Snap istantaneo a **Est** (`Orientation.EAST`) |
| `KEY_NUMPAD2` | `CONTROL` | `numpad.orient.south` | Snap istantaneo a **Sud** (`Orientation.SOUTH`) |
| `KEY_NUMPAD4` | `CONTROL` | `numpad.orient.west` | Snap istantaneo a **Ovest** (`Orientation.WEST`) |
| `KEY_NUMPAD7` | `CONTROL` | `numpad.orient.north_west` | Snap a **Nord-Ovest** |
| `KEY_NUMPAD9` | `CONTROL` | `numpad.orient.north_east` | Snap a **Nord-Est** |
| `KEY_NUMPAD1` | `CONTROL` | `numpad.orient.south_west` | Snap a **Sud-Ovest** |
| `KEY_NUMPAD3` | `CONTROL` | `numpad.orient.south_east` | Snap a **Sud-Est** |
| `KEY_NUMPAD5` | `CONTROL` | `numpad.orient.narrate_coordinates` | Lettura coordinate assolute X, Y, Z |
| `KEY_NUMPADDECIMAL` | `CONTROL` | `numpad.orient.narrate_target_coords` | Lettura coordinate del blocco/entità puntato |
| `KEY_NUMPAD0` | `CONTROL` | `numpad.orient.look_behind` | Ruota di 180° alle spalle |
| `KEY_NUMPADDIVIDE` | `CONTROL` | `numpad.poi.group_prev` | Categoria POI precedente |
| `KEY_NUMPADMULTIPLY` | `CONTROL` | `numpad.poi.group_next` | Categoria POI successiva |
| `KEY_NUMPADSUBTRACT` | `CONTROL` | `numpad.poi.item_prev` | Oggetto POI precedente |
| `KEY_NUMPADADD` | `CONTROL` | `numpad.poi.item_next` | Oggetto POI successivo |
| `KEY_NUMPADENTER` | `CONTROL` | `numpad.poi.look_at_current_object` | Guarda l'oggetto puntato dal Radar POI |

---

### 3.3 Layer 2: `Alt` + Numpad (Diagnostica, Vertici & Mobilità Automatica)

| Tasto GLFW | Modificatore | ID Binding Kuma | Azione / Metodo Chiamato |
| :--- | :--- | :--- | :--- |
| `KEY_NUMPAD8` | `ALT` | `numpad.status.mainhand` | Lettura oggetto in mano principale |
| `KEY_NUMPAD2` | `ALT` | `numpad.status.offhand` | Lettura oggetto in mano secondaria |
| `KEY_NUMPAD4` | `ALT` | `numpad.status.effects` | Lettura effetti di stato attivi |
| `KEY_NUMPAD6` | `ALT` | `numpad.status.durability` | Lettura durabilità strumento impugnato |
| `KEY_NUMPAD5` | `ALT` | `numpad.camera.narrate_facing` | Lettura direzione orizzontale e inclinazione verticale in gradi |
| `KEY_NUMPAD1` | `ALT` | `numpad.camera.look_nadir` | Guarda dritto ai piedi (**Nadir** a +90°) |
| `KEY_NUMPAD3` | `ALT` | `numpad.camera.look_zenith` | Guarda dritto in cielo (**Zenith** a -90°) |
| `KEY_NUMPAD0` | `ALT` | `numpad.action.auto_walk` | Toggle Marcia Automatica (**Auto-Walk**) |
| `KEY_NUMPADDECIMAL` | `ALT` | `numpad.action.toggle_sprint` | Toggle Corsa / Camminata in Auto-Walk |
| `KEY_NUMPADENTER` | `ALT` | `numpad.status.access_menu` | Apertura Access Menu (`F4`) |
| `KEY_NUMPADENTER` | `ALT` | `numpad.status.access_menu` | Apertura/Chiusura Access Menu (`F4`) |
| `KEY_NUMPADADD` | `ALT` | `numpad.status.bossbar_next` | Lettura bossbar successiva |
| `KEY_NUMPADSUBTRACT` | `ALT` | `numpad.status.bossbar_prev` | Lettura bossbar precedente |

---

## 4. Personalizzazione, Rimappatura & Preset Mancino

### 4.1 Differenze del Preset Mancino (`LEFT_HANDED`)
Nel preset per mancini (pensato per chi usa la mano sinistra sul tastierino e la destra sulle frecce/mouse):
* `Numpad +` e `Numpad Enter` vengono scambiati con `Numpad /` e `Numpad 7` per le azioni primarie, oppure i tasti d'azione sono invertiti per garantire la massima comodità al pollice sinistro.
* Tutti i tasti restano comunque riassegnabili singolarmente dal menu Vanilla `Assegnazione tasti`.

---

## 5. Analisi di Validazione e Verifica

1. **Validità e Standard GLFW/Kuma**:
   - Piena separazione tra codici tastierino numerico (`KEY_NUMPAD*`) e numeri riga superiore.
   - Pieno supporto ai modificatori `SHIFT`, `CONTROL`, `ALT` senza interferenze con le funzioni di digitazione chat o menu.
2. **Coesistenza Senza Regressioni**:
   - I controlli classici da tastiera (`I, J, K, L`, `Page Up/Down`, `[`, `]`, `\`) rimangono attivi al 100%. L'utente può usare indifferentemente l'uno o l'altro sistema o una combinazione di entrambi.
3. **Controllo Schermate e Menu**:
   - Tutti gli handler utilizzano `handleWorldInput(...)`. Se una GUI (inventario, cassa, chat, menu pausa) è aperta, i tasti non scatenano azioni nel mondo di gioco.
4. **Master Switch & Flessibilità**:
   - Se `numpadControls.enabled = false`, il modulo smette istantaneamente di elaborare gli input, garantendo un controllo totale all'utente.
