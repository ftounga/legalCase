# Mini-spec — F-17 / SF-17-06 Workspace switcher

## Identifiant

`F-17 / SF-17-06`

## Feature parente

`F-17` — Gestion des membres workspace

## Statut

`ready`

## Date de création

2026-03-20

## Branche Git

`feat/SF-17-06-workspace-switcher`

---

## Objectif

Permettre à un utilisateur appartenant à plusieurs workspaces de basculer entre eux depuis le shell, sans avoir à se déconnecter.

---

## Comportement attendu

### Cas nominal

**Flux : switcher le workspace actif**

1. L'utilisateur voit dans le header le nom du workspace actif avec une icône de dropdown
2. Il clique → menu affichant tous ses workspaces (workspace actif coché, autres listés)
3. Il sélectionne un autre workspace → `POST /api/v1/workspaces/{id}/switch`
4. Backend bascule `is_primary = false` sur l'ancien, `is_primary = true` sur le nouveau
5. Frontend recharge : workspace signal mis à jour, navigation vers `/case-files` → les dossiers du nouveau workspace s'affichent

**Flux : un seul workspace**

- Le nom du workspace s'affiche dans le header sans dropdown (ou dropdown non cliquable)

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Workspace cible inexistant | 404 → snackbar erreur |
| Workspace cible non membre | 403 → snackbar erreur |
| Erreur réseau | Snackbar erreur, workspace inchangé |

---

## Critères d'acceptation

- [ ] Un utilisateur avec plusieurs workspaces voit un dropdown dans le header listant ses workspaces
- [ ] Cliquer sur un workspace déclenche le switch → les dossiers du nouveau workspace s'affichent sans rechargement de page
- [ ] Le workspace actif est mis en évidence dans le dropdown (coche ou style différent)
- [ ] Un utilisateur avec un seul workspace ne voit pas de dropdown (juste le nom)
- [ ] Impossible de switcher vers un workspace dont on n'est pas membre (403)

---

## Périmètre

### Hors scope (explicite)

- Création de workspace depuis le switcher (déjà dans SF-02-03)
- Quitter un workspace depuis le switcher
- Réordonner les workspaces

---

## Technique

### Endpoints

| Méthode | URL | Auth | Notes |
|---------|-----|------|-------|
| GET | `/api/v1/workspaces` | Oui | Liste tous les workspaces de l'utilisateur |
| POST | `/api/v1/workspaces/{id}/switch` | Oui | Bascule is_primary vers ce workspace |

### Backend

- `GET /api/v1/workspaces` : `WorkspaceController.list()` → `WorkspaceService.listUserWorkspaces()`
  - Utilise `findByUser(user)` ou `findByUserAndPrimaryTrue` + `findByUserAndPrimaryFalse`
  - Retourne `List<WorkspaceResponse>` avec un champ `primary: boolean`
- `POST /api/v1/workspaces/{id}/switch` : `WorkspaceController.switchWorkspace()`
  - Vérifie que l'utilisateur est membre du workspace cible
  - Bascule `is_primary` : met `false` sur l'actuel, `true` sur la cible
  - Retourne `WorkspaceResponse` du nouveau workspace actif

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| workspace_members | UPDATE | is_primary = false sur l'ancien, true sur le nouveau |

### Migration Liquibase

- [x] Non applicable

### Composants Angular

- `ShellComponent` — dropdown workspace dans le header (visible si > 1 workspace)
- `WorkspaceService` — ajout de `listWorkspaces()` et `switchWorkspace(id)`

---

## Plan de test

### Tests d'intégration backend

- [ ] `GET /api/v1/workspaces` → liste les workspaces de l'utilisateur avec `primary` correct
- [ ] `POST /api/v1/workspaces/{id}/switch` → 200, is_primary basculé
- [ ] `POST /api/v1/workspaces/{id}/switch` → 403 si non membre du workspace cible
- [ ] `POST /api/v1/workspaces/{id}/switch` → 404 si workspace inexistant

### Tests Angular (Karma)

- [ ] `ShellComponent` — 1 workspace → pas de dropdown
- [ ] `ShellComponent` — 2 workspaces → dropdown visible
- [ ] `ShellComponent` — clic sur workspace → `switchWorkspace` appelé + workspace rechargé + navigate `/case-files`
- [ ] `ShellComponent` — erreur switch → snackbar erreur, workspace inchangé

### Isolation workspace

- [ ] Applicable — `POST /api/v1/workspaces/{id}/switch` : refus si l'utilisateur n'est pas membre du workspace cible

---

## Dépendances

### Subfeatures bloquantes

- SF-17-01 (infra is_primary) — statut : done
- SF-02-03 (onboarding) — statut : done

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- `WorkspaceResponse` existant est réutilisé, on ajoute un champ `primary: boolean`
- Le `findByUser(User user)` n'existe pas encore dans `WorkspaceMemberRepository` — à ajouter
- Le switch est atomique : les deux UPDATE (`false` + `true`) sont dans la même transaction
