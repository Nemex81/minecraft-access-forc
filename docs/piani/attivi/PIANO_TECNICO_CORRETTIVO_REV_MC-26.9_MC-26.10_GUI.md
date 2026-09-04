# Piano tecnico correttivo — Rev MC-26.9 e MC-26.10: robustezza GUI e ownership dello Shift

**Ramo di lavoro:** `feat/cognitive-orchestrator`  
**Stato:** `[PIANO FORMALE — PRONTO ALLA SOTTOFASE 1B; NESSUNA MODIFICA DI CODICE CONTENUTA IN QUESTO DOCUMENTO]`  
**Ambito:** due difetti GUI indipendenti, rilevati nel collaudo in-game dopo la chiusura tecnica della Fase 3B.  
**Riferimento diagnostico:** `docs/report/REPORT_STATO_SISTEMA_E_HANDOFF_ANOMALIE_GUI.md`  
**Baseline verificata:** branch pulito, Fase 3B già distribuita e collaudata; suite precedente: 185 test, zero errori/fallimenti.

---

## 0. Decisione, obiettivo e confini

Questo piano risolve in modo chirurgico:

1. **Rev MC-26.9 — `InventoryControls`:** un input di navigazione arrivato durante o subito dopo la chiusura di una GUI può usare uno `currentScreen` non più valido, generare una `NullPointerException`, muovere il mouse verso coordinate prive di contesto e narrare uno slot ormai inesistente.
2. **Rev MC-26.10 — `FallDetector` / `SafetyMovementGuard`:** Shift usato come modificatore di una scorciatoia GUI viene reinterpretato come comando di accovacciamento nel mondo, con posture e suoni non richiesti.

Il risultato richiesto è semplice per Luca:

- chiudere una GUI durante una navigazione non deve generare errore, movimento del mouse o voce residua;
- Shift dentro una GUI resta un modificatore GUI o quick-move e non modifica il giocatore;
- Shift manuale nel mondo resta autorevole;
- una protezione anticaduta attiva viene revocata entrando in GUI, senza poter riemergere come token sintetico;
- l'uscita dalla GUI ripristina il comportamento normale al tick successivo, senza stati appesi.

### Entro il perimetro

- `InventoryControls.java` e test mirati alla sua navigazione;
- `SafetyMovementGuard.java`, `FallDetector.java` e test di traversal/sicurezza;
- documentazione di chiusura, test, build e collaudo manuale.

### Esplicitamente fuori perimetro

- `RawCrouchIntentProvider`: deve rimanere una lettura fedele dell'hardware GLFW;
- `MinecraftSneakOverridePort`: resta l'unico adapter che scrive lo stato effettivo di crouch;
- `ObstacleDetector`, `CrosshairFeedbackManager`, `NarrateCrosshair`, `CognitiveCoordinator`, mixin, configurazioni pubbliche, binding, mondo e salvataggi;
- nuove opzioni GUI, ristrutturazioni estetiche, rinomini estesi e modifiche opportunistiche.

L'intervento non riapre né altera la Fase 3B: ne preserva l'invariante di ownership unica dell'input.

---

## 1. Invarianti non negoziabili

| ID | Invariante | Conseguenza verificabile |
|---|---|---|
| I-1 | Un input di navigazione è elaborabile solo mentre la stessa GUI contenitore è ancora attiva. | Nessuna eccezione, mouse move o voce dopo chiusura/cambio schermata. |
| I-2 | Il `SafetyMovementGuard` è l'unico proprietario delle scritture verso `SneakOverridePort`. | Nessuna nuova scrittura diretta a `keyShift` o a `player.setShiftKeyDown` fuori dal port. |
| I-3 | `RawCrouchIntentProvider` descrive l'hardware, non il contesto della GUI. | Nessuna condizione `screen` aggiunta nel probe raw. |
| I-4 | Solo il token `systemOverrideActive`, non `lastAppliedCrouch`, prova che il sistema possiede il crouch. | Il crouch manuale non viene mai rilasciato entrando in GUI. |
| I-5 | Una GUI sospende la sicurezza locale ma non interroga né riconcilia Shift fisico. | Shift premuto per `Shift+C`, `Shift+K`, `Shift+V` o quick-move non genera `applyEffectiveCrouch(true)`. |
| I-6 | Entrare in GUI mentre la protezione anticaduta è attiva rilascia una sola volta il token sintetico. | Una sola scrittura `false`, poi zero scritture nei tick GUI successivi. |
| I-7 | Alla ripresa nel mondo il normale reconciler torna a usare Shift fisico. | Shift ancora premuto produce un unico `true`; Shift rilasciato non genera sticky-sneak. |
| I-8 | Gli avvisi CRITICAL di caduta, la discesa validata e la Fase 3B restano invariati fuori dalle GUI. | Suite completa e collaudo in-game senza regressioni. |

