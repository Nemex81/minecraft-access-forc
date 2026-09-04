# Rapporto di indirizzo correttivo — Traversal Safety Analyzer

**Destinatario:** Antigravity  
**Ambito:** affinamento strutturale Fase 3A — FallDetector / discesa sicura  
**Stato richiesto:** sola analisi e piano; applicare la Regola 0, senza modificare codice.

## 1. Esito della verifica sul test in gioco

Il test reale sul tetto ha evidenziato un conflitto di semantica: il sistema ostacoli riconosce e annuncia una scala a pioli, mentre il FallDetector classifica la stessa traiettoria come burrone e attiva l'auto-sneak. L'utente non può quindi entrare nel volume della scala per agganciarsi e scendere.

Il comportamento è confermato dalla telemetria: alle narrazioni di scala seguono avvisi di burrone con profondità 3–4 blocchi. Non è un problema di sola comunicazione: è una classificazione geometrico-funzionale errata che blocca un percorso verticale lecito.

## 2. Correzione della strategia

Non implementare una collezione di eccezioni nel FallDetector e non introdurre una semplice regola del tipo «se W è premuto vicino a una scala, rilascia Shift». Tale soluzione sarebbe fragile per tasti rimappati, movimento laterale/indietro, controller, diagonali, botole e scale moddate; inoltre non risolverebbe la riapplicazione dello sneak a ogni tick.

Non creare neppure un super-scanner che invochi direttamente FallDetector, ObstacleDetector e gli altri rilevatori in sequenza. Questo invertirebbe le dipendenze, creerebbe doppie scansioni e rischierebbe cicli fra sottosistemi.

La direzione approvata è un componente specializzato, autonomo e riutilizzabile del dominio Sicurezza: **TraversalSafetyAnalyzer** (denominazione da confermare nel piano). Il componente non sostituisce i rilevatori: interpreta in modo unitario il contesto di attraversamento e restituisce un esito strutturato.

```text
FallDetector + stato giocatore/mondo ─┐
                                     ├─→ TraversalSafetyAnalyzer
Osservazioni di altri sensori* ──────┘          │
                                                 ├─→ SafetyMovementGuard
                                                 └─→ CognitiveCoordinator

* Estensione predisposta, non integrazione diretta in questa sottofase.
```

## 3. Perimetro vincolante della Fase 3A

1. L'implementazione corrente deve limitarsi al FallDetector, allo stato del giocatore e alla geometria del mondo necessaria per valutare una discesa.
2. ObstacleDetector resta invariato e non deve essere chiamato direttamente: la sua migrazione è materia della Fase 3B, tuttora congelata.
3. È consentito predisporre una piccola interfaccia/DTO per future osservazioni standardizzate, purché senza modificare produttori della Fase 3B.
4. Il CognitiveCoordinator conserva il ruolo di regista dell'output: nessuna narrazione o suono nuovo deve aggirarlo quando è abilitato.

## 4. Contratto funzionale richiesto

Il nuovo analizzatore deve restituire un esito esplicito, non simulare la sicurezza impostando artificiosamente `drop = 0`:

- `DANGEROUS_DROP`: caduta reale; FallDetector mantiene avviso critico e protezione fisica.
- `SAFE_DESCENT_AVAILABLE`: discesa arrampicabile effettivamente raggiungibile nella direzione dell'intento del giocatore; può consentire l'aggancio.
- `AMBIGUOUS_OR_UNSAFE_DESCENT`: dati insufficienti, percorso interrotto o non raggiungibile; vale il principio fail-safe e resta la protezione.
- `NOT_APPLICABLE`: nessuna struttura di attraversamento pertinente.

L'analisi deve usare un contesto/snapshot coerente contenente almeno posizione e hitbox del giocatore, vettore di moto/intento già normalizzato, blocchi nelle celle coinvolte e stato del mondo. Non basarsi sul solo tasto `W`.

## 5. Regole di validazione della discesa

Il piano deve specificare come validare congiuntamente:

