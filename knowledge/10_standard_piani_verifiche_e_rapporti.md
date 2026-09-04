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
   - `docs\piani\attivi\`: Piani tecnici attivi.
   - `docs\piani\completati\`: Piani tecnici collaudati e integrati.
   - `docs\strategie\`: Documenti di strategia, architettura e metodologie.
   - `docs\report\`: Relazioni diagnostiche, audit e collaudi.
   - `docs\idee\`: Promemoria, spunti futuri e meccaniche da esplorare.
   - `docs\manuali\`: Manuali d'uso e guide comandi in-game.

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
7. **Aggiornamento Backup PC Portatile**: Solo dopo il collaudo manuale positivo di Luca, promozione del JAR stabile nella cartella di backup:
   `C:\Users\nemex\OneDrive\progetti dei frati\accessible games\minecraft archivio backup\minecraft backup\Minecraft 26.2 Access 1.12.0 pc portatile\minecraft\mods\`
8. **Archiviazione del Piano Tecnico**: Spostamento del file del piano nella cartella `docs\piani\completati\` con marcatura `[COMPLETATO, COLLAUDATO E INTEGRATO]`.
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
