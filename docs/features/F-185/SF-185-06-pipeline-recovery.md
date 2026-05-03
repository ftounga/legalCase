# SF-185-06 — Recovery automatique des analyses zombies au démarrage

## Objectif (1 phrase)
Au démarrage du backend, marquer toutes les `CaseAnalysis` et jobs IA bloqués en `PENDING/PROCESSING/PARTIAL` comme `FAILED` pour éviter qu'un crash / redéploiement / OOM laisse des analyses zombies en DB qui bloquent indéfiniment l'utilisateur (HTTP 409 "Une analyse est déjà en cours").

## Motivation
Constat 2026-05-03 sur staging — après 4-5 redéploiements dans la même journée pour Chen 7, deux `CaseAnalysis` v1 + v2 sont restées en `PROCESSING` après que les pods qui les exécutaient aient été tués par les rolling updates. L'utilisateur ne pouvait plus relancer l'analyse → message "Une analyse est déjà en cours" + obligation d'intervenir manuellement en DB pour débloquer.

Précédent : F-147 SF-147-01 a corrigé le même problème pour `DocumentAnalysis` au niveau worker (marquer FAILED quand l'analyse Anthropic échoue). Mais le hook **au démarrage** manque pour CaseAnalysis et pour les jobs en général.

## Comportement nominal après fix

1. Backend démarre (Spring Boot `ApplicationReadyEvent` émis)
2. Bean `PipelineRecoveryRunner` détecte l'événement
3. Update transactionnel :
   - `case_analyses SET analysis_status='FAILED' WHERE analysis_status IN ('PENDING','PROCESSING','PARTIAL')`
   - `analysis_jobs SET status='FAILED', error_message='Pipeline interrompu (redémarrage serveur)' WHERE status IN ('PENDING','PROCESSING','PARTIAL') AND job_type IN ('CASE_ANALYSIS','QUESTION_GENERATION','ENRICHED_ANALYSIS')`
   - **Hors scope** : `DOCUMENT_ANALYSIS` jobs (déjà géré par F-147 SF-147-01 au niveau worker — pas de double recovery pour éviter les conflits avec une analyse réelle qui démarre juste après)
4. Log INFO : `Pipeline recovery on startup — N case_analyses + M jobs marked FAILED`
5. L'utilisateur peut immédiatement relancer une analyse au prochain login

## Cas d'erreur

- DB indisponible au démarrage : déjà bloquant pour Spring (Hibernate fail-fast). Le hook ne se déclenche pas — comportement attendu.
- Analyse RÉELLE en cours pendant le rolling update : peut être marquée FAILED faussement si le nouveau pod démarre AVANT que l'ancien finisse. Risque accepté — le pire cas est que l'utilisateur revoit "Analyse échouée" et relance, vs aujourd'hui où il est bloqué pour toujours. **Mitigation** : ne pas appliquer le recovery aux analyses créées dans les 30 dernières secondes (laisser une fenêtre de grâce pour que les pods en transit terminent).

## Critères d'acceptation

1. ✅ Bean `PipelineRecoveryRunner` (ou nom équivalent) créé, profil `prod` + `local` (pas `dev` H2 pour ne pas perturber les tests d'intégration)
2. ✅ Listener sur `ApplicationReadyEvent` (pas `ContextRefreshedEvent` qui se déclenche aussi à chaque rafraîchissement de contexte interne)
3. ✅ Mark FAILED pour `case_analyses` avec status IN (PENDING, PROCESSING, PARTIAL)
4. ✅ Mark FAILED pour `analysis_jobs` avec job_type IN (CASE_ANALYSIS, QUESTION_GENERATION, ENRICHED_ANALYSIS) et status IN (PENDING, PROCESSING, PARTIAL)
5. ✅ `error_message = "Pipeline interrompu (redémarrage serveur)"` sur les jobs
6. ✅ Fenêtre de grâce 30s : `created_at < NOW() - INTERVAL '30 seconds'` pour ne pas tuer les analyses qui viennent de démarrer
7. ✅ Log INFO unique listant le décompte : `Pipeline recovery on startup — N case_analyses + M jobs marked FAILED`
8. ✅ Idempotent : 2 démarrages successifs sans nouvelle analyse → 0 update au 2ᵉ
9. ✅ Tests UT : recovery happy path + idempotence + grace window respectée

## Plan de test

### Backend (UT)
- T-01 : `PipelineRecoveryRunnerTest` — démarrage avec 3 case_analyses (1 PENDING anciennne, 1 PROCESSING ancienne, 1 PROCESSING dans les 30 dernières secondes) → 2 marquées FAILED, 1 préservée
- T-02 : `PipelineRecoveryRunnerTest` — démarrage avec 2 jobs CASE_ANALYSIS PROCESSING anciens + 1 job DOCUMENT_ANALYSIS PROCESSING (hors scope F-147) → seuls les 2 CASE_ANALYSIS jobs sont marqués FAILED
- T-03 : idempotence — double appel `runRecovery()` → 2ᵉ appel met 0 row à jour

### Smoke staging post-merge
1. Trigger une analyse, regarder les logs `Pipeline recovery` au prochain redéploiement
2. Vérifier en DB qu'aucune CaseAnalysis n'est en PROCESSING > 30s après le démarrage du nouveau pod

## Tables / endpoints / composants impactés

- **Backend** :
  - Nouveau composant `PipelineRecoveryRunner.java` (~80 lignes)
  - Aucune migration DB — pas de schema change
- **Frontend** : aucun impact (le bug ne se voyait que via HTTP 409 transient)

## Hors périmètre

- Recovery des `DOCUMENT_ANALYSIS` jobs (déjà géré F-147 SF-147-01 au niveau worker)
- Mécanisme de heartbeat / lease sur les analyses pour distinguer "vraiment en cours" vs "zombie" (nécessiterait un thread de heartbeat — overkill pour V1)
- Recovery côté workers DocumentAnalysis interrompus en cours de pipeline chunk → document (gère F-147 SF-147-01 sur Anthropic failures, pas sur pod kill)
- Notification utilisateur "Votre analyse a été interrompue par une maintenance" (V2 si signal terrain)

## Analyse de cohérence transversale

- **Auth/Principal** : non concerné — tâche système au démarrage, pas d'utilisateur impliqué
- **Workspace context** : non concerné — opération transversale tous workspaces
- **Plans/limites** : non concerné
- **Navigation/routing** : non concerné
- **Outil décisionnel métier** : non concerné — infra pipeline IA
- **Préoccupation transversale "nouveau pattern partagé"** : un nouveau bean Spring qui touche directement aux tables d'analyse — pas de pattern réutilisable, c'est du recovery one-shot au boot

## Impact par domaine métier

Transversal — aucune adaptation par domaine (Travail / Immigration / Famille) ni par pays (FR / BE), c'est de l'infra du pipeline IA.

## Risques

- **False positive** : analyse réelle en cours au moment du démarrage du nouveau pod (pendant un rolling update où l'ancien pod n'a pas fini son consume RabbitMQ). Mitigation : grace window 30s. Dans le pire cas, l'utilisateur voit "FAILED" à tort et relance — pire qu'aujourd'hui ? Non : aujourd'hui il est bloqué tout court.
- **Conflit avec F-147** : DocumentAnalysis jobs explicitement exclus du scope (F-147 gère). Pas de double-marquage.
