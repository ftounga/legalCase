# Mini-spec — F-26 / SF-26-01 Infra auth locale

## Identifiant

`F-26 / SF-26-01`

## Feature parente

`F-26` — Auth locale (email/mot de passe)

## Statut

`draft`

## Date de création

2026-03-21

## Branche Git

`feat/SF-26-01-infra-auth-locale`

---

## Objectif

Mettre en place le schéma de base de données et les entités JPA nécessaires à l'auth locale :
extension de `auth_accounts` pour les comptes LOCAL, et deux nouvelles tables de tokens
(validation email et reset de mot de passe).

---

## Comportement attendu

### Cas nominal

Après migration :
- `auth_accounts` dispose de deux colonnes supplémentaires : `password_hash` (nullable, rempli
  uniquement pour les comptes LOCAL) et `email_verified` (boolean, `true` par défaut pour les
  comptes OAuth existants).
- La table `email_verification_tokens` existe et peut stocker des tokens avec expiration 24h.
- La table `password_reset_tokens` existe et peut stocker des tokens avec expiration 24h.
- Les entités JPA `EmailVerificationToken` et `PasswordResetToken` sont utilisables par les
  services des SF suivantes.

### Cas d'erreur

Aucun comportement métier exposé dans cette subfeature — infra uniquement.

---

## Critères d'acceptation

- [ ] Migration Liquibase 022 appliquée : `password_hash` et `email_verified` ajoutés à `auth_accounts`
- [ ] Les `auth_accounts` OAuth existants ont `email_verified = true` après migration
- [ ] Table `email_verification_tokens` créée avec les colonnes : id, user_id, token (unique), expires_at, used_at
- [ ] Table `password_reset_tokens` créée avec les colonnes : id, user_id, token (unique), expires_at, used_at
- [ ] Entité `AuthAccount` mise à jour : champs `passwordHash` et `emailVerified`
- [ ] Entités `EmailVerificationToken` et `PasswordResetToken` créées
- [ ] Repositories `EmailVerificationTokenRepository` et `PasswordResetTokenRepository` créés
- [ ] Build et tests existants verts après migration

---

## Périmètre

### Hors scope

- Logique métier d'inscription, login, validation, reset (SF-26-02 à SF-26-04)
- Frontend (SF-26-05)
- Index de performance sur les colonnes token (sera ajouté si nécessaire)

---

## Technique

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| auth_accounts | ALTER — ajout colonnes | `password_hash VARCHAR(255) NULL`, `email_verified BOOLEAN NOT NULL DEFAULT TRUE` |
| email_verification_tokens | CREATE | Voir colonnes ci-dessous |
| password_reset_tokens | CREATE | Voir colonnes ci-dessous |

### Colonnes `email_verification_tokens`

| Colonne | Type | Contraintes |
|---------|------|-------------|
| id | UUID | PK |
| user_id | UUID | FK → users.id, NOT NULL |
| token | VARCHAR(255) | UNIQUE, NOT NULL |
| expires_at | TIMESTAMPTZ | NOT NULL |
| used_at | TIMESTAMPTZ | NULL |
| created_at | TIMESTAMPTZ | NOT NULL |

### Colonnes `password_reset_tokens`

| Colonne | Type | Contraintes |
|---------|------|-------------|
| id | UUID | PK |
| user_id | UUID | FK → users.id, NOT NULL |
| token | VARCHAR(255) | UNIQUE, NOT NULL |
| expires_at | TIMESTAMPTZ | NOT NULL |
| used_at | TIMESTAMPTZ | NULL |
| created_at | TIMESTAMPTZ | NOT NULL |

### Logique du champ `email_verified`

- Comptes OAuth (GOOGLE, MICROSOFT) existants et futurs : `email_verified = true`
  (le provider OAuth a déjà vérifié l'email)
- Comptes LOCAL créés : `email_verified = false` jusqu'à clic sur le lien de validation

### Logique du champ `provider` pour les comptes LOCAL

- `provider = 'LOCAL'`
- `providerUserId` = email de l'utilisateur (identifiant stable et unique côté LOCAL)
- `providerEmail` = email de l'utilisateur
- `password_hash` = hash BCrypt du mot de passe

### Migration Liquibase

- [ ] Oui — `022-infra-auth-locale.xml`
- Réversible : les colonnes ajoutées sont nullable ou ont une valeur par défaut

### Composants Angular (si applicable)

N/A — backend uniquement

---

## Plan de test

### Tests unitaires

Aucun — cette subfeature est purement structurelle.

### Tests d'intégration

- [ ] Migration appliquée sur H2 : colonnes présentes sur `auth_accounts`
- [ ] `email_verification_tokens` et `password_reset_tokens` créées et accessibles via JPA
- [ ] Données existantes en `auth_accounts` : `email_verified = true` après migration

### Isolation workspace

- [ ] Non applicable

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- `email_verified DEFAULT TRUE` en migration : les comptes OAuth existants sont considérés vérifiés
  par construction. Les nouveaux comptes LOCAL partiront à `false` via la logique applicative.
- `used_at` sur les tokens permet de les invalider après usage sans les supprimer,
  facilitant l'audit et le débogage.
- Les tokens expirés et utilisés seront nettoyés par un job planifié (hors scope V1 — aucun
  impact fonctionnel, volume faible en V1).
- `providerUserId = email` pour les comptes LOCAL : garantit l'unicité contrainte
  par `auth_accounts(provider, providerUserId)` si une contrainte unique est ajoutée.
