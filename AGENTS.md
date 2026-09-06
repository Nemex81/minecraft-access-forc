# Minecraft Access — Direttive di Progetto per Codex / ChatGPT (ASTRALIS v3.0.0)
# Autore: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA)
# Target AI: Codex / ChatGPT (Copilota Ausiliario e Peer Programmer)
# Framework: ASTRALIS v3.0.0
# Eredita da: C:\Users\nemex\.codex\AGENTS.md (Direttive Globali)
# Hub di Contesto Master: GEMINI.md

Questo repository implementa la mod di accessibilità per non vedenti **Minecraft Access**.
Tutta l'interazione con Minecraft, i menu, il mondo di gioco e gli strumenti di sviluppo avviene tramite sintesi vocale (NVDA / SAPI), feedback acustici 3D e comandi da tastiera completi (ZERO MOUSE).

---

## ⚡ 1. REGOLA DI INGAGGIO E CARICAMENTO PROGRESSIVO (ON-DEMAND)

Per garantire la massima velocità di risposta e preservare la finestra di contesto di Codex:
- **Richieste brevi, chiarimenti o domande veloci**: usa unicamente questo file `AGENTS.md` senza caricare la documentazione estesa del progetto.
- **Pianificazione, implementazione, diagnosi o test**: attiva la consultazione approfondita delle Fonti di Verità (Sezione 2) caricando **esclusivamente le 1–3 schede in `knowledge/` pertinenti** al modulo da toccare (usando l'Indice Ragionato in `GEMINI.md`).
- **Divieto di sovraccarico**: non caricare mai in massa l'intera cartella `knowledge/`, i piani archiviati in `docs/piani/completati/` o le revisioni chiuse in `docs/report/ARCHIVIO_REVISIONI.md`.

---

## 🏛️ 2. FONTI DI VERITÀ E REGOLE DI PROGETTO (POINTER HUB DRY)

Quando il compito richiede pianificazione, implementazione, diagnosi o test approfonditi (secondo la Regola di Ingaggio sopra), consulta i seguenti nodi documentali:
- `GEMINI.md`: Hub centrale di contesto con le regole fondamentali del dominio voxel e i protocolli operativi.
- `knowledge/`: Base di conoscenza modulare (architettura, audio 3D, voxel raycasting, controlli tastiera, diario modifiche).
- `docs/strategie/attive/`: Strategie logico-cognitive UPCS di Fase 0 in corso di elaborazione.
- `docs/piani/attivi/`: Piani tecnici formali delle attività correnti (Sotto-Fase 1A/1B).
- `docs/report/REGISTRO_REVISIONI.md`: Registro aperto delle anomalie e revisioni emerse dai collaudi (RRU).

---

## 🛡️ 3. VINCOLI TECNICI INVIOLABILI

1. **Regola 0 (Default Consultivo Permanente & Gating Semantico)**:
   - Non effettuare MAI modifiche autonome al codice sorgente o ai file di configurazione senza il comando esplicito di Luca (*"procedi"*, *"applica"*, *"esegui"*).
   - Richieste come *"cosa ne pensi?"*, *"valuta"*, *"analizza"* richiedono risposte esclusivamente consultive.
   - Gating Semantico Fase 1: *"passa alla fase 1"* autorizza SOLO la stesura del piano tecnico (1A) con Stop Obbligatorio prima del codice (1B).
   - Validazione preventiva: proposta verificata sui **7 Assi di Qualità** e **3 Livelli di Simulazione**.
2. **Accessibilità Vocale & Zero Mouse**:
   - Nessuna GUI o funzionalità deve richiedere l'uso del mouse.
   - Ogni notifica passa attraverso `MainClass.narrate`.
   - I volumi sonori dell'audio 3D posizionale sono congelati per ragioni di sicurezza acustica tra `0.7f` e `0.8f`.
3. **Rigore I18N & Ordinamento Alfabetico JSON**:
   - Gestiamo unicamente le localizzazioni `it_it.json` ed `en_us.json`.
   - In tutti i file `.json` in `src/main/resources/assets/minecraft_access/lang/`, **le chiavi devono essere rigorosamente ordinate in ordine alfabetico crescente**, altrimenti falliranno i test di integrazione continua (`jq -e "keys != keys_unsorted"`).
4. **Stack Tecnologico, Resilienza Build & Flag Anti-Lock**:
   - Target: Minecraft 26.2 (1.21.x), Fabric + NeoForge (Architectury Loom), SpongePowered Mixin, Java 25.
   - Build comando: `.\gradlew.bat --no-daemon --no-watch-fs shadowJar`
   - Test comando: `.\gradlew.bat --no-daemon --no-watch-fs test`
   - Non avviare mai demoni Gradle persistenti: usare sempre `--no-daemon --no-watch-fs` per prevenire lock di file su OneDrive/Windows.
5. **Protocollo 12 — Inner Codex Pattern (6 Cancelli Inviolabili)**:
   - Cancello 1 (Rifiuto Patching Euristico): divieto di forzare budget A*, pesi o ritardi artificiali;
   - Cancello 2 (Hardware Grounding): probing GLFW puro per takeover manuale (`keySneak`);
   - Cancello 3 (Hitbox & Clearance Continua): clearance occhi/testa ($0.6 \times 1.8\text{ m}$) e forme di collisione sottili ($0.1875\text{ m}$ per scale a pioli);
   - Cancello 4 (Named Contracts D0..DN / S1..SN): contratti formali numerati;
   - Cancello 5 (Determinismo Headless): test seams package-private a 0 ms senza `Thread.sleep`;
   - Cancello 6 (Budget Token & Anti-Bloat Normativo): file router $\le 250$ righe, zero duplicazioni nei prompt.
6. **Meta-Governance & Coesistenza Multi-AI**:
   - Conformità ai 6 Canoni ASTRALIS: questo router resta snello ($\le 250$ righe). Le modifiche al codice sono gestite primariamente con Antigravity, mentre Codex opera in consultazione critica, peer review e pianificazione parallela.
