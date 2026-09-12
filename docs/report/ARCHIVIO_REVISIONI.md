# Archivio Storico delle Revisioni & Collaudi Conclusi (RRU)
- **Framework di Riferimento:** ASTRALIS Framework v2.7.1 (Protocollo 6 & RRU Disaccoppiato)
- **Progetto:** Minecraft Access (Fork 26.2 / 1.21.x)
- **Autore:** Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity
- **Revisori:** Luca / Antigravity / GPT Codex / ChatGPT
- **Data Ultimo Aggiornamento:** 2026-09-12
- **Stato:** [ARCHIVIO STORICO PERENNE — 32 REVISIONI COLLAUDATE CON SUCCESSO]
- **Registro Attivo Correlato:** [`docs/report/REGISTRO_REVISIONI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/REGISTRO_REVISIONI.md)

Questo documento costituisce la memoria storica e forense perenne di tutte le anomalie, correzioni e rifiniture collaudate e chiuse con successo nel ciclo di vita di Minecraft Access. Ciascuna voce archiviata mantiene la sintesi del problema, la causa radice, la soluzione adottata e i collegamenti diretti ai relativi Piani Tecnici e Report di Sessione archiviati.

---

## 🏛️ STORICO REVISIONI COLLAUDATE CON SUCCESSO (CICLO 26.2)

### 🟢 Rev MC-26.23 — AutoWalk Verticale & Assistente Tattico di Scalata (Climb Assistant)
- **Stato**: `[COLLAUDATA CON SUCCESSO AL 100% IN-GAME DA LUCA]`
- **Versione Chiusura**: 26.2-1.21.0 (Data 2026-09-12)
- **Sintesi**: introdotto e stabilizzato il motore verticale condiviso per scale a pioli e blocchi arrampicabili, usato sia da AutoWalk sia dal Climb Assistant semi-automatico.
- **Esito Collaudo**: Luca ha confermato in-game il funzionamento completo in salita e discesa alla Torre del Belvedere, con sbarco corretto, zero cadute e annuncio vocale coerente.
- **Riferimenti**:
  - [`REPORT_HANDOVER_CODEX_PIANO_AUTOWALK_VERTICALE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_HANDOVER_CODEX_PIANO_AUTOWALK_VERTICALE.md)
  - [`PIANO_TECNICO_CORRETTIVO_TRANSIZIONI_VERTICALI_E_LANDING.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_CORRETTIVO_TRANSIZIONI_VERTICALI_E_LANDING.md)
  - [`STRATEGIA_AUTOWALK_VERTICALE_E_CLIMB_ASSISTANT.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/archiviate/STRATEGIA_AUTOWALK_VERTICALE_E_CLIMB_ASSISTANT.md)
  - [`STRATEGIA_CORRETTIVA_DISMOUNT_SALITA_E_DISCESA.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/archiviate/STRATEGIA_CORRETTIVA_DISMOUNT_SALITA_E_DISCESA.md)

### 🟢 Rev MC-26.22 — Neutralizzazione Mixin Tasti Vanilla F1..F6 & Batteria Interruttori Suoni Radar POI per Categoria
- **Stato**: `[COLLAUDATA CON SUCCESSO AL 100% IN-GAME DA LUCA]`
- **Versione Chiusura**: 26.2-1.20.0 (Data 2026-09-09)
- **Problema Riscontrato (Esperienza Luca)**:
  1. *Conflitto Tasti Funzione Vanilla*: Alla pressione di `Ctrl+Alt+F3` (buche corte) Minecraft apriva la Debug Screen con grafici e testo a schermo; su `Ctrl+Alt+F5` (suono mirino) cambiava la prospettiva della telecamera in terza persona;
  2. *Rumore di Fondo Radar POI*: Impossibilità di silenziare singole categorie dello scanner POI (porte, minerali, fluidi, mob passivi, ecc.) mantenendo lo scanner attivo e la narrazione testuale;
  3. *Distinzione Sentinella vs Radar Ostili*: Necessità di separare la sentinella minacce ravvicinate 6m (`Ctrl+Alt+F6`, allarme basedrum) dal pinging periodico a 24m per i mob ostili.
- **Causa Radice**: La pipeline di input vanilla esegue `toggleDebugOverlay()` al rilascio di F3 (`action == 0`) e registra click per `keyTogglePerspective` (F5) e `keyToggleGui` (F1) prima che il gameplay possa sopprimerli. Inoltre `POIGroup` emetteva suoni incondizionatamente per tutti i gruppi senza un controllo granulare per categoria.
- **Soluzioni Applicate (PRAPI)**:
  1. *Neutralizzazione Mixin F1, F3, F5*:
     * `DebugScreenEntryListMixin`: Annullamento preventivo di `toggleDebugOverlay()` a monte con `ci.cancel()` se `ModifierUtils.hasControlAndAlt()` è attivo;
     * `KeyboardHandlerMixin`: Firma corretta API Mojang 26.2 `(KeyEvent event, CallbackInfoReturnable<Boolean> cir)` con restituzione deterministica a `cir.setReturnValue(true)`;
     * `MinecraftMixin`: All'inizio di `handleKeybinds()`, svuotamento a vuoto dei click pendenti di `keyTogglePerspective` (F5) e `keyToggleGui` (F1) quando `Ctrl+Alt` sono premuti.
  2. *Controllo Granulare Suoni POI via `BooleanSupplier`*:
     * `POIGroup`: Campo `BooleanSupplier soundEnabledSupplier` e guardia difensiva all'inizio di `playSoundForGroupItems()`;
     * `Config`: 7 flag `boolean` per blocchi e 9 per entità, tutti attivi (`true`) di default;
     * Batteria Kuma 9 interruttori su tastiera IT: `Ctrl+Alt+F7..F12` (Minerali, Funzionali, Porte, Portali, Scale, Fluidi) + `Ctrl+Alt+G` (GUI/Forzieri) + `Ctrl+Alt+H` (Radar Ostili periodico 24m) + `Ctrl+Alt+P` (Animali Passivi);
     * Schermata aiuto rapido `F1`: Aggiunta Categoria 8 *"Interruttori Suoni Radar POI"* (`cat_poi_sound_toggles`);
     * Localizzazioni complete e rigorosamente ordinate in `it_it.json` ed `en_us.json`.
- **Piani Tecnici e Rapporti di Riferimento**:
  - [`PIANO_TECNICO_REV_MC-26.22_MIXIN_VANILLA_FN_E_TOGGLE_POI_SUONI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_REV_MC-26.22_MIXIN_VANILLA_FN_E_TOGGLE_POI_SUONI.md)
  - [`REPORT_SESSIONE_REV_MC-26.21_E_26.22_INTERRUTTORI_SENSORI_MIXIN_E_SUONI_POI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_REV_MC-26.21_E_26.22_INTERRUTTORI_SENSORI_MIXIN_E_SUONI_POI.md)
- **Esito Collaudo**: Collaudata e validata al 100% in-game da Luca: tutti i 9 interruttori commutati con successo, azzeramento selettivo dei suoni confermato, zero sovrapposizioni visive o grafiche.

---

### 🟢 Rev MC-26.21 — Interruttori Sensori Contigui (Ctrl+Alt+F1..F6), Univocità Didgeridoo & Supporto Control Destro
- **Stato**: `[COLLAUDATA CON SUCCESSO AL 100% IN-GAME DA LUCA]`
- **Versione Chiusura**: 26.2-1.20.0 (Data 2026-09-09)
- **Problema Riscontrato (Esperienza Luca)**:
  1. *Collisione Acustica Storica*: Il radar buche a lungo raggio usava la campanella `NOTE_BLOCK_BELL`, generando ambiguità con lo scanner POI e i waypoint;
  2. *Assenza Interruttori Rapidi Sensoriali*: Impossibilità di silenziare il bip del waypoint durante il crafting/riposo e assenza di scorciatoie rapide per sensori ostacoli, buche e mirino;
  3. *Asimmetria Control Destro*: `Ctrl+Alt+Home` per la lettura coordinate POI rispondeva solo premendo il `Ctrl` sinistro.
- **Causa Radice**: Sovrapposizione del suono campana in `LongRangeFallDetector`, assenza di comandi dedicati e interrogazione hardware limitata al solo `GLFW_KEY_LEFT_CONTROL` in `ModifierUtils`.
- **Soluzioni Applicate (PRAPI)**:
  1. *Univocità Acustica*: Assegnato `NOTE_BLOCK_DIDGERIDOO` (pitch `0.8f`) al radar orografico buche lontane 7..24m;
  2. *Batteria Contigua F1..F6*: `Ctrl+Alt+F1` (Waypoint), `F2` (Ostacoli), `F3` (Buche corte), `F4` (Buche lontane), `F5` (Suono mirino), `F6` (Sentinella minacce ostili 6m) con annuncio vocale e persistenza;
  3. *Supporto Hardware Simmetrico*: Aggiornato `ModifierUtils` per interrogare sia tasto sinistro che destro (`GLFW_KEY_RIGHT_CONTROL`, `GLFW_KEY_RIGHT_ALT`);
  4. *Suite Headless*: Aggiornati test di regressione (354/354 verdi a 0 ms).
- **Piani Tecnici e Rapporti di Riferimento**:
  - [`PIANO_TECNICO_REV_MC-26.21_INTERRUTTORI_SENSORI_UNIVOCITA_DIDGERIDOO_E_CTRL_DESTRO.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_REV_MC-26.21_INTERRUTTORI_SENSORI_UNIVOCITA_DIDGERIDOO_E_CTRL_DESTRO.md)
  - [`REPORT_SESSIONE_REV_MC-26.21_E_26.22_INTERRUTTORI_SENSORI_MIXIN_E_SUONI_POI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_REV_MC-26.21_E_26.22_INTERRUTTORI_SENSORI_MIXIN_E_SUONI_POI.md)
- **Esito Collaudo**: Collaudata e validata al 100% in-game da Luca: Didgeridoo profondo e inconfondibile, sequenza F1..F6 attiva e reattiva, tasto Control destro perfettamente riconosciuto.

---

### 🟢 Rev MC-26.20 — Null Safety in ObjectTracker.isObjectValid() su Selezione Vuota
- **Stato**: `[COLLAUDATA CON SUCCESSO AL 100% IN-GAME DA LUCA]`
- **Versione Chiusura**: 26.2-1.19.4 (Data 2026-09-09)
- **Problema Riscontrato (Esperienza Luca & Telemetria Live)**:
  - Alla pressione del tasto per rivolgere la visuale al punto d'interesse selezionato (`ObjectTracker.lookAtCurrentObject()`), se nessun oggetto è attualmente selezionato (`currentObject == null`), il client catturava un errore non gestito: `java.lang.NullPointerException` scatenata da `isObjectValid()`.
- **Evidenza Telemetrica (latest.log ore 00:50:19)**:
  ```
  [00:50:19] [Render thread/ERROR]: Error executing task on Client
  java.lang.NullPointerException
  	at java.base/java.util.Objects.requireNonNull(Objects.java:220)
  	at knot//org.mcaccess.minecraftaccess.features.point_of_interest.ObjectTracker.isObjectValid(ObjectTracker.java:444)
  	at knot//org.mcaccess.minecraftaccess.features.point_of_interest.ObjectTracker.lookAtCurrentObject(ObjectTracker.java:330)
  ```
- **Causa Radice**: In Java 21+, il costrutto pattern-matching `return switch (object)` presente in `isObjectValid(Object object)` (riga 444) compila implicitamente con un controllo `Objects.requireNonNull(object)` a monte. Poiché `lookAtCurrentObject()` chiama `if (!isObjectValid(currentObject))` passando `null`, lo switch lancia un'eccezione a runtime prima di poter intercettare la condizione di oggetto non selezionato.
- **Soluzione Applicata (PRAPI)**:
  1. Inserita guardia difensiva `if (object == null) return false;` all'inizio di `isObjectValid(Object object)`;
  2. Consentito a `lookAtCurrentObject()` e `narrateCoordinatesOfCurrentObject()` di raggiungere regolarmente la notifica vocale NVDA: *"Nessun punto di interesse selezionato"*;
  3. Creata la suite headless deterministica `ObjectTrackerTest.java` (4 test su 4 verdi a 0 ms; suite complessiva a 354/354 test verdi).
- **Piani Tecnici e Rapporti di Riferimento**:
  - [`PIANO_TECNICO_REV_MC-26.20_OBJECT_TRACKER_NULL_SAFETY.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_REV_MC-26.20_OBJECT_TRACKER_NULL_SAFETY.md)
  - [`REPORT_SESSIONE_REV_MC-26.20_OBJECT_TRACKER_NULL_SAFETY.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_REV_MC-26.20_OBJECT_TRACKER_NULL_SAFETY.md)
- **Esito Collaudo**: Collaudata e validata al 100% in-game da Luca: azionamento dei tasti di puntamento su selezione vuota vocalizza regolarmente l'avviso vocale senza crash né eccezioni nei log.

---

### 🟢 Rev MC-26.19 — Quiete Sensoriale AutoWalk & Navigatore, Verbosità di Progressione & Silenziamento Mirino e Incudine
- **Stato**: `[COLLAUDATA CON SUCCESSO AL 100% IN-GAME DA LUCA]`
- **Versione Chiusura**: 26.2-1.19.3 (Data 2026-09-09)
- **Problema Riscontrato (Esperienza Luca)**:
  1. *Accoppiamento Monolitico Telemetria Passi*: La lettura vocale dei passi mancanti a traguardi di 5 era legata alla variabile didattica globale `narrateHints`, privando l'utente del conteggio se disattivava i suggerimenti;
  2. *Arpeggio Invasivo del Mirino nelle Curve*: Quando l'AutoWalk sterzava per seguire la rotta, il blocco mirato variava quota fino a 20°/tick scatenando note d'arpa continue (`NOTE_BLOCK_HARP`);
  3. *Allarme Incudine Spurio su Tracciato Sicuro (PRAPI)*: Il transiente acustico metallico dell'incudine (`ANVIL_LAND`) risuonava all'avvicinarsi a $1.0 - 1.5\text{ m}$ da cigli e discese pur essendo su una rotta A* geometricamente protetta.
- **Causa Radice**: Assenza di un enum dedicato alla progressione in Cloth Config, mancanza di una guardia sul cue sonoro di elevazione in `NarrateCrosshair.java` durante l'AutoWalk e limitazione del silenziamento acustico anticaduta al solo xilofono di Zona 1 in `ProximityFallDetector.java`.
- **Soluzioni Applicate (PRAPI & PRAPI-B)**:
  1. *Verbosità Regolabile (`ProgressionFeedbackMode`)*: Introdotto enum a 4 stati in `Config.AutoWalk` (`SOUND_AND_VOICE` default amato da Luca, `SOUND_ONLY`, `VOICE_ONLY`, `OFF`), con I18N IT/EN rigidamente ordinata in ordine alfabetico crescente;
  2. *Svincolo Telemetria in `AutoWalkMotor`*: Cadenza vocale a 5 passi autonoma e pura;
  3. *Silenziamento Mirino*: Soppressione di `playRelativePositionSoundCue` in `NarrateCrosshair` a rotta attiva, con tutela dell'interrogazione manuale `B` via `DirectInteractionShield`;
  4. *Silenziamento Incudine in AutoWalk*: Soppressione di `SoundEvents.ANVIL_LAND` e dell'Edge Bump durante l'AutoWalk quando `silenceFallWarningsDuringWalk` è abilitato; ripristino immediato a 0 ms dell'incudine su stallo, takeover manuale o navigazione inattiva;
  5. *Costruttori Pubblici*: Resi pubblici i costruttori di `Config.ObstacleDetector` e `Config.NarrateCrosshair` per piena conformità headless;
  6. *Suite Headless*: 350/350 test unitari superati a 0 ms.
- **Piani Tecnici e Rapporti di Riferimento**:
  - [`PIANO_TECNICO_REV_MC-26.19_QUIETE_SENSORIALE_AUTOWALK_E_NAVIGATORE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_REV_MC-26.19_QUIETE_SENSORIALE_AUTOWALK_E_NAVIGATORE.md)
  - [`REPORT_SESSIONE_REV_MC-26.19_QUIETE_SENSORIALE_AUTOWALK_E_NAVIGATORE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_REV_MC-26.19_QUIETE_SENSORIALE_AUTOWALK_E_NAVIGATORE.md)
  - [`STRATEGIA_QUIETE_SENSORIALE_AUTOWALK_E_NAVIGATORE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/archiviate/STRATEGIA_QUIETE_SENSORIALE_AUTOWALK_E_NAVIGATORE.md)
