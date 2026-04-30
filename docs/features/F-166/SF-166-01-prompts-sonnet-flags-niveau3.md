# Mini-spec — F-166 / SF-166-01 — Prompts Sonnet : 8 flags décisionnels Travail FR (niveau 3)

## Identifiant

`F-166 / SF-166-01`

## Feature parente

`F-166` — Niveau 3 : enrichissement prompts IA Sonnet pour 8 flags décisionnels Travail FR (suite F-165)

## Statut

`draft`

## Date de création

2026-04-30

## Branche Git

`feat/SF-166-01-prompts-sonnet-flags`

---

## Objectif

Enrichir le prompt Sonnet du domaine `DROIT_DU_TRAVAIL` pour produire 8 nouveaux booleans dans `travail_extracted_data`, et étendre le record `TravailExtractedData` (23 → 31 args) + le parser `CaseAnalysisResponse` afin que ces flags soient lisibles par le pipeline IA. Préparation de SF-166-02 (migration Liquibase + `extractDetectedSituations` qui consommera ces flags pour bascule ALWAYS_ON → CONTEXTUAL des 8 derniers outils Travail FR).

---

## Comportement attendu

### Cas nominal

L'IA Sonnet, lorsqu'elle analyse un dossier `DROIT_DU_TRAVAIL`, produit dans son JSON de sortie l'objet `travail_extracted_data` enrichi de 8 booleans :

| Flag JSON | Outil concerné | Critère de détection (résumé) |
|---|---|---|
| `rappel_salaire_detecte` | F-DT-20 — Rappel de salaire | Indice d'arriérés salariaux : bulletin litigieux, mise en demeure, mention "salaire impayé" / "rappel de salaire" / "heures non rémunérées" / "primes non versées" |
| `travail_dissimule_detecte` | F-DT-21 — Travail dissimulé | Indice de dissimulation L.8221-3+ : heures non déclarées, pas de DPAE, fausse qualification, sous-déclaration salariale, témoignages |
| `clause_non_concurrence_detectee` | F-DT-24 — Clause non-concurrence | Présence textuelle d'une clause de non-concurrence dans le contrat de travail (avec ou sans contrepartie financière) |
| `statut_protege_detecte` | F-DT-30 — Protection des représentants du personnel | Statut de représentant du personnel mentionné : CSE titulaire/suppléant, DS, RSS, conseiller prud'homme/du salarié, défenseur syndical, médecin du travail, CHSCT historique |
| `transaction_envisagee` | F-DT-31 — Transaction | Indice qu'une transaction post-rupture est envisagée, négociée ou signée : protocole transactionnel, courrier de proposition, mention "concessions réciproques", "indemnité transactionnelle" |
| `at_mp_detecte` | F-DT-33 — Accident du travail / maladie pro | Présence d'un AT/MP : déclaration CPAM, certificat médical initial, taux IPP, contestation de reconnaissance, congé maladie professionnelle |
| `urgence_procedurale` | F-DT-34 — Référé prud'homal | Indices d'urgence : non-paiement de salaires en cours, mise à pied conservatoire, contestation de licenciement nul (réintégration), provisions à obtenir rapidement, mesures conservatoires |
| `contestation_are_envisagee` | F-DT-35 — Contestation ARE France Travail | Indice d'un litige avec France Travail : refus ARE notifié, recours hiérarchique, contestation de carence, démission requalifiée |

