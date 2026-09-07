# Strategia Logico-Cognitiva: Gestione Intelligente e Automatica di Porte e Varchi (AutoOpen & AutoClose Doors)

- **Autori**: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity (Senior AI Pair Programmer)
- **Ramo Git di Riferimento**: `feat/cognitive-orchestrator`
- **Piattaforma Target**: Minecraft 1.26.2 (Fabric / Java 25)
- **Framework di Governance**: ASTRALIS v3.0.2 (Fase 3 — Chiusura Tecnica & Living Documentation)
- **Stato**: `[STRATEGIA COMPLETATA, COLLAUDATA AL 100% E CONVALIDATA DA LUCA IN-GAME]`
- **Data di Convalida Luca**: 2026-09-07
- **Documenti Correlati**:
  * [`docs/piani/attivi/PIANO_TECNICO_REV_MC-26.13_AUTO_DOORS.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/attivi/PIANO_TECNICO_REV_MC-26.13_AUTO_DOORS.md)
  * [`docs/strategie/attive/STRATEGIA_COGNITIVE_COORDINATOR.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/attive/STRATEGIA_COGNITIVE_COORDINATOR.md)
  * [`docs/report/REGISTRO_REVISIONI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/REGISTRO_REVISIONI.md)
  * [`src/main/java/org/mcaccess/minecraftaccess/Config.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/Config.java)
  * [`src/main/java/org/mcaccess/minecraftaccess/features/autowalk/MovementCoordinator.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/autowalk/MovementCoordinator.java)
  * [`src/main/java/org/mcaccess/minecraftaccess/features/door/DoorInteractionHelper.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/door/DoorInteractionHelper.java)

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
- **Preservare l'orientamento mentale**: sia nel movimento manuale sia in AutoWalk, la chiusura è programmatica, istantanea e diegetica alle spalle, a visuale rigorosamente fissa (Zero Disorientamento della Visuale), preservando al 100% l'orientamento di Yaw/Pitch e lasciando all'audio 3D posizionale HRTF il feedback di chiusura dietro le orecchie.

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

#### 3.1 Flusso A: Movimento Manuale (WASD / Giocatore a Piedi — Copertura 100%)

Il movimento manuale gestisce sia i varchi orizzontali (porte, cancelli), sia le botole a soffitto su rampe o scale a pioli:

1. **Varchi Orizzontali (Porte e Cancelli)**:
   - *Avanzamento*: Rileva la porta chiusa lungo il vettore di marcia a $d \le 1.5\text{ m}$;
   - *AutoOpen*: Invia `useItemOn` mirato al blocco porta. Zero alterazione di Yaw/Pitch (la testa del giocatore non si muove);
   - *Transito & AutoClose*: Appena il giocatore supera la soglia ed entra nel blocco adiacente a distanza di sicurezza calibrata ($d \ge 0.90\text{ m}$ dal centro varco, normalizzato su blocco canonico `LOWER`), invia `useItemOn` per chiudere la porta alle spalle con riproduzione diegetica 3D del suono di chiusura alle spalle (volume calibrato $0.75\text{f}$) e notifica vocale inequivocabile *"Porta chiusa alle spalle"*. Zero disorientamento della visuale (Yaw/Pitch invariati al 100%).

2. **Varchi Verticali & Scale a Pioli (Botole a Soffitto — `TrapDoorBlock`)**:
   - *Salita su Scala a Pioli o Rampa*: Quando il giocatore è in arrampicata (`isClimbing() == true`) o si muove verso l'alto con `W`/`Spazio` e a quota testa/soffitto ($Y+1$ o $Y+2$) è presente una botola chiusa (`!IRON_TRAPDOOR`);
   - *AutoOpen Verticale*: La botola viene aperta automaticamente prima dell'impatto della testa, consentendo la salita fluida senza urti;
   - *Emersione & AutoClose a Terra*: Non appena il giocatore emerge sul pavimento superiore e la sua hitbox orizzontale esce dalla sagoma del foro $1 \times 1$ ($d_{XZ} \ge 0.85\text{ m}$ con piedi a quota $\ge Y_{\text{trapdoor}}$), la botola viene richiusa automaticamente sotto i piedi con riproduzione 3D del suono alle spalle/in basso e notifica vocale *"Botola chiusa alle spalle"*, sigillando la tromba scale.

