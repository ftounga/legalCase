# SF-212-38 — Frontend : section « conciliation CPH — BCO/BCA »

> Feature F-212. Outil : `F-DT-84-conciliation-cph-bca`. Contrat API : `SF-212-37` (figé).

## Objectif

Afficher la section permettant à l'avocat de préparer la phase BCO, calculer le montant minimum BCA et évaluer l'opportunité de la conciliation.

## Comportement nominal

Composant standalone `ConciliationCphBcaSectionComponent`, sous `F-DT-84-conciliation-cph-bca`. Affiché en `CONTEXTUAL` (flag `conciliation_cph_envisagee`). **Gate `isFrance`**.

Formulaire (4 champs) : ancienneté (mois), salaire mensuel brut, montant total demandes, opportunité conciliation (select). Bouton « Analyser » → encadré BCA (palier + montant minimum) + comparaison BCA vs Macron + checklist BCO.

**Pré-remplissage IA** depuis `aiData.travailExtractedData.conciliationCphDetail`. **Cohérence F-IA-03** sur 3 `critereCode`.

## Cas d'erreur

- Backend 422 → message outil FR-only.
- 4xx/5xx → `MatSnackBar`.

## Critères d'acceptation

1. Section visible uniquement si `conciliation_cph_envisagee = true`.
2. Palier BCA et montant affiché.
3. Comparaison BCA vs Macron présente.
4. Checklist BCO non vide.
5. Gate `isFrance` → bannière.
6. Pré-remplissage IA + `getPrefillCount()`.
7. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu, pré-remplissage, palier BCA, checklist non vide.

## Tables / endpoints / composants impactés

- Nouveaux fichiers composant, helper, service.
- Modifié : `TOOL_REGISTRY`.

## Hors périmètre

Backend (→ SF-212-37).
