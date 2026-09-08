# Strategia Logico-Cognitiva (UPCS) — Migrazione Integrale dei Domini Legacy Residui nel Coordinatore Cognitivo

- **Autori:** Luca (Lead Developer Non Vedente con Screen Reader NVDA) & Antigravity (AI Pair Programmer)
- **Framework:** ASTRALIS v3.0.2
- **Data e Ora:** 2026-09-08
- **Stato Documento:** [ATTIVO — FASE 0 STRATEGIA LOGICO-COGNITIVA]
- **Repository:** `minecraft-access` (Minecraft 26.2, Fabric / NeoForge, Balm, Java 25)
- **Revisioni Correlate nel Registro RRU:**
  * [`Rev MC-26.14 — Integrazione Dominio Vitalità e Stato Fisiologico`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/REGISTRO_REVISIONI.md)
  * [`Rev MC-26.15 — Integrazione Dominio Ambiente e Sensi Passivi`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/REGISTRO_REVISIONI.md)
  * [`Rev MC-26.16 — Integrazione Dominio Didattico & Mentore (Fase 6)`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/REGISTRO_REVISIONI.md)
  * [`Rev MC-26.17 — Integrazione Dominio Radar Passivo Mob e POI`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/REGISTRO_REVISIONI.md)

---

## 🧭 1. Visione d'Insieme & Principio di Integrità Evolutiva (Regola 8)

Con il completamento delle Fasi 1–5 dell'architettura cognitiva (Sicurezza Motoria, Porte e Navigatore AutoWalk), il nucleo cinetico del mod è pienamente coordinato e libero da sovrapposizioni vocali.
Tuttavia, all'interno del repository permangono routine storiche che invocano direttamente `MainClass.narrate(...)` all'esterno dell'arbitraggio centrale, rischiando di troncare o scavalcare allarmi prioritari con sintesi vocale NVDA.

In conformità al **Principio di Integrità Evolutiva & Bonifica dei Residui (ASTRALIS Canone 8)**, questo documento definisce la roadmap strategica definitiva per:
1. Assorbire l'intero output vocale/acustico dei moduli residui nel `CognitiveCoordinator`;
2. Strutturare gli eventi in 4 Domini Specialistici dotati di EventFactory immutabili;
3. Sigillare formalmente le esclusioni permanenti volute per design architetturale.

---

## 🚫 2. Le Due Invarianti di Esclusione Permanente (Blindate per Design)

Al fine di preservare le prestazioni e la fluidità d'uso da tastiera (Zero Mouse), due specifici componenti sono dichiarati **esplicitamente e permanentemente fuori dall'arbitraggio cognitivo**:

### Invariante 1 — Interfaccia Utente & GUI (Latenza Zero Assoluta)
- **Componenti**: `InventoryControls.java`, schermate container, ricettario, casse, incudine, libri, chat e menu Cloth Config.
- **Disciplina**: Conservano l'invocazione diretta di `MainClass.narrate(msg, true)` a latenza zero ($0\text{ ms}$).
- **Motivazione Cognitiva**: La navigazione cella per cella da tastiera (frecce direzionali o tasti dedicati) richiede feedback immediato all'istante di rilascio del tasto GLFW. L'accodamento o l'arbitraggio a fine tick creerebbe una sensazione di ritardo gommoso inaccettabile per uno sviluppatore e giocatore non vedente.
- **Integrazione Passiva**: La presenza di una schermata aperta attiva `suspendForGui()` e silenzia gli annunci ambientali del mondo.

### Invariante 2 — Scanner a Richiesta Manuale (`DirectionalPathScanner`)
- **Componente**: `DirectionalPathScanner.java` (attivato con `Pagina Su`, `Pagina Giù` e combinazioni modificatori con Numpad Layer 3 per scansione colture, dislivelli e blocchi terreno).
- **Disciplina**: Rimane un tool autonomo on-demand non intermediato dal loop continuo.
- **Motivazione Cognitiva**: È un'azione di campionamento deliberata e circoscritta voluta da Luca in un punto fisso, non un sensore continuo che parla spontaneamente in background.

---

## 🏛️ 3. I 4 Domini Specialistici di Migrazione Residua

Tutte le emissioni spontanee residue del mod vengono ripartite in 4 domini funzionali:

