# Mini-spec — [F-245 / SF-245-02] Correction des tests d'intégration obsolètes révélés

> Suite de SF-245-01. Le découpage de F-245 est justifié dans
> `SF-245-01-isolation-harnais-it.md` (section « Découpage de F-245 »).

---

## Identifiant

`F-245 / SF-245-02`

## Feature parente

`F-245` — Hygiène du harnais de tests d'intégration

## Statut

`ready`

## Date de création

2026-05-18

## Branche Git

`feat/SF-245-02-correction-it-obsoletes`

---

## Objectif

> En une phrase : que fait cette subfeature ?

Corriger les 49 tests d'intégration obsolètes que le `mvnw verify` rendu déterministe par
SF-245-01 a révélés, pour obtenir un `mvnw verify` 100 % vert, et basculer la CI backend
de `mvnw test` vers `mvnw verify` afin que les IT ne puissent plus pourrir sans alerte.

---

## Comportement attendu

### Cas nominal

`./mvnw verify` se termine `BUILD SUCCESS`, 0 échec / 0 erreur, de façon déterministe ;
la CI backend exécute désormais les IT à chaque push.

Les 49 échecs sont traités par catégorie. Pour chaque test : déterminer si la cause est
un **test obsolète** (le code de production est correct, le test n'a pas suivi une
évolution → on corrige le test) ou un **bug produit réel** (le test est correct, le code
de production est fautif → on corrige le code de production).

**Catégorie A — tests obsolètes (corrections mécaniques), 36 tests :**

| Cause racine | Classes | Tests | Correction |
|---|---|---|---|
| `case_files.legal_domain` / `created_by_user_id` devenus `NOT NULL`, `setUp()` ne les renseigne pas | `AnalysisJobControllerIT`, `UsageEventControllerIT`, `AdminUsageControllerIT`, `AiQuestionControllerIT`, `SynthesisSearchControllerIT`, `ChunkingServiceIT` | 30 | Renseigner `legalDomain` et `createdBy` sur le `CaseFile` du `setUp()` |
| `workspace.legal_domain` `NOT NULL` non renseigné | `WorkspaceInvitationControllerIT` | 2 | Renseigner `legalDomain` sur le `Workspace` du `setUp()` |
| `@DataJpaTest` sans le bean `StripeCustomerService` ni `EmailService` requis par `WorkspaceService` | `WorkspaceServiceIT` | 4 | Fournir les beans manquants au slice (`@MockitoBean` ou `@Import` ciblé) |

**Catégorie B/C — investigation par cas, 13 tests :**

| Classe | Tests | Démarche |
|---|---|---|
| `RecherchePaterniteControllerIT` | 3 | `verdictRecevabilite` attendu `ELEVEE`, obtenu `MOYENNE` — vérifier si le barème de recevabilité a évolué (test obsolète) ou régressé (bug) |
| `AncienneteControllerIT` | 1 | `conventionCode` attendu `BTP`, obtenu `IDCC_1596` — probable migration assumée des codes courts vers codes IDCC → test obsolète |
| `ReferentialControllerIT` | 3 | `LazyInitializationException` sur un proxy `Workspace` — corriger la stratégie de fetch / le périmètre transactionnel |
| `AnalysisStatusStreamControllerIT` | 2 | Endpoint renvoie `200` au lieu de `404`/`403` — **vérifier en priorité s'il s'agit d'une faille d'isolation workspace réelle** |
| `CaseFileControllerIT` | 1 | `NullPointerException` sur un `JsonNode` — structure de réponse JSON désynchronisée |
| `LegalReferentialDescriptionIntegrityIT` | 1 | Entrée(s) système sans `description` — compléter la donnée ou le test |
| `LocalLoginControllerIT` | 1 | `401` après login — dépendance implicite à une donnée laissée par un autre test |
| `WorkspaceInvitationControllerIT` | 1 | `createInvitation` renvoie `402` au lieu de `201` — gate de plan / quota à neutraliser dans le test |

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| Un cas B/C s'avère un **bug produit non trivial ou à fort impact** (ex. faille d'isolation workspace) | Le bug est **escaladé en feature de correction dédiée** (`PRODUCT_SPEC.md`) ; le test est mis en quarantaine documentée (`@Disabled` + référence) plutôt que faussement « corrigé » en assertant le comportement fautif. La quarantaine d'un test ne masque jamais un bug : elle le trace. |
| Un cas B/C s'avère un **bug produit trivial et sûr à corriger** | Le code de production est corrigé dans cette SF, avec test de non-régression ; la correction est documentée dans la PR. |
| La bascule CI `mvnw test` → `mvnw verify` rendrait la CI rouge | Interdit : la bascule n'est faite **qu'après** un `verify` 100 % vert vérifié. |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** : `RecherchePaterniteControllerIT` / `AncienneteControllerIT` touchent des outils décisionnels — la correction se limite à l'alignement du test sur le comportement actuel, sans modifier la logique métier (sauf bug avéré, alors escaladé).
- [x] **Autres pays / domaines** : non applicable — corrections de tests, pas de logique pays/domaine.
- [x] **Flows transversaux** : `AnalysisStatusStreamControllerIT` et `LocalLoginControllerIT` touchent l'isolation workspace et l'auth — d'où la règle d'escalade explicite ci-dessus pour tout vrai bug.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|---|---|---|
| Tests d'intégration obsolètes | Oui | Cœur de la SF — corrigés un par un |
| Code de production | Conditionnel | Modifié uniquement si bug trivial avéré ; sinon escaladé |
| Workflow CI (`backend.yml`) | Oui | Bascule `mvnw test` → `mvnw verify` |

