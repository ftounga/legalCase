# Mini-spec — F-02 / SF-02-03 Nom workspace obligatoire à l'onboarding

## Identifiant

`F-02 / SF-02-03`

## Feature parente

`F-02` — Onboarding & workspace

## Statut

`ready`

## Date de création

2026-03-20

## Branche Git

`feat/SF-02-03-onboarding-workspace-name`

---

## Objectif

Forcer le nouvel utilisateur à saisir le nom de son workspace avant d'accéder au dashboard, au lieu de le créer automatiquement avec son adresse email.

---

## Comportement attendu

### Cas nominal

**Flux : nouvel utilisateur (première connexion)**

1. Login OAuth2 → `CustomOidcUserService.createUser()` crée User + AuthAccount **sans workspace**
2. Redirect OAuth2 → `/case-files` → `authGuard` détecte l'absence de workspace (`GET /api/v1/workspaces/current` → 404)
3. `authGuard` redirige vers `/onboarding`
4. `OnboardingComponent` affiche un formulaire : champ "Nom de votre cabinet / workspace" (obligatoire, 2–100 caractères)
5. Soumission → `POST /api/v1/workspaces` avec `{name: "..."}` → backend crée workspace + rôle OWNER + membre `is_primary = true`
6. Redirect vers `/case-files` → dashboard normal

**Flux : utilisateur existant (déjà un workspace)**

- `authGuard` : `GET /api/v1/workspaces/current` → 200 → accès normal
- `/onboarding` avec workspace existant → redirect immédiate vers `/case-files`

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Nom vide ou < 2 caractères | Message d'erreur inline sur le champ, pas de soumission |
| Nom > 100 caractères | Message d'erreur inline |
| Erreur réseau lors de la création | Snackbar erreur, bouton réactivé pour réessayer |
| Utilisateur accède à `/onboarding` alors qu'il a déjà un workspace | Redirect automatique vers `/case-files` |

---

## Critères d'acceptation

- [ ] Un nouvel utilisateur est redirigé vers `/onboarding` après sa première connexion OAuth2
- [ ] Le formulaire d'onboarding valide : nom obligatoire, 2–100 caractères
- [ ] Après soumission valide, l'utilisateur est redirigé vers `/case-files` et son workspace est créé avec le nom saisi
- [ ] Un utilisateur existant (workspace existant) n'est jamais redirigé vers `/onboarding`
- [ ] La route `/onboarding` est accessible sans shell (pas de sidenav)
- [ ] `createDefaultWorkspace` n'est plus appelé lors du login OAuth2

---

## Périmètre

### Hors scope (explicite)

- Modification du nom du workspace après création (feature séparée)
- Onboarding multi-étapes (domaine juridique, etc.)
- Migration des workspaces existants (déjà nommés avec l'email)

---

## Technique

### Endpoints

| Méthode | URL | Auth | Notes |
|---------|-----|------|-------|
| POST | `/api/v1/workspaces` | Oui | Crée le workspace initial — à créer |
| GET | `/api/v1/workspaces/current` | Oui | Déjà existant — utilisé par authGuard |

### Backend

- `CustomOidcUserService.createUser()` : supprimer l'appel à `workspaceService.createDefaultWorkspace(user)`
- Nouveau endpoint `POST /api/v1/workspaces` dans `WorkspaceController` (à créer) avec DTO `CreateWorkspaceRequest { name }`
- `WorkspaceService.createWorkspace(User user, String name)` : logique extraite de `createDefaultWorkspace`
- Validation : `@NotBlank`, `@Size(min=2, max=100)` sur le champ `name`

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| workspaces | INSERT | name fourni par l'utilisateur |
| workspace_members | INSERT | rôle OWNER, is_primary = true |

### Migration Liquibase

- [ ] Non applicable — pas de changement de schéma

### Composants Angular

- `OnboardingComponent` — nouveau, standalone, hors shell (route `/onboarding`)
- `authGuard` — ajout de la détection "pas de workspace" → redirect `/onboarding`

---

## Plan de test

### Tests unitaires

- [ ] `WorkspaceService.createWorkspace()` — nom valide → workspace créé avec OWNER
- [ ] `WorkspaceService.createWorkspace()` — utilisateur a déjà un workspace → exception ou guard côté controller

### Tests d'intégration

- [ ] `POST /api/v1/workspaces` → 201 avec `{name: "Mon Cabinet"}` → workspace créé
- [ ] `POST /api/v1/workspaces` → 400 avec nom vide
- [ ] `POST /api/v1/workspaces` → 400 avec nom > 100 caractères
- [ ] `POST /api/v1/workspaces` — utilisateur a déjà un workspace → 409

### Tests Angular (Karma)

- [ ] `OnboardingComponent` — soumission avec nom valide → appel POST + redirect
- [ ] `OnboardingComponent` — soumission avec nom vide → erreur inline, pas d'appel HTTP
- [ ] `authGuard` — 404 sur `/workspaces/current` → redirect `/onboarding`
- [ ] `authGuard` — 200 sur `/workspaces/current` → accès autorisé

### Isolation workspace

- [ ] Applicable — `POST /api/v1/workspaces` : un user ne peut créer qu'un workspace si `workspace_members` ne contient pas déjà ce user → 409

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- `createDefaultWorkspace` est conservé dans `WorkspaceService` pour compatibilité avec les tests existants mais n'est plus appelé en prod (sauf si besoin de seeds/fixtures)
- L'`authGuard` existant gère déjà le cas non-authentifié (redirect `/login`). On ajoute la logique workspace en aval
- Le workspace Stripe customer est créé de façon fail-open lors du `POST /api/v1/workspaces` (comportement identique à l'actuel)
