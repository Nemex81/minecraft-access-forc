# Standard Operativi: Piani Tecnici, Verifiche, Rapporti e Accessibilità per Screen Reader

Questa scheda stabilisce le regole metodologiche, comportamentali e operative che Antigravity deve applicare rigorosamente in ogni fase di analisi, pianificazione, verifica, implementazione e collaudo del progetto Minecraft Access.

---

## 1. Accessibilità Cognitiva & Formattazione Lineare per Screen Reader (Regola Inviolabile)

Poiché lo sviluppo e il collaudo avvengono con uno sviluppatore e giocatore totalmente non vedente che utilizza sintesi vocale (NVDA / SAPI):
1. **Divieto Assoluto di Diagrammi Grafici o Disegni 2D**:
   - Mai inserire schemi ASCII art, diagrammi con frecce visuali multidirezionali, diagrammi Mermaid grafici o immagini per spiegare logiche o geometrie.
   - La sintesi vocale legge riga per riga dall'alto in basso: i disegni 2D risultano spezzati, confusi e inaccessibili.
2. **Formattazione Semantica Sequenziale**:
   - Tutte le logiche decisionali devono essere scritte in modo sequenziale con elenchi puntati o numerati e formule logiche esplicite:
     - *Se [Condizione A] -> Allora [Esito A]*
     - *Altrimenti se [Condizione B] -> Allora [Esito B]*
3. **Descrizione Verbale e Vettoriale della Geometria 3D**:
   - Le posizioni spaziali dei blocchi devono essere sempre espresse in modo verbale e numerico chiaro (es. "quota piedi Y", "quota occhi Y+1", "spazio salto Y+2", "blocco frontale", "vettore di direzione").
4. **Tabelle Markdown Semplici**:
   - Le tabelle devono essere semantiche, con intestazioni di colonna chiare e lineari, prive di celle vuote o caratteri grafici decorativi, per consentire la navigazione cella per cella con i comandi da tastiera dello screen reader (es. Ctrl + Alt + Frecce).

---

## 2. Ciclo di Vita dei Piani Tecnici & Cartella Archivio

