# SF-212-19 — Backend : outil décisionnel « mise à pied disciplinaire »

> Feature F-212. Outil : `F-DT-48-mise-a-pied-disciplinaire`. Fondement : L. 1331-1 CT ; L. 1332-1 à L. 1332-3 CT (procédure disciplinaire) ; jurisprudence Cass. soc.

## Objectif

Fournir le moteur backend qui analyse la régularité d'une mise à pied disciplinaire (durée, procédure, suspension de salaire) et la distingue de la mise à pied conservatoire — les deux étant souvent confondues.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/mise-a-pied-disciplinaire` + `GET`.

L'analyseur vérifie :
- **Nature de la mise à pied** : disciplinaire (sanction définitive, inscrite au dossier, prive de salaire) vs conservatoire (mesure provisoire dans l'attente de la décision, normalement rémunérée si la sanction finale est inférieure à un licenciement — Cass. soc. 26/10/2010).
- **Procédure** (L. 1332-1 à L. 1332-3) : convocation entretien préalable, entretien tenu, délai de notification de la sanction (2 mois à compter de la connaissance des faits — L. 1332-4).
- **Durée** : la durée d'une mise à pied disciplinaire doit être définie dans le règlement intérieur (L. 1311-2) ou dans l'accord collectif. Si indéfinie : irrégularité formelle.
- **Suspension de salaire** : uniquement pour la mise à pied disciplinaire ; si la sanction finale est une mise à pied conservatoire suivie d'un licenciement, le salaire de la période conservatoire est dû (sauf faute lourde).
- **Double sanction** (jurisprudence Cass.) : interdiction de sanctionner deux fois les mêmes faits.

Verdict `AnalyseMiseAPied` : `REGULIERE` / `IRREGULIERE_FORME` / `IRREGULIERE_FOND` / `CONSERVATOIRE` + liste des `PointRegularite`.

## Cas d'erreur

- `caseFileId` hors workspace → 404.
- Domaine ≠ `DROIT_DU_TRAVAIL` → 422.
- Corps invalide → 400.

## Contrat API (figé — référence pour SF-212-20)

```
POST /api/v1/case-files/{caseFileId}/mise-a-pied-disciplinaire
Request {
  natureMiseAPied: enum DISCIPLINAIRE|CONSERVATOIRE|INCONNUE,
  procedureEntretienSuivie: boolean,
  prescriptionFauteVerifiee: boolean,       // faits < 2 mois avant sanction
  dureeDefiniedansRI: boolean,
  dureeJours: int,
  salaireSuspendu: boolean,
  sancionsAnterieuresMemesFaits: boolean,
  salaireMensuelBrutEuros: double
}
Response 200 {
  ...inputs (snapshot),
  analyseMiseAPied: REGULIERE|IRREGULIERE_FORME|IRREGULIERE_FOND|CONSERVATOIRE,
  scoreMiseAPied: int,
  pointsRegularite: [{code, libelle, fondement, conclusion}],
  salaireDuPeriodeMiseAPiedEuros: double,    // 0 si disciplinaire régulière
  basesJuridiques: [String],
  messages: [String],
  calculatedAt: Instant
}
GET …/mise-a-pied-disciplinaire → 200 | 204
```

`critereCode` F-IA-03 : `DT48_NATURE_MISE_A_PIED`, `DT48_PROCEDURE_SUIVIE`, `DT48_PRESCRIPTION_FAUTE`, `DT48_DOUBLE_SANCTION`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `mise_a_pied_detail` :
`miseAPiedNature`, `miseAPiedProcedureSuivie`, `miseAPiedPrescription`, `miseAPiedDureeJours`.
Extension `LegalDomainPromptBuilder`.

## Critères d'acceptation

1. Mise à pied disciplinaire + procédure suivie + durée dans RI → `REGULIERE`, salaire suspendu = 0 dû.
2. Nature conservatoire + sanction finale = licenciement → `CONSERVATOIRE`, salaire dû pendant période.
3. Double sanction mêmes faits → `IRREGULIERE_FOND`.
4. Prescription dépassée → `IRREGULIERE_FOND`.
5. 422 hors `DROIT_DU_TRAVAIL`.
6. Isolation workspace → 404.
7. `tool_id=F-DT-48-mise-a-pied-disciplinaire` dans `KNOWN_FRONTEND_TOOL_IDS`.

## Plan de test

- **UT `MiseAPiedDisciplinaireCalculatorTest`** : nature ; procédure ; double sanction ; prescription.
- **IT `MiseAPiedDisciplinaireControllerIT`**.

## Tables / endpoints / composants impactés

- **Nouvelle table** `mise_a_pied_disciplinaire_analyses`.
- **Seed** : `tool_id=F-DT-48-mise-a-pied-disciplinaire`, `trigger_field=mise_a_pied_disciplinaire_detectee`, `trigger_value=true`.
- Nouveaux fichiers + modifications standard.

## Hors périmètre

Frontend (→ SF-212-20). Règlement intérieur en tant que tel (P3, F-218).