Tous les flags sont par défaut `false` quand le critère n'est pas détecté ; jamais `null` (pour simplifier le parsing et la migration SF-166-02).

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| Dossier `DROIT_DU_TRAVAIL` mais pays Belgique | Les 8 flags doivent rester à `false` (instruction explicite dans le prompt). La détection est FR-only ; les équivalents BE existent mais reposent sur des critères différents et seront traités séparément dans le backlog jumeau BE. |
| LLM omet un flag | Le parser `CaseAnalysisResponse` traite le champ manquant comme `false` (default). Pas d'exception. |
| LLM produit un type non boolean (ex. string "true") | Le parser tolère `"true"` / `"false"` / `true` / `false` ; toute autre valeur → `false` (fail-safe). |
| Domaine ≠ `DROIT_DU_TRAVAIL` | Le prompt n'inclut pas ces instructions ; le record `TravailExtractedData` n'est pas instancié → aucun risque. |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils décisionnels Travail FR** : F-DT-03/04/07/08/09/10/11/12/13/14/15/16/17/18/19/22/25/26/27/28/29/32 — déjà CONTEXTUAL (F-165) ou ALWAYS_ON essentiels (F-DT-03 prescription, F-DT-04 fiche prud'homale, F-DT-07 ancienneté). **Les 8 flags de cette SF couvrent les 8 derniers outils Travail FR encore ALWAYS_ON.**
- [x] **Autres outils Travail BE** : F-DT-06/11/12/13/15/19/28/29/32-BE — gérés par leur propre référentiel CCT BE, pas concernés par ces flags FR.
- [x] **Autres domaines** : Immigration et Famille ont leurs propres extraits (`type_procedure_detectee`, `divorce_consentement_validity_detection`, etc.) — pas d'impact.
- [x] **Autres pays** : Belgique sur Travail — équivalents existent (`travail au noir BE`, `clause non-concurrence BE`, `transaction BE`, `incapacité de travail Fedris BE`, `recours ONEM`) mais critères distincts → **backlog jumeau BE à ouvrir** plutôt qu'embarquer dans cette SF (éviterait dette de convergence).

### Niveaux de vérification

- [x] **Modèle TypeScript** : `CaseAnalysisResponse.ts` (frontend) — non impacté ici (les 8 flags resteront accessibles via `synthesis.travailExtractedData.*` après SF-166-01 ; SF-166-02 les exposera réellement). Les composants frontend qui en dépendent (panel F-IA-04 via `extractDetectedSituations`) consomment via le service backend, pas directement.
- [x] **Record backend `TravailExtractedData`** : extension 23 → 31 args, ajout d'un constructeur 31 + préservation des 4 constructeurs rétrocompat (9, 17, 18, 23 args).
- [x] **Service / parser** : `CaseAnalysisResponse.parseAnalysisResult()` lit le JSON `travail_extracted_data` et instancie le record — à étendre pour mapper les 8 nouveaux champs.
- [x] **Entité JPA + schéma DB** : `case_analyses.analysis_result` est un TEXT JSON — pas de migration de schéma. Les 8 booleans sont sérialisés dans le JSON (rétrocompat naturelle : un dossier ancien sans ces flags les recevra à `false`).
- [x] **Tests existants** : `CaseAnalysisResponseTest`, `LegalDomainPromptBuilderTest` — à étendre.

### Cas spécifique : nouvel outil décisionnel

Non applicable — cette SF n'introduit pas un outil mais des flags de détection. Les outils F-DT-20/21/24/30/31/33/34/35 existent déjà (Terminée vague 2026-04-25) et sont aujourd'hui en ALWAYS_ON. SF-166-02 les fera basculer en CONTEXTUAL en consommant ces flags.

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — ni nouveau composant Angular, ni nouveau service applicatif, ni nouvel endpoint, ni nouvelle directive. Pure extension du contrat IA existant.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|---|---|---|
| 8 outils ALWAYS_ON Travail FR (F-DT-20/21/24/30/31/33/34/35) | Oui | **Ciblé directement par cette SF + SF-166-02** |
| Travail BE équivalent (8 outils symétriques) | Partiellement | **Backlog jumeau F-XXX-BE à ouvrir** (à créer en fin de SF-166-02 si pas déjà présent dans le backlog) — critères BE distincts (CCT 109, Fedris, ONEM) |
| Immigration / Famille | Non | Domaines distincts, mécanismes propres |
| Autres pays | Non | Pas d'autre pays prévu V1-V8 |
| Frontend `TravailExtractedData` modèle TS | Indirectement | Sera consommé par F-IA-04 panel via backend ; pas de modification directe ici |

### Décision

- [x] Étendu à toutes les cibles applicables FR dans cette SF + SF-166-02
- [ ] Subfeature(s) parallèle(s) créée(s)
- [x] Backlog V8+ à ouvrir : **8 jumeaux Travail BE** (rappel salaire BE, travail au noir BE, clause non-concurrence BE, protection délégué syndical BE, transaction BE, incapacité Fedris BE, référé président tribunal travail BE, contestation ONEM) — à créer comme entrée backlog dédiée après merge SF-166-02
- [x] Non applicable aux autres domaines (justification : Immigration et Famille ont leurs propres mécanismes de détection)

---

## Impact par domaine métier

Cette SF est **sensible au domaine** :

- **Droit du travail FR** : ciblé directement — les 8 flags sont produits uniquement quand `legal_domain = DROIT_DU_TRAVAIL` ET pays = `FR`.
- **Droit du travail BE** : non concerné (le prompt impose explicitement de laisser tous les flags à `false` pour un dossier BE). Backlog jumeau prévu.
- **Droit de l'immigration** : non concerné (prompt distinct, pas d'inclusion de `travail_extracted_data`).
- **Droit de la famille** : non concerné (idem).