- **Esito Collaudo**: Collaudata e validata al 100% in-game da Luca in due sessioni (17:28 e 00:56): marcia fluida e silenziosa, cadenza vocale perfetta a intervalli di 5 passi, arpeggio arpa spento nelle curve, incudine muta sul percorso sicuro e vigile a piedi, integrità salvataggi verificata.

---

### 🟢 Rev MC-26.18 — De-monolitizzazione & Architettura Duale Cadute (Prossimità 1..6 & Lungo Raggio 7..24)
- **Stato**: `[COLLAUDATA CON SUCCESSO AL 100% IN-GAME DA LUCA]`
- **Versione Chiusura**: 26.2-1.19.2 (Data 2026-09-08)
- **Problema Riscontrato (Esperienza Luca)**:
  1. *Monolitismo di FallDetector*: La classe storica (915 righe) sommava scansione orografica, controllo ciglio, gestione scale/tuffi e debouncing, rendendo opaco il coordinamento degli allarmi.
  2. *Assenza di Pre-Allerta su Lungo Raggio*: Assenza di percezione anticipata su voragini, scarpate o burroni orografici distanti 7..24 blocchi prima di entrare nell'area di prossimità a ridosso del pericolo.
  3. *Uscita sonora dell'incudine soffocata (PRAPI)*: Il suono `SoundEvents.ANVIL_HIT` su bus `BLOCKS` veniva facilmente mascherato dal parlato simultaneo di NVDA.
