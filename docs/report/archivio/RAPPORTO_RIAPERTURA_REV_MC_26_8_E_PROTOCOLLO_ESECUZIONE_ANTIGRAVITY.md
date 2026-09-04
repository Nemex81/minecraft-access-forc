# Rapporto operativo vincolante — Riapertura Rev. MC-26.8 e protocollo di esecuzione per Antigravity

**Destinatario operativo:** Antigravity  
**Ramo vincolato:** `feat/cognitive-orchestrator`  
**Stato reale della revisione:** `[RIAPERTA — FASE A NON SUPERATA — STOP A FASI B/C/D]`  
**Piano autorevole:** `docs/piani/attivi/PIANO_TECNICO_IMPLEMENTATIVO_TRAVERSAL_SAFETY_E_COGNITIVE.md`  
**Strategia autorevole:** `docs/strategie/STRATEGIA_SISTEMICA_TRAVERSAL_SAFETY_E_CENTRALIZZAZIONE_COGNITIVA.md`  
**Data:** 2026-09-03

---

## 0. Ordine operativo immediato

La Rev. MC-26.8 non è chiusa. Il commit `5e9ba7d` ha introdotto nomi e tipi preliminari, ma non ha realizzato il disaccoppiamento richiesto tra input fisico dell'utente e override sintetico dello sneak.

Antigravity deve:

1. trattare la Fase A come **non superata**;
2. non iniziare Fasi B, C o D;
3. non dichiarare build, test, deploy o collaudo come validi per il nuovo comportamento se eseguiti prima del commit corretto;
4. non chiudere/archiviare di nuovo la revisione fino ai gate espliciti di questo documento;
5. non modificare file della Fase 3B (`ObstacleDetector`, mirino, Crosshair) o sistemi estranei.

Questo rapporto non autorizza deploy, copia di JAR, modifica delle istanze PrismLauncher o build automatica. Queste attività richiedono una decisione separata dell'utente dopo i test.

---

## 1. Evidenza della non conformità attuale

### 1.1 Il deadlock non è stato risolto

Nel codice attuale `RawCrouchIntentProvider.isPhysicalCrouchHeld()` continua a leggere:

```java
client.options.keyShift.isDown()
```

Ma `SafetyMovementGuard` continua a usare:

```java
client.options.keyShift.setDown(state)
```

Il valore letto come "fisico" è dunque lo stesso valore che il mod può aver scritto artificialmente. Il nuovo provider è un wrapper del comportamento precedente, non una sorgente raw indipendente.

Traccia di fallimento attuale:

1. `engageFallProtection()` imposta `systemOverrideActive=true` e `keyShift.setDown(true)`;
2. la scala è riconosciuta e `allowValidatedDescent(...)` imposta `systemOverrideActive=false`;
3. il provider legge ancora `keyShift.isDown()==true`, causato dall'override del passo 1;
4. il guard lo interpreta come Shift manuale e non esegue `setDown(false)`;
5. lo sneak reale rimane attivo e il player può restare bloccato sul bordo.

La condizione è aggravata dal fatto che `systemOverrideActive` può essere `false` mentre lo stato effettivo di crouch è ancora `true`: il modello interno e l'effetto nel client divergono.

### 1.2 L'autorizzazione residua non è stata corretta

`FallDetector` non è stato modificato nel commit della presunta Fase A. In particolare, quando `moveDir == null`, il presidio statico continua a saltare la protezione se `getCurrentAllowedDescentId() != null`.

L'ID della discesa autorizzata non viene invalidato in modo esplicito quando il giocatore si ferma, devia, cambia candidato o torna in un contesto non compatibile. Questo viola l'invariante fail-safe: una scala passata non può autorizzare un ciglio nuovo.

### 1.3 Nuovi tipi non integrati

- `CrouchIntent` contiene `pressed` e `reliable`, ma il guard continua a ricevere il solo booleano di `CrouchIntentProbe`.
- `RawCrouchIntentProvider.readIntent()` non partecipa alla decisione del guard.
- `readIntent()` restituisce `reliable=true` anche dopo un fallimento della lettura, perché `isPhysicalCrouchHeld()` cattura l'eccezione e restituisce `false`.
- Non esiste `SneakOverridePort`.
- Non esiste un punto unico di riconciliazione tra token sintetico, intento raw e stato effettivo.
- Non esistono telemetria debug, test nuovi o integrazione `FallDetector` per questa Fase A.

