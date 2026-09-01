# Piano Tecnico Integrale (Fase 1A): Intento di Movimento Continuo, Ispezione Panoramica a 360° & Configurazione Modulare (ObstacleDetector)

## 📌 1. Obiettivo e Quadro di Riferimento
Rendere il modulo **Rilevatore di Ostacoli** (`ObstacleDetector`) un sistema completo, ergonomico e personalizzabile per l'orientamento spaziale e la sicurezza del giocatore non vedente:
1. **Intento di Movimento Continuo (Anti-Ammutolimento)**: Continuare a segnalare vocalmente e con audio 3D posizionale la barriera di collisione quando il giocatore preme i tasti di movimento (`W`, `A`, `S`, `D`) contro un ostacolo, superando l'azzeramento della velocità fisica imposto dal motore di Minecraft.
2. **Ispezione Panoramica a 360° su `Alt + V`**: Scansione a lungo raggio configurabile (da 1 a 24 blocchi) su tutti i settori (4 punti cardinali o 8 direzioni con diagonali) con indicazione della distanza in blocchi e verifica della clearance verticale del soffitto/volta ($Y+2$) per la fattibilità del salto.
3. **Piena Personalizzazione GUI**: 10 parametri configurabili con slider bounded e pulsanti Enum per calibrare delay, raggi d'azione differenziati, modalità direzionale e volumi.

---

## 🛠️ 2. Architettura Tecnica per Componente

