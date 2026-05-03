# SF-185-02 — Q&A async post-synthèse : SSE event QUESTION_GENERATION

## Objectif (1 phrase)
Émettre un événement SSE `QUESTION_GENERATION_DONE` / `QUESTION_GENERATION_FAILED` à la fin de la génération asynchrone des questions IA pour que le frontend bascule du skeleton vers le panneau questions sans polling.

## Constat

Le pipeline Q&A est **déjà asynchrone** depuis la livraison initiale : `CaseAnalysisService.finalizeCaseAnalysis` publie `AiQuestionGenerationMessage` sur RabbitMQ, `AiQuestionService.consumeQuestionGeneration` (`@RabbitListener`) consomme et persiste les questions, met à jour `AnalysisJob.QUESTION_GENERATION` à DONE.

**Ce qui manque** : aucun événement SSE n'est publié à la fin → le frontend dépend du polling régulier des `analysisJobs` pour détecter la fin (latence visible 5-15 s entre la fin réelle et l'affichage du panneau questions). Sur la page détail dossier, le spinner "Génération des questions complémentaires…" reste affiché alors que les questions sont déjà persistées.

## Comportement nominal

1. Le worker RabbitMQ génère les questions et appelle `finalizeQuestionGeneration` qui persiste les rows `ai_questions`, met à jour `AnalysisJob.QUESTION_GENERATION` à DONE.
2. **NOUVEAU** : après commit, `eventPublisher.publishEvent(new AnalysisStatusEvent(caseFileId, AnalysisStatus.DONE, JobType.QUESTION_GENERATION))`.
3. `SseNotificationService` émet l'événement `QUESTION_GENERATION_DONE` sur les SSE des emitters abonnés au dossier.
4. Frontend `AnalysisSseService` reçoit l'événement, le propage dans `events$`.
5. `SynthesisComponent` (page synthèse) refetch `loadQuestionsForVersion()` → panneau questions s'affiche immédiatement.
6. `CaseFileDetailComponent` (page détail dossier) refetch les analysisJobs → le spinner disparaît, la bannière "N question(s) en attente" apparaît.

## Cas d'erreur

- Question generation FAILED → événement `QUESTION_GENERATION_FAILED` émis. Frontend masque le spinner (la pile actuelle a déjà un fallback timeout).
- SSE non disponible (Pod restart, déconnexion réseau) → fallback inchangé : le polling régulier de `analysisJobs` détecte le DONE après quelques secondes.

## Critères d'acceptation

1. ✅ `finalizeQuestionGeneration` publie un `AnalysisStatusEvent` après commit (DONE ou FAILED).
2. ✅ `AnalysisSseService` (frontend) écoute `QUESTION_GENERATION_DONE` et `QUESTION_GENERATION_FAILED` en plus des événements existants.
3. ✅ `SynthesisComponent` rafraîchit ses questions quand l'événement `QUESTION_GENERATION_DONE` arrive sur le caseFileId courant.
4. ✅ `CaseFileDetailComponent` rafraîchit ses analysisJobs (le spinner disparaît immédiatement).
5. ✅ Aucune régression sur le flow synchrone existant (les rows `ai_questions` sont toujours persistées avant l'événement).
6. ✅ Backend et frontend tests passent.

## Plan de test

### Backend (UT)
- T-01 : `finalizeQuestionGeneration_done_publishesAnalysisStatusEvent` — vérifie que `eventPublisher.publishEvent` est appelé avec `(caseFileId, DONE, QUESTION_GENERATION)` après afterCommit synchronization.
- T-02 : `finalizeQuestionGeneration_failed_publishesFailedEvent` — vérifie que sur exception, l'événement `(caseFileId, FAILED, QUESTION_GENERATION)` est publié.

### Frontend (Jest)
- T-03 : `AnalysisSseService` écoute `QUESTION_GENERATION_DONE` (étendre le test existant).

### Smoke
- Lancer une analyse end-to-end sur staging → vérifier que le spinner "Génération des questions" disparaît dès la fin de la génération (pas après le polling régulier).

## Tables / endpoints / composants impactés

- **Backend** : `AiQuestionService.java` (1 fichier — inject `ApplicationEventPublisher`, publish dans `finalizeQuestionGeneration`).
- **Frontend** : `analysis-sse.service.ts` (1 fichier — ajout de 2 event names), `synthesis.component.ts` (1 fichier — handle `QUESTION_GENERATION` dans SSE subscription).
- **Pas de migration DB**, pas de nouvel endpoint, pas de changement de contrat.

## Hors périmètre

- Skeleton spécifique pour le panneau questions pendant la génération (l'UX existante "spinner + texte" suffit).
- Refonte du flow Q&A (déjà async).
- Q&A streaming (pas demandé, viendra dans un éventuel SF-185-04).

## Analyse de cohérence transversale

- **CASE_ANALYSIS_PARTIAL** vient d'être livré par SF-185-01 — pattern strictement réutilisé (publishEvent dans afterCommit synchronization).
- Pas de divergence introduite : le pattern d'émission SSE est uniforme avec `CASE_ANALYSIS_DONE`, `ENRICHED_ANALYSIS_DONE`, `DOCUMENT_ANALYSIS_DONE` déjà existants.

## Impact par domaine métier

Transversal — aucune adaptation par domaine (Travail / Immigration / Famille) ni par pays (FR / BE), c'est de l'infra UX du pipeline IA.
