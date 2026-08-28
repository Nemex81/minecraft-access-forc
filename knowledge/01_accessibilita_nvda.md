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

---

## 4. Controlli della Visuale e Navigazione nel Mondo

Il modulo `features.camera_controls` consente di orientare lo sguardo del giocatore tramite tastiera con incrementi angolari precisi (Yaw e Pitch), annunciando via sintesi vocale i punti cardinali (Nord, Sud, Est, Ovest) e l'inclinazione (Orizzontale, Verso l'alto, Verso il basso).
