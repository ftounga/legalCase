# SF-216-15 — Adoption de l'enfant du conjoint (adoption intra-familiale) — backend

## Objectif

Outil décisionnel `F-FA-ADOPTION-INTRA` : évalue les conditions de recevabilité d'une adoption de l'enfant du conjoint dans un couple recomposé (art. 343-1 al. 2 + 345-1 Cciv, réforme 21/2/2022) et détermine s'il faut procéder en adoption simple ou en adoption plénière.

> Outil distinct de `F-FA-18-adoption` (adoption générique plénière/simple) — situation juridique spécifique : parent biologique + conjoint de ce parent.

## Comportement nominal

- Endpoint `POST/GET /api/v1/case-files/{caseFileId}/adoption-intra-familiale`.
- Body :
  - `formeAdoptionDemandee` (PLENIERE | SIMPLE)
  - `ageAdoptant` (int, requis) — âge du conjoint adoptant
  - `ageAdopte` (int, requis)
  - `mariageOuPacsAdoptantParent` (boolean, requis) — lien mariage ou PACS (exigé par art. 345-1)
  - `consentementEnfant` (boolean, optionnel) — exigé si enfant ≥ 13 ans (art. 360 al. 2)
  - `filiationOrigineRompue` (boolean, optionnel) — adoption plénière rompt la filiation avec l'autre parent biologique
  - `consentementAutreParentBiologique` (boolean, optionnel) — si autre parent biologique vivant + non déchu
  - `decheanceAutreParent` (boolean, optionnel)
  - `autreParentDecede` (boolean, optionnel)
- Calculator :
  - **Conditions communes** : adoptant marié/pacsé avec le parent + durée de vie commune ≥ 1 an (art. 345-1 al. 1).
  - **Adoption plénière** (art. 345-1 al. 2) : possible si l'autre parent biologique est décédé, déchu, ou a consenti. Rompt toute filiation antérieure (irréversible).
  - **Adoption simple** (art. 360 Cciv) : cumulable avec la filiation d'origine — recommandée si autre parent vivant et présent.
  - **Consentement enfant** ≥ 13 ans → requis.
  - **Réforme 2022** : âge minimum de l'adopté supprimé (ex. possible dès la naissance).
  - **Alerte irréversibilité** : adoption plénière = rupture définitive de la filiation d'origine → message fort.
- Retourne : `formePossible[]`, `conditionsRemplies`, `alerteIrreversibilite`, `consentementRequis`, `baseLegale`, `messages`, `alertes`.
- Persiste 1:1 par dossier.

## Cas d'erreur

- `country ≠ FRANCE` → 400 (réforme 2022 FR-only).
- `ageAdoptant < 28` et `ageAdopte + 10 ≥ ageAdoptant` → alerte (art. 344 al. 1 — 15 ans de différence dans les cas généraux, mais 343-1 déroge pour intra-familiale : 10 ans de différence suffisent).
- `mariageOuPacsAdoptantParent = false` → 400.

## Source juridique

- **art. 343-1 al. 2 Cciv** — adoption par conjoint du parent.
- **art. 345-1 Cciv** — adoption plénière de l'enfant du conjoint.
- **art. 360-362 Cciv** — adoption simple.
- **Loi n°2022-219 du 21/2/2022** — réforme adoption : abaissement conditions d'âge, extension au couple pacsé.
- **Cass. 1ère civ., 9/3/2011** — conditions du consentement de l'autre parent.

## Champs IA à extraire (FamilleExtractedData)

**Réutilisés (F-246)** :
- `filiation_detection_v2.agesEnfantsDetectes`
- `filiation_detection_v2.adoptantMarieDetected`

**Nouveaux champs à ajouter** :
- `adoptionIntraEnvisagee` (boolean | null) — détecté si mention « adoption enfant du conjoint », « couple recomposé + adoption », « 345-1 ».
- `mariageOuPacsAdoptantParentDetecte` (boolean | null) — lien mariage/PACS entre adoptant et parent détecté.
- `consentementAutreParentDetecte` (boolean | null) — consentement de l'autre parent biologique détecté.
- `autreParentDecedeDetecte` (boolean | null) — autre parent décédé détecté dans les pièces.

## Plan de test

- UT calculator : (a) marié + autre parent décédé → adoption plénière + simple possibles ; (b) autre parent vivant sans consentement → simple seulement ; (c) enfant 14 ans → consentement requis ; (d) non marié non pacsé → 400.
- UT service : gates pays.
- IT : POST + GET.

## Composants impactés

- Migration Liquibase 285 : table `adoption_intra_analyses`.
- Migration Liquibase 286 : INSERT `decision_tool_visibility_rules` CONTEXTUAL `adoptionIntraEnvisagee`, `DROIT_FAMILLE`, `FRANCE`, priority 105.
- Java : `AdoptionIntraCalculator`, result, analysis, repository, service, controller.
- `CaseAnalysisResponse.java` — ajout `adoptionIntraEnvisagee`, `mariageOuPacsAdoptantParentDetecte`, `consentementAutreParentDetecte`, `autreParentDecedeDetecte`.
- `LegalDomainPromptBuilder`.

## Critères d'acceptation

- AC1 : marié + autre parent décédé → plénière et simple possibles.
- AC2 : autre parent vivant non consentant → simple seulement, alerte.
- AC3 : alerte irréversibilité adoption plénière systématique.
- AC4 : enfant ≥ 13 ans → consentement requis signalé.
- AC5 : `country=BELGIQUE` → 400.

## Hors périmètre

- Frontend (SF-216-16).
- Adoption internationale (SF-216-17/18).
- Adoption générique plénière/simple (outil existant `F-FA-18-adoption`).
