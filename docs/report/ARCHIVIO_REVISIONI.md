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
