# SF-212-12 — Frontend : section « modification du contrat — refus »

> Feature F-212. Outil : `F-DT-70-modification-contrat-refus`. Contrat API : `SF-212-11` (figé).

## Objectif

Afficher la section permettant à l'avocat de qualifier la mesure de l'employeur (modification du contrat vs changement des conditions de travail) et d'évaluer les conséquences du refus.

## Comportement nominal

Composant standalone `ModificationContratRefusSectionComponent`, sous `F-DT-70-modification-contrat-refus`. Affiché en `CONTEXTUAL` (flag `modification_contrat_refusee`).

Formulaire (6 champs) : élément modifié (select), toggle contractualisé, motif éco (toggle), notification L. 1222-6 (toggle), délai de réflexion (mois), réponse du salarié (select). Bouton « Analyser » → verdict (MODIFICATION_CONTRAT or / CHANGEMENT_CONDITIONS_TRAVAIL vert / INCERTAIN gris) + liste des conséquences avec fondement.

**Pré-remplissage IA** depuis `aiData.travailExtractedData.modifContratDetail`. **Cohérence F-IA-03** sur 4 `critereCode`.

## Cas d'erreur

- Backend 422 → message hors domaine.
- 4xx/5xx → `MatSnackBar`.

## Critères d'acceptation

1. Section visible uniquement si `modification_contrat_refusee = true`.
2. Verdict couleur correcte (3 états).
3. Conséquences du refus affichées avec fondement.
4. Pré-remplissage IA + `getPrefillCount()`.
5. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu, pré-remplissage, verdict 3 états, conséquences.

## Tables / endpoints / composants impactés

- Nouveaux fichiers composant, helper, service.
- Modifié : `TOOL_REGISTRY`.

## Hors périmètre

Backend (→ SF-212-11).
