# Mini-spec — F-257 / SF-257-01 — Gate Anthropic centralisé (durcissement F-34)

## Identifiant

`F-257 / SF-257-01`

## Feature parente

`F-257` — Gate Anthropic centralisé (durcissement F-34)

## Statut

`draft`

## Date de création

2026-05-28

## Branche Git

`feat/SF-257-01-gate-centralise-anthropic`

---

## Objectif

Rendre **impossible par construction** un appel sortant vers l'API Anthropic qui ne traverse pas le gate `PlanLimitService.isMonthlyTokenBudgetExceeded` et l'enregistrement `UsageEventService.record`, en déplaçant ces deux opérations dans `AnthropicService` lui-même via un paramètre obligatoire `AiCallContext`.

---

## Comportement attendu

### Cas nominal — appel user-level (workspace identifié)

1. Le caller métier construit un `AiCallContext` avec `workspaceId` (non null), `userId` (non null), `caseFileId` (nullable selon le contexte), `jobType` (un des `JobType` user-facing : `CHUNK_ANALYSIS`, `DOCUMENT_ANALYSIS`, `CASE_ANALYSIS`, `QUESTION_GENERATION`, `ENRICHED_ANALYSIS`, `CHAT_MESSAGE`).
2. Le caller invoque une méthode publique de `AnthropicService` (`analyzeFast`, `analyze`, `analyzeWithModel`, `analyzeWithSystemCache`, `analyzeWithSystemCacheStreaming`, `analyzeWithImages`, `analyzeWithWebSearch`, `analyzeChunk`) en lui passant le contexte.
3. `AnthropicService` appelle `planLimitService.isMonthlyTokenBudgetExceeded(ctx.workspaceId())` **avant** l'appel HTTP Anthropic.
4. Si `false` → l'appel HTTP Anthropic est effectué, la réponse est récupérée (`AnthropicResult` avec `inputTokens` + `outputTokens` réels).
5. `AnthropicService` appelle `usageEventService.record(ctx.caseFileId(), ctx.userId(), ctx.jobType(), in, out)` **avant** de retourner le résultat au caller.
6. Le caller reçoit le `AnthropicResult` et poursuit son traitement.

### Cas nominal — appel system-level (cron, blog, super-admin, vision, conclusion…)

1. Le caller métier construit un `AiCallContext` avec `workspaceId = null`, `userId = null`, `caseFileId` nullable, `jobType` parmi les nouveaux `JobType.SYSTEM_*` (`SYSTEM_REFERENTIAL_CHECK`, `SYSTEM_BLOG_GENERATION`, `SYSTEM_JP_BOOTSTRAP`, `SYSTEM_HELP_CHAT`, `SYSTEM_VISION_ENRICHMENT`, `SYSTEM_PIECE_DETECTION`, `SYSTEM_STYLE_LEARNING`, `SYSTEM_CASE_CONCLUSION`, `SYSTEM_SEMANTIC_DIFF`, `SYSTEM_JURISPRUDENCE_VERIFICATION`, `SYSTEM_CHAT_SUMMARY`).
2. `AnthropicService` détecte un `JobType.SYSTEM_*` ou `workspaceId == null` → **skip** le gate user.
3. L'appel HTTP Anthropic est effectué.
4. `usageEventService.record(ctx.caseFileId(), null, ctx.jobType(), in, out)` est appelé systématiquement (userId null autorisé). Tous les coûts restent dans `usage_events` pour traçabilité globale.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP (si propagé) |
|-----------|---------------------|------------------------|
| `AiCallContext` null | `IllegalArgumentException` immédiate | 500 (bug appelant) |
| `JobType` null dans `AiCallContext` | `IllegalArgumentException` immédiate | 500 |
| `JobType` user-level (`CHUNK_ANALYSIS` etc.) ET `workspaceId == null` | `IllegalArgumentException` immédiate | 500 |
| `JobType` user-level (`CHUNK_ANALYSIS` etc.) ET `userId == null` | `IllegalArgumentException` immédiate | 500 |
| Gate user dépassé (`isMonthlyTokenBudgetExceeded == true`) sur appel user-level | `PaymentRequiredException(PaymentRequiredCode.TOKEN_BUDGET_EXCEEDED, "Budget tokens mensuel dépassé.")` levée **avant** l'appel HTTP Anthropic | 402 (mapping existant) |
| Erreur HTTP Anthropic (5xx) | comportement actuel conservé (retry exponentiel `{5, 15, 30, 60}` secondes) — **pas de record** si pas de réponse exploitable | 500 (propagé) |
| Erreur HTTP Anthropic non-retryable (4xx) | exception propagée — **pas de record** | 500 |

