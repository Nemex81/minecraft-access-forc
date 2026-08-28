# Piano Tecnico Implementativo: Potenziamento del Rilevatore di Cadute (Fall Detector)

## 1. Obiettivo e Sintesi del Progetto

Il presente documento definisce la strategia, l'architettura tecnica e i dettagli implementativi per il miglioramento del sistema di rilevamento e prevenzione cadute in **Minecraft Access 1.12.0** su **Minecraft Java 26.2**.

L'aggiornamento risolve il problema delle cadute accidentali durante la corsa veloce (`sprint`) e lo sprint-jumping, introducendo:
1. **Look-Ahead continuo ad alta reattività (ad ogni tick)**: Intercetta i burroni lungo il vettore di avanzamento del giocatore prima del ciglio, anche durante i salti a mezz'aria.
2. **Macchina a Stati per Freno Automatico e Ripristino Corsa**: Interrompe lo sprint e passa a camminata quando si punta verso il baratro entro la distanza di sicurezza (default 3 blocchi), memorizza lo stato iniziale del giocatore (`wasSprintingBeforeIntervention`), e ripristina fluidamente la corsa quando si torna in zona sicura.
3. **Avviso Vocale Direzionale (Anti-Spam)**: Comunica direzione relativa, distanza e profondità del baratro una sola volta all'ingresso nella zona di pericolo.
4. **Comando On-Demand "Sonda Terreno / Ispezione Burroni" (`Alt + F`)**: Esegue una scansione a 360° su richiesta da fermi, vocalizzando la buca più vicina o confermando *"Nessuna buca nei paraggi"*.
5. **Comando Globale "Ripeti Ultimo Parlato" (`Alt + G`)**: Consente di riascoltare istantaneamente con la mano sinistra l'ultimo messaggio vocalizzato dalla sintesi vocale.
6. **Configurabilità Totale**: Nuove opzioni nel menu `Config` e localizzazione completa in italiano (`it_it.json`) e inglese (`en_us.json`).

---

## 2. Modifiche Architetturali per File

### A. `Config.java`
Aggiunta delle nuove impostazioni all'interno della classe statica `Config.FallDetector`:
- `public boolean autoSlowdown = true;` (Abilita/Disabilita rallentamento automatico).
- `public int slowdownDistance = 3;` (Distanza di sicurezza in blocchi dal bordo).
- `public boolean autoRestoreSprint = true;` (Ripristino automatico dello sprint all'uscita).
- `public boolean voiceWarning = true;` (Abilita/Disabilita l'avviso vocale automatico).

### B. `MainClass.java`
Implementazione del buffer dell'ultima notifica vocale:
- Variabile statica: `private static String lastNarrationText = "";`
- Modifica del metodo `narrate(String text, boolean interrupt)`: memorizzazione di `text` in `lastNarrationText` se non vuoto.
- Nuovo metodo: `public static void repeatLastNarration()` per pronunciare nuovamente `lastNarrationText` con `interrupt = true` o *"Nessun messaggio precedente"* se il buffer è vuoto.

### C. `FallDetector.java`
Ristrutturazione completa e potenziamento della classe:
1. **Gestione del Tick (`tick`)**:
   - Esecuzione continua a ogni tick di gioco per il **Look-Ahead Direzionale** (indipendentemente da `player.onGround()`, per proteggere anche durante i salti).
   - Mantenimento del timer `config.delay` per la scansione ambientale audio 3D (suono incudine).
2. **Algoritmo Look-Ahead Direzionale (`checkDirectionalSafety`)**:
   - Calcolo del vettore di spostamento/puntamento del giocatore.
   - Controllo dei blocchi lungo la traiettoria da 1 a `slowdownDistance` blocchi in avanti.
   - Calcolo della profondità del vuoto verticale (`getDepth`).
   - Se pericolo rilevato:
     - Se `autoSlowdown == true`:
       - Se `player.isSprinting()`: imposta `wasSprinting = true` e chiama `player.setSprinting(false)`.
       - Imposta `safetyIntervention = true`.
     - Se `voiceWarning == true` e non ancora notificato per questa posizione: vocalizza l'avviso una sola volta.
   - Se pericolo cessato (giocatore allontanato o girato):
     - Se `safetyIntervention == true`:
       - Se `autoRestoreSprint && wasSprinting`: ripristina `player.setSprinting(true)` se il giocatore sta ancora avanzando.
       - Reset completo dello stato (`safetyIntervention = false`, `wasSprinting = false`).
3. **Keybinding `Alt + F` (Sonda Terreno / Fall Inspector)**:
   - Registrazione con Kuma: `InputBinding.key(InputConstants.KEY_F, KeyModifiers.of(KeyModifier.ALT))`.
   - Esegue BFS a 360° entro `config.range`.
   - Se trova burroni: calcola la buca più vicina, formula direzione (`NarrationUtils.narrateRelativePositionOfPlayerAnd`), distanza e profondità, ed emette suono + messaggio vocale.
   - Se non trova burroni: pronuncia *"Nessuna buca nei paraggi"*.
4. **Keybinding `Alt + G` (Ripeti Ultimo Parlato)**:
   - Registrazione con Kuma: `InputBinding.key(InputConstants.KEY_G, KeyModifiers.of(KeyModifier.ALT))`.
   - Richiama `MainClass.repeatLastNarration()`.

### D. File di Localizzazione (`it_it.json` e `en_us.json`)
Aggiunta delle chiavi di traduzione per:
- Voci di configurazione Cloth Config (`autoSlowdown`, `slowdownDistance`, `autoRestoreSprint`, `voiceWarning`).
- Nomi dei Keybinding nel menu Controlli (`inspect_fall`, `repeat_last_narration`).
- Messaggi di sintesi vocale:
  - `minecraft_access.fall_detector.warning`: *"Attenzione: burrone %s, profondità %s blocchi"*
  - `minecraft_access.fall_detector.pit_found`: *"Burrone %s, profondità %s blocchi"*
  - `minecraft_access.fall_detector.no_pit_nearby`: *"Nessuna buca nei paraggi"*
  - `minecraft_access.other.no_previous_narration`: *"Nessun messaggio precedente da ripetere"*

---

## 3. Piano di Verifica e Validazione (Test Plan)

1. **Test di Compilazione**:
   - Esecuzione `./gradlew checkstyleMain` e `./gradlew compileJava` per garantire zero errori di sintassi e conformità al checkstyle del progetto.
2. **Test di Funzionamento Dinamico**:
   - *Test Corsa verso Burrone*: Scattare a piena velocità verso un burrone/crepaccio. Verificare che a 3 blocchi lo sprint si interrompa, il passo rallenti e la voce avvisi una sola volta.
   - *Test Ripristino Corsa*: Girarsi di 180° e continuare ad avanzare. Verificare che lo sprint si riattivi automaticamente.
   - *Test Camminata Normale*: Avvicinarsi camminando normalmente a una buca e poi allontanarsi. Verificare che il giocatore continui a camminare e NON parta in corsa involontaria.
   - *Test Sonda Manuale (`Alt + F`)*: Premere `Alt + F` in campo aperto ("Nessuna buca nei paraggi") e vicino a un burrone ("Burrone a X blocchi...").
   - *Test Ripetizione Parlato (`Alt + G`)*: Eseguire qualsiasi azione con notifica e premere `Alt + G` per verificare l'immediata ripetizione vocale.
