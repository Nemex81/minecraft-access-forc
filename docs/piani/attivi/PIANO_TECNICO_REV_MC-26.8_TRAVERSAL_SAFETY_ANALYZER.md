# Piano Tecnico Implementativo — Rev. MC-26.8: Interruttore Diagnostico del Cognitive Coordinator (Ctrl+Alt+C) (ASTRALIS v2.6.2)

**Ramo di lavoro:** feat/cognitive-orchestrator  
**Ambito:** revisione MC-26.8 — Interruttore Diagnostico Globale Volatile (`CognitiveCoordinator`) (Differita a post-Fase 5 / pre-Fase 6)  
**Stato:** [PIANO AGGIORNATO E DIFFERITO A POST-FASE 5 / PRE-FASE 6 — STOP OBBLIGATORIO REGOLA 0]  
**Riferimenti:**
- docs/report/archivio/RAPPORTO_INDIRIZZO_CORRETTIVO_TRAVERSAL_SAFETY_ANALYZER.md
- docs/strategie/STRATEGIA_COGNITIVE_COORDINATOR.md
- docs/report/REGISTRO_REVISIONI.md

---

## 0. Decisione e perimetro

Il collaudo sul campo eseguito da Luca ha verificato che il problema storico dello sticky-sneak sulle scale a pioli a parete è **risolto**: davanti a una scala viene annunciata correttamente *"discesa sicura"* e il giocatore può attraversarla e scendere liberamente.

Per evitare sovraingegnerizzazione e preservare il comportamento positivo già validato:
- **Zero modifiche al dominio motorio/sicurezza:** non vengono modificati `FallDetector`, `TraversalSafetyAnalyzer`, `SafetyMovementGuard` né i relativi test;
- **Nessuna nuova factory:** non si introduce `TraversalSafetyEventFactory`;
- **Nessun refactor di calcolo caduta:** non si rimuovono i rami storici di `calculateDangerousDrop`;
- **Nessuna macchina a stati:** non si introduce alcuna macchina a stati per la transizione sulle scale;
- **Fase 3B protetta:** la Fase 3B (`ObstacleDetector`, mirino) resta formalmente chiusa, verificata e fuori perimetro.

L'anomalia segnalata (annuncio di *"discesa sicura"* su dislivelli minimi e non significativi) viene registrata nel Registro Revisioni come **Rev MC-26.10**, differita a una sessione successiva per riproduzione e test mirato, senza intaccare l'attuale comportamento protettivo.

Lo **scope operativo esclusivo** di MC-26.8 viene quindi circoscritto all'**interruttore diagnostico globale volatile di sessione per il `CognitiveCoordinator`**.

**Pianificazione Temporale (Direttiva Luca Chiusura Fase 4)**: Su direttiva esplicita di Luca, la lavorazione di questo piano viene formalmente **posticipata a valle del completamento e collaudo della Fase 5 (Movimento & Didattica)**, per essere affrontata come fase iniziale preparatoria pre-Fase 6.

---

## 1. Obiettivo operativo: Interruttore Diagnostico del Cognitive Coordinator (Ctrl+Alt+C)

Per consentire a Luca di eseguire test A/B immediati sul campo tra la nuova gestione cognitiva arbitrata e l'output legacy diretto senza arbitraggio, viene introdotto un interruttore rapido globale.

### 1.1 Specifiche Tecniche Vincolanti

1. **Scorciatoia Kuma**:
   - Combinazione: `Ctrl+Alt+C`;
   - Binding: `InputBinding.key(InputConstants.KEY_C, KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT))`;
   - Categoria: `KeyMappingCategories.OTHER` (categoria esistente nel progetto);
   - Tasto `KEY_C` verificato: libero con modificatori `CONTROL + ALT`.

2. **Owner e Ciclo di Vita**:
   - Gestito e posseduto da `CognitiveCoordinator` all'interno del proprio ciclo client;
   - Registrato nei KeyMappings di sistema tramite il medesimo schema Kuma già adottato dal progetto (`Kuma.createKeyMapping(...).withDefault(...).overrideCategory(...).handleWorldInput(...).build()`);
   - L'handler accetta il comando solo con `ModifierUtils.hasControlAndAlt()`, così da escludere combinazioni con Shift e mantenere la semantica delle scorciatoie esistenti.

3. **Natura Volatile di Sessione (Zero Scrittura su Disco)**:
   - Modifica unicamente un flag in memoria (`sessionDiagnosticDisabled` o equivalente), valido per l'intera sessione dell'applicazione Minecraft;
   - Non invoca `Config.saveConfig()` e non altera il file di configurazione dell'utente su disco;
   - Al successivo avvio dell'applicazione viene applicata normalmente la preferenza persistente `Config.getInstance().cognitiveCoordinator.cognitiveCoordinatorEnabled`;
   - Connessione, disconnessione o cambio mondo svuotano soltanto i buffer già previsti: non devono modificare lo stato scelto per la sessione.

4. **Svuotamento Atomico dei Buffer su Commutazione**:
   - A ogni commutazione (sia ON -> OFF sia OFF -> ON) viene invocato `CognitiveCoordinator.clearAllBuffers()`;
   - Lo svuotamento azzera istantaneamente: `tickBuffer`, `shortQueue`, memorie di attenzione, cache LRU di deduplicazione e `DirectInteractionShield`;
   - Nessun messaggio accumulato prima dello switch viene emesso tardivamente o come voce fantasma dopo il cambio di stato;
   - La riattivazione riparte a freddo, senza recuperare eventi pregressi.

