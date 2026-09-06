# 01 — Accessibilità Vocale, Screen Reader & Comandi da Tastiera

## 1. Visione e Filosofia Fondante

Questo progetto ha lo scopo di rendere **Minecraft completamente accessibile ed autonomo al 100%** per **Luca**, sviluppatore e giocatore totalmente non vedente.
Ogni feature, interfaccia, interazione e dinamica di gioco deve rispettare i seguenti principi inderogabili:

1. **Zero Dipendenza dal Mouse o da Indicatori Visivi**: Nessuna operazione deve richiedere puntamento visivo del cursore del mouse, trascinamenti (drag & drop) o riconoscimento ottico/cromatico.
2. **Accessibilità Vocale Completa (Screen Reader Proxy)**: Tutto ciò che accade a schermo — blocchi mirati, entità nel raggio, interfacce grafiche, inventari, fornaci, ricettari, chat e notifiche — deve essere convertito in stringhe semantiche e inviato al proxy vocale.
3. **Audio Posizionale 3D e Segnali Sonori**: Utilizzo di pitch, pan e riverbero per conferire profondità spaziale, localizzazione dei pericoli (creeper, burroni, lava), coordinate di navigazione e conferme acustiche di posizionamento/estrazione blocchi.
4. **Navigazione Logica e Sequenziale da Tastiera**: Qualsiasi interfaccia a griglia (inventari, forzieri, tavoli da lavoro) o a schede deve essere esplorabile tramite tasti direzionali, raggruppamenti concettuali e scorciatoie dirette.

---

## 2. Architettura del Proxy Screen Reader (`screen_reader/`)

Il package `org.mcaccess.minecraftaccess.screen_reader` funge da ponte verso la sintesi vocale del sistema operativo (NVDA, Tolk, SAPI):

- **Inoltro Vocale**: Le stringhe vengono generate dinamicamente e inviate tramite la chiamata master:
  ```java
  MainClass.narrate(Component text, boolean interrupt);
  ```
- **Priorità e Interruzione (`interrupt`)**:
  - `interrupt = true`: Notifiche ad alta priorità (danno subito, pericoli immediati, cambi di schermata drastici, conferme esplicite di comandi).
  - `interrupt = false`: Lettura sequenziale (scorrimento elementi inventario, chat in sottofondo, dettagli descrittivi).
- **Semantica e Brevità**: I testi per la sintesi vocale devono essere chiari, privi di acronimi oscuri e strutturati in modo che le informazioni critiche (nome oggetto, quantità) vengano lette per prime.

---

## 3. Standard di Navigazione Interfacce e Inventari

La navigazione degli inventari (`features.inventory_controls`) non usa il puntamento coordinate dello schermo, ma un modello a **Gruppi Logici e Celle**:

- **Gruppi Logici (`GroupGenerator.java`)**:
  - Gli elementi dell'interfaccia sono suddivisi in gruppi logici (es. *Barra Rapida*, *Inventario Principale*, *Equipaggiamento/Armatura*, *Slot di Creazione/Crafting*, *Ricette*, *Risultato*, *Input/Combustibile Fornace*).
  - Tasto **`C`**: Scorrimento in avanti tra i gruppi logici.
  - Tasto **`Shift + C`**: Scorrimento all'indietro tra i gruppi logici.
- **Navigazione a Griglia Interna**:
  - Tasti **`I` (Su)**, **`K` (Giù)**, **`J` (Sinistra)**, **`L` (Destra)**: Spostamento preciso tra le celle/slot del gruppo attivo.
- **Azioni su Slot e Oggetti**:
  - Tasto **`È`**: Selezione, presa o deposito dello stack di oggetti sotto il cursore.
  - Tasto **`Shift + È`**: Trasferimento rapido (*Quick Move*) dello stack verso l'inventario o il contenitore collegato.
  - Tasto **`U`**: Lettura stato avanzamento/cottura e carburante (fornace, affumicatore, supporto pozioni).
  - Tasto **`X`**: Lettura prerequisiti ed ingredienti della ricetta selezionata (con bypass focus via `.ignoreScreenFocus()`).
  - Tasto **`V` / `Shift + V`**: Scorrimento schede del ricettario.

### 3.1 Feedback Atomico GUI, Statistiche di Pagina e Navigazione a Frecce (Rev 26.1 - 26.6)

