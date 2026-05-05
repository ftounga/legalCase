# Mini-spec — F-166 / SF-166-02 — Migration ALWAYS_ON → CONTEXTUAL pour 8 outils Travail FR (niveau 3)

## Identifiant

`F-166 / SF-166-02`

## Feature parente

`F-166` — Niveau 3 : enrichissement prompts IA Sonnet pour 8 flags décisionnels Travail FR (suite F-165)

## Statut

`draft`

## Date de création

2026-04-30

## Branche Git

`feat/SF-166-02-migration-contextual-8-tools` (branche stacked sur `feat/SF-166-01-prompts-sonnet-flags` — à rebaser sur master après merge PR #718)

---

## Objectif

Bascule des 8 derniers outils ALWAYS_ON Travail FR (F-DT-20/21/24/30/31/33/34/35) en CONTEXTUAL via migration Liquibase + extension de `DecisionToolVisibilityService.extractDetectedSituations` qui lit les 8 nouveaux booleans produits par SF-166-01.

---

## Comportement attendu

### Cas nominal

1. La migration `199-shift-tools-to-contextual-niveau3-travail-fr.xml` :
   - DELETE 8 entrées ALWAYS_ON pour les `tool_id` F-DT-20/21/24/30/31/33/34/35 (FRANCE, DROIT_DU_TRAVAIL).
   - INSERT 8 entrées CONTEXTUAL avec `trigger_field = "<flag>"`, `trigger_value = "true"`.
2. Le service `DecisionToolVisibilityService.extractDetectedSituations` lit les 8 booleans du JSON `travail_extracted_data` (champs `rappel_salaire_detecte`, `travail_dissimule_detecte`, `clause_non_concurrence_detectee`, `statut_protege_detecte`, `transaction_envisagee`, `at_mp_detecte`, `urgence_procedurale`, `contestation_are_envisagee`) et émet la valeur `"true"` dans la map `detected` quand le flag est à `true` (skip si `false`).
3. Effet final : sur un dossier travail FR vide ou sans détection, les 8 outils ne sont plus dans ALWAYS_ON. Sur un dossier où l'IA détecte un flag à `true`, l'outil correspondant remonte en CONTEXTUAL et apparaît dans le panel F-IA-04.

### Mapping figé

| `tool_id` | `trigger_field` | `trigger_value` | priority |
|---|---|---|---|
| F-DT-20-rappel-salaire | `rappel_salaire_detecte` | `true` | 57 |
| F-DT-21-travail-dissimule | `travail_dissimule_detecte` | `true` | 51 |
| F-DT-24-non-concurrence | `clause_non_concurrence_detectee` | `true` | 60 |
| F-DT-30-protection-rp | `statut_protege_detecte` | `true` | 58 |
| F-DT-31-transaction | `transaction_envisagee` | `true` | 61 |
| F-DT-33-at-mp | `at_mp_detecte` | `true` | 59 |
| F-DT-34-refere-prudhomal | `urgence_procedurale` | `true` | 63 |
| F-DT-35-contestation-are-fr | `contestation_are_envisagee` | `true` | 64 |

UUID namespace : `f1a04001-0000-0000-0000-eeee20000XXX` (suit la convention F-165).

### Cas d'erreur

| Situation | Comportement |
|---|---|
| JSON `analysis_result` ne contient pas `travail_extracted_data` | Aucune valeur émise pour les 8 trigger_fields → outils restent dans le catalog (pas affichés). Pattern existant `addIfPresent` skip null. |
| JSON contient `travail_extracted_data` mais pas un des 8 booleans | Idem — flag absent traité comme `false`, aucune émission. |
| 8 booleans tous à `false` (cas nominal d'un dossier sans détection) | Aucune valeur émise → outils dans catalog. Comportement attendu. |
| Migration appliquée 2 fois (rerun Liquibase) | Idempotente : les DELETE sont guardés par `WHERE layer = 'ALWAYS_ON'` et les INSERT ont des UUIDs fixes uniques. Liquibase trace via `databasechangelog`. |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Pattern F-165** réutilisé strictement — même structure de migration (DELETE ALWAYS_ON + INSERT CONTEXTUAL avec trigger_field et trigger_value), même UUID namespace, même méthode `addIfPresent`. Aucun nouveau mécanisme.
- [x] **8 autres flags potentiels** : il n'y a pas d'autres flags niveau 3 prévus pour Travail FR à cette date — F-166 ferme le sujet ALWAYS_ON Travail FR. Si un nouveau flag apparaît plus tard (ex. F-DT-36+), une migration dédiée le couvrira.
- [x] **Travail BE** : non concerné par cette migration (les 8 outils BE équivalents seront créés dans le backlog jumeau).
- [x] **Test d'intégrité `DecisionToolVisibilityIntegrityIT`** : doit rester vert. Les 8 `tool_id` sont déjà présents dans `KNOWN_FRONTEND_TOOL_IDS` côté frontend (TOOL_REGISTRY contient bien les 8 entrées — vérifié par grep avant la mini-spec). Pas de mise à jour à prévoir.
- [x] **Pattern lecture booleans** : `extractDetectedSituations` lit aujourd'hui des strings (`type_rupture`, `type_contrat`, etc.). Pour les booleans, on doit lire `node.path("flag").asBoolean(false)` puis émettre `"true"` si le booléen est vrai. Nouveau pattern — à isoler dans une méthode privée `addBooleanFlagIfTrue` réutilisable.

### Niveaux de vérification

- [x] **Migration Liquibase** : nommage `199-...`, UUID namespace `f1a04001-0000-0000-0000-eeee20000XXX`, rollback fourni.
- [x] **Service backend** : extension `extractDetectedSituations` avec helper `addBooleanFlagIfTrue`.
- [x] **Tests UT** : `DecisionToolVisibilityServiceTest` (extension), couvrir 8 flags isolés + tous false + JSON sans `travail_extracted_data`.
- [x] **Test IT** : `DecisionToolVisibilityIntegrityIT` reste vert (les 8 tool_id sont dans `KNOWN_FRONTEND_TOOL_IDS`).
- [x] **Frontend TOOL_REGISTRY** : pas de modification — les 8 entrées existent déjà (F-DT-20 à F-DT-35).

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — pas de composant partagé, pas de nouveau service. Helper `addBooleanFlagIfTrue` est une méthode privée locale à `DecisionToolVisibilityService`.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|---|---|---|
| 8 outils ALWAYS_ON Travail FR | Oui | **Migration 199 + extension service** |
| Autres flags niveau 3 Travail FR | Non | Ferme le sujet (F-166 finit) |
| 8 jumeaux Travail BE | Non | **Backlog dédié** à ouvrir après merge SF-166-02 |
| Test d'intégrité visibility seed | Vérifié | 8 tool_id dans `KNOWN_FRONTEND_TOOL_IDS` (TOOL_REGISTRY frontend OK) |

### Décision

- [x] Étendu à toutes les cibles applicables FR dans cette SF
- [x] Backlog Travail BE à ouvrir après merge

---

## Impact par domaine métier

- **Travail FR** : ciblé directement (8 outils basculent ALWAYS_ON → CONTEXTUAL).
- **Travail BE** : non touché. Les ALWAYS_ON BE existants (F-DT-06, F-DT-12-BE, etc.) restent en l'état.
- **Immigration / Famille** : non touché.

---

## Parité des domaines métier

Non applicable — pas de nouvel outil décisionnel niveau ≥ 5 livré ici. Mécanisme de visibilité uniquement.

---

## Critères d'acceptation

- [ ] **CA-01** : Migration `199-shift-tools-to-contextual-niveau3-travail-fr.xml` créée, Liquibase exécutée sans erreur en local.
- [ ] **CA-02** : Après migration, `decision_tool_visibility_rules` contient 8 entrées CONTEXTUAL pour les 8 tool_id avec mapping flag → trigger_field figé ci-dessus, et 0 entrée ALWAYS_ON pour ces tool_id.
- [ ] **CA-03** : `extractDetectedSituations` lit les 8 booleans dans `travail_extracted_data` et émet `"true"` dans la map `detected` quand le flag est à `true`. Aucune émission si flag absent ou `false`.
- [ ] **CA-04** : Sur dossier travail FR vide (aucune détection), aucun des 8 outils n'est dans `alwaysOn` ni dans `contextual` — ils sont dans `catalog` (cas attendu post-migration).
- [ ] **CA-05** : Sur dossier travail FR avec un flag à `true`, l'outil correspondant remonte dans `contextual`.
- [ ] **CA-06** : Tests UT couvrent : (a) 8 cas — chaque flag isolé à `true` ; (b) 1 cas — tous false ; (c) 1 cas — `travail_extracted_data` absent ; (d) 1 cas — multiple flags simultanés.
- [ ] **CA-07** : `DecisionToolVisibilityIntegrityIT` reste vert (test d'intégrité tool_id ↔ TOOL_REGISTRY frontend).
- [ ] **CA-08** : Rollback de la migration restaure les 8 ALWAYS_ON avec leurs UUIDs et priorités d'origine.

---

## Périmètre

### Hors scope (explicite)

- **Frontend** : aucun changement. Les 8 entrées TOOL_REGISTRY existent déjà.
- **Prompts Sonnet** : déjà fait dans SF-166-01.
- **Equivalents BE** : backlog dédié.
- **Smoke test E2E sur Sonnet réel** : à valider en staging post-merge sur dossier travail FR avec contenus déclencheurs.

---

## Contraintes de validation

- `trigger_value` choisi = chaîne `"true"` (cohérent avec le format VARCHAR(100) de la colonne ; le service compare la string `"true"` produite par le helper).
- `priority` figée à la valeur d'origine du tool (récupérée des migrations 110/143/149/150/157/158/166/175) pour ne pas modifier l'ordre d'affichage relatif dans le panel F-IA-04.
- Aucun changement de schéma DB (table existante).

---

## Technique

### Migration Liquibase

- [x] Oui — `199-shift-tools-to-contextual-niveau3-travail-fr.xml`
- Nommage conforme : `{NNN}-{description}.xml`
- Rollback fourni (restore 8 ALWAYS_ON avec UUIDs et priorités d'origine)
- UUIDs namespace `f1a04001-0000-0000-0000-eeee20000XXX` (range distinct de F-165 `eeee0000...`)

### Tables impactées

| Table | Opération |
|---|---|
| `decision_tool_visibility_rules` | DELETE 8 ALWAYS_ON + INSERT 8 CONTEXTUAL |

### Endpoints

Aucun nouveau endpoint. L'endpoint existant `GET /api/v1/case-files/{id}/decision-tools/visible` (F-IA-04) consomme automatiquement les nouvelles règles.

### Fichiers backend modifiés

| Fichier | Modification |
|---|---|
| `backend/src/main/resources/db/changelog/migrations/199-shift-tools-to-contextual-niveau3-travail-fr.xml` | Nouvelle migration |
| `backend/src/main/resources/db/changelog/db.changelog-master.xml` | Référencer 199 |
| `backend/src/main/java/fr/ailegalcase/casefile/DecisionToolVisibilityService.java` | Étendre `extractDetectedSituations` + ajouter helper `addBooleanFlagIfTrue` |
| `backend/src/test/java/fr/ailegalcase/casefile/DecisionToolVisibilityServiceTest.java` (ou test associé) | Ajouter tests UT |

---

## Plan de test

### Tests unitaires

- [ ] **TU-01 à TU-08** : `extractDetectedSituations` avec un seul flag à `true` (chaque flag isolé) → la map `detected` contient `(<flag>, {"true"})` et aucune autre entrée pour les 7 autres flags.
- [ ] **TU-09** : Tous les 8 flags à `false` → aucune entrée dans la map pour ces 8 trigger_fields.
- [ ] **TU-10** : `travail_extracted_data` absent du JSON → aucune entrée pour les 8 trigger_fields (rétrocompat dossiers analysés avant SF-166-01).
- [ ] **TU-11** : Plusieurs flags simultanés (3 à `true`, 5 à `false`) → 3 entrées émises.

### Tests d'intégration

- [ ] **IT-01** : `DecisionToolVisibilityIntegrityIT` reste vert (validation seed DB ↔ TOOL_REGISTRY).
- [ ] **IT-02** : Test bout-en-bout sur l'endpoint visibility `GET /case-files/{id}/decision-tools/visible` avec dossier travail FR vide → 0 outil F-DT-20/21/24/30/31/33/34/35 dans `alwaysOn`.
- [ ] **IT-03** : Idem avec dossier travail FR analyse-fixture où `at_mp_detecte = true` → F-DT-33 dans `contextual`.

### Isolation workspace

- [x] Non applicable — pas de nouvel accès données. Le service existant filtre déjà par workspace_id.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale**

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|---|---|---|
| Panel F-IA-04 (`<decisional-tools-panel>`) | Les 8 outils passent en CONTEXTUAL — **comportement visuel change** : sur dossier vide, ils n'apparaissent plus en haut, mais dans le catalogue | Manuel staging post-merge |
| `DecisionToolVisibilityIntegrityIT` | Aucun, les 8 tool_id existent déjà côté TOOL_REGISTRY | Test reste vert |

### Smoke tests E2E concernés

- [x] Aucun smoke test E2E concerné — modification interne au moteur de visibilité, pas de flow critique transversal.

---

## Dépendances

### Subfeatures bloquantes

- **SF-166-01** (PR #718) — fournit le record étendu et les 8 booleans dans `travail_extracted_data`. **Cette SF est codée sur une branche stacked sur SF-166-01** ; à rebaser sur master après merge #718.

### Subfeature suivante

- **F-167 SF-167-01** : indépendante de F-166, peut démarrer en parallèle.
- **Backlog jumeau Travail BE** : à ouvrir au PRODUCT_SPEC.md après merge de cette SF.

### Questions ouvertes impactées

- [x] Aucune.

---

## Notes et décisions

- **Choix `trigger_value = "true"` (string)** au lieu d'un type boolean DB : la colonne `trigger_value` est `VARCHAR(100)` et tous les triggers existants utilisent des strings. Aucune raison de différer.
- **Helper `addBooleanFlagIfTrue`** isolé dans `DecisionToolVisibilityService` car réutilisable pour d'éventuels futurs flags boolean (Travail BE, autres domaines). Méthode privée — pas de service partagé.
- **Mise à jour PRODUCT_SPEC.md** : marquer F-166 Terminée + ouvrir backlog jumeau BE en post-merge (étape 6 du cycle).
