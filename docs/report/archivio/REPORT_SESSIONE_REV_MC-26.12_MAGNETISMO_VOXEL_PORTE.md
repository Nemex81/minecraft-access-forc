# Report di Sessione: Rev MC-26.12 — Assist di Interazione Varchi e Magnetismo Voxel per Porte Aperte

- **Autori**: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity (Senior AI Pair Programmer)
- **Ramo Git**: `feat/cognitive-orchestrator`
- **Data Sessione**: 2026-09-07
- **Framework di Riferimento**: ASTRALIS v3.0.2 (Protocollo 4, 5 & RRU)
- **Esito Collaudo**: `[COLLAUDATO CON SUCCESSO AL 100% IN-GAME DA LUCA]`

---

## 🎯 1. Obiettivo & Causa Radice

Nel gameplay con screen reader NVDA, il mirino accessibile campiona lo spazio tramite *Micro-Voxel Raymarch Snap* su elementi sottili (`PlayerUtils.java`), annunciando la presenza e lo stato della porta (es. *"Porta aperta di abete"* o *"Cancelletto aperto"*).
Tuttavia, all'atto pratico del click di interazione (`startUseItem`), Minecraft Vanilla utilizza il raycast fisico nativo calcolato sulle `VoxelShape` reali. A porta aperta, il battente ruotato occupa una lamina di soli 3 pixel ($0.1875\text{ m}$), mentre l'$81\%$ del blocco è aria pura. Il raycast nativo attraversava l'aria vuota colpendo il pavimento o il muro posteriore, rendendo la chiusura a mano estremamente difficoltosa e frustrante.

---

## 🛠️ 2. Architettura & File Modificati

1. **`src/main/java/org/mcaccess/minecraftaccess/features/door/DoorInteractionHelper.java` [NUOVO]**:
   - `isInteractableOpenDoorOrGate(BlockState)`: metodo puro headless per validare se il blocco è una porta, cancelletto o botola aperta azionabile a mano (escludendo categoricamente ferro `Blocks.IRON_DOOR` e `Blocks.IRON_TRAPDOOR`);
   - `resolvePermissiveDoorHit(Minecraft)`: se il giocatore inquadra il vano porta aperto entro il raggio di interazione e Vanilla non sta già puntando a un'entità o direttamente allo stipite, restituisce il `BlockHitResult` permissivo della porta.
2. **`src/main/java/org/mcaccess/minecraftaccess/mixin/MinecraftMixin.java` [MODIFICATO]**:
   - Iniezione `@Inject(method = "startUseItem", at = @At("HEAD"))` che assegna temporaneamente `this.hitResult = permissiveHit`. Il motore Vanilla esegue `useItemOn` chiudendo la porta al primo tocco da mouse fisico, tasto `]` o `Invio` del Numpad.
3. **`src/test/java/org/mcaccess/minecraftaccess/features/door/DoorInteractionHelperTest.java` [NUOVO]**:
   - 7 test headless completi (legno vs ferro, porte, cancelli, botole, blocchi generici, bypass entità).

---

## 🧪 3. Metriche di Verifica & Collaudo

- **Test Automatici**: `315/315` test passati (0 failures, 0 skipped, tempo suite 13.05s).
- **Compilazione**: `BUILD SUCCESSFUL in 48s` (jar `minecraft-access-1.12.0-SNAPSHOT.jar`).
- **Deploy Automatico**: distribuito nelle istanze PrismLauncher:
  - `Minecraft 26.2 Access 1.12.0`
  - `Minecraft 26.2 Access - Server Tenuta`
- **Collaudo In-Game**: Eseguito da Luca sul campo: chiusura istantanea della porta aperta al primo tocco puntando verso il vano d'aria, senza alcuna necessità di centrare lo stipite. Esito positivo al 100%.
