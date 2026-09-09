# Piano Tecnico Formale — Rev MC-26.22: Neutralizzazione Mixin Tasti Vanilla F1..F6 e Batteria Interruttori Suoni Radar POI per Categoria (ASTRALIS v3.0.4)

- **Tipologia**: CORRETTIVO + EVOLUTIVO / ACCESSIBILITY ENHANCEMENT (Rev MC-26.22)
- **Autore**: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity (Senior AI Pair Programmer)
- **Revisori**: Luca / Antigravity
- **Data e Ora**: 2026-09-09T19:52:00+02:00
- **Ramo Git di Riferimento**: feat/dual-fall-safety-subsystem
- **Incremento Versione Target (AVF)**: 26.2-1.20.0 -> 26.2-1.21.0
- **Stato Operativo**: [SOTTO-FASE 1A: PIANO TECNICO AGGIORNATO — STOP OBBLIGATORIO PRIMA DEL CODICE]
- **Documenti Correlati (Pointer Hub DRY)**:
  * docs/report/REGISTRO_REVISIONI.md
  * knowledge/06_controlli_avanzati_e_bridge_chatgpt.md
  * docs/piani/attivi/PIANO_TECNICO_REV_MC-26.21_INTERRUTTORI_SENSORI_UNIVOCITA_DIDGERIDOO_E_CTRL_DESTRO.md

---

## Scaletta Fasi Implementative (Gating: [ ] In Attesa, [/] In Corso, [x] Completato)

- [x] D0: Mixin KeyboardHandlerMixin.java (signature corretta KeyEvent) + DebugScreenEntryListMixin.java + MinecraftMixin.java (F1/F5)
- [x] D1: Config.POI.Blocks — 7 flag soundEnabled per categorie blocchi
- [x] D2: Config.POI.Entities — flag soundEnabledHostile e soundEnabledPassive (+ altri 7 per future espansioni)
- [x] D3: POIGroup.java — BooleanSupplier soundEnabledSupplier + guardia in playSoundForGroupItems()
- [x] D4: BuiltinBlockPOIGroups.java — collegamento ogni gruppo alla config flag
- [x] D5: BuiltinEntityPOIGroups.java — collegamento ogni gruppo alla config flag
- [x] D6: Batteria Kuma Ctrl+Alt+F7..F12 + G + H + P in POIBlocks.java e POIEntities.java
- [x] D7: QuickKeysHelpScreen.java — Categoria 8 Interruttori Suoni Radar POI
- [x] D8: I18N it_it.json + en_us.json (ordinate alfabeticamente)
- [x] D9: minecraft_access.mixins.json — aggiunta KeyboardHandlerMixin e DebugScreenEntryListMixin
- [x] D10: knowledge/06_controlli_avanzati_e_bridge_chatgpt.md aggiornata
- [x] Verifica: gradlew test + shadowJar + deploy proattivo [COMPLETATO]
- [/] Collaudo In-Game (Luca)

---

## 1. Contesto e Motivazione

### Problema A — Conflitto F1..F6 con tasti funzione vanilla Minecraft
Quando Luca preme Ctrl+Alt+F3, Minecraft vanilla intercetta F3 in KeyboardHandler.handleDebugKeys()
e apre la Debug Screen. Stesso problema per F5 (cambio prospettiva). I 6 interruttori funzionano
correttamente a livello logico ma il side-effect vanilla degrada l'esperienza.

Causa Radice: La pipeline input processa le hotkey debug in KeyboardHandler.handleDebugKeys prima
che Kuma possa segnalare il consumo dell'evento.
Pattern gia noto nel progetto: MinecraftMixin.java intercetta gia handleKeybinds con
@Inject + ci.cancel() — stesso pattern da applicare qui.

### Problema B — Nessun interruttore per le singole categorie del radar POI
Il radar POI emette suoni ogni 3 secondi per ogni categoria. Non e possibile silenziare solo
le PORTE lasciando attivi i MINERALI, oppure solo il radar OSTILI lasciando attiva la sentinella F6.
Luca ha richiesto un interruttore dedicato per ogni categoria.

---

## 2. Distinzione Critica: Ctrl+Alt+F6 (sentinella) vs Ctrl+Alt+H (radar ostili)

Questi due meccanismi sono DISTINTI e NON si sovrappongono:

Ctrl+Alt+F6 (gia esistente — Rev MC-26.21):
  - Campo: Config.POI.Entities.hostileThreatAlerts
  - Funzione: sentinella ravvicinata — allarme basedrum ogni 3.5s se un mob ostile e entro 6 blocchi
  - Scopo: avviso di pericolo imminente (allarme di emergenza)