- **Causa Radice**: Assenza di decomposizione a responsabilità singola per fasce di distanza, accoppiamento acustico spurio tra xilofono e incudine, e uso di un campione sonoro debole con bus d'attenuazione ambientale.
- **Soluzioni Applicate (PRAPI & PRAPI-B)**:
  1. *Decomposizione Modulare nel Package `features.safety.fall`*:
     - `CentralFallSafetyManager`: orchestratore unico su tick client e coordinatore delle guardie;
     - `ProximityFallDetector`: corto raggio $1..6\text{ m}$, scala a 3 zone (Zona 1 xilofono $2..6\text{ m}$, Zona 2A pre-freno $1.0..1.5\text{ m}$ con mutua esclusione acustica, Zona 2B ciglio meccanico $\le 0.85\text{ m}$);
     - `LongRangeFallDetector`: radar orografico periodico $7..24\text{ m}$ (campanella 3.5s attenuata con curva decadimento $50\%$, proiezione vettoriale OpenAL $[2.5 .. 12.0]\text{ m}$ ed emissione diretta);
     - `FallDetector`: facciata retrocompatibile preservata al 100%.
  2. *Escalation Dinamica & Potenziamento Pre-Freno (PRAPI-B)*:
     - Sostituito `SoundEvents.ANVIL_HIT` con `SoundEvents.ANVIL_LAND` (`block.anvil.land`), dotato di transiente metallico ad altissima energia penetrante attraverso la sintesi NVDA;
     - Instradato l'evento salvavita sul bus `SoundSource.PLAYERS` (immunità da attenuazione blocchi ambientali);
     - Implementato `isStatusEscalation` per garantire lo scatto reattivo dell'incudine all'avvicinarsi continuo alla stessa voragine.
  3. *Quiete Sensoriale Assoluta in AutoWalk*: Soppressione totale di campanella, xilofono e annunci descrittivi durante la marcia automatica, preservando esclusivamente l'auto-sneak di emergenza.
  4. *Suite di Test Headless*: 344 test unitari eseguiti a 0 ms (100% verdi).
- **Piani Tecnici e Rapporti di Riferimento**:
  - [`PIANO_TECNICO_REV_MC-26.18_ARCHITETTURA_DUALE_CADUTE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_REV_MC-26.18_ARCHITETTURA_DUALE_CADUTE.md)
  - [`REPORT_SESSIONE_REV_MC-26.18_ARCHITETTURA_DUALE_CADUTE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_REV_MC-26.18_ARCHITETTURA_DUALE_CADUTE.md)
  - [`STRATEGIA_SISTEMA_CADUTE_PROSSIMITA_E_LUNGO_RAGGIO.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/archiviate/STRATEGIA_SISTEMA_CADUTE_PROSSIMITA_E_LUNGO_RAGGIO.md)
- **Esito Collaudo**: Collaudata e validata con entusiasmo al 100% da Luca in tre sessioni in-game (14:38, 14:58, 15:16): incudine nitidissima e squillante, escalation progressiva perfetta, perfetta discesa su scale a pioli, zero errori nei log.

---

### 🟢 Rev MC-26.10 — Perfezionamento Soglia Dislivello Minimo, Disaccoppiamento Soglie Anticaduta & Pervietà Corridoio Discesa in Acqua
- **Stato**: `[COLLAUDATA CON SUCCESSO AL 100% IN-GAME DA LUCA]`
- **Versione Chiusura**: 26.2-1.19.1 (Data 2026-09-08)
- **Problema Riscontrato (Esperienza Luca)**:
  1. *Accoppiamento Monolitico*: L'uso di un'unica soglia (`depth = 4`) per l'avviso vocale e l'auto-sneak forzava a scegliere tra incollare il giocatore su dislivelli innocui di 3 blocchi (danno nullo) o perdere l'avviso preventivo su salti significativi.
  2. *Falsi Positivi Discesa Sicura*: L'annuncio "Discesa sicura" scattava su gradini e rampe minime ($\le 2$ blocchi). Inoltre, allo spawn su sentiero di terra, la presenza di una falda acquifera sotterranea a $Y=59$ coperta da terra piena innescava una raffica di 21 annunci spuri di "Discesa sicura".
- **Causa Radice**: Assenza di disaccoppiamento tra Zona 1 (Percezione) e Zona 2 (Intervento) in `FallDetector`, e scansione verticale verso il basso in `findDescentCandidate` cieca rispetto alla consistenza dei blocchi solidi intermedi tra il piano di cammino e l'acqua.
- **Soluzioni Applicate (PRAPI)**:
  1. *Disaccoppiamento Soglie (Contratto D1 & D3)*: Introdotto `warningDepth = 3` per pre-allerta vocale/sonora e `autoSneakDepth = 4` per accovacciamento meccanico sul ciglio; sui salti di 3 blocchi il giocatore sente l'avviso ma cammina e salta liberamente con `W`.
  2. *Silenziamento Discese Minime (Contratto D2)*: Discese con $\Delta Y < warningDepthThreshold$ (1-2 blocchi) classificate come cammino calpestabile ordinario (`NOT_APPLICABLE`).
  3. *Pervietà Corridoio Verticale Acqua (Contratto D2.1)*: Inserito controllo di pervietà continua: il ciclo di scansione della colonna d'acqua si interrompe all'istante (`break;`) se incontra un ostacolo solido con collision shape non vuota (`!probeState.getCollisionShape(level, waterProbe).isEmpty()`).
  4. *Suite di Test (Contratto D4)*: 327 test JUnit eseguiti con successo (100% verdi).
- **Piani Tecnici e Rapporti di Riferimento**:
  - [`PIANO_TECNICO_REV_MC-26.10_DISACCOPPIAMENTO_SOGLIE_ANTICADUTA.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_REV_MC-26.10_DISACCOPPIAMENTO_SOGLIE_ANTICADUTA.md)
  - [`REPORT_SESSIONE_REV_MC-26.10_DISACCOPPIAMENTO_SOGLIE_ANTICADUTA.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_REV_MC-26.10_DISACCOPPIAMENTO_SOGLIE_ANTICADUTA.md)
- **Esito Collaudo**: Convalidata empiricamente al 100% in-game da Luca: azzerati tutti i falsi allarmi allo spawn, perfetta fluidità sui dislivelli da 3 blocchi, protezione integra e salute a 20.0 cuori.

---

### 🟢 Rev MC-26.13 — Unificazione Architetturale AutoClose a Visuale Fissa & Clean Sweep (AutoOpen & AutoClose Doors)
- **Stato**: `[COLLAUDATA CON SUCCESSO AL 100% IN-GAME DA LUCA]`
- **Versione Chiusura**: 26.2-1.12.0-SNAPSHOT (Data 2026-09-07)
- **Problema Riscontrato (Esperienza Luca)**:
  1. *Movimento Manuale*: Rischio costante di infiltrazione mob dopo l'ingresso nella propria base, richiedendo lente e disorientanti rotazioni per richiudere la porta alle spalle;
  2. *AutoWalk*: La manovra cinetica a 180° inserita in `AutoWalkMotor` soffriva di intermittenze e scatti legati al framerate e al clock della CPU, con perdita occasionale del comando di chiusura.
- **Causa Radice**: Disallineamento tra i 20 TPS logici e il framerate grafico nella macchina a stati cinetica a 3 tick, unito all'assenza di un gestore unificato per la chiusura automatica.
- **Soluzioni Applicate (PRAPI)**:
  1. *Hub Centralizzato (`DoorInteractionManager`)*: Gestore FSM su tick client con soglia calibrata a $0.90\text{ m}$ dal centro varco normalizzato `LOWER`, supporto *Passage Renewal* nel vano porta ($d \le 0.65\text{ m}$) e Watchdog timeout di $6000\text{ ms}$;
  2. *Zero Disorientamento della Visuale*: Chiusura programmatica alle spalle sia a piedi sia in AutoWalk con Yaw e Pitch 100% immutati, emissione suono 3D alle spalle (volume calibrato $0.75\text{f}$) e annuncio vocale univoco *"Porta chiusa alle spalle"*;
  3. *ASTRALIS Clean Sweep*: Eliminazione integrale di `doorToClosePos`, `doorCloseManeuverTicks` e dell'intero blocco di rotazione a 180° da `AutoWalkMotor`, delegando l'apertura e la registrazione a `DoorInteractionManager`;
  4. *Configurazione & Test*: Nuova categoria Cloth Config `doorInteraction`, localizzazioni bilingue ordinate alfabeticamente e suite di test JUnit 5 headless a 0 ms (323/323 test passati).
- **Piani Tecnici e Rapporti di Riferimento**:
  - [`PIANO_TECNICO_REV_MC-26.13_AUTO_DOORS.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_REV_MC-26.13_AUTO_DOORS.md)
  - [`REPORT_SESSIONE_REV_MC-26.13_AUTO_DOORS.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_REV_MC-26.13_AUTO_DOORS.md)
  - [`STRATEGIA_COGNITIVA_GESTIONE_PORTE_E_VARCHI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/archiviate/STRATEGIA_COGNITIVA_GESTIONE_PORTE_E_VARCHI.md)
