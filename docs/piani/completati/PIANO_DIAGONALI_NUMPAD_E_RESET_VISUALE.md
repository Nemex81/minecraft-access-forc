# Piano Tecnico: Diagonali 2D Combinate Tastierino Numerico (7, 9, 1, 3) & Risoluzione Reset Visuale

Documento di specifica tecnica, analisi di convalida a 7 assi e piano di implementazione per trasformare i tasti 7, 9, 1, 3 del tastierino numerico in movimenti diagonali 2D combinati fluidi (sia discreti che continui) e correggere definitivamente l'azzeramento/snap indesiderato dello Yaw al centraggio con il tasto 5.

---

## 1. Analisi di Convalida Preventiva a 7 Assi

1. **Validità Tecnica**:
   - I tasti `7`, `9`, `1`, `3` in Layer 0 applicano vettori di rotazione atomici combinati $(\Delta H, \Delta V)$ su `player.turn(deltaH, deltaV)`.
   - `7` = Alto-Sinistra $(-1, -1)$
   - `9` = Alto-Destra $(+1, -1)$
   - `1` = Basso-Sinistra $(-1, +1)$
   - `3` = Basso-Destra $(+1, +1)$
2. **Efficacia & Usabilità**:
   - Layout 3x3 del tastierino perfettamente coerente per l'orientamento spaziale a 8 direzioni.
   - Supporto nativo Dual-Mode: step discreto su pressione breve (<200ms) e rotazione continua fluida su tenuta prolungata (>=200ms).
3. **Coerenza dei Layer & Zero Conflitti**:
   - **Layer 0 (Diretto)**: 8 direzioni relative (4 ortogonali + 4 diagonali) + 5 Centra orizzonte.
   - **Layer 2 (Ctrl + Numpad)**: Snap assoluto ai 4 punti cardinali e alle 4 diagonali cardinali (NO, NE, SO, SE).
   - **Layer 3 (Alt + Numpad)**: Spostamento di `Look Nadir` (Piedi) su `Alt + 1` e `Look Zenith` (Cielo) su `Alt + 3` (slot precedentemente liberi).
4. **Completezza & Rigore I18N**:
   - Nuove chiavi di traduzione in `it_it.json` e `en_us.json` rigorosamente ordinate alfabeticamente.
5. **Precisione Matematica & Risoluzione Bug**:
   - `centerCameraHorizon()` (Tasto 5): azzera esclusivamente il Pitch (`player.setXRot(0.0f)` e `player.xRotO = 0.0f`), preservando inalterato al 100% lo Yaw reale del giocatore.
   - Eliminazione della singolarità `atan2(0,0) - 90°` per Nadir e Zenith impostando direttamente il Pitch a $\pm 90^\circ$ senza passare da `lookAt`.
   - Nel passaggio dell'orizzonte in `rotateCameraBy`, azzeramento atomico di `xRot` senza invocare `rotateCameraTo`.
6. **Affidabilità & Prestazioni**:
   - Timer `long` in tick con complessità $O(1)$, zero allocazioni per frame e nessun carico sulla CPU.
7. **Assenza di Regressioni**:
   - Piena compatibilità con i controlli tastiera (`I, J, K, L`, `.`, `,`), con il Lock-on, con l'AutoWalk e il Pathfinder.

---

## 2. Modifiche Effettuate nel Dettaglio

### Componente: NumpadControls & Camera Controls
- **`NumpadControls.java`**:
  - Sostituzione di `keyPitchUp` e `keyPitchDown` con `keyLookUpLeft`, `keyLookUpRight`, `keyLookDownLeft`, `keyLookDownRight`.
  - Aggiunta dei timer di tenuta continua `holdStartLookUpLeft`, `holdStartLookUpRight`, `holdStartLookDownLeft`, `holdStartLookDownRight`.
  - Mappatura dei tasti `KP_7`, `KP_9`, `KP_1`, `KP_3` a 2D Diagonals in Layer 0.
  - Spostamento di `look_nadir` su `Alt + KP_1` e `look_zenith` su `Alt + KP_3` in Layer 3.
  - Riformulazione di `centerCameraHorizon()` per azzerare solo il pitch con `player.setXRot(0.0f)`.
  - Creazione di `rotateCameraToPitch` per Nadir e Zenith a $\pm 90^\circ$.
  - Correzione del crossing dell'orizzonte in `rotateCameraBy` per non alterare lo Yaw orizzontale.
- **`CameraControls.java`**:
  - Adozione di `rotateCameraToPitch` per i tasti `.` e `Alt + .`.
  - Protezione dello Yaw al passaggio dell'orizzonte in `rotateCameraBy`.
- **File di Traduzione (`it_it.json`, `en_us.json`)**:
  - Aggiunta delle stringhe di localizzazione in ordine alfabetico crescente.
- **Suite di Test (`NumpadControlsTest.java`)**:
  - Aggiunti test per i calcoli dei passi diagonali 2D e aggiornato il test di completezza chiavi.

---

## 3. Esito Collaudo & Chiusura
- **Test Automatici Gradle**: Superati al 100% (`BUILD SUCCESSFUL`).
- **Collaudo In-Game di Luca**: Superato con successo con verifica dei log di runtime.
