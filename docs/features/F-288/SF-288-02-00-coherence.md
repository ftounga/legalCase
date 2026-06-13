# F-288 / Vague 2 (chefs de demande) — Cadrage cohérence (étape 0)

> Réexamen de la dimension « chefs de demande » de l'écran de composition, à la lumière du code. 2026-06-13.
> Parent : `SF-288-00-coherence.md` (qui listait les chefs de demande comme 3ᵉ trou candidat).

## Verdict : **STOP — gadget (redux de F-262), 0 code recommandé**

---

## Constat code (vérifié 2026-06-13)

1. **Les chefs de demande ne sont PAS un ingrédient du prompt de génération.** `HeadsOfClaimService` / `HeadOfClaimCatalog` (fondation F-262) **ne sont jamais consommés** par `CaseConclusionPromptBuilder` ni par `CaseConclusionService.prepare`. Il n'existe donc **rien à filtrer** : contrairement à la vague 1 (outils calculés = ingrédient réel via `assembleDecisionToolTiles`), il n'y a pas de liste de chefs injectée dans la génération.

2. **Les chefs transverses sont forcés en dur, comme postes systématiques.** `REDACTION_QUALITY_GUARD` (SF-98-55, `CaseConclusionPromptBuilder`) impose : *« postes systématiques : article 700 du Code de procédure civile, dépens, exécution provisoire, intérêts au taux légal et leur capitalisation »*. Ce ne sont pas des éléments « sélectionnables » : ils sont systématiques **par conception** (anti-oubli).

## Pourquoi « curer les chefs » est un gadget

Décomposition du catalogue F-262 (5 transverses + 7 outillés travail FR) :

| Catégorie de chef | « Curable » utilement ? |
|---|---|
| **Outillés** (faute inexcusable, harcèlement, résiliation judiciaire, prise d'acte…) | ❌ **Doublon vague 1** : ce sont des outils décisionnels → déjà curables en décochant l'outil. |
| **Transverses** (art. 700, dépens, intérêts+capitalisation, exécution provisoire) | ❌ **Anti-valeur** : postes systématiques qu'un avocat demande **toujours**. Les retirer = produire un dispositif incomplet — exactement l'inverse du but anti-oubli de F-262. |
| **Métier non outillés** (préjudice moral/vexatoire…) | ❌ **Gadget** : F-262 a déjà établi (investigation 2026-06-10) qu'ils **n'ont aucun flag de détection fiable** → applicabilité non fiable. |

➡️ Aucune des trois catégories n'offre une curation à la fois **réelle** (effet sur le prompt) et **utile** (pas un doublon, pas anti-valeur). C'est la **même conclusion que F-262**, qui fut clos à la fondation pour ce motif précis.

## Challenge amont / aval
- Amont : il faudrait **d'abord injecter** un ingrédient « chefs de demande » dans le prompt (non fait, et F-262 a jugé cette injection redondante/gadget). Donc trou amont = la brique n'existe pas et n'est pas souhaitable.
- Aval : sans ingrédient, rien à exploiter.

## Décision finale

**STOP. Zéro SF, zéro code** — conforme au précédent F-262 (clos à la fondation pour doublon/gadget) et à l'invariant projet « un outil = une situation » + « anti-gadget ». La curation des **outils** (vague 1, livrée) couvre déjà les chefs **outillés** ; la garde SF-98-55 couvre les **transverses** ; les **non-outillés** restent non détectables.

**Réversible** sur signal terrain avocat précis (ex. demande répétée de retirer un poste systématique donné) — auquel cas le micro-scope serait « rendre 1-2 postes systématiques de SF-98-55 conditionnels », à arbitrer par le PO. **Recommandation : ne pas construire.**
