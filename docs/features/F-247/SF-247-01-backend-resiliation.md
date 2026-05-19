# Mini-spec — F-247 / SF-247-01 — Backend résiliation d'abonnement self-service

## Identifiant

`F-247 / SF-247-01`

## Feature parente

`F-247` — Résiliation d'abonnement en self-service

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-247-01-backend-cancel-subscription`

---

## Objectif

Exposer à l'OWNER d'un workspace des endpoints permettant de programmer la résiliation de son abonnement Stripe en fin de période de facturation, de l'annuler, et de connaître l'état de résiliation de son abonnement.

---

## Comportement attendu

### Cas nominal

1. L'OWNER appelle `POST /api/v1/billing/cancel`. Le service résout le workspace courant, vérifie le rôle OWNER, récupère la `Subscription` du workspace.
2. Le service appelle Stripe : `Subscription.update(stripeSubscriptionId, {cancel_at_period_end: true})`. Stripe renvoie la subscription mise à jour.
3. Le service persiste sur l'entité locale `Subscription` : `cancelAtPeriodEnd = true` et `currentPeriodEnd` (lu depuis la réponse Stripe `current_period_end`).
4. Réponse `200` `{planCode, status, cancelAtPeriodEnd: true, currentPeriodEnd}`.
5. À l'échéance, Stripe émet `customer.subscription.deleted` → `StripeWebhookService.handleSubscriptionDeleted` (déjà en place) repasse le workspace en `FREE`. SF-247-01 ajoute seulement le reset `cancelAtPeriodEnd = false` dans ce handler.
6. `POST /api/v1/billing/resume` (OWNER) appelle `Subscription.update(..., {cancel_at_period_end: false})`, repasse `cancelAtPeriodEnd = false`, réponse `200`.
7. `GET /api/v1/billing/subscription` (tout membre du workspace) renvoie l'état courant pour l'affichage frontend.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|---|---|---|
| Appelant non authentifié | Rejet | 401 |
| Appelant membre du workspace mais rôle ≠ OWNER | « Seul le propriétaire peut résilier l'abonnement » | 403 |
| Workspace en plan FREE / pas de `stripeSubscriptionId` | « Aucun abonnement payant à résilier » | 409 |
| `resume` alors qu'aucune résiliation n'est programmée (`cancelAtPeriodEnd` déjà false) | « Aucune résiliation programmée » | 409 |
| Échec de l'appel API Stripe | « La résiliation a échoué, réessayez » | 502 |
| Stripe désactivé (`app.stripe.enabled=false`) | No-op côté Stripe, l'état local n'est pas modifié, réponse explicite | 409 |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** — non applicable : SF de billing, aucun outil décisionnel.
- [x] **Autres pays** — non applicable : la facturation Stripe est indépendante du pays du workspace (FR/BE).
- [x] **Autres domaines** — non applicable : transversal aux 3 domaines, aucune logique par domaine.
- [x] **Autres UI patterns** — backend pur, aucun pattern UI.
- [x] **Autres flows transversaux** — **Plans / limites** : la résiliation effective (downgrade FREE) est déjà gérée par `handleSubscriptionDeleted` ; aucune nouvelle logique de gate. **Webhook Stripe** : `customer.subscription.updated` et `...deleted` déjà branchés.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|---|---|---|
| `StripeWebhookService.handleSubscriptionDeleted` | Oui | Étendu dans cette SF : ajout du reset `cancelAtPeriodEnd = false` au downgrade FREE existant. |
| `StripeWebhookService.handleSubscriptionUpdated` | Oui | Étendu dans cette SF : synchronise `cancelAtPeriodEnd` + `currentPeriodEnd` depuis la subscription Stripe (le webhook `updated` est émis par Stripe au moment du `cancel_at_period_end`). |
| `StripeCustomerService.cancelSubscription` (annulation immédiate, F-25) | Non | Inchangé — la suppression destructive super-admin garde l'annulation immédiate. SF-247-01 ajoute des méthodes distinctes (`scheduleCancellation` / `resumeSubscription`). |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF backend pure, aucun composant frontend décisionnel.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — justification : SF de billing, aucun outil décisionnel à champs saisissables, aucune extraction IA.

---

## Critères d'acceptation

- [ ] `POST /api/v1/billing/cancel` en tant qu'OWNER d'un workspace payant programme `cancel_at_period_end=true` côté Stripe et renvoie `200` avec `cancelAtPeriodEnd: true` + `currentPeriodEnd` non nul.
- [ ] `POST /api/v1/billing/cancel` par un membre non-OWNER renvoie `403` et n'appelle pas Stripe.
- [ ] `POST /api/v1/billing/cancel` sur un workspace FREE (sans `stripeSubscriptionId`) renvoie `409`.
- [ ] `POST /api/v1/billing/resume` après une résiliation programmée renvoie `200` avec `cancelAtPeriodEnd: false`.
- [ ] `POST /api/v1/billing/resume` sans résiliation programmée renvoie `409`.
- [ ] `GET /api/v1/billing/subscription` renvoie `planCode`, `status`, `cancelAtPeriodEnd`, `currentPeriodEnd` pour le workspace courant.
- [ ] À la réception de `customer.subscription.deleted`, le workspace repasse `FREE` **et** `cancelAtPeriodEnd` est remis à `false`.
- [ ] Isolation workspace : un OWNER ne peut résilier que l'abonnement de son propre workspace courant.

---

## Périmètre

### Hors scope (explicite)

- Remboursement au prorata (résiliation toujours en fin de période).
- Suppression du workspace / des données (reste F-25 super-admin).
- Frontend (SF-247-02).
- Email de confirmation de résiliation (non prévu V1 — l'écran suffit).

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs | Notes |
|---|---|---|---|
| `cancel_at_period_end` (colonne) | Oui | booléen, `NOT NULL DEFAULT false` | — |
| `current_period_end` (colonne) | Non | timestamp nullable | renseigné depuis Stripe `current_period_end` |

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---|---|---|---|
| POST | `/api/v1/billing/cancel` | Oui | OWNER |
| POST | `/api/v1/billing/resume` | Oui | OWNER |
| GET | `/api/v1/billing/subscription` | Oui | MEMBER (workspace courant) |

**Contrat de réponse (figé pour parallélisation SF-247-02)** :
`200` → `{"planCode": string, "status": string, "cancelAtPeriodEnd": boolean, "currentPeriodEnd": string|null (ISO-8601)}`
Erreurs : `403` rôle, `409` rien à résilier / déjà dans l'état cible, `502` échec Stripe — corps `{"message": string}` via `GlobalExceptionHandler`.

### Tables impactées

| Table | Opération | Notes |
|---|---|---|
| `subscriptions` | ALTER (ajout `cancel_at_period_end`, `current_period_end`) + UPDATE | colonnes nouvelles |

### Migration Liquibase

- [x] Oui — `249-add-subscription-cancellation-fields.xml` (ajout `cancel_at_period_end` BOOLEAN NOT NULL DEFAULT false, `current_period_end` TIMESTAMP nullable sur `subscriptions`). Réversible (drop columns).

### Classes impactées

- `Subscription` (entité) — 2 champs `cancelAtPeriodEnd`, `currentPeriodEnd`.
- `StripeCustomerService` — `scheduleCancellation(stripeSubscriptionId)` + `resumeSubscription(stripeSubscriptionId)` (renvoient la `Subscription` Stripe pour lecture de `current_period_end`).
- `BillingCancellationController` (nouveau) — 3 endpoints, pattern `@AuthenticationPrincipal OidcUser` + `Principal` + `OAuthProviderResolver` (cf. `BillingSeatsController`).
- `BillingCancellationService` (nouveau) — résolution user + workspace + rôle OWNER (pattern `WorkspaceMemberService`), orchestration Stripe + persistance.
- `StripeWebhookService` — `handleSubscriptionUpdated` / `handleSubscriptionDeleted` étendus.

---

## Plan de test

### Tests unitaires

- [ ] `BillingCancellationService` — cancel nominal : programme la résiliation, persiste `cancelAtPeriodEnd=true`.
- [ ] `BillingCancellationService` — cancel par non-OWNER → 403.
- [ ] `BillingCancellationService` — cancel sur workspace FREE → 409.
- [ ] `BillingCancellationService` — resume sans résiliation programmée → 409.
- [ ] `StripeWebhookService` — `customer.subscription.deleted` → planCode FREE + `cancelAtPeriodEnd=false`.
- [ ] `StripeWebhookService` — `customer.subscription.updated` avec `cancel_at_period_end=true` → synchro locale.

### Tests d'intégration

- [ ] `POST /api/v1/billing/cancel` → 200 (OWNER, workspace payant simulé).
- [ ] `POST /api/v1/billing/cancel` → 403 (membre non-OWNER).
- [ ] `POST /api/v1/billing/cancel` → 409 (workspace FREE).
- [ ] `POST /api/v1/billing/resume` → 200 puis 409 au second appel.
- [ ] `GET /api/v1/billing/subscription` → 200 avec les 4 champs.

### Isolation workspace

- [x] Applicable — test : un OWNER du workspace A ne peut pas résilier l'abonnement du workspace B (le service n'opère que sur la `Subscription` du workspace courant résolu).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Plans / limites** — le downgrade FREE post-résiliation ré-applique les quotas FREE. Aucune nouvelle gate : le mécanisme existe déjà (`handleSubscriptionDeleted` + `PlanLimitService`).
- [ ] Auth / Principal — non (réutilise le pattern existant).
- [ ] Workspace context — non.
- [ ] Navigation / routing — non.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression |
|---|---|---|
| `StripeWebhookService` (checkout / topup / updated / deleted) | handlers `updated` / `deleted` modifiés | IT webhook existants + nouveaux IT |
| `PlanLimitService` | consomme `planCode` — inchangé | couvert par IT downgrade |

### Smoke tests E2E concernés

- [ ] `cd e2e && npm test` — préoccupation **Plans / limites** cochée → smoke tests obligatoires avant push (login + parcours nominal).

---

## Dépendances

### Subfeatures bloquantes

- Aucune. SF-247-02 (frontend) parallélisable — contrat d'API figé ci-dessus.

### Questions ouvertes impactées

- [ ] Aucune (`docs/OPEN_QUESTIONS.md` non impacté).

---

## Notes et décisions

- Le webhook `customer.subscription.deleted` downgrade déjà en `FREE` (code existant) — SF-247-01 ne réinvente pas ce flux, elle l'étend a minima.
- `cancel_at_period_end` est la seule sémantique retenue (pas d'annulation immédiate self-service) — cf. invariant 1 du cadrage cohérence.