### 1.4 Evidenza test non utilizzabile

Il report Gradle disponibile riporta 154 test verdi alle **18:05**. Il commit `5e9ba7d` è delle **18:08**. Quei risultati precedono il codice da validare e non possono essere citati come certificazione della Fase A.

Inoltre, i test correnti di `SafetyMovementGuard` iniettano lambda che simulano già un input fisico corretto; non riproducono il percorso di produzione `setDown(true)` seguito dal probe di default. Per questo possono restare verdi pur lasciando intatto il deadlock.

---

## 2. Principio fondamentale da applicare

> Un contratto non è implementato perché esiste una classe con il nome giusto. È implementato solo quando il flusso di produzione usa quel contratto, i test dimostrano il comportamento richiesto e il diff non lascia il vecchio percorso come sorgente effettiva di verità.

Antigravity deve distinguere sempre fra:

| Livello | Prova richiesta |
|---|---|
| Dichiarazione/documentazione | Non è una prova del comportamento. |
| Classe o record creato | Non prova che sia integrato. |
| Test unitario con mock | Prova solo il contratto coperto. |
| Test della catena di produzione | Prova l'integrazione del percorso reale. |
| Build eseguita dopo il commit | Prova compilazione/test della versione precisa. |
| Collaudo NVDA | Prova l'esperienza reale, non sostituisce i test automatici. |

Non è consentito elevare una prova di livello inferiore a certificazione di un livello superiore.

---

## 3. Perimetro obbligatorio della Fase A

### 3.1 Obiettivo unico

Separare l'ownership del crouch sintetico dalla rilevazione dell'intento fisico e revocare in modo fail-safe una discesa autorizzata non più valida.

### 3.2 File consentiti

- `src/main/java/.../features/safety/traversal/CrouchIntentProbe.java` oppure suo successore esplicitamente motivato;
- `src/main/java/.../features/safety/traversal/CrouchIntent.java`;
- `src/main/java/.../features/safety/traversal/RawCrouchIntentProvider.java`;
- nuovo `SneakOverridePort.java` e, se necessario, una sola implementazione client dedicata;
- `SafetyMovementGuard.java`;
- `FallDetector.java` limitatamente a invalida/revoca/riconciliazione dell'autorizzazione di discesa;
- test sotto `src/test/java/.../features/safety/traversal` e `.../features`;
- report di implementazione e aggiornamento corretto del registro.

### 3.3 File vietati

- `ObstacleDetector.java`, `NarrateCrosshair.java`, `CrosshairFeedbackManager.java`;
- mixin, GUI, Config pubblica, comandi, build scripts, JAR e deployment;
- modifiche opportunistiche o formattazione estesa;
- Fasi B/C/D (`TraversalStateMachine`, swept-volume, guida cognitiva) prima del Gate A1.

---

## 4. Contratto corretto da implementare

### 4.1 Input raw affidabile

Il guard deve ricevere un risultato completo, non un booleano ambiguo:

```java
record CrouchIntent(boolean pressed, boolean reliable) {}

interface RawCrouchIntentProvider {
    CrouchIntent readIntent();
}
```

L'implementazione di produzione deve leggere un input indipendente dall'override che il mod scrive. GLFW può essere usato soltanto come dettaglio dell'adapter, dopo aver verificato il tipo di binding e l'API della versione corrente. Un adapter che chiama `keyShift.isDown()` dopo che il mod usa `setDown()` **non è raw** e deve essere rifiutato.

Se tastiera, controller o binding configurato non possono essere letti con affidabilità, l'adapter restituisce `reliable=false`; non simula una certezza inesistente.

### 4.2 Porta dell'effetto sintetico

```java
interface SneakOverridePort {
    void applyEffectiveCrouch(boolean crouching);
}
```

Solo la sua implementazione client può scrivere lo stato effettivo nel client. Nessun altro ramo in `FallDetector` o nel guard può scrivere direttamente `keyShift.setDown(...)` o `player.setShiftKeyDown(...)`.

### 4.3 Guard e token

Il guard possiede il solo `systemCrouchToken`. Per ogni riconciliazione:

```text
se systemCrouchToken è attivo:
    effective crouch = true
se token è rilasciato e intent raw è affidabile e non premuto:
    effective crouch = false
se token è rilasciato e intent raw è affidabile e premuto:
    effective crouch = true
se intent raw non è affidabile:
    applicare la policy conservativa documentata; non dichiarare il rilascio riuscito
```

