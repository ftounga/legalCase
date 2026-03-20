# Mini-spec — F-26 / SF-26-03 Login local + fusion OAuth

## Identifiant

`F-26 / SF-26-03`

## Feature parente

`F-26` — Auth locale (email/mot de passe)

## Statut

`draft`

## Date de création

2026-03-21

## Branche Git

`feat/SF-26-03-login-local-fusion-oauth`

---

## Objectif

Exposer un endpoint public de connexion email/mot de passe qui crée une session Spring Security,
mettre à jour `/api/me` pour supporter les sessions LOCAL, et implémenter la fusion automatique
des comptes : un login OAuth sur un email déjà enregistré en LOCAL attache le compte OAuth à
l'utilisateur existant sans créer de doublon.

---

## Comportement attendu

### Cas nominal — Login local

1. Le client envoie `POST /api/v1/auth/login` avec `{ email, password }`.
2. L'`AuthAccount` LOCAL pour cet email est trouvé.
3. `emailVerified = true` — sinon 403.
4. BCrypt match — sinon 401.
5. Une `UsernamePasswordAuthenticationToken` est créée avec l'email comme principal.
6. Le contexte Spring Security est mis à jour et la session HTTP est sauvegardée.
7. Réponse : `200 OK` — même corps que `/api/me` : `{ id, email, firstName, lastName, provider, isSuperAdmin }`.

### Cas nominal — `/api/me` pour session LOCAL

- Si le principal est un `OidcUser` → flux OAuth existant (inchangé).
- Si le principal est une `UsernamePasswordAuthenticationToken` → look-up via `AuthAccount(LOCAL, email)` → retourne le même `MeResponse`.

### Cas nominal — Fusion OAuth

Quand un utilisateur avec un compte LOCAL se connecte via Google/Microsoft avec le **même email** :
1. `CustomOidcUserService.findOrCreateUser()` détecte que cet `auth_account` OAuth n'existe pas encore.
2. Au lieu de créer un nouvel `User`, il cherche un `User` existant par email.
3. Si trouvé : crée uniquement un nouvel `AuthAccount` OAuth rattaché au `User` existant.
4. Si non trouvé : comportement actuel (crée `User` + `AuthAccount`).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Email inconnu (pas de compte LOCAL) | "Identifiants invalides." | 401 |
| Mot de passe incorrect | "Identifiants invalides." | 401 |
| Email non vérifié (`emailVerified = false`) | "Veuillez valider votre email avant de vous connecter." | 403 |
| Champ email ou password absent | 400 | 400 |

Note : les messages d'erreur ne distinguent pas intentionnellement "email inconnu" de "mauvais mot de passe" (sécurité).

---

## Critères d'acceptation

- [ ] `POST /api/v1/auth/login` est un endpoint public (pas d'auth requise)
- [ ] Login nominal → session créée, 200 avec `{ id, email, firstName, lastName, provider: "LOCAL", isSuperAdmin }`
- [ ] Email inconnu → 401 "Identifiants invalides."
- [ ] Mot de passe incorrect → 401 "Identifiants invalides."
- [ ] `emailVerified = false` → 403 "Veuillez valider votre email avant de vous connecter."
- [ ] Champs manquants → 400
- [ ] `/api/me` retourne les infos correctes pour une session LOCAL
- [ ] Fusion OAuth : login Google/Microsoft sur email LOCAL existant → `User` unique, 2 `auth_accounts`
- [ ] Fusion OAuth : login Google/Microsoft sur email inconnu → comportement actuel (création User)

---

## Périmètre

### Hors scope

- Reset mot de passe (SF-26-04)
- Frontend (SF-26-05)
- Cas "LOCAL tente de créer un compte sur email OAuth existant" — erreur 409 déjà géré en SF-26-02

---

## Contraintes de validation

| Champ | Obligatoire | Format | Notes |
|-------|-------------|--------|-------|
| email | Oui | non vide | normalisé toLowerCase avant recherche |
| password | Oui | non vide | comparé via BCrypt |

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/auth/login` | Non (public) | — |
| GET | `/api/me` | Oui | — (modifié pour LOCAL) |

### Composants Spring

- `LocalLoginRequest` — DTO (record) : email, password
- `LocalAuthController` — ajout de `POST /api/v1/auth/login`
- `LocalAuthService` — ajout de `login(request, HttpServletRequest, HttpServletResponse)`
- `MeController` — gestion `UsernamePasswordAuthenticationToken` en plus de `OidcUser`
- `CustomOidcUserService.findOrCreateUser()` — logique de fusion par email
- `UserRepository` — ajout `findByEmail(String email)`
- `SecurityConfig` — ajout `/api/v1/auth/login` en `permitAll()`

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| auth_accounts | SELECT | vérification credentials LOCAL |
| auth_accounts | INSERT | fusion OAuth — nouvel AuthAccount sur User existant |
| users | SELECT | look-up email pour /api/me LOCAL + fusion OAuth |

### Migration Liquibase

- [ ] Non applicable — schéma SF-26-01 suffit

### Composants Angular (si applicable)

N/A — backend uniquement

---

## Plan de test

### Tests unitaires

- [ ] `LocalAuthService.login()` — nominal : AuthAccount LOCAL trouvé, emailVerified, BCrypt OK → authentication créée
- [ ] `LocalAuthService.login()` — email inconnu → 401
- [ ] `LocalAuthService.login()` — mauvais mot de passe → 401
- [ ] `LocalAuthService.login()` — emailVerified = false → 403
- [ ] `CustomOidcUserService.findOrCreateUser()` — fusion : email LOCAL existant → User existant réutilisé, 2nd AuthAccount créé
- [ ] `CustomOidcUserService.findOrCreateUser()` — pas de fusion : email inconnu → User + AuthAccount créés (comportement actuel)

### Tests d'intégration

- [ ] `POST /api/v1/auth/login` → 200 nominal, session cookie présent
- [ ] `POST /api/v1/auth/login` → 200 puis `GET /api/me` avec session → retourne user LOCAL
- [ ] `POST /api/v1/auth/login` email inconnu → 401
- [ ] `POST /api/v1/auth/login` mauvais mdp → 401
- [ ] `POST /api/v1/auth/login` emailVerified = false → 403
- [ ] `POST /api/v1/auth/login` champ manquant → 400
- [ ] Fusion OAuth : `findOrCreateUser()` sur email LOCAL existant → 1 User, 2 AuthAccounts en base

### Isolation workspace

- [ ] Non applicable — login précède tout workspace

---

## Dépendances

### Subfeatures bloquantes

- SF-26-01 — statut : done ✓
- SF-26-02 — statut : done ✓

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- Même message d'erreur pour "email inconnu" et "mauvais mdp" : bonne pratique sécurité (évite l'énumération d'emails).
- Session Spring Security standard (HttpSession) — cohérent avec le flux OAuth existant.
- `UsernamePasswordAuthenticationToken` utilisé comme principal LOCAL : le nom (`.getName()`) est l'email normalisé, utilisé par `MeController` pour retrouver l'AuthAccount.
- `MeController` : `@AuthenticationPrincipal OidcUser` sera `null` pour les sessions LOCAL → fallback sur `principal.getName()`.
- `UserRepository.findByEmail` : ajout d'une méthode `Optional<User> findByEmail(String email)` (en plus de `existsByEmail` existant).
- Fusion asymétrique : OAuth → LOCAL (fusion) ; LOCAL → OAuth (erreur 409 en SF-26-02).
