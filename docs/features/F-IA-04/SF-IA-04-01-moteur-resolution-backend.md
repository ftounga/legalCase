# Mini-spec — F-IA-04 / SF-IA-04-01 Moteur de résolution d'affichage — backend

## Identifiant
`F-IA-04 / SF-IA-04-01`

## Feature parente
`F-IA-04` — Moteur d'affichage conditionnel des outils décisionnels par détection IA

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-IA-04-01-visibility-engine-backend`

---

## Objectif

Poser la brique backend du moteur F-IA-04 : une table de mapping déclarative `decision_tool_visibility_rules` + un service `DecisionToolVisibilityService` qui, pour un `caseFile` donné, résout la liste des outils décisionnels à afficher en trois couches (always-on / contextuels / catalogue en repli). Exposer un endpoint `GET` de consultation. Aucune intégration frontend ni migration du patch F-132 existant dans cette SF.

---

## Comportement attendu

### Cas nominal

`GET /api/v1/case-files/{caseFileId}/decision-tools-visibility` :
1. Résout le `caseFile` et vérifie l'appartenance au workspace (pattern `AncienneteService` / `IndemniteComparatifService`).
2. Résout le triplet `(legalDomain, country)` depuis `caseFile.workspace`.
3. Charge les règles `decision_tool_visibility_rules` matchant `(legalDomain, country)` — les règles `country = NULL` sont ajoutées (règles transversales au domaine).
4. Extrait de `caseFile.latestCaseAnalysis()` les codes de situation détectés par l'IA (cf. section "Extraction des situations détectées" ci-dessous).
5. Construit la réponse `VisibleToolSetResponse` avec trois listes :
   - **alwaysOn** : `tool_id` des règles `layer = ALWAYS_ON`, triées par `priority ASC`, `tool_id ASC`
   - **contextual** : `tool_id` des règles `layer = CONTEXTUAL` dont `trigger_field` présent dans les situations détectées ET `trigger_value` égal à la valeur détectée, triées par `priority ASC`, `tool_id ASC`. Un même `tool_id` n'apparaît qu'une seule fois même si plusieurs règles matchent.
   - **catalog** : `tool_id` des règles `layer = CONTEXTUAL` du domaine+pays **non activés** dans `contextual`, distincts, triés par `tool_id ASC`.

### Cas d'erreur

| Situation | Comportement | Code |
|---|---|---|
| `caseFile` inexistant ou autre workspace | `Case file not found` | 404 |
| `caseFile` en statut `DELETED` | même traitement que 404 (pattern existant) | 404 |
| `caseFile.workspace.legalDomain` null (ne devrait pas arriver) | log warning + renvoie `VisibleToolSet` vide | 200 |
| `caseFile.latestCaseAnalysis()` absente (pas encore analysé) | nominal : `alwaysOn` rempli, `contextual` vide, `catalog` contient tous les outils contextuels du domaine+pays | 200 |

---

## Analyse de cohérence transversale

### Périmètres scannés

| Cible | Applicable ? | Traitement |
|---|---|---|
| Autres outils décisionnels (F-DT-07/08/09/10, F-FA-05/06/07, F-IM-05/06/07, F-132) | Oui — **tous** les outils existants doivent être enregistrés dans le seed de cette SF | **Intégré** : migration 105 seed les règles pour les ~23 outils existants |
| Autres pays (FR / BE) | Oui — la table a une colonne `country` nullable | **Intégré** : les 2 pays sont seedés |
| Autres domaines (travail / famille / immigration) | Oui — la table a une colonne `legal_domain` non nullable | **Intégré** : les 3 domaines sont seedés |
| Cohérence IA (F-IA-03) | Non applicable — cet outil ne saisit pas de réponse avocat, c'est un mécanisme d'affichage | Note ajoutée |
| Refresh dashboard (F-IA-02) | Oui, côté effet — quand une nouvelle analyse IA arrive, la visibilité peut changer | **SF-IA-04-04** branchera le refresh sur le dashboard. Cette SF se contente de l'endpoint. |
| Pré-remplissage IA | Non applicable | — |
| Persistance inputs | Non applicable — pas de saisie avocat ici | — |
| Masquage conditionnel selon type | **C'est précisément le mécanisme qu'on pose.** F-132 actuel (`@if type_rupture === RUPTURE_CONVENTIONNELLE` côté frontend) sera migré en **SF-IA-04-03** pour consommer cet endpoint | **SF-IA-04-03** |
| Alertes actives après calcul | Non applicable | — |

### Pattern nouveau / service partagé

- [x] **Où le nouveau pattern pourrait-il être réutilisé ?** Partout où un dossier affiche un ensemble d'outils décisionnels — à ce jour 1 seule zone (`case-file-detail` panneau outils + dashboard F-IA-02). La migration complète (SF-IA-04-02/03/04) couvrira les 2 zones.
- [x] **Patterns concurrents à remplacer** :
  - **F-132 masquage ad hoc** `@if type_rupture == RUPTURE_CONVENTIONNELLE` dans `case-file-detail` → à migrer vers `visibleTools.contextual` en **SF-IA-04-03**. Garder dette = 2 mécanismes de masquage qui divergent ⇒ interdit.
  - Aucun autre masquage conditionnel par type actuellement présent côté UI (vérifié via scan — F-DT-08 et F-DT-10 ne s'affichent pas conditionnellement aujourd'hui, les 2 sont toujours visibles, ce qui est précisément le bug F-132 à éviter).
- [x] **Le service peut-il servir à d'autres features ?** Oui, toute expansion d'outils décisionnels (F-IM-08, F-IM-09, F-IM-14, F-DT-11+, F-FA-08+) reposera dessus. C'est le sens de la feature.
- [x] **Composant équivalent à remplacer ?** Pas de composant backend équivalent aujourd'hui.

### Décision

- [x] Étendu aux cibles applicables dans cette SF : les 23 outils existants sont seedés FR+BE × 3 domaines
- [x] SFs parallèles créées pour les cibles restantes :
  - **SF-IA-04-02** — composant frontend `<decisional-tools-panel>` (3 couches + catalogue filtrable)
  - **SF-IA-04-03** — migration rétroactive du patch F-132 vers le nouveau mécanisme
  - **SF-IA-04-04** — intégration dans le dashboard F-IA-02
- [x] Non applicable : F-IA-03 (pas de saisie avocat), pré-remplissage IA

---

## Critères d'acceptation

- [ ] Nouvelle table `decision_tool_visibility_rules` créée via migration Liquibase `104-create-decision-tool-visibility-rules.xml` avec colonnes : `id UUID PK`, `legal_domain VARCHAR NOT NULL` (DROIT_DU_TRAVAIL / DROIT_IMMIGRATION / DROIT_FAMILLE), `country VARCHAR NULL` (FRANCE / BELGIQUE / NULL = transversal au domaine), `tool_id VARCHAR(100) NOT NULL` (identifiant stable de l'outil, ex. `"F-DT-08-licenciement-validity"`), `layer VARCHAR NOT NULL` (ALWAYS_ON / CONTEXTUAL), `trigger_field VARCHAR(100) NULL`, `trigger_value VARCHAR(100) NULL`, `priority INTEGER NOT NULL DEFAULT 0`, `created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`
- [ ] Contrainte CHECK : si `layer = 'ALWAYS_ON'` alors `trigger_field IS NULL AND trigger_value IS NULL` ; si `layer = 'CONTEXTUAL'` alors `trigger_field IS NOT NULL AND trigger_value IS NOT NULL`
- [ ] Index `idx_dtvr_domain_country ON decision_tool_visibility_rules (legal_domain, country)` pour la requête principale du service
- [ ] Migration Liquibase `105-seed-decision-tool-visibility-rules.xml` seedant les **23 outils décisionnels existants** selon le mapping listé en annexe (voir "Annexe — mapping initial")
- [ ] Nouvelle entity JPA `DecisionToolVisibilityRule` mappée sur la table, avec enum `Layer { ALWAYS_ON, CONTEXTUAL }`
- [ ] Nouveau repository `DecisionToolVisibilityRuleRepository` avec méthode `findByLegalDomainAndCountryOrCountryNull(LegalDomain, Country)` renvoyant les règles du domaine (country donné + country null)
- [ ] Nouveau service `DecisionToolVisibilityService` avec méthode publique `resolveVisibleTools(UUID caseFileId, User user) → VisibleToolSetResponse` (isolation workspace via `WorkspaceMemberRepository.findByUserAndPrimaryTrue`, pattern existant)
- [ ] Méthode interne `extractDetectedSituations(CaseAnalysis) → Map<String, Set<String>>` : lit les champs IA `type_rupture`, `type_procedure_detectee`, `type_recours_code`, `type_titre_sejour_code`, `regime_matrimonial`, `mode_garde_detaille` depuis `case_analyses.compensation_data` (JSON) et les colonnes dédiées — les noms de champ utilisés sont les mêmes que les `trigger_field` de la table. Un champ absent ou null ⇒ non inclus dans la map (aucune règle ne matchera).
- [ ] Le service tolère l'absence de `CaseAnalysis` : les situations détectées sont vides → `contextual` vide, `catalog` plein, `alwaysOn` rempli.
- [ ] Nouveau DTO record `VisibleToolSetResponse(List<String> alwaysOn, List<String> contextual, List<String> catalog)`
- [ ] Nouveau controller `DecisionToolVisibilityController` exposant `GET /api/v1/case-files/{caseFileId}/decision-tools-visibility`
- [ ] Tous les cas d'erreur listés retournent le bon code HTTP et message
- [ ] Aucun changement côté frontend, ni dans les controllers/services des outils existants, ni dans le patch F-132 frontend (cohabitation — cleanup en SF-IA-04-03)

---

## Périmètre

### Hors scope

- **Frontend** : composant `<decisional-tools-panel>`, intégration `case-file-detail` → **SF-IA-04-02**
- **Migration patch F-132** (retirer le `@if type_rupture === RUPTURE_CONVENTIONNELLE` au profit de `visibleTools.contextual`) → **SF-IA-04-03**
- **Intégration dashboard F-IA-02** (le dashboard filtre ses cards selon la visibilité) → **SF-IA-04-04**
- **Règles d'affichage des outils des futures features (F-IM-08 OQTF, F-IM-09 AES, etc.)** → seedées au fil de l'ajout de chaque outil, pas ici
- **Ajout de nouveaux champs de détection IA** (ex. `type_procedure_detectee` étendu à plus de 3 valeurs cf. F-IM-16) → hors scope, l'extension de l'enum est le travail des features d'expansion
- **Administration UI** (édition des règles par OWNER) → hors V1 du moteur ; la table est alimentée par migrations Liquibase

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|---|---|---|
| `priority` | 0 | Si non spécifiée dans le seed |
| `created_at` | NOW() | Base |
| `country` | NULL | Si le mappage s'applique aux deux pays du domaine |

Comportements à la création :
- Les règles sont insérées par la migration 105 (aucun endpoint d'écriture en V1)
- Les `tool_id` sont des identifiants stables documentés dans l'annexe de cette mini-spec — à toucher uniquement si un outil est supprimé/renommé

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format | Unicité |
|---|---|---|---|---|
| `legal_domain` | Oui | — | Enum `LegalDomain` | Non |
| `country` | Non | — | Enum `Country` ou NULL | Non |
| `tool_id` | Oui | 100 | Alphanumerique + tirets (regex `^[A-Z0-9-]+(-[a-z0-9-]+)*$`) | Non (un outil peut avoir plusieurs règles) |
| `layer` | Oui | — | Enum `Layer` (ALWAYS_ON / CONTEXTUAL) | Non |
| `trigger_field` | Conditionnel (NOT NULL si CONTEXTUAL) | 100 | Alphanumerique + underscores | Non |
| `trigger_value` | Conditionnel (NOT NULL si CONTEXTUAL) | 100 | Alphanumerique + underscores | Non |
| `priority` | Oui (default 0) | — | Integer ≥ 0 | Non |

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| GET | `/api/v1/case-files/{caseFileId}/decision-tools-visibility` | Oui | MEMBER du workspace |

### Tables impactées

| Table | Opération |
|---|---|
| `decision_tool_visibility_rules` (nouvelle) | CREATE (migration 104), INSERT seed (migration 105), SELECT (service) |
| `case_files` | SELECT (résolution workspace + legalDomain via workspace + dernière analyse) |
| `case_analyses` | SELECT (extraction `compensation_data`, `type_procedure_detectee`, etc.) |
| `workspaces` | SELECT (country, legalDomain) |

### Migration Liquibase

- [x] Oui — `104-create-decision-tool-visibility-rules.xml` (table + index + CHECK)
- [x] Oui — `105-seed-decision-tool-visibility-rules.xml` (données initiales des 23 outils)

### Composants Angular

- Aucun dans cette SF (backend only). SF-IA-04-02 introduira `<decisional-tools-panel>`.

---

## Plan de test

### Tests unitaires

- `DecisionToolVisibilityServiceTest` :
  - Dossier DROIT_DU_TRAVAIL / FRANCE / `type_rupture = RUPTURE_CONVENTIONNELLE` → `contextual` contient `F-DT-10-rupture-conv-validity` + `F-132-rupture-conv-indemnite` ; `F-DT-08-licenciement-validity` et `F-DT-09-comparateur-indemnites` sont dans `catalog`, pas dans `contextual`
  - Dossier DROIT_DU_TRAVAIL / FRANCE / `type_rupture = LICENCIEMENT` → contextuel contient F-DT-08 + F-DT-09 ; F-DT-10 + F-132 dans catalog
  - Dossier DROIT_DU_TRAVAIL / BELGIQUE / `type_rupture = LICENCIEMENT_ORDINAIRE` → contextuel contient les outils BE, les FR absents des always-on et contextual
  - Dossier DROIT_IMMIGRATION / FRANCE / sans analyse → `alwaysOn` = [F-IM-05, F-IM-07, …], `contextual` vide, `catalog` plein
  - Dossier DROIT_FAMILLE / FRANCE / `regime_matrimonial = COMMUNAUTE_LEGALE` → …
  - Workspace `country = null` (édge-case) → charger uniquement les règles `country = null` + domaine
  - `CaseAnalysis` avec plusieurs situations détectées (ex. `type_rupture = LICENCIEMENT` ET `type_recours_code = RECOURS_TA`) → les 2 outils contextuels sont activés
- `DecisionToolVisibilityRuleRepositoryTest` (@DataJpaTest) :
  - `findByLegalDomainAndCountryOrCountryNull` ramène bien les règles pays-spécifiques + transversales domaine
  - Isole par domaine : un dossier travail ne voit pas les règles immigration

### Tests d'intégration

- `GET /api/v1/case-files/{id}/decision-tools-visibility` → 200 avec body conforme
- `GET` sur un `caseFile` autre workspace → 404
- `GET` sur un `caseFile` DELETED → 404
- `GET` sur un `caseFile` sans analyse → 200, alwaysOn rempli, contextual vide, catalog plein
- Pattern isolation `WorkspaceMemberRepository.findByUserAndPrimaryTrue` vérifié

### Isolation workspace

- [x] Applicable — un utilisateur du workspace A ne peut pas résoudre la visibilité du workspace B → 404

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [x] Workspace context — le service lit `caseFile.workspace.legalDomain` et `workspace.country`. Aucune modification du contexte, juste lecture. **Aucun impact de régression**.
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [ ] Aucune préoccupation transversale

### Composants / endpoints existants potentiellement impactés

Aucun. Cette SF n'introduit **que** du nouveau code backend. Le frontend continue de voir tous les outils (cohabitation jusqu'à SF-IA-04-02/03).

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — cette SF n'ajoute qu'un endpoint non consommé. Les smoke tests existants ne connaissent pas cet endpoint et continueront de passer.

---

## Dépendances

### Subfeatures bloquantes
Aucune.

### Questions ouvertes impactées
Aucune.

---

## Notes et décisions

### Choix `tool_id` stable vs enum Java

Choix **VARCHAR** (pas d'enum Java) pour permettre d'ajouter un nouvel outil en seed (migration Liquibase) sans recompiler le backend. Les identifiants utilisés sont documentés dans l'annexe — l'enum logique est côté documentation, pas côté code.

Inconvénient : un typo dans le seed ne lève pas d'erreur de compilation. Atténué par les tests d'intégration qui vérifient que les règles seedées correspondent aux IDs attendus.

### Choix `trigger_field` VARCHAR vs enum

Même choix VARCHAR pour la même raison. Le service `extractDetectedSituations` contient un mapping interne `trigger_field (String) → fonction d'extraction` :

