# Piano Tecnico Implementativo — Rev. MC-26.18: De-monolitizzazione & Architettura Duale Cadute (Prossimità 1..6 & Lungo Raggio 7..24)

- **Tipologia:** ARCHITETTURALE & FUNZIONALE
- **Autori:** Luca (Lead Developer Non Vedente con Screen Reader NVDA) & Antigravity (AI Pair Programmer)
- **Revisori:** Luca / Antigravity / Codex
- **Data e Ora:** 2026-09-08
- **Stato Operativo:** [SOTTO-FASE 1B COMPLETATA — DEPLOY EFFETTUATO — IN ATTESA DI COLLAUDO IN-GAME FASE 2]
- **Incremento Versione Target (AVF):** Minore `v26.2-1.20.0` (Nuova feature architetturale duale e arricchimento sonoro)
- **Documenti & Piani Correlati:**
  * [`docs/strategie/attive/STRATEGIA_SISTEMA_CADUTE_PROSSIMITA_E_LUNGO_RAGGIO.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/attive/STRATEGIA_SISTEMA_CADUTE_PROSSIMITA_E_LUNGO_RAGGIO.md)
  * [`docs/report/REGISTRO_REVISIONI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/REGISTRO_REVISIONI.md)
  * [`GEMINI.md` — Regole 4 e 7](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/GEMINI.md)
  * [`knowledge/05_specifiche_dominio_voxel_e_comandi.md` — Regole 8 e 9](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/knowledge/05_specifiche_dominio_voxel_e_comandi.md)
- **Conformità ai 6 Cancelli (Protocollo 12 Inner Codex):**
  * *Cancello 1 (Rifiuto Patching Euristico)*: Riconoscimento geometrico continuo della discesa senza budget o ritardi arbitrari;
  * *Cancello 2 (Hardware Grounding)*: Probing GLFW puro per takeover manuale (`keySneak`) intatto in `SafetyMovementGuard`;
  * *Cancello 3 (Hitbox & Clearance Continua)*: Clearance occhi/testa e scansione pervietà colonna fluida (Contract D2.1);
  * *Cancello 4 (Named Contracts D0..D6)*: Scomposizione atomica numerata;
  * *Cancello 5 (Determinismo Headless)*: Test seams package-private a 0 ms senza `Thread.sleep`;
  * *Cancello 6 (Budget Token & Anti-Bloat Normativo)*: Rete documentale Pointer DRY.

---

> [!IMPORTANT]
> ### 🌿 Disciplina di Versionamento Git (Ramo Dedicato di Feature)
> Data la complessità architetturale dell'intervento, la scomposizione del monolite storico da 915 righe (`FallDetector.java`) e l'assimilazione/riscrittura di algoritmi legacy della community in una scansione polare perimetrale, l'implementazione del codice (Sotto-Fase 1B) e il collaudo empirico (Fase 2) si svolgeranno interamente all'interno di un ramo Git dedicato denominato:
> **`feat/dual-fall-safety-subsystem`** (ramificato direttamente da `feat/cognitive-orchestrator`).
> Il ri-merge in `feat/cognitive-orchestrator` avverrà **esclusivamente POST-CONVALIDA formale positiva** da parte di Luca dopo collaudo pratico in-game con sintesi vocale NVDA.

---

## 🗺️ Sommario Operativo & Registro di Avanzamento (Checklist con Gating di Convalida)

> **Regola Aurea di Avanzamento (Matrice a 3 Stati)**:
> - `- [ ] [DA AVVIARE]`: Attività pianificata ma non ancora iniziata.
> - `- [/] [IMPLEMENTATO — IN ATTESA DI CONVALIDA]`: Codice scritto o intervento completato, ma in attesa di test o collaudo formale (spunta parziale).
> - `- [x] [CONVALIDATO CON SUCCESSO]`: Spunta definitiva concessa **esclusivamente POST-CONVALIDA** (approvazione di Luca per la 1A, test suite 100% verde per la 1B, collaudo pratico in-game di Luca per la Fase 2).

