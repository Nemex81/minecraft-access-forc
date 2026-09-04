# Piano Tecnico Implementativo — Cognitive Coordinator

**Riferimento strategico:** `docs/strategie/STRATEGIA_COGNITIVE_COORDINATOR.md`  
**Stato:** `[SOTTO-FASE 1A — PIANO DA CONVALIDARE]`  
**Ramo previsto:** `feat/cognitive-orchestrator`, creato da `mymaster` solo dopo conferma operativa di Luca  
**Incremento AVF proposto:** `v26.2-1.19.0` — da confermare prima dell'implementazione  
**Ambito:** Refactor client-side della gestione cognitiva di segnali ambientali, operativi e guidati.  
**Esclusioni:** Nessuna modifica in questa fase a sorgenti Java, mixin, configurazioni, localizzazioni, build, JAR, PrismLauncher o `mymaster`.

---

## 1. Obiettivo operativo

Creare un coordinatore cognitivo centrale che riceva eventi già interpretati dai sottogestori di dominio, ne governi priorità, validità temporale, deduplicazione, concatenazione e canale di uscita, quindi invii il risultato tramite l'astrazione esistente `MainClass.narrate` e i suoni 3D della mod.

Il refactor non deve sostituire i rilevamenti voxel, raycast, entità o inventario. Ogni modulo continua a rilevare e formattare le informazioni del proprio dominio; il nuovo livello decide esclusivamente se, quando e in quale forma l'utente le riceve.

## 2. Perimetro e confini architetturali

### Inclusi nella prima migrazione

| Dominio | Produttori attuali | Risultato atteso |
|---|---|---|
| Sicurezza | `FallDetector`, `ObstacleDetector`, sentinella ostili in `POIEntities` | Allarmi critici immediati, avvisi di percorso ordinati, ritorno alla sicurezza dichiarato dal dominio competente. |
| Esplorazione | `NarrateCrosshair`, `CrosshairFeedbackManager`, scanner direzionale, POI | Mirino e ostacoli compatibili concatenati; eventi passivi differiti o scartati se inattuali. |
| Movimento | `AutoWalkManager` e `AutoWalkController` | Avvio, blocco, avanzamento, arrivo e annullamento coerenti con sicurezza e mirino. |
| Guida | `ContextualMentor`, `AcademyManager`, `HelpNarrator` | Istruzioni protette, preemptibili da pericoli reali e riprendibili o ripetibili dopo l'interruzione. |

### Esplicitamente esclusi dalla prima migrazione

- Navigazione immediata di inventari, ricettari, chat, campi di testo, Cloth Config e menu.
- Lettura richiesta direttamente dall'utente tramite tasti, compresi coordinate e controlli Numpad.
- Refactor completo delle oltre 180 chiamate esistenti a `MainClass.narrate`.
- Modifiche ai mixin non coinvolti dal percorso cognitivo ambientale.

Questi flussi mantengono la latenza diretta. Il loro rapporto con l'ambiente sarà gestito soltanto tramite il token temporaneo descritto nella sezione 7.

## 3. Architettura prevista

```text
Sensori tecnici
       ↓
Sottogestori: Safety / Exploration / Movement / Status / Guidance
       ↓  CognitiveEvent
CognitiveCoordinator
  ├─ Fast-path CRITICAL
  ├─ buffer e flush a fine tick
  ├─ deduplicazione e TTL
  ├─ concatenazione I18N
  ├─ memoria attentiva breve
  └─ gestione soppressioni
       ↓
MainClass.narrate + cue audio 3D della mod
```

### Package proposto

`org.mcaccess.minecraftaccess.features.cognitive`

Il package contiene solo orchestrazione, contratti dati e policy. Non deve conoscere la geometria voxel né duplicare logiche già presenti in `ObstacleDetectionUtils`, `FallDetector`, `PathRaycaster` o `CrosshairFeedbackManager`.

## 4. Contratti da introdurre