```java
Map<String, Function<CaseAnalysis, Optional<String>>> extractors = Map.of(
    "type_rupture", ca -> Optional.ofNullable(ca.compensationData()).map(cd -> cd.typeRupture()),
    "type_procedure_detectee", ca -> Optional.ofNullable(ca.typeProcedureDetectee()),
    "type_recours_code", ca -> Optional.ofNullable(ca.typeRecoursCode()),
    "type_titre_sejour_code", ca -> Optional.ofNullable(ca.typeTitreSejourCode()),
    "regime_matrimonial", ca -> Optional.ofNullable(ca.liquidationCommunauteData()).map(lc -> lc.regimeMatrimonial()),
    "mode_garde_detaille", ca -> Optional.ofNullable(ca.pensionAlimentaireData()).map(pa -> pa.modeGardeDetaille())
);
```

Ajouter un nouveau `trigger_field` = ajouter une ligne dans cette map + seeder une règle. Documenté.

### Pourquoi pas de table d'administration UI

Hors V1 du moteur. En V1 la table est alimentée par migrations Liquibase versionnées, ce qui est plus sûr (traçabilité git, rollback possible). Une UI d'édition par OWNER pourrait venir plus tard (backlog) si un besoin d'autonomie se manifeste — pour l'instant, un OWNER ne devrait pas avoir besoin de modifier les règles de visibilité, c'est une décision produit.

