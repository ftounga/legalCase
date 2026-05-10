# SF-205-01 — Backend prompt + TravailExtractedData (extension flags Travail FR)

## Objectif (1 phrase)

Étendre le prompt LLM `TRAVAIL_INSTRUCTION` et le record `TravailExtractedData` de **23 nouveaux booleans niveau 3 FRANCE** afin d'alimenter ultérieurement les outils manquants P1 (F-206) et P2 (F-212), sans bascule de visibilité (réservée aux features dérivées).

## Contexte

- F-166 SF-166-01 (avril 2026) a déjà ajouté **8 flags Travail FR** (`rappel_salaire_detecte`, `travail_dissimule_detecte`, `clause_non_concurrence_detectee`, `statut_protege_detecte`, `transaction_envisagee`, `at_mp_detecte`, `urgence_procedurale`, `contestation_are_envisagee`) qui drivent F-DT-20/21/24/30/31/33/34/35.
- F-204 (mai 2026) a ajouté **5 flags Travail BE** (`harcelement_be_detecte`, `discrimination_be_detectee`, `inaptitude_medicale_be_detectee`, `heures_sup_mentionnees_be`, `motif_grave_be_envisage`).
- L'audit `docs/features/F-191/audit-travail-fr-exhaustif.md` (Tableau C) recense ~24 nouveaux flags FR additionnels nécessaires pour driver les **20 outils P1+P2 manquants** (F-206 et F-212 à venir).
- Cette SF livre **uniquement les flags + le prompt + l'extracteur + la propagation visibility**. Aucune migration `decision_tool_visibility_rules` ni nouveau composant frontend.

## Liste exacte des 23 flags ajoutés

Le brief énonce 24 flags candidats. Après vérification croisée avec F-166 SF-166-01 :

- `salarie_protege_detecte` (n°19 du brief) est **redondant avec `statut_protege_detecte` déjà présent dans F-166**. Il est **exclu** de cette SF (consigne brief : "Si certains flags sont déjà couverts par F-166 SF-166-01, ne pas les dupliquer").

Les **23 flags réellement ajoutés** :

| # | Flag | Référence juridique / Outil cible |
|---|------|-----------------------------------|
| 1 | `abandon_poste_detecte` | Loi 21/12/2022 — présomption démission après 15 j mise en demeure (F-DT-42) |
| 2 | `arret_maladie_long_detecte` | Loi 22/04/2024 — acquisition CP pendant arrêt maladie (F-DT-75) |
| 3 | `prise_acte_envisagee` | Prise d'acte CPH — F-DT-39 |
| 4 | `resiliation_judiciaire_envisagee` | Résiliation judiciaire CPH — F-DT-40 |
| 5 | `forfait_jours_detecte` | Art. L.3121-58+ — F-DT-50 |
| 6 | `transfert_entreprise_detecte` | Art. L.1224-1 — F-DT-72 |
| 7 | `faute_inexcusable_envisagee` | Art. L.452-1 CSS — F-DT-91 |
| 8 | `cs_crp_envisage` | Contrat de Sécurisation Professionnelle — art. L.1233-65+ |
| 9 | `csp_propose` | Variante CSP / CRP côté employeur (F-DT-44) |
| 10 | `mutation_refusee` | Clause mobilité refusée (F-DT-71) |
| 11 | `modification_contrat_refusee` | Refus modification du contrat (F-DT-70) |
| 12 | `faute_grave_envisagee` | Faute grave — F-DT-36 (séparé de F-DT-08) |
| 13 | `faute_lourde_envisagee` | Faute lourde / intention de nuire — F-DT-36 |
| 14 | `cdd_requalification_envisagee` | Art. L.1245-1+ — F-DT-43 |
| 15 | `interim_requalification_envisagee` | Art. L.1251-40+ |
| 16 | `forfait_jours_validite_contestee` | Validité forfait jours (Cass. 29/06/2011) — F-DT-50 |
| 17 | `prescription_proche_detectee` | 1 an / 2 ans / 3 ans selon type — F-DT-03 contextualisable |
| 18 | `rupture_amiable_negociee` | CDI rupture — distincte conv RC |
| 19 | `entretien_preavis_obtenu` | Lien CSP — entretien préalable / préavis |
| 20 | `cse_consultation_demandee` | F-DT-65 — élections CSE / consultation |
| 21 | `irp_election_demandee` | Procédure élections IRP |
| 22 | `inspection_travail_saisie` | Saisine de l'inspection du travail |
| 23 | `mediation_judiciaire_envisagee` | Médiation CPH |

## Comportement nominal + cas d'erreur

### Nominal
- Pour un dossier `DROIT_DU_TRAVAIL` × `FRANCE`, l'IA Sonnet remplit les 23 booleans dans le bloc JSON `travail_extracted_data` (default `false` si absent ou indéterminable).
- Le record `TravailExtractedData` expose chaque flag via accessor record.
- `DecisionToolVisibilityService.extractDetectedSituations` propage chaque flag à `true` dans la map `detected[flag_name] = "true"` (pattern miroir SF-166-02 / F-204).

