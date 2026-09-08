# Piano Tecnico Implementativo — Rev. MC-26.10: Disaccoppiamento Soglie Anticaduta & Perfezionamento Discesa Sicura
- **Tipologia:** CORRETTIVO & REFACTORING
- **Autore:** Luca (Lead Developer Non Vedente con Screen Reader NVDA) & Antigravity (AI Pair Programmer)
- **Revisori:** Luca / Antigravity
- **Data e Ora:** 2026-09-07
- **Stato Operativo:** [COMPLETATO E CONVALIDATO IN-GAME — 2026-09-08]
- **Incremento Versione Target (AVF):** v26.2-1.19.1
- **Piani & Documenti Correlati:**
  * [`docs/report/REGISTRO_REVISIONI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/REGISTRO_REVISIONI.md)
  * [`knowledge/05_specifiche_dominio_voxel_e_comandi.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/knowledge/05_specifiche_dominio_voxel_e_comandi.md)
  * [`knowledge/10_standard_piani_verifiche_e_rapporti.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/knowledge/10_standard_piani_verifiche_e_rapporti.md)
- **Conformità ai 6 Cancelli (Protocollo 12 Inner Codex):** Conforme al 100% (Rifiuto patching euristico, Hardware grounding, Hitbox e clearance continua, Contratti denominati D1..D4, Determinismo headless a 0 ms, Budget token)

---

## 🗺️ Sommario Operativo & Registro di Avanzamento (Checklist con Gating di Convalida)

> **Regola Aurea di Avanzamento (Matrice a 3 Stati)**:
> - `- [ ] [DA AVVIARE]`: Attività pianificata ma non ancora iniziata.
> - `- [/] [IMPLEMENTATO — IN ATTESA DI CONVALIDA]`: Codice scritto o intervento completato, ma in attesa di test o collaudo formale (spunta parziale).
> - `- [x] [CONVALIDATO CON SUCCESSO]`: Spunta definitiva concessa **esclusivamente POST-CONVALIDA** (approvazione di Luca per la 1A, test suite 100% verde per la 1B, collaudo pratico in-game di Luca per la Fase 2).

- [x] **0. Decisione, RCA e Perimetro Operativo** [CONVALIDATO CON SUCCESSO]
- [x] **1. Contratto D1: Configurazione Disaccoppiata in Cloth Config (`warningDepth = 3`, `autoSneakDepth = 4`)** [CONVALIDATO CON SUCCESSO]
- [x] **2. Contratto D2: TraversalSafetyContext e Perfezionamento Soglia Minima "Discesa Sicura"** [CONVALIDATO CON SUCCESSO]
  - [x] **Contratto D2.1: Pervietà del Corridoio Verticale per Discesa in Acqua (Affinamento PRAPI)** [CONVALIDATO CON SUCCESSO]
- [x] **3. Contratto D3: FallDetector & Architettura a Due Zone (Zona 1 Percezione vs Zona 2 Intervento)** [CONVALIDATO CON SUCCESSO IN-GAME]
- [x] **4. Contratto D4: Localizzazioni I18N Alfabetiche (IT/EN) & Suite di Test Unitari Headless a 0 ms** [CONVALIDATO CON SUCCESSO — 327/327 VERDI]
- [x] **5. Checkpoint Vincolante & Deploy Proattivo** [CONVALIDATO CON SUCCESSO IN-GAME — 2026-09-08]

---

## 0. Decisione, RCA e Perimetro Operativo

### 0.1 Root Cause Analysis (RCA)
1. **Accoppiamento Monolitico Percezione/Intervento**: 
   Nel modulo `FallDetector`, un'unica variabile (`config.depth`) regolava contemporaneamente l'avviso vocale di prossimità e il blocco fisico immediato con auto-accovacciamento (`autoSneakOnEdge`). In Minecraft vanilla, il danno da caduta inizia a 4 blocchi (un salto di 3 blocchi infligge 0 cuori di danno). Usare un'unica soglia costringeva a un compromesso forzato: o bloccare il giocatore anche su discese innocue di 3 blocchi, oppure perdere la pre-allerta su salti significativi.
2. **Rev MC-26.10 (Falsi Positivi "Discesa Sicura")**:
   Nel metodo `TraversalSafetyAnalyzer.analyzeTraversal()`, la presenza di una struttura di discesa (scala a pioli, liane, impalcature o acqua) veniva validata ed emessa come `SAFE_DESCENT_AVAILABLE` prima di verificare se il dislivello complessivo $\Delta Y$ costituisse un effettivo salto rilevante. Di conseguenza, il sistema annunciava *"Discesa sicura"* anche su gradini minimi o pozzanghere di 1 o 2 blocchi, creando un fastidioso inquinamento vocale durante il cammino.
3. **Caso Limite Acqua Sotterranea Coperta (Emerso dal Collaudo In-Game)**:
   Nel metodo `findDescentCandidate`, il ciclo di scansione della colonna d'acqua verso il basso cercava un blocco d'acqua senza verificare se i blocchi intermedi tra il punto di cammino (`entryPos`) e l'acqua fossero solidi (terra/pietra). Trovando una falda acquifera sotterranea sepolta sotto 4 blocchi di terra, il sistema la considerava erroneamente una discesa sicura in acqua attraverso il terreno solido.

### 0.2 Perimetro Operativo
- **In Perimetro**:
  - Introduzione di due parametri indipendenti: `warningDepth` (default = 3) e `autoSneakDepth` (default = 4) in `Config.FallDetector`;
  - Aggiornamento di `TraversalSafetyContext` per trasportare le soglie disaccoppiate;
  - Silenziamento dell'annuncio "discesa sicura" in `TraversalSafetyAnalyzer` per discese inferiori a `warningDepth`;
  - Pervietà del corridoio verticale per discesa in acqua in `findDescentCandidate` (interruzione su collisione solida);
  - Disaccoppiamento tra Zona 1 (avviso vocale/sonoro su `warningDepth`) e Zona 2 (auto-sneak sul ciglio su `autoSneakDepth`);
  - Aggiornamento simmetrico e rigorosamente alfabetico di `it_it.json` ed `en_us.json`;
  - Suite di test automatici headless a 0 ms.
- **Fuori Perimetro (Componenti Congelati e Protetti)**:
  - Nessuna modifica alla logica di hardware probing GLFW di `RawCrouchIntentProvider` (Cancello 2);
  - Nessuna alterazione al disaccoppiamento `suspendForGui()` in `SafetyMovementGuard`;
  - Nessuna modifica a `AutoWalkMotor`, `RouteNavigator` o `MovementCoordinator`.

---

## 1. Contratto D1: Configurazione Disaccoppiata in Cloth Config

Nel file `src/main/java/org/mcaccess/minecraftaccess/Config.java`, all'interno della classe `FallDetector`:

1. **Nuovi Campi Discreti**:
   ```java
   @ConfigEntry.BoundedDiscrete(min = 2, max = 20)
   public int warningDepth = 3;

   @ConfigEntry.BoundedDiscrete(min = 2, max = 20)
   public int autoSneakDepth = 4;
   ```
2. **Retrocompatibilità & Getter Difensivo**:
   - Mantenere il campo legacy `public int depth = 4;` marcato con `@Deprecated` oppure sincronizzato per non rompere config salvate su disco;
   - Metodo helper `getEffectiveWarningDepth()`:
     ```java
     public int getEffectiveWarningDepth() {
         return Math.min(warningDepth, autoSneakDepth);
     }
     ```
     *Invariante difensiva*: la soglia di pre-allerta percettiva non può mai essere superiore alla soglia di blocco fisico.

---

## 2. Contratto D2: TraversalSafetyContext e Perfezionamento "Discesa Sicura"

### 2.1 Aggiornamento Record `TraversalSafetyContext`
Nel file `src/main/java/org/mcaccess/minecraftaccess/features/safety/traversal/TraversalSafetyContext.java`:
- Aggiornare i campi per includere entrambe le soglie:
  ```java
  public record TraversalSafetyContext(
          @NotNull Vec3 playerPos,
          @NotNull AABB playerBoundingBox,
          int playerBaseY,
          @Nullable Vec3 movementIntent,
          boolean hasMovementIntent,
          int warningDepthThreshold,
          int autoSneakDepthThreshold,
          @NotNull BlockGetter level
  ) {
  }
  ```

### 2.2 Calibrazione in `TraversalSafetyAnalyzer.analyzeTraversal`
Nel file `src/main/java/org/mcaccess/minecraftaccess/features/safety/traversal/TraversalSafetyAnalyzer.java`:
1. **Filtro Minimo su `findDescentCandidate`**:
   - Una colonna di discesa (scala, acqua, ecc.) calcola l'atterraggio `candidate.landingPos()`.
   - Calcolare il dislivello effettivo $\Delta Y = \text{playerBaseY} - \text{candidate.landingPos().getY()}$.
   - *Se* $\Delta Y < context.warningDepthThreshold()$:
     *Allora* la discesa è un dislivello innocuo e trascurabile ($\le 2$ blocchi): NON classificare come `SAFE_DESCENT_AVAILABLE`, ma considerare il corridoio come cammino calpestabile ordinario (`notApplicable`), azzerando l'annuncio "discesa sicura".
   - *Se* $\Delta Y \ge context.warningDepthThreshold()$:
     *Allora* la discesa è significativa ($\ge 3$ blocchi): classificare regolarmente come `SAFE_DESCENT_AVAILABLE`.
2. **Valutazione Baratro Sottostante a Scala Spezzata (`hasBrokenClimbableAhead`)**:
   - Confrontare il baratro finale con `autoSneakDepthThreshold` per salvaguardare la vita dell'utente.

### 2.3 Contratto D2.1: Pervietà del Corridoio Verticale per Discesa in Acqua (Affinamento PRAPI)
Nel file `src/main/java/org/mcaccess/minecraftaccess/features/safety/traversal/TraversalSafetyAnalyzer.java`:
1. **Analisi del Caso Limite**:
   - *Se* sotto il candidato di ingresso `candidateEntry` è presente terreno solido ordinario (es. terra o pietra), e diversi blocchi più in basso ($Y - 5$) esiste una falda sotterranea d'acqua;
   - *Allora* il ciclo di scansione della colonna d'acqua non deve attraversare blocchi solidi.
2. **Invariante di Pervietà Fisica (Cancello 3 Inner Codex — Hitbox & Clearance Continua)**:
   - Una colonna d'acqua costituisce una valida discesa **esclusivamente se** il tragitto verticale tra la quota di ingresso (`entryPos`) e l'acqua (`waterProbe`) è **fisicamente pervio e non sbarrato da ostacoli solidi**.
   - Durante il ciclo verso il basso in `findDescentCandidate`:
     ```java
     BlockPos waterProbe = entryPos;
     for (int d = 0; d <= Math.max(8, dangerThreshold + 2); d++) {
         FluidState fluid = level.getFluidState(waterProbe);
         if (isSafeWater(fluid)) {
             SafeDescentCandidate waterCol = validateWaterColumn(level, entryPos, waterProbe, dangerThreshold);
             if (waterCol != null) return waterCol;
         }
         // Se prima dell'acqua si incontra un blocco solido impenetrabile, il corridoio verticale è sbarrato
         BlockState probeState = level.getBlockState(waterProbe);
         if (!probeState.getCollisionShape(level, waterProbe).isEmpty()) {
             break;
         }
         waterProbe = waterProbe.below();
     }
     ```
   - *Effetto*: Se sotto i piedi c'è terra o roccia piena, il loop si arresta al primo blocco solido (`break;`), scartando l'acqua sotterranea coperta ed eliminando alla radice l'annuncio spurio.

---

## 3. Contratto D3: FallDetector & Architettura a Due Zone

Nel file `src/main/java/org/mcaccess/minecraftaccess/features/FallDetector.java`:

1. **Zona 1 — Percezione (Pre-Allerta Vocale / Sonora & Slowdown)**:
   - In `findDangerAhead`:
     - Verificare `drop >= config.warningDepth`;
     - Se superato, restituisce `DangerInfo` con la profondità rilevata;
     - In `handleDangerDetected`: l'avviso vocale e sonoro scatta regolarmente.
   - In `searchNearbyPositions` (scansione 3D d'ambiente) e `inspectNearbyFalls` (Alt+F):
     - Confronto con `config.warningDepth`.
2. **Zona 2 — Intervento Posturale (Auto-Sneak sul Ciglio)**:
   - In `checkLookAheadSafety`:
     - Se `distance <= EDGE_SNEAK_THRESHOLD`: l'auto-sneak forzato interviene **esclusivamente se** `danger.depth() >= config.autoSneakDepth`.
   - In `isStandingOnDangerousEdge` (presidio da fermo):
     - Scansione radiale a 8 punti: considera pericoloso il ciglio e attiva lo sneak solo se `drop >= config.autoSneakDepth`.
3. **Comportamento Risultante nella Finestra $[warningDepth, autoSneakDepth)$ (es. salto di 3 blocchi)**:
   - L'utente sente l'avviso vocale ("Burrone avanti, 3 blocchi");
   - Lo sprint viene rallentato (`autoSlowdown`);
   - **L'auto-sneak NON interviene**: l'utente può premere `W` e saltare/scendere fluidamente senza subire danni fisici.

---

## 4. Contratto D4: Localizzazioni I18N Alfabetiche (IT/EN) & Test Unitari Headless

### 4.1 Localizzazioni (`it_it.json` & `en_us.json`)
Aggiungere le chiavi per Cloth Config mantenendo il rigoroso **ordine alfabetico crescente**:
- `text.autoconfig.minecraft-access.option.fallDetector.autoSneakDepth`
  - IT: *"Profondità minima per blocco anticaduta"*
  - EN: *"Minimum depth for fall protection sneak"*
- `text.autoconfig.minecraft-access.option.fallDetector.autoSneakDepth.@Tooltip`
  - IT: *"Dislivello minimo in blocchi per attivare l'accovacciamento automatico sul ciglio. Valore raccomandato: 4 (dove inizia il danno da caduta)."*
  - EN: *"Minimum drop in blocks to trigger auto-sneak at the edge. Recommended: 4 (where fall damage starts)."*
- `text.autoconfig.minecraft-access.option.fallDetector.warningDepth`
  - IT: *"Profondità minima per avviso vocale"*
  - EN: *"Minimum depth for vocal warning"*
- `text.autoconfig.minecraft-access.option.fallDetector.warningDepth.@Tooltip`
  - IT: *"Dislivello minimo in blocchi per annunciare il burrone e la discesa sicura su scale. Valore raccomandato: 3."*
  - EN: *"Minimum drop in blocks to announce falls and safe descent on ladders. Recommended: 3."*

### 4.2 Test Suite Deterministica Headless a 0 ms
Creare/aggiornare i test unitari:
1. `TraversalSafetyAnalyzerTest`:
   - Test con scala a 2 blocchi: verificare che NON generi `SAFE_DESCENT_AVAILABLE`.
   - Test con scala a 4 blocchi: verificare che generi `SAFE_DESCENT_AVAILABLE`.
   - Test con scala rotta su baratro $\ge 4$: verificare `AMBIGUOUS_OR_UNSAFE_DESCENT`.
   - **Test D2.1**: Acqua sotterranea a $Y-5$ coperta da terra solida a $Y-1$ -> verificare `NOT_APPLICABLE` (corridoio sbarrato, zero discesa sicura).
   - **Test D2.2**: Acqua a cielo aperto a $Y-5$ con colonna d'aria libera -> verificare `SAFE_DESCENT_AVAILABLE`.
2. `FallDetectorTwoZoneTest`:
   - Test drop = 3 blocchi: verificare avviso vocale = TRUE, auto-sneak = FALSE.
   - Test drop = 4 blocchi: verificare avviso vocale = TRUE, auto-sneak = TRUE.

---

## 5. Checkpoint Vincolante (Stop Obbligatorio Gating Semantico)

In ottemperanza alla **Regola 0 (Principio di Dialogo a 2 Tempi & Gating Semantico)**:
- Questo documento conclude la **Sotto-Fase 1A (Piano Tecnico Formale)**;
- È fatto divieto assoluto di modificare codice sorgente o configurazioni prima dell'esplicito comando di Luca (*"procedi"*, *"applica"*, *"esegui"*);
- Al via libera di Luca si passerà alla **Sotto-Fase 1B (Esecuzione, Compilazione `--no-daemon` e Test Automatici)**.
