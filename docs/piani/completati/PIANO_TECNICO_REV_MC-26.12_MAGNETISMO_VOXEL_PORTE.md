# Piano Tecnico: Rev MC-26.12 — Assist di Interazione Varchi e Magnetismo Voxel per Porte Aperte (Permissive Door Interaction)

- **Autori**: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity (Senior AI Pair Programmer)
- **Ramo Git di Riferimento**: `feat/cognitive-orchestrator`
- **Framework di Governance**: ASTRALIS v3.0.2 (Sotto-Fase 1A — Contratti Formali D1..D4)
- **Stato**: `[COMPLETATO E COLLAUDATO CON SUCCESSO AL 100% IN-GAME DA LUCA]`
- **Data di Redazione**: 2026-09-07
- **Documenti Correlati**:
  * [`docs/report/ARCHIVIO_REVISIONI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/ARCHIVIO_REVISIONI.md)
  * [`docs/report/archivio/REPORT_SESSIONE_REV_MC-26.12_MAGNETISMO_VOXEL_PORTE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_REV_MC-26.12_MAGNETISMO_VOXEL_PORTE.md)
  * [`docs/strategie/archiviate/STRATEGIA_COGNITIVA_REV_MC-26.12_MAGNETISMO_VOXEL_PORTE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/archiviate/STRATEGIA_COGNITIVA_REV_MC-26.12_MAGNETISMO_VOXEL_PORTE.md)

---

## 🎯 Obiettivo Tecnico

Risolvere la discrepanza tra il feedback vocale (micro-voxel raymarch che aggancia e vocalizza l'intero volume del vano porta aperta) e il raycast fisico vanilla (che attraversa l'aria vuota della porta aperta mancando la lamina da 3 pixel dello stipite), permettendo a Luca di chiudere le porte al primo tocco premendo il tasto interazione (`startUseItem`).

---

## 📋 Contratti di Consegna (D1..D4)

### Contratto D1: Helper Puro & Testabile `DoorInteractionHelper.java` [COMPLETATO]
- **Percorso**: `src/main/java/org/mcaccess/minecraftaccess/features/door/DoorInteractionHelper.java`
- **Metodo Headless**:
  `public static boolean isInteractableOpenDoorOrGate(@Nullable BlockState state)`
  * Riconosce `DoorBlock`, `FenceGateBlock`, `TrapDoorBlock` in stato aperto (`OPEN == true`);
  * Esclude tassativamente porte e botole di ferro (`Blocks.IRON_DOOR`, `Blocks.IRON_TRAPDOOR`);
  * Restituisce false per aria o blocchi chiusi.
- **Metodo Client**:
  `public static @Nullable BlockHitResult resolvePermissiveDoorHit(@Nullable Minecraft client)`
  * Se `client`, `player` o `level` sono null: `return null`;
  * Se `client.hitResult` è di tipo `ENTITY`: `return null` (rispetto interazione mob/NPC);
  * Se `client.hitResult` ha già colpito una porta aperta: `return null` (trasparenza vanilla);
  * Calcola reach con `Math.max(player.blockInteractionRange(), player.entityInteractionRange())`;
  * Interroga `PlayerUtils.crosshairTarget(reach)`;
  * Se `crosshairTarget` intercetta un blocco porta/varco aperto interagibile: restituisce il `BlockHitResult`;
  * Altrimenti `return null`.

### Contratto D2: Mixin su `Minecraft.startUseItem` [COMPLETATO]
- **Percorso**: `src/main/java/org/mcaccess/minecraftaccess/mixin/MinecraftMixin.java`
- **Iniezione**:
  `@Shadow public HitResult hitResult;`
  `@Inject(method = "startUseItem", at = @At("HEAD"))`
- **Comportamento**:
  Se `DoorInteractionHelper.resolvePermissiveDoorHit((Minecraft)(Object)this)` restituisce un hit non nullo, assegna temporaneamente `this.hitResult = permissiveHit`. Minecraft Vanilla procede eseguendo `useItemOn` sul blocco porta.

### Contratto D3: Suite di Test Unitari Headless [COMPLETATO]
- **Percorso**: `src/test/java/org/mcaccess/minecraftaccess/features/door/DoorInteractionHelperTest.java`
- **Copertura**:
  1. Porte in legno aperte vs chiuse;
  2. Porte di ferro aperte e chiuse (escluse);
  3. Cancelletti aperti vs chiusi;
  4. Botole in legno aperte vs chiuse;
  5. Botole di ferro aperte e chiuse (escluse);
  6. Blocchi generici (pietra, aria, terra);
  7. Bypass se `vanillaHit` è entità o se mira già alla porta.
  Tutti i 7 test verdi (suite a 315/315).

### Contratto D4: Compilazione & Deploy nelle Istanze PrismLauncher [COMPLETATO]
- **Verifica**: Compilazione con `.\gradlew.bat --no-daemon shadowJar`.
- **Deploy**: Copia del jar nelle cartelle mods di:
  * `Minecraft 26.2 Access 1.12.0`
  * `Minecraft 26.2 Access - Server Tenuta`
