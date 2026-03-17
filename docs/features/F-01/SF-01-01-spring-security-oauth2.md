# Mini-spec — [F-01 / SF-01-01] Configurer Spring Security OAuth2/OIDC

## Identifiant

`F-01 / SF-01-01`

## Feature parente

`F-01` — Authentification OAuth2

## Statut

`ready`

## Date de création

2026-03-17

## Branche Git

`feat/SF-01-01-spring-security-oauth2`

---

## Objectif

Configurer Spring Security pour protéger les endpoints de l'API REST et déléguer l'authentification aux providers OAuth2/OIDC Google et Microsoft, sans aucun mot de passe local.

---

## Comportement attendu

### Cas nominal

1. L'application Spring Boot démarre avec Spring Security configuré en mode OAuth2 Login.
2. Les providers Google et Microsoft sont enregistrés via `spring.security.oauth2.client` dans `application.yml`.
3. Les credentials (client-id, client-secret) sont externalisés en variables de configuration — jamais hardcodés.
4. Tous les endpoints `/api/**` exigent une authentification.
5. Les URLs OAuth2 sont publiques : `/oauth2/authorization/**`, `/login/oauth2/code/**`.
6. Un endpoint `/api/**` appelé sans authentification retourne **401 JSON** — pas de redirect HTML.
7. La configuration fonctionne sur les profils `dev` (H2) et `prod` (PostgreSQL) sans modification de code.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Appel `/api/**` sans token | 401 JSON (pas de redirect) | 401 |
| Provider OAuth non configuré (client-id absent) | Erreur au démarrage de l'app (fail-fast) | — |
| Endpoint inexistant sans auth | 401 (l'auth est vérifiée avant le routing) | 401 |

---

## Critères d'acceptation

- [ ] CA-01 : L'application démarre sans erreur avec la configuration OAuth2 (Google + Microsoft déclarés)
- [ ] CA-02 : `GET /api/v1/me` sans authentification retourne 401 avec un corps JSON (pas de page HTML de login)
- [ ] CA-03 : `GET /oauth2/authorization/google` est accessible sans authentification et retourne une redirection (302) vers Google
- [ ] CA-04 : `GET /oauth2/authorization/microsoft` est accessible sans authentification et retourne une redirection (302) vers Microsoft
- [ ] CA-05 : Aucun client-id ni client-secret OAuth n'est présent en dur dans le code source
- [ ] CA-06 : Le profil `dev` (H2) démarre correctement avec la configuration OAuth2

---

## Périmètre

### Hors scope (explicite)

- Création ou lookup de l'utilisateur en base de données au callback OAuth (SF-01-02)
- Endpoint `/api/v1/me` (SF-01-03)
- Page de login Angular (SF-01-04)
- Guard Angular et interceptor HTTP (SF-01-05)
- Création du workspace utilisateur (F-02)
- Magic link email (non décidé pour V1)
- SSO entreprise (V2+)

---

## Valeurs initiales

Non applicable — cette subfeature ne crée pas d'entité en base.

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs | Notes |
|-------|-------------|-----------------|-------|
| `client-id` Google | Oui | String non vide | Externalisé en variable d'env |
| `client-secret` Google | Oui | String non vide | Externalisé en variable d'env |
| `client-id` Microsoft | Oui | String non vide | Externalisé en variable d'env |
| `client-secret` Microsoft | Oui | String non vide | Externalisé en variable d'env |

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth requise | Notes |
|---------|-----|-------------|-------|
| GET | `/oauth2/authorization/google` | Non | Initie le flux OAuth Google — géré par Spring |
| GET | `/oauth2/authorization/microsoft` | Non | Initie le flux OAuth Microsoft — géré par Spring |
| GET | `/login/oauth2/code/*` | Non | Callback OAuth — géré par Spring |
| Tout `/api/**` | — | Oui | Retourne 401 JSON si absent |

### Tables impactées

Aucune table touchée dans cette subfeature.

### Migration Liquibase

Non applicable — pas de changement de schéma.

### Composants Angular

Non applicable.

### Fichiers techniques impactés

- `src/main/java/fr/ailegalcase/auth/SecurityConfig.java` — configuration Spring Security
- `src/main/resources/application.yml` — section `spring.security.oauth2.client`
- `src/main/resources/application-dev.yml` — credentials dev (gitignore ou variable d'env)

---

## Plan de test

### Tests unitaires

| # | Classe | Description | Résultat attendu |
|---|--------|-------------|-----------------|
| U-01 | `SecurityConfigTest` | La SecurityFilterChain est créée sans erreur | Bean instancié, pas d'exception |

> Note : la configuration Spring Security est principalement couverte par les tests d'intégration.

### Tests d'intégration

| # | Endpoint | Contexte | Code HTTP attendu | Corps réponse |
|---|----------|---------|------------------|---------------|
| I-01 | `GET /api/v1/me` | Sans token | 401 | JSON (pas de HTML) |
| I-02 | `GET /oauth2/authorization/google` | Sans auth | 302 | Redirection vers Google |
| I-03 | `GET /oauth2/authorization/microsoft` | Sans auth | 302 | Redirection vers Microsoft |
| I-04 | `GET /api/v1/me` | Header `Accept: application/json`, sans auth | 401 | Body JSON sans stacktrace |
| I-05 | Application startup | Config OAuth valide, profil dev | Démarrage OK | Pas d'exception |
| I-06 | `GET /api/v1/inexistant` | Sans auth | 401 | L'auth est vérifiée avant le routing |
| I-07 | Body réponse 401 | Contenu du corps | — | JSON valide, pas de HTML, pas de stacktrace |

### Isolation workspace

Non applicable — aucun accès aux données workspace dans cette subfeature.

### Traçabilité critères d'acceptation

| Critère | Tests couvrants |
|---------|----------------|
| CA-01 — App démarre sans erreur | I-05 |
| CA-02 — `/api/v1/me` sans auth → 401 JSON | I-01, I-04, I-07 |
| CA-03 — OAuth Google accessible | I-02 |
| CA-04 — OAuth Microsoft accessible | I-03 |
| CA-05 — Pas de credentials hardcodés | Revue statique du code (review-checklist) |
| CA-06 — Profil dev fonctionne | I-05 |

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune question ouverte non résolue.

---

## Notes et décisions

- **Credentials OAuth — décision actée (2026-03-17) :**
  - Environnement dev : credentials Google réels (client-id + client-secret issus de la Google Console), stockés dans un fichier `.env` local ou `application-dev.yml`, ignoré par git.
  - Environnement test : mock OAuth via Spring Security Test (`@WithMockUser` / mock du `OAuth2UserService`). Le vrai provider OAuth n'est jamais appelé dans les tests.

- Spring Security OAuth2 Client gère nativement le flux OIDC pour Google et Microsoft. Pas de code custom pour le handshake OAuth.

- Le comportement 401 JSON (vs redirect HTML) est obtenu en désactivant le `formLogin` et en configurant un `AuthenticationEntryPoint` retournant 401 en JSON.

- SF-01-02 ajoutera un `OAuth2UserService` custom pour la persistance utilisateur. Cette subfeature se limite à la configuration de sécurité.
