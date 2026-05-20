# SF-212-09 — Backend : outil décisionnel « faute inexcusable de l'employeur »

> Feature F-212. Outil : `F-DT-91-faute-inexcusable-employeur`. Fondement : L. 452-1 à L. 452-5 CSS ; Cass. ass. plén. 24/06/2005 (conscience du danger) ; art. L. 4121-1 CT (obligation sécurité).

## Objectif

Fournir le moteur backend qui évalue si l'AT/MP est susceptible d'être reconnu comme dû à la faute inexcusable de l'employeur, et calcule la majoration de rente et les préjudices indemnisables — distinct de F-DT-33-at-mp qui se limite à la déclaration et aux IJ.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/faute-inexcusable-employeur` + `GET`.

L'analyseur vérifie les **conditions de la faute inexcusable** (Cass. ass. plén. 24/06/2005) :
- L'employeur avait ou aurait dû avoir **conscience du danger** auquel était exposé le salarié.
- L'employeur n'a **pas pris les mesures nécessaires** pour l'en préserver (obligation de sécurité L. 4121-1).
- Procédure : recours amiable devant la CPAM (reconnaissance judiciaire possible) → action devant le **pôle social du TJ** (non le CPH) — distinction procédurale essentielle.

Calcul indemnisation :
- **Majoration de la rente** : portée au maximum légal (L. 452-2 CSS).
- **Préjudices personnels** du salarié indemnisables (Cass. ass. plén. 24/06/2005 et CC décision 2010-8 QPC) : souffrances physiques et morales, préjudice esthétique, agrément, sexuel, établissement.
- **Recours subrogation CPAM** contre l'employeur.

Verdict `EvaluationFauteInexcusable` : `FAUTE_INEXCUSABLE_PROBABLE` / `FAUTE_INEXCUSABLE_POSSIBLE` / `FAUTE_INEXCUSABLE_PEU_PROBABLE` + liste des `FacteurFauteInexcusable`.

## Cas d'erreur

- `caseFileId` hors workspace → 404.
- Domaine ≠ `DROIT_DU_TRAVAIL` → 422.
- Corps invalide → 400.

## Contrat API (figé — référence pour SF-212-10)

```
POST /api/v1/case-files/{caseFileId}/faute-inexcusable-employeur
Request {
  conscienceDangerEmployeurEtablie: boolean,
  signalementDangerPrior: boolean,           // salarié ou tiers a signalé le danger avant l'AT
  mesuresPreventionPrises: boolean,
  documentUniqueEvalue: boolean,             // DUER à jour ?
  formationSecuriteProdiguee: boolean,
  taux IPP: double,                          // taux d'IPP reconnu par CPAM (%)
  renteMensuelleEuros: double|null,
  salaireMensuelBrutEuros: double
}
Response 200 {
  ...inputs (snapshot),
  evaluationFauteInexcusable: FAUTE_INEXCUSABLE_PROBABLE|FAUTE_INEXCUSABLE_POSSIBLE|FAUTE_INEXCUSABLE_PEU_PROBABLE,
  scoreFauteInexcusable: int,
  facteursFauteInexcusable: [{code, libelle, fondement, poids, explication}],
  majorationRenteEstimeeEuros: double|null,
  alerteProcedurePolesSocial: String,        // "Action devant le pôle social du TJ, non devant le CPH"
  basesJuridiques: [String],
  messages: [String],
  calculatedAt: Instant
}
GET …/faute-inexcusable-employeur → 200 | 204
```

`critereCode` F-IA-03 : `DT91_CONSCIENCE_DANGER`, `DT91_SIGNALEMENT_PRIOR`, `DT91_MESURES_PREVENTION`, `DT91_DUER`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `faute_inexcusable_detail` :
`fauteInexcusableConscienceDanger`, `fauteInexcusableSignalementPrior`, `fauteInexcusableMesuresPrevention`, `fauteInexcusableTauxIPP`.
Extension `LegalDomainPromptBuilder`.

## Critères d'acceptation

1. Conscience du danger + pas de mesures → `FAUTE_INEXCUSABLE_PROBABLE`.
2. Signalement prior détecté → facteur `DT91_SIGNALEMENT_PRIOR`, poids fort.
3. DUER non évalué → facteur `DT91_DUER`.
4. `alerteProcedurePolesSocial` toujours présent dans la réponse (invariant : jamais oublier la distinction procédurale).
5. Majoration de rente calculée si taux IPP > 0 et rente existante.
6. 422 hors `DROIT_DU_TRAVAIL`.
7. Isolation workspace → 404.
8. `tool_id=F-DT-91-faute-inexcusable-employeur` dans `KNOWN_FRONTEND_TOOL_IDS`.

## Plan de test

- **UT `FauteInexcusableEmployeurCalculatorTest`** : conscience du danger ; signalement ; mesures prises ; calcul majoration rente.
- **IT `FauteInexcusableEmployeurControllerIT`**.

## Tables / endpoints / composants impactés

- **Nouvelle table** `faute_inexcusable_employeur_analyses`.
- **Seed** : `tool_id=F-DT-91-faute-inexcusable-employeur`, `trigger_field=faute_inexcusable_envisagee`, `trigger_value=true`.
- Nouveaux fichiers + modifications standard.

## Hors périmètre

- Frontend (→ SF-212-10).
- Calcul IJ majorées AT/MP (couvert F-DT-33).
- Procédure pénale éventuelle (hors périmètre V1).
