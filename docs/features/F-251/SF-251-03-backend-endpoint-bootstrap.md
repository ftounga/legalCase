# Mini-spec — F-251 / SF-251-03 — Endpoint super-admin `prospect-bootstrap` + mise à jour skill

## Identifiant

`F-251 / SF-251-03`

## Feature parente

`F-251` — Fiabilisation de la période d'évaluation pour les comptes provisionnés en bypass IHM

## Statut

`ready`

## Date de création

2026-05-25

## Branche Git

`feat/SF-251-03-prospect-bootstrap-backend`

---

## Objectif

Remplacer la chaîne d'`INSERT` SQL directs de la skill `prospect-account-bootstrap` (étape 4) par un endpoint super-admin métier `POST /api/v1/super-admin/prospect-bootstrap` qui orchestre user + workspace + membership + subscription via JPA — bénéficie automatiquement du `@PrePersist` SF-251-02, élimine définitivement la possibilité de recréer le bug Renversez via la skill.

---

## Comportement attendu

### Cas nominal — création complète

Un super-admin authentifié appelle `POST /api/v1/super-admin/prospect-bootstrap` avec `{firstName, lastName, email, password, country, legalDomain, workspaceName}`. Le service :

1. Normalise l'email (`toLowerCase().trim()`)
2. Vérifie qu'aucun `users.email` ne correspond — sinon branche cas B (compte existant)
3. Crée le `User` (`status=ACTIVE`, `firstName`/`lastName` trim)
4. Crée l'`AuthAccount` `provider='LOCAL'`, `providerUserId=email`, `passwordHash=encode(password)`, `emailVerified=true`
5. Appelle `WorkspaceService.createWorkspaceForBootstrappedUser(user, workspaceName, legalDomain, country)` — méthode publique extraite de `createFirstWorkspace` (refactor minimaliste) : crée workspace `ACTIVE` `FREE`, membership OWNER primary, subscription FREE (`@PrePersist` pose `expiresAt = now + 14d`), tente la création Stripe customer (no-op si désactivé)
6. Retourne `201 ProspectBootstrapResponse {userId, workspaceId, workspaceName, expiresAt}`

### Cas nominal — compte existant sans workspace (cas A skill)

Si le `User` existe mais n'a aucun `WorkspaceMember` (compte vierge, ex. Renversez 13/05 avant le 14/05) :

