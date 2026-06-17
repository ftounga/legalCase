# SF-289-08 — Indicateur « dossier à ré-synthétiser » étendu (réponses IA + nouveaux documents)

> Cadrages : étape 0 (`SF-289-08-00-coherence.md`, GO avec ajustements) + étape 0 bis (`SF-289-08-00b-ux-coherence.md`, GO avec ajustements). Extension de F-289 (Terminée).

## Objectif (une phrase)
Sur la Vue d'ensemble, signaler que le dossier a évolué **depuis la dernière synthèse enrichie** — pas seulement via les pièces en attente d'analyse, mais aussi quand des **réponses IA** ont été données ou des **documents ajoutés** depuis — pour inciter l'avocat à relancer une synthèse.

## Comportement nominal
Dans `OverviewService.overview(...)`, le booléen de péremption qui pilote l'item d'attention `ANALYSE_OBSOLETE` devient :
```
analysisStale = (pendingPiecesCount > 0)
             OU (réponse IA créée APRÈS la dernière synthèse enrichie DONE)
             OU (document créé APRÈS la dernière synthèse enrichie DONE)
```
- Dernière synthèse enrichie : `caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisTypeAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, ENRICHED, DONE)` → `updatedAt` (`Optional<Instant>`).
- Réponses IA depuis : `aiQuestionAnswerRepository.existsByAiQuestion_CaseFile_IdAndCreatedAtAfter(caseFileId, lastEnrichedAt)`.
- Documents depuis : `documentRepository.findByCaseFile_IdAndCreatedAtAfterOrderByCreatedAtDesc(caseFileId, lastEnrichedAt)` non vide.
- **Libellé cause-aware** de l'item `ANALYSE_OBSOLETE` (urgence INFO, action `RELAUNCH_ANALYSIS` inchangée) :
  - `pendingPiecesCount > 0` → « N pièce(s) non analysée(s) » (inchangé, dominant).
  - sinon réponses + documents → « Réponses et documents ajoutés depuis la dernière synthèse ».
  - sinon réponses seules → « Réponses ajoutées depuis la dernière synthèse ».
  - sinon documents seuls → « Documents ajoutés depuis la dernière synthèse ».

## Cas d'erreur / fail-open
- **Aucune synthèse enrichie DONE** (`Optional` vide) → `newAnswers = newDocs = false` ; seul `pendingPiecesCount` joue (comportement actuel). Le premier run n'est jamais signalé « périmé ».
- Toute requête repo qui lève → `safe(...)` fail-open → contribution ignorée (jamais de faux « périmé » bloquant), l'agrégat reste servi (F-289 strict).

## Critères d'acceptation vérifiables
- **CA1** : réponse IA postérieure à la dernière synthèse enrichie, 0 pièce en attente → `analysisStale = true`, item `ANALYSE_OBSOLETE` libellé « Réponses ajoutées… ».
- **CA2** : document postérieur à la dernière synthèse enrichie, 0 pièce en attente → `analysisStale = true`, libellé « Documents ajoutés… ».
- **CA3** : réponse **antérieure** à la dernière synthèse enrichie (déjà matérialisée), 0 pièce, 0 doc récent → `analysisStale = false`, **pas** d'item.
- **CA4** : `pendingPiecesCount > 0` → libellé « N pièce(s) non analysée(s) » (priorité, inchangé) même si réponses/docs récents.
- **CA5** : aucune synthèse enrichie DONE → réponses/docs ne déclenchent PAS la péremption (seul pending joue).
- **CA6** : une source repo qui jette → fail-open, `overview` répond quand même, péremption non bloquée par l'erreur.
- **CA7** : action de l'item = `RELAUNCH_ANALYSIS` route `/re-analyze` (inchangée).

## Plan de test minimal
- **Unitaires `OverviewServiceTest`** : CA1–CA6 (mock `caseAnalysisRepository`, `aiQuestionAnswerRepository`, `documentRepository`). Vérifier `pilotage.analysisStale` + présence/libellé de l'item `ANALYSE_OBSOLETE`.
- **Isolation workspace** : inchangée (résolution dossier en tête, déjà couverte).

## Tables / endpoints / composants impactés
- **`OverviewService`** : injecter `CaseAnalysisRepository` + `AiQuestionAnswerRepository` (DocumentRepository déjà injecté) ; calcul `analysisStale` étendu + libellé cause-aware dans `buildAttention`.
- **DTO `OverviewResponse.Pilotage`** : inchangé (`analysisStale` reflète le signal combiné, `pendingPiecesCount` inchangé).
- **Aucune migration, aucun nouveau repo, aucun changement frontend** (l'item d'attention est rendu génériquement par son `label` + action).

## Hors périmètre
- Bandeau visuel dédié « à jour / périmé » (cadrage : pas de bloc dédié ; l'item d'attention suffit en V1).
- Péremption sur changements de statut pièces/pistes/risques (F-176/F-192/F-193/F-195) — déférée (les pièces en attente + réponses + documents couvrent l'essentiel du besoin exprimé ; extension possible si signal terrain).
- Notification push / email.