5. **Instradamento Dinamico & Compatibilità Legacy**:
   - Quando disattivato: il coordinatore bypassa l'arbitraggio e inoltra direttamente l'output ai consumatori legacy già configurati; i moduli che possiedono un proprio fallback diretto lo conservano;
   - Quando attivato: gli eventi tornano all'arbitraggio ordinato del `CognitiveCoordinator`;
   - Zero impatto fisico: le protezioni motorie (`FallDetector`, `SafetyMovementGuard`, auto-sneak) continuano a funzionare regolarmente a prescindere dallo stato dell'interruttore vocale.

6. **Feedback Vocale Diretto**:
   - Poiché il coordinatore può essere appena stato spento dall'interruttore, la notifica di commutazione viene emessa **direttamente** via `MainClass.narrate(msg, true)`, senza dipendere dall'arbitraggio cognitivo;
   - Chiavi I18n dedicate:
     - `minecraft_access.cognitive.diagnostic_toggle.disabled`: "Coordinatore cognitivo disattivato (sessione)"
     - `minecraft_access.cognitive.diagnostic_toggle.enabled`: "Coordinatore cognitivo attivato (sessione)"
     - Versioni corrispondenti in `en_us.json`, posizionate in rigoroso ordine alfabetico crescente.

---

## 2. Inventario dei file

### 2.1 File da Modificare (Solo per Toggle Diagnostico)
- `src/main/java/org/mcaccess/minecraftaccess/features/cognitive/CognitiveCoordinator.java`: integrazione del toggle `Ctrl+Alt+C`, flag volatile in memoria, svuotamento atomico buffer ed emissione feedback diretto;
- `src/main/resources/assets/minecraft_access/lang/it_it.json`: aggiunta chiavi toggle in ordine alfabetico crescente;
- `src/main/resources/assets/minecraft_access/lang/en_us.json`: aggiunta chiavi toggle in ordine alfabetico crescente;
- `src/test/java/org/mcaccess/minecraftaccess/features/cognitive/CognitiveCoordinatorDiagnosticToggleTest.java`: test unitari dedicati per la commutazione volatile, lo svuotamento buffer e l'instradamento legacy.

### 2.2 File Rigorosamente CONGELATI e NON TOCCATI
- `FallDetector.java`, `TraversalSafetyAnalyzer.java`, `SafetyMovementGuard.java` e tutti i DTO in `features.safety.traversal`;
- `ObstacleDetector.java`, `CrosshairFeedbackManager.java`, `NarrateCrosshair.java` (Fase 3B chiusa e protetta);
- `NumpadControls.java` (differito a Rev MC-26.9 con `Ctrl+Alt+F8`);
- Nessuna nuova classe `TraversalSafetyEventFactory.java`;
- Nessun file di configurazione persistente su disco.

---

## 3. Piano di test obbligatorio

### 3.1 Test Unitari del Toggle Diagnostico (Nuovi)
1. **Verifica Stato Volatile**: invocazione toggle commuta lo stato del coordinatore in memoria; `Config.getInstance().cognitiveCoordinator.cognitiveCoordinatorEnabled` resta invariato;
2. **Verifica Svuotamento Atomico**: alla commutazione (sia disattivazione sia riattivazione), `clearAllBuffers()` azzera code, buffer e scudi;
3. **Verifica Routing A/B**:
   - con toggle disattivato: gli eventi inviati a `CognitiveCoordinator` non sono arbitrati e vengono inoltrati direttamente ai consumatori legacy configurati;
   - con toggle riattivato: ripresa normale del dispatch e dell'arbitraggio;
4. **Verifica Feedback Diretto**: invocazione del toggle produce la vocalizzazione appropriata a latenza zero.

### 3.2 Collaudo Manuale NVDA sul Campo
1. Test in-game sul percorso del tetto e scale: pressione di `Ctrl+Alt+C` per alternare tra arbitraggio cognitivo attivo e output legacy diretto;
2. Verifica che la scala rimanga transitabile e la protezione caduta resti attiva in entrambe le modalità;
3. Verifica dell'immediatezza della sintesi vocale e dell'assenza di annunci fantasma residui.

---

## 4. Criteri di accettazione finali

- La scala sul tetto continua a consentire la discesa fluida senza regressioni;
- Nessun file del dominio motorio/sicurezza o della Fase 3B è stato modificato;
- L'interruttore `Ctrl+Alt+C` commuta istantaneamente lo stato del coordinatore in memoria;
- I buffer vengono svuotati atomicamente ad ogni switch;
- Zero modifiche alla configurazione salvata su disco;
- Tutta la suite di test automatizzati è verde (`0 failure, 0 error`).

---

## 5. Checkpoint vincolante (Regola 0)

Questo documento autorizza esclusivamente la pianificazione e l'allineamento dei requisiti tecnici.

**Divieto Assoluto di Modifica Autonoma**: nessun file sorgente Java o file di build è stato modificato né può essere modificato prima dell'esplicita approvazione congiunta di Luca e di ChatGPT.
