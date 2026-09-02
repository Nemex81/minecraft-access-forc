# Diario Modifiche Personali & Contributi Fork (AVF)
# Autore: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity
# Progetto: Minecraft Access (Fork di nemex81 / Luca)
# Ambito: Cronologia Feature Personali & Disciplina I18N
# Riferimento Standard: Astralis Versioning Framework (AVF 3+1 Livelli)
# Baseline di Partenza: v26.2-1.12.0 (Fork Inception a inizio Agosto 2026)

Questo documento costituisce il **Diario Ufficiale delle Modifiche del Fork Personale in lingua Italiana**.
Poiché il `README.md` pubblico e la documentazione del repository upstream rimangono in lingua Inglese per la community internazionale con la sola sezione `## [Unreleased]`, tutte le novità, i refactoring e i miglioramenti sviluppati sui nostri rami (`mymaster`, `dev`) vengono tracciati qui secondo la disciplina AVF (`V.A.R[.M]`).

## 🚀 [v26.2-1.17.0] — 2026-09-02 (Feed Mirino WASD, Riqualificazione Tasto B, Alt+B & Mentor Adattivo — Versione Attuale)

### 🌟 Mirino, Movimento & Tasti Rapidi
- **Feed Mirino in Movimento (`WASD`)**:
  - Tracciamento automatico dei blocchi sui passi laterali (`A`/`D`) e della distanza sui passi frontali (`W`/`S`) in `NarrateCrosshair.java`.
  - Nuove modalità in Cloth Config (`MovementFeedbackMode`): `TARGET_AND_DISTANCE` (default), `TARGET_ONLY`, `FULL_FORMAT` e `OFF`.
- **Riqualificazione Tasto `B` (Mano Sinistra)**:
  - Comando rapido a 1 tocco per la lettura istantanea e completa del mirino integrato in `CrosshairFeedbackManager.java` con supporto al vuoto (*"Nessun bersaglio nel mirino"*).
- **Blindatura Tracciatore Risorse (`Alt + B`)**:
  - Separazione netta dei modificatori in Kuma/GLFW per garantire piena parità di funzionamento con `Alt + Numpad 7`.
- **Armonizzazione con `ObstacleDetector`**:
  - Finestra di grazia preventiva (250ms) per azzerare troncamenti vocali in cammino.

### 🧠 Mentor Vocale Adattivo (Rev MC-27.1)
- **Direzione Spaziale Dinamica**: Riconoscimento dell'asse reale WASD di collisione (*"a sinistra"*, *"a destra"*, *"davanti"*, *"dietro"*).
- **Keybinding Introspection**: Risoluzione dinamica dei tasti a runtime da Minecraft/Kuma per Salto (*"Spazio"*) e Ispezione Ostacolo (*"Alt + V"*).
- **Eliminazione Prefisso Spurio**: Risoluzione diretta `I18n.get(key, args)` senza artefatti di formato.

---

## 🚀 [v26.2-1.16.1] — 2026-09-02 (Feedback Eventi Visivi & Auto-Focus Schermate Specialistiche)

### 🌟 Accessibilità & Interfacce Specialistiche (GUI)
- **Tagliapietre (`StonecutterScreen`)**:
  - Vocalizzazione dinamica delle forme disponibili (*"%d forme disponibili per il taglio"*).
  - Auto-focus automatico sul primo elemento tagliabile (`recipesGroup`) all'inserimento del blocco.
- **Telaio (`LoomScreen`)**:
  - Tracciamento e annuncio motivi disponibili (*"%d motivi disponibili per lo stendardo"*) all'inserimento di stendardo e colorante.
  - Selezione automatica del gruppo motivi per la navigazione immediata.
- **Fornaci & Alambicco (`AbstractFurnaceMenu`, `BrewingStandMenu`)**:
  - Notifiche vocali discrete al completamento del ciclo di cottura (*"Cottura completata"*) e distillazione (*"Distillazione completata"*).
- **Nuovo Helper di Navigazione**: Metodo `selectGroupByKey(groupKey, interrupt)` in `InventoryControls.java` per l'aggiornamento e l'atterraggio istantaneo sul gruppo richiesto.

---

## 🚀 [v26.2-1.16.0] — 2026-09-02 (Avanzamento GUI Ricettario 26.2, 4 Frecce & Stabilità)

