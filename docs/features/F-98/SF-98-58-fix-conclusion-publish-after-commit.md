# Mini-spec — F-98 / SF-98-58 (bugfix) — Publier le message de génération après commit

> Bugfix — exempté étapes 0 / 0 bis (aucun élément visible nouveau, aucun workflow nouveau).

## Identifiant
`F-98 / SF-98-58`

## Statut
`ready`

## Branche
`fix/SF-98-58-conclusion-publish-after-commit`

## Problème (constaté en staging 2026-06-10)
`CaseConclusionCommandService.triggerGeneration` est `@Transactional` : la ligne `CaseConclusion` PENDING est créée (`save`, l.134) puis le message RabbitMQ est **publié l.137 AVANT le commit** (le commit a lieu au retour de la méthode). Avec `@RabbitListener(concurrency="2")` × 2 replicas, le worker peut consommer et faire `findById(conclusionId)` **avant** que la ligne soit visible → `prepare()` logge « Conclusion {} introuvable — message ignoré » et **ack le message sans retry** → la génération reste **PENDING indéfiniment** ; tout re-déclenchement renvoie `409 ALREADY_GENERATING`. Race **publish-before-commit** classique, latente (dépend du timing de consommation).

## Comportement attendu
Le message `CaseConclusionMessage` n'est publié qu'**après le commit** de la transaction qui a créé la ligne PENDING. Le worker trouve donc toujours la ligne. En l'absence de transaction active (appel direct en test), publication synchrone immédiate (comportement inchangé pour les tests).

## Correctif
Dans `triggerGeneration`, remplacer l'appel direct `rabbitTemplate.convertAndSend(...)` par une publication enregistrée via `TransactionSynchronizationManager` :
- si `TransactionSynchronizationManager.isSynchronizationActive()` → `registerSynchronization(new TransactionSynchronization(){ afterCommit(){ publish } })` ;
- sinon → publier immédiatement (repli test / non-transactionnel).

## Critères d'acceptation
- [ ] Le message est publié **après** le commit (plus de fenêtre « introuvable »).
- [ ] Génération bout-en-bout : PENDING → PROCESSING → DONE (plus de blocage PENDING).
- [ ] Aucun changement de contrat API (toujours `202 {status:PENDING, versionNumber:N}`).
- [ ] Tests existants verts ; ajout d'un test couvrant le repli « pas de transaction active → publication immédiate ».

## Hors scope
- Les autres `convertAndSend` (analysis, style learning, chunking) — **même pattern potentiel à auditer** dans un lot séparé (non confirmés défaillants ; l'analyse fonctionne en l'état). Signalé, non corrigé ici.

## Plan de test
- [ ] UT : appel hors transaction → `rabbitTemplate.convertAndSend` invoqué (repli immédiat). Tests existants de `triggerGeneration` restent verts.
- [ ] Validation staging : régénérer des conclusions → DONE (+ observer le bordereau SF-98-57).

## Préoccupations transversales
- [x] Aucune (pas d'auth/workspace/plan/navigation ; pas de changement de schéma ni d'API).

## Notes
- Cause non liée à SF-98-57 (le diff bordereau n'a pas touché ce chemin) — bug pré-existant exposé par le timing post-déploiement.
