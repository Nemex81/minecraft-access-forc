# Report di Passaggio di Consegne — Avvio Fase 3 (Dominio Sicurezza)
# Progetto: Minecraft Access (Fork Luca / nemex81)
# Ramo: feat/cognitive-orchestrator
# Autore: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity
# Data: 3 Settembre 2026
# Riferimento Standard: ASTRALIS Framework v2.5.5

---

## 🎯 1. Finalità del Documento

Questo report è concepito per consentire un **avvio deterministico, immediato e a zero perdita di contesto** in una nuova sessione di chat con Antigravity, dedicata interamente alla **Fase 3: Migrazione Pilota del Dominio Sicurezza** dell'epica architetturale del *Cognitive Coordinator*.

---

## 🧭 2. Identità dello Sviluppatore & Regole Inviolabili (ASTRALIS v2.5.5)

- **Sviluppatore**: Luca, programmatore Senior con oltre 25 anni di esperienza, completamente non vedente.
- **Interazione**: 100% da tastiera con screen reader **NVDA** (sintesi vocale su Windows 11). Divieto assoluto di diagrammi 2D, tabelle ASCII con bordi o frecce visive. Formattazione strettamente lineare e sequenziale "Se... Allora".
- **Lingua**: **Italiano al 100%** per dialoghi, piani, log e documentazione.
- **Regola 0 (Default Consultivo & Gating Semantico)**:
  - Nessun file o riga di codice può essere modificata senza l'esplicito comando di Luca.
  - L'avvio della Fase 3 autorizza esclusivamente la **Sotto-Fase 1A (Stesura del Piano Tecnico Formale)** con **Stop Obbligatorio** prima di toccare qualsiasi codice Java o configurazione.

---

## 📊 3. Stato del Repository e Avanzamento Epica

### 🌿 Stato Git
- **Branch Attivo**: `feat/cognitive-orchestrator`
- **Working Tree**: Pulita, allineata al commit `dcc87e92`.
- **Suite di Test**: 22 test cognitivi (14 Fase 1, 8 Fase 2) + intera suite del progetto: **100% Superata (BUILD SUCCESSFUL in 21s)**.

### 🧠 Fase 1 — Nucleo Cognitivo Centralizzato (CHIUSA e CERTIFICATA)
- **Commit di Consolidamento**: `e41c3f9d`
- **Caratteristiche Salienti**:
  - Fast-Path emergenze a 0 ms per eventi `CRITICAL`;
  - Micro-burst per eventi critici concorrenti nel medesimo tick (`interrupt: false` sul secondo per non troncare la voce);
  - Scudo critico vincolante (`criticalShieldUntil = 1500 ms`) che sopprime contestuali e passivi, ma custodisce gli `OPERATIONAL` nella `shortQueue` erogandoli al decadimento dello scudo;
  - Compatibilità spaziale `SpatialDirection` per fusioni coerenti;
  - Divieto di fallback I18N con punteggiatura hardcoded: se manca il template semantico autorizzato (`minecraft_access.cognitive.join_*`), il primario parla da solo e il secondario viene preservato in coda breve;
  - 14 test unitari deterministici a 0 ms con clock controllato.