1. **Sede Unica dei Piani di Sviluppo Attivi**:
   - Tutti i piani tecnici in lavorazione o in attesa di collaudo risiedono in:
     📁 `C:\Users\nemex\OneDrive\Documenti\GitHub\minecraft-access\docs\piani\attivi\`
2. **Regola dei Soli Piani Attivi**:
   - Nella cartella dei piani attivi devono essere presenti **esclusivamente** i piani tecnici implementativi attualmente in fase di definizione o in lavorazione (da implementare o in attesa di collaudo manuale).
3. **Archiviazione Post-Collaudo Manuale Utente**:
   - Un piano tecnico NON viene archiviato automaticamente dopo la compilazione o il deploy di prova.
   - Viene spostato nella sottocartella `docs\piani\completati\` **esclusivamente dopo che Luca ha effettuato il test manuale in-game di persona aprendo il gioco e confermato formalmente il superamento del collaudo**.
4. **Organizzazione Documentale in `docs/`**:
   - `docs\strategie\attive\`: Strategie logico-cognitive in elaborazione o discussione (Fase 0).
   - `docs\strategie\archiviate\`: Strategie convalidate, convertite in piani tecnici o assimilate.
   - `docs\piani\attivi\`: Piani tecnici attivi in stesura o lavorazione (Fase 1).
   - `docs\piani\completati\`: Piani tecnici collaudati con successo e integrati.
   - `docs\report\`: Registro Revisioni attivo (`REGISTRO_REVISIONI.md`) e storico (`ARCHIVIO_REVISIONI.md`).
   - `docs\report\archivio\`: Report di sessione e telemetria storici normalizzati URCP.
   - `docs\idee\`: Promemoria, spunti futuri e meccaniche da esplorare.
   - `docs\manuali\`: Manuali d'uso e guide comandi in-game.

5. **I 7 Archetipi dei Piani Tecnici ASTRALIS (v2.7.1)**:
   - Ogni piano tecnico appartiene a uno dei 7 archetipi deterministici:
     * **Implementativo** *(Feature & New Modules)*: nuove funzionalità, architettura a layer, contratti denominati (D0..DN, S1..SN) e localizzazione I18N ordinata;
     * **Correttivo** *(Bug Fix & PRAPI Mirato)*: evidenza empirica, Root Cause Analysis (RCA) senza pezze euristiche (Cancello 1), test di riproduzione e patch chirurgica;
     * **Refactoring** *(Architectural Grooming)*: pulizia debito tecnico, disaccoppiamento interfacce/DIP e garanzia di **invarianza assoluta del comportamento esterno**;
     * **Bonifica & Pulizia** *(Dead Code Purge)*: eliminazione codice/asset morti con la Strategia a 5 Barriere di Sicurezza (Protocollo 11);
     * **Migrazione & Aggiornamento Stack** *(Runtime Upgrade)*: avanzamento versione runtime/motore (Java, Minecraft, Fabric, Python), breaking changes e compatibilità binaria;
     * **Convalida, Hardening & Suite Test** *(QA Engineering)*: test seams headless a 0 ms (Cancello 5), eliminazione `Thread.sleep` e matrici di stress-test;
     * **Esplorativo & Fattibilità** *(Spike & PoC)*: benchmark preventivi su incertezze complesse (es. analisi chunk MCA) e dossier decisionale per il piano implementativo.

6. **Intestazione a 8 Campi & Sommario Operativo con Gating di Convalida**:
   - *Intestazione Standard*: Titolo con ID, Tipologia, Autore, Revisori, Data e Ora, Stato Operativo (`[IN STESURA 1A]`, `[APPROVATO 1B]`, `[COMPLETATO]`), Target Version AVF, Documenti Correlati e Audit dei 5 Cancelli (Protocollo 12);
   - *Sommario Operativo & Registro di Avanzamento (Checklist)*: collocato subito in cima come Sezione 0 per consentire l'atterraggio istantaneo con tasto `H` in NVDA;
   - *La Matrice a 3 Stati per NVDA*:
     * `- [ ] [DA AVVIARE]`: Attività pianificata ma non iniziata;
     * `- [/] [IMPLEMENTATO — IN ATTESA DI CONVALIDA]`: Spunta parziale. Codice scritto e compilato, ma **NON ancora convalidato**;
     * `- [x] [CONVALIDATO CON SUCCESSO]`: Spunta definitiva concessa **esclusivamente POST-CONVALIDA formale** (approvazione di Luca per la 1A, test suite 100% verde per la 1B, collaudo empirico in-game di Luca per la Fase 2).

---


## 3. Protocollo di Analisi Preliminare & Prevenzione Falsi Positivi

Prima di scrivere una sola riga di codice o di proporre modifiche:
1. **Analisi Spaziale e Fisica di Minecraft**:
   - Analizzare accuratamente la bounding box del giocatore (0.6 x 1.8 x 0.6) e le altezze di salto.
   - Distinguere sempre tra blocchi d'aria, blocchi attraversabili privi di collisione solida (erba alta, fiori, torce, cartelli, polvere di redstone) e blocchi con collisione solida reale (`!state.getCollisionShape(level, pos).isEmpty()`).
2. **Verifica delle Luci Libere (Clearance)**:
   - Verificare sia lo spazio verticale sopra l'ostacolo di destinazione (2 blocchi d'aria), sia lo spazio verticale sopra la testa del giocatore nella posizione attuale per evitare tentativi di salto impossibili (es. soffitti bassi di caverne o gallerie 1x2).
3. **Spiegazione e Allineamento con l'Utente**:
   - Presentare all'utente tutte le casistiche e le simulazioni teoriche in formato testuale lineare e attendere la sua conferma.

---

## 4. Verifica Preventiva di Compatibilità e Tasti

1. **Scansione Tasti Esistenti**:
   - Prima di proporre una combinazione di tasti per una nuova funzione, eseguire una ricerca nel codice per verificare se la combinazione (tasto + modificatori Shift, Alt, Ctrl) è già occupata in Minecraft vanilla o in altri moduli di Minecraft Access.
2. **Ergonomia, Simmetria e Non-Interferenza Posturale**:
   - Evitare l'uso di `Shift Sinistro` per comandi nel mondo aperto (per prevenire l'accovacciamento/sneak involontario).
   - Raggruppare i comandi per famiglie logiche omogenee (es. Famiglia `Home/End` per i POI, Famiglia `V` per posizione e vista).

---

## 5. Protocollo di Verifica e Validazione Multidimensionale (7 Assi)

Ogni piano o soluzione tecnica deve essere convalidato rispetto a 7 criteri:
1. **Validità**: Rispetto rigoroso dei framework di Minecraft 1.21.x / Fabric / NeoForge / Balm / Kuma.
2. **Efficacia**: Risoluzione reale e tangibile del problema di accessibilità.
3. **Coerenza**: Piena integrazione con gli altri moduli (ObjectTracker, FallDetector, LockingHandler, AccessMenu).
4. **Completezza**: Gestione di tutti i casi limite, opzioni di configurazione e testi esplicativi.
5. **Precisione**: Calcoli geometrici esatti, assenza di falsi allarmi o spam sonoro/vocale.
6. **Affidabilità & Prestazioni**: Zero lag, algoritmi leggeri, debounce/cooldown temporale.
7. **Assenza di Regressioni & Zero Sovraingegnerizzazione**: Non intaccare le funzioni preesistenti ed evitare complessità architetturali non necessarie.

---

## 6. Rigore I18N & Focus su Italiano e Inglese

1. **Zero Stringhe Hardcoded**:
   - Qualsiasi messaggio, etichetta GUI, avviso vocale o opzione di configurazione deve utilizzare chiavi di traduzione (`Component.translatable` o `I18n.get`).
2. **Focus sulle Due Lingue Primarie**:
   - In fase di sviluppo e manutenzione nel fork locale, ci concentriamo su **Italiano (`it_it.json`)** (lingua madre dell'utente) e **Inglese (`en_us.json`)** (standard universale per PR upstream). Le restanti lingue vengono gestite dalla community ufficiale di Minecraft Access su Crowdin.
3. **Ordinamento Alfabetico JSON Tassativo**:
   - Tutte le chiavi nei file `.json` devono essere rigorosamente ordinate in ordine alfabetico crescente per superare la CI di GitHub.

---

## 7. Pipeline Ufficiale a 4 Fasi (Pianificazione, Esecuzione, Deploy, Chiusura e Auto-Apprendimento)

La conclusione di ogni sessione implementativa segue tassativamente una sequenza a 4 fasi:

### Fase 1: Pianificazione Formale, Esecuzione Tecnica & Test Automatici (Disaccoppiamento 1A / 1B)
1. **Sotto-Fase 1A (Pianificazione & Checkpoint di Stop Obbligatorio)**:
   - Redazione del piano tecnico implementativo in `docs/piani/attivi/` e nell'artifact `implementation_plan.md`.
   - **DIVIETO ASSOLUTO DI SCRITTURA CODICE**: L'assistente deve tassativamente fermarsi, presentare il piano a Luca e attendere la convalida esplicita prima di modificare file sorgenti o configurazioni.
2. **Sotto-Fase 1B (Esecuzione Tecnica & Test - Solo post-convalida piano)**:
   - *Pre-Flight Environment Check*: Verifica preliminare di conformità dell'ambiente (JDK 25, `$env:JAVA_HOME` e flag `--no-daemon` per evitare blocchi file di OneDrive).
   - *Modifiche Sorgenti & I18N*: Editing del codice e verifica dell'ordinamento alfabetico JSON (`jq -e "keys != keys_unsorted"`).
   - *Verifica Compilazione*: Esecuzione di `.\gradlew.bat --no-daemon compileJava compileTestJava`.
   - *Esecuzione Test Unitari*: Esecuzione di `.\gradlew.bat --no-daemon :test` per confermare che tutti i test passino al 100%.
   - *Generazione Pacchetto JAR*: Esecuzione di `.\gradlew.bat --no-daemon shadowJar`.

### Fase 2: Deploy di Prova e Collaudo Manuale Utente
3. **Deploy Provvisorio nelle Istanze**: Copia e sovrascrittura del file `.jar` appena compilato nelle cartelle `mods/` delle istanze PrismLauncher attive del giocatore (per consentire l'apertura del gioco).
4. **Rapporto e Consegna a Luca**: Presentazione del resoconto modifiche e avvio del test manuale in-game condotto secondo il manuale [`PROTOCOLLO_COLLAUDO_E_SESSIONI_MONITORATE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/manuali/PROTOCOLLO_COLLAUDO_E_SESSIONI_MONITORATE.md). In questa fase il piano rimane attivo in `docs/piani/attivi/` e la cartella di backup rimane intatta.

