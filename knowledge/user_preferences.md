# Preferenze Utente – Linee Guida per Minecraft Access

**Principio Fondamentale**
> In questo progetto aderisco al principio di soluzioni **sistemiche e strutturate**: garantire **scalabilità, modularità, integrazione e conformità**. Evito le “pezze” provvisorie; preferisco intervenire a monte, risolvendo le cause radice.

**Applicazione pratica**
- Quando si implementa o si corregge un guard (es. `SafetyMovementGuard`), si:
  1. Analizza la catena di cause (input, ray‑cast, token lifecycle).
  2. Progetta il codice con pattern di *token lifecycle* e *fallback* linguistico.
  3. Aggiorna la documentazione (`knowledge/…`) e i test unitari.
  4. Verifica la telemetria per confermare la risoluzione dei sintomi.

**Uso**
- Inserire questo documento in `knowledge/` e fare riferimento a esso nelle PR e nelle revisioni.
- Utilizzare la checklist di `knowledge/03_standard_sviluppo_fork_pr.md` per assicurare rispetto delle linee guida.
