# Mini-spec — F-260 / SF-260-01 — Numérotation persistante & ordre des pièces

> Base : `project-governance/templates/subfeature-template.md`. Amont de SF-98-57.

## Identifiant
`F-260 / SF-260-01`

## Feature parente
`F-260` — Numérotation persistante & ordre des pièces communiquées

## Statut
`ready` (étape 0 GO + 0 bis GO avec ajustements)

## Date
2026-06-09

## Branche
`feat/SF-260-01-numerotation-pieces`

---

## Objectif
> Donner à chaque pièce un **numéro persistant et stable** (qui ne glisse pas à l'ajout/suppression d'un document), réordonnable par l'avocat, lu par les conclusions (F-98) **et** la fiche prud'homale.

## Comportement attendu

### Cas nominal
1. À la création d'une pièce (`DocumentPiece`), un `piece_number` lui est attribué = **max(piece_number du dossier) + 1** → les nouvelles pièces s'**ajoutent à la fin**, sans toucher les numéros existants.
2. Suppression d'un document/pièce : les numéros des autres pièces **ne changent pas** (trou conservé).
3. L'avocat **réordonne** les documents dans la table (onglet Dossier) → `PUT …/pieces/order` réassigne `piece_number` 1..N dans le nouvel ordre (renumérotation **explicite**, voulue).
4. `loadNumberedPieces` (F-98) et `buildPiecesList` (fiche prud'homale) lisent le `piece_number` **persistant** au lieu de le recalculer.
5. La table des documents affiche la **colonne « N° »**.

### Cas d'erreur
| Situation | Comportement | HTTP |
|---|---|---|
| Réordonnancement avec liste de pièces incomplète/étrangère au dossier | rejet | 400 |
| Pièce/dossier d'un autre workspace | accès refusé | 404 |
| Body order vide | no-op ou 400 | 400 |

---

## Analyse de cohérence transversale
- **Consommateurs du numéro** : `CaseConclusionService.loadNumberedPieces` (F-98) ET `PrudhomeFicheService.buildPiecesList` (fiche). **Bascule simultanée obligatoire** (sinon F-98 et fiche divergent) — invariant « source unique ».
- **Tribunal du travail fiche** (`tribunal-travail-fiche-section`) : vérifier si elle liste aussi des pièces → aligner si oui.
- **Pays/domaines** : numérotation agnostique (mécanique pure sur `document_pieces`).
- **Pattern partagé** : nouvel endpoint d'ordre + colonne persistante.

### Résultat du scan
| Cible | Applicable | Traitement |
|---|---|---|
| F-98 `loadNumberedPieces` | Oui | Intégré (bascule vers `piece_number`) |
| Fiche prud'homale `buildPiecesList` | Oui | Intégré (bascule + alignement piece-level) |
| Tribunal travail fiche | À vérifier au dev | Aligner si elle numérote des pièces, sinon N/A |
| SF-98-57 bordereau | Oui | Consommateur aval (SF suivante) |

### Décision
- [x] Étendu à tous les consommateurs de numérotation dans cette SF (bascule simultanée).
- [x] Réordonnancement avancé multi-niveaux (pièces dans composite) = backlog si besoin.

## Conformité F-IA-04
- [x] **Non applicable** — pas de section décisionnelle/`TOOL_REGISTRY` ; feature d'infrastructure documentaire (numérotation + table documents).

## Champs IA à extraire
- [x] **Aucun pré-remplissage** — le numéro/ordre est posé par l'avocat ou attribué par le système, pas extrait par l'IA.

---

## Critères d'acceptation
- [ ] Une pièce créée reçoit `piece_number = max+1` ; ajouter un document **ne modifie pas** les numéros existants.
- [ ] Supprimer un document **ne modifie pas** les numéros des pièces restantes (trou conservé).
- [ ] `PUT …/pieces/order` réassigne 1..N dans l'ordre fourni ; isolation workspace (404 cross-workspace).
- [ ] `loadNumberedPieces` (F-98) renvoie le `piece_number` persistant (plus de recalcul `createdAt DESC`).
- [ ] `buildPiecesList` (fiche prud'homale) renvoie le `piece_number` persistant ; F-98 et fiche affichent **le même** numéro pour une pièce donnée.
- [ ] Backfill : les dossiers existants reçoivent une numérotation déterministe (ordre chronologique `createdAt ASC` + `orderIndex`) sans casser les pièces/labels F-145.
- [ ] La table des documents (onglet Dossier) affiche la colonne « N° » ; réordonnancement persiste et renumérote.
- [ ] Isolation workspace sur l'endpoint d'ordre.

## Périmètre
### Hors scope
- Réordonnancement fin des pièces **à l'intérieur** d'un document composite (reste piloté par `orderIndex` F-145).
- Renumérotation automatique « compactante » à la suppression (on conserve les trous — pratique avocat).

## Valeurs initiales
| Champ | Valeur | Règle |
|---|---|---|
| `document_pieces.piece_number` | `max(dossier)+1` à la création | jamais réutilisé après suppression |

## Technique
### Endpoints
| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| PUT | `/api/v1/case-files/{caseFileId}/pieces/order` | Oui | LAWYER |
| GET | `/api/v1/case-files/{caseFileId}/documents` (existant — expose `pieceNumber` par pièce) | Oui | MEMBER |

Body PUT : `{ "orderedPieceIds": [uuid, …] }` (ordre cible des pièces du dossier) → 200 (liste à jour).

### Tables
| Table | Opération | Notes |
|---|---|---|
| `document_pieces` | ALTER (+`piece_number` INT) + backfill + UPDATE (réordonnancement) + SELECT | unique par `case_file` |

### Migration Liquibase
- [x] Oui — `598-add-piece-number-to-document-pieces.xml` (numéro **à confirmer au dev** = prochain libre ; UUID pré-assigné). Colonne `piece_number` INT nullable puis **backfill** déterministe puis éventuellement NOT NULL. Backfill : `ROW_NUMBER()` par `case_file` ordonné `documents.created_at ASC, document_pieces.order_index ASC`.

### Backend — fichiers
- `DocumentPiece` (+ `pieceNumber`), `DocumentPieceSummary` / `DocumentResponse` (+ `pieceNumber`).
- Assignation à la création de pièce (service F-145 de création de `DocumentPiece`) : `max+1` par dossier.
- `CaseConclusionService.loadNumberedPieces` → lit `piece_number`.
- `PrudhomeFicheService.buildPiecesList` → lit `piece_number` (alignement piece-level).
- Nouveau `PieceOrderController`/méthode + service de réordonnancement (réassignation 1..N, isolation workspace).
- Repository : tri par `piece_number`.

### Composants Angular
- `case-file-detail` table documents (`#section-documents`) : colonne « N° » + réordonnancement (drag `cdkDrag`/`cdkDropList` ou flèches) → appel `PUT …/pieces/order`. `OnPush` + `markForCheck()`.
- Modèle TS document/pièce : `pieceNumber`.

---

## Plan de test
### Unitaires (backend)
- [ ] Création pièce → `piece_number = max+1` ; ajout ne touche pas l'existant.
- [ ] Suppression → numéros restants inchangés (trou).
- [ ] Réordonnancement → réassignation 1..N correcte ; rejet si liste incomplète/étrangère.
- [ ] `loadNumberedPieces` & `buildPiecesList` lisent `piece_number` ; cohérence F-98 ↔ fiche.
- [ ] Backfill déterministe (test sur jeu de données).
### Intégration
- [ ] `PUT …/pieces/order` 200 / 400 (liste invalide) / 404 (cross-workspace).
- [ ] `GET …/documents` expose `pieceNumber`.
### Isolation workspace
- [x] Applicable — réordonnancement borné au workspace.

## Analyse d'impact
### Préoccupations transversales
- [x] **Aucune** (auth/workspace/plans/navigation inchangés ; isolation réutilise le pattern documents existant).
### Smoke tests E2E
- [ ] Aucun smoke E2E n'exerce la numérotation des pièces ; couvert par UT/IT + validation staging.

## Dépendances
- F-145 (pièces : `DocumentPiece` type/label/orderIndex) — `done`.
- **Bloque** SF-98-57 (bordereau).

## Notes et décisions
- **Granularité = pièce** (`DocumentPiece`), unité communiquée et déjà celle de F-98 ; la fiche prud'homale s'aligne piece-level (mono-pièce = inchangé visuellement).
- **Stabilité = append + gap** : ajouter/supprimer ne renumérote jamais ; seul le réordonnancement explicite renumérote.
- **Source unique** : F-98 et fiche lisent le même `piece_number`.