La decisione deve essere centralizzata e testabile. Il guard non deve leggere direttamente il mondo né creare eventi vocali.

### 4.4 Revoca dell'autorizzazione alla discesa

`FallDetector` deve invalidare `currentAllowedDescentId` prima del presidio statico quando si verifica almeno una delle condizioni seguenti:

- `moveDir == null`;
- l'assessment non conferma il medesimo `candidateId`;
- il player cambia candidato/direzione o entra in un contesto escluso;
- reset: GUI, acqua/nuoto, detector disabilitato, cambio mondo/dimensione, morte, respawn, disconnessione;
- il player termina o abbandona la discesa.

La Fase A non autorizza una finestra di grazia: questa sarà responsabilità della macchina a stati della Fase B. Finché la macchina non esiste, nessun permesso deve persistere oltre l'assessment corrente.

---

## 5. Procedura obbligatoria di esecuzione

### Passo A0 — preflight senza modifiche sorgente

1. Annotare commit di partenza, stato del worktree e file già modificati.
2. Verificare nella versione Minecraft/Balm presente l'API effettivamente utilizzabile per l'input fisico; riportare tipo di keybinding, vincoli tastiera/controller e comportamento headless.
3. Definire in un breve appunto tecnico quale policy viene scelta per `reliable=false` e perché è fail-safe.
4. Individuare gli hook di reset già presenti e quelli realmente disponibili senza introdurre mixin.
5. Non dichiarare la Fase A iniziata finché queste cinque evidenze non sono state riportate.

### Passo A1 — test rossi prima del refactor

Creare o aggiornare test che falliscono sul codice corrente:

1. **Deadlock produzione simulato:** porta effettiva inizialmente riceve `true` da `engageFallProtection`; provider raw affidabile restituisce `false`; `allowValidatedDescent` deve produrre `false` effettivo.
2. **Shift manuale:** token rilasciato con provider affidabile `pressed=true`; output effettivo resta `true`.
3. **Input non affidabile:** la policy conservativa scelta viene verificata esplicitamente.
4. **Permesso residuo:** candidato autorizzato, poi `moveDir==null`; il candidato è invalidato prima del presidio statico.
5. **Reset:** GUI/acqua/disabilitazione eliminano token e autorizzazione.
6. **Nessun doppio output:** la modifica non altera le narrazioni cognitive/legacy esistenti.

Il test non può essere una lambda che presuppone già la correttezza del provider di produzione: deve esercitare l'adapter/porta o un seam che ne replica fedelmente la sequenza di stato.

### Passo A2 — implementazione minima

1. Introdurre gli adapter e il token conformi alla sezione 4.
2. Rifattorizzare il guard affinché consumi `CrouchIntent` e usi solo `SneakOverridePort` per l'effetto.
3. Aggiornare `FallDetector` per revocare un candidato decaduto prima dei rami statici.
4. Aggiungere logging `debug` non narrato di: intent `(pressed,reliable)`, token, output effettivo, candidateId, motivo della revoca.
5. Rieseguire i test scritti nel Passo A1; devono diventare verdi senza indebolire le asserzioni.

### Passo A3 — revisione obbligatoria del diff

Prima di commit, Antigravity deve produrre una tabella di verifica:

| Domanda | Evidenza richiesta |
|---|---|
| Il provider legge davvero una fonte indipendente? | Riga/API precisa, non una descrizione. |
| `CrouchIntent.reliable` influenza il guard? | Test e punto di consumo. |
| Esiste un solo writer verso crouch? | Ricerca dei riferimenti nel perimetro. |
| `FallDetector` revoca l'ID a fermo? | Diff e test d'integrazione. |
| Fase 3B è intatta? | Lista file cambiati confrontata col perimetro. |
| Test eseguiti dopo il commit? | Timestamp, commit SHA e output. |
| Il collaudo non è stato anticipato come risultato? | Report con stato "da collaudare" finché non eseguito. |

È vietato fare commit o aggiornare il registro come "chiuso" se anche una sola riga non ha evidenza concreta.

### Passo A4 — commit, test e gate

1. Commit atomico proposto: `fix(safety): separate system crouch ownership from raw intent`.
2. Eseguire la suite dopo il commit e registrare SHA, comando, orario, numero test e risultato.
3. Verificare che i test siano stati effettivamente modificati per coprire il nuovo percorso.
4. Scrivere un report di implementazione che separi: compilazione, test automatici, limiti noti e collaudo NVDA ancora da eseguire.
5. Fermarsi al Gate A1 e attendere revisione/autorizzazione dell'utente.

