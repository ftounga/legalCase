# Mini-spec — F-266 / SF-266-02 — Frontend : export à en-tête du cabinet

> Programme Conclusions V2 / F-266. Frontend-only. Étape 0 GO avec ajustements + 0 bis GO avec ajustements.

## Identifiant
`F-266 / SF-266-02`

## Statut
`ready`

## Branche
`feat/SF-266-02-export-entete-cabinet`

## Objectif
> Permettre à l'avocat de **saisir un en-tête de cabinet** (nom, adresse, barreau…) **au moment de l'export** des conclusions, afin que le fichier **Word/PDF** porte cet en-tête en première page, au lieu d'un document neutre — **sans modifier** le `content` markdown stocké ni persister l'en-tête côté serveur.

## Comportement attendu

### Cas nominal
1. Section Conclusions, version `DONE`. Sous les boutons d'export, une action repliée **« Ajouter un en-tête de cabinet »** révèle un `textarea` compact **« En-tête du cabinet (optionnel) »**.
2. L'avocat saisit son en-tête (multi-lignes, ex. « Cabinet Durand & Associés / 12 rue de la Loi, 75001 Paris / Avocat au Barreau de Paris »). La valeur est mémorisée le temps de la session (signal), **non envoyée au serveur**.
3. Clic **Exporter Word** ou **Exporter PDF** → le service reçoit un `content` **préfixé** d'un **bloc d'en-tête markdown** construit depuis la saisie (titre `#` = 1ʳᵉ ligne, lignes suivantes en paragraphes, filet `---`), suivi du `content` original. Le fichier exporté porte l'en-tête en tête de document.
4. Si l'en-tête est **vide** → export **identique au comportement actuel** (neutre).

### Cas d'erreur / bords
| Situation | Comportement |
|---|---|
| En-tête vide / espaces uniquement | export neutre (préfixe omis) — comportement actuel inchangé |
| En-tête multi-lignes | 1ʳᵉ ligne = titre, lignes suivantes = paragraphes, filet de séparation `---` avant le corps |
| Caractères markdown dans l'en-tête (`#`, `*`) | échappés pour rester du texte (l'en-tête est un libellé, pas du markdown à interpréter) |
| Version non `DONE` / pas de `content` | boutons export déjà désactivés (inchangé) ; en-tête sans effet |

## Critères d'acceptation
- [ ] L'en-tête saisi apparaît en tête du **fichier exporté** (Word et PDF), avant le corps des conclusions.
- [ ] Le `content` markdown **stocké** (version) n'est **jamais** modifié ; l'aperçu et les versions restent identiques.
- [ ] En-tête vide → export strictement identique à l'actuel (non-régression).
- [ ] L'en-tête n'est **pas persisté** (aucun appel réseau, aucune sauvegarde).
- [ ] Markdown-safe : le bloc d'en-tête est du markdown valide, tokenisé par le même pipeline (`marked.lexer`) → pas de marqueur brut visible dans le rendu.
- [ ] Le champ est **opt-in** (replié par défaut) — la barre d'actions n'est pas alourdie.

## Tables / endpoints / composants impactés
- **Composant** : `conclusions-section.component` — signal `cabinetHeader = signal('')` ; toggle `showCabinetHeader` ; helper pur `buildExportContent(header, content): string` (préfixe markdown d'en-tête + `---` + content, ou `content` seul si header vide) ; `downloadWord()`/`downloadPdf()` passent `buildExportContent(...)` au lieu de `current.content`.
- **Template** : action repliable « Ajouter un en-tête de cabinet » + `textarea` (mode lecture, à côté des boutons export).
- **Services d'export** : **inchangés** (`DocxExportService`/`PdfExportService.exportConclusion(content, …)` — le préfixe passe par le `content` argument, réutilise le pipeline markdown existant).
- **Aucun** backend, endpoint, route, guard, migration, table.

## Hors périmètre
- **Identité cabinet persistée** (profil cabinet réutilisable, logo, table workspace) → backlog (décision data-model, étape 0 propre requise).
- Logo / image dans l'en-tête (V1 = texte).
- En-tête dans l'aperçu écran ou le `content` (l'en-tête vit **uniquement** dans le fichier exporté).

## Plan de test (Jest)
- [ ] `buildExportContent('', content)` → renvoie `content` inchangé (export neutre).
- [ ] `buildExportContent(header, content)` → préfixe markdown (titre + lignes + `---`) puis `content` ; markdown valide ; caractères `#`/`*` de l'en-tête échappés.
- [ ] `downloadWord()`/`downloadPdf()` appellent le service avec `buildExportContent(header, content)` ; `current.content` non muté.
- [ ] Toggle : le champ est masqué par défaut, révélé au clic.

## Analyse d'impact transversal
- **Auth/workspace/plan/navigation** : aucun (frontend pur, pas de persistance).
- **Outil décisionnel** : N/A.
- **Smoke E2E** : non requis. Couvert par Jest + non-régression export (en-tête vide ⇒ identique).

## Dépendances
- SF-98-50 / SF-98-51 (services d'export), F-259/F-264 (pipeline markdown) — `done`.

## Notes
- **Réversible** : si un signal terrain demande un profil cabinet réutilisable, l'en-tête saisi à l'export sera remplacé par une valeur pré-remplie depuis le profil — `buildExportContent` reste le point d'injection.
