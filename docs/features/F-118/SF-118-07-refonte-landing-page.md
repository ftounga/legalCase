# Mini-spec — F-118 / SF-118-07 Refonte complète landing page

## Identifiant

`F-118 / SF-118-07`

## Feature parente

`F-118` — Refonte visuelle / polish UX

## Branche Git

`feat/SF-118-07-refonte-landing-page`

## Date de création

2026-04-17

## Statut

`draft`

---

## Objectif

Refonte complète de la landing page pour maximiser la conversion et refléter l'étendue actuelle du produit (3 domaines juridiques × FR+BE, 10 outils décisionnels, pipeline IA 3 niveaux, traçabilité sources, contrôle de cohérence). Le design actuel est fonctionnel mais dense, avec 9 feature cards identiques et peu de hiérarchie visuelle.

---

## Sections de la nouvelle landing (10)

1. **Header** — sticky minimal : logo + 5 nav items + CTA doré
2. **Hero** — plein écran navy, headline percutant centré sur la valeur, sous-titre, 2 CTAs (primaire or + outline blanc), 4 métriques clés animées
3. **Social proof strip** — bande fine avec "Conçu pour avocats FR + BE", badges confiance (RGPD, hébergement Paris, OAuth2)
4. **Problème/Solution** — 3 pain points visuels + la réponse AI LegalCase en 6 check-items
5. **Domaines** — 3 cards colorées (Travail bleu, Immigration or, Famille vert) avec 3-4 outils phares par domaine
6. **Pipeline IA** — 5 étapes visuelles sur fond sombre, chiffres clés (temps d'analyse, tokens, coût)
7. **10 outils décisionnels** — grille 2×5 avec icônes + description courte + badge "FR+BE"
8. **Différenciation** — 4 colonnes "Pourquoi AI LegalCase" : Pipeline 3 niveaux / Traçabilité / Contrôle cohérence / Multi-domaines
9. **Pricing** — 4 plans (FREE/SOLO/TEAM/PRO) avec feature comparison
10. **FAQ** — 7 questions accordion + **CTA final** + **Footer**

---

## Design

- Palette : navy `#1A3A5C`, or `#C9973A`, blanc `#FFFFFF`, fond clair `#F5F6FA`, texte `#1C2B3A`
- Typo : Merriweather 700 titres, Inter 400 corps
- Animations : fade-in au scroll via IntersectionObserver (existant préservé)
- Responsive : breakpoints 900px + 480px

---

## Hors scope

- Modification du TS (SEO, meta tags, JSON-LD) — préservé tel quel
- Modification du routage
- Ajout d'images/illustrations (SVG mat-icons uniquement)
- A/B testing
- Landing pages par domaine

---

## Critères d'acceptation

- [ ] Les 10 sections sont présentes et visuellement distinctes
- [ ] Les 3 domaines juridiques sont présentés avec leurs outils phares
- [ ] Les 10 outils décisionnels sont listés
- [ ] Les 4 plans pricing sont affichés avec features
- [ ] Le design suit le DESIGN_SYSTEM.md (couleurs, typo, espacement)
- [ ] Responsive mobile (480px) et tablette (900px)
- [ ] Animations scroll préservées
- [ ] Build prod vert, SSG compatible
- [ ] SEO préservé (meta tags, JSON-LD, OG inchangés)
