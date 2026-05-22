# Mini-spec — F-254 / SF-254-01 Galerie de démos vidéo `/demos`

## Identifiant

`F-254 / SF-254-01`

## Feature parente

`F-254` — Galerie de démos vidéo dédiée (`/demos`)

## Statut

`ready`

## Date de création

2026-05-23

## Branche Git

`feat/SF-254-01-galerie-demos`

---

## Objectif

Livrer une page publique Angular `/demos` qui liste toutes les démos vidéo dans une grille de cards cliquables, avec player en lecture inline, CTAs de conversion (Essai gratuit + Calendly) et SEO indexable — accessible depuis la landing via un CTA sous le carousel et un lien footer.

---

## Comportement attendu

### Cas nominal

1. Prospect arrive sur `https://legalcase.fr/demos` (URL directe, depuis carousel landing CTA, ou depuis footer).
2. La page rend immédiatement (SSG) avec hero, grille de cards et CTAs de sortie. Pas de spinner, pas de fetch.
3. Chaque card affiche : thumbnail YouTube (`maxresdefault.jpg`), titre, sous-titre, icône play au survol.
4. Clic sur une card → la card sélectionnée passe en player inline (toggle in-place, comme la landing `selectVideo(id)`). L'iframe YouTube charge l'URL `videoEmbedUrl(id)`. Toutes les autres cards restent en thumbnail.
5. Re-clic ailleurs ou bouton « Fermer » → retour à thumbnail.
6. CTAs en bas de page :
   - `Essai gratuit 14 jours` (primary navy) → `/onboarding`
   - `Prendre rendez-vous` (secondary outline) → URL Calendly (constante d'env partagée avec la landing)
7. Header de la page : logo LegalCase (lien vers `/`) + fil d'Ariane minimal « Démonstrations » + bouton retour landing (`/#demos`).
8. La page est indexée par Google (sitemap + meta + JSON-LD).
9. Sur la landing : sous `.video-carousel` apparaît un CTA `Voir toutes les démos →` (routerLink `/demos`).
10. Dans le footer landing : ajout d'un lien `Démonstrations` vers `/demos`.

### Cas d'erreur

| Situation | Comportement attendu | Code |
|-----------|---------------------|------|
| `videoId` mal formé (impossible côté code, données statiques) | Card affichée sans player ouvert ; iframe ne se charge pas, fallback CSS sur thumbnail | n/a |
| Connexion réseau hors-ligne au moment du clic sur une card | YouTube iframe affiche son message d'erreur natif ; pas de gestion custom | n/a |
| JavaScript désactivé | Les cards restent navigables (HTML statique) ; le clic ouvre le lien direct YouTube via `<a href="https://www.youtube.com/watch?v={id}">` en fallback `<noscript>` | n/a |
| Route inconnue après `/demos/...` | Wildcard route renvoie sur `NotFoundComponent` (F-83 livré) | 404 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : non applicable — feature marketing
- [x] **Autres pays** : la galerie est universelle (FR + BE consultent la même page), pas de bascule pays
- [x] **Autres domaines** : la galerie liste les démos de tous les domaines sans filtre en V1 (filtres = SF-254-02 différée)
- [x] **Autres UI patterns** : le player inline est dupliqué depuis le carousel landing (`selectVideo`, `videoEmbedUrl`, `videoThumbnailUrl`) — refactor en service partagé requis
- [x] **Autres flows transversaux** : nouvelle route publique (préoccupation transversale Navigation/routing — voir section ad hoc)

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `landing.component.ts` `DEMO_VIDEOS` | Oui | **Promu en source partagée** : extrait dans `frontend/src/app/shared/demos/demo-videos.data.ts` (export const + interface `DemoVideo`). Consommé par les 2 composants (landing carousel et nouvelle galerie). Single source of truth — toute nouvelle vidéo s'ajoute à un endroit unique. |
| `landing.component.ts` méthodes `videoEmbedUrl(id)` / `videoThumbnailUrl(id)` | Oui | **Extraites** dans le même fichier `demo-videos.data.ts` comme fonctions pures (`getDemoEmbedUrl(id, sanitizer)` + `getDemoThumbnailUrl(id)`). |
| Player inline pattern (`selectedVideoId` signal + toggle in-place) | Oui | **Dupliqué** délibérément dans la nouvelle galerie — pattern simple, pas besoin de composant partagé pour 2 occurrences (refactor envisageable si > 3 lieux). |
| Footer landing | Oui | Ajout d'un lien `Démonstrations` vers `/demos` dans la section liens du footer. |
| Section `.video-showcase` landing | Oui | Ajout d'un CTA `Voir toutes les démos →` sous le carousel (zone après les arrows + thumbnails). |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (promotion `DEMO_VIDEOS` en source partagée + ajustements landing carousel/footer + nouvelle page).
- [ ] Subfeature(s) parallèle(s) — non applicable
- [ ] Backlog — SF-254-02 (filtres domaine/tag) reste différée
- [ ] Non applicable aux autres cibles — n/a

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF marketing/landing, pas un outil décisionnel. Aucune entrée `TOOL_REGISTRY`, aucun pré-fill IA, aucune validation F-IA-03, aucun refresh dashboard.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — justification : SF marketing/landing, aucun formulaire saisissable, aucun champ IA à extraire.

---

## Critères d'acceptation

- [x] La route `/demos` est publique (no `authGuard`), accessible sans login.
- [x] La page rend en mode SSG (Angular 19 pre-render) — le HTML statique sert sans appel API.
- [x] La grille affiche les 9 vidéos actuelles (lecture depuis `demo-videos.data.ts`).
- [x] Clic sur une card ouvre le player inline ; clic ailleurs ou sur la croix le referme.
- [x] Un seul player YouTube actif à la fois sur la page (auto-pause des autres).
- [x] Le footer landing contient un lien `Démonstrations` → `/demos`.
- [x] Sous `.video-carousel` apparaît un CTA `Voir toutes les démos →` (routerLink `/demos`).
- [x] La page contient les meta `title`, `description`, `og:title`, `og:description`, `og:image`, `og:url`, `twitter:card`, `<link rel="canonical">`, et un bloc JSON-LD `WebPage` (pattern SF-158-03).
- [x] La page est ajoutée au sitemap `public/sitemap.xml`.
- [x] Le contraste, focus visible, labels ARIA et navigation clavier sont conformes WCAG AA (pattern landing F-158).
- [x] La grille est responsive : 3 cards/ligne ≥ 1024px, 2 cards/ligne 640-1023px, 1 card/ligne < 640px.
- [x] Aucune marque tierce visible dans les titres/sous-titres des cards ([[feedback_no_thirdparty_brands_landing]]).
- [x] Aucune mention « IA » dans les titres marketing (sous-titres techniques OK).
- [x] Tests Jest : ≥ 12 tests sur le nouveau composant (rendu, sélection vidéo, toggle player, navigation clavier, état initial).
- [x] Build production sans erreur, taille bundle < +20 KB (la nouvelle page partage `demo-videos.data.ts`).
- [x] Smoke E2E (Playwright) : `e2e/smoke/landing.spec.ts` reste vert + nouveau smoke `e2e/smoke/demos.spec.ts` (chargement page, ≥ 1 card visible, CTA Essai gratuit fonctionne).

---

## Périmètre

### Hors scope (explicite)

- **SF-254-02 — filtres domaine/tag** : différée jusqu'à ~15 vidéos (à 9, filtre = 3 par domaine = peu utile).
- **SF-254-03 — page détail `/demos/:slug`** : V2, conditionnée à signal terrain (partage par démo demandé) ou volume > 20 vidéos.
- **Admin de gestion vidéos** : édition reste un commit du fichier `demo-videos.data.ts` (suffisant V1, pas de CRUD).
- **Analytics par vidéo** : GA4 standard suffit en V1 (pas de tracking custom par démo).
- **Sous-titres / transcripts téléchargeables** : V2 si demande.
- **Playlists / chapitrage** : V2.
- **Player non-YouTube** (Vimeo / hébergement self) : pas de raison fonctionnelle en V1.

---

## Valeurs initiales

Non applicable — aucune entité créée, aucune persistance.

---

## Contraintes de validation

Non applicable — aucun input utilisateur (page publique en lecture).

---

## Technique

### Endpoint(s)

Non applicable — aucun endpoint backend.

### Tables impactées

Non applicable — aucune table.

### Migration Liquibase

- [ ] Oui
- [x] Non applicable

### Composants Angular

| Composant | Chemin | Rôle |
|---|---|---|
| `DemosPageComponent` (nouveau) | `frontend/src/app/demos/demos-page.component.ts` | Page standalone consommant `DEMO_VIDEOS`, gère `selectedVideoId` signal, player inline, CTAs de sortie |
| `demo-videos.data.ts` (nouveau) | `frontend/src/app/shared/demos/demo-videos.data.ts` | Source partagée : export `DEMO_VIDEOS`, interface `DemoVideo`, helpers `getDemoEmbedUrl(id, sanitizer)` + `getDemoThumbnailUrl(id)` |
| `LandingComponent` (modifié) | `frontend/src/app/landing/landing.component.ts` | Importe `DEMO_VIDEOS` depuis la source partagée (suppression du `const` local). Ajoute le CTA `Voir toutes les démos →` sous `.video-carousel`. Ajoute le lien `Démonstrations` au footer. |
| `app.routes.ts` (modifié) | `frontend/src/app/app.routes.ts` | Ajout de la route `{ path: 'demos', loadComponent: () => import('./demos/demos-page.component').then(m => m.DemosPageComponent) }` |
| Pre-render config | `angular.json` ou `prerender.config.ts` | Ajout de `/demos` aux routes pré-rendues (SSG) |

### Routes Angular

| Route | Composant | Auth | Pre-render |
|---|---|---|---|
| `/demos` | `DemosPageComponent` | Aucune | Oui (SSG) |

### Sitemap

| Fichier | Modification |
|---|---|
| `frontend/public/sitemap.xml` ou équivalent | Ajout d'une entrée `<url><loc>https://legalcase.fr/demos</loc>...</url>` |

---

## Plan de test

### Tests Jest (composant)

- [x] `DemosPageComponent` — rendu initial : N cards affichées, aucune en mode player, hero présent, CTAs présents.
- [x] `DemosPageComponent` — clic sur card N → `selectedVideoId() === videoN.videoId`, iframe rendue uniquement sur cette card.
- [x] `DemosPageComponent` — clic sur card M après N → `selectedVideoId() === videoM.videoId`, l'ancienne iframe N a disparu.
- [x] `DemosPageComponent` — clic sur croix fermer → `selectedVideoId() === null`, retour grille.
- [x] `DemosPageComponent` — navigation clavier : `Tab` parcourt les cards, `Enter` sélectionne, `Esc` ferme le player.
- [x] `DemosPageComponent` — `videoEmbedUrl` retourne une `SafeResourceUrl` valide pour un id donné.
- [x] `DemosPageComponent` — labels ARIA présents (`aria-label="Lire : <titre>"`, `aria-pressed` sur la card active).
- [x] `DemosPageComponent` — CTA `Essai gratuit` a `routerLink="/onboarding"`.
- [x] `DemosPageComponent` — CTA `Prendre rendez-vous` ouvre l'URL Calendly dans un nouvel onglet (`target="_blank" rel="noopener"`).
- [x] `demo-videos.data.ts` — `getDemoThumbnailUrl(id)` renvoie `https://img.youtube.com/vi/{id}/maxresdefault.jpg`.
- [x] `demo-videos.data.ts` — `getDemoEmbedUrl(id, sanitizer)` renvoie une URL `youtube-nocookie.com/embed/{id}` sanitizée.
- [x] `LandingComponent` (régression) — la suppression du `const DEMO_VIDEOS` local n'a pas cassé le carousel (tests existants restent verts).

### Tests d'intégration

Non applicable — aucun endpoint backend.

### Smoke E2E (Playwright)

- [x] `e2e/smoke/demos.spec.ts` (nouveau) :
  - Charge `https://staging.legalcase.fr/demos` → status 200, H1 visible
  - Au moins 9 cards visibles
  - Clic sur la première card → iframe YouTube apparaît dans la card
  - CTA `Essai gratuit` est visible et a `href` ou `routerLink="/onboarding"`
- [x] `e2e/smoke/landing.spec.ts` (existant) reste vert après modification (CTA + footer link ajoutés).

### Isolation workspace

- [x] **Non applicable** — page publique, aucune donnée workspace impliquée.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — non concerné (no-auth)
- [ ] Workspace context — non concerné
- [ ] Plans / limites — non concerné
- [x] **Navigation / routing frontend** — ajout d'une nouvelle route publique `/demos`, modifications mineures `LandingComponent` + `app.routes.ts`
- [ ] Aucune préoccupation transversale

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression |
|---|---|---|
| `LandingComponent` | Suppression du `const DEMO_VIDEOS` local (déplacé en source partagée). Ajout d'un CTA sous `.video-carousel` + lien footer. | Tests Jest existants `landing.component.spec.ts` doivent rester verts + smoke `e2e/smoke/landing.spec.ts` |
| `app.routes.ts` | Nouvelle route lazy-loaded `/demos`. Pas d'impact sur les routes existantes. | Smoke E2E sur les routes existantes (login, dashboard) — pattern F-83 |
| Sitemap | Ajout d'une URL — pas de suppression. | Vérification du sitemap servi correctement par CloudFront (200 + format XML) |

### Smoke tests E2E concernés

- [x] `e2e/smoke/landing.spec.ts` — doit rester vert (suite à l'ajout du CTA et du lien footer)
- [x] `e2e/smoke/demos.spec.ts` (nouveau) — doit passer
- [x] Aucun autre smoke test concerné

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` impactée.

---

## Notes et décisions

- **Pas de filtres en V1** : à 9 vidéos, des filtres par domaine donneraient des groupes de 3 vidéos — friction supérieure au gain. Décision PO 2026-05-23 (cf. SF-254-00b-ux-coherence.md).
- **Player inline plutôt que modale** : cohérent avec le pattern landing (`selectVideo` toggle in-place) + plus simple à implémenter et à pré-rendre SSG. La modale est une alternative à explorer en V2 si l'UX feedback le demande.
- **URL Calendly** : utiliser la même constante / variable d'environnement que la landing (pas de duplication d'URL). Si pas de constante existante, créer `frontend/src/environments/environment.ts` `marketingCtas: { calendlyUrl }`.
- **SSG-safe** : la page est statique, pas d'@if conditionnel sur des handlers — pas de risque d'event-replay ([[project_angular_ssg_event_replay]]).
- **Build-time bundle target** : viser < +20 KB après gzip pour la nouvelle page (composant léger + données statiques partagées).
- **JSON-LD** : utiliser type `WebPage` avec `breadcrumb` (LegalCase → Démonstrations) et `mainContentOfPage` listant les démos (pattern symétrique SF-158-03).
- **Pas de référentiel `parcours-ecran-prospect-acquisition.md` à créer dans cette SF** — différé à une étape 6 documentaire post-merge, pour ne pas surcharger la PR (le doc SF-254-00b suffit comme référence pour la mini-spec).

---
