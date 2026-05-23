# Mini-spec — F-255 / SF-255-02 Frontend admin `/super-admin/promo-codes`

## Identifiant
`F-255 / SF-02`

## Feature parente
`F-255` — Codes promo / coupons partenaires

## Statut
`draft`

## Date de création
2026-05-23

## Branche Git
`feat/SF-255-02-frontend-admin-promo-codes`

---

## Objectif

Livrer l'écran Angular `/super-admin/promo-codes` qui permet à un super-admin de créer, lister et désactiver des codes promo en consommant les endpoints SUPER_ADMIN figés par SF-255-01.

---

## Comportement attendu

### Cas nominal
1. Super-admin clique sur le sous-module « Codes promo » depuis `/super-admin` (lien à ajouter à la page d'accueil).
2. Arrive sur `/super-admin/promo-codes`.
3. Voit 2 blocs : **formulaire de création** en haut, **table des codes existants** en bas.
4. Remplit le formulaire (code, type, valueDays, partnerLabel, maxUses, expiresAt) → submit.
5. Création réussie → toast succès + le code apparaît en tête de la table (refresh auto).
6. Pour désactiver un code : clic sur le bouton « Désactiver » en bout de ligne → confirmation Mat-Dialog → POST endpoint → flip `active=false` + refresh + toast.

### Cas d'erreur
- 400 / 409 backend → toast d'erreur avec le `message` du body (champ `error.error.message`)
- 403 non super-admin → redirection `/dashboard` (guard ou intercepteur existants)
- Réseau down → toast d'erreur générique

---

## Critères d'acceptation
- [ ] **C1** — Route `/super-admin/promo-codes` accessible (composant standalone Angular)
- [ ] **C2** — Page d'accueil `/super-admin` liste « Codes promo » avec lien fonctionnel
- [ ] **C3** — Formulaire de création : 6 champs (code, type, valueDays conditionnel, partnerLabel, maxUses, expiresAt), validation client cohérente avec les contraintes backend
- [ ] **C4** — Le champ `valueDays` est visible/required uniquement si `type=TRIAL_EXTENSION`
- [ ] **C5** — Submit → POST `/api/v1/super-admin/promo-codes` → toast succès + refresh table + reset formulaire
- [ ] **C6** — Erreur backend (409 duplicate, etc.) → toast d'erreur avec message backend
- [ ] **C7** — Table affiche tous les codes triés par `createdAt DESC` avec colonnes : code, type, partnerLabel, usesCount/maxUses, expiresAt, active, créé le, actions
- [ ] **C8** — Bouton « Désactiver » par ligne (visible si `active=true`), confirmation MatDialog avant POST
- [ ] **C9** — Désactivation → toast + refresh + `active` passe à false dans la ligne (sans rechargement complet de la page)
- [ ] **C10** — Tests Jest : > 90% couverture sur le composant + le service Angular

---

## Périmètre / Hors-scope

- **Hors scope V1** : édition d'un code existant (édition = supprimer + recréer), filtres/recherche, export CSV, pagination (volume attendu < 100 codes), analytics par partenaire (SF-255-05 V2).
- **Pas d'animation** ni de gamification (cf. invariant 7 étape 0 bis).

---

## Technique

### Composant Angular
- **Nouveau composant standalone** : `frontend/src/app/super-admin/promo-codes/super-admin-promo-codes.component.ts` (+ `.html`, `.scss`, `.spec.ts`)
- Pattern de référence : `frontend/src/app/super-admin/traction-onepager/traction-onepager.component.ts`
- Signal-based state : `codes = signal<PromoCode[]>([])`, `loading = signal(false)`, `submitting = signal(false)`

### Service Angular
- **Nouveau service** : `frontend/src/app/super-admin/promo-codes/promo-code-admin.service.ts`
- Méthodes : `createCode(req)`, `listCodes()`, `deactivateCode(id)` — chacune retourne `Observable<PromoCodeDto>` ou `Observable<PromoCodeDto[]>`

### Modèles TypeScript
- `PromoCodeDto` (record TS reflétant le DTO backend)
- `PromoCodeCreateRequest` (record TS)
- Enums : `PromoCodeType = 'TRIAL_EXTENSION' | 'STRIPE_DISCOUNT'`

### Route
- Ajout dans `frontend/src/app/app.routes.ts` :
  ```ts
  { path: 'super-admin/promo-codes', component: SuperAdminPromoCodesComponent, canActivate: [authGuard] }
  ```
- Ajout d'un lien dans la page `/super-admin` (sous-module « Codes promo » dans la liste existante)

### Endpoints consommés (figés par SF-255-01)
- `POST /api/v1/super-admin/promo-codes`
- `GET /api/v1/super-admin/promo-codes`
- `POST /api/v1/super-admin/promo-codes/{id}/deactivate`

### Composants Angular Material utilisés
- `mat-form-field` (outline appearance) pour les inputs
- `mat-select` pour le type
- `mat-table` + `mat-sort` pour le listing
- `mat-button` / `mat-flat-button` / `mat-stroked-button`
- `MatSnackBar` pour les toasts (pattern existant)
- `MatDialog` pour la confirmation de désactivation
- `mat-icon` pour les actions inline

### Design System (DESIGN_SYSTEM.md)
- Palette : navy (`--marine` / `#1A3A5C`) pour les en-têtes, or (`#C9973A`) pour les CTA primaires, rouge pour le bouton « Désactiver » (alerte sobre)
- Police : Inter pour le contenu, JetBrains Mono pour les codes (`<span class="font-mono">ACE2026</span>`)
- Espacement multiples de 4px
- Pas de `window.alert()` / `window.confirm()`

---

## Plan de test

### Tests Jest
- [ ] Création nominale (mock service, vérifier appel + toast + reset form)
- [ ] Création avec erreur 409 (toast d'erreur affichage)
- [ ] Listing (mock service, vérifier rendu de la table)
- [ ] Désactivation avec confirmation (mock MatDialog `confirm=true`)
- [ ] Désactivation avec annulation (mock MatDialog `confirm=false` → aucun appel)
- [ ] Conditional rendering : `valueDays` visible si type TRIAL_EXTENSION

### Smoke tests E2E
- Aucun smoke E2E direct V1 (super-admin n'est pas dans les flows utilisateur standard couverts par `e2e/smoke/`).

---

## Analyse d'impact

### Préoccupations transversales
- **Navigation / routing frontend** ✅ — nouvelle route `/super-admin/promo-codes` ajoutée, vérif que le `authGuard` (ou équivalent existant pour `/super-admin/*`) protège bien la route.
- Plans/limites, Auth, Workspace : non touchés directement (l'écran est admin pur).

### Composants impactés
- `app.routes.ts` : ajout d'une entrée
- Page `/super-admin` (composant accueil) : ajout d'un lien sous-module

---

## Dépendances

### Bloquantes
- ✅ SF-255-01 mergée (PR #1253) — contrat API figé.

### Démarrables en parallèle
- SF-255-03 (frontend user) — composant différent, branche isolée.

---

## Notes
- Le composant est standalone (pas de module Angular).
- Pas de NgRx ni de store : signal-based state local au composant.
- Le service utilise `HttpClient` standard + `inject()` (pattern Angular 19).
