# SF-212-21 — Backend : outil décisionnel « démission équivoque »

> Feature F-212. Outil : `F-DT-41-demission-validite-equivoque`. Fondement : L. 1237-1 CT ; jurisprudence Cass. soc. sur la volonté claire et non équivoque de démissionner.

## Objectif

Fournir le moteur backend qui évalue si une démission peut être requalifiée en raison de son caractère équivoque (démission donnée sous pression, lors d'un conflit, par mail dans l'instant, sous état émotionnel) — situation fréquente où la démission ne repose pas sur une volonté libre et éclairée.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/demission-validite-equivoque` + `GET`.

L'analyseur évalue le caractère équivoque :
- **Contexte de la démission** : lors d'une altercation, sous pression, via SMS/mail impulsif, immédiatement après un entretien difficile.
- **Conditions** : pression, contrainte, menace, altercation, état émotionnel perturbé.
- **Temporalité** : démission retirée rapidement (dans les heures ou jours suivants).
- **Manquements de l'employeur** contemporains (impayés, harcèlement) → les griefs peuvent révéler que la démission n'est pas libre.
- **Jurisprudence** : la démission doit résulter d'une volonté claire et non équivoque (Cass. soc. 09/05/2007) ; si équivoque → le CPH peut la requalifier en prise d'acte ou en licenciement sans cause réelle et sérieuse.

Verdict `AnalyseDemission` : `VOLONTE_CLAIRE` / `DEMISSION_EQUIVOQUE` / `RETRACTATION_POSSIBLE` + scoring équivocité 0-100.

## Cas d'erreur

- `caseFileId` hors workspace → 404.
- Domaine ≠ `DROIT_DU_TRAVAIL` → 422.
- Corps invalide → 400.

## Contrat API (figé — référence pour SF-212-22)

```
POST /api/v1/case-files/{caseFileId}/demission-validite-equivoque
Request {
  modeExpressionDemission: enum ECRIT_FORMEL|EMAIL|SMS|ORAL|AUTRE,
  contexteAltercation: boolean,
  pressionOuMenace: boolean,
  retractationDansDelai: boolean,
  delaiRetractationJours: int|null,
  manquementsEmployeurContemporains: boolean,
  etatEmotionnelPerturbé: boolean
}
Response 200 {
  ...inputs (snapshot),
  analyseDemission: VOLONTE_CLAIRE|DEMISSION_EQUIVOQUE|RETRACTATION_POSSIBLE,
  scoreEquivocite: int,
  facteursEquivocite: [{code, libelle, fondement, poids, explication}],
  requalificationPossible: boolean,
  basesJuridiques: [String],
  messages: [String],
  calculatedAt: Instant
}
GET …/demission-validite-equivoque → 200 | 204
```

`critereCode` F-IA-03 : `DT41_CONTEXTE_ALTERCATION`, `DT41_PRESSION`, `DT41_RETRACTATION`, `DT41_MANQUEMENTS_EMPLOYEUR`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `demission_equivoque_detail` :
`demissionModeExpression`, `demissionContexteAltercation`, `demissionPression`, `demissionRetractation`, `demissionManquementsEmployeur`.
Extension `LegalDomainPromptBuilder`.

## Critères d'acceptation

1. Altercation + pression → `DEMISSION_EQUIVOQUE`, score élevé, `requalificationPossible = true`.
2. Rétractation rapide → `RETRACTATION_POSSIBLE`.
3. Écrit formel sans contexte particulier → `VOLONTE_CLAIRE`.
4. Manquements contemporains → facteur aggravant équivocité.
5. 422 hors `DROIT_DU_TRAVAIL`.
6. Isolation workspace → 404.
7. `tool_id=F-DT-41-demission-validite-equivoque` dans `KNOWN_FRONTEND_TOOL_IDS`.

## Plan de test

- **UT `DemissionValiditeEquivoqueCalculatorTest`** : altercation ; pression ; rétractation ; combinaisons.
- **IT `DemissionValiditeEquivoqueControllerIT`**.

## Tables / endpoints / composants impactés

- **Nouvelle table** `demission_validite_equivoque_analyses`.
- **Seed** : `tool_id=F-DT-41-demission-validite-equivoque`, `trigger_field=demission_equivoque_pressentie`, `trigger_value=true`.
- Nouveaux fichiers + modifications standard.

## Hors périmètre

Frontend (→ SF-212-22). Prise d'acte (couvert F-206 SF-206-05). Résiliation judiciaire (F-206 SF-206-07).
