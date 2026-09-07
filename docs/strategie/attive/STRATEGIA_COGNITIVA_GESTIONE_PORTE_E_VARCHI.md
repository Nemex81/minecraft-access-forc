# Strategia Logico-Cognitiva: Gestione Intelligente e Automatica di Porte e Varchi (AutoOpen & AutoClose Doors)

- **Autori**: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity (Senior AI Pair Programmer)
- **Ramo Git di Riferimento**: `feat/cognitive-orchestrator`
- **Framework di Governance**: ASTRALIS v3.0.2 (Fase 0 — UPCS Unified Progressive Cognitive Strategy)
- **Stato**: `[STRATEGIA ATTIVA — FASE 0 IN ATTESA DI CONVALIDA FORMALE DI LUCA]`
- **Data di Redazione**: 2026-09-07
- **Documenti Correlati**:
  * [`docs/strategie/STRATEGIA_COGNITIVE_COORDINATOR.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/STRATEGIA_COGNITIVE_COORDINATOR.md)
  * [`docs/strategie/archivio/STRATEGIA_COGNITIVA_SILENZIAMENTO_SENSORIALE_AUTOWALK.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/archivio/STRATEGIA_COGNITIVA_SILENZIAMENTO_SENSORIALE_AUTOWALK.md)
  * [`docs/report/REGISTRO_REVISIONI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/REGISTRO_REVISIONI.md)
  * [`src/main/java/org/mcaccess/minecraftaccess/Config.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/Config.java)
  * [`src/main/java/org/mcaccess/minecraftaccess/features/autowalk/MovementCoordinator.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/autowalk/MovementCoordinator.java)

---

## 🧭 1. Visione Cognitiva & Obiettivo di Accessibilità (Screen Reader NVDA)

Nel gameplay di Minecraft per giocatori non vedenti, l'interazione con le porte, i cancelletti e le botole costituisce uno dei punti di massimo attrito operativo, disorientamento spaziale e vulnerabilità tattica:

1. **Vulnerabilità nei Rientri Tattici (Inseguimento Mob)**:
   - Quando il giocatore torna alla propria base o rifugio inseguito da creature ostili (zombie, scheletri, creeper), fermarsi sulla soglia, ruotare la visuale di 180° alla cieca, inquadrare il battente aperto e azionare la chiusura è un'operazione complessa e lenta che spesso culmina nell'infiltrazione dei nemici o nella morte del personaggio;
2. **Attrito e Frammentazione della Marcia Guidata (AutoWalk)**:
   - Durante la navigazione automatica, incontrare una porta chiusa impone oggi l'arresto completo del motore con la richiesta vocale *"Porta chiusa davanti a te. Premi Tasto Destro per aprire"*. Il giocatore deve intervenire manualmente, attendere il passaggio e poi ricordarsi di chiudere la porta manualmente una volta giunto all'interno, spezzando la continuità dell'esperienza assistita;
3. **Disorientamento Spaziale da Rotazione Cieca nel Movimento Manuale**:
   - Cercare manualmente una porta appena attraversata per chiuderla comporta la perdita dell'allineamento dello sguardo (Yaw/Pitch) e della rotta mentale, costringendo il giocatore a raddrizzare la testa a Nord con `5` o a verificare nuovamente i gradi bussola.

### 1.1 Il Principio di Soluzione ASTRALIS: Transizioni Fluide e Zero Disorientamento
L'obiettivo di questa strategia è implementare un sistema unificato, robusto e configurabile per:
- **Aprire automaticamente la porta** davanti al giocatore quando vi è un evidente intento di avanzamento;
- **Chiudere automaticamente la porta alle spalle** una volta completato l'attraversamento fisico della soglia;
- **Preservare l'orientamento mentale**: nel movimento manuale, la rotazione di chiusura è una micro-manovra transitoria atomica che ripristina all'istante l'orientamento precedente; nell'AutoWalk, il cinetismo esegue la manovra fluida e riprende automaticamente il tracciato calcolato.

---

## 🏛️ 2. Collocazione Architetturale & Nuova Sezione di Configurazione

Per garantire la massima coerenza e reperibilità dell'opzione sia per chi usa il movimento manuale sia per chi usa il navigatore, viene introdotta in Cloth Config la nuova categoria dedicata:

### Sezione `doorInteraction` (*"Interazione con Porte e Varchi"*)
- **`autoOpenDoors`** (boolean, default: `true`):
  - *Etichetta*: `"Apri porte automaticamente"`
  - *Tooltip*: `"Se abilitato, apre automaticamente le porte chiuse quando ci si muove verso di esse sia manualmente sia in navigazione automatica."`
- **`autoCloseDoors`** (boolean, default: `true`):
  - *Etichetta*: `"Chiudi porte automaticamente dopo il passaggio"`
  - *Tooltip*: `"Se abilitato, richiude automaticamente la porta alle proprie spalle una volta superata la soglia sia a piedi sia in navigazione automatica."`
- **`includeGatesAndTrapdoors`** (boolean, default: `true`):
  - *Etichetta*: `"Includi cancelletti e botole"`
  - *Tooltip*: `"Estende l'apertura e la chiusura automatica anche ai cancelletti di staccionata e alle botole orizzontali."`
- **`doorNarration`** (boolean, default: `true`):
  - *Etichetta*: `"Notifiche vocali azioni sulle porte"`
  - *Tooltip*: `"Vocalizza brevemente gli interventi automatici sulle porte (es. 'Porta aperta', 'Porta chiusa')."`

---

## 🔄 3. I Due Flussi Cinetici & Macchina a Stati Finita (FSM)

Il componente centrale `DoorInteractionManager` opera tramite una Macchina a Stati Finita con due modalità operative disaccoppiate:

### 3.1 Flusso A: Movimento Manuale (WASD / Giocatore a Piedi)

```text
[Giocatore a contatto con Porta Chiusa] + [Tasto W premuto verso la porta]
                          │
                          ▼
             [Fase 1: Snapshot Visuale]
        (Salva savedYaw, savedPitch attuali)
                          │
                          ▼
             [Fase 2: Interazione Apertura]
        (Punta centro porta, useItemOn mirato)
                          │
                          ▼
             [Fase 3: Ripristino Istantaneo]
        (Ripristina subito savedYaw e savedPitch)
        (Il giocatore continua a camminare dritto)
                          │
                          ▼
       [Fase 4: Rilevamento Attraversamento Varco]
 (DoorTraversalTracker: distanza dal varco > 1.3m sul lato opposto)
                          │
                          ▼
             [Fase 5: Micro-Manovra Chiusura]
        (Salva Yaw/Pitch correnti del giocatore)
        (Mira su pannello porta aperta alle spalle)
        (useItemOn mirato di chiusura)
        (Ripristina all'istante Yaw/Pitch salvati)
        (Annuncio discreto: "Porta chiusa")
        (Nessun avanzamento automatico: controllo 100% all'utente)
```

### 3.2 Flusso B: Navigazione Automatica (AutoWalk Motor)

```text
[AutoWalk rileva Porta Chiusa sul prossimo nodo percorso]
                          │
                          ▼
             [Fase 1: Notifica Vocale / Sonora]
            (Annuncio: "Porta chiusa, apertura...")
                          │
                          ▼
             [Fase 2: Apertura Porta]
   (Orientamento sguardo verso la porta, useItemOn)
                          │
                          ▼
             [Fase 3: Attraversamento Controllato]
(AutoWalkMotor fa avanzare il personaggio di 1.5 - 2.0 nodi oltre la soglia)
                          │
                          ▼
             [Fase 4: Arresto Breve & Rotazione]
       (Frenata avanzamento, rotazione mirata verso
          la porta aperta lasciata alle spalle)
                          │
                          ▼
             [Fase 5: Chiusura & Feedback]
             (useItemOn per chiudere la porta)
          (Segnale acustico / Voce: "Porta chiusa")
                          │
                          ▼
             [Fase 6: Riallineamento & Ripresa]
(Riorientamento fluido dello sguardo verso il prossimo nodo del tracciato)
      (Ripresa automatica della marcia senza interruzioni)
```

---

## 🛡️ 4. I 5 Cancelli Inviolabili di Sicurezza (Inner Codex Pattern)

In conformità con il Protocollo 12 del framework ASTRALIS, la progettazione rispetta rigorosamente i seguenti vincoli architetturali:

1. **Cancello 1 — Scudo d'Uso Oggetti in Mano (`ItemUseShield`)**:
   - *Rischio*: Invocare un click destro generico tenendo in mano un secchio di lava, una pozione, del cibo o un blocco provocherebbe l'uso involontario dell'oggetto (versamento lava, bevuta, piazzamento errato);
   - *Regola Inviolabile*: L'interazione deve essere inoltrata **esclusivamente tramite `client.gameMode.useItemOn(...)`** specificando la mano e il `BlockHitResult` geometrico esatto della porta. In alternativa, il controller invoca l'azione assicurandosi che `BlockState.useWithoutItem(...)` abbia la precedenza nativa sul blocco.

2. **Cancello 2 — Porte di Ferro & Varchi Meccanici (`IronDoorBlock`)**:
   - *Rischio*: Le porte di ferro non rispondono al click destro manuale (richiedono pulsanti, leve o redstone). Tentare di aprirle col tasto destro genererebbe tentativi a vuoto o loop infiniti;
   - *Regola Inviolabile*: Verifica preventiva `if (state.getBlock() instanceof IronDoorBlock)`. Le porte di ferro vengono escluse dall'auto-apertura diretta, con notifica vocale dedicata: *"Porta di ferro, richiede interruttore o piastra"*.

3. **Cancello 3 — Calcolo Geometrico della Hitbox della Porta Aperta (Voxel Offset a 90°)**:
   - *Rischio*: Quando una porta si apre, il pannello ruota di 90° adagiandosi allo stipite laterale. Mirare al centro della coordinata del blocco d'aria lascerebbe il raggio nel vuoto;
   - *Regola Inviolabile*: Il vettore di puntamento per la chiusura deve calcolare l'offset effettivo della piastra in base alle proprietà del blockstate (`DoorBlock.FACING`, `DoorBlock.HINGE` e `DoorBlock.OPEN`), garantendo che il raycast colpisca infallibilmente la sagoma della porta aperta.

4. **Cancello 4 — Watchdog di Timeout Fail-Safe (2.0 Secondi)**:
   - *Rischio*: Se durante la manovra di attraversamento o chiusura un ostacolo imprevisto (un mob sulla soglia, un dislivello, un blocco caduto) impedisce il superamento del varco;
   - *Regola Inviolabile*: Il modulo include un timer watchdog massimo di **$2000\text{ ms}$**. Al superamento della soglia senza completamento del passaggio, la manovra viene abortita, la visuale originale viene ripristinata e il controllo viene restituito immediatamente all'utente con notifica vocale.

5. **Cancello 5 — Determinismo Headless & Zero Sleep a 0 ms**:
   - *Regola Inviolabile*: La macchina a stati finita deve essere interamente testabile tramite fixture headless disaccoppiate da OpenGL/LWJGL (`DoorInteractionTest`), simulando transizioni di stato, distanze e orientamenti matematici a latenza zero ($0\text{ ms}$).

---

## 🗺️ 5. Piano delle Fasi Successive

- **Fase 0 (Questo Documento)**: Sottoposizione della strategia logico-cognitiva alla revisione e convalida di Luca.
- **Sotto-Fase 1A**: Redazione del Piano Tecnico Formale con i contratti denominati D1..DN in `docs/piani/attivi/` e Stop Obbligatorio.
- **Sotto-Fase 1B**: Implementazione del codice, configurazione Cloth Config, stringhe `it_it.json`/`en_us.json` e suite di test unitari a 0 regressioni.
- **Fase 2**: Deploy proattivo nelle istanze PrismLauncher e telemetria live durante il collaudo pratico in-game di Luca.
