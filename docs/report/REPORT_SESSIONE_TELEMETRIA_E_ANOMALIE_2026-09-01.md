# Report di Sessione Telemetria In-Game & Registro Revisioni Future
**Data**: 01/09/2026  
**Autore**: Luca (Sviluppatore & Collaudatore) & Antigravity (AI Pair Programmer)  
**Ambiente**: Minecraft 26.2 (Fabric + NeoForge), PrismLauncher ("Minecraft 26.2 Access - Server Tenuta"), Java 25, Screen Reader NVDA  
**Scopo del Documento**: Fornire il contesto diagnostico integrale, le evidenze dai log (`latest.log`), le cause radice e le strategie correttive pronte per l'apertura della nuova sessione di lavoro dedicata.

---

## 1. Sintesi Generale del Collaudo

Il test in-game eseguito sul server dedicato ha confermato la perfetta stabilità e reattività dei sistemi di mobilità e navigazione:
- **Teletrasporto Coordinate**: Eseguito con successo alle ore 18:32:51 con comando `/tp @s 660 81 -3310`, approdo preciso a `[660.5, 81.0, -3309.5]` e annuncio vocale corretto del bioma.
- **Vettori POI & Waypoints**: Tracciamento impeccabile del *"rifugio fronte porta"* (a 1 blocco sotto i piedi) e del punto *"Ultima Morte"* (a 5 blocchi avanti, 1 sotto, 4 a destra).
- **Interazione Voxel & Mirino**: Rilevamento continuo e reattivo dei blocchi (assi di quercia, assi di betulla, terra) con annunci angolari precisi della visuale.
- **Mentor Vocale**: Intervento contestuale non invasivo per stati di idle (`HINT_IDLE_STUCK`).

---

## 2. Dossier Anomalie & Diagnostica Chirurgica

### 🔴 Anomalia 1 — ClassCastException al cambio scheda del Ricettario (Tasto `V` / `Shift+V`)

