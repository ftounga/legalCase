# Mini-spec — F-26 / SF-26-02 Inscription et validation email

## Identifiant

`F-26 / SF-26-02`

## Feature parente

`F-26` — Auth locale (email/mot de passe)

## Statut

`draft`

## Date de création

2026-03-21

## Branche Git

`feat/SF-26-02-inscription-validation-email`

---

## Objectif

Exposer un endpoint public d'inscription email/mot de passe qui crée l'utilisateur et son compte LOCAL,
génère un token de validation email (24h) et envoie l'email de confirmation.
Exposer un second endpoint public pour valider l'email via le token reçu.

---

## Comportement attendu

### Cas nominal — Inscription

1. Le client envoie `POST /api/v1/auth/register` avec `{ firstName, lastName, email, password }`.
2. Le service valide les champs (voir contraintes).
3. L'email n'existe pas encore dans `users` — sinon 409.
4. Un `User` est créé (`status=ACTIVE`).
5. Un `AuthAccount` LOCAL est créé :
   - `provider = "LOCAL"`
   - `providerUserId = email` (minuscule normalisé)
   - `providerEmail = email` (minuscule normalisé)
   - `passwordHash = BCrypt(password)`
   - `emailVerified = false`
6. Un `EmailVerificationToken` est créé : token UUID aléatoire, `expiresAt = now + 24h`.
7. Un email est envoyé à l'adresse fournie avec le lien de validation.
8. Réponse : `201 Created` — corps vide.

### Cas nominal — Validation email

1. Le client appelle `GET /api/v1/auth/verify-email?token=xxx`.
2. Le token est trouvé, non utilisé, non expiré.
3. `usedAt` est renseigné sur le token.
4. `emailVerified = true` sur l'`AuthAccount` LOCAL correspondant.
5. Réponse : `200 OK` — `{ "message": "Email validé avec succès." }`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Champ obligatoire absent (firstName, lastName, email, password) | 400 | 400 |
| Format email invalide | 400 | 400 |
| Mot de passe < 8 caractères | 400 | 400 |
| Email déjà utilisé (compte existant quelconque) | Message : "Cet email est déjà utilisé. Si vous avez un compte Google ou Microsoft associé, connectez-vous via le bouton correspondant." | 409 |
| Token inconnu | 400 | 400 |
| Token déjà utilisé (`usedAt != null`) | 400 | 400 |
| Token expiré (`expiresAt < now`) | 400 | 400 |
| Envoi email échoue | fail-open — inscription réussit, email non envoyé loggé en ERROR | 201 |

---

## Critères d'acceptation

