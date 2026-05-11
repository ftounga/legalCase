# SF-211-03 — Mesures provisoires Tribunal de la famille BE — backend

## Objectif (1 phrase)
Analyser le niveau d'urgence d'une situation familiale belge (violence, déplacement enfant, dilapidation patrimoine) et recommander les mesures provisoires applicables sous l'art. 1280 CJ (résidence séparée, contribution alimentaire provisoire, autorité parentale exclusive temporaire).

## Comportement nominal
- POST `/api/v1/case-files/{caseFileId}/tribunal-famille-be-mesures-provisoires-analysis`
- Body : `violenceFamiliale` (boolean), `deplacementEnfantImminent` (boolean), `dilapidationPatrimoine` (boolean), `besoinResidenceSeparee` (boolean), `besoinContributionAlimentaire` (boolean), `besoinAutoriteParentaleExclusive` (boolean)
- Calculator `TribunalFamilleBeMesuresProvisoiresCalculator` calcule :
  - Score d'urgence :
    - violenceFamiliale → +3
    - deplacementEnfantImminent → +3
    - dilapidationPatrimoine → +2
  - `urgenceLevel` ∈ {HAUTE (≥ 3), MOYENNE (2), BASSE (1), AUCUNE (0)}
  - Liste de `mesuresRecommandees` selon les besoins exprimés (résidence séparée, contribution provisoire, autorité parentale exclusive)
  - `verdict` ∈ {URGENT_REFERE, NORMAL_AUDIENCE, AUCUNE_MESURE}
- Persistance 1:1 `tribunal_famille_be_mesures_provisoires_analyses`
- GET → 200 ou 404

## Cas d'erreur
- 400 si tous les flags à false (aucune urgence détectée — invalide pour cet outil)
- 400 si workspace.country ≠ BELGIQUE
- 400 si caseFile.legalDomain ≠ DROIT_FAMILLE
- 404 isolation workspace

## Critères d'acceptation vérifiables
- [x] Violence familiale → HAUTE
- [x] Déplacement enfant → HAUTE
- [x] Dilapidation patrimoine seul → MOYENNE
- [x] Aucun besoin déclaré → 400
- [x] POST FR retourne 400
- [x] GET sans POST → 404

## Plan de test minimal
- **UT** `TribunalFamilleBeMesuresProvisoiresCalculatorTest` : 10+ tests (chaque niveau d'urgence, combinaisons, mesures, cas vides)

## Tables / endpoints / composants impactés
- **Nouvelle table** `tribunal_famille_be_mesures_provisoires_analyses` (id, case_file_id UNIQUE, violence_familiale BOOLEAN, deplacement_enfant_imminent BOOLEAN, dilapidation_patrimoine BOOLEAN, besoin_residence_separee BOOLEAN, besoin_contribution_alimentaire BOOLEAN, besoin_autorite_parentale_exclusive BOOLEAN, country VARCHAR(20) NOT NULL, result_data TEXT NOT NULL, timestamps)
- **Migration** `226-create-tribunal-famille-be-mesures-provisoires-analyses.xml` (table — pas de seed visibility ici)
- **Endpoint** `TribunalFamilleBeMesuresProvisoiresController` (POST, GET)

## Hors périmètre
- Composant Angular (SF F-211 frontend ultérieure)
- Seed `decision_tool_visibility_rules` (différé pour CI verte ; mode ALWAYS_ON BE Famille prévu)

## Impact par domaine métier
**Sensible Famille BE uniquement.** Concept FR équivalent = juge aux affaires familiales en référé (art. 255 CC FR — mesures provisoires divorce). En BE, le tribunal de la famille statue en référé sur art. 1280 CJ. Aucun impact Travail / Immigration.

## Parité des domaines métier
Niveau 5 (scoring urgence). FR équivalent : art. 255 CC FR (mesures provisoires divorce) couvert par F-FA-05 généralisé — pas de scoring d'urgence FR distinct dédié.

## Analyse de cohérence transversale
- **Référence pattern** : F-208 calculators (JldRetentionCalculator pour scoring d'urgence/score numérique).
- **Pas de chevauchement** avec DC-BE (SF-211-01) ou DDI-BE (SF-211-02) — outil orthogonal couvrant la phase urgences pendant la procédure.

## Audit "Impact F-166 cross-C×D"
- **BE×Famille** : nouvel outil ALWAYS_ON candidat (urgences transversales). Seed différé.
- Autres : non concernés.

## Audit "exhaustivité droit national BE"
- Source juridique : CJ art. 1280 (mesures urgentes et provisoires), art. 1253ter+ (tribunal de la famille).
- Compétence : tribunal de la famille en référé.
- FR équivalent : art. 255 CC FR + art. 1071 CPC FR (procédure de référé familial) — couvert différemment.

## Contrat API
**POST** `/api/v1/case-files/{caseFileId}/tribunal-famille-be-mesures-provisoires-analysis`
```json
{
  "violenceFamiliale": true,
  "deplacementEnfantImminent": false,
  "dilapidationPatrimoine": false,
  "besoinResidenceSeparee": true,
  "besoinContributionAlimentaire": true,
  "besoinAutoriteParentaleExclusive": false
}
```
Réponse :
```json
{
  "caseFileId": "...",
  "country": "BELGIQUE",
  "scoreUrgence": 3,
  "urgenceLevel": "HAUTE",
  "mesuresRecommandees": ["RESIDENCE_SEPAREE", "CONTRIBUTION_ALIMENTAIRE_PROVISOIRE"],
  "verdict": "URGENT_REFERE",
  "formule": "Situation urgente nécessitant référé immédiat...",
  "baseJuridique": "CJ art. 1280, art. 1253ter+",
  "messages": ["..."]
}
```