In tutte le schermate a griglia, inventari e ricettari:
1. **Feedback Acustico Istantaneo**: Ogni pressione di tasti per cambio scheda, categoria, pagina o filtro (`V`, `Shift+V`, `Shift+I`, `Shift+K`, `R`) deve riprodurre immediatamente il suono di click di interfaccia (`SoundEvents.UI_BUTTON_CLICK`).
2. **Modello di Narrazione a 3 Livelli Atomici**:
   - *Livello 1 (Contesto/Pagina/Filtro)*: Categoria attiva, numero pagina o stato filtro (es. *"Categoria: Meccanismi e Redstone"*, *"Pagina 2 di 6"*, *"Mostra solo realizzabili"*).
   - *Livello 2 (Statistiche Aggregate di Pagina)*: Conteggio dinamico con concordanza singolare/plurale (`RecipePageStats`):
     - Miste: `"[T] ricette: [R] realizzabili, [N] non realizzabili"` (es. *"15 ricette: 1 realizzabile, 14 non realizzabili"*)
     - Tutte realizzabili: `"[T] ricette realizzabili"` (o *"1 ricetta realizzabile"*)
     - Nessuna realizzabile: `"[T] ricette non realizzabili"` (o *"1 ricetta non realizzabile"*)
   - *Livello 3 (Dettaglio Elemento Puntato)*: Stato di realizzabilità, quantità e nome dell'oggetto (es. *"Realizzabile 1 Ascia di legno"*).
3. **Gestione Semantica dei Limiti di Pagina (Boundary Gating)**:
   - Se l'utente preme `Shift+I` a pagina 1: enuncia *"Prima pagina, [Stats]. [Primo Elemento]"* senza inviare click hardware a vuoto.
   - Se l'utente preme `Shift+K` all'ultima pagina: enuncia *"Ultima pagina, [Stats]. [Primo Elemento]"*.
   - Se la categoria ha 1 sola pagina: enuncia *"Unica pagina, [Stats]. [Primo Elemento]"*.
   - In tutti i casi di limite, il cursore viene riposizionato con fermezza sul primo elemento disponibile leggendone i dati.
4. **Risoluzione Registro Categorie in Minecraft 26.2**:
   - Evitare `category.toString()` che restituisce il puntatore memoria Java `Classe@hashcode`.
   - Utilizzare l'estrazione da registro `BuiltInRegistries.RECIPE_BOOK_CATEGORY.getKey(recipeCat)` e gestire `SearchRecipeBookCategory` per i tab globali.
5. **Navigazione Universale a Griglia con 4 Frecce Direzionali (Dual-Binding)**:
   - Gli slot di tutte le interfacce (`AbstractContainerScreen`) sono navigabili sia con i tasti storici **`I, K, J, L`**, sia con le **`4 Frecce Direzionali` (Su, Giù, Sinistra, Destra)**.
   - Quando una casella di testo (`EditBox`) è attiva con `T`, le frecce muovono il cursore di testo; premendo `Invio`, le frecce tornano istantaneamente a navigare gli slot.

---

## 4. Controlli della Visuale e Navigazione nel Mondo

Il modulo `features.camera_controls` consente di orientare lo sguardo del giocatore tramite tastiera con incrementi angolari precisi (Yaw e Pitch), annunciando via sintesi vocale i punti cardinali (Nord, Sud, Est, Ovest) e l'inclinazione (Orizzontale, Verso l'alto, Verso il basso).

---

## 5. Principio di Simmetria Universale & Coesistenza Visiva/Acustica

In conformità al nostro standard architetturale:
- **Doppio Canale Parallelo**:
  * *Canale Acustico & Vocale (Non Vedenti)*: Screen Reader Proxy (`MainClass.narrate`), suoni 3D posizionali e navigazione a gruppi logici (Zero Mouse).
  * *Canale Grafico & Visivo (Normovedenti)*: Tutte le modifiche grafiche, HUD, rendering di blocchi, inventari e menu devono preservare la qualità visiva, le texture, le animazioni e l'interazione nativa con il mouse per giocatori vedenti.
