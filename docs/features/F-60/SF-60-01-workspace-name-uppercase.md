# Mini-spec — F-60 / SF-60-01 : Normalisation nom workspace en majuscules

---

## Identifiant

`F-60 / SF-60-01`

## Feature parente

`F-60` — Normalisation nom workspace en majuscules

## Statut

`done`

## Date de création

2026-03-28

## Branche Git

`feat/SF-60-01-workspace-name-uppercase`

---

## Objectif

Convertir le nom du workspace en majuscules avant stockage, à la fois côté backend (source de vérité) et côté frontend (feedback immédiat à l'utilisateur).

---

## Comportement attendu

### Cas nominal

1. L'utilisateur saisit `"Mon Cabinet"` dans l'onboarding → stocké `"MON CABINET"` en base
2. L'onboarding affiche le nom en majuscules en temps réel (transformation live sur le champ input)
3. La création via le workspace par défaut OAuth (email comme nom) est aussi normalisée en majuscules

### Cas d'erreur

| Situation | Comportement |
|-----------|-------------|
| Nom vide ou espaces seuls | Comportement inchangé (validation existante) |
| Nom déjà en majuscules | Idempotent, pas de double transformation |

---

## Critères d'acceptation

- [x] `workspace.name` stocké en majuscules en base pour tout nouveau workspace
- [x] Champ nom onboarding transforme en majuscules à la saisie
- [x] `createDefaultWorkspace` (nom = email) stocke en majuscules
- [x] Tests unitaires backend mis à jour

---

## Périmètre

### Dans le scope

- `WorkspaceService.createWorkspace` : `name.strip().toUpperCase()`
- `WorkspaceService.createDefaultWorkspace` : `user.getEmail().toUpperCase()`
- Onboarding frontend : transformation live du champ texte

### Hors scope

- Migration des workspaces existants
- Renommage post-création

---

## Technique

### Fichiers impactés

- `WorkspaceService.java` — `.toUpperCase()` sur `setName` (2 points)
- `onboarding.component.html` — `(input)` event pour uppercase live

### Migration Liquibase

- [x] Non applicable

---

## Plan de test

### Tests unitaires backend

- `WorkspaceServiceTest` : assertion `workspace.getName()` en majuscules après création

### Tests frontend

- Onboarding spec : vérifier que `createWorkspace` reçoit la valeur en majuscules

---

## Analyse d'impact

### Préoccupations transversales

- [ ] Auth / Principal — non touché
- [ ] Workspace context — non touché
- [ ] Plans / limites — non touché
- [ ] Navigation / routing — non touché

### Smoke tests E2E

- [ ] Aucun concerné