---

## 2. Architettura di destinazione

```text
Input hardware GLFW
        |
        v
RawCrouchIntentProvider              GUI aperta
(solo verita fisica)                     |
        |                                v
        +--> SafetyMovementGuard <--- FallDetector.resetSafetyStateForGui()
                  |                         |
                  |                         +-- nessun reconcile del raw input
                  v
          SneakOverridePort
          (unico writer)

Kuma screen input
        |
        v
InventoryControls.isActiveContainerScreen()
        |
   falso|                     vero
        v                         v
 restituisce false          naviga/muove/narra
 (nessun effetto)           solo nella GUI corrente
```

La GUI non falsifica l'hardware e l'hardware non decide da solo se debba intervenire nel mondo: il contesto viene gestito dal `FallDetector`, proprietario del ciclo di sicurezza.

---

## 3. Rev MC-26.9 — contratto di validita della GUI inventario

### 3.1 Causa tecnica da preservare nel test

`InventoryControls.tick(...)` aggiorna lo stato in modo periodico. Un key event Kuma puo arrivare quando la schermata e stata chiusa, cambiata in un menu non contenitore o non e piu la stessa istanza che ha generato gli slot. Il controllo su `slotItem` non basta: lo slot puo essere non nullo mentre il suo contenitore non esiste piu.

### 3.2 Nuovo predicato centrale

Introdurre in `InventoryControls` un solo helper privato, ad esempio:

```java
private boolean isActiveContainerScreen() {
    Minecraft client = Minecraft.getInstance();
    return currentScreen != null
            && client.gui.screen() instanceof AbstractContainerScreen
            && client.gui.screen() == currentScreen;
}
```

L'identita dell'oggetto e richiesta: una GUI contenitore diversa non puo riutilizzare gruppi, slot o coordinate della GUI precedente.

### 3.3 Ordine di sincronizzazione del ciclo GUI

Nel `tick(Minecraft client)`:

1. mantenere il controllo `client.player == null`;
2. prima di `interval.isReady()`, verificare se la GUI corrente non e un `AbstractContainerScreen`;
3. in tal caso invocare un unico helper di pulizia, ad esempio `clearNavigationState()`, che azzeri in modo coerente `previousScreen`, `currentScreen`, gruppi, slot corrente, recipe widget e cache pertinenti;
4. solo per una GUI contenitore attiva applicare il debounce dell'intervallo e ricostruire gruppi/slot.

Questo non sostituisce i guard d'ingresso: riduce la finestra temporale di stato stantio e rende esplicito il lifecycle.

### 3.4 Guard a monte: nessun comando zombie

Il predicato deve essere consultato prima di ogni percorso che puo mutare il focus, muovere il mouse o narrare uno slot:

- callback Kuma che delegano a `focusSlotItemAt(...)`, `changeGroup(...)`, `selectGroup(...)` o navigazione equivalente: se il predicato e falso, restituire `false` cosi l'input non viene consumato dal sottosistema inventario;
- `focusSlotItemAt(...)`, `changeGroup(...)`, `selectGroup(...)` e `focusSlotItem(...)`: uscita immediata se il predicato e falso;
- eventuali callback differite che richiamano il movimento slot: stessa verifica prima di modificare stato o narrare.

Il guard deve avvenire **prima** dell'assegnazione a `currentSlotItem`, prima di `MainClass.narrate(...)` e prima di calcolare coordinate.

### 3.5 Guard a valle: difesa dell'ultimo miglio

Entrambi gli overload:

```java
moveToSlotItem(SlotItem slotItem)
moveToSlotItem(SlotItem slotItem, int delay)
```

devono conservare:

