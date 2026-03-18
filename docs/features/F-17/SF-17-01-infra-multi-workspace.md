# Mini-spec — F-17 / SF-17-01 Infrastructure multi-workspace

> Statut : done

---

## Identifiant

`F-17 / SF-17-01`

## Feature parente

`F-17` — Gestion des membres workspace

## Statut

`done`

## Date de création

2026-03-18

## Branche Git

`feat/SF-17-01-infra-multi-workspace`

---

## Objectif

Poser les fondations du modèle multi-workspace : migration Liquibase ajoutant `is_primary` sur `workspace_members`, création de la table `workspace_invitations`, et remplacement de `findFirstByUser` par `findByUserAndPrimaryTrue` dans tous les services.

---

## Comportement attendu

### Cas nominal

- La colonne `is_primary` est ajoutée à `workspace_members` avec `DEFAULT TRUE` pour les membres existants.
- La table `workspace_invitations` est créée avec ses colonnes, FK, et index.
- Tout accès au workspace courant d'un utilisateur passe par `findByUserAndPrimaryTrue`.
- La création d'un workspace par défaut (lors du premier login) positionne `is_primary = true` sur le membre créé.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Utilisateur sans workspace primaire | 404 Workspace not found | 404 |

---

## Critères d'acceptation

- [x] Migration 018 : colonne `is_primary` ajoutée à `workspace_members`, défaut `true`
- [x] Migration 019 : table `workspace_invitations` créée avec FK vers `workspaces` et `users`
- [x] `WorkspaceMember.primary` (boolean) — Lombok génère `isPrimary()` / `setPrimary()`
- [x] `WorkspaceMemberRepository.findByUserAndPrimaryTrue(User)` remplace `findFirstByUser`
- [x] Tous les services (9) utilisent `findByUserAndPrimaryTrue`
- [x] Tous les tests (unitaires + IT) mis à jour et verts

---

## Périmètre

### Hors scope

- Invitation par email (SF-17-02 et SF-17-03)
- Workspace switcher frontend (SF-17-04)
- API REST membres/invitations (SF-17-02)

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| is_primary | true | Premier workspace d'un utilisateur = primaire |
| status (invitation) | PENDING | Toute invitation commence PENDING |

---

## Technique

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| workspace_members | ALTER TABLE | Ajout colonne is_primary BOOLEAN NOT NULL DEFAULT TRUE |
| workspace_invitations | CREATE TABLE | Nouvelle table avec FK workspaces + users |

### Migration Liquibase

- [x] `018-add-is-primary-to-workspace-members.xml`
- [x] `019-create-workspace-invitations.xml`

### Composants Angular

N/A — subfeature purement backend.

---

## Plan de test

### Tests unitaires

- [x] `WorkspaceServiceGetCurrentTest` — cas nominal : workspace primaire retourné
- [x] `WorkspaceServiceGetCurrentTest` — cas d'erreur : pas de workspace primaire → 404
- [x] Tous les services mockent `findByUserAndPrimaryTrue`

### Tests d'intégration

- [x] Tests IT existants mis à jour : `WorkspaceMember.setPrimary(true)` dans les fixtures

### Isolation workspace

- [x] Applicable — `findByUserAndPrimaryTrue` garantit que seul le workspace primaire de l'utilisateur est retourné

---

## Dépendances

### Subfeatures bloquantes

- Aucune

### Questions ouvertes impactées

- Modèle multi-workspace : is_primary flag — tranché le 2026-03-18
- Invitation workspace : email avec token — tranché le 2026-03-18

---

## Notes et décisions

- Le champ JPA est nommé `primary` (boolean) et non `isPrimary` pour éviter que Lombok génère `isIsPrimary()`. Lombok génère `isPrimary()` / `setPrimary()` correctement depuis `primary`.
- Spring Data JPA : le champ s'appelant `primary`, la méthode dérivée doit être `findByUserAndPrimaryTrue` (et non `findByUserAndIsPrimaryTrue`).
