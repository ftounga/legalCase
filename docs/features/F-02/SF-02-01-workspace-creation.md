# Mini-spec — F-02 / SF-02-01 Création automatique workspace + rôle OWNER

---

## Identifiant

`F-02 / SF-02-01`

## Feature parente

`F-02` — Onboarding & workspace

## Statut

`ready`

## Date de création

2026-03-17

## Branche Git

`feat/SF-02-01-workspace-creation`

---

## Objectif

Au premier login OAuth2 d'un utilisateur, créer automatiquement un workspace personnel et l'assigner comme membre avec le rôle OWNER.

---

## Comportement attendu

### Cas nominal

1. `CustomOidcUserService.createUser()` crée un nouveau `User`.
2. Après la création du user, `WorkspaceService.createDefaultWorkspace(user)` est appelé.
3. Un `Workspace` est créé avec :
   - `name` = email de l'utilisateur (provisoire — sera modifiable en F-17)
   - `slug` = UUID unique (simplifié en V1)
   - `owner_user_id` = id de l'utilisateur
   - `plan_code` = `"STARTER"`
   - `status` = `"ACTIVE"`
4. Un `WorkspaceMember` est créé avec :
   - `workspace_id` = id du workspace créé
   - `user_id` = id de l'utilisateur
   - `member_role` = `"OWNER"`
5. Au login suivant (user existant) : aucun workspace créé — idempotent.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Erreur en base lors de la création du workspace | Rollback transactionnel — le login échoue avec 500 | 500 |

---

## Critères d'acceptation

- [ ] Au premier login, un `Workspace` et un `WorkspaceMember` (OWNER) sont créés en base
- [ ] Au login suivant (user existant), aucun nouveau workspace n'est créé
- [ ] Le `workspace.owner_user_id` est bien l'id du user créé
- [ ] Le `workspace_member.member_role` est `"OWNER"`
- [ ] La création est transactionnelle (workspace + membre dans la même transaction)

---

## Périmètre

### Hors scope (explicite)

- Personnalisation du nom du workspace (F-17)
- Invitation de membres (F-17)
- Endpoint de consultation du workspace (SF-02-02)
- Facturation / plan (F-16)

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `workspace.name` | email de l'utilisateur | Provisoire, modifiable ultérieurement |
| `workspace.slug` | UUID string | Unique, généré automatiquement |
| `workspace.plan_code` | `STARTER` | Imposé à la création |
| `workspace.status` | `ACTIVE` | Toujours à la création |
| `workspace_member.member_role` | `OWNER` | Toujours pour le créateur |

---

## Contraintes de validation

Aucune contrainte de format sur les champs — tout est généré automatiquement.

---

## Technique

### Package

`fr.ailegalcase.workspace`

### Composants

- `Workspace` — entité JPA
- `WorkspaceMember` — entité JPA
- `WorkspaceRepository` — `JpaRepository<Workspace, UUID>`
- `WorkspaceMemberRepository` — `JpaRepository<WorkspaceMember, UUID>` (PK composite)
- `WorkspaceService` — logique de création
- `CustomOidcUserService` — appel à `WorkspaceService.createDefaultWorkspace(user)` après `userRepository.save(user)`

### Endpoint(s)

Aucun — opération interne déclenchée au login.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `workspaces` | INSERT | Nouvelles migrations 004 |
| `workspace_members` | INSERT | Migration 005 |

### Migration Liquibase

- [ ] `004-create-workspaces.xml`
- [ ] `005-create-workspace-members.xml`

---

## Plan de test

### Tests unitaires

- [ ] `WorkspaceService` — premier login → workspace + membre créés
- [ ] `WorkspaceService` — login existant (user avec workspace existant) → aucune création

### Tests d'intégration

- [ ] `WorkspaceServiceIT` / `@DataJpaTest` — workspace + membre persistés en base
- [ ] Isolation : `workspace.owner_user_id` = id du user créé

### Isolation workspace

- [ ] Non applicable à cette subfeature — la création est automatique au premier login

---

## Dépendances

### Subfeatures bloquantes

- SF-01-02 — statut : done (création user)

### Questions ouvertes impactées

- Aucune

---

## Notes et décisions

- `WorkspaceService` est dans le package `fr.ailegalcase.workspace` — dependency de `auth` vers `workspace` acceptable dans un monolith V1.
- Le `slug` est un UUID string en V1 (pas de slug lisible) — simplification délibérée.
- La clé primaire de `workspace_members` est une PK composite `(workspace_id, user_id)`.
