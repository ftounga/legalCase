# Mini-spec — F-98 / SF-98-59 (bugfix) — StyleCorpus : publier le message après commit

> Bugfix — exempté étapes 0 / 0 bis. Jumeau de SF-98-58 (même race, autre site).

## Problème (audit publish-before-commit 2026-06-10)
`StyleCorpusCommandService.upload` est `@Transactional` : crée un `StyleCorpusDocument` PENDING (`save`), puis publie `StyleCorpusMessage(documentId, storageKey)` **avant le commit**. Le worker `StyleCorpusExtractionService.consumeStyleCorpus` (`@RabbitListener concurrency=2`) fait `findById(documentId)` dans `markProcessing` → si la ligne n'est pas encore visible → « introuvable — message ignoré » (ack sans retry) → upload de corpus de style **bloqué PENDING**. Race **publish-before-commit** identique à SF-98-58 (conclusions). Seul autre site confirmé RACE par l'audit ; les 5 autres `convertAndSend` sont SAFE (id d'entité préexistante committée, ou déjà `afterCommit`).

## Correctif
Publier via `TransactionSynchronizationManager.afterCommit` quand une transaction est active ; repli synchrone immédiat sinon (tests / non transactionnel). L'upload S3 reste dans la transaction ; seul le `convertAndSend` est différé. Patron strictement identique à `CaseConclusionCommandService.publishAfterCommit`.

## Critères d'acceptation
- [ ] Le message n'est publié qu'après le commit de la ligne PENDING (plus de fenêtre « introuvable »).
- [ ] Upload de corpus → extraction va jusqu'à son terme (plus de blocage PENDING).
- [ ] Aucun changement de contrat API (`StyleCorpusUploadResponse.pending`).
- [ ] Tests existants verts (le repli hors-transaction conserve l'assertion `convertAndSend`).

## Hors scope
Les 5 autres `convertAndSend` (analysis/chunking/finalize) — audités SAFE, aucun fix requis.

## Plan de test
- [ ] UT : `StyleCorpusCommandServiceTest` (12) verts — l'appel hors transaction publie immédiatement (repli).
- [ ] Validation staging facultative : upload d'un document de corpus → statut passe PENDING → PROCESSING → DONE.

## Préoccupations transversales
- [x] Aucune (pas d'auth/workspace/plan/navigation ; pas de changement de schéma ni d'API).
