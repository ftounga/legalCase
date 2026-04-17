# Mini-spec — F-120 / SF-120-01 Modéliser les articles + topics et exposer les endpoints REST publics (lecture)

## Identifiant

`F-120 / SF-120-01`

## Feature parente

`F-120` — Blog SEO automatisé (auto-publication d'articles juridiques FR + BE)

## Statut

`draft`

## Date de création

2026-04-17

## Branche Git

`feat/SF-120-01-modele-articles`

---

## Objectif

Poser les fondations backend du blog : tables `blog_article` + `blog_topic` (Liquibase), entités JPA, repositories, et 3 endpoints REST publics en **lecture seule** pour servir la liste, le détail et le fragment sitemap des articles publiés.

---

## Comportement attendu

### Cas nominal

Trois endpoints publics (pas d'authentification) :

1. **`GET /api/v1/blog/articles`** — retourne une `Page<BlogArticleSummaryResponse>` paginée des articles `PUBLISHED` triés par `publishedAt DESC`. Filtres optionnels : `country` (FR / BE), `legalDomain` (DROIT_DU_TRAVAIL / DROIT_IMMIGRATION / DROIT_FAMILLE), `page`, `size`. Defaults : page 0, size 20, max 50.
2. **`GET /api/v1/blog/articles/{slug}`** — retourne `BlogArticleDetailResponse` pour un slug donné si l'article est `PUBLISHED`. 404 sinon.
3. **`GET /api/v1/blog/articles/sitemap-fragment`** — retourne `List<BlogSitemapEntry>` (slug, country, publishedAt, lastModifiedAt) pour tous les articles `PUBLISHED`. Pas de pagination. Sera consommé par SF-120-07 pour assembler le sitemap XML.

Aucun endpoint d'écriture dans cette SF. La création/publication viendront avec SF-120-02 (génération) et SF-120-08 (admin).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Slug inconnu sur `GET /{slug}` | Réponse vide, message générique | 404 |
| Article existe mais statut ≠ `PUBLISHED` (DRAFT, UNPUBLISHED) sur `GET /{slug}` | Traité comme inexistant — pas de fuite d'info | 404 |
| Paramètre `country` invalide (autre que FR / BE) | Erreur de validation explicite | 400 |
| Paramètre `legalDomain` invalide | Erreur de validation explicite | 400 |
| Paramètre `size` > 50 | Borné silencieusement à 50 | 200 |
| Paramètre `page` négatif | Erreur de validation | 400 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — Non applicable. Le blog est du contenu public, pas un outil décisionnel par dossier.
- [x] **Autres pays** — France + Belgique couverts dès cette SF via la colonne `country`.
- [x] **Autres domaines** — Les 3 domaines V1 (travail, immigration, famille) couverts via `legal_domain`.
- [x] **Autres UI patterns** — Non applicable (pas de frontend dans cette SF).
- [x] **Autres flows transversaux** — Auth touchée (cf. ci-dessous).

### Niveaux de vérification couverts

- [x] **Modèle TypeScript / API exposée** — DTOs documentés ci-dessous (frontend dans SF-120-06)
- [x] **Record / DTO backend** — `BlogArticleSummaryResponse`, `BlogArticleDetailResponse`, `BlogSitemapEntry`
- [x] **Service / logique métier** — `BlogArticleQueryService` (lecture seule pour cette SF)
- [x] **Entité JPA + schéma DB** — `BlogArticle`, `BlogTopic` + migrations 078/079
- [x] **Tests existants** — Aucun pré-existant. Suite complète à créer.

### Cas spécifique : nouveau pattern UI ou service partagé

Cette SF introduit le module backend `blog` (nouveau package `fr.ailegalcase.blog`). Elle pose les conventions pour toutes les SF suivantes de F-120.

- [x] **Pattern partagé** — package `fr.ailegalcase.blog` (controller, service, repository, entity, dto, mapper) suit la convention déjà établie pour les autres modules (`fr.ailegalcase.casefile`, etc.). Pas de pattern concurrent.
- [x] **Endpoint public sans auth** — précédent : `/api/v1/health/**`, ressources statiques. Le pattern reste cohérent : config Spring Security autorise GET `/api/v1/blog/**` sans auth.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Domaines (Travail / Immigration / Famille) | Oui | Intégré dès SF-120-01 via `legal_domain` |
| Pays (FR / BE) | Oui | Intégré dès SF-120-01 via `country` |
| Auth (endpoints publics) | Oui | Modification `SecurityConfig` dans cette SF — autorise GET `/api/v1/blog/**` |
| Outils décisionnels par dossier | Non | Le blog est du contenu public, hors flows métier dossier |
| Frontend (page /blog) | Non | Cible de SF-120-06 |
| Génération IA | Non | Cible de SF-120-02 |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (domaines, pays)
- [x] Subfeature(s) parallèle(s) prévue(s) pour les cibles restantes : SF-120-02 à SF-120-08

---

## Critères d'acceptation

- [ ] Migration Liquibase 078 crée la table `blog_article` avec toutes les colonnes et contraintes décrites ci-dessous
- [ ] Migration Liquibase 079 crée la table `blog_topic` avec toutes les colonnes et contraintes décrites ci-dessous
- [ ] Entité JPA `BlogArticle` + repository Spring Data JPA `BlogArticleRepository`
- [ ] Entité JPA `BlogTopic` + repository Spring Data JPA `BlogTopicRepository`
- [ ] `GET /api/v1/blog/articles` retourne `Page<BlogArticleSummaryResponse>` avec filtres `country` et `legalDomain` opérationnels, tri `publishedAt DESC`, accessible sans authentification
- [ ] `GET /api/v1/blog/articles/{slug}` retourne `BlogArticleDetailResponse` pour un article `PUBLISHED`, 404 pour DRAFT / UNPUBLISHED / inconnu, accessible sans authentification
- [ ] `GET /api/v1/blog/articles/sitemap-fragment` retourne `List<BlogSitemapEntry>` (tous les articles `PUBLISHED`), accessible sans authentification
- [ ] `SecurityConfig` autorise GET `/api/v1/blog/**` en accès anonyme (pas d'impact sur les autres routes — vérifié par smoke tests `auth.spec.ts`)
- [ ] Slug unique en base (contrainte DB) + index pour lookup rapide
- [ ] Validation Bean (`@NotBlank`, `@Size`, `@Pattern` ASCII kebab-case sur slug) sur les entités
- [ ] Tests unitaires service (≥ 8 cas) + tests d'intégration controller (≥ 12 cas) — total ≥ 20 tests verts
- [ ] Smoke tests E2E `auth.spec.ts` passent toujours après modif SecurityConfig

---

## Périmètre

### Hors scope (explicite)

- Aucune écriture publique (pas de POST/PUT/DELETE) — viendra avec SF-120-08
- Pas de génération d'article IA — viendra avec SF-120-02
- Pas de page Angular `/blog` — viendra avec SF-120-06
- Pas d'image hero (colonne `hero_image_url` créée mais nullable, remplie par SF-120-04)
- Pas de sitemap XML formaté — cette SF expose les données brutes JSON, l'assemblage XML est dans SF-120-07
- Pas d'endpoint admin — viendra avec SF-120-08
- Pas de seeder d'articles de démo — les premiers articles arriveront via SF-120-02 ou seront créés à la main via DB en attendant

---

## Valeurs initiales

### Table `blog_article`

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `status` | DRAFT | Imposée à la création (par défaut). PUBLISHED requiert action explicite (SF-120-05 ou SF-120-08). |
| `created_at` | NOW() | Renseigné automatiquement par la base |
| `updated_at` | NOW() | Renseigné automatiquement, mis à jour à chaque UPDATE |
| `published_at` | NULL | Renseigné uniquement quand l'article passe en PUBLISHED |
| `author_name` | "Franck Tounga" | Imposée par le métier (cf. F-120 décisions) |
| `author_url` | `https://www.linkedin.com/in/franck-tounga-51a15268/` | Imposée par le métier (URL LinkedIn fondateur) |

### Table `blog_topic`

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `status` | AVAILABLE | À la création |
| `used_at` | NULL | Renseigné uniquement quand status passe à USED |
| `article_id` | NULL | Renseigné quand status passe à USED, référence l'article généré |
| `created_at` | NOW() | Auto |

---

## Contraintes de validation

### Table `blog_article`

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `slug` | Oui | 200 | ASCII, kebab-case, regex `^[a-z0-9]+(-[a-z0-9]+)*$` | **Oui** (DB + JPA) | trim + lowercase |
| `title` | Oui | 500 | Texte non vide après trim | Non | trim |
| `subtitle` | Non | 1000 | Texte | Non | trim |
| `body_markdown` | Oui | — (TEXT) | Markdown non vide | Non | — |
| `hero_image_url` | Non (créé NULL) | 500 | URL HTTPS | Non | — |
| `hero_image_alt` | Non | 500 | Texte | Non | trim |
| `country` | Oui | 10 | `FR`, `BE` | Non | uppercase |
| `legal_domain` | Oui | 30 | `DROIT_DU_TRAVAIL`, `DROIT_IMMIGRATION`, `DROIT_FAMILLE` | Non | — |
| `author_name` | Oui | 200 | Texte | Non | trim |
| `author_url` | Non | 500 | URL HTTPS | Non | — |
| `meta_title` | Oui | 70 | Texte (limite SEO) | Non | trim |
| `meta_description` | Oui | 160 | Texte (limite SEO) | Non | trim |
| `reading_time_minutes` | Oui | — | Entier ≥ 1 | Non | — |
| `status` | Oui | 20 | `DRAFT`, `PUBLISHED`, `UNPUBLISHED` | Non | — |
| `topic_id` | Oui | — | UUID | Non | FK vers `blog_topic.id` |

### Table `blog_topic`

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `slug` | Oui | 200 | ASCII kebab-case | **Oui** | trim + lowercase |
| `title` | Oui | 500 | Texte | Non | trim |
| `description` | Non | — (TEXT) | Texte | Non | — |
| `category` | Oui | 30 | `DROIT_DU_TRAVAIL`, `DROIT_IMMIGRATION`, `DROIT_FAMILLE` | Non | — |
| `country_scope` | Oui | 10 | `FR`, `BE`, `FR_BE` (sujet transverse) | Non | — |
| `status` | Oui | 20 | `AVAILABLE`, `RESERVED`, `USED` | Non | — |

Notes :
- `slug` unique sur `blog_article` ET sur `blog_topic` (deux contraintes indépendantes)
- Relation `blog_article.topic_id → blog_topic.id` : `ON DELETE RESTRICT` (interdit la suppression d'un topic utilisé)

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum | Notes |
|---------|-----|------|-------------|-------|
| GET | `/api/v1/blog/articles` | Non | — (anonyme) | Filtres `country`, `legalDomain`, pagination |
| GET | `/api/v1/blog/articles/{slug}` | Non | — (anonyme) | 404 si non PUBLISHED |
| GET | `/api/v1/blog/articles/sitemap-fragment` | Non | — (anonyme) | Pour SF-120-07 |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `blog_article` | CREATE (migration 078) + SELECT (lecture) | Nouvelle table |
| `blog_topic` | CREATE (migration 079) + SELECT (lecture) | Nouvelle table |

### Migration Liquibase

- [x] Oui — `078-create-blog-topic.xml` + `079-create-blog-article.xml`
- Les deux migrations sont créées dans cette SF (079 dépend de la FK vers 078 — topic en premier)

### Composants Angular (si applicable)

- Aucun dans cette SF — frontend dans SF-120-06

### Configuration Spring Security

Modification `SecurityConfig` :
```java
.requestMatchers(HttpMethod.GET, "/api/v1/blog/**").permitAll()
```
À placer avant les `.anyRequest().authenticated()` pour ne pas exiger d'auth sur les endpoints publics du blog.

---

## Plan de test

### Tests unitaires

- [ ] `BlogArticleQueryService.findPublished(...)` — cas nominal : filtres + pagination + tri
- [ ] `BlogArticleQueryService.findPublished(...)` — pays seul, domaine seul, sans filtre
- [ ] `BlogArticleQueryService.findBySlug(...)` — cas nominal : article PUBLISHED retourné
- [ ] `BlogArticleQueryService.findBySlug(...)` — DRAFT → empty (pas de fuite)
- [ ] `BlogArticleQueryService.findBySlug(...)` — UNPUBLISHED → empty
- [ ] `BlogArticleQueryService.findBySlug(...)` — slug inconnu → empty
- [ ] `BlogArticleQueryService.sitemapEntries()` — retourne uniquement PUBLISHED, ordre `publishedAt DESC`
- [ ] `BlogArticleMapper` — transforme entité en SummaryResponse + DetailResponse correctement
- [ ] Validation Bean : slug invalide (caractères spéciaux) → erreur de contrainte
- [ ] Validation Bean : country hors enum → erreur

### Tests d'intégration

- [ ] `GET /api/v1/blog/articles` → 200 sans auth, body Page valide
- [ ] `GET /api/v1/blog/articles?country=FR` → 200, filtre appliqué, pas d'articles BE
- [ ] `GET /api/v1/blog/articles?legalDomain=DROIT_DU_TRAVAIL` → 200, filtre appliqué
- [ ] `GET /api/v1/blog/articles?country=FR&legalDomain=DROIT_DU_TRAVAIL` → 200, deux filtres cumulés
- [ ] `GET /api/v1/blog/articles?country=INVALID` → 400
- [ ] `GET /api/v1/blog/articles?size=100` → 200, size borné à 50
- [ ] `GET /api/v1/blog/articles?page=-1` → 400
- [ ] `GET /api/v1/blog/articles/{slug-published}` → 200, body BlogArticleDetailResponse complet
- [ ] `GET /api/v1/blog/articles/{slug-draft}` → 404 (pas de fuite)
- [ ] `GET /api/v1/blog/articles/{slug-unpublished}` → 404
- [ ] `GET /api/v1/blog/articles/{slug-inconnu}` → 404
- [ ] `GET /api/v1/blog/articles/sitemap-fragment` → 200, contient uniquement les PUBLISHED

### Isolation workspace

- [x] **Non applicable** — raison : le blog est du contenu public partagé entre tous les workspaces (et même les visiteurs anonymes). Pas de colonne `workspace_id` sur `blog_article` ni `blog_topic`.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Auth / Principal** — modification `SecurityConfig` pour autoriser GET `/api/v1/blog/**` sans authentification
- [ ] Workspace context — non touché
- [ ] Plans / limites — non touché
- [ ] Navigation / routing frontend — non applicable (pas de FE dans cette SF)

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `SecurityConfig` | Ajout d'une règle `permitAll()` sur GET `/api/v1/blog/**`. Risque : casser l'ordre des matchers, exposer involontairement d'autres routes | IT existants Spring Security + smoke `auth.spec.ts` (login, logout, redirect non-authentifié) |
| Tous les endpoints authentifiés (auth requise) | Aucun changement attendu, mais à vérifier qu'aucun endpoint existant ne devient anonyme par effet de bord | Smoke `auth.spec.ts` couvre les redirects |

### Smoke tests E2E concernés

- [x] `e2e/smoke/auth.spec.ts` — login local, login OAuth, logout, redirect non-authentifié — **doit passer sans régression** après modif SecurityConfig
- [ ] `e2e/smoke/workspace.spec.ts` — non concerné (le blog n'a pas de notion de workspace)
- [ ] `e2e/smoke/navigation.spec.ts` — non concerné (pas de routing FE dans cette SF)

---

## Dépendances

### Subfeatures bloquantes

- Aucune — c'est la première SF de F-120

### Questions ouvertes impactées

- [x] Aucune question bloquante — vérification effectuée dans le découpage F-120 (Provider LLM tranché Anthropic, RabbitMQ tranché async, S3 tranché stockage)

---

## Notes et décisions

- **Slug unique au niveau global** (pas par pays/domaine) : un slug identifie un article unique, pas une combinaison. Cela évite les ambiguïtés sur les URLs et simplifie les redirections futures.
- **Statuts UNPUBLISHED conservés** : un article dépublié n'est pas supprimé physiquement, il passe en `UNPUBLISHED` (soft delete). Permet le bouton "dépublier 1-clic" (SF-120-08) avec retour HTTP 410 Gone propre pour Google (à implémenter dans SF-120-06/07 côté frontend/seo).
- **Pas d'historique de versions** dans cette SF : un article publié est figé. Si une coquille doit être corrigée, ce sera via SF-120-08 (édition admin). Le champ `updated_at` permettra de détecter une modif post-publication pour la balise `<lastmod>` du sitemap.
- **`reading_time_minutes` calculé à la création** par le service de génération (SF-120-02), formule simple : `max(1, ceil(wordCount / 200))`. Stocké en colonne pour ne pas recalculer à chaque lecture.
- **Convention package** : `fr.ailegalcase.blog.{controller,service,repository,entity,dto,mapper}` — suit la convention des autres modules.
- **Pas de cache HTTP** dans cette SF (pas de `Cache-Control`, `ETag`). Pourra être ajouté en optimisation post-SF-120-07 si besoin.
