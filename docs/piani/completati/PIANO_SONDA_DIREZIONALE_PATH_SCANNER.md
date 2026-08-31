# Piano Tecnico Completato — Sonda Direzionale di Percorso (DirectionalPathScanner)

Modulo **Sonda Direzionale di Percorso** per Minecraft Access 1.12.0 (Minecraft 26.2).  
Completato e Convalidato In-Game con successo il 31 Agosto 2026.

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
