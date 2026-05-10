# SF-204-01 — Backend prompt + TravailExtractedData (5 flags BE)

## Objectif
Étendre le prompt IA Travail BE de 5 flags décisionnels niveau 3 et les exposer dans `TravailExtractedData` pour que F-IA-04 puisse basculer F-DT-11/12/15/19/27 (BE) ALWAYS_ON → CONTEXTUAL.

## Comportement nominal
- Pour un dossier Travail BELGIQUE, l'IA renvoie 5 booléens : `harcelement_be_detecte`, `discrimination_be_detectee`, `inaptitude_medicale_be_detectee`, `heures_sup_mentionnees_be`, `motif_grave_be_envisage`.
- Tous default false.
- Dossier Travail FRANCE : les 5 flags BE doivent rester à false (les régimes FR équivalents sont gérés par F-166 SF-166-01 ou F-DT-08 validity).

## Critères d'acceptation
1. `LegalDomainPromptBuilder.TRAVAIL_INSTRUCTION` contient les 5 flags avec mention "BELGIQUE UNIQUEMENT" + références juridiques (Loi 4/8/1996, Loi 10/5/2007, art. 34 Loi 03/07/1978, art. 29 Loi 16/03/1971, art. 35 Loi 03/07/1978).
2. `TravailExtractedData` étendu avec 5 nouveaux champs `boolean` (en queue de classe, après les 8 flags F-166-01 FR).
3. 4 constructeurs rétrocompat préservés (9-arg, 17-arg, 18-arg, 23-arg).
4. `extractTravailData` parse les 5 flags via `booleanOrFalse`.
5. `DecisionToolVisibilityService.extractDetectedSituations` propage les 5 flags.

## Audit "Impact F-166 cross-C×D"
- ⚪ Travail FR : couvert par F-166 (déjà livré migration 199).
- ✅ Travail BE : objet de cette SF.
- ⚪ Immigration FR/BE : F-201/F-203 parallèles.
- ⚪ Famille FR/BE : F-200/F-202 ultérieurs.

## Audit "exhaustivité droit national BE"
5 flags couvrent les régimes BE clés :
- Harcèlement (Loi du 4 août 1996 sur le bien-être au travail).
- Discrimination (Loi du 10 mai 2007 anti-discrimination, 5 motifs principaux).
- Inaptitude médicale (art. 34 Loi du 03/07/1978 + AR 28/05/2003 trajet réintégration).
- Heures supplémentaires (art. 29 Loi du 16/03/1971 — sursalaire 50 % semaine, 100 % dimanche/JF).
- Motif grave (art. 35 Loi du 03/07/1978 — délai 3 jours ouvrables × 2).

Outils BE-only restants (clause non-concurrence CCT 13/2/2013, RCC variants, transaction BE, etc.) : couverts par F-213 P2 ultérieurement.

## Plan de test
- UT `LegalDomainPromptBuilderTest` : prompt TRAVAIL contient les 5 flags BE + mention "BELGIQUE UNIQUEMENT".
- UT `extractTravailData` : parse les 5 flags depuis JSON (true/false/absent).
- IT visibility intégrité reste vert.

## Tables / endpoints / composants impactés
- `LegalDomainPromptBuilder.java`
- `CaseAnalysisResponse.java` (record TravailExtractedData + extractTravailData)
- `DecisionToolVisibilityService.java`

## Hors périmètre
- Migration visibility (SF-204-02).
- Outils Travail BE MANQUE : F-213 P2.
- Travail FR : F-166 / F-205.
