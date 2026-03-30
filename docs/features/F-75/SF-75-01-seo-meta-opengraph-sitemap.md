# Mini-spec — F-75 / SF-75-01 SEO — Meta tags, Open Graph, sitemap, robots.txt

## Identifiant
`F-75 / SF-75-01`

## Feature parente
`F-75` — SEO de base : indexation Google et partage réseaux sociaux

## Statut
`ready`

## Date de création
2026-03-30

## Branche Git
`feat/SF-75-01-seo-meta-opengraph-sitemap`

---

## Objectif

Rendre la landing page indexable par Google et partageable proprement sur LinkedIn/Twitter en ajoutant les balises meta, Open Graph, un sitemap.xml et un robots.txt.

---

## Comportement attendu

### Cas nominal

1. Google crawle `legalcase.ng-itconsulting.com` → trouve `robots.txt` (Allow: /) et `sitemap.xml`
2. `sitemap.xml` référence les URLs publiques : `/`, `/mentions-legales`, `/privacy`, `/cgu`
3. Un partage de `legalcase.ng-itconsulting.com` sur LinkedIn/Twitter affiche :
   - Titre : "AI LegalCase — L'IA au service de vos dossiers juridiques"
   - Description : "Analysez automatiquement vos dossiers juridiques en quelques minutes. Faits clés, risques, timeline, points de droit. Essai gratuit 14 jours."
   - Image : logo ou bannière de l'application
4. La page `/` a un `<title>` et une `<meta name="description">` uniques
5. Les pages légales (`/mentions-legales`, `/privacy`, `/cgu`) ont des titres distincts

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Route authentifiée (ex: /case-files) | Pas de meta SEO — ces pages ne sont pas indexables |
| Image OG manquante | Fallback sur le logo par défaut |

---

## Critères d'acceptation

- [ ] `robots.txt` servi à `/robots.txt` — `Allow: /`, `Sitemap:` pointant vers le sitemap
- [ ] `sitemap.xml` servi à `/sitemap.xml` — contient `/`, `/mentions-legales`, `/privacy`, `/cgu`
- [ ] `<meta name="description">` présente sur `/`
- [ ] Balises Open Graph présentes sur `/` : `og:title`, `og:description`, `og:image`, `og:url`, `og:type`
- [ ] Balises Twitter Card présentes sur `/` : `twitter:card`, `twitter:title`, `twitter:description`
- [ ] `<title>` dynamique sur les pages légales via `Title` service Angular
- [ ] Pages derrière `authGuard` non indexées (`noindex` ou hors sitemap)
- [ ] `legalcase-logo.png` ou bannière accessible à une URL publique pour og:image

---

## Périmètre

### Hors scope
- Google Search Console (configuration manuelle par l'utilisateur)
- Google Analytics / Plausible (M-07, feature séparée)
- SEO dynamique côté serveur (SSR Angular) — non justifié en V1
- Balises meta sur les pages authentifiées

---

## Technique

### Fichiers impactés

| Fichier | Opération | Notes |
|---------|-----------|-------|
| `frontend/src/index.html` | Modifier | Ajouter meta description + Open Graph globaux |
| `frontend/src/robots.txt` | Créer | Fichier statique |
| `frontend/src/sitemap.xml` | Créer | Fichier statique |
| `frontend/angular.json` | Modifier | Ajouter robots.txt et sitemap.xml dans `assets` |
| `frontend/src/app/landing/landing.component.ts` | Modifier | Injecter `Title` + `Meta` Angular pour override dynamique |
| `frontend/src/app/legal/legal-page.component.ts` | Modifier | Titre dynamique par page légale |

### Pas de backend, pas de migration, pas d'endpoint

### Composants Angular impactés
- `LandingComponent` — injecte `Title` et `Meta` au init
- `LegalPageComponent` — injecte `Title` au init (titre déjà dans `data` de la route)

---

## Plan de test

### Tests unitaires
- [ ] `LandingComponent` — `Title.setTitle()` appelé avec le bon titre
- [ ] `LandingComponent` — `Meta.updateTag()` appelé pour `og:title`, `og:description`
- [ ] `LegalPageComponent` — titre dynamique défini depuis `ActivatedRoute.data`

### Tests d'intégration
- [ ] Non applicable — frontend statique

### Isolation workspace
- [ ] Non applicable — pages publiques uniquement

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] **Navigation / routing frontend** — LegalPageComponent lit `ActivatedRoute.data` pour le titre

### Composants existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression |
|-----------|-----------------|----------------------|
| `LegalPageComponent` | Ajout injection `Title` — aucun impact fonctionnel | Test unitaire titre |
| `LandingComponent` | Ajout injection `Title` + `Meta` — aucun impact visuel | Test unitaire meta |

### Smoke tests E2E concernés
- [ ] Aucun smoke test concerné — pas de changement de routing, guard, ou auth

---

## Dépendances
- Aucune subfeature bloquante

## Notes et décisions
- Approche 100% statique (pas de SSR) : les balises meta sont dans `index.html` pour le cas général et overridées par Angular `Meta` service pour la landing. Google indexe suffisamment bien les SPA Angular en 2026.
- `og:image` pointe vers `legalcase-logo.png` déjà présent dans `frontend/src/`
- URL de production : `https://legalcase.ng-itconsulting.com`