### Cas d'erreur
- JSON LLM absent → `extractTravailData` retourne `null` (comportement existant) ; aucun flag visible.
- Flag absent ou non booléen → `booleanOrFalse` retourne `false` (fail-safe).
- Flag à `true` pour un dossier BE → consigne explicite : "TOUS ces 23 flags FR DOIVENT rester false pour BE" ; le LLM est instruit en ce sens, et l'extracteur n'ajoute pas de filtre runtime (cohérent avec F-166).
- Constructeurs rétrocompat 9/17/18/23/31-args existants → tous propagent `false` pour les 23 nouveaux flags.

## Critères d'acceptation vérifiables

1. `LegalDomainPromptBuilder.TRAVAIL_INSTRUCTION` contient **les 23 noms de flags FR exacts** dans une section `F-205`, située après le bloc F-204.
2. Le record `TravailExtractedData` expose **23 nouveaux accesseurs `boolean`** dans l'ordre listé ci-dessus, après les 5 flags F-204.
3. Un nouveau constructeur rétrocompat `36-args` existe (post-F-204) qui passe `false ×23` pour les nouveaux flags.
4. Les 4 constructeurs rétrocompat existants (9/17/18/23-args) compilent toujours et propagent `false` pour les 23 nouveaux flags.
5. `extractTravailData` parse les 23 flags via `booleanOrFalse(node, "<flag>")`.
6. `DecisionToolVisibilityService.extractDetectedSituations` propage les 23 flags via `addBooleanFlagIfTrue(detected, travailNode, "<flag>")`.
7. Test UT prompt : un test paramétré assert que les 23 noms apparaissent dans `domainSpecificInstruction("DROIT_DU_TRAVAIL")`.
8. Test UT extracteur : un test parse un JSON contenant les 23 flags à `true` et vérifie que les 23 accesseurs renvoient `true`.
9. `DecisionToolVisibilityIntegrityIT` continue à passer (aucune nouvelle entrée dans la table `decision_tool_visibility_rules`).

## Plan de test minimal

- **UT prompt** (`LegalDomainPromptBuilderTest`) :
  - `domainSpecificInstruction_travail_contains23F205Flags` → assert les 23 noms.
  - `domainSpecificInstruction_travail_F205Flags_explicitlyExcludeBE` → assert présence du wording "FRANCE UNIQUEMENT" + "BE" dans le bloc F-205.
- **UT extracteur** (`CaseAnalysisResponseTest`) :
  - `extractTravailData_F205Flags_parsedFromJson` → 23 flags à `true`.
  - `extractTravailData_F205Flags_defaultFalseIfAbsent`.
  - `travailExtractedData_constructeur31Args_legacy_setsAllF205FlagsToFalse` (constructeur post-F-204).
- **Pas de test d'intégration** sur `DecisionToolVisibilityService` car aucune règle ne consomme encore les 23 flags (rendez-vous F-206/F-212).
- **Pas de test E2E** : les flags ne déclenchent aucun affichage frontend dans cette SF.

## Tables / endpoints / composants impactés

- **Backend uniquement** :
  - `backend/src/main/java/fr/ailegalcase/analysis/LegalDomainPromptBuilder.java`
  - `backend/src/main/java/fr/ailegalcase/analysis/CaseAnalysisResponse.java` (record `TravailExtractedData` + `extractTravailData`)
  - `backend/src/main/java/fr/ailegalcase/casefile/DecisionToolVisibilityService.java` (propagation)
  - Tests : `LegalDomainPromptBuilderTest`, `CaseAnalysisResponseTest`
- **Aucune migration Liquibase** dans cette SF.
- **Aucun composant frontend** modifié.

## Hors périmètre

- Migration `decision_tool_visibility_rules` (réservée à F-206 P1 et F-212 P2 — chacune branchera ses propres outils sur les flags livrés ici).
- Création des outils décisionnels frontend/backend correspondants (F-DT-36/39/40/42/44/50/70/71/72/75/91 etc.) — tous deferred.
- Ajout d'entrées au `TOOL_REGISTRY` frontend (rien à activer tant qu'aucun outil n'est livré).
- Renaming/normalisation des flags F-166 existants — pattern conservé.

## Analyse de cohérence transversale

- **Domaine métier** : Travail FR uniquement. BE = tous false (instruction explicite dans le prompt). Famille / Immigration : non concernés (record dédié `FamilleExtractedData` / `ImmigrationExtractedData`).
- **Pays** : FR uniquement. Le bloc F-205 du prompt clarifie "FRANCE UNIQUEMENT" et "BE → tous false" pour éviter la dérive observée pré-F-166.
- **Outils décisionnels concernés** : aucun consommateur dans cette SF — les outils correspondants seront livrés en F-206 / F-212. Les flags sont déclarés dès maintenant pour préparer le terrain (la cohérence Tableau C audit-travail-fr-exhaustif.md est respectée).
- **Pattern** : strict miroir de F-166 SF-166-01 + F-204 (ajout en queue du record, constructeur rétrocompat, propagation visibility, test UT prompt + extracteur).