### A. Configurazione Estesa (`Config.java`)
Nella classe interna `Config.ObstacleDetector`, integrazione dei parametri:
- `public boolean enabled = true;` (Abilitazione generale modulo)
- `public boolean voiceWarning = true;` (Avviso vocale automatico in camminata)
- `public boolean playAudioCues = true;` (Segnali acustici 3D posizionali)
- `@ConfigEntry.BoundedDiscrete(min = 0, max = 100)` -> `public int volume = 50;` (Volume effetti sonori in %)
- `@ConfigEntry.BoundedDiscrete(min = 100, max = 2000)` -> `public int delay = 500;` (Intervallo ripetizione in ms durante la spinta)
- `@ConfigEntry.BoundedDiscrete(min = 1, max = 5)` -> `public int detectionRange = 1;` (Raggio d'allarme in camminata continua)
- `@ConfigEntry.BoundedDiscrete(min = 1, max = 24)` -> `public int panoramicRange = 8;` (Raggio ispezione panoramica Alt+V)
- `public boolean checkHeadroomClearance = true;` (Verifica soffitto basso/salto in ispezione)
- `public boolean lookAtObstacleOnInspection = true;` (Puntamento automatico sguardo su ostacolo frontale)
- `@ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)` -> `public NarrationStyle narrationStyle = NarrationStyle.BLOCK;`
- `@ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)` -> `public DirectionFeedbackMode directionFeedbackMode = DirectionFeedbackMode.FOUR_DIRECTIONS;`

---

### B. Intento di Movimento & Scansione Continua (`ObstacleDetector.java`)
1. **Rilevamento Intento Tasti**:
   - `boolean hasMoveInput = client.options.keyUp.isDown() || client.options.keyDown.isDown() || client.options.keyLeft.isDown() || client.options.keyRight.isDown();`
2. **Calcolo Vettore Desiderato (`calculateIntendedMoveDir`)**:
   - Quando `hasMoveInput == true`, estrazione dell'angolo relativo di input dai tasti premuti (`W` = $0^\circ$, `S` = $180^\circ$, `A` = $270^\circ$, `D` = $90^\circ$, diagonali $45^\circ / 135^\circ / 225^\circ / 315^\circ$) combinato con lo yaw del giocatore.
3. **Loop di Rilevamento & Cadenza Contatto**:
   - Se `isMoving || hasMoveInput`:
     - Raycast nella direzione del moto/intento su raggio `config.detectionRange`.
     - Se viene rilevato un ostacolo (`state != CLEAR`):
       - A ogni ciclo `config.delay` (500 ms), invia la narrazione con prefisso (*"A sinistra: ostacolo di staccionata di quercia"*) e suona il cue 3D posizionale ([`playSoundCue`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/ObstacleDetector.java#L157-L164)) alle coordinate esatte del blocco.
4. **Silenzio Assoluto a Riposo**:
   - Se `!isMoving && !hasMoveInput`, reset dello stato e silenzio totale (zero spam da fermi).

---

### C. Ispezione Panoramica a 360° & Soffitto (`ObstacleDetectionUtils.java`)
1. **Algoritmo `scanPanoramic(Level level, Vec3 playerPos, float playerYaw, int range, DirectionFeedbackMode mode, boolean checkHeadroom)`**:
   - **Verifica Soffitto ($Y+2$)**: Se `checkHeadroom == true`, campiona la colonna sopra la testa (`playerHeadroomPos`). Se solido, registra lo stato del blocco a soffitto.
   - **Raycast Multi-Direzionale**: Lancia raggi a $360^\circ$ sulle direzioni cardinali (4 direzioni) o cardinali + diagonali (8 direzioni) campionando da $d = 0.1\text{ m}$ a `range` con passo $0.25\text{ m}$.
   - Registra per ogni direzione il primo ostacolo incontrato e la distanza esatta in blocchi ($d = \text{round}(\text{distanza})$).
2. **Composizione Semantica del Messaggio per Screen Reader**:
   - **Tutto Libero**: *"Tutto libero attorno a te per %d blocchi"* (`minecraft_access.obstacle_detector.panoramic_clear`).
   - **Soffitto Basso Sopra la Testa**: *"Soffitto basso sopra di te di %s (salto impossibile). "*
   - **Silenzio Selettivo**: Elenca solo le direzioni che presentano ostacoli con la relativa distanza (es. *"A sinistra: ostacolo di staccionata di quercia a 1 blocco. Davanti: blocco di terra a 4 blocchi, salto possibile."*), chiudendo con il sommario delle direzioni libere.

---

### D. Localizzazioni I18N (`it_it.json` ed `en_us.json`)
- Inserimento delle chiavi di configurazione e narrazione con ordinamento alfabetico JSON tassativo:
  - `minecraft_access.obstacle_detector.at_distance`: `"%s a %d blocchi"` / `"%s at %d blocks"`
  - `minecraft_access.obstacle_detector.at_distance_single`: `"%s a 1 blocco"` / `"%s at 1 block"`
  - `minecraft_access.obstacle_detector.panoramic_clear`: `"Tutto libero attorno a te per %d blocchi"` / `"All clear around you for %d blocks"`
  - `minecraft_access.obstacle_detector.player_headroom_blocked`: `"Soffitto basso sopra di te di %s (salto impossibile)"` / `"Low ceiling above you of %s (jumping impossible)"`
  - `text.autoconfig.minecraft-access.option.obstacleDetector.panoramicRange`: `"Raggio ispezione panoramica (Alt+V)"` / `"Panoramic inspection range (Alt+V)"`
  - `text.autoconfig.minecraft-access.option.obstacleDetector.checkHeadroomClearance`: `"Controlla soffitto basso sopra la testa"` / `"Check low ceiling above head"`

---

### E. Test Unitari Automatizzati (`ObstacleDetectorTest.java`)
- Test di calcolo angolare da combinazioni tasti WASD (4 cardinali e 4 diagonali).
- Test per `scanPanoramic` su 4 e 8 direzioni, con calcolo distanze e rilevamento soffitto $Y+2$.
- Test di conformità presenza e ordinamento di tutte le chiavi I18N in IT ed EN.

---

## 🧪 3. Piano di Verifica e Collaudo

### Verifica Automatica (Fase 1B)
1. Compilazione e suite JUnit:
   ```powershell
   $env:JAVA_HOME = "C:\Users\nemex\AppData\Roaming\PrismLauncher\java\java-runtime-epsilon"
   .\gradlew.bat --no-daemon test
   ```
2. Verifica ordinamento alfabetico JSON in Python.
3. Compilazione ShadowJar:
   ```powershell
   .\gradlew.bat --no-daemon shadowJar
   ```

### Collaudo Manuale In-Game di Luca (Fase 2)
1. **Spinta Continua**: Premere `A` contro una staccionata: verificare avviso vocale e suono 3D ripetuto ogni 500 ms.
2. **Rilascio**: Rilasciare i tasti: verificare silenzio immediato.
3. **Ispezione Panoramica (`Alt+V`)**:
   - In campo aperto: verificare annuncio spazio libero sul raggio impostato.
   - Sotto un albero o galleria: verificare annuncio soffitto basso e salto bloccato.
   - Vicino a spigoli/diagonali: verificare annuncio dell'ostacolo con distanza in blocchi e suono 3D.
