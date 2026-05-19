# Mini-spec — [F-156 / SF-156-02] Frontend — dialog sélecteur de plan + Stripe redirect + banderole d'état

> Produite à partir de `project-governance/templates/subfeature-template.md`.
> Cadrage cohérence : `SF-156-00-coherence.md` (verdict GO).
> Cadrage écran : `SF-156-00b-ux-coherence.md` (verdict GO).
> Contrat API figé dans `SF-156-01-backend-gate-plan.md` § « Contrat API figé ».

## Identifiant

`F-156 / SF-156-02`

## Feature parente

`F-156` — Création d'un workspace supplémentaire — réservée aux plans TEAM / PRO.

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-156-02-frontend-dialog-stripe`

## Objectif

Restreindre côté frontend la création d'un workspace supplémentaire aux OWNER TEAM/PRO, enrichir le dialog de création d'un sélecteur de plan obligatoire (cards TEAM/PRO), rediriger vers Stripe Checkout, et signaler l'état `PENDING_PAYMENT` du nouveau workspace via une banderole tant que l'activation Stripe n'est pas confirmée.

## Comportement attendu

### Cas nominal

1. L'OWNER d'un workspace **TEAM** ou **PRO** ouvre le **workspace switcher** (header) → dropdown affiche la liste de ses workspaces **+ une option « + Créer un workspace »** au bas du menu.
2. Clic « + Créer un workspace » → `WorkspaceCreateDialogComponent` s'ouvre (`MatDialog`, taille moyenne).
3. Dialog : champ texte **« Nom du workspace »** (obligatoire, ≤ 100 caractères) + **cards plan TEAM / PRO** (cliquables, mutuellement exclusives, affichant prix + nb sièges max via le pricing F-123) + bouton **« Créer + payer »** (disabled tant que nom + plan ne sont pas tous deux renseignés) + bouton « Annuler ».
4. L'avocat saisit nom + sélectionne plan + clique « Créer + payer » → `POST /api/v1/workspaces` avec `{ name, plan }`.
5. Sur `201` : `window.location.href = response.stripeCheckoutUrl` (redirection complète vers Stripe, pas un `router.navigate`).
6. Stripe Checkout traite le paiement → redirige vers `https://<host>/workspaces?workspace_created=success&workspace_id=<id>`.
7. Le frontend (handler de route ou guard sur `/workspaces`) détecte le query param → **bascule automatiquement** sur le nouveau workspace via le `WorkspaceSwitcherService` → navigate vers le dashboard du nouveau workspace.
8. Sur le dashboard du nouveau workspace : une **banderole d'état `WorkspaceStatusBanner`** s'affiche en haut du contenu, libellé selon `workspace.status` :
   - `PENDING_PAYMENT` (webhook pas encore reçu) → bandeau ambre « Paiement en cours d'activation… ».
   - `ACTIVE` (webhook reçu) → bandeau vert « Bienvenue dans <nom workspace> — invitez vos premiers membres », auto-dismissé après 5 s.
9. Tant que `PENDING_PAYMENT`, les actions d'écriture (créer dossier, inviter, uploader) sont **désactivées visuellement** (boutons grisés + tooltip explicatif).

### Cas d'erreur