- **Esito Collaudo**: Collaudata con successo al 100% da Luca in-game il 07/09/2026 sia in manuale sia su rotte multi-porta AutoWalk ("ingresso est tenuta", "casa porta primo piano"): fluidità assoluta, zero sobbalzi, chiusura perfetta alle spalle. 323/323 test automatici verdi.

---

### 🟢 Rev MC-26.12 — Assist di Interazione Varchi e Magnetismo Voxel per Porte Aperte (Permissive Door Interaction)
- **Stato**: `[COLLAUDATA CON SUCCESSO AL 100% IN-GAME DA LUCA]`
- **Versione Chiusura**: 26.2-1.12.0-SNAPSHOT (Data 2026-09-07)
- **Problema Riscontrato (Esperienza Luca)**: Quando una porta o cancelletto era aperto, la voce di NVDA annunciava regolarmente *"Porta aperta di abete"* (grazie al micro-voxel raymarch), ma premendo il tasto interazione (tasto destro, `]` o `Invio` del Numpad) la porta non si chiudeva perché il raycast fisico nativo di Vanilla Minecraft attraversava l'aria vuota del varco aperto mancando la lamina da 3 pixel ($0.1875\text{ m}$) dello stipite. Per chiudere la porta era necessaria una ricerca millimetrica dello stipite.
- **Causa Radice**: Discrepanza geometrica tra `PlayerUtils.crosshairTarget` (micro-voxel snap su intero volume cubico) e `client.hitResult` nativo di Vanilla (collision box reale di 3 pixel a porta aperta).
- **Soluzioni Applicate (PRAPI)**:
  1. Helper puro `DoorInteractionHelper`:
     - `isInteractableOpenDoorOrGate(BlockState)`: riconosce porte in legno/rame/bambù, cancelletti e botole aperte, escludendo tassativamente porte e botole di ferro (`IRON_DOOR`, `IRON_TRAPDOOR`);
     - `resolvePermissiveDoorHit(Minecraft)`: se Vanilla non sta già mirando a un mob o direttamente allo stipite, interroga `PlayerUtils.crosshairTarget(reach)` e restituisce il `BlockHitResult` del blocco porta aperto;
  2. Iniezione `MinecraftMixin`:
     - In `@Inject(method = "startUseItem", at = @At("HEAD"))`: applica atomisticamente `this.hitResult = permissiveHit`; Vanilla procede con `useItemOn` chiudendo la porta al primo colpo da mouse fisico, tasto `]` o Numpad Enter;
  3. Suite di 7 test headless mirati (`DoorInteractionHelperTest`) con 315/315 test passati.
