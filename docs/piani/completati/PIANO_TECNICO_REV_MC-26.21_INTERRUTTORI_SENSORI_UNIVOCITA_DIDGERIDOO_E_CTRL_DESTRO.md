# Piano Tecnico Formale — Rev MC-26.21: Interruttori Sensori Contigui (Ctrl+Alt+F1..F6), Univocità Sonora Didgeridoo e Supporto Simmetrico Control Destro (ASTRALIS v3.0.4)

- **Tipologia**: EVOLUTIVO / ACCESSING & AUDIO ENHANCEMENT (Rev MC-26.21)
- **Autore**: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity (Senior AI Pair Programmer)
- **Revisori**: Luca / Antigravity
- **Data e Ora**: 2026-09-09T02:35:00+02:00
- **Ramo Git di Riferimento**: `feat/dual-fall-safety-subsystem`
- **Incremento Versione Target (AVF)**: `26.2-1.20.0`
- **Stato Operativo**: `[SOTTO-FASE 1A: PIANO TECNICO FORMALIZZATO — STOP OBBLIGATORIO PRIMA DEL CODICE]`
- **Documenti Correlati (Pointer Hub DRY)**:
  * [`docs/report/REGISTRO_REVISIONI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/REGISTRO_REVISIONI.md)
  * [`knowledge/06_controlli_avanzati_e_bridge_chatgpt.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/knowledge/06_controlli_avanzati_e_bridge_chatgpt.md)
  * [`src/main/java/org/mcaccess/minecraftaccess/utils/ModifierUtils.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/utils/ModifierUtils.java)
  * [`src/main/java/org/mcaccess/minecraftaccess/features/point_of_interest/ObjectTracker.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/point_of_interest/ObjectTracker.java)
  * [`src/main/java/org/mcaccess/minecraftaccess/features/safety/fall/LongRangeFallDetector.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/safety/fall/LongRangeFallDetector.java)

---

## 📋 Scaletta Fasi Implementative & Checkbox di Progresso (Gating a 3 Stati: `[ ]` In Attesa, `[/]` In Corso, `[x]` Completato)

- [x] **Contratto D0**: Risoluzione Hardware Simmetrica Control/Alt Destro e Sinistro in `ModifierUtils.java`
- [x] **Contratto D1**: Allineamento e Guardie Esclusive in `ObjectTracker.java` (supporto `Ctrl+Alt+Home` simmetrico)
- [x] **Contratto D2**: Univocità Acustica Assoluta (`NOTE_BLOCK_DIDGERIDOO`) in `LongRangeFallDetector.java`
- [x] **Contratto D3**: Batteria dei 6 Interruttori Sensori (`Ctrl+Alt+F1`..`F6`) nei relativi moduli e persistenza `Config.java`
- [x] **Contratto D4**: Protezione Esclusiva `QuickHelpKey.java` (`F1` liscio protetto con `hasNoModifiers()`)
- [x] **Contratto D5**: Categoria Interruttori `cat_toggles` nell'Aiuto In-Game `QuickKeysHelpScreen.java`
- [x] **Contratto D6**: Rigore I18N IT/EN con Ordinamento Alfabetico Crescente in `it_it.json` ed `en_us.json`
- [x] **Contratto D7**: Allineamento Schede di Conoscenza [`knowledge/06_controlli_avanzati_e_bridge_chatgpt.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/knowledge/06_controlli_avanzati_e_bridge_chatgpt.md)
- [x] **Verifica & Test**: Esecuzione Suite Headless a 0 ms (`.\gradlew.bat test`), Compilazione ShadowJar e Deploy Proattivo
- [/] **Collaudo In-Game (Luca)**: Verifica suoni, interruttori e tasto Control destro sul campo
- [ ] **Chiusura Tecnica (Fase 3)**: Archiviazione Piano, Redazione Report di Sessione e Migrazione RRU

---

## 🎯 1. Visione d'Insieme & Obiettivi Architetturali

Questo piano definisce l'implementazione organica di tre requisiti fondamentali formulati e validati da Luca:

