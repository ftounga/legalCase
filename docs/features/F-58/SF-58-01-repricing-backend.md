# Mini-spec — F-58 / SF-58-01 : Repricing backend — plans SOLO/TEAM/PRO

---

## Identifiant

`F-58 / SF-58-01`

## Feature parente

`F-58` — Repricing — Plans SOLO/TEAM/PRO

## Statut

`in-progress`

## Date de création

2026-03-28

## Branche Git

`feat/SF-58-01-repricing-backend`

---

## Objectif

Mettre à jour PlanLimitService, StripeCheckoutService, StripeWebhookService et application.yml pour les nouveaux plans SOLO/TEAM/PRO avec les nouveaux quotas validés, et ajouter une migration qui renomme STARTER→SOLO sur les subscriptions existantes.

---

## Comportement attendu

### Cas nominal

1. Un workspace sur plan SOLO peut ouvrir jusqu'à 15 dossiers, uploader 15 documents/dossier, lancer 8 analyses/dossier, 3 re-analyses enrichies/dossier, 100 messages chat/mois, 6M tokens/mois
2. Un workspace sur plan TEAM peut ouvrir jusqu'à 40 dossiers, 30 docs/dossier, 15 analyses/dossier, 8 re-analyses enrichies/dossier, 300 messages chat/mois, 18M tokens/mois
3. Un workspace sur plan PRO : dossiers illimités, 50 docs/dossier, analyses illimitées, re-analyses illimitées, 1000 messages chat/mois, 60M tokens/mois
4. Un workspace FREE trial peut lancer 1 re-analyse enrichie/dossier (plus de gate plan-only)
5. Stripe checkout accepte SOLO, TEAM, PRO comme planCode valide
6. Le webhook Stripe résout correctement les priceId SOLO, TEAM, PRO
7. Les subscriptions existantes en base avec plan_code='STARTER' sont migrées en 'SOLO'

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| planCode inconnu envoyé au checkout | 400 Bad Request | 400 |
| Quota analyses atteint (ex : SOLO ≥ 8) | 402 Payment Required | 402 |
| Quota re-analyses atteint (ex : FREE ≥ 1) | 402 Payment Required | 402 |
| Budget tokens mensuel dépassé | Analyse SKIPPED (fail-open) | — |
| Quota chat atteint | 402 Payment Required | 402 |

---

## Critères d'acceptation

- [ ] `PlanLimitService` : constantes SOLO/TEAM/PRO correctes pour les 6 quotas
- [ ] `PlanLimitService` : FREE re-analyses = 1 (était 0 / bloqué par plan-only)
- [ ] `isEnrichedAnalysisAllowed` supprimé ou remplacé par vérification quota uniquement
- [ ] `StripeCheckoutService` : VALID_PLANS = {SOLO, TEAM, PRO}
- [ ] `StripeWebhookService` : résolution SOLO/TEAM/PRO depuis priceId
- [ ] `application.yml` : price-id-solo, price-id-team, price-id-pro configurés
- [ ] Migration Liquibase : STARTER→SOLO sur subscriptions existantes
- [ ] Tests unitaires PlanLimitService : tous les quotas par plan couverts
- [ ] Tests d'intégration : gate analyses, re-analyses, chat par plan

---

## Périmètre

### Hors scope

- Interface frontend (SF-58-02)
- Création des produits/prix dans le dashboard Stripe (action manuelle)
- Early adopter coupons
- Nouveau plan d'utilisateurs multiples (TEAM déjà supporté par le workspace existant)

---

## Contraintes de validation

| Champ | Valeurs autorisées | Notes |
|-------|-------------------|-------|
| planCode checkout | SOLO, TEAM, PRO | Validation stricte dans StripeCheckoutService |
| plan_code en base | FREE, SOLO, TEAM, PRO | varchar(20), migration renomme STARTER→SOLO |

Quotas validés :

