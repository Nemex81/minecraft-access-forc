# [COMPLETATO, COLLAUDATO E INTEGRATO] Piano Tecnico Implementativo: Rilevatore Ostacoli Frontali e Dislivelli (ObstacleDetector)

> **STATO: COMPLETATO E COLLAUDATO CON SUCCESSO IN-GAME DA LUCA**  
> **Data di Rilascio e Chiusura**: 26 Agosto 2026  
> **Versione Target**: Minecraft Access 1.12.0 (Minecraft Java Edition 26.2)  
> **Esito Collaudo**: Superato con successo in-game. Riconoscimento a contatto, dislivello assoluto $\Delta Y$ con auto-step a $0.6\text{ m}$, navigazione rive a pelo d'acqua, 4 stili di narrazione adattivi e tasto `Ctrl + Home` per `ObjectTracker`. Promosso nel backup ufficiale del PC Portatile.

---

## 1. Descrizione del Problema e Obiettivi di Accessibilità

### 1.1 Contesto e Necessità
Nella navigazione di Minecraft, un giocatore non vedente ha a disposizione il modulo `FallDetector` per prevenire cadute in dirupi e dislivelli verso il basso. Con l'introduzione di `ObstacleDetector`, camminando in avanti è possibile:
1. Riconoscere in tempo reale la presenza di dislivelli, blocchi o ostacoli frontali.
2. Distinguere con precisione fisica voxel tra:
   - **Cammino piano e auto-step continuo** ($\Delta Y \le 0.6$ blocchi): sentieri sterrati (`dirt_path`), campi arati (`farmland`), fango (`mud`), suolo d'anime (`soul_sand`), lastre a terra da $0.5$ e gradini bassi di scale che il motore di Minecraft scavalca automaticamente senza saltare $\rightarrow$ Silenzioso / Passaggio Libero (`CLEAR`).
   - **Dislivelli saltabili** ($0.6 < \Delta Y \le 1.20$ blocchi): dislivelli reali di 1 blocco (pietra, terra, bauli, balle di fieno) che richiedono la pressione della barra spaziatrice $\rightarrow$ Segnalazione *"Blocco di [Nome], salto possibile"*.
   - **Ostacoli insormontabili** ($\Delta Y > 1.20$ blocchi o quota occhi solida): staccionate ($1.5$), muretti ($1.5$), cancelletti chiusi ($1.5$) e pareti verticali di 2+ blocchi $\rightarrow$ Segnalazione *"Ostacolo di [Nome]"*.
   - **Dislivelli con soffitto basso**: dislivello a terra saltabile ma con spazio di salto o di arrivo ostruito $\rightarrow$ *"Blocco di [Nome], soffitto basso"*.
   - **Ostacoli sospesi ad altezza testa**: rami, travi, foglie a quota occhi $Y+1$ con passaggio a terra libero $\rightarrow$ *"Ostacolo in alto di [Nome]"*.
3. **Navigazione a Pelo d'Acqua & Uscita verso la Riva**:
   - In acqua aperta l'acqua è trasparente (zero collisione, `CLEAR`).
   - Avvicinandosi a riva a pelo d'acqua:
     - Riva regolare da 1 blocco ($\Delta Y \approx 1.0\text{ m}$): *"Sabbia/Erba, salto possibile"* $\rightarrow$ segnala al giocatore non vedente che può saltare fuori dall'acqua.
     - Scogliera/Palizzata alta ($\Delta Y > 1.20\text{ m}$): *"Ostacolo di Pietra/Staccionata"* $\rightarrow$ segnala che la riva è troppo alta e bisogna costeggiare.
   - *Nota*: La navigazione volumetrica 3D in immersione profonda è demandata al futuro modulo indipendente `UnderwaterSonar` (vedi `PROMEMORIA_FUTURO_UNDERWATER_SONAR.md`).
4. **Armonizzazione dei Comandi da Tastiera**:
   - `V` (singolo): Narra posizione XYZ (`PositionNarrator`).
   - `Alt + V`: Ispezione ostacolo frontale / Look-At (`ObstacleDetector`).
   - `Ctrl + Home`: Orienta sguardo verso il Punto di Interesse tracciato senza sneak (`ObjectTracker`).
5. Libertà per l'utente di scegliere dal menu di configurazione della mod il proprio stile di vocalizzazione preferito tra 4 formulazioni cognitive differenti (`BLOCCO`, `DISLIVELLO`, `DIRETTO`, `SALITA`).

---

## 2. Modello Geometrico Voxel & Motore di Rilevamento Universale

### 2.1 Punti Geometrici e Calcolo del Dislivello Assoluto $\Delta Y$
1. **Posizione Attuale del Giocatore**:
   - Quota piedi esatta: $Y_{feet} = \text{playerPos.y}$.
   - Quota occhi: $Y_{eyes} = Y_{feet} + 1.62$.
   - Quota soffitto giocatore: $Y_{feet} + 2.0$.
2. **Calcolo della Sommità del Blocco Frontale ($Y_{top}$)**:
   - Per ciascuna coordinata frontale $(X, Z)$ campionata lungo il raggio da $d = 0.1\text{ m}$ a `range`:
     - $Y_{top} = \text{quota massima di collisione tra i blocchi a terra e sopra terra}$.
     - $\Delta Y = Y_{top} - Y_{feet}$.

