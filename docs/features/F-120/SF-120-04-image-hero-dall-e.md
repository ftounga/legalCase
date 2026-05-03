# Mini-spec — F-120 / SF-120-04 Image hero DALL-E 3 avec retry borné + fallback

## Identifiant

`F-120 / SF-120-04`

## Feature parente

`F-120` — Blog SEO automatisé (auto-publication d'articles juridiques FR + BE)

## Statut

`draft`

## Date de création

2026-05-03

## Branche Git

`feat/SF-120-04-dall-e-hero-image`

---

## Objectif

Générer l'image hero d'un article via DALL-E 3 à partir d'un prompt dérivé du titre et de la catégorie, avec une stratégie de **retry borné + fallback "article sans hero"** pour absorber les échecs sans replan ni surcoût, et brancher l'appel sur le `IaCostTracker` introduit en SF-120-03 pour bloquer toute génération qui ferait dépasser le cap mensuel OpenAI (20 €).

---

## Comportement attendu

### Cas nominal

1. À la fin de SF-120-03 (texte article généré, statut `DRAFT`), le service `BlogHeroImageService.generateForArticle(articleId)` est appelé.
2. Vérification `IaCostTracker.canSpend(OPENAI, estimatedCost)` :
   - Cap mensuel OpenAI = 20 € (config `blog.ia.budget.openai-eur=20`).
   - Estimation : 0,04 USD ≈ 0,037 €/image.
   - Si le cap serait dépassé → **bloque** (cf. cas d'erreur "cap atteint"), pas de retry.
3. Construction du prompt DALL-E à partir du titre + catégorie :
   - Style figé : illustration vectorielle minimaliste, palette sobre (navy + or + blanc), pas de texte dans l'image, pas de photo réaliste, pas de logo, ratio 16:9.
   - Template : `"Minimal vector illustration for a French/Belgian legal blog article titled '{title}'. Subject: {categoryDescription}. Style: clean, professional, navy blue (#0B2147) and gold (#C9A54B) accents on white background, no text, no photorealism, 16:9 aspect ratio."`
4. Appel DALL-E 3 (`POST https://api.openai.com/v1/images/generations`) avec `model=dall-e-3, size=1792x1024, quality=standard, n=1`.
5. L'image (URL temporaire OpenAI, 60 min) est téléchargée puis **stockée sur S3** dans le bucket existant sous la clé `blog/articles/{articleId}/hero-{timestamp}.png` (réutilise `S3StorageService` existant).
6. L'URL S3 publique est persistée dans `blog_articles.hero_image_url` (colonne créée en SF-120-01).
7. `UsageEvent` enregistré (provider=`OPENAI`, model=`dall-e-3`, costEur=0.037) **dans la même transaction** que la mise à jour de l'article — atomicité du `IaCostTracker`.
8. Retour : `BlogHeroImageResult.success(s3Url)`.

### Cas d'erreur

| Situation | Comportement attendu | Effet sur l'article |
|-----------|---------------------|---------------------|
| Cap mensuel OpenAI atteint (`IaCostTracker` refuse) | Aucun appel DALL-E. Événement `BLOG_HERO_IMAGE_BUDGET_BLOCKED` loggé. | Article reste `DRAFT`, `hero_image_url=NULL` ; le scheduler SF-120-05 retentera le mois suivant. |
| Erreur transitoire DALL-E (`429`, `500-599`, timeout, network) | Retry avec backoff exponentiel (500 ms, 2 s) — **2 retries max, donc 3 appels au total**. | Si succès au retry → cas nominal. Sinon → fallback "no hero" (cf. ligne suivante). |
| Échec après 3 tentatives | Événement `BLOG_HERO_IMAGE_FAILED` loggé (super-admin tab SF-120-08). | Article publié **sans hero** (`hero_image_url=NULL`). Compteur : ce mode est compté comme dépense (3 × 0,04 USD ≈ 0,11 €). |
| Erreur définitive (`400 Bad Request`, `content_policy_violation`, `invalid_request_error`, `insufficient_quota`) | **Aucun retry**. Événement loggé. | Article publié **sans hero** comme ci-dessus. Si `content_policy_violation` → log spécifique pour révision manuelle du titre. |
| Téléchargement de l'URL OpenAI échoue | 1 seul retry. Si échec persistant → fallback "no hero". | Image facturée mais perdue ; loggué. Budget impacté de 0,037 €. |
| Upload S3 échoue | 2 retries. Si échec persistant → fallback "no hero" + log critique (incident infra). | Image facturée mais non stockée. |
| `OPENAI_API_KEY` absente du contexte | Exception au démarrage du bean → l'application ne démarre pas (fail-fast). | N/A (problème de déploiement). |

### Mode fallback "article sans hero"

Quand `hero_image_url=NULL`, le rendu frontend (SF-120-06) doit gérer le cas :
- Carte de liste : afficher un placeholder coloré (gradient navy/or, le titre par-dessus) — pas de bloc image vide ni d'image cassée.
- Page article : aucun bloc hero affiché, le titre prend la place habituellement occupée par l'image.

Cette règle est **figée ici** pour qu'elle soit reprise telle quelle dans la mini-spec SF-120-06.

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — Non applicable (génération d'image blog, pas un outil décisionnel par dossier).
- [x] **Autres pays** — Pas de logique pays-dépendante côté image (le prompt DALL-E mentionne "French/Belgian" pour rester cohérent stylistiquement, mais pas de routage différent).
- [x] **Autres domaines** — Le prompt DALL-E intègre la catégorie via `categoryDescription` (Travail / Immigration / Famille). Mapping figé dans `CategoryPromptMapper`.
- [x] **Autres UI patterns** — Pas de FE dans cette SF.
- [x] **Autres flows transversaux** — Réutilise `S3StorageService` existant (pas de nouveau pattern de stockage). Réutilise `UsageEventRepository` (pas de nouvelle source de vérité coût).

### Niveaux de vérification couverts

- [x] **Modèle TypeScript / API exposée** — Aucune API REST exposée dans cette SF (service interne, appelé par SF-120-03/05).
- [x] **Record / DTO backend** — `BlogHeroImageResult` (sealed interface : `Success | BudgetBlocked | Failed`), `DallEResponse`.
- [x] **Service / logique métier** — `BlogHeroImageService`, `DallEClient`, `RetryableImageGenerator`, `CategoryPromptMapper`.
- [x] **Entité JPA + schéma DB** — `BlogArticle.heroImageUrl` (colonne déjà créée SF-120-01).
- [x] **Tests existants** — Aucun test existant à étendre (SF nouvelle).

### Cas spécifique : nouveau pattern UI ou service partagé

Cette SF introduit `IaCostTracker` (déjà créé en SF-120-03) **étendu pour OpenAI**. Scan effectué :

- [x] **Où le pattern pourrait-il être réutilisé ?** Le `IaCostTracker` est conçu dès SF-120-03 pour être multi-provider — l'extension OpenAI ici est sa raison d'être. Aucun autre module n'a besoin de plafonner le coût IA en V1 (le pipeline IA dossier F-IA-* a son propre quotas plan via F-16).
- [x] **Patterns concurrents ?** Aucun. La table `usage_events` est déjà la source de vérité commune.
- [x] **Le service peut-il servir à d'autres features ?** Oui — toute future feature qui consomme une API IA payante hors du flux F-16 devrait utiliser `IaCostTracker`. Pas d'exposition publique (service interne Spring).
- [x] **Équivalent design existant ?** Pas pour les caps mensuels par provider. Existe pour les quotas plan utilisateur (F-16) mais c'est une dimension différente.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Domaines (Travail / Immigration / Famille) | Oui | Intégré via `CategoryPromptMapper` (3 entrées) |
| Pays (FR / BE) | Oui | Mention dans le prompt (style cohérent), pas de branche pays |
| Auth | Non | Service interne |
| Workspace context | Non | Contenu public (cf. SF-120-01) |
| Outils décisionnels | Non | Hors panel F-IA-04 |
| Frontend | Non | Couvert par SF-120-06 (et règle fallback figée ci-dessus) |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature.
- [x] Pas de cible restante nécessitant une SF parallèle.

---

## Impact par domaine métier

Cette SF est **transversale aux 3 domaines V1** (droit du travail, droit de l'immigration, droit de la famille) et aux 2 pays (FR + BE).

- Le `CategoryPromptMapper` couvre les 3 catégories dès cette SF (pas d'asymétrie).
- Aucune adaptation pays nécessaire : un même style visuel s'applique aux articles FR et BE.

---

## Critères d'acceptation

- [ ] Service `BlogHeroImageService` avec méthode `generateForArticle(articleId): BlogHeroImageResult`.
- [ ] Vérification `IaCostTracker.canSpend(OPENAI, 0.037)` avant tout appel DALL-E.
- [ ] Si cap atteint → retour `BlogHeroImageResult.BudgetBlocked`, aucun appel HTTP, événement loggé.
- [ ] Construction du prompt via `CategoryPromptMapper` avec template figé (cf. section "Cas nominal" point 3).
- [ ] Appel DALL-E 3 avec `model=dall-e-3, size=1792x1024, quality=standard, n=1` (paramètres en constantes).
- [ ] Retry exponentiel 500 ms / 2 s **uniquement** sur erreurs transitoires (`429`, `5xx`, `IOException`, `SocketTimeoutException`).
- [ ] **Aucun retry** sur erreurs définitives (`400`, `content_policy_violation`, `invalid_request_error`, `insufficient_quota`).
- [ ] Limite stricte : **2 retries max (3 appels au total)** par article — implémenté via `RetryableImageGenerator` testable.
- [ ] Téléchargement de l'URL OpenAI (1 retry) puis upload S3 (2 retries) sous la clé `blog/articles/{articleId}/hero-{timestamp}.png`.
- [ ] Persistence atomique : `UsageEvent(provider=OPENAI, model=dall-e-3, costEur=0.037)` + `BlogArticle.heroImageUrl` dans la même transaction.
- [ ] En cas d'échec après retries : `BlogArticle.heroImageUrl` reste `NULL`, événement `BLOG_HERO_IMAGE_FAILED` loggé.
- [ ] Compteur `usage_events` enregistre **chaque tentative payée**, pas seulement les succès (DALL-E facture les `429` retried mais pas les `400` rejetés instantanément — comportement aligné facturation OpenAI).
- [ ] Aucune fuite de la clé `OPENAI_API_KEY` dans les logs (entête `Authorization` masqué).
- [ ] Tous les codes d'erreur DALL-E mappés explicitement vers transitoire/définitif (pas de `default = retry`).

---

## Périmètre

### Hors scope (explicite)

- Aucune génération de variations / `n>1` — un seul candidat par article.
- Aucune logique de sélection multi-image (pas d'A/B sur le hero).
- Pas de modération du prompt côté app (DALL-E modère lui-même via `content_policy_violation`). Si on observe trop de rejets → SF future de templating de prompt amélioré.
- Pas de re-génération automatique d'une image existante (re-run manuel via SF-120-08 admin).
- Pas de gestion d'image "draft" ou de prévisualisation — l'image générée est directement persistée.
- Pas de redimensionnement / optimisation côté app (DALL-E renvoie déjà 1792×1024 PNG ; CDN/cache front en SF-120-06).
- Pas de stockage des prompts DALL-E historisés (loggés en INFO seulement).

---

## Valeurs initiales

### Configuration `application.yml` (chargée en SF-120-03 — extension ici)

```yaml
blog:
  ia:
    budget:
      openai-eur: 20.00      # cap mensuel hard
      openai-alert-eur: 10.00 # alerte email super-admin
    dalle:
      model: dall-e-3
      size: 1792x1024
      quality: standard
      retry-max: 2
      retry-backoff-ms: [500, 2000]
      cost-per-image-eur: 0.037
```

### Mapping catégorie → prompt (`CategoryPromptMapper`)

| Catégorie | `categoryDescription` injecté |
|-----------|-------------------------------|
| `DROIT_DU_TRAVAIL` | "labor law: workplace, contracts, employment relationships" |
| `DROIT_IMMIGRATION` | "immigration law: residency, work permits, administrative procedures" |
| `DROIT_FAMILLE` | "family law: divorce, custody, household matters" |

---

## Contraintes de validation

| Champ | Règle |
|-------|-------|
| `BlogArticle.heroImageUrl` | URL S3 publique HTTPS, ≤ 500 caractères, ou `NULL` (déjà posé en SF-120-01) |
| Budget cap | Lu depuis `application.yml`, type `BigDecimal`, scale 2 |
| Backoff retries | Tableau d'entiers ≥ 0 ms, taille ≤ 5 (sécurité config) |

---

## Technique

### Endpoint(s)

Aucun endpoint REST exposé dans cette SF.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `blog_articles` | UPDATE de `hero_image_url` | Colonne déjà créée SF-120-01 |
| `usage_events` | INSERT (1 ligne par appel DALL-E facturé) | Table existante (F-16) — étendue avec valeur enum `OPENAI` si pas déjà présente |

### Migration Liquibase

- [ ] **Conditionnelle** : si `usage_events.provider` n'accepte pas encore `OPENAI`, ajouter une migration `081-usage-events-add-openai-provider.xml` (extension du `CHECK` constraint ou de l'enum). À vérifier au démarrage du dev.

### Composants Angular

- Aucun dans cette SF — le rendu hero est dans SF-120-06.

### Classes Spring à créer

| Classe | Package | Rôle |
|--------|---------|------|
| `BlogHeroImageService` | `fr.ailegalcase.blog.service` | Orchestrateur (cost check → prompt → DALL-E → S3 → persist) |
| `DallEClient` | `fr.ailegalcase.blog.client` | Client HTTP DALL-E (Spring `RestClient`), masque l'API key |
| `RetryableImageGenerator` | `fr.ailegalcase.blog.service` | Wrapper retry borné, classification transitoire/définitif |
| `CategoryPromptMapper` | `fr.ailegalcase.blog.service` | Mapping catégorie → fragment de prompt |
| `BlogHeroImageResult` (sealed) | `fr.ailegalcase.blog.service` | Variantes : `Success(s3Url)`, `BudgetBlocked`, `Failed(reason)` |
| `DallEResponse` (record) | `fr.ailegalcase.blog.client` | DTO réponse OpenAI |
| `DallEErrorClassifier` | `fr.ailegalcase.blog.client` | Classe utilitaire : transitoire vs définitif |

### Extension d'`IaCostTracker` (créé en SF-120-03)

Ajouter / vérifier que l'API supporte le provider OpenAI :

```java
public boolean canSpend(IaProvider provider, BigDecimal estimatedCostEur);
public void record(IaProvider provider, String model, BigDecimal actualCostEur);
```

`IaProvider` enum doit inclure `ANTHROPIC, OPENAI` dès SF-120-03.

---

## Plan de test

### Tests unitaires

- [ ] `CategoryPromptMapper` retourne le bon fragment pour chaque catégorie + lève `IllegalArgumentException` sur catégorie inconnue.
- [ ] `DallEErrorClassifier` : `429` → transitoire, `500-599` → transitoire, `IOException` → transitoire, `400` → définitif, `content_policy_violation` → définitif, `insufficient_quota` → définitif.
- [ ] `RetryableImageGenerator` : succès au 1er appel → 1 appel total.
- [ ] `RetryableImageGenerator` : succès au 2e appel après `429` → 2 appels.
- [ ] `RetryableImageGenerator` : succès au 3e appel après 2× `500` → 3 appels (dernier retry).
- [ ] `RetryableImageGenerator` : 3 échecs `500` consécutifs → renonce, retourne `Failed`.
- [ ] `RetryableImageGenerator` : `400` au 1er appel → **pas de retry**, retourne `Failed` (1 appel total).
- [ ] `BlogHeroImageService.generateForArticle` : cap budget atteint → `BudgetBlocked`, aucun appel HTTP (mock `DallEClient` non invoqué).
- [ ] `BlogHeroImageService.generateForArticle` : cas nominal → `Success(s3Url)`, `usage_events` enregistré, `hero_image_url` persisté.
- [ ] `BlogHeroImageService.generateForArticle` : DALL-E échoue après retries → `Failed`, article reste `hero_image_url=NULL`, événement loggé.
- [ ] `BlogHeroImageService.generateForArticle` : `content_policy_violation` → `Failed` immédiat, log distinct (pas de spam retry).
- [ ] `BlogHeroImageService.generateForArticle` : upload S3 échoue 3 fois → `Failed`, log critique.

### Tests d'intégration

- [ ] Flow complet avec mock OpenAI (WireMock) répondant 200 → article persisté avec `hero_image_url` S3 + `usage_events` ligne créée, transaction atomique.
- [ ] Flow `429` × 2 puis 200 → succès, 3 lignes `usage_events` (DALL-E facture les retries) — **ou 1 ligne** si on ne facture qu'au succès (à figer selon la politique OpenAI réelle, par défaut "1 ligne par appel"). À valider avec un appel manuel en dev.
- [ ] Flow `400` immédiat → `hero_image_url=NULL`, événement `BLOG_HERO_IMAGE_FAILED` créé.
- [ ] Flow cap atteint → `BudgetBlocked`, aucun appel WireMock reçu.
- [ ] L'`Authorization` header n'apparaît pas dans les logs Spring (vérification programmatique sur l'`appender` de test).

### Isolation workspace

- [x] **Non applicable** — blog public partagé, pas de `workspace_id`.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — non touché (service interne).
- [ ] Workspace context — non touché.
- [x] **Plans / limites** — touché indirectement : extension de `usage_events` avec un nouveau provider (`OPENAI`). Vérifier que `PlanLimitService` ne compte PAS les `OPENAI` dans le quota plan de l'utilisateur (le blog est un coût plateforme, pas un coût utilisateur). Test à ajouter.
- [ ] Navigation / routing frontend — non applicable.
- [ ] Outil décisionnel métier — non applicable.

### Composants / endpoints existants potentiellement impactés

- `PlanLimitService` (F-16) — vérifier qu'il filtre par `provider IN (ANTHROPIC)` ou par `workspace_id != NULL` (le blog n'a pas de workspace), pour ne pas comptabiliser les coûts blog dans les quotas utilisateurs. **Action** : ajouter un test d'intégration qui crée un `UsageEvent(provider=OPENAI, workspaceId=NULL)` et vérifie que `PlanLimitService.usedThisMonth(workspaceId)` ne le compte pas.
- `S3StorageService` — réutilisé tel quel.
- `UsageEventRepository` — étendu via la valeur enum `OPENAI` (vérifier que la colonne accepte la valeur, sinon migration 081).

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné (pas de flow utilisateur affecté).

---

## Dépendances

### Subfeatures bloquantes

- `SF-120-01` — statut : **done** ✅ (fournit `blog_articles.hero_image_url`).
- `SF-120-03` — statut : **à faire** ⏳ (fournit `IaCostTracker` et `IaProvider` enum). **Doit être mergée avant le démarrage de SF-120-04.**

### Questions ouvertes impactées

- [x] Aucune question bloquante de `docs/OPEN_QUESTIONS.md` impactée.

---

## Notes et décisions

### Pourquoi retry borné ET fallback "no hero" et pas seulement retry

Le retry seul protège contre les erreurs transitoires mais pas contre les erreurs définitives (`content_policy_violation`, `400`). Sans fallback, un article serait bloqué pour toujours. Le fallback "no hero" garantit que **le contenu textuel toujours coûteux à générer (Claude)** n'est pas perdu — l'avocat peut publier sans image et ajouter une image plus tard manuellement.

### Pourquoi pas de retry sur les erreurs définitives

Retry sur `400` ou `content_policy_violation` = retry payé qui échouera à coup sûr. Coût : 0,04 USD pour rien × 2 = 0,08 USD perdu par article problématique. Sur 100 articles avec 5 % de rejet politique = 0,4 USD/mois gaspillés inutilement. Détecter immédiatement et passer en fallback.

### Pourquoi 2 retries (3 tentatives) et pas 3 ou 5

- 1 seule tentative = trop fragile (un `429` random tue l'article).
- 2 retries (3 appels) = couvre le cas `429` ponctuel + reprise après hiccup réseau, à coût borné de 0,12 USD/échec (négligeable).
- 5+ retries = potentiel de 0,20 USD par article problématique × 100 articles = 20 USD/mois gaspillés en cas d'incident OpenAI prolongé. Trop coûteux.

### Pourquoi `IaCostTracker` et pas une simple condition `if monthlyCount > X`

- Source de vérité unique (`usage_events`) — déjà la table de référence pour les quotas (F-16).
- Atomicité transactionnelle entre check et write — évite que 2 articles parallèles passent ensemble au-dessus du cap.
- Permet l'extensibilité : ajouter un cap journalier, une alerte email, un dashboard super-admin (SF-120-08) sans refactor du service.

### Coût d'exploitation estimé

| Volume | Coût DALL-E (≈ 0,037 €/image) | Cap config |
|--------|-------------------------------|-----------|
| Phase 1 (2 articles/sem = 8/mois) | 0,30 € | 20 € |
| Phase 2 (1 article/sem = 4/mois) | 0,15 € | 20 € |
| Worst-case (bug → 100 tentatives sur 1 article) | 4 € (3 × 100 = 300 appels max via retry borné × 0,04 USD) | bloqué bien avant par cap |

Le cap 20 € est confortable même si on monte à 1 article/jour (≈ 30/mois × 0,037 = 1,11 €).

### Fallback frontend (SF-120-06) — à reprendre tel quel

Quand `hero_image_url=NULL`, ne pas afficher de bloc image vide. Choix de design figé ici :
- **Carte de liste** : placeholder gradient navy → or, titre par-dessus en blanc.
- **Page article** : titre en pleine largeur, pas de bloc hero.

Cette règle évite que SF-120-06 se pose la question et adopte un autre comportement (par ex. image cassée), source de dette de convergence.
