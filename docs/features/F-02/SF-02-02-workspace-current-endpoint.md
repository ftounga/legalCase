# Mini-spec — F-02 / SF-02-02 Endpoint GET /api/v1/workspaces/current

---

## Identifiant

`F-02 / SF-02-02`

## Feature parente

`F-02` — Onboarding & workspace

## Statut

`ready`

## Date de création

2026-03-17

## Branche Git

`feat/SF-02-02-workspace-current`

---

## Objectif

Exposer un endpoint `GET /api/v1/workspaces/current` retournant le workspace de l'utilisateur authentifié, utilisé par le frontend pour initialiser le contexte applicatif.

---

## Comportement attendu

### Cas nominal

1. Le client envoie `GET /api/v1/workspaces/current` avec une session active.
2. Le serveur récupère l'utilisateur via `@AuthenticationPrincipal OidcUser`.
3. Il cherche le `WorkspaceMember` lié à cet utilisateur via `provider` + `providerUserId`.
4. Retourne le workspace associé.

Réponse attendue :
```json
{
  "id": "uuid",
  "name": "john@example.com",
  "slug": "uuid-slug",
  "planCode": "STARTER",
  "status": "ACTIVE"
}
```

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Non authentifié | `{"error":"Unauthorized","message":"Authentication required"}` | 401 |
| Aucun workspace trouvé (incohérence de données) | `{"error":"Not Found","message":"Workspace not found"}` | 404 |

---

## Critères d'acceptation

- [ ] `GET /api/v1/workspaces/current` avec session active → 200 avec `id`, `name`, `slug`, `planCode`, `status`
- [ ] Sans session → 401 JSON
- [ ] Utilisateur sans workspace → 404 JSON
- [ ] L'`id` retourné est bien celui de la table `workspaces`

---

## Périmètre

### Hors scope (explicite)

- Modification du workspace
- Accès à la liste de tous les workspaces
- Gestion des membres du workspace

---

## Technique

### Endpoint

| Méthode | URL | Auth |
|---------|-----|------|
| GET | `/api/v1/workspaces/current` | Oui |

### DTO de réponse

```java
public record WorkspaceResponse(UUID id, String name, String slug, String planCode, String status) {}
```

### Implémentation

- `WorkspaceController` dans `fr.ailegalcase.workspace`
- `WorkspaceService.getCurrentWorkspace(OidcUser, String provider)` — lookup via `AuthAccountRepository` → `User` → `WorkspaceMemberRepository` → `Workspace`
- `GlobalExceptionHandler` (`@ControllerAdvice`) pour gérer les 404

### Tables impactées

| Table | Opération |
|-------|-----------|
| `auth_accounts` | SELECT |
| `workspace_members` | SELECT |
| `workspaces` | SELECT |

### Migration Liquibase

- [x] Non applicable

---

## Plan de test

### Tests unitaires

- [ ] `WorkspaceService` — utilisateur avec workspace → retourne le workspace
- [ ] `WorkspaceService` — utilisateur sans workspace → lève exception 404

### Tests d'intégration

- [ ] `GET /api/v1/workspaces/current` sans auth → 401 JSON
- [ ] `GET /api/v1/workspaces/current` avec session → 200 avec les bons champs

### Isolation workspace

- [x] Non applicable — un utilisateur retourne uniquement son propre workspace

---

## Dépendances

### Subfeatures bloquantes

- SF-02-01 — statut : done

### Questions ouvertes impactées

- Aucune

---

## Notes et décisions

- `WorkspaceController` introduit le `@ControllerAdvice` global (`GlobalExceptionHandler`) requis par les coding rules — sera réutilisé par toutes les features suivantes.
- Le lookup passe par `AuthAccount → User → WorkspaceMember → Workspace` pour rester cohérent avec le modèle d'identité.