```java
if (slotItem == null || !isActiveContainerScreen()) return;
```

Non costruire coordinate, non richiamare `MouseUtils` e non ritardare azioni quando il contesto non e piu valido. Questo e un backstop, non il solo meccanismo di correttezza.

### 3.6 Effetti vietati

Quando `isActiveContainerScreen()` e falso sono vietati:

- aggiornare `currentSlotItem`, `currentGroup`, cache di testo o tempo di narrazione;
- chiamare `MouseUtils.move`, `moveAfterDelay`, click o scroll;
- chiamare `MainClass.narrate` per slot, gruppo o bordo inventario;
- consumare un key event Kuma destinato alla GUI successiva.

---

## 4. Rev MC-26.10 — contratto di ownership del crouch in GUI

### 4.1 Stato esistente e distinzione fondamentale

- `systemOverrideActive` e il token che indica la proprieta del sistema di sicurezza.
- `currentAllowedDescentId` e una autorizzazione di transito che deve decadere entrando in GUI.
- `lastAppliedCrouch` e una cache per evitare scritture duplicate sul port; puo riflettere anche un crouch manuale. **Non e un token di ownership.**

### 4.2 Nuova operazione di dominio

Aggiungere a `SafetyMovementGuard`:

```java
/**
 * Suspends traversal safety while a GUI owns keyboard input.
 * It never reads raw input and releases only a crouch previously owned by
 * the system safety token.
 */
public void suspendForGui() {
    boolean releaseSystemCrouch = systemOverrideActive;
    currentAllowedDescentId = null;
    systemOverrideActive = false;

    if (releaseSystemCrouch) {
        applyIfChanged(false);
    }
}
```

Regole del metodo:

1. catturare `systemOverrideActive` **prima** di azzerarlo;
2. non chiamare `intentProbe.readIntent()`;
3. non usare `lastAppliedCrouch` per decidere il rilascio;
4. usare `applyIfChanged(false)` per preservare deduplicazione e unico writer;
5. essere idempotente: tick GUI ripetuti dopo il primo non emettono nuove scritture;
6. annullare sempre `currentAllowedDescentId`, anche se nessun token era attivo.

`clearSystemOverride()` resta invariato per i normali reset nel mondo: li la riconciliazione con Shift fisico e desiderata.

### 4.3 Routing esplicito in FallDetector

Separare il ramo GUI dal reset generico:

```java
if (client.gui.screen() != null) {
    resetSafetyStateForGui();
    return;
}

if (player.isUnderWater() || /* altri stati liquidi esistenti */) {
    resetSafetyState();
    return;
}
```

Estrarre un helper comune per cancellare lo stato locale del detector (`safetyInterventionActive`, sprint, `autoSneakActive`, pericolo e candidato notificato). Poi:

- `resetSafetyState()` invoca `clearSystemOverride()` e ripristina lo stato locale;
- `resetSafetyStateForGui()` invoca `suspendForGui()` e ripristina lo stesso stato locale;
- nessun ramo GUI chiama `reconcileCrouchState()` direttamente o indirettamente.

Alla chiusura della GUI, il ramo normale di `tick(...)` resta l'unico punto che riavvia `checkLookAheadSafety(...)` e `reconcileCrouchState()`.

### 4.4 Comportamenti attesi

| Situazione | Scritture port attese | Stato atteso |
|---|---:|---|
| Shift gia manualmente attivo, token sistema falso, apertura GUI | 0 | Nessuna modifica alla postura manuale. |
| Token anticaduta vero, apertura GUI | un `false` | Token e discesa autorizzata revocati. |
| Shift premuto soltanto dentro GUI | 0 | Il giocatore non viene piegato dal mod. |
| GUI chiusa con Shift ancora premuto | un `true` nel primo reconcile mondo | Crouch manuale normale. |
| GUI chiusa con Shift rilasciato | nessun `true` | Nessun sticky-sneak. |

---

## 5. Modifiche autorizzate per file

