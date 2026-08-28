# Piano Tecnico Implementativo: Navigatore & Marcia Automatica verso Target (Auto-Walk)

## 1. Obiettivo e Visione del Progetto

Il presente documento definisce la strategia, l'architettura tecnica e i dettagli implementativi per il nuovo modulo di **Navigatore e Marcia Automatica verso Bersaglio (Auto-Walk Navigation System)** in **Minecraft Access 1.12.0** su **Minecraft Java 26.2** (Fabric/NeoForge).

L'obiettivo è consentire a **Luca** di agganciare qualsiasi bersaglio (Blocco, Entità/NPC o Waypoint tramite `ObjectTracker` / `LockingHandler`) e avviare una marcia automatica sicura, fluida ed efficiente che:
1. Calcola il percorso voxel 3D più lineare e sicuro evitando ostacoli e pericoli nel raggio configurato.
2. Esegue avanzamento e orientamento continuo verso la meta.
3. Supera scalini fino a 1.20m con salto calcolato e **Re-Path istantaneo all'atterraggio**.
4. Affronta discese sicure ($\le 3$ blocchi) con attesa atterraggio e **Re-Path istantaneo**.
5. Attraversa corsi d'acqua (**Auto-Swim**) mantenendo il galleggiamento a pelo d'acqua e riprendendo la marcia sulla riva.
6. Garantisce la massima sicurezza con **Human Takeover al tick 0** (disattivazione immediata se viene premuto un tasto di movimento) e **Watchdog anti-incastro**.
7. Offre una **scheda dedicata nelle impostazioni di Minecraft Access (Cloth Config)** con interruttore generale e tutti i regolatori di precisione.
8. Fornisce feedback vocale NVDA/SAPI e audio 3D posizionale.

---

## 2. Checklist Operativa di Avanzamento Lavori

- [x] **Fase 1: Scheda Dedicata & Parametri di Configurazione (`Config.java`)**
  - [x] Aggiunta categoria Cloth Config `@ConfigEntry.Category("autoWalk")`.
  - [x] Interruttore generale: `public boolean enabled = true;`.
  - [x] Regolatore raggio massimo: `@ConfigEntry.BoundedDiscrete(min = 16, max = 128) public int maxRange = 64;`.
  - [x] Interruttore salto automatico: `public boolean autoJump = true;`.
  - [x] Interruttore nuoto automatico: `public boolean autoSwim = true;`.
  - [x] Interruttore Human Takeover: `public boolean stopOnManualInput = true;`.
  - [x] Interruttore feedback vocale: `public boolean voiceFeedback = true;`.
  - [x] Interruttore segnali sonori nodi: `public boolean playNodeSoundCue = true;`.
  - [x] Regolatore volume audio: `@ConfigEntry.BoundedDiscrete(min = 0, max = 1) public float audioCueVolume = 0.25f;`.
  - [x] Interruttore puntamento arrivo: `public boolean lookAtTargetOnArrival = true;`.
- [x] **Fase 2: Algoritmo di Pathfinding 3D A\* Voxel (`AutoWalkPathfinder.java`)**
  - [x] Struttura dati nodi e PriorityQueue con euristica di linearità e penalty per svolte.
  - [x] Validazione nodi in piano (floor solido, clearance testa/piedi).
  - [x] Validazione diagonali sicure (anti-snagging su hitbox 0.6x0.6).
  - [x] Validazione scalini in salita (`STEP_CLIMBABLE`, 0.60-1.20m con headroom $Y+2$ libera).
  - [x] Validazione discese sicure ($\le 3$ blocchi con floor solido; esclusione cadute $\ge 4$ blocchi).
  - [x] Validazione nodi d'acqua a pelo d'acqua con aria sopra.
  - [x] Blacklist assoluta pericoli (lava, fuoco, falò, cespugli bacche, cactus, rose wither, neve polverosa).
  - [x] Risoluzione target adiacente per blocchi solidi ed entità mobili.
