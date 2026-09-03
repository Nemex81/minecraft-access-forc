# Registro Attivo delle Revisioni & Affinamenti Post-Collaudo (RRU)
# Progetto: Minecraft Access (Fork 26.2 / 1.21.x)
# Autore: Luca (Sviluppatore & Collaudatore) & Antigravity (AI Pair Programmer)
# Percorso: docs/report/REGISTRO_REVISIONI.md
# Archivio Storico: docs/report/ARCHIVIO_REVISIONI.md

Questo documento costituisce il **Registro Attivo Snello** del progetto Minecraft Access. Ospita *esclusivamente* le revisioni aperte o in lavorazione. A collaudo positivo confermato da Luca, le voci vengono migrate nell'**Archivio Storico delle Revisioni** (`docs/report/ARCHIVIO_REVISIONI.md`), mantenendo questo file sempre leggero e rapido da consultare con NVDA.

---

## 📋 REVISIONI ATTIVE IN CORSO

---

### 🔵 Rev MC-26.7 — Resilienza & Fallback Traduzioni per Blocchi di Mod Terze (es. Macaw's Doors)
- **Stato**: `[APERTA]`
- **Data Rilevamento**: 2026-09-01
- **Problema Riscontrato (Esperienza Luca)**: In presenza di mod terze (es. Macaw's Doors) prive di localizzazione italiana, il mirino o il raycast vocalizzano la chiave grezza (es. *"Ostacolo di block.mcwdoors.dark_oak_barn_door a 6 blocchi"*).
- **Evidenza Telemetrica / Log**: `Narrating=block.mcwdoors.dark_oak_barn_door`.
- **Causa Radice**: La chiave non ha traduzione in `it_it.json` e il sistema vanilla restituisce la chiave non tradotta.
- **Soluzione di Affinamento (PRAPI)**:
  1. Fallback su lingua inglese (`en_us`) in `ObstacleDetectionUtils` / `WorldNarrator` quando la stringa inizia con `block.` o manca in italiano;
  2. Formattazione leggibile dall'identificatore del blocco (es. estrazione di *"dark oak barn door"* dalla chiave);
  3. Override di dizionario per le mod del modpack ufficiale in `minecraft_access/lang/it_it.json`.
- **Piano Tecnico di Riferimento**: In fase di pianificazione.
- **Esito Collaudo**: In attesa di lavorazione e collaudo.

---

### 🔵 Rev MC-26.8 — Discesa Sicura su Scale a Pioli ed Elementi Arrampicabili (Climbable Bypass in FallDetector)
- **Stato**: `[APERTA]`
- **Data Rilevamento**: 2026-09-03
- **Problema Riscontrato (Esperienza Luca)**: Salendo sul tetto tramite scala a pioli, l'utente non riesce più a scendere: `FallDetector` classifica il vuoto attorno alla scala come burrone letale (`profondità 4 blocchi`), attiva lo sticky-sneak sul ciglio e l'auto-sneak forzato, bloccando fisicamente il giocatore e costringendolo a disattivare la protezione anticaduta (`Ctrl + Alt + F`) per poter scendere la scala.
- **Evidenza Telemetrica / Log**: `[15:43:35] Narrating(interrupt:true)= Sul ciglio: burrone 1 blocchi in basso , profondità 4 blocchi`, `[15:44:33] Narrating(interrupt:true)= Attenzione: burrone 1 blocchi avanti 1 blocchi in basso , profondità 3 blocchi`.
- **Causa Radice**: 
  1. `isStandingOnDangerousEdge` campiona radialmente 8 punti attorno alla hitbox: i campioni laterali/diagonali rilevano aria e vuoto oltre il perimetro del tetto (dove non c'è la scala), forzando lo sticky-sneak anche se davanti c'è una colonna di discesa sicura;
  2. Il motore fisico nativo di Minecraft impedisce a un giocatore accovacciato (Shift attivo) di scendere da un blocco solido;
  3. Il raycast di look-ahead non riconosce la scala a pioli attaccata alla parete o a quota piedi/sottostante quando la traiettoria punta deliberatamente alla scala.
- **Soluzione Proposta (PRAPI / Protocollo 5)**:
  1. Estendere il riconoscimento degli elementi di discesa sicura a tutti i blocchi arrampicabili: scale a pioli (`LadderBlock`), liane (`VineBlock`, `WeepingVinesBlock`, `TwistingVinesBlock`, `CaveVinesBlock`), impalcature (`ScaffoldingBlock`), botole aperte sopra scale (`TrapDoorBlock`) e tag `#minecraft:climbable`;
  2. Quando il giocatore punta e si muove deliberatamente verso una colonna discendente sicura, sospendere temporaneamente l'auto-sneak forzato (`keyShift.setDown(false)`), consentendo l'aggancio fisico alla scala;
  3. Escludere la colonna della scala dalla segnalazione di burrone e fornire riscontro acustico/vocale positivo di discesa sicura anziché allarme di caduta.
- **Piano Tecnico di Riferimento**: In fase di consultazione e pianificazione.
- **Esito Collaudo**: Segnalata in-game durante il collaudo della Fase 3A.

---
