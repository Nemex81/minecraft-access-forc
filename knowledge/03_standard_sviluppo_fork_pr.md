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