### Cohabitation avec le patch F-132 frontend actuel

Cette SF **ne touche pas** le patch F-132. Le frontend continue d'afficher F-DT-10 conditionnellement via son `@if` actuel. L'endpoint nouvellement exposé n'est consommé par personne en fin de SF — ce qui est volontaire, la migration se fera en SF-IA-04-03. La cohabitation courte (temps d'une SF) est acceptable ; la règle anti-dette de convergence s'applique au steady state.

---

## Annexe — Mapping initial des 23 outils (seed migration 105)

### Droit du travail — FRANCE

| `tool_id` | `layer` | `trigger_field` | `trigger_value` | `priority` |
|---|---|---|---|---|
| `F-DT-03-prescription-litige` | ALWAYS_ON | — | — | 10 |
| `F-DT-04-fiche-prudhomale` | ALWAYS_ON | — | — | 20 |
| `F-DT-07-anciennete-conges-prime` | ALWAYS_ON | — | — | 30 |
| `F-DT-01-calcul-indemnite-simple` | ALWAYS_ON | — | — | 40 |
| `F-DT-08-licenciement-validity` | CONTEXTUAL | `type_rupture` | `LICENCIEMENT` | 10 |
| `F-DT-08-licenciement-validity` | CONTEXTUAL | `type_rupture` | `LICENCIEMENT_ECONOMIQUE` | 10 |
| `F-DT-09-comparateur-indemnites` | CONTEXTUAL | `type_rupture` | `LICENCIEMENT` | 20 |
| `F-DT-09-comparateur-indemnites` | CONTEXTUAL | `type_rupture` | `LICENCIEMENT_ECONOMIQUE` | 20 |
| `F-DT-10-rupture-conv-validity` | CONTEXTUAL | `type_rupture` | `RUPTURE_CONVENTIONNELLE` | 10 |
| `F-132-rupture-conv-indemnite` | CONTEXTUAL | `type_rupture` | `RUPTURE_CONVENTIONNELLE` | 20 |

