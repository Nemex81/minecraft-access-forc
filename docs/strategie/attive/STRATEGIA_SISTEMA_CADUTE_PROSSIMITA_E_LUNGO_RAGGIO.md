# Strategia Logico-Cognitiva (UPCS) — Architettura Duale di Rilevamento Cadute: Prossimità e Lungo Raggio

- **Autori:** Luca (Lead Developer Non Vedente con Screen Reader NVDA) & Antigravity (AI Pair Programmer)
- **Framework:** ASTRALIS v3.0.2
- **Data:** 2026-09-08
- **Stato Documento:** [ATTIVO — FASE 0 STRATEGIA LOGICO-COGNITIVA]
- **Repository:** `minecraft-access` (Minecraft 26.2, Fabric / NeoForge, Balm, Java 25)
- **Revisione Correlata nel Registro RRU:**
  * [`Rev MC-26.18 — De-monolitizzazione & Architettura Duale Cadute (Prossimità 1..6 & Lungo Raggio 7..24)`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/REGISTRO_REVISIONI.md)
- **Documenti e Regole Correlate:**
  * [`GEMINI.md` — Regola 4 (Geometria Voxel) e Regola 7 (Quiete Sensoriale AutoWalk)](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/GEMINI.md)
  * [`knowledge/05_specifiche_dominio_voxel_e_comandi.md` — Regola 8 (Disaccoppiamento Soglie) e Regola 9 (Pervietà Corridoi Verticali)](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/knowledge/05_specifiche_dominio_voxel_e_comandi.md)

---

## 🧭 1. Visione d'Insieme & Diagnosi Architetturale

La classe `FallDetector.java` nel nostro repository è cresciuta progressivamente fino a raggiungere **915 righe di codice**, accumulando al proprio interno responsabilità eterogenee:
1. Cinematica di sicurezza ad alta frequenza (tick-by-tick), calcolo del vettore di movimento e auto-sneak sul ciglio;
2. Algoritmo di attraversamento di scale, discese in acqua e liane (`TraversalSafetyAnalyzer`, `SafetyMovementGuard`);
3. Integrazione con l'arbitraggio centrale `CognitiveCoordinator`;
4. Rilevamento manuale della buca più vicina (`Alt+F`);
5. Scansione periodica ad area BFS ereditata dal modulo upstream legacy della community.

Questa concentrazione monolitica viola il principio di singola responsabilità (SRP) e mescola due esigenze cognitive radicalmente diverse per un giocatore non vedente:
- **La Sicurezza Cinetica Immediata a Terra (Corto Raggio / Prossimità)**: dove atterrano i piedi nel prossimo secondo di marcia (necessita di reattività a latenza zero, voce descrittiva, rallentamento corsa e barriera meccanica auto-sneak);
- **La Consapevolezza Orografica dello Scenario (Lungo Raggio / Territorio)**: dove si aprono forre, burroni o cave distanti nell'ambiente (necessita di un sonar/radar discreto a bassa frequenza, che non allarmi falsamente e non tocchi in alcun modo il movimento del giocatore).

Inoltre, nel modulo upstream legacy (`upstream/dev`), il rilevatore effettuava un BFS ad area a raggio 6 emettendo esclusivamente il suono pesante dell'incudine (`SoundEvents.ANVIL_HIT`), privo di pervietà, privo di linea di vista e privo di protezione fisica.

La presente strategia definisce la **de-monolitizzazione definitiva** e la ristrutturazione in un sottosistema a 3 componenti coordinati, scalabili e indipendenti.

---

## 🏛️ 2. Topologia del Sottosistema Cadute (`features.safety.fall`)

La logica viene scomposta in tre classi specializzate residenti nel sottopackage dedicato `org.mcaccess.minecraftaccess.features.safety.fall`:

- `CentralFallSafetyManager.java`: Unico modulo client registrabile (`BalmClientModule` con ID `minecraft_access:fall_detector`) e unico interlocutore verso `CognitiveCoordinator`. Gestisce il ciclo di vita client (`ClientPlayingTick.AFTER`), i comandi Kuma (`Alt+F` ispezione, `Ctrl+Alt+F` toggle auto-sneak), l'arbitraggio di quiete reciproca e l'integrazione silenziosa con AutoWalk.
- `ProximityFallDetector.java`: Rilevatore di prossimità a corto raggio (1..6 blocchi). Gestisce la cinematica di marcia ad alta frequenza (tick-by-tick), la Zona 1 (pre-allerta con xilofono e rallentamento corsa), la Zona 2 (pre-freno incudine e blocco meccanico auto-sneak) e i contratti con `TraversalSafetyAnalyzer` e `SafetyMovementGuard`.
- `LongRangeFallDetector.java`: Radar orografico a lungo raggio (7..24 blocchi). Esegue una scansione polare a settori/raggi a intervalli rilassati (3.5 secondi), con verifica di pervietà continua e occlusione da pareti, emettendo la campanella 3D `NOTE_BLOCK_BELL` a volume attenuato senza toccare la postura o il movimento del giocatore.

