# SF-212-31 — Backend : outil décisionnel « élections CSE — conformité »

> Feature F-212. Outil : `F-DT-65-elections-cse-conformite`. Fondement : L. 2314-1 à L. 2314-37 CT ; R. 2314-1+ CT ; ordonnances Macron 22/09/2017 (création CSE).

## Objectif

Fournir le moteur backend qui vérifie la conformité du processus électoral du CSE (protocole d'accord préélectoral, collèges, candidatures, vote) et évalue les bases d'une contestation électorale.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/elections-cse-conformite` + `GET`.

L'analyseur vérifie :
- **Obligation d'élections** : entreprises ≥ 11 salariés (L. 2311-2). Carence si syndicat ne présente pas de candidats → PV de carence.
- **Protocole d'accord préélectoral (PAP)** : négocié avec les OS, fixe collèges, sièges, modalités de vote.
- **Calendrier** : invitation OS à négocier le PAP au moins 2 mois avant l'expiration des mandats (L. 2314-4).
- **Collèges électoraux** : au moins 2 (ouvriers/employés et agents de maîtrise/cadres) — L. 2314-11. Regroupement possible accord majoritaire.
- **Vote électronique** : accord collectif ou décision unilatérale employeur, expertise obligatoire.
- **Délai de contestation** : **15 jours** à compter de l'élection pour contester le résultat (L. 2314-32) — délai très court, P1 procédural.

Verdict `AnalyseElectionsCse` : `CONFORME` / `IRREGULARITE_MINEURE` / `IRREGULARITE_MAJEURE` + bases de contestation.

## Cas d'erreur

- `caseFileId` hors workspace → 404.
- Domaine ≠ `DROIT_DU_TRAVAIL` → 422.
- `country` ≠ `FRANCE` → 422 (CSE FR-only).
- Corps invalide → 400.

## Contrat API (figé — référence pour SF-212-32)

```
POST /api/v1/case-files/{caseFileId}/elections-cse-conformite
Request {
  effectifEntreprise: int,
  papNegocieAvecOS: boolean,
  delaiInvitationOSRespectee: boolean,
  colleges_conformes: boolean,
  resultatsContestes: boolean,
  dateElection: LocalDate|null,
  motifContestation: String|null
}
Response 200 {
  ...inputs (snapshot),
  analyseElectionsCse: CONFORME|IRREGULARITE_MINEURE|IRREGULARITE_MAJEURE,
  scoreConformite: int,
  pointsIrregularite: [{code, libelle, fondement}],
  delaiContestationJours: int,              // 15
  dateLimitContestationSiConnue: LocalDate|null,
  alerteDelaiContestation: boolean,
  basesJuridiques: [String],
  messages: [String],
  country: "FRANCE",
  calculatedAt: Instant
}
GET …/elections-cse-conformite → 200 | 204
```

`critereCode` F-IA-03 : `DT65_PAP_NEGOCIE`, `DT65_DELAI_INVITATION`, `DT65_COLLEGES`, `DT65_CONTESTATION`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `elections_cse_detail` :
`electionCseDateElection`, `electionCsePapNegocie`, `electionCseCollegesConformes`, `electionCseResultatsContestes`.
Extension `LegalDomainPromptBuilder`.

## Critères d'acceptation

1. PAP non négocié → `IRREGULARITE_MAJEURE`.
2. `delaiContestationJours = 15` invariant.
3. `alerteDelaiContestation = true` si date connue + < 15 j.
4. Effectif < 11 → message « CSE non obligatoire ».
5. 422 hors `FRANCE`.
6. Isolation workspace → 404.
7. `tool_id=F-DT-65-elections-cse-conformite` dans `KNOWN_FRONTEND_TOOL_IDS`.

## Plan de test

- **UT `ElectionsCseConformiteCalculatorTest`** : effectif < 11 ; PAP absent ; délai contestation.
- **IT `ElectionsCseConformiteControllerIT`**.

## Tables / endpoints / composants impactés

- **Nouvelle table** `elections_cse_conformite_analyses`.
- **Seed** : `tool_id=F-DT-65-elections-cse-conformite`, `trigger_field=election_cse_detectee`, `trigger_value=true`.
- Nouveaux fichiers + modifications standard.

## Hors périmètre

Frontend (→ SF-212-32). Négociation collective au CSE (P3, F-218). Protection représentants du personnel (couvert F-DT-30).
