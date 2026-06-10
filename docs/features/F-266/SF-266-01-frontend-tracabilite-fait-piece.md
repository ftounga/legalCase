# Mini-spec — F-266 / SF-266-01 — Frontend : traçabilité fait → pièce au survol

> Programme Conclusions V2 / F-266. Frontend-only. Étape 0 GO avec ajustements + 0 bis GO avec ajustements.

## Identifiant
`F-266 / SF-266-01`

## Statut
`ready`

## Branche
`feat/SF-266-01-tracabilite-fait-piece`

## Objectif
> Dans l'**aperçu « acte »** des conclusions, rendre chaque renvoi **« Pièce n° X »** survolable : au survol, afficher le **libellé et le type** de la pièce correspondante (n° persistant F-260), pour vérifier l'ancrage **sans quitter l'acte**.

## Comportement attendu

### Cas nominal
1. L'avocat est sur la section Conclusions, version `DONE`, aperçu « acte » affiché (`ConclusionDocumentComponent`).
2. Le composant reçoit, en plus du `content`, la **liste des pièces** du dossier (`pieceNumber`, `type`, `label`) — déjà chargée par le parent (`DocumentService.listDocuments`).
3. Après le rendu markdown→HTML, chaque occurrence du motif **`Pièce n° X`** (X entier) dont **X correspond à une pièce connue** est décorée : soulignement pointillé léger + curseur d'aide + `title` natif = « `<libellé>` — `<type lisible>` » (anti-jargon : type via libellé FR, pas l'enum brut).
4. Au survol → l'info-bulle native révèle la pièce. Aucun clic, aucune navigation, aucune requête réseau.

### Cas d'erreur / bords
| Situation | Comportement |
|---|---|
| `Pièce n° X` sans pièce connue (X hors liste, pièce supprimée) | **aucune décoration** (texte brut inchangé) — jamais d'info inventée |
| Liste de pièces vide / non fournie | aucune décoration ; aperçu identique à l'actuel |
| Aucun `Pièce n° X` dans le texte | aperçu identique à l'actuel |
| Variantes de casse/espaces (`Pièce n°4`, `Pièce n° 4`) | reconnues (regex tolérante aux espaces) |
| Contenu markdown contenant déjà du HTML | la décoration opère sur le HTML rendu de façon sûre (pas d'injection — voir sécurité) |

## Critères d'acceptation
- [ ] Un `Pièce n° X` avec pièce connue est décoré d'un `title` = libellé + type lisible.
- [ ] Un `Pièce n° X` sans pièce connue n'est **pas** décoré (texte intact).
- [ ] Le `content` markdown stocké **n'est pas modifié** ; seul le rendu HTML de l'aperçu est décoré.
- [ ] Aucun appel réseau ajouté ; la liste des pièces vient du parent (déjà chargée).
- [ ] Round-trip export non régressé (l'export ne consomme pas le HTML décoré, il part du `content`).
- [ ] Sécurité : la décoration n'introduit pas de HTML non sanitizé (échappement du libellé dans le `title`).

## Tables / endpoints / composants impactés
- **Composant** : `ConclusionDocumentComponent` — nouvel `@Input() pieces: PieceRef[]` (`{ pieceNumber, label, type }`) ; le `computed html()` post-traite le HTML pour décorer les `Pièce n° X` connus (helper pur `annotatePieceReferences(html, piecesByNumber)`).
- **Parent** : `conclusions-section.component` (ou `case-file-detail`) passe la liste des pièces au `ConclusionDocumentComponent` (mappée depuis `DocumentService.listDocuments` → `DocumentPieceSummary`). Réutilise un type lisible (`DocumentPieceType` → libellé FR, helper front existant si présent, sinon table de correspondance locale).
- **Aucun** backend, endpoint, route, guard, migration.

## Hors périmètre
- Survol montant→calcul (F-263 clos doublon) et article→texte (différé, pas de base légale).
- Clic/navigation vers la pièce (backlog si signal) — V1 = info-bulle seule.
- Décoration en mode édition (textarea) — l'aide est en lecture.

## Plan de test (Jest)
- [ ] `annotatePieceReferences()` : décore `Pièce n° 4` quand la pièce 4 est connue (title = libellé+type) ; laisse intact `Pièce n° 9` inconnu ; gère `Pièce n°4`/`Pièce n° 4`.
- [ ] Échappement : un libellé contenant `"`/`<`/`>` ne casse pas l'attribut `title` (pas d'injection).
- [ ] `ConclusionDocumentComponent` : sans `pieces`, le HTML rendu est identique à l'actuel (non-régression).
- [ ] `content` non muté (le signal d'entrée reste byte-identique).

## Analyse d'impact transversal
- **Auth/workspace/plan/navigation** : aucun (présentation pure, données déjà en mémoire).
- **Outil décisionnel** : N/A.
- **Smoke E2E** : non requis (pas d'auth/workspace/navigation). Couvert par Jest.

## Dépendances
- F-260 (`pieceNumber` persistant), F-259/F-264 (`ConclusionDocumentComponent`), `DocumentService.listDocuments` — `done`.

## Notes
- Décoration **markdown-safe** par construction : on n'altère jamais le `content` (markdown), seulement le HTML d'aperçu → export Word/PDF/versions intacts.