## Nouveau pattern UI ou service partagé

Aucun. La SF ne touche aucun composant frontend, n'introduit ni service ni directive partagée. Elle étend uniquement deux artefacts existants (prompt builder, record DTO) et une méthode existante (extractDetectedSituations) — pattern strictement identique à F-166 / F-204.

## Impact F-166 cross-C×D

Aucune modification du registre `decision_tool_visibility_rules` dans cette SF — donc l'audit cross-C×D est **non applicable** (pas de bascule visibility, pas de changement de mode pour aucun outil existant).

Justification : les 23 flags livrés ici sont des **booleans alimentés par le LLM** mais **non consommés** côté visibility tant que F-206 ou F-212 n'ajoutent pas leurs propres règles (qui devront, elles, passer l'audit cross-C×D obligatoire). À ce stade, sur les 6 cellules C×D :

| C × D | Impact panel F-IA-04 | Justification |
|-------|---------------------|---------------|
| FR × Travail | aucun | flags présents dans le record, mais aucune règle ne les consomme |
| FR × Immigration | aucun | record distinct |
| FR × Famille | aucun | record distinct |
| BE × Travail | aucun | flags forcés à false par le prompt (instruction explicite) |
| BE × Immigration | aucun | record distinct |
| BE × Famille | aucun | record distinct |

## Exhaustivité droit national FR

Cette SF ajoute uniquement des entrées **côté FR**. Conformément au garde-fou F-199 SF-199-02 :

- **Source juridique FR** : Code du travail (art. L.1232-1+, L.1233-65+, L.1224-1, L.1245-1+, L.1251-40+, L.3121-58+, L.452-1 CSS), Code du travail issu des lois 21/12/2022 (abandon de poste) et 22/04/2024 (CP pendant arrêt maladie), Cass. 29/06/2011 (forfait jours).
- **Équivalent BE** : les 23 concepts FR couverts ici n'ont **pas tous** d'équivalent direct côté BE (CSP / CRP, prise d'acte CPH, résiliation judiciaire CPH, présomption d'abandon de poste sont des mécanismes purement français). Les concepts BE équivalents sont :
  - `motif_grave_be_envisage` (déjà livré F-204) ≈ `faute_grave_envisagee` FR.
  - Inaptitude médicale BE (`inaptitude_medicale_be_detectee` F-204) ≈ `arret_maladie_long_detecte` partiel.
  - Régimes BE autres (transfert d'entreprise par CCT 32 bis, indemnité de protection BE) → traités par les outils BE existants ou backlog F-204+.
- **Cohérence terminologique** : noms en `snake_case` français, pattern miroir des flags F-166 / F-204. Aucun conflit ou collision de nom détecté.

Conclusion : pas d'asymétrie créée — les flags BE pertinents sont déjà couverts par F-204 (5 flags) et les concepts FR additionnels (CSP, prise d'acte, résiliation judiciaire, abandon de poste loi 2022) sont propres au droit français. Aucun rattrapage BE supplémentaire n'est ouvert par cette SF (cf. mémoire `feedback_belgique_never_forget` : couverture exhaustive BE distincte, pas miroir FR).

## Impact par domaine métier

- **Droit du travail** : sensible — la SF étend exclusivement le record `TravailExtractedData` et le prompt domaine `DROIT_DU_TRAVAIL`. Côté FR uniquement.
- **Droit de l'immigration** : non sensible — record distinct (`ImmigrationExtractedData`), pipeline distinct.
- **Droit de la famille** : non sensible — record distinct (`FamilleExtractedData`), pipeline distinct.

## Parité des domaines métier

Non applicable : SF de niveau ≤ 4 (extension de prompt + DTO booleans). Aucun scoring (≥ 5), aucun comparateur (6), aucune détection d'événement déclencheur (7) introduit dans cette SF. Les outils F-DT-36/39/40/42/44/50/70/71/72/75/91 que ces flags driveront seront livrés dans F-206 / F-212 ; chaque outil scoring/comparateur/triggering devra alors remplir cette section au moment de sa SF dédiée.

## Préoccupations transversales

- **Auth / Principal** : non touché.
- **Workspace context** : non touché.
- **Plans / limites** : non touché.
- **Navigation / routing** : non touché.
- **Outil décisionnel métier** : oui — la SF prépare l'arrivée des outils F-DT-36/39/40/42/44/50/70/71/72/75/91 etc., listés dans `audit-travail-fr-exhaustif.md` Tableau C. Aucun outil existant n'est modifié, aucun éclatement d'outil multi-situations introduit. La règle "un outil = une situation" sera appliquée par les SF de F-206 / F-212.

## Règle "Migration Liquibase qui INSERT/UPDATE dans `decision_tool_visibility_rules`"

Non applicable — **aucune migration livrée** dans cette SF. Les bascules de visibilité seront livrées par F-206 (P1) et F-212 (P2) après cette SF.

## Estimation

≤ 1 jour (extension prompt + 23 booleans + tests). Pattern strict miroir SF-166-01 / F-204 SF-204-01.