Ctrl+Alt+H (nuovo — Rev MC-26.22):
  - Campo: Config.POI.Entities.soundEnabledHostile
  - Funzione: silenzia il suono NOTE_BLOCK_BELL (pitch 2.0f) del radar POI periodico per i mob ostili
               entro 24 blocchi, emesso ogni 3s dallo scanner generale
  - Scopo: ridurre il rumore di fondo del radar in zone affollate di mob

---

## 3. Mappa Definitiva dei Tasti (Tastiera IT fino a F12)

Batteria esistente F1..F6 (Rev MC-26.21 — invariata):
  Ctrl+Alt+F1 -> Faro waypoint (POIWaypoints.toggleAudioBeacon)
  Ctrl+Alt+F2 -> Rilevatore ostacoli (ObstacleDetector.toggleObstacleDetector)
  Ctrl+Alt+F3 -> Buche corto raggio (CentralFallSafetyManager.toggleProximityFallDetector)
  Ctrl+Alt+F4 -> Radar orografico (CentralFallSafetyManager.toggleLongRangeFallDetector)
  Ctrl+Alt+F5 -> Suono mirino elevazione (NarrateCrosshair.toggleCrosshairAudio)
  Ctrl+Alt+F6 -> Sentinella mob ostili ravvicinata (POIEntities.toggleHostileRadar)

Nuova batteria F7..F12 + lettere mnemoniche (Rev MC-26.22):
  Ctrl+Alt+F7  -> POI Blocchi MINERALI (ORE)          -> soundEnabledOre
  Ctrl+Alt+F8  -> POI Blocchi FUNZIONALI (FUNCTIONAL)  -> soundEnabledFunctional
  Ctrl+Alt+F9  -> POI Blocchi PORTE (DOOR)             -> soundEnabledDoor
  Ctrl+Alt+F10 -> POI Blocchi PORTALI (PORTAL)         -> soundEnabledPortal
  Ctrl+Alt+F11 -> POI Blocchi SCALE (LADDER)           -> soundEnabledLadder
  Ctrl+Alt+F12 -> POI Blocchi FLUIDI (FLUID)           -> soundEnabledFluid
  Ctrl+Alt+G   -> POI Blocchi GUI/FORZIERI (HAVE_INTERFACE) -> soundEnabledGui   [G=GUI]
  Ctrl+Alt+H   -> POI Entita RADAR MOB OSTILI (HOSTILE)    -> soundEnabledHostile [H=Hostile radar, DISTINTO da F6]
  Ctrl+Alt+P   -> POI Entita ANIMALI PASSIVI (PASSIVE)     -> soundEnabledPassive [P=Passive]

Categorie entita rimanenti (YOUR_PETS, OTHER_PETS, BOSS, PLAYER, VEHICLE, ITEM, DISPLAY):
  Flag config aggiunti per completezza (tutti = true di default), tasto rapido rinviato a revisione futura.

---

## 4. Analisi Architetturale e Contratti

### D0 — KeyboardHandlerMixin
Target: net.minecraft.client.KeyboardHandler — metodo handleDebugKeys(long, int, int, int)
Strategia: @Inject(method = "handleDebugKeys", at = @At("HEAD"), cancellable = true)
Se ModifierUtils.hasControlAndAlt() -> ci.cancel() — termina il metodo vanilla senza side-effect.
Invariante Cancello 1 (Anti-Patching Euristico): nessuna modifica a Kuma, nessun ritardo artificiale.
Invariante Cancello 5 (Determinismo Headless): il Mixin non ha stato; verificato tramite collaudo in-game.

### D1..D2 — Flag soundEnabled in Config
In Config.POI.Blocks (7 campi boolean, tutti true per default):
  soundEnabledOre, soundEnabledFunctional, soundEnabledDoor, soundEnabledPortal,
  soundEnabledLadder, soundEnabledFluid, soundEnabledGui

In Config.POI.Entities (9 campi boolean, tutti true per default):
  soundEnabledHostile, soundEnabledYourPets, soundEnabledOtherPets, soundEnabledBoss,
  soundEnabledPassive, soundEnabledPlayer, soundEnabledVehicle, soundEnabledItem, soundEnabledDisplay
  (solo soundEnabledHostile e soundEnabledPassive hanno tasto rapido in questa revisione)