1. UPDATE `auth_accounts.password_hash` pour le compte LOCAL existant (création de l'`AuthAccount` LOCAL si absent — cas OAuth pur)
2. Force `emailVerified=true`
3. Crée workspace + membership + subscription identiques au cas nominal
4. Retourne `201` avec `{userId existant, workspaceId, …}`

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|----------------------|-----------|
| Non authentifié | 401 `Unauthorized` | 401 |
| Authentifié mais `user.isSuperAdmin() == false` | 403 `Forbidden` | 403 |
| Email manquant / invalide (regex simple) | `400 ValidationException` | 400 |
| `firstName` ou `lastName` vide après trim | 400 | 400 |
| `password` < 8 caractères | 400 | 400 |
| `country` ∉ {`FRANCE`,`BELGIQUE`} | 400 | 400 |
| `legalDomain` ∉ {`DROIT_DU_TRAVAIL`,`DROIT_IMMIGRATION`,`DROIT_FAMILLE`} | 400 | 400 |
| `workspaceName` vide après trim | 400 | 400 |
| User existe ET a déjà ≥ 1 `WorkspaceMember` (cas B skill — compte productif) | 409 `Conflict` avec message « Compte déjà actif — ne pas bootstrap » | 409 |
| Erreur Stripe customer create | Loggée WARN, subscription persiste sans `stripeCustomerId`, 201 quand même (pattern identique à `createFirstWorkspace`) | 201 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : non applicable (endpoint admin, pas un outil décisionnel)
- [x] **Autres pays** : `country` est paramètre — FRANCE / BELGIQUE supportés
- [x] **Autres domaines** : `legalDomain` est paramètre — 3 domaines V1
- [x] **Autres UI patterns** : non applicable (SF backend)
- [x] **Autres flows transversaux** :
  - **Auth / Principal** : crée `AuthAccount` LOCAL avec `emailVerified=true` (court-circuite le flow `EmailVerificationToken` de `LocalAuthService.register`) — justifié par la nature bootstrap (super-admin verbalise verbalement le consentement avec le prospect en démo)
  - **Workspace context** : appelle `WorkspaceService.createWorkspaceForBootstrappedUser` (nouvelle méthode publique factorisée de `createFirstWorkspace`)
  - **Plans / limites** : la subscription FREE créée bénéficie du `@PrePersist` SF-251-02 → `expiresAt` posé garanti
  - **Navigation / routing** : non applicable côté backend

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `LocalAuthService.register` | Oui | Pas modifié — le bootstrap utilise un chemin parallèle dédié pour rester explicite (emailVerified=true vs false, pas de mail de vérif). Documentation cross-référence dans Javadoc |
| `WorkspaceService.createFirstWorkspace` (privée) | Oui | Extraite en méthode publique `createWorkspaceForBootstrappedUser` — corps identique, surface API stable |
| `EmailService.sendOnboardingWelcome` | Oui | **Non appelée** lors du bootstrap : l'opérateur envoie son propre mail manuel (skill étape 6) avec identifiants — éviter le double mail |
| `StripeCustomerService.createCustomer` | Oui | Appelée comme dans `createFirstWorkspace`, pattern best-effort |
| Skill `prospect-account-bootstrap.md` | Oui | Mise à jour étape 4 dans cette même SF (commit groupé) |

### Décision

- [x] Étendu à toutes les cibles applicables couvertes par cette SF
- [x] Skill mise à jour bundle dans le commit backend (1 PR)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — SF backend pure (endpoint super-admin + service + tests + doc skill). Aucun composant frontend décisionnel.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — endpoint admin de provisionnement, pas un outil décisionnel.

---

## Critères d'acceptation

- [x] Nouveau endpoint `POST /api/v1/super-admin/prospect-bootstrap` exposé via `SuperAdminController` (pattern `assertSuperAdmin` réutilisé).
- [x] Nouveau service `SuperAdminProspectBootstrapService` injectable, transactionnel.
- [x] Nouveau record DTO `ProspectBootstrapRequest` (Bean Validation `@NotBlank`, `@Email`, `@Size(min=8)`, `@Pattern` sur country/legalDomain).
- [x] Nouveau record DTO `ProspectBootstrapResponse {userId, workspaceId, workspaceName, expiresAt}`.
- [x] Refactor `WorkspaceService` : extraction de `createFirstWorkspace` (privée) en méthode publique `createWorkspaceForBootstrappedUser(User user, String name, String legalDomain, String country)`. Comportement strictement identique — appel interne depuis `createWorkspace` inchangé.
- [x] Tests unitaires `SuperAdminProspectBootstrapServiceTest` (≥ 8 cas — nominal user inexistant / cas A user existant sans workspace / cas B conflit 409 / password trop court / email invalide / pays inconnu / domaine inconnu / workspace name vide).
- [x] Tests d'intégration `SuperAdminProspectBootstrapControllerIT` (≥ 5 cas — 401 / 403 / 201 nominal vérifie subscription.expires_at non null / 409 cas B / 400 validation).
- [x] Skill `ai-skills/prospect-account-bootstrap.md` mise à jour : étape 4 remplacée par appel HTTP (login super-admin → cookie → POST endpoint), commentaire « SF-251-03 ferme le risque NULL ».
- [x] PRODUCT_SPEC.md fiche F-251 mise à jour post-merge : `Terminée 2/2 SF` → `Terminée 3/3 SF` + ligne historique 2026-05-25.
- [x] Smoke build : `./mvnw verify` 100 % vert.
- [x] Skill testée à blanc en dry-run (curl avec session locale dev — pas en prod sans demande explicite).

---

## Périmètre

### Hors scope (explicite)

- Page super-admin frontend (couverte par SF-251-04 parallèle).
- Génération automatique du mot de passe (l'opérateur le choisit, copie-colle dans le formulaire — pattern actuel skill).
- Envoi automatique d'email de bienvenue par le backend (l'opérateur garde la main, skill étape 6).
- Création automatique d'un Stripe customer en mode test si pas en prod — pattern existant best-effort.
- Reset workspace existant (cas B) — si compte productif, on refuse, l'opérateur doit traiter le cas manuellement.
- API token / clé super-admin pour appel CLI sans session — utilisation de la session OAuth/local existante.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `users.status` | `'ACTIVE'` | Pattern `LocalAuthService.register` |
| `auth_accounts.provider` | `'LOCAL'` | Bootstrap = compte local mot de passe |
| `auth_accounts.emailVerified` | `true` | Spécifique bootstrap — l'opérateur vérifie verbalement |
| `auth_accounts.passwordHash` | `passwordEncoder.encode(password)` | BCrypt |
| `workspaces.status` | `ACTIVE` | Pattern `createFirstWorkspace` |
| `workspaces.planCode` | `'FREE'` | Trial démarre |
| `subscriptions.planCode` | `'FREE'` | |
| `subscriptions.status` | `'ACTIVE'` | |
| `subscriptions.startedAt` | `now()` | |
| `subscriptions.expiresAt` | `now() + 14 days` | Posé par le service (et garanti par `@PrePersist` SF-251-02 si oublié) |
| `workspace_members.memberRole` | `'OWNER'` | |
| `workspace_members.isPrimary` | `true` | Pattern `createFirstWorkspace` |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur | Format / valeurs autorisées | Normalisation |
|-------|-------------|----------|----------------------------|---------------|
| firstName | Oui | 1-100 | non vide après trim | `trim()` |
| lastName | Oui | 1-100 | non vide après trim | `trim()` |
| email | Oui | ≤ 320 | regex `@Email` Bean Validation | `toLowerCase().trim()` |
| password | Oui | 8-100 | texte libre (pas de complexité forcée — le prospect le change au 1er login) | aucune |
| country | Oui | — | `FRANCE` ou `BELGIQUE` | upper |
| legalDomain | Oui | — | `DROIT_DU_TRAVAIL` / `DROIT_IMMIGRATION` / `DROIT_FAMILLE` | upper |
| workspaceName | Oui | 1-100 | non vide après trim | `trim()` + upper (cf. SF-60-01 normalisation existante) |

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/super-admin/prospect-bootstrap` | Oui (session) | SUPER_ADMIN (`assertSuperAdmin`) |

**Contrat API figé pour SF-251-04 frontend** :

```
POST /api/v1/super-admin/prospect-bootstrap
Content-Type: application/json
Cookie: SESSION=... (auth super-admin)

Request body :
{
  "firstName": "Marjolaine",
  "lastName": "RENVERSEZ",
  "email": "avocat@renversez.com",
  "password": "printemps2026",
  "country": "FRANCE",
  "legalDomain": "DROIT_DU_TRAVAIL",
  "workspaceName": "RENVERSEZ-MARJOLAINE"
}

Response 201 :
{
  "userId": "42bd8b7e-54e0-4d33-916d-2c6afe881f59",
  "workspaceId": "5d07e421-3e3c-4076-91a1-9ff8e8aaf7b8",
  "workspaceName": "RENVERSEZ-MARJOLAINE",
  "expiresAt": "2026-06-08T01:00:00Z"
}

Erreurs :
- 400 { "message": "<champ> est invalide", "field": "<champ>" }
- 401 (non authentifié)
- 403 (authentifié mais pas super-admin)
- 409 { "message": "Compte déjà actif — ne pas bootstrap", "userId": "<existing-id>" }
```

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `users` | INSERT (ou SELECT si cas A) | `status=ACTIVE` |
| `auth_accounts` | INSERT (ou UPDATE cas A pour `password_hash`) | provider=LOCAL, emailVerified=true |
| `workspaces` | INSERT | via `createWorkspaceForBootstrappedUser` |
| `workspace_members` | INSERT | OWNER, primary |
| `subscriptions` | INSERT | FREE, `@PrePersist` SF-251-02 garantit `expires_at` |

### Migration Liquibase

- [x] Non applicable — aucune modification de schéma.

### Composants Angular (si applicable)

Aucun (couvert par SF-251-04 parallèle).

---

## Plan de test

### Tests unitaires

- [x] `SuperAdminProspectBootstrapServiceTest.bootstrap_userInexistant_creeUserAuthWorkspace`
- [x] `SuperAdminProspectBootstrapServiceTest.bootstrap_userExistantSansWorkspace_resetPasswordEtCreeWorkspace` (cas A)
- [x] `SuperAdminProspectBootstrapServiceTest.bootstrap_userExistantAvecWorkspace_jette409` (cas B)
- [x] `SuperAdminProspectBootstrapServiceTest.bootstrap_passwordCourt_jette400`
- [x] `SuperAdminProspectBootstrapServiceTest.bootstrap_emailInvalide_jette400`
- [x] `SuperAdminProspectBootstrapServiceTest.bootstrap_paysInconnu_jette400`
- [x] `SuperAdminProspectBootstrapServiceTest.bootstrap_domaineInconnu_jette400`
- [x] `SuperAdminProspectBootstrapServiceTest.bootstrap_workspaceNameVide_jette400`
- [x] `SuperAdminProspectBootstrapServiceTest.bootstrap_emailNormalise` (vérifie toLowerCase + trim)

### Tests d'intégration

- [x] `SuperAdminProspectBootstrapControllerIT.bootstrap_nonAuth_jette401`
- [x] `SuperAdminProspectBootstrapControllerIT.bootstrap_authNonSuperAdmin_jette403`
- [x] `SuperAdminProspectBootstrapControllerIT.bootstrap_superAdminPayloadValide_jette201_etSubscriptionExpiresAtNonNull` (le cœur — vérifie que le bug Renversez ne peut plus se reproduire via ce chemin)
- [x] `SuperAdminProspectBootstrapControllerIT.bootstrap_emailDejaActif_jette409`
- [x] `SuperAdminProspectBootstrapControllerIT.bootstrap_passwordTropCourt_jette400`
- [x] `WorkspaceServiceIT.createFirstWorkspace_inchangé` (vérifie que le refactor `createWorkspaceForBootstrappedUser` n'a pas régressé le flow nominal)

### Isolation workspace

- [x] **Non applicable** — endpoint cross-workspace (super-admin), pas d'isolation par workspace courant.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Auth / Principal** — création directe d'`AuthAccount` LOCAL avec `emailVerified=true` (court-circuite `EmailVerificationToken`)
- [x] **Workspace context** — refactor `WorkspaceService` (extraction méthode publique), aucun changement de comportement nominal
- [x] **Plans / limites** — bénéficie de `@PrePersist` SF-251-02, pas de nouvelle logique gates
- [ ] Navigation / routing

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `WorkspaceService.createWorkspace` | Refactor interne (méthode `createFirstWorkspace` privée → appel à `createWorkspaceForBootstrappedUser` publique) — comportement identique | `WorkspaceServiceIT` (`createFirstWorkspace_*` + `createWorkspace_premierWorkspace_*`) reste 100% vert |
| `LocalAuthService.register` | Aucun changement — bootstrap est un chemin parallèle dédié | `LocalAuthServiceTest` / `LocalAuthControllerIT` reste verts |
| `SuperAdminController` | Ajout endpoint, autres endpoints inchangés | `SuperAdminControllerIT` reste vert |
| Smoke E2E auth | Aucun — bootstrap est interne | — |

### Smoke tests E2E concernés

- [x] **Aucun smoke test concerné** — endpoint super-admin interne, pas de flow utilisateur avocat visible.

---

## Dépendances

### Subfeatures bloquantes

- `SF-251-02` — **Terminée 2026-05-20**, hook `@PrePersist` requis pour la garantie `expiresAt`.

### Subfeatures parallèles

- `SF-251-04` (frontend page super-admin) — contrat API figé ci-dessus, peut être développée en parallèle dans worktree isolé.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- **Pourquoi un service dédié `SuperAdminProspectBootstrapService` plutôt qu'une méthode dans `SuperAdminService`** : isolation responsabilité (la logique d'orchestration user+workspace+auth est suffisamment dense pour mériter son service), facilite les tests unitaires.
- **Pourquoi `emailVerified=true` automatique** : le bootstrap se fait avec consentement verbal explicite du prospect en démo (la skill rappelle ce pré-requis). Envoyer un mail de vérification ajoute une étape qui annule l'intérêt du bootstrap (= éliminer la friction de l'activation autonome).
- **Pourquoi pas d'envoi mail backend automatique** : la skill étape 6 montre le mail à l'opérateur avant envoi (validation manuelle des PJ, ton, créneaux RDV). Forcer l'envoi backend retire ce contrôle.
- **Pourquoi conserver le refactor minimaliste de `createFirstWorkspace`** : duplication de code = risque de divergence ultérieure (ex. si on ajoute un side-effect dans `createFirstWorkspace` et qu'on oublie dans le chemin bootstrap). Extraire en méthode publique = source unique de vérité.
- **Skill étape 4 nouvelle version** : login super-admin via `POST /api/v1/auth/login` (compte super-admin LOCAL de l'opérateur), récupération cookie SESSION, puis `POST /api/v1/super-admin/prospect-bootstrap` avec le cookie. Documenté dans la skill comme bloc bash 5 lignes. Le fallback SQL direct est supprimé de la skill (pas de "garder en backup au cas où" — sinon le bug peut revenir).
- **Cas B "compte existant avec workspace" → 409** : choix de la skill — si le prospect a déjà un workspace, c'est qu'il s'est inscrit ailleurs (autre démo, sign-up autonome, etc.), l'opérateur doit comprendre la situation avant d'agir. Pas de reset automatique.
