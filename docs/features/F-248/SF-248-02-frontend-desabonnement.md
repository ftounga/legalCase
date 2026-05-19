# Mini-spec — F-248 / SF-248-02 — Frontend page publique de désinscription

## Identifiant

`F-248 / SF-248-02`

## Feature parente

`F-248` — Désabonnement des emails non-transactionnels (conformité RGPD / LCEN)

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-248-02-frontend-email-unsubscribe`

---

## Objectif

Fournir une page publique `/unsubscribe` qui, à partir d'un token, désinscrit l'utilisateur des emails non-transactionnels et lui permet aussi de se réinscrire.

---

## Comportement attendu

### Cas nominal

1. L'utilisateur clique sur le lien `/unsubscribe?token={uuid}` depuis un email.
2. La page (publique, hors `ShellComponent`, sans `authGuard`) lit le `token` dans les query params et appelle `GET /api/v1/public/email/subscription-status?token=`.
3. **Si `optedOut === false`** : la page déclenche immédiatement la désinscription (`POST .../unsubscribe`) puis affiche la confirmation : « Vous êtes désinscrit des emails d'information de LegalCase. » + précision « Les emails liés à vos dossiers (analyse terminée, alertes de délai) continuent d'être envoyés. » + un bouton secondaire « Me réabonner ».
4. **Si `optedOut === true`** (l'utilisateur rouvre le lien) : la page affiche « Vous êtes désinscrit. » + un bouton « Me réabonner » (`POST .../resubscribe`).
5. Le clic sur « Me réabonner » / « Me désinscrire » bascule l'état et met à jour le message affiché.
6. État terminal : page de confirmation. Un lien discret « Retour à LegalCase » (vers `/`) est proposé, non obligatoire au parcours.

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| `token` absent de l'URL | Message : « Lien de désinscription invalide ou incomplet. » — pas d'appel backend |
| `token` inconnu (`404`) | Message : « Ce lien de désinscription n'est pas valide. » |
| Appel backend en échec réseau / `5xx` | Message d'erreur + bouton « Réessayer » |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils / pays / domaines** — non applicable : page publique transversale.
- [x] **Autres UI patterns** — la page suit le pattern des **pages publiques token-based existantes** (`verify-email`, `reset-password`, `public-share`) : route top-level hors shell, lecture d'un token en query param, page autonome mono-message. Aucun nouveau pattern partagé introduit.
- [x] **Autres flows transversaux** — Navigation : nouvelle route publique top-level `/unsubscribe` à ajouter dans `app.routes.ts`, au même niveau que `verify-email` / `reset-password` (hors bloc `authGuard`).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|---|---|---|
| Routes publiques `verify-email` / `reset-password` / `public-share` | Oui | Pattern réutilisé pour `/unsubscribe` (route, structure de composant). |

### Décision

- [x] Étendu à la cible applicable ; aucun composant partagé nouveau → pas de dette de convergence.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : page publique de gestion d'abonnement email, ce n'est pas une section décisionnelle.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — justification : aucun champ saisissable, aucune extraction IA.

---

## Critères d'acceptation

- [ ] `/unsubscribe?token=...` se charge sans authentification, hors `ShellComponent` (pas de menu latéral).
- [ ] Avec un token valide non désinscrit, la page désinscrit l'utilisateur et affiche la confirmation.
- [ ] La confirmation précise que les emails transactionnels continuent.
- [ ] Avec un token déjà désinscrit, la page propose « Me réabonner » et le réabonnement fonctionne.
- [ ] Sans token dans l'URL, un message d'erreur explicite s'affiche, sans appel backend.
- [ ] Un token inconnu (`404`) affiche un message d'erreur dédié.
- [ ] La page est bidirectionnelle : on peut désinscrire puis réabonner depuis la même page.

---

## Périmètre

### Hors scope (explicite)

- Toggle de réactivation dans un écran de préférences in-app (ajustement A2 du cadrage écran `SF-248-00b` — aucun écran de préférences utilisateur n'existe ; la réactivation est couverte par cette page bidirectionnelle).
- Logique backend (SF-248-01).
- Prerendering SSG de la route (route dynamique token-based, comme `verify-email` — SPA classique, pas SSG).

---

## Technique

### Composants Angular

- `UnsubscribeComponent` (nouveau, standalone) — lecture `token` via `ActivatedRoute`, signals `status` (`loading` / `unsubscribed` / `resubscribed` / `error`), méthodes `unsubscribe()` / `resubscribe()`.
- Service email frontend — `getStatus(token)`, `unsubscribe(token)`, `resubscribe(token)`.
- `app.routes.ts` — ajout de la route `{ path: 'unsubscribe', loadComponent: ... }` au niveau des routes publiques (hors bloc `canActivate: [authGuard]`).

### Endpoints consommés

`GET /api/v1/public/email/subscription-status`, `POST /api/v1/public/email/unsubscribe`, `POST /api/v1/public/email/resubscribe` — fournis par SF-248-01 (contrat figé dans `SF-248-01-backend-desabonnement.md`).

### Migration Liquibase

- [x] Non applicable.

---

## Plan de test

### Tests unitaires (Jest)

- [ ] `UnsubscribeComponent` — token valide non désinscrit → appelle `unsubscribe`, affiche la confirmation.
- [ ] `UnsubscribeComponent` — token déjà désinscrit → affiche l'état + bouton réabonnement.
- [ ] `UnsubscribeComponent` — clic « Me réabonner » → appelle `resubscribe`, met à jour le message.
- [ ] `UnsubscribeComponent` — token absent de l'URL → message d'erreur, aucun appel service.
- [ ] `UnsubscribeComponent` — `404` backend → message d'erreur dédié.
- [ ] `UnsubscribeComponent` — échec réseau → message + bouton « Réessayer ».

### Isolation workspace

- [x] Non applicable — page publique per-user, sans contexte workspace.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Navigation / routing frontend** — nouvelle route publique `/unsubscribe`. Vérifier : (a) elle est hors du bloc `authGuard` ; (b) elle ne masque pas une route existante ; (c) le wildcard `**` (NotFound) reste en dernier.
- [ ] Auth / Principal — non (page publique).
- [ ] Workspace context — non.
- [ ] Plans / limites — non.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression |
|---|---|---|
| `app.routes.ts` | ajout d'une route — risque d'ordre vs `**` | suite Jest routing existante + smoke E2E |

### Smoke tests E2E concernés

- [ ] `cd e2e && npm test` — préoccupation Navigation cochée → smoke tests obligatoires avant push (vérifier que les routes publiques existantes et le login ne régressent pas).

---

## Dépendances

### Subfeatures bloquantes

- `SF-248-01` — contrat d'API figé ; développement parallélisable, intégration finale après merge backend (cf. mémoire `feedback_pre_merge_endpoint_check`).

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- Page bidirectionnelle (désinscription + réabonnement) — ajustement A1 du cadrage écran `SF-248-00b-ux-coherence.md`.
- Pas de toggle in-app — ajustement A2 (aucun écran de préférences utilisateur n'existe ; substitution documentée, pas réduction de scope).
- Route publique hors shell, pattern `verify-email` — ajustement A3.