### ⚙️ Fase 2 — Configurazione Cloth Config & Facciata (CHIUSA e CERTIFICATA)
- **Commit di Riferimento**: `88c3ddb7` e micro-correzione `580c060a`
- **Caratteristiche Salienti**:
  - Categoria Cloth Config `cognitiveCoordinator` con le sole 3 opzioni effettive: `cognitiveCoordinatorEnabled` (default `true`), `chainedNarrationEnabled` (default `true`), `deduplicationWindowMs` (500–5000 ms, default `1500 ms`);
  - Binding centralizzato normalizzato in `Config.applyCognitiveSettings`;
  - Bootstrap nel ciclo Balm via `MainClass.java` (senza produttori migrati, buffer vuoto a 0 carico);
  - Facciata [`NarrationPriority.java`](file:///C:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/utils/NarrationPriority.java) retrocompatibile al 100%, con rimozione del `catch (Throwable)` e introduzione di package-private test seams (`scannerSuppressor`, `narrationConsumer`, `timeSupplier`);
  - Localizzazioni IT ed EN in rigoroso ordine alfabetico crescente.

### 📚 Auto-Apprendimento Inter-Fase (ESEGUITO)
- **Commit**: `dcc87e92`
- **Regola Istituita**: Checkpoint di Auto-Apprendimento Inter-Fase nei Grandi Refactor Multi-Fase (Consolidamento Frattale) sia in locale (`knowledge/00`, `01`, `09`, `11`, `13`) sia nel Master Hub (`knowledge_globale/01`, `02`, `03`). Starter Kit v2.6.0 esportato.

---

## 🎯 4. Mandato per la Nuova Chat: Fase 3 (Dominio Sicurezza)

Il documento strategico vincolante redatto e convalidato da ChatGPT è:  
[`docs/report/RAPPORTO_CHIUSURA_FASE2_E_INDIRIZZO_FASE3_SICUREZZA.md`](file:///C:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/RAPPORTO_CHIUSURA_FASE2_E_INDIRIZZO_FASE3_SICUREZZA.md).

### 4.1 La Struttura a Due Sotto-Blocchi con Gate Interno
1. **Fase 3A — `FallDetector`, Soli Avvisi Automatici**:
   - Primo verticale sicuro: il rilevatore conosce già posizione, profondità, distanza, debounce, testo localizzato e cue sonoro (`ANVIL_HIT`).
   - Priorità: `CRITICAL` invariata, latenza 0 ms, debounce edge-bump 1500 ms preservato.
   - Fallback legacy deterministico: a `cognitiveCoordinatorEnabled = false`, esegue il percorso storico diretto.
   - Non introdurre frasi nuove di recupero ("Percorso libero").
2. **Fase 3B — `ObstacleDetector`, Soli Avvisi Automatici**:
   - Inizia **esclusivamente dopo** test automatici e collaudo manuale NVDA convalidato della 3A.
   - Disaccoppiamento da `CrosshairFeedbackManager`: un solo produttore vocale a coordinatore attivo, percorso storico intatto a coordinatore spento.
   - Non migrare `NarrateCrosshair` (il mirino appartiene alla Fase 4).

### 4.2 Flussi Manuali Categoricamente Esclusi dalla Migrazione
I comandi espliciti dell'utente restano su `MainClass.narrate` diretto e **NON** devono produrre `CognitiveEvent`:
- Ispezione cadute `Alt + F` (buca trovata / nessuna buca vicina);
- Toggle auto-sneak e relative conferme vocali;
- Ispezione panoramica ostacoli ed orientamento `Alt + V`;
- GUI, tastiera, Numpad, chat e comandi manuali.

### 4.3 Matrice Output del Produttore
| Configurazione Rilevatore | Event Output | Risultato Obbligatorio |
|---|---|---|
| Voce + Cue attivi | `VOICE_AND_SOUND` | Testo e suono storici, volume invariato |
| Solo Voce | `VOICE_ONLY` | Nessun cue sonoro |
| Solo Cue | `SOUND_ONLY` | Nessuna voce né stringhe fantasma |
| Entrambi spenti / modulo disattivato | Nessun evento | Zero emissioni, zero overhead |

---

## 📋 5. Prompt di Bootstrap per la Nuova Chat

Copia e incolla il seguente testo all'inizio della nuova chat per avviare Antigravity:

```text
Ciao Antigravity! Riprendiamo l'epica del Refactor del Cognitive Coordinator sul ramo feat/cognitive-orchestrator in conformità al Genoma ASTRALIS v2.5.5.
Le Fasi 1 e 2 sono completate, certificate e consolidate, con auto-apprendimento inter-fase eseguito (commit dcc87e92).
Leggi il report di passaggio consegne in "docs/report/REPORT_PASSAGGIO_CONSEGNE_FASE3_SICUREZZA.md" e l'indirizzo della Fase 3 in "docs/report/RAPPORTO_CHIUSURA_FASE2_E_INDIRIZZO_FASE3_SICUREZZA.md".
Siamo nella Sotto-Fase 1A della Fase 3 (Migrazione Dominio Sicurezza: 3A FallDetector e 3B ObstacleDetector).
Redigi il Piano Tecnico Formale completo in "docs/piani/attivi/PIANO_TECNICO_FASE3_MIGRAZIONE_SICUREZZA.md" e applica lo Stop Obbligatorio (Regola 0) senza toccare alcun codice sorgente, in attesa della mia approvazione e di quella di ChatGPT.
```