- **Piani Tecnici e Rapporti di Riferimento**:
  - [`PIANO_TECNICO_REV_MC-26.12_MAGNETISMO_VOXEL_PORTE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_REV_MC-26.12_MAGNETISMO_VOXEL_PORTE.md)
  - [`REPORT_SESSIONE_REV_MC-26.12_MAGNETISMO_VOXEL_PORTE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_REV_MC-26.12_MAGNETISMO_VOXEL_PORTE.md)
  - [`STRATEGIA_COGNITIVA_REV_MC-26.12_MAGNETISMO_VOXEL_PORTE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/archiviate/STRATEGIA_COGNITIVA_REV_MC-26.12_MAGNETISMO_VOXEL_PORTE.md)
- **Esito Collaudo**: Collaudata con successo al 100% da Luca in-game il 07/09/2026: chiusura immediata delle porte aperte al primo tocco puntando verso il varco. 315/315 test automatici verdi.

---

### 🟢 Rev MC-26.11 — Revisione 5D.7 (R1, R2, R3): Geometria LadderBlock, Disaccoppiamento Shift Umano, Clearance Volumetrica FallDetector e Convergenza Totale Torre Belvedere
- **Stato**: `[COLLAUDATA CON SUCCESSO AL 100% IN-GAME DA LUCA]`
- **Versione Chiusura**: 26.2-1.19.0-dev (Data 2026-09-05)
- **Problemi Riscontrati (Esperienza Luca)**:
  1. *Falso Blocco Torre Belvedere*: Il navigatore vocale restituiva `Nessun percorso sicuro` (`NO_PATH`) nel tentativo di raggiungere o scendere dalla torre Belvedere, bloccandosi sulla curva stretta a L tra le due rampe a quota 79.
  2. *Falso Annullamento AutoWalk da Sneak di Sicurezza*: Durante l'avanzamento verso la scala a pioli, l'arresto per la rotazione vicino al ciglio della tromba scale faceva attivare l'accovacciamento protettivo di `SafetyMovementGuard`; `AutoWalkMotor` interpretava lo sneak sintetico di Minecraft come una pressione manuale del tasto Shift da parte dell'utente, revocando la navigazione con *"Navigazione automatica annullata"*.
  3. *Falsi Allarmi di Caduta con Soffitto Basso o Muro Sopra*: `FallDetector` allarmava dislivelli anche se il varco superiore era bloccato da blocchi solidi ad altezza occhi.
- **Evidenza Telemetrica / Log**:
  - Salita Belvedere: `Pass1: NO_PATH (192 nodes)`. Scavalcata la scala a pioli: `Pass1: FOUND (4 nodes)`.
  - Tromba scale: `SafetyMovementGuard.engageFallProtection()` -> `client.options.keyShift.setDown(true)` -> `AutoWalkMotor.isManualMovementKeyPressed` = `true` -> `cancel()`.
- **Soluzioni Applicate (PRAPI)**:
  1. *Contratto D6 (Geometria Voxel LadderBlock a 4 Pilastri)*:
     - `AutoWalkPathfinder.isPassable`: restituisce `true` per `LadderBlock`.
     - `AutoWalkPathfinder.isClearHeadroom`: restituisce `true` per `LadderBlock`.
     - `AutoWalkPathfinder.isStandable`: restituisce categoricamente `false` per `belowState instanceof LadderBlock`, impedendo cadute nel vuoto o salite spurie verso botole e tetti.
     - `AutoWalkPathfinder.isSolid`: restituisce `false` per `LadderBlock`, garantendo la trasparenza nelle discese verticali e linea di vista.
  2. *Contratto D7 (Disaccoppiamento Shift Umano in AutoWalkMotor)*:
     - Iniezione di `CrouchIntentProbe` con implementazione `RawCrouchIntentProvider` (lettura GLFW hardware puro).
     - Riforma di `isManualMovementKeyPressed`: solo i tasti Shift fisici reali attivano il Takeover manuale. Lo sneak di emergenza sintetico non interrompe la navigazione.
  3. *Contratto D8 (Clearance Volumetrica Occhi/Testa in FallDetector)*:
     - In `isStandingOnDangerousEdge` e `findDangerAhead`: verifica clearance su `stepPos.above()`. Se il blocco a quota occhi è solido/non calpestabile, la cella viene scartata perché il giocatore non può fisicamente cadervi attraverso.
- **Piani Tecnici e Rapporti di Riferimento**:
  - [`PIANO_CORRETTIVO_FASE5D7_BUDGET_PORTE_GOAL_WAYPOINT_E_CONVERGENZA.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_CORRETTIVO_FASE5D7_BUDGET_PORTE_GOAL_WAYPOINT_E_CONVERGENZA.md)
  - [`RAPPORTO_REVISIONE_5D7_R3_LADDER_BLOCK_DISACCOPPIAMENTO_SHIFT_E_TORRE_BELVEDERE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/RAPPORTO_REVISIONE_5D7_R3_LADDER_BLOCK_DISACCOPPIAMENTO_SHIFT_E_TORRE_BELVEDERE.md)
- **Report di Sessione & File Correlati**: [`RAPPORTO_REVISIONE_5D7_R3_LADDER_BLOCK_DISACCOPPIAMENTO_SHIFT_E_TORRE_BELVEDERE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/RAPPORTO_REVISIONE_5D7_R3_LADDER_BLOCK_DISACCOPPIAMENTO_SHIFT_E_TORRE_BELVEDERE.md), [`RAPPORTO_TELEMETRIA_E_ANALISI_PORTE_LOOKAT_FASE5D.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/RAPPORTO_TELEMETRIA_E_ANALISI_PORTE_LOOKAT_FASE5D.md)
- **Esito Collaudo**: Collaudata con successo empirico al 100% da Luca in-game il 05/09/2026: percorsi lunghi (stalla cava 90m in 27s, granaio, corte) e scalata ininterrotta alla torre Belvedere (81m in 21s) senza alcuna interruzione. 299/299 test automatici verdi.

---

### 🟢 Rev MC-26.9 — NullPointer Guard su currentScreen e Anti-Ghost in InventoryControls
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.19.0-dev (Data 2026-09-04)
- **Problema Riscontrato (Esperienza Luca)**: Durante la transizione o chiusura rapida dell'inventario verso il menu di gioco, la pressione di un tasto di navigazione slot poteva generare NPE su `currentScreen`, muovere il mouse senza contesto o produrre narrazioni residue dello slot.
- **Evidenza Telemetrica / Log**:
  ```text
  Caused by: java.lang.NullPointerException: Cannot invoke "org.mcaccess.minecraftaccess.mixin.AbstractContainerScreenAccessor.getLeftPos()" because "this.currentScreen" is null
      at knot//org.mcaccess.minecraftaccess.features.inventory_controls.InventoryControls.moveToSlotItem(InventoryControls.java:1022)
  ```
- **Soluzione Applicata (PRAPI)**:
  1. Predicato centrale `isActiveContainerScreen()` con verifica rigorosa dell'identità d'istanza (`activeScreen instanceof AbstractContainerScreen && activeScreen == currentScreen`);
  2. Sincronizzazione ciclo lifecycle in `tick()` prima del debounce dell'intervallo con `clearNavigationState()`;
  3. Guard a monte su tutti i 18 handler Kuma e su tutti i metodi di navigazione/focus (`changeGroup`, `selectGroup`, `focusSlotItemAt`, `focusSlotItem`, `changeRecipeTab`, `changeCreativeInventoryTab`, `narrateRecipeInfo`);
  4. Guard a valle in entrambi gli overload di `moveToSlotItem` (`if (slotItem == null || !isActiveContainerScreen()) return;`);
  5. Inizializzazione difensiva di `interval` con `Interval.ms(150)` e null-check su `Config.getInstance()`.
- **Piano Tecnico di Riferimento**: [`PIANO_TECNICO_CORRETTIVO_REV_MC-26.9_MC-26.10_GUI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_CORRETTIVO_REV_MC-26.9_MC-26.10_GUI.md)
- **Report di Sessione & File Correlati**: [`REPORT_STATO_SISTEMA_E_HANDOFF_ANOMALIE_GUI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_STATO_SISTEMA_E_HANDOFF_ANOMALIE_GUI.md)
- **Esito Collaudo**: Collaudata con successo sul campo in-game; zero eccezioni nei log (`latest.log`) e navigazione da tastiera solida e priva di ghost narration.

---

### 🟢 Rev MC-26.10 — Soppressione Accovacciamento Non Intenzionale (Shift Sneak Hijack) all'Interno delle Schermate GUI
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.19.0-dev (Data 2026-09-04)
- **Problema Riscontrato (Esperienza Luca)**: All'interno di qualsiasi interfaccia GUI (inventario, banco di lavoro, fornace, cassa), la pressione del tasto `Shift` per combinazioni di tasti o quick-move attivava contemporaneamente l'accovacciamento nel mondo con rintocchi audio `SHOVEL_FLATTEN`.
- **Soluzione Applicata (PRAPI)**:
  1. `RawCrouchIntentProvider` preservato puro al 100% come fedele lettore hardware GLFW (Single Responsibility);
  2. Metodo `suspendForGui()` in `SafetyMovementGuard` con ownership token rigoroso: rilascia il crouch con `applyIfChanged(false)` solo se `systemOverrideActive` era vero, senza toccare la postura manuale né interrogare il probe hardware;
  3. Routing esplicito in `FallDetector.tick`: se `client.gui.screen() != null`, esecuzione prioritaria di `resetSafetyStateForGui()` (che chiama `suspendForGui()`), separata dal reset ordinario nel mondo (`resetSafetyState()`);
  4. Revoca immediata di `currentAllowedDescentId` e ripresa trasparente dello Shift manuale una volta chiusa la schermata.
- **Piano Tecnico di Riferimento**: [`PIANO_TECNICO_CORRETTIVO_REV_MC-26.9_MC-26.10_GUI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_CORRETTIVO_REV_MC-26.9_MC-26.10_GUI.md)
- **Report di Sessione & File Correlati**: [`REPORT_STATO_SISTEMA_E_HANDOFF_ANOMALIE_GUI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_STATO_SISTEMA_E_HANDOFF_ANOMALIE_GUI.md)
- **Esito Collaudo**: Collaudata con successo sul campo in-game; nessun accovacciamento o suono di pala durante l'uso di Shift nelle schermate GUI e ripresa immediata nel mondo.

---

### 🟢 Rev MC-26.8 — Discesa Sicura su Scale a Pioli ed Elementi Arrampicabili (Climbable Bypass in FallDetector)
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.19.0-dev (Data 2026-09-03)
- **Problema Riscontrato**: In presenza di scale a pioli a bordo piattaforma, l'auto-sneak bloccava l'accesso alla discesa considerandola un burrone/caduta.
- **Soluzione Applicata (PRAPI)**:
  1. Integrazione eccezione elementi arrampicabili (`BlockTags.CLIMBABLE`, `LadderBlock`, `VineBlock`, `ScaffoldingBlock`) nella scansione verticale di `FallDetector`;
  2. Riconoscimento della discesa intenzionale con bypass sicuro (`depth = 0`) e notifica vocale `Discesa sicura`.
- **Piano Tecnico di Riferimento**: [`PIANO_TECNICO_REV_MC-26.8_TRAVERSAL_SAFETY_E_ARRAMPICATA.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/attivi/PIANO_TECNICO_REV_MC-26.8_TRAVERSAL_SAFETY_E_ARRAMPICATA.md)
- **Report di Sessione & File Correlati**: [`RAPPORTO_STRATEGIA_SISTEMICA_DISCESA_LATCHING_E_CENTRATURA.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/RAPPORTO_STRATEGIA_SISTEMICA_DISCESA_LATCHING_E_CENTRATURA.md), [`RAPPORTO_CONVALIDA_STRATEGIA_SISTEMICA_CHATGPT_TRAVERSAL.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/RAPPORTO_CONVALIDA_STRATEGIA_SISTEMICA_CHATGPT_TRAVERSAL.md), [`RAPPORTO_RIAPERTURA_REV_MC_26_8_E_PROTOCOLLO_ESECUZIONE_ANTIGRAVITY.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/RAPPORTO_RIAPERTURA_REV_MC_26_8_E_PROTOCOLLO_ESECUZIONE_ANTIGRAVITY.md), [`RAPPORTO_VALUTAZIONE_E_CONVERGENZA_REVISIONE_CHATGPT_TRAVERSAL.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/RAPPORTO_VALUTAZIONE_E_CONVERGENZA_REVISIONE_CHATGPT_TRAVERSAL.md)
- **Esito Collaudo**: Collaudata con successo sul campo in entrambe le istanze.

---

### 🟢 Rev MC-29.0 — Feedback Adattivo di Dislivello Verticale & Altezza Cubi
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.18.0 (Data 2026-09-02)
- **Problema Riscontrato**: Necessità di percepire istantaneamente e con precisione il dislivello di blocchi ed entità rispetto al giocatore, sia tramite suoni dedicati sia tramite sintesi vocale configurabile.
- **Soluzione Applicata (PRAPI)**:
  1. Introdotte 4 modalità in `Config.java` (`SOUND_AND_VOICE`, `SOUND_ONLY`, `VOICE_ONLY`, `OFF`);
  2. Introdotte 3 modalità di verbosità vocale (`DESCRIPTIVE`, `COMPACT`, `DELTA_ONLY`);
  3. Aggiunto toggle `narrateSameLevel` per escludere facoltativamente gli annunci a quota zero;
  4. Implementato calcolo matematico deterministico di $\Delta Y = Y_{\text{target}} - Y_{\text{player\_feet}}$.
