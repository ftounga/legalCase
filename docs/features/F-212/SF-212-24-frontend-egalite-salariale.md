# SF-212-24 — Frontend : section « égalité salariale femmes/hommes »

> Feature F-212. Outil : `F-DT-56-egalite-salariale-femmes-hommes`. Contrat API : `SF-212-23` (figé).

## Objectif

Afficher la section permettant à l'avocat d'évaluer une discrimination salariale et de visualiser les éléments de preuve disponibles.

## Comportement nominal

Composant standalone `EgaliteSalarialeSectionComponent`, sous `F-DT-56-egalite-salariale-femmes-hommes`. Affiché en `CONTEXTUAL` (flag `egalite_salariale_pressentie`).

Formulaire (10 champs) : sexe salarié (select), salaire mensuel brut, ancienneté, qualification, nb comparants, écart moyen (€ et %), index égalité connu (toggle), score index, justifications objectives (toggle). Bouton « Analyser » → verdict (PROBABLE rouge / POSSIBLE or / PAS_APPARENT vert) + alerte non-plafonnement (invariant : toujours visible) + facteurs de disparité.

**Pré-remplissage IA** depuis `aiData.travailExtractedData.egaliteSalarialeDetail`. **Cohérence F-IA-03** sur 4 `critereCode`.

## Cas d'erreur

- Backend 422 → message hors domaine.
- 4xx/5xx → `MatSnackBar`.

## Critères d'acceptation

1. Section visible uniquement si `egalite_salariale_pressentie = true`.
2. Alerte non-plafonnement **toujours visible** (invariant).
3. Verdict 3 états couleur.
4. Prescription 5 ans affichée.
5. Pré-remplissage IA + `getPrefillCount()`.
6. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu, pré-remplissage, alerte non-plafonnement invariant, verdict 3 états, prescription 5 ans.

## Tables / endpoints / composants impactés

- Nouveaux fichiers composant, helper, service.
- Modifié : `TOOL_REGISTRY`.

## Hors périmètre

Backend (→ SF-212-23).
