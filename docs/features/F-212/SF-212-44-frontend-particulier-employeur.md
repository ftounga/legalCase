# SF-212-44 — Frontend : section « particulier employeur — CESU »

> Feature F-212. Outil : `F-DT-108-particuliers-employeurs-cesu`. Contrat API : `SF-212-43` (figé).

## Objectif

Afficher la section permettant à l'avocat de calculer les indemnités dues à un salarié du particulier employeur selon la CCN applicable.

## Comportement nominal

Composant standalone `ParticulierEmployeurCesuSectionComponent`, sous `F-DT-108-particuliers-employeurs-cesu`. Affiché en `CONTEXTUAL` (flag `particulier_employeur_detecte`). **Gate `isFrance`**.

Formulaire (6 champs) : type CCN (select : particulier employeur / assistant maternel), ancienneté (mois), salaire mensuel brut, heures hebdo moyennes, type de rupture (select), CP non pris (jours). Bouton « Analyser » → tableau indemnités (préavis + IL CCN + CP + total) + CCN applicable affichée.

**Pré-remplissage IA** depuis `aiData.travailExtractedData.particulierEmployeurDetail`. **Cohérence F-IA-03** sur 4 `critereCode`.

## Cas d'erreur

- Backend 422 → message outil FR-only.
- 4xx/5xx → `MatSnackBar`.

## Critères d'acceptation

1. Section visible uniquement si `particulier_employeur_detecte = true`.
2. Tableau indemnités correct selon CCN et ancienneté.
3. CCN applicable affichée clairement.
4. Gate `isFrance` → bannière.
5. Pré-remplissage IA + `getPrefillCount()`.
6. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu, pré-remplissage, tableau indemnités 2 CCN, gate `isFrance`.

## Tables / endpoints / composants impactés

- Nouveaux fichiers composant, helper, service.
- Modifié : `TOOL_REGISTRY`.

## Hors périmètre

Backend (→ SF-212-43).