- [x] **0. Decisione, motivazione e perimetro architetturale** [CONVALIDATO CON SUCCESSO]
- [x] **1. Contratto D0: Package `features.safety.fall` e disaccoppiamento classi** [CONVALIDATO CON SUCCESSO]
- [x] **2. Contratto D1: `CentralFallSafetyManager` (Lifecycle, Routing, Quiete Reciproca & AutoWalk)** [CONVALIDATO CON SUCCESSO]
- [x] **3. Contratto D2: `ProximityFallDetector` (1..6 blocchi, Zona 1 Xilofono, Zona 2 Pre-freno Incudine & Freno Meccanico con Mutua Esclusione Acustica)** [CONVALIDATO CON SUCCESSO]
- [x] **4. Contratto D3: `LongRangeFallDetector` (7..24 blocchi, Scansione Polare 16 Settori, Campanella 3D)** [CONVALIDATO CON SUCCESSO]
- [x] **5. Contratto D4: Riconfigurazione `Config.FallDetector`, Clamping e Normalizzazione `validateAndNormalize()`** [CONVALIDATO CON SUCCESSO]
- [x] **6. Contratto D5: Localizzazione I18N con ordinamento alfabetico crescente** [CONVALIDATO CON SUCCESSO]
- [x] **7. Contratto D6: Seam di test deterministici a 0 ms e suite di test unitari** [CONVALIDATO CON SUCCESSO]
- [x] **8. Inventario file (Nuovi, Modificati, Congelati)** [CONVALIDATO CON SUCCESSO]
- [/] **9. Criteri di accettazione finali** [IN COLLAUDO IN-GAME DA PARTE DI LUCA]
- [/] **10. Deploy proattivo & Telemetria Live (Fase 2)** [DEPLOY COMPLETATO NELLE ISTANZE — PRONTO AL TEST]

---

## 0. Decisione, Motivazione e Perimetro

La classe monolitica `FallDetector.java` (915 righe) viene decomposta in un sottosistema a 3 componenti ad alta coesione e basso accoppiamento.
Il rilevatore ad area legacy della community viene evoluto in un **Radar Orografico a Lungo Raggio (7..24 blocchi)**, mentre la cinematica direzionale di sicurezza costituisce il **Rilevatore di Prossimità a Corto Raggio (1..6 blocchi)**.

### Cosa è FUORI PERIMETRO (Componenti Congelati e Protetti):
- `TraversalSafetyAnalyzer.java`: i contratti geometrici puri di discesa sicura (scale, gradini, acqua, collision shapes) restano **intatti e congelati**;
- `SafetyMovementGuard.java`: la logica di riconciliazione dello sneak, dei token di ownership e di `suspendForGui()` resta **intatta e congelata**;
- `MovementCoordinator.java` e `AutoWalkManager.java`: l'AutoWalk non viene modificato internamente, ma interrogato passivamente per la quiete sensoriale;
- Nessuna alterazione al modulo del mirino o ad altri domini cognitivi.

---

## 1. Contratto D0: Architettura del Package `features.safety.fall`

Viene creato il package `org.mcaccess.minecraftaccess.features.safety.fall` contenente:
1. `CentralFallSafetyManager.java`: Entry point Balm, orchestrazione, keybinding e interfaccia con `CognitiveCoordinator`;
2. `ProximityFallDetector.java`: Gestore corto raggio vettoriale (1..6m);
3. `LongRangeFallDetector.java`: Gestore lungo raggio polare (7..24m);
4. La classe originale `org.mcaccess.minecraftaccess.features.FallDetector.java` viene convertita in una facade delegante per mantenere la piena compatibilità con eventuali riferimenti storici e test di regressione.

---

## 2. Contratto D1: `CentralFallSafetyManager`

### 2.1 Identità & Registrazione
- Modulo Balm registrato con ID: `Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "fall_detector")`;
- Aggancio a `ClientPlayingTick.AFTER`: esegue la guardia tick-by-tick delegando a `proximityDetector.tick(...)` e coordina il timer periodico delegando a `longRangeDetector.tick(...)`.

