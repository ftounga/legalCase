# Mini-spec — F-259 / SF-259-01 — Rendu « document juridique » des conclusions générées (frontend)

## Identifiant
`F-259 / SF-259-01`

## Feature parente
F-259 — Refonte de l'affichage des conclusions générées (de texte brut « réponse Claude » → document juridique soigné). Refinement UX de F-98 (générateur de conclusions).

## Statut
`ready`

## Date
2026-06-08

## Branche Git
`feat/SF-259-01-conclusions-document-rendering`

## Contexte / motif
Aujourd'hui le projet de conclusions (mode lecture) s'affiche dans un `<pre class="cs-content">{{ conclusion()?.content }}</pre>` (`conclusions-section.component.html:232`) : texte brut, police Inter, fond beige hors charte, **aucune structuration** → « look markdown/Claude ». Le contenu généré possède pourtant une structure exploitable : les titres de section sont en MAJUSCULES (`POUR`, `CONTRE`, `EXPOSÉ DES FAITS`, `DISCUSSION`, `PAR CES MOTIFS`, `INVENTAIRE DES PIÈCES`), et l'export Word les détecte déjà via l'heuristique `isSectionHeading` (`docx-export.service.ts`).

## Objectif (1 phrase)
Remplacer, en **mode lecture uniquement**, le `<pre>` brut par un composant qui parse le texte en sections et le rend comme un **document juridique sobre** conforme au design system (titres Merriweather marine, corps Inter justifié, « feuille » blanche), **sans modifier le contenu généré ni l'export ni l'édition**.

## Comportement attendu

### Cas nominal (mode lecture, `editing() === false`)
- Nouveau composant standalone OnPush `<app-conclusion-document [content]="conclusion()?.content">` qui remplace le `<pre>` ligne 232.
- **Parsing** : un util/service `ConclusionParser` découpe `content` (texte brut) en sections `{ title: string|null, lines: string[] }` :
  - une ligne est un **titre de section** si elle satisfait l'heuristique MAJUSCULES (réutiliser/centraliser la logique `isSectionHeading` de `docx-export.service.ts` : trimmée, ≤ 60 caractères, lettres toutes en majuscules, ≥ 1 lettre).
  - le texte avant le 1er titre (s'il existe) forme une section sans titre (`title: null`).
- **Rendu** :
  - Conteneur « feuille » : fond blanc `#FFFFFF`, `mat-card`-like (border-radius 8px, ombre légère), padding généreux. Doit porter `data-testid="conclusions-content"` (préserver le testid existant).
  - **En-tête POUR / CONTRE** : si les 1ères sections sont `POUR …` / `CONTRE …`, les mettre en valeur (parties identifiées, serif marine).
  - **Titres de section** : Merriweather 600, marine `#1A3A5C`, avec un fin filet or `#C9973A` en dessous. Espacement aéré au-dessus.
  - **Corps** : Inter 16px, `color #1C2B3A`, interligne ~1.7, **texte justifié**, paragraphes séparés (une ligne vide du source = saut de paragraphe).
  - **« PAR CES MOTIFS »** : si les lignes de cette section ressemblent à un dispositif (items successifs), les rendre en **liste numérotée** (1., 2., …). Sinon, paragraphes normaux.
  - **Pied de page écran** : mention discrète *« Document de travail — à vérifier par l'avocat »* en petit, gris secondaire `#6B7A8D`. **Écran uniquement** (n'est PAS dans `content`, donc absent de l'export Word/PDF).

### Cas limites
| Situation | Comportement |
|---|---|
| `content` null / vide | état vide existant inchangé (ne pas rendre la feuille) |
| `content` sans aucun titre MAJUSCULES détecté | fallback : rendu en paragraphes simples justifiés (pas de crash, pas de section vide) |
| Ligne MAJUSCULES isolée au milieu du corps | traitée comme titre (cohérent avec l'export Word — comportement assumé) |

### Mode édition (`editing() === true`)
- **Inchangé** : le `<textarea class="cs-editor">` reste tel quel (on édite le texte brut). Aucune régression sur SF-98-49 (save/cancel).

## Design system (contraintes)
- Palette : marine `#1A3A5C`, or `#C9973A`, texte `#1C2B3A` / secondaire `#6B7A8D`, fond `#FFFFFF`, divider `#E0E4EA`. **Pas de beige ad-hoc, pas de nouvelle couleur.**
- Typo : **Merriweather** (serif) pour les titres de section, **Inter** pour le corps. JetBrains Mono interdit ici.
- Sobre, sans ornement excessif, sans branding LegalCase sur le document (c'est le document de l'avocat).

## Critères d'acceptation
- [ ] En mode lecture, les conclusions s'affichent en sections hiérarchisées (titres serif marine + filet or, corps justifié) dans une feuille blanche — plus de `<pre>` beige.
- [ ] Les titres sont détectés par l'heuristique MAJUSCULES partagée (mêmes sections que l'export Word).
- [ ] POUR/CONTRE mis en valeur ; « PAR CES MOTIFS » numéroté si dispositif.
- [ ] `data-testid="conclusions-content"` toujours présent sur le conteneur de rendu.
- [ ] Mention « Document de travail — à vérifier par l'avocat » visible à l'écran, **absente** du `content` exporté.
- [ ] `content` sans titre → fallback paragraphes (pas de crash). `content` vide → état vide inchangé.
- [ ] Mode édition (textarea + save/cancel) inchangé.
- [ ] Conforme design system (palette + Merriweather/Inter). `npm run build` vert.

## Plan de test
- **Jest parser** (`ConclusionParser`) : sections multiples ; texte avant 1er titre → section `title:null` ; aucun titre → 1 section fallback ; détection POUR/CONTRE ; PAR CES MOTIFS → items.
- **Jest composant** (`ConclusionDocumentComponent`) : rend N sections, applique les classes titres/corps, préserve `data-testid`, gère content null/vide, pied de page présent.
- **Régression** : specs existantes `conclusions-section.component.spec.ts` toujours vertes (le testid `conclusions-content` est préservé ; mode édition inchangé).
- `npm run build`.

## Tables / endpoints / composants impactés
- **Nouveau** : `ConclusionDocumentComponent` (`frontend/src/app/case-files/conclusion-document/`) + `ConclusionParser` (util ou service partagé).
- **Modifié** : `conclusions-section.component.html` (remplace le `<pre>` ligne 232 par `<app-conclusion-document>`), `.scss` (retire `.cs-content` si plus utilisé), `.ts` (import du composant).
- **Idéalement partagé** : centraliser `isSectionHeading` pour que `docx-export.service.ts` et le parser écran utilisent la MÊME logique (convergence écran/Word). Refacto optionnelle si risquée — sinon dupliquer la logique à l'identique avec commentaire.
- **AUCUN** changement backend, **aucun** changement du `content` généré, **aucun** changement de l'export.

## Hors périmètre
- Génération backend des conclusions (inchangée).
- Export Word (déjà fonctionnel) — hors refacto optionnelle de l'heuristique partagée.
- **Export PDF des conclusions** : gap pré-existant (pas de `exportConclusion()` dans `pdf-export.service.ts`) — noté, traité séparément.
- Mode édition / éditeur riche.
