# Mini-spec — F-01 / SF-01-03 Endpoint GET /api/me

---

## Identifiant

`F-01 / SF-01-03`

## Feature parente

`F-01` — Authentification OAuth2

## Statut

`ready`

## Date de création

2026-03-17

## Branche Git

`feat/SF-01-03-api-me-endpoint`

---

## Objectif

Exposer un endpoint `GET /api/me` qui retourne les informations de l'utilisateur authentifié depuis la session OAuth2.

---

## Comportement attendu

### Cas nominal

1. Le client envoie `GET /api/me` avec une session Spring Security active (cookie `JSESSIONID`).
2. Le serveur lit le `OidcUser` depuis le `SecurityContext`.
3. Il retourne un JSON avec les informations de l'utilisateur : `id`, `email`, `firstName`, `lastName`, `provider`.
4. HTTP 200.

Réponse attendue :
```json
{
  "id": "uuid",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "provider": "GOOGLE"
}
```

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Pas de session active (non authentifié) | `{"error":"Unauthorized","message":"Authentication required"}` | 401 |

---

## Critères d'acceptation

- [ ] `GET /api/me` avec session active retourne 200 et les champs `id`, `email`, `firstName`, `lastName`, `provider`
- [ ] `GET /api/me` sans session retourne 401 JSON (via `UnauthorizedEntryPoint`)
- [ ] `id` retourné est celui de la table `users` (UUID persisté), pas le `sub` OAuth2
- [ ] `provider` retourné est le provider OAuth2 utilisé (`GOOGLE` ou `MICROSOFT`)

---

## Périmètre

### Hors scope (explicite)

- Mise à jour des informations utilisateur
- Gestion des rôles / permissions (feature ultérieure)
- Isolation workspace (pas encore de workspace en V1 auth)
- Refresh de session

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/me` | Oui | utilisateur authentifié |

### DTO de réponse

`MeResponse` :
```java
public record MeResponse(UUID id, String email, String firstName, String lastName, String provider) {}
```

### Implémentation

- `MeController` — `@RestController`, injection de `AuthAccountRepository` et `UserRepository`
- Lit le `OidcUser` depuis `@AuthenticationPrincipal`
- Récupère le `User` et l'`AuthAccount` via `AuthAccountRepository.findByProviderAndProviderUserId`
- Retourne le `MeResponse`

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `users` | SELECT | lecture via `UserRepository` |
| `auth_accounts` | SELECT | lookup par `(provider, provider_user_id)` |

### Migration Liquibase

- [x] Non applicable — aucune modification de schéma

---

## Plan de test

### Tests unitaires

- [ ] `MeController` — utilisateur authentifié → retourne `MeResponse` avec les bons champs
- [ ] `MeController` — `AuthAccount` introuvable → lève `OAuth2AuthenticationException`

### Tests d'intégration

- [ ] `GET /api/me` sans session → 401 JSON
- [ ] `GET /api/me` avec `@WithMockUser` → nécessite `OidcUser` → utiliser `@WithMockOidcUser` ou mock manuel

### Isolation workspace

- [x] Non applicable — pas de workspace en V1 auth

---

## Dépendances

### Subfeatures bloquantes

- SF-01-02 — statut : done

### Questions ouvertes impactées

- Aucune

---

## Notes et décisions

- Le `provider` est lu depuis l'`AuthAccount` retrouvé par `(provider, providerUserId)` — pas depuis le token directement.
- En V1, un utilisateur peut avoir plusieurs `AuthAccount` (un par provider) — `GET /api/me` retourne le provider utilisé pour la session en cours.
- L'`id` retourné est le UUID de la table `users`, jamais le `sub` OAuth2.
