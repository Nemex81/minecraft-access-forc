# Piano di Implementazione - Sistema Waypoint & Punti di Interesse per Minecraft Access

Questo documento definisce l'architettura tecnica dettagliata per l'introduzione del sistema di **Waypoint e Punti di Interesse Personalizzati a Lungo Raggio** nella mod *Minecraft Access*. Il sistema è concepito specificamente per garantire orientamento, autonomia e tracciamento a qualsiasi distanza per giocatori non vedenti, integrandosi con l'ecosistema esistente di `ObjectTracker` e `LockingHandler`.

---

## 1. Obiettivi e Requisiti Chiave

1. **Tracciamento a Lungo Raggio Indipendente dai Chunk**:
   * Memorizzazione basata su coordinate matematiche assolute ($X, Y, Z$, Dimensione).
   * Funzionamento su distanze illimitate (anche tra continenti e dimensioni differenti).
2. **Integrazione Totale con le Categorie di `ObjectTracker`**:
   * Categoria dedicata navigabile con <kbd>Ctrl + Pagina Su / Giù</kbd>.
   * Scorrimento dei waypoint con <kbd>Pagina Su / Giù</kbd>.
3. **Punti di Interesse Speciali Automatici**:
   * *Ultima Morte*: salvataggio automatico del punto e della dimensione alla morte del giocatore.
   * *Letto / Punto di Rinascita*: salvataggio automatico quando il giocatore dorme o imposta lo spawn.
4. **Comandi di Orientamento & Lettura Universali** (per **TUTTI** i target: Blocchi, Mob, Waypoint):
   * <kbd>Home</kbd>: Lettura nome, dimensione, distanza e direzione relativa.
   * <kbd>Alt + Home</kbd>: Lettura coordinate assolute $X, Y, Z$.
   * <kbd>Shift + Home</kbd>: Allineamento manuale istantaneo della visuale (Look-At One-Shot).
   * <kbd>Y</kbd>: Aggancio continuo della testa (Lock-On).
   * <kbd>Alt + Y</kbd>: Sblocco visuale (Unlock).
5. **Salvataggio Accessibile con Dialog Box**:
   * <kbd>Alt + D</kbd>: Apertura schermata `SaveWaypointScreen` con campo di testo `EditBox` accessibile e focus immediato.
6. **Conversione Coordinate tra Dimensioni (Overworld $\leftrightarrow$ Nether)**:
   * Calcolo automatico della distanza e direzione relativa equivalente nel Nether o Overworld (fattore 1:8).
7. **Opzioni di Configurazione Complete**:
   * Nuova sezione `Waypoints` in `Config.java` (interruttori, volumi e intervalli personalizzabili).

---

## 2. Modifiche ai File e Nuovi Componenti

### Modello Dati e Persistenza (`org.mcaccess.minecraftaccess.features.point_of_interest.waypoints`)

* **[NEW] `Waypoint.java`**:
  * Struttura dati (Record):
    * `String id`: Identificativo univoco (UUID).
    * `String name`: Nome visualizzato/pronunciato.
    * `BlockPos pos`: Coordinate del punto.
    * `Identifier dimension`: Dimensione (`minecraft:overworld`, `minecraft:the_nether`, `minecraft:the_end`).
    * `WaypointType type`: Tipo (`CUSTOM`, `DEATH`, `BED`).
    * `long timestamp`: Data di creazione.
* **[NEW] `WaypointType.java`**:
  * Enum: `CUSTOM`, `DEATH`, `BED`.
* **[NEW] `WaypointManager.java`**:
  * Gestione salvataggio/caricamento su file JSON in `.minecraft/minecraft-access/waypoints/<world_or_server_id>.json`.
  * Rilevamento automatico evento morte e interazione letto/spawn.
  * Metodi: `addWaypoint`, `removeWaypoint`, `updateWaypoint`, `getWaypoints`, `getDeathWaypoint`, `getBedWaypoint`.

---

### Integrazione POI & Scanner (`org.mcaccess.minecraftaccess.features.point_of_interest`)

* **[NEW] `POIWaypoints.java`**:
  * Gestisce il `POIGroup<Waypoint>` per `ObjectTracker`.
  * Emette il suono periodico direzionale (audio beacon) verso il waypoint selezionato.