---

## 🎚️ 3. La Scala di Sonificazione a 3 Livelli (Psicoacustica & Urgenza)

La gerarchia dei suoni viene riorganizzata per azzerare il sovraccarico sensoriale e garantire la massima leggibilità per un utente con screen reader:

1. **Livello 3 — Consapevolezza Orografica Lontana (7..24 blocchi)**:
   - **Suono**: `SoundEvents.NOTE_BLOCK_BELL` (Campanella melodica posizionale);
   - **Priorità Cognitiva**: `PASSIVE` o `CONTEXTUAL`;
   - **Comportamento Acustico**: Volume attenuato con la distanza nello spazio 3D ($1/r$ naturale), posizionato esattamente sopra la gola o il dirupo;
   - **Impatto Motorio**: Zero assoluto. Il giocatore non viene rallentato né accovacciato.

2. **Livello 2 — Pre-Allerta Cinetica di Marcia (2..6 blocchi)**:
   - **Suono**: `SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE` (Xilofono di ferro);
   - **Priorità Cognitiva**: `OPERATIONAL`;
   - **Comportamento Acustico**: Rintocco chiaro, metallico ma non traumatico, orientato lungo il vettore di marcia;
   - **Impatto Motorio**: Attiva la Zona 1 (rallentamento corsa `autoSlowdown` se abilitato, annuncio vocale informativo con metratura), lasciando il pieno controllo direzionale al giocatore per deviare o saltare liberamente.