### Droit du travail — BELGIQUE

| `tool_id` | `layer` | `trigger_field` | `trigger_value` | `priority` |
|---|---|---|---|---|
| `F-DT-05-preavis-be` | ALWAYS_ON | — | — | 10 |
| `F-DT-06-requete-tribunal-travail` | ALWAYS_ON | — | — | 20 |
| `F-DT-07-anciennete-conges-prime` | ALWAYS_ON | — | — | 30 |
| `F-DT-08-licenciement-validity` | CONTEXTUAL | `type_rupture` | `LICENCIEMENT_ORDINAIRE` | 10 |
| `F-DT-09-comparateur-indemnites` | CONTEXTUAL | `type_rupture` | `LICENCIEMENT_ORDINAIRE` | 20 |

### Droit de l'immigration — FR + BE (règles transversales au domaine, `country = NULL`)

| `tool_id` | `layer` | `trigger_field` | `trigger_value` | `priority` |
|---|---|---|---|---|
| `F-IM-05-arbre-decisionnel-titre` | ALWAYS_ON | — | — | 10 |
| `F-IM-07-droit-au-travail` | ALWAYS_ON | — | — | 20 |
| `F-IM-01-checklist-pieces` | CONTEXTUAL | `type_titre_sejour_code` | `*` (voir note ci-dessous) | 10 |
| `F-IM-06-recours` | CONTEXTUAL | `type_recours_code` | `RECOURS_GRACIEUX_PREFET` | 20 |
| `F-IM-06-recours` | CONTEXTUAL | `type_recours_code` | `RECOURS_CONTENTIEUX_TA` | 20 |
| `F-IM-06-recours` | CONTEXTUAL | `type_recours_code` | `RECOURS_CNDA` | 20 |
| `F-IM-06-recours` | CONTEXTUAL | `type_recours_code` | `RECOURS_CGRA` | 20 |
| `F-IM-06-recours` | CONTEXTUAL | `type_recours_code` | `RECOURS_CCE` | 20 |
| `F-IM-06-recours` | CONTEXTUAL | `type_recours_code` | `RECOURS_CE_BELGIQUE` | 20 |

