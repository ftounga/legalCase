# Mini-spec — [F-245 / SF-245-01] Isolation et déterminisme du harnais de tests d'intégration

> Template : copier ce fichier, renommer en `SF-XX-YY-nom.md`, placer dans `docs/features/FEAT-XX/`
> Ce document doit être validé AVANT de démarrer le dev.

---

## Identifiant

`F-245 / SF-245-01`

## Feature parente

`F-245` — Hygiène du harnais de tests d'intégration — isolation de la base H2 entre IT

## Statut

`ready`

## Date de création

2026-05-18

## Branche Git

`feat/SF-245-01-isolation-harnais-it`

---

## Découpage de F-245

F-245 est livrée en **deux subfeatures**. Le découpage a été figé en cours de dev,
l'hypothèse initiale (« tous les échecs du `verify` global sont dus à la pollution
croisée ») ayant été **falsifiée par l'exécution** : une fois l'isolation en place et le
`verify` rendu déterministe, **49 tests d'intégration restent en échec — non à cause de
la pollution mais parce qu'ils sont obsolètes sur `master`** (colonnes devenues
`NOT NULL` non renseignées, bean manquant dans un slice `@DataJpaTest`, assertions
désynchronisées). La CI ne lance que `mvnw test` (surefire), jamais `verify` (failsafe) :
ces 49 IT ont donc pourri sans alerte.

- **SF-245-01** (cette SF) — **isolation déterministe du harnais**. Profil de test dédié
  + nettoyage automatique de la base entre tests. Résultat : `mvnw verify` devient
  **déterministe** ; il révèle un ensemble stable et inventorié de 49 échecs IT
  pré-existants. Cette SF ne corrige **aucun** test existant — elle rend le harnais
  fiable, condition préalable indispensable pour corriger les tests de façon sûre.
- **SF-245-02** — **correction des 49 IT obsolètes** révélés, et bascule de la CI de
  `mvnw test` vers `mvnw verify` (pour que les IT ne puissent plus pourrir). Résultat :
  `mvnw verify` **100 % vert**.

Découper ainsi sépare un changement d'infrastructure homogène et à faible risque
(SF-245-01) du travail hétérogène de correction tests par tests (SF-245-02), qui touche
~15 fichiers et peut faire émerger de vrais bugs produit nécessitant leur propre
traitement.

---

## Objectif

> En une phrase : que fait cette subfeature ?

Rendre l'exécution de `./mvnw verify` **déterministe** en isolant les tests d'intégration
les uns des autres — profil de test dédié + base H2 nettoyée automatiquement avant chaque
test — afin de supprimer la pollution croisée et les cascades non reproductibles, sans
dépendre du nettoyage manuel partiel et fragile fait dans chaque IT.

---

## Comportement attendu

### Cas nominal

> Description précise du flux principal (entrée → traitement → sortie).

**Diagnostic de l'existant.** Les 171 fichiers `*IT.java` sont exécutés par
`maven-failsafe-plugin` (sans `<configuration>` → `forkCount=1`, `reuseForks=true`) :
**tous les IT tournent dans un seul fork JVM**. Le profil actif par défaut (`application.yml`
→ `spring.profiles.active: dev`) pointe la base H2 sur
`jdbc:h2:mem:legalcasedb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` : une base mémoire
**nommée et partagée**, maintenue vivante pour toute la durée de la JVM (`DB_CLOSE_DELAY=-1`).
Conséquence : les 171 IT écrivent dans la **même** base, jamais réinitialisée.

Seuls 30 IT sur 171 nettoient la base dans un `@BeforeEach` (`repository.deleteAll()`),
chacun avec un sous-ensemble de tables différent et un ordre de suppression différent.
Les IT qui ne nettoient pas laissent des lignes derrière eux ; les données s'accumulent
de test en test (jamais libérées à cause de `DB_CLOSE_DELAY=-1`), provoquant violations
de contraintes uniques / FK dans les `@BeforeEach` voisins, puis propagation en cascade
(échec de chargement de contexte) au-delà de ~233 IT du même fork. Les tests par SF
passent isolément ; le `mvnw verify` global ne passe pas.