### 3.2 Flusso B: Navigazione Automatica (AutoWalk Motor — Percorsi Calpestabili Unificati)

Nel flusso AutoWalk, la chiusura automatica delle porte viene unificata al 100% con la logica del movimento manuale, eliminando la rotazione cinetica a 180° dello sguardo (che introduceva latenze e desincronizzazioni legate al clock del processore e al frame rate).

Il ciclo di attraversamento e chiusura unificato in AutoWalk opera in modo lineare e sequenziale:
- **Se** il navigatore AutoWalk rileva una porta chiusa lungo la rotta sul prossimo nodo:
  - **Allora** arresta l'avanzamento ed emette l'annuncio vocale *"Porta chiusa, apertura automatica"*;
- **Se** la porta non è di ferro ed è interagibile a mano:
  - **Allora** invia il comando di apertura `DoorInteractionManager.interactWithDoor` e registra la sessione di varco in `DoorInteractionManager` con la posizione canonica del blocco (`LOWER`);
- **Se** l'apertura ha avuto successo:
  - **Allora** AutoWalk riprende fluidamente la marcia sul tracciato senza interruzioni;
- **Se** il personaggio attraversa la soglia e si allontana nel blocco adiacente oltre la distanza di sicurezza ($d \ge 0.90\text{ m}$):
  - **Allora** `DoorInteractionManager` (in esecuzione sul tick client) invia programmaticamente il comando di chiusura `interactWithDoor` direttamente sul blocco della porta lasciata alle spalle, emette il suono diegetico 3D alle spalle e notifica con sintesi vocale *"Porta chiusa alle spalle"*;
- **Se** la chiusura avviene durante la navigazione:
  - **Allora** la visuale (Yaw e Pitch) del giocatore non subisce alcuna alterazione (Zero Disorientamento della Visuale), preservando la rotta della bussola e la fluidità di marcia verso il goal.

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

4. **Cancello 4 — Watchdog di Timeout Fail-Safe (6.0 Secondi) & Passage Renewal**:
   - *Rischio*: Se durante la manovra di attraversamento o chiusura un ostacolo imprevisto o un'esitazione del giocatore fa scadere prematuramente la sessione;
   - *Regola Inviolabile*: Il modulo include un timer watchdog rilassato di **$6000\text{ ms}$** dotato di **Passage Renewal**: se il giocatore viene rilevato attivamente nel vano della porta ($dist \le 0.65\text{ m}$), il timer si azzera dinamicamente al tick corrente perché il passaggio è in corso. Al superamento definitivo dei 6 secondi senza completamento del transito, la sessione decade silenziosamente senza bloccare il gioco.

5. **Cancello 5 — Determinismo Headless & Zero Sleep a 0 ms**:
   - *Regola Inviolabile*: La macchina a stati finita deve essere interamente testabile tramite fixture headless disaccoppiate da OpenGL/LWJGL (`DoorInteractionTest`), simulando transizioni di stato, distanze e orientamenti matematici a latenza zero ($0\text{ ms}$).

---

## 🗺️ 5. Piano delle Fasi Successive

- **Fase 0 (Questo Documento)**: Sottoposizione della strategia logico-cognitiva alla revisione e convalida di Luca.
- **Sotto-Fase 1A**: Redazione del Piano Tecnico Formale con i contratti denominati D1..DN in `docs/piani/attivi/` e Stop Obbligatorio.
- **Sotto-Fase 1B**: Implementazione del codice, configurazione Cloth Config, stringhe `it_it.json`/`en_us.json` e suite di test unitari a 0 regressioni.
- **Fase 2**: Deploy proattivo nelle istanze PrismLauncher e telemetria live durante il collaudo pratico in-game di Luca.
