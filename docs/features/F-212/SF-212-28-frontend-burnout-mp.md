# SF-212-28 — Frontend : section « burn-out — reconnaissance MP »

> Feature F-212. Outil : `F-DT-64-burnout-reconnaissance-mp`. Contrat API : `SF-212-27` (figé).

## Objectif

Afficher la section permettant à l'avocat d'évaluer les chances de reconnaissance du burn-out comme maladie professionnelle par le CRRMP.

## Comportement nominal

Composant standalone `BurnoutReconnaissanceMpSectionComponent`, sous `F-DT-64-burnout-reconnaissance-mp`. Affiché en `CONTEXTUAL` (flag `burnout_detecte`). **Gate `isFrance`** : bannière info.

Formulaire (8 champs) : diagnostic posé (toggle), taux IPP estimé (%), années exposition, surcharge documentée (toggle), manquements sécurité (toggle), harcèlement concomitant (toggle), arrêts maladie multiples (toggle), lien causal direct (toggle). Bouton « Analyser » → verdict (CHANCES_IMPORTANTES vert / MODEREES or / FAIBLES rouge) + alerte si IPP < 25 % (condition non remplie) + facteurs dossier + délai instruction.

**Pré-remplissage IA** depuis `aiData.travailExtractedData.burnoutDetail`. **Cohérence F-IA-03** sur 4 `critereCode`.

## Cas d'erreur

- Backend 422 → message outil FR-only.
- 4xx/5xx → `MatSnackBar`.

## Critères d'acceptation

1. Section visible uniquement si `burnout_detecte = true`.
2. Alerte IPP < 25 % bien visible si applicable.
3. Verdict 3 états couleur.
4. Délai instruction 4-6 mois affiché.
5. Gate `isFrance` → bannière.
6. Pré-remplissage IA + `getPrefillCount()`.
7. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu, pré-remplissage, alerte IPP, verdict 3 états, délai instruction.

## Tables / endpoints / composants impactés

- Nouveaux fichiers composant, helper, service.
- Modifié : `TOOL_REGISTRY`.

## Hors périmètre

Backend (→ SF-212-27).