**Convention listeners async (RabbitMQ)** : les callers async (`ChunkAnalysisService`, `CaseAnalysisService`, `DocumentAnalysisService`, `EnrichedAnalysisService`, `AiQuestionService`, `CaseConclusionService`, `StyleCorpusExtractionService`) doivent catcher `PaymentRequiredException` localement et marquer l'item comme SKIPPED (cohérent avec le pattern actuel `ChunkAnalysisService:127`). Ces catchs sont ajoutés dans cette SF.

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : aucun appel direct à Anthropic depuis les outils décisionnels (F-DT-*, F-IM-*, F-FA-*, F-215, F-217, F-246) — vérifié dans l'audit 2026-05-28. **Non applicable**.
- [x] **Autres pays** : aucun impact pays-spécifique (gate technique). **Non applicable**.
- [x] **Autres domaines** : aucun impact domaine-spécifique. **Non applicable**.
- [x] **Autres UI patterns** : aucun (SF backend pure). **Non applicable**.
- [x] **Autres flows transversaux** : **applicable** — Plans / limites (gate centralisé), Workspace context (workspaceId propagé sur tout le chemin).

### Cas spécifique : nouveau service partagé

- [x] **Où le nouveau pattern AiCallContext pourrait-il être réutilisé ?** → uniquement par les callers d'`AnthropicService`. Tous identifiés (25 sites). Pas de "réutilisation orpheline" possible.
- [x] **Y a-t-il des patterns concurrents que ce nouveau pattern remplace ?** → oui : appels directs `analyze(...)` sans contexte. Tous remplacés en bloc dans cette SF.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `AnthropicService` (toutes signatures publiques) | Oui | Refonte dans cette SF |
| 4 sites gatés actuels (`CaseAnalysisCommandService`, `ReAnalysisCommandService`, `ChunkAnalysisService`, gate hérité) | Oui | Migration vers nouvelle signature, suppression du check pré-emptif sauf si batch (cas `ChunkAnalysisService` qui skip un chunk : pré-check conservé pour éviter `PaymentRequiredException` × N) |
| 12 sites bypass purs | Oui | Migration vers nouvelle signature avec `AiCallContext` user-level OU system-level selon le caller |
| 3 sites bypass partiels (record manquant) | Oui | Migration, gate hérité conservé, record automatique |
| 6 sites system-level (cron référentiels, blog, JP super-admin) | Oui | Migration vers `JobType.SYSTEM_*`, skip gate, record obligatoire |
| Tests existants `AnthropicServiceTest` | Oui | Adaptation des constructeurs et appels — dépendances `PlanLimitService` + `UsageEventService` à mocker |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (PR atomique).
- [ ] Subfeature(s) parallèle(s) — non applicable.
- [ ] Backlog — non applicable.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF backend pure, aucun composant Angular touché, aucun outil décisionnel modifié, pas d'endpoint nouveau exposé au frontend.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — justification : SF backend pure, aucun formulaire utilisateur, aucun champ saisissable, aucun outil décisionnel touché.

---

## Critères d'acceptation

### Architecture & API

