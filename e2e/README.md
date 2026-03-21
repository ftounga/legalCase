# E2E Smoke Tests — AI LegalCase

Suite de tests de non-régression sur les chemins critiques d'intégration.

## Objectif

Ces tests ne couvrent **pas** la logique métier de chaque feature (couverte par les tests unitaires/IT).
Ils couvrent les **points de jonction** entre features, là où les régressions apparaissent silencieusement :
- Auth (login local, OAuth, logout, redirects)
- Workspace context (switch → rechargement des données)
- Navigation / routing (guards, invitations, routes protégées)

## Prérequis

### 1. Serveurs démarrés

```bash
# Backend (port 8080)
source .env.local && cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Frontend (port 4200)
source ~/.nvm/nvm.sh && nvm use 22 && cd frontend && npm start
```

### 2. Compte de test local en base

Créer un compte local vérifié (email_verified = true) :

```sql
-- Option 1 : s'inscrire via l'interface et valider l'email manuellement
UPDATE auth_accounts SET email_verified = true
WHERE provider = 'LOCAL' AND provider_user_id = 'e2e@legalcase.test';

-- Option 2 : créer directement via l'API
POST /api/v1/auth/register { "email": "e2e@legalcase.test", "password": "E2ePassword1!", ... }
```

### 3. Variables d'environnement

```bash
export E2E_LOCAL_EMAIL=e2e@legalcase.test
export E2E_LOCAL_PASSWORD=E2ePassword1!
export E2E_BASE_URL=http://localhost:4200  # optionnel, valeur par défaut
```

## Installation

```bash
cd e2e
npm install
npx playwright install chromium
```

## Lancer les tests

```bash
cd e2e
npm test                  # mode CI (headless)
npm run test:ui           # mode interactif avec UI Playwright
npm run test:report       # voir le rapport HTML
```

## Structure

```
e2e/
  smoke/
    auth.spec.ts          # Login, logout, redirect non-authentifié
    workspace.spec.ts     # Contexte workspace, switch workspace
    navigation.spec.ts    # Invitation → /login, guards, routes inconnues
  helpers/
    auth.helper.ts        # loginLocal(), logout(), TEST_USER
  playwright.config.ts
  README.md
```

## Règle de gouvernance

Toute subfeature cochant une **préoccupation transversale** (auth, workspace, navigation)
doit faire passer les smoke tests concernés **avant le push**.

Référence : `CLAUDE.md — Préoccupations transversales — règle anti-régression`
