# Mini-spec — F-147 Robustesse pipeline IA face aux échecs Anthropic

## Identifiant · `F-147`
## Date · `2026-04-22` · Branche · `feat/F-147-pipeline-robustness`

## Contexte / incident déclencheur
Staging 2026-04-22 : test F-145 sur dossier E32. Upload → OCR OK → extraction DONE → pipeline IA démarre → **Anthropic 400 "Your credit balance is too low"** → `DocumentAnalysis` marqué FAILED **mais `analysis_jobs.DOCUMENT_ANALYSIS` reste PROCESSING à vie**. Conséquences :
- Barre de progression frontend bloquée
- Suppression du case file refusée (`409 CONFLICT` car `isPipelineActive` = true)
- Utilisateur impuissant, aucun recours UI

Bug systémique de design : `DocumentAnalysisService.finalizeAnalysis` et `ChunkAnalysisService` ne mettent à jour `analysis_jobs` **que** dans le chemin DONE. Tout incident IA (400, 429, 5xx, timeout réseau) → job zombie permanent.

## Objectif
3 SF complémentaires :
1. **SF-147-01** — Fix systémique : `analysis_jobs` marqué FAILED dès qu'une analyse individuelle échoue. Plus aucun zombie n'est créé.
2. **SF-147-02** — Endpoint super-admin `POST /super-admin/case-files/{id}/reset-pipeline` : débloque les cases files déjà coincés (créés avant ou malgré SF-147-01).
3. **SF-147-03** — Scheduled task `ZombieJobResetScheduler` : filet de sécurité automatique — passe FAILED les jobs PROCESSING/PENDING dont `updated_at` > 30 min, toutes les 5 min.

## Comportement après livraison
- Anthropic 400 → `DocumentAnalysis.status = FAILED` **+** `analysis_jobs.DOCUMENT_ANALYSIS.status = FAILED` (idem ChunkAnalysis)
- Le user peut immédiatement supprimer / relancer son case file
- Si un zombie apparaît quand même (crash pod, race condition), la scheduled task le nettoie sous 5-30 min
- En dernier recours, super-admin peut forcer un reset manuel via l'endpoint

## Critères d'acceptation
- [x] `DocumentAnalysisService.finalizeAnalysis` : appelle `markDocumentAnalysisJobFailed` quand `failure != null`
- [x] `ChunkAnalysisService.analyze` : appelle `markChunkJobFailed` quand l'analyse échoue
- [x] `AnalysisJobRepository.forceFailActiveJobsForCaseFile` : query JPQL `UPDATE` ciblée (case file × statuts actifs)
- [x] `AnalysisJobRepository.forceFailZombieJobs` : query JPQL `UPDATE` ciblée (statuts actifs × updated_at < seuil)
- [x] `SuperAdminService.resetCaseFilePipeline` + endpoint `POST /super-admin/case-files/{id}/reset-pipeline`
- [x] `ZombieJobResetScheduler` avec `@Scheduled(fixedDelay = 5 min)`, seuil staleness 30 min
- [x] Tests U-02 `DocumentAnalysisServiceTest` vérifie `job.status == FAILED + errorMessage` après exception Anthropic
- [x] Tests U-01 `ZombieJobResetSchedulerTest` : appel repo avec staleBefore ≈ now-30min
- [x] Full suite 1004 backend verts

## Tables / endpoints / composants impactés
- `DocumentAnalysisService.java` (+helper `markDocumentAnalysisJobFailed`)
- `ChunkAnalysisService.java` (+helper `markChunkJobFailed`)
- `AnalysisJobRepository.java` (+2 queries modifiantes)
- `SuperAdminService.java` (+méthode `resetCaseFilePipeline`)
- `SuperAdminController.java` (+endpoint + inner record `ResetPipelineResponse`)
- `ZombieJobResetScheduler.java` (nouveau)
- `DocumentAnalysisServiceTest.java` (U-02 enrichi)
- `ZombieJobResetSchedulerTest.java` (nouveau, 2 tests)

### Pas impacté
- `CaseAnalysisService.finalizeCaseAnalysis` — gère déjà FAILED correctement (ligne 269)
- `AiQuestionService.finalizeQuestionGeneration` — gère déjà FAILED correctement
- Frontend — aucun changement côté UI pour SF-147-01 (les jobs FAILED étaient déjà rendus correctement, c'est juste que l'état n'y arrivait jamais avant)

## Analyse de cohérence transversale
| Cible | Évaluation | Classement |
|-------|-----------|------------|
| F-121 SF-121-04 "any failed" (extractions) | Même philosophie — propagation FAILED au niveau job, UI déjà prête | Intégré |
| Autres services avec pattern `if DONE` conditionnel | Auditées : CaseAnalysis + AiQuestion OK. Seuls DocumentAnalysis + ChunkAnalysis avaient le bug. | Intégré |
| Tests d'intégration existants | Les tests unitaires `consumeDocumentAnalysis_anthropicError_*` déjà en place, enrichis pour vérifier le job aussi | Intégré |

## Préoccupations transversales
- **Plans / limites** : aucun impact
- **Auth / Principal** : endpoint super-admin protégé par `assertSuperAdmin`
- **Workspace context** : le reset super-admin s'applique à n'importe quel case file (par design, usage opérateur) — pas d'isolation workspace sur les actions super-admin
- **Navigation / routing** : aucun impact

## Hors scope
- UI super-admin pour le bouton "reset pipeline" (pour l'instant curl uniquement — à ajouter plus tard si besoin fréquent)
- Rejeu automatique d'un job FAILED (ex: après rechargement crédits Anthropic, l'user doit supprimer + réuploader — pas de retry auto)
- Notification email / Sentry quand un zombie est reseté (event Sentry suffit via `reportJobFailureToSentry` existant dans CaseAnalysis — non étendu ici)
