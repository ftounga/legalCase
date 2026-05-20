# SF-216-23 — Donation entre époux (avantage matrimonial) — backend

## Objectif

Outil décisionnel `F-FA-DONATION-ENTRE-EPOUX` : évalue la validité et l'étendue d'une donation entre époux (art. 1096 Cciv) — révocabilité, interaction avec la réserve héréditaire, effet de la dissolution du mariage, distinction avec l'avantage matrimonial.

## Comportement nominal

- Endpoint `POST/GET /api/v1/case-files/{caseFileId}/donation-entre-epoux`.
- Body :
  - `avantageMatrimonialType` (DONATION_BIEN_PRESENT | DONATION_BIENS_FUTURS | AVANTAGE_MATRIMONIAL | ASSURANCE_VIE | CLAUSE_ATTRIBUTION_INTEGRALE)
  - `dateContrat` (LocalDate, optionnel)
  - `regimeMatrimonial` — enum
  - `mariageDissous` (boolean, optionnel) — si divorce/séparation en cours
  - `revocabiliteDetectee` (boolean, optionnel) — révocation expresse ou tacite (art. 1096 al. 2)
  - `bienDonneType` (IMMOBILIER | MOBILIER | PORTEFEUILLE | NUMERAIRE | AUTRE)
  - `valeurBienDonneEur` (int, optionnel)
  - `enfantsNonCommunsDetected` (boolean, optionnel) — enfants non communs = action en retranchement possible art. 1527 al. 2
- Calculator :
  - **Révocabilité** : toute donation entre époux est révocable jusqu'au décès (art. 1096 al. 1, sauf biens présents dans certains régimes).
  - **Dissolution mariage** : si divorce prononcé → révocation de plein droit des donations faites à l'ex-conjoint (art. 265 al. 2 Cciv).
  - **Clause attribution intégrale** (avantage matrimonial) : si `CLAUSE_ATTRIBUTION_INTEGRALE` + `enfantsNonCommuns` → action en retranchement possible (art. 1527 al. 2).
  - **Réserve héréditaire** : donation biens futurs testamentaires soumise à la quotité disponible.
- Retourne : `validite`, `revocabilite`, `effetDisso`, `actionRetranchementPossible`, `impactReserve`, `baseLegale`, `messages`, `alertes`.
- Persiste 1:1 par dossier.

## Cas d'erreur

- `country ≠ FRANCE` → 400.
- `legalDomain ≠ DROIT_FAMILLE` → 400.

## Source juridique

- **art. 1096 Cciv** — donation entre époux.
- **art. 265 al. 2 Cciv** — révocation donation en cas de divorce.
- **art. 1527 al. 2 Cciv** — action en retranchement enfants non communs (avantage matrimonial excessif).
- **art. 912-928 Cciv** — réserve héréditaire.
- **Cass. 1ère civ., 4/6/2014** — distinction avantage matrimonial / libéralité.

## Champs IA à extraire (FamilleExtractedData)

**Réutilisés (F-246)** :
- `regimes_vie_commune_detection_v2.regimeMatrimonialDetecte`
- `regimes_vie_commune_detection_v2.clauseAttributionIntegraleDetected`
- `regimes_vie_commune_detection_v2.enfantsNonCommunsDetected`
- `regimes_vie_commune_detection_v2.contratNotarieDetected`

**Nouveaux champs à ajouter** :
- `donationEntreEpouxEnvisagee` (boolean | null) — détecté si mention « donation entre époux », « avantage matrimonial », « art. 1096 ».
- `revocabiliteDetectee` (boolean | null) — révocation expresse mentionnée dans les pièces.
- `bienDonnePrincipalType` (String | null) — type de bien donné identifié.

## Plan de test

- UT calculator : (a) clause attribution intégrale + enfants non communs → action retranchement ; (b) divorce prononcé + donation biens futurs → révocation automatique ; (c) donation biens présents + révocabilité → alerte.
- UT service : gates.
- IT : POST + GET.

## Composants impactés

- Migration Liquibase 293 : table `donation_entre_epoux_analyses`.
- Migration Liquibase 294 : INSERT `decision_tool_visibility_rules` CONTEXTUAL `donationEntreEpouxEnvisagee`, `DROIT_FAMILLE`, `FRANCE`, priority 109.
- Java : `DonationEntreEpouxCalculator`, result, analysis, repository, service, controller.
- `CaseAnalysisResponse.java` — ajout `donationEntreEpouxEnvisagee`, `revocabiliteDetectee`, `bienDonnePrincipalType`.
- `LegalDomainPromptBuilder`.

## Critères d'acceptation

- AC1 : clause attribution intégrale + enfants non communs → action retranchement.
- AC2 : divorce → révocation automatique art. 265 al. 2.
- AC3 : `country=BELGIQUE` → 400.

## Hors périmètre

- Frontend (SF-216-24).
- Donation entre vifs générique (outil existant F-FA-24-donation).