### D3 — BooleanSupplier in POIGroup
Campo aggiunto: private final BooleanSupplier soundEnabledSupplier
Nuovo costruttore 4°: POIGroup(String nameKey, Sound sound, BooleanSupplier soundEnabledSupplier, Predicate<T> predicate)
Costruttori esistenti: tutti ricevono soundEnabledSupplier = () -> true per retrocompatibilita totale.
In playSoundForGroupItems(): guardia iniziale if (!soundEnabledSupplier.getAsBoolean()) return;

### D4..D5 — Collegamento enum gruppi alla config
Esempio BuiltinBlockPOIGroups:
  ORE(new POIGroup<>(
      "minecraft_access.point_of_interest.group.ore",
      new POIGroup.Sound(SoundEvents.ITEM_PICKUP, -5.0f),
      () -> Config.getInstance().poi.blocks.soundEnabledOre,   // BooleanSupplier
      pos -> Ore.PREDICATE.test(getBlockState(pos).getBlock())
  ))
Stessa struttura per tutte le 7+9 categorie.
Nota: PORTAL e LADDER in BuiltinBlockPOIGroups non hanno Sound nel costruttore attuale
(usano POIGroup(name, predicate) a 2 parametri) — aggiungere il BooleanSupplier richiede
un adattamento del costruttore anche per questi gruppi (mantenendo Sound null).

### D6 — Batteria Kuma F7..F12 + G + H + P
Registrazione in initialize() di POIBlocks per F7..F12 e G:
  Ctrl+Alt+F7  -> toggleSoundOre()
  Ctrl+Alt+F8  -> toggleSoundFunctional()
  Ctrl+Alt+F9  -> toggleSoundDoor()
  Ctrl+Alt+F10 -> toggleSoundPortal()
  Ctrl+Alt+F11 -> toggleSoundLadder()
  Ctrl+Alt+F12 -> toggleSoundFluid()
  Ctrl+Alt+G   -> toggleSoundGui()    (InputConstants.KEY_G)

Registrazione in initialize() di POIEntities per H e P:
  Ctrl+Alt+H -> toggleSoundHostile()  (InputConstants.KEY_H)
  Ctrl+Alt+P -> toggleSoundPassive()  (InputConstants.KEY_P)

Guardie esclusive: hasControlAndAlt() da ModifierUtils — identico agli interruttori F1..F6.
Annuncio vocale: MainClass.narrate("Suono <categoria> ON/OFF", true) per ogni toggle.

---

## 5. Validazione Preventiva sui 7 Assi di Qualita

1. Validita: handleDebugKeys e il punto corretto; stesso pattern di MinecraftMixin.handleKeybinds.
2. Efficacia: ci.cancel() interrompe completamente il metodo vanilla.
3. Coerenza: BooleanSupplier rispetta DIP; Config e sorgente di verita unica.
4. Completezza: 7 flag blocchi tutti con tasto rapido; 2 flag entita prioritarie (HOSTILE, PASSIVE) con tasto rapido; restanti 7 flag entita senza tasto rapido (rinviati).
5. Precisione: Guardia hasControlAndAlt() simmetrica agli interruttori F1..F6.
6. Affidabilita: Tutti i flag inizializzati a true garantiscono zero regressioni.
7. Assenza di Regressioni: () -> true nei costruttori esistenti e retrocompatibile al 100%.

### Matrice di Simulazione a 3 Livelli

Livello 1 — Happy Path:
- Ctrl+Alt+F3: Mixin cancella handleDebugKeys -> Debug Screen NON appare -> buche corto raggio commutato -> annuncio vocale
- Ctrl+Alt+F5: Mixin cancella -> prospettiva NON cambia -> suono mirino commutato -> annuncio vocale
- Ctrl+Alt+F9: suono porte disabilitato -> porte ancora narrate dal mirino -> annuncio "Suono Porte disattivato"
- Ctrl+Alt+H: radar suono HOSTILE disabilitato -> F6 sentinella ravvicinata rimane attiva -> annuncio "Suono radar ostili disattivato"

Livello 2 — Alternative Paths:
- F3 senza modificatori -> hasControlAndAlt() = false -> Mixin non cancella -> Debug Screen normale
- Tutti i suoni categoria disattivati -> scanner continua, narrazione testuale funziona, solo suoni soppressi
- Ctrl+Alt+F6 OFF + Ctrl+Alt+H OFF -> sentinella E radar entrambi disattivati; comportamento coerente
- Config salvata -> stati persistiti tra sessioni (AutoConfig)

Livello 3 — Corner Cases:
- handleDebugKeys signature (long window, int key, int scanCode, int action): @At("HEAD") non dipende dagli argomenti
- Nomi metodo Mojang-mapped: Loom deofusca automaticamente con mappings incluse nel progetto
- PORTAL e LADDER in BuiltinBlockPOIGroups usano costruttore a 2 parametri (senza Sound):
  bisogna aggiungere un costruttore intermedio POIGroup(name, BooleanSupplier, predicate)
  o passare Sound null esplicitamente — da gestire in D3
