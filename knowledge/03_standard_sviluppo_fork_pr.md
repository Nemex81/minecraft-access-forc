# 03 — Standard Sviluppo Fork, PR Upstream & Localizzazione (I18N)

## 1. Filosofia del Fork Locale e Contributi Upstream

Lo sviluppo di `minecraft-access` segue una rigorosa separazione tra la documentazione interna e i contributi al repository ufficiale della community:

1. **Pull Request Upstream (Inglese Puro)**:
   - Qualsiasi modifica destinata ad essere inviata tramite Pull Request (PR) o Merge Request (MR) al repository upstream **deve essere redatta interamente in lingua inglese impeccabile**.
   - I messaggi di commit, le intestazioni di branch, i commenti Javadoc nel codice sorgente Java e le descrizioni delle PR devono essere rigorosamente in inglese tecnico.
2. **Documentazione Locale e di Progetto (Italiano)**:
   - I file di coordinamento locale con Luca, i manuali d'uso personali, le schede in `knowledge/`, `GEMINI.md` e i canali di dialogo con ChatGPT sono redatti in lingua italiana.

---

## 2. Regole di Localizzazione (I18N) & Gestione Community Weblate

Tutte le stringhe vocalizzate o mostrate a schermo sono internazionalizzate nei file JSON situati in:  
📁 `src/main/resources/assets/minecraft_access/lang/`

### Focus Esclusivo di Sviluppo (IT ed EN):
- **`en_us.json`**: Lingua master ufficiale del progetto (utilizzata come base e fallback automatico di Minecraft).
- **`it_it.json`**: Lingua italiana nativa curata per l'accessibilità e la sintesi vocale di Luca.
- **Restanti 14 Lingue**: Delegate interamente alla community upstream tramite la piattaforma collaborativa Weblate (`https://hosted.weblate.org/git/minecraft-access/mod/`). Non è richiesto né necessario aggiornare manualmente le altre lingue durante lo sviluppo locale.

---

## 3. Vincoli Mandatori di CI/CD e Formattazione JSON

1. **Ordinamento Alfabetico Obbligatorio delle Chiavi**:  
   Ogni file `.json` modificato nella cartella `lang/` **DEVE avere le chiavi rigorosamente ordinate in ordine alfabetico crescente**.  
   Il workflow di CI GitHub (`.github/workflows/linting.yml`) esegue il controllo:
   ```bash
   jq -e "keys != keys_unsorted"
   ```
   Se una sola chiave non rispetta l'ordine alfabetico nel file modificato, la pipeline di CI fallisce bloccando la build.
2. **Allineamento IT/EN**:  
   Ogni nuova chiave aggiunta a `en_us.json` deve essere contestualmente inserita con la relativa traduzione italiana in `it_it.json`.
3. **Preservazione dei Segnaposto Formato**:  
   Mantenere esattamente i segnaposto numerati e posizionali (es. `%s`, `%d`, `%1$s`, `%2$d`) per evitare `IllegalFormatException` a runtime.
4. **Convenzione Naming Chiavi**:
   - Chiavi di binding tasti: `key.minecraft_access.<modulo>.<azione>`
   - Categorie tasti: `category.minecraft_access.<modulo>`
   - Stringhe parlate/UI: `minecraft_access.<modulo>.<descrizione>`

---

## 4. Architettura dei Branch del Fork Personale

Il repository personale (`Nemex81/minecraft-access-forc`) è organizzato secondo la seguente gerarchia:

1. **`dev` (Specchio Upstream)**:
   - Mantenuto identico al branch principale della community ufficiale (`upstream/dev`).
   - Viene aggiornato periodicamente con `git pull upstream dev` senza modifiche manuali dirette.
2. **`mymaster` (Master Stabile Personale)**:
   - Ramo centrale "tutto incluso" contenente tutte le funzionalità stabili e collaudate (AutoWalk, Numpad, Waypoint, Ricettario, traduzioni IT e fix).
   - È il ramo ufficiale da cui si compila il `.jar` per l'istanza di gioco PrismLauncher.
3. **Rami Feature & Fix (`feat/*`, `fix/*`)**:
   - Creati per lo sviluppo isolato di nuove feature o fix mirati.
   - Una volta testati e stabili, vengono uniti in `mymaster`. Se destinati alla community, la PR viene aperta direttamente dal branch specifico verso `upstream/dev`.

---

## 5. Protocollo di Sincronizzazione con Upstream & Rebase Sicuro

Quando il repository ufficiale rilascia nuovi aggiornamenti su `upstream/dev`, l'allineamento di `mymaster` segue una sequenza rigorosamente protetta:

1. **Aggiornamento Specchio `dev` (Fast-Forward)**:
   ```powershell
   git checkout dev
   git fetch upstream dev
   git merge --ff-only upstream/dev
   git push origin dev
   ```

2. **Simulazione Preventiva 3-Way Merge (Zero Conflitti)**:
   - Prima di applicare modifiche, simulare il merge in memoria senza toccare il working tree:
   ```powershell
   git merge-tree $(git merge-base mymaster dev) mymaster dev
   ```
   - Se l'output non presenta marcatori di conflitto (`<<<<<<<`), il rebase è garantito al 100% pulito.