1. Corridoio realmente attraversabile dalla hitbox, includendo la cella corrente, quella di ingresso, le celle sotto e la parete di aggancio; non otto campioni radiali indipendenti.
2. Arrampicabili tramite `#minecraft:climbable`, mantenendo compatibilità con contenuti modded che espongono il tag.
3. Orientamento e parete di supporto delle scale; una scala presente ma non raggiungibile non può disattivare una protezione.
4. Continuità verticale e atterraggio: una colonna interrotta, una caduta oltre la soglia configurata o un punto di arrivo non sicuro non sono una discesa sicura.
5. Botole: solo aperte e solo come parte di una geometria di passaggio collegata a una discesa già validata; mai come esenzione autonoma.
6. Fluidi: acqua eventualmente valutabile come atterraggio sicuro secondo criteri espliciti; lava e fluidi pericolosi mai classificati sicuri per il solo fatto di essere fluidi.
7. Intenzione: la liberazione della protezione è ammessa soltanto verso un candidato validato e mentre il giocatore persegue quella traiettoria.

## 6. Controllo del movimento

Separare la decisione geometrica dalla scrittura del tasto Shift. Il piano deve prevedere un `SafetyMovementGuard` (o equivalente interno, con responsabilità isolata) che:

- applica l'auto-sneak esclusivamente per un `DANGEROUS_DROP`;
- rilascia l'override solo quando la discesa sicura è validata e intenzionale;
- non cancella lo Shift volontariamente mantenuto dall'utente;
- evita che il ciclo successivo riapplichi automaticamente uno sneak appena rilasciato per consentire l'aggancio.

Non è accettabile una chiamata ad hoc a `keyShift.setDown(false)` priva di un modello di proprietà dell'override.

## 7. Feedback cognitivo

L'eventuale conferma «discesa sicura disponibile» deve essere un evento semantico, localizzato e deduplicato:

- dominio `SAFETY` o `MOVEMENT`, con motivazione esplicita della scelta;
- priorità inferiore a una caduta reale;
- emesso una sola volta alla transizione/cambio candidato, mai a ogni tick;
- instradato dal CognitiveCoordinator quando abilitato, con fallback legacy preservato.

Prima di definire nuove chiavi, verificare se il modello attuale di `CognitiveEvent` e le regole di deduplicazione sono sufficienti; estenderle solo se necessario.

## 8. Osservazioni sul codice attuale da verificare

Nel checkout attuale sono già presenti tentativi locali di riconoscimento `CLIMBABLE`, scale, liane e impalcature dentro `FallDetector`. Il piano deve valutarli criticamente, non assumerli risolutivi:

- la classificazione dell'arrampicabile avviene dopo passaggi che possono già trattare la scala come barriera o il suo intorno come vuoto;
- il caso `current.above()` viene inoltrato al validatore con la posizione inferiore anziché con il candidato arrampicabile effettivo;
- la verifica di discesa usa soglie fisse e non esprime il rischio rispetto alla configurazione;
- la gestione globale dei fluidi va riesaminata perché non tutti i fluidi sono sicuri;
- la copertura di test esistente riguarda il dispatch cognitivo, non la geometria completa scala/bordo/aggancio.

## 9. Piano tecnico richiesto ad Antigravity

Redigere un piano formale prima di qualsiasi modifica. Il piano deve includere:

1. inventario delle classi e metodi interessati, con dipendenze e motivazione del posizionamento del nuovo modulo;
2. contratto dei DTO/esiti e politica fail-safe;
3. algoritmo di selezione e validazione del candidato di discesa;
4. strategia di ownership dell'input e ripristino dello stato;
5. integrazione con CognitiveCoordinator e compatibilità legacy;
6. piano di rollback e perimetro esplicito dei file;
7. matrice di test automatizzati e manuali NVDA;
8. stop obbligatorio per revisione di ChatGPT e approvazione dell'utente.

## 10. Criteri di accettazione minimi

- Il caso reale «tetto, scala a parete, vuoto laterale, profondità 4» consente la discesa intenzionale senza disattivare globalmente la protezione.
- Un bordo identico senza scala, con scala interrotta o con scala non raggiungibile resta bloccato e genera avviso critico.
- Movimento diagonale o parallelo a una scala non apre un varco non voluto.
- Scale, liane, impalcature, arrampicabili taggati da mod, botola aperta/chiusa, acqua e lava hanno test separati.
- Shift manuale dell'utente non viene annullato dall'override del sistema.
- Nessuna modifica a ObstacleDetector/Fase 3B in questa attività.
- Nessuna regressione dei test esistenti di FallDetector e CognitiveCoordinator.

## 11. Richiesta di verifica

Antigravity deve rispondere confermando o confutando ciascun vincolo con riferimenti al codice reale. Se propone una semplificazione, deve dimostrare che conserva i criteri di sicurezza sopra elencati. Non procedere all'implementazione finché il piano non è verificato e approvato.
