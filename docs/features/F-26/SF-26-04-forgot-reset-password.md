# Mini-spec — F-26 / SF-26-04 Mot de passe oublié / reset

## Identifiant

`F-26 / SF-26-04`

## Feature parente

`F-26` — Auth locale (email/mot de passe)

## Statut

`draft`

## Date de création

2026-03-21

## Branche Git

`feat/SF-26-04-forgot-reset-password`

---

## Objectif

Exposer deux endpoints publics permettant à un utilisateur LOCAL d'initier une réinitialisation
de mot de passe (envoi d'un lien par email valable 24h) et de soumettre le nouveau mot de passe
via le token reçu.

---

## Comportement attendu

### Cas nominal — Mot de passe oublié

1. Le client envoie `POST /api/v1/auth/forgot-password` avec `{ email }`.
2. Si un compte LOCAL existe pour cet email : un `PasswordResetToken` est créé (UUID, 24h) et
   un email est envoyé avec le lien de reset.
3. Si aucun compte LOCAL n'existe pour cet email : aucune action, même réponse (fail-silent).
4. Réponse : `200 OK` — `{ "message": "Si un compte existe pour cet email, vous recevrez un lien de réinitialisation." }` (toujours, quelle que soit l'existence du compte).

### Cas nominal — Réinitialisation du mot de passe

1. Le client envoie `POST /api/v1/auth/reset-password` avec `{ token, newPassword }`.
2. Le token est trouvé, non utilisé, non expiré.
3. `usedAt` est renseigné sur le token.
4. `passwordHash` est mis à jour sur l'`AuthAccount` LOCAL correspondant via BCrypt.
5. Réponse : `200 OK` — `{ "message": "Mot de passe réinitialisé avec succès." }`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Email absent dans forgot-password | 400 | 400 |
| Compte LOCAL inexistant pour l'email | Même réponse 200 (fail-silent, anti-énumération) | 200 |
| Envoi email échoue | fail-open — 200 retourné, erreur loggée | 200 |
| Token inconnu dans reset-password | 400 | 400 |
| Token déjà utilisé | 400 | 400 |
| Token expiré | 400 | 400 |
| newPassword absent ou < 8 caractères | 400 | 400 |

---

## Critères d'acceptation

- [ ] `POST /api/v1/auth/forgot-password` est un endpoint public
- [ ] Email LOCAL existant → PasswordResetToken créé en base, email envoyé, 200
- [ ] Email inconnu → 200 (même message, pas de token créé)
- [ ] Champ email absent → 400
- [ ] Fail-open email : 200 retourné même si envoi échoue
- [ ] `POST /api/v1/auth/reset-password` est un endpoint public
- [ ] Token valide + newPassword ≥ 8 chars → passwordHash mis à jour, usedAt renseigné, 200
- [ ] Token inconnu → 400
- [ ] Token déjà utilisé → 400
- [ ] Token expiré → 400
- [ ] newPassword absent ou < 8 chars → 400

---

## Périmètre

### Hors scope

- Login (SF-26-03)
- Frontend (SF-26-05)
- Invalidation des sessions actives lors du reset (hors V1)
- Limite du nombre de demandes de reset par heure (hors V1)

---

## Contraintes de validation

| Champ | Obligatoire | Format | Notes |
|-------|-------------|--------|-------|
| email (forgot) | Oui | non vide | normalisé toLowerCase |
| token (reset) | Oui | non vide | UUID string |
| newPassword | Oui | min 8, max 72 | BCrypt |

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/auth/forgot-password` | Non (public) | — |
| POST | `/api/v1/auth/reset-password` | Non (public) | — |

### Composants Spring

- `ForgotPasswordRequest` — DTO (record) : email
- `ResetPasswordRequest` — DTO (record) : token, newPassword
- `LocalAuthController` — ajout des deux endpoints
- `LocalAuthService` — ajout `forgotPassword()` et `resetPassword()`
- `EmailService` — ajout `sendPasswordReset(String toEmail, String token)`
- `SecurityConfig` — ajout des deux routes en `permitAll()`

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| auth_accounts | SELECT | cherche compte LOCAL par email |
| password_reset_tokens | INSERT | forgot-password |
| password_reset_tokens | SELECT + UPDATE | reset-password (usedAt) |
| auth_accounts | UPDATE | passwordHash mis à jour |

### Migration Liquibase

- [ ] Non applicable — schéma SF-26-01 suffit

### Composants Angular (si applicable)

N/A — backend uniquement

---

## Plan de test

### Tests unitaires

- [ ] `LocalAuthService.forgotPassword()` — compte LOCAL existant → token créé, email envoyé
- [ ] `LocalAuthService.forgotPassword()` — email inconnu → 200 silencieux, aucun token créé
- [ ] `LocalAuthService.forgotPassword()` — fail-open email : réussit même si envoi échoue
- [ ] `LocalAuthService.resetPassword()` — token valide → passwordHash mis à jour, usedAt renseigné
- [ ] `LocalAuthService.resetPassword()` — token inconnu → 400
- [ ] `LocalAuthService.resetPassword()` — token déjà utilisé → 400
- [ ] `LocalAuthService.resetPassword()` — token expiré → 400

### Tests d'intégration

- [ ] `POST /api/v1/auth/forgot-password` email LOCAL → 200, token créé en base
- [ ] `POST /api/v1/auth/forgot-password` email inconnu → 200, aucun token créé
- [ ] `POST /api/v1/auth/forgot-password` email absent → 400
- [ ] `POST /api/v1/auth/reset-password` token valide → 200, passwordHash mis à jour en base
- [ ] `POST /api/v1/auth/reset-password` token inconnu → 400
- [ ] `POST /api/v1/auth/reset-password` token expiré → 400
- [ ] `POST /api/v1/auth/reset-password` token déjà utilisé → 400
- [ ] `POST /api/v1/auth/reset-password` newPassword trop court → 400
- [ ] Les deux endpoints accessibles sans auth

### Isolation workspace

- [ ] Non applicable — reset password est pré-workspace

---

## Dépendances

### Subfeatures bloquantes

- SF-26-01 — statut : done ✓
- SF-26-02 — statut : done ✓
- SF-26-03 — statut : done ✓

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- Réponse 200 identique que l'email existe ou non (fail-silent) : prévient l'énumération d'emails.
- Fail-open email : cohérent avec SF-26-02 et la politique projet.
- Un seul token actif par user n'est pas contraint en V1 (volume faible) — plusieurs tokens
  peuvent coexister, seul le dernier lien envoyé sera utilisable si les autres expirent.
- `EmailService.sendPasswordReset` re-throw comme `sendEmailVerification` — le service le catch.
