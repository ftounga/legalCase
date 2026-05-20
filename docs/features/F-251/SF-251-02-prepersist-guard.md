# Mini-spec — F-251 / SF-251-02 — Garde-fou backend `@PrePersist` Subscription FREE

## Identifiant

`F-251 / SF-251-02`

## Feature parente

`F-251` — Fiabilisation de la période d'évaluation pour les comptes provisionnés en bypass IHM

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-251-02-prepersist-guard`

---

## Objectif

Empêcher toute persistance d'une `Subscription` FREE avec `expires_at` NULL en ajoutant un hook JPA `@PrePersist` qui force `expires_at = COALESCE(started_at, now()) + 14 days` quand c'est requis — y compris pour les flux super-admin qui contournent `WorkspaceService.createWorkspace`.

---

## Comportement attendu

### Cas nominal

Toute insertion d'une `Subscription` (via JPA / `subscriptionRepository.save()`) déclenche `@PrePersist Subscription.applyTrialExpiresAtFallback()` :

- Si `planCode == "FREE"` ET `expiresAt == null` :
  - si `startedAt != null` → `expiresAt = startedAt + 14 days`
  - sinon → `startedAt = now()` ET `expiresAt = startedAt + 14 days`
- Sinon (plan payant OU `expiresAt` déjà fourni) → no-op.

L'INSERT SQL direct **n'est pas couvert** par `@PrePersist` (qui est une fonctionnalité JPA, pas une contrainte DB). Documentation interne (SF-251-03 optionnelle) doit rappeler aux opérateurs super-admin de passer par `WorkspaceService.createWorkspace` ou son équivalent SQL paramétré.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|----------------------|
| FREE + `expiresAt = null` + `startedAt = null` | `@PrePersist` fixe `startedAt = now()` puis `expiresAt = now+14d` |
| SOLO/TEAM/PRO + `expiresAt = null` | No-op — plan payant, géré par Stripe webhook |
| FREE + `expiresAt` fourni | No-op — pas d'écrasement défensif |
| Mise à jour (`@PreUpdate`) | **Hors scope** — uniquement la création (`@PrePersist`). Une UPDATE qui efface `expiresAt` reste possible mais n'est pas un cas observé. |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : non applicable
- [x] **Autres pays** : non applicable
- [x] **Autres domaines** : non applicable
- [x] **Autres UI patterns** : non applicable
- [x] **Autres flows transversaux** : **Plans / limites** scanné — `@PrePersist` garantit que toute nouvelle row FREE arme correctement `PlanLimitService.isExpiredFree` ; **Workspace context** scanné — `WorkspaceService.createWorkspace` fixe déjà `expiresAt` explicitement, `@PrePersist` est défensif (no-op dans ce cas).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `WorkspaceService.createWorkspace` | Oui | Aucune modif requise — pose déjà `expiresAt`, le hook est no-op |
| `WorkspaceService.createDefaultWorkspace` | Oui | Idem — no-op |
| Webhook Stripe `customer.subscription.deleted` (downgrade FREE) | Oui | À vérifier : si downgrade pose `expiresAt = now+14d` explicitement → no-op ; sinon le hook protège |
| INSERT SQL super-admin direct | **Non couvert** par `@PrePersist` | SF-251-03 documentation skill `prospect-account-bootstrap` |

### Décision

- [x] Étendu à toutes les cibles applicables couvertes par JPA dans cette subfeature
- [x] Cible SQL direct documentée hors couverture, traitement SF-251-03 (optionnelle, traitée si signal de récidive)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — SF backend pure (entité JPA + test), pas de composant frontend décisionnel.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — SF backend pure, sans outil décisionnel ni champ saisissable.

---

## Critères d'acceptation

- [x] Méthode `@PrePersist Subscription.applyTrialExpiresAtFallback()` ajoutée à `Subscription.java`.
- [x] Test unitaire `SubscriptionPrePersistTest` qui couvre :
  - FREE + `expiresAt = null` + `startedAt = ancien` → `expiresAt = startedAt + 14d`
  - FREE + `expiresAt = null` + `startedAt = null` → `startedAt = now`, `expiresAt = now + 14d`
  - FREE + `expiresAt = fourni` → inchangé (no-op)
  - SOLO + `expiresAt = null` → inchangé (no-op)
- [x] Test d'intégration `SubscriptionPrePersistIT` qui appelle `subscriptionRepository.save(new Subscription())` avec FREE/`expiresAt=null` et vérifie post-flush que `expiresAt` est non NULL.
- [x] Le test IT `WorkspaceServiceIT.createDefaultWorkspace_*` existant reste vert (no-op pour le flux nominal qui pose déjà `expiresAt`).
- [x] Smoke build : `./mvnw verify` passe vert.

---

## Périmètre

### Hors scope (explicite)

- Couverture des INSERT SQL directs (impossibilité technique de `@PrePersist`).
- `@PreUpdate` hook — non observé dans le diagnostic, pas de cas d'usage identifié.
- Contrainte DB `NOT NULL` sur `expires_at` — risquerait de bloquer les plans payants legacy.
- Mise à jour de la skill `prospect-account-bootstrap` — SF-251-03.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `expiresAt` (Subscription FREE + null) | `COALESCE(startedAt, now()) + 14 days` | Appliqué par `@PrePersist` |
| `startedAt` (Subscription + null) | `now()` (seulement si FREE) | Appliqué par `@PrePersist` quand `expiresAt` doit être calculé |

---

## Contraintes de validation

| Champ | Obligatoire | Notes |
|-------|-------------|-------|
| `expiresAt` (plan FREE) | Oui via `@PrePersist` | Pas de contrainte DB — défense applicative seule |
| `startedAt` (plan FREE) | Oui via `@PrePersist` (NOT NULL en DB de toute façon) | Le hook le pose si manquant pour pouvoir calculer `expiresAt` |

---

## Technique

### Endpoint(s)

Aucun.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `subscriptions` | INSERT (interception JPA) | Pas de modif schéma |

### Migration Liquibase

- [x] Non applicable — modification entité JPA seule.

### Composants Angular (si applicable)

Aucun.

---

## Plan de test

### Tests unitaires

- [x] `SubscriptionPrePersistTest.freeWithStartedAt_nullExpiresAt_setsExpiresAtToStartedPlus14d`
- [x] `SubscriptionPrePersistTest.freeWithNullStartedAt_setsBothNowAndExpiresAt`
- [x] `SubscriptionPrePersistTest.freeWithFournisExpiresAt_isNoOp`
- [x] `SubscriptionPrePersistTest.soloWithNullExpiresAt_isNoOp`

### Tests d'intégration

- [x] `SubscriptionPrePersistIT.saveFreeWithoutExpiresAt_persistsWithExpiresAtSet` — `@DataJpaTest`, save direct via repository, flush, re-fetch, assert non NULL.

### Isolation workspace

- [x] Non applicable — modification entité globale.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Plans / limites** — garantit que toutes les nouvelles rows FREE arment `isExpiredFree`. Pas de changement de logique applicative.
- [x] **Workspace context** — `WorkspaceService.createWorkspace` n'est pas modifié ; le hook est no-op pour le flux nominal.
- [ ] Auth / Principal
- [ ] Navigation / routing

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `WorkspaceService.createWorkspace` | No-op (pose déjà expiresAt) | `WorkspaceServiceIT` existant reste vert |
| `WorkspaceService.createDefaultWorkspace` | No-op (pose déjà expiresAt) | `WorkspaceServiceIT` existant reste vert |
| Webhook Stripe `customer.subscription.deleted` | À vérifier : si downgrade FREE pose `expiresAt`, no-op ; sinon le hook complète | Test IT existant `StripeWebhookControllerIT` reste vert |

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné (justification : modification entité JPA, pas de modif de flow utilisateur visible).

---

## Dépendances

### Subfeatures bloquantes

- `SF-251-01` — fix data rétroactif (idéalement merged avant ; sinon, ne pose pas problème — le hook ne touche que les futures inserts).

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- **Choix `@PrePersist` plutôt que validation `@Valid` côté service** : `@PrePersist` intercepte tous les flux JPA (WorkspaceService, webhook Stripe, repository direct), pas seulement `WorkspaceService.createWorkspace`. La validation `@Valid` aurait nécessité d'annoter chaque callsite, plus fragile.
- **Choix `@PrePersist` plutôt que contrainte DB `NOT NULL`** : risque de bloquer des rows Stripe payantes legacy avec `expires_at` NULL ; le garde-fou applicatif cible spécifiquement FREE. À reconsidérer post F-251 si l'audit montre que toutes les rows payantes ont `expires_at` rempli côté webhook (backlog).
- **Cas SQL direct hors couverture** : c'est la limite technique de `@PrePersist`. SF-251-03 documente la procédure opérateur. Pour une couverture totale future : trigger PostgreSQL (overkill V1, non priorisé).
- **Comportement défensif (no-op si déjà fourni)** : pas d'écrasement, on respecte la valeur explicite quand elle existe.
