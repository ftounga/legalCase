# Mini-spec — F-158 / SF-158-06 — Nettoyage claims + SEO V4 + allègement charge écran

## Identifiant

`F-158 / SF-158-06`

## Feature parente

`F-158` — Refonte landing page (vague V4). Dernière SF de la vague.

## Statut

`ready`

## Date de création

2026-06-03

## Branche Git

`feat/SF-158-06-claims-seo-allegement`

---

## Objectif

Aligner la fiabilité et le SEO de la landing sur le produit V4 : retirer les claims non sourcés, corriger le « 92 » résiduel du SEO et alléger la charge de l'écran (anti-surcharge cadrage étape 0 bis).

---

## Comportement attendu

### Cas nominal

1. **Claim « 10× plus rapide » retiré** (hero stat, l.46-47) — non documenté (invariant anti-overclaim étape 0). Remplacé par une stat **factuelle vérifiable** : valeur `2`, label « Pays — France & Belgique ». La grille hero garde 4 stats.
2. **Claim « fax 200 dpi » nuancé** (l.382-383) : retrait de la précision technique invérifiable « PDF scannés à 200 dpi » ; message général conservé (scans, fax administratifs, photocopies anciennes) sans chiffre non testé.
3. **SEO aligné V4** (`landing.component.ts`) : les 5 occurrences de « 92 » (title l.107, description l.108, shortDescription l.109, JSON-LD description l.156, featureList l.158) → « 250+ » ; et enrichissement avec **génération de conclusions** + **jurisprudence Cassation vérifiable** (cohérence avec le contenu V4). JSON-LD `featureList` complété de 2 capacités (conclusions, jurisprudence).
4. **Allègement charge écran** : section « Fonctionnalités » (8 cartes, l.221-297) réduite à **6 cartes** — fusion/retrait des 2 cartes désormais redondantes avec le contenu V4 : « Export PDF & comparaison » (couvert par la nouvelle section Conclusions) et une 2ᵉ recoupant Pipeline/Différenciation (ex. « Synthèse structurée » ↔ Pipeline). Aucune information réelle perdue (couverte ailleurs).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Stat hero sans valeur numérique | Le compteur animé doit avoir un `data-target` numérique valide |
| SEO title vide / trop long | title < 70 car, description < 160 car (bonnes pratiques) |

---

## Analyse de cohérence transversale

- **Anti-overclaim (invariant étape 0)** : après cette SF, plus aucun claim chiffré non sourcé (« 10× », « 200 dpi ») sur la landing. Le « 250+ » est sourcé (TOOL_REGISTRY).
- **Cohérence SEO ↔ contenu** : le SEO doit refléter le contenu visible V4 (conclusions, jurisprudence, 250+).
- **Charge écran (invariant étape 0 bis)** : la vague V4 a ajouté 1 section primaire (`#conclusions`). L'allègement de Fonctionnalités (8→6) compense et tient le plafond « +1 net max ».
- **Préoccupation transversale** : aucune (page publique statique, aucun backend/route/auth/workspace/outil).

---

## Critères d'acceptation vérifiables

1. ✅ Le hero ne contient plus « 10× » ni « Plus rapide qu'une lecture manuelle » ; il a une stat « France & Belgique » / `2`.
2. ✅ La section OCR ne contient plus « 200 dpi ».
3. ✅ `landing.component.ts` ne contient plus « 92 » ; title/description/JSON-LD mentionnent « 250+ » et la génération de conclusions.
4. ✅ La section Fonctionnalités a 6 cartes (au lieu de 8).
5. ✅ Aucun claim chiffré non sourcé ne subsiste sur la landing (vérif grep).
6. ✅ SEO : title < 70 car, description < 160 car.
7. ✅ Style inchangé (DESIGN_SYSTEM), aucune nouvelle palette.

---

## Plan de test minimal

- **Jest `landing.component.spec.ts`** : (a) le template hero ne contient plus « 10× » / « Plus rapide » ; (b) la section OCR ne contient plus « 200 dpi » ; (c) la section Fonctionnalités rend 6 cartes ; (d) garde-fou : le template ne contient plus « 92 ».
- **Jest tests SEO existants** : adapter les assertions « 92 » → « 250+ » (les 2 tests SEO actuels restent verts).
- **Smoke E2E** : N/A (pas de nouvelle structure ; assertions existantes inchangées).
- **Isolation workspace** : N/A (page publique).

---

## Tables / endpoints / composants impactés

- `frontend/src/app/landing/landing.component.html` — hero stat (l.46-47), OCR (l.382-383), Fonctionnalités (l.221-297).
- `frontend/src/app/landing/landing.component.ts` — SEO title/meta/JSON-LD (l.107-158).
- `frontend/src/app/landing/landing.component.spec.ts` — tests.
- **Aucun** backend, table, endpoint, migration, outil décisionnel, catalogue.

---

## Hors périmètre

- Toute modification du catalogue / des outils / du backend / de la jurisprudence.
- Refonte visuelle / charte.
- Section Conclusions (livrée SF-158-05).
