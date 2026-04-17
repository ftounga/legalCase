# Mini-spec — F-120 / SF-120-02 Sélecteur de sujets avec quota glissant + seed initial

## Identifiant

`F-120 / SF-120-02`

## Feature parente

`F-120` — Blog SEO automatisé (auto-publication d'articles juridiques FR + BE)

## Statut

`draft`

## Date de création

2026-04-17

## Branche Git

`feat/SF-120-02-selecteur-sujets`

---

## Objectif

Fournir le moteur de sélection de sujets (`TopicSelectorService`) qui, appelé par la SF-120-03 (génération d'article) ou SF-120-05 (scheduler), retourne le prochain topic à traiter en respectant le quota éditorial 60/25/15 (travail/immigration/famille) et l'équilibre 70/30 (FR/BE) sur une fenêtre glissante 4 semaines, tout en garantissant l'idempotence via un verrouillage de statut.

---

## Comportement attendu

### Cas nominal

`TopicSelectorService.selectNext()` :

1. **Calcule le déficit** du quota éditorial sur la fenêtre glissante 4 semaines (nombre d'articles `USED` par `(category, country_scope)`).
2. **Choisit la catégorie** avec le plus grand déficit relatif (vs cible 60/25/15).
3. **Choisit le pays** avec le plus grand déficit relatif (vs cible 70/30 FR/BE).
4. **Sélectionne le premier topic `AVAILABLE`** matchant `(category, country_scope)` trié par `created_at ASC` (FIFO).
5. **Verrouille** le topic : `UPDATE blog_topics SET status='RESERVED', used_at=NOW() WHERE id=? AND status='AVAILABLE'` (conditionnel — évite la race).
6. **Retourne** le topic réservé (UUID + slug + title + category + country_scope + description).
7. **L'appelant** (SF-120-03) a la responsabilité de passer `RESERVED → USED` après génération réussie, ou `RESERVED → AVAILABLE` en cas d'échec (opération expliquée dans SF-120-03 — cette SF expose juste les méthodes `markAsUsed(topicId, articleId)` et `releaseReservation(topicId)`).

### Cas d'erreur

| Situation | Comportement attendu | Comportement retour |
|-----------|---------------------|---------------------|
| Aucun topic `AVAILABLE` pour (category, country_scope) cible | Fallback : retente avec la 2ème catégorie en déficit, puis la 3ème, puis n'importe quel topic AVAILABLE | Retourne `Optional.empty()` si zéro topic AVAILABLE en base |
| Race condition : 2 appels simultanés choisissent le même topic | L'`UPDATE` conditionnel échoue pour le second (rowsAffected=0) → retry de la sélection avec les topics restants | Comportement transparent — idempotence garantie |
| `markAsUsed(topicId)` appelé sur un topic non `RESERVED` | `IllegalStateException` | Cas anormal — génération orpheline, à logguer |
| `markAsUsed(topicId, articleId)` avec `articleId` pointant sur un article qui n'existe pas | Erreur de contrainte FK Postgres | 500 côté appelant |
| `releaseReservation(topicId)` sur un topic non `RESERVED` | No-op silencieux (statut reste) | Idempotent |
| Topic `RESERVED` depuis > 1 heure sans `markAsUsed`/`release` | Cron de nettoyage (hors scope — SF-120-05) | — |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — Non applicable. Le sélecteur de sujets est un moteur interne du blog, pas un outil décisionnel par dossier.
- [x] **Autres pays** — France + Belgique couverts dès cette SF via la colonne `country_scope` + quota 70/30.
- [x] **Autres domaines** — Les 3 domaines V1 (travail, immigration, famille) couverts via `category` + quota 60/25/15.
- [x] **Autres UI patterns** — Non applicable (pas de frontend dans cette SF).
- [x] **Autres flows transversaux** — Aucun (service interne pur, pas d'endpoint, pas d'auth modifiée).

### Niveaux de vérification couverts

- [x] **Modèle TypeScript / API exposée** — Non applicable (aucune API exposée dans cette SF)
- [x] **Record / DTO backend** — `TopicSelection` (record interne avec le topic verrouillé)
- [x] **Service / logique métier** — `TopicSelectorService` + `TopicSeeder`
- [x] **Entité JPA + schéma DB** — `BlogTopic` (créée en SF-120-01, étendue ici avec la contrainte d'idempotence sur l'UPDATE conditionnel)
- [x] **Tests existants** — `BlogTopicRepository` existant sera étendu (méthodes de requête)

### Cas spécifique : nouveau pattern UI ou service partagé

Cette SF introduit un nouveau service interne `TopicSelectorService`. Scan effectué :

- [x] **Où le pattern pourrait-il être réutilisé ?** Aucun équivalent — c'est un mécanisme spécifique au blog SEO (quota éditorial sur fenêtre glissante). Aucun autre module n'a besoin de ce pattern en V1.
- [x] **Patterns concurrents ?** Aucun — il n'existe aucun autre mécanisme de sélection pondérée dans le projet.
- [x] **Le service peut-il servir à d'autres features ?** Non — il est couplé au modèle `BlogTopic` et aux règles éditoriales F-120. Pas de généralisation prématurée.
- [x] **Équivalent design existant ?** Non.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Domaines (Travail / Immigration / Famille) | Oui | Intégré dès cette SF via quota 60/25/15 |
| Pays (FR / BE) | Oui | Intégré dès cette SF via quota 70/30 |
| Auth | Non | Service interne, aucune exposition |
| Workspace context | Non | Contenu public, pas de `workspace_id` (cf. SF-120-01) |
| Outils décisionnels | Non | Moteur blog, pas un outil dossier |
| Frontend | Non | Cible de SF-120-06 |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (domaines, pays)
- [x] Pas de cible restante nécessitant une SF parallèle

---

## Critères d'acceptation

- [ ] Migration Liquibase `080-seed-blog-topics.xml` insère 25 topics initiaux (15 FR + 10 BE) couvrant les 3 domaines avec le ratio 60/25/15 — liste des 25 validée avec le product owner
- [ ] Classe `TopicSelectorService` avec méthode `selectNext(): Optional<TopicSelection>` qui applique la logique de quota glissant 4 semaines
- [ ] La méthode `selectNext()` respecte la priorité : (1) catégorie la plus en déficit vs 60/25/15, puis (2) pays le plus en déficit vs 70/30
- [ ] Fallback si aucun topic `AVAILABLE` dans la combinaison prioritaire : essayer la 2ème catégorie, puis la 3ème, puis n'importe quel `AVAILABLE`
- [ ] Retourne `Optional.empty()` si aucun topic `AVAILABLE` existe en base
- [ ] Verrouillage transactionnel : `UPDATE ... WHERE id=? AND status='AVAILABLE'` avec `rowsAffected > 0` comme condition d'acquisition
- [ ] Méthode `markAsUsed(topicId, articleId)` : passe `RESERVED → USED`, renseigne `article_id`
- [ ] Méthode `releaseReservation(topicId)` : passe `RESERVED → AVAILABLE`, remet `used_at=NULL` (idempotent si statut déjà ≠ RESERVED)
- [ ] Tests unitaires couvrent : quota sur fenêtre glissante, fallback catégorie, fallback pays, race condition (2 threads concurrents), cas vide
- [ ] Tests d'intégration sur `BlogTopicRepository` : transitions de statut
- [ ] La migration 080 est idempotente (skip si les topics existent déjà — via changeSet avec `preConditions`)
- [ ] Logs INFO pour chaque sélection : `"Topic sélectionné : {slug} (category={cat}, country={co}, quotaDeficitCategory={X%}, quotaDeficitCountry={Y%})"`

---

## Périmètre

### Hors scope (explicite)

- Aucun appel IA dans cette SF (pas de Claude, pas d'OpenAI) — la génération d'article est dans SF-120-03
- Aucun endpoint REST exposé — service interne uniquement (l'appelant sera SF-120-03 ou SF-120-05)
- Aucune interface admin — viendra avec SF-120-08 (CRUD topics par super-admin)
- Pas de cron de nettoyage des réservations orphelines — sera dans SF-120-05
- Pas de métriques/dashboard (nombre d'articles générés par domaine, taux d'utilisation) — viendra avec SF-120-08
- Pas de pondération fine (ex. boost sur topics récemment ajoutés, recency bias) — FIFO strict par `created_at ASC`

---

## Valeurs initiales

### Seed topics (migration 080)

25 topics insérés avec les valeurs suivantes :

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `id` | `gen_random_uuid()` | Généré par Postgres |
| `status` | `AVAILABLE` | Tous les topics seed démarrent disponibles |
| `article_id` | NULL | Pas encore consommé |
| `used_at` | NULL | Pas encore consommé |
| `created_at` | `NOW()` | Auto |

Composition des 25 topics :

| Domaine | Quota cible | FR | BE | Total |
|---------|-------------|----|----|-------|
| DROIT_DU_TRAVAIL | 60% | 9 | 6 | 15 |
| DROIT_IMMIGRATION | 25% | 4 | 2 | 6 |
| DROIT_FAMILLE | 15% | 3 | 1 | 4 |
| **Total** | **100%** | **15 (60%)** | **10 (40%)** | **25** |

> Note : la composition du seed initial sacrifie légèrement le ratio 70/30 (60/40 effectif) pour s'assurer qu'il y a au moins 2 topics belges par domaine. Le quota 70/30 se rééquilibrera naturellement avec l'ajout de topics FR ultérieurs (SF-120-08 admin).

---

## Contraintes de validation

### Topic seed (contraintes déjà posées par SF-120-01)

Les contraintes de validation sont celles posées dans SF-120-01 (cf. mini-spec SF-120-01 section "Contraintes de validation"). Cette SF ne modifie pas le schéma — uniquement une migration de données.

### Nouveaux paramètres techniques (pas d'enum utilisateur)

| Paramètre | Valeur | Règle |
|-----------|--------|-------|
| Fenêtre glissante | 4 semaines (28 jours) | Fixe dans cette SF — viendra en config applicative si besoin de l'ajuster |
| Quota catégorie cible | 60 / 25 / 15 (DT / IM / FA) | Fixe, en constantes Java |
| Quota pays cible | 70 / 30 (FR / BE) | Fixe, en constantes Java |

---

## Technique

### Endpoint(s)

Aucun endpoint dans cette SF. Service interne uniquement.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `blog_topics` | INSERT (seed — 25 lignes) + SELECT + UPDATE (transitions de statut) | Pas de nouvelles colonnes — réutilise le schéma SF-120-01 |
| `blog_articles` | SELECT (pour compter les articles `USED` sur fenêtre glissante par catégorie/pays) | Read-only |

### Migration Liquibase

- [x] Oui — `080-seed-blog-topics.xml`
- Insère 25 topics avec `preConditions` : skip si `blog_topics` n'est pas vide (idempotent)
- Pas de changement de schéma

### Composants Angular

- Aucun dans cette SF — frontend dans SF-120-06

### Classes Spring à créer

| Classe | Package | Rôle |
|--------|---------|------|
| `TopicSelectorService` | `fr.ailegalcase.blog.service` | Logique de sélection avec quota + transitions de statut |
| `TopicSelection` (record) | `fr.ailegalcase.blog.service` | DTO interne (topic verrouillé) |
| `QuotaCalculator` (package-private) | `fr.ailegalcase.blog.service` | Calcul des déficits par fenêtre glissante |

### Extension `BlogTopicRepository`

Ajouter les méthodes :

```java
// Sélection candidate par catégorie + pays, FIFO
Optional<BlogTopic> findFirstByStatusAndCategoryAndCountryScopeOrderByCreatedAtAsc(
    BlogTopicStatus status, String category, String countryScope);

// Fallback : n'importe quel topic AVAILABLE
Optional<BlogTopic> findFirstByStatusOrderByCreatedAtAsc(BlogTopicStatus status);

// UPDATE conditionnel (verrouillage optimiste)
@Modifying
@Query("UPDATE BlogTopic t SET t.status = :newStatus, t.usedAt = :usedAt " +
       "WHERE t.id = :id AND t.status = :expectedStatus")
int compareAndSwapStatus(UUID id, BlogTopicStatus expectedStatus, BlogTopicStatus newStatus, Instant usedAt);
```

### Extension `BlogArticleRepository`

Ajouter la méthode :

```java
// Compte par catégorie + pays sur la fenêtre glissante
@Query("SELECT t.category AS category, t.countryScope AS countryScope, COUNT(a) AS count " +
       "FROM BlogArticle a JOIN BlogTopic t ON a.topicId = t.id " +
       "WHERE a.status = 'PUBLISHED' AND a.publishedAt >= :since " +
       "GROUP BY t.category, t.countryScope")
List<CategoryCountryCount> countPublishedSince(Instant since);
```

---

## Plan de test

### Tests unitaires

- [ ] `QuotaCalculator.computeCategoryDeficit(...)` — retourne déficit correct sur 10 articles (6 DT / 3 IM / 1 FA → 0, 25%→25%→50%)
- [ ] `QuotaCalculator.computeCountryDeficit(...)` — retourne déficit correct (8 FR / 2 BE → FR=−14%, BE=−20% → prioriser BE)
- [ ] `QuotaCalculator` — cas 0 article (tout en déficit → ordre lexicographique ou défaut)
- [ ] `TopicSelectorService.selectNext()` — cas nominal : DT/FR déficit max → retourne topic DT/FR
- [ ] `TopicSelectorService.selectNext()` — fallback catégorie : aucun DT/FR AVAILABLE → essaie IM/FR
- [ ] `TopicSelectorService.selectNext()` — fallback pays : aucun DT/FR, mais DT/BE AVAILABLE → retourne DT/BE
- [ ] `TopicSelectorService.selectNext()` — base vide → `Optional.empty()`
- [ ] `TopicSelectorService.selectNext()` — verrouillage réussi (rowsAffected=1)
- [ ] `TopicSelectorService.selectNext()` — verrouillage échoué (rowsAffected=0 — race simulée) → retente avec le topic suivant
- [ ] `TopicSelectorService.markAsUsed(topicId, articleId)` — cas nominal : RESERVED → USED
- [ ] `TopicSelectorService.markAsUsed(topicId, articleId)` — topic non RESERVED → `IllegalStateException`
- [ ] `TopicSelectorService.releaseReservation(topicId)` — cas nominal : RESERVED → AVAILABLE, used_at=NULL
- [ ] `TopicSelectorService.releaseReservation(topicId)` — topic non RESERVED → no-op silencieux

### Tests d'intégration

- [ ] Migration 080 chargée : `blog_topics` contient 25 lignes avec le bon ratio catégorie/pays
- [ ] Migration 080 idempotente : rejoue sur une base déjà seedée ne duplique pas
- [ ] `TopicSelectorService.selectNext()` avec seed chargé → retourne un topic `AVAILABLE`, le fait passer `RESERVED`, vérifie persistence
- [ ] Test de concurrence : 2 threads appellent `selectNext()` en parallèle → chacun obtient un topic distinct (aucune race)
- [ ] `TopicSelectorService.markAsUsed(...)` persiste correctement (article_id + status=USED)
- [ ] `TopicSelectorService.releaseReservation(...)` restaure correctement (article_id=NULL + status=AVAILABLE + used_at=NULL)

### Isolation workspace

- [x] **Non applicable** — raison : le blog est du contenu public partagé (cf. SF-120-01). Pas de colonne `workspace_id` sur `blog_topics` ni `blog_articles`.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — non touché (service interne)
- [ ] Workspace context — non touché
- [ ] Plans / limites — non touché (pas de quota utilisateur, seulement un quota éditorial interne)
- [ ] Navigation / routing frontend — non applicable (pas de FE)
- [x] **Aucune préoccupation transversale** — subfeature isolée au module `blog`

### Composants / endpoints existants potentiellement impactés

Aucun. Cette SF :
- Ajoute un service et des méthodes de repository
- N'étend pas `SecurityConfig`
- Ne touche pas aux endpoints publics existants du blog (SF-120-01)
- N'impacte aucun autre module

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — cette SF n'impacte pas les flows utilisateur (auth, navigation, workspace).

---

## Dépendances

### Subfeatures bloquantes

- `SF-120-01` — statut : **done** ✅ (fournit le schéma `blog_topics` et `blog_articles`, les entités JPA, les repositories de base)

### Questions ouvertes impactées

- [x] Aucune question bloquante de `docs/OPEN_QUESTIONS.md` impactée (vérifié)

---

## Notes et décisions

### Algorithme de quota — calcul du déficit

Sur une fenêtre glissante de 28 jours (`now - 4 semaines`), compter `N` articles `PUBLISHED` par couple `(category, country_scope)`.

**Déficit catégorie** : `deficitCat(c) = cibleCat(c) - (N(c) / totalArticles)`
- Où `cibleCat(DT)=0.60`, `cibleCat(IM)=0.25`, `cibleCat(FA)=0.15`
- La catégorie avec le déficit positif le plus grand est prioritaire
- Si aucune catégorie n'est en déficit (quota respecté ou dépassé partout), choisir celle qui a le ratio `N(c) / totalArticles` le plus bas (la plus en retard en valeur absolue)

**Déficit pays** (à déficit catégorie figé) : `deficitCo(p, c) = cibleCo(p) - (N(c,p) / N(c))`
- Où `cibleCo(FR)=0.70`, `cibleCo(BE)=0.30`
- Calcul après le choix de catégorie

### Choix d'architecture

- **Service interne, pas d'endpoint** : le sélecteur n'est jamais appelé par l'utilisateur final. L'appelant (SF-120-03 générateur, SF-120-05 scheduler) est un composant interne. Exposer un endpoint REST créerait une attaque surface sans valeur ajoutée.
- **UPDATE conditionnel plutôt que `SELECT FOR UPDATE`** : évite les verrous Postgres prolongés, plus scalable. Le `compareAndSwap` est suffisant car un seul thread gagne la course.
- **Fenêtre glissante 4 semaines en dur** : pour simplifier. Si besoin de l'ajuster, passer en `@Value("${blog.quota.window-days}")` dans une SF future.
- **Pas de `recency bias`** : FIFO strict par `created_at ASC`. Le product owner peut ajuster la priorité manuellement dans SF-120-08 (admin).
- **Statut `RESERVED` sans timeout** : un topic RESERVED depuis trop longtemps sans `markAsUsed`/`release` reste bloqué. Le nettoyage sera un cron dans SF-120-05 (`@Scheduled` qui release les RESERVED > 1h).

### Seed initial — liste des 25 topics proposés

**Droit du travail — France (9)**
1. `rupture-conventionnelle-qui-prend-en-charge-l-avocat` — Rupture conventionnelle : qui prend en charge les frais d'avocat ?
2. `indemnite-licenciement-bareme-macron-2026` — Indemnités de licenciement : comment utiliser le barème Macron en 2026
3. `clause-non-concurrence-conditions-validite` — Clause de non-concurrence : les 5 conditions de validité à vérifier
4. `licenciement-economique-procedure-2026` — Licenciement économique : la procédure étape par étape (2026)
5. `harcelement-moral-preuve-prudhommes` — Harcèlement moral : comment constituer un dossier solide aux prud'hommes
6. `cse-consultation-obligatoire-licenciement` — Consultation du CSE : les cas obligatoires avant un licenciement
7. `preavis-licenciement-calcul-duree` — Calcul du préavis de licenciement : règles et exceptions
8. `salaire-reference-indemnite-primes` — Salaire de référence pour l'indemnité : comment traiter les primes variables
9. `transaction-apres-licenciement-avantages-risques` — Transaction après licenciement : avantages, risques, pièges à éviter

**Droit du travail — Belgique (6)**
10. `licenciement-motif-grave-belgique` — Licenciement pour motif grave en Belgique : procédure et délais
11. `cct-109-motivation-licenciement` — CCT 109 : obligation de motivation du licenciement en Belgique
12. `preavis-licenciement-belgique-calcul-2026` — Calcul du préavis en Belgique : barème 2026 complet
13. `indemnite-licenciement-manifestement-deraisonnable` — Indemnité pour licenciement manifestement déraisonnable (3 à 17 semaines)
14. `protection-delegue-syndical-belgique` — Protection spéciale des délégués syndicaux : que risque l'employeur ?
15. `rupture-amiable-vs-licenciement-belgique` — Rupture amiable ou licenciement : quel choix stratégique en Belgique ?

**Droit de l'immigration — France (4)**
16. `titre-sejour-salarie-changement-employeur` — Titre de séjour salarié : changer d'employeur sans perdre son droit au travail
17. `refus-titre-sejour-recours-hierarchique-juridique` — Refus de titre de séjour : recours hiérarchique ou recours juridictionnel ?
18. `obligation-quitter-territoire-delai-recours` — OQTF : délais, recours et suspensif devant le TA
19. `regularisation-travail-salarie-circulaire-valls` — Régularisation par le travail : conditions de la circulaire Valls 2026

**Droit de l'immigration — Belgique (2)**
20. `permis-unique-belgique-procedure-2026` — Permis unique de travail en Belgique : procédure actualisée 2026
21. `regroupement-familial-belgique-conditions` — Regroupement familial en Belgique : les conditions qui changent en 2026

**Droit de la famille — France (3)**
22. `divorce-consentement-mutuel-avocat-separe` — Divorce par consentement mutuel : pourquoi 2 avocats sont obligatoires
23. `partage-communaute-biens-expertise-immobiliere` — Partage de communauté : gérer une expertise immobilière contestée
24. `prestation-compensatoire-calcul-criteres-2026` — Prestation compensatoire : 5 critères du juge en 2026

**Droit de la famille — Belgique (1)**
25. `divorce-separation-de-fait-belgique` — Divorce par séparation de fait en Belgique : comment prouver la rupture

> Les 25 slugs respectent la regex `^[a-z0-9]+(-[a-z0-9]+)*$` imposée par SF-120-01.
> Titres ≤ 100 caractères, parlants pour le SEO, orientés B2B avocats.
> Les descriptions courtes seront générées par la suite (pas obligatoires dans le seed — `description` est nullable).
