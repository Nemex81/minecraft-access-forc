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

## 7. Pipeline Ufficiale a 4 Fasi (Compilazione, Test, Deploy, Chiusura e Auto-Apprendimento)

La conclusione di ogni sessione implementativa segue tassativamente una sequenza a 4 fasi:

### Fase 1: Build e Test Automatici
1. **Verifica Compilazione**: Esecuzione di `.\gradlew.bat compileJava` e `compileTestJava`.
2. **Esecuzione Test Unitari**: Esecuzione di `.\gradlew.bat :test` per confermare che tutti i test passino al 100%.
3. **Generazione Pacchetto JAR**: Esecuzione di `.\gradlew.bat shadowJar`.

### Fase 2: Deploy di Prova e Collaudo Manuale Utente
4. **Deploy Provvisorio nelle Istanze**: Copia e sovrascrittura del file `.jar` appena compilato nelle cartelle `mods/` delle istanze PrismLauncher attive del giocatore (per consentire l'apertura del gioco).
5. **Rapporto e Consegna a Luca**: Presentazione del resoconto modifiche e avvio del test manuale in-game condotto secondo il manuale [`PROTOCOLLO_COLLAUDO_E_SESSIONI_MONITORATE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/manuali/PROTOCOLLO_COLLAUDO_E_SESSIONI_MONITORATE.md). In questa fase il piano rimane attivo in `docs/piani/attivi/` e la cartella di backup rimane intatta.

### Fase 3: Chiusura Ufficiale Simultanea (Solo dopo il test in-game di Luca)
6. **Aggiornamento Backup PC Portatile**: Solo dopo il collaudo manuale positivo di Luca, promozione del JAR stabile nella cartella di backup:
   `C:\Users\nemex\OneDrive\progetti dei frati\accessible games\minecraft archivio backup\minecraft backup\Minecraft 26.2 Access 1.12.0 pc portatile\minecraft\mods\`
7. **Archiviazione del Piano Tecnico**: Spostamento del file del piano nella cartella `docs\piani\completati\` con marcatura `[COMPLETATO, COLLAUDATO E INTEGRATO]`.

### Fase 4: Auto-Apprendimento Automatico Post-Chiusura (Trigger Sistematico)
8. **Analisi Retrospettiva Autonoma**: Subito dopo la Fase 3, Antigravity avvia **automaticamente e senza richiesta esplicita** una sessione di analisi diagnostica retrospettiva sulle implementazioni svolte, sui problemi affrontati e sulle soluzioni fisiche/ergonomiche adottate.
9. **Strategia e Proposta di Nuove Regole**: Antigravity valuta quali lezioni hanno valore generale permanente, elabora la strategia di conversione in regole formali per `knowledge/` e `gemini.md`, presenta a Luca un riepilogo chiaro e strutturato e richiede la conferma prima di applicarle.