| Tipo | Responsabilità | Vincoli |
|---|---|---|
| `CognitiveEvent` | Evento immutabile prodotto da un sottogestore. | Dominio, priorità, chiave semantica, firma di stato, testo già localizzato, posizione opzionale, distanza, output, cue, TTL, interrompibilità e concatenabilità. |
| `CognitivePriority` | Ordinamento: `CRITICAL`, `OPERATIONAL`, `CONTEXTUAL`, `PASSIVE`. | Nessun valore inferiore può interrompere un valore superiore. |
| `SourceDomain` | Provenienza: `SAFETY`, `EXPLORATION`, `MOVEMENT`, `STATUS`, `GUIDANCE`, `INTERFACE`. | `INTERFACE` è inizialmente escluso dal buffer ambientale. |
| `StateSignature` | Descrive variazioni rilevanti per il dominio. | Include almeno bucket distanza, gravità e ID bersaglio quando applicabili. Non deriva dal testo localizzato. |
| `SoundCue` | Dati per un suono della mod. | Evento, sorgente, posizione, volume e pitch già autorizzati dalla configurazione del produttore. |
| `CognitiveCoordinator` | Unico arbitro per gli eventi dei moduli già migrati. | È confinato al client thread, non effettua scansioni del mondo e non costruisce testi di dominio. |
| `DirectInteractionShield` | Token temporaneo per interazione GUI/tastiera diretta. | Può differire solo eventi `CONTEXTUAL` e `PASSIVE`; non ritarda mai `CRITICAL` né annulla una richiesta esplicita. |

`CognitiveEvent` trasporta testo localizzato già pronto per NVDA. La composizione tra due eventi usa template I18N nuovi, non concatenazione grezza di stringhe.

## 5. Politica di arbitraggio

### Fast-path critico

1. Un evento `CRITICAL` non entra nel buffer di fine tick.
2. Il primo evento critico del tick usa `MainClass.narrate(testo, true)` e abilita lo scudo contro gli eventi ambientali inferiori.
3. Un secondo evento critico concorrente, se non obsoleto, usa testo ultra-breve e `MainClass.narrate(testoBreve, false)`; non può troncare il primo.
4. I cue relativi restano possibili, ma il ducking riguarda solo suoni secondari prodotti da Minecraft Access.
5. L'escalation della stessa minaccia supera la deduplicazione se cambia `StateSignature` o priorità.

### Flush non critico a fine tick

Il coordinatore si registra tramite `ClientPlayingTick.AFTER`, l'evento già adottato dal progetto. Per ogni tick:

1. rimuove eventi oltre TTL e duplicati;
2. ordina per priorità, richiesta esplicita e tempo di ricezione;
3. elimina passivi incompatibili con sicurezza, guida o interazione diretta;
4. fonde al massimo due eventi compatibili secondo template I18N;
5. emette l'evento dominante; conserva soltanto un eventuale secondo evento valido in una coda corta con TTL, invece di perderlo;
6. svuota il buffer del tick.

Se `chainedNarrationEnabled` è disattivato, gli eventi compatibili non sono fusi: il secondo resta nella coda breve ed è emesso solo se non scade o non viene superato da un evento più importante.

## 6. Deduplicazione, memoria e stato di sicurezza

La chiave di deduplicazione è:

`(domain, semanticKey, targetPos, stateSignature, priority)`.

La memoria attentiva contiene al massimo otto voci e conserva:

- ultimo pericolo attivo, gravità, posizione e timestamp;
- ultimo bersaglio stabile del mirino;
- ultime chiavi deduplicate e relativo momento di emissione;
- coda breve non critica;
- stato dello scudo della guida e dell'interazione diretta.

Il messaggio **"Percorso libero"** non è inferito dal coordinatore. Viene prodotto solo quando il sottogestore responsabile dichiara il rientro: `FallDetector` per il bordo, `ObstacleDetector` per l'ostacolo e `AutoWalkManager` per la rotta automatica. La conferma richiede stabilità sicura di almeno 300 ms.

