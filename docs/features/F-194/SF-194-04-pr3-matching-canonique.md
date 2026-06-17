# SF-194-04 PR3 — Matching canonique du statut pièce (robustesse au drift de libellé)

> Dernière PR de SF-194-04. Cadrage (étape 0/0bis) et mini-spec parente déjà produits en PR1 (`SF-194-04-piece-recue-document.md`). Ce document couvre le **delta robustesse + migration** validé par le PO (option « Robuste + migration », 2026-06-17).

## Objectif (une phrase)
Faire survivre le statut avocat d'une pièce (notamment `OBTENUE`) à la dérive du libellé produit par le LLM entre deux runs de Synthèse enrichie, en ancrant le matching sur le **libellé canonique du socle F-294** plutôt que sur le seul libellé brut normalisé.

## Problème (constaté au test)
- Le statut est stocké avec la clé `(case_file_id, piece_libelle_normalise)` = libellé **brut LLM** normalisé (`trim().toLowerCase()`).
- À la ré-analyse, le LLM produit une variante du libellé → la clé change → le statut n'est plus retrouvé → la pièce « obtenue » réapparaît comme « à demander ».
- **Incohérence latente découverte** : marquer depuis la **Synthèse** (clé = libellé brut) vs depuis la **Vue d'ensemble** (clé = libellé déjà canonisé par F-294) peut créer **deux lignes de statut distinctes** pour la même pièce ; la matérialisation (qui canonise) n'en voit qu'une.

## Comportement nominal
1. **À l'écriture** (`PUT .../pieces-manquantes/status`, les 2 écrans) : le backend calcule le **libellé canonique** du socle (`getExpectedPieces(legalDomain, country, procedureStage)`) par correspondance **normalisée exacte** (jamais de fuzzy → zéro risque de fusion de pièces distinctes). S'il existe, il est stocké dans la nouvelle colonne `piece_libelle_canonique` ; sinon `null` (pièce hors socle → comportement F-194 strict inchangé).
2. **À la matérialisation** (`materializeForAnalysis`) : l'index des statuts est construit sur le libellé brut normalisé **ET** sur le libellé canonique normalisé. Chaque pièce IA (déjà canonisée par F-294) retrouve donc son statut via la clé canonique, stable d'un run à l'autre.
3. La clé d'unicité `(case_file_id, piece_libelle_normalise)` reste **inchangée** (rétro-compatibilité, pas de migration de données).

## Cas d'erreur / fail-open
- Référentiel absent / dossier incomplet / exception de résolution → `piece_libelle_canonique = null`, aucun changement de comportement (comme `buildCanonicalByNorm`, CA7 F-294).
- Pièce hors socle → pas de canonique → drift résiduel possible (limite documentée ; le prompt socle F-294 pousse le LLM à réutiliser les libellés canoniques).
- Anciennes lignes (avant migration) : `piece_libelle_canonique = null` → matching brut, comme aujourd'hui (pas de régression). Le canonique se renseigne au prochain marquage.

## Critères d'acceptation vérifiables
- **CA1** : marquer `OBTENUE` une pièce du socle → la colonne `piece_libelle_canonique` est renseignée au libellé canonique.
- **CA2** : ré-analyse produisant un libellé brut **dérivé mais canonisable** (même entrée socle) → le statut `OBTENUE` est conservé dans l'alignement (pièce non réapparue en `A_DEMANDER`).
- **CA3** : marquer la même pièce depuis Synthèse (brut) puis Vue d'ensemble (canonique) → une seule ligne de statut effective côté alignement (pas de doublon).
- **CA4** : pièce hors socle → `piece_libelle_canonique = null`, comportement strictement identique à avant.
- **CA5** : aucune pièce obtenue/non-applicable n'est **perdue ni dupliquée** dans l'alignement (statut consommé via clé canonique = non re-ajouté par la boucle de repli).
- **CA6** : `upsertStatus` reste un PUT pur (aucune mutation de `analysis_result`, cohérence F-92 — test régression conservé).

## Plan de test minimal
- **Unitaires `PieceManquanteStatusServiceTest`** : canonique renseigné quand socle match (CA1) ; null hors socle (CA4) ; fail-open si référentiel null.
- **Unitaires `PieceManquanteAlignmentServiceTest`** : matching par clé canonique (CA2) ; pas de doublon (CA5) ; cohérence Synthèse/Vue d'ensemble (CA3).
- **Isolation workspace** : couverte par les tests d'isolation existants de `upsertStatus` (inchangés).

## Tables / endpoints / composants impactés
- **Migration 610** : `ALTER TABLE piece_manquante_status ADD piece_libelle_canonique VARCHAR(500) NULL` (+ index de lookup), rollback `dropColumn`.
- **Entité** `PieceManquanteStatus` : champ `pieceLibelleCanonique`.
- **`PieceManquanteStatusService`** : injection `LegalReferentialService` (nullable, miroir alignment), calcul canonique à l'écriture.
- **`PieceManquanteAlignmentService.materializeForAnalysis`** : index + lookup canonique, suivi des statuts consommés.
- **Aucun changement de contrat API, aucun changement frontend** (le `documentId` de PR2/PR2b suffit ; le canonique est purement backend).

## Hors périmètre
- Matching **fuzzy** / par tokens (risque de fusionner des pièces distinctes — explicitement écarté par le choix PO).
- Backfill des `piece_libelle_canonique` des lignes existantes (se renseigne au prochain marquage ; pas de migration de données).
- Réduction du bruit des doublons résiduels hors socle (suivi séparé).