- Ctrl+Alt+G/H/P: verificare che G, H, P non siano gia usati da altri keymapping del progetto
  (dalla ricerca nel codice: nessuna occorrenza di KEY_G, KEY_H, KEY_P in Kuma binding -> liberi)

---

## 6. File Coinvolti

[NEW]    src/main/java/org/mcaccess/minecraftaccess/mixin/KeyboardHandlerMixin.java
[MODIFY] src/main/resources/minecraft_access.mixins.json  (aggiunta "KeyboardHandlerMixin" in array client)
[MODIFY] src/main/java/org/mcaccess/minecraftaccess/features/point_of_interest/POIGroup.java
[MODIFY] src/main/java/org/mcaccess/minecraftaccess/Config.java
[MODIFY] src/main/java/org/mcaccess/minecraftaccess/features/point_of_interest/BuiltinBlockPOIGroups.java
[MODIFY] src/main/java/org/mcaccess/minecraftaccess/features/point_of_interest/BuiltinEntityPOIGroups.java
[MODIFY] src/main/java/org/mcaccess/minecraftaccess/features/point_of_interest/POIBlocks.java
[MODIFY] src/main/java/org/mcaccess/minecraftaccess/features/point_of_interest/POIEntities.java
[MODIFY] src/main/java/org/mcaccess/minecraftaccess/features/help/QuickKeysHelpScreen.java
[MODIFY] src/main/resources/assets/minecraft_access/lang/it_it.json
[MODIFY] src/main/resources/assets/minecraft_access/lang/en_us.json
[MODIFY] knowledge/06_controlli_avanzati_e_bridge_chatgpt.md

---

## 7. Chiavi I18N Previste (~30 chiavi totali per lingua)

In it_it.json e en_us.json, ordine alfabetico obbligatorio:
  minecraft_access.poi.sound_toggle.blocks.door.off
  minecraft_access.poi.sound_toggle.blocks.door.on
  minecraft_access.poi.sound_toggle.blocks.fluid.off
  minecraft_access.poi.sound_toggle.blocks.fluid.on
  minecraft_access.poi.sound_toggle.blocks.functional.off
  minecraft_access.poi.sound_toggle.blocks.functional.on
  minecraft_access.poi.sound_toggle.blocks.gui.off
  minecraft_access.poi.sound_toggle.blocks.gui.on
  minecraft_access.poi.sound_toggle.blocks.ladder.off
  minecraft_access.poi.sound_toggle.blocks.ladder.on
  minecraft_access.poi.sound_toggle.blocks.ore.off
  minecraft_access.poi.sound_toggle.blocks.ore.on
  minecraft_access.poi.sound_toggle.blocks.portal.off
  minecraft_access.poi.sound_toggle.blocks.portal.on
  minecraft_access.poi.sound_toggle.entities.hostile.off
  minecraft_access.poi.sound_toggle.entities.hostile.on
  minecraft_access.poi.sound_toggle.entities.passive.off
  minecraft_access.poi.sound_toggle.entities.passive.on
  minecraft_access.help.cat_poi_sound_toggles
  minecraft_access.help.desc_poi_sound_toggles
  (+ eventuali chiavi help per singoli tasti)

---

## 8. Piano di Verifica

Build e Test:
  .\gradlew.bat --no-daemon --no-watch-fs test
  .\gradlew.bat --no-daemon --no-watch-fs shadowJar

Deploy: copia JAR in entrambe le istanze PrismLauncher (*26.2*Access*)

Collaudo In-Game (Luca):
  1. Ctrl+Alt+F3 -> Debug Screen NON appare + annuncio vocale buche corto raggio
  2. Ctrl+Alt+F5 -> prospettiva NON cambia + annuncio vocale mirino
  3. Ctrl+Alt+F9 -> suono porte cessa; porte ancora narrate dal mirino
  4. Ctrl+Alt+F9 di nuovo -> suono porte riprende
  5. Ctrl+Alt+H -> suono radar ostili cessa; Ctrl+Alt+F6 sentinella rimane attiva
  6. Ctrl+Alt+P -> suono animali passivi cessa; scanner aggiornato
  7. Tutti gli altri suoni non toccati rimangono invariati

---

Stato: CONFERMATO DA LUCA — PRONTO PER IMPLEMENTAZIONE (SOTTO-FASE 1B)
