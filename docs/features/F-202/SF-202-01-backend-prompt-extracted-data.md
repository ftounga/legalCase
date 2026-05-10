# SF-202-01 — Backend prompt FAMILLE BE + FamilleExtractedData (5 flags BE)

## Objectif
Étendre le prompt IA Famille BE de 5 flags décisionnels niveau 3 et créer le record `FamilleExtractedData` (miroir d'`ImmigrationExtractedData`) afin de préparer la bascule ALWAYS_ON → CONTEXTUAL des outils Famille BE existants et futurs (F-IA-04).

## Comportement nominal
- Pour un dossier Famille BELGIQUE, l'IA renvoie 5 booléens additionnels dans `famille_extracted_data` : `divorce_dc_envisage`, `divorce_ddi_envisage`, `cohabitation_legale_be_detectee`, `pacte_successoral_envisage`, `kafala_recueil_detecte`.
- Tous les booléens default false ; ne deviennent true qu'en présence d'indices factuels documentés (mots-clés "consentement mutuel" / "DC", "désunion irrémédiable" / "DDI" / "séparation N mois", "cohabitation légale" / "déclaration commune officier état civil", "pacte successoral" / "renonciation héréditaire", "kafala" / "recueil légal").
- Pour un dossier Famille FRANCE, les 5 flags BE doivent rester à false.
- `DecisionToolVisibilityService.extractDetectedSituations` propage les 5 flags vers la map `detected[trigger_field] = "true"` quand `true`.

## Cas d'erreur
- JSON IA absent / malformé → fallback à `false` (pattern `booleanOrFalse`).
- Flag présent mais valeur non-boolean → false.
- `famille_extracted_data` absent du JSON IA → tous les flags à false (pattern aligné sur `extractTravailData` / `extractImmigrationData`).

## Critères d'acceptation
1. `LegalDomainPromptBuilder.FAMILLE_INSTRUCTION` contient les 5 flags avec wording explicite "BELGIQUE UNIQUEMENT" et la règle "TOUS ces 5 flags DOIVENT rester false" pour un dossier FR.
2. `FamilleExtractedData` record créé avec 5 champs `boolean` initiaux (commenté `// === Flags BE (F-202) ===`). Construction miroir d'`ImmigrationExtractedData`.
3. `extractFamilleData(JsonNode root)` parse les 5 flags via `booleanOrFalse` depuis `root.path("famille_extracted_data")`.
4. `CaseAnalysisResponse` expose `familleExtractedData` (record component nullable) et `extractFamilleData` est invoqué dans `from()` quand `analysisResult` est lisible.
5. `DecisionToolVisibilityService.extractDetectedSituations` lit `famille_extracted_data` et émet la string `"true"` pour chaque flag à true.
6. Aucune migration de données — les flags par défaut false ne dégradent pas les dossiers existants.
7. Coordination F-200 : nos 5 flags BE sont placés en queue avec commentaire `// === Flags BE (F-202) ===` pour minimiser le merge conflict avec les ~30 flags FR de F-200 qui s'inséreraient avant.

## Contrat API
Aucun changement d'API HTTP. La structure JSON de la réponse `case_analyses.synthesis_json` gagne 5 champs additionnels dans la nouvelle clé `famille_extracted_data` (parallèle à `travail_extracted_data` et `immigration_extracted_data`). `CaseAnalysisResponse` Java gagne un champ `familleExtractedData` exposé en JSON sous `familleExtractedData` (camelCase).

## Plan de test
- UT `LegalDomainPromptBuilderTest` : prompt Famille contient les 5 flags + mention "BELGIQUE UNIQUEMENT" + règle d'exclusion FR.
- UT `CaseAnalysisResponseTest` : `extractFamilleData` parse correctement les 5 flags depuis JSON (true / false / absent / non-boolean).
- IT `DecisionToolVisibilityIntegrityIT` reste vert (KNOWN_FRONTEND_TOOL_IDS inchangé — outils déjà inscrits).
- Smoke compilation : `./mvnw compile` passe sans erreur signature (constructeurs rétrocompat préservés s'il y en a).

## Tables / endpoints / composants impactés
- `backend/src/main/java/fr/ailegalcase/analysis/LegalDomainPromptBuilder.java` (section `FAMILLE_INSTRUCTION`).
- `backend/src/main/java/fr/ailegalcase/analysis/CaseAnalysisResponse.java` (record `FamilleExtractedData` créé + `extractFamilleData` + ajout champ `familleExtractedData` dans le record principal).
- `backend/src/main/java/fr/ailegalcase/casefile/DecisionToolVisibilityService.java` (`extractDetectedSituations` enrichi de 5 lectures `famille_extracted_data`).

## Audit "Impact F-166 cross-C×D"
Toute modification de `decision_tool_visibility_rules` (mode `visibility` ou flag) est analysée croisée Country × Domain (cf. garde-fou F-199 SF-199-02).

| Cellule C×D | Impact F-202 SF-202-01 | Justification |
|---|---|---|
| FR × Travail | ⚪ Non impacté | F-166 livré (migration 199), flags FR `travail_extracted_data` distincts. |
| BE × Travail | ⚪ Non impacté | F-204 livré (migration 215), flags BE `travail_extracted_data` distincts. |
| FR × Immigration | ⚪ Non impacté | F-201 livré (migration 213), flags FR `immigration_extracted_data` distincts. |
| BE × Immigration | ⚪ Non impacté | F-203 livré (migration 214), flags BE `immigration_extracted_data` distincts. |
| FR × Famille | ⚪ Non impacté | F-200 parallèle, ~30 flags FR `famille_extracted_data`. Coordination via commentaires d'ancrage `// === Flags FR (F-200) ===` / `// === Flags BE (F-202) ===` pour rendre le merge mécanique. Cette SF crée le record si F-200 ne l'a pas créé en premier ; sinon F-200 ajoute son bloc avant le mien. |
| **BE × Famille** | ✅ **Impacté (objet de cette SF)** | 5 flags BE niveau 3 ajoutés au record. La SF-202-02 jumelle convertit l'outil BE existant `F-FA-11-desunion-irremediable-be` ALWAYS_ON → CONTEXTUAL (trigger `divorce_ddi_envisage`). |

## Audit "exhaustivité droit national BE"
Source juridique pour chaque flag BE :

| Flag | Source juridique nationale BE | Équivalent FR ? |
|---|---|---|
| `divorce_dc_envisage` | CJ art. 1287+ ; Loi 27/04/2007 réformant la procédure DC | FR : `divorce_consentement_validity_detection` (F-152) — capture la même intention mais via 7 critères de validité, pas un flag binaire. Pas de remplacement par symétrie : la procédure DC BE a son propre cheminement (1ʳᵉ et 2ᵉ comparution, délai 3 mois). |
| `divorce_ddi_envisage` | CC art. 229 § 1 et § 3 ; CJ art. 1255 § 1 et § 2 | FR : pas d'équivalent direct (FR a `divorce-alteration` 1 an + `divorce-faute` + `divorce-accepte`, BE a 3 voies DDI distinctes). |
| `cohabitation_legale_be_detectee` | Loi 23/11/1998 ; CC art. 1475+ et 1476 | FR : `F-FA-20-pacs-dissolution` couvre PACS — concept différent (PACS = contrat ; cohabitation légale BE = simple déclaration officier état civil sans contrat obligatoire). Pas d'équivalent symétrique FR. |
| `pacte_successoral_envisage` | CC art. 1100/1+ (réforme Loi 31/07/2017, en vigueur 01/09/2018) | FR : interdit (Cciv art. 1130 al. 2 — "on ne peut renoncer à une succession non ouverte"). **Spécificité BE post-2018**, aucun équivalent FR. |
| `kafala_recueil_detecte` | CDIP belge ; CC art. 343 al. 2 nouveau (exclut adoption-kafala mais admet recueil légal via DIP) | FR : Cciv art. 370-3 — interdiction d'adopter un enfant kafala. Reconnaissance plus large en BE. **Spécificité BE**, aucun équivalent FR direct. |

Justification de la non-symétrie : 4 des 5 flags BE n'ont pas d'équivalent FR par construction (la cohabitation légale, le pacte successoral et la kafala sont spécifiques au droit belge ; le DDI BE est une voie procédurale distincte des 3 voies FR `divorce-alteration`/`divorce-faute`/`divorce-accepte`). Le 5ᵉ flag (`divorce_dc_envisage`) a un cousin FR (F-152 critères de validité du divorce par consentement mutuel) mais la procédure BE est suffisamment différente pour mériter un flag dédié.

## Impact par domaine métier
- **Famille BE** : impacté (objet de cette SF).
- **Famille FR** : non impacté (F-200 parallèle, flags FR distincts, coordination par commentaires d'ancrage).
- **Immigration FR/BE, Travail FR/BE** : non impacté (records séparés, F-201/F-203/F-204/F-166 déjà livrés).

## Hors périmètre
- Migration `decision_tool_visibility_rules` (couverte par SF-202-02).
- 16 autres flags Famille BE pour outils MANQUE futurs (`regime_algerien_be_detecte`, `liquidation_partage_judiciaire_detecte`, `mariage_etranger_reconnaissance_detecte`, `protection_majeur_be_detectee`, etc.) — reportés à F-211/F-217+.
- Création des outils MANQUE Famille BE (cohabitation légale BE / pacte successoral BE / kafala BE) — features futures distinctes.
- Frontend Angular (les flags sont consommés côté backend uniquement par F-IA-04 ; aucune surface UI directe ne dépend de ces 5 booleans dans cette SF).
