# SF-158-01 — Refonte messaging landing V3 + repricing V7

## Objectif

Repositionner la landing : passer du discours "**analyseur de documents juridiques**" au discours "**plateforme de 92 outils décisionnels métier × 3 domaines × 2 pays**" où l'IA pré-remplit chaque outil depuis les pièces uploadées. Mettre à jour le pricing landing (V1 obsolète 59/119/249) en V7 conforme F-123 (SOLO 99 / TEAM 219 / PRO 429).

## Comportement nominal

- Hero refondu : tagline + sub-tagline + stats repositionnés sur "plateforme outils décisionnels" plutôt que "analyseur de documents".
- Section "Problème" reframée : l'avocat n'a pas que des documents à analyser, il a aussi des dizaines de calculs / scorings / comparateurs / générateurs / détecteurs à faire à la main (indemnités, ancienneté, conventions collectives, recevabilité, recours, scoring divorce, partage, etc.).
- Section "Solution" reframée : LegalCase est une **plateforme** qui combine (1) extraction OCR + Vision, (2) analyse IA structurée, (3) **92 outils décisionnels** pré-remplis automatiquement.
- Pricing remis en V7 : 4 cards (Essai gratuit / SOLO 99 / TEAM 219 / PRO 429) — actuellement encore V1 (Free / Solo 59 / Team 119 / Pro 249).
- SEO meta description, OG tags et JSON-LD mis à jour pour le nouveau positionnement.
- Hero stats : remplacer "10 outils décisionnels intégrés" par "92 outils décisionnels", garder les autres compteurs.

## Cas d'erreur

- Aucune erreur de runtime — c'est une modif HTML/CSS pur côté frontend.
- Risque produit : si le compteur 92 devient obsolète (ajouts/retraits TOOL_REGISTRY), il faudra le mettre à jour manuellement. Acceptable : un refresh trimestriel suffit pour la landing.

## Critères d'acceptation

- [x] Le hero affiche le nouveau messaging (« 92 outils décisionnels juridiques » dans le titre ou la sous-phrase).
- [x] Les 4 stats du hero sont cohérents avec le nouveau positionnement (au moins l'une mentionne "92 outils").
- [x] Pricing : 4 cards correspondant à V7 (Essai gratuit 0 € / SOLO 99 € / TEAM 219 € / PRO 429 €) — quotas et features alignés avec ceux de `workspace-billing`.
- [x] Section "Problème" : 3 items reformulés sur le terrain "outils manuels" plutôt que "lecture manuelle uniquement".
- [x] Section "Solution" : 5 items reformulés (extraction → analyse → outils pré-remplis → divergence IA détectée → cohérence multi-source).
- [x] SEO meta description : nouvelle copie qui mentionne la plateforme outils décisionnels.
- [x] JSON-LD `description` mis à jour.
- [x] `npm run build` passe sans erreur.
- [x] Tests Jest existants `landing.component.spec.ts` continuent à passer (ou sont mis à jour si les selectors testés ont changé).

## Plan de test minimal

- Unitaires Jest : adapter les expectations de `landing.component.spec.ts` si elles testent le messaging (titre, stats).
- Visuel : `npm start` puis ouvrir http://localhost:4200 et vérifier que le hero, le pricing et les 2 sections refondues affichent le nouveau contenu.
- Accessibilité : la nav header et le footer continuent à pointer vers /blog (préservé du fix précédent).

## Tables / endpoints / composants impactés

- `frontend/src/app/landing/landing.component.html` (hero, problème, solution, pricing)
- `frontend/src/app/landing/landing.component.ts` (meta tags + JSON-LD)
- `frontend/src/app/landing/landing.component.spec.ts` (adaptations si selectors)
- Aucun backend, aucune migration, aucun nouveau composant.

## Hors périmètre

- Composant grille 92 outils filtrable → SF-158-02
- Showcase OCR + Vision animations → SF-158-02
- Refonte SCSS profonde (palette, layout) → garder l'existant SF-118 qui est déjà aligné DESIGN_SYSTEM.md
- Refonte FAQ
- Refonte mentions légales / CGU / privacy (déjà solides)
- Mise à jour sitemap.xml → SF-158-03
- E2E smoke tests → SF-158-03

## Analyse de cohérence transversale

- **Préoccupations transversales** : aucune (auth, workspace, plans, navigation, outil décisionnel — toutes intactes).
- **Nouveau pattern UI ou service partagé** : aucun nouveau composant — modifs HTML/SCSS du landing seul.
- **Impact par domaine métier** : transversal — landing publique, mention équilibrée des 3 domaines.

## Parité des domaines métier

- Pas applicable : SF marketing/landing, pas un outil décisionnel.

## Contrat API

- Pas applicable : SF frontend pure, aucun appel API.