- **Piano Tecnico di Riferimento**: [`PIANO_TECNICO_FEEDBACK_ADATTIVO_DISLIVELLO_E_ALTEZZA_CUBI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_FEEDBACK_ADATTIVO_DISLIVELLO_E_ALTEZZA_CUBI.md)
- **Report di Sessione & File Correlati**: [`RAPPORTO_STRATEGICO_CROSSHAIR_FEEDBACK_MANAGER_PUNTO_15.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/RAPPORTO_STRATEGICO_CROSSHAIR_FEEDBACK_MANAGER_PUNTO_15.md), [`REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md)
- **Esito Collaudo**: Collaudato con pieno successo in-game.

---

### 🟢 Rev MC-29.1 — Regolatore di Verbosità Faccia del Blocco
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.18.0 (Data 2026-09-02)
- **Problema Riscontrato**: Necessità di controllare la verbosità dell'annuncio della faccia colpita dal mirino per non saturare la sintesi durante l'esplorazione.
- **Soluzione Applicata (PRAPI)**:
  1. Aggiunte 4 modalità di verbosità in `Config.java` (`DESCRIPTIVE`, `TOP_BOTTOM_ONLY`, `COMPACT`, `OFF`);
  2. Integrazione con `BlockFace` e localizzazioni IT/EN.
- **Piano Tecnico di Riferimento**: [`PIANO_TECNICO_FEEDBACK_ADATTIVO_DISLIVELLO_E_ALTEZZA_CUBI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_FEEDBACK_ADATTIVO_DISLIVELLO_E_ALTEZZA_CUBI.md)
- **Report di Sessione & File Correlati**: [`RAPPORTO_STRATEGICO_CROSSHAIR_FEEDBACK_MANAGER_PUNTO_15.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/RAPPORTO_STRATEGICO_CROSSHAIR_FEEDBACK_MANAGER_PUNTO_15.md), [`REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md)
- **Esito Collaudo**: Collaudato con pieno successo in-game.

---

### 🟢 Rev MC-29.2 — Architettura SSOT & Centralizzazione Mirino in `CrosshairFeedbackManager`
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.18.0 (Data 2026-09-02)
- **Problema Riscontrato**: Race condition e duplicazione messaggi tra rotazione testa (Yaw/Pitch), centramento orizzonte (`KP_5`/`M`), lettura manuale (`B`) e tick del mirino.
- **Soluzione Applicata (PRAPI)**:
  1. Creato `CrosshairFeedbackManager.java` come Presentation Coordinator e Single Source of Truth;
  2. Disaccoppiati e coordinati i canali: Canale A (Tick/Movimento), Canale B (Centramento `onCameraCentered`), Canale C (Lettura Manuale `B`);
  3. Stato atomico unico e debouncing temporale unificato.
- **Piano Tecnico di Riferimento**: [`PIANO_TECNICO_FEEDBACK_ADATTIVO_DISLIVELLO_E_ALTEZZA_CUBI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_FEEDBACK_ADATTIVO_DISLIVELLO_E_ALTEZZA_CUBI.md)
- **Report di Sessione & File Correlati**: [`RAPPORTO_STRATEGICO_CROSSHAIR_FEEDBACK_MANAGER_PUNTO_15.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/RAPPORTO_STRATEGICO_CROSSHAIR_FEEDBACK_MANAGER_PUNTO_15.md), [`REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md)
- **Esito Collaudo**: Collaudato con pieno successo in-game.

---

### 🟢 Rev MC-29.3 — Bonifica Dead Code & Ottimizzazione Mirino in Movimento
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.18.0 (Data 2026-09-02)
- **Problema Riscontrato**: Dead code legacy in `CrosshairFeedbackManager`, import orfano `Interval` e doppio raycast ridondante in `MinecraftAccess.narrate`.
- **Soluzione Applicata (PRAPI)**:
  1. Bonifica a 5 barriere: eliminati metodi e campi orfani;
  2. `MinecraftAccess.narrate` sfrutta direttamente il raycast passato in ingresso senza rieseguirlo;
  3. Raggio di interazione allineato a `Math.max(blockRange, entityRange)` (4.5m).
- **Piano Tecnico di Riferimento**: [`PIANO_TECNICO_FEEDBACK_ADATTIVO_DISLIVELLO_E_ALTEZZA_CUBI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_FEEDBACK_ADATTIVO_DISLIVELLO_E_ALTEZZA_CUBI.md)
- **Report di Sessione & File Correlati**: [`RAPPORTO_STRATEGICO_CROSSHAIR_FEEDBACK_MANAGER_PUNTO_15.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/RAPPORTO_STRATEGICO_CROSSHAIR_FEEDBACK_MANAGER_PUNTO_15.md), [`REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md)
- **Esito Collaudo**: Collaudato con pieno successo in-game.

---

### 🟢 Rev MC-29.4 — Armonizzazione Concorrenza & Soppressione Loop da Fermi
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.18.0 (Data 2026-09-02)
- **Problema Riscontrato**: Ripetizione continua a intervalli fissi dell'ObstacleDetector in condizioni di fermata contro ostacoli.
- **Soluzione Applicata (PRAPI)**:
  1. Rimossa la ripetizione forzata da fermi quando la posizione e lo stato dell'ostacolo non variano;
  2. Preservata la reattività istantanea sui cambi di blocco e all'avvicinamento.
- **Piano Tecnico di Riferimento**: [`PIANO_TECNICO_FEEDBACK_ADATTIVO_DISLIVELLO_E_ALTEZZA_CUBI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_FEEDBACK_ADATTIVO_DISLIVELLO_E_ALTEZZA_CUBI.md)
- **Report di Sessione & File Correlati**: [`RAPPORTO_STRATEGICO_CROSSHAIR_FEEDBACK_MANAGER_PUNTO_15.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/RAPPORTO_STRATEGICO_CROSSHAIR_FEEDBACK_MANAGER_PUNTO_15.md), [`REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md)
- **Esito Collaudo**: Collaudato con pieno successo in-game.

---

### 🟢 Rev MC-29.5 — Ripristino Cadenza Podometro & Aggancio Volumetrico Voxel per Lamine Sottili
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.18.0 (Data 2026-09-02)
- **Problema Riscontrato**:
  1. La soppressione di blocchi uguali adiacenti toglieva il "contapassi / radar di cadenza" al giocatore non vedente mentre camminava lungo una parete;
  2. Nei passi laterali veloci, il mirino saltava porte e pannelli di vetro a causa dello spessore ridotto ($0.12\text{--}0.18\text{m}$).
- **Soluzione Applicata (PRAPI)**:
  1. Rimossa la soppressione silenziosa in `CrosshairFeedbackManager.java`: ogni coordinata voxel attraversata emette il feedback compatto ritmico (*"Assi di quercia, a 1 blocco"*);
  2. Campionamento volumetrico continuo lungo la linea di vista in `PlayerUtils.crosshairTarget` per `DoorBlock`, `CrossCollisionBlock`, `FenceBlock`, `IronBarsBlock`, `FenceGateBlock`, `TrapDoorBlock`.
- **Piano Tecnico di Riferimento**: [`PIANO_TECNICO_FEEDBACK_ADATTIVO_DISLIVELLO_E_ALTEZZA_CUBI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_FEEDBACK_ADATTIVO_DISLIVELLO_E_ALTEZZA_CUBI.md)
- **Report di Sessione & File Correlati**: [`RAPPORTO_STRATEGICO_CROSSHAIR_FEEDBACK_MANAGER_PUNTO_15.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/RAPPORTO_STRATEGICO_CROSSHAIR_FEEDBACK_MANAGER_PUNTO_15.md), [`REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md)
- **Esito Collaudo**: Collaudato con pieno successo in-game.

---

### 🟢 Rev MC-29.6 — Dispacciamento Diretto Ostacoli (`onObstacleDetected`), Micro-Voxel Raymarch ($0.05\text{m}$) & Armonizzazione $XZ$
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.18.0 (Data 2026-09-02)
- **Problema Riscontrato**:
  1. Suono ostacolo attivo ma voce muta durante l'avvicinamento frontale verso un ostacolo a causa di un meccanismo passivo di pending warning;
  2. Duplicazione ridondante del prefisso frontale (*"Davanti: Ostacolo... Davanti: ..."*) per mancato allineamento di colonna orizzontale $XZ$;
  3. Salto delle lamine sottili a coordinate negative ($X = -64.8$) con passo $0.25\text{m}$.
- **Soluzione Applicata (PRAPI)**:
  1. **Dispacciamento Diretto al Manager (`onObstacleDetected`)**: Invocazione diretta da `ObstacleDetector` a `CrosshairFeedbackManager.onObstacleDetected(...)`, garantendo sincronia immediata tra cue sonoro 3D e sintesi vocale:
     > *"Davanti: Ostacolo di Pannello di vetro, a 3 blocchi"*;
  2. **Armonizzazione Colonna Unica ($XZ$)**: Se piedi e sguardo puntano alla stessa barriera/colonna frontale, eroga un unico messaggio pulito senza ridondanze; per movimenti laterali o retro, compone fluidamente (*"A destra: Salita su Fornace. Davanti: Assi di quercia, a 2 blocchi"*);
  3. **Micro-Voxel Raymarch Continuo ($0.05\text{m}$)**: Avvio del campionamento a $d = 0.05\text{m}$ con passo $0.10\text{m}$ in `PlayerUtils.crosshairTarget`.