### 2.2 Keybinding Gestiti
- `Alt+F`: Ispezione manuale della buca più vicina nel raggio di prossimità (con annuncio vocale e suono campana);
- `Ctrl+Alt+F`: Toggle dell'auto-sneak forzato sul ciglio (con notifica vocale e pling/bass).

### 2.3 Regola di Quiete Reciproca
- *Se* `proximityDetector` rileva una condizione di allarme (Zona 1 o Zona 2) OPPURE un corridoio di discesa sicura autorizzato (`SAFE_DESCENT_AVAILABLE`), *Allora* qualsiasi notifica acustica del `longRangeDetector` viene soppressa per quel tick e per i 2000 ms successivi.

### 2.4 Integrazione con AutoWalk (Regola 7)
- *Se* `MovementCoordinator.isAutoWalkActive()` è `true`, *Allora*:
  1. `longRangeDetector` è disattivato al 100% (zero scansioni e zero pings);
  2. `proximityDetector` sopprime xilofono, annunci vocali descrittivi e rallentamento corsa;
  3. È preservato unicamente il freno meccanico estremo (`autoSneak`) con incudine a $d \le 0.85\text{ m}$ in caso di baratro letale imprevisto.

---

## 3. Contratto D2: `ProximityFallDetector` (Corto Raggio 1..6 Blocchi) & Mutua Esclusione Acustica

### 3.1 Cinematica Vettoriale
- Calcola ad ogni tick il vettore intenzionale del giocatore partendo dai tasti fisici GLFW (`keyUp`, `keyDown`, `keyLeft`, `keyRight`) o dalla velocità residua `deltaMovement`;
- Passa il contesto a `TraversalSafetyAnalyzer.analyzeTraversal(...)` con i raggi effettivi validati:
  * `effectiveProxMin` e `effectiveProxMax` (inclusi rigidamente in $[1, 6]$).

### 3.2 Scala di Allarme di Prossimità e Regola di Mutua Esclusione Acustica
1. **Zona 1 — Pre-Allerta Informativa ($d \in [2.0, effectiveProxMax]$)**:
   - Scatta quando $\Delta Y \ge warningDepthThreshold$ (default 3);
   - Emette `SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE` a pitch 1.0f e volume `config.volume`;
   - Se abilitato `autoSlowdown`, converte lo sprint in camminata normale;
   - Invia evento `OPERATIONAL` con voce descrittiva *"Burrone [direzione] a X blocchi, profondo Y"*.
2. **Zona 2A — Pre-Freno d'Emergenza ($d \in [1.0, 1.5\text{ m}]$)**:
   - Scatta un istante prima del ciglio solo quando $\Delta Y \ge autoSneakDepthThreshold$ (default 4);
   - **Regola di Mutua Esclusione Acustica Assoluta**: a questa distanza l'emissione dell'incudine `SoundEvents.ANVIL_HIT` **spegne e sopprime istantaneamente lo xilofono**; nessun rintocco di xilofono può coesistere con l'incudine;
   - Invia evento `CRITICAL`;
   - **Nessun auto-sneak forzato in questa sotto-fascia**: concede al giocatore la finestra reattiva per rilasciare `W` e fermarsi con la postura eretta.
3. **Zona 2B — Intervento Meccanico di Salvataggio ($d \le 0.85\text{ m}$)**:
   - Scatta sul ciglio fisico;
   - Ingaggia `SafetyMovementGuard.engageFallProtection()`, attivando l'auto-accovacciamento forzato `autoSneakActive = true`;
   - Se il giocatore continua a premere verso il vuoto, ripete il debounce di collisione ciglio (`Edge Bump`).

---

## 4. Contratto D3: `LongRangeFallDetector` (Lungo Raggio 7..24 Blocchi)

