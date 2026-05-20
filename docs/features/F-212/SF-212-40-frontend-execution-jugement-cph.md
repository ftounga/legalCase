# SF-212-40 — Frontend : section « exécution jugement CPH — AGS »

> Feature F-212. Outil : `F-DT-88-execution-jugement-cph`. Contrat API : `SF-212-39` (figé).

## Objectif

Afficher la section permettant à l'avocat d'analyser les voies d'exécution d'un jugement prud'homal et de calculer les montants garantis par l'AGS.

## Comportement nominal

Composant standalone `ExecutionJugementCphSectionComponent`, sous `F-DT-88-execution-jugement-cph`. Affiché en `CONTEXTUAL` (flag `execution_jugement_cph_detectee`). **Gate `isFrance`**.

Formulaire (7 champs) : procédure collective (toggle), type de procédure (select), créances totales, salaire mensuel brut, créances salariales 3 mois, jugement prononcé (toggle), date jugement. Bouton « Analyser » → verdict + encadré AGS (applicable / non applicable + plafond + montant garanti) + indicateur exécution provisoire de droit.

**Pré-remplissage IA** depuis `aiData.travailExtractedData.executionJugementDetail`. **Cohérence F-IA-03** sur 4 `critereCode`.

## Cas d'erreur

- Backend 422 → message outil FR-only.
- 4xx/5xx → `MatSnackBar`.

## Critères d'acceptation

1. Section visible uniquement si `execution_jugement_cph_detectee = true`.
2. Encadré AGS visible si `agsApplicable = true` avec plafond et montant garanti.
3. Indicateur exécution provisoire de droit affiché si jugement post-2020.
4. Gate `isFrance` → bannière.
5. Pré-remplissage IA + `getPrefillCount()`.
6. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu, pré-remplissage, encadré AGS, indicateur exécution provisoire.

## Tables / endpoints / composants impactés

- Nouveaux fichiers composant, helper, service.
- Modifié : `TOOL_REGISTRY`.

## Hors périmètre

Backend (→ SF-212-39).