**Comportement cible.**

1. **Profil de test dédié `test`.** Un fichier `src/test/resources/application-test.yml`
   définit explicitement la configuration de test : base H2 mémoire isolée
   (`jdbc:h2:mem:legalcase-test;...`), `ddl-auto: none`, Liquibase actif, identifiants
   OAuth2 / Anthropic factices, mail désactivé. Le profil `test` est **forcé pour toute la
   phase de test** via `maven-surefire-plugin` et `maven-failsafe-plugin`
   (`<systemPropertyVariables><spring.profiles.active>test</spring.profiles.active></...>`).
   Aucun des 171 fichiers IT n'est modifié : le forçage est centralisé dans `pom.xml`.

2. **Nettoyage déterministe entre chaque test.** Une extension JUnit 5
   `DatabaseCleanupExtension` (implémentant `BeforeEachCallback`) est **enregistrée
   automatiquement** pour tous les tests via `junit-platform.properties`
   (`junit.jupiter.extensions.autodetection.enabled=true`) + fichier de service
   `META-INF/services/org.junit.jupiter.api.extension.Extension`. Avant chaque test :
   - si le test n'a pas de contexte Spring (test unitaire pur) → no-op silencieux ;
   - sinon, l'extension récupère le `DataSource` du contexte Spring, puis exécute
     `SET REFERENTIAL_INTEGRITY FALSE` → `TRUNCATE TABLE` de **toutes** les tables
     applicatives (découvertes dynamiquement via `INFORMATION_SCHEMA`, en excluant
     `DATABASECHANGELOG` / `DATABASECHANGELOGLOCK`) → `SET REFERENTIAL_INTEGRITY TRUE`.

   Chaque test démarre ainsi sur une base vide quel que soit l'état laissé par le test
   précédent. Le nettoyage est centralisé : il ne dépend plus du fait que chaque IT pense
   à appeler `deleteAll()` dans le bon ordre.

3. Le nettoyage se fait dans `BeforeEachCallback` (et non `AfterEach`) pour s'exécuter
   hors de toute transaction de test ouverte — évite tout conflit de verrou avec un
   `@DataJpaTest` transactionnel.

Les 30 `@BeforeEach { deleteAll() }` existants deviennent redondants mais restent
inoffensifs (idempotents sur une base déjà vide) ; ils ne sont **pas** retirés dans cette
SF (voir Hors scope).

### Cas d'erreur

> Lister tous les cas d'erreur identifiés.

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Test unitaire pur sans contexte Spring | `DatabaseCleanupExtension` détecte l'absence de contexte (`SpringExtension.getApplicationContext` indisponible) → no-op, aucun échec | — |
| `DataSource` non-H2 (profil mal résolu) | L'extension vérifie le produit de base (`H2`) ; sur une base non-H2, log d'avertissement et no-op plutôt que `TRUNCATE` hasardeux | — |
| Échec SQL pendant le `TRUNCATE` | Exception propagée → le test échoue explicitement (le bruit doit remonter, pas être masqué) | — |
| Table ajoutée par une migration future | Découverte dynamique via `INFORMATION_SCHEMA` → la nouvelle table est nettoyée sans modification du code de l'extension | — |

---

## Analyse de cohérence transversale

> Avant d'écrire les critères d'acceptation, scanner les cibles où le même mécanisme pourrait s'appliquer.

### Périmètres à scanner

- [x] **Autres outils métier** : non applicable — SF d'infrastructure de test, aucun outil métier touché.
- [x] **Autres pays** : non applicable — aucune logique dépendante du pays.
- [x] **Autres domaines** : non applicable — aucune logique métier.
- [x] **Autres UI patterns** : non applicable — aucun frontend touché.
- [x] **Autres flows transversaux** : le harnais de test couvre auth / workspace / navigation, mais le code applicatif n'est pas modifié — seul le harnais l'est.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Code applicatif backend (`src/main`) | Non | Aucune ligne de `src/main` modifiée — seuls `pom.xml` et `src/test` changent |
| Code frontend | Non | Aucun impact |
| Harnais de test (`src/test`) | Oui | Cœur de la SF : profil `test` + extension de nettoyage, appliqués globalement à tous les IT |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature — le mécanisme de
  nettoyage est global (auto-enregistré) : il couvre les 171 IT d'un coup, sans
  duplication ni cible orpheline.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF d'infrastructure de test pure. Aucun
  composant frontend, aucun endpoint, aucun outil décisionnel. Aucune ligne de
  `frontend/` ni de `backend/src/main/` n'est modifiée.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — justification : SF d'infrastructure de test. Aucun
  outil décisionnel, aucun formulaire, aucun champ saisissable.