### 4.1 Frequenza & Scansione Polare
- Intervallo di scansione temporale: `config.longRangeScanInterval` (default 3500 ms);
- Non esegue BFS cubico. Campiona lungo **16 raggi polari** ($22.5^\circ$ di separazione):
  * Raggio $r$ varia da `effectiveLongMin` a `effectiveLongMax` (clamped in $[7, 24]$) a passi discreti di 2 blocchi;
  * Per ogni punto $(X, Z)$ campionato lungo il raggio:
    1. Verifica occlusione: se un blocco solido impenetrabile è presente a quota occhi ($Y+1$) lungo il cammino dal centro, interrompe il raggio corrente (`break;`);
    2. Calcola il piano di calpestio a terra;
    3. Se rileva aria continua verso il basso per $\ge longRangeDepthThreshold$ (default 4), convalida il candidato;
    4. Verifica pervietà (Regola 9 / Contract D2.1): se alla base c'è fluido preceduto da blocchi solidi pieni, la colonna viene rigettata;
    5. Emette `SoundEvents.NOTE_BLOCK_BELL` posizionato nello spazio 3D sopra la buca, con attenuazione naturale proporzionale alla distanza;
    6. Invia evento cognitivo `PASSIVE` (zero interferenza con la sintesi vocale prioritaria).

---

## 5. Contratto D4: Riconfigurazione `Config.FallDetector` & Routine di Normalizzazione Inviolabile

In `Config.java`, la classe statica `FallDetector` viene strutturata con la routine di sanitizzazione **`validateAndNormalize()`**:

```java
public static final class FallDetector {
    public boolean enabled = true;
    public float volume = 0.4f; // Volume congelato nei limiti di sicurezza (0.7f-0.8f max combinato)

    // Sotto-configurazione Corto Raggio (Prossimità)
    public boolean proximityEnabled = true;
    @ConfigEntry.BoundedDiscrete(min = 1, max = 6)
    public int proximityMinRange = 1;
    @ConfigEntry.BoundedDiscrete(min = 1, max = 6)
    public int proximityMaxRange = 6;
    public int warningDepthThreshold = 3;
    public int autoSneakDepthThreshold = 4;
    public boolean autoSneakOnEdge = true;
    public boolean autoSlowdown = true;
    public boolean voiceWarning = true;
    public boolean playAudioCues = true;
    public EdgeBumpFeedbackMode edgeBumpFeedbackMode = EdgeBumpFeedbackMode.SOUND_AND_VOICE;

    // Sotto-configurazione Lungo Raggio (Orografia)
    public boolean longRangeEnabled = true;
    @ConfigEntry.BoundedDiscrete(min = 7, max = 24)
    public int longRangeMinRange = 7;
    @ConfigEntry.BoundedDiscrete(min = 7, max = 24)
    public int longRangeMaxRange = 24;
    public int longRangeDepthThreshold = 4;
    public int longRangeScanInterval = 3500; // millisecondi
    public float longRangeVolumeMultiplier = 0.7f;

    // Routine Formale di Sanitizzazione & Clamping Difensivo
    public void validateAndNormalize() {
        // 1. Clamping rigido dei limiti assoluti
        proximityMinRange = Math.clamp(proximityMinRange, 1, 6);
        proximityMaxRange = Math.clamp(proximityMaxRange, 1, 6);
        longRangeMinRange = Math.clamp(longRangeMinRange, 7, 24);
        longRangeMaxRange = Math.clamp(longRangeMaxRange, 7, 24);

        // 2. Risoluzione automatica di eventuali inversioni utente (Swap Guard)
        if (proximityMinRange > proximityMaxRange) {
            int tmp = proximityMinRange;
            proximityMinRange = proximityMaxRange;
            proximityMaxRange = tmp;
        }
        if (longRangeMinRange > longRangeMaxRange) {
            int tmp = longRangeMinRange;
            longRangeMinRange = longRangeMaxRange;
            longRangeMaxRange = tmp;
        }
    }

    public int getEffectiveProxMin() { validateAndNormalize(); return proximityMinRange; }
    public int getEffectiveProxMax() { validateAndNormalize(); return proximityMaxRange; }
    public int getEffectiveLongMin() { validateAndNormalize(); return longRangeMinRange; }
    public int getEffectiveLongMax() { validateAndNormalize(); return longRangeMaxRange; }
}
```

