# SF-216-21 — Recel de succession FR — backend

## Objectif

Outil décisionnel `F-FA-RECEL-SUCCESSION` : détecte et qualifie un recel de succession (art. 778 Cciv) — dissimulation d'un bien successoral — et évalue les sanctions civiles applicables (privation de la portion sur le bien recelé + obligation de rapport sans émolument).

## Comportement nominal

- Endpoint `POST/GET /api/v1/case-files/{caseFileId}/recel-succession`.
- Body :
  - `typeRecel` (DISSIMULATION_BIEN | DISSIMULATION_DONATION | DESTRUCTION_TESTAMENT | RECEL_CREANCE | AUTRE)
  - `bienCeleValeurEur` (int, optionnel)
  - `preuveRecel` (AVEUX | DOCUMENT | TEMOIGNAGE | EXPERTISE | FAISCEAU_INDICES | AUCUNE)
  - `receleurQualite` (HERITIER | LEGATAIRE | DONATAIRE | TIERS_COMPLICITE)
  - `dateOuvertureSuccession` (LocalDate, requis)
  - `actionIntentee` (boolean, optionnel)
- Calculator :
  - **Qualification** : art. 778 al. 1 — dissimulation intentionnelle + élément matériel.
  - **Sanction civile** : privation de la part successorale sur le bien recelé (art. 778 al. 2). Obligation de rapporter sans émolument.
  - **Preuve** : charge sur les cohéritiers demandeurs ; faisceau d'indices suffisant (Cass.).
  - **Délai** : 5 ans depuis l'ouverture ou la découverte.
  - **Articulation pénale** : si `typeRecel` = destruction testament → art. 441-8 CP (destruction de titre) signalé.
- Retourne : `qualificationRecel`, `sanctionCivile`, `preuveRequise`, `delaiAction`, `baseLegale`, `messages`, `alertes`.
- Persiste 1:1 par dossier.

## Cas d'erreur

- `country ≠ FRANCE` → 400.
- `dateOuvertureSuccession` future → 400.

## Source juridique

- **art. 778 Cciv** — recel de succession + sanction civile.
- **Cass. 1ère civ., 14/11/2012** — caractérisation du recel par faisceau d'indices.
- **art. 441-8 CP** — destruction de titre (volet pénal complémentaire).

## Champs IA à extraire (FamilleExtractedData)

**Réutilisés (F-246)** :
- `succession_detection_v2.dateOuvertureSuccessionDetectee`
- `succession_detection_v2.montantSuccessionEur`
- `succession_detection_v2.montantDonationsRecuesEur`

**Nouveaux champs à ajouter** :
- `recelSuccessionDetecte` (boolean | null) — détecté si mention « recel succession », « bien dissimulé », « art. 778 ».
- `typeRecelDetecte` (String | null) — type de recel qualifié dans les pièces.
- `preuveRecelDetectee` (String | null) — nature de la preuve mentionnée dans les pièces.

## Plan de test

- UT calculator : (a) dissimulation bien + faisceau indices → sanction art. 778 ; (b) destruction testament → articulation pénale signalée ; (c) sans preuve → verdict insuffisant.
- UT service : gates.
- IT : POST + GET.

## Composants impactés

- Migration Liquibase 291 : table `recel_succession_analyses`.
- Migration Liquibase 292 : INSERT `decision_tool_visibility_rules` CONTEXTUAL `recelSuccessionDetecte`, `DROIT_FAMILLE`, `FRANCE`, priority 108.
- Java : `RecelSuccessionCalculator`, result, analysis, repository, service, controller, `TypeRecelEnum`.
- `CaseAnalysisResponse.java` — ajout `recelSuccessionDetecte`, `typeRecelDetecte`, `preuveRecelDetectee`.
- `LegalDomainPromptBuilder`.

## Critères d'acceptation

- AC1 : dissimulation bien + preuve → sanction art. 778 calculée.
- AC2 : destruction testament → alerte volet pénal.
- AC3 : `country=BELGIQUE` → 400.

## Hors périmètre

- Frontend (SF-216-22).
- Volet pénal art. 441-8 CP (référencé, non implémenté — hors scope LegalCase V1).