3. **Livello 1 — Emergenza Ciglio & Freno Meccanico (0..1.5 blocchi)**:
   - **Sotto-Fascia 1A — Allarme Acustico Pre-Freno (1.0 - 1.5 m)**:
     * **Suono**: `SoundEvents.ANVIL_HIT` (Colpo secco dell'incudine);
     * **Priorità Cognitiva**: `CRITICAL`;
     * **Obiettivo Ergonomico**: Scatta **un istante prima** del blocco meccanico, dando al giocatore esperto il tempo di rilasciare `W` e fermarsi autonomamente, preservando la postura eretta e la fluidità di gioco.
   - **Sotto-Fascia 1B — Intervento Meccanico di Salvataggio (d <= 0.85 m)**:
     * **Azione**: Scatto immediato dell'**auto-accovacciamento forzato (`autoSneak`)** tramite `SafetyMovementGuard`;
     * **Condizione**: Dislivello letale Delta Y >= 4 (`autoSneakDepth`);
     * **Obiettivo**: Paracadute fisico assoluto salva-vita che impedisce la caduta anche in caso di corsa veloce, lag o ritardo di riflessi.

---

## 🛡️ 4. Integrazione con AutoWalk (Regola 7: Quiete Sensoriale Totale)

In conformità alla **Regola 7 di `GEMINI.md`**, durante la marcia automatica `MovementCoordinator.isAutoWalkActive()`:
1. **Lungo Raggio**: Completamente disattivato (100% silenzioso, zero campanelle ambientali che disturberebbero la navigazione);
2. **Corto Raggio**:
   - Soppressi il suono dello xilofono, il chatter vocale informativo e il rallentamento cinetico ordinario;
   - Preservato unicamente il fail-safe d'emergenza fisica: se il bot incontra una voragine imprevista a d <= 0.85 m, l'auto-sneak fisico e l'incudine intervengono all'istante bloccando il movimento e interrompendo l'AutoWalk in sicurezza.

---

## ⚙️ 5. Range di Azione Modulabili & Clamping di Sicurezza

In Cloth Config (`Config.FallDetector`), i raggi minimo e massimo di entrambi i sensori sono esposti all'utente con limiti vincolanti:

1. **Parametri Corto Raggio (Prossimità)**:
   - `proximityMinRange`: Bounded Discrete **da 1 a 6** (Default: `1`);
   - `proximityMaxRange`: Bounded Discrete **da 1 a 6** (Default: `6`);
   - `warningDepthThreshold`: profondità dislivello pre-allerta (Default: `3`);
   - `autoSneakDepthThreshold`: profondità dislivello auto-sneak (Default: `4`).

2. **Parametri Lungo Raggio (Orografia)**:
   - `longRangeMinRange`: Bounded Discrete **da 7 a 24** (Default: `7`);
   - `longRangeMaxRange`: Bounded Discrete **da 7 a 24** (Default: `24`);
   - `longRangeDepthThreshold`: profondità minima baratro distante (Default: `4`);
   - `longRangeScanInterval`: intervallo di scansione in millisecondi (Default: `3500 ms`).

3. **Invariante di Clamping Matematico**:
   - Nel codice:
     * `effectiveProxMin = Math.min(proximityMinRange, proximityMaxRange)`;
     * `effectiveProxMax = Math.max(proximityMinRange, proximityMaxRange)`;
     * `effectiveLongMin = Math.min(longRangeMinRange, longRangeMaxRange)`;
     * `effectiveLongMax = Math.max(longRangeMinRange, longRangeMaxRange)`.
   - Questa guardia impedisce qualsiasi inconsistenza logica anche se l'utente configura manualmente valori invertiti nel file JSON.

---

## ⚡ 6. Budget Computazionale & Ottimizzazione a Raggio 24

Una scansione a raggio 24 blocchi copre un'area potenziale di 2.401 colonne. Un BFS esaustivo blocco-per-blocco causerebbe inevitabili micro-lag. L'algoritmo di `LongRangeFallDetector` adotta le seguenti ottimizzazioni deterministiche:
1. **Scansione Polare a 16 Settori (Raggi Direzionali)**: Campiona l'orografia lungo 16 raggi radiali equidistanti (22.5 gradi) centrati sul giocatore a passi discreti (ogni 2 o 3 blocchi tra min e max), riducendo le verifiche da 2.400 a meno di 120 colonne per scansione;
2. **Pervietà & Occlusione (Regola 9 & Line-of-Sight)**: Se un raggio intercetta una parete solida continua a quota occhi/testa, il raggio si interrompe immediatamente (`break;`). I burroni al di là di pareti chiuse non vengono scansionati né vocalizzati;
3. **Verifica Acque Sotterranee (Regola 9)**: Rispetta rigidamente il `Contract D2.1`: se una colonna presenta un fluido preceduto da blocchi solidi pieni, la colonna viene rigettata all'istante, prevenendo falsi pings da falde sotterranee.

---

## 📋 7. Matrice di Simulazione a 3 Livelli

1. **Scenario Comune (Marcia in Pianura / Terreno Ondulato)**:
   - Corto raggio: analizza 1..6m lungo il vettore di marcia. Terreno pianeggiante -> `CLEAR`, zero suoni, zero voce, marcia fluida;
   - Lungo raggio: rileva una fenditura a 16 metri a nord-est. Emette un rintocco morbido di `NOTE_BLOCK_BELL` a volume attenuato da quella direzione. Il giocatore sa che a destra c'è un burrone ma continua a camminare dritto indisturbato.
2. **Scenario Meno Comune (Avvicinamento Diretto al Burrone)**:
   - A 12 metri: campanella lontana;
   - A 5 metri: entra in prossimità. Scatta lo xilofono `NOTE_BLOCK_IRON_XYLOPHONE` e la voce *"Burrone avanti a 5 blocchi, profondo 12"*, la corsa rallenta a passo normale;
   - A 1.2 metri: clang perentorio dell'incudine `ANVIL_HIT`. Il giocatore rilascia `W` e si arresta in piedi sul bordo senza sneak forzato;
   - Se il giocatore insiste ad avanzare: a 0.85 m scatta l'auto-sneak forzato e lo blocca fisicamente.
3. **Scenario Limite (Discesa Scale o Tuffo in Acqua Sicura)**:
   - Davanti a scale a pioli o lago profondo: `TraversalSafetyAnalyzer` valida `SAFE_DESCENT_AVAILABLE`.
   - Il `CentralFallSafetyManager` sopprime all'istante sia l'incudine che le campanelle a lungo raggio.
   - Il giocatore scende fluidamente senza interruzioni.

---

## 🚦 8. Piano di Transizione & Gating Semantico (Regola 0)

La presente Strategia Logico-Cognitiva costituisce il riferimento immutabile di **Fase 0**.

- **Prossimo Passo Formale**: Sotto-Fase 1A (Redazione del Piano Tecnico Formale con scomposizione contratti D0..DN in `docs/piani/attivi/PIANO_TECNICO_REV_MC-26.18_ARCHITETTURA_DUALE_CADUTE.md`);
- **Stop Obbligatorio (Gating Semantico)**: Nessun file sorgente Java (`FallDetector.java`, `Config.java`, ecc.) o di localizzazione viene toccato fino alla preventiva approvazione formale del Piano Tecnico da parte di Luca.