1. **Univocità Acustica Assoluta (Principio ASTRALIS)**:
   - Eliminazione della collisione acustica storica tra il **Radar Buche a Lungo Raggio** (`LongRangeFallDetector`) e i **Punti di Interesse / Waypoint** (`ObjectTracker`, `POIWaypoints`), che condividevano entrambi la campanella (`NOTE_BLOCK_BELL`);
   - Assegnazione al radar buche a lungo raggio del nuovo timbro profondo, cavernoso e tellurico del **Didgeridoo** (`NOTE_BLOCK_DIDGERIDOO`), preservando la campanella in purezza per i soli POI e Waypoint.

2. **Batteria Contigua di Interruttori Sensori (`Ctrl+Alt+F1` .. `F6`)**:
   - Creazione di una sequenza ordinata, accessibile e contigua da F1 a F6 per attivare e disattivare a richiesta i singoli sottosistemi sensoriali (faro acustico rotte da fermi, ostacoli, buche vicine, buche lontane, arpeggio mirino, radar mob ostili);
   - Annuncio vocale chiaro per NVDA a ogni commutazione (*"Attivo"* / *"Disattivato"*);
   - Salvataggio automatico dello stato nella configurazione;
   - Protezione esclusiva su `F1` liscio (`QuickHelpKey`) per prevenire aperture spurie della schermata di aiuto.

3. **Supporto Simmetrico Universale del Control Destro (`GLFW_KEY_RIGHT_CONTROL`)**:
   - Potenziamento di `ModifierUtils.hasControl()` e `hasAlt()` per verificare via hardware GLFW sia il tasto sinistro che il tasto destro (`GLFW_KEY_LEFT_CONTROL` / `GLFW_KEY_RIGHT_CONTROL`, `GLFW_KEY_LEFT_ALT` / `GLFW_KEY_RIGHT_ALT`);
   - Abilitazione del funzionamento impeccabile di `Ctrl+Alt+Home` (lettura coordinate oggetto tracciato in `ObjectTracker`) sia con Control sinistro che con Control destro.

4. **Allineamento Rete Documentale & Guida Rapida**:
   - Aggiornamento della schermata di aiuto rapido in-game (`QuickKeysHelpScreen.java`) con la nuova categoria interruttori;
   - Aggiornamento della scheda comandi `knowledge/06_controlli_avanzati_e_bridge_chatgpt.md`;
   - Rigoroso ordinamento alfabetico crescente in `it_it.json` ed `en_us.json`.

---

## 📦 2. Contratti Denominati di Implementazione (Inner Codex Pattern — Cancello 4)

### Contratto D0 — Supporto Simmetrico Control/Alt Destro e Sinistro in `ModifierUtils`
- **File Coinvolti**:
  * [`src/main/java/org/mcaccess/minecraftaccess/utils/ModifierUtils.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/utils/ModifierUtils.java)
- **Modifiche Operative**:
  1. In `hasControl()`: verificare la pressione fisica di entrambi i tasti prima del fallback su `client.hasControlDown()`:
     ```java
     public static boolean hasControl() {
         Minecraft client = Minecraft.getInstance();
         if (client == null) return false;
         try {
             Window window = client.getWindow();
             if (window != null) {
                 if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                         || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL)) {
                     return true;
                 }
             }
             return client.hasControlDown();
         } catch (Exception e) {
             return false;
         }
     }
     ```
  2. In `hasAlt()`: verificare sia `GLFW_KEY_LEFT_ALT` sia `GLFW_KEY_RIGHT_ALT` via `InputConstants.isKeyDown(window, ...)`.
- **Invariante**: Qualsiasi combinazione che richieda `Control` o `ControlAndAlt` deve scattare indistintamente sia premendo il tasto `Ctrl` a sinistra sia quello a destra della barra spaziatrice.

### Contratto D1 — Allineamento e Guardie Esclusive in `ObjectTracker` (`Home`, `Ctrl+Home`, `Ctrl+Alt+Home`)
- **File Coinvolti**:
  * [`src/main/java/org/mcaccess/minecraftaccess/features/point_of_interest/ObjectTracker.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/point_of_interest/ObjectTracker.java)
