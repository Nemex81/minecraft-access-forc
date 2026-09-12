# 07 — Sincronizzazione Macchine, Build & Pipeline di Deploy

## 1. Procedura di Compilazione Locale

La compilazione del progetto avviene tramite Gradle Wrapper e richiede Java 25.

L'esecuzione dei task di verifica e compilazione deve avvenire esclusivamente nell'ambito del gate autorizzato:

```powershell
# Esecuzione esplicita dei controlli statici e test (se previsti dal gate)
.\gradlew.bat --no-daemon --no-watch-fs test checkstyleMain checkstyleTest

# Compilazione del JAR (shadowJar non esegue implicitamente i test o checkstyle)
.\gradlew.bat --no-daemon --no-watch-fs shadowJar
```

- **Output generato**: Artefatto `.jar` posizionato nella cartella `build\libs\`.
- **Risoluzione Deterministica e Validazione dell'Artefatto**:
  Lo script di deploy non deve mai selezionare un file basandosi sulla data di modifica. La selezione deve:
  1. Individuare l'artefatto dichiarato come output del task `shadowJar`, escludendo classificatori di servizio (`-sources`, `-dev`);
  2. Fallire con errore esplicito se il conteggio dei candidati è zero oppure maggiore di uno (ambiguità);
  3. Verificare nome, dimensione e SHA-256 del file;
  4. Ispezionare il contenuto del JAR per verificare che l'identificativo mod e la versione dichiarati nel descrittore (`fabric.mod.json`) corrispondano esattamente al target autorizzato.

---

## 2. Canone Dinamico & Auto-Discovery di Sistema

Per garantire la massima resilienza e indipendenza dai percorsi cablati, l'assistente adotta il protocollo di risoluzione dinamica in sola lettura:

### A. Auto-Discovery Dinamica delle Istanze Candidate:
- **Regola di Discovery**: L'auto-discovery individua esclusivamente le istanze candidate e ne presenta l'elenco chiaro e strutturato all'utente.
- **Divieto di Deploy Implicito**: La scoperta di un'istanza nel filesystem NON autorizza automaticamente il deploy.
- **Risoluzione al Volo (PowerShell — Sola Lettura)**:
```powershell
# Rilevamento delle sole istanze candidate (discovery non distruttiva)
$candidateInstances = Get-ChildItem "$env:APPDATA\PrismLauncher\instances" -Directory -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -match "Minecraft.*26\.2.*Access" }
```

### B. Risoluzione Dinamica del Runtime Java (JDK 25):
- **Regola**: Non fissare versioni statiche volatili di Java.
- **Risoluzione al Volo (PowerShell)**:
```powershell
$jdk = Get-ChildItem "$env:ProgramFiles\Microsoft\jdk-25*", "$env:APPDATA\PrismLauncher\java\*" -Directory -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending | Select-Object -First 1
if ($jdk) { $env:JAVA_HOME = $jdk.FullName }
```

### C. Risoluzione Dinamica dei Canali di Backup & Archivio:
- **Radice Archivio e Backup**: I percorsi di backup su storage sincronizzato sono risolti dinamicamente tramite variabili d'ambiente (`$env:OneDrive`, `$env:USERPROFILE`).
- **Associazione del Profilo Candidato**: L'interrogazione dell'ambiente di sistema (es. `$env:COMPUTERNAME`) individua unicamente il profilo candidato per la postazione; non decide né impone autonomamente una destinazione di copia, che richiede sempre la conferma nel contesto operativo.

---

## 3. Procedura di Deploy Guidato dall'Autorizzazione (Fase 2)

Dopo la Sotto-Fase 1B, il deploy può essere eseguito esclusivamente nel relativo gate operativo autorizzato della Fase 2, rispettando una rigida disciplina di isolamento e autorizzazione esplicita:

1. **Gate di Pre-Deploy**:
   - Prima di qualsiasi operazione di copia, l'assistente deve dichiarare esplicitamente al responsabile decisionale:
     * Branch Git e hash del commit sorgente;
     * Nome, dimensione e SHA-256 del file JAR verificato;
     * Istanza o istanze candidate con relativi ruoli astratti (es. istanza client di collaudo, istanza per gioco online);
     * Destinazione esatta nel filesystem (`<cartella-istanza>\minecraft\mods\`);
     * Metodo di rollback disponibile in caso di anomalia.
2. **Autorizzazione Granulare dell'Utente o del Responsabile Decisionale**:
   - Il responsabile decisionale autorizza formalmente la singola istanza target prescelta oppure un insieme nominativo di target.
   - **Target Primario per il Collaudo**: Per il collaudo pratico iniziale si aggiorna prioritariamente una sola istanza concordata. L'aggiornamento di una seconda istanza richiede una nuova autorizzazione espressa, salvo ordine cumulativo preventivo dell'utente.
   - È tassativamente vietato eseguire il deploy automatico o simultaneo su tutte le istanze rilevate.
3. **Disaccoppiamento Rigoroso dei Gate**:
   - Compilazione (build), deploy, collaudo pratico in-game, merge Git e promozione nel backup stabile su cloud restano cancelli distinti e sequenziali. Il passaggio da un cancello al successivo richiede sempre l'autorizzazione esplicita dell'utente.
4. **Profili Locali vs Conoscenza Astratta**:
   - I nomi specifici delle istanze e le configurazioni concrete appartengono ai profili locali privati. La knowledge distribuibile adotta ruoli astratti (es. istanza client di collaudo, istanza per gioco online, istanza destinataria autorizzata).
   - L'assenza di una cartella `saves/` può essere normale per specifiche istanze (es. profili dedicati al gioco su server remoto), ma deve essere trattata come caratteristica del profilo locale e non come assunzione globale.
5. **Regola di Codifica dei File di Configurazione (`options.txt`)**:
   - `options.txt` e gli altri file testuali devono essere scritti con la codifica prevista dal rispettivo formato.
   - Per `options.txt` utilizzare UTF-8 senza BOM, verificando dopo la scrittura che la prima chiave sia leggibile e che il file non sia stato troncato o corrotto.

---

## 4. Regola di Parità tra Profili Macchina

- **Parità tra Profili**: Le impostazioni accessibili collaudate e i pacchetti mod vengono confrontati e allineati esclusivamente mediante operazioni transazionali autorizzate, mantenendo un solo autore attivo per i dati persistenti.
- **Verifica Preventiva**: Prima di avviare una sessione di sviluppo o collaudo su un dispositivo alternativo, verificare la presenza del JAR allineato al commit del branch attivo e lo stato dei salvataggi conformi.

---

## 5. Disciplina Transazionale di Trasferimento Salvataggi & Waypoint POI

- **Dualità dei Dati**: In Minecraft Access, i blocchi e il terreno risiedono in `minecraft/saves/<mondo>/`, mentre i **Punti di Interesse (Waypoint)** sono serializzati in `minecraft/config/minecraft-access/waypoints/singleplayer_<mondo>.json`.
- **Divieto di Sincronizzazione Continua**: È vietata la sincronizzazione automatica o continua in tempo reale della cartella `saves/` tramite cloud drive, a causa dei continui lock di file di Minecraft e del rischio concreto di corruzione delle regioni NBT (`.mca`).
- **Protocollo Transazionale a Scrittore Unico con Rollback sul Destinatario**:
  Il trasferimento di una partita tra ambienti diversi segue tassativamente la sequenza:
  1. *Chiusura del Gioco sull'Origine*: Arresto completo di Minecraft sul computer di origine per garantire il flush di tutti i file di chunk e waypoints su disco;
  2. *Creazione Snapshot Filtrato*: Compressione della sola cartella del mondo e del relativo file JSON dei waypoint in un pacchetto zip timestampato (escludendo session.lock e file temporanei);
  3. *Generazione Manifest Completo*: Redazione di un manifest contenente elenco percorsi relativi, dimensioni e SHA-256 di ciascun file archiviato;
  4. *Scrittura Protetta del Marcatore di Pacchetto Pronto (`.ready.json`)*:
     - Il file marcatore `.ready.json` viene scritto nello storage condiviso **esclusivamente DOPO il completamento stabile dello ZIP** (mai durante la scrittura dell'archivio);
     - Il marcatore deve contenere in formato strutturato nome, dimensione e SHA-256 dello ZIP, nonché l'hash del manifest;
     - Sul destinatario, dopo aver verificato la stabilità dei file sincronizzati e prima di qualsiasi estrazione o importazione, la procedura confronta nome, dimensione e SHA-256 effettivi dello ZIP con i valori dichiarati nel marcatore e verifica analogamente l'hash del manifest. Marcatore assente, incompleto o non corrispondente comporta l'arresto immediato senza modificare la destinazione;
  5. *Snapshot Preventivo sul Destinatario*: A gioco rigorosamente chiuso sul computer di destinazione, creazione preliminare di uno snapshot di sicurezza dello stato corrente prima di qualsiasi manipolazione;
  6. *Estrazione in Staging & Riscontro Hash*: Decompressione del pacchetto in una directory temporanea di staging e verifica di integrità di tutti i file rispetto al manifest;
  7. *Sostituzione Controllata e Recuperabile*: Sostituzione delle cartelle target solo a validazione completata con successo e su autorizzazione esplicita dell'utente;
  8. *Procedura di Rollback Disponibile*: In caso di mancata corrispondenza degli hash o errore di avvio, ripristino immediato e verificato dello snapshot preventivo del destinatario.

---

## 6. Architettura Multi-Canale di Resilienza & Sincronizzazione (ASTRALIS v3.0.4)

Per garantire la totale separazione tra codice, dati runtime pesanti e archivi storici, Minecraft Access adotta la matrice a **4 Canali Funzionali di ASTRALIS**:

1. **Canale 1 — Versionamento del Codice Sorgente (VCS / Git)**:
   - *Finalità*: Tracciamento atomico di file `.java`, `.json`, `.gradle`, documentazione di progetto e piani tecnici.
   - *Target*: Repository Git sul branch di lavoro attivo.
   - *Regola*: Vietato aggiungere file binari superiori a 10 MB, cartelle `saves/` o dump di log completi.
2. **Canale 2 — Ponte Transazionale Multi-Dispositivo (Staging Salvataggi & Config)**:
   - *Finalità*: Scambio transazionale controllato di pacchetti salvataggio snapshot e waypoint tra postazioni diverse secondo il Protocollo Transazionale a Scrittore Unico.
   - *Target*: Cartella cloud sincronizzata dedicata a pacchetti compressi e verificati.
   - *Regola*: Vietata la sincronizzazione live o cartelle aperte `saves/`.
3. **Canale 3 — Archivio Storico & Disaster Recovery (Cold Archive)**:
   - *Finalità*: Snapshot di sicurezza preventiva pre-intervento, archivio build `.jar` ufficiali e memoria perenne dei rilasci.
   - *Target*: Cartella di archivio e disaster recovery definita nel profilo del workspace.
   - *Regola*: Aggiornato esclusivamente in Fase 3 (Chiusura Tecnica) post-convalida dell'utente o prima di manipolazioni territoriali distruttive (vedi [12_integrita_mondi_e_disaster_recovery.md](12_integrita_mondi_e_disaster_recovery.md)).
4. **Canale 4 — Backup Primario Resiliente (Sostitutivo di Git per Progetti No-VCS)**:
   - *Finalità*: Destinato a progetti non tecnici privi di Git: durante un gate di chiusura autorizzato, lo strumento di backup crea uno snapshot ZIP timestampato e verificato; il cloud drive conserva e trasferisce esclusivamente il pacchetto completato, senza sincronizzare direttamente le directory operative.
