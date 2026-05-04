# SF-158-02 — Grille interactive 92 outils + showcase OCR/Vision

## Objectif

Donner une démonstration **concrète** du repositionnement F-158 : ne plus dire "92 outils" mais les **rendre visibles, filtrables, en grille** sur la landing, et expliquer **comment l'IA les pré-remplit** via une mini-section dédiée OCR (Textract) + Vision (Claude multimodal).

## Comportement nominal

### Composant `<app-landing-tools-showcase>`
- Affiche par défaut les 92 outils décisionnels en grille auto-fill.
- Filtres en chips :
  - Domaine : Tous / Travail / Immigration / Famille
  - Pays : Tous / France / Belgique
- Filtres cumulables (AND).
- Compteur dynamique « N outils affichés ».
- Chaque card : badge type (CALCULATEUR / SCORING / COMPARATEUR / GÉNÉRATEUR / DÉTECTEUR / OUTIL), label en JetBrains Mono, badges domaine + pays en bas.
- État vide : message « Aucun outil pour ces filtres ».

### Section OCR + Vision
- 2 cards : OCR Textract et Claude Vision.
- Animations CSS subtiles :
  - OCR : ligne de scan dorée traversant un faux document de haut en bas (loop 2.4s).
  - Vision : 3 bulles SMS qui apparaissent en stagger (loop 3s).
- Respect `prefers-reduced-motion: reduce`.

### Catalogue auto-généré
- Source de vérité : `TOOL_REGISTRY` (decisional-tools-panel.component.ts) + `TOOL_LABEL` static des composants outil.
- Script Python `/tmp/build-catalog.py` régénère `frontend/src/app/landing/landing-tools-catalog.ts` à la demande (refresh trimestriel).
- 92 entries au moment de la SF.

## Cas d'erreur

- Aucun appel API. Tout côté client. Pas d'erreur runtime.
- Si le catalogue diverge de TOOL_REGISTRY (ajout d'outil sans refresh catalogue) → simple sous-affichage cosmétique. Pas de bug fonctionnel.

## Critères d'acceptation

- [x] La grille affiche 92 cards par défaut.
- [x] Filtre domaine TRAVAIL réduit à `n` cards = `LANDING_TOOLS_CATALOG.filter(t => t.domain === 'TRAVAIL').length`.
- [x] Filtre pays BE réduit à `n` cards = nombre BE.
- [x] Filtre cumulé domaine + pays applique le AND correctement.
- [x] Section OCR + Vision affiche 2 cards avec animations SVG.
- [x] Animations désactivées via `prefers-reduced-motion`.
- [x] Build prod passe.
- [x] Tests Jest verts (showcase + landing existants).

## Plan de test minimal

- Tests unitaires `landing-tools-showcase.component.spec.ts` :
  - 92 outils dans le catalogue
  - Affichage par défaut = 92 cards
  - Filtre domaine TRAVAIL → bon count
  - Filtre pays BE → bon count
  - Cumul filtres
  - Présence des data-attributes
- Tests landing existants continuent de passer.

## Tables / endpoints / composants impactés

- **Nouveau composant** : `frontend/src/app/landing/landing-tools-showcase/landing-tools-showcase.component.{ts,html,scss,spec.ts}`
- **Nouveau fichier de données** : `frontend/src/app/landing/landing-tools-catalog.ts` (auto-généré, 92 entries)
- **Modifs** : `landing.component.html` (import `<app-landing-tools-showcase>` + nouvelle section OCR+Vision), `landing.component.ts` (import composant), `landing.component.scss` (styles OCR+Vision)

## Hors périmètre

- SEO meta tags V3 complets → SF-158-03
- Sitemap.xml → SF-158-03 (déjà dynamique côté backend, juste à vérifier)
- Smoke E2E test landing → SF-158-03
- Refresh catalogue automatique (build-time generator) — SF-158-02 garde le refresh manuel

## Analyse de cohérence transversale

- **Préoccupations transversales** : aucune (composant client-side autonome, pas d'auth, pas de workspace, pas de plan, pas de routing modifié).
- **Nouveau pattern UI** : oui — `LandingTool` interface + `LANDING_TOOLS_CATALOG` constant + composant standalone. Pattern isolé à la landing publique, n'introduit pas de dette de convergence avec les composants outils décisionnels (qui consomment TOOL_REGISTRY au runtime). Le catalogue landing est **figé** au build-time.
- **Impact par domaine métier** : transversal, démonstration équilibrée des 3 domaines.

## Parité des domaines métier

- Pas applicable : SF marketing/landing, pas un outil décisionnel.

## Contrat API

- Pas applicable : SF frontend pure, aucun appel API.