| File | Modifica ammessa | Non modificare |
|---|---|---|
| `features/inventory_controls/InventoryControls.java` | Predicato di GUI attiva, pulizia lifecycle, guard a monte e a valle. | Layout della navigazione, keybind, semantica di gruppi e ricette. |
| `features/safety/traversal/SafetyMovementGuard.java` | Solo `suspendForGui()` e commenti contrattuali necessari. | Algoritmo di discesa, probe raw, port, modifiche ai token esistenti non motivate. |
| `features/FallDetector.java` | Routing GUI e helper di reset locale senza duplication. | Geometria voxel, analizzatore traversal, eventi cognitivi, soglie e feedback. |
| `SafetyMovementGuardTest.java` | Nuova matrice ownership/sospensione. | Test esistenti non correlati, salvo adattamenti strettamente necessari. |
| Nuovo test GUI mirato oppure test esistente idoneo | Verifica anti-NPE/anti-ghost. | Dipendenze di runtime, mixin o configurazione pubblica. |
| Report/registro di chiusura | Risultati verificabili dopo collaudo. | Riscrittura storica non correlata. |

`RawCrouchIntentProvider.java`, `MinecraftSneakOverridePort.java` e i file della Fase 3B sono congelati per questa revisione.

---

## 6. Piano di test automatizzato

### 6.1 SafetyMovementGuardTest — test nuovi obbligatori

Usare un `AtomicReference<CrouchIntent>`, una lista delle scritture al port e, se utile, un probe contatore per dimostrare che `suspendForGui()` non interroga l'hardware.

1. **Manuale invariato entrando in GUI**
   - Portare il guard a crouch manuale applicato con token sistema falso.
   - Chiamare `suspendForGui()`.
   - Assert: nessuna nuova scrittura, token falso, autorizzazione discesa nulla, nessuna lettura aggiuntiva del probe.
2. **Token di sistema rilasciato una sola volta**
   - `engageFallProtection()`, poi `suspendForGui()` due volte.
   - Assert: sequenza `[true, false]`, nessun secondo `false`, token falso, autorizzazione nulla.
3. **Shift premuto solo in GUI non viene propagato**
   - Probe impostato a `pressed=true`, guard senza token sistema e senza scritture precedenti.
   - Chiamare `suspendForGui()`.
   - Assert: zero scritture totali e zero letture del probe dal metodo.
4. **Ripresa manuale alla chiusura GUI**
   - Attivare protezione, sospendere, impostare raw Shift a `true`, chiamare il normale `reconcileCrouchState()`.
   - Assert: `[true, false, true]`, nessuna riattivazione del token sistema.
5. **Nessuno sticky-sneak alla chiusura**
   - Attivare protezione, sospendere, impostare raw Shift a `false`, riconciliare.
   - Assert: `[true, false]`, nessun `true` aggiuntivo.
6. **Autorizzazione di discesa sempre revocata**
   - Concedere una discesa valida, sospendere per GUI.
   - Assert: `isDescentAllowedFor(id)` falso e id corrente nullo.

I test esistenti per discesa validata, fallback fail-safe e cambiamento del raw input non devono essere indeboliti.

### 6.2 InventoryControls — test anti-race

Poiche il difetto e di lifecycle, privilegiare un test mirato al confine anziche una falsa simulazione completa del client:

1. estrarre il minimo helper privato/package-private testabile che esprima la validita della GUI corrente oppure usare una seam di screen identity gia disponibile;
2. simulare un evento di navigazione con `currentScreen` assente o non coincidente con la GUI corrente;
3. assert: callback non consumata quando applicabile, nessuna mutazione del focus, nessuna invocazione mouse e nessuna narrazione;
4. simulare lo stesso evento con GUI contenitore identica attiva;
5. assert: il percorso normale resta consentito.

Non introdurre reflection pesante, nuovi mixin o un fake client globale solo per il test. Se il test di confine non e realizzabile senza ampliare l'API, documentare il limite e compensare con il collaudo manuale obbligatorio sotto; non ridurre le guard di produzione.

### 6.3 Suite completa

Eseguire senza cache:

```powershell
.\gradlew.bat --no-daemon test --rerun-tasks
```

Accettazione:

- tutti i 185 test preesistenti piu i nuovi casi presenti;
- zero failure, error e skipped inattesi;
- ispezione degli XML in `build/test-results/test` se l'output console e troncato;
- nessuna modifica non pianificata nel diff Git.

---

## 7. Collaudo manuale NVDA obbligatorio

Usare una copia costruita dal commit di correzione, con istanza chiusa durante il deploy.

