# SF-201-01 — Backend prompt + ImmigrationExtractedData (9 flags FR)

## Objectif
Étendre le prompt IA Immigration FR de 9 flags décisionnels niveau 3 et les exposer dans `ImmigrationExtractedData` pour que F-IA-04 puisse basculer 10 outils Immigration FR ALWAYS_ON → CONTEXTUAL.

## Comportement nominal
- Pour un dossier Immigration FRANCE, l'IA renvoie 9 booléens additionnels dans `immigration_extracted_data` : `aes_metiers_tension_eligible_detecte`, `aes_familial_eligible_detecte`, `aes_humanitaire_eligible_detecte`, `aes_etudiant_eligible_detecte`, `changement_statut_envisage_detecte`, `procedure_asile_detectee`, `naturalisation_envisagee_detectee`, `client_mineur_detecte`, `mesure_eloignement_detectee`.
- Tous les booléens default false ; ne deviennent true qu'en présence d'indices factuels documentés.
- Pour un dossier Immigration BELGIQUE, les 9 flags FR doivent rester à false.
- `DecisionToolVisibilityService.extractDetectedSituations` propage les 9 flags vers la map `detected[trigger_field] = "true"` quand `true`.

## Cas d'erreur
- JSON IA absent / malformé → fallback à `false` (pattern `booleanOrFalse`).
- Flag présent mais valeur non-boolean → false.

## Critères d'acceptation
1. `LegalDomainPromptBuilder.IMMIGRATION_INSTRUCTION` contient les 9 flags avec wording explicite "FRANCE UNIQUEMENT".
2. `ImmigrationExtractedData` record étendu avec 9 nouveaux champs `boolean`.
3. 5 constructeurs rétrocompat préservés (4-arg, 6-arg, 8-arg, 9-arg, 14-arg).
4. `extractImmigrationData` parse les 9 flags via `booleanOrFalse`.
5. Aucune migration de données — les flags par défaut false ne dégradent pas les dossiers existants.

## Contrat API
Aucun changement d'API HTTP. La structure JSON de la réponse `case_analyses.synthesis_json` gagne 9 champs additionnels dans `immigration_extracted_data`.

## Plan de test
- UT `LegalDomainPromptBuilderTest` : prompt Immigration contient les 9 flags + mention "FRANCE UNIQUEMENT".
- UT `CaseAnalysisResponseTest` : `extractImmigrationData` parse correctement les 9 flags depuis JSON (true/false/absent).
- IT `DecisionToolVisibilityIntegrityIT` reste vert (KNOWN_FRONTEND_TOOL_IDS inchangé — outils existants, juste leur layer change).

## Tables / endpoints / composants impactés
- `backend/src/main/java/fr/ailegalcase/analysis/LegalDomainPromptBuilder.java` (section IMMIGRATION_INSTRUCTION)
- `backend/src/main/java/fr/ailegalcase/analysis/CaseAnalysisResponse.java` (record ImmigrationExtractedData + extractImmigrationData)
- `backend/src/main/java/fr/ailegalcase/casefile/DecisionToolVisibilityService.java` (extractDetectedSituations)

## Audit "Impact F-166 cross-C×D"
- ✅ Immigration FR : impacté (objet de cette SF).
- ⚪ Immigration BE : non impacté (F-203 parallèle, flags BE distincts).
- ⚪ Travail FR : déjà couvert F-166 SF-166-01 (migration 199).
- ⚪ Travail BE : non impacté (F-204 parallèle).
- ⚪ Famille FR : non impacté (F-200 ultérieur).
- ⚪ Famille BE : non impacté (F-202 ultérieur).

## Audit "exhaustivité droit national FR"
Les 9 flags couvrent les voies de séjour FR principales :
- 4 AES variants (art. L.435-1 à L.435-4 CESEDA — métiers tension, familial, humanitaire, étudiant).
- Changement statut (art. L.412-1).
- Asile (procédure OFPRA / CNDA).
- Naturalisation (art. 21-15 CCiv par décret + art. 21-2/21-13 par déclaration).
- Mineurs (MNA / ASE / autorité parentale).
- Mesures éloignement (OQTF L.614-5 + L.731-1, ITF, expulsion L.631-1+, IRTF).

Régime algérien (Accord franco-algérien 27/12/1968) : couvert via `nationalite_ue=false` + `nationalite='Algérienne'` côté front (déjà extrait par IA). Pas de flag dédié — voie d'amélioration future si besoin terrain.

## Hors périmètre
- Migration `decision_tool_visibility_rules` (couverte par SF-201-02).
- Audit visuel staging (SF-201-03 ultérieur si besoin).
- Immigration BE : F-203.
- Outils Immigration FR MANQUE (P1-P4) : F-208/F-214/F-220 ultérieurs.
