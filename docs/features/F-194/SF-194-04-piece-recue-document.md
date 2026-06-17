# SF-194-04 — Pièce reçue liée à un document (+ robustesse ré-analyse)

> Extension de **F-194** (pièces manquantes markables). Issue du test 2026-06-17. Multi-PR : PR1 backend (lien document + matching stable + migration), PR2 frontend (sélecteur sur Synthèse + Vue d'ensemble).

## Étape 0 — Cadrage cohérence (verdict : GO)
**Workflow avocat** : l'IA liste les pièces manquantes → l'avocat les demande au client → quand il les reçoit, il les **uploade** et **marque la pièce « obtenue »**. **Bug constaté (vérifié en base)** : le statut OBTENUE est matché par **libellé exact** ; la **ré-analyse régénère un libellé légèrement différent** → la pièce **réapparaît « à demander »**. **Pré-requis amont OK** : les documents sont déjà uploadés et listés (`DocumentService`), le statut existe (F-194), le pattern « pièce source » d'un round (SF-282-03, `sourceDocumentId`) est un précédent direct. **Sortie exploitable** : une pièce reçue **liée à son fichier** = traçabilité fait→pièce + statut **stable**. **Anti-doublon** : extension de F-194, pas un nouvel outil. **Invariant anti-gadget** : l'effet doit être réel (la pièce reste obtenue après ré-analyse) — sinon gadget.

## Étape 0 bis — Cohérence écran (verdict : GO avec ajustements)
2 zones : (1) **Synthèse** (panneau pièces manquantes — surface principale du marquage) ; (2) **Vue d'ensemble** (bloc attention, « Marquer obtenue »). **Ajout** : au clic « obtenue », un **sélecteur de document** (liste des pièces uploadées du dossier), optionnel. **Invariant anti-surcharge** : le sélecteur ne s'affiche qu'à la demande (au clic), pas en permanence ; libellé du document lié affiché de façon discrète sur la pièce obtenue. Réutilise le pattern `mat-select` « Pièce source » de SF-282-03.

## SF (objectif)
Au marquage « obtenue » d'une pièce manquante, permettre de **lier le document reçu** (depuis Synthèse ET Vue d'ensemble), et rendre le statut **robuste à la ré-analyse** en matchant aussi sur le **`critere_code`** (stable) en plus du libellé.

## Comportement nominal (PR1 backend)
- `piece_manquante_status` gagne `obtained_document_id` (UUID null, FK documents) et `critere_code` (varchar null) — **migration 609**.
- `updateStatus` accepte `documentId` (optionnel) + capture `critereCode` (optionnel) ; persistés sur l'entité.
- **Alignement robuste** (`PieceManquanteAlignmentService`) : une pièce d'analyse est considérée OBTENUE/NON_APPLICABLE si un statut matche par **`critere_code` égal** (quand les deux en ont un) **OU** par libellé normalisé (fallback). → survit à la dérive de libellé pour les pièces codées.
- L'alignement expose `obtainedDocumentId` (+ nom du document si résoluble) pour l'affichage.

## Comportement nominal (PR2 frontend)
- Synthèse + Vue d'ensemble : au clic « Marquer obtenue », `mat-select` optionnel « Document reçu » (liste `DocumentService.list`) → PUT avec `documentId`.
- Pièce obtenue : affiche le **nom du document lié** (cliquable, ouvre l'aperçu).

## Cas d'erreur / limites
1. **Aucun document choisi** → marquage obtenue sans lien (rétro-compatible, comportement actuel).
2. **Pièce sans `critere_code`** (free-form) → fallback libellé (drift résiduel possible — accepté V1, documenté).
3. **Document supprimé** → lien orphelin toléré (affichage dégradé, pas d'erreur).

## Critères d'acceptation (PR1)
- [ ] Migration 609 ajoute les 2 colonnes (nullable), réversible.
- [ ] `updateStatus(..., documentId, critereCode)` persiste les 2 champs.
- [ ] Une pièce marquée OBTENUE **avec `critere_code`** reste OBTENUE après une ré-analyse qui change son libellé (match par code).
- [ ] L'alignement renvoie `obtainedDocumentId`.
- [ ] Isolation workspace préservée (résolution inchangée).

## Plan de test (PR1)
- `PieceManquanteStatusServiceTest` : persistance documentId + critereCode.
- `PieceManquanteAlignmentServiceTest` : match par critere_code malgré libellé différent ; fallback libellé ; exposition obtainedDocumentId.
- Migration testée sur base propre (IT pipeline existant).

## Composants impactés
- **PR1** : migration 609, `PieceManquanteStatus` (entité), `PieceManquanteStatusService`, `PieceManquanteStatusController`/DTO, `PieceManquanteAlignmentService` + record `PieceManquanteAlignment`.
- **PR2** : `synthesis.component`, `case-overview.component`, modèles/service front.

## Hors périmètre
- Dédup des pièces obtenues à libellés multiples déjà en base (purge ponctuelle).
- Drift des pièces **sans** code (fallback libellé — V1).

## Analyse transversale
- **Workspace** : statut et documents déjà gatés par workspace — inchangé. **Pré-fill IA** : N/A. **Navigation** : aucune route nouvelle. **Outil décisionnel** : N/A (pièces, pas outil).
- **Migration** : 609, additive, réversible. **Smoke E2E** : aucun (pas d'impact auth/nav).