### Fase 3: Chiusura Ufficiale, Merge & Documentazione Viva (Solo dopo il test in-game di Luca)
5. **Merge su `mymaster`**: Esecuzione di `git merge --no-ff feat/nome-feature` sul branch master personale.
6. **Aggiornamento Documentazione Viva**:
   - Aggiornamento di `docs/content/changelog.md` con il dettaglio delle modifiche.
   - Allineamento di `docs/architecture.md` e `docs/api.md` (se modificate architetture o API).
   - Aggiornamento di `README.md`, `keybindings.md` e `features.md` (se introdotti nuovi comandi/tasti).
7. **Aggiornamento Backup PC Portatile**: Solo dopo il collaudo manuale positivo di Luca,
   - **Promozione Backup JAR Stabile**: Copia del `.jar` con tag/versione aggiornata nella suite `backup istanze/`.
   - **Esempio Percorso Suite Backup**:
   `C:\Users\nemex\OneDrive\progetti dei frati\accessible games\minecraft archivio backup\backup istanze\`
8. **Archiviazione del Piano Tecnico & Report di Sessione**: Spostamento del file del piano nella cartella `docs\piani\completati\` con marcatura `[COMPLETATO, COLLAUDATO E INTEGRATO]`, migrazione automatica del Report di Sessione in `docs/report/archivio/` e aggiornamento deterministico dei link in `docs/report/ARCHIVIO_REVISIONI.md`.
9. **Commit & Push su `origin/mymaster`**.
10. **Chiusura con Domanda Ponte Obbligatoria**:
    > *"Vuoi che avviamo ora la sessione formale di Auto-Apprendimento (Fase 4) per elaborare la bozza dettagliata delle regole e aggiornare le schede di conoscenza e governance?"*

### Fase 4: Auto-Apprendimento Continuo a Doppio Binario (Al via libera di Luca)
11. **Mappatura Schede**: Individuazione chirurgica delle schede locali (`knowledge/`) e globali (Master Hub) da aggiornare.
12. **Redazione Bozza Pronta**: Stesura dei testi completi e richiesta di convalida finale prima di applicarli.

---

## 8. Protocollo di Revisione e Affinamento Post-Implementazione (PRAPI)

Quando durante la Fase 2 (Collaudo manuale in-game di Luca) emergono micro-anomalie comportamentali, calibrazioni metriche o esigenze di rifinitura:
1. **Acquisizione Feedback & Diagnosi Telemetrica**:
   - Analisi mirata dei log di gioco (`latest.log`) e telemetria per isolare l'origine esatta dello scostamento;
2. **Riformulazione Voxel/Logica & Aggiornamento Piano Attivo (Sotto-Fase 1A Rapida)**:
   - Redazione della strategia correttiva, registrazione nel registro revisioni e aggiornamento del piano tecnico in `docs/piani/attivi/`;
3. **Stop Obbligatorio & Gating di Convalida**:
   - Richiesta formale di conferma a Luca prima di applicare modifiche ai sorgenti;
4. **Implementazione Chirurgica, Build & Re-Deploy (Sotto-Fase 1B)**:
   - Applicazione modifiche, esecuzione test JUnit, compilazione con `--no-daemon` su JDK 25 e deploy automatico nelle istanze attive prima del nuovo collaudo.

---

## 9. Protocollo di Convalida Empirica a Tre Fonti (Triangolazione Test - Telemetria - Persistenza)

Nelle verifiche e nei rapporti di chiusura di fase (Fase 2 / Fase 3), la convalida di un sistema percettivo e motorio complesso non può basarsi unicamente su test automatici o su resoconti orali generici. Si applica la **Triangolazione a Tre Fonti Indipendenti**:

1. **Fonte 1: Test Automatici Headless Deterministiche (Verifica di Coerenza Logica)**:
   - Suite completa JUnit a 0 ms con mock e clock simulato;
   - Certificazione dei contratti API, scadenze TTL, code di priorità e assenza di eccezioni.
2. **Fonte 2: Telemetria Live & Log di Runtime (`latest.log`) (Verifica di Percezione Sensoriale)**:
   - Monitoraggio delle stringhe effettivamente inviate al driver Tolk/SAPI e narrate a schermo;
   - Verifica di `interrupt: true` vs `interrupt: false`, assenza di soppressioni indebite, timing tra eventi e verifica di coesistenza armonica tra domini (Sicurezza vs Esplorazione vs Movimento).
3. **Fonte 3: Persistenza su Disco del Mondo di Gioco (Verifica degli Effetti Reali)**:
   - Ispezione dei file di salvataggio (`level.dat`, `region/*.mca`, `players/stats/<uuid>.json`);
   - Riscontro incrociato tra ciò che è stato vocalizzato ed eseguito (es. mob agganciato e abbattuto, danni inflitti, drop raccolti nell'inventario e blocchi estratti) e i dati registrati deterministicamente dall'engine di Minecraft.

Solo la convergenza simultanea e coerente di tutte e tre le fonti sancisce il **superamento definitivo della Fase 2** e autorizza il passaggio alla Fase 3 (Chiusura Tecnica).


---

## 10. Audit Avversariale Preventivo sui 5 Cancelli Inviolabili (Protocollo 12 — Inner Codex Pattern)

In ogni Piano Tecnico Formale (Sotto-Fase 1A), prima di richiedere la convalida a Luca, Antigravity include obbligatoriamente la sezione di **Audit Avversariale Preventivo**, valutando la soluzione contro il "peggior scenario possibile" secondo i 5 Cancelli Inviolabili:

1. **Cancello 1 — Rifiuto Patching Euristico (Invariante Voxel vs Sintomo Numerico)**:
   - *Verifica*: La proposta affronta la causa radice topologica (arco mancante nel grafo, calpestabilità errata di gradini/scale a pioli, orientamento voxel) o tenta di mascherare il problema aumentando budget di espansione A*, allungando delay di tick o iniettando mosse di fuga fisse?
2. **Cancello 2 — Purezza dell'Intento Fisico nei Sistemi Ibridi (Hardware Grounding)**:
   - *Verifica*: Nei sistemi cooperativi human-in-the-loop, la logica di takeover o decisione utente interroga direttamente l'hardware reale (GLFW probe raw input) o legge stati logici simulati (`isSneaking()`, `isDown()`) alterati da guardie concorrenti?
3. **Cancello 3 — Integrità Hitbox e Volumetria Continua**:
   - *Verifica*: L'algoritmo tratta il giocatore come prisma 3D continuo ($0.6 \times 1.8\text{ m}$) verificando clearance continua ad altezza occhi/testa (`stepPos.above()`) e collision shapes reali dei blocchi sottili (scale a pioli, porte, lastre), evitando semplificazioni a punti discreti?
4. **Cancello 4 — Disciplina dei Contratti Denominati e Chiusi (Named Contract Pattern)**:
   - *Verifica*: L'intervento è suddiviso in contratti formali atomici e numerati ($D_0 \dots D_N$, $S_1 \dots S_N$) con precondizioni, postcondizioni, complessità e invarianti anti-regressione esplicite?
5. **Cancello 5 — Determinismo Headless e Time-Seams a 0 ms**:
   - *Verifica*: Tutti i comportamenti dipendenti dal tempo (finestre di soppressione, debouncing vocale, cooldown, TTL) espongono package-private time seams per consentire test unitari JUnit istantanei a 0 ms senza `Thread.sleep`?

---

## 11. Standard Ufficiale del Report di Sessione & Telemetria (Pointer Hub & Cronologia Inversa)

Quando durante la Fase 2 (Deploy e Collaudo) o il Protocollo 5 (PRAPI) si apre o si aggiorna la sessione di lavoro, Antigravity redige e mantiene il file `docs/report/REPORT_SESSIONE_[NOME_TASK].md` secondo le seguenti regole vincolanti:

1. **Intestazione Rigida a 7 Campi**:
   - Subito sotto il titolo del file, il report deve obbligatoriamente contenere:
     * `- **Autore**: [Chi ha redatto il rapporto: Antigravity / Luca / GPT Codex]`
     * `- **Revisori**: [Chi partecipa alla revisione/collaudo: Luca, GPT Codex, Antigravity]`
     * `- **Data e Ora**: YYYY-MM-DD HH:mm`
     * `- **Stato dell'Implementazione**: [IN PIANIFICAZIONE] | [IN TELEMETRIA / ATTIVO] | [IN REVISIONE PRAPI] | [COMPLETATO E COLLAUDATO]`
     * `- **Obiettivo/i**: [Elenco sintetico obiettivi]`
     * `- **Piani & Strategie Correlate**: [Link ai piani in docs/piani/ e strategie]`
     * `- **Breve Descrizione**: [2-3 righe dense di contesto]`
2. **Flusso Decisionale a Cronologia Inversa (Newest First per NVDA)**:
   - Ogni nuovo aggiornamento, messaggio, osservazione di collaudo o proposta di ChatGPT/Codex deve essere inserito **tassativamente in cima alla sezione messaggi** (subito sotto l'intestazione `## 💬 Flusso Decisioni & Revisioni`).
   - Questo garantisce che all'apertura del file con screen reader NVDA, premendo una sola volta il tasto rapido intestazione (`H`), il cursore atterri all'istante sull'ultimo stato e decisione presa, eliminando lo scrolling verso il fondo.
3. **Standard Atomico del Messaggio ad Alto Segnale (4 Campi Obbligatori)**:
   - Ciascuna voce di messaggio si struttura su 4 campi:
     * `Contesto / Sintomo`: massimo 2 righe su cosa accade in-game o nel test;
     * `Causa Radice Concettuale`: il perché geometrico o architetturale;
     * `Approccio & Strategia Risolutiva`: la soluzione formulata ad elenchi compatti "Se... Allora", zero codice prolisso;
     * `Puntatori & Riferimenti Estesi`: elenco link `file:///...` a classi, righe di `latest.log`, test seams e ID di revisione `Rev XX.Y`.
   - **Divieto Assoluto di File Bloat**: vietato incollare dump grezzi di log o metodi di codice interi (> 3 righe).
4. **Sinergia a Puntatore Diretto RRU -> Report (DRY Pattern)**:
   - Le singole note nel `REGISTRO_REVISIONI.md` mantengono una sintesi estrema e puntano direttamente al Report di Sessione (`Report di Sessione & File Correlati`).
   - Il Report è l'**unica fonte di verità (Single Source of Truth)** per l'elenco dei file, delle righe modificate e dei log coinvolti, evitando doppie registrazioni disallineate.
5. **Archiviazione Automatica a Zero Residui (Fase 3 - Protocollo 6)**:
   - A collaudo manuale positivo di Luca, durante la Chiusura Tecnica (Fase 3), il file `docs/report/REPORT_SESSIONE_[TASK].md` viene **spostato automaticamente** in `docs/report/archivio/REPORT_SESSIONE_[TASK].md`.
   - Contestualmente, nella voce migrata in `ARCHIVIO_REVISIONI.md`, il link viene aggiornato automaticamente per puntare al percorso di archivio del report, garantendo zero link rotti e memoria storica perenne.

6. **Procedura di Ingestione In-Flight & Archiviazione Report (Standard URCP — ASTRALIS v2.7.1)**:
   - *Finalità*: Garantire che ogni report (proveniente dall'esterno come ChatGPT/Codex, tester, o note di collaudo) sia uniformato allo standard ASTRALIS v2.7.1 sia durante la fase attiva (post-analisi) sia prima dell'archiviazione finale.
   - *Ingestione In-Flight Post-Analisi (Protocollo 4)*:
     * Non appena un documento o report esterno viene sottoposto ad analisi/valutazione, Antigravity formula le proprie osservazioni e contestualmente propone a Luca la conversione URCP;
     * Con il via libera di Luca (*"procedi"*), il file viene subito convertito ed è pronto per essere fruito linearmente con NVDA e collegato al `REGISTRO_REVISIONI.md`.
   - *Procedura a 4 Passi (URCP)*:
     1. **Normalizzazione Intestazione a 7 Campi**: Estrazione e popolamento dei campi obbligatori (`Autore`, `Revisori`, `Data e Ora`, `Stato dell'Implementazione`, `Obiettivo/i`, `Piani & Strategie Correlate`, `Breve Descrizione`);
     2. **Inversione Cronologica per NVDA**: Posizionamento dell'esito/stato conclusivo in alto e delle sezioni storiche in basso per fruizione vocale immediata;
     3. **Denoising & Puntatori Intelligenti (DRY Pattern)**: Condensazione dei dump di log estesi con puntamento a `latest.log`, mantenendo intatta la conoscenza geometrica, le coordinate e le disamine architetturali;
     4. **Bonifica Tecnica**: Rimozione del BOM UTF-8 (`\ufeff`), formattazione corretta dei link markdown `file:///` e normalizzazione dei marcatori.

---

## 12. Standard Ufficiale delle Strategie Logico-Cognitive (UPCS — ASTRALIS v2.7.2)

Le **Strategie Logico-Cognitive** costituiscono il **quarto pilastro fondamentale** dell'ecosistema ASTRALIS. Si collocano nella **Fase 0 (Pre-Pianificazione)** e governano il *modello mentale*, la *dialettica ingegneristica* e il *congelamento delle invarianti logico-geometriche ed acustiche* prima della stesura del Piano Tecnico Formale.

1. **Disaccoppiamento Epistemologico dei 4 Pilastri**:
   - *Strategia Cognitiva (Fase 0)*: **Proattiva e Deliberativa** (*Il Perché e il Modello Mentale*). Esplora lo spazio delle soluzioni e congela le invarianti;
   - *Piano Tecnico (Fase 1)*: **Esecutivo e Deterministico** (*Il Cosa e il Dove*). Dettaglia contratti D0..DN, classi, metodi, test seams e checklist a 3 stati;
   - *Report di Sessione & Telemetria (Fase 2)*: **Retrospettivo ed Empirico** (*L'Evidenza sul Campo*). Registra i log in-game, i dialoghi operativi a cronologia inversa e il collaudo di Luca;
   - *Registro Revisioni RRU (Fase 3)*: **Sintesi Forense e Memoria Perenne** (*La Tracciabilità nel Tempo*). Mantiene lo stato aperto/chiuso delle anomalie e punta bi-direzionalmente a Strategie, Piani e Report.

2. **I 7 Archetipi Strategici Formali**:
   - *Concettuale-Implementativa (Ideazione & Feature Design)*: traduce esigenze o idee in modelli concettuali stabili;
   - *Diagnostico-Correttiva (Anomalie Ostiche & Root-Cause Discovery)*: analizza bug sistemici concorrenti smentendo ipotesi e isolando l'invariante infranta;
   - *Refactoring Strutturale & Disaccoppiamento (Architectural Restructuring)*: separa layer accoppiati ed elimina God Objects preservando l'invarianza del comportamento;
   - *Euristico-Cognitiva per Screen Reader & Audio 3D (UX Audio & NVDA)*: progetta la gerarchia vocale, anti-chatter, ducking sonoro e volumi di sicurezza (0.7f - 0.8f);
   - *Dialettica Avversariale & Convergenza Multi-AI (Inner Codex Pattern)*: dirime divergenze tra copiloti AI sui 5 Cancelli Inviolabili;
   - *Integrazione & Interoperabilità di Runtime*: isola librerie terze (Cloth Config, Loom, Fabric API) e gestisce fallback difensivi;
   - *Bonifica, Migrazione & Deprecazione (Zero-Debt Clean-up)*: governa la rimozione sicura di codice morto o formati dati con strategia a 5 barriere.

3. **Intestazione Istituzionale & Checklist di Convergenza di Fase 0**:
   - Ogni strategia adotta il template `STRATEGIA_COGNITIVA_TEMPLATE.md` con intestazione formale e Registro di Convergenza con checklist a 3 stati:
     * `- [ ] [DA DISCUTERE / APERTO]`: Tesi in esplorazione;
     * `- [/] [CONVERGENZA PRELIMINARE — IN ATTESA DI CONVALIDA]`: Modello logico delineato, in attesa di decisione sovrana di Luca;
     * `- [x] [CONVALIDATO — INVARIANTE CONGELATA]`: Principio approvato, pronto per la conversione in Piano Tecnico.

4. **La Formula di Conversione (Da Strategia a Piano Tecnico)**:
   - Quando Luca convalida la Strategia (*"approvo la strategia, convertiamola in piano"*):
     * Le *Invarianti Inviolabili* della Strategia diventano i *Named Contracts D0..DN* del Piano Tecnico;
     * Le *Classi e i Layer* individuati diventano i target di modifica della Sotto-Fase 1B;
     * Gli *Scenari di Stress* discussi diventano la *Matrice di Simulazione a 3 Livelli* del Piano;
     * Lo stato della strategia passa a `[CONVERTITA IN PIANO TECNICO]`.

5. **Archiviazione Automatica a Zero Residui (Protocollo 6)**:
   - A collaudo positivo di Luca, la strategia migra simultaneamente da `docs/strategie/attive/` a `docs/strategie/archiviate/` con stato `[ARCHIVIATA CON SUCCESSO]`, aggiornando tutti i puntatori incrociati nel Piano e nel Registro Revisioni.