- **Ruolo di Antigravity come Copilota Visivo**:
  * Durante lo sviluppo, Antigravity supervisiona proattivamente che i menu di configurazione, i messaggi a schermo e gli overlay grafici siano posizionati con proporzioni corrette, spaziature armoniche e contrasti cromatici conformi agli standard WCAG.
- **Zero Conflitti**: La sintesi vocale e i controlli accessibili non alterano la fedeltà grafica del gioco, e la grafica non oscura né tronca la voce di NVDA.

---

## 6. Propriocezione Tattile-Acustica del Personaggio & Regolazione Passi

- **Importanza Cognitiva**: In assenza di vista, il suono dei passi costituisce la prima ancora di propriocezione in tempo reale: conferma che il movimento sta avvenendo (prevenendo la corsa a vuoto contro ostacoli), scandisce il ritmo della marcia e fornisce l'identificazione immediata del materiale calpestato (legno, pietra, terra, ghiaia, sabbia, lana).
- **Doppio Canale di Regolazione**:
  - *Configurativo*: Cursore percentuale da `0%` a `300%` (default `100%`) in `Config.java` (`playerStepSoundVolume`).
  - *On-The-Fly (Zero Sneak)*: Tasti rapidi **`Alt + Page Up`** (+10%) e **`Alt + Page Down`** (-10%) con vocalizzazione istantanea del livello e salvataggio automatico persistente.

---

## 7. Auto-Focus Immediato su Menu di Gioco (`PauseScreen`) & Disaccoppiamento Mouse

1. **Il Problema dell'Assenza di Focus Iniziale all'Apertura dei Menu**:
   - In Minecraft vanilla, premendo `Esc` in partita per aprire `PauseScreen`, il sistema non assegna automaticamente il focus della tastiera al primo pulsante se il cursore del mouse era posizionato altrove.
   - Questo costringeva il giocatore non vedente a premere `Tab` prima di poter iniziare a scorrere le voci del menu con le 4 frecce direzionali.
2. **La Soluzione Integrata in `MenuFix.java`**:
   - **Inclusione `PauseScreen.class`**: Il menu di pausa è registrato nel set `MENUS_NEED_FIX`.
   - **Riposizionamento Mouse a Coordinate di Sicurezza**: Il cursore del mouse viene istantaneamente spostato a $(10, 10)$ per non interferire visivamente o acusticamente con gli hover.
   - **Iniezione Auto-Focus Logico (`ensureInitialFocus`)**: Se `screen.getFocused() == null`, il focus viene immediatamente agganciato al primo `AbstractWidget` attivo ("Torna al gioco").
   - **Accessibilità Istantanea**: NVDA vocalizza all'istante il primo pulsante e le frecce Su/Giù e Sinistra/Destra sono immediatamente attive al primo tocco senza dover mai premere `Tab`.

---

## 8. Onestà Percettiva delle Impostazioni Cloth Config & Divieto di Controlli Decorativi

1. **Il Canone dell'Onestà Percettiva**:
   - Per un utente vedente, un'opzione grigia o decorativa può essere interpretata visivamente come non implementata; per un utente non vedente che naviga con lo screen reader NVDA, ogni controllo focalizzabile viene annunciato con pari dignità e autorevolezza (nome, stato, valore).
   - L'esposizione in Cloth Config di controlli prematuri (es. densità vocale o ducking audio prima che il codice li supporti) genera false aspettative e confusione sensoriale, spingendo il giocatore a chiedersi perché la modifica di un parametro non produca alcun effetto in-game.
2. **Standard Operativo Vincolante**:
   - **Zero Opzioni Decorative**: Una nuova categoria o opzione Cloth Config può essere esposta all'utente **esclusivamente se** il motore logico sottostante è già in grado di interpretarla e produrre un effetto misurabile a runtime.
   - **Rinvio Trasparente**: Le opzioni pianificate per fasi future (es. `ambientSpeechDensity` o `criticalModAudioDucking`) rimangono confinate nel design document e vengono inserite in `Config.java` e nelle traduzioni I18N solo contestualmente all'attivazione del loro codice reale.

---

## 9. Arbitraggio di Narrazione Concorrente & Pattern "Silent Commit" in Movimento

