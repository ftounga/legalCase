# SF-212-14 — Frontend : section « mutation — clause de mobilité »

> Feature F-212. Outil : `F-DT-71-mutation-clause-mobilite`. Contrat API : `SF-212-13` (figé).

## Objectif

Afficher la section permettant à l'avocat d'analyser la validité d'une clause de mobilité et les conséquences d'un refus de mutation.

## Comportement nominal

Composant standalone `MutationClauseMobiliteSectionComponent`, sous `F-DT-71-mutation-clause-mobilite`. Affiché en `CONTEXTUAL` (flag `mutation_refusee`).

Formulaire (7 champs) : toggle clause présente, zone précise, intérêt légitime, délai de prévenance (semaines), situation familiale contraignante, motif professionnel, réponse salarié. Bouton « Analyser » → verdict (VALIDE vert / INVALIDE rouge / ABSENCE or) + conséquences du refus + points d'analyse.

**Pré-remplissage IA** depuis `aiData.travailExtractedData.mutationDetail`. **Cohérence F-IA-03** sur 4 `critereCode`.

## Cas d'erreur

- Backend 422 → message hors domaine.
- 4xx/5xx → `MatSnackBar`.

## Critères d'acceptation

1. Section visible uniquement si `mutation_refusee = true`.
2. Verdict 3 états couleur.
3. Conséquences du refus affichées.
4. Pré-remplissage IA + `getPrefillCount()`.
5. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu, pré-remplissage, verdict 3 états.

## Tables / endpoints / composants impactés

- Nouveaux fichiers composant, helper, service.
- Modifié : `TOOL_REGISTRY`.

## Hors périmètre

Backend (→ SF-212-13).
