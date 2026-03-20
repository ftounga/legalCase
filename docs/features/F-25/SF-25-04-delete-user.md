# Mini-spec — F-25 / SF-25-04 Suppression utilisateur (super-admin)

## Identifiant

`F-25 / SF-25-04`

## Feature parente

`F-25` — Super-admin plateforme

## Statut

`ready`

## Date de création

2026-03-20

## Branche Git

`feat/SF-25-04-delete-user`

---

## Objectif

Exposer `DELETE /api/v1/super-admin/users/{id}` supprimant un utilisateur de tous ses workspaces,
avec suppression en cascade des workspaces dont il est l'unique OWNER.

---

## Comportement attendu

### Cas nominal

Un super-admin appelle `DELETE /api/v1/super-admin/users/{id}`.

1. Récupérer toutes les memberships de l'utilisateur.
2. Pour chaque workspace où l'utilisateur est OWNER :
   - Si c'est le **seul** OWNER du workspace → supprimer le workspace entier en cascade
     (appel interne à `deleteWorkspace()`).
   - Sinon → supprimer uniquement la membership de l'utilisateur dans ce workspace.
3. Supprimer les memberships restantes de l'utilisateur (rôles non-OWNER ou workspaces
   avec plusieurs OWNERs après l'étape 2).
4. Supprimer les invitations en attente créées par cet utilisateur
   (`workspace_invitations.invited_by_user_id = userId`).
5. Supprimer les `auth_accounts` de l'utilisateur.
6. Supprimer l'utilisateur (`users`).

Retourne `204 No Content`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Utilisateur inexistant | Not found | 404 |
| Non `is_super_admin` | Accès refusé | 403 |
| Non authentifié | Non autorisé | 401 |

---

## Critères d'acceptation

- [ ] `DELETE /api/v1/super-admin/users/{id}` retourne 204 avec un super-admin
- [ ] Un workspace dont l'utilisateur est l'unique OWNER est entièrement supprimé en cascade
- [ ] Un workspace où il existe d'autres OWNERs n'est pas supprimé — seule la membership est retirée
- [ ] Les `auth_accounts` de l'utilisateur sont supprimés
- [ ] L'utilisateur est supprimé de la table `users`
- [ ] 404 si utilisateur inexistant
- [ ] 403 si `is_super_admin = false`

---

## Périmètre

### Hors scope

- Frontend (SF-25-05)
- Suppression des `usage_events` liés à l'utilisateur
  (pas de FK DB → données historiques conservées au niveau workspace)
- Notification de l'utilisateur supprimé

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| DELETE | `/api/v1/super-admin/users/{id}` | Oui | `is_super_admin = true` |

### Tables impactées (ordre de suppression)

| Table | Opération | Notes |
|-------|-----------|-------|
| workspaces (cascade) | DELETE (via `deleteWorkspace`) | Pour chaque workspace dont user est sole OWNER |
| workspace_members | DELETE WHERE user_id = ... | Memberships restantes |
| workspace_invitations | DELETE WHERE invited_by_user_id = ... | Invitations créées par l'utilisateur |
| auth_accounts | DELETE WHERE user_id = ... | |
| users | DELETE WHERE id = ... | |

### Migration Liquibase

- [x] Non applicable

### Nouveaux repository methods requis

- `WorkspaceInvitationRepository.deleteByInvitedByUserId(UUID userId)`
- `AuthAccountRepository.deleteByUser(User user)`

### Composants Angular (si applicable)

N/A — backend uniquement

---

## Plan de test

### Tests unitaires

- [ ] `SuperAdminService.deleteUser()` — utilisateur sole OWNER d'un workspace
  → workspace supprimé en cascade via `deleteWorkspace`
- [ ] `SuperAdminService.deleteUser()` — utilisateur membre non-OWNER
  → membership seule supprimée, workspace intact
- [ ] `SuperAdminService.deleteUser()` — utilisateur OWNER parmi d'autres OWNERs
  → workspace conservé, membership supprimée
- [ ] `SuperAdminService.deleteUser()` — utilisateur inexistant → 404

### Tests d'intégration

- [ ] `DELETE /api/v1/super-admin/users/{id}` → 204, user + auth_accounts supprimés
- [ ] `DELETE /api/v1/super-admin/users/{uuid-inexistant}` → 404
- [ ] `DELETE /api/v1/super-admin/users/{id}` avec non super-admin → 403

### Isolation workspace

- [ ] Non applicable — opération super-admin cross-workspace intentionnelle

---

## Dépendances

### Subfeatures bloquantes

- SF-25-01 — statut : done
- SF-25-02 — statut : done
- SF-25-03 — statut : done

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- `deleteWorkspace()` est réutilisé tel quel pour les workspaces dont l'utilisateur est sole OWNER.
  Il gère déjà la transaction et le fail-open Stripe.
- `usage_events.user_id` n'a pas de FK DB → les events restent (historique workspace).
- `workspace_invitations.invited_by_user_id` n'a pas de FK DB → supprimées par précaution (invitations
  orphelines inutilisables).
- La suppression est atomique dans une `@Transactional` unique, sauf Stripe (fail-open dans `deleteWorkspace`).
