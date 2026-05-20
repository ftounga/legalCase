# SF-212-13 — Backend : outil décisionnel « mutation — validité de la clause de mobilité »

> Feature F-212. Outil : `F-DT-71-mutation-clause-mobilite`. Fondement : L. 1221-1 CT ; jurisprudence Cass. soc. sur la clause de mobilité (zone géographique précise, intérêt légitime, mise en oeuvre bonne foi, délai de prévenance).

## Objectif

Fournir le moteur backend qui analyse la validité d'une clause de mobilité et les conséquences d'un refus de mutation du salarié.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/mutation-clause-mobilite` + `GET`.

L'analyseur vérifie :
- **Existence de la clause** dans le contrat de travail.
- **Validité de la clause** (jurisprudence Cass. soc.) :
  - Zone géographique définie avec précision (pas de clause « France entière » générale sauf si secteur d'activité le justifie — Cass. soc. 24/01/2008).
  - Intérêt légitime de l'entreprise.
  - Mise en oeuvre de bonne foi (pas de détournement de pouvoir — Cass. soc. 23/02/2005).
  - Délai de prévenance raisonnable (Cass. soc. 03/03/2010 : délai insuffisant = modification).
  - Pas d'atteinte disproportionnée à la vie personnelle et familiale.
- **Conséquences du refus** :
  - Clause valide : refus = faute pouvant justifier un licenciement.
  - Clause invalide (zone imprécise, délai insuffisant) : refus = pas de faute, licenciement éventuel = sans cause réelle et sérieuse.
  - Mutation sans clause : refus peut constituer un refus de modification du contrat.

Verdict `AnalyseMutation` : `CLAUSE_VALIDE` / `CLAUSE_INVALIDE` / `ABSENCE_CLAUSE` + conséquences du refus.

## Cas d'erreur

- `caseFileId` hors workspace → 404.
- Domaine ≠ `DROIT_DU_TRAVAIL` → 422.
- Corps invalide → 400.

## Contrat API (figé — référence pour SF-212-14)

```
POST /api/v1/case-files/{caseFileId}/mutation-clause-mobilite
Request {
  clauseMobilitePresente: boolean,
  zoneGeographiquePrecise: boolean,            // zone définie avec précision
  interetLegitimeEmployeur: boolean,
  delaiPrevenanceSemaines: int,
  situationFamilialeSalarieContraingnante: boolean,  // enfants, conjoint, etc.
  motifMutationProfessionnel: boolean,
  reponseSalarie: enum REFUS|ACCEPTATION|EN_ATTENTE
}
Response 200 {
  ...inputs (snapshot),
  analyseMutation: CLAUSE_VALIDE|CLAUSE_INVALIDE|ABSENCE_CLAUSE,
  scoreMutation: int,
  pointsAnalyse: [{code, libelle, fondement, conclusion}],
  consequencesRefus: String,
  basesJuridiques: [String],
  messages: [String],
  calculatedAt: Instant
}
GET …/mutation-clause-mobilite → 200 | 204
```

`critereCode` F-IA-03 : `DT71_CLAUSE_PRESENTE`, `DT71_ZONE_PRECISE`, `DT71_INTERET_LEGITIME`, `DT71_DELAI_PREVENANCE`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `mutation_detail` :
`mutationClausePresente`, `mutationZonePrecise`, `mutationDelaiPrevenance`, `mutationSituationFamiliale`.
Extension `LegalDomainPromptBuilder`.

## Critères d'acceptation

1. Clause présente + zone précise + délai raisonnable → `CLAUSE_VALIDE`, refus = faute.
2. Zone imprécise → `CLAUSE_INVALIDE`, refus sans faute.
3. Délai de prévenance < 2 semaines → facteur invalide.
4. Absence de clause → `ABSENCE_CLAUSE`, conséquences = modification du contrat.
5. 422 hors `DROIT_DU_TRAVAIL`.
6. Isolation workspace → 404.
7. `tool_id=F-DT-71-mutation-clause-mobilite` dans `KNOWN_FRONTEND_TOOL_IDS`.

## Plan de test

- **UT `MutationClauseMobiliteCalculatorTest`** : chaque combinaison validité ; délai.
- **IT `MutationClauseMobiliteControllerIT`**.

## Tables / endpoints / composants impactés

- **Nouvelle table** `mutation_clause_mobilite_analyses`.
- **Seed** : `tool_id=F-DT-71-mutation-clause-mobilite`, `trigger_field=mutation_refusee`, `trigger_value=true`.
- Nouveaux fichiers + modifications standard.

## Hors périmètre

Frontend (→ SF-212-14). Génération courrier de refus (F-98).
