# SF-208-04 — Victime de violences L.425-6 — backend

## Objectif (1 phrase)
Analyser la validité d'une demande de carte de séjour vie privée et familiale sur le fondement de l'art. L.425-6+ CESEDA (titre de plein droit pour victime de violences conjugales bénéficiant d'une ordonnance de protection JAF — anciennement L.316-3) et persister l'analyse 1:1 par dossier.

## Comportement nominal
- POST `/api/v1/case-files/{caseFileId}/victime-violences-l4256-analysis`
- Body : `dateOrdonnanceProtection` (LocalDate, requis), `juridiction` (string : "JAF + ville"), `dureeProtectionMois` (int, 6 par défaut Cciv 515-11), `dateExpirationProtection` (LocalDate, optionnel — défaut = ordonnance + dureeProtectionMois), `enfantsAcharge` (int, ≥ 0), `nationalite` (string libre)
- Analyzer `VictimeViolencesL4256Analyzer` :
  - `eligibiliteScore` ∈ {ELIGIBLE_PLEIN_DROIT, ELIGIBLE_SOUS_RESERVE, NON_ELIGIBLE} en fonction des conditions L.425-6
  - `criteresValides` : list (présence ordonnance protection, juridiction = JAF, durée non expirée, etc.)
  - `criteresManquants` : list
  - `dateExpirationProtectionEffective` : recalcul si non fourni
  - `dureeTitreSejour` : 1 an renouvelable (L.425-7)
  - `messages` : rappel L.425-9 (étranger malade / autre fondement), articulation avec ordonnance JAF Cciv 515-9 à 515-13
- GET `/api/v1/case-files/{caseFileId}/victime-violences-l4256-analysis`

## Cas d'erreur
- 400 si dateOrdonnanceProtection futur, dureeProtectionMois ≤ 0, enfantsAcharge < 0
- 400 si workspace.country ≠ FRANCE
- 400 si caseFile.legalDomain ≠ DROIT_IMMIGRATION
- 404 isolation

## Critères d'acceptation vérifiables
- [x] POST nominal avec ordonnance JAF en cours retourne ELIGIBLE_PLEIN_DROIT
- [x] POST avec ordonnance expirée → NON_ELIGIBLE ou ELIGIBLE_SOUS_RESERVE selon délai
- [x] POST workspace BE → 400, dossier travail → 400
- [x] Analyzer UT couvre les 3 verdicts + critères manquants
- [x] IT couvre nominal + erreurs gates + isolation

## Plan de test minimal
- **UT** `VictimeViolencesL4256AnalyzerTest` : 8+ tests
- **IT** `VictimeViolencesL4256ControllerIT` : 6+ tests
- **Intégrité** : ajout `F-IM-24-victime-violences-l4256-fr` dans `KNOWN_FRONTEND_TOOL_IDS`

## Tables / endpoints / composants impactés
- **Nouvelle table** `victime_violences_l4256_analyses` (id UUID, case_file_id UUID UNIQUE, date_ordonnance_protection DATE NOT NULL, juridiction VARCHAR(80) NOT NULL, duree_protection_mois INT NOT NULL DEFAULT 6, date_expiration_protection DATE, enfants_a_charge INT NOT NULL DEFAULT 0, nationalite VARCHAR(80), country VARCHAR(20) NOT NULL, result_data TEXT NOT NULL, created_at TIMESTAMP, updated_at TIMESTAMP)
- **Migration Liquibase** `221-create-victime-violences-l4256-analyses.xml` + seed `decision_tool_visibility_rules` (ALWAYS_ON, DROIT_IMMIGRATION, FRANCE, F-IM-24-victime-violences-l4256-fr — visible proactivement, pas de flag IA dédié `victime_violences_conjugales_detectee` existant ; à ajouter dans F-205 ultérieur).
- **Endpoint** `VictimeViolencesL4256Controller`
- **Test d'intégrité** : ajout `F-IM-24-victime-violences-l4256-fr`

## Hors périmètre
- Frontend Angular (SF future)
- Génération PDF demande de titre (différé)
- Calcul du droit au séjour parents enfants français (cas séparé L.423-7)

## Impact par domaine métier
**Sensible Immigration FR uniquement** : article L.425-6 est une protection française spécifique. Le droit BE a un mécanisme différent (loi 15/12/1980 art. 11 §2 — protection victime traite êtres humains, pas violences conjugales identiquement). Aucun impact Travail / Famille direct (mais article articulé avec ordonnance JAF qui relève de F-FA-14).

## Parité des domaines métier
Niveau 5 (analyse de validité / scoring d'éligibilité). **Parité Famille** : F-FA-14 ordonnance protection est l'outil amont (Famille FR) — articulation à valider en SF frontend ultérieure (passer la dateOrdonnance JAF du dossier Famille au dossier Immigration). **Parité BE** : sans objet (mécanisme distinct, pas équivalent strict — F-209 si pertinent).

## Analyse de cohérence transversale
- **Autre outil Immigration FR** : F-IM-05 arbre décisionnel titre (générique) — l'analyzer L.425-6 est plus spécialisé.
- **Articulation F-FA-14** (Famille FR ordonnance protection) : non bloquante pour cette SF backend mais à reconnecter dans la SF frontend (pré-fill dateOrdonnance depuis le dossier Famille du même client si existant).
- **Pattern réutilisé** : pattern Annexe13Be (entity 1:1 + service + controller + analyzer pure).

## Audit "Impact F-166 cross-C×D"
- **FR×Immigration** : ajout 1 entrée ALWAYS_ON `F-IM-24-victime-violences-l4256-fr`. ALWAYS_ON justifié : pas de flag IA dédié, et urgence — l'avocat doit pouvoir saisir proactivement.
- Autres cellules : non concernées.

## Audit "exhaustivité droit national FR"
- Source : CESEDA L.425-6 à L.425-8 + Cciv 515-9 à 515-13 (ordonnance protection JAF) + L.425-9 (étranger malade) pour articulation.
- Équivalent BE : régime de protection de la victime de violences conjugales BE — Loi 15/12/1980 art. 11 §2/9bis — modalités distinctes. **Justifié de différer** : F-209 P1 Immigration BE.

## Contrat API
**POST** `/api/v1/case-files/{caseFileId}/victime-violences-l4256-analysis`
```json
{
  "dateOrdonnanceProtection": "2026-03-01",
  "juridiction": "JAF Paris",
  "dureeProtectionMois": 6,
  "dateExpirationProtection": null,
  "enfantsAcharge": 2,
  "nationalite": "Marocaine"
}
```
Erreurs : 400 / 404.
