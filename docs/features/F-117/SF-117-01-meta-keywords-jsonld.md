# Mini-spec — F-117 / SF-117-01 Mots-clés métier dans meta tags + JSON-LD

---

## Identifiant

`F-117 / SF-117-01`

## Feature parente

`F-117` — SEO — Mots-clés métier dans meta tags + structured data JSON-LD

## Statut

`draft`

## Date de création

2026-04-06

## Branche Git

`feat/SF-117-01-meta-keywords-jsonld`

---

## Objectif

Enrichir les meta tags (title, description) avec les mots-clés que la cible recherche (avocat, droit du travail, contentieux, analyse IA) et ajouter un bloc JSON-LD schema.org SoftwareApplication pour apparaître dans les rich snippets Google.

---

## Comportement attendu

### Cas nominal

1. Le `<title>` de `index.html` est enrichi avec des mots-clés métier
2. La `<meta name="description">` inclut les termes "avocat", "droit du travail", "contentieux", "cabinet"
3. Le `LandingComponent` met à jour dynamiquement le title et la description (déjà en place — enrichir le contenu)
4. Un bloc `<script type="application/ld+json">` est injecté dans la landing avec un schema SoftwareApplication contenant : nom, description, catégorie, offre (essai gratuit), URL, éditeur
5. Le JSON-LD est présent dans le HTML pré-rendu (vérifié via build SSG)

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| JSON-LD mal formé | Google l'ignore silencieusement — pas d'impact sur le site |
| Title trop long (> 60 chars) | Google tronque dans les SERP — rester sous 60 chars |

---

## Critères d'acceptation

- [ ] `<title>` dans `index.html` contient "avocat" et "droit du travail"
- [ ] `<meta description>` contient "avocat", "droit du travail", "contentieux", "cabinet"
- [ ] `LandingComponent.ngOnInit()` met à jour title et description avec les mêmes mots-clés enrichis
- [ ] Les meta OG (og:title, og:description) sont cohérents avec le nouveau title/description
- [ ] Un bloc JSON-LD `SoftwareApplication` est présent dans le HTML de la landing
- [ ] Le JSON-LD contient : name, description, applicationCategory, offers (Free Trial), url, publisher
- [ ] Le JSON-LD est valide (structure conforme à schema.org)
- [ ] Le HTML pré-rendu de `/` contient le JSON-LD
- [ ] Build prod OK
- [ ] Tous les tests existants restent verts

---

## Périmètre

### Hors scope (explicite)

- Changement de nom de domaine (les URLs restent sur legalcase.ng-itconsulting.com)
- Création d'une image OG dédiée (point séparé, hors code)
- SEO des pages légales (title/description déjà gérés par LegalPageComponent)
- Meta keywords tag (ignoré par Google depuis 2009)

---

## Technique

### Fichiers impactés

| Fichier | Modification |
|---------|-------------|
| `src/index.html` | Enrichir title et meta description |
| `src/app/landing/landing.component.ts` | Enrichir title/description dynamiques, ajouter JSON-LD |
| `src/app/landing/landing.component.html` | Injecter le bloc JSON-LD (ou via component) |

### Migration Liquibase

- [ ] Non applicable

---

## Plan de test

### Tests unitaires

- [ ] `LandingComponent` — title mis à jour avec mots-clés métier
- [ ] Tests existants restent verts

### Tests d'intégration

- [ ] Build SSG — HTML pré-rendu de `/` contient le JSON-LD
- [ ] Build SSG — HTML pré-rendu de `/` contient les mots-clés dans le title

### Isolation workspace

- [ ] Non applicable

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — modification purement SEO/contenu

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné (justification : modification de contenu meta/JSON-LD uniquement, aucun changement de routing ou comportement)

---

## Dépendances

### Subfeatures bloquantes

- Aucune

### Questions ouvertes impactées

- [ ] Aucune

---

## Notes et décisions

- Le JSON-LD est injecté directement dans le template HTML de la landing (pas via un service) pour être présent dans le HTML pré-rendu SSG
- On utilise le type `SoftwareApplication` de schema.org, le plus adapté pour un SaaS
- Le title reste sous 60 caractères pour éviter la troncation Google