- **Modifiche Operative**:
  1. In `narrate_current_object` (`Home`): aggiungere guardia esclusiva `if (!ModifierUtils.hasNoModifiers()) return false;`;
  2. In `look_at_current_object` (`Ctrl+Home`): aggiungere guardia esclusiva `if (!ModifierUtils.hasControlOnly()) return false;`;
  3. In `narrate_coordinates_current_object`: accettare sia `Alt+Home` (`ModifierUtils.hasAltOnly()`) sia `Ctrl+Alt+Home` (`ModifierUtils.hasControlAndAlt()`), consentendo l'attivazione con Control sia sinistro che destro.

### Contratto D2 — Univocità Sonora del Radar Buche a Lungo Raggio (`NOTE_BLOCK_DIDGERIDOO`)
- **File Coinvolti**:
  * [`src/main/java/org/mcaccess/minecraftaccess/features/safety/fall/LongRangeFallDetector.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/safety/fall/LongRangeFallDetector.java)
  * [`src/test/java/org/mcaccess/minecraftaccess/features/safety/fall/LongRangeFallDetectorTest.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/test/java/org/mcaccess/minecraftaccess/features/safety/fall/LongRangeFallDetectorTest.java)
- **Modifiche Operative**:
  1. Sostituire `SoundEvents.NOTE_BLOCK_BELL.value()` con `SoundEvents.NOTE_BLOCK_DIDGERIDOO.value()` in `LongRangeFallDetector.java`;
  2. Volume impostato al livello di sicurezza ASTRALIS `0.8f`;
  3. Aggiornare i test unitari in `LongRangeFallDetectorTest.java` per verificare il timbro Didgeridoo a 0 ms.

### Contratto D3 — Interruttori Sensori Contigui (`Ctrl+Alt+F1` .. `F6`)
- **File Coinvolti**:
  * `POIWaypoints.java` (`Ctrl+Alt+F1` — Toggle Faro Acustico Waypoint / Traccia Rotte);
  * `ObstacleDetector.java` (`Ctrl+Alt+F2` — Toggle Rilevatore Ostacoli);
  * `CentralFallSafetyManager.java` (`Ctrl+Alt+F3` — Toggle Rilevatore Buche Corto Raggio; `Ctrl+Alt+F4` — Toggle Rilevatore Buche Lungo Raggio);
  * `NarrateCrosshair.java` (`Ctrl+Alt+F5` — Toggle Mirino Narrante Acustico);
  * `POIEntities.java` (`Ctrl+Alt+F6` — Toggle Radar Mob Ostili Passivo);
  * `Config.java` (aggiornamento/integrazione campi di persistenza).
- **Semantica di Funzionamento**:
  - Pressione del tasto -> Inversione flag booleano -> Notifica vocale via `MainClass.narrate` -> Salvataggio configurazione;
  - Tutte le registrazioni Kuma protette con `ModifierUtils.hasControlAndAlt()`.

### Contratto D4 — Protezione Esclusiva `QuickHelpKey` (`F1` liscio)
- **File Coinvolti**:
  * [`src/main/java/org/mcaccess/minecraftaccess/features/help/QuickHelpKey.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/help/QuickHelpKey.java)
- **Modifiche Operative**:
  - Inserimento di `if (!ModifierUtils.hasNoModifiers()) return false;` in testa a `handleWorldInput`. Previene l'apertura della schermata di aiuto premendo `Ctrl+Alt+F1`.

### Contratto D5 — Sistema di Aiuto Rapido In-Game (`QuickKeysHelpScreen`)
- **File Coinvolti**:
  * [`src/main/java/org/mcaccess/minecraftaccess/features/help/QuickKeysHelpScreen.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/help/QuickKeysHelpScreen.java)
- **Modifiche Operative**:
  - Aggiunta della categoria `cat_toggles` (*"Interruttori Sensori (F1..F6)"*) con descrizione vocale dettagliata di ciascuno dei 6 tasti consultabile premendo `F1`.

### Contratto D6 — Rigore I18N IT/EN con Ordinamento Alfabetico Crescente
- **File Coinvolti**:
  * `src/main/resources/assets/minecraft_access/lang/it_it.json`
  * `src/main/resources/assets/minecraft_access/lang/en_us.json`
