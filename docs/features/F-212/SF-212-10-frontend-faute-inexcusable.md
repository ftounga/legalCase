# SF-212-10 — Frontend : section « faute inexcusable de l'employeur »

> Feature F-212. Outil : `F-DT-91-faute-inexcusable-employeur`. Contrat API : `SF-212-09` (figé).

## Objectif

Afficher la section permettant à l'avocat d'évaluer la faute inexcusable et de visualiser la majoration de rente estimée, avec une alerte sur la procédure (pôle social TJ, non CPH).

## Comportement nominal

Composant standalone `FauteInexcusableEmployeurSectionComponent`, enregistré sous `F-DT-91-faute-inexcusable-employeur`. Affiché en `CONTEXTUAL` (flag `faute_inexcusable_envisagee`).

Formulaire (8 champs) : toggles conscience du danger / signalement / mesures prévention / DUER / formation sécurité, taux IPP (%), rente mensuelle, salaire mensuel brut. Bouton « Analyser » → verdict (PROBABLE vert / POSSIBLE or / PEU_PROBABLE rouge) + facteurs avec fondement + encadré alerte procédure (bannière info constante : « Action devant le pôle social du TJ — non devant le CPH ») + majoration rente estimée.

**Pré-remplissage IA** depuis `aiData.travailExtractedData.fauteInexcusableDetail`. **Cohérence F-IA-03** sur 4 `critereCode`.

## Cas d'erreur

- Backend 422 → message hors domaine.
- 4xx/5xx → `MatSnackBar`.

## Critères d'acceptation

1. Section visible uniquement si `faute_inexcusable_envisagee = true`.
2. Bannière alerte procédure **toujours visible** (pas de condition d'affichage).
3. Verdict couleur correcte.
4. Majoration rente affichée si calculée.
5. Pré-remplissage IA + `getPrefillCount()`.
6. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu, pré-remplissage, bannière alerte procédure, verdict 3 niveaux, majoration rente.

## Tables / endpoints / composants impactés

- Nouveaux fichiers composant, helper, service.
- Modifié : `TOOL_REGISTRY`.

## Hors périmètre

Backend (→ SF-212-09).