---

## 6. Contratto D5: Localizzazione I18N con Ordinamento Alfabetico

In `src/main/resources/assets/minecraft_access/lang/it_it.json` ed `en_us.json`:
- Vengono aggiunte le nuove etichette di configurazione Cloth Config per i parametri di prossimità e lungo raggio;
- **Vincolo Inviolabile CI**: Tutte le chiavi devono essere rigorosamente ordinate in ordine alfabetico crescente.

---

## 7. Contratto D6: Seam di Test Deterministici a 0 ms

I tre nuovi manager implementano seams package-private:
- `proximityAudioConsumer` e `longRangeAudioConsumer` per intercettare l'emissione dei suoni senza invocare `Minecraft.getInstance().level.playLocalSound`;
- Verifica che a $d \le 1.5$m venga emesso esclusivamente `ANVIL_HIT` e lo xilofono riceva 0 invocazioni;
- `cognitiveEventConsumer` per verificare la corretta priorità (`CRITICAL`, `OPERATIONAL`, `PASSIVE`);
- `clockSupplier` per simulare avanzamenti temporali senza `Thread.sleep`;
- `autoWalkStateSupplier` per testare la quiete sensoriale in AutoWalk;
- Suite di test dedicata `CentralFallSafetyManagerTest.java`, `ProximityFallDetectorTest.java` e `LongRangeFallDetectorTest.java`.

---

## 8. Inventario dei File

### 8.1 File Nuovi
- `src/main/java/org/mcaccess/minecraftaccess/features/safety/fall/CentralFallSafetyManager.java` [NEW]
- `src/main/java/org/mcaccess/minecraftaccess/features/safety/fall/ProximityFallDetector.java` [NEW]
- `src/main/java/org/mcaccess/minecraftaccess/features/safety/fall/LongRangeFallDetector.java` [NEW]
- `src/test/java/org/mcaccess/minecraftaccess/features/safety/fall/CentralFallSafetyManagerTest.java` [NEW]
- `src/test/java/org/mcaccess/minecraftaccess/features/safety/fall/ProximityFallDetectorTest.java` [NEW]
- `src/test/java/org/mcaccess/minecraftaccess/features/safety/fall/LongRangeFallDetectorTest.java` [NEW]

### 8.2 File da Modificare
- `src/main/java/org/mcaccess/minecraftaccess/Config.java` [MODIFY]
- `src/main/java/org/mcaccess/minecraftaccess/features/FallDetector.java` [MODIFY - ridotto a facade delegante o migrato]
- `src/main/resources/assets/minecraft_access/lang/it_it.json` [MODIFY]
- `src/main/resources/assets/minecraft_access/lang/en_us.json` [MODIFY]

### 8.3 File Rigorosamente Congelati
- `src/main/java/org/mcaccess/minecraftaccess/features/safety/traversal/*` (Tutti congelati)
- `src/main/java/org/mcaccess/minecraftaccess/features/cognitive/*` (Congelati)
- `src/main/java/org/mcaccess/minecraftaccess/features/autowalk/*` (Congelati)

---

## 9. Criteri di Accettazione Finali

