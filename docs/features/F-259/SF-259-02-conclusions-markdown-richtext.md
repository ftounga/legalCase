# Mini-spec — F-259 / SF-259-02 — Rendu Markdown + texte riche des conclusions (frontend)

## Identifiant
`F-259 / SF-259-02`

## Statut
`ready`

## Date
2026-06-08

## Branche Git
`feat/SF-259-02-conclusions-markdown-richtext`

## Contexte / motif
SF-259-01 a transformé le `<pre>` brut en composant document (`ConclusionDocumentComponent`) + typo soignée. **MAIS** : le contenu généré par l'IA est en **Markdown** (`#`/`##`/`###` titres, `**gras**`, `*italique*`, listes `-`/`1.`, `***`) — vérifié sur capture réelle (`screenshoot/conclusions.png` : `# CONSEIL DE PRUD'HOMMES DE PARIS`, `## Section Introductive`, `**POUR** Madame…`, `**CONTRE** La société…`). Le composant actuel parse seulement les titres en MAJUSCULES et affiche le reste tel quel → **les marqueurs Markdown (`#`, `##`, `**`, `***`) restent visibles en littéral**, et il n'y a ni gras, ni italique, ni listes rendues. Demande PO : rendre le Markdown ET offrir du style riche (gras, italique, souligné, couleur sobre).

## Objectif (1 phrase)
Faire rendre au `ConclusionDocumentComponent` le **Markdown** du contenu (titres, gras, italique, listes, citations, règles) en HTML stylisé « document juridique » conforme au design system — fini les `#`/`##`/`**` en littéral — en restant sobre (palette marine/or, pas de couleurs criardes).

## Comportement attendu

### Rendu Markdown (mode lecture)
- Le contenu `content` (Markdown) est **parsé** (lib `marked`, légère) → HTML, puis rendu via `[innerHTML]` **sanitizé** par Angular (DomSanitizer / sécurité Angular par défaut — pas de script/style inline injecté ; le style vient des classes CSS du composant, pas du contenu).
- Le composant remplace l'ancien parsing « MAJUSCULES » par le rendu Markdown. (L'util `section-heading.ts` reste utilisé par l'export Word jusqu'à son propre correctif — hors périmètre ici.)
- Mapping de style (classes CSS du composant, tokens design system) :
  | Markdown | Rendu |
  |---|---|
  | `#` (h1) | Merriweather 700, marine `#1A3A5C`, filet or `#C9973A` dessous, centré possible (titre juridiction) |
  | `##` (h2) | Merriweather 600, marine, filet or léger, espacement aéré |
  | `###` (h3) | Merriweather 600, marine, plus petit |
  | `**gras**` (`<strong>`) | **gras** marine `#1A3A5C` |
  | `*italique*` / `_…_` (`<em>`) | *italique* |
  | souligné | supporter `<u>` (et la convention `__texte__` → souligné si simple à activer) ; sinon styliser `<u>` si présent |
  | listes `-`/`1.` (`<ul>`/`<ol>`) | puces/numéros marqueur or `#C9973A`, indentation propre |
  | `>` citation (`<blockquote>`) | filet gauche marine, fond très léger |
  | `---` (`<hr>`) | filet fin divider `#E0E4EA` |
  | paragraphes `<p>` | Inter 16px, `#1C2B3A`, interligne 1.7, **justifié** |
- **Couleur sobre et purposive** : marine pour les titres et le gras, or pour les accents (filets, marqueurs de liste). Pas d'arc-en-ciel. (Rappel : la conclusion reste un acte sobre.)
- Conserver : la « feuille » blanche, le pied de page écran « Document de travail — à vérifier par l'avocat », et `data-testid="conclusions-content"` sur le conteneur.

### Cas limites
| Situation | Comportement |
|---|---|
| `content` null/vide | rien rendu (état vide parent inchangé) |
| `content` sans Markdown (texte brut) | rendu en paragraphes justifiés (marked rend le texte en `<p>`) — pas de marqueur résiduel, pas de crash |
| HTML dangereux dans le contenu | neutralisé par la sanitization Angular |

### Mode édition
- **Inchangé** : le textarea édite le Markdown brut (l'avocat voit/édite le source). Save/cancel SF-98-49 inchangés.

## Hors périmètre (noté pour la suite)
- **Export Word/PDF** : a le MÊME problème (heuristique MAJUSCULES, ne parse pas le Markdown → `##`/`**` en littéral dans l'export). À corriger dans une SF dédiée (SF-259-03 : parser Markdown → `docx`/`pdfmake`). **Non traité ici.**
- Génération backend / prompt (le contenu reste en Markdown, c'est OK — on le rend).
- Couleur « par type de contenu juridique » (ex. colorer les références d'articles) — itération ultérieure si demandé.

## Critères d'acceptation
- [ ] Plus aucun `#`, `##`, `###`, `**`, `***`, `*`, `-` de Markdown visible en littéral à l'écran.
- [ ] Titres `#`/`##`/`###` → vrais titres Merriweather marine + filet or.
- [ ] `**gras**` → gras, `*italique*` → italique, listes → puces/numéros stylés, `>` → citation, `---` → filet.
- [ ] Souligné rendu si présent (`<u>` ou convention).
- [ ] Feuille blanche + pied de page + `data-testid="conclusions-content"` conservés ; sanitization OK.
- [ ] Sobre, conforme design system (marine/or, Merriweather/Inter), pas de couleur hors charte.
- [ ] `content` vide → rien ; texte sans markdown → paragraphes (pas de crash).
- [ ] Mode édition inchangé. Specs `conclusions-section` non-régressées. `npm run build` vert.

## Plan de test
- **Jest composant** : rend h1/h2/h3 depuis `#/##/###` (pas de `#` littéral) ; `**x**`→`<strong>` ; `*x*`→`<em>` ; liste→`<ul>/<ol>` ; `>` →`<blockquote>` ; `---`→`<hr>` ; `<u>` souligné ; content vide→rien ; texte brut→`<p>` ; `data-testid` présent ; pied de page présent ; sanitization (un `<script>` ne s'exécute pas).
- **Régression** : `conclusions-section.component.spec.ts` verte (testid + édition).
- `npm run build`.

## Composants impactés
- **Modifié** : `conclusion-document.component.{ts,html,scss}` (rendu Markdown + styles riches). Ajout dép **`marked`** (package.json). Le parser MAJUSCULES `conclusion-parser.ts` devient inutile pour l'écran → retiré ou conservé sans usage (préférer retrait + maj specs).
- **Inchangé** : `conclusions-section` (toujours `<app-conclusion-document [content]>`), backend, export, édition.