1. **Il Problema dell'Annuncio Posticipato Fuori Tempo (Lag Mutation Alert)**:
   - Quando due sottosistemi di feedback automatico operano in contemporanea (es. `ObstacleDetector` ad alta priorità e `CrosshairFeedbackManager` a monitoraggio continuo del blocco puntato), la soppressione temporanea del canale secondario tramite semplice `return` o mute causa una deriva di stato.
   - Durante il cammino verso un ostacolo, il mirino continua a campionare blocchi diversi; se la voce viene soppressa per $100\text{ ms}$ senza aggiornare i campi di memoria, al primo tick utile dopo la riattivazione il modulo rileva la discrepanza tra lo stato precedente (vecchio blocco) e l'attuale, interpretandola erroneamente come una nuova mutazione fresca e annunciando un blocco già superato a fermata avvenuta.
2. **Il Pattern "Silent Commit" (`absorbAutomaticMovementFeedbackIfSuppressed`)**:
   - Se il canale secondario si trova all'interno della finestra di soppressione e il giocatore è in movimento attivo, il modulo **non deve solo tacere**: deve aggiornare internamente lo stato corrente (`currentTarget`, `currentNarration`, `currentDistance`) prima di scartare l'annuncio vocale.
   - In questo modo, alla scadenza della soppressione non esiste alcun differenziale di stato obsoleto: la voce resta pulita e interviene solo se si verifica una reale nuova variazione successiva.
3. **Finestra di Soppressione Monotona**:
   - L'estensione della soppressione temporale deve essere monotona crescente:
     $$\text{suppressedUntil} = \max(\text{suppressedUntil}, \text{clock} + \text{duration})$$
     impedendo che chiamate concorrenti ravvicinate possano inavvertitamente abbreviare o resettare una finestra di silenzio ancora attiva.
4. **Bypass Assoluto per Comandi Espliciti**:
   - I comandi espliciti da tastiera dell'utente (`Alt+V` per l'orientamento, tasto `B` per il mirino manuale) ignorano totalmente le finestre di soppressione automatica, garantendo latenza $0\text{ ms}$ e risposta reattiva immediata.

---

## 10. Architettura a Doppio Guard e Protezione Ciclo di Vita nelle GUI

Durante la navigazione delle interfacce grafiche (inventari, forzieri, tavoli di lavoro) e nelle transizioni rapide di apertura/chiusura:

1. **Il Pericolo delle Ghost Narrations e Dereferenziazioni Asincrone**:
   - Se l'utente chiude rapidamente una schermata con `Esc` o se si verifica una transizione mentre viene premuta una scorciatoia da tastiera (es. tasti Kuma per navigazione griglia o `Shift`), l'evento di input può raggiungere i gestori di navigazione quando l'interfaccia non è più attiva o `currentScreen` è già stato posto a `null`.
   - Ciò scatena crash per `NullPointerException` (es. accessor di posizione slot) e/o narrazioni residue di oggetti non più a schermo (*ghost narrations*).
2. **Lo Standard del Doppio Guard**:
   - *Guard a Monte (Routing & Handlers)*: Ogni metodo di navigazione, cambio gruppo, focus o handler tasti deve verificare preventivamente:
     ```java
     if (!isActiveContainerScreen()) return;
     ```
     dove `isActiveContainerScreen()` controlla sia il tipo di schermata (`AbstractContainerScreen`), sia l'identità dell'istanza attiva rispetto a quella referenziata dal controller (`activeScreen == currentScreen`).
   - *Guard a Valle (Esecuzione Fisica)*: I metodi che comandano fisicamente il puntatore o la selezione (`moveToSlotItem`) devono contenere una guardia difensiva indipendente:
     ```java
     if (slotItem == null || !isActiveContainerScreen()) return;
     ```
3. **Sincronizzazione Atomica nel `tick()` di Lifecycle**:
   - Il metodo `tick()` del gestore interfacce deve monitorare lo stato dello schermo ad ogni ciclo. Se lo schermo non è valido o è cambiato, deve invocare immediatamente `clearNavigationState()` **prima ancora** di verificare o aggiornare i timer o i debouncer dell'intervallo.
4. **Inviolabilità dell'Input Manuale nei Menu**:
   - L'uso di tasti modificatori (`Shift`, `Ctrl`, `Alt`) all'interno delle schermate non deve mai propagarsi come comando di movimento o postura nel mondo di gioco (es. divieto assoluto di sneak sintetico o suoni di pala).