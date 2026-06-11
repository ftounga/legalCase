# SF-267-02 — Mode édition des conclusions en pleine largeur & embelli

> Itération UX sur F-267 (signal PO 2026-06-11 : « en mode modifier, l'aperçu et l'édition sont trop étroits »). Frontend-only, présentation.

## Objectif (1 phrase)
En **mode édition** des conclusions, élargir la page (éditeur + aperçu côte à côte) bien au-delà de la feuille de lecture 880px et soigner les deux panneaux, sans toucher le confort de lecture.

## Comportement nominal
- **Lecture** : la feuille d'acte reste à **880px** (inchangé).
- **Édition** (`.cs-edit-split` présent) : l'en-tête + la feuille passent à **`min(1640px, 100%)`** via `:has(.cs-edit-split)` → ~2× plus large. Transition douce.
- **Panneaux** : éditeur (textarea) `min-height: 58vh` + remplit la hauteur ; aperçu rendu comme une **feuille** (surface blanche, filet or, ombre douce) avec **défilement interne** (`max-height: 72vh`) et **sticky**, pour que les 2 panneaux restent alignés. Gap porté à 24px.
- **Libellés** symétriques **« Édition » / « Aperçu »** au-dessus des panneaux (large écran).
- **Étroit (< 900px)** : inchangé — une colonne + bascule éditeur/aperçu (les libellés se masquent).

## Cas limites
- Acte très long → l'aperçu défile dans son cadre (la page ne s'allonge plus démesurément).
- Navigateur sans `:has()` → la feuille reste à 880px (dégradation gracieuse, pas de casse).

## Critères d'acceptation
1. Hors édition : feuille à 880px (non-régression lecture).
2. En édition : feuille élargie ; éditeur et aperçu nettement plus larges.
3. Les deux libellés « Édition » et « Aperçu » présents en mode édition (testé).
4. Responsive < 900px : bascule conservée, une colonne.

## Plan de test
- Jest : libellés présents en édition (`conclusions-section.component.spec.ts`).
- Largeur/hauteur = CSS pur (`:has`, vh) → vérif visuelle Playwright sur staging.

## Composants impactés
- `case-file-conclusions-page.component.scss` (largeur édition `:has`).
- `conclusions-section.component.{html,scss,spec.ts}` (libellé éditeur + hauteur/carte aperçu).

## Hors périmètre
- Logique d'édition / sauvegarde / co-rédaction / export (inchangées).
- Mode lecture (largeur inchangée).