- [ ] `POST /api/v1/auth/register` est un endpoint public (pas d'auth requise)
- [ ] Inscription nominale → User + AuthAccount LOCAL créés, emailVerified = false, token 24h créé, email envoyé, 201
- [ ] Email déjà présent dans users → 409
- [ ] Champs manquants → 400
- [ ] Email format invalide → 400
- [ ] Password < 8 caractères → 400
- [ ] `GET /api/v1/auth/verify-email?token=xxx` est un endpoint public
- [ ] Token valide → emailVerified = true, usedAt renseigné, 200
- [ ] Token inconnu → 400
- [ ] Token déjà utilisé → 400
- [ ] Token expiré → 400
- [ ] Fail-open email : inscription réussit même si l'envoi de l'email échoue

---

## Périmètre

### Hors scope

- Login local (SF-26-03)
- Fusion compte OAuth existant (SF-26-03)
- Reset mot de passe (SF-26-04)
- Frontend (SF-26-05)
- Renvoi manuel de l'email de validation (non prévu en V1)
- Nettoyage des tokens expirés (hors scope V1)

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| User.status | ACTIVE | Immédiatement actif — l'accès est bloqué via emailVerified |
| AuthAccount.provider | LOCAL | Fixe |
| AuthAccount.emailVerified | false | Jusqu'à clic sur le lien |
| EmailVerificationToken.expiresAt | now + 24h | Calculé à la création |
| EmailVerificationToken.usedAt | null | Renseigné à la validation |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format | Unicité | Normalisation |
|-------|-------------|-------------|--------|---------|---------------|
| firstName | Oui | 100 | non vide après trim | Non | trim() |
| lastName | Oui | 100 | non vide après trim | Non | trim() |
| email | Oui | 255 | format email valide | Oui (users.email) | toLowerCase().trim() |
| password | Oui | 72 (BCrypt) | min 8 caractères | Non | — |

Notes :
- Le password n'est jamais stocké en clair — BCrypt uniquement.
- L'email est normalisé en minuscules avant toute vérification d'unicité.
- La vérification d'unicité porte sur `users.email` (tous providers confondus).

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/auth/register` | Non (public) | — |
| GET | `/api/v1/auth/verify-email` | Non (public) | — |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| users | INSERT | Nouveau user LOCAL |
| auth_accounts | INSERT | provider=LOCAL, emailVerified=false |
| email_verification_tokens | INSERT | Token UUID, 24h |

### Composants Spring

- `LocalAuthController` — contrôleur dédié à l'auth locale (register + verify-email)
- `LocalAuthService` — logique inscription + validation
- `RegisterRequest` — DTO (record) : firstName, lastName, email, password
- `LocalAuthService` réutilise `EmailService` existant pour l'envoi (nouvelle méthode `sendEmailVerification`)
- `SecurityConfig` — ajout des deux routes en `permitAll()`

### Migration Liquibase

- [ ] Non applicable — schéma créé en SF-26-01

### Composants Angular (si applicable)

N/A — backend uniquement

---

## Plan de test

### Tests unitaires

- [ ] `LocalAuthService` — inscription nominale : User + AuthAccount + Token créés, email envoyé
- [ ] `LocalAuthService` — email déjà utilisé → `EmailAlreadyUsedException` (409)
- [ ] `LocalAuthService` — password < 8 chars → `ValidationException` (400)
- [ ] `LocalAuthService` — fail-open email : EmailService.send() lève exception → inscription réussit
- [ ] `LocalAuthService` — validateEmail token valide → emailVerified = true, usedAt renseigné
- [ ] `LocalAuthService` — validateEmail token expiré → exception 400
- [ ] `LocalAuthService` — validateEmail token déjà utilisé → exception 400
- [ ] `LocalAuthService` — validateEmail token inconnu → exception 400

### Tests d'intégration

- [ ] `POST /api/v1/auth/register` → 201 nominal (User + AuthAccount créés en base)
- [ ] `POST /api/v1/auth/register` → 409 email déjà existant
- [ ] `POST /api/v1/auth/register` → 400 champ manquant
- [ ] `POST /api/v1/auth/register` → 400 password < 8 chars
- [ ] `POST /api/v1/auth/register` → 400 format email invalide
- [ ] `GET /api/v1/auth/verify-email?token=xxx` → 200 token valide (emailVerified = true en base)
- [ ] `GET /api/v1/auth/verify-email?token=inconnu` → 400
- [ ] `GET /api/v1/auth/verify-email?token=expiré` → 400
- [ ] `GET /api/v1/auth/verify-email?token=déjà-utilisé` → 400
- [ ] Les deux endpoints sont accessibles sans authentification

### Isolation workspace

- [ ] Non applicable — inscription précède tout workspace, pas de filtre workspace_id ici

---

## Dépendances

### Subfeatures bloquantes

- SF-26-01 — statut : done ✓

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- `LocalAuthController` distinct de l'existant `MeController`/`SecurityConfig` pour isoler
  le code de l'auth locale.
- `EmailService` réutilisé (déjà testé en SF-17-03) via une nouvelle méthode `sendEmailVerification`.
- Fail-open sur l'email : cohérent avec la politique existante du projet (SF-17-03).
- `User.status = ACTIVE` dès l'inscription : l'accès applicatif sera conditionné à `emailVerified = true`
  dans SF-26-03 (login local vérifie emailVerified avant d'autoriser la session).
- BCrypt via `PasswordEncoder` Spring Security (`BCryptPasswordEncoder`) — déjà dans le classpath.
- Token de validation : `UUID.randomUUID().toString()` — suffisamment aléatoire pour V1.
- L'URL du lien dans l'email : `${app.base-url}/verify-email?token=xxx` — propriété de config à ajouter.