### Dominio A: Vitalità e Stato Fisiologico (`Rev MC-26.14`)
- **Sottosistemi Coinvolti**: `PlayerStatus.java`, `HUDStatus.java`.
- **Nuova Factory Dedicata**: `StatusCognitiveEventFactory.java`.
- **Mappatura Eventi & Priorità**:
  - `CRITICAL` (Fast-Path 0 ms con Scudo Vocale 1500 ms):
    * Annegamento imminente (ultime 3 bolle d'aria);
    * Soffocamento dentro blocchi solidi;
    * Combustione da lava/fuoco.
  - `OPERATIONAL` (Fine-Tick Flush con coordinate relative):
    * Danno improvviso subito da mob (es. *"Danno da Scheletro, dietro"*);
    * Cuori critici ($\le 3$ cuori rimasti).
  - `CONTEXTUAL` (Fine-Tick con concatenazione ammissibile):
    * Fame moderata ($3$ cosciotti) o fame critica ($0$ cosciotti);
    * Esaurimento o applicazione effetti pozione (Veleno, Rigenerazione, Cecità).

---

### Dominio B: Ambiente e Sensi Passivi (`Rev MC-26.15`)
- **Sottosistemi Coinvolti**: `BiomeIndicator.java`, `TimeIndicator.java`, `XPIndicator.java`, `Weather.java`, `LightLevel.java`, `FluidDetector.java`.
- **Nuova Factory Dedicata**: `EnvironmentCognitiveEventFactory.java`.
- **Mappatura Eventi & Priorità**:
  - `CONTEXTUAL`:
    * Transizione in zona a luce zero (rischio comparsa mob);
    * Inizio pioggia, temporale o grandine.
  - `PASSIVE` (Fine-Tick di Background con finestre di grazia):
    * Notifica cambio bioma durante la marcia;
    * Annuncio transizione temporale (Alba, Mezzogiorno, Tramonto, Mezzanotte);
    * Guadagno livelli di esperienza (XP).
- **Regola di Quiete**: Durante la marcia veloce (AutoWalk o sprint) o in presenza di ostacoli/dislivelli, le notifiche ambientali di bioma vengono soppresse o differite a quando il moto si arresta per almeno $500\text{ ms}$.

---

### Dominio C: Didattica, Mentore & Onboarding (`Rev MC-26.16 — Fase 6 Roadmap`)
- **Sottosistemi Coinvolti**: `ContextualMentor.java`, `Academy`, `HelpNarrator.java`, `FirstRunHandler.java`.
- **Nuova Factory Dedicata**: `GuidanceCognitiveEventFactory.java`.
- **Mappatura Eventi & Priorità**:
  - `GUIDANCE` (Priorità Contestuale Didattica Dedicata):
    * Suggerimenti contestuali di stallo (quando il giocatore si arresta contro un ostacolo sconosciuto);
    * Promemoria per tasti dimenticati o interazioni raccomandate;
    * Istruzioni delle missioni tutorial dell'Accademia.
- **Invariante di Cessione Istantanea**: La voce del Mentore cede il passo all'istante (troncamento pulito) se sopraggiunge un evento di Sicurezza (`CRITICAL` o `OPERATIONAL`) o una variazione di stato del Navigatore AutoWalk.

---

### Dominio D: Radar Passivo Mob e POI Ambientali (`Rev MC-26.17`)
- **Sottosistemi Coinvolti**: `ObjectTracker.java`, `POIEntities.java`, `POIMarking.java`.
- **Nuova Factory Dedicata**: `EntityTrackingCognitiveEventFactory.java`.
- **Mappatura Eventi & Priorità**:
  - `OPERATIONAL`: Mob ostile avvistato o in avvicinamento entro $6\text{ metri}$ con provenienza spaziale cardinale;
  - `CONTEXTUAL`: Animali pacifici o compagni rilevati nelle vicinanze;
  - `PASSIVE`: Aggiornamento periodico dell'elenco entità remote ($> 10\text{ metri}$).
- **Disaccoppiamento Comandi Manuali**: L'aggancio o lock mirato con tasto `X` rimane protetto da `DirectInteractionShield` a latenza zero.

---

## 🔬 4. Matrice di Concorrenza e Regole di Arbitraggio

```text
[Priorità CRITICAL]   → Annegamento, Lava, Burrone, Ciglio Letale
      ↓ (interrompe)
[Priorità OPERATIONAL]→ Ostacolo Frontale, Danno Subito, Arrivo Navigatore, Mob < 6m
      ↓ (interrompe)
[Priorità GUIDANCE]   → Mentore Didattico, Tutorial Accademia
      ↓ (interrompe)
[Priorità CONTEXTUAL] → Gradino Saltabile, Fame, Buio, Fine Pozione
      ↓ (interrompe)
[Priorità PASSIVE]    → Mirino Automatico, Cambio Bioma, Meteo, Orario
```

- **Regola del Micro-Burst**: Se un evento di Danno (`OPERATIONAL`) coincide nello stesso tick con un allarme di Burrone (`CRITICAL`), il burrone parte a 0 ms con `interrupt=true` e il danno segue in coda immediata senza essere troncato.
- **Regola della Monotonia Spaziale**: Se il mirino passivo e l'ostacolo appartengono allo stesso bucket di coordinate $XZ$, l'evento viene unificato in una sola frase armonica (*"Ostacolo di Legno davanti, a 2 blocchi"*).

---

## 📋 5. Roadmap Sequenziale di Esecuzione

1. **Sprint 1 (Vitalità & Fisiologia — Rev MC-26.14)**:
   - Creazione di `StatusCognitiveEventFactory`;
   - Migrazione di `PlayerStatus` e `HUDStatus`;
   - Test unitari a 0 ms per cuori, fame, aria e danno.
2. **Sprint 2 (Ambiente & Indicatori — Rev MC-26.15)**:
   - Creazione di `EnvironmentCognitiveEventFactory`;
   - Migrazione di `BiomeIndicator`, `TimeIndicator`, `Weather`;
   - Test di silenziamento in marcia.
3. **Sprint 3 (Didattica & Mentore — Rev MC-26.16 / Fase 6)**:
   - Coordinamento del `ContextualMentor` con le code cognitive;
   - Garanzia di cessione immediata della voce del mentore su pericoli o cambi navigatore.
4. **Sprint 4 (Radar Passivo Mob & POI — Rev MC-26.17)**:
   - Integrazione passiva perimetrale di mostri e tracciamento.
5. **Sprint 5 (Collaudo Globale Fase 7 & Pulizia Totale Chiamate Legacy)**:
   - Audit a 5 barriere: verifica di zero chiamate orfane a `MainClass.narrate` eccetto quelle formalmente autorizzate per GUI e Scanner.
