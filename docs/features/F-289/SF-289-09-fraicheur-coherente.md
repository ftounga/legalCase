# SF-289-09 — Cohérence de fraîcheur du dossier vivant (overlay statut live + compteur après enrichie)

> Bugfix (étapes 0/0bis exemptées). Issu du test 2026-06-22. Deux incohérences d'affichage liées au modèle « matérialisation au run enrichi ».

## Objectif (une phrase)
Faire que (A) une pièce marquée « obtenue / non applicable » **disparaisse immédiatement** du bloc d'attention de la Vue d'ensemble (comme une question répondue), et (B) le compteur « N pièces non analysées / N nouvelles pièces » **se vide après une synthèse enrichie** — sans casser la cohérence F-176 (PUT pur).

## Problème
- **A** : la Vue d'ensemble lit les pièces depuis l'**alignement matérialisé** (`pieces_alignment_json`, figé au dernier run enrichi), alors que les questions IA sont lues **en direct**. Le PUT statut « obtenue » est pur (F-176, aucun recompute) → l'item persiste jusqu'au prochain run enrichi.
- **B** : `PiecesWaveService` compare `document.createdAt` au seul timestamp du job `CASE_ANALYSIS`. La synthèse enrichie (`ENRICHED_ANALYSIS`) ne le met jamais à jour → le compteur ne se vide jamais après une enrichie (confusion : l'avocat relance dans le vide).

## Comportement nominal
- **A** : `OverviewService.buildAttention` superpose le **statut live** (`piece_manquante_status`) sur l'alignement figé. Une pièce dont le statut live est `OBTENUE` ou `NON_APPLICABLE` n'est **pas** affichée en `PIECE_MANQUANTE`, même si le snapshot dit encore `A_DEMANDER`. Index sur le libellé normalisé brut **et** canonique (PR3).
- **B** : `PiecesWaveService.lastSuccessfulAnalysisAt` retourne le **max(CASE_ANALYSIS, ENRICHED_ANALYSIS DONE)**.

## Cas d'erreur / fail-open
- Lecture du statut live en `safe(...)` fail-open (échec → pas d'overlay, comportement antérieur).
- Le **PUT reste pur** (F-176) : aucune re-matérialisation. L'overlay est purement à la lecture de l'overview.
- Aucune synthèse enrichie → `max` retombe sur CASE_ANALYSIS (comportement inchangé).

## Critères d'acceptation
- **CA1** : alignement figé `A_DEMANDER` + statut live `OBTENUE` → l'item PIECE_MANQUANTE **disparaît** ; les autres pièces A_DEMANDER restent.
- **CA2** : statut live `NON_APPLICABLE` → item masqué aussi.
- **CA3** : aucun statut live → comportement inchangé (l'alignement fait foi).
- **CA4** : CASE_ANALYSIS DONE T1 + ENRICHED_ANALYSIS DONE T2>T1 → référence = T2 ; pièces créées avant T2 ne sont plus « nouvelles » → compteur vidé.
- **CA5** : pas d'enrichie → référence = CASE_ANALYSIS (inchangé).

## Plan de test
- `OverviewServiceTest` : overlay OBTENUE/NON_APPLICABLE (CA1–CA3).
- `PiecesWaveServiceTest` (nouveau) : référence max (CA4), repli CASE_ANALYSIS (CA5), aucun job → none.
- Isolation workspace : inchangée.

## Tables / endpoints / composants
- **`OverviewService`** : injecte `PieceManquanteStatusRepository`, overlay live dans `buildAttention`.
- **`PiecesWaveService`** : référence = max(CASE_ANALYSIS, ENRICHED_ANALYSIS).
- **Aucune migration, aucun frontend, aucun changement de contrat** (les écrans rendent l'overview / la vague tels quels).

## Hors périmètre
- Faire que la synthèse enrichie ré-analyse **en profondeur** les nouveaux documents (→ correctif D séparé).
- Re-extraction des moyens adverses (→ correctif B / F-261).
