# Mini-spec — F-16 / SF-16-01 — Infrastructure subscriptions

## Identifiant
`F-16 / SF-16-01`

## Feature parente
`F-16` — Gestion des abonnements

## Statut
`in-progress`

## Date de création
`2026-03-18`

## Branche Git
`feat/SF-16-01-infra-subscriptions`

---

## Objectif

Créer la table `subscriptions`, l'entité JPA, le repository, et initialiser automatiquement un abonnement STARTER à la création de chaque workspace.

---

## Comportement attendu

### Cas nominal

- À la création d'un workspace (flux onboarding), une entrée `subscriptions` est créée automatiquement avec :
  - `plan_code = STARTER`
  - `status = ACTIVE`
  - `started_at = now()`
  - `expires_at = null`
- Un workspace a exactement une subscription (contrainte unique sur `workspace_id`).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Tentative de créer deux subscriptions pour le même workspace | Erreur contrainte unique en base | 500 (ne doit pas arriver — protégé par la logique idempotente de `createDefaultWorkspace`) |
| Workspace introuvable lors de la création | Exception propagée par le service workspace | 500 |

---

## Critères d'acceptation

- [ ] La table `subscriptions` est créée via migration Liquibase `017-create-subscriptions.xml`
- [ ] L'entité `Subscription` est mappée JPA avec tous les champs
- [ ] `SubscriptionRepository` expose `findByWorkspaceId(UUID)`
- [ ] `WorkspaceService.createDefaultWorkspace` crée une `Subscription` STARTER en même temps que le workspace
- [ ] Contrainte unique `workspace_id` en base
- [ ] Tests unitaires couvrant la création de la subscription lors de l'onboarding
- [ ] Test d'intégration vérifiant qu'une subscription STARTER existe après création du workspace

---

## Périmètre

### Hors scope (explicite)

- API REST d'exposition de l'abonnement (SF-16-06)
- Changement de plan (SF-16-02, SF-16-03, SF-16-04)
- Contrôle d'accès par plan (SF-16-02, SF-16-03, SF-16-04)
- Intégration Stripe (hors V1)

---

## Technique

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `subscriptions` | CREATE (migration) + INSERT | Nouvelle table |
| `workspaces` | — | Pas de modification — `plan_code` déjà présent |

### Migration Liquibase

- [x] Oui — `017-create-subscriptions.xml`

### Valeurs initiales à la création

| Champ | Valeur | Règle |
|-------|--------|-------|
| plan_code | STARTER | Toujours STARTER à l'onboarding |
| status | ACTIVE | Toujours ACTIVE à la création |
| started_at | now() | Timestamp de création du workspace |
| expires_at | null | Pas d'expiration par défaut |

---

## Plan de test

### Tests unitaires

- [ ] `WorkspaceService.createDefaultWorkspace` — vérifie que `subscriptionRepository.save()` est appelé avec plan STARTER
- [ ] `WorkspaceService.createDefaultWorkspace` — idempotent : ne crée pas de seconde subscription si workspace existant

### Tests d'intégration

- [ ] Après onboarding, `GET /api/v1/workspaces/current` → workspace retourné existe en base
- [ ] En base : une subscription STARTER ACTIVE existe pour le workspace créé

### Isolation workspace

- [ ] Applicable — `workspace_id` est la clé de filtrage, contrainte unique garantie en base

---

## Dépendances

### Subfeatures bloquantes
- Aucune

### Questions ouvertes impactées
- Aucune

---

## Notes et décisions

- Le plan est géré à la fois sur `workspaces.plan_code` (existant) et sur la table `subscriptions` (nouvelle). `subscriptions` est la source de vérité pour le cycle de vie de l'abonnement ; `workspaces.plan_code` est conservé pour lecture rapide sans jointure.
- `expires_at = null` signifie pas d'expiration — plans à durée indéterminée en V1 (pas de trial, pas de renouvellement mensuel automatique).