- [x] **Fase 3: Motore di Marcia & Controller Dinamico (`AutoWalkController.java`)**
  - [x] State Machine: `IDLE`, `WALKING`, `JUMPING`, `SWIMMING`, `ARRIVED`, `CANCELLED`.
  - [x] Steering Yaw continuo verso il centro del nodo $(X+0.5, Z+0.5)$.
  - [x] Iniezione marcia avanti continua (`client.options.keyUp.setDown(true)`).
  - [x] Jump Timing predittivo a distanza $0.30 - 0.55$ m dallo scalino.
  - [x] Ciclo Salto con attesa `player.onGround()` e **Re-Pathing post-atterraggio da quota $+1$**.
  - [x] Ciclo Discesa con attesa `player.onGround()` e **Re-Pathing post-atterraggio**.
  - [x] Modalità Auto-Swim in acqua (`keyJump` continuo) e transizione a terra con re-path.
  - [x] Repath dinamico se il mob bersaglio si sposta $> 2$ blocchi.
  - [x] Watchdog anti-incastro (stuck detection su 12 tick con movimento $< 0.15$ m).
  - [x] Human Takeover al tick 0 su pressione di qualsiasi tasto di movimento manuale.
  - [x] Frenata e arresto di precisione all'arrivo con orientamento dello sguardo (Look-At).
- [x] **Fase 4: Modulo di Gestione, Keybindings & Access Menu (`AutoWalkManager.java`)**
  - [x] Registrazione modulo `BalmClientModule` e ciclo tick (`ClientPlayingTick.AFTER`).
  - [x] Keybinding Tastiera: `Alt + W` (`other.auto_walk`).
  - [x] Keybinding Numpad Layer 3: `Alt + Numpad 0` (`numpad.action.auto_walk` in `NumpadControls.java`).
  - [x] Integrazione funzione Access Menu (`features/accessmenu/AutoWalk.java` e `CoreAddon.java`).
- [x] **Fase 5: Localizzazione I18N (Focus Esclusivo IT ed EN)**
  - [x] Aggiornamento `it_it.json` con ordinamento alfabetico crescente rigoroso (opzioni Config, tasti, sintesi vocale).
  - [x] Aggiornamento `en_us.json` con ordinamento alfabetico crescente rigoroso.
- [x] **Fase 6: Compilazione, Linting, Deploy & Verifica sul Campo**
  - [x] Build Gradle `shadowJar` con Java 25 completata con esito positivo (`BUILD SUCCESSFUL`).
  - [x] Verifica del linting JSON CI (`keys == sorted(keys)` superato su tutti i 16 file).
  - [x] Deploy automatico del `.jar` in entrambe le istanze attive di PrismLauncher:
    - `Minecraft 26.2 Access 1.12.0`
    - `Minecraft 26.2 Access - Server Tenuta`
  - [x] Deploy nella cartella backup di OneDrive per la macchina corrente (**PC Portatile**):
    - `C:\Users\nemex\OneDrive\progetti dei frati\accessible games\minecraft archivio backup\minecraft backup\Minecraft 26.2 Access 1.12.0 pc portatile\minecraft\mods\`
  - [x] Aggiornamento e sincronizzazione della scheda di conoscenza `knowledge/07_sincronizzazione_salvataggi_e_deplo- [x] **Fase 7: Perfezionamento Corsa Fluida, Isteresi Anti-Chattering & Rotazione Continua (`AutoWalkController.java`)**
  - [x] **Isteresi Anti-Chattering dello Sprint (Zero Tremolio FOV)**: Timer di stabilizzazione a 20 tick (1 secondo) durante e dopo le virate, evitando continui attacca-stacca di sprint.
  - [x] **Rotazione Angolare Continua ($20^\circ$/tick)**: Sterzata fluida e decisa a $20^\circ$ per tick senza sottosterzo né correzioni a zig-zag.
  - [x] **Puntamento Diretto & Continuo**: Eliminazione delle discontinuità vettoriali ai nodi intermedi a favore della continuità differenziale `Mth.wrapDegrees`.
- [x] **Fase 8: Protezione Vocale dell'Arrivo & Calibrazione Audio Feedback (`NarrateCrosshair.java`, `ObstacleDetector.java` & `AutoWalkController.java`)**
  - [x] **Protezione Vocale Arrivo (Anti-Troncamento Bilaterale)**: Soppressione temporanea di 1.5s per **sia `NarrateCrosshair` che `ObstacleDetector`** all'arrivo per garantire la pronuncia integrale e ininterrotta del messaggio *"Arrivato a destinazione: [Nome]"* senza falsi avvisi ostacolo.
  - [x] **Calibrazione Canale & Volume Suono di Arrivo**: Riproduzione della campana (`NOTE_BLOCK_BELL`) sul canale primario `SoundSource.PLAYERS` a volume nitido `0.8f` e pitch `1.2f`.