> Note `*` sur F-IM-01 : `type_titre_sejour_code` a 16 valeurs enum. Seeder 16 lignes pour ce même `tool_id` est la solution la plus simple et explicite. Le service déduplique naturellement (`distinct()`) quand plusieurs règles activent le même `tool_id`.

### Droit de la famille — FR + BE (règles transversales, `country = NULL`)

| `tool_id` | `layer` | `trigger_field` | `trigger_value` | `priority` |
|---|---|---|---|---|
| `F-FA-01-prestation-compensatoire` | ALWAYS_ON | — | — | 10 |
| `F-FA-02-pension-alimentaire` | ALWAYS_ON | — | — | 20 |
| `F-FA-04-liquidation-communaute` | ALWAYS_ON | — | — | 30 |
| `F-FA-05-partage-immobilier` | CONTEXTUAL | `regime_matrimonial` | `COMMUNAUTE_LEGALE` | 10 |
| `F-FA-05-partage-immobilier` | CONTEXTUAL | `regime_matrimonial` | `PARTICIPATION_ACQUETS` | 10 |
| `F-FA-06-calendrier-garde` | CONTEXTUAL | `mode_garde_detaille` | `ALTERNEE_FR` | 20 |
| `F-FA-06-calendrier-garde` | CONTEXTUAL | `mode_garde_detaille` | `DVH_CLASSIQUE_FR` | 20 |
| `F-FA-06-calendrier-garde` | CONTEXTUAL | `mode_garde_detaille` | `DVH_ELARGI_FR` | 20 |
| `F-FA-06-calendrier-garde` | CONTEXTUAL | `mode_garde_detaille` | `ALTERNEE_BE` | 20 |
| `F-FA-06-calendrier-garde` | CONTEXTUAL | `mode_garde_detaille` | `SECONDAIRE_BE` | 20 |
| `F-FA-06-calendrier-garde` | CONTEXTUAL | `mode_garde_detaille` | `SECONDAIRE_ELARGI_BE` | 20 |
| `F-FA-07-checklist-divorce` | CONTEXTUAL | `type_procedure_detectee` | `DIVORCE_CONSENTEMENT_MUTUEL` | 30 |
| `F-152-divorce-consentement-scoring` | CONTEXTUAL | `type_procedure_detectee` | `DIVORCE_CONSENTEMENT_MUTUEL` | 40 |
| `F-153-fourchettes-jaf` | ALWAYS_ON | — | — | 40 |

### Résumé

- **ALWAYS_ON** : 10 outils (4 FR travail + 3 BE travail + 2 immigration + 4 famille — certains dédoublonnés)
- **CONTEXTUAL** : 23 règles correspondant à ~13 outils distincts
- **Total lignes seedées** : ~37 lignes dans la table `decision_tool_visibility_rules`

Ces règles sont à auditer lors de la readiness — certaines zones (F-IM-01 avec 16 valeurs possibles) demandent un choix explicite : soit seeder 16 règles, soit introduire un mécanisme "wildcard". **Choix V1** : seeder 16 règles explicites pour F-IM-01 (trivial, et rend le mapping lisible en SQL). À revoir si l'explosion combinatoire devient un problème (ne semble pas probable).
