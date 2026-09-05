# Rapporto di Stato del Sistema & Handoff Anomalie GUI
- **Autore:** Luca (Senior Developer), Antigravity & ChatGPT
- **Revisori:** Luca
- **Data e Ora:** 2026-09-04 — 02:20 CEST
- **Stato dell'Implementazione:** [COMPLETATO E ARCHIVIATO — HANDOFF CONFERMATO]
- **Obiettivo/i:**
  * Fotografia dello stato del sistema cognitivo al completamento delle prime 4 fasi
  * Diagnosi delle anomalie nell'apertura dei menu Cloth Config e Waypoint GUI
  * Definizione della matrice dei test logici di non-regressione
- **Piani & Strategie Correlate:**
  * [`REGISTRO_REVISIONI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/REGISTRO_REVISIONI.md)
- **Breve Descrizione:** Riepilogo dello stato di sincronizzazione del framework ASTRALIS v2.6.1 e handoff operativo per la risoluzione dei difetti di interfaccia.

---
## 🧭 1. STATO DEL SISTEMA COGNITIVO CENTRALE

L'epica del **Cognitive Orchestrator** ha completato con successo le prime tre macro-fasi, con stabilità certificata su 185 test headless e oltre 1h di collaudo in-game continuo:
- **Fase 1 (Architettura Base & Cognitive Coordinator)**: Eventi immutabili, 4 priorità gerarchiche (`CRITICAL`, `OPERATIONAL`, `CONTEXTUAL`, `PASSIVE`) e bus atomico con seam disaccoppiate.
- **Fase 2 (Dominio 1 — Mirino & Orientamento)**: Migrazione `CrosshairFeedbackManager` e pattern *Token Composition*.
- **Fase 3 (Dominio 2 — Sicurezza Voxel & Movimento)**:
  - 3A `FallDetector` con `SafetyMovementGuard`, 2 zone anticaduta e blocco salto su ciglio;
  - 3B `ObstacleDetector` con `ObstacleNarrationComposer`, parità legacy e pattern *"Silent Commit"*.
- **Fase 4 Macro-Piano (Prossimi Canali Percettivi)**: Sospesa temporaneamente per consentire la bonifica prioritaria e chirurgica delle due anomalie GUI aperte.

---

## 🔍 2. DIAGNOSI E STRATEGIA CONSOLIDATA DELLE ANOMALIE GUI

---

### A. Rev MC-26.9 — NullPointer & Anti-Ghost Narration su `currentScreen`
- **Problema Logico**: Se una schermata viene chiusa (es. con `Esc`) mentre un evento di navigazione slot o un delay del mouse è in transito, `currentScreen` diventa `null`, generando eccezioni nei metodi del mouse e possibili narrazioni vocali di elementi ormai distrutti.
- **Strategia Logica Consolidata (Dual Guard)**:
  1. *Guard a Monte*: Gli handler Kuma e i metodi di navigazione della griglia (`changeGroup`, `selectGroup`, `focusSlotItem`, `focusSlotItemAt`) verificano preventivamente `currentScreen != null`; se lo schermo è chiuso, scartano l'evento senza mutare lo slot né avviare narrazioni obsolete.
  2. *Guard a Valle*: I metodi `moveToSlotItem` mantengono il controllo difensivo su `currentScreen == null` per azzerare calcoli mouse su accessor inesistente.

---

### B. Rev MC-26.10 — Soppressione Accovacciamento Non Intenzionale (`Shift Hijack`) in GUI
- **Problema Logico**: Dentro le schermate GUI, `FallDetector` invocava il reset ad ogni tick; `SafetyMovementGuard` rileggeva il tasto Shift fisico da GLFW e lo imponeva all'entità giocatore, facendo accovacciare il personaggio nel mondo quando l'utente premeva `Shift` per scorciatoie GUI (`Shift+C`, `Shift+K`, `Shift+V`) o quick-move.
- **Strategia Logica Consolidata (Separazione Rigorosa di Responsabilità)**:
  1. *`RawCrouchIntentProvider` (Invariato)*: Rimane un probe hardware puro verso GLFW (Single Responsibility Principle). Non deve contenere logiche di interfaccia.
  2. *`SafetyMovementGuard.suspendForGui()`*: Nuovo metodo esplicito che revoca i token di sicurezza e rilascia l'accovacciamento sintetico **solo se immediatamente prima era attivo il token di sistema (`systemOverrideActive`)**. Non tocca la postura se era manuale e non legge GLFW durante la GUI.
  3. *`FallDetector`*: Su `client.gui.screen() != null`, delega a `suspendForGui()` e termina il tick senza riconciliazioni.
  4. *Ripresa Naturale*: Alla chiusura della GUI, il tick successivo riprende il normale ciclo leggendo Shift solo se ancora tenuto premuto nel mondo.

---

## 🧪 3. MATRICE DEI TEST LOGICI DI CERTIFICAZIONE

La correzione sarà validata tramite 5 test mirati in `SafetyMovementGuardTest` e `InventoryControlsTest`:
1. **GUI con Shift manuale attivo**: nessuna scrittura a port (la postura manuale non viene toccata).
2. **GUI con token anticaduta attivo**: esattamente una scrittura `false` (rilascio pulito del solo token di sistema).
3. **Shift premuto dentro la GUI**: nessuna scrittura `true` al port.
4. **Uscita da GUI con Shift premuto**: esattamente un `true` al primo tick nel mondo (ripresa naturale).
5. **Chiusura GUI concorrente**: zero eccezioni, zero coordinate mouse invalide, zero narrazioni fantasma.
6. **Non-regressione Voxel**: suite completa dei 185 test headless verdi.

---

## 🚀 4. PROSSIMO PASSO OPERATIVO

Questo rapporto costituisce la base logica per redigere il **Piano Tecnico Correttivo Formale (Sotto-Fase 1A)** in `docs/piani/attivi/` ed eseguire l'implementazione chirurgica (Sotto-Fase 1B) nella nuova chat.
