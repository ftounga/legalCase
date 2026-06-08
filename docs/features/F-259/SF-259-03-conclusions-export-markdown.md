# Mini-spec — F-259 / SF-259-03 — Export Word/PDF des conclusions parse le Markdown (frontend)

## Identifiant
`F-259 / SF-259-03`

## Statut
`ready`

## Date
2026-06-08

## Branche Git
`feat/SF-259-03-conclusions-export-markdown`

## Contexte / motif
Le contenu des conclusions est en **Markdown** (`#`/`##`/`###`, `**gras**`, `*italique*`, listes, `>`, `---`). SF-259-02 a rendu ça proprement **à l'écran**. Mais l'**export** est en retard :
- **Word** (`docx-export.service.ts`, `exportConclusion()`) : utilise une heuristique MAJUSCULES (`isSectionHeading`) → traite `# CONSEIL…` comme un paragraphe contenant le `#`, et `**POUR**` reste avec les `**`. Le `.docx` déposé/envoyé contient donc les marqueurs Markdown en littéral. **C'est le livrable réel de l'avocat → le plus grave.**
- **PDF** (`pdf-export.service.ts`) : **aucune** méthode d'export des conclusions (`exportConclusion()` absente ; le service ne gère que les synthèses d'analyse).

## Objectif (1 phrase)
Faire produire à l'export **Word** ET **PDF** un document de conclusions **propre** (vrais titres, gras, italique, listes — zéro marqueur Markdown), **neutre et sans branding** (l'avocat le reprend sur son en-tête de cabinet), en parsant le Markdown du `content`.

## Comportement attendu

### Parsing commun
- Utiliser **`marked.lexer(content)`** (déjà dépendance, ajoutée SF-259-02) pour obtenir des **tokens structurés** (heading depth, paragraph + tokens inline `strong`/`em`/`text`, list ordered/unordered + items, blockquote, hr, code) — plus fiable que parser du HTML pour mapper vers `docx`/`pdfmake`.
- Idéalement factoriser un petit mapper de tokens partagé entre Word et PDF (au moins la logique inline gras/italique).

### Word (`docx-export.service.ts` — `exportConclusion()`)
- Remplacer l'heuristique MAJUSCULES par le mapping des tokens Markdown :
  | Token | Rendu docx |
  |---|---|
  | heading 1 | `HeadingLevel.HEADING_1` (gras, taille > corps) |
  | heading 2 | `HEADING_2` |
  | heading 3 | `HEADING_3` |
  | paragraph | `Paragraph` avec `TextRun` par token inline (`strong`→`bold:true`, `em`→`italics:true`, `text` brut) |
  | list (ordered/unordered) | items en `Paragraph` avec `numbering`/`bullet` docx |
  | blockquote | `Paragraph` indenté / style citation |
  | hr | `Paragraph` séparateur ou `ThematicBreak` |
  | code/codespan | police mono (sobre) |
- **Zéro marqueur** `#`/`**`/`*`/`-` dans le rendu. Document **neutre** : pas de logo/cartouche LegalCase. Mise en page sobre (marges raisonnables, interligne lisible).

### PDF (`pdf-export.service.ts` — nouvelle `exportConclusion()`)
- Créer une méthode symétrique qui parse les mêmes tokens → contenu **`pdfmake`** stylé : headings (gras, taille décroissante), paragraphes (avec runs gras/italique via `{ text, bold/italics }`), listes `ol`/`ul`, blockquote, hr. Document neutre, sobre, sans branding.
- **Câbler** le déclencheur d'export PDF des conclusions dans `conclusions-section.component` s'il n'existe pas encore (vérifier les boutons d'export existants ; ajouter « Exporter en PDF » à côté de « Exporter en Word » si absent), avec le même nom de fichier conventionnel que le Word.

### Cas limites
| Situation | Comportement |
|---|---|
| `content` vide/null | pas d'export (bouton désactivé / no-op, comme l'existant) |
| `content` sans Markdown (texte brut) | paragraphes simples (marked rend `<p>`/paragraph tokens) — pas de marqueur résiduel |
| inline imbriqué (gras+italique) | combiné (`bold:true, italics:true`) |

## Critères d'acceptation
- [ ] Export **Word** : ouvre un `.docx` sans aucun `#`/`##`/`**`/`*`/`-` littéral ; titres en vrais styles Heading, gras/italique appliqués, listes rendues.
- [ ] Export **PDF** : `exportConclusion()` existe, produit un PDF propre (mêmes règles), déclenchable depuis l'UI.
- [ ] Les deux documents sont **neutres** (aucun branding LegalCase).
- [ ] `content` vide → pas d'export ; texte brut → paragraphes (pas de crash).
- [ ] Specs existantes `docx-export.service.spec.ts` adaptées (l'ancien test D-54 sur `isSectionHeading` pour les conclusions est remplacé par des tests Markdown→docx) ; pas de régression sur les autres exports (synthèses).
- [ ] `npm run build` vert.

## Plan de test
- **Jest docx** : `exportConclusion` sur un Markdown type (`# Titre`, `**gras**`, `*ital*`, liste, `>`) → vérifier la structure docx (headings, runs bold/italics, numbering) et **l'absence des marqueurs** dans le texte des runs.
- **Jest pdf** : `exportConclusion` → vérifier le `docDefinition` pdfmake (styles heading, runs bold/italics, listes), absence de marqueurs.
- **Régression** : exports de synthèses (docx + pdf) inchangés ; `conclusions-section.component.spec.ts` vert.
- `npm run build`.

## Composants impactés
- **Modifié** : `core/services/docx-export.service.ts` (`exportConclusion` → markdown tokens), `core/services/pdf-export.service.ts` (nouvelle `exportConclusion`), éventuellement `conclusions-section.component.{ts,html}` (bouton export PDF si absent), specs des deux services.
- **Nouveau (optionnel)** : un util `markdown-tokens.ts` partagé (lexer + mapping inline) pour éviter la duplication Word/PDF.
- **Possiblement retiré** : `core/utils/section-heading.ts` si plus aucun usage après bascule (vérifier les références avant suppression).
- **Inchangé** : backend, contenu généré, rendu écran (SF-259-02), édition.

## Hors périmètre
- Mise en page « papier à en-tête de cabinet » personnalisable (l'avocat re-brande lui-même).
- Couleur/soulignement automatique par type de contenu juridique (suivi éventuel).