`clearAllBuffers()` deve essere invocato in modo esplicito su disconnessione/cambio server, cambio mondo o dimensione, morte e respawn. Il piano di implementazione deve individuare gli hook client concreti; `ServerChangeDetector` da solo copre soltanto una parte di queste transizioni.

## 7. Configurazione e compatibilità

### Regola "Filtro a monte, arbitraggio a valle"

Ogni produttore continua a rispettare integralmente la propria configurazione esistente prima di inviare l'evento:

- `FallDetector`: `enabled`, `voiceWarning`, `playAudioCues`, volume e `edgeBumpFeedbackMode`;
- `ObstacleDetector`: `enabled`, voce, cue, volume, range, ritardo, direzione e `narrationStyle` effettivo;
- `NarrateCrosshair`: abilitazione, ordine lettura, feedback in movimento, debounce, filtro e cue relativi;
- `AutoWalk`: `maxRange`, auto-jump, auto-swim, arresto su input manuale, feedback vocale e cue nodo;
- `DirectionalPathScanner`: range, rilevamenti selezionati, verbosità e modalità audio;
- guida: `mentorEnabled`, `autoAdvanceMissions`, `helpPriorityOverride`.

Se sia voce sia suono sono disabilitati per un rilevatore, esso non invia alcun evento. Il coordinatore non deve riattivare un canale che l'utente ha disabilitato.

### Nuova categoria `CognitiveSettings`

| Campo | Default | Funzione |
|---|---:|---|
| `cognitiveCoordinatorEnabled` | `true` | Per i soli moduli migrati, abilita il coordinatore; se falso, ripristina il comportamento diretto storico. |
| `chainedNarrationEnabled` | `true` | Autorizza la fusione I18N di massimo due eventi compatibili. |
| `ambientSpeechDensity` | `EQUILIBRATA` | Filtra eventi ambientali non critici: `MINIMA`, `EQUILIBRATA`, `COMPLETA`. |
| `criticalModAudioDucking` | `true` | Attenua o silenzia temporaneamente i cue secondari generati dalla mod, mai NVDA o l'audio di sistema. |
| `deduplicationWindowMs` | `1500` | Riduce ripetizioni di uno stato identico; non blocca escalation di distanza o gravità. |

Le chiavi IT/EN devono essere aggiunte in ordine alfabetico. Nuove impostazioni di frequenza o verbosità per il Mentore richiedono una decisione separata: non sono opzioni già presenti.

### Facciata `NarrationPriority`

`NarrationPriority` resta disponibile come Adapter/Facade. Le sue API pubbliche mantengono comportamento compatibile e inoltrano al coordinatore quando attivo; se disabilitato, conservano la soppressione storica di mirino e ostacoli. Ciò tutela `ToastManagerMixin`, `ClientPacketListenerMixin` e ogni chiamante non ancora migrato.

### Interazione diretta

Le GUI e i tasti a risposta immediata continuano a usare `MainClass.narrate` direttamente. Prima dell'emissione diretta essi acquisiscono o rinnovano `DirectInteractionShield`; alla sua scadenza l'ambiente torna disponibile. Gli eventi critici bypassano sempre il token.

## 8. Piano di migrazione

1. **Nucleo silenzioso** — contratti, coordinatore, buffer, fast-path, memoria, token e test puri. Nessun produttore migrato.
2. **Configurazione e facciata** — `CognitiveSettings`, localizzazioni IT/EN e Adapter `NarrationPriority`, con fallback verificabile.
3. **Pilota Sicurezza** — `FallDetector` e `ObstacleDetector`; collaudo NVDA mirato su bordi, dislivelli, scala, lava e oscillazioni.
4. **Esplorazione** — `CrosshairFeedbackManager`, `NarrateCrosshair`, POI/sentinella e scanner direzionale; prova concatenazione ostacolo + mirino.
5. **Movimento e guida** — auto-walk, Mentore e Accademia; preemption della guida e continuità post-pericolo.
6. **Stabilizzazione** — test concorrenti, rapporto PRAPI, build e deploy di prova; collaudo manuale NVDA. Il merge richiede consenso esplicito di Luca.

