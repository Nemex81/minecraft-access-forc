# Archivio Storico delle Revisioni & Collaudi Conclusi (RRU)
# Progetto: Minecraft Access (Fork 26.2 / 1.21.x)
# Autore: Luca (Sviluppatore & Collaudatore) & Antigravity (AI Pair Programmer)
# Percorso: docs/report/ARCHIVIO_REVISIONI.md
# Registro Attivo: docs/report/REGISTRO_REVISIONI.md
# Fonte Originale: docs/report/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md

Questo documento raccoglie la memoria storica di tutte le anomalie, correzioni e rifiniture collaudate e chiuse con successo nel ciclo di vita di Minecraft Access.

---

## 🏛️ STORICO REVISIONI COLLAUDATE CON SUCCESSO (CICLO 26.2)

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
- **Piano Tecnico di Riferimento**: `docs/piani/completati/PIANO_TECNICO_CORRETTIVO_REV_MC-26.9_MC-26.10_GUI.md`
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
- **Piano Tecnico di Riferimento**: `docs/piani/completati/PIANO_TECNICO_CORRETTIVO_REV_MC-26.9_MC-26.10_GUI.md`
- **Esito Collaudo**: Collaudata con successo sul campo in-game; nessun accovacciamento o suono di pala durante l'uso di Shift nelle schermate GUI e ripresa immediata nel mondo.

---

### 🟢 Rev MC-26.8 — Discesa Sicura su Scale a Pioli ed Elementi Arrampicabili (Climbable Bypass in FallDetector)
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.19.0-dev (Data 2026-09-03)
- **Problema Riscontrato**: In presenza di scale a pioli a bordo piattaforma, l'auto-sneak bloccava l'accesso alla discesa considerandola un burrone/caduta.
- **Soluzione Applicata (PRAPI)**:
  1. Integrazione eccezione elementi arrampicabili (`BlockTags.CLIMBABLE`, `LadderBlock`, `VineBlock`, `ScaffoldingBlock`) nella scansione verticale di `FallDetector`;
  2. Riconoscimento della discesa intenzionale con bypass sicuro (`depth = 0`) e notifica vocale `Discesa sicura`.
- **Piano Tecnico di Riferimento**: `docs/piani/attivi/PIANO_TECNICO_REV_MC-26.8_TRAVERSAL_SAFETY_E_ARRAMPICATA.md`
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
- **Piano Tecnico di Riferimento**: `docs/piani/completati/PIANO_TECNICO_FEEDBACK_ADATTIVO_DISLIVELLO_E_ALTEZZA_CUBI.md`
- **Esito Collaudo**: Collaudato con pieno successo in-game.

---

### 🟢 Rev MC-29.1 — Regolatore di Verbosità Faccia del Blocco
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.18.0 (Data 2026-09-02)
- **Problema Riscontrato**: Necessità di controllare la verbosità dell'annuncio della faccia colpita dal mirino per non saturare la sintesi durante l'esplorazione.
- **Soluzione Applicata (PRAPI)**:
  1. Aggiunte 4 modalità di verbosità in `Config.java` (`DESCRIPTIVE`, `TOP_BOTTOM_ONLY`, `COMPACT`, `OFF`);
  2. Integrazione con `BlockFace` e localizzazioni IT/EN.
- **Piano Tecnico di Riferimento**: `docs/piani/completati/PIANO_TECNICO_FEEDBACK_ADATTIVO_DISLIVELLO_E_ALTEZZA_CUBI.md`
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
- **Piano Tecnico di Riferimento**: `docs/piani/completati/PIANO_TECNICO_FEEDBACK_ADATTIVO_DISLIVELLO_E_ALTEZZA_CUBI.md`
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
- **Piano Tecnico di Riferimento**: `docs/piani/completati/PIANO_TECNICO_FEEDBACK_ADATTIVO_DISLIVELLO_E_ALTEZZA_CUBI.md`
- **Esito Collaudo**: Collaudato con pieno successo in-game.

---