- [ ] Record `AiCallContext(UUID workspaceId, UUID userId, UUID caseFileId, JobType jobType)` créé dans `fr.ailegalcase.analysis` avec validation à la construction : `jobType` non null ; si `jobType.isUserLevel()` alors `workspaceId` non null ET `userId` non null.
- [ ] Méthode `JobType.isUserLevel()` (et inversement `JobType.isSystemLevel()`) ajoutée — retourne `true` pour `CHUNK_ANALYSIS`, `DOCUMENT_ANALYSIS`, `CASE_ANALYSIS`, `QUESTION_GENERATION`, `ENRICHED_ANALYSIS`, `CHAT_MESSAGE`. `false` pour les nouveaux `SYSTEM_*`.
- [ ] Les 8 méthodes publiques de `AnthropicService` (`analyzeFast`, `analyze`, `analyzeWithModel`, `analyzeWithSystemCache`, `analyzeWithSystemCacheStreaming`, `analyzeWithImages`, `analyzeWithWebSearch`, `analyzeChunk`) acceptent un `AiCallContext` **en premier paramètre**, non null.
- [ ] La méthode privée `doAnalyze` intègre le gate (avant appel HTTP) et le record (après appel HTTP).
- [ ] Toutes les anciennes signatures publiques sans contexte sont **supprimées** (pas de surcharge dépréciée — fail-fast au compile time).
- [ ] Le constructeur package-private test-only est **conservé** (mais accepte aussi des mocks de `PlanLimitService` + `UsageEventService`).

### Comportement runtime

- [ ] Sur appel user-level avec gate non dépassé : appel HTTP Anthropic exécuté, puis `UsageEventService.record(...)` invoqué avec `tokensInput` + `tokensOutput` issus de la réponse réelle.
- [ ] Sur appel user-level avec gate dépassé : `PaymentRequiredException(TOKEN_BUDGET_EXCEEDED, ...)` levée **avant** l'appel HTTP, **aucun token consommé chez Anthropic, aucun record créé**.
- [ ] Sur appel system-level : appel HTTP exécuté inconditionnellement (skip gate), `UsageEventService.record(...)` invoqué avec `userId=null` et `jobType` system.
- [ ] Sur appel streaming (`analyzeWithSystemCacheStreaming`) : record effectué après agrégation finale du stream (`StreamAggregator.outputTokens`).
- [ ] Sur erreur HTTP non récupérée (après retry) : pas de record, exception propagée intacte.

### Migration callers

- [ ] Les 25 sites d'appel listés dans l'audit 2026-05-28 utilisent la nouvelle signature.
- [ ] Les 4 sites déjà gatés (`CaseAnalysisCommandService`, `ReAnalysisCommandService`, `ChunkAnalysisService` listener, `EnrichedAnalysisService` gate hérité) **conservent** leur check pré-emptif (utilisé pour batch-skip avant d'entrer dans une boucle d'appels) mais **suppriment** leur record manuel : le record devient automatique.
- [ ] Les listeners async qui peuvent maintenant recevoir une `PaymentRequiredException` au milieu d'un job catchent l'exception et marquent l'item SKIPPED (pattern existant `ChunkAnalysisService:127`).

### Sécurité

- [ ] Isolation workspace conservée : `usage_events.workspace_id` reste cohérent (vérifié via `caseFileId` → workspace ; pour les appels sans caseFileId, le record est créé sans rattachement workspace mais avec `userId` pour traçabilité user).
- [ ] Pas de TOCTOU exploitable : le gate vérifié dans `AnthropicService` est immédiatement suivi de l'appel HTTP (latence < 1 s entre les deux). Le risque résiduel (user qui burst plusieurs appels en parallèle dépassant légèrement le quota) est acceptable et préexistant à cette SF.

---

## Périmètre

### Hors scope (explicite)