1. Creazione del ramo Git dedicato `feat/dual-fall-safety-subsystem` prima di toccare qualsiasi codice;
2. La classe `FallDetector.java` cessa di essere un monolite da 915 righe, delegando pulitamente ai 3 componenti specializzati;
3. Il Corto Raggio (1..6m) suona lo xilofono `NOTE_BLOCK_IRON_XYLOPHONE` per avvisi 2..6m e rallenta la corsa;
4. Il Corto Raggio suona l'incudine `ANVIL_HIT` a $1.0 - 1.5$m spegnendo all'istante lo xilofono (mutua esclusione acustica);
5. Il Corto Raggio innesta l'auto-sneak forzato a $d \le 0.85$m su burroni $\Delta Y \ge 4$;
6. Il Lungo Raggio (7..24m) suona la campanella `NOTE_BLOCK_BELL` attenuata a 3.5s solo in assenza di pareti solide continue;
7. In AutoWalk, il Lungo Raggio è silenziato al 100% e il Corto Raggio sopprime voci e xilofono;
8. La routine `validateAndNormalize()` garantisce che i range siano vincolati in $[1,6]$ e $[7,24]$ e scambiati automaticamente se invertiti;
9. La suite completa di test (esistenti + nuovi) è 100% verde senza regressioni (`.\gradlew.bat --no-daemon --no-watch-fs test`);
10. I file JSON di localizzazione mantengono il perfetto ordinamento alfabetico delle chiavi.

---

## 10. Ciclo di Revisione & Affinamento PRAPI (Protocollo 5 — Resa Acustica & Risoluzione Bug Sonori)

A seguito del primo collaudo empirico in-game di Luca (ore 13:00-13:10), è emersa una carenza di percezione acustica dovuta a 4 fattori concomitanti. Il presente ciclo PRAPI integra la soluzione confermata:

### 10.1 Contratto PRAPI-1: Transizione Dinamica di Stato (`lastWarnedStatus`) in `ProximityFallDetector`
- **Diagnosi:** In avvicinamento continuo, `dangerPos` (la coordinata della buca) non cambia, per cui `isNewDanger` rimaneva falso e l'incudine in Zona 2A (1.0..1.5m) non scattava mai.
- **Specifica Tecnica:**
  * Introduzione del campo d'istanza `private ProximityStatus lastWarnedStatus = ProximityStatus.CLEAR;`;
  * Calcolo di `boolean isStatusEscalation = (status != lastWarnedStatus);`;
  * La notifica scatta su `(isNewDanger || isStatusEscalation)`;
  * All'ingresso in `PRE_BRAKE_ZONE_2A`, l'incudine suona immediatamente azzerando lo xilofono;
  * In `handleDangerCleared` e `resetSafetyState`, `lastWarnedStatus` viene reimpostato a `CLEAR`.

### 10.2 Contratto PRAPI-2: Curva di Decadimento Lento Locale & Proiezione Vettoriale Anti-Cutoff in `LongRangeFallDetector`
- **Diagnosi:** OpenAL taglia a volume zero qualsiasi suono oltre 16 blocchi. Un beacon a 2.0m fissi avrebbe però cancellato la percezione della distanza richiesta da Luca.
- **Specifica Tecnica:**
  * *Curva di Decadimento Lento:* Calcolo del volume su distanza reale:
    $$V(d) = V_{base} - \left(\frac{d - d_{min}}{d_{max} - d_{min}}\right) \times (V_{base} - V_{floor})$$
    con $V_{base} = 0.80f$ e $V_{floor} = 0.40f$. A 7m suona a 0.80f, a 24m suona a 0.40f (nitido e mai ammutolito);
  * *Proiezione Vettoriale Sicura:* Coordinate del suono proiettate lungo il raggio direzionale verso il baratro, con distanza mappata nella finestra acustica sicura $[2.5 .. 12.0]$ metri dalla testa del giocatore:
    `soundVec = eyePos.add(dir.normalize().scale(acousticDist));`
  * *Isolamento Assoluto:* Nessuna alterazione delle impostazioni globali di OpenAL, listener o di altri suoni di gioco (passi, mob, blocchi).

### 10.3 Contratto PRAPI-3: Calibrazione Volumi di Default a Standard ASTRALIS in `Config.java`
- **Diagnosi:** Volume di default 0.4f sottodimensionato per l'udibilità con screen reader.
- **Specifica Tecnica:**
  * `public float volume = 0.80f;` (Standard aureo ASTRALIS 0.7f - 0.8f);
  * `public float longRangeVolumeMultiplier = 0.80f;`;
  * Clamping in `validateAndNormalize()` tra 0.1f e 1.0f.

