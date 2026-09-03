# Rapporto di chiusura Fase 1 e indirizzo operativo per il piano della Fase 2

**Destinatario:** Antigravity  
**Ramo di lavoro:** `feat/cognitive-orchestrator`  
**Stato:** Fase 1 completata e verificata; Fase 2 autorizzata solo per la pianificazione tecnica  
**Data:** 3 settembre 2026

---

## 1. Esito della revisione della Fase 1

La revisione finale del codice dà esito **positivo**: la Fase 1 del Cognitive Coordinator è completa rispetto al piano validato.

Il nucleo cognitivo dispone dei contratti dati, del fast-path per eventi `CRITICAL`, del buffer a fine tick, della deduplicazione, del TTL, della coda breve e della concatenazione vincolata a template I18N. I 14 test unitari includono anche la correzione finale: se una concatenazione compatibile non ha un template disponibile, il secondario `PASSIVE` resta in coda ed è pronunciato solo al tick successivo se non è scaduto.

Sono inoltre confermati i seguenti vincoli:

- nessun fallback vocale hardcoded per la concatenazione;
- nessuna migrazione prematura di `FallDetector`, `ObstacleDetector`, mirino o altri sensori;
- nessuna regressione progettuale del percorso diretto legacy;
- il primo critico interrompe, i critici concorrenti successivi sono accodati senza troncare la frase già avviata;
- la coda e i buffer rispettano il TTL anche nel caso di fallback I18N.

Riferimenti verificati:

- `src/main/java/org/mcaccess/minecraftaccess/features/cognitive/CognitiveCoordinator.java`;
- `src/test/java/org/mcaccess/minecraftaccess/features/cognitive/CognitiveCoordinatorTest.java`;
- `docs/report/RAPPORTO_MICROCORREZIONE_F1_1_CODA_I18N.md`.

La Fase 1 è quindi chiusa sul piano tecnico. Questo non autorizza il merge in `mymaster`: ogni avanzamento resta confinato nel ramo dedicato e soggetto a revisione e consenso esplicito di Luca.

---

## 2. Mandato ad Antigravity

Non serve una nuova strategia generale: quella in `docs/strategie/STRATEGIA_COGNITIVE_COORDINATOR.md` rimane il riferimento architetturale. Il prossimo deliverable richiesto ad Antigravity è il **piano tecnico implementativo completo della sola Fase 2**, da sottoporre a revisione prima di modificare qualsiasi file di produzione.

Il piano deve essere operativo, verificabile e limitato a:

1. categoria di configurazione cognitiva in `Config.java`;
2. chiavi I18N IT/EN e presentazione accessibile nella schermata Cloth Config;
3. trasformazione di `NarrationPriority` in facciata/adapter retrocompatibile;
4. collegamento affidabile tra configurazione persistita e parametri già esposti dal `CognitiveCoordinator`;
5. test automatici e verifica di non-regressione.

**Non implementare ancora il piano.** Non sono consentite in Fase 2 modifiche a sensori, rilevatori, mixin, `MainClass.narrate`, AutoWalk, mirino, POI, Mentore o Accademia. La prima migrazione di produttori appartiene alla Fase 3.

---

## 3. Strategia logica vincolante della Fase 2

### 3.1 Obiettivo

Rendere il nucleo cognitivo controllabile dall’utente e introdurre un punto di compatibilità per le chiamate storiche a `NarrationPriority`, senza cambiare l’esperienza effettiva dei produttori legacy.

La Fase 2 non deve aumentare la copertura funzionale del coordinatore: deve solo preparare una base configurabile, reversibile e trasparente per le migrazioni future.

### 3.2 Principio guida: compatibilità prima dell’adozione

`NarrationPriority` è oggi un piccolo scudo sincrono che chiama direttamente `NarrateCrosshair.suppressNarration(...)`, `ObstacleDetector.suppressWarnings(...)` e `MainClass.narrate(...)`. Nella Fase 2 non va eliminato né ne va cambiata l’API pubblica.

La facciata deve mantenere i metodi esistenti, le loro firme e la semantica osservabile dai chiamanti legacy. Quando il coordinatore è attivo, la facciata può aggiornare il suo stato di protezione e delegare soltanto le responsabilità già esistenti; quando il coordinatore è disattivato, il comportamento storico deve restare disponibile senza dipendere da produttori già migrati.

Nessuna chiamata diretta esistente deve diventare implicitamente un `CognitiveEvent` in questa fase: ciò trasformerebbe una preparazione di compatibilità in una migrazione non autorizzata.

### 3.3 Configurazione: filtro dell’utente, non nuova logica di rilevamento

In `Config.java` introdurre una categoria transitive dedicata, ad esempio `cognitiveCoordinator`, collocata in modo coerente con le categorie esistenti. La configurazione deve esporre soltanto impostazioni che il nucleo attuale può rispettare davvero:

| Campo | Default | Effetto richiesto |
|---|---:|---|
| `cognitiveCoordinatorEnabled` | `true` | Attiva il coordinatore per i soli moduli che lo useranno in futuro; se falso, la facciata conserva il percorso legacy. |
| `chainedNarrationEnabled` | `true` | Abilita/disabilita la fusione I18N di massimo due eventi compatibili. |
| `ambientSpeechDensity` | `EQUILIBRATA` | Preferenza dichiarata e stabile; nessun sensore deve essere filtrato finché non sarà migrato in Fase 3 o oltre. |
| `criticalModAudioDucking` | `true` | Governa solo audio secondario generato dal mod durante eventi critici; mai NVDA o audio di sistema. |
| `deduplicationWindowMs` | `1500` | Valore limitato e validato che alimenta la deduplicazione del coordinatore. |

