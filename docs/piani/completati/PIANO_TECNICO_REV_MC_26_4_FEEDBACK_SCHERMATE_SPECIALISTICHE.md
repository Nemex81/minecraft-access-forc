# Piano Tecnico Formale (Sotto-Fase 1A): Feedback Eventi Visivi & Auto-Focus su Schermate Specialistiche (Rev MC-26.4)
# Autore: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity
# Progetto: Minecraft Access (Fork 26.2 / 1.21.x)
# Data: 2026-09-02
# Ambito: GUI & Accessibility Refinement (PRAPI)
# Incremento Versione Target (AVF): Revisione / Patch (v26.2-1.16.1)
# Stato: [BOZZA IN ATTESA DI CONVALIDA]

---

## 📌 1. Quadro di Riferimento & Razionale

Il presente piano affronta e risolve la voce aperta **`🟡 Rev MC-26.4`** del Registro Revisioni di Minecraft Access, colmando il gap di accessibilità e feedback acustico/vocale su quattro schermate specialistiche di lavorazione:
1. **Tagliapietre (`StonecutterScreen`)**: Quando si inserisce un blocco di pietra, il sistema annuncia le forme disponibili ma non rigenera contestualmente il gruppo di selezione né posiziona il focus;
2. **Telaio (`LoomScreen`)**: Quando si inseriscono stendardo e tintura, manca l'annuncio dei motivi disponibili (`SelectablePatterns`) e il focus immediato sul selettore motivi;
3. **Fornaci (`AbstractFurnaceScreen`, Forno Fusorio, Fumatore)**: Manca un feedback discreto al completamento del ciclo di cottura;
4. **Alambicco (`BrewingStandScreen`)**: Manca la notifica al completamento del ciclo di distillazione delle pozioni.

---

## 🛡️ 2. Validazione Preventiva sui 7 Assi di Qualità

1. **Validità**: Piena conformità all'architettura GUI di Minecraft Access e all'Astralis Versioning Framework.
2. **Efficacia**: Feedback tempestivi e navigazione fluida da tastiera senza richiedere esplorazione manuale ridondante.
3. **Coerenza**: Armonia con gli standard sonori e vocali esistenti (ducking, linearità NVDA, zero mouse).
4. **Completezza**: Gestione esaustiva dei 4 container specialistici vanilla/modded.
5. **Precisione per NVDA**: Messaggi brevi, informativi e non invasivi.
6. **Affidabilità & Prestazioni**: Controllo su tick interval già presente in `InventoryControls.java`, zero allocazioni pesanti.
7. **Assenza di Regressioni**: Nessuna alterazione ai container standard (bauli, crafting table, sculker box).

---

## 🧪 3. Matrice di Simulazione a 3 Livelli (Stress-Test Logico)

### 🟢 Simulazione 1: Utilizzo del Tagliapietre (Scenario Comune)
- **Input**: Il giocatore inserisce 1 blocco di Pietra nello slot di input.
- **Flusso Esecutivo**:
  - `stonecutterScreen.getMenu().getNumberOfVisibleRecipes()` passa da 0 a 14;
  - `InventoryControls` rileva la transizione, annuncia *"14 forme disponibili per il taglio"*, invoca il refresh del `recipesGroup` e imposta il focus sul primo elemento tagliabile;
  - Con le 4 Frecce o il Numpad il giocatore scorre direttamente tra le forme e preme `Invio`/`Spazio` per tagliare.
- **Esito**: Esperienza immediata e autonoma al 100%.

### 🟡 Simulazione 2: Completamento Cottura Fornace a Schermata Aperta (Scenario Meno Comune)
- **Input**: Il giocatore tiene aperto il menu della fornace mentre cucina del ferro grezzo.
- **Flusso Esecutivo**:
  - `furnace.getBurnProgress()` raggiunge il completamento ($1.0 \rightarrow 0.0$) e lo stack di output aumenta;
  - L'assistente riproduce un discreto cue audio/vocale (*"Cottura completata"*);
  - Se il giocatore è su un'altra finestra o chiude la fornace, il watcher si resetta senza falsi positivi.
- **Esito**: Segnale puntuale, zero rumore.

### 🔴 Simulazione 3: Rimozione Rapida degli Oggetti prima del Taglio/Motivo (Caso Limite)
- **Input**: Il giocatore inserisce e rimuove immediatamente la pietra dal tagliapietre o la tintura dal telaio.
- **Flusso Esecutivo**:
  - Il conteggio opzioni torna istantaneamente a 0;
  - I contatori precedenti (`previousStonecutterOptionsCount`, `previousLoomPatternsCount`) si azzerano;
  - I gruppi tornano allo stato base senza eccezioni `NullPointerException` né blocchi di focus.
- **Esito**: Robustezza totale e assenza di crash.

---

## 🛠️ 4. File Coinvolti e Modifiche Previste

### 🎯 Progetto `minecraft-access`:
1. **`src/main/java/org/mcaccess/minecraftaccess/features/inventory_controls/InventoryControls.java`**:
   - Aggiunta tracciamento `previousLoomPatternsCount`;
   - Rigenerazione dinamica gruppi al caricamento di ricette/motivi con auto-selezione del gruppo `recipes`;
   - Tracciamento fine-cottura per `AbstractFurnaceMenu` e fine-distillazione per `BrewingStandMenu` con notifica discreta.
2. **`src/main/resources/assets/minecraft_access/lang/it_it.json`**:
   - Inserimento chiavi:
     * `"minecraft_access.inventory_controls.loom_patterns": "%d motivi disponibili per lo stendardo"`
     * `"minecraft_access.inventory_controls.furnace_smelt_complete": "Cottura completata"`
     * `"minecraft_access.inventory_controls.brewing_complete": "Distillazione completata"`
   - Garanzia ordinamento alfabetico crescente rigoroso.
3. **`src/main/resources/assets/minecraft_access/lang/en_us.json`**:
   - Inserimento chiavi corrispondenti in inglese con ordinamento alfabetico.
4. **`docs/report/REGISTRO_REVISIONI.md`**:
   - Aggiornamento stato di `Rev MC-26.4` da `[APERTA]` a `[IN LAVORAZIONE]`.

---

## 🏁 5. Piano di Verifica & Collaudo

1. **Compilazione & Linting**:
   - `.\gradlew.bat --no-daemon shadowJar` con exit code 0;
   - Verifica ordinamento alfabetico JSON.
2. **Collaudo In-Game di Luca**:
   - Deploy su PrismLauncher (`*26.2*Access*`);
   - Test pratico su Tagliapietre, Telaio e Fornaci.
