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

### Passo 4: Registrazione & Sincronizzazione Multi-Workspace
- Aggiungere immediatamente un nuovo record strutturato nella scheda [`09_registro_bug_e_soluzioni.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/knowledge/09_registro_bug_e_soluzioni.md).
- Se la soluzione introduce una nuova regola o vincolo generale, aggiornare `GEMINI.md` e le schede pertinenti, sincronizzando i file in tutte le cartelle collegate.
