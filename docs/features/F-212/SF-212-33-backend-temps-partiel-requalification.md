# SF-212-33 — Backend : outil décisionnel « temps partiel — requalification en temps plein »

> Feature F-212. Outil : `F-DT-49-temps-partiel-requalification`. Fondement : L. 3123-1 à L. 3123-20 CT ; L. 3123-9 CT (heures complémentaires) ; Cass. soc. sur l'absence des mentions obligatoires.

## Objectif

Fournir le moteur backend qui détecte les irrégularités d'un contrat de travail à temps partiel et évalue la possibilité de requalification en temps complet avec rappel de salaire.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/temps-partiel-requalification` + `GET`.

L'analyseur vérifie :
- **Mentions obligatoires** du contrat TP (L. 3123-6) : durée hebdomadaire (ou mensuelle), répartition des horaires entre les jours de la semaine ou les semaines du mois, conditions modification répartition, limites heures complémentaires.
- **Absence de mentions** : si le contrat écrit ne mentionne pas la durée ou la répartition → **présomption de temps complet** (Cass. soc. 22/01/1992 — présomption réfragable).
- **Heures complémentaires abusives** (L. 3123-9) : > 1/3 de la durée contractuelle → requalification partielle (10 % majorées au-delà de 10 %).
- **Modification unilatérale répartition** : si l'employeur modifie sans respecter la procédure → salarié peut refuser.
- **Rappel de salaire** : si requalification → rappel des heures non rémunérées sur 3 ans (L. 3245-1).

Verdict `AnalyseTempsPartielRequalification` : `REQUALIFICATION_PROBABLE` / `REQUALIFICATION_POSSIBLE` / `PAS_DE_REQUALIFICATION` + calcul rappel.

## Cas d'erreur

- `caseFileId` hors workspace → 404.
- Domaine ≠ `DROIT_DU_TRAVAIL` → 422.
- Corps invalide → 400.

## Contrat API (figé — référence pour SF-212-34)

```
POST /api/v1/case-files/{caseFileId}/temps-partiel-requalification
Request {
  dureeContractuelleHeures: double,          // durée hebdo contractuelle
  mentionsDureePresentes: boolean,
  mentionsRepartitionPresentes: boolean,
  mentionsHCPresentes: boolean,
  heuresComplementairesReelleMoyenneHeures: double,  // HC effectuées en moyenne/sem
  modificationRepartitionUnilaterale: boolean,
  ancienneteMois: int,
  salaireMensuelContractuelEuros: double
}
Response 200 {
  ...inputs (snapshot),
  analyseRequalification: REQUALIFICATION_PROBABLE|REQUALIFICATION_POSSIBLE|PAS_DE_REQUALIFICATION,
  scoreRequalification: int,
  facteursRequalification: [{code, libelle, fondement, poids}],
  rappelSalaire3AnsEstimeEuros: double|null,
  basesJuridiques: [String],
  messages: [String],
  calculatedAt: Instant
}
GET …/temps-partiel-requalification → 200 | 204
```

`critereCode` F-IA-03 : `DT49_MENTIONS_DUREE`, `DT49_MENTIONS_REPARTITION`, `DT49_HC_ABUSIVES`, `DT49_MODIFICATION_UNILATERALE`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `temps_partiel_detail` :
`tempsPartielDureeContractuelle`, `tempsPartielMentionsDuree`, `tempsPartielMentionsRepartition`, `tempsPartielHCMoyenne`.
Extension `LegalDomainPromptBuilder`.

## Critères d'acceptation

1. Mentions durée absentes → `REQUALIFICATION_PROBABLE`.
2. HC > 1/3 durée contractuelle → facteur `DT49_HC_ABUSIVES`, `REQUALIFICATION_POSSIBLE`.
3. Rappel salaire estimé sur 3 ans si requalification probable/possible.
4. 422 hors `DROIT_DU_TRAVAIL`.
5. Isolation workspace → 404.
6. `tool_id=F-DT-49-temps-partiel-requalification` dans `KNOWN_FRONTEND_TOOL_IDS`.

## Plan de test

- **UT `TempsPartielRequalificationCalculatorTest`** : mentions absentes ; HC > 1/3 ; modification unilatérale.
- **IT `TempsPartielRequalificationControllerIT`**.

## Tables / endpoints / composants impactés

- **Nouvelle table** `temps_partiel_requalification_analyses`.
- **Seed** : `tool_id=F-DT-49-temps-partiel-requalification`, `trigger_field=temps_partiel_requalification_envisagee`, `trigger_value=true`.
- Nouveaux fichiers + modifications standard.

## Hors périmètre

Frontend (→ SF-212-34). Requalification CDD en CDI (couvert F-DT-22).
