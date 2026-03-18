# Mini-spec — F-17 / SF-17-02 API REST membres et invitations

---

## Identifiant

`F-17 / SF-17-02`

## Feature parente

`F-17` — Gestion des membres workspace

## Statut

`in-progress`

## Date de création

2026-03-18

## Branche Git

`feat/SF-17-02-api-membres-invitations`

---

## Objectif

Exposer les endpoints REST permettant de lister les membres, inviter un utilisateur (token généré, email différé à SF-17-03), accepter une invitation, révoquer un membre et une invitation.

---

## Comportement attendu

### Cas nominal

**Liste des membres**
`GET /api/v1/workspaces/current/members` → liste des membres du workspace primaire courant avec `userId`, `email`, `firstName`, `lastName`, `memberRole`, `createdAt`.

**Invitation**
`POST /api/v1/workspaces/current/invitations` (OWNER ou ADMIN) → crée une `WorkspaceInvitation` `PENDING` avec token UUID aléatoire, expiration +7 jours. Retourne `201`. Aucun email envoyé.

**Liste des invitations**
`GET /api/v1/workspaces/current/invitations` (OWNER ou ADMIN) → liste des invitations `PENDING` du workspace.

**Révocation invitation**
`DELETE /api/v1/workspaces/current/invitations/{id}` (OWNER ou ADMIN) → passe `status → REVOKED`.

**Acceptation invitation**
`POST /api/v1/workspace/invitations/accept` — body `{ "token": "..." }` → valide le token, vérifie l'email de l'invité = email de l'utilisateur connecté, ajoute le membre, bascule is_primary.

**Révocation membre**
`DELETE /api/v1/workspaces/current/members/{userId}` (OWNER uniquement) → supprime le `WorkspaceMember`.

### Cas d'erreur

| Situation | Comportement | HTTP |
|-----------|-------------|------|
| Invitation PENDING déjà existante pour cet email | Message explicite | 409 |
| Token inexistant | Token invalide | 404 |
| Token expiré ou non PENDING | Token expiré ou déjà utilisé | 409 |
| Email invité ≠ email utilisateur connecté | Accès refusé | 403 |
| Révocation de l'OWNER par lui-même | Interdit | 403 |
| Membre inexistant dans le workspace | Not found | 404 |
| Invitation inexistante ou autre workspace | Not found | 404 |
| ADMIN tente de révoquer un membre | Accès refusé | 403 |
| Rôle invalide dans l'invitation | Bad request | 400 |

---

## Critères d'acceptation

- [ ] `GET /api/v1/workspaces/current/members` retourne la liste des membres du workspace courant
- [ ] `POST /api/v1/workspaces/current/invitations` crée une invitation PENDING avec token UUID et expiry +7j
- [ ] Double invitation PENDING pour le même email → 409
- [ ] `GET /api/v1/workspaces/current/invitations` retourne les invitations PENDING
- [ ] `DELETE /api/v1/workspaces/current/invitations/{id}` passe à REVOKED
- [ ] `POST /api/v1/workspace/invitations/accept` valide le token, ajoute le membre, bascule is_primary
- [ ] Token expiré ou déjà utilisé → 409
- [ ] Email invité ≠ email connecté → 403
- [ ] `DELETE /api/v1/workspaces/current/members/{userId}` supprime le membre
- [ ] Révocation de l'OWNER par lui-même → 403
- [ ] Isolation workspace : impossible d'accéder aux données d'un autre workspace

---

## Périmètre

### Hors scope

- Envoi email (SF-17-03)
- Frontend (SF-17-04)
- Changement de rôle d'un membre existant

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| status (invitation) | PENDING | Toujours à la création |
| expiresAt | now + 7 jours | Calculé côté service |
| token | UUID.randomUUID().toString() | Généré côté service |
| is_primary (nouveau membre) | true | Bascule l'ancien primaire à false |

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs autorisées |
|-------|-------------|---------------------------|
| email (invitation) | Oui | Format email valide |
| role (invitation) | Oui | ADMIN, LAWYER, MEMBER (pas OWNER) |
| token (acceptation) | Oui | Non vide |

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/workspaces/current/members` | Oui | MEMBER |
| POST | `/api/v1/workspaces/current/invitations` | Oui | ADMIN |
| GET | `/api/v1/workspaces/current/invitations` | Oui | ADMIN |
| DELETE | `/api/v1/workspaces/current/invitations/{id}` | Oui | ADMIN |
| POST | `/api/v1/workspace/invitations/accept` | Oui | tout utilisateur connecté |
| DELETE | `/api/v1/workspaces/current/members/{userId}` | Oui | OWNER |

### Tables impactées

| Table | Opération |
|-------|-----------|
| workspace_members | SELECT, INSERT (accept), DELETE |
| workspace_invitations | SELECT, INSERT, UPDATE |
| users | SELECT |

### Composants Angular

N/A — subfeature purement backend.

---

## Plan de test

### Tests unitaires

- [ ] `WorkspaceMemberService` — listMembers : retourne les membres du workspace
- [ ] `WorkspaceInvitationService` — createInvitation : token généré, expiry +7j, status PENDING
- [ ] `WorkspaceInvitationService` — createInvitation : 409 si PENDING existant pour cet email
- [ ] `WorkspaceInvitationService` — acceptInvitation : membre ajouté, is_primary basculé
- [ ] `WorkspaceInvitationService` — acceptInvitation : 409 si token expiré
- [ ] `WorkspaceInvitationService` — acceptInvitation : 409 si status != PENDING
- [ ] `WorkspaceMemberService` — removeMember : 403 si OWNER se révoque lui-même

### Tests d'intégration

- [ ] `GET /api/v1/workspaces/current/members` → 200 avec liste
- [ ] `POST /api/v1/workspaces/current/invitations` → 201 avec payload valide
- [ ] `POST /api/v1/workspaces/current/invitations` → 409 si doublon PENDING
- [ ] `POST /api/v1/workspace/invitations/accept` → 200 avec token valide
- [ ] `POST /api/v1/workspace/invitations/accept` → 409 avec token expiré
- [ ] `DELETE /api/v1/workspaces/current/members/{userId}` → 403 si OWNER se révoque
- [ ] `DELETE /api/v1/workspaces/current/invitations/{id}` → 403 si workspace différent

### Isolation workspace

- [ ] Un utilisateur du workspace A ne peut pas lister les membres du workspace B
- [ ] Token d'invitation valide mais workspace vérifié → 403 si email ne correspond pas

---

## Dépendances

- **SF-17-01** — done

---

## Notes et décisions

- Rôle OWNER non attribuable par invitation.
- Email de l'invité doit correspondre à l'email de l'utilisateur connecté (vérification dans le service).
- Vérification des rôles (ADMIN/OWNER) dans le service, pas via @PreAuthorize.
