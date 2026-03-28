# SF-49-02 — Frontend top-up tokens

**Feature parente :** F-49 — Top-up de crédits tokens
**Branche :** feat/SF-49-02-frontend-topup
**Statut :** ready
**Date de création :** 2026-03-28

---

## Objectif

Permettre à un OWNER ou ADMIN d'acheter un pack de tokens supplémentaires depuis la page abonnement via 3 cartes de pack (1M / 5M / 20M tokens), avec redirection Stripe et feedback visuel post-paiement.

---

## Comportement attendu

### Cas nominal

1. L'utilisateur OWNER ou ADMIN navigue vers la page abonnement (`/workspace/billing`).
2. Une section "Acheter des tokens supplémentaires" est affichée sous la grille des plans, avec 3 cartes :
   - **1M tokens** — 9,90 €
   - **5M tokens** — 39,90 €
   - **20M tokens** — 129,90 €
3. L'utilisateur clique sur un pack → `BillingService.createTopupSession(packCode)` → `POST /api/v1/stripe/topup-session` → `{ checkoutUrl }`.
4. Le navigateur est redirigé vers `checkoutUrl` (Stripe Checkout).
5. Après paiement réussi, Stripe redirige vers `/workspace/billing?topup=success`.
6. La page affiche un snackbar : "Tokens ajoutés à votre compte !"
7. Si paiement annulé, Stripe redirige vers `/workspace/billing?topup=canceled`.
8. La page affiche un snackbar : "Achat de tokens annulé."

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Erreur HTTP lors de la création de session | Snackbar d'erreur : "Erreur lors de la redirection vers le paiement." — bouton réactivé |
| Clic sur un second pack pendant qu'un achat est en cours | Bouton du pack en cours : spinner — autres packs : désactivés |

---

## Critères d'acceptation

- [ ] Section top-up visible sur la page `/workspace/billing` pour OWNER/ADMIN
- [ ] 3 cartes de pack affichées avec label, tokens, prix
- [ ] Clic sur un pack → redirection Stripe (appel `POST /api/v1/stripe/topup-session`)
- [ ] Spinner sur le pack cliqué + désactivation des autres pendant la redirection
- [ ] `?topup=success` → snackbar succès
- [ ] `?topup=canceled` → snackbar annulation
- [ ] Coexistence avec les params `?success=true` / `?canceled=true` (abonnement plan) sans interférence

---

## Périmètre

### Hors scope

- Affichage du solde de crédits restants (nécessite extension backend `/api/v1/admin/usage`)
- Restriction MEMBER côté frontend (le backend renvoie 403 — l'UI ne cache pas le bouton par rôle dans cette itération)
- Expiration ou remboursement de crédits

---

## Technique

### Endpoint consommé

| Méthode | URL | Auth | Body | Réponse |
|---------|-----|------|------|---------|
| POST | `/api/v1/stripe/topup-session` | Oui | `{ packCode: string }` | `{ checkoutUrl: string }` |

### Composants Angular modifiés

- **`BillingService`** — ajout de `createTopupSession(packCode: string): Observable<{ checkoutUrl: string }>`
- **`WorkspaceBillingComponent`** — ajout d'une section top-up : 3 cartes, signal `buying`, gestion `?topup=success/canceled`

### Modèles

Aucun nouveau modèle. Le type de retour est `{ checkoutUrl: string }` déjà utilisé par `createCheckoutSession`.

---

## Plan de test

### Tests unitaires

- [ ] `BillingService.createTopupSession` — appelle `POST /api/v1/stripe/topup-session` avec `{ packCode }`

### Tests du composant

- [ ] `WorkspaceBillingComponent` — `?topup=success` → snackbar succès affiché
- [ ] `WorkspaceBillingComponent` — `?topup=canceled` → snackbar annulation affiché
- [ ] `WorkspaceBillingComponent` — `buyTopup('TOKENS_1M')` appelle `createTopupSession` et redirige

### Isolation workspace

- Non applicable — l'isolation est garantie côté backend (403 si non OWNER/ADMIN)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Navigation / routing frontend** — ajout de la gestion des params `?topup=success/canceled`

### Composants existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|-----------------|------------------------------|
| `WorkspaceBillingComponent` | Gestion de query params étendue — vérifier non-interférence avec `?success=true`/`?canceled=true` existants | Test du composant sur les deux types de params |

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — la page billing n'est pas couverte par les smoke tests actuels (`auth`, `workspace`, `navigation`)

---

## Dépendances

### Subfeatures bloquantes

- SF-49-01 — statut : done (backend top-up implémenté et mergé)

### Questions ouvertes impactées

- Aucune

---

## Notes et décisions

- Le signal `buying` (analogue à `upgrading`) prend le `packCode` en cours d'achat pour afficher le spinner sur la bonne carte.
- Les packs sont définis en dur dans le composant (label, packCode, tokens, price) — pas de chargement dynamique depuis le backend.
- La section top-up est toujours visible sur la page billing (pas de condition sur le quota atteint) — simplicité maximale.
- La redirection Stripe success/cancel pour les topups pointe sur `/workspace/billing?topup=success|canceled` pour éviter la confusion avec `?success=true` utilisé par les upgrades de plan.