Non sono autorizzati `shadowJar`, deploy o collaudo in istanze di gioco dentro questo passo.

---

## 6. Gate di qualità non aggirabili

### Gate A0 — fattibilità verificata

Passa solo se l'API raw, la policy `reliable=false` e gli hook disponibili sono documentati con evidenza di codice/API.

### Gate A1 — Fase A corretta

Passa solo se tutti i requisiti sono veri:

- il test deadlock falliva prima e passa dopo;
- il provider di produzione non usa `keyShift.isDown()` come input raw contaminato;
- `CrouchIntent` è realmente consumato dal guard;
- `SneakOverridePort` è l'unico writer nel perimetro;
- l'ID di discesa decade a fermo e nei reset richiesti;
- nuovi test e report sono successivi al commit corretto;
- nessun file di Fase 3B è cambiato;
- il report non dichiara ancora successo NVDA/deploy non avvenuti.

Se un requisito fallisce, la Fase A resta aperta. Non è ammesso compensare la mancanza iniziando Fase B, C o D.

### Gate A2 — collaudo NVDA, esterno al commit

Solo su istruzione esplicita dell'utente: build, installazione controllata e test nella scena reale del tetto. Registrare posizione/scenario, input manuale e sintetico, stato del token, aggancio, esito e regressioni.

Il passaggio ad A2 non implica automaticamente l'approvazione della Fase B.

---

## 7. Regole contro omissioni future del piano

1. **Leggere il piano per intero prima di ogni fase.** Non estrarre solo i nomi delle classi o il primo passo.
2. **Trascrivere il perimetro in una checklist del commit.** Ogni requisito incluso/vietato deve avere stato: soddisfatto, non applicabile con motivazione, o bloccato.
3. **Implementare prima i test che dimostrano il difetto.** Nessuna certificazione basata solo su test preesistenti.
4. **Confrontare sempre baseline → HEAD e HEAD → worktree.** Un report non sostituisce il diff.
5. **Confrontare sempre timestamp test → SHA commit.** Risultati precedenti al commit sono storici, non certificazione.
6. **Non chiudere per nome.** Una classe chiamata `Raw...`, `Port` o `Token` non prova la separazione se il flusso di produzione usa ancora quello vecchio.
7. **Non semplificare gli invarianti.** `reliable`, revoca a fermo e writer unico non sono optional tecnici.
8. **Segnalare i blocchi invece di aggirarli.** Se l'API controller/raw non è verificabile, fermarsi e richiedere decisione; non assumere compatibilità.
9. **Aggiornare i documenti di stato solo dopo il gate.** Registro e archivio riflettono prove concluse, non intenzioni o ottimismo.
10. **Una fase, un esito.** Vietato fondere Fase A con macchina a stati, sweep o guida cognitiva per "completare" rapidamente.

---

## 8. Correzione documentale richiesta

Il registro e l'archivio che marcano Rev. MC-26.8 come chiusa/collaudata devono essere corretti nella prossima modifica documentale autorizzata:

- stato: `[RIAPERTA — FASE A NON CONFORME]`;
- motivazione: provider raw ancora contaminato da `keyShift.setDown`, assenza di integrazione FallDetector/test/telemetria;
- i test delle 18:05 devono essere etichettati come baseline precedente al commit 18:08;
- nessun claim di collaudo NVDA o deploy può restare senza evidenza datata e riferita al commit corretto.

Questa correzione non è cosmetica: impedisce che una falsa chiusura diventi base per Fasi B/C/D e renda difficile il rollback diagnostico.

---

## 9. Esito richiesto ad Antigravity

Al termine del solo Gate A1, Antigravity deve consegnare un rapporto conciso con:

1. SHA di baseline e SHA del commit Fase A;
2. file modificati e file esplicitamente non modificati;
3. API raw verificata e limiti supporto input;
4. test nuovi, quale difetto riproducono e risultati post-commit;
5. prova della revoca dell'ID a fermo;
6. estratto telemetria debug o motivazione se non eseguibile senza gioco;
7. stato esatto: `PRONTA PER COLLAUDO NVDA`, non `CHIUSA`, finché il collaudo non è eseguito;
8. richiesta esplicita di autorizzazione prima di build/deploy o Fase B.

Nessuna risposta che ometta uno di questi punti è considerata una chiusura valida della Fase A.