| | FREE | SOLO | TEAM | PRO |
|---|---|---|---|---|
| Dossiers ouverts | 2 | 15 | 40 | illimité (Integer.MAX_VALUE) |
| Documents/dossier | 5 | 15 | 30 | 50 |
| Analyses/dossier | 2 | 8 | 15 | illimité |
| Re-analyses enrichies/dossier | 1 | 3 | 8 | illimité |
| Budget tokens/mois | 500 000 | 6 000 000 | 18 000 000 | 60 000 000 |
| Chat messages/mois | 10 | 100 | 300 | 1 000 |

---

## Technique

### Endpoints

Aucun nouvel endpoint. Modification interne uniquement.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| subscriptions | UPDATE | Migration STARTER→SOLO |

### Migration Liquibase

- [x] Oui — `033-rename-starter-to-solo.xml`

### Fichiers impactés

- `PlanLimitService.java` — quotas + suppression gate plan-only enrichi
- `StripeCheckoutService.java` — VALID_PLANS, priceIdTeam
- `StripeWebhookService.java` — résolution 3 plans, priceIdTeam injecté
- `application.yml` — price-id-solo, price-id-team
- `033-rename-starter-to-solo.xml` — migration

---

## Plan de test

### Tests unitaires

- [ ] `PlanLimitService` — SOLO : 15 dossiers, 15 docs, 8 analyses, 3 re-analyses, 6M tokens, 100 chat
- [ ] `PlanLimitService` — TEAM : 40 dossiers, 30 docs, 15 analyses, 8 re-analyses, 18M tokens, 300 chat
- [ ] `PlanLimitService` — PRO : illimité dossiers/analyses/re-analyses, 50 docs, 60M tokens, 1000 chat
- [ ] `PlanLimitService` — FREE : 2 dossiers, 5 docs, 2 analyses, 1 re-analyse, 500k tokens, 10 chat
- [ ] `PlanLimitService` — FREE expiré : 0 dossiers, chat bloqué
- [ ] Re-analyse enrichie accessible FREE (1 quota), SOLO (3), TEAM (8), PRO (illimité)

### Tests d'intégration

- [ ] Checkout → 400 si planCode invalide
- [ ] Checkout → 200 si planCode = SOLO/TEAM/PRO

### Isolation workspace

- [ ] Non applicable — modification de PlanLimitService (lecture subscription par workspaceId déjà en place)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Plans / limites** — cœur de la subfeature : PlanLimitService modifié

### Composants / endpoints potentiellement impactés

| Composant | Impact potentiel | Test de non-régression |
|-----------|-----------------|----------------------|
| `CaseFileService` | Appelle `getMaxOpenCaseFilesForWorkspace` — comportement inchangé, valeurs changent | Tests unitaires PlanLimitService |
| `DocumentService` | Appelle `getMaxDocumentsPerCaseFileForWorkspace` — idem | Tests unitaires |
| `CaseAnalysisCommandService` | Appelle `isCaseAnalysisLimitReached` — idem | Tests unitaires |
| `ReAnalysisCommandService` | Appelle `isEnrichedAnalysisAllowedForWorkspace` — méthode supprimée, remplacée par quota | Tests unitaires |
| `ChatService` | Appelle `isChatMessageLimitReached` — comportement inchangé, valeurs changent | Tests unitaires |

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — modification des seuils, pas des flux de navigation

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- `isEnrichedAnalysisAllowed(planCode)` supprimé : la logique devient `isReAnalysisLimitReached(workspaceId, caseFileId)` qui fonctionne pour tous les plans y compris FREE
- `isEnrichedAnalysisAllowedForWorkspace` devient redondant et est supprimé ; `ReAnalysisCommandService` utilisera uniquement `isReAnalysisLimitReached`
- "illimité" = `Integer.MAX_VALUE` pour les compteurs, évite une valeur null dans le code
- `ChatService.useEnriched` (choix Sonnet vs Haiku) reste lié au plan utilisateur : SOLO+ peut utiliser Sonnet pour le chat enrichi
