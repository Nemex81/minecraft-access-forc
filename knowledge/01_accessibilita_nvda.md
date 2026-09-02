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