# Mini-spec — [F-156 / SF-156-01] Backend — gate plan TEAM/PRO + état `PENDING_PAYMENT`

> Produite à partir de `project-governance/templates/subfeature-template.md`.
> Cadrage cohérence : `SF-156-00-coherence.md` (verdict GO).
> Cadrage écran : `SF-156-00b-ux-coherence.md` (verdict GO).

## Identifiant

`F-156 / SF-156-01`

## Feature parente

`F-156` — Création d'un workspace supplémentaire — réservée aux plans TEAM / PRO.

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-156-01-backend-gate-plan`

## Objectif

Restreindre côté backend la création d'un workspace supplémentaire aux OWNER actifs en TEAM ou PRO, et créer le nouveau workspace dans un état transitoire `PENDING_PAYMENT` jusqu'à confirmation Stripe.

## Comportement attendu

### Cas nominal

1. Un OWNER avec un plan **TEAM** ou **PRO actif** appelle `POST /api/v1/workspaces` avec `{ name: "...", plan: "TEAM" }` (ou `"PRO"`).
2. Le contrôleur valide : OWNER plan ∈ {TEAM, PRO}, `plan` ∈ {TEAM, PRO}, `name` non vide, ≤ 100 caractères.
3. Le service crée un `Workspace` en `status = PENDING_PAYMENT`, sans abonnement actif.
4. Le service crée une session Stripe Checkout en mode `subscription` (price = TEAM ou PRO selon choix), avec `success_url` = `https://<host>/workspaces?workspace_created=success&workspace_id=<id>` et `cancel_url` = `https://<host>/workspaces?workspace_created=cancelled&workspace_id=<id>`.
5. Réponse `201 Created` : `{ workspaceId, stripeCheckoutUrl }`.
6. À la réception du webhook `customer.subscription.created` pour ce workspace, l'enregistrement `Workspace.status` passe à `ACTIVE` (idempotent : un même `subscription.id` reçu deux fois ne provoque qu'une transition).
7. Le workspace est alors utilisable comme tout autre.

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|---|---|---|
| OWNER courant en FREE ou SOLO | Refus avec message « La création d'un workspace supplémentaire nécessite un abonnement TEAM ou PRO » | `403 Forbidden` |
| Champ `plan` absent / null | Refus avec message « Le plan est obligatoire (TEAM ou PRO) » | `400 Bad Request` |
| Champ `plan` ∉ {TEAM, PRO} | Refus avec message « Plan invalide — seuls TEAM et PRO sont acceptés pour un workspace supplémentaire » | `400 Bad Request` |
| Champ `name` vide ou > 100 caractères | Refus avec message de validation standard | `400 Bad Request` |
| Erreur Stripe (création session) | Refus, workspace **non créé** (rollback transactionnel), erreur loggée | `502 Bad Gateway` ou `503` |
| Webhook `customer.subscription.created` reçu deux fois pour le même `subscription.id` | Deuxième traitement no-op gracieux (idempotence) | `200 OK` |
| Webhook `customer.subscription.deleted` ou `invoice.payment_failed` reçu pour un workspace `PENDING_PAYMENT` | Workspace passe à `CANCELLED` (audit) puis est supprimé par le cron de nettoyage | n/a |
| Workspace en `PENDING_PAYMENT` depuis plus de **24 h** | Le cron `@Scheduled` (hourly) le passe à `CANCELLED` et le supprime, supprime aussi la session Stripe pendante si possible | n/a |
| Utilisateur non OWNER (MEMBER) tente la création | Refus | `403 Forbidden` |

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres workflows multi-tenant** : F-154 switcher (Terminée, consomme `Workspace.status`), F-WS invitations (consomme `Workspace.status`), F-billing (consomme `Workspace.status` pour les rapports). Tous doivent **ignorer** les workspaces en `PENDING_PAYMENT` dans leurs listes par défaut.
- [x] **Endpoints existants `Workspace*`** : à scanner pour s'assurer qu'aucun endpoint d'écriture (création de dossier, upload, invitation…) n'accepte un workspace `PENDING_PAYMENT` — invariant SF-156-00 §1.
- [x] **Webhooks Stripe existants** : `customer.subscription.created` est déjà branché pour l'onboarding initial — l'extension doit gérer le nouveau cas « workspace pré-existant en `PENDING_PAYMENT` » sans casser le cas onboarding.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|---|---|---|
| `WorkspaceController.createWorkspace` | Oui | **Cœur de la SF** — gate + validation + état |
| `WorkspaceService.create` | Oui | Création en `PENDING_PAYMENT` + appel Stripe |
| Listings de workspaces (F-154 switcher, etc.) | Oui | Filtrer `status != CANCELLED` — `PENDING_PAYMENT` reste **visible** (l'avocat doit le voir avec sa banderole) |
| Endpoints d'écriture sur dossiers / membres | Oui | Ajouter un guard `workspace.status == ACTIVE` (rejet 409 si `PENDING_PAYMENT`) |
| Webhook handler Stripe | Oui | Étendre pour gérer la transition `PENDING_PAYMENT → ACTIVE` |
| Cron de nettoyage `PENDING_PAYMENT` 24 h | Nouveau | `@Scheduled` hourly |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette SF.

## Conformité F-IA-04

- [x] **Non applicable** — SF backend pure, aucun outil décisionnel concerné.

## Champs IA à extraire

- [x] **Aucun pré-remplissage IA** — SF de gating de plan + état workspace.

## Critères d'acceptation

- [ ] **CA1** — `POST /api/v1/workspaces` avec OWNER TEAM ou PRO + plan ∈ {TEAM, PRO} → `201` + `{ workspaceId, stripeCheckoutUrl }` + workspace créé en `status = PENDING_PAYMENT`.
- [ ] **CA2** — `POST /api/v1/workspaces` avec OWNER FREE → `403` + message explicite « TEAM ou PRO requis ».
- [ ] **CA3** — `POST /api/v1/workspaces` avec OWNER SOLO → `403` + même message.
- [ ] **CA4** — `POST /api/v1/workspaces` avec `plan` ∈ {FREE, SOLO, null, "INVALID"} → `400` + message explicite.
- [ ] **CA5** — Webhook `customer.subscription.created` pour un workspace `PENDING_PAYMENT` → `status` passe à `ACTIVE`. Deuxième réception du même webhook → no-op (idempotence).
- [ ] **CA6** — Webhook `customer.subscription.deleted` / `invoice.payment_failed` pour un workspace `PENDING_PAYMENT` → `status` passe à `CANCELLED`.
- [ ] **CA7** — Workspace `PENDING_PAYMENT` depuis > 24 h → supprimé automatiquement par le cron `WorkspacePendingPaymentCleanupJob`.
- [ ] **CA8** — Tout endpoint d'écriture sur dossier / membre rejette un workspace `PENDING_PAYMENT` avec `409 Conflict` (« Workspace en attente d'activation »).
- [ ] **CA9** — Isolation workspace : un OWNER A qui crée le workspace B (`PENDING_PAYMENT`) ne voit pas, depuis A, les dossiers / membres de B (test d'isolation cross-workspace).
- [ ] **CA10** — Échec côté Stripe à la création de session Checkout → rollback transactionnel (aucun workspace créé), réponse `502` + erreur loggée.

## Périmètre

### Hors scope (explicite)

- **Duplication de données** entre workspaces (le nom historique « clone » ne désigne aucune copie — invariant SF-156-00 §5).
- **Downgrade** d'un workspace FREE existant vers payant (couvert par F-16 / F-58 billing).
- **Changement de plan ultérieur** sur un workspace TEAM↔PRO (couvert par le flux billing standard).
- **Limite de nombre de workspaces par OWNER** — pas de hard cap en V1, on laisse le pricing (chaque workspace = 1 abonnement) faire le régulateur naturel.
- **UI / dialog / pages d'erreur** — couverts par SF-156-02.

## Valeurs initiales

| Champ | Valeur | Règle |
|---|---|---|
| `Workspace.status` à la création | `PENDING_PAYMENT` | constante enum |
| `Workspace.planCode` à la création | `null` (à figer par le webhook) ou `TEAM`/`PRO` selon le choix utilisateur | à trancher en dev — préférence : pré-positionner à la valeur choisie pour cohérence UI |
| `Subscription` | non créé à la création du workspace — créé par le webhook `customer.subscription.created` | — |

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs autorisées |
|---|---|---|
| `name` | Oui | string non vide, ≤ 100 caractères, trim |
| `plan` | Oui | enum `WorkspacePlan` ∈ {`TEAM`, `PRO`} — rejet `FREE`, `SOLO`, null |

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum | Status |
|---|---|---|---|---|
| `POST` | `/api/v1/workspaces` | Oui | OWNER (de son workspace courant) | **Modifié** par SF-156-01 — gate + validation `plan` + retour `stripeCheckoutUrl` |

### Contrat API figé (parallélisation back / front)

**Requête** :

```http
POST /api/v1/workspaces
Content-Type: application/json
Authorization: Bearer <session>

{
  "name": "Cabinet Bordeaux",
  "plan": "TEAM"
}
```

**Réponse 201** :

```json
{
  "workspaceId": "uuid-v4",
  "stripeCheckoutUrl": "https://checkout.stripe.com/c/pay/cs_...",
  "status": "PENDING_PAYMENT"
}
```

**Réponse 403 (OWNER FREE/SOLO)** :

```json
{
  "error": "Forbidden",
  "message": "La création d'un workspace supplémentaire nécessite un abonnement TEAM ou PRO",
  "code": "PLAN_REQUIRED"
}
```

**Réponse 400 (plan invalide)** :

```json
{
  "error": "Bad Request",
  "message": "Plan invalide — seuls TEAM et PRO sont acceptés pour un workspace supplémentaire",
  "code": "PLAN_INVALID"
}
```

### Tables impactées

| Table | Opération | Notes |
|---|---|---|
| `workspaces` | **ALTER** (Liquibase) | ajout valeur `PENDING_PAYMENT` à l'enum `status` (ou colonne `status` VARCHAR avec contrainte). À vérifier : si `Workspace.status` est déjà un VARCHAR libre, pas de migration nécessaire — juste un commentaire. **Si enum strict**, migration `ALTER TYPE workspace_status ADD VALUE 'PENDING_PAYMENT'`. |
| `subscriptions` | Inchangée | la création de la `Subscription` se fait dans le webhook handler, pas à la création du workspace. |

### Migration Liquibase

- [x] **Conditionnelle** — confirmer en dev si `Workspace.status` accepte déjà des valeurs arbitraires (VARCHAR) ou si une migration enum est requise. Si migration : numérotation prochaine disponible (à vérifier au dev), changeset idempotent (`preCondition` `runOnChange:false`).

### Composants Java

- `WorkspaceController.createWorkspace` — gate plan, validation `plan`, appel `WorkspaceService.createPendingPayment`.
- `WorkspaceService` — nouvelle méthode `createPendingPayment(currentUser, name, plan)` qui crée le workspace en `PENDING_PAYMENT` + appelle `StripeCheckoutService.createSubscriptionSession(...)`.
- `StripeCheckoutService.createSubscriptionSession` — nouveau (ou extension) pour créer une session Checkout en mode `subscription` avec les bons `success_url` / `cancel_url`.
- `StripeWebhookHandler` — étendre pour gérer la transition `PENDING_PAYMENT → ACTIVE` sur `customer.subscription.created` et `→ CANCELLED` sur `customer.subscription.deleted` / `invoice.payment_failed`.
- `WorkspacePendingPaymentCleanupJob` — nouveau `@Scheduled(cron = "0 0 * * * *")` (hourly) qui supprime les `PENDING_PAYMENT` > 24 h.
- `WorkspaceWriteGuard` — extension à factoriser : tout endpoint d'écriture sur dossier / membre vérifie `workspace.status == ACTIVE`. **Option simple** : annotation `@ActiveWorkspaceOnly` + AOP interceptor, ou check dans `WorkspaceResolver` (à trancher au dev).

## Plan de test

### Tests unitaires

- [ ] `WorkspaceServiceTest.createPendingPayment_ownerFree_throws403` — OWNER FREE → exception 403.
- [ ] `WorkspaceServiceTest.createPendingPayment_ownerSolo_throws403` — OWNER SOLO → exception 403.
- [ ] `WorkspaceServiceTest.createPendingPayment_ownerTeam_createsPendingPayment` — OWNER TEAM → workspace créé en `PENDING_PAYMENT` + appel Stripe mocké.
- [ ] `WorkspaceServiceTest.createPendingPayment_ownerPro_createsPendingPayment` — idem PRO.
- [ ] `WorkspaceServiceTest.createPendingPayment_invalidPlan_throws400` — plan ∈ {FREE, SOLO, null, "INVALID"} → 400.
- [ ] `WorkspaceServiceTest.createPendingPayment_stripeFailure_rollback` — Stripe throw → workspace **non persisté**.
- [ ] `StripeWebhookHandlerTest.subscriptionCreated_pendingPayment_activates` — webhook → `status = ACTIVE`.
- [ ] `StripeWebhookHandlerTest.subscriptionCreated_idempotent` — double réception → un seul `update`.
- [ ] `StripeWebhookHandlerTest.subscriptionDeleted_pendingPayment_cancels` — webhook → `status = CANCELLED`.
- [ ] `WorkspacePendingPaymentCleanupJobTest.deletes24hOldPendingPayment` — workspace `PENDING_PAYMENT` > 24 h → supprimé.

### Tests d'intégration

- [ ] `WorkspaceControllerIT.POST_workspaces_ownerTeam_returns201` — OWNER TEAM, plan TEAM → 201 + `stripeCheckoutUrl` non nul + workspace en BDD `PENDING_PAYMENT`.
- [ ] `WorkspaceControllerIT.POST_workspaces_ownerFree_returns403` — OWNER FREE → 403 + body avec `code: PLAN_REQUIRED`.
- [ ] `WorkspaceControllerIT.POST_workspaces_ownerSolo_returns403` — OWNER SOLO → 403.
- [ ] `WorkspaceControllerIT.POST_workspaces_planFree_returns400` — plan FREE dans le body → 400.
- [ ] `WorkspaceControllerIT.POST_workspaces_writeOnPendingPayment_returns409` — créer un dossier sur un workspace `PENDING_PAYMENT` → 409.
- [ ] `WorkspaceControllerIT.POST_workspaces_isolation_otherOwnerSeesNothing` — un autre OWNER ne voit ni ne peut accéder au workspace `PENDING_PAYMENT`.

### Isolation workspace

- [x] **Applicable** — la nouvelle SF crée un workspace. Le test d'isolation existant doit être étendu pour `PENDING_PAYMENT` (un workspace en `PENDING_PAYMENT` reste isolé exactement comme un workspace `ACTIVE`).

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Plans / limites** — nouvelle gate plan (TEAM/PRO), nouvelle valeur d'enum `PENDING_PAYMENT`, comportement différent par plan. Composants impactés : `WorkspaceController`, `WorkspaceService`, `StripeCheckoutService`, `StripeWebhookHandler`, `WorkspacePendingPaymentCleanupJob`, tout endpoint d'écriture sur dossier/membre (guard `ACTIVE` only).
- [x] **Workspace context** — nouvel état `PENDING_PAYMENT` qui influence l'accès. Composants impactés : listings de workspaces (cf. F-154), résolution workspace dans les services.
- [ ] Auth / Principal — non touché (l'authentification reste OIDC).
- [ ] Navigation / routing — non applicable (backend).

### Composants / endpoints potentiellement impactés

| Composant / Endpoint | Impact | Test de non-régression prévu |
|---|---|---|
| `WorkspaceController.createWorkspace` | Cœur du changement | IT dédié + UT |
| Tout endpoint POST/PUT/PATCH sur `workspace_id` (dossiers, membres, uploads, etc.) | Ajout du guard `status == ACTIVE` | Nouveau IT générique sur 2-3 endpoints représentatifs (`CaseFileController.create`, `WorkspaceInvitationController.create`) |
| F-154 switcher (listing workspaces) | Doit inclure `PENDING_PAYMENT` dans la liste (visible pour l'avocat) mais le composant frontend SF-156-02 le marque visuellement | Test SF-154 existant à vérifier vert |
| Webhook Stripe `customer.subscription.created` | Étendu pour gérer le cas `PENDING_PAYMENT → ACTIVE` (en plus du cas onboarding initial) | UT + IT |

### Smoke tests E2E concernés

- [x] `e2e/smoke/workspace.spec.ts` — touchée (workspace context). Exécutés post-déploiement staging via `smoke.yml`.
- [x] `e2e/smoke/auth.spec.ts` — non touchée (pas de changement auth).

## Dépendances

### Subfeatures bloquantes

- Aucune. F-154 (switcher), F-123 (pricing), `StripeCustomerService` (intégration Stripe) sont tous Terminés.

### Pré-requis

- ✅ F-154 workspace switcher (Terminée).
- ✅ F-123 pricing SOLO/TEAM/PRO (déployé prod).
- ✅ Intégration Stripe Checkout + webhooks (Terminée — réutilisée).

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` touchée.

## Notes et décisions

- **Pourquoi pas de hard cap sur le nombre de workspaces par OWNER** : le pricing (1 abonnement = 1 workspace) joue déjà le régulateur. Un PRO qui veut 10 workspaces paie 10 abonnements PRO — c'est cohérent avec le modèle. Si abus constaté (signal terrain), on ajustera en V2.
- **Pourquoi rollback transactionnel sur échec Stripe** : créer le workspace puis annuler si Stripe échoue génèrerait des zombies systématiques sur les pannes Stripe. Mieux vaut une erreur 502 explicite et aucun effet de bord.
- **Pourquoi 24 h pour le cleanup `PENDING_PAYMENT`** : l'avocat peut abandonner l'onglet Stripe puis revenir le lendemain pour payer. 24 h couvre ce cas tout en évitant l'accumulation indéfinie.
- **Décision plan figé à la création vs au webhook** : on pré-positionne `planCode = "TEAM"` (ou `"PRO"`) à la création du workspace en `PENDING_PAYMENT` pour que l'UI puisse afficher « Cabinet Bordeaux (TEAM, en attente de paiement) » sans attendre le webhook. Le webhook confirme et active.
