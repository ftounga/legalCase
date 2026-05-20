# SF-216-11 — Retrait d'autorité parentale FR — backend

## Objectif

Outil décisionnel `F-FA-RETRAIT-AP` : évalue la recevabilité d'une demande de retrait total ou partiel de l'autorité parentale (art. 378-381 Cciv + loi 2/3/2022 violences) et détermine la voie procédurale (retrait accessoire à une condamnation pénale vs retrait civil JAF).

## Comportement nominal

- Endpoint `POST/GET /api/v1/case-files/{caseFileId}/retrait-autorite-parentale`.
- Body :
  - `typeRetrait` (TOTAL | PARTIEL_EXERCICE | PARTIEL_ATTRIBUTS)
  - `motifRetrait` (CONDAMNATION_PENALE | DANGER_CARACTERISE_VIOLENCES | DESINTERET_GRAVE | COMPORTEMENT_GRAVEMENT_COMPROMETTANT | VIOLENCES_LMVSS_2022)
  - `condamnationPenaleDetectee` (boolean)
  - `dangerCaracterise` (boolean)
  - `violencesConjugalesDetectees` (boolean) — loi 2/3/2022 : retrait automatique si violence sur le conjoint en présence de l'enfant
  - `ageEnfant` (int, requis)
  - `decisionsJudiciairesPrecedentes` (boolean, optionnel)
- Calculator :
  - **Retrait accessoire pénal** (art. 378 al. 1) : si condamnation pénale pour crime sur l'enfant → retrait de plein droit.
  - **Retrait civil JAF** (art. 378 al. 2) : si mauvais traitements, abus d'autorité, comportement compromettant + saisine JAF.
  - **Loi 2/3/2022 LMVSS** : si violences conjugales graves en présence de l'enfant → suspension puis retrait accéléré.
  - **Conséquences** : délégation à tiers ? tutelle ouverte ? admissibilité en vue d'adoption ?
- Retourne : `verdictRetrait`, `voieProcédurale`, `consequencesJuridiques`, `admissibiliteAdoption`, `baseLegale`, `messages`, `alertes`.
- Persiste 1:1 par dossier.

## Cas d'erreur

- `country ≠ FRANCE` → 400.
- `legalDomain ≠ DROIT_FAMILLE` → 400.
- `ageEnfant > 18` → 400.

## Source juridique

- **art. 378-381 Cciv** — retrait total et partiel de l'autorité parentale.
- **Loi n°2022-140 du 7/2/2022** (LMVSS) — retrait accéléré si violences conjugales en présence de l'enfant.
- **art. 378-1 Cciv** — retrait partiel (attributs ou exercice seulement).
- **art. 343-1 al. 2 Cciv** — retrait AP = condition d'admissibilité adoption intra-familiale.
- **Cass. 1ère civ., 26/10/2011** — conditions du retrait civil.

## Champs IA à extraire (FamilleExtractedData)

**Réutilisés (F-246)** :
- `protection_divorce_detection_v2.violencesAllegueesDetectees`
- `protection_divorce_detection_v2.dangerCaracteriseDetecte`
- `filiation_detection_v2.agesEnfantsDetectes`

**Nouveaux champs à ajouter** :
- `retraitApEnvisage` (boolean | null) — détecté si mention « retrait autorité parentale », « art. 378 », « déchéance parentale ».
- `condamnationPenaleDetectee` (boolean | null) — détecté si mention « condamné », « jugement correctionnel/criminel », « peine prononcée ».
- `violencesLmvss2022Detectees` (boolean | null) — détecté si mention « violences conjugales en présence de l'enfant », « loi 2022 » + violences.

## Plan de test

- UT calculator : (a) condamnation pénale crime enfant → retrait de plein droit art. 378 al. 1 ; (b) danger + JAF → retrait civil ; (c) loi 2022 violences → suspension accélérée ; (d) retrait total → `admissibiliteAdoption=true`.
- UT service : gates.
- IT controller : POST + GET.

## Composants impactés

- Migration Liquibase 281 : table `retrait_ap_analyses`.
- Migration Liquibase 282 : INSERT `decision_tool_visibility_rules` CONTEXTUAL `retraitApEnvisage`, `DROIT_FAMILLE`, `FRANCE`, priority 103.
- Java : `RetraitAutoriteParentaleCalculator`, result, analysis, repository, service, controller.
- `CaseAnalysisResponse.java` — ajout `retraitApEnvisage`, `condamnationPenaleDetectee`, `violencesLmvss2022Detectees`.
- `LegalDomainPromptBuilder`.

## Critères d'acceptation

- AC1 : condamnation pénale → retrait de plein droit.
- AC2 : violences loi 2022 → alerte suspension accélérée.
- AC3 : retrait total → `admissibiliteAdoption = true`.
- AC4 : `country=BELGIQUE` → 400.

## Hors périmètre

- Frontend (SF-216-12).
- Déchéance autorité parentale loi 2/3/2022 volet pénal (art. 227-3 CP — F-222 P3).