---

## Parité des domaines métier

Non applicable — cette SF ne livre **pas un outil décisionnel de niveau ≥ 5** (scoring / comparateur / détection d'événement déclencheur). Les outils concernés (F-DT-20 à F-DT-35) ont des niveaux variés mais existent déjà ; cette SF ajoute uniquement leur **mécanisme de visibilité** (flag IA → règle CONTEXTUAL). Les équivalents BE qui restent à livrer relèvent du même outil métier projeté en Belgique, pas d'un nouvel outil ; ils sont tracés via le backlog jumeau ci-dessus.

---

## Critères d'acceptation

- [ ] **CA-01** : Le prompt `LegalDomainPromptBuilder.TRAVAIL_INSTRUCTION` contient une section explicite décrivant les 8 nouveaux flags avec critères de détection, valeurs par défaut `false`, et règle "laisser `false` pour un dossier BE".
- [ ] **CA-02** : Le record `TravailExtractedData` accepte 31 args (23 existants + 8 booleans non null avec default `false`).
- [ ] **CA-03** : 4 constructeurs rétrocompat (9, 17, 18, 23 args) sont préservés et passent les 8 nouveaux flags à `false`.
- [ ] **CA-04** : Le parser `CaseAnalysisResponse.parseAnalysisResult()` lit les 8 champs JSON `rappel_salaire_detecte`, `travail_dissimule_detecte`, `clause_non_concurrence_detectee`, `statut_protege_detecte`, `transaction_envisagee`, `at_mp_detecte`, `urgence_procedurale`, `contestation_are_envisagee` et les mappe sur le record.
- [ ] **CA-05** : Champ JSON manquant → flag `false` (pas d'exception). Champ JSON `null` → `false`. Champ JSON string `"true"` → `true`. Toute autre valeur non-boolean → `false`.
- [ ] **CA-06** : Tests UT couvrent : (a) 8 cas — chaque flag activé isolément ; (b) 1 cas — tous flags `false` ; (c) 1 cas — JSON sans aucun des 8 flags (rétrocompat) ; (d) 1 cas — JSON avec valeurs non-boolean (fail-safe) ; (e) 1 cas — dossier BE avec tous flags `false` (test d'instruction prompt non régressée).
- [ ] **CA-07** : Aucune régression sur les 23 args existants — l'ensemble des tests `CaseAnalysisResponseTest` et `LegalDomainPromptBuilderTest` passe.
- [ ] **CA-08** : Aucune régression sur les autres domaines (Immigration, Famille) — leurs prompts et records ne sont pas touchés.

---

## Périmètre

### Hors scope (explicite)

- **Migration Liquibase** des règles de visibilité dans `decision_tool_visibility_rules` → SF-166-02.
- **Extension `extractDetectedSituations`** dans `DecisionToolVisibilityService` pour lire ces 8 flags → SF-166-02.
- **Frontend** : aucun changement du modèle TS, aucun nouveau composant, aucune entrée TOOL_REGISTRY (les 8 outils existent déjà avec leurs entrées).
- **Equivalents BE** des 8 outils → backlog jumeau (à ajouter au PRODUCT_SPEC.md en fin de SF-166-02).
- **Optimisation prompt token-count** : pas de mesure de l'impact sur le coût Sonnet ici (à mesurer en staging après merge).
- **Smoke test E2E sur Sonnet réel** : non bloquant pour cette SF (tests UT sur fixtures suffisent ; vérification de bout-en-bout dans la PR de SF-166-02 sur dossier travail FR réel).

---

## Contraintes de validation

| Champ | Type | Format | Default | Notes |
|---|---|---|---|---|
| `rappel_salaire_detecte` | `boolean` (non null) | `true` / `false` | `false` | Rétrocompat : absent JSON → `false` |
| `travail_dissimule_detecte` | `boolean` (non null) | idem | `false` | idem |
| `clause_non_concurrence_detectee` | `boolean` (non null) | idem | `false` | idem |
| `statut_protege_detecte` | `boolean` (non null) | idem | `false` | idem |
| `transaction_envisagee` | `boolean` (non null) | idem | `false` | idem |
| `at_mp_detecte` | `boolean` (non null) | idem | `false` | idem |
| `urgence_procedurale` | `boolean` (non null) | idem | `false` | idem |
| `contestation_are_envisagee` | `boolean` (non null) | idem | `false` | idem |

Note : choix `boolean` non nullable (avec default `false`) plutôt que `Boolean` nullable car (a) la sémantique "non détecté" = `false` est claire (pas besoin de distinguer "non détecté" vs "pas applicable") ; (b) simplifie la migration SF-166-02 (un seul check `IS TRUE` côté DB) ; (c) cohérent avec le pattern existant dans `LicenciementValidityDetection` où l'absence d'évaluation est représentée par "INCONNU" string explicite, pas null.

---

## Technique

### Endpoint(s)

Aucun endpoint nouveau ni modifié. Les 8 flags transitent par le pipeline IA existant (`POST /api/v1/case-files/{id}/analyze` puis lecture via `GET /api/v1/case-files/{id}`).

### Tables impactées

| Table | Opération | Notes |
|---|---|---|
| `case_analyses` | Aucun changement de schéma | Les 8 booleans sont sérialisés dans la colonne `analysis_result` (TEXT JSON) — rétrocompat naturelle |

### Migration Liquibase

- [ ] Oui
- [x] Non applicable — pas de changement de schéma DB

### Composants Angular

Aucun.

### Fichiers backend modifiés

| Fichier | Modification |
|---|---|
| `backend/src/main/java/fr/ailegalcase/analysis/LegalDomainPromptBuilder.java` | Étendre `TRAVAIL_INSTRUCTION` (lignes 80-145) avec une nouvelle section "8 flags décisionnels niveau 3 (F-DT-20/21/24/30/31/33/34/35)" |
| `backend/src/main/java/fr/ailegalcase/analysis/CaseAnalysisResponse.java` | Étendre le record `TravailExtractedData` (ligne 79) à 31 args + ajouter constructeur rétrocompat 23-args ; étendre `parseAnalysisResult()` (vers ligne 1007) pour lire les 8 nouveaux champs JSON |
| `backend/src/test/java/fr/ailegalcase/analysis/CaseAnalysisResponseTest.java` | Ajouter ≥ 12 cas de test (8 flags isolés + tous `false` + JSON manquant + valeurs non-boolean + dossier BE + rétrocompat 23 args) |
| `backend/src/test/java/fr/ailegalcase/analysis/LegalDomainPromptBuilderTest.java` | Ajouter test de présence des 8 noms de flag dans le prompt + test que le prompt mentionne explicitement "BE → false" |

---

## Plan de test

### Tests unitaires

- [ ] **TU-01** `CaseAnalysisResponseTest.parseAnalysisResult_avec_rappel_salaire_detecte_true` : JSON avec un seul flag à `true`, vérifier que le record contient `rappelSalaireDetecte=true` et les 7 autres à `false`.
- [ ] **TU-02 à TU-08** : idem pour les 7 autres flags isolés.
- [ ] **TU-09** `parseAnalysisResult_avec_tous_flags_false` : JSON avec tous les 8 flags présents à `false` → record avec tous à `false`.
- [ ] **TU-10** `parseAnalysisResult_sans_aucun_flag_dans_json` : JSON ne contenant aucun des 8 champs (cas dossier ancien analysé avant SF-166-01) → record avec tous à `false` (rétrocompat).
- [ ] **TU-11** `parseAnalysisResult_avec_valeurs_non_boolean` : JSON avec `"rappel_salaire_detecte": "yes"` (non boolean) → flag `false` (fail-safe).
- [ ] **TU-12** `parseAnalysisResult_avec_string_true_false` : JSON avec `"rappel_salaire_detecte": "true"` (string) → flag `true` ; `"false"` → `false`.
- [ ] **TU-13** `TravailExtractedData_constructeur_9_args_pose_flags_a_false` : instancier le record avec le constructeur 9 args legacy, vérifier que les 8 flags sont à `false`.
- [ ] **TU-14** `TravailExtractedData_constructeur_23_args_pose_flags_a_false` : idem 23 args.
- [ ] **TU-15** `LegalDomainPromptBuilderTest_travail_instruction_contient_8_flags` : vérifier que `TRAVAIL_INSTRUCTION` contient les 8 noms de champ JSON exacts.
- [ ] **TU-16** `LegalDomainPromptBuilderTest_travail_instruction_mentionne_BE_false` : vérifier que la section explicite la règle "dossier BE → tous les 8 flags à `false`".

### Tests d'intégration

- [ ] **IT-01** : Aucun nouveau IT requis pour cette SF (pas de nouvel endpoint, pas de migration DB). Les IT existants (`CaseAnalysisServiceIT`) doivent passer sans modification.

### Isolation workspace

- [x] Non applicable — pas de nouvel accès données. Les flags transitent par le record existant qui est déjà filtré par `workspace_id` au niveau du `CaseAnalysisService`.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — extension purement métier du record et du prompt, sans toucher au framework.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|---|---|---|
| `CaseAnalysisService` | Crée des `TravailExtractedData` avec le constructeur — vérifier que les constructeurs rétrocompat passent les nouveaux flags à `false` | TU-13/14 + IT existants |
| `EnrichedAnalysisService` | Idem | IT existants |
| Frontend `synthesis.travailExtractedData` | Réception JSON enrichi — frontend ignore les champs inconnus (Angular HttpClient) | Smoke test manuel staging post-merge |
| F-IA-04 panel `decisional-tools-panel` | **Pas d'impact tant que SF-166-02 n'est pas mergée** (les 8 outils restent ALWAYS_ON) | Vérifié par test d'intégrité `DecisionToolVisibilityIntegrityIT` |

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — la SF étend le contrat IA sans toucher aux flows critiques (auth, workspace, navigation). Les flags ne sont consommés par aucun composant tant que SF-166-02 n'est pas mergée.

---

## Dépendances

### Subfeatures bloquantes

- F-165 SF-165-01 (Terminée 2026-04-27, PR #689 + 7 fixes #690-#696) — fournit le pattern `extractDetectedSituations` que SF-166-02 réutilisera.

### Subfeature suivante

- **SF-166-02** (séquentielle) — migration Liquibase ALWAYS_ON → CONTEXTUAL pour les 8 outils + extension `extractDetectedSituations` qui lit les 8 flags du JSON `travail_extracted_data`. Ne peut pas démarrer avant le merge de SF-166-01.

### Questions ouvertes impactées

- [x] Aucune question ouverte de `docs/OPEN_QUESTIONS.md` impactée.

---

## Notes et décisions

- **Choix `boolean` non nullable** (avec default `false`) au lieu de `Boolean` nullable : voir section "Contraintes de validation" — cohérence avec le pattern existant et simplification de la migration SF-166-02.
- **Pas de sous-objet `niveau_3_flags`** englobant les 8 booleans : choix d'aplatir au niveau `travail_extracted_data` pour cohérence avec les autres flags simples (`salaire_est_deduit`, `nationalite_ue` côté immigration). Un sous-objet introduirait un niveau de nesting sans gain sémantique.
- **Backlog jumeau BE** : à ajouter au PRODUCT_SPEC.md à la fin de SF-166-02 (entrée unique groupant les 8 équivalents BE), pas dans cette SF pour ne pas alourdir le périmètre.
- **Coût Sonnet** : impact estimé négligeable (+ ~200-300 tokens prompt + 8 booleans output = quelques tokens). À surveiller en staging post-merge mais pas bloquant.
- **Risque** : variance LLM sur la détection des 8 critères (cas borderline). Le prompt sera formulé avec des critères factuels stricts — le LLM doit s'appuyer sur des indices documentaires concrets, pas sur de l'interprétation. Cas borderline → flag `false` par défaut. À itérer si besoin via feedback terrain (cf. F-172 pattern de débiaisage).
