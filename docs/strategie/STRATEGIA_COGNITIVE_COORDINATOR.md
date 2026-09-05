# Documento Strategico Implementativo Unificato: Sistema Cognitivo Centralizzato del Personaggio (ASTRALIS v2.6.3)

Autori: Luca (Sviluppatore Senior Non Vedente) & Antigravity (Senior AI Pair Programmer)  
Revisione Strategica Congiunta: Antigravity & ChatGPT  
Repository: `minecraft-access` (Minecraft 26.2, Fabric / NeoForge, Balm, Java 25)  
Stato Documento: **In Corso — Fasi 1, 2, 3, 4 e 5 Implementate; Fase 5 in Revisione Post-Implementazione 5D.3 e Collaudo Telemetrico In-Game**

---

## 🧭 1. Visione d'Insieme & Obiettivo Fondamentale

Realizzare un’architettura cognitiva centrale per il giocatore non vedente in Minecraft che raccolga gli eventi significativi prodotti dai sottogestori specialistici (sicurezza, movimento, mirino, POI, orientamento, stato, didattica), ne valuti urgenza, contesto, ridondanza e complementarità, e produca un feedback vocale (NVDA / Tolk tramite l'astrazione del client) e sonoro 3D pulito, ordinato e coerente.

### 1.1 Il Problema Sistemico Attuale nel Codebase
Attualmente il mod presenta oltre 180 chiamate dirette a `MainClass.narrate(...)` distribuite in decine di moduli indipendenti. 
Esistono soluzioni tampone isolate:
- `NarrationPriority`: scudo temporale grezzo (`shieldUntil`) che sopprime temporaneamente il mirino e l'ostacolo durante eventi salienti;
- `CrosshairFeedbackManager`: arbitro locale che combina parzialmente il mirino con gli ostacoli frontali;
- `PlayerContextEngine`: campionamento di contesto ogni 10 tick (500 ms) riservato a Mentore e Accademia.

La mancanza di un arbitro globale provoca:
- **Speech Truncation (Troncamento Vocale con NVDA)**: se due sensori emettono un messaggio con `interrupt = true` nello stesso tick o a distanza di 50 ms, NVDA tronca a metà la prima parola ("Bur...", "Zom...", "Terra");
- **Accodamento Pericoloso**: se si usa `interrupt = false` senza coordinamento, un avviso critico di lava o burrone rischia di finire accodato dopo una frase descrittiva di 4 secondi pronunciata dal Mentore o dall'indicatore di bioma;
- **Chatter Ridondante**: ripetizioni continue di messaggi simili durante il movimento o le oscillazioni della visuale.

### 1.2 Principio di Soluzione
Il nuovo sistema non sostituisce i sensori esistenti e non ne appesantisce i calcoli: agisce come **direttore d'orchestra dell'output**. I sensori rilevano, interpretano nel proprio dominio e inviano un evento strutturato; il coordinatore decide tempi, priorità, soppressioni, suoni ed eventuale concatenazione delle frasi.

---

## 🏛️ 2. Architettura a Tre Livelli del Sistema Cognitivo

```text
[Livello 1] Sensori & Rilevatori Tecnici (Voxel, Raycast, Entità, Inventario, Luce)
       ↓
[Livello 2] Sottogestori di Dominio Specialistici (Sicurezza, Esplorazione, Movimento, Stato, Guida)
       ↓
[Livello 3] Cognitive Coordinator Centrale (Buffer, Fast-Path, Arbitraggio Fine-Tick, Memoria Attentiva)
       ↓
[Uscita Protetta] MainClass.narrate (Astrazione Narratore/Tolk/OS) + Audio 3D Posizionale
```

### Livello 1 — Sensori e Rilevatori Tecnici
- **Responsabilità**: Calcoli geometrici puri, raycast voxel, collisioni, bounding box, rilevamento entità, inventario e fluidi.
- **Disciplina**: Non decidono autonomamente quando parlare né invocano `MainClass.narrate`. Producono esclusivamente dati grezzi o notifiche interne verso il proprio sottogestore.

### Livello 2 — Sottogestori di Dominio
Interpretano i dati tecnici della propria area, applicano i filtri configurati dall'utente e producono un `CognitiveEvent` pronto per l'arbitraggio.
- **SafetyManager**: cadute, bordi, lava, soffocamento, ostacoli non superabili, collisioni violente e minacce ostili a distanza ravvicinata.
- **ExplorationManager**: mirino (`NarrateCrosshair`), scanner direzionale (`DirectionalPathScanner`), POI, bussole e tracciatore risorse.
- **MovementManager**: auto-walk (`AutoWalkManager`), avanzamento percorso, arresti di rotta, orientamento cardinale continuo e passi sonori.
- **StatusManager**: barra cuori, fame, bolle d'aria, effetti di stato, livello luce, meteo e ciclo orario.
- **GuidanceManager**: Mentore contestuale (`ContextualMentor`), Accademia, tutorial e conferme didattiche.
- **InterfaceManager**: navigazione GUI, inventari, toast, ricettario e messaggi di sistema (esclusi dall'arbitraggio ambientale, vedi Sezione 7).

### Livello 3 — Cognitive Coordinator Centrale
Riceve gli eventi cognitivi, mantiene una memoria attentiva breve dello stato del giocatore e decide in modo deterministico l'azione di output attraverso l'astrazione di `MainClass.narrate`.

---

## ⚡ 3. Canali di Elaborazione Temporale: Fast-Path vs Arbitraggio a Fine-Tick

Per evitare il rischio mortale di ritardare allarmi salvavita, il coordinatore adotta un doppio binario operativo collegato al ciclo di vita reale del client (`ClientPlayingTick.AFTER`):

### 3.1 Canale Fast-Path Immediato (Latenza 0 ms)
- **Applicazione**: Riservato unicamente alla priorità `CRITICA` (caduta nel vuoto, contatto con lava o fuoco, danno rapido da mob, annegamento).
- **Comportamento & Astrazione**: Bypassa qualsiasi buffer o attesa di fine tick. Invia l'allarme tramite l'astrazione ufficiale del client `MainClass.narrate(text, true)` (preservando i controlli su finestra attiva, Tolk/SAPI e narratore di gioco), zittisce i cue sonori secondari della mod e impone uno scudo protettivo vocale di 1500 ms contro qualsiasi distrazione di fondo.
- **Risoluzione di Concorrenza tra Critici Simultanei**: Se due pericoli critici scattano nello stesso tick (es. burrone + danno da freccia):
  1. Il primo pericolo rilevato viene pronunciato **immediatamente** con interrupt;
  2. Il secondo evento critico non cancella il primo a metà parola: è emesso subito dopo in **coda immediata in forma ultra-breve**, tassativamente con `MainClass.narrate(testoBreve, false)`. Il primo usa `MainClass.narrate(testo, true)`; questa distinzione impedisce il troncamento;
  3. I relativi suoni 3D di danno e pericolo suonano entrambi istantaneamente.

### 3.2 Canale Arbitrato a Fine-Tick Flush (`ClientPlayingTick.AFTER`)
- **Applicazione**: Priorità `OPERATIVA`, `CONTESTUALE` e `PASSIVA`.
- **Comportamento**: Durante il tick corrente del client, gli eventi generati dai vari sottogestori vengono raccolti in un buffer leggero. All'evento `ClientPlayingTick.AFTER` (a fine tick), il coordinatore:
  1. Ordina gli eventi raccolti in base alla gerarchia di priorità;
  2. Rimuove duplicati identici o eventi resi obsoleti da eventi superiori nello stesso tick;
  3. Valuta le regole di concatenazione semantica (se due eventi sono compatibili, li unisce in una frase unica);
  4. Invia l'output risultante tramite `MainClass.narrate(...)` o il canale sonoro, azzerando il buffer per il tick successivo.

---

## 📦 4. Contratto Unico degli Eventi: `CognitiveEvent`

Ogni evento cognitivo scambiato nel sistema è rappresentato da un record Java immutabile:

```java
public record CognitiveEvent(
        SourceDomain domain,           // SAFETY, EXPLORATION, MOVEMENT, STATUS, GUIDANCE, INTERFACE
        CognitivePriority priority,    // CRITICAL, OPERATIONAL, CONTEXTUAL, PASSIVE
        String semanticKey,            // Chiave semantica base (es. "obstacle:ahead", "crosshair:target")
        StateSignature stateSignature, // Firma di stato per deduplicazione fine (gravità, distanza bucket, entità)
        String narrationText,          // Testo già localizzato (I18n) e pronto per la sintesi vocale
        @Nullable BlockPos targetPos,  // Posizione spaziale associata (se applicabile)
        double distance,               // Distanza dal giocatore in blocchi (-1 se non applicabile)
        OutputType outputType,         // VOICE_AND_SOUND, VOICE_ONLY, SOUND_ONLY, SILENT
        @Nullable SoundCue soundCue,   // Suono 3D, volume configurato dall'utente e pitch
        long ttlMillis,                // Tempo di validità dell'evento (Time-To-Live)
        boolean interruptible,         // Se può essere interrotto da eventi di pari o superiore priorità
        boolean canChain               // Se è autorizzato a concatenarsi con un evento compatibile
) {}
```

### Regole Rigide sul Contratto
- **Testo già Pronto**: I sottogestori conoscono il proprio dominio, leggono la lingua attiva e formattano la stringa localizzata. Il coordinatore non deve interpretare la grammatica dei singoli blocchi.
- **Deduplicazione Semantica ad Alta Fedeltà**: La deduplicazione confronta la quintupla: `(domain, semanticKey, targetPos, stateSignature, priority)`. Se una minaccia si avvicina (es. da 3 blocchi a 1 blocco) o muta gravità, la variazione di `stateSignature` garantisce che il nuovo allarme venga pronunciato e non scartato come duplicato.
- **Delta-Driven Emission**: I sensori emettono un `CognitiveEvent` solo su variazione effettiva di stato o superamento di soglia temporale, evitando la saturazione della memoria heap ad ogni tick.

---

## 🎛️ 5. Integrazione Sistemica a Due Livelli con le Impostazioni Cloth Config

L'architettura rispetta ed esalta ogni singola opzione delle schede impostazioni esistenti nel nostro fork, secondo il principio: **"Filtro a Monte, Arbitraggio a Valle"**.

### 5.1 Livello 1 — Filtro a Monte (Nelle Schede Specifiche dei Sensori)
Le schede attuali di Cloth Config continuano a governare *SE*, *COSA* e *COME* ciascun sensore rileva:
- **`FallDetector` (`Config.fallDetector`)**:
  - Se `voiceWarning` è falso: l'evento viene generato come `SOUND_ONLY`.
  - Se `playAudioCues` è falso: l'evento non richiede cue sonori.
  - Se entrambi sono falsi: il rilevatore non sottopone alcun evento al coordinatore.
  - I volumi configurati dall'utente (anche se inferiori a 0.7f, es. 0.25f o 0.5f) vengono integralmente preservati nel `SoundCue`. La fascia 0.7f-0.8f rappresenta il tetto massimo di sicurezza ASTRALIS, non una forzatura sui gusti dell'utente.
  - Se il modulo è disabilitato (`config.enabled = false`): zero eventi generati.
- **`ObstacleDetector` (`Config.obstacleDetector`)**:
  - Il testo dell'evento viene formattato a monte secondo il valore effettivo di `narrationStyle` (`NarrationStyle`, attualmente configurato su `BLOCK`), senza introdurre nomi di modalità non presenti nella configurazione.
  - I range di rilevamento e i tipi di direzione impostati vengono rispettati al 100%.
- **`NarrateCrosshair` (`Config.narrateCrosshair`)**:
  - La modalità movimento (`movementFeedbackMode`: `OFF`, `TARGET_ONLY`, `TARGET_AND_DISTANCE`) determina la forma del testo passivo.
  - L'ordine di lettura (`readingOrder`) e i suoni di elevazione (`relativePositionSoundCue`) sono rispettati prima dell'invio al coordinatore.
- **`AutoWalk` e `DirectionalPathScanner`**:
  - Vengono rispettati i campi effettivi: `enabled`, `maxRange`, `autoJump`, `autoSwim`, `stopOnManualInput`, `voiceFeedback`, `playNodeSoundCue` e `audioCueVolume` per AutoWalk; `scanRange`, rilevamenti abilitati, `verbosityMode` e `audioFeedback` per lo scanner direzionale.
- **`HelpSettings` (Mentore e Accademia)**:
  - `mentorEnabled`, `autoAdvanceMissions` e `helpPriorityOverride` rimangono interamente sotto il controllo dell'utente. Eventuali controlli aggiuntivi di frequenza o verbosità saranno una nuova scelta progettuale, non un'impostazione già esistente.

### 5.2 Livello 2 — Nuova Categoria Globale: `CognitiveSettings` in Cloth Config
Aggiunta in `Config.java` come categoria ordinata e localizzata (IT/EN in ordine alfabetico):
1. **`cognitiveCoordinatorEnabled` (Booleano, Default: `true`)**:
   - Se disattivato, i moduli migrati eseguono un fallback immediato al loro comportamento storico diretto. I moduli non ancora migrati continuano già a parlare direttamente.
2. **`chainedNarrationEnabled` (Booleano, Default: `true`)**:
   - Abilita o disabilita la fusione di due eventi compatibili nello stesso tick. Se disattivato, gli eventi restano annunciati singolarmente.
3. **`ambientSpeechDensity` (Enum: `MINIMA`, `EQUILIBRATA`, `COMPLETA`, Default: `EQUILIBRATA`)**:
   - `MINIMA`: Solo pericoli critici e comandi espliciti da tastiera;
   - `EQUILIBRATA`: Pericoli, comandi, ostacoli del cammino e mirino su cambio bersaglio;
   - `COMPLETA`: Include cambi luce di sottofondo, bioma continuo e meteo.
4. **`criticalModAudioDucking` (Booleano, Default: `true`)**:
   - Attenua o silenzia temporaneamente i suoni secondari *generati da Minecraft Access* (es. click di scansione, passi sonori, sweep orientamento) quando scatta un allarme critico, senza toccare l'audio di sistema o di NVDA.
5. **`deduplicationWindowMs` (Intero, Default: `1500` ms)**:
   - Finestra temporale entro cui una minaccia statica nella stessa posizione e con la stessa gravità non viene ripetuta a voce piena.

### 5.3 Conservazione di `NarrationPriority` come Facciata Compatibile (Facade Pattern)
La classe `NarrationPriority` non viene cancellata: viene trasformata in un Adapter/Facade verso `CognitiveCoordinator`. Le chiamate storiche ancora presenti in Mixin non migrati (es. `ToastManagerMixin`, `ClientPacketListenerMixin`) continueranno a funzionare senza modifiche al bytecode.

---

## 🎓 6. Mentore, Accademia e Didattica

Mentore e Accademia costituiscono un dominio autonomo rispetto al Navigatore e alla marcia automatica. La loro funzione è accompagnare Luca nella comprensione di un'attività, non dirigere lo spostamento del personaggio.

1. **Mentore contestuale**: propone un consiglio breve soltanto quando il contesto lo rende utile. Un consiglio opzionale può attendere o essere abbandonato se Luca ha già cambiato situazione.
2. **Accademia**: accompagna una missione avviata volontariamente da Luca, confermando l'inizio, il successo e il passo seguente senza spezzare il filo dell'apprendimento.
3. **Priorità funzionale**: un pericolo o un evento del Navigatore che richiede una decisione immediata interrompe la didattica. Al termine, la didattica riprende solo dall'obiettivo ancora attuale, mai da una frase rimasta indietro.
4. **Discrezione**: il Mentore non ripete consigli già consegnati e non compete con una risposta richiesta direttamente da Luca.
5. **Separazione dei collaudi**: il comportamento di Mentore e Accademia viene affrontato in una fase distinta, dopo il completamento del Navigatore e dell'Auto-Walk. Questo permette di verificare con chiarezza sia l'esperienza di movimento sia quella di apprendimento.

---

## 🚫 7. Esclusione Deliberata: Input Diretti da Tastiera e Menu GUI

Non tutti i messaggi del mod devono transitare dal coordinatore cognitivo:
- **Interazione Diretta Tastiera / GUI**:
  - Navigazione inventario e slot contenitori;
  - Digitazione in campi testo (`EditBoxMixin`, chat, comandi);
  - Schermate Cloth Config, menu di aiuto rapido, AccessMenu;
  - Controlli Numpad e lettura coordinate su richiesta esplicita.
- **Regola Operativa**: Questi eventi appartengono all'azione motoria immediata del giocatore. Mantengono la via diretta a latenza zero (`MainClass.narrate`) e, quando attivi, zittiscono temporaneamente la percezione cognitiva ambientale di fondo.
  - Il piano tecnico deve introdurre un token temporaneo esplicito, ad esempio `DirectInteractionShield`, attivato dall'input diretto e consultato dal coordinatore. Il token differisce solo gli eventi ambientali passivi o contestuali: non può mai bloccare né ritardare un evento `CRITICAL`.

---

## 🔗 8. Regole di Sintesi e Concatenazione I18N

Quando a fine tick sono presenti eventi compatibili di aree diverse:
1. **Gerarchia di Composizione**:
   - Prima la Sicurezza (ostacolo/pericolo), poi la Posizione/Bersaglio (mirino), infine lo Stato secondario.
2. **Limite di Fusione**:
   - Massimo 2 eventi in una singola frase sintetica.
3. **Template I18N Formali (Nessuna concatenazione grezza con virgole)**:
   - Template Ostacolo + Mirino:  
     `it_it.json`: `"minecraft_access.cognitive.join_obstacle_target": "%s. Mirino: %s"`  
     `en_us.json`: `"minecraft_access.cognitive.join_obstacle_target": "%s. Crosshair: %s"`
   - Esempio acustico: *"Davanti: gradino saltabile a 1 blocco. Mirino: Tronco di quercia"*.
4. **Regola di Incompatibilità**:
   - Eventi di ambiti spaziali opposti (es. "Nemico a sinistra" e "Risorsa a destra") o con priorità discordanti non vengono fusi: l'evento prioritario viene pronunciato subito, il secondario viene rimandato o tradotto in solo suono 3D.

---

## 🧠 9. Memoria Attentiva Breve (Short-Term Attention Memory)

Il coordinatore conserva una struttura dati leggera (massimo 8 voci, azzerata a ogni cambio mondo, morte, respawn, disconnessione o cambio dimensione):
- **Ultimo Pericolo Attivo**: tipologia, coordinate, livello di gravità e timestamp dell'ultimo allarme critico;
- **Ultimo Bersaglio Focalizzato**: ID entità o BlockPos dell'ultimo mirino stabile;
- **Stato di Ripristino di Sicurezza ("Percorso Libero")**:
  - Se un pericolo di burrone o ostacolo alto era stato annunciato, e il giocatore cambia rotta o arretra entrando in uno stato di cammino sicuro per almeno 300 ms, il coordinatore pronuncia una conferma sintetica: *"Percorso libero"*.
  - Questo elimina l'incertezza del giocatore non vedente ("sarò ancora sul bordo del burrone o mi sono disimpegnato?").

### 9.1 Origine delle Transizioni e Reset Affidabili
- Solo il sottogestore competente può dichiarare il ritorno alla sicurezza: `FallDetector` per il burrone, `ObstacleDetector` per l'ostacolo e `AutoWalkManager` per il percorso automatico. Il coordinatore non deve inferirlo da solo.
- Il piano tecnico deve collegare `clearAllBuffers()` a hook espliciti: cambio o disconnessione dal server, cambio di livello/dimensione e transizioni morte/respawn. Non basta il solo reset su connessione già offerto da `ServerChangeDetector`.

---

## 🛡️ 10. Validazione Preventiva ASTRALIS sui 7 Assi di Qualità

1. **Validità**: Pienamente compatibile con Balm, Fabric, NeoForge e Java 25. Nessun Mixin invasivo necessario. Utilizzo corretto di `ClientPlayingTick.AFTER` e `MainClass.narrate`.
2. **Efficacia**: Obiettivo di test primario: eliminazione totale degli speech truncation con NVDA durante camminata, combattimento ed esplorazione.
3. **Coerenza**: Integra armoniosamente `NarrationPriority` (come facciata retrocompatibile), `CrosshairFeedbackManager` e `PlayerContextEngine` come organi specializzati del coordinatore.
4. **Completezza**: Copre ogni tipologia di evento: Critico (Fast-Path), Operativo, Contestuale, Passivo e Didattico.
5. **Precisione**: Deduplicazione basata su coordinate voxel, chiavi semantiche e firme di stato, preservando la percezione delle variazioni di distanza.
6. **Affidabilità e Prestazioni**: Target prestazionale verificabile via telemetria: tempo di elaborazione inferiore a 0.15 ms per tick sul thread client a regime ordinario.
7. **Assenza di Regressioni**: La presenza del fallback su `MainClass.narrate`, della facciata `NarrationPriority` e della configurazione `cognitiveCoordinatorEnabled` garantisce che ciascun sensore migrato possa tornare istantaneamente al comportamento storico.

---

## 🧪 11. Matrice di Simulazione a 3 Livelli

### Livello 1 — Scenari Comuni (Happy Path)
- **Scenario 1.1: Camminata con dislivello e mirino su blocco**  
  - *Condizione*: Il giocatore cammina. `ObstacleDetector` rileva un gradino (+0.5 m); il mirino incontra pietra a 2 blocchi.  
  - *Esito*: Il coordinatore fonde i due eventi: *"Davanti: gradino saltabile, Pietra a 2 blocchi"*. Nessun troncamento.
- **Scenario 1.2: Auto-walk lineare e arrivo a waypoint**  
  - *Condizione*: Auto-walk arriva a destinazione mentre il mirino punta aria.  
  - *Esito*: L'evento Operativo di arrivo domina. Emette cue 3D di arrivo e vocalizza *"Destinazione raggiunta"*. Mirino silenziato.
- **Scenario 1.3: Raccolta drop mentre si inquadra una cassa**  
  - *Condizione*: Focus mirino su cassa, passaggio sopra drop di ferro a terra.  
  - *Esito*: L'annuncio del ferro raccolto ha precedenza e attiva lo scudo. Il mirino della cassa tace per 1200 ms e riprende solo se il giocatore continua a guardarla.

### Livello 2 — Scenari Meno Comuni & Concorrenza (Alternative Paths)
- **Scenario 2.1: Tripla emergenza — Burrone + Freccia Scheletro + Mirino**  
  - *Condizione*: Rilevato burrone, subito danno da freccia, mirino su ghiaia.  
  - *Esito*: Il burrone parte istantaneamente su Fast-Path (< 1 ms); il danno da scheletro segue a brevissima distanza in micro-burst ("Burrone profondo davanti! Danno Scheletro"). Il mirino viene scartato. Audio cue di pericolo e danno riprodotti all'istante.
- **Scenario 2.2: Ingresso in cunicolo buio 1x2**  
  - *Condizione*: Soffitto basso (ostacolo testa), luce 0 (buio), parete a 1 blocco, cambio bioma.  
  - *Esito*: Ostacolo e parete fusi: *"Spazio stretto, parete a 1 blocco"*. Il buio produce cue sonoro; il bioma viene differito a quando il movimento si stabilizza.
- **Scenario 2.3: Apertura inventario durante allarme fame**  
  - *Condizione*: Pressione `E` per aprire l'inventario mentre la fame scende a 3 cosciotti.  
  - *Esito*: La GUI possiede il canale vocale istantaneo (*"Inventario..."*). L'allarme fame non interrompe la navigazione dello slot; viene memorizzato ed emesso alla chiusura della GUI.
- **Scenario 2.4: Ostacolo imprevisto durante spiegazione del Mentore**  
  - *Condizione*: Auto-walk sbatte su un blocco mentre il Mentore illustra una funzione.  
  - *Esito*: L'evento Operativo di arresto interrompe la voce del Mentore: *"Auto-walk interrotto: ostacolo davanti"*. La sicurezza prevale sulla didattica.

### Livello 3 — Casi Limite & Corner Cases (Boundary, Zero, Null, Error)
- **Scenario 3.1: Morte, respawn o cambio dimensione (Nether Portal)**  
  - *Condizione*: Il giocatore muore o attraversa un portale con puntatori temporaneamente nulli.  
  - *Esito*: Reset istantaneo della memoria attentiva breve (`clearAllBuffers()`). Coda svuotata, zero annunci fantasma, zero crash o NPE.
- **Scenario 3.2: Edge-Oscillator (Oscillazione continua sul ciglio del burrone)**  
  - *Condizione*: Il giocatore è fermo sul millimetro di bordo ed esegue micro-movimenti a 60 FPS.  
  - *Esito*: La deduplicazione semantica riconosce le stesse coordinate e la stessa stateSignature. Dopo il primo annuncio vocale completo, entra in modalità Audio-Only (ripete solo un battito sonoro leggero ogni 1500 ms senza ripetere la frase a parole).
- **Scenario 3.3: Perdita di focus finestra (`!client.isWindowActive()`)**  
  - *Condizione*: Pressione `Alt+Tab` durante il movimento o scansione.  
  - *Esito*: Arresto immediato dell'emissione e svuotamento dei buffer passivi. Al ritorno in finestra, nessun accumulo a raffica di messaggi arretrati.
- **Scenario 3.4: Narratore OFF o screen reader non disponibile**  
  - *Condizione*: Screen reader non attivo o driver disconnesso.  
  - *Esito*: La parte vocale viene soppressa in modo sicuro dall'astrazione esistente di `MainClass` e `GameNarratorMixin`; i suoni 3D posizionali continuano a funzionare regolarmente. Il piano non deve promettere il rilevamento o il recupero automatico di un crash esterno del lettore vocale senza evidenza tecnica specifica.

---

## 🚀 12. Tabella Gerarchica delle Priorità Cognitive

- **1. CRITICA (Fast-Path 0 ms tramite `MainClass.narrate`)**:
  - *Interrompe*: Operativa, Contestuale, Passiva.
  - *Interrotto da*: Nessuno (due critici concorrenti si succedono in micro-burst senza troncamento).
  - *Esempi*: Burrone, lava, soffocamento, danno ravvicinato da mob.
- **2. OPERATIVA (Fine-Tick Flush via `ClientPlayingTick.AFTER`)**:
  - *Interrompe*: Contestuale, Passiva.
  - *Interrotto da*: Critica.
  - *Esempi*: Arrivo AutoWalk, ostacolo di blocco, aggancio target POI (`X`), ispezione ostacoli (`U`), raccolta oggetti.
- **3. CONTESTUALE (Fine-Tick Flush con Concatenazione)**:
  - *Interrompe*: Passiva.
  - *Interrotto da*: Critica, Operativa.
  - *Esempi*: Gradino saltabile (auto-step), variazione fame/vita non letale, mob a media distanza (5-10 blocchi), buio.
- **4. PASSIVA (Fine-Tick Flush di Background)**:
  - *Interrompe*: Nessuno.
  - *Interrotto da*: Critica, Operativa, Contestuale.
  - *Esempi*: Mirino continuo su blocchi ordinari, bioma di fondo, meteo, passi sonori.

---

## 🗺️ 13. Roadmap di Implementazione a 9 Fasi (Branch `feat/cognitive-orchestrator`)

Tutto il lavoro sarà isolato nel branch `feat/cognitive-orchestrator` creato da `mymaster`. 
**Vincolo di Sicurezza ASTRALIS (Regola 0)**: Nessun codice raggiungerà `mymaster` senza collaudo completo in-game con NVDA, esito positivo e via libera esplicito di Luca.

- [x] **Fase 1 — Modello Dati e Coordinatore Silenzioso (Completata — Commit `e41c3f9d`)**:
  - [x] Creazione di `CognitiveEvent.java`, `CognitivePriority.java`, `SourceDomain.java` e `StateSignature.java`.
  - [x] Creazione di `CognitiveCoordinator.java` con buffer a fine tick (`ClientPlayingTick.AFTER`), Fast-Path e memoria attentiva breve.
  - [x] Test automatici unitari (JUnit) su priorità, scadenze TTL e deduplicazione (14 test superati a 0 ms).
- [x] **Fase 2 — Integrazione Scheda Cloth Config & Facciata `NarrationPriority` (Completata — Commit `88c3ddb7`, `580c060a`)**:
  - [x] Aggiunta di `CognitiveSettings` in `Config.java` e nei file di lingua (`it_it.json`, `en_us.json` ordinati alfabeticamente).
  - [x] Trasformazione di `NarrationPriority` in facciata retrocompatibile verso il coordinatore (con seam package-private per test headless).
  - [x] Verifica trasparenza: il gioco continua a funzionare identicamente allo stato attuale (8 nuovi test unitari).
- [x] **Fase 3 — Migrazione Pilota Sicurezza & Bonifica Anomalie GUI (Completata e Collaudata in-game — Commit `6413d721`)**:
  - [x] Sotto-Fase 3A: Collegamento di `FallDetector` a `CognitiveCoordinator` (avvisi burrone e ciglio CRITICAL/OPERATIONAL, bypass climbable scale a pioli `Rev MC-26.8`).
  - [x] Sotto-Fase 3B: Collegamento di `ObstacleDetector` a `CognitiveCoordinator` (`ObstacleSafetyEventFactory`, `ObstacleNarrationComposer`, `CrosshairFeedbackManager` soppressione monotona e silent commit).
  - [x] Bonifica Post-Collaudo GUI: Risoluzione `Rev MC-26.9` (doppio guard `InventoryControls` e `isActiveContainerScreen()`) e `Rev MC-26.10` (`suspendForGui()` con ownership token e soppressione Shift sneak hijack).
  - [x] Collaudo in-game con NVDA su burroni, dislivelli, scale a pioli e navigazione inventari (0 eccezioni, telemetria pulita al 100%).
- [x] **Fase 4 — Migrazione Esplorazione, Mirino & POI Cognitivi (Completata e Collaudata in-game — Commit `b05ea8f9`, `80c8d66d`, `4bc424c3`)**:
  - [x] Sotto-Fase 4A: Gate di routing cognitivo per l'esplorazione (`explorationCognitiveRoutingEnabled`) e reset deterministico di sessione su cambio dimensione/morte/respawn.
  - [x] Sotto-Fase 4B: Migrazione del feed automatico del mirino (`CrosshairExplorationEventFactory`, ID canonici `EXPLORATION` / `PASSIVE`, fallback diretto).
  - [x] Sotto-Fase 4C: `DirectInteractionShield` per comandi espliciti (tasto `B`, rotazione visuale/sguardo livellato, radar POI e lock entità `X`).
  - [x] Collaudo in-game positivo con NVDA (tracciamento mucca, XP/drop, centramento visuale, 208 test JUnit verdi, zero eccezioni).
- [x] **Fase 5 — Navigatore e Auto-Walk (Implementata — Revisione Post-Implementazione 5D.3 e Collaudo Telemetrico In-Game in Corso)**:
  - [x] Ordinamento dei messaggi dell'intero Navigatore: scelta della meta, avvio, avanzamento significativo, percorso non disponibile, arresto, arrivo e restituzione del controllo manuale.
  - [x] Priorità dei cambiamenti di navigazione rispetto alle descrizioni ambientali di sfondo.
  - [ ] Lo scanner direzionale attivato con Pagina Su e Pagina Giù rimane un sistema storico indipendente ed è esplicitamente fuori dallo scopo del coordinamento cognitivo.
- [ ] **Fase 6 — Mentore e Accademia**:
  - [ ] Ordinamento delle missioni, dei traguardi e dei suggerimenti contestuali senza sovrapposizioni o ripetizioni tardive.
  - [ ] Protezione della didattica attiva dalle informazioni ambientali di sfondo, senza mai ritardare sicurezza o cambiamenti importanti del Navigatore.
  - [ ] Collaudo autonomo delle esperienze di apprendimento.
- [ ] **Fase 7 — Collaudo Globale della Strategia Cognitiva**:
  - [ ] Stress-test in-game di tutti i domini consolidati con screen reader NVDA;
  - [ ] Verifica che Navigatore, didattica, sicurezza ed esplorazione convivano senza messaggi sovrapposti o tardivi;
  - [ ] Convalida di Luca del completamento dei punti strategici.
- [ ] **Fase 8 — Buffer Registro Revisioni Post-Strategia**:
  - [ ] Affrontare, soltanto dopo la Fase 7 convalidata, tutte le revisioni oggi differite nel Registro Revisioni;
  - [ ] Eseguire per ogni revisione il proprio ciclo di verifica e collaudo, senza riaprire gli ambiti già protetti se non strettamente necessario.
- [ ] **Fase 9 — Validazione Finale, Documentazione e Merge**:
  - [ ] Rieseguire il collaudo globale dopo il buffer delle revisioni;
  - [ ] Aggiornare documentazione e rapporto di rilascio;
  - [ ] Presentare il resoconto a Luca e proporre il merge su `mymaster` esclusivamente su suo esplicito consenso.

---

## 14. Strategia Funzionale Fase 5 — Navigatore e Auto-Walk

**Stato:** Implementata; le osservazioni emerse dal collaudo in-game sono gestite nella revisione post-implementazione 5D.3, senza riaprire le sotto-fasi già concluse.

Questa sezione definisce l'esperienza di navigazione desiderata. Non prescrive soluzioni tecniche e non autorizza modifiche al codice.

### 14.1 Obiettivo percepito dal giocatore

Luca deve poter sapere sempre se il Navigatore ha una meta valida, se la marcia è iniziata, se il percorso prosegue, se occorre intervenire e quando la destinazione è stata raggiunta.

La voce non deve commentare la marcia regolare passo per passo. Interviene con frasi brevi nei cambiamenti che modificano concretamente le possibilità di agire. I passi e i suoni ambientali restano il sottofondo naturale della navigazione.

### 14.2 Ordine funzionale delle informazioni

1. **Sicurezza immediata**: pericoli e avvisi che richiedono reazione prevalgono sempre e possono interrompere ogni altra frase.
2. **Azione esplicita di Luca**: la richiesta di avvio o arresto, la scelta di una meta e la ripresa del controllo manuale ricevono una risposta chiara e tempestiva.
3. **Cambiamento del Navigatore**: percorso non disponibile, bersaglio troppo lontano, arresto necessario, arrivo e ripresa della marcia sono più importanti delle descrizioni ambientali di sfondo.
4. **Informazioni ambientali spontanee**: mirino e descrizioni di sfondo restano gli ultimi a parlare e non devono ricomparire tardi dopo un evento del Navigatore.

### 14.3 Regola della frase utile unica

In ogni istante il giocatore deve ricevere una sola informazione utile, non una sequenza di messaggi in concorrenza. Quando un messaggio perde valore perché la situazione cambia, viene abbandonato invece di essere pronunciato in ritardo.

Le conferme richieste direttamente da Luca non vengono sacrificate ai messaggi automatici. Un messaggio relativo a una meta precedente non deve comparire dopo che Luca ha annullato la marcia, ripreso il controllo o scelto un'altra destinazione.

### 14.4 Esperienza del Navigatore

Il Navigatore deve dare certezze nei momenti di cambiamento: meta assente o non raggiungibile, partenza, avanzamento significativo, arresto, arrivo alla destinazione e restituzione immediata del controllo a Luca.

Durante una marcia regolare non deve commentare ogni passo o ogni piccolo avanzamento. Se il sistema trova autonomamente una soluzione a una difficoltà temporanea, continua con discrezione. Parla solo quando la situazione richiede una decisione di Luca oppure quando il risultato finale della marcia è cambiato.

Se Luca riprende il movimento manuale, la sua scelta prevale senza ambiguità. La risposta deve confermare il nuovo stato una volta sola, senza lasciare annunci residui della marcia automatica.

### 14.5 Scenari guida da preservare

1. Luca chiede di navigare senza avere scelto una meta: riceve una risposta immediata e comprensibile, senza messaggi ambientali sovrapposti.
2. La meta è troppo lontana oppure non esiste un percorso sicuro: il Navigatore spiega il motivo una sola volta e restituisce il controllo a Luca.
3. La marcia automatica procede normalmente: il sistema resta discreto e non trasforma il tragitto in una cronaca vocale.
4. Il Navigatore deve arrestarsi perché non può proseguire: l'avviso domina le descrizioni ambientali e lascia Luca libero di decidere cosa fare.
5. Luca riprende manualmente il controllo o raggiunge la meta: il sistema riconosce subito il nuovo stato e non continua a parlare come se la marcia fosse ancora attiva.

### 14.6 Confini della Fase 5

La Fase 5 riguarda soltanto l'ordine e la chiarezza dei messaggi del Navigatore e dell'Auto-Walk. Mentore, Accademia, scanner direzionale con Pagina Su e Pagina Giù, protezioni già affidabili, mirino e suoni spaziali non vengono ridefiniti.

Non si introducono nuove abitudini di gioco prima di avere dimostrato che le informazioni già esistenti sono ordinate, comprensibili e non intrusive.

### 14.7 Criteri per passare al piano tecnico

La strategia sarà considerata consolidata quando Luca confermerà questi cinque risultati percepibili:

1. nessun pericolo viene ritardato dal Navigatore;
2. ogni avvio, arresto, annullamento e arrivo è chiaro, breve e univoco;
3. una meta assente, troppo lontana o non raggiungibile riceve una spiegazione utile senza ripetizioni;
4. la ripresa manuale del controllo è immediata e non lascia messaggi residui;
5. le descrizioni ambientali non scavalcano mai un cambiamento importante della navigazione.
