# Piano Tecnico Formale: Feed Mirino in Movimento (WASD), Riqualificazione Tasto "B", Armonizzazione con ObstacleDetector e Mentor Vocale Adattivo (Rev MC-27.1)
# Autore: Luca & Antigravity
# Data: 2026-09-02
# Ambito: Repository `minecraft-access`
# Incremento Versione Target (AVF): Minor Revision (Rev MC-27.1 — v1.12.0-SNAPSHOT)
# Stato: In Lavorazione / Affinamento PRAPI

---

## 📌 1. Obiettivo e Quadro di Riferimento

Il presente Piano Tecnico definisce l'architettura, le strutture dati e i dettagli implementativi per:
1. **Feed Mirino in Movimento (WASD)**: Notifiche fluide e calibrate per passi laterali `A`/`D` e frontali `W`/`S` con 4 modalità Cloth Config *(COMPLETATO)*;
2. **Riqualificazione Moderna del Tasto `B` (Mano Sinistra)**: Lettura istantanea forzata del mirino su `CrosshairFeedbackManager` *(COMPLETATO)*;
3. **Blindatura di `Alt + B`**: Piena affidabilità e parità con `Alt + Numpad 7` per la scansione 3D risorse *(COMPLETATO)*;
4. **Armonizzazione con `ObstacleDetector`**: Periodo di grazia (250ms) per prevenire collisioni vocali in cammino *(COMPLETATO)*;
5. **Rev MC-27.1 — Mentor Vocale Adattivo con Risoluzione Real-Time dei Tasti (Keybinding Introspection)**:
   - Direzione spaziale contestuale reale dell'ostacolo (`a sinistra`, `a destra`, `davanti`, `dietro`) ricavata dall'input di movimento attivo al momento dell'impatto;
   - Risoluzione dinamica dei tasti a runtime interrogando le registrazioni reali in Kuma/Minecraft (`client.options.keyJump`, `ObstacleDetector.keyInspect`, `NumpadControls`), eliminando qualsiasi testo hardcodato.

---

## 🏛️ 2. Dettagli Architetturali Rev MC-27.1 (Mentor Intelligente)

### 2.1 Cattura del Contesto Spaziale di Collisione
In [`PlayerContextEngine.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/context/PlayerContextEngine.java):
- **Se** il giocatore si muove e scontra un blocco (`player.horizontalCollision == true`);
- **Allora** ricava l'angolo relativo dell'input (`calculateIntendedMoveAngle(up, down, left, right)`);
- **E** associa la parola direzionale localizzata:
  - Input `A` -> *"a sinistra"* / *"on your left"*;
  - Input `D` -> *"a destra"* / *"on your right"*;
  - Input `W` -> *"davanti"* / *"in front of you"*;
  - Input `S` -> *"dietro"* / *"behind you"*.

### 2.2 Risoluzione Dinamica dei Tasti (Keybinding Introspection)
In [`ContextualMentor.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/mentor/ContextualMentor.java):
- I suggerimenti mentor supportano la risoluzione dinamica degli argomenti `%s`:
  - `HINT_WALL_STUCK`:
    - Arg 1: Direzione contestuale (es. *"a sinistra"*);
    - Arg 2: Tasto Salto reale (es. `"Spazio"` ricavato da `client.options.keyJump.getTranslatedKeyMessage().getString()`);
    - Arg 3: Tasto Ispezione reale (es. `"Alt + V"` ricavato dal mapping registrato per `obstacle_detector.inspect`).
  - `HINT_IDLE_STUCK`:
    - Arg 1: Tasto Centratura visuale (es. `"5 del tastierino"` / `"M"` ricavato da `NumpadControls.keyCenterHorizon` o `CameraControls`);
    - Arg 2: Tasto Radar POI (es. `"Fine"` / `"Numpad 5 orient"`).

---

## 🛠️ 3. Mappa dei File Coinvolti (Rev MC-27.1)

1. `PlayerContextSnapshot.java` & `PlayerContextEngine.java`
   - Aggiunta del campo direzionale relativo alla collisione/movimento.
2. `MentorRule.java` & `MentorRuleRegistry.java`
   - Supporto per provider di argomenti dinamici nei messaggi didattici.
3. `ContextualMentor.java`
   - Risoluzione a runtime dei tasti e formattazione con `String.format(I18n.get(rule.messageKey()), ...args)`.
4. `it_it.json` & `en_us.json`
   - Aggiornamento delle stringhe didattiche per accogliere i placeholder dinamici `%s` in ordine alfabetico crescente.

---

## 🛡️ 4. Protocollo di Validazione Preventiva (7 Assi)
1. **Validità**: Elimina l'avviso errato "di fronte" quando si impatta lateralmente e fornisce il comando esatto per ispezionare l'ostacolo.
2. **Efficacia**: Massima chiarezza didattica per nuovi e veterani giocatori.
3. **Coerenza**: Zero discrepanze tra tasti configurati nelle opzioni e tasti pronunciati dal Mentor.
4. **Completezza**: Copre collisioni su tutti i 4 assi WASD e inattività prolungata.
5. **Precisione**: Risoluzione da `KeyMapping.getTranslatedKeyMessage()` nativo.
6. **Prestazioni**: Risoluzione lazy on-demand solo al momento dell'erogazione del suggerimento.
7. **Assenza di Regressioni**: Tutti i cooldown e i flag di configurazione rimangono attivi.
