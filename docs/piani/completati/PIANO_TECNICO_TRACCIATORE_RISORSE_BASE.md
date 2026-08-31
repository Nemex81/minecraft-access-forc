# Piano Tecnico [ATTIVO]: Implementazione del Tracciatore Risorse Base di Sopravvivenza (Legno, Pietra, Cibo)
# Autore: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA)
# Co-Autore: Antigravity (Senior AI Pair Programmer & Software Engineer)
# Progetto: Minecraft Access
# Data: 2026-08-30
# Riferimento Strategico: docs/strategie/STRATEGIA_TRACCIATORE_RISORSE_BASE.md

---

## 🎯 1. OBIETTIVI E SPECIFICHE FUNZIONALI

1. **Autonomia & Isolamento dal POI Radar**:
   - Modulo indipendente che non altera il cursore né la selezione dei gruppi in `POIManager` / `ObjectTracker`.
2. **Triade di Sopravvivenza (3 Categorie Mirate)**:
   - **Legno**: Tronchi naturali (`#minecraft:logs`), banchi da lavoro posizionati (`Blocks.CRAFTING_TABLE`), assi piazzate (`#minecraft:planks`).
   - **Pietra (con Regola Tassativa Aria)**: Pietra, ciottoli, deepslate, andesite, diorite, granito, arenaria **solo con almeno una faccia esposta all'aria** (`level.getBlockState(neighbor).isAir()`). Zero pietra cieca sepolta.
   - **Cibo**: Animali passivi commestibili, colture 100% mature (`CropBlock.isMaxAge() == true`), cespugli di bacche mature, cibo droppato a terra.
3. **Semantica Vocale Standardizzata**:
   - Formula: `[Nome Risorsa] [Distanza e Vettore Relativo], [Cardinale e Gradi Bussola], [Dislivello Verticale]`.
   - Esempio: *"Risorse: Legno 8 blocchi avanti, nord a 10 gradi, in alto 2 blocchi; Pietra 14 blocchi a destra, est a 90 gradi, in basso 3 blocchi; Cibo 6 blocchi indietro a sinistra, sud-ovest a 220 gradi, stesso livello."*
4. **Doppia Modalità di Attivazione (Manuale & Periodica)**:
   - Manuale: **`Alt + Numpad 7`** (Layer 2 Numpad) e **`Alt + B`** (Tastiera estesa / laptop con la sola mano sinistra).
   - Periodica in background: Toggle e intervallo (default 30s) con smart debounce anti-spam.
5. **Integrazione Completa & Configurazione**:
   - Sezione dedicata `SurvivalTracker` in `Config.java` (raggio 8..64 blocchi, intervallo, filtri selettivi).
   - Funzione registrata nell'Access Menu (`M` / `Alt + Numpad Enter`).
   - File di localizzazione `it_it.json` e `en_us.json` rigorosamente ordinati alfabeticamente.

---

## 🏗️ 2. ARCHITETTURA DELLE MODIFICHE

### 2.1. Nuovo Package `features.survival_tracker`
- `SurvivalResourceType.java`: Enum delle 3 categorie (`WOOD`, `STONE`, `FOOD`).
- `SurvivalResourceTarget.java`: Record con dettagli geometrici, distanze, coordinate e testi descrittivi.
- `SurvivalScanner.java`: Motore di scansione voxel ed entità a raggio sferico/cubico con filtro facce aria.
- `SurvivalResourceTracker.java`: `BalmClientModule` principale con registrazione comandi Kuma, tick handler periodico e generazione vocale.

### 2.2. Configurazione (`Config.java`)
- Aggiunta di `@ConfigEntry.Category("survivalTracker")` `public SurvivalTracker survivalTracker = new SurvivalTracker();`.
- Campi configurabili con annotazioni BoundedDiscrete e Gui.EnumHandler.

### 2.3. Integrazione Modulo Master (`MainClass.java`)
- Istanza e registrazione `SurvivalResourceTracker`.

### 2.4. Access Menu (`AccessMenu.java`)
- Aggiunta funzione `"narrate_survival_resources"`.

### 2.5. Localizzazioni I18N (`it_it.json`, `en_us.json`)
- Inserimento chiavi ordinate alfabeticamente.

---

## 🛡️ 3. VALIDAZIONE PREVENTIVA A 7 ASSI

1. **Validità**: Rispetta appieno le API Minecraft 26.2/1.21.x e il framework Balm/Kuma.
2. **Efficacia**: Fornisce al giocatore non vedente le informazioni essenziali per il survival con un solo tocco di tastiera.
3. **Coerenza**: Sintassi vocale unificata con il navigatore `AutoWalk` e `PlayerPositionUtils`.
4. **Completezza**: Gestisce tutte e 3 le risorse, tutti i quadranti angolari, dislivelli positivi/negativi/nulli e casi di risorsa assente.
5. **Precisione**: Il filtro voxel sulle 6 facce aria impedisce falsi positivi da pietra cieca sotterranea.
6. **Affidabilità & Prestazioni**: Scansione delimitata a $R \le 64\text{ m}$ con campionamento ottimizzato e debounce; zero lag.
7. **Assenza di Regressioni & Prevenzione Anomalie**: Modulo isolato che non modifica `ObjectTracker` né altera i controlli di movimento (Zero Shift).

---

## 🧪 4. PIANO DI VERIFICA & COLLAUDO

1. **Test Automatici (JUnit / Gradle)**:
   - Creazione di `SurvivalTrackerTest.java` per validare calcolo vettori relativi, gradi bussola, dislivelli, filtro facce aria e composizione stringhe.
   - Compilazione ed esecuzione test con Gradle: `.\gradlew.bat --no-daemon test shadowJar`.
2. **Collaudo Manuale In-Game (Fase 2)**:
   - Deploy su PrismLauncher.
   - Collaudo di Luca con screen reader NVDA nel mondo di gioco (scansione manuale `Alt + Numpad 7`, verifica pietra affiorante vs sepolta, verifica cibo e alberi/banchi da lavoro).