Ogni fase deve essere compilabile e reversibile indipendentemente. Finché un modulo non è migrato conserva il percorso diretto esistente.

## 9. Test automatici previsti

Nuovi test JUnit puri, senza avviare Minecraft:

- ordinamento totale delle quattro priorità;
- primo e secondo critico nello stesso tick (`true` poi `false`);
- TTL e svuotamento buffer;
- deduplicazione di stato identico e rilascio su escalation;
- limite massimo di due eventi concatenati;
- incompatibilità spaziale e di priorità;
- coda breve quando la concatenazione è disattivata;
- `DirectInteractionShield` che differisce passivi ma non critici;
- fallback con `cognitiveCoordinatorEnabled = false`;
- reset della memoria e assenza di eventi fantasma.

Test di integrazione e collaudo in-game successivi:

- burrone + danno + mirino;
- auto-walk + ostacolo + guida;
- apertura inventario durante avviso contestuale;
- oscillazione sul bordo con audio-only debounced;
- cambio mondo, dimensione, morte, respawn e perdita focus finestra;
- lettore vocale indisponibile, con cue 3D della mod ancora funzionanti.

La base di test esistente include già JUnit e test per `ObstacleDetector`, `CrosshairFeedbackManager`, AutoWalk e scanner direzionale; i nuovi test devono riusare lo stesso stile di mock dove possibile.

## 10. Validazione ASTRALIS — 7 assi

1. **Validità:** codice comune client-side, compatibile con Fabric, NeoForge, Balm e Java 25; uso di `ClientPlayingTick.AFTER` e `MainClass.narrate`.
2. **Efficacia:** obiettivo misurabile: nessun troncamento vocale nei casi concorrenti della matrice NVDA.
3. **Coerenza:** nessuna duplicazione di geometria o formattazione; riuso di `NarrationPriority`, `CrosshairFeedbackManager` e `PlayerContextEngine` nei rispettivi ruoli.
4. **Completezza:** copertura di sicurezza, esplorazione, movimento e guida, con confini espliciti per UI e input diretto.
5. **Precisione:** firma di stato, posizione e TTL impediscono sia spam sia perdita di escalation.
6. **Affidabilità e prestazioni:** nessuna scansione aggiuntiva del mondo; buffer a dimensione limitata; target da misurare in telemetria inferiore a 0,15 ms/tick a regime ordinario.
7. **Assenza di regressioni:** fallback per modulo migrato, Facade retrocompatibile e collaudo NVDA prima di ogni promozione.

## 11. Criteri di accettazione della Fase 1B

L'implementazione può iniziare soltanto dopo il via libera esplicito di Luca. Sarà accettata per la fase iniziale quando:

- il ramo dedicato esiste ed è derivato dal commit corrente di `mymaster`;
- il nucleo è testato senza migrare sensori;
- nessuna chiamata diretta dei moduli non migrati cambia comportamento;
- `NarrationPriority` continua a proteggere toast e pacchetti già esistenti;
- le configurazioni IT/EN sono ordinate e accessibili da tastiera;
- build e test automatici passano con Java 25 e `--no-daemon`;
- il JAR è distribuito solo dopo build riuscita e secondo il flusso PrismLauncher previsto;
- il merge su `mymaster` avviene soltanto dopo collaudo NVDA positivo e consenso esplicito.

## 12. Checkpoint obbligatorio

Questo piano conclude la Sotto-Fase 1A. Non autorizza la creazione del ramo, modifiche di codice, configurazione, localizzazione, build, test, deploy o merge.

Per passare alla Sotto-Fase 1B è necessario un comando esplicito di Luca, ad esempio: **"Approvo il piano: crea il ramo dedicato e procedi con l'implementazione."**