---

## Critères d'acceptation

> Chaque critère est vérifiable. Pas d'ambiguïté.

- [x] **CA1** — Déterminisme : deux exécutions consécutives de `./mvnw verify` donnent
  exactement le même résultat (même nombre de tests exécutés, mêmes échecs). La cascade
  non reproductible (`ApplicationContext failure threshold`, échecs variables au-delà de
  ~233 IT) a disparu.
- [x] **CA2** — La CI backend (`./mvnw test`, phase surefire) reste **verte** : les 4269
  tests unitaires passent. Le niveau de log du profil `test` est identique au profil
  `dev` pour ne masquer aucun événement attendu par un test.
- [x] **CA3** — Le profil `test` est forcé pour `surefire` **et** `failsafe` ; aucun test
  n'écrit dans la base `legalcasedb` du profil `dev` ni dans une base PostgreSQL.
- [x] **CA4** — `DatabaseCleanupExtension` est enregistrée automatiquement (aucun
  `@ExtendWith` ajouté dans les fichiers de test) et ne fait pas échouer les tests
  unitaires purs (sans contexte Spring).
- [x] **CA5** — Les tables de référence seedées par Liquibase
  (`legal_referentials`, `decision_tool_visibility_rules`…) ne sont pas tronquées : les
  IT d'outils décisionnels qui les consultent ne régressent pas.
- [x] **CA6** — Aucun fichier de `backend/src/main/` n'est modifié ; aucun fichier
  `*IT.java` ni `*Test.java` **existant** n'est modifié. Les seuls fichiers de test
  ajoutés sont les 3 nouveaux fichiers `testsupport/` de cette SF.
- [x] **CA7** — La base de test est isolée test-à-test : un IT qui insère des lignes sans
  les nettoyer ne fait pas échouer l'IT suivant (prouvé par `DatabaseIsolationIT`).
- [ ] **CA8** *(hors SF-245-01 — assuré par SF-245-02)* — `./mvnw verify` se termine
  `BUILD SUCCESS`, 0 échec / 0 erreur. SF-245-01 livre un `verify` déterministe ;
  SF-245-02 corrige les 49 IT obsolètes que ce `verify` déterministe révèle.

---

## Échecs IT pré-existants révélés par le `verify` déterministe

Une fois l'isolation en place, `./mvnw verify` est déterministe et révèle **49 tests
d'intégration en échec, stables d'un run à l'autre**. Ces échecs **ne sont pas causés par
SF-245-01** : ce sont des tests obsolètes sur `master`, masqués jusqu'ici par le
caractère non déterministe et jamais-exécuté-en-CI du `verify`. Inventaire (corrigé par
**SF-245-02**) :