- **Piano Tecnico di Riferimento**: [`PIANO_TECNICO_FEEDBACK_ADATTIVO_DISLIVELLO_E_ALTEZZA_CUBI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_FEEDBACK_ADATTIVO_DISLIVELLO_E_ALTEZZA_CUBI.md)
- **Report di Sessione & File Correlati**: [`RAPPORTO_STRATEGICO_CROSSHAIR_FEEDBACK_MANAGER_PUNTO_15.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/RAPPORTO_STRATEGICO_CROSSHAIR_FEEDBACK_MANAGER_PUNTO_15.md), [`REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md)
- **Esito Collaudo**: Collaudato con pieno successo in-game e confermato da telemetria live.

---

### 🟢 Rev MC-28.0 — Navigatore Automatico: Calibrazione Fisica Salto Automatico su Dislivelli & Guardia Cloth Config
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.17.1 (Data 2026-09-02)
- **Problema Riscontrato (Esperienza Luca)**: Con opzione "Salto automatico ostacoli superabili" attiva (`config.autoJump == true`), il pilota automatico si arrestava davanti a un blocco saltabile (+1 Y) e dichiarava *"Percorso ostruito, marcia arrestata"* invece di eseguire il salto.
- **Evidenza Telemetrica / Log**: `[17:08:03] Percorso ostruito, marcia arrestata` $\rightarrow$ Risolto in telemetria live: `[17:38:33] Arrivato a destinazione: Aperto Porta di betulla` e `[17:39:47] Arrivato a destinazione`.
- **Causa Radice**: In `AutoWalkController.java:319`, il salto richiedeva rigidamente `distH < 0.65`. Essendo il centro del blocco distante $0.5\text{ m}$ e il raggio della hitbox del giocatore $0.3\text{ m}$, la collisione fisica contro il blocco avviene a $\text{distH} \approx 0.80\text{ m}$. La soglia $< 0.65$ richiedeva una compenetrazione fisica impossibile dentro il blocco solido.
- **Soluzione Applicata (PRAPI)**:
  1. Ricalibrata la condizione di salto automatico: $\text{distH} \le 1.25\text{ m}$ oppure `player.horizontalCollision == true`, con dislivello saltabile $0.30 < \Delta Y \le 1.25$ e appoggio al suolo `onGround == true`;
  2. Spinta verticale estesa a `jumpHoldingTicks = 4` (200ms) per garantire il superamento del blocco;
  3. Tutela assoluta della guardia `config.autoJump`: se disattivato in Cloth Config, il pilota non salta e si arresta per il controllo manuale.
- **Piano Tecnico di Riferimento**: [`PIANO_TECNICO_SALTO_AUTOMATICO_PILOTA_E_CALIBRAZIONE_HITBOX.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_SALTO_AUTOMATICO_PILOTA_E_CALIBRAZIONE_HITBOX.md)
- **Report di Sessione & File Correlati**: [`REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md)
- **Esito Collaudo**: Superato con successo al 100% in telemetria live su rotte da 16 e 53 metri.

---

### 🟢 Rev MC-28.1 — Menu di Pausa (`Esc`): Auto-Focus Iniziale & Navigazione Immediata a Frecce Direzionali
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.17.1 (Data 2026-09-02)
- **Problema Riscontrato (Esperienza Luca)**: Premendo `Esc` in partita per aprire il menu di gioco (`PauseScreen`), il focus della tastiera rimaneva perso o bloccato altrove, costringendo a premere `Tab` per iniziare a scorrere i pulsanti con le frecce.
- **Evidenza Telemetrica / Log**: `PauseScreen.class` non era incluso nel set `MENUS_NEED_FIX` e `screen.getFocused() == null`.
- **Causa Radice**: Assenza di gestione di `PauseScreen` in `MenuFix.java` e mancata focalizzazione proattiva del primo widget.
- **Soluzione Applicata (PRAPI)**:
  1. Aggiunto `PauseScreen.class` in `MENUS_NEED_FIX` in `MenuFix.java`;
  2. Implementato `ensureInitialFocus(screen)` per focalizzare all'istante il primo pulsante attivo ("Torna al gioco");
  3. Spostamento preventivo del mouse a coordinate (10, 10) per non interferire.
- **Piano Tecnico di Riferimento**: [`PIANO_TECNICO_SALTO_AUTOMATICO_PILOTA_E_CALIBRAZIONE_HITBOX.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_SALTO_AUTOMATICO_PILOTA_E_CALIBRAZIONE_HITBOX.md)
- **Report di Sessione & File Correlati**: [`REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md)
- **Esito Collaudo**: Superato con successo in telemetria live: `[17:39:55] Pulsante Riprendi la partita. Elemento a schermo 1 di 9` annunciato all'istante all'apertura del menu.

---

### 🟢 Rev MC-27.1 — Mentor Vocale: Direzione Spaziale Contestuale & Keybinding Introspection
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.12.0 (Data 2026-09-02)
- **Problema Riscontrato (Esperienza Luca)**: Muovendosi lateralmente con `A` o `D` contro una parete, il Mentor pronunciava la frase fissa *"Hai un ostacolo di fronte..."* e non forniva il comando reale per ispezionare l'ostacolo.
- **Evidenza Telemetrica / Log**: `[16:13:01] Delivered contextual mentor hint: HINT_WALL_STUCK`.
- **Causa Radice**: La regola `HINT_WALL_STUCK` usava una stringa hardcodata senza contestualizzazione dell'input WASD e senza interrogazione dei keybinding reali di gioco.
- **Soluzione Applicata (PRAPI)**:
  1. Riconoscimento dinamico dell'asse reale di collisione/movimento (`a sinistra`, `a destra`, `davanti`, `dietro`, `avanti a sinistra`, ecc.) in `PlayerContextEngine`;
  2. Risoluzione dei tasti a runtime (*Keybinding Introspection*) per Salto (`keyJump` -> *"Spazio"*) e Ispezione Ostacolo ([`ObstacleDetector`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/ObstacleDetector.java#L66) -> *"Alt + V"*);
  3. Passaggio diretto degli argomenti a `I18n.get(key, args)` eliminando il prefisso spurio *"Format error:"*;
  4. Frase finale erogata: *"Hai un ostacolo a sinistra. Premi Spazio per saltare se è basso, oppure premi Alt + V per ispezionarlo."*.
- **Piano Tecnico di Riferimento**: [`PIANO_TECNICO_FEED_MIRINO_IN_MOVIMENTO_E_LETTURA_MANUALE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_FEED_MIRINO_IN_MOVIMENTO_E_LETTURA_MANUALE.md)
- **Report di Sessione & File Correlati**: [`REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md)
- **Esito Collaudo**: Superato con pieno successo in telemetria live e convalidato da Luca.

---

### 🟢 Rev MC-26.0A — ClassCastException al cambio scheda Ricettario (`V` / `Shift+V`)
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.12.0 (Data 2026-09-02)
- **Problema Riscontrato (Esperienza Luca)**: Crash dell'handler di input premendo `V` o `Shift+V` nel ricettario a causa di un cast improprio.
- **Evidenza Telemetrica / Log**: `java.lang.ClassCastException: class RecipeBookCategory cannot be cast to SearchRecipeBookCategory` in `InventoryControls.java:836-838`.
- **Causa Radice**: `recipeBookComponentAccessor.getSelectedTab().getCategory()` in 26.2 non implementa `SearchRecipeBookCategory`.
- **Soluzione Applicata (PRAPI)**: Rimosso il cast forzato e inserita lettura sicura della categoria con guardia difensiva.
- **Report di Sessione & File Correlati**: [`REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md)
- **Esito Collaudo**: Risolto e collaudato con successo in-game.

---

### 🟢 Rev MC-26.0B — Avviso GUI Mancante per Singleton `Config.instance`
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.12.0 (Data 2026-09-02)
- **Problema Riscontrato (Esperienza Luca)**: Log inondato da warning di Cloth Config all'avvio.
- **Evidenza Telemetrica / Log**: `No GUI provider registered for field 'private static Config instance'`.
- **Causa Radice**: AutoConfig di Cloth Config analizza per riflessione tutti i campi non esclusi.
- **Soluzione Applicata (PRAPI)**: Aggiunta l'annotazione `@ConfigEntry.Gui.Excluded` sopra il singleton `instance` in `Config.java`.
- **Report di Sessione & File Correlati**: [`REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md)
- **Esito Collaudo**: Nessun warning o errore nei log di avvio e configurazione.

---

### 🟢 Rev MC-26.1 — Feedback Vocale & Auto-Focus al Cambio Categoria Ricettario (`V` / `Shift+V`)
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.12.0 (Data 2026-09-02)
- **Problema Riscontrato (Esperienza Luca)**: Cambio scheda silenzioso e cursore disorientato se non a fuoco sul ricettario.
- **Evidenza Telemetrica / Log**: Nessun annuncio o evento audio al cambio tab.
- **Causa Radice**: Assenza di feedback sonoro e di logica di auto-focus all'evento tasto `V`.
- **Soluzione Applicata (PRAPI)**:
  1. Suono di interazione `UI_BUTTON_CLICK` alla pressione di `V` / `Shift+V`;
  2. Risoluzione dinamica del nome localizzato in italiano (*"Costruzione"*, *"Attrezzatura"*, *"Varie"*, *"Meccanismi e Redstone"*);
  3. Selezione automatica del gruppo ricette e posizionamento cursore sul primo elemento;
  4. Annuncio coordinato *"Categoria: [Nome]. [Statistiche ricette]"*.
- **Report di Sessione & File Correlati**: [`REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md)
- **Esito Collaudo**: Superato con successo in-game.