### 7.1 GUI inventario

1. Aprire inventario, cassa, tavolo da lavoro, fornace e alambicco.
2. Navigare rapidamente slot e gruppi; premere `Esc` durante frecce, gruppi e movimenti ritardati del mouse.
3. Assert: nessuna NPE in `latest.log`, nessuna voce di slot/gruppo dopo la chiusura e nessun movimento mouse spurio osservabile.
4. Aprire subito menu pausa o un'altra GUI dopo una chiusura: i comandi inventario non devono consumare input della nuova schermata.

### 7.2 Shift nelle GUI

Per almeno inventario, cassa e tavolo da lavoro:

1. eseguire `Shift+C`, `Shift+K`, `Shift+V` se configurati e un quick-move vanilla;
2. ascoltare l'assenza dei suoni di piegamento/rialzata e verificare che il giocatore non cambi postura per l'azione GUI;
3. tenere Shift, chiudere la GUI e verificare che il crouch manuale compaia solo nel mondo;
4. ripetere con Shift rilasciato prima della chiusura: nessun crouch residuo.

### 7.3 Sicurezza traversal

1. Sul ciglio del tetto, verificare ancora protezione anticaduta, revoca validata verso scala e discesa senza deadlock.
2. Aprire una GUI mentre la protezione e attiva, chiuderla e rieseguire un movimento sicuro lontano dal ciglio.
3. Assert: nessuna caduta indotta, nessun token sticky, nessuna regressione nella discesa di scala o botola.

### 7.4 Evidenze richieste

- hash del JAR build e dell'istanza di collaudo coincidenti;
- estratto di `latest.log` dall'avvio al test, incluso ricerca di `ERROR`, `Exception`, `InventoryControls` e messaggi di crouch;
- orario del salvataggio e stato del giocatore come telemetria passiva, senza modificare il mondo per la verifica;
- esito soggettivo di Luca per voce, postura e navigazione.

---

## 8. Build, deploy e chiusura controllata

### 8.1 Gate prima di distribuire

1. controllare branch, HEAD e `git status`;
2. eseguire i test completi con esito verde;
3. generare l'artefatto:

```powershell
.\gradlew.bat --no-daemon shadowJar
```

4. registrare hash SHA-256 dell'artefatto;
5. solo con autorizzazione di Luca e a istanza chiusa, sostituire il JAR nell'istanza di collaudo scelta; distribuire alla seconda istanza solo se Luca richiede parita immediata.

Non sovrascrivere mai un JAR in uso. Conservare identificabile l'artefatto precedente fino al superamento del collaudo, senza usare reset Git o azioni distruttive.

### 8.2 Gate di chiusura

La revisione e chiudibile solo se tutti i punti sono veri:

- test automatici verdi;
- JAR effettivamente caricato e hash verificato;
- test manuali GUI e traversal superati;
- nessuna nuova eccezione `InventoryControls` nel log di sessione;
- Shift GUI privo di effetti posturali e Shift mondo ancora manuale;
- `git diff --check` privo di errori introdotti;
- aggiornamento del rapporto di implementazione, del registro revisioni e commit atomico con soli file pianificati.

Se uno dei gate fallisce: fermarsi, preservare log/diff/hash, classificare il difetto e non distribuire ulteriormente l'artefatto.

---

## 9. Sequenza esecutiva obbligatoria

1. Rileggere questo piano e il rapporto di handoff; verificare base Git pulita.
2. Implementare prima `MC-26.10` nel guard e nel routing GUI di `FallDetector`, con i test unitari di ownership.
3. Implementare `MC-26.9` con lifecycle, guard di confine e difese mouse; aggiungere il test piu leggero che dimostri la race.
4. Eseguire la suite completa e correggere esclusivamente difetti entro perimetro.
5. Effettuare code review di responsabilita: nessuna modifica al probe raw, al port, alla Fase 3B o al coordinatore.
6. Costruire, distribuire con autorizzazione e collaudare in game.
7. Solo dopo evidenze complete aggiornare i report, archiviare/chiudere `MC-26.9` e `MC-26.10`, poi creare il commit atomico.

La Fase 4 del Cognitive Orchestrator resta sospesa fino alla chiusura di entrambi i gate GUI.
