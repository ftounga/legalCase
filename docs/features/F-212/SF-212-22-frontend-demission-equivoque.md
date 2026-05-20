# SF-212-22 — Frontend : section « démission équivoque »

> Feature F-212. Outil : `F-DT-41-demission-validite-equivoque`. Contrat API : `SF-212-21` (figé).

## Objectif

Afficher la section permettant à l'avocat d'évaluer le caractère équivoque d'une démission et les possibilités de requalification.

## Comportement nominal

Composant standalone `DemissionValiditeEquivoqueSectionComponent`, sous `F-DT-41-demission-validite-equivoque`. Affiché en `CONTEXTUAL` (flag `demission_equivoque_pressentie`).

Formulaire (7 champs) : mode d'expression (select), toggles altercation / pression / rétractation / manquements employeur / état émotionnel, délai de rétractation (jours). Bouton « Analyser » → verdict (VOLONTE_CLAIRE vert / DEMISSION_EQUIVOQUE rouge / RETRACTATION_POSSIBLE or) + score équivocité + facteurs avec fondement + indicateur `requalificationPossible`.

**Pré-remplissage IA** depuis `aiData.travailExtractedData.demissionEquivoqueDetail`. **Cohérence F-IA-03** sur 4 `critereCode`.

## Cas d'erreur

- Backend 422 → message hors domaine.
- 4xx/5xx → `MatSnackBar`.

## Critères d'acceptation

1. Section visible uniquement si `demission_equivoque_pressentie = true`.
2. Score équivocité affiché (0-100).
3. Indicateur requalification possible visible.
4. Verdict 3 états couleur.
5. Pré-remplissage IA + `getPrefillCount()`.
6. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu, pré-remplissage, score, verdict 3 états, indicateur requalification.

## Tables / endpoints / composants impactés

- Nouveaux fichiers composant, helper, service.
- Modifié : `TOOL_REGISTRY`.

## Hors périmètre

Backend (→ SF-212-21).