### 10.4 Contratto PRAPI-4: Canale Audio Diretto per il Radar in `LongRangeFallDetector`
- **Diagnosi:** Il cue `PASSIVE` della campanella veniva scartato da `CognitiveCoordinator.flushTick` se c'erano eventi concorrenti del mirino o degli ostacoli.
- **Specifica Tecnica:**
  * Emissione del cue sonoro direttamente tramite `legacyAudioConsumer.accept(cue)` per garantire l'udibilità del rintocco ogni 3.5 secondi senza scarti verbali.

---

## 11. Checklist di Avanzamento PRAPI (Matrice a 3 Stati)

- [x] **PRAPI-1: Transizione dinamica `lastWarnedStatus` in `ProximityFallDetector` (Scatto Incudine continuo)** [COMPLETATO]
- [x] **PRAPI-2: Curva di decadimento lento del volume + Proiezione vettoriale sicura in `LongRangeFallDetector`** [COMPLETATO]
- [x] **PRAPI-3: Calibrazione volume base a 0.80f (Standard ASTRALIS) in `Config.java`** [COMPLETATO]
- [x] **PRAPI-4: Canale audio diretto per campanella Lungo Raggio (Zero scarto da eventi concorrenti)** [COMPLETATO]
- [x] **PRAPI-5: Test unitari headless a 0 ms aggiornati e verifica 100% verde** [COMPLETATO]
- [x] **PRAPI-6: Build shadowJar, re-deploy proattivo nelle istanze e secondo collaudo in-game di Luca** [COMPLETATO]

---

## 13. Ciclo PRAPI-B: Potenziamento Acustico Pre-Freno (`ANVIL_LAND` & `SoundSource.PLAYERS`)

### 13.1 Diagnosi del Secondo Collaudo In-Game
Dal secondo collaudo in-game (sessione delle 14:18-14:31) è emerso che:
1. La cinematica, le distanze (escalation 4 -> 3 -> 2 -> 1 -> orlo), l'auto-sneak, la soppressione e la campanella a lungo raggio funzionano perfettamente;
2. Tuttavia, il suono di pre-freno (Zona 2A a 1.0..1.5m) risulta quasi inudibile o sembra assente:
   - `SoundEvents.ANVIL_HIT` è un "toc" sordo e smorzato di durata brevissima ($< 150\text{ ms}$) privo di armoniche squillanti;
   - La contemporanea vocalizzazione ad alta priorità di NVDA (*"Attenzione: burrone..."*) produce mascheramento psicoacustico completo sul debole campione sonoro;
   - La categoria `SoundSource.BLOCKS` è soggetta al cursore "Blocchi" delle opzioni audio, che viene spesso tenuto basso dai giocatori per non sentire rumori fastidiosi di scavo.

### 13.2 Contratti Tecnici PRAPI-B

#### Contratto PRAPI-B1: Sostituzione Campione Sonoro con `SoundEvents.ANVIL_LAND`
- **File:** `ProximityFallDetector.java`, `FallDetector.java`
- **Specifica:**
  * Sostituire il supplier di default dell'incudine da `() -> SoundEvents.ANVIL_HIT` a `() -> SoundEvents.ANVIL_LAND`;
  * In caso di supplier nullo, il fallback deterministico usa `SoundEvents.ANVIL_LAND`;
  * In `FallDetector.java` (facade), aggiornare i riferimenti di fallback da `ANVIL_HIT` a `ANVIL_LAND`.
  * *Razionale:* `ANVIL_LAND` (`block.anvil.land`) possiede un transiente metallico acuto ad altissima energia seguito da un rimbombo potente: taglia all'istante qualsiasi parlato NVDA nelle cuffie e trasmette un'immediata sensazione di pericolo imminente.