### 🟢 Rev MC-29.4 — Armonizzazione Concorrenza & Soppressione Loop da Fermi
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.18.0 (Data 2026-09-02)
- **Problema Riscontrato**: Ripetizione continua a intervalli fissi dell'ObstacleDetector in condizioni di fermata contro ostacoli.
- **Soluzione Applicata (PRAPI)**:
  1. Rimossa la ripetizione forzata da fermi quando la posizione e lo stato dell'ostacolo non variano;
  2. Preservata la reattività istantanea sui cambi di blocco e all'avvicinamento.
- **Piano Tecnico di Riferimento**: `docs/piani/completati/PIANO_TECNICO_FEEDBACK_ADATTIVO_DISLIVELLO_E_ALTEZZA_CUBI.md`
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
- **Piano Tecnico di Riferimento**: `docs/piani/completati/PIANO_TECNICO_FEEDBACK_ADATTIVO_DISLIVELLO_E_ALTEZZA_CUBI.md`
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
- **Piano Tecnico di Riferimento**: `docs/piani/completati/PIANO_TECNICO_FEEDBACK_ADATTIVO_DISLIVELLO_E_ALTEZZA_CUBI.md`
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
- **Piano Tecnico di Riferimento**: `docs/piani/completati/PIANO_TECNICO_SALTO_AUTOMATICO_PILOTA_E_CALIBRAZIONE_HITBOX.md`
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
- **Piano Tecnico di Riferimento**: `docs/piani/completati/PIANO_TECNICO_SALTO_AUTOMATICO_PILOTA_E_CALIBRAZIONE_HITBOX.md`
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
- **Piano Tecnico di Riferimento**: `docs/piani/completati/PIANO_TECNICO_FEED_MIRINO_IN_MOVIMENTO_E_LETTURA_MANUALE.md`
- **Esito Collaudo**: Superato con pieno successo in telemetria live e convalidato da Luca.

---

### 🟢 Rev MC-26.0A — ClassCastException al cambio scheda Ricettario (`V` / `Shift+V`)
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.12.0 (Data 2026-09-02)
- **Problema Riscontrato (Esperienza Luca)**: Crash dell'handler di input premendo `V` o `Shift+V` nel ricettario a causa di un cast improprio.
- **Evidenza Telemetrica / Log**: `java.lang.ClassCastException: class RecipeBookCategory cannot be cast to SearchRecipeBookCategory` in `InventoryControls.java:836-838`.
- **Causa Radice**: `recipeBookComponentAccessor.getSelectedTab().getCategory()` in 26.2 non implementa `SearchRecipeBookCategory`.
- **Soluzione Applicata (PRAPI)**: Rimosso il cast forzato e inserita lettura sicura della categoria con guardia difensiva.
- **Esito Collaudo**: Risolto e collaudato con successo in-game.

---

### 🟢 Rev MC-26.0B — Avviso GUI Mancante per Singleton `Config.instance`
- **Stato**: `[COLLAUDATA CON SUCCESSO]`
- **Versione Chiusura**: 26.2-1.12.0 (Data 2026-09-02)
- **Problema Riscontrato (Esperienza Luca)**: Log inondato da warning di Cloth Config all'avvio.
- **Evidenza Telemetrica / Log**: `No GUI provider registered for field 'private static Config instance'`.
- **Causa Radice**: AutoConfig di Cloth Config analizza per riflessione tutti i campi non esclusi.
- **Soluzione Applicata (PRAPI)**: Aggiunta l'annotazione `@ConfigEntry.Gui.Excluded` sopra il singleton `instance` in `Config.java`.
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
- **Piano Tecnico di Riferimento**: `docs/piani/completati/PIANO_TECNICO_REV_MC_26_4_FEEDBACK_SCHERMATE_SPECIALISTICHE.md`
- **Esito Collaudo**: Superato con pieno successo in telemetria live e confermato da Luca.

### 🟢 Rev MC-26.8 — Discesa Sicura su Scale a Pioli ed Elementi Arrampicabili (Climbable Bypass in FallDetector)
- **Stato**: `[CHIUSA]`
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
- **Esito Collaudo**: Concluso con successo nella Fase A.
