# SF-216-17 — Adoption internationale FR — backend

## Objectif

Outil décisionnel `F-FA-ADOPTION-INTERNATIONALE` : vérifie les conditions d'agrément, la conformité à la Convention de La Haye du 29/5/1993 et à l'art. 370-3+ Cciv pour une adoption internationale, et détermine la voie procédurale (OAA agréé / voie autonome) ainsi que la nécessité d'un exequatur.

## Comportement nominal

- Endpoint `POST/GET /api/v1/case-files/{caseFileId}/adoption-internationale`.
- Body :
  - `paysOrigineEnfant` (String ISO-3166, requis)
  - `conventionLaHayeApplicable` (boolean) — pays d'origine a ratifié la Convention
  - `agrement2025` (boolean, requis) — agrément délivré par le Conseil départemental (depuis 2002, durée 5 ans)
  - `ageAdoptant` (int, requis)
  - `ageAdopte` (int, requis)
  - `adoptantMarie` (boolean)
  - `voieProcedure` (OAA_AGREE | VOIE_AUTONOME | ORGANISME_ETAT)
  - `formeAdoptionDemandee` (PLENIERE | SIMPLE)
  - `exequaturRequis` (boolean, optionnel) — si décision rendue à l'étranger
- Calculator :
  - **Agrément** : condition sine qua non en France pour toute adoption internationale (loi 2002, renouvelable 5 ans).
  - **Convention La Haye 1993** : si pays signataire → procédure bilatérale via Autorité centrale. Délais typiques 2-5 ans.
  - **Hors convention** : possibilité voie autonome ou OAA mais risque de refus de transcription en France (art. 370-3 al. 2 Cciv).
  - **Exequatur** : si une décision étrangère d'adoption a été rendue → demande exequatur TJ (art. 370-5 Cciv).
  - **Alerte kafala** : si pays d'origine = Maroc / Algérie / Tunisie → kafala ≠ adoption (irréductiblement inacceptable en droit FR — art. 370-3 al. 3 Cciv).
- Retourne : `conditionsRemplies`, `voieProcedure`, `conventionApplicable`, `alerteKafala`, `exequaturRequis`, `delaiEstime`, `baseLegale`, `messages`, `alertes`.
- Persiste 1:1 par dossier.

## Cas d'erreur

- `country ≠ FRANCE` → 400.
- `agrement2025 = false` → verdict `AGREMENT_REQUIS` (pas d'erreur 400, verdict bloquant).
- `paysOrigineEnfant = null` → 400.

## Source juridique

- **art. 370-3 à 370-5 Cciv** — conditions de l'adoption internationale.
- **Convention La Haye 29/5/1993** — protection enfants + coopération en matière d'adoption internationale.
- **Loi n°2001-111 du 6/2/2001** — agrément obligatoire pour adoption internationale.
- **Cass. 1ère civ., 10/10/2006** — kafala incompatible avec adoption FR.
- **Loi n°2022-219 du 21/2/2022** — réforme adoption : nouvelles conditions âge + OAA.

## Champs IA à extraire (FamilleExtractedData)

**Réutilisés (F-246)** :
- `filiation_detection_v2.agesEnfantsDetectes`
- `filiation_detection_v2.adoptantMarieDetected`
- `filiation_detection_v2.pupilleEtatDetected`

**Nouveaux champs à ajouter** :
- `adoptionInternationaleEnvisagee` (boolean | null) — détecté si mention « adoption internationale », « Convention La Haye », « OAA », « agrément ».
- `paysOrigineAdopteDetecte` (String | null) — pays d'origine extrait des pièces.
- `agrement2025DetecteValide` (boolean | null) — mention d'un agrément valide dans les pièces.
- `exequaturRequisDetecte` (boolean | null) — décision étrangère mentionnée dans les pièces.

## Plan de test

- UT calculator : (a) agrément valide + pays signataire → voie OAA agréé + convention ; (b) sans agrément → verdict AGREMENT_REQUIS ; (c) kafala Maroc → alerte kafala ; (d) décision étrangère → exequatur requis.
- UT service : gates.
- IT : POST + GET.

## Composants impactés

- Migration Liquibase 287 : table `adoption_internationale_analyses`.
- Migration Liquibase 288 : INSERT `decision_tool_visibility_rules` CONTEXTUAL `adoptionInternationaleEnvisagee`, `DROIT_FAMILLE`, `FRANCE`, priority 106.
- Java : `AdoptionInternationaleCalculator`, result, analysis, repository, service, controller.
- `CaseAnalysisResponse.java` — ajout `adoptionInternationaleEnvisagee`, `paysOrigineAdopteDetecte`, `agrement2025DetecteValide`, `exequaturRequisDetecte`.
- `LegalDomainPromptBuilder`.

## Critères d'acceptation

- AC1 : pays signataire + agrément → voie OAA convention, délai estimé.
- AC2 : kafala Maroc → alerte kafala incompatible.
- AC3 : sans agrément → verdict AGREMENT_REQUIS.
- AC4 : `country=BELGIQUE` → 400.

## Hors périmètre

- Frontend (SF-216-18).
- Reconnaissance jugement étranger en général (F-FA-EXEQUATUR-AC — P3+).