### 2.2 Soglie Fisiche di Rilevamento (Allineate a Minecraft Vanilla)
- $\Delta Y \le 0.6\text{ m}$: **Passaggio Libero (`CLEAR`)** $\rightarrow$ Rientra nella capacità di passo automatico (*Step Height* $0.6$) di Minecraft. Nessun avviso su sentieri, erba, lastre e scale.
- $0.6\text{ m} < \Delta Y \le 1.20\text{ m}$: **Dislivello Saltabile (`STEP_CLIMBABLE`)** $\rightarrow$ Dislivello reale superabile con l'impulso di salto standard ($1.25\text{ m}$).
- $\Delta Y > 1.20\text{ m}$: **Ostacolo Insormontabile (`WALL`)** $\rightarrow$ Staccionate ($1.5$), muretti ($1.5$) e barriere alte.

---

### 2.3 Macchina a Stati e Logica Sequenziale di Valutazione

1. **Stato 1: Passaggio Libero (`CLEAR`)**:
   - Condizione: `!headSolid && deltaY <= 0.6`.
   - Vocalizzazione: *"Nessun ostacolo davanti"*.

2. **Stato 2: Ostacolo Frontale Insormontabile (`WALL` / `OBSTACLE`)**:
   - Condizione: `headSolid || deltaY > 1.20`.
   - Segnale acustico: Tono grave/sordo (`NOTE_BLOCK_BASS`, pitch 0.6).
   - Vocalizzazione: *"Ostacolo di [NomeBlocco]"*.

3. **Stato 3: Dislivello Saltabile (`STEP_CLIMBABLE`)**:
   - Condizione: `deltaY > 0.6 && deltaY <= 1.20 && !headSolid && !targetHeadroomSolid && !playerHeadroomSolid`.
   - Segnale acustico: Tono acuto ascendente (`NOTE_BLOCK_PLING`, pitch 1.5).
   - Vocalizzazione adattiva in base allo stile scelto (`BLOCK`, `ELEVATION`, `DIRECT`, `SLOPE`).

4. **Stato 4: Dislivello con Soffitto Basso (`LOW_CEILING`)**:
   - Condizione: `deltaY > 0.6 && deltaY <= 1.20 && !headSolid && (targetHeadroomSolid || playerHeadroomSolid)`.
   - Segnale acustico: Tono grave/sordo (`NOTE_BLOCK_BASS`, pitch 0.6).
   - Vocalizzazione adattiva in base allo stile scelto.

5. **Stato 5: Ostacolo Sospeso ad Altezza Testa (`HEAD_OBSTACLE`)**:
   - Condizione: `deltaY <= 0.6 && headSolid`.
   - Segnale acustico: Tono grave/sordo (`NOTE_BLOCK_BASS`, pitch 0.6).
   - Vocalizzazione: *"Ostacolo in alto di [NomeBlocco]"*.

---

### 2.4 Scala di Priorità Look-At su Ispezione (`Alt + V`)
Quando viene premuto `Alt + V`, la visuale del personaggio viene puntata sul blocco target prioritario del primo ostacolo rilevato:
1. *Priorità 1 (Soffitto giocatore)*: Se il soffitto sopra la testa è solido $\rightarrow$ mira al soffitto.
2. *Priorità 2 (Ostacolo occhi)*: Se la quota occhi è solida $\rightarrow$ mira al blocco ad altezza occhi.
3. *Priorità 3 (Soffitto destinazione)*: Se il soffitto sopra il dislivello è solido $\rightarrow$ mira al soffitto del dislivello.
4. *Priorità 4 (Blocco a terra)*: Se c'è un dislivello saltabile o una staccionata a terra $\rightarrow$ mira al blocco a terra.

---

## 3. Architettura dei File e Modifiche al Codice

### 3.1 Componente Configurazione (`Config.java`)
- Categoria `@ConfigEntry.Category("obstacleDetector")` con classe `ObstacleDetector`:
  - `public boolean enabled = true;`
  - `public boolean playAudioCues = true;`
  - `public float volume = 0.5f;`
  - `public boolean voiceWarning = true;`
  - `public boolean lookAtObstacleOnInspection = true;`
  - `public int delay = 500;`
  - `public int detectionRange = 1;`
  - `@ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)`  
    `public NarrationStyle narrationStyle = NarrationStyle.BLOCK;`

### 3.2 Componente Logica Pura (`ObstacleDetectionUtils.java`)
- Calcolo del dislivello assoluto $\Delta Y$ basato sull'altezza della superficie di collisione.
- Applicazione delle soglie $\Delta Y \le 0.6$ per auto-step e $\Delta Y > 1.20$ per ostacoli insormontabili.
- Funzione `scan(...)` con campionamento progressivo continuo.

### 3.3 Componente Modulo & Eventi (`ObstacleDetector.java`, `ObjectTracker.java` e `MainClass.java`)
- Integrazione con `BalmClientModule` e `ClientPlayingTick.AFTER`.
- Binding tasto `Alt + V` per `ObstacleDetector` e `Ctrl + Home` per `ObjectTracker.look_at_current_object`.

### 3.4 Componente Localizzazione I18N (16 File JSON)
- Registrazione ordinata alfabeticamente nei 16 file JSON di lingua delle 21 chiavi con traduzioni convalidate.

---

## 4. Verifica e Ciclo di Vita
- **Test Automatici**: `ObstacleDetectorTest.java` (100% superati).
- **Collaudo Manuale**: Eseguito da Luca in ambiente di sopravvivenza in-game (sentieri, gradini, staccionate, muretti, Look-At con `Ctrl + Home`).
- **Chiusura**: Modulo promosso nel backup ufficiale e piano archiviato in `archivio completati/`.
