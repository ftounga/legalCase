# SF-212-36 — Frontend : section « PDV / RCC — conformité »

> Feature F-212. Outil : `F-DT-46-pdv-rcc-conformite`. Contrat API : `SF-212-35` (figé).

## Objectif

Afficher la section permettant à l'avocat de vérifier la conformité d'une RCC ou d'un PDV et de confirmer le droit à l'ARE.

## Comportement nominal

Composant standalone `PdvRccConformiteSectionComponent`, sous `F-DT-46-pdv-rcc-conformite`. Affiché en `CONTEXTUAL` (flag `pdv_rcc_envisage`). **Gate `isFrance`**.

Formulaire (7 champs) : type dispositif (select RCC/PDV), toggles accord majoritaire / validation DREETS / délai 15 j / indemnités ≥ légales / adhésion volontaire / information complète. Bouton « Analyser » → verdict + badge droit ARE confirmé (si RCC conforme) + points d'irrégularité.

**Pré-remplissage IA** depuis `aiData.travailExtractedData.pdvRccDetail`. **Cohérence F-IA-03** sur 4 `critereCode`.

## Cas d'erreur

- Backend 422 → message outil FR-only.
- 4xx/5xx → `MatSnackBar`.

## Critères d'acceptation

1. Section visible uniquement si `pdv_rcc_envisage = true`.
2. Badge droit ARE visible si `droitAreConfirme = true`.
3. Verdict 3 états couleur.
4. Gate `isFrance` → bannière.
5. Pré-remplissage IA + `getPrefillCount()`.
6. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu, pré-remplissage, badge ARE, verdict 3 états, gate `isFrance`.

## Tables / endpoints / composants impactés

- Nouveaux fichiers composant, helper, service.
- Modifié : `TOOL_REGISTRY`.

## Hors périmètre

Backend (→ SF-212-35).