| Situation | Comportement | UI |
|---|---|---|
| OWNER FREE ou SOLO ouvre le switcher | L'option « + Créer un workspace » n'apparaît pas dans le dropdown | dropdown sans l'option |
| OWNER FREE / SOLO bricole l'URL vers `/workspaces/new` (ou équivalent) | Route protégée par `WorkspacePlanGuard` qui redirige vers une **page d'erreur dédiée** `WorkspaceUpgradeRequiredPageComponent` | page avec titre « Création d'un workspace supplémentaire », sous-titre « Cette fonctionnalité nécessite TEAM ou PRO », CTA « Découvrir les plans » → ouvre `/pricing` en **nouvelle page** |
| `POST /workspaces` → `403 PLAN_REQUIRED` (cas inattendu) | Dialog ferme + `MatSnackBar` rouge « Plan TEAM ou PRO requis » + redirection vers la page d'erreur dédiée | snackbar 6 s |
| `POST /workspaces` → `400 PLAN_INVALID` | `MatSnackBar` rouge « Plan invalide, veuillez sélectionner TEAM ou PRO » | snackbar 6 s |
| `POST /workspaces` → `502/503` (Stripe down) | `MatSnackBar` rouge « Service de paiement indisponible, réessayez dans quelques minutes » + dialog reste ouvert | snackbar 6 s, retry manuel possible |
| Stripe → `cancel_url` (paiement abandonné) | Frontend détecte `workspace_created=cancelled` → snackbar « Création annulée — aucun frais prélevé » + bascule sur le workspace d'origine (pas sur le `PENDING_PAYMENT` qui sera nettoyé par le cron backend) | snackbar 4 s |
| Webhook Stripe en retard (> 30 s après redirect retour) | Banderole reste sur « Paiement en cours d'activation… » indéfiniment ; un polling léger toutes les 10 s rafraîchit le statut du workspace (option simple) OU le frontend laisse l'avocat F5 manuellement (option minimaliste) — à trancher au dev | banderole persistante |
| Mobile < 600 px | Cards TEAM / PRO empilées verticalement dans le dialog | layout responsive |

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **`WorkspaceSwitcherComponent`** (F-154) — modifié pour calculer conditionnellement la visibilité de l'option « + Créer ».
- [x] **`WorkspaceCreateDialogComponent`** — modifié pour le sélecteur de plan.
- [x] **Page pricing** `/pricing` (F-123) — réutilisée telle quelle comme cible du CTA upgrade.
- [x] **Routes `/workspaces*`** — éventuel ajout d'une route `/workspaces/new` (page d'erreur si FREE/SOLO bricolent l'URL).
- [x] **Tous les boutons d'action d'écriture sur dossier / membre / upload** — doivent être désactivés visuellement quand `workspace.status == PENDING_PAYMENT` (avec tooltip).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|---|---|---|
| `WorkspaceSwitcherComponent` template | Oui | Condition `*ngIf="canCreateWorkspace()"` sur l'option « + Créer » |
| `WorkspaceCreateDialogComponent` template + ts | Oui | Sélecteur de plan obligatoire, `MatRadioButton` ou cards custom |
| `WorkspaceCreateDialogService` (si existe) | Oui | Inclure `plan` dans le payload POST |
| Nouveau `WorkspaceStatusBanner` (composant) | Oui | Bandeau ambre/vert/rouge selon `status` |
| Nouveau `WorkspaceUpgradeRequiredPageComponent` | Oui | Page d'erreur FREE/SOLO |
| Nouveau `WorkspacePlanGuard` (canActivate sur `/workspaces/new`) | Oui | Redirige FREE/SOLO vers la page upgrade |
| Boutons d'écriture sur dossiers / membres | Oui | Directive partagée `[appDisabledIfPendingPayment]` (à trancher au dev) OU check inline dans chaque composant via un service `WorkspaceStateService` |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette SF.

## Conformité F-IA-04

- [x] **Non applicable** — aucun composant décisionnel touché.

## Champs IA à extraire

- [x] **Aucun pré-remplissage IA**.

## Critères d'acceptation

- [ ] **CA1** — OWNER TEAM/PRO : option « + Créer un workspace » visible dans le dropdown switcher.
- [ ] **CA2** — OWNER FREE/SOLO : option « + Créer un workspace » **invisible** dans le dropdown.
- [ ] **CA3** — Dialog avec sélecteur de plan obligatoire — bouton « Créer + payer » disabled tant que nom + plan non choisis.
- [ ] **CA4** — Cards TEAM/PRO affichent prix + nb sièges max issus du pricing F-123 (pas de hardcode).
- [ ] **CA5** — Submit avec OWNER TEAM/PRO + plan valide → POST réussi → redirection navigateur vers `stripeCheckoutUrl`.
- [ ] **CA6** — Retour Stripe `?workspace_created=success&workspace_id=<id>` → switch automatique sur le nouveau workspace + dashboard affiché.
- [ ] **CA7** — `WorkspaceStatusBanner` affichée tant que `workspace.status == PENDING_PAYMENT`, libellé « Paiement en cours d'activation… ».
- [ ] **CA8** — Quand `workspace.status` passe à `ACTIVE` (webhook reçu), banderole « Bienvenue » 5 s puis disparaît.
- [ ] **CA9** — Boutons d'écriture (créer dossier, inviter, uploader) désactivés + tooltip quand `workspace.status == PENDING_PAYMENT`.
- [ ] **CA10** — Retour Stripe `?workspace_created=cancelled` → snackbar + bascule sur le workspace d'origine.
- [ ] **CA11** — OWNER FREE/SOLO qui navigue manuellement vers `/workspaces/new` → page `WorkspaceUpgradeRequiredPageComponent` (pas de 404 brut, pas de 403 silencieux).
- [ ] **CA12** — Mobile < 600 px : cards TEAM/PRO empilées verticalement, dialog reste utilisable.
- [ ] **CA13** — Aucune utilisation de `window.alert` / `window.confirm` ; toutes les erreurs via `MatSnackBar`.

