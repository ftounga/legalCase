# SF-212-18 — Frontend : section « rupture anticipée CDD »

> Feature F-212. Outil : `F-DT-43-rupture-anticipee-cdd`. Contrat API : `SF-212-17` (figé).

## Objectif

Afficher la section permettant à l'avocat d'analyser la légalité d'une rupture anticipée de CDD et de calculer les indemnités dues.

## Comportement nominal

Composant standalone `RuptureAnticipeeCddSectionComponent`, sous `F-DT-43-rupture-anticipee-cdd`. Affiché en `CONTEXTUAL` (flag `rupture_anticipee_cdd_detectee`).

Formulaire (6 champs) : auteur de la rupture (select), motif (select), date terme CDD, date rupture effective, salaire mensuel brut, rémunération brute totale contrat. Bouton « Analyser » → verdict (LEGITIME vert / ILLEGITIME_EMPLOYEUR rouge / ILLEGITIME_SALARIE or) + tableau indemnités (rupture anticipée + précarité + total).

**Pré-remplissage IA** depuis `aiData.travailExtractedData.ruptureAnticipeeCddDetail`. **Cohérence F-IA-03** sur 3 `critereCode`.

## Cas d'erreur

- Backend 422 (contrat ≠ CDD) → message « Outil réservé aux dossiers CDD ».
- 4xx/5xx → `MatSnackBar`.

## Critères d'acceptation

1. Section visible uniquement si `rupture_anticipee_cdd_detectee = true`.
2. Tableau indemnités correct selon auteur + motif.
3. Verdict 3 états couleur.
4. Message outil réservé CDD si 422.
5. Pré-remplissage IA + `getPrefillCount()`.
6. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu, pré-remplissage, tableau indemnités 3 motifs, message 422 CDD.

## Tables / endpoints / composants impactés

- Nouveaux fichiers composant, helper, service.
- Modifié : `TOOL_REGISTRY`.

## Hors périmètre

Backend (→ SF-212-17).
