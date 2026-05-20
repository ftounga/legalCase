# SF-212-26 — Frontend : section « protection du lanceur d'alerte »

> Feature F-212. Outil : `F-DT-61-lanceur-alerte-protection`. Contrat API : `SF-212-25` (figé).

## Objectif

Afficher la section permettant à l'avocat de vérifier le statut de lanceur d'alerte et d'identifier les mesures de représailles et leur nullité.

## Comportement nominal

Composant standalone `LanceurAlerteProtectionSectionComponent`, sous `F-DT-61-lanceur-alerte-protection`. Affiché en `CONTEXTUAL` (flag `lanceur_alerte_detecte`). **Gate `isFrance`** : bannière info si workspace ≠ FRANCE.

Formulaire (6 champs) : nature du signalement (select), contrepartie financière (toggle), procédure (select), référent interne saisi (toggle tri-état), mesure de représaille (toggle), nature mesure. Bouton « Analyser » → verdict (PROTECTION_FORTE vert / PARTIELLE or / HORS_CHAMP gris) + nullité de la mesure si applicable + montant minimum DI (10 000 €).

**Pré-remplissage IA** depuis `aiData.travailExtractedData.lanceurAlerteDetail`. **Cohérence F-IA-03** sur 4 `critereCode`.

## Cas d'erreur

- Backend 422 → message outil FR-only.
- 4xx/5xx → `MatSnackBar`.

## Critères d'acceptation

1. Section visible uniquement si `lanceur_alerte_detecte = true`.
2. Montant minimum DI 10 000 € affiché si protection forte/partielle.
3. Nullité mesure de représaille signalée clairement.
4. Gate `isFrance` → bannière.
5. Pré-remplissage IA + `getPrefillCount()`.
6. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu, pré-remplissage, montant DI, nullité, gate `isFrance`.

## Tables / endpoints / composants impactés

- Nouveaux fichiers composant, helper, service.
- Modifié : `TOOL_REGISTRY`.

## Hors périmètre

Backend (→ SF-212-25).