#### Contratto PRAPI-B2: Allineamento Canale Audio a `SoundSource.PLAYERS`
- **File:** `ProximityFallDetector.java`
- **Specifica:**
  * In `buildFallEvent`: generare `SoundCue.of(soundEvent, SoundSource.PLAYERS, dangerPos, volume, 1.0f)`;
  * In `dispatchFallAlert` (legacy bypass): generare `SoundCue.of(soundEvent, SoundSource.PLAYERS, dangerPos, volume, 1.0f)`.
  * *Razionale:* Si conforma allo standard aureo di sicurezza di `PlayerStatus.java` e `knowledge/11_audio_3d_e_gerarchia_vocale.md`. Gli allarmi di incolumità personale operano sul bus `PLAYERS` garantendo immunità dall'attenuazione dei blocchi del mondo.

#### Contratto PRAPI-B3: Aggiornamento e Compatibilità della Suite di Test
- **File Coinvolti:**
  1. `ProximityFallDetectorTest.java`:
     - Test 3 (`testZone2APreBrakeWithAcousticMutualExclusivity`): asserisce `SoundEvents.ANVIL_LAND`;
     - Test 4 (`testZone2BMechanicalAutoSneak`): asserisce `SoundEvents.ANVIL_LAND`;
     - Test 7 (`testZone1ToZone2AEscalationOnSameDanger`): asserisce `SoundEvents.ANVIL_LAND`.
  2. `FallDetectorCognitiveDispatchTest.java`:
     - Test 1 (`testNewFallDangerEmitsCriticalFastPathWithSoundAndVoice`): asserisce `SoundEvents.ANVIL_LAND` e `SoundSource.PLAYERS`;
     - Test 3 (`testFallDangerSoundOnlyProducesNoSpokenText`): asserisce `SoundEvents.ANVIL_LAND` e `SoundSource.PLAYERS`;
     - Test 4 (`testEdgeBumpDebounce1500MsPreserved`): asserisce `SoundEvents.ANVIL_LAND`;
     - Test 6 (`testLegacyBypassWhenCoordinatorDisabled`): asserisce `SoundEvents.ANVIL_LAND` e `SoundSource.PLAYERS`.
  3. `SafetyEventFactoryTest.java`:
     - Test 4 (`testBuildFallEventVoiceAndSound`): asserisce `SoundEvents.ANVIL_LAND` e `SoundSource.PLAYERS`;
     - Test 6 (`testBuildFallEventSoundOnly`): asserisce `SoundEvents.ANVIL_LAND` e `SoundSource.PLAYERS`;
     - Test 7 (`testBuildFallEventEdgeBump`): asserisce `SoundEvents.ANVIL_LAND`.

---

## 14. Checklist di Avanzamento PRAPI-B (Matrice a 3 Stati)

- [x] **PRAPI-B1: Adozione di `SoundEvents.ANVIL_LAND` in `ProximityFallDetector` e `FallDetector`** [COMPLETATO]
- [x] **PRAPI-B2: Allineamento canale su `SoundSource.PLAYERS` in `ProximityFallDetector`** [COMPLETATO]
- [x] **PRAPI-B3: Aggiornamento asserzioni in `ProximityFallDetectorTest`** [COMPLETATO]
- [x] **PRAPI-B4: Aggiornamento asserzioni in `FallDetectorCognitiveDispatchTest` e `SafetyEventFactoryTest`** [COMPLETATO]
- [x] **PRAPI-B5: Esecuzione test suite 100% verde (344+ test a 0 ms)** [COMPLETATO]
- [x] **PRAPI-B6: Build shadowJar, re-deploy proattivo nelle istanze e terzo collaudo in-game di Luca** [COMPLETATO]

---

## 15. Stop Obbligatorio (Gating Semantico Sotto-Fase 1A PRAPI-B)

Il piano tecnico integrativo PRAPI-B è aggiornato e formalizzato.
**È fatto divieto assoluto di modificare il codice sorgente prima dell'esplicito comando di Luca ("procedi", "applica", "esegui").**