| Cause racine | Classes IT | Tests | Catégorie |
|---|---|---|---|
| `case_files.legal_domain` / `created_by_user_id` devenus `NOT NULL`, `setUp()` ne les renseigne pas | `AnalysisJobControllerIT`, `UsageEventControllerIT`, `AdminUsageControllerIT`, `AiQuestionControllerIT`, `SynthesisSearchControllerIT`, `ChunkingServiceIT` | 30 | test obsolète |
| `workspace.legal_domain` `NOT NULL` non renseigné | `WorkspaceInvitationControllerIT` | 2 | test obsolète |
| `@DataJpaTest` sans le bean `StripeCustomerService` | `WorkspaceServiceIT` | 4 | test obsolète |
| Assertions désynchronisées / à investiguer (dont possible faille d'isolation workspace sur `AnalysisStatusStreamController` — à challenger) | `RecherchePaterniteControllerIT`, `ReferentialControllerIT`, `AnalysisStatusStreamControllerIT`, `AncienneteControllerIT`, `CaseFileControllerIT`, `LegalReferentialDescriptionIntegrityIT`, `LocalLoginControllerIT`, `WorkspaceInvitationControllerIT` | 13 | à trier en SF-245-02 |

SF-245-02 triera chaque cas (test obsolète vs bug produit réel) et corrigera ; un
éventuel vrai bug produit sera escaladé séparément.

---

## Périmètre

### Hors scope (explicite)

- **Correction des 49 IT obsolètes** révélés par le `verify` déterministe → **SF-245-02**.
  SF-245-01 rend le harnais fiable ; corriger les tests sur un harnais non déterministe
  serait hasardeux. La correction est donc une SF distincte.
- **Bascule de la CI** de `mvnw test` vers `mvnw verify` → **SF-245-02** (une fois le
  `verify` vert, sans quoi la CI casserait).
- **Réécriture des `@SpringBootTest(properties = {...})`** pour fusionner les contextes
  Spring : c'est un sujet de **performance** (durée du `verify`), pas de correctness. Si,
  après cette SF, le `verify` est trop lent à cause de la fragmentation des contextes,
  une optimisation séparée sera ouverte au backlog. Cette SF vise la **fiabilité et le
  déterminisme**, pas la vitesse.
- **Suppression des `@BeforeEach { deleteAll() }` redondants** dans les 30 IT concernés :
  inoffensifs une fois le nettoyage centralisé en place, leur retrait est un nettoyage
  cosmétique différé (éviterait de toucher 30 fichiers et d'élargir la surface de revue
  / de risque de cette SF d'infra).
- **Migration vers Testcontainers / PostgreSQL de test** : H2 reste la base de test ;
  changer de moteur de base est hors périmètre.
- **Parallélisation des tests** (`forkCount > 1`) : hors périmètre — le déterminisme est
  obtenu en mono-fork ; la parallélisation est une optimisation séparée.

---

## Technique

### Endpoint(s)

Aucun — SF d'infrastructure de test.

### Tables impactées

Aucune table applicative modifiée. L'extension `TRUNCATE` toutes les tables du schéma de
**la base H2 de test** entre chaque test ; aucune migration, aucune table créée/altérée.

### Migration Liquibase

- [ ] Oui
- [x] Non applicable

### Fichiers créés / modifiés

| Fichier | Opération | Rôle |
|---------|-----------|------|
| `backend/pom.xml` | modifié | Forçage `spring.profiles.active=test` sur `surefire` + `failsafe` |
| `backend/src/test/resources/application-test.yml` | créé | Profil `test` : H2 isolée, identifiants factices, mail off |
| `backend/src/test/resources/junit-platform.properties` | créé | `junit.jupiter.extensions.autodetection.enabled=true` |
| `backend/src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension` | créé | Auto-enregistrement de `DatabaseCleanupExtension` |
| `backend/src/test/java/fr/ailegalcase/testsupport/DatabaseCleanupExtension.java` | créé | Extension JUnit 5 : `TRUNCATE` toutes les tables avant chaque test |
| `backend/src/test/java/fr/ailegalcase/testsupport/DatabaseCleanupExtensionTest.java` | créé | Tests unitaires de l'extension |
| `backend/src/test/java/fr/ailegalcase/testsupport/DatabaseIsolationIT.java` | créé | IT prouvant l'isolation test-à-test (CA7) |

### Composants Angular (si applicable)

Aucun.

---

## Plan de test

### Tests unitaires

- [ ] `DatabaseCleanupExtensionTest` — `beforeEach` sans contexte Spring (extension
  context vide) → aucune exception, aucun accès base.
- [ ] `DatabaseCleanupExtensionTest` — la liste des tables à tronquer exclut bien
  `DATABASECHANGELOG` et `DATABASECHANGELOGLOCK`.

### Tests d'intégration

- [x] `DatabaseIsolationIT` — **test A** insère des entités (`User`) sans nettoyage
  explicite ; **test B** (même classe) vérifie que la base est vide au démarrage → prouve
  le nettoyage `BeforeEach` (CA7). L'ordre des tests est fixé via
  `@TestMethodOrder(OrderAnnotation.class)`.
- [x] `./mvnw verify` complet exécuté deux fois → résultat identique (déterminisme, CA1).
- [x] Phase surefire (`./mvnw test`) → 4269 tests, 0 échec (CA2).

### Isolation workspace

- [x] Applicable — l'isolation testée ici est l'**isolation des tests entre eux** (la
  base, pas le workspace). L'isolation workspace métier reste couverte par les IT
  existants (`*ControllerIT` avec leurs assertions 403 cross-workspace) ; cette SF ne la
  modifie pas, elle la rend simplement fiable en supprimant la pollution croisée.
- [ ] Non applicable

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] **Auth / Principal**
- [ ] **Workspace context**
- [ ] **Plans / limites**
- [ ] **Navigation / routing frontend**
- [x] **Aucune préoccupation transversale** — la SF ne touche que le harnais de test
  (`pom.xml` phase test + `src/test`). Aucune ligne de code applicatif (`src/main`)
  n'est modifiée. Les préoccupations transversales restent couvertes par les IT
  existants, qui deviennent simplement fiables.

### Composants / endpoints existants potentiellement impactés

Aucun composant applicatif impacté. Le seul « consommateur » modifié est le harnais de
test lui-même : tous les `*IT.java` et `*Test.java` voient désormais une base nettoyée
avant chaque test. Risque de régression = un test qui dépendait *implicitement* de
données laissées par un test précédent (anti-pattern) ; ce cas est précisément ce que la
SF corrige et est détecté par le `mvnw verify` complet.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — justification : la SF ne modifie ni le code applicatif
  backend, ni le frontend, ni le déploiement. Les smoke tests E2E (`e2e/smoke/`) ciblent
  l'app déployée et ne sont pas affectés par le harnais d'IT Maven.

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` impactée.

---

## Notes et décisions

- **Choix de stratégie d'isolation.** Le diagnostic F-245 listait trois pistes :
  (a) rollback transactionnel systématique, (b) base H2 par classe de test,
  (c) `@DirtiesContext` ciblé. Retenu : **nettoyage `TRUNCATE` déterministe avant chaque
  test, via une extension JUnit auto-enregistrée**, pour les raisons suivantes :
  - (a) `@Transactional` sur les `@SpringBootTest`+`MockMvc` est fragile — les requêtes
    MockMvc traversent la pile web, les opérations `REQUIRES_NEW` et les flushs manuels
    cassent, et beaucoup d'IT asynchrones (pipeline IA) assertent sur un état committé.
    Un rollback global casserait une part importante des 171 IT.
  - (b) une base par classe n'élimine pas la pollution **entre tests d'une même classe**
    et multiplie les bases ; insuffisant seul.
  - (c) `@DirtiesContext` recharge le contexte Spring (lent, ~secondes par test) sans
    nettoyer les données — il traite la mauvaise cause.
  - Le `TRUNCATE`-avant-chaque-test traite la cause exacte (données résiduelles) au bon
    grain (le test), de façon centralisée (zéro fichier IT modifié) et rapide
    (`TRUNCATE` H2 mémoire est quasi instantané).
- **Pourquoi `BeforeEach` et non `AfterEach`.** Le nettoyage avant le test garantit un
  état propre indépendamment de l'ordre et des échecs ; il s'exécute hors de toute
  transaction de test ouverte (pas de conflit de verrou avec un `@DataJpaTest`).
- **Pourquoi auto-enregistrement et non classe de base.** Une classe de base
  `AbstractIntegrationTest` imposerait de modifier les 171 `*IT.java` (`extends`) — large
  surface de revue et de risque. L'auto-enregistrement JUnit applique l'extension à tout
  le harnais sans toucher un seul fichier de test (CA6).
- **`spring.profiles.active=test` forcé via `pom.xml`** plutôt que `@ActiveProfiles` :
  même raison — éviter de modifier 171 fichiers. Les `@SpringBootTest(properties = {...})`
  inline restent valides (ils s'empilent par-dessus le profil).