## Périmètre

### Hors scope (explicite)

- Backend (gate, validation, état, Stripe Checkout, webhook) — couvert par SF-156-01.
- Duplication de données entre workspaces.
- Refonte du switcher F-154 (uniquement ajout conditionnel de l'option « + Créer »).
- Refonte de la page `/pricing` F-123 (uniquement réutilisée comme cible).
- Polling en temps réel du statut du workspace (option minimaliste : refresh manuel ou polling léger 10 s — à trancher au dev).

## Technique

### Composants Angular

- `WorkspaceSwitcherComponent` (modifié) — méthode `canCreateWorkspace(): boolean` (= `currentOwnerPlan ∈ {TEAM, PRO}`).
- `WorkspaceCreateDialogComponent` (modifié) — ajout du sélecteur de plan (cards) + bouton désactivé tant que invalid.
- `WorkspaceCreateService` (ou méthode existante) — POST avec `{ name, plan }`, redirige `window.location.href` vers `stripeCheckoutUrl`.
- `WorkspaceStatusBanner` (nouveau, standalone) — bandeau ambre/vert/rouge selon `status` reçu en `@Input`.
- `WorkspaceUpgradeRequiredPageComponent` (nouveau, standalone) — page d'erreur FREE/SOLO avec CTA pricing.
- `WorkspacePlanGuard` (nouveau, `CanActivateFn`) — sur `/workspaces/new` : si OWNER FREE/SOLO, redirige vers `WorkspaceUpgradeRequiredPageComponent`.
- `WorkspaceStateService` (extension si existant) — expose `workspace.status` réactif pour piloter la désactivation des actions d'écriture + la banderole.
- Directive partagée `appDisabledIfPendingPayment` (à trancher au dev) — gris + tooltip sur boutons d'écriture si `PENDING_PAYMENT`.

### Routes Angular

- `/workspaces/new` (nouveau, protégé par `WorkspacePlanGuard`) — page d'erreur FREE/SOLO. **Pas** la route de création réelle (la création passe par le dialog du switcher).
- Handler sur `/workspaces` ou la route racine pour détecter `?workspace_created=success|cancelled&workspace_id=<id>` et déclencher la bascule / snackbar.

### Modèles TypeScript

- `WorkspacePlan` enum : `'FREE' | 'SOLO' | 'TEAM' | 'PRO'`.
- `WorkspaceStatus` enum : extension avec `'PENDING_PAYMENT'` (en plus de `'ACTIVE'` / `'CANCELLED'` existants).
- `WorkspaceCreateRequest` : `{ name: string; plan: 'TEAM' | 'PRO' }`.
- `WorkspaceCreateResponse` : `{ workspaceId: string; stripeCheckoutUrl: string; status: WorkspaceStatus }`.

## Plan de test

### Tests unitaires Jest

- [ ] `WorkspaceSwitcherComponent.spec.ts` — `canCreateWorkspace()` : true pour TEAM/PRO, false pour FREE/SOLO. Option visible/invisible dans le template selon le retour.
- [ ] `WorkspaceCreateDialogComponent.spec.ts` — bouton submit disabled tant que `name` vide OU plan non choisi ; enabled quand les deux sont renseignés. POST avec `{ name, plan }`. `window.location.href` mis à jour avec `stripeCheckoutUrl` sur 201.
- [ ] `WorkspaceCreateDialogComponent.spec.ts` — sur 403, snackbar + dialog ferme.
- [ ] `WorkspaceStatusBanner.spec.ts` — libellé selon `status` (PENDING_PAYMENT / ACTIVE / CANCELLED). Auto-dismiss 5 s en mode ACTIVE.
- [ ] `WorkspacePlanGuard.spec.ts` — OWNER TEAM/PRO → autorisé. OWNER FREE/SOLO → redirigé vers `/workspaces/upgrade-required` (ou route équivalente).
- [ ] `WorkspaceUpgradeRequiredPageComponent.spec.ts` — affiche titre + CTA, CTA ouvre `/pricing` en `target="_blank"`.

### Tests d'intégration / E2E

- [x] Couverts par les smoke tests `e2e/smoke/workspace.spec.ts` post-déploiement staging. Le flow complet (création → Stripe → retour → switch) ne peut pas être testé localement (Stripe externe) — c'est testé en staging avec compte Stripe test.

### Isolation workspace

- [x] **Applicable** — la bascule automatique sur le nouveau workspace ne doit pas exposer de données du précédent. Test : après bascule, le dashboard du nouveau workspace est **vide** (pas de dossiers reportés de l'ancien).

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Plans / limites** — visibilité conditionnelle de l'option « + Créer » selon plan, sélecteur de plan obligatoire, page d'erreur FREE/SOLO. Composants impactés : `WorkspaceSwitcherComponent`, `WorkspaceCreateDialogComponent`, `WorkspacePlanGuard`, `WorkspaceUpgradeRequiredPageComponent`.
- [x] **Workspace context** — bascule automatique post-création, banderole d'état, désactivation des actions d'écriture sur `PENDING_PAYMENT`. Composants impactés : `WorkspaceStateService`, `WorkspaceStatusBanner`, tous les composants exposant des boutons d'écriture.
- [x] **Navigation / routing** — nouvelle route `/workspaces/new` + `/workspaces/upgrade-required` (ou équivalents) + `WorkspacePlanGuard`. Composants impactés : `AppRoutingModule` (ou config standalone).
- [ ] Auth / Principal — non touché.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|---|---|---|
| `WorkspaceSwitcherComponent` | Ajout option conditionnelle | Spec existant + spec SF-156-02 |
| `WorkspaceCreateDialogComponent` | Sélecteur de plan + nouveau payload | Spec dédié SF-156-02 |
| Boutons d'écriture (CaseFileCreate, WorkspaceInviteDialog, FileUpload…) | Désactivation sur `PENDING_PAYMENT` | Test de smoke E2E sur 1-2 composants représentatifs |
| Route `/workspaces` (root) | Handler query param `workspace_created=success` | Spec route handler |

### Smoke tests E2E concernés

- [x] `e2e/smoke/workspace.spec.ts` — flow complet (création → Stripe test → switch) exécuté post-déploiement staging.
- [x] `e2e/smoke/navigation.spec.ts` — navigation switcher / dropdown.
- [ ] `e2e/smoke/auth.spec.ts` — non touchée.

## Dépendances

### Subfeatures bloquantes

- **SF-156-01 backend** — contrat API doit être livré et déployé staging pour que le frontend puisse tester de bout en bout. **Toutefois**, le dev frontend peut démarrer **en parallèle** (contrat figé dans SF-156-01 § « Contrat API figé ») avec un mock local — c'est le mode de travail standard du projet (cf. mémoire `feedback_parallel_frontback_default`).

### Pré-requis

- ✅ F-154 workspace switcher.
- ✅ F-123 pricing (données prix + sièges max pour les cards).
- ✅ `MatDialog` + `MatSnackBar` (Angular Material).

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` touchée.

## Notes et décisions

- **Pourquoi `window.location.href` et pas `router.navigate`** : Stripe Checkout est une URL externe — `router.navigate` ne sortirait pas de l'SPA. `window.location.href` est le pattern standard.
- **Pourquoi une page d'erreur dédiée FREE/SOLO et pas juste un toast** : l'avocat qui bricole l'URL mérite une explication claire + un CTA, pas un message éphémère qui disparaît.
- **Pourquoi `target="_blank"` sur le CTA pricing** : préserve le contexte de l'app (l'avocat peut consulter les plans puis revenir sans perdre où il en était).
- **Polling vs refresh manuel pour le statut PENDING_PAYMENT → ACTIVE** : à trancher au dev. Polling 10 s = simple, peu coûteux. Si on a déjà un SSE workspace events, on peut s'y brancher. Le doc liste les 2 options.
- **Désactivation des actions d'écriture sur `PENDING_PAYMENT`** : la directive partagée est plus propre mais nécessite de l'ajouter sur N composants ; le check inline via `WorkspaceStateService` est plus simple à démarrer. À trancher au dev en fonction du nombre de composants concernés.