---

## 3. Dettaglio Architetturale delle Classi

### A. Scheda Dedicata in `Config.java`
```java
@ConfigEntry.Category("autoWalk")
@ConfigEntry.Gui.TransitiveObject
public AutoWalk autoWalk = new AutoWalk();

public static final class AutoWalk {
    public boolean enabled = true;
    
    @ConfigEntry.BoundedDiscrete(min = 16, max = 128)
    public int maxRange = 64;
    
    public boolean autoJump = true;
    public boolean autoSwim = true;
    public boolean sprint = true;
    public boolean stopOnManualInput = true;
    public boolean voiceFeedback = true;
    public boolean playNodeSoundCue = true;
    
    @ConfigEntry.BoundedDiscrete(min = 0, max = 1)
    public float audioCueVolume = 0.25f;
    
    public boolean lookAtTargetOnArrival = true;
}
```

### B. Algoritmo di Pathfinding `AutoWalkPathfinder.java`
- Ricerca 3D A* su griglia di nodi calpestabili con supporto varchi porte, cancelletti e botole.
- Euristica: Distanza Euclidea con penalità per cambi di direzione frequenti (linearità) e penalità controllata per blocchi d'acqua.
- Bounding Box circoscritto a `Config.getInstance().autoWalk.maxRange`.
- Validazione adiacenza sicura per blocchi solidi (cassa, fornace, porta) e Waypoint individuando il miglior blocco d'aria calpestabile limitrofo.

### C. Controller di Movimento `AutoWalkController.java`
- Gestisce la fisica e gli input in `ClientPlayingTick.AFTER`.
- Gestisce i 4 stati fisici: Marcia su terra, Salto parabolico, Discesa libera controllata, Nuoto a pelo d'acqua.
- Gestisce l'atterraggio sicuro e il re-path istantaneo da quota reale.
- Gestisce il controllo dello stato di blocco (Watchdog anti-incastro) e l'interruzione manuale (Human Takeover).
- Gestisce la commutazione Corsa/Camminata con sterzata progressiva ($20^\circ$/tick), isteresi anti-chattering (timer 20 tick), protezione vocale bilaterale (1.5s) e audio feedback squillante su `SoundSource.PLAYERS`.

### D. Modulo di Controllo `AutoWalkManager.java` & Keybindings
- Intercetta i comandi:
  - `Alt + W` (Avvio/Stop Marcia Tastiera standard)
  - `Ctrl + Alt + W` (Alterna Corsa/Camminata Tastiera standard)
  - `Alt + Numpad 0` (Avvio/Stop Marcia Numpad Layer 3)
  - `Alt + Numpad .` (Alterna Corsa/Camminata Numpad Layer 3)
  - Voce in Access Menu
- Controlla `config.enabled`: se disabilitato, avvisa *"Modulo navigatore disabilitato nelle impostazioni"*.

---

## 4. Localizzazione Completa (`it_it.json` ed `en_us.json`)

Nuove chiavi (disposte in ordine alfabetico):

