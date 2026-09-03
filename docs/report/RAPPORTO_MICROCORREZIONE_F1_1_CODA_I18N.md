# Rapporto di Micro-Correzione — F1-1 Coda Secondario senza Template I18N

**Data:** 2026-09-03  
**Destinatario operativo:** Antigravity  
**Branch:** `feat/cognitive-orchestrator`  
**Esito:** `[ULTIMA CORREZIONE RICHIESTA PRIMA DELLA CERTIFICAZIONE FASE 1]`

---

## 1. Riscontro della contro-validazione

La mini-revisione ha risolto correttamente il fallback hardcoded: `defaultResolveTemplate` restituisce ora `null` quando il template I18N manca e non costruisce più una frase con punteggiatura generica.

Resta tuttavia incompleta la seconda metà del requisito F1-1: quando la fusione viene negata, il secondario valido deve essere differito nella `shortQueue` con il TTL residuo.

Nel flusso corrente, il secondario viene accodato soltanto se `OPERATIONAL` o `CONTEXTUAL`. Il caso di test F1-1 usa invece un secondario `PASSIVE` (*mirino su Quercia*), che viene quindi perso dopo l'emissione del primario *Gradino*.

## 2. Correzione richiesta

### Politica vincolante

Quando una coppia è compatibile ma il resolver I18N non restituisce un template valido:

1. Il primario viene emesso singolarmente nel rispetto del proprio `OutputType`.
2. Il secondario viene conservato una sola volta nella `shortQueue` se è ancora entro TTL, anche se ha priorità `PASSIVE`.
3. Al flush successivo il secondario viene emesso singolarmente se ancora valido; se il TTL è scaduto viene scartato silenziosamente.
4. La coda resta limitata e non può accumulare duplicati: applicare la normale deduplicazione e il limite massimo già scelto dal coordinatore.

Questa regola vale esclusivamente per il fallimento di una concatenazione che era altrimenti autorizzata da domini, direzione e flag `canChain`. Non modifica la politica dello shield critico o dello shield di interazione diretta, dove i passivi restano deliberatamente sacrificabili.

## 3. Indicazione tecnica per il coordinatore

Nel ramo non concatenato attivato da template assente, la conservazione del secondario non deve limitarsi a `OPERATIONAL` e `CONTEXTUAL`. Deve includere anche `PASSIVE` nel caso specifico di fallback I18N, purché:

- il secondario non sia `SILENT`;
- disponga di almeno un canale di output autorizzato;
- non sia scaduto;
- non sia già deduplicato;
- la `shortQueue` abbia capacità disponibile.

Non introdurre una concatenazione alternativa, testo hardcoded o un nuovo template di emergenza.

## 4. Test obbligatorio

Aggiornare `testMissingTemplateResolutionDeniesChaining` oppure aggiungere un test dedicato.

### Scenario

- Primario: `SAFETY`, `CONTEXTUAL`, testo “Gradino”.
- Secondario: `EXPLORATION`, `PASSIVE`, testo “Quercia”.
- Direzione compatibile, `canChain=true` e resolver template che restituisce `null`.

### Asserzioni richieste

1. Primo flush: viene pronunciato esclusivamente “Gradino”; non appare alcuna frase “Gradino. Quercia”.
2. Secondo flush, ancora entro TTL: viene pronunciato “Quercia” singolarmente.
3. Variante TTL: se il secondo flush avviene dopo la scadenza del secondario, “Quercia” non viene pronunciata.
4. Nessuna regressione nei test di direzione, `OutputType`, scudo critico e interazione diretta.

## 5. Validazione e uscita

Antigravity deve:

1. applicare questa correzione nel solo package `features.cognitive` e nei suoi test;
2. eseguire il test mirato `CognitiveCoordinatorTest` con Java 25 e `--no-daemon`;
3. eseguire la suite completa `test` con Java 25 e `--no-daemon`;
4. riportare il numero effettivo dei test, gli esiti e i soli file modificati;
5. attendere contro-validazione indipendente.

Quando questi controlli saranno positivi, la Fase 1 potrà essere certificata come chiusa e sarà possibile valutare l'avvio della Fase 2.

---

**Checkpoint ASTRALIS:** Questo rapporto non autorizza Fase 2, migrazione di sensori, deploy, merge o aggiornamenti `knowledge/`. Autorizza esclusivamente la correzione isolata F1-1 e la relativa verifica.
