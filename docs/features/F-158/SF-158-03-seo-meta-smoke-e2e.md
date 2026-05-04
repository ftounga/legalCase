# SF-158-03 — SEO meta V3 complet + smoke E2E + galerie vidéos rafraîchie

## Objectif

Finaliser F-158 : compléter les meta SEO (Twitter Card, canonical), rafraîchir la copie de la galerie vidéos pour le nouveau positionnement, ajouter un smoke E2E qui garantit que la landing V3 s'affiche correctement (titre 92 outils, pricing V7, composant catalogue présent).

## Comportement nominal

### SEO meta complet
- Twitter Card : `twitter:card`, `twitter:title`, `twitter:description`, `twitter:image`.
- Canonical URL : `<link rel="canonical" href="https://legalcase.ng-itconsulting.com/">`.
- OG image (déjà présent dans index.html — vérifier).
- JSON-LD `SoftwareApplication` enrichi avec `featureList` listant les capacités majeures (92 outils, OCR, Vision, multi-domaines, multi-pays).

### Galerie vidéos
- Sous-titre actuel : "Cinq cas d'usage concrets, du droit du travail au droit de l'immigration."
- Nouveau sous-titre : « De l'upload des pièces jusqu'aux outils décisionnels pré-remplis — 5 cas concrets sur 3 domaines. »

### Smoke E2E `landing.spec.ts`
- `e2e/smoke/landing.spec.ts` (Playwright)
- Vérifie sur le déploiement staging :
  - H1 contient « 92 outils décisionnels »
  - Section pricing : prix 99 €, 219 €, 429 € présents (pas 59/119/249)
  - Composant `<app-landing-tools-showcase>` rendu (présence d'au moins 1 `.tool-card`)
  - Section OCR + Vision présente
  - Lien `/blog` dans la nav header

## Cas d'erreur

- Aucun nouveau code metier — modifs HTML/SCSS/test pur. Pas de cas d'erreur runtime.

## Critères d'acceptation

- [x] Twitter Card meta tags émis sur la home.
- [x] Canonical URL émis.
- [x] JSON-LD SoftwareApplication enrichi de `featureList`.
- [x] Galerie vidéos sous-titre rafraîchi.
- [x] Smoke E2E `landing.spec.ts` créé et exécutable.
- [x] Tests Jest existants verts (16 landing + 7 showcase).
- [x] Build prod OK.

## Plan de test minimal

- Tests Jest existants : doivent passer sans changement.
- Test E2E smoke : ne sera exécuté que contre staging déployé (CI ou manuel) — la SF garantit que le test compile et s'arme.

## Tables / endpoints / composants impactés

- `frontend/src/app/landing/landing.component.ts` (Twitter Card + JSON-LD enrichi)
- `frontend/src/index.html` (canonical link si besoin)
- `frontend/src/app/landing/landing.component.html` (vidéos sous-titre)
- `e2e/smoke/landing.spec.ts` (nouveau)

## Hors périmètre

- Sitemap.xml : déjà servi dynamiquement par le backend (`/api/sitemap.xml`), aucune modif nécessaire.
- Hreflang multi-langues (français/anglais) : hors scope V3, sera traité par F-143 ou ultérieur.
- Optimisation images OG (génération PNG dédié 1200×630) : hors scope V3.

## Analyse de cohérence transversale

- **Préoccupations transversales** : aucune.
- **Nouveau pattern UI** : aucun.
- **Impact par domaine métier** : transversal — meta SEO inclusif des 3 domaines.

## Parité des domaines métier

- Pas applicable.

## Contrat API

- Pas applicable.
