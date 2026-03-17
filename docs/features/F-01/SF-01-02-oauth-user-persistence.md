# Mini-spec — [F-01 / SF-01-02] Persister l'utilisateur au callback OAuth

## Identifiant

`F-01 / SF-01-02`

## Feature parente

`F-01` — Authentification OAuth2

## Statut

`ready`

## Date de création

2026-03-17

## Branche Git

`feat/SF-01-02-oauth-user-persistence`

---

## Objectif

Lors du callback OAuth2/OIDC réussi, créer ou retrouver l'utilisateur en base de données à partir des claims OIDC, de façon à ce que chaque login soit associé à un enregistrement persisté dans `users` et `auth_accounts`.

---

## Comportement attendu

### Cas nominal

1. L'utilisateur complète le flux OAuth sur Google ou Microsoft.
2. Spring Security reçoit les claims OIDC via le callback.
3. Le `CustomOidcUserService` est invoqué avec l'`OidcUserRequest`.
4. Recherche dans `auth_accounts` par (`provider`, `provider_user_id` = claim `sub`).
5. **Si trouvé** → retourne l'utilisateur existant. Aucune écriture en base.
6. **Si non trouvé** → crée un enregistrement `users` (status=`ACTIVE`) + un enregistrement `auth_accounts`. Retourne le nouvel utilisateur.
7. Spring Security authentifie l'utilisateur avec le `OidcUser` enrichi du `userId` interne.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Claim `sub` absent | Exception → redirect vers `/login?error` | — |
| Claim `email` absent | Exception → redirect vers `/login?error` | — |
| Erreur base de données | Exception → redirect vers `/login?error` | — |

---

## Critères d'acceptation

- [ ] CA-01 : Premier login → un enregistrement `users` est créé avec `email`, `first_name`, `last_name`, `status=ACTIVE`
- [ ] CA-02 : Premier login → un enregistrement `auth_accounts` est créé avec `provider`, `provider_user_id` (= claim `sub`), `provider_email`
- [ ] CA-03 : Deuxième login avec le même compte → aucun doublon en base (lookup par `provider` + `provider_user_id`)
- [ ] CA-04 : Login Google + login Microsoft avec le même email → deux `auth_accounts` distincts, deux `users` distincts (pas de linkage automatique en V1)
- [ ] CA-05 : Claim `sub` absent → exception levée, pas d'écriture en base
- [ ] CA-06 : Claim `email` absent → exception levée, pas d'écriture en base

---

## Périmètre

### Hors scope (explicite)

- Création du workspace (F-02)
- Attribution du rôle OWNER (F-02)
- Linkage automatique de comptes par email entre providers (décision V1 : non fait)
- Mise à jour du profil utilisateur à chaque login
- Endpoint `/api/v1/me` (SF-01-03)

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `users.status` | `ACTIVE` | Imposé à la création |
| `users.created_at` | `NOW()` | Base de données |
| `users.updated_at` | `NOW()` | Base de données |
| `auth_accounts.provider` | `GOOGLE` ou `MICROSOFT` | Déduit du `registrationId` Spring |

---

## Contraintes de validation

| Champ | Obligatoire | Notes |
|-------|-------------|-------|
| `users.email` | Oui | Issu du claim `email` OIDC — non vide |
| `users.first_name` | Non | Issu du claim `given_name` — null si absent |
| `users.last_name` | Non | Issu du claim `family_name` — null si absent |
| `auth_accounts.provider_user_id` | Oui | Claim `sub` OIDC — bloquant si absent |
| `auth_accounts.provider` | Oui | `GOOGLE` ou `MICROSOFT` |
| `auth_accounts.provider_email` | Non | Claim `email` du provider |
| `auth_accounts.access_scope` | Non | Scopes accordés — stockés en String |

**Décision actée (2026-03-17) :** pas de linkage automatique entre providers par email. Chaque combinaison (`provider`, `provider_user_id`) est un compte indépendant.

---

## Technique

### Endpoint(s)

Aucun nouvel endpoint. La logique est déclenchée par Spring Security au callback OAuth via `CustomOidcUserService` injecté dans `SecurityConfig.oauth2Login().userInfoEndpoint()`.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `users` | INSERT + SELECT | Créé au premier login, lu aux suivants |
| `auth_accounts` | INSERT + SELECT | Lookup par `provider` + `provider_user_id` |

### Migration Liquibase

- [x] `002-create-users.xml` — table `users`
- [x] `003-create-auth-accounts.xml` — table `auth_accounts` avec FK sur `users(id)`

### Composants

| Fichier | Package | Rôle |
|---------|---------|------|
| `User.java` | `fr.ailegalcase.auth` | Entité JPA |
| `AuthAccount.java` | `fr.ailegalcase.auth` | Entité JPA |
| `UserRepository.java` | `fr.ailegalcase.auth` | JPA repository |
| `AuthAccountRepository.java` | `fr.ailegalcase.auth` | JPA repository |
| `CustomOidcUserService.java` | `fr.ailegalcase.auth` | Logique find-or-create |

---

## Plan de test

### Tests unitaires

| # | Classe | Description | Résultat attendu |
|---|--------|-------------|-----------------|
| U-01 | `CustomOidcUserServiceTest` | Premier login — claims complets | `users` + `auth_accounts` créés, `OidcUser` retourné |
| U-02 | `CustomOidcUserServiceTest` | Login existant — même `provider` + `sub` | Aucune création, user existant retourné |
| U-03 | `CustomOidcUserServiceTest` | Claim `sub` absent | `OAuth2AuthenticationException` levée |
| U-04 | `CustomOidcUserServiceTest` | Claim `email` absent | `OAuth2AuthenticationException` levée |
| U-05 | `CustomOidcUserServiceTest` | Deux providers, même email | Deux `users` distincts créés |

### Tests d'intégration (repository)

| # | Repository | Description | Résultat attendu |
|---|------------|-------------|-----------------|
| I-01 | `AuthAccountRepository` | `findByProviderAndProviderUserId` — existant | `Optional` présent |
| I-02 | `AuthAccountRepository` | `findByProviderAndProviderUserId` — inconnu | `Optional` vide |
| I-03 | `UserRepository` | `save` + `findById` | User persisté récupéré |

### Isolation workspace

Non applicable — aucun accès à des données liées à un workspace.

### Traçabilité critères d'acceptation

| Critère | Tests couvrants |
|---------|----------------|
| CA-01 — `users` créé au premier login | U-01, I-03 |
| CA-02 — `auth_accounts` créé au premier login | U-01, I-01 |
| CA-03 — Pas de doublon au deuxième login | U-02 |
| CA-04 — Deux providers = deux users | U-05 |
| CA-05 — `sub` absent → exception | U-03 |
| CA-06 — `email` absent → exception | U-04 |

---

## Dépendances

### Subfeatures bloquantes

- SF-01-01 — statut : `done` ✅

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- `CustomOidcUserService` étend `OidcUserService` de Spring Security — compatible Google et Microsoft (tous deux OIDC).
- Le `provider` est déduit du `registrationId` Spring (`google` → `GOOGLE`, `microsoft` → `MICROSOFT`).
- `SecurityConfig.oauth2Login()` sera mis à jour pour injecter le `CustomOidcUserService`.
- La table `users` n'est pas encore liée à `workspaces` — ce lien sera créé en F-02.
