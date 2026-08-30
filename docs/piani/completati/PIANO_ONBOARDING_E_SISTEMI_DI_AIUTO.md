# Piano Tecnico di Implementazione: Onboarding & Sistemi di Aiuto per Principianti
# Percorso: docs/piani/attivi/PIANO_ONBOARDING_E_SISTEMI_DI_AIUTO.md

**Modulo**: `minecraft-access` (Fork Luca)  
**Obiettivo**: Perfezionare l'architettura integrata di onboarding (Mentor Contestuale + Accademia a Mini-Missioni) con:
1. Avanzamento sequenziale automatico (`autoAdvanceMissions`).
2. Guard Rail di protezione fisica e disabilitazione adattiva nell'Hub per prevenire l'avvio di missioni non compatibili con la modalità di gioco (`GameType`).
3. Precisione assoluta dei comandi suggeriti vocalmente per tastiera estesa italiana e tastierino numerico.

---

## 👥 Specifiche e Vincoli Rispettati

- **Priorità Vocale Assoluta**: I messaggi didattici e i suggerimenti contestuali usano lo scudo temporale dinamico `HelpNarrator.narrateHelp`, silenziando la lettura continua del mirino (`NarrateCrosshair`) e i rilevatori per tutta la durata della frase. Opzione `helpPriorityOverride: true/false` configurabile.
- **Avanzamento Sequenziale Automatico**: Al termine di una missione, dopo 4.5 secondi di respiro viene avviata automaticamente la missione successiva non completata coerente con la GameMode. Toggle `autoAdvanceMissions` configurabile nell'Hub e nei settings.
- **Guard Rail & Adattività GameMode**:
  - `AcademyManager.startMission`: impedisce fisicamente l'avvio di missioni incoerenti con la GameMode attiva (es. missione di volo in Sopravvivenza) e vocalizza una spiegazione immediata.
  - `AcademyAndHelpScreen`: disabilita i pulsanti delle missioni incompatibili (`button.active = false`) e aggiunge il tag vocale esplicito `[Richiede Modalità Creativa]` / `[Richiede Modalità Sopravvivenza]`.
- **Mappa Comandi Parlati**:
  - Allineamento Orizzonte/Nord: `M` (estesa) e `5` (tastierino).
  - Azione Primaria (Attacco/Scavo): `è` (estesa) e `0` (tastierino).
  - Azione Secondaria (Piazzamento/Uso/Cibo): `+` (estesa) e `Invio` (tastierino).
- **Accessibilità Totale (Zero Mouse)**: Navigazione 100% da tastiera per tutti i flussi.
- **Rigore I18N**: Chiavi in `it_it.json` ed `en_us.json` rigorosamente ordinate alfabeticamente.

---

## 🏛️ Dettaglio delle Modifiche da Applicare

### 1. `features.academy.AcademyManager`
- In `startMission(Mission mission)`:
  - Verificare la modalità di gioco attiva (`Minecraft.getInstance().gameMode`).
  - Se `mission.id().startsWith("CREATIVE_")` e il giocatore non è in modalità Creativa:
    - Rifiutare l'avvio e chiamare `HelpNarrator.narrateHelp(I18n.get("minecraft_access.academy.error_requires_creative"), true)`.
    - Ritorno immediato `return;`.
  - Se `mission.id().startsWith("SURVIVAL_")` e il giocatore è in Creativa/Spettatore:
    - Rifiutare l'avvio e chiamare `HelpNarrator.narrateHelp(I18n.get("minecraft_access.academy.error_requires_survival"), true)`.
    - Ritorno immediato `return;`.

### 2. `features.help.AcademyAndHelpScreen`
- Nella costruzione dei pulsanti delle missioni:
  - Rilevare se la missione è compatibile con la modalità attiva.
  - Se non compatibile:
    - Impostare `button.active = false`.
    - Modificare l'etichetta del pulsante: `[Titolo Missione] - [Richiede Modalità Creativa]` (o `[Richiede Modalità Sopravvivenza]`).
  - Lo screen reader NVDA leggerà chiaramente lo stato disattivato e il motivo dell'incompatibilità.

### 3. File di Lingua I18N (`it_it.json` ed `en_us.json`)
- Inserire le nuove chiavi:
  - `minecraft_access.academy.error_requires_creative`:
    - IT: `"Impossibile avviare questa missione: richiede la Modalità Creativa."`
    - EN: `"Cannot start this mission: requires Creative Mode."`
  - `minecraft_access.academy.error_requires_survival`:
    - IT: `"Impossibile avviare questa missione: richiede la Modalità Sopravvivenza."`
    - EN: `"Cannot start this mission: requires Survival Mode."`
  - `minecraft_access.gui.academy_hub.requires_creative_tag`:
    - IT: `"Richiede Modalità Creativa"`
    - EN: `"Requires Creative Mode"`
  - `minecraft_access.gui.academy_hub.requires_survival_tag`:
    - IT: `"Richiede Modalità Sopravvivenza"`
    - EN: `"Requires Survival Mode"`
- Mantenere l'ordinamento alfabetico rigoroso delle chiavi JSON.

### 4. Suite di Test Diagnostici (`HelpSystemTest.java`)
- Aggiungere test dedicati al Guard Rail di `AcademyManager`:
  - Tentativo di avvio missione creativa in modalità Sopravvivenza -> Bloccato con successo.
  - Tentativo di avvio missione sopravvivenza in modalità Spettatore -> Bloccato con successo.
  - Avvio corretto di missione coerente -> Eseguito regolarmente.

---

## 📋 Pipeline di Realizzazione a 4 Fasi

### Fase 1: Sviluppo, Rigore I18N & Test Automatici Gradle
1. Modifica di `AcademyManager.java` e `AcademyAndHelpScreen.java`.
2. Aggiornamento e ordinamento dei file I18N (`it_it.json`, `en_us.json`).
3. Aggiornamento della suite di test in `HelpSystemTest.java`.
4. Compilazione e validazione con Java 25:
   ```powershell
   $env:JAVA_HOME = "C:\Users\nemex\AppData\Roaming\PrismLauncher\java\java-runtime-epsilon"
   .\gradlew.bat --no-daemon test shadowJar
   ```

### Fase 2: Deploy Provvisorio & Collaudo Manuale In-Game con NVDA
- Distribuzione del nuovo JAR in entrambe le istanze PrismLauncher (`Minecraft 26.2 Access 1.12.0` e `Minecraft 26.2 Access - Server Tenuta`).
- Collaudo manuale di Luca con NVDA:
  1. Verifica che in un mondo Sopravvivenza i pulsanti delle missioni Creative siano disabilitati con la voce *"Richiede Modalità Creativa (Disattivato)"*.
  2. Verifica che le missioni di Sopravvivenza avanzino in sequenza automatica dopo 4.5s.
  3. Verifica in un mondo Creativo dell'attivazione esclusiva delle missioni Creative.

### Fase 3: Chiusura Tecnica & Documentazione Viva
- Aggiornamento documentale viva (`changelog.md`, `architecture.md`).
- Spostamento di questo piano in `docs/piani/completati/`.
- Domanda Ponte formale per la transizione alla Fase 4.

### Fase 4: Auto-Apprendimento Continuo
- Aggiornamento schede `knowledge/` e governance su autorizzazione di Luca.
