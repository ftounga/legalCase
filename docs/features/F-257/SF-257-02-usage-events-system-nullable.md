# Mini-spec — F-257 / SF-257-02 — usage_events compatible jobs SYSTEM_*

## Identifiant
`F-257 / SF-257-02`

## Date de création
2026-05-29

## Branche Git
`feat/SF-257-02-usage-events-system-nullable`

## Type
Bugfix — régression de production introduite par SF-257-01 (PR #1451, mergée 2026-05-29).

---

## Objectif (1 phrase)
Rendre la table `usage_events` compatible avec les appels Anthropic `SYSTEM_*` (sans dossier ni utilisateur), pour que le gate centralisé F-257 puisse enregistrer leur usage sans violer de contrainte.

---

## Contexte / cause racine
Depuis SF-257-01, `AnthropicService.recordUsage` appelle `UsageEventService.record(caseFileId, userId, jobType, …)` après **chaque** appel Anthropic. Pour les 11 `JobType.SYSTEM_*` (ex. `SYSTEM_JP_BOOTSTRAP`), `caseFileId` **et** `userId` sont `null`. Or `usage_events` (migration 016) impose :
- `case_file_id` `NOT NULL`
- `user_id` `NOT NULL`
- `event_type varchar(30)` — trop court pour `SYSTEM_JURISPRUDENCE_VERIFICATION` (33 car.)

→ l'`INSERT` jette une contrainte, l'exception remonte, et l'appel Anthropic entier est compté comme **échoué** (ex. `ClaudeJurisprudenceEvaluator` renvoie `NONE`). **Tous les jobs `SYSTEM_*` appelant Anthropic sont cassés** depuis le merge F-257 (bootstrap jurisprudence, blog, help chat, vision, détection de pièces, style, conclusions, veille, web_search BE, résumé chat).

Constaté en staging 2026-05-29 sur le bootstrap F-JU-01 batch-2 : 2 runs (165 + 46 entrées) → **0 mapping créé**, 100 % skip, logs `usage_events … violates not-null constraint`.

---

## Comportement attendu

### Nominal
- Un appel Anthropic `SYSTEM_*` enregistre une ligne `usage_events` avec `case_file_id = null`, `user_id = null`, `event_type = <SYSTEM_*>`, tokens + coût renseignés. Aucune exception. L'appel réussit et retourne son résultat.
- Un appel non-system (dossier/user présents) continue d'insérer normalement (inchangé).

### Cas d'erreur
- Aucun nouveau cas. Le comportement du gate (refus si budget mensuel workspace dépassé) reste strictement identique pour les jobs non-system ; les jobs system continuent de skipper le gate user (déjà le cas SF-257-01).

---

## Critères d'acceptation (vérifiables)
- [ ] `usage_events.case_file_id` et `usage_events.user_id` sont nullable en base (migration Liquibase).
- [ ] `usage_events.event_type` accepte ≥ 40 caractères (élargi de 30 → 40).
- [ ] L'entité `UsageEvent` autorise `caseFileId`/`userId` null et `eventType` length 40.
- [ ] `UsageEventService.record(null, null, SYSTEM_JP_BOOTSTRAP, …)` persiste sans exception.
- [ ] `UsageEventService.record(null, null, SYSTEM_JURISPRUDENCE_VERIFICATION, …)` persiste sans troncature (33 car.).
- [ ] Anti-régression : un `record` non-system (caseFileId/userId présents) persiste comme avant.
- [ ] Les budgets/limites par workspace restent inchangés : les lignes system (`case_file_id` null) sont **exclues** des agrégats (INNER JOIN `case_files`).

---

## Plan de test minimal
- **UT `UsageEventServiceTest`** : (1) `record(null, null, SYSTEM_JP_BOOTSTRAP, …)` → 1 ligne persistée, champs null acceptés ; (2) `record(null, null, SYSTEM_JURISPRUDENCE_VERIFICATION, …)` → `event_type` complet (pas de troncature) ; (3) anti-régression record non-system inchangé.
- **Isolation workspace** : N/A directement (table globale sans `workspace_id`), mais on vérifie que `sumTokensByWorkspaceIdSince` ignore les lignes `case_file_id` null (INNER JOIN — couvert par la logique existante, pas de requête modifiée).

---

## Tables / endpoints / composants impactés
- **Migration** : `447-relax-usage-events-for-system-jobs.xml` (dropNotNull case_file_id + user_id ; modifyDataType event_type varchar(40)).
- **Entité** : `UsageEvent.java` (nullable + length 40).
- **Aucun** changement de service, requête, endpoint ou frontend.

---

## Préoccupations transversales
- **Plans / limites (gate F-257)** : les lignes system `usage_events` (`case_file_id` null) sont exclues de tous les agrégats par workspace (`UsageEventRepository.aggregateByWorkspaceId` / `sumTokensByWorkspaceIdSince` / `…AllTime` utilisent `JOIN case_files cf ON cf.id = u.case_file_id`). Donc le coût des jobs system **n'est pas imputé** au quota d'un client — comportement voulu. Vérifié, aucune requête à modifier.
- Auth / Workspace / Navigation : non concernés.

---

## Hors périmètre
- Refonte du suivi de coût des jobs system dans une table dédiée (option C écartée 2026-05-29 au profit de l'option « colonnes nullable », plus rapide et alignée avec l'intention F-257 de tout tracer). Réévaluable si le besoin d'analytics system émerge.
- Le bootstrap F-JU-01 lui-même (sera relancé une fois ce fix déployé).
- Affinage des mots-clés bootstrap (déjà fait : `bootstrap-batch-2-refined.csv`).
