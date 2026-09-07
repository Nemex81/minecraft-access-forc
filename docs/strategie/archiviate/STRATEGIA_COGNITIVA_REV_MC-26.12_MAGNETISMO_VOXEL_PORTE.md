# Strategia Logico-Cognitiva: Rev MC-26.12 — Assist di Interazione Varchi e Magnetismo Voxel per Porte Aperte (Permissive Door Interaction)

- **Autori**: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity (Senior AI Pair Programmer)
- **Ramo Git di Riferimento**: `feat/cognitive-orchestrator`
- **Framework di Governance**: ASTRALIS v3.0.2 (Fase 0 — UPCS Unified Progressive Cognitive Strategy)
- **Stato**: `[STRATEGIA ATTUATA E COLLAUDATA CON SUCCESSO AL 100% IN-GAME DA LUCA]`
- **Data di Redazione**: 2026-09-07
- **Documenti Correlati**:
  * [`docs/report/ARCHIVIO_REVISIONI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/ARCHIVIO_REVISIONI.md)
  * [`docs/report/archivio/REPORT_SESSIONE_REV_MC-26.12_MAGNETISMO_VOXEL_PORTE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_REV_MC-26.12_MAGNETISMO_VOXEL_PORTE.md)
  * [`docs/piani/completati/PIANO_TECNICO_REV_MC-26.12_MAGNETISMO_VOXEL_PORTE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_REV_MC-26.12_MAGNETISMO_VOXEL_PORTE.md)
  * [`docs/strategie/attive/STRATEGIA_COGNITIVA_GESTIONE_PORTE_E_VARCHI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/attive/STRATEGIA_COGNITIVA_GESTIONE_PORTE_E_VARCHI.md)

---

## 🧭 1. Visione Cognitiva & Obiettivo di Accessibilità (Screen Reader NVDA)

Nel gameplay di Minecraft per giocatori non vedenti, l'interazione con le porte aperte presenta una severa discrepanza tra la percezione vocale e l'azione fisica del mouse:

1. **Il Paradosso Voce vs Interazione Fisica**:
   - Grazie al *Micro-Voxel Raymarch Snap* implementato in `PlayerUtils.java:L121-L145`, il mirino accessibile campiona lo spazio ogni 10 cm e riconosce l'intero volume cubico del blocco dove risiede la porta, vocalizzando regolarmente *"Porta aperta di abete"*;
   - Tuttavia, al momento della pressione del tasto di interazione (`startUseItem`), Minecraft Vanilla utilizza il suo raycast standard basato sulla `VoxelShape` fisica;
   - Quando una porta è aperta, il battente ruota di 90° e si adagia allo stipite, riducendosi a una lamina di **soli 3 pixel di spessore** ($0.1875\text{ m}$). Il restante $81\%$ del blocco è considerata aria pura;
   - Il raycast di Vanilla attraversa l'aria vuota e intercetta il blocco di sfondo o fa `MISS`. Di conseguenza, il tasto destro non ha effetto sulla porta e il giocatore è costretto a una frustrante ricerca millimetrica dello stipite.

2. **Obiettivo della Revisione Raggiunto**:
   - Allineare l'interazione fisica al feedback vocale: se lo screen reader annuncia che il giocatore sta inquadrando la porta aperta, la pressione del tasto interazione chiude la porta al primo colpo, con tolleranza permissiva su tutto il volume del varco.

---

## 🏛️ 2. Architettura della Soluzione (Magnetismo Voxel d'Interazione)

La soluzione si articola in due componenti sinergici:

1. **Helper Puro e Testabile (`DoorInteractionHelper.java`)**:
   - `resolvePermissiveDoorHit(Minecraft client)`:
     * Verifica preliminare: se `client.hitResult` ha già colpito direttamente la porta, restituisce `null` (nessun override necessario);
     * Altrimenti, invoca `PlayerUtils.crosshairTarget(range)` calcolato sul raggio d'interazione massimo (`player.blockInteractionRange()`);
     * Se il mirino accessibile intercetta un blocco `DoorBlock`, `FenceGateBlock` o `TrapDoorBlock` in stato aperto (`OPEN == true`) e non è una porta di ferro (`!IronDoorBlock`), restituisce il `BlockHitResult` permissivo calcolato sulla porta;
     * Se non vi sono porte aperte o la porta è già chiusa, restituisce `null`.

2. **Iniezione a Monte in `MinecraftMixin.java` (`startUseItem`)**:
   - Nel metodo `private void startUseItem()` di `Minecraft.class`:
     * Con `@Inject(method = "startUseItem", at = @At("HEAD"))`;
     * Se `DoorInteractionHelper.resolvePermissiveDoorHit((Minecraft) (Object) this)` restituisce un `BlockHitResult`:
     * Aggiorna atomisticamente `this.hitResult = permissiveHit`;
     * L'esecuzione nativa di Minecraft prosegue con `this.hitResult` correttamente puntato sulla porta, invocando `this.gameMode.useItemOn(...)` e chiudendo la porta istantaneamente.

---

## 🛡️ 3. I 4 Cancelli Inviolabili di Sicurezza

1. **Cancello 1 — Trasparenza per Porte già Mirate**:
   - Se il cursore del giocatore colpisce già con precisione lo stipite fisico della porta, il sistema non altera `hitResult`, delegando l'azione al flusso nativo.
2. **Cancello 2 — Esclusione Porte di Ferro Meccaniche**:
   - `IronDoorBlock` viene rigorosamente ignorato: le porte di ferro non rispondono al tasto destro e richiedono circuiti redstone.
3. **Cancello 3 — Range di Reach Rigoroso**:
   - Il calcolo permissivo rispetta incondizionatamente il raggio massimo di interazione del giocatore (`player.blockInteractionRange()`), impedendo azionamenti remoti o "a distanza di cheat".
4. **Cancello 4 — Determinismo Headless**:
   - `DoorInteractionHelper` è interamente testato con test unitari headless a 0 ms simulando blockstate aperti/chiusi e hit results (7/7 test verdi).