- Refonte des tarifs (F-123 V7 déjà livré, ne pas y toucher).
- Facturation à l'usage / surfacturation au-delà du quota (V9+).
- Table de coûts globale system-level distincte (rester sur `usage_events` avec `userId=null`).
- Interception AOP / décorateur magique (rejeté — verbosité supérieure pour gain marginal, plus opaque à reviewer).
- Modification du calcul `PlanLimitService.isMonthlyTokenBudgetExceeded` lui-même (logique préservée).
- Modification de `UsageEventService.record` (signature préservée, on l'invoque depuis un nouveau site).
- Modification du frontend (la `PaymentRequiredException` se propage déjà en HTTP 402 — le frontend gère déjà ce cas).

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `usage_events.user_id` (system-level) | `null` | Cas systémique cron/blog/super-admin/vision/etc. |
| `usage_events.case_file_id` (system-level sans dossier) | `null` | Pour les crons et le blog SEO. |

---

## Contraintes de validation

| Champ `AiCallContext` | Obligatoire | Règle |
|-----------------------|-------------|-------|
| `jobType` | Oui | Non null. |
| `workspaceId` | Oui si `jobType.isUserLevel()` | Sinon nullable. |
| `userId` | Oui si `jobType.isUserLevel()` | Sinon nullable. |
| `caseFileId` | Non | Nullable selon contexte (jobs sans dossier : help, blog, JP). |

---

## Technique

### Endpoint(s)

Aucun nouvel endpoint exposé. La `PaymentRequiredException` se propage par le mapping global existant (HTTP 402).

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `usage_events` | INSERT | Nombre d'insertions augmente (≈3× volume actuel, vu que 17 sites passent en record). Pas de migration de schéma. |

### Migration Liquibase

- [x] Non applicable — aucun changement de schéma. `usage_events.user_id` est déjà nullable côté entity (vérifié dans `UsageEventService.record` qui accepte `UUID userId` non annoté `@NonNull`).

### Composants Angular

Aucun.

### Classes backend impactées (exhaustif)

**Créées** :
- `fr/ailegalcase/analysis/AiCallContext.java` (record)

**Modifiées** :
- `fr/ailegalcase/analysis/JobType.java` — ajout `SYSTEM_REFERENTIAL_CHECK`, `SYSTEM_BLOG_GENERATION`, `SYSTEM_JP_BOOTSTRAP`, `SYSTEM_HELP_CHAT`, `SYSTEM_VISION_ENRICHMENT`, `SYSTEM_PIECE_DETECTION`, `SYSTEM_STYLE_LEARNING`, `SYSTEM_CASE_CONCLUSION`, `SYSTEM_SEMANTIC_DIFF`, `SYSTEM_JURISPRUDENCE_VERIFICATION`, `SYSTEM_CHAT_SUMMARY` + méthodes `isUserLevel()` / `isSystemLevel()`.
- `fr/ailegalcase/analysis/AnthropicService.java` — refonte signatures publiques, injection `PlanLimitService` + `UsageEventService`, gate dans `doAnalyze`.
- Les 25 callers (file:line dans l'audit 2026-05-28) :
  1. `analysis/ChunkAnalysisService.java:158` — déjà gaté, supprime record manuel à 191/197
  2. `analysis/DocumentAnalysisService.java:102` — supprime record manuel à 204
  3. `analysis/CaseAnalysisService.java:261, 288` — supprime record manuel à 491
  4. `analysis/AiQuestionService.java:175` — supprime record manuel à 281
  5. `analysis/EnrichedAnalysisService.java:250, 274` — supprime record manuel à 619
  6. `analysis/EnrichedAnalysisService.java:653` — `summarizeChat` → `JobType.SYSTEM_CHAT_SUMMARY`
  7. `chat/ChatService.java:118, 119` — supprime record manuel à 130, `JobType.CHAT_MESSAGE`
  8. `analysis/JurisprudenceVerificationService.java:171` — `JobType.SYSTEM_JURISPRUDENCE_VERIFICATION`
  9. `analysis/SemanticDiffService.java:106, 127` — `JobType.SYSTEM_SEMANTIC_DIFF`
  10. `casefile/conclusion/CaseConclusionService.java:127` — `JobType.SYSTEM_CASE_CONCLUSION`
  11. `document/ExtractionService.java:380` — `JobType.SYSTEM_VISION_ENRICHMENT`
  12. `document/vision/VisionEnrichmentService.java:272` — `JobType.SYSTEM_VISION_ENRICHMENT`
  13. `document/DocumentPieceDetectionService.java:212` — `JobType.SYSTEM_PIECE_DETECTION`
  14. `stylelearning/StyleCorpusExtractionService.java:108` — `JobType.SYSTEM_STYLE_LEARNING`
  15. `help/HelpChatService.java:27` — `JobType.SYSTEM_HELP_CHAT`
  16. `blog/service/BlogArticleGeneratorService.java:169` — `JobType.SYSTEM_BLOG_GENERATION`
  17. `blog/service/LegalCitationVerifier.java:56` — `JobType.SYSTEM_BLOG_GENERATION`
  18. `referential/ReferentialCheckService.java:115` — `JobType.SYSTEM_REFERENTIAL_CHECK`
  19. `referential/ReferentialValidationService.java:53` — `JobType.SYSTEM_REFERENTIAL_CHECK`
  20. `jurisprudencemapping/ClaudeJurisprudenceEvaluator.java:129` — `JobType.SYSTEM_JP_BOOTSTRAP`
  21. `jurisprudencemapping/JurisprudenceBeWebSearchClient.java:116` — `JobType.SYSTEM_JP_BOOTSTRAP`

---

## Plan de test

### Tests unitaires `AnthropicServiceTest`

- [ ] `analyzeFast` user-level — gate non dépassé → appel exécuté, record invoqué avec `tokensInput`/`tokensOutput` issus du mock réponse.
- [ ] `analyze` user-level — gate dépassé → `PaymentRequiredException(TOKEN_BUDGET_EXCEEDED)` levée, **aucun appel** `restClient.post()` n'est effectué (vérification via Mockito `verifyNoInteractions`).
- [ ] `analyzeWithSystemCacheStreaming` user-level — gate non dépassé → stream consommé, record invoqué après agrégation.
- [ ] `analyzeWithImages` system-level — pas de check gate, record invoqué avec `userId=null`.
- [ ] `analyzeWithWebSearch` system-level — pas de check gate, record invoqué.
- [ ] `AiCallContext` user-level avec `workspaceId=null` → `IllegalArgumentException` au constructeur.
- [ ] `AiCallContext` user-level avec `userId=null` → `IllegalArgumentException` au constructeur.
- [ ] `AiCallContext` avec `jobType=null` → `IllegalArgumentException`.
- [ ] `JobType.isUserLevel()` / `isSystemLevel()` — vérif exhaustive sur tous les enum values.
- [ ] Erreur HTTP 5xx puis succès au retry → record invoqué une seule fois avec les tokens de la réponse réussie.
- [ ] Erreur HTTP 5xx persistante → exception propagée, **aucun record**.

### Tests d'intégration

- [ ] `HelpChatServiceIT` — appel `/api/v1/help/chat` → vérifier qu'un row `usage_events` est créé avec `event_type = SYSTEM_HELP_CHAT` (auj. aucun row n'apparaît).
- [ ] `CaseConclusionServiceIT` — déclencher une génération de conclusions F-243 → vérifier `usage_events` row `SYSTEM_CASE_CONCLUSION` créé.
- [ ] `VisionEnrichmentServiceIT` (F-148) — vérifier `usage_events` `SYSTEM_VISION_ENRICHMENT` créé.
- [ ] `ChunkAnalysisServiceIT` — workspace au-delà du quota → `ChunkAnalysis` créé en statut `SKIPPED`, **aucun appel** Anthropic (vérif par Mockito `verifyNoInteractions` sur le client HTTP).
- [ ] `CaseAnalysisCommandServiceIT` — workspace au-delà du quota → endpoint POST retourne HTTP 402 avec corps `TOKEN_BUDGET_EXCEEDED` (régression — comportement déjà existant).

### Isolation workspace

- [x] Applicable — test `AnthropicServiceTest` : appel avec `AiCallContext(workspaceId=W1, ...)` → seul le compteur de W1 est lu (`isMonthlyTokenBudgetExceeded(W1)` invoqué, pas W2).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Plans / limites** — gate centralisé sur `AnthropicService`, impacte les 25 sites callers et toutes les futures features qui appelleront Anthropic.
- [x] **Workspace context** — `workspaceId` propagé à travers `AiCallContext` sur 21 callers user-level (les 4 system-level natifs passent `null`).
- [ ] **Auth / Principal** — non touché.
- [ ] **Navigation / routing frontend** — non touché.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `POST /api/v1/case-files/{id}/analyze` | Appelle `CaseAnalysisCommandService` qui appelle indirectement `AnthropicService`. Gate déjà présent en amont, comportement HTTP 402 inchangé. | IT existant `CaseAnalysisCommandServiceIT` + nouveau IT quota dépassé. |
| `POST /api/v1/case-files/{id}/re-analyze` | idem via `ReAnalysisCommandService`. | IT existant `ReAnalysisCommandServiceIT`. |
| `POST /api/v1/case-files/{id}/chat` | `ChatService` — gate messages séparé conservé, gate tokens ajouté en plus. | IT `ChatServiceIT`. |
| `POST /api/v1/help/chat` | `HelpChatService` — gate ajouté (system-level skip), nouveau record. | Nouveau IT `HelpChatServiceIT`. |
| `POST /api/v1/case-files/{id}/conclusion` | `CaseConclusionService` (F-243) — gate ajouté (system-level skip), nouveau record. | Nouveau IT. |
| Crons référentiels / blog / JP super-admin | Skip gate, record ajouté avec `userId=null`. | IT `ReferentialCheckServiceIT`, `BlogArticleGeneratorServiceIT` adaptés. |

### Smoke tests E2E concernés

- [x] `e2e/smoke/case-analysis.spec` — workflow complet upload → analyse → synthèse → outils décisionnels. Doit passer sans régression : le gate reste fail-closed sur user-level mais le quota par défaut TEAM (18 M tokens/mois) n'est pas dépassé en E2E.
- [x] `e2e/smoke/help-chat.spec` (si existe) — vérifier que HelpChat fonctionne toujours (système skip gate, record nouveau invisible côté UI).

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

- [ ] `docs/OPEN_QUESTIONS.md` — vérifié 2026-05-28, aucune question ouverte sur le gate Anthropic / tokens / quotas.

---

## Notes et décisions

- **Décision 1** : option A (gate dans `AnthropicService` directement) plutôt que B (wrapper `AnthropicGatedService`). Rationale : simplicité, moins d'indirection, contrainte au compile time via suppression des anciennes signatures.
- **Décision 2** : `JobType.SYSTEM_*` skip gate user mais **record obligatoire** (option « trackable mais skip gate » validée par le PO le 2026-05-28). Permet de quantifier le coût Anthropic system-level dans une seule table.
- **Décision 3** : pas d'AOP / décorateur. Le passage explicite de `AiCallContext` est plus verbeux mais plus lisible et auditable. Recommandation rejetée en faveur de la signature directe.
- **Décision 4** : `PaymentRequiredException(TOKEN_BUDGET_EXCEEDED)` réutilise le mapping HTTP 402 existant — pas de nouveau code d'erreur.
- **Décision 5** : les 4 sites déjà gatés conservent leur check pré-emptif (efficace pour skip un batch en amont) mais perdent leur record manuel (automatisé).
- **Décision 6** : le `ChatService` continue de gater séparément `isChatMessageLimitReached` (compteur de messages, indépendant du compteur de tokens). Cumul des deux gates conservé.
- **Décision 7** : pas de feature flag — la centralisation est appliquée en bloc dans un PR atomique. Rollback = revert du PR.