- **Priorità**: Alta (Bug bloccante dell'handler tastiera nel ricettario)
- **Occorrenze nei Log**: Ore 19:24:14, 19:24:17, 19:24:20, 19:24:25, 19:24:36, 19:24:37
- **Stacktrace Registrato**:
  ```text
  [19:24:14] [Render thread/ERROR]: Error executing task on Client
  net.minecraft.ReportedException: keyPressed event handler
  Caused by: java.lang.ClassCastException: class net.minecraft.world.item.crafting.RecipeBookCategory cannot be cast to class net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory (net.minecraft.world.item.crafting.RecipeBookCategory and net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory are in unnamed module of loader 'knot' @778d1062)
  ```
- **File Sorgente**: `src/main/java/org/mcaccess/minecraftaccess/features/inventory_controls/InventoryControls.java`
- **Righe Coinvolte**: 836-838
- **Codice Attuale**:
  ```java
  ExtendedRecipeBookCategory category = recipeBookComponentAccessor.getSelectedTab().getCategory();
  log.debug("Change tab to {}", ((SearchRecipeBookCategory) category).name());
  ```
- **Causa Radice**:
  In Minecraft 26.2, l'oggetto restituito da `recipeBookComponentAccessor.getSelectedTab().getCategory()` implementa l'interfaccia `ExtendedRecipeBookCategory` (o è un enum `RecipeBookCategory` vanilla), ma **NON** è un'istanza di `SearchRecipeBookCategory`. Il cast diretto nel log di debug genera una `ClassCastException` che interrompe l'esecuzione dell'handler di input.
- **Strategia Correttiva per la Nuova Sessione**:
  1. Rimuovere il cast forzato `(SearchRecipeBookCategory) category`.
  2. Sostituire con `category != null ? category.toString() : "null"` o con la lettura sicura del nome/identificatore della categoria.
  3. Aggiungere una guardia difensiva per prevenire qualsiasi crash analogo durante il cambio tab.

---

### 🟡 Anomalia 2 — Avviso GUI Mancante per Singleton `Config.instance`

- **Priorità**: Media (Pulizia log & conformità AutoConfig Cloth Config)
- **Occorrenze nei Log**: Ore 18:44:29, 19:05:38
- **Messaggio di Log**:
  ```text
  [18:44:29] [Render thread/ERROR]: No GUI provider registered for field 'private static org.mcaccess.minecraftaccess.Config org.mcaccess.minecraftaccess.Config.instance'!
  ```
- **File Sorgente**: `src/main/java/org/mcaccess/minecraftaccess/Config.java`
- **Righe Coinvolte**: 23-25
- **Codice Attuale**:
  ```java
  @me.shedaniel.autoconfig.annotation.Config(name = "minecraft-access")
  public final class Config implements ConfigData {
      @Getter
      private static Config instance;
  ```
- **Causa Radice**:
  La libreria AutoConfig (Cloth Config) analizza per riflessione tutti i campi dichiarati nella classe `@Config`. Poiché il campo singleton statico `instance` non è marcato con `@ConfigEntry.Gui.Excluded`, Cloth Config cerca di istanziare un widget di editing grafico per l'oggetto `Config` stesso, fallendo e loggando un errore.
- **Strategia Correttiva per la Nuova Sessione**:
  1. Aggiungere l'annotazione `@ConfigEntry.Gui.Excluded` sopra la dichiarazione di `private static Config instance;` in `Config.java`.

---

### 🔵 Anomalia 3 — Vocalizzazione Chiavi Grezze per Blocchi di Mod Terze (Macaw's Doors)

- **Priorità**: Bassa / Qualità dell'Accessibilità
- **Occorrenze nei Log**: Ore 19:46:55, 19:47:15
- **Messaggio Narrato**:
  ```text
  [19:46:55] [Render thread/INFO]: Narrating(interrupt:false)= block.mcwdoors.dark_oak_barn_door     
  [19:47:15] [Render thread/INFO]: Narrating(interrupt:true)= Davanti: Ostacolo di block.mcwdoors.dark_oak_barn_door a 6 blocchi.
  ```
- **Causa Radice**:
  La mod esterna `mcwdoors` (Macaw's Doors) non include la traduzione italiana (`it_it.json`) per il blocco `block.mcwdoors.dark_oak_barn_door`. Quando `getName().getString()` o il sistema di localizzazione vanilla non trova la stringa nella lingua attiva, restituisce la chiave non tradotta.
- **Strategia Correttiva per la Nuova Sessione**:
  1. Valutare l'introduzione di un fallback resiliente in `ObstacleDetectionUtils.java` / `WorldNarrator`: se una chiave di blocco inizia con `block.` o non ha traduzione italiana, tentare il recupero della traduzione inglese (`en_us`) o formattare il nome leggibile dall'identificatore (es. *"dark oak barn door"*).
  2. Opzionalmente, includere override di dizionario per le mod del modpack ufficiale in `minecraft_access/lang/it_it.json`.

---

## 3. Esiti del Collaudo In-Game (Sessione 02/09/2026)

- **ClassCastException Ricettario (`V` / `Shift+V`)**: ✅ **Risolto e Collaudato**. Il cambio scheda non genera più eccezioni e l'input risponde fluidamente.
- **Provider GUI Singleton (`Config.instance`)**: ✅ **Risolto e Collaudato**. Nessun warning o errore nei log di avvio e configurazione.
- **Diagnostica Esperienziale emersa dal test**: Sebbene il crash sia risolto, l'azione di cambio scheda e cambio pagina nel ricettario rimane silenziosa per l'utente non vedente, richiedendo un arricchimento strutturato del feedback acustico e vocale.

---

## 4. Registro Revisioni Future & Roadmap Evolutiva GUI

### 🟢 Revisione 26.1 — Feedback Vocale & Auto-Focus al Cambio Categoria Ricettario (`V` / `Shift+V`)
- **Problema**: Premendo `V` o `Shift+V`, il cambio scheda nel ricettario non emette alcun suono né vocalizzazione del nome della categoria. Se il cursore si trova altrove (es. inventario del giocatore o crafting input), il focus non si sposta, lasciando l'utente disorientato sul contesto attivo.
- **Soluzione da Implementare**:
  1. Suono di interazione `SoundEvents.UI_BUTTON_CLICK` alla pressione di `V` / `Shift+V`.
  2. Risoluzione del nome localizzato della categoria (es. *"Costruzione"*, *"Attrezzatura"*, *"Varie"*, *"Pietrarossa"*).
  3. Selezione automatica del gruppo `recipes` e posizionamento del cursore sulla prima ricetta disponibile.
  4. Vocalizzazione atomica coordinata: *"Categoria: [Nome]. Realizzabile [X] [Nome Ricetta]"* (oppure *"Nessuna ricetta disponibile"*).

### 🟢 Revisione 26.2 — Feedback Vocale & Auto-Focus al Cambio Pagina Ricettario (`Shift+I` / `Shift+K`)
- **Problema**: Premendo `Shift+I` (Pagina Precedente) o `Shift+K` (Pagina Successiva), la pagina gira visivamente ma non vocalizza il numero di pagina né riposiziona il cursore sul primo elemento della nuova pagina.
- **Soluzione da Implementare**:
  1. Estrazione di `currentPage` e `totalPages` tramite accessors Mixin in `RecipeBookPageAccessor`.
  2. Suono di click `UI_BUTTON_CLICK` e aggiornamento della griglia slot.
  3. Spostamento del cursore sulla prima ricetta della nuova pagina.
  4. Vocalizzazione atomica: *"Pagina [X] di [Y]. Realizzabile [Z] [Nome Ricetta]"*.

### 🟢 Revisione 26.3 — Navigazione Universale a Slot con le 4 Frecce Direzionali
- **Problema**: La navigazione negli inventari e container è storicamente affidata ai tasti `I, K, J, L`. L'uso intuitivo delle 4 Frecce Direzionali (`Freccia Su, Giù, Sinistra, Destra`) deve essere supportato nativamente su ogni interfaccia aperta.
- **Soluzione da Implementare**:
  1. Mappatura delle 4 Frecce Direzionali (`KEY_UP`, `KEY_DOWN`, `KEY_LEFT`, `KEY_RIGHT`) come input universali di navigazione slot in `InventoryControls`.
  2. Garantire la piena compatibilità con tutte le finestre derivate da `AbstractContainerScreen` (inventario personale, casse, forni, alambicchi, banchi, villici/NPC, ecc.).
  3. Mantenimento del disaccoppiamento con le caselle di testo (`EditBox` con `T` / `Invio`): quando la casella di testo è a fuoco le frecce muovono il cursore di testo; quando non è a fuoco le frecce navigano gli slot.

### 🟢 Revisione 26.4 — Feedback Eventi Visivi su Interfacce Specialistiche
- **Schermate Candidate**:
  - **Tagliapietre (`StonecutterScreen`)**: Annuncio numero tagli e focus prima opzione all'inserimento del blocco.
  - **Telaio (`LoomScreen`)**: Annuncio motivi disponibili all'inserimento di stendardo e tintura.
  - **Fornace / Alambicco**: Notifiche vocali discrete sul completamento ciclo/cottura.