# Mini-spec — F-IM-07 / SF-IM-07-02 Endpoint analyse droit au travail

## Identifiant
`F-IM-07 / SF-IM-07-02`

## Feature parente
`F-IM-07` — Analyse droit au travail du demandeur

## Branche Git
`feat/SF-IM-07-02-endpoint-work-right`

## Objectif
Endpoint POST (titreType, country) → résout le droit au travail via le référentiel, persiste (upsert 1:1), retourne le résultat. GET retourne l'analyse existante.

## Endpoints
- `POST /api/v1/case-files/{id}/immigration/work-right`
- `GET /api/v1/case-files/{id}/immigration/work-right`

## Critères d'acceptation
- POST résout et persiste, GET retourne l'existant
- Isolation workspace, domaine DROIT_IMMIGRATION
- Titre inconnu → 400, upsert fonctionne
- France + Belgique

## Dépendances
- SF-IM-07-01 — done
