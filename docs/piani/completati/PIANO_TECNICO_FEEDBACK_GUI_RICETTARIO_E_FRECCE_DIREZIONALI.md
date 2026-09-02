# Piano Tecnico Formale (Fase 1A): Feedback Vocale GUI, Statistiche di Pagina & Concordanza Grammaticale (Rev 26.1 - 26.6)

## 📌 1. Quadro di Riferimento & Specifiche (Rev 26.6)

Integrazione della concordanza grammaticale singolare/plurale in `RecipePageStats.formatSummary()` per una resa vocale italiana e inglese naturale:

### 1.1 Regole di Flessione Grammaticale
- **Caso A: Tutte Realizzabili ($N == 0$)**:
  - $T == 1$: `"1 ricetta realizzabile"` (`en`: `"1 recipe, craftable"`)
  - $T > 1$: `"%d ricette realizzabili"` (`en`: `"%d recipes, all craftable"`)
- **Caso B: Nessuna Realizzabile ($R == 0$)**:
  - $T == 1$: `"1 ricetta non realizzabile"` (`en`: `"1 recipe, not craftable"`)
  - $T > 1$: `"%d ricette non realizzabili"` (`en`: `"%d recipes, none craftable"`)
- **Caso C: Miste ($R > 0$ e $N > 0$)**:
  - Prefisso totale: `"%d ricette: "`
  - Segmento realizzabile:
    - $R == 1$: `"1 realizzabile"` (`en`: `"1 craftable"`)
    - $R > 1$: `"%d realizzabili"` (`en`: `"%d craftable"`)
  - Segmento non realizzabile:
    - $N == 1$: `"1 non realizzabile"` (`en`: `"1 not craftable"`)
    - $N > 1$: `"%d non realizzabili"` (`en`: `"%d not craftable"`)
  - Formula unificata: `"%d ricette: %s, %s"`

---

## 🛡️ 2. Validazione Preventiva sui 7 Assi (Focus su Rev 26.6)

1. **Validità**:
   - Piena compatibilità con `I18n.get()` e standard di internazionalizzazione Fabric Loom.
2. **Efficacia**:
   - Risolve l'anomalia fonetica del plurale forzato su quantità unitarie (*"1 ricetta realizzabile"* anziché *"1 ricette realizzabili"*).
3. **Coerenza**:
   - Mantiene la sintassi `[Contesto], [Stats]. [Dettaglio]` immutata su tutte le schermate.
4. **Completezza**:
   - Copre le 6 combinazioni aritmetiche possibili ($T=1$, $R=1 \land N=1$, $R=1 \land N>1$, $R>1 \land N=1$, $R>1 \land N>1$, $N=0$, $R=0$).
5. **Precisione**:
   - Logica incapsulata interamente dentro `RecipePageStats.formatSummary()`, senza toccare i controller esterni.
6. **Affidabilità & Prestazioni**:
   - Zero calcoli pesanti o istanziazioni ridondanti.
7. **Assenza di Regressioni**:
   - Non altera il timing o il posizionamento del cursore negli slot.

---

## 🧪 2.1 Simulazione Completa dei Casi Grammaticali

### Simulazione 1: Singola ricetta disponibile (es. Fornace con filtro `R` attivo)
- **Input**: $T=1, R=1, N=0$.
- **Output Vocale**: *"Cotture realizzabili, 1 ricetta realizzabile. Realizzabile 1 Carbonella"*.

### Simulazione 2: Singola ricetta non disponibile (es. Categoria con 1 sola ricetta bloccata)
- **Input**: $T=1, R=0, N=1$.
- **Output Vocale**: *"Categoria: Varie, 1 ricetta non realizzabile. Non realizzabile 1 Campana"*.

### Simulazione 3: Mista con 1 sola realizzabile e molte non realizzabili (es. Fornace)
- **Input**: $T=15, R=1, N=14$.
- **Output Vocale**: *"Tutte le ricette, 15 ricette: 1 realizzabile, 14 non realizzabili. Non realizzabile 1 Patata al forno"*.

### Simulazione 4: Mista con molte realizzabili e 1 sola non realizzabile
- **Input**: $T=10, R=9, N=1$.
- **Output Vocale**: *"Categoria: Costruzione, 10 ricette: 9 realizzabili, 1 non realizzabile. Realizzabile 1 Scala di quercia"*.

### Simulazione 5: Mista con 1 realizzabile e 1 non realizzabile (pagina da 2 ricette)
- **Input**: $T=2, R=1, N=1$.
- **Output Vocale**: *"Pagina 1 di 1, 2 ricette: 1 realizzabile, 1 non realizzabile. Realizzabile 1 Torcia"*.

### Simulazione 6: Plurali standard
- **Input**: $T=20, R=5, N=15$.
- **Output Vocale**: *"Pagina 2 di 6, 20 ricette: 5 realizzabili, 15 non realizzabili. Realizzabile 1 Baule"*.

---

## 🛠️ 3. Modifiche ai File

### File: `it_it.json` & `en_us.json`
- Aggiunta chiavi (in ordine alfabetico):
  - `minecraft_access.inventory_controls.recipe_page_stats_craftable_count`
  - `minecraft_access.inventory_controls.recipe_page_stats_craftable_one`
  - `minecraft_access.inventory_controls.recipe_page_stats_not_craftable_count`
  - `minecraft_access.inventory_controls.recipe_page_stats_not_craftable_one`
  - `minecraft_access.inventory_controls.recipe_page_stats_single_craftable`
  - `minecraft_access.inventory_controls.recipe_page_stats_single_not_craftable`

### File: `InventoryControls.java`
- Aggiornamento di `RecipePageStats.formatSummary()`.
