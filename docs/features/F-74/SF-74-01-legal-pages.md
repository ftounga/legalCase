# SF-74-01 — Pages légales Angular (mentions légales, CGU, politique de confidentialité)

## Objectif
Rendre accessibles publiquement les 3 pages légales via des routes Angular dédiées, avec liens dans le footer de la landing page.

## Comportement nominal
- `/mentions-legales` → Mentions légales (sans auth)
- `/privacy` → Politique de confidentialité (sans auth)
- `/cgu` → CGU (sans auth)
- Footer landing page : 3 liens vers ces pages

## Cas d'erreur
Aucun — contenu statique.

## Critères d'acceptation
1. Les 3 routes accessibles sans authentification
2. Footer landing contient les 3 liens
3. Contenu conforme aux documents rédigés
4. Design system respecté

## Plan de test
- T1 : route /mentions-legales — rendu sans auth
- T2 : route /privacy — rendu sans auth
- T3 : route /cgu — rendu sans auth
- T4 : footer landing — 3 liens présents

## Composants impactés
- Nouveau : LegalPageComponent (standalone)
- Modifié : app.routes.ts (3 nouvelles routes publiques)
- Modifié : LandingComponent (footer)

## Hors périmètre
Rendu markdown dynamique, i18n, cookie banner