- **Modifiche Operative**:
  - Inserimento di tutte le etichette di keymapping e messaggi vocali nel rispetto dell'ordinamento alfabetico crescente obbligatorio (`jq -e "keys != keys_unsorted"`).

### Contratto D7 — Aggiornamento Guide Knowledge
- **File Coinvolti**:
  * [`knowledge/06_controlli_avanzati_e_bridge_chatgpt.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/knowledge/06_controlli_avanzati_e_bridge_chatgpt.md)
- **Modifiche Operative**:
  - Inserimento della tabella ordinata degli Interruttori Sensori `Ctrl+Alt+F1..F6` e della specifica di simmetria Control Sinistro / Control Destro.

---

## 🧪 3. Validazione Preventiva sui 7 Assi di Qualità ASTRALIS

1. **Validità**: Conforme alle API GLFW, Kuma, Balm e Java 25.
2. **Efficacia**: Massima ergonomia motoria e cognitiva (fila continua F1..F6), eliminazione del rumore acustico da fermi e separazione timbrica netta buche/POI.
3. **Coerenza**: Uniformità assoluta di comportamento tra tutti gli interruttori (toggle, voce, persistenza).
4. **Completezza**: Copre sia il fronte runtime (tasti, suoni, guardie), sia il fronte documentale (help F1, I18N, schede knowledge).
5. **Precisione**: Interventi chirurgici e disaccoppiati su ciascun modulo di pertinenza.
6. **Affidabilità & Prestazioni**: Esecuzione a 0 cost computazionale; test deterministici a 0 ms senza thread sleep.
7. **Assenza di Regressioni**: Zero impatti sui controlli esistenti (Numpad, inventario, AutoWalk).

---

## 🔬 4. Matrice di Simulazione a 3 Livelli

### Livello 1 — Scenari Comuni (Happy Path)
- **Scenario 1.1**: Luca è fermo a craftare, preme `Ctrl+Alt+F1`: *"Faro waypoint disattivato"*; il bip cessa. Quando riparte, preme `Ctrl+Alt+F1`: *"Faro waypoint attivo"*; il bip riprende.
- **Scenario 1.2**: In esplorazione, una voragine a 15 blocchi emette il rintocco profondo del didgeridoo, senza alcuna interferenza con i waypoint.
- **Scenario 1.3**: Luca preme `Right Ctrl + Alt + Home`: le coordinate del POI vengono lette istantaneamente senza dover usare il Ctrl sinistro.

### Livello 2 — Scenari Meno Comuni & Concorrenza (Alternative Paths)
- **Scenario 2.1**: Luca preme `Ctrl+Alt+F1` per zittire il faro: `QuickHelpKey` non scatta (grazie a `hasNoModifiers()`), aprendo solo il toggle desiderato.
- **Scenario 2.2**: Luca commuta un interruttore durante la marcia AutoWalk: il cambio di stato avviene senza intoppi e viene vocalizzato nitidamente.

### Livello 3 — Casi Limite & Corner Cases (Boundary, Concorrenza Hardware)
- **Scenario 3.1 (Rilascio asincrono tasti su F4)**: 
  - *Condizione*: Luca rilascia `Ctrl` prima di `Alt` su `Ctrl+Alt+F4`;
  - *Verifica empirica*: Monitorare se Windows intercetta `Alt+F4`. Se instabile, fallback immediato a `Ctrl+Alt+F8`.
- **Scenario 3.2 (Pressione contemporanea di più modificatori)**: Se `Shift` è premuto insieme a `Ctrl+Alt`, le guardie `ModifierUtils.hasControlAndAlt()` scartano l'input prevenendo trigger spuri.

---

## 🛑 5. GATING SEMANTICO E PROTOCOLLO DI STOP (Regola 0)

> [!IMPORTANT]
> **STOP OBBLIGATORIO PRIMA DELLA SOTTO-FASE 1B (CODICE SORGENTE)**:
> In conformità alla Regola 0 e al Canone ASTRALIS v3.0.4, la presente Sotto-Fase 1A è formalmente conclusa e registrata.
> **Nessun file di codice Java o configurazione verrà modificato** prima del tuo esplicito comando di procedere (*"procedi"*, *"applica"*, *"esegui"*).
