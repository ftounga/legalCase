# SF-212-42 — Frontend : section « VRP — statut et indemnité de clientèle »

> Feature F-212. Outil : `F-DT-104-vrp-statut`. Contrat API : `SF-212-41` (figé).

## Objectif

Afficher la section permettant à l'avocat de vérifier le statut VRP et de calculer l'indemnité de clientèle estimée.

## Comportement nominal

Composant standalone `VrpStatutSectionComponent`, sous `F-DT-104-vrp-statut`. Affiché en `CONTEXTUAL` (flag `statut_vrp_detecte`). **Gate `isFrance`**.

Formulaire (6 champs) : toggles profession de représentation / exclusivité / opérations personnelles absentes, commissions moyennes annuelles (€), années sur la clientèle, type de rupture (select). Bouton « Analyser » → verdict statut (CONFIRME vert / PROBABLE or / IMPROBABLE rouge) + encadré indemnité de clientèle estimée (ou 0 si faute grave) + conditions manquantes.

**Pré-remplissage IA** depuis `aiData.travailExtractedData.vrpDetail`. **Cohérence F-IA-03** sur 4 `critereCode`.

## Cas d'erreur

- Backend 422 → message outil FR-only.
- 4xx/5xx → `MatSnackBar`.

## Critères d'acceptation

1. Section visible uniquement si `statut_vrp_detecte = true`.
2. Encadré indemnité clientèle (0 si faute grave).
3. Conditions manquantes listées si statut non confirmé.
4. Gate `isFrance` → bannière.
5. Pré-remplissage IA + `getPrefillCount()`.
6. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu, pré-remplissage, verdict 3 états, indemnité 0 si faute grave, gate `isFrance`.

## Tables / endpoints / composants impactés

- Nouveaux fichiers composant, helper, service.
- Modifié : `TOOL_REGISTRY`.

## Hors périmètre

Backend (→ SF-212-41).
