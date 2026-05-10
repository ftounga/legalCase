# SF-200-01 — Backend prompt + FamilleExtractedData (30 flags FR)

## Objectif
Étendre le prompt IA Famille FR de 30 flags décisionnels niveau 3 et créer le record `FamilleExtractedData` (miroir d'`ImmigrationExtractedData`) pour que F-IA-04 puisse basculer 30 outils Famille FR ALWAYS_ON → CONTEXTUAL.

## Comportement nominal
- Pour un dossier Famille FRANCE, l'IA renvoie 30 booléens additionnels dans `famille_extracted_data` (nouveau noeud JSON top-level).
- Tous les booléens default false ; ne deviennent true qu'en présence d'indices factuels documentés.
- Pour un dossier Famille BELGIQUE, les 30 flags FR doivent rester à false (équivalents BE traités par F-202 ultérieur).
- `DecisionToolVisibilityService.extractDetectedSituations` propage les 30 flags vers la map `detected[trigger_field] = "true"` quand `true`.

## Flags livrés (30)
1. `divorce_consentement_mutuel_envisage` (F-FA-07)
2. `divorce_alteration_lien_envisage` (F-FA-08)
3. `divorce_faute_envisage` (F-FA-09)
4. `divorce_accepte_envisage` (F-FA-10)
5. `revision_post_divorce_envisagee` (F-FA-13)
6. `ordonnance_protection_envisagee` (F-FA-14)
7. `recompenses_envisagees` (F-FA-15)
8. `regime_communaute_universelle_detecte` (F-FA-16)
9. `partage_judiciaire_envisage` (F-FA-17)
10. `adoption_envisagee` (F-FA-18)
11. `reconnaissance_paternelle_envisagee` (F-FA-18-reconnaissance-paternelle)
12. `contestation_paternite_envisagee` (F-FA-18-contestation-paternite)
13. `recherche_paternite_envisagee` (F-FA-18-recherche-paternite)
14. `possession_etat_envisagee` (F-FA-18-possession-etat)
15. `changement_residence_envisage` (F-FA-19-changement-residence)
16. `desaccord_parental_detecte` (F-FA-19-desaccords-parentaux)
17. `pacs_dissolution_envisagee` (F-FA-20)
18. `separation_corps_envisagee` (F-FA-21)
19. `indivision_envisagee` (F-FA-22)
20. `ordonnance_requete_envisagee` (F-FA-23)
21. `succession_envisagee` (F-FA-24-devolution-legale, partage, indivision-successorale)
22. `testament_envisage` (F-FA-24-testament-validite)
23. `donation_envisagee` (F-FA-24-donation)
24. `reserve_hereditaire_envisagee` (F-FA-24-reserve-heriditaire)
25. `partage_successoral_envisage` (F-FA-24-partage-successoral)
26. `indivision_successorale_envisagee` (F-FA-24-indivision-successorale)
27. `rapport_succession_envisage` (F-FA-24-rapport-succession)
28. `protection_majeur_envisagee` (F-FA-25)
29. `changement_etat_civil_envisage` (F-FA-26)
30. `pma_gpa_envisagee` (F-FA-27)

## Cas d'erreur
- JSON IA absent / malformé → fallback à `false` (pattern `booleanOrFalse`).
- Flag présent mais valeur non-boolean → false.

## Critères d'acceptation
1. `LegalDomainPromptBuilder.FAMILLE_INSTRUCTION` contient les 30 flags avec wording explicite "FRANCE UNIQUEMENT".
2. `FamilleExtractedData` record créé avec 30 champs `boolean`.
3. `extractFamilleData(JsonNode)` parse les 30 flags via `booleanOrFalse`.
4. `DecisionToolVisibilityService.extractDetectedSituations` propage les 30 flags depuis le noeud `famille_extracted_data`.
5. Aucune migration de données — les flags par défaut false ne dégradent pas les dossiers existants.

## Contrat API
Aucun changement d'API HTTP. La structure JSON de `case_analyses.synthesis_json` gagne un noeud top-level `famille_extracted_data` avec 30 champs booleans additionnels (FR uniquement).

## Plan de test
- UT `LegalDomainPromptBuilderTest` : prompt Famille contient les 30 flags + mention "FRANCE UNIQUEMENT".
- UT `CaseAnalysisResponseTest` : `extractFamilleData` parse correctement 30 flags depuis JSON (true/false/absent).
- IT `DecisionToolVisibilityIntegrityIT` reste vert (KNOWN_FRONTEND_TOOL_IDS inchangé — outils existants, juste leur layer change).

## Tables / endpoints / composants impactés
- `backend/src/main/java/fr/ailegalcase/analysis/LegalDomainPromptBuilder.java` (section FAMILLE_INSTRUCTION)
- `backend/src/main/java/fr/ailegalcase/analysis/CaseAnalysisResponse.java` (nouveau record FamilleExtractedData + extractFamilleData)
- `backend/src/main/java/fr/ailegalcase/casefile/DecisionToolVisibilityService.java` (extractDetectedSituations — lecture famille_extracted_data)

## Audit "Impact F-166 cross-C×D"
- ✅ Famille FR : impacté (objet de cette SF).
- ⚪ Famille BE : non impacté (F-202 parallèle, flags BE distincts — droit successoral / régimes BE distincts).
- ⚪ Travail FR : déjà couvert F-166 SF-166-01 (migration 199).
- ⚪ Travail BE : déjà couvert F-204 (migration 215).
- ⚪ Immigration FR : déjà couvert F-201 (migration 213).
- ⚪ Immigration BE : déjà couvert F-203 (migration 214).

## Audit "exhaustivité droit national FR"
Les 30 flags couvrent les branches du droit de la famille FR (sources : audit-famille-fr-exhaustif.md Tableau C) :
- 4 cas de divorce (consentement mutuel art. 229-1, altération 237-238, faute 242, accepté 233-234) — branche B.3.
- Régimes matrimoniaux (récompenses 1433+, communauté universelle 1526-1527 al. 2, partage judiciaire 840) — branche B.5.
- Filiation (reconnaissance 316, contestation 332-335, recherche 327, possession d'état 311-1/317) — branche B.7.
- Adoption art. 343-370-3 — branche B.8.
- Autorité parentale (changement résidence 373-2, désaccords 373-2-10) — branche B.9.
- PACS dissolution 515-7-1 — branche B.2.
- Séparation de corps 296-308 — branche B.4.
- Indivision 815, ordonnance sur requête 493 CPC — branches B.5 / procédure.
- Successions / libéralités (dévolution, testament, donation, réserve, partage, indivision successorale, rapport) — branche B.6.
- Protection des majeurs (tutelle 425, curatelle, sauvegarde) — branche B.11.
- État civil (changement nom 61-1, prénom 61-5) — branche B.12.
- Ordonnance protection 515-9-12 (violences conjugales) — branche B.10.
- PMA / GPA (loi bioéthique 2/8/2021) — branche B.7.

Couvre 12 des 13 branches du droit de la famille FR identifiées dans l'audit (manque B.13 "autres situations" = mariage, devoirs époux structurels qui restent ALWAYS_ON via F-FA-12 / F-FA-19-autorite-parentale ou F-FA-05/06/07 mixed).

Famille BE : flags équivalents distincts (succession Wallonie/Bruxelles/Flandre, partage UC, art. 229+ CC belge) → F-202.

## Hors périmètre
- Migration `decision_tool_visibility_rules` (couverte par SF-200-02).
- Audit visuel staging (SF-200-03 ultérieur).
- Famille BE : F-202.
- Création de `FamilleExtractedData` au niveau du record principal `CaseAnalysisResponse` : pour cette SF, le record est créé comme structure interne parseable + `static extractFamilleData(JsonNode)` ; il n'est pas thread à travers le record principal pour limiter le blast-radius (les flags ne sont consommés que par `DecisionToolVisibilityService`, pas par le frontend de cette SF). Si une SF ultérieure ajoute du pré-fill IA frontend pour ces flags, une SF dédiée étendra le record principal avec rétrocompat.
- Outils Famille FR MANQUE (Top 10 D.2 audit-famille-fr-exhaustif.md) : SF de rattrapage ultérieures.
