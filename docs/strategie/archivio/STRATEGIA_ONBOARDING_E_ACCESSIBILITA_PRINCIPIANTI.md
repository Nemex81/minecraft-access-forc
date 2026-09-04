# Strategia di Onboarding e Accessibilità per Giocatori Non Vedenti Inesperti
# Modulo: Minecraft Access (Fork Luca)
# Percorso: docs/strategie/STRATEGIA_ONBOARDING_E_ACCESSIBILITA_PRINCIPIANTI.md

Questo documento definisce le linee guida pedagogiche, l'architettura tecnica e i principi di design non visivo per ridurre drasticamente la curva di apprendimento di Minecraft per giocatori non vedenti senza esperienza pregressa in ambienti 3D.

---

## 🎯 1. Visione Pedagogica e Principi Guida

### A. Il Principio di De-Cluttering Sensoriale (Pulizia Acustica Assoluta)
Nei giochi 3D per non vedenti, l'eccesso di segnali audio simultanei (passi, mirino sonoro continuo, radar di prossimità, allarmi ostacoli, ronzii di bussola) genera una rapida fatica uditiva (*auditory fatigue*) e confusione cognitiva.
- **Regola Strategica**: Nessun segnale acustico continuo o astratto di fondo.
- **Preferenza Vocale Semantica**: La voce sintetica dello screen reader (NVDA / Tolk), emessa al momento giusto con istruzioni chiare e contestualizzate, è infinitamente più rassicurante e comprensibile di qualsiasi codice sonoro complesso.
- **Audio Ducking Integrato**: Quando il sistema di aiuto parla, il volume degli effetti ambientali e dei mostri di Minecraft viene attenuato automaticamente del 40% per massimizzare la leggibilità della voce.

### B. Il Principio di Divulgazione Progressiva (Progressive Disclosure)
La mod `minecraft-access` dispone di oltre 80 comandi. Presentarli tutti insieme genera paralisi.
- **Regola Strategica**: All'inizio vengono abilitati e spiegati solo i 5-6 comandi primari. I comandi avanzati (es. mira balistica per l'arco, aggancio continuo Y, filtri POI avanzati) vengono introdotti solo quando la situazione di gioco lo richiede.

### C. La Doppia Dimensione Didattica: Strumento di Accessibilità + Meccanica Vanilla
L'apprendimento non riguarda solo Minecraft in sé, ma il modo in cui Minecraft si gioca attraverso la mod:
- Non si insegna solo "trova un albero", si insegna *"premi End per orientare il mirino sul tronco più vicino"*.
- Non si insegna solo "rompi il blocco", si insegna *"tieni premuto il tasto attacco (è della tastiera o 0 del tastierino) fino al suono di rottura"*.

---

## 🏛️ 2. I 7 Pilastri Architetturali del Sistema

```
+-------------------------------------------------------------------+
|                     PLAYER CONTEXT ENGINE                         |
|   (Snapshot a 5 assi: Spaziale, Mirino, Inventario, Tempo, Azioni)|
+---------------------------------+---------------------------------+
                                  |
            +---------------------+---------------------+
            |                                           |
+-----------v-----------+                   +-----------v-----------+
|  MENTOR CONTESTUALE   |                   |  ACCADEMIA A MISSIONI |
|  (In-Game Advisor)    |                   |  (State Machine FSM)  |
|  - Consigli reattivi  |                   |  - Avanzamento auto   |
|  - Debounce anti-spam |                   |  - Guard Rail GameMode|
|  - Riconosce GameMode |                   |  - Percorsi IT/EN     |
+-----------+-----------+                   +-----------+-----------+
            |                                           |
            +---------------------+---------------------+
                                  |
+---------------------------------v---------------------------------+
|               HELP NARRATOR & PRIORITÀ ASSOLUTA                   |
|   - Scudo temporale dinamico anti-troncamento                     |
|   - Silenziamento temporaneo scanner mirino ed ostacoli           |
|   - Opzione configurabile 'helpPriorityOverride'                  |
+-------------------------------------------------------------------+
```

### Pilastro 1: Player Context Engine (Osservatore di Stato a 5 Assi)
Raccoglie ogni 250-500 ms una fotografia fedele dello stato del giocatore:
1. *Asse Spaziale/Motorio*: $(X,Y,Z)$, bioma, livello luce, inerzia reale (rilevamento blocco contro ostacoli).
2. *Asse Sguardo/Mirino*: `HitResult` del blocco o entità puntata, distanza esatta.
3. *Asse Inventario/Equipaggiamento*: Oggetti impugnati, conteggio risorse chiave (legno, assi, torce, cibo, banco), schermata GUI aperta.
4. *Asse Temporale/Ambientale*: Ora del mondo (`timeOfDay % 24000`), meteo, mob ostili nel raggio di prossimità.
5. *Asse Comportamentale*: Tick di inattività da tastiera, cronologia delle ultime azioni compiute.

