# SF-277-01 — Éditeur WYSIWYG des conclusions (round-trip markdown garanti)

> Étape 0 (`SF-277-00-coherence.md`, GO conditionné) + 0 bis (`SF-277-00b-ux-coherence.md`, GO) + spike (défauts `[…]`→`\[…\]` et `-`→`*` identifiés, mitigeables). Frontend-only.

## Objectif (1 phrase)
Offrir à l'avocat une **surface d'édition WYSIWYG** de l'acte (zéro syntaxe markdown visible), **le markdown restant la source de vérité stockée**, avec un **round-trip markdown garanti** (prouvé par gate CI) pour ne casser aucun consommateur existant (export, sommaire, co-rédaction, diff, alerte placeholders).

## Approche technique (dé-risquée par le spike)
- **ProseMirror** sur le **schéma `prosemirror-markdown`** (parser `defaultMarkdownParser` + serializer **custom**) — parser et serializer **partagent le même schéma** (round-trip bien défini), évite la cartographie de schéma d'un wrapper tiers.
- **Serializer custom** (`conclusionMarkdownSerializer`) dérivé de `defaultMarkdownSerializer` avec :
  - dé-échappement post-traitement qui **n'échappe PAS `[` / `]`** (placeholders `[…]` préservés) ;
  - `bulletList` forçant le marqueur **`-`** (aligné sur le markdown généré par le LLM) ;
  - vérifié **idempotent** sur corpus.
- **Composant Angular** `conclusion-wysiwyg-editor` : monte un `EditorView` ProseMirror, `@Input() markdown`, `@Output() markdownChange` (débattu/normalisé), `keymap` + `history` + `baseKeymap`.
- **Barre d'outils** : réutilise les mêmes actions que F-264 (gras, italique, titre ##, titre ###, liste, citation) câblées sur des commandes ProseMirror (`toggleMark`, `setBlockType`, `wrapIn`, `wrapInList`).
- **Lazy-load** : la lib ProseMirror est chargée sur la route conclusions (pas dans le bundle principal).

## Comportement nominal
- En mode **Modifier**, la **surface WYSIWYG remplace** le textarea markdown **et** l'aperçu live (devenu redondant) → 1 seule colonne rendue comme l'acte (style `conclusion-document`).
- L'avocat met en forme directement (gras, titres, listes) ; à chaque changement, `markdownChange` émet le **markdown** sérialisé → alimente `draftContent` (inchangé en aval : save F-279, régénération, diff F-280, sommaire F-276, co-rédaction F-265 continuent sur le markdown).
- **Co-rédaction IA (F-265)** : barre conservée au-dessus de la surface.
- **Toggle « source markdown »** (replié par défaut) : réaffiche le textarea brut (fallback power-user / cas limite).
- **Première génération / lecture** : inchangées.

## Cas d'erreur / bords
- Markdown non parsable → fallback **textarea markdown** + message non bloquant (jamais de perte de contenu).
- Placeholders `[…]`, renvois « Pièce n° X », montants, titres `##/###` : **préservés** (gate).
- Lib non chargée (réseau) → fallback textarea.

## Critères d'acceptation
1. **Gate round-trip (CI)** : pour un **corpus d'actes réels** (DURAND + échantillons multi-cellules), `serialize(parse(md))` :
   - préserve **à l'identique** : titres `##/###`, placeholders `[…]` (sans `\`), « Pièce n° X », montants, **gras**, listes `-` ;
   - est **idempotent** : `rt(rt(md)) === rt(md)` ;
   - ne produit **aucun `\[`** ni marqueur de liste `*`.
2. Éditer en WYSIWYG puis enregistrer → `draftContent` reste du **markdown propre** ; export/diff/sommaire/co-rédaction/alerte placeholders **inchangés** (tests existants verts).
3. La barre d'outils applique gras/italique/titres/listes/citation.
4. Toggle « source markdown » fonctionne (WYSIWYG ↔ textarea).
5. Mode WYSIWYG = **1 surface** (aperçu live retiré en WYSIWYG) ; responsive OK.
6. Lib lazy-loadée (pas dans `main`).

## Plan de test
- **Unitaire (gate)** : `conclusion-markdown-serializer.spec.ts` — round-trip + idempotence + invariants (corpus).
- **Composant** : `conclusion-wysiwyg-editor.component.spec.ts` — markdown in → édition → markdownChange ; toolbar ; fallback parse-error.
- **Non-régression** : suites existantes `conclusions-section`, `conclusion-diff.util`, `conclusion-sections.util`, `extractPlaceholders` restent vertes.

## Composants / fichiers
- **Créer** : `conclusion-wysiwyg-editor/` (composant + serializer custom + util round-trip + specs).
- **Modifier** : `conclusions-section.component.{html,ts,scss}` (mode édition : surface WYSIWYG + toggle source), `package.json` (deps prosemirror).
- **Inchangé** : stockage, export, diff, sommaire, co-rédaction, alerte placeholders (consomment le markdown).

## Hors périmètre
- Changement de format de stockage (reste markdown).
- Collaboration temps réel ; tables/images complexes (markdown de base : titres/gras/italique/listes/citation/paragraphes).
- Suppression de F-264 (le textarea reste accessible en fallback « source »).

## Dépendances ajoutées
`prosemirror-model`, `prosemirror-state`, `prosemirror-view`, `prosemirror-markdown`, `prosemirror-commands`, `prosemirror-keymap`, `prosemirror-history`, `prosemirror-schema-list` (lazy-loadées).