### Chiavi Config & Categoria:
- `text.autoconfig.minecraft-access.category.autoWalk`: *"Navigatore Automatico"* / *"Auto-Walk"*
- `text.autoconfig.minecraft-access.option.autoWalk.enabled`: *"Abilitato"* / *"Enabled"*
- `text.autoconfig.minecraft-access.option.autoWalk.maxRange`: *"Raggio massimo di navigazione (blocchi)"* / *"Maximum navigation range (blocks)"*
- `text.autoconfig.minecraft-access.option.autoWalk.autoJump`: *"Salto automatico ostacoli superabili"* / *"Auto-jump climbable obstacles"*
- `text.autoconfig.minecraft-access.option.autoWalk.autoSwim`: *"Nuoto automatico attraverso l'acqua"* / *"Auto-swim across water"*
- `text.autoconfig.minecraft-access.option.autoWalk.sprint`: *"Usa la corsa durante la navigazione"* / *"Sprint during navigation"*
- `text.autoconfig.minecraft-access.option.autoWalk.stopOnManualInput`: *"Interrompi su movimento manuale (Human Takeover)"* / *"Interrupt on manual movement (Human Takeover)"*
- `text.autoconfig.minecraft-access.option.autoWalk.voiceFeedback`: *"Feedback vocale"* / *"Voice feedback"*
- `text.autoconfig.minecraft-access.option.autoWalk.playNodeSoundCue`: *"Riproduci segnale sonoro ai checkpoint"* / *"Play sound cues at checkpoints"*
- `text.autoconfig.minecraft-access.option.autoWalk.audioCueVolume`: *"Volume segnali sonori"* / *"Audio cue volume"*
- `text.autoconfig.minecraft-access.option.autoWalk.lookAtTargetOnArrival`: *"Punta lo sguardo sul bersaglio all'arrivo"* / *"Aim at target on arrival"*

### Chiavi Tasti & Messaggi Vocali:
- `key.minecraft_access.other.auto_walk`: *"Navigazione e Marcia Automatica"* / *"Auto-Walk Navigation"*
- `key.minecraft_access.other.auto_walk_toggle_sprint`: *"Alterna Corsa/Camminata Navigatore"* / *"Toggle Auto-Walk Sprint/Walk"*
- `key.minecraft_access.numpad.action.auto_walk`: *"Navigazione Automatica"* / *"Auto-Walk"*
- `key.minecraft_access.numpad.action.auto_walk_sprint`: *"Alterna Corsa/Camminata Navigatore (Tastierino)"* / *"Toggle Auto-Walk Sprint/Walk (Numpad)"*
- `minecraft_access.access_menu.auto_walk`: *"Avvia Marcia verso Bersaglio"* / *"Start Auto-Walk to Target"*
- `minecraft_access.autowalk.arrived`: *"Arrivato a destinazione: %s"* / *"Arrived at destination: %s"*
- `minecraft_access.autowalk.cancelled`: *"Navigazione automatica annullata"* / *"Auto-walk cancelled"*
- `minecraft_access.autowalk.disabled`: *"Navigatore disabilitato nelle impostazioni"* / *"Auto-walk disabled in settings"*
- `minecraft_access.autowalk.no_path`: *"Nessun percorso sicuro trovato per %s"* / *"No safe path found for %s"*
- `minecraft_access.autowalk.out_of_range`: *"Bersaglio oltre il raggio di navigazione: %s metri"* / *"Target out of navigation range: %s meters"*
- `minecraft_access.autowalk.sprint_disabled`: *"Navigazione: camminata abilitata"* / *"Navigation: walking enabled"*
- `minecraft_access.autowalk.sprint_enabled`: *"Navigazione: corsa abilitata"* / *"Navigation: sprinting enabled"*
- `minecraft_access.autowalk.start`: *"Navigazione verso %s, distanza %s metri, %s passi"* / *"Navigating to %s, distance %s meters, %s steps"*
- `minecraft_access.autowalk.stuck`: *"Percorso ostruito, marcia arrestata"* / *"Path obstructed, auto-walk stopped"*

---

## 5. Esito della Verifica e Validazione Finale

1. **Compilazione Gradle**: Superata con successo (`BUILD SUCCESSFUL`, `minecraft-access-1.12.0-SNAPSHOT.jar` generato).
2. **Checkstyle & Linting**: 0 violazioni nei nuovi moduli.
3. **Ordinamento I18N**: Validazione automatizzata superata al 100% su tutti i file `.json`.
4. **Deploy & Backup Eseguiti**:
   - Istanze PrismLauncher: `Minecraft 26.2 Access 1.12.0` e `Minecraft 26.2 Access - Server Tenuta` (Allineate).
   - Backup OneDrive: `C:\Users\nemex\OneDrive\progetti dei frati\accessible games\minecraft archivio backup\minecraft backup\Minecraft 26.2 Access 1.12.0 pc portatile\minecraft\mods\` (Allineato).
5. **Collaudo In-Game**: Convalidato al 100% da Luca in data **27 Agosto 2026**. Progetto archiviato con successo.