### Décision

- [x] Étendu à toutes les cibles applicables ; les vrais bugs produit non triviaux sont escaladés en features dédiées (pas de correction silencieuse ni de fausse correction de test).

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — SF de correction de tests backend. Aucun composant frontend.

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — SF de correction de tests. Aucun outil décisionnel créé/modifié.

---

## Critères d'acceptation

- [ ] **CA1** — `./mvnw verify` (backend) se termine `BUILD SUCCESS`, 0 échec / 0 erreur.
- [ ] **CA2** — Déterminisme : deux `./mvnw verify` consécutifs donnent le même résultat vert.
- [ ] **CA3** — Les 49 tests inventoriés en SF-245-01 passent, ou sont en quarantaine
  `@Disabled` documentée pointant une feature de correction de bug escaladée.
- [ ] **CA4** — La correction d'un test obsolète ne masque jamais un bug : aucune
  assertion n'est affaiblie pour « passer » sans que le comportement testé soit le
  comportement correct attendu.
- [ ] **CA5** — La CI backend (`.github/workflows/backend.yml`) exécute `mvnw verify`
  (IT inclus) et reste verte après merge.
- [ ] **CA6** — Tout bug produit réel découvert est soit corrigé avec test de
  non-régression, soit escaladé en feature `PRODUCT_SPEC.md` et tracé.

---

## Périmètre

### Hors scope (explicite)

- **Optimisation des performances du `verify`** (fusion des contextes Spring) — backlog,
  comme indiqué dans SF-245-01.
- **Refonte / réécriture profonde des tests** au-delà du strict nécessaire pour les
  rendre corrects et fiables.
- **Suppression des `@BeforeEach { deleteAll() }` redondants** — cosmétique, hors scope
  (déjà hors scope de SF-245-01).
- **Correction des bugs produit non triviaux** révélés — escaladés en features dédiées.

---

## Technique

### Fichiers impactés

| Fichier | Opération | Rôle |
|---|---|---|
| ~11 `*IT.java` (catégorie A) | modifié | `setUp()` complété (`legalDomain`, `createdBy`), beans de slice |
| jusqu'à ~8 `*IT.java` (catégorie B/C) | modifié | Assertions réalignées, fetch/transaction corrigés |
| Code de production (conditionnel) | modifié | Uniquement si bug trivial avéré |
| `.github/workflows/backend.yml` | modifié | `mvnw test` → `mvnw verify` |

### Migration Liquibase

- [ ] Oui
- [x] Non applicable — sauf si `LegalReferentialDescriptionIntegrityIT` impose de
  compléter une `description` côté donnée seedée (à confirmer en dev).

---

## Plan de test

### Tests d'intégration

- [ ] `./mvnw verify` complet → `BUILD SUCCESS`, 0 échec / 0 erreur (CA1), exécuté deux
  fois (CA2).
- [ ] Chaque classe IT corrigée passe intégralement.
- [ ] Tout bug produit corrigé est couvert par un test de non-régression explicite.

### Isolation workspace

- [x] Applicable — `AnalysisStatusStreamControllerIT` teste l'isolation workspace d'un
  endpoint de streaming. Si la SF découvre que l'isolation est défaillante, c'est un bug
  produit escaladé (cf. cas d'erreur).
- [ ] Non applicable

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Auth / Principal** — `LocalLoginControllerIT` (correction de test, pas de code auth sauf bug avéré).
- [x] **Workspace context** — `AnalysisStatusStreamControllerIT` : si bug d'isolation avéré → escalade.
- [ ] **Plans / limites**
- [ ] **Navigation / routing frontend**

### Composants / endpoints potentiellement impactés

| Composant | Impact potentiel | Test de non-régression |
|---|---|---|
| `AnalysisStatusStreamController` | Faille d'isolation workspace possible | IT `stream_otherWorkspaceCaseFile_returns403` — doit passer en assertant 403 |
| CI `backend.yml` | Bascule sur `verify` : la CI devient plus longue (IT inclus) | Le run post-merge sur `master` doit être vert |

### Smoke tests E2E concernés

- [x] Aucun — la SF corrige des IT backend et le workflow CI ; aucun impact sur l'app
  déployée ni sur les smoke tests `e2e/smoke/`.

---

## Dépendances

### Subfeatures bloquantes

- `SF-245-01` — statut : **done** (PR #1063 mergée). Indispensable : sans le `verify`
  déterministe, corriger les tests serait hasardeux.

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` impactée.

---

## Notes et décisions

- **Principe directeur** : un test qui échoue dit quelque chose. Avant de « corriger » un
  test, on établit lequel de deux a raison — le test ou le code. On ne fait jamais passer
  un test en affaiblissant une assertion correcte (CA4).
- **Escalade plutôt que dissimulation** : un vrai bug produit non trivial n'est pas
  « corrigé » en faisant croire au test que le comportement fautif est attendu. Il est
  tracé (feature dédiée) et son test mis en quarantaine `@Disabled` explicite.