### Pilastro 2: Adattività & Guard Rail per Modalità di Gioco (`GameMode`)
Il sistema interroga istantaneamente `client.gameMode.getPlayerMode()`:
- **Sopravvivenza (Survival) & Avventura**: Percorso completo focalizzato su fame, ciclo giorno/notte, raccolta risorse e difesa.
- **Creativa (Creative)**: Percorso costruttore. Disattivazione totale di avvisi fame, mostri e durabilità; focus su volo (`Spazio`/`Shift`), posizionamento blocchi e catalogo infinito.
- **Guard Rail di Incompatibilità Assoluta**:
  - È fisicamente impossibile avviare missioni di volo/creativa in modalità Sopravvivenza.
  - Nell'Hub dell'Accademia (`F4`), i pulsanti incompatibili vengono disabilitati (`button.active = false`) ed etichettati vocalmente per lo screen reader: *"Volo e Quota - [Richiede Modalità Creativa] (Disattivato)"*.
  - Nel motore `AcademyManager.startMission`, ogni chiamata non compatibile viene intercettata e spiegata vocalmente con messaggio dedicato.

### Pilastro 3: Modulo 1 — Il "Mentor Vocale Contestuale"
Un osservatore discreto che interviene solo in presenza di eventi salienti:
- **Temporale**: Avviso tramonto (ora 11500), notte fonda, alba.
- **Comportamentale**: Suggerimento riallineamento (`M` o `5` del tastierino, oppure `End`) dopo 25 secondi di fermo totale o blocco continuo contro un muro.
- **Progressione**: Guida al primo legno raccolto, fame critica con cibo disponibile, buio totale con torce in borsa.
- **Anti-Spam**: Registro persistente `deliveredHints` per erogare ogni consiglio una sola volta per mondo/sessione.

### Pilastro 4: Modulo 2 — L'Accademia a Mini-Missioni & Avanzamento Automatico
Macchina a stati finiti interattiva con predicati formali (`Predicate<PlayerContextSnapshot>`):
- **Avanzamento Automatico Sequenziale (`autoAdvanceMissions = true`)**:
  - Al completamento di ogni missione, il sistema riproduce il rintocco di successo, vocalizza la vittoria, attende 4.5 secondi e avvia automaticamente la missione successiva non completata coerente con la GameMode.
  - Al termine di tutte le missioni del percorso, vocalizza le congratulazioni finali.
- **Percorso Sopravvivenza (5 Missioni)**:
  1. *Piedi e Sguardo*: Movimento WASD, centratura orizzonte con `M` (o `5` del tastierino).
  2. *Vedere con le Orecchie*: Tracciamento POI del primo albero con `End` e avvicinamento a $d \le 3.5\text{ m}$.
  3. *Interazione e Raccolta*: Distruzione del tronco con attacco continuo (`è` o `0` del tastierino) e raccolta nell'inventario.
  4. *Crafting Accessibile*: Creazione assi con `E`, creazione Banco da Lavoro.
  5. *Sicurezza e Rifugio*: Posa torcia o illuminazione dell'area per proteggersi dai mostri.
- **Percorso Creativo (2 Missioni)**:
  1. *Volo e Quota*: Decollo con doppio Spazio, salita e discesa.
  2. *Allineamento e Costruzione*: Puntamento blocco solido e posizionamento.

### Pilastro 5: Gerarchia Vocale & Priorità Assoluta con Scudo Temporale
Istruzioni e consigli non devono mai essere troncati da scanner di sfondo:
- **Scudo Dinamico**: $\text{Durata (ms)} = (\text{Parole} \times 280\text{ ms}) + 600\text{ ms}$.
- **Silenziamento Scanner**: Durante lo scudo, `NarrateCrosshair` e `ObstacleDetector` ordinari non emettono voce.
- **Prevenzione Conflitti**: Solo danni critici e pericoli mortali possono scavalcare un'istruzione didattica.
- **Toggle Config**: Opzione `helpPriorityOverride: true/false` modificabile dall'utente.

### Pilastro 6: Ergonomia di Avvio & Controllo Permanente
- **Primo Avvio Assoluto**: Attesa di 1,5 secondi post-spawn -> Dialogo a 3 scelte: Accademia guidata, Mentor libero o Aiuti disattivati -> Selezione rapida profilo tastiera (Desktop con Numpad vs Portatile senza Numpad).
- **Controllo Permanente**:
  - Voce **"Accademia e Mentor"** accessibile direttamente da `F4` (Access Menu).
  - Tasto **`F1`** per la Guida Rapida Comandi.
  - Toggle rapidi per Mentor e Avanzamento Automatico direttamente nella schermata dell'Hub.

---

## 📈 3. Metriche di Successo e Validazione
1. *Tempo di Primo Crafting*: Riduzione del tempo medio per creare il primo piccone da 25+ minuti a meno di 5 minuti per un non vedente assoluto.
2. *Indice di Disorientamento*: Riduzione a zero dei casi di blocco contro muri o smarrimento all'aperto di notte.
3. *Trasparenza Cognitiva*: Chiarezza al 100% di ogni tasto suggerito, perfettamente aderente alla tastiera e al layout in uso.