* **[MODIFY] `POIManager.java`**:
  * Istanziazione di `WaypointManager` e `POIWaypoints`.
* **[MODIFY] `ObjectTracker.java`**:
  * Inclusione di `POIWaypoints.group` nell'elenco categorie `getPOIGroups()`.
  * Gestione del tipo `Waypoint` in `isObjectValid()` e `narrateCurrentObject()`.
  * Registrazione dei nuovi comandi:
    * <kbd>Shift + Home</kbd> $\rightarrow$ `lookAtCurrentObject()` (supporta `BlockPos`, `Entity`, `Waypoint`).
    * <kbd>Alt + Home</kbd> $\rightarrow$ `narrateCoordinatesOfCurrentObject()` (supporta `BlockPos`, `Entity`, `Waypoint`).
* **[MODIFY] `LockingHandler.java`**:
  * Supporto per aggancio a un oggetto di tipo `Waypoint` con il tasto <kbd>Y</kbd>.

---

### Interfaccia Utente & Accessibilità (`org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.gui`)

* **[NEW] `SaveWaypointScreen.java`**:
  * Schermata accessibile per salvare un waypoint con nome.
  * Contiene un `EditBox` focalizzato automaticamente all'apertura con nome suggerito precompilato.
  * Supporta conferma con <kbd>Invio</kbd> e annullamento con <kbd>Esc</kbd>.
* **[NEW] `ManageWaypointsScreen.java`**:
  * Schermata con elenco dei waypoint per visualizzazione, eliminazione o rinomina.

---

### Integrazione Access Menu (`org.mcaccess.minecraftaccess.addon.accessmenu`)

* **[NEW] `SaveWaypoint.java`**: Funzione per salvare waypoint tramite menu <kbd>F4</kbd>.
* **[NEW] `ManageWaypoints.java`**: Funzione per aprire il gestore waypoint da menu <kbd>F4</kbd>.

---

### Configurazione & Localizzazione

* **[MODIFY] `Config.java`**:
  * Aggiunta della classe `Waypoints`:
    * `boolean enabled = true;`
    * `boolean autoSaveDeathPoint = true;`
    * `boolean autoSaveBedPoint = true;`
    * `boolean playAudioBeacon = true;`
    * `int beaconInterval = 2500;`
    * `float beaconVolume = 0.35f;`
    * `boolean crossDimensionConversion = true;`
* **[MODIFY] `src/main/resources/assets/minecraft_access/lang/it_it.json` & `en_us.json`**:
  * Stringhe di traduzione per categorie, messaggi vocali, intestazioni GUI e descrizioni tasti.

---

## 3. Piano di Verifica e Collaudo

1. **Test di Navigazione Scanner Universale**:
   * Scorrere con <kbd>Ctrl + Pagina Su/Giù</kbd> e verificare il passaggio fluido tra categorie tradizionali e Waypoint.
   * Premere <kbd>Shift + Home</kbd> su blocchi, mob e waypoint verificando l'allineamento istantaneo dello sguardo.
   * Premere <kbd>Alt + Home</kbd> su blocchi, mob e waypoint verificando la lettura corretta delle coordinate $X, Y, Z$.
2. **Test di Salvataggio ed Accessibilità Dialog (<kbd>Alt + D</kbd>)**:
   * Premere <kbd>Alt + D</kbd>, verificare l'apertura immediata della schermata con focus nell'`EditBox` e annuncio vocale.
   * Digitare un nome personalizzato, premere <kbd>Invio</kbd> e verificare la conferma vocale e la presenza del punto nella lista.
3. **Test Funzioni Automatiche (Morte e Letto)**:
   * Simulare la morte del giocatore e verificare la generazione automatica del punto *"Ultima Morte"*.
   * Dormire in un letto e verificare la registrazione del punto *"Letto"*.
4. **Test Distanza e Dimensioni (Overworld $\leftrightarrow$ Nether)**:
   * Creare un waypoint nell'Overworld, entrare nel Nether e verificare il calcolo esatto delle coordinate 1:8 e della direzione/distanza relativa.
5. **Test Lock-On (<kbd>Y</kbd> e <kbd>Alt + Y</kbd>)**:
   * Selezionare un waypoint e premere <kbd>Y</kbd>, camminare e verificare il tracciamento continuo della testa.
   * Premere <kbd>Alt + Y</kbd> per verificare il corretto sblocco.