3. **Rebase di `mymaster` su `dev`**:
   ```powershell
   git checkout mymaster
   git rebase dev
   git push origin mymaster --force-with-lease
   ```

---

## 6. Standard dei Commit Semantici (Conventional Commits)

Ogni commit nel repository deve seguire lo standard semantico:

```text
<tipo>(<ambito>): <titolo sintetico all'indicativo presente>

[Corpo opzionale: spiegazione dettagliata del perché e della logica di modifica]
```

### Tipi Ammessi (*Types*):
- **`feat`**: Nuova funzionalità (es. `feat(autowalk): implement continuous smooth rotation`).
- **`fix`**: Risoluzione bug (es. `fix(recipe-book): resolve focus lock on key X`).
- **`docs`**: Modifiche alla documentazione o alle regole (es. `docs: add architecture and api guides`).
- **`refactor`**: Modifiche di codice che non alterano il comportamento esterno.
- **`test`**: Aggiunta o aggiornamento di test unitari JUnit.
- **`chore`**: Modifiche al build system Gradle, CI o dipendenze.

### Regole di Formattazione:
- Titolo conciso ($\le 72$ caratteri), senza punto finale.
- Lingua Inglese obbligatoria per commit di codice Java, feature o fix destinati alla community.

---

## 7. Ciclo di Vita dei Feature Branch & Criterio di Proporzionalità

Per conciliare rigore architetturale, stabilità di `mymaster` e agilità operativa senza burocrazia:

### A. Quando Creare un Ramo Dedicato (`feat/*`, `fix/*`):
- **Nuove Funzionalità Complesse**: Sviluppo di nuovi moduli Java (es. sonar subacqueo, nuovi filtri POI, menu avanzati).
- **Piani Tecnici Strutturati**: Qualsiasi modifica legata a un piano attivo in `docs/piani/attivi/`.
- **Refactoring Architetturali Rilevanti**: Modifiche a Mixin o strutture dati centrali.
- **Contributi per Upstream**: Qualsiasi PR destinata alla community ufficiale.
- **Richiesta Esplicita di Luca**: Quando Luca richiede specificamente di isolare l'intervento.

### Procedura Feature Branch:
1. **Creazione Ramo**: `git checkout -b feat/nome-feature mymaster`
2. **Sviluppo & Collaudo Isolati**: Lo sviluppo, i test JUnit e la compilazione del `.jar` per il collaudo di Luca (Fase 2) avvengono rimanendo all'interno del branch `feat/*`.
3. **Merge su `mymaster` Post-Collaudo**:
   - **Solo dopo la conferma positiva di Luca**, si effettua il merge con `--no-ff`:
     ```powershell
     git checkout mymaster
     git merge --no-ff feat/nome-feature -m "feat(modulo): merge nome-feature into mymaster"
     git push origin mymaster
     ```

---

### B. Fast Path Diretto su `mymaster` (Senza Creare Branch):
È consentito e raccomandato lavorare direttamente su `mymaster` per interventi rapidi che non giustificano l'overhead di un branch:
- **Aggiornamento Documentazione Viva**: Modifiche a `docs/content/changelog.md`, `README.md`, `docs/architecture.md`, `docs/api.md`, schede di `knowledge/` e manuali in `docs/manuali/`.
- **Piccoli Ritocchi Numerici**: Modifica mirata di una costante numerica (es. volume audio `0.7f -> 0.6f`, velocità di rotazione o delay).
- **Traduzioni e Refusi I18N**: Correzione o inserimento di poche chiavi nei file `.json` di lingua.
- **Fix Minori One-Line**: Piccole correzioni puntuali già verificate e approvate da Luca.

---

## 8. Disciplina Dual-Track Changelog & Regola di Isolamento PR Upstream

Per garantire perfetta igiene verso i maintainer upstream e contestuale sovranità del fork personale:

1. **Il Doppio Binario dei Changelog**:
   - **Binario Pubblico Upstream (`CHANGELOG.md` di radice)**:
     * Redatto in **lingua inglese pura** seguendo lo standard *Keep a Changelog*;
     * Raccoglie tutte le novità sviluppate sotto la sola sezione **`## [Unreleased]`**;
     * Zero version forcing: non inventa numeri di versione ufficiali per evitare conflitti di merge nelle PR.
   - **Binario Sovrano Locale (`knowledge/13_diario_modifiche_e_contributi_fork.md`)**:
     * Redatto in **lingua italiana**;
     * Governa il **versionamento semantico personale AVF** (`V.A.R[.M]`) per le build locali di Luca;
     * Traccia il razionale architetturale, i feedback per screen reader NVDA e la cronologia completa delle release.

2. **Regola Inviolabile di Isolamento delle Pull Request Upstream**:
   - Quando si prepara una PR verso `upstream/dev`:
     * **Consentito nella PR**: Codice sorgente Java/Mixin (`src/main/java`), risorse e traduzioni (`src/main/resources`), test automatici (`src/test/java`) e le righe di `CHANGELOG.md` `## [Unreleased]`.
     * **Tassativamente Vietato nella PR**: `gemini.md`, l'intera cartella `knowledge/`, le cartelle `docs/piani/` e `docs/report/`, prompt o script locali di ASTRALIS.


