# SF-148-04 — Indicateurs Vision globaux (liste docs + bandeau + auto-refresh)

## Objectif
Rendre visible l'état de l'enrichissement LegalCase Vision (async) depuis la
vue dossier — sans avoir à ouvrir le `DocumentPreviewDialog`. Corrige aussi
le bug d'absence de rafraîchissement automatique des chips pièces pendant
que le pipeline travaille en arrière-plan.

## Comportement nominal
- **Bandeau global violet** au-dessus de la liste des documents :
  "Analyse visuelle en cours sur N pièce(s)…" — visible tant qu'au moins
  une pièce a `visionStatus === 'PENDING'` sur le dossier. Disparaît dès que
  tout est DONE / FAILED / NOT_APPLICABLE.
- **Badge Vision par document** (colonne Nom) :
  - `PENDING` : "Vision…" teinte navy animée (spinner icon)
  - `DONE` : "Vision" teinte violette (icône `visibility`)
  - `NONE` : pas de badge
- **Polling auto** : tant que `visionPendingCount > 0`, le polling du dossier
  continue (toutes les 3 s) et rafraîchit la liste — chips pièces et badges
  passent de `PENDING` à `DONE` sans rechargement manuel.
- **Démarrage à l'ouverture** : si l'avocat rouvre un dossier dont l'
  enrichissement est encore en cours, le polling démarre automatiquement.

## Bug corrigé
Avant cette SF, `documentsContentEqual` comparait uniquement
`id/extractionStatus/failureReason/ocrRunning/ocrExtracted`. Les `pieces`
et leurs `visionStatus` étaient ignorés → le diff anti-flicker renvoyait
`true` même quand le backend avait produit de nouvelles pièces ou changé
leur statut vision → **les chips pièces n'apparaissaient qu'après un F5**.

## Cas d'erreur
- **Aucune pièce détectée** (documents pré-F-145 ou texte pur) :
  `documentVisionState` retourne `NONE` → aucun badge, pas de bandeau. Pas
  de régression.
- **Vision FAILED** : non différencié visuellement du NOT_APPLICABLE (pas
  de badge). Volontaire — on n'expose pas les échecs internes à l'avocat ;
  F-148 est conçu fail-open.
- **Polling coincé** : si le backend oublie de passer une pièce en `DONE`,
  le bandeau persiste. Protection : le polling s'arrête lorsque l'onglet
  change ou que le composant est détruit (via `stopPolling`).

## Critères d'acceptation
- [x] `documentsContentEqual` détecte un changement de `pieces[].visionStatus`
- [x] `documentsContentEqual` détecte un ajout/suppression de pièce
- [x] `documentsContentEqual` reste `true` pour des pièces strictement
  identiques (anti-flicker préservé)
- [x] `visionPendingCount` / `visionDoneCount` agrègent correctement
- [x] `documentVisionState(doc)` retourne PENDING / DONE / NONE selon l'
  état des pièces
- [x] Bandeau rendu ssi `visionPendingCount() > 0`
- [x] Badge `--pending` / `--done` rendu sur la ligne document selon l'état
- [x] Polling démarre à l'ouverture si des pièces sont PENDING (même sans
  job actif)
- [x] Polling continue tant que vision PENDING existe, même si les jobs
  d'analyse sont tous DONE
- [x] 11 tests unitaires nouveaux, 118 tests totaux verts

## Plan de test
- **Unit frontend** : `SF-148-04 — indicateurs vision` (11 tests)
  - Diff changement visionStatus
  - Diff ajout pièce
  - Diff identique (anti-flicker)
  - Comptage PENDING/DONE
  - État par document (PENDING/DONE/NONE + doc sans pieces)
  - Rendu bandeau + badges pending/done
- **Intégration (staging)** :
  1. Uploader un document scanné contenant ≥ 1 pièce éligible vision
     (photo, pièce d'identité).
  2. Observer le bandeau "Analyse visuelle en cours…" apparaître **sans
     F5** au-dessus de la liste.
  3. Observer le badge "Vision…" apparaître sur la ligne du document.
  4. Attendre ~10-30 s → badge bascule automatiquement en "Vision" violet
     et le bandeau disparaît.
- **Non-régression** : les 107 tests existants du composant restent verts.

## Hors périmètre
- **Différenciation FAILED** : volontairement non affichée (fail-open).
- **Détail par pièce dans la liste** : reste dans le `DocumentPreviewDialog`
  (SF-148-03 existant).
- **Backend** : aucune modification — les données `visionStatus` sont déjà
  exposées via `DocumentPieceSummary` (F-148).

## Composants impactés
- `case-file-detail.component.ts` :
  - `documentsContentEqual` + nouveau `piecesEqual`
  - `visionPendingCount`, `visionDoneCount`, `documentVisionState`
  - `managePolling` : condition étendue (visionPending)
  - `loadDocuments` : démarre le polling si vision en cours à l'ouverture
- `case-file-detail.component.html` :
  - Bandeau `.vision-banner`
  - Badge `.badge-vision--pending` / `.badge-vision--done`
- `case-file-detail.component.scss` : styles des 2 composants UI

## Impact par domaine métier
Transversale — l'enrichissement vision s'applique aux 3 domaines (droit du
travail, immigration, famille) et aux 2 pays. Les badges / bandeau n'ont
aucune adaptation par domaine.

## Analyse de cohérence transversale
- **Préoccupation polling** : pattern existant dans `managePolling` — on
  ajoute une condition, pas de nouveau mécanisme.
- **Anti-flicker** : `documentsContentEqual` existait déjà pour éviter le
  re-render de la table à chaque tick. On étend la comparaison plutôt que
  de la supprimer — conservation du bénéfice.
- **Cohérence UI avec le dialog** (SF-148-03) : palette violette identique,
  icône `visibility` identique, libellé "Vision" identique.
- **Smoke tests** : touche le polling (préoccupation transversale
  "Navigation / routing") → les smoke tests `navigation.spec.ts` et
  l'ouverture normale du dossier doivent rester verts.

## Nouveau pattern UI ou service partagé
- **`.badge-vision`** : pattern de badge sur ligne document. Proche des
  `.badge-ocr-running` / `.chip-ocr` existants. Non extrait en composant
  partagé pour l'instant (3 badges similaires dans le même template ne
  justifient pas d'abstraction). À revoir si un 4e badge apparaît.
- **`.vision-banner`** : bandeau de statut au-dessus d'une liste — seul
  exemple pour le moment, pas de pattern à extraire.
