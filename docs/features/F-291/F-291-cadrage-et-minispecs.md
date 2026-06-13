# F-291 — Fiabilisation du chiffrage et des citations des conclusions

> Origine : **audit de la V2 des conclusions (test 2026-06-13)**. 5 défauts constatés, un constat systémique : le prompt demande un acte complet mais ne fournit au LLM que les **outils calculés** ; tout le reste est **généré librement** → erreurs. Branche : `feat/F-291-fiabilisation-chiffrage`.

## Étape 0 — Cadrage cohérence (verdict : GO)
**Workflow** : synthèse → outils → conclusions. La génération produit l'acte final déposé au tribunal → **un chiffre ou une citation faux = rédhibitoire**. Pré-requis amont (outils, référentiels) **existent** : le barème calcule déjà plancher/plafond **légal** (`IndemniteComparatifResult.baremePlancherMois/Plafond`), l'article d'exécution provisoire prud'homal est connu (514 CPC / R.1454-28). Sortie exploitable : l'acte gagne en fiabilité juridique. **Anti-doublon** : distinct de F-263 (chiffrage auditable, qui trace) — ici on **fiabilise la source** transmise au LLM et on **borne** ses affirmations. **Invariant « 1 outil = 1 situation » intact** : SF-291-01 **observe** les valeurs déjà calculées par le comparateur (ne crée ni n'altère aucun outil).
**Invariants anti-gadget** : (1) l'effet doit être réel sur l'acte (le plafond légal apparaît, les citations non sourcées disparaissent) ; (2) principe **« silence > erreur »** (F-179/F-IA) : à défaut de source, formulation générique + `[à vérifier]`, jamais une affirmation inventée.

## Étape 0 bis — Cohérence écran (verdict : GO, impact minime)
Seul impact écran : SF-291-01 enrichit la **ligne secondaire** de la tuile « Indemnités » (onglet Décision) — ajoute les bornes légales. Pas de nouvel élément, pas de nouvelle zone : une ligne déjà présente devient plus informative. **Invariant anti-surcharge** : rester sur une ligne courte (`mois (montant € – montant €)`), ne pas allonger la tuile. SF-291-02 = aucun impact écran (prompt backend ; le contenu de l'acte change, pas la mise en page).

---

## SF-291-01 — Exposer les bornes légales du barème dans la tuile comparateur
**Objectif** : la tuile « Indemnités » (F-DT-09) doit transmettre aux conclusions **le barème légal (plancher/plafond)** en plus de la fourchette jurisprudentielle, pour que l'acte ne confonde plus les deux et réclame le **plafond légal**.
**Cause** : `CaseFileDashboardService.tileFromIndemniteComparatifAnalysis()` expose `primaryValue = fourchetteBasse–Haute` (jurisprudentielle) + `secondaryValue = baremeSource`. Le LLM lit ça et écrit « le **barème légal** fixe une fourchette de 14 008 à 23 896 € » (faux) en réclamant le haut jurisprudentiel.
**Comportement** :
- `secondaryValue` enrichie : `"Barème légal L.1235-3 : {plancherMois}–{plafondMois} mois ({plancherMontant} – {plafondMontant} €) · fourchette jurisprudentielle indicative"`, avec `plancherMontant = salaireMensuel × baremePlancherMois`, `plafondMontant = salaireMensuel × baremePlafondMois`.
- `primaryValue` inchangée (la fourchette jurisprudentielle reste l'estimation réaliste affichée).
- Robustesse : bornes nulles / branche RuptureConv → on conserve le `secondaryValue` actuel (pas de bornes légales injectées), fail-open existant préservé.
**Critères** : la tuile comparateur Macron (FR licenciement) affiche en secondaire les bornes légales calculées ; un dossier rupture conventionnelle (autre branche) garde son libellé ; aucune modification de `primaryValue` ni du calcul de l'outil.
**Tests** : unitaire/IT sur `tileFromIndemniteComparatifAnalysis` (Macron → secondaire contient « Barème légal » + montants plancher/plafond ; branche RuptureConv → inchangé).
**Impact** : backend (`CaseFileDashboardService`). Aucune migration. Tuile = ligne secondaire enrichie (étape 0 bis OK).

## SF-291-02 — Garde-fou « citations & chiffres » du prompt conclusions
**Objectif** : interdire au LLM d'**affirmer une donnée juridique non fournie** (numéro d'article, durée conventionnelle, montant) et épingler l'article d'exécution provisoire.
**Cause** : `CaseConclusionPromptBuilder` demande préavis / exécution provisoire / fin de contrat **sans fournir** les valeurs ni épingler les articles → le LLM comble (#2 préavis « 2 mois » non sourcé, #3 `L.1237-19-10` inventé, #4 art. 515 périmé, #5 arithmétique).
**Comportement** : ajout d'un fragment de garde (sur le modèle de `REDACTION_QUALITY_GUARD`, F-98 SF-98-55) au prompt système :
- Ne **jamais** citer un **numéro d'article**, une **durée conventionnelle** (préavis…) ou un **montant chiffré** qui ne sont pas explicitement fournis dans les données ci-dessus → employer une formulation générique et marquer **`[à vérifier]`**.
- **Exécution provisoire (prud'hommes)** : citer **art. 514 CPC** (exécution de droit) et **R. 1454-28** du Code du travail — **jamais l'art. 515**.
- **Solde de tout compte** : si cité, l'article est **L. 1234-20** (et non un article de rupture conventionnelle collective).
- Principe « **silence > erreur** » : mieux vaut une formulation prudente qu'une affirmation fausse.
**Critères** : le prompt système contient le garde-fou (assertions interdites + article exécution provisoire épinglé) ; `finalize`/modèle/MAX_TOKENS inchangés ; contenu produit toujours cohérent.
**Tests** : `CaseConclusionPromptBuilder` (unitaire) — le message système contient le fragment de garde (mots-clés « 514 », « R. 1454-28 », « [à vérifier] », interdiction d'articles non fournis).
**Impact** : backend (`CaseConclusionPromptBuilder`). Aucune migration. Aucun impact écran.

## Hors périmètre
- Injection des valeurs des outils préavis / indemnité légale **quand ils sont calculés** (amélioration future : les feeder au prompt) — ici on **borne** le LLM, on ne feed pas ces outils additionnels.
- Refonte de l'affichage du comparateur (la fourchette jurisprudentielle reste l'estimation phare de la tuile).

## Analyse transversale
- **Outil décisionnel** : SF-291-01 **observe** le comparateur (lecture seule, aucune altération) → invariant « 1 outil = 1 situation » respecté.
- **Auth/workspace/plans/navigation** : aucun. **Pré-fill IA** : non applicable.
- **Smoke E2E** : aucun (pas d'impact auth/workspace/navigation).
