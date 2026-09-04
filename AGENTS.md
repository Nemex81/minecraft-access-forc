# Minecraft Access — Direttive di Progetto per Codex / ChatGPT (ASTRALIS v2.6.3)
# Autore: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA)
# Target AI: Codex / ChatGPT (Copilota Ausiliario e Peer Programmer)
# Framework: ASTRALIS v2.6.3
# Eredita da: C:\Users\nemex\.codex\AGENTS.md (Direttive Globali)
# Hub di Contesto Master: GEMINI.md

Questo repository implementa la mod di accessibilità per non vedenti **Minecraft Access**.
Tutta l'interazione con Minecraft, i menu, il mondo di gioco e gli strumenti di sviluppo avviene tramite sintesi vocale (NVDA / SAPI), feedback acustici 3D e comandi da tastiera completi (ZERO MOUSE).

---

## 🏛️ 1. FONTI DI VERITÀ E REGOLE DI PROGETTO

Prima di proporre modifiche o analizzare il codice, consulta sempre i seguenti file di riferimento:
- `GEMINI.md`: Hub centrale di contesto con le regole fondamentali del dominio voxel e i protocolli operativi.
- `knowledge/`: Base di conoscenza modulare (architettura, audio 3D, voxel raycasting, controlli tastiera, diario modifiche).
- `docs/piani/attivi/`: Piani tecnici formali delle attività correnti.
- `docs/report/REGISTRO_REVISIONI.md`: Registro aperto delle anomalie e revisioni emerse dai collaudi.

---

## ⚡ 2. REGOLA DI CARICAMENTO PROGRESSIVO (ON-DEMAND)

Per garantire la massima velocità e preservare la finestra di contesto di Codex:
- **Richieste brevi, chiarimenti o domande veloci**: usa unicamente questo file `AGENTS.md` senza caricare la documentazione del progetto.
- **Pianificazione, implementazione, diagnosi o test**: consulta prima `GEMINI.md` e carica **esclusivamente le 1–3 schede in `knowledge/` pertinenti** al modulo da toccare (usando l'Indice Ragionato in `GEMINI.md`).
- **Divieto di sovraccarico**: non caricare mai in massa l'intera cartella `knowledge/`, i piani archiviati in `docs/piani/completati/` o le revisioni chiuse in `docs/report/ARCHIVIO_REVISIONI.md`.

---

## 🛡️ 3. VINCOLI TECNICI INVIOLABILI

1. **Regola 0 (Default Consultivo Permanente)**:
   - Non effettuare MAI modifiche autonome al codice sorgente o ai file di configurazione senza il comando esplicito di Luca (*"procedi"*, *"applica"*, *"esegui"*).
   - Richieste come *"cosa ne pensi?"*, *"valuta"*, *"analizza"* richiedono risposte esclusivamente consultive.
2. **Accessibilità Vocale & Zero Mouse**:
   - Nessuna GUI o funzionalità deve richiedere l'uso del mouse.
   - Ogni notifica passa attraverso `MainClass.narrate`.
   - I volumi sonori dell'audio 3D posizionale sono congelati per ragioni di sicurezza acustica tra `0.7f` e `0.8f`.
3. **Rigore I18N & Ordinamento Alfabetico JSON**:
   - Gestiamo unicamente le localizzazioni `it_it.json` ed `en_us.json`.
   - In tutti i file `.json` in `src/main/resources/assets/minecraft_access/lang/`, **le chiavi devono essere rigorosamente ordinate in ordine alfabetico crescente**, altrimenti falliranno i test di integrazione continua (`jq -e "keys != keys_unsorted"`).
4. **Stack Tecnologico & Compilazione**:
   - Target: Minecraft 26.2 (1.21.x), Fabric + NeoForge (Architectury Loom), SpongePowered Mixin, Java 25.
   - Build comando: `.\gradlew.bat --no-daemon shadowJar`
   - Test comando: `.\gradlew.bat --no-daemon test`
   - Non avviare mai demoni Gradle persistenti (usare sempre `--no-daemon` per evitare file lock su cloud/OneDrive).