---

### 🟢 Rev MC-26.2 — Feedback Vocale & Auto-Focus al Cambio Pagina Ricettario (`Shift+I` / `Shift+K`)
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.12.0 (Data 2026-09-02)
- **Problema Riscontrato (Esperienza Luca)**: La pagina del ricettario girava visivamente ma senza vocalizzare il numero di pagina né riposizionare il cursore.
- **Evidenza Telemetrica / Log**: Pagine non sincronizzate acusticamente.
- **Causa Radice**: Mancanza di accessor Mixin per `currentPage` e `totalPages` e assenza di riposizionamento cursore.
- **Soluzione Applicata (PRAPI)**:
  1. Accessor Mixin `RecipeBookPageAccessor` per estrarre `currentPage` e `totalPages`;
  2. Suono click e spostamento cursore sulla prima ricetta della nuova pagina;
  3. Annunci dedicati per limiti (*"Prima pagina"*, *"Ultima pagina"*, *"Unica pagina"*).
- **Report di Sessione & File Correlati**: [`REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md)
- **Esito Collaudo**: Superato con successo in-game.

---

### 🟢 Rev MC-26.3 — Navigazione Universale a Slot con le 4 Frecce Direzionali
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.12.0 (Data 2026-09-02)
- **Problema Riscontrato (Esperienza Luca)**: Navigazione slot confinata a `I, K, J, L`; necessità di navigare intuitivamente con le 4 Frecce Direzionali.
- **Evidenza Telemetrica / Log**: Frecce non intercettate in `AbstractContainerScreen`.
- **Causa Radice**: Handler tastiera vincolato solo ai keycode legacy.
- **Soluzione Applicata (PRAPI)**:
  1. Mappatura universale delle 4 Frecce in `InventoryControls.java`;
  2. Piena compatibilità con tutte le schermate contenitore (casse, forni, banchi, villici);
  3. Disaccoppiamento con le caselle di testo `EditBox` (le frecce muovono il testo se a fuoco, navigano gli slot se non a fuoco).
- **Report di Sessione & File Correlati**: [`REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md)
- **Esito Collaudo**: Superato con successo in-game.

---

### 🟢 Rev MC-26.5 — Armonizzazione Sistemica Statistiche di Pagina (`V`, `Shift+I`/`K`, `R`)
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.12.0 (Data 2026-09-02)
- **Problema Riscontrato (Esperienza Luca)**: Mancanza di un quadro chiaro sul numero di ricette realizzabili rispetto a quelle totali della pagina aperta.
- **Evidenza Telemetrica / Log**: Telemetria ore 03:04.
- **Causa Radice**: Annunci privi del conteggio aggregato dello stato di crafting.
- **Soluzione Applicata (PRAPI)**:
  1. Calcolo ricette realizzabili ($R$) e non realizzabili ($N$) sulla pagina corrente;
  2. Annuncio atomico sincronizzato: `"[T] ricette: [R] realizzabili, [N] non realizzabili"`, `"[T] ricette realizzabili"` o `"[T] ricette non realizzabili"`.
- **Report di Sessione & File Correlati**: [`REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md)
- **Esito Collaudo**: Superato con precisione 100% in-game.

---

### 🟢 Rev MC-26.6 — Concordanza Grammaticale Singolare/Plurale nelle Statistiche
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.12.0 (Data 2026-09-02)
- **Problema Riscontrato (Esperienza Luca)**: Quando il conteggio ricette è pari a 1, la sintesi usava il plurale (es. *"1 ricette realizzabili"* anziché *"1 ricetta realizzabile"*).
- **Evidenza Telemetrica / Log**: Telemetria ore 03:27.
- **Causa Radice**: Formattazione con stringhe fisse senza flessione grammaticale per $T=1$.
- **Soluzione Applicata (PRAPI)**:
  1. Introdotte chiavi I18N differenziate singolare/plurale in `it_it.json` ed `en_us.json`;
  2. Flessione dinamica: $1 \rightarrow$ *"1 ricetta realizzabile"*, $>1 \rightarrow$ *"%d ricette realizzabili"*.
- **Report di Sessione & File Correlati**: [`REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md)
- **Esito Collaudo**: Superato con successo in-game.

---

### 🟢 Rev MC-26.4 — Feedback Eventi Visivi & Auto-Focus su Schermate Specialistiche
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.16.1 (Data 2026-09-02)
- **Problema Riscontrato (Esperienza Luca)**: Alcune schermate specialistiche mancavano di annunci dedicati all'inserimento di oggetti o al completamento di cicli di lavorazione e richiedevano la navigazione manuale tra gruppi.
- **Evidenza Telemetrica / Log**: `StonecutterScreen`, `LoomScreen`, `FurnaceScreen`, `BrewingStandScreen`.
- **Causa Radice**: Assenza di listener di stato dedicati nei tick di controllo per container specialistici e mancata rigenerazione del focus sul gruppo ricette.
- **Soluzione Applicata (PRAPI)**:
  1. *Tagliapietre (`StonecutterScreen`)*: Vocalizzazione forme disponibili e posizionamento automatico del focus sul primo taglio con `selectGroupByKey("recipes", false)`;
  2. *Telaio (`LoomScreen`)*: Tracciamento dinamico e annuncio motivi disponibili all'inserimento di stendardo e tintura con focus sul selettore motivi;
  3. *Fornaci & Alambicco*: Notifiche vocali discrete (*"Cottura completata"*, *"Distillazione completata"*) al termine della cottura o della distillazione.
- **Piano Tecnico di Riferimento**: [`PIANO_TECNICO_REV_MC_26_4_FEEDBACK_SCHERMATE_SPECIALISTICHE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_REV_MC_26_4_FEEDBACK_SCHERMATE_SPECIALISTICHE.md)
- **Report di Sessione & File Correlati**: [`REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md)
- **Esito Collaudo**: Superato con pieno successo in telemetria live e confermato da Luca.

### 🟢 Rev MC-26.8-FaseA (Bozza Storica Preliminare) — Discesa Sicura su Scale a Pioli ed Elementi Arrampicabili
- **Stato**: `[SUPERATA DA REV MC-26.8B / COLLAUDATA CON SUCCESSO]`
- **Data Rilevamento**: 2026-09-03
- **Data Chiusura**: 2026-09-03
- **Problema Riscontrato (Esperienza Luca)**: Salendo sul tetto tramite scala a pioli, l'utente non riesce più a scendere: `FallDetector` classifica il vuoto attorno alla scala come burrone letale (`profondità 4 blocchi`), attiva lo sticky‑sneak sul ciglio e l'auto‑sneak forzato, bloccando fisicamente il giocatore e costringendolo a disattivare la protezione anticaduta (`Ctrl + Alt + F`) per poter scendere la scala.
- **Evidenza Telemetrica / Log**: `[15:43:35] Narrating(interrupt:true)= Sul ciglio: burrone 1 blocchi in basso , profondità 4 blocchi`, `[15:44:33] Narrating(interrupt:true)= Attenzione: burrone 1 blocchi avanti 1 blocchi in basso , profondità 3 blocchi`.
- **Causa Radice**:
  1. `isStandingOnDangerousEdge` campiona radialmente 8 punti attorno alla hitbox, i campioni laterali/diagonali rilevano aria e vuoto oltre il perimetro del tetto, forzando lo sticky‑sneak anche se davanti c'è una colonna di discesa sicura;
  2. Il motore fisico nativo di Minecraft impedisce a un giocatore accovacciato (Shift attivo) di scendere da un blocco solido;
  3. Il raycast di look‑ahead non riconosce la scala a pioli attaccata alla parete o a quota piedi/sottostante quando la traiettoria punta deliberatamente alla scala.
- **Soluzione Proposta (PRAPI / Protocollo 5)**:
  1. Estendere il riconoscimento degli elementi di discesa sicura a tutti i blocchi arrampicabili (scale a pioli, liane, impalcature, botole sopra scale, tag `#minecraft:climbable`).
  2. Quando il giocatore si muove deliberatamente verso una colonna discendente sicura, sospendere temporaneamente l'auto‑sneak forzato (`keyShift.setDown(false)`).
  3. Escludere la colonna della scala dalla segnalazione di burrone e fornire riscontro acustico/vocale positivo di discesa sicura.
- **Piano Tecnico di Riferimento**: In fase di consultazione e pianificazione.
- **Report di Sessione & File Correlati**: [`REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md)
- **Esito Collaudo**: Concluso con successo nella Fase A.
