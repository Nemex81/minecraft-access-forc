# Piano Tecnico Completato — Sonda Direzionale di Percorso (DirectionalPathScanner)
- **Tipologia:** ESPLORATIVO
- **Autore:** Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity
- **Revisori:** Luca / Antigravity / GPT Codex
- **Data e Ora:** 2026-09-03
- **Stato Operativo:** [COMPLETATO E ARCHIVIATO — CONVALIDATO AL 100% DA LUCA IN-GAME]
- **Incremento Versione Target (AVF):** [Tracciato nel Diario Modifiche Fork]
- **Piani & Documenti Correlati:**
  * [`docs/report/REGISTRO_REVISIONI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/REGISTRO_REVISIONI.md)
- **Conformità ai 5 Cancelli (Protocollo 12):** Conforme al 100% (Rifiuto patching euristico, Hardware grounding, Hitbox continua, Contratti denominati, Determinismo headless a 0 ms)

---
## 🗺️ Sommario Operativo & Registro di Avanzamento (Checklist con Gating di Convalida)

> **Regola Aurea di Avanzamento (Matrice a 3 Stati)**:
> - `- [ ] [DA AVVIARE]`: Attività pianificata ma non ancora iniziata.
> - `- [/] [IMPLEMENTATO — IN ATTESA DI CONVALIDA]`: Codice scritto o intervento completato, ma in attesa di test o collaudo formale (spunta parziale).
> - `- [x] [CONVALIDATO CON SUCCESSO]`: Spunta definitiva concessa **esclusivamente POST-CONVALIDA** (approvazione di Luca per la 1A, test suite 100% verde per la 1B, collaudo pratico in-game di Luca per la Fase 2).

- [x] **🎯 Obiettivo Raggiunto** [CONVALIDATO CON SUCCESSO]
- [x] **🎮 Controlli & Mappatura Tasti Definitiva** [CONVALIDATO CON SUCCESSO]
- [x] **⚙️ I 3 Livelli di Verbosità** [CONVALIDATO CON SUCCESSO]

---
## 🎯 Obiettivo Raggiunto

Fornire al giocatore non vedente una **"Sonda Virtuale di Cammino" (Path Look-Ahead Probe)** per esplorare in anticipo una direttrice (Nord, Sud, Est, Ovest, Diagonali, Avanti nello sguardo, Dietro alle spalle) fino a $X$ blocchi di distanza (fino a 32 blocchi).
Il sistema rileva:
- Calpestabilità e tipo di blocco del pavimento/terreno sotto i piedi (Erba, Pietra, Sabbia, Legno, Terra zappata, ecc.).
- Variazioni di quota ($\Delta Y$), gradini saltabili $\le 1.20\text{ m}$ e burroni/vuoto.
- Ostacoli solidi (muri, recinzioni, tronchi) e gap a fessura stretta (corner pinching a 45°).
- Risorse a terra (`ItemEntity` droppati).
- **Colture e Ortaggi piantati** (`CropBlock`: Grano, Carote, Patate, Barbabietole; `SweetBerryBushBlock`, Angurie, Zucche, Cacao, Canne da zucchero).
- Mob pacifici, neutrali e mostri ostili.
- Fluidi pericolosi (Lava e Acqua).

---

## 🎮 Controlli & Mappatura Tasti Definitiva

### A. Tastierino Numerico (`Ctrl + Alt + Numpad` — Layer 3)
- `Ctrl + Alt + 8` $\rightarrow$ Scansione **Nord**
- `Ctrl + Alt + 2` $\rightarrow$ Scansione **Sud**
- `Ctrl + Alt + 4` $\rightarrow$ Scansione **Ovest**
- `Ctrl + Alt + 6` $\rightarrow$ Scansione **Est**
- `Ctrl + Alt + 7` / `9` / `1` / `3` $\rightarrow$ Scansioni **Diagonali** (NO, NE, SO, SE)
- `Ctrl + Alt + 5` $\rightarrow$ Scansione **Avanti** (direzione dello sguardo corrente)
- `Ctrl + Alt + 0` $\rightarrow$ Scansione **Dietro** (alle spalle)

### B. Tastiera Estesa (`Ctrl + Alt + Frecce`)
- `Ctrl + Alt + Freccia Su` $\rightarrow$ Scansione **Avanti**
- `Ctrl + Alt + Freccia Giù` $\rightarrow$ Scansione **Dietro**
- `Ctrl + Alt + Freccia Sinistra` $\rightarrow$ Scansione **Sinistra**
- `Ctrl + Alt + Freccia Destra` $\rightarrow$ Scansione **Destra**

---

## ⚙️ I 3 Livelli di Verbosità

1. **`SUMMARY_ONLY`**: Sintesi istantanea del primo ostacolo o percorso libero.
2. **`COMPACT`**: Primo tratto calpestabile sicuro, terreno, risorse/mob incontrati e primo ostacolo di arresto.
3. **`DETAILED`**: Scansione a pieno raggio (fino a 32 blocchi, non si arresta) con mappa completa di tratti, quote, ostacoli e ortaggi.