L’enum `ambientSpeechDensity` deve avere valori espliciti `MINIMA`, `EQUILIBRATA`, `COMPLETA`, testo localizzato e default stabile. Non introdurre opzioni future o non applicabili (frequenze del Mentore, soglie dei sensori, politiche GUI) solo per anticipazione.

Il piano deve specificare il punto unico in cui i valori persistiti vengono applicati ai setter del coordinatore all’avvio e dopo il salvataggio della configurazione. Non sono accettabili valori di runtime scollegati dalla configurazione, né letture sparse di `Config` distribuite nel coordinatore.

### 3.4 I18N e accessibilità della configurazione

Tutte le chiavi di categoria, opzione, descrizione ed enum devono essere presenti sia in `it_it.json` sia in `en_us.json`, con nomi brevi e inequivocabili per lettura NVDA. I due file restano ordinati alfabeticamente secondo la convenzione del repository.

Il piano deve verificare che Cloth Config presenti una categoria navigabile da tastiera e che i controlli enum siano configurati con il handler già adottato nel progetto. Le descrizioni devono dichiarare chiaramente l’effetto, soprattutto per “coordinatore disattivato”, “densità ambientale” e ducking; non devono promettere migrazioni o comportamenti che la Fase 2 non realizza.

### 3.5 Facciata `NarrationPriority`: separare responsabilità e preservare sicurezza

Il piano tecnico deve definire la facciata con questa ripartizione:

- **API legacy invariata:** `suppressBackgroundScanners`, `isShieldActive`, `narrateSalient`, `narrateSalientQueued` restano utilizzabili senza modifiche dai chiamanti esistenti.
- **Scudo locale conservato:** fino alla Fase 3 le chiamate a `NarrateCrosshair.suppressNarration` e `ObstacleDetector.suppressWarnings` restano necessarie per proteggere produttori non migrati.
- **Coordinatore configurato:** la facciata e/o il bootstrap applicano le preferenze configurate al coordinatore senza registrare produttori né generare eventi artificiali.
- **Fallback deterministico:** con coordinatore disabilitato, i metodi salienti mantengono la voce diretta e lo scudo legacy; non devono diventare silenziosi o introdurre ritardi.
- **Nessuna doppia emissione:** nessun percorso della facciata deve far parlare sia `MainClass.narrate` sia il coordinatore per lo stesso messaggio.

L’eventuale uso di `DirectInteractionShield` va soltanto preparato come API o punto di integrazione se già indispensabile alla facciata; non va collegato a GUI, tastiera o mixin in Fase 2. La disciplina resta: input esplicito e GUI mantengono la loro latenza diretta; un futuro shield non può mai bloccare un `CRITICAL`.

### 3.6 Confini espliciti e assenza di regressioni

Il piano deve elencare file da toccare e ragione di ciascuno. L’elenco atteso è ristretto a configurazione, bootstrap/config binding, `NarrationPriority`, localizzazioni e test; eventuali file supplementari devono essere motivati prima dell’implementazione.

Restano esclusi:

- produzione di `CognitiveEvent` da sensori e sottogestori;
- modifica dei calcoli voxel/raycast, del debounce specifico dei sensori o delle loro opzioni attuali;
- modifica di `MainClass.narrate` e delle integrazioni Tolk/NVDA;
- modifiche a mixin, deploy, PrismLauncher, artefatti JAR e `mymaster`.

### 3.7 Sequenza sicura della Fase 2

Il piano tecnico deve articolare il lavoro in blocchi indipendenti e reversibili:

1. mappatura degli attuali punti di bootstrap di `Config`, della schermata Cloth Config e dei chiamanti di `NarrationPriority`;
2. modello `CognitiveSettings`, default, vincoli numerici e migrazione non distruttiva di configurazioni esistenti;
3. localizzazioni IT/EN ordinate e controllo di navigabilità/accessibilità;
4. adapter `NarrationPriority` con comportamento legacy verificabile sia a coordinatore attivo sia disattivo;
5. binding centralizzato Config → Coordinator, senza dipendenze dal mondo/client in test unitari;
6. test mirati, build e revisione del diff; nessun collaudo NVDA è sostitutivo dei test automatici.

Ogni blocco deve avere criteri di accettazione, test associati, rischio, rollback e una definizione chiara di “nessuna modifica ai produttori”.

---

## 4. Requisiti minimi del piano tecnico che Antigravity deve consegnare

Il piano proposto deve contenere:

1. analisi dei file e delle API realmente esistenti, senza inventare classi o chiavi di configurazione;
2. elenco file-per-file con responsabilità, modifiche previste e invarianti;
3. schema del binding Config → Coordinator e ciclo di aggiornamento dopo salvataggio;
4. contratto di compatibilità di ogni metodo pubblico di `NarrationPriority`;
5. matrice di test che copra default, persistenza, disable/fallback, scudo legacy, assenza di doppia voce, limite/validazione della finestra di deduplicazione e I18N IT/EN;
6. controlli di compilazione con Java 25 e `--no-daemon`, senza deploy o merge;
7. criteri di accettazione e rollback per la Fase 2;
8. una sezione finale che dichiari in modo verificabile che nessun sensore è migrato.

Il piano non deve autorizzare di per sé modifiche: dopo la sua redazione sarà sottoposto a revisione di Codex e quindi alla conferma esplicita di Luca.

---

## 5. Decisione richiesta dopo la pianificazione

Antigravity deve ora produrre il piano tecnico della Fase 2 e fermarsi. Solo dopo revisione positiva e un comando esplicito di Luca — ad esempio: **“Approvo il piano della Fase 2: procedi sul ramo dedicato”** — potrà iniziare l’implementazione.

