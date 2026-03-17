# Mini-spec — F-01 / SF-01-04 Déconnexion

---

## Identifiant

`F-01 / SF-01-04`

## Feature parente

`F-01` — Authentification OAuth2

## Statut

`ready`

## Date de création

2026-03-17

## Branche Git

`feat/SF-01-04-logout`

---

## Objectif

Permettre à l'utilisateur authentifié de se déconnecter via `POST /api/logout`, invalidant la session Spring Security côté serveur.

---

## Comportement attendu

### Cas nominal

1. Le client envoie `POST /api/logout` avec une session active.
2. Spring Security invalide la session (`HttpSession.invalidate()`).
3. Le cookie de session est effacé côté client (header `Set-Cookie` avec `Max-Age=0`).
4. Le serveur retourne HTTP 200 avec un corps JSON confirmatoire.

Réponse attendue :
```json
{
  "message": "Logged out successfully"
}
```

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Appel sans session active | 200 — le logout est idempotent |

---

## Critères d'acceptation

- [ ] `POST /api/logout` avec session active → 200 JSON `{"message":"Logged out successfully"}`
- [ ] Après logout, `GET /api/me` avec le même cookie → 401
- [ ] Le logout est idempotent : `POST /api/logout` sans session → 200 (pas de 401 ni 500)
- [ ] Aucune stacktrace dans la réponse

---

## Périmètre

### Hors scope (explicite)

- Révocation du token OAuth2 côté provider (Google/Microsoft) — non requis en V1
- Redirect vers une URL frontend post-logout — traité dans SF-01-05
- Gestion du CSRF sur `/api/logout` — CSRF déjà ignoré sur `/api/**` dans `SecurityConfig`

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Notes |
|---------|-----|------|-------|
| POST | `/api/logout` | Non obligatoire (idempotent) | Gère le cas sans session |

### Implémentation

- Configurer Spring Security `.logout()` avec :
  - `logoutUrl("/api/logout")`
  - `invalidateHttpSession(true)`
  - `deleteCookies("JSESSIONID")`
  - `logoutSuccessHandler` → réponse JSON 200 (pas de redirect)
- Pas de controller dédié — géré par le filtre Spring Security

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable

---

## Plan de test

### Tests unitaires

- Aucun — pas de logique métier, comportement couvert par les tests d'intégration.

### Tests d'intégration

- [ ] `POST /api/logout` avec session active → 200 JSON `{"message":"Logged out successfully"}`
- [ ] `GET /api/me` après logout → 401
- [ ] `POST /api/logout` sans session → 200 (idempotent)

### Isolation workspace

- [x] Non applicable

---

## Dépendances

### Subfeatures bloquantes

- SF-01-01 — statut : done
- SF-01-03 — statut : done (utilisé pour vérifier l'état post-logout)

### Questions ouvertes impactées

- Aucune

---

## Notes et décisions

- Le logout est géré par le filtre Spring Security, pas par un controller — cohérent avec la config existante dans `SecurityConfig`.
- La réponse JSON est produite par un `LogoutSuccessHandler` custom (pas de redirect HTML).
- CSRF déjà ignoré sur `/api/**` — aucune configuration CSRF supplémentaire nécessaire.
