# Report di Sessione: Rev MC-26.13 — Unificazione Architetturale AutoClose a Visuale Fissa & Clean Sweep

- **Autori**: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity (Senior AI Pair Programmer)
- **Ramo Git**: `feat/cognitive-orchestrator`
- **Data Sessione**: 2026-09-07
- **Framework di Riferimento**: ASTRALIS v3.0.2 (Protocollo 4, 5, RRU & Fase 3 Chiusura Tecnica)
- **Esito Collaudo**: `[COLLAUDATO CON SUCCESSO AL 100% IN-GAME DA LUCA]`

---

## 🎯 1. Obiettivo & Causa Radice

Nel gameplay con screen reader NVDA, la gestione delle porte costituiva uno dei maggiori punti di attrito:
1. **Movimento Manuale**: Il giocatore rischiava infiltrazioni di mob alle spalle dopo essere entrato nella propria base;
2. **AutoWalk**: La vecchia manovra cinetica a 180° inserita in `AutoWalkMotor` risultava fragile e intermittente a causa delle desincronizzazioni tra tick rate logico di Minecraft (20 TPS) e clock del processore / framerate di rendering, provocando scatti visivi e perdita dei comandi;
3. **Soluzione ASTRALIS Unificata**: Creazione di un hub centralizzato (`DoorInteractionManager`) a visuale rigorosamente fissa (**Zero Disorientamento della Visuale** sia in manuale sia in AutoWalk), con chiusura programmatica alle spalle superata la soglia di $0.90\text{ m}$, suono 3D OpenAL posizionato sul blocco alle spalle e notifica vocale univoca *"Porta chiusa alle spalle"*.

---

## 🛠️ 2. Architettura & File Coinvolti

1. **`src/main/java/org/mcaccess/minecraftaccess/features/door/DoorInteractionManager.java` [NUOVO]**:
   - Gestore FSM centralizzato su tick client (`BalmClientModule`);
   - Metodo pubblico `registerSession` per accogliere sessioni sia dal manuale WASD sia dalla navigazione AutoWalk;
   - Monitoraggio geometrico: scatto chiusura a $d \ge 0.90\text{ m}$ dal centro varco normalizzato `LOWER`;
   - *Passage Renewal*: rinnovo dinamico del timestamp se il giocatore staziona nel vano ($d \le 0.65\text{ m}$);
   - Fail-safe *Watchdog*: timeout a $6000\text{ ms}$ per esitazioni prolungate o retromarce.
2. **`src/main/java/org/mcaccess/minecraftaccess/features/door/DoorInteractionHelper.java` [AGGIORNATO]**:
   - Predicati geometrici purs testabili a 0 ms (`isPlayerAcrossDoor`, `isPlayerInsideDoorWay`, `isPlayerSafelyOutsideTrapdoorShaft`);
   - Riconoscimento porte/cancelli/botole interagibili a mano ed esclusione tassativa di porte di ferro (`Blocks.IRON_DOOR`, `Blocks.IRON_TRAPDOOR`);
   - Metodo `playDoorCloseSound` per audio 3D posizionale OpenAL con volume di sicurezza congelato ($0.75\text{f}$).
3. **`src/main/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkMotor.java` [BONIFICATO & UNIFICATO]**:
   - **ASTRALIS Clean Sweep**: eliminazione completa dei campi orfani `doorToClosePos` e `doorCloseManeuverTicks` e del blocco di codice a 3 tick della manovra a 180°;
   - Delega diretta in `processDoorWait`: all'apertura della porta, registrazione atomica della sessione in `DoorInteractionManager` con coordinate canoniche `LOWER`;
   - Proseguimento fluido della marcia senza soste e senza toccare Yaw o Pitch.
4. **`src/main/java/org/mcaccess/minecraftaccess/Config.java` & I18N (`it_it.json`, `en_us.json`)**:
   - Nuova categoria Cloth Config `doorInteraction` (`autoOpenDoors`, `autoCloseDoors`, `includeGatesAndTrapdoors`, `doorNarration`);
   - Localizzazioni bilingue ordinate alfabeticamente per piena conformità `linting.yml`.
5. **`src/test/java/org/mcaccess/minecraftaccess/features/door/DoorInteractionManagerTest.java` [NUOVO]**:
   - 8 test unitari headless a 0 ms con mock e clock virtuale deterministico.

---

## 🧪 3. Metriche di Verifica, Telemetria & Collaudo In-Game

- **Test Automatici**: `323/323` test passati (100% verdi, 0 failures, 0 errori).
- **Compilazione**: `BUILD SUCCESSFUL` (jar `minecraft-access-1.12.0-SNAPSHOT.jar`).
- **Deploy Automatico**: Distribuito nelle istanze PrismLauncher `Minecraft 26.2 Access 1.12.0` e `Minecraft 26.2 Access - Server Tenuta`.
- **Evidenza Telemetrica In-Game (latest.log)**:
  - Ore 14:32:11 - Movimento manuale WASD: apertura automatica e chiusura alle spalle a $d \ge 0.90\text{ m}$;
  - Ore 14:33:02 e 14:33:12 - AutoWalk multi-porta verso "ingresso est tenuta": due porte consecutive aperte in corsa e richiuse alle spalle a marcia continuativa;
  - Ore 14:33:38 e 14:33:42 - AutoWalk verso "casa porta primo piano": transito intermedio e arrivo a destinazione con chiusura perfetta della porta finale;
  - Zero errori, zero eccezioni, zero scatti visivi.
