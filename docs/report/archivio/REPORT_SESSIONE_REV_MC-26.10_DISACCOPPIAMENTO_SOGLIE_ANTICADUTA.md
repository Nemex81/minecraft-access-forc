# Report di Sessione: Rev MC-26.10 — Disaccoppiamento Soglie Anticaduta & Pervietà Corridoio Discesa in Acqua

- **Autori**: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity (Senior AI Pair Programmer)
- **Ramo Git**: `feat/cognitive-orchestrator`
- **Data Sessione**: 2026-09-08
- **Framework di Riferimento**: ASTRALIS v3.0.2 (Protocolli 1..6, RRU Disaccoppiato & AVF v26.2-1.19.1)
- **Esito Collaudo**: `[COLLAUDATO CON SUCCESSO AL 100% IN-GAME DA LUCA]`

---

## 🎯 1. Obiettivo & Causa Radice

Nel modulo di sicurezza anticaduta `FallDetector`, due anomalie limitavano la fluidità del gameplay:
1. **Accoppiamento Monolitico Percezione/Intervento**: L'uso della medesima soglia (`depth = 4`) costringeva il sistema a bloccare con accovacciamento immediato (`autoSneak`) anche su salti di 3 blocchi (danno vanilla = 0 cuori), incollando il giocatore al terreno ed impedendogli di scendere o saltare liberamente con `W`.
2. **Falsi Positivi Discesa Sicura & Caso Limite Acqua Sotterranea**:
   - `TraversalSafetyAnalyzer` emetteva *"Discesa sicura"* anche su rampe e gradini trascurabili ($\le 2$ blocchi);
   - In presenza di falde acquifere o grotte sotterranee sepolte sotto roccia/terra a diversi blocchi di profondità (come allo spawn $X=-83, Y=64, Z=-34$), il ciclo di scansione verticale verso il basso in `findDescentCandidate` non verificava la consistenza solida dei blocchi intermedi, scambiando il terreno compatto per un corridoio aperto di tuffo in acqua (generando 21 annunci a raffica).

---

## 🛠️ 2. Architettura & File Coinvolti

1. **`src/main/java/org/mcaccess/minecraftaccess/Config.java`**:
   - Introdotti `warningDepth = 3` e `autoSneakDepth = 4` con `@ConfigEntry.BoundedDiscrete(min = 2, max = 20)`;
   - Preservata retrocompatibilità con `depth = 4` (`@Deprecated`);
   - Introdotto getter difensivo `getEffectiveWarningDepth() = Math.min(warningDepth, autoSneakDepth)`.
2. **`src/main/java/org/mcaccess/minecraftaccess/features/safety/traversal/TraversalSafetyContext.java`**:
   - Esteso il record con `warningDepthThreshold` e `autoSneakDepthThreshold`, preservando il costruttore di retrocompatibilità.
3. **`src/main/java/org/mcaccess/minecraftaccess/features/safety/traversal/TraversalSafetyAnalyzer.java`**:
   - Silenziate le discese con $\Delta Y < warningDepthThreshold$ (1-2 blocchi), classificate come cammino calpestabile ordinario (`NOT_APPLICABLE`);
   - **Contratto D2.1 (Pervietà Corridoio Acqua)**: Nel loop di probing verso il basso in `findDescentCandidate`, introdotto il controllo di pervietà continua: interruzione anticipata immediata (`break;`) se si incontra un blocco solido con sagoma di collisione non vuota (`!probeState.getCollisionShape(level, waterProbe).isEmpty()`).
4. **`src/main/java/org/mcaccess/minecraftaccess/features/FallDetector.java`**:
   - Zona 1 (Percezione, avviso vocale e slowdown): sincronizzata su `getEffectiveWarningDepth()` (3 blocchi);
   - Zona 2 (Intervento meccanico, auto-sneak sul ciglio e `isStandingOnDangerousEdge`): sincronizzata su `autoSneakDepth` (4 blocchi).
5. **Localizzazioni (`it_it.json`, `en_us.json`)**:
   - Inserite le 4 chiavi Cloth Config in rigoroso ordine alfabetico crescente.
6. **`src/test/java/org/mcaccess/minecraftaccess/features/safety/traversal/TraversalSafetyAnalyzerTest.java`**:
   - Aggiunti test deterministici headless: Test 8 (scale brevi silenziate), Test 9 (dislivello 3 blocchi non-applicabile a sneak), Test 10 (acqua sotterranea coperta da pietra -> NOT_APPLICABLE), Test 11 (tuffo in aria aperta -> SAFE_DESCENT_AVAILABLE).

---

## 🧪 3. Metriche di Verifica, Telemetria & Collaudo In-Game

- **Test Automatici**: `327/327` test passati (100% verdi, 0 failures, 0 skipped in 1m 14s).
- **Compilazione**: `BUILD SUCCESSFUL` (jar `minecraft-access-26.2-1.19.0.SNAPSHOT.jar`, 7.451.017 byte).
- **Deploy Proattivo**: Aggiornato in entrambe le istanze PrismLauncher (`Minecraft 26.2 Access 1.12.0` e `Minecraft 26.2 Access - Server Tenuta`).
- **Evidenza Telemetrica In-Game (`latest.log` ore 02:55 - 03:00)**:
  - Annunci "Discesa sicura" allo spawn su sentiero: **0 (zero)**, falso positivo su acqua sotterranea azzerato al 100%;
  - Dislivelli di 3 blocchi: emesso avviso vocale di cautela (*"Attenzione: burrone ... profondità 3 blocchi"*), zero chiamate a *"Sul ciglio"* e zero blocco di movimento con auto-sneak;
  - Baratri da 4+ blocchi: protezione e auto-sneak sul ciglio pienamente operativi;
  - Salute finale: 20.0/20.0 cuori intatti.
