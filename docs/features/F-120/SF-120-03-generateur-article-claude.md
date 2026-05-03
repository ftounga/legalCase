# Mini-spec — F-120 / SF-120-03 Générateur d'article Claude Sonnet 4.6 + vérification Haiku 4.5

## Identifiant

`F-120 / SF-120-03`

## Feature parente

`F-120` — Blog SEO automatisé (auto-publication d'articles juridiques FR + BE)

## Statut

`draft`

## Date de création

2026-05-03

## Branche Git

`feat/SF-120-03-generateur-article-claude`

---

## Objectif

Générer un article de blog complet (intro, 4-6 sections H2, conclusion, CTA, méta SEO) à partir d'un topic réservé par `TopicSelectorService` (SF-120-02), via Claude Sonnet 4.6 (rédaction) + Claude Haiku 4.5 (vérification des citations juridiques), en plafonnant strictement le coût mensuel Anthropic via un service `IaCostTracker` app-side qui devient le garde-fou principal de F-120 (cf. `PREREQUIS-AVANT-SF-120-03.md` v2).

---

## Comportement attendu

### Cas nominal

1. **Topic réservé** : `TopicSelectorService.selectNext()` retourne un topic (statut `RESERVED`).
2. **Check budget Anthropic** : `IaCostTracker.canSpend(ANTHROPIC, estimatedCostEur=0.06)`.
   - Cap mensuel Anthropic = 30 € (config `blog.ia.budget.anthropic-eur=30`).
   - Si refusé → `ArticleGenerationResult.BudgetBlocked`, le topic est libéré (`RESERVED → AVAILABLE` via `releaseReservation`), événement `BLOG_ARTICLE_BUDGET_BLOCKED` loggé. Aucune persistance article.
3. **Génération texte (Claude Sonnet 4.6)** : 1 appel unique avec un prompt structuré qui demande à Claude de produire un JSON contenant tous les fragments :
   ```json
   {
     "metaTitle": "string ≤ 60 chars",
     "metaDescription": "string 140-160 chars",
     "introduction": "markdown 150-250 mots",
     "sections": [{"heading": "H2", "content": "markdown 300-500 mots"}, ...],
     "conclusion": "markdown 100-200 mots",
     "ctaBlock": "markdown 50-100 mots avec lien {{LEGALCASE_CTA}}",
     "legalCitations": [{"reference": "...", "context": "..."}]
   }
   ```
   - Modèle : `claude-sonnet-4-6` (constante `BlogClaudeModels.SONNET`).
   - Max tokens output : 6000 (couvre un article de ~3500 mots avec marge).
   - Le prompt système figé inclut : audience (avocats francophones), domaines V1, ton, guidelines SEO, contrainte JSON strict (pas de markdown autour), interdiction d'inventer des citations juridiques.
4. **Parsing JSON** :
   - Tentative 1 : parse direct.
   - Si échec : fallback `extractJsonFromMarkdown(response)` (strip ```json ... ```).
   - Si toujours échec : `ArticleGenerationResult.GenerationFailed("INVALID_JSON")`, topic libéré, événement loggé.
5. **Vérification jurisprudence (Claude Haiku 4.5)** : 1 appel avec liste `legalCitations` extraites, prompt qui demande à Haiku de classifier chaque citation `KNOWN | UNVERIFIABLE | LIKELY_HALLUCINATION`.
   - Modèle : `claude-haiku-4-5` (constante `BlogClaudeModels.HAIKU`).
   - Max tokens output : 1000.
   - Si une seule citation `LIKELY_HALLUCINATION` : article passé en statut `NEEDS_REVIEW` au lieu de `DRAFT` (visible super-admin tab SF-120-08, pas auto-publié).
6. **Concaténation markdown** : assemblage final `introduction + sections (ordered) + conclusion + ctaBlock` en un seul `body_markdown`.
7. **Persistance article** dans `blog_articles` :
   - `status = DRAFT` (ou `NEEDS_REVIEW` si hallucination détectée).
   - `topic_id`, `slug` (recopié du topic), `meta_title`, `meta_description`, `body_markdown`.
   - `hero_image_url = NULL` (sera renseigné par SF-120-04).
   - `ai_model_text = 'claude-sonnet-4-6'`, `ai_model_review = 'claude-haiku-4-5'`.
   - `created_at = NOW()`, `published_at = NULL`.
8. **Mise à jour topic** : `TopicSelectorService.markAsUsed(topicId, articleId)` (`RESERVED → USED`).
9. **Enregistrement coût** : `IaCostTracker.record(ANTHROPIC, model, actualCostEur)` — **2 lignes `usage_events`** (une Sonnet + une Haiku) **dans la même transaction** que la persistance article.
10. **Retour** : `ArticleGenerationResult.Success(articleId, status)`.

### Cas d'erreur

| Situation | Comportement attendu | Effet sur l'article |
|-----------|---------------------|---------------------|
| Cap mensuel Anthropic atteint avant Sonnet | Pas d'appel IA. Topic libéré. Événement `BLOG_ARTICLE_BUDGET_BLOCKED` loggé. | Pas d'article créé. |
| Cap atteint **entre** Sonnet et Haiku | Article quand même persisté en `NEEDS_REVIEW` (le coût Sonnet est déjà payé). Événement `BLOG_HAIKU_BUDGET_BLOCKED` loggé. | Article visible super-admin, marqué "vérification jurisprudence non faite". |
| Erreur transitoire Anthropic (`429`, `5xx`, timeout) sur Sonnet | Retry borné : 2 retries max, backoff 1 s / 4 s. Si succès → cas nominal. Sinon → topic libéré, événement loggé, `GenerationFailed("ANTHROPIC_TRANSIENT")`. | Pas d'article créé. |
| Erreur définitive Anthropic (`400`, `invalid_request`, `permission_denied`) sur Sonnet | **Pas de retry**. Topic libéré. Événement loggé avec code erreur. | Pas d'article créé. |
| JSON Sonnet invalide (parse échoué + fallback échoué) | 1 retry du même appel Sonnet (Anthropic peut renvoyer un JSON propre la fois suivante). Si encore échec → topic libéré. | Pas d'article créé. Coût : 2× appel Sonnet (~0,12 €). |
| Erreur transitoire Haiku | 1 retry. Si échec → article persisté en `NEEDS_REVIEW` (pas bloquant). | Article créé en `NEEDS_REVIEW`. |
| Erreur définitive Haiku | Article persisté en `NEEDS_REVIEW`. | Article créé en `NEEDS_REVIEW`. |
| Réponse Sonnet contient < 3 sections H2 | Article persisté quand même en `NEEDS_REVIEW` (avocat super-admin décide). | Article créé en `NEEDS_REVIEW`. |
| `metaTitle` > 60 chars ou `metaDescription` hors plage 140-160 | Article persisté en `NEEDS_REVIEW`. | Article créé en `NEEDS_REVIEW`. |
| `topic_id` déjà associé à un article (race) | Contrainte unique DB → exception → topic libéré + log critique. | Pas de doublon (idempotence garantie). |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — Non applicable (générateur de contenu blog, pas un outil décisionnel par dossier).
- [x] **Autres pays** — Le prompt Sonnet inclut le `country_scope` du topic ; le système prompt explicite "France" ou "Belgique" pour adapter les références juridiques (Code du travail FR vs Loi 1978/Code judiciaire BE, Code civil FR vs Code civil BE, etc.).
- [x] **Autres domaines** — Les 3 domaines V1 (travail, immigration, famille) couverts via `category` injectée dans le prompt + `domainContext` (terminologie juridique attendue par domaine).
- [x] **Autres UI patterns** — Pas de FE dans cette SF.
- [x] **Autres flows transversaux** — Réutilise `UsageEventRepository` (table `usage_events`, F-16). Réutilise `AnthropicClient` existant ? **À vérifier au démarrage du dev** : si un client Anthropic existe déjà (pipeline IA dossier), le réutiliser ; sinon créer `BlogAnthropicClient` dédié pour ne pas perturber les configs existantes.

### Niveaux de vérification couverts

- [x] **Modèle TypeScript / API exposée** — Aucune API REST exposée dans cette SF (service interne).
- [x] **Record / DTO backend** — `ArticleGenerationResult` (sealed : `Success | BudgetBlocked | GenerationFailed`), `GeneratedArticleJson` (DTO mappé sur la réponse Sonnet), `LegalCitationVerdict` (DTO Haiku).
- [x] **Service / logique métier** — `BlogArticleGeneratorService`, `IaCostTracker` (NOUVEAU partagé), `BlogPromptBuilder`, `BlogAnthropicClient`, `LegalCitationVerifier`.
- [x] **Entité JPA + schéma DB** — `BlogArticle` (déjà créée SF-120-01) — vérifier que les colonnes `ai_model_text`, `ai_model_review`, `body_markdown`, `meta_title`, `meta_description` existent. Sinon migration de complétion.
- [x] **Tests existants** — Réutilise les tests `BlogArticleRepository` de SF-120-01.

### Cas spécifique : nouveau pattern UI ou service partagé

Cette SF introduit **`IaCostTracker`** — service partagé multi-provider qui sera étendu en SF-120-04 (OpenAI). Scan effectué :

- [x] **Où le pattern pourrait-il être réutilisé ?** Toute future feature qui consomme une API IA payante hors du flux quota plan F-16 (ex. : F-IA-XX d'analyse en background, futurs assistants). Le pattern est conçu provider-agnostique pour anticiper.
- [x] **Patterns concurrents ?** F-16 `PlanLimitService` → quotas par utilisateur/workspace. `IaCostTracker` → cap par provider sur la plateforme. Les deux sont complémentaires (axes différents). Pas de fusion : F-16 facture l'utilisateur final, `IaCostTracker` protège l'entreprise.
- [x] **Le service peut-il servir à d'autres features ?** Oui, c'est sa raison d'être. Conçu en service Spring `@Service` injectable.
- [x] **Équivalent design existant ?** Non — pas de cap mensuel global par provider en V1 hors F-16.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Domaines (Travail / Immigration / Famille) | Oui | Intégré via `category` + `domainContext` dans le prompt (3 entrées dans `DomainContextMapper`) |
| Pays (FR / BE) | Oui | Intégré via `country_scope` du topic injecté dans le système prompt |
| Auth | Non | Service interne, aucune exposition |
| Workspace context | Non | Contenu public partagé (cf. SF-120-01) |
| Outils décisionnels | Non | Hors panel F-IA-04 |
| Frontend | Non | Couvert par SF-120-06 |
| Plans / limites | **Vérification** | `usage_events` doit accepter `provider=ANTHROPIC` avec `workspace_id=NULL` sans être comptabilisé dans `PlanLimitService.usedThisMonth(workspaceId)`. Test d'intégration ajouté. |

### Décision

- [x] Étendu à toutes les cibles applicables (FR + BE, 3 domaines).
- [x] Pas de cible restante nécessitant une SF parallèle.

---

## Impact par domaine métier

Cette SF est **transversale aux 3 domaines V1** (Travail / Immigration / Famille) et aux 2 pays (FR + BE).

- `DomainContextMapper` couvre les 3 catégories dès cette SF (3 fragments de prompt avec terminologie juridique attendue).
- `country_scope` du topic injecté dans le prompt système permet à Claude d'adapter les références (Code du travail FR vs Loi du 3 juillet 1978 BE, par exemple).
- Aucun risque d'asymétrie : le pattern de génération est identique pour les 6 combinaisons (3 domaines × 2 pays).

### Parité des domaines métier

Niveau de l'outil livré : **Niveau 2 — Générateur de document** (selon la grille des 7 niveaux CLAUDE.md). Cette SF livre la même capacité pour les 3 domaines simultanément, donc **pas d'asymétrie** à signaler ni de feature jumelle à ouvrir.

---

## Critères d'acceptation

- [ ] Service `BlogArticleGeneratorService` avec méthode `generateForTopic(topicId): ArticleGenerationResult`.
- [ ] Service `IaCostTracker` introduit (nouveau, partagé) avec méthodes `canSpend(provider, estimatedCostEur)` et `record(provider, model, actualCostEur)`.
- [ ] Enum `IaProvider` créé avec valeurs `ANTHROPIC`, `OPENAI` (préparation SF-120-04).
- [ ] Migration Liquibase `082-usage-events-add-provider-column.xml` (si la colonne `provider` n'existe pas) ou vérification que le schéma actuel accepte la nouvelle valeur enum.
- [ ] Caps mensuels lus depuis `application.yml` : `blog.ia.budget.anthropic-eur=30`, `blog.ia.budget.anthropic-alert-eur=15`.
- [ ] Système prompt + user prompt figés dans une classe dédiée `BlogPromptBuilder` (testable indépendamment).
- [ ] `BlogPromptBuilder.buildSonnetPrompt(topic)` injecte : `category`, `domainContext`, `country_scope`, `slug`, `title`, `description`, instructions JSON strict.
- [ ] `BlogPromptBuilder.buildHaikuPrompt(citations)` injecte : liste citations + classification attendue (`KNOWN`/`UNVERIFIABLE`/`LIKELY_HALLUCINATION`).
- [ ] `BlogAnthropicClient` avec `RestClient` Spring, masque `ANTHROPIC_API_KEY` dans les logs.
- [ ] Modèles : `claude-sonnet-4-6` pour rédaction, `claude-haiku-4-5` pour vérification.
- [ ] Retry borné Sonnet : 2 retries max, backoff 1 s / 4 s, **uniquement** sur erreurs transitoires (`429`, `5xx`, `IOException`).
- [ ] Pas de retry sur erreurs définitives (`400`, `invalid_request`, `permission_denied`).
- [ ] Parsing JSON robuste : tentative directe puis `extractJsonFromMarkdown` puis 1 retry du prompt.
- [ ] Validation post-parsing : `metaTitle.length ≤ 60`, `metaDescription.length ∈ [140, 160]`, `sections.size ≥ 3` — sinon `NEEDS_REVIEW`.
- [ ] Vérification Haiku : si ≥ 1 citation `LIKELY_HALLUCINATION` → article passe en `NEEDS_REVIEW`.
- [ ] Persistence atomique : 1 `BlogArticle` + 2 `UsageEvent` (Sonnet + Haiku) + transition topic `RESERVED → USED` dans la même transaction.
- [ ] Cas d'erreur entre Sonnet et Haiku : article quand même persisté en `NEEDS_REVIEW`, le `UsageEvent` Sonnet est enregistré, le coût Haiku est 0.
- [ ] `topic_id` est unique sur `blog_articles` (contrainte DB) — vérifier que SF-120-01 l'a posée, sinon ajouter migration.
- [ ] Aucune fuite de `ANTHROPIC_API_KEY` dans les logs (entête `x-api-key` masqué).
- [ ] `usage_events` enregistré avec `workspace_id=NULL` pour les coûts blog (pas comptabilisé dans `PlanLimitService` plan utilisateur).

---

## Périmètre

### Hors scope (explicite)

- Aucune génération d'image hero (couvert par SF-120-04).
- Aucune publication automatique (`status` reste `DRAFT` ou `NEEDS_REVIEW` — SF-120-05 fera la publication).
- Aucun endpoint REST exposé (SF-120-08 exposera des endpoints super-admin).
- Aucune interface admin de revue (couverte par SF-120-08).
- Pas de versioning d'article (pas de `BlogArticleHistory`).
- Pas de re-génération automatique d'un article existant (manuelle via SF-120-08).
- Pas de scheduling (SF-120-05).
- Pas de validation orthographique externe (Claude est suffisant en V1).
- Pas de conversion markdown → HTML (le frontend SF-120-06 utilisera un parser markdown côté Angular).
- Pas de gestion d'images dans le corps de l'article (uniquement hero, géré par SF-120-04).

---

## Valeurs initiales

### Configuration `application.yml`

```yaml
blog:
  ia:
    budget:
      anthropic-eur: 30.00          # cap mensuel hard
      anthropic-alert-eur: 15.00    # alerte super-admin
    sonnet:
      model: claude-sonnet-4-6
      max-tokens: 6000
      retry-max: 2
      retry-backoff-ms: [1000, 4000]
      cost-per-input-mtok-eur: 2.80   # ~3 USD/Mtok input
      cost-per-output-mtok-eur: 14.00 # ~15 USD/Mtok output
    haiku:
      model: claude-haiku-4-5
      max-tokens: 1000
      retry-max: 1
      retry-backoff-ms: [500]
      cost-per-input-mtok-eur: 0.75
      cost-per-output-mtok-eur: 3.75
```

### Mapping domaine → contexte prompt (`DomainContextMapper`)

| Catégorie | `domainContext` |
|-----------|-----------------|
| `DROIT_DU_TRAVAIL` | "Code du travail (FR) / Loi du 3 juillet 1978 (BE), CCT (BE), prud'hommes (FR), tribunal du travail (BE), conventions collectives, droit individuel et collectif" |
| `DROIT_IMMIGRATION` | "CESEDA (FR) / Loi du 15 décembre 1980 (BE), titres de séjour, autorisations de travail, OQTF (FR) / OQT (BE), recours administratifs et juridictionnels" |
| `DROIT_FAMILLE` | "Code civil (FR + BE), divorce, autorité parentale, prestation compensatoire (FR) / pension alimentaire (BE), procédures gracieuses et contentieuses" |

---

## Contraintes de validation

| Champ | Règle |
|-------|-------|
| `meta_title` | ≤ 60 caractères, non vide |
| `meta_description` | 140-160 caractères |
| `body_markdown` | ≥ 1500 mots, ≤ 5000 mots |
| `sections.size` | ≥ 3, ≤ 8 |
| `slug` | Recopié depuis le topic (déjà validé SF-120-01) |
| Statut accepté | `DRAFT` ou `NEEDS_REVIEW` (pas de `PUBLISHED` directement) |

---

## Technique

### Endpoint(s)

Aucun endpoint REST exposé dans cette SF.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `blog_articles` | INSERT (1 ligne par génération) | Schéma déjà créé SF-120-01 |
| `blog_topics` | UPDATE (transition RESERVED → USED) | Via `TopicSelectorService.markAsUsed` (SF-120-02) |
| `usage_events` | INSERT (2 lignes par génération : Sonnet + Haiku) | Table existante F-16, étendue avec `provider=ANTHROPIC` |

### Migration Liquibase

- [ ] **À évaluer au démarrage du dev** : `082-usage-events-add-provider-column.xml` si la colonne `provider` n'existe pas ou ne supporte pas `ANTHROPIC` / `OPENAI`. À confirmer en lisant le schéma `usage_events` actuel.
- [ ] Si nécessaire : `083-blog-articles-add-ai-model-columns.xml` pour `ai_model_text`, `ai_model_review` si non posées en SF-120-01.

### Composants Angular

- Aucun dans cette SF.

### Classes Spring à créer

| Classe | Package | Rôle |
|--------|---------|------|
| `BlogArticleGeneratorService` | `fr.ailegalcase.blog.service` | Orchestrateur (topic → check budget → Sonnet → Haiku → persist) |
| `IaCostTracker` | `fr.ailegalcase.ia.cost` | Service partagé : check + record cost mensuel par provider |
| `IaProvider` (enum) | `fr.ailegalcase.ia.cost` | Valeurs `ANTHROPIC, OPENAI` |
| `BlogAnthropicClient` | `fr.ailegalcase.blog.client` | Client HTTP Anthropic (Spring `RestClient`), masque API key |
| `BlogPromptBuilder` | `fr.ailegalcase.blog.service` | Construit prompts Sonnet + Haiku (testable) |
| `DomainContextMapper` | `fr.ailegalcase.blog.service` | Mapping catégorie → fragment de prompt |
| `LegalCitationVerifier` | `fr.ailegalcase.blog.service` | Wrapper appel Haiku + classification |
| `RetryableAnthropicCall<T>` | `fr.ailegalcase.blog.client` | Retry borné, classification transitoire/définitif |
| `AnthropicErrorClassifier` | `fr.ailegalcase.blog.client` | Classe utilitaire |
| `GeneratedArticleJson` (record) | `fr.ailegalcase.blog.service` | DTO Sonnet response |
| `LegalCitationVerdict` (record) | `fr.ailegalcase.blog.service` | DTO Haiku response |
| `ArticleGenerationResult` (sealed) | `fr.ailegalcase.blog.service` | `Success(articleId, status) \| BudgetBlocked \| GenerationFailed(reason)` |

### Extension `UsageEvent` / `UsageEventRepository`

```java
// Méthode à ajouter au repository :
@Query("SELECT COALESCE(SUM(u.costEur), 0) FROM UsageEvent u " +
       "WHERE u.provider = :provider AND u.workspaceId IS NULL " +
       "AND u.createdAt >= :monthStart")
BigDecimal sumPlatformCostThisMonth(IaProvider provider, Instant monthStart);
```

> **Note importante** : `workspace_id IS NULL` filtre les coûts plateforme (blog) des coûts utilisateur (F-16). Si la colonne `workspace_id` est non-nullable actuellement, prévoir une migration pour la rendre nullable + valeur par défaut `NULL` pour les coûts blog. À vérifier au démarrage du dev.

---

## Plan de test

### Tests unitaires

- [ ] `DomainContextMapper` retourne le bon fragment pour chaque catégorie ; lève `IllegalArgumentException` sur catégorie inconnue.
- [ ] `BlogPromptBuilder.buildSonnetPrompt(topic)` inclut bien `category`, `country_scope`, `domainContext`, `slug`, `title`, instructions JSON strict.
- [ ] `BlogPromptBuilder.buildHaikuPrompt(citations)` inclut bien la liste de citations.
- [ ] `AnthropicErrorClassifier` : `429` → transitoire, `500-599` → transitoire, `400` → définitif, `permission_denied` → définitif.
- [ ] `RetryableAnthropicCall` : succès au 1er appel → 1 appel total ; succès au 2e après `429` → 2 appels ; 3 échecs `500` → renonce.
- [ ] `RetryableAnthropicCall` : `400` au 1er appel → pas de retry, 1 appel total.
- [ ] `IaCostTracker.canSpend(ANTHROPIC, 0.06)` quand cumul mois = 29,95 € → false.
- [ ] `IaCostTracker.canSpend(ANTHROPIC, 0.06)` quand cumul mois = 25 € → true.
- [ ] `IaCostTracker.canSpend(OPENAI, ...)` retourne true (rien consommé encore — préparation SF-120-04).
- [ ] `LegalCitationVerifier` : ≥ 1 `LIKELY_HALLUCINATION` → article doit passer en `NEEDS_REVIEW`.
- [ ] `LegalCitationVerifier` : 0 hallucination → status reste `DRAFT`.
- [ ] `BlogArticleGeneratorService.generateForTopic` : cap atteint → `BudgetBlocked`, topic libéré, aucun appel HTTP (mock client non invoqué).
- [ ] `BlogArticleGeneratorService.generateForTopic` : cas nominal → article DRAFT créé, 2 lignes `usage_events` Sonnet+Haiku, topic USED.
- [ ] `BlogArticleGeneratorService.generateForTopic` : Sonnet retourne JSON invalide × 2 → `GenerationFailed`, topic libéré.
- [ ] `BlogArticleGeneratorService.generateForTopic` : Sonnet OK + Haiku échoue → article `NEEDS_REVIEW`, 1 ligne `usage_events` Sonnet uniquement.
- [ ] `BlogArticleGeneratorService.generateForTopic` : metaTitle 65 chars → article `NEEDS_REVIEW` (validation post-parsing).
- [ ] `BlogArticleGeneratorService.generateForTopic` : 2 sections H2 (< 3) → `NEEDS_REVIEW`.

### Tests d'intégration

- [ ] Flow complet avec mock Anthropic (WireMock) répondant 200 sur Sonnet + Haiku → article DRAFT persisté avec body markdown complet, topic USED, 2 lignes `usage_events`.
- [ ] Flow `429` × 1 puis 200 sur Sonnet → succès, 2 appels Sonnet enregistrés dans `usage_events` (Anthropic facture les retries).
- [ ] Flow Sonnet 200 + Haiku échec définitif → article `NEEDS_REVIEW`, 1 seule ligne `usage_events` (Sonnet).
- [ ] Flow cap atteint → `BudgetBlocked`, aucun appel WireMock reçu, topic libéré (`AVAILABLE`).
- [ ] Flow JSON invalide × 2 retries → topic libéré, événement `BLOG_GENERATION_INVALID_JSON` loggé.
- [ ] **Test isolation F-16** : créer un `UsageEvent(provider=ANTHROPIC, workspaceId=NULL, costEur=5)` → vérifier que `PlanLimitService.usedThisMonth(anyWorkspaceId)` ne le compte PAS.
- [ ] L'`x-api-key` header n'apparaît dans aucune log Spring (vérification programmatique sur l'`appender` de test).
- [ ] Contrainte unique `topic_id` sur `blog_articles` : 2 appels concurrents sur le même topic (un seul `markAsUsed` réussit) → 1 seul article créé, l'autre voit `IllegalStateException` ou la contrainte FK.

### Isolation workspace

- [x] **Cas spécifique vérifié** : les coûts blog (`workspace_id=NULL`) ne sont **pas comptabilisés** dans `PlanLimitService` plan utilisateur. Test d'intégration explicite ci-dessus.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — non touché (service interne).
- [ ] Workspace context — non touché (blog public, pas de `workspace_id`).
- [x] **Plans / limites** — touché : extension de `usage_events` avec `provider=ANTHROPIC, workspace_id=NULL`. **Composants impactés** : `PlanLimitService`, `UsageEventRepository`. **Action** : ajouter test d'intégration garantissant que les coûts blog ne sont pas comptabilisés dans le plan utilisateur (cf. plan de test).
- [ ] Navigation / routing frontend — non applicable.
- [ ] Outil décisionnel métier — non applicable.

### Composants / endpoints existants potentiellement impactés

- `PlanLimitService` (F-16) — vérification anti-régression : ajouter test que `workspace_id IS NULL` n'est pas compté (cf. plan de test intégration).
- `UsageEventRepository` — extension : ajouter méthode `sumPlatformCostThisMonth(provider, monthStart)`.
- `AnthropicClient` (si existe pour pipeline IA dossier) — **à vérifier au démarrage du dev** : si un client Anthropic existant est extensible, le réutiliser. Sinon créer `BlogAnthropicClient` dédié (couplage minimal).

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné (pas de flow utilisateur affecté ; aucun endpoint exposé).

---

## Dépendances

### Subfeatures bloquantes

- `SF-120-01` — statut : **done** ✅ (fournit `BlogArticle` entité + colonnes meta).
- `SF-120-02` — statut : **done** ✅ (fournit `TopicSelectorService.selectNext/markAsUsed/releaseReservation`).

### Subfeatures dépendantes (consommatrices)

- `SF-120-04` (DALL-E hero) — utilise `IaCostTracker` pour OpenAI.
- `SF-120-05` (scheduler) — orchestre les appels à ce service.
- `SF-120-08` (super-admin tab) — consultera les `usage_events` blog + statut `NEEDS_REVIEW`.

### Questions ouvertes impactées

- [x] Aucune question bloquante de `docs/OPEN_QUESTIONS.md` impactée.

### Configuration k8s à patcher (avant déploiement staging)

- [ ] Vérifier que `ANTHROPIC_API_KEY` est dans le secret `backend-secrets` namespace `staging`. Si absent : `kubectl patch secret backend-secrets -n staging -p='{"data":{"ANTHROPIC_API_KEY":"<base64>"}}'`. À faire au moment du déploiement par le dev.

---

## Notes et décisions

### Pourquoi un seul appel Sonnet (pas de découpage par section)

Plusieurs appels Sonnet (1 par section) coûteraient plus en input tokens (le contexte se répète) et casseraient la cohérence rédactionnelle entre sections. Un seul appel structuré JSON est plus économique (~0,06 €/article vs ~0,15 €) et donne un résultat plus cohérent. Le risque "trop de tokens output" est borné par `max-tokens=6000` (≈ 4500 mots, suffisant).

### Pourquoi Haiku pour la vérification jurisprudence

Haiku 4.5 coûte ~5× moins cher que Sonnet, et la tâche de "classifier des citations" est simple (pas de génération créative). Le coût additionnel est ~0,005 €/article, négligeable. Sans ce filet, on prend le risque de publier un article avec une jurisprudence hallucinée → catastrophe réputationnelle pour un blog SEO juridique.

### Pourquoi `NEEDS_REVIEW` plutôt que rejeter complètement

Si Sonnet livre un article presque bon (1 hallucination, ou metaTitle 65 chars), on a déjà payé la génération. Plutôt que jeter et rebours-faire (= 2× le coût), on flagge `NEEDS_REVIEW` et l'avocat super-admin peut corriger en 30 secondes via SF-120-08.

### Pourquoi `usage_events` partagée avec F-16 plutôt qu'une nouvelle table

- Single source of truth pour tout coût IA.
- Même format permet d'agréger plateforme + utilisateur dans des dashboards plus tard.
- Filtre `workspace_id IS NULL` est naturel (le blog n'a pas de workspace).

Le seul risque est que `PlanLimitService` compte les coûts blog dans le quota utilisateur, ce qui serait absurde. Couvert par test d'intégration explicite.

### Pourquoi pas de retry sur Haiku au-delà de 1

Haiku coûte si peu que 2+ retries n'apportent rien. Si Haiku échoue 2 fois → article passe en `NEEDS_REVIEW`, l'avocat super-admin verra. Plus simple et plus sûr.

### Format du prompt système (extrait pour validation produit)

```
Tu es un rédacteur juridique francophone expert pour un blog B2B destiné aux avocats.
Tu maîtrises le {domainContext} et les spécificités {country_scope}.

Génère un article de blog SEO à partir du topic suivant :
- Titre : {title}
- Slug : {slug}
- Description : {description}
- Catégorie : {category}
- Pays : {country_scope}

Contraintes strictes :
- Audience : avocats francophones {country_scope}
- Ton : professionnel, précis, pédagogique
- Pas de "Je suis une IA"
- Citations juridiques précises (article + texte de loi)
- Si tu n'es pas sûr d'une citation, mentionne-la dans `legalCitations[].context = "À vérifier"`
- Réponse en JSON strict (sans markdown autour, sans préfixe, sans suffixe)

Format de réponse JSON :
{
  "metaTitle": "≤ 60 chars, accroche SEO",
  "metaDescription": "140-160 chars, accroche meta",
  "introduction": "markdown 150-250 mots, accroche + plan",
  "sections": [{"heading": "...", "content": "markdown 300-500 mots"}, ...],
  "conclusion": "markdown 100-200 mots, synthèse + ouverture",
  "ctaBlock": "markdown 50-100 mots avec lien {{LEGALCASE_CTA}} qui sera remplacé en post-traitement",
  "legalCitations": [{"reference": "Code du travail, art. L1234-X", "context": "..."}]
}

Pas de texte hors JSON.
```

Cette spec est figée pour SF-120-03. Une SF future pourra raffiner via templates.

### Coûts d'exploitation estimés

| Volume | Coût Sonnet | Coût Haiku | Total |
|--------|-------------|------------|-------|
| Phase 1 (8/mois) | 8 × 0,055 € = 0,44 € | 8 × 0,005 € = 0,04 € | **0,48 €/mois** |
| Phase 2 (4/mois) | 4 × 0,055 € = 0,22 € | 4 × 0,005 € = 0,02 € | **0,24 €/mois** |
| Worst-case (100 retries 1 article) | 100 × 0,055 = 5,50 € | — | bloqué bien avant par cap 30 € |

Le cap 30 € est très confortable. Le vrai risque est un bug de scheduler qui boucle et qui serait coupé net par `IaCostTracker`.