### 🌟 Accessibilità & Interfacce (GUI)
- **Navigazione Ricettario Avanzata (`InventoryControls.java`)**:
  - *Cambio Categoria (`V` / `Shift+V`)*: Suono click, risoluzione dinamica nomi in italiano (*"Costruzione"*, *"Attrezzatura"*, *"Varie"*, *"Meccanismi e Redstone"*), auto-focus sulla prima ricetta e annuncio statistiche aggregate.
  - *Cambio Pagina (`Shift+I` / `Shift+K`)*: Accessor Mixin `RecipeBookPageAccessor`, conteggio dinamico pagine e gestione intelligente dei limiti (*"Prima pagina"*, *"Ultima pagina"*, *"Unica pagina"*).
  - *Concordanza Singolare/Plurale*: Flessione grammaticale dinamica in italiano e inglese per conteggi unitari ($1$) e multipli ($>1$).
  - *Navigazione Griglia a 4 Frecce*: Supporto nativo per le 4 Frecce Direzionali su tutti i container con disaccoppiamento automatico dalle caselle di testo `EditBox`.

### 🐞 Bugfix & Stabilità
- Risolta `ClassCastException` al cambio scheda nel ricettario (`Rev MC-26.0A`).
- Risolto warning Cloth Config con annotazione `@ConfigEntry.Gui.Excluded` su singleton `Config.instance` (`Rev MC-26.0B`).
- Guardie difensive anti-NPE su gruppi slot inventario.

---

## 🚀 [v26.2-1.15.0] — 2026-08-28 (Sonda Direzionale, Survival Tracker, Sentinella Minacce & Occlusione 5 Livelli)

### 🌟 Movimento, Suono & Consapevolezza Tattica
- **Sonda Direzionale di Percorso (`DirectionalPathScanner`)**: Scansione progressiva a passo singolo con rilevamento automatico di terreni zappati (`farmland`) e stadi di crescita delle colture (Layer 3 Numpad).
- **Survival Resource Tracker (`Alt+B` / `Alt+Numpad 7`)**: Conteggio acustico istantaneo delle scorte vitali (cibo, attrezzi, blocchi solidi, frecce) con isolamento dei modificatori.
- **Sentinella Minacce Ostili & Sicurezza Fluidi**: Protezione del `FallDetector` su margini fluidi (lava/acqua), sentinella proattiva mob ostili e navigazione intelligente delle porte.
- **Occlusione Sonora a 5 Livelli**: Smorzamento acustico raycast attraverso materiali (Legno, Pietra, Vetro, Metallo, Fogliame) e vocalizzazione bersagli "dietro la parete".
- **Footstep Proprioception**: Regolazione del volume dei passi del giocatore locale con tasti rapidi `Alt+PageUp` / `Alt+PageDown`.
- **Crosshair Feedback Manager**: Coordinatore modulare a token per la vocalizzazione atomica di coordinate voxel, orientamento cardinale, proprietà dei blocchi e livello di luce.

---

## 🚀 [v26.2-1.14.0] — 2026-08-20 (Accademia Novizi, Mentor Contestuale & Bussola Acustica 360°)

### 🌟 Apprendimento & Orientamento Spaziale
- **Onboarding Interattivo & Accademia Novizi**: Missioni tutorial guidate per movimento, orientamento, raccolta legna, crafting e combattimento con guardrail di sicurezza per modalità di gioco.
- **Contextual Mentor & Snapshot Engine**: Motore di campionamento del contesto in tempo reale (`PlayerContextEngine`) e supporto audio gentile per situazioni di blocco, collisione, buio e fame.
- **Priority Speech Shield (`HelpNarrator`)**: Protezione delle istruzioni tutorial vocali dal troncamento causato da scanner o eventi secondari.
- **Bussola Acustica Continua a 360°**: Calcolo dinamico dell'angolo ($0^\circ \dots 359^\circ$) con modulazione di pitch e frequenza per l'orientamento a 360 gradi.
- **Telecamera Diagonale 2D**: Mappatura controlli a passo fisso e centratura orizzonte.

---

## 🚀 [v26.2-1.13.0] — 2026-08-10 (AutoWalk, Gestione Waypoints & Numpad Ergonomico)

### 🌟 Sistemi Core di Navigazione
- **Motore AutoWalk & Pathfinding**: Camminata automatica assistita con calcolo del percorso continuo e superamento ostacoli in tempo reale (`AutoWalkController`, `AutoWalkPathfinder`).
- **Gestione Waypoints & POI**: Schermate accessibili per salvare, gestire e tracciare Waypoints personalizzati (`ManageWaypointsScreen`, `SaveWaypointScreen`, `POIWaypoints`).
- **Numpad Ergonomico a 3 Layer (Zero-Shift)**: Mappatura ergonomica priva di tasto Shift per evitare accovacciamenti involontari (Layer 0 Azioni dirette, Layer 1 Coordinate/Snap, Layer 2 Ispezione/Status).

---

## 🚀 [v26.2-1.12.0] — 2026-08-01 (Inception Fork & Setup Ambiente)

### 📦 Setup Iniziale
- Fork del repository ufficiale a monte su Minecraft 26.2 / 1.21.x con Fabric + NeoForge, Architectury Loom e Java 25.
- Setup della base di conoscenza locale in `knowledge/` (00..12) e delle pipeline di collaudo con PrismLauncher e NVDA.
