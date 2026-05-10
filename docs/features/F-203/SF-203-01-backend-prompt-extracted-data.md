# SF-203-01 — Backend prompt + ImmigrationExtractedData (5 flags BE)

## Objectif
Étendre le prompt IA Immigration BE de 5 flags décisionnels niveau 3 et les exposer dans `ImmigrationExtractedData` pour que F-IA-04 puisse basculer 5 outils Immigration BE ALWAYS_ON → CONTEXTUAL.

## Comportement nominal
- Pour un dossier Immigration BELGIQUE, l'IA renvoie 5 booléens additionnels : `procedure_9bis_envisagee`, `procedure_9ter_medicale_detectee`, `regroupement_40bis_detecte`, `regroupement_40ter_detecte`, `oqt_annexe13_detectee`.
- Tous default false ; ne deviennent true qu'en présence d'indices factuels documentés.
- Pour un dossier Immigration FRANCE, les 5 flags BE doivent rester false.

## Critères d'acceptation
1. `LegalDomainPromptBuilder.IMMIGRATION_INSTRUCTION` contient les 5 flags avec mention "BELGIQUE UNIQUEMENT".
2. `ImmigrationExtractedData` étendu avec 5 nouveaux champs `boolean` (en queue de classe, après les 9 flags F-201).
3. Constructeurs rétrocompat préservés.
4. `DecisionToolVisibilityService.extractDetectedSituations` propage les 5 flags vers `detected[trigger_field] = "true"`.

## Audit "Impact F-166 cross-C×D"
- ⚪ Immigration FR : couvert par F-201 (parallèle, flags FR distincts).
- ✅ Immigration BE : objet de cette SF.
- ⚪ Travail FR/BE / Famille FR/BE : non impactés.

## Audit "exhaustivité droit national BE"
5 flags couvrent les 4 articles BE clés de la Loi du 15/12/1980 :
- Art. 9bis (régularisation par circonstances exceptionnelles humanitaires).
- Art. 9ter (régularisation médicale).
- Art. 40bis (regroupement familial citoyen UE/EEE/Suisse).
- Art. 40ter (regroupement familial Belge ou ressortissant tiers).
- + Art. 7 + 74/14 (OQT / Annexe 13).

Outils BE-only restants (single permit Loi 30/04/1999, AESM tutelle MENA, carte H Brexit, etc.) : couverts par F-209+ ultérieurement.

## Plan de test
- UT prompt builder : prompt Immigration BE contient les 5 flags + mention "BELGIQUE UNIQUEMENT".
- UT `extractImmigrationData` : parse les 5 flags depuis JSON.
- IT visibility intégrité reste vert.

## Hors périmètre
- Migration visibility (SF-203-02).
- Outils MANQUE BE : F-209+.
- Immigration FR : F-201.
