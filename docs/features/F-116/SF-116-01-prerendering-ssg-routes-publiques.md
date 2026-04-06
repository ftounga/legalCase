# Mini-spec — F-116 / SF-116-01 Prerendering SSG des routes publiques

---

## Identifiant

`F-116 / SF-116-01`

## Feature parente

`F-116` — SEO — Prerendering SSG de la landing page

## Statut

`draft`

## Date de création

2026-04-06

## Branche Git

`feat/SF-116-01-prerendering-ssg`

---

## Objectif

Pré-rendre en HTML statique les routes publiques de l'application Angular (landing, pages légales, contact, login) afin que les crawlers Google indexent le contenu réel au lieu d'une coquille `<app-root>` vide.

---

## Comportement attendu

### Cas nominal

1. Au moment du `ng build`, Angular génère des fichiers HTML pré-rendus pour chaque route publique configurée
2. Les fichiers HTML contiennent le DOM complet (texte, balises meta, structured data) tel qu'il serait rendu dans un navigateur
3. Quand un crawler ou un utilisateur accède à `/`, il reçoit du HTML complet immédiatement (pas besoin d'exécuter le JS)
4. Après chargement du JS, Angular s'hydrate sur le HTML existant — l'application redevient interactive (SPA classique)
5. Les routes derrière `authGuard` (dashboard, case-files, etc.) ne sont PAS pré-rendues — elles restent en SPA classique
6. Les appels `document`, `window`, `IntersectionObserver` dans `LandingComponent` sont protégés par `isPlatformBrowser()` pour ne pas planter le prerendering côté serveur

### Routes pré-rendues

| Route | Composant |
|-------|-----------|
| `/` | LandingComponent |
| `/login` | LoginComponent |
| `/contact` | ContactComponent |
| `/mentions-legales` | LegalPageComponent |
| `/privacy` | LegalPageComponent |
| `/cgu` | LegalPageComponent |

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| API browser appelée pendant le prerendering (Node) | Guard `isPlatformBrowser` empêche l'exécution — pas de crash |
| Route protégée par authGuard demandée en prerendering | Non incluse dans la liste des routes à pré-rendre — ignorée |
| Build SSG échoue | Le build `ng build` échoue — erreur visible en CI, pas de déploiement silencieux cassé |

---

## Critères d'acceptation

- [ ] `@angular/ssr` est ajouté comme dépendance
- [ ] `angular.json` configure le prerendering avec la liste des 6 routes publiques
- [ ] `ng build` produit des fichiers HTML pré-rendus dans `dist/` pour chaque route publique
- [ ] Le HTML pré-rendu de `/` contient le texte visible de la landing (h1, description, fonctionnalités)
- [ ] Le HTML pré-rendu contient les balises meta OG et Twitter Card
- [ ] `LandingComponent` protège ses appels `document`/`window`/`IntersectionObserver` avec `isPlatformBrowser()`
- [ ] Les autres composants avec API browser (si concernés : LoginComponent, ContactComponent) sont protégés
- [ ] L'application s'hydrate correctement après chargement du JS — navigation SPA fonctionnelle
- [ ] Les routes derrière auth ne sont pas pré-rendues
- [ ] Le build CI/CD (GitHub Actions) reste fonctionnel
- [ ] Le Dockerfile frontend reste fonctionnel (serve les fichiers pré-rendus via nginx)
- [ ] Tous les tests existants restent verts

---

## Périmètre

### Hors scope (explicite)

- Server-Side Rendering (SSR) dynamique — on fait du SSG (Static Site Generation) uniquement, pas de serveur Node en prod
- Prerendering des routes authentifiées
- Changement de nom de domaine (F-116 ne touche pas aux URLs)
- Structured data JSON-LD (amélioration séparée)
- Modification du contenu de la landing page

---

## Technique

### Dépendances ajoutées

| Package | Type |
|---------|------|
| `@angular/ssr` | dependencies |

### Fichiers impactés

| Fichier | Modification |
|---------|-------------|
| `package.json` | Ajout `@angular/ssr` |
| `angular.json` | Config prerender dans architect build |
| `src/app/app.config.ts` ou `main.ts` | Ajout `provideClientHydration()` |
| `src/app/landing/landing.component.ts` | Guard `isPlatformBrowser` sur `ngAfterViewInit` |
| `src/app/app.config.server.ts` | Nouveau — config serveur pour le prerendering |
| `src/main.server.ts` | Nouveau — point d'entrée serveur |
| `tsconfig.app.json` | Ajout des fichiers serveur si nécessaire |
| `Dockerfile` (frontend) | Vérifier que nginx sert les fichiers pré-rendus correctement |
| `.github/workflows/frontend.yml` | Vérifier que le build CI produit et déploie les fichiers pré-rendus |

### Migration Liquibase

- [ ] Non applicable

### Composants Angular modifiés

- `LandingComponent` — ajout guard `isPlatformBrowser` sur les appels DOM/window dans `ngAfterViewInit`
- `app.config.ts` — ajout `provideClientHydration()`

---

## Plan de test

### Tests unitaires

- [ ] `LandingComponent` — ne crash pas quand `isPlatformBrowser` retourne false (simule environnement serveur)
- [ ] Tests existants de LandingComponent restent verts

### Tests d'intégration

- [ ] `ng build` en configuration production produit des fichiers `.html` pour les 6 routes
- [ ] Le HTML de `/index.html` (landing) contient le texte "Analysez vos dossiers"
- [ ] Le HTML de `/index.html` contient les balises `<meta property="og:title"`

### Tests de non-régression

- [ ] Tous les tests frontend existants restent verts
- [ ] Le build CI passe
- [ ] Navigation SPA fonctionne après hydratation (vérification manuelle)

### Isolation workspace

- [ ] Non applicable — aucune donnée workspace impliquée, modification purement frontend/build

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [x] **Navigation / routing frontend** — ajout de `provideClientHydration()`, modification du bootstrap Angular
- [ ] Aucune préoccupation transversale

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|-----------------|------------------------------|
| `AppComponent` | Hydratation au lieu du bootstrap classique — vérifier que le layout shell, la barre de progression et le chatbot s'initialisent correctement | Test manuel post-build |
| `authGuard` | Le guard ne doit pas être exécuté pendant le prerendering (routes auth exclues) | Vérifier que les routes auth ne sont pas dans la liste de prerendering |
| `LandingComponent` | `ngAfterViewInit` utilise `document`/`window` — doit être protégé | Test unitaire isPlatformBrowser=false |
| CI/CD `frontend.yml` | Le build doit produire et copier les fichiers pré-rendus | Vérifier le workflow après merge |
| `Dockerfile` frontend | nginx doit servir les fichiers HTML pré-rendus (try_files) | Vérifier la config nginx |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/navigation.spec.ts` — vérifier que la landing page charge correctement après prerendering
- [ ] `e2e/smoke/auth.spec.ts` — vérifier que le login fonctionne après hydratation

---

## Dépendances

### Subfeatures bloquantes

- Aucune

### Questions ouvertes impactées

- [ ] Aucune question de `docs/OPEN_QUESTIONS.md` impactée

---

## Notes et décisions

- On choisit le **SSG (Static Site Generation)** et non le SSR dynamique : pas besoin d'un serveur Node en production, nginx continue de servir les fichiers statiques
- Seules les routes publiques sont pré-rendues — les routes authentifiées sont exclues car elles n'ont pas de contenu indexable
- L'hydratation Angular 19 est mature et stable (`provideClientHydration()` est l'API recommandée)
- Le `ViewEncapsulation.None` sur LandingComponent ne pose pas de problème pour le SSG
