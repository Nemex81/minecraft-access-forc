# 08 — Protocollo di Auto-Miglioramento Continuo

## 1. Obiettivo del Protocollo

Il Protocollo di Auto-Miglioramento garantisce che l'assistente AI evolva continuamente la propria conoscenza del progetto, prevenendo la reiterazione di errori, bug o incomprensioni sui vincoli di accessibilità di Luca.

---

## 2. Algoritmo Operativo in 4 Passi

```
[1. Rilevamento] -> [2. Diagnosi & Causa Radice] -> [3. Risoluzione & Test] -> [4. Registrazione & Sync]
```

### Passo 1: Rilevamento & Intercettazione
- Rilevare qualsiasi errore a runtime nei log (`latest.log`), blocchi di compilazione Gradle, fallimenti di CI GitHub (es. Checkstyle, linting JSON) o anomalie di vocalizzazione NVDA segnalate da Luca.

### Passo 2: Diagnosi & Causa Radice
- Isolare l'origine esatta del problema (es. mancato focus handler in Kuma UI, collisione di Mixin, chiave JSON non ordinata alfabeticamente, comando Minecraft ridondante).
- Non limitarsi a un workaround temporaneo: identificare la causa architetturale.

### Passo 3: Risoluzione Modulare & Collaudo
- Applicare la modifica minima e pulita nel codice o nella configurazione.
- Eseguire la compilazione con `.\gradlew.bat shadowJar`.
- Verificare che il fix rispetti i principi di accessibilità vocale al 100%.

### Passo 4: Registrazione & Commit Git nel Repository
- Aggiungere immediatamente un nuovo record strutturato nella scheda [`09_registro_bug_e_soluzioni.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/knowledge/09_registro_bug_e_soluzioni.md).
- Se la soluzione introduce una nuova regola o vincolo generale, aggiornare `GEMINI.md` e le schede pertinenti **esclusivamente all'interno del repository Git**, committando e inviando le modifiche al branch remoto `origin/mymaster` (senza alcuna copia in cartelle esterne).

---

## 3. Le 3 Dimensioni dell'Auto-Miglioramento & Trigger Proattivo

L'auto-apprendimento si sviluppa sistematicamente su tre assi coordinati (come definito nella scheda [`00_consuetudini_operative_e_sinergia_assistente.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/knowledge/00_consuetudini_operative_e_sinergia_assistente.md)):

1. **Dimensione Tecnica & Voxel**: Raffinamento di raycast, collisioni, debouncing e Mixin.
2. **Dimensione Operativa & Metodologica**: Strutturazione delle fasi, organizzazione dei piani in `docs/piani/` e standard di validazione a 7 assi.
3. **Dimensione Comunicativa & Cognitiva per NVDA**: Perfezionamento del linguaggio lineare, sintesi e riduzione del carico cognitivo per lo screen reader.

### Trigger Proattivo di Proposta Nuove Regole
Quando Antigravity individua una preferenza o procedura ricorrente che ottimizza il lavoro futuro:
- Risponde prima alla richiesta operativa di Luca.
- Inserisce in calce un box di proposta per formalizzare la regola in `knowledge/00` o nella scheda di riferimento, attendendo sempre l'approvazione esplicita prima di applicarla.