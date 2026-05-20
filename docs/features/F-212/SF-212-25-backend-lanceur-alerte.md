# SF-212-25 — Backend : outil décisionnel « protection du lanceur d'alerte »

> Feature F-212. Outil : `F-DT-61-lanceur-alerte-protection`. Fondement : L. 1132-3-3 CT ; loi Sapin II n° 2016-1691 du 09/12/2016 ; loi Waserman n° 2022-401 du 21/03/2022 (transposition directive 2019/1937).

## Objectif

Fournir le moteur backend qui vérifie si un salarié bénéficie du statut de lanceur d'alerte et analyse les mesures de représailles éventuelles, en tenant compte du régime renforcé de la loi Waserman 2022.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/lanceur-alerte-protection` + `GET`.

L'analyseur vérifie :
- **Qualité de lanceur d'alerte** (loi Waserman 2022) : personne physique qui signale ou divulgue, sans contrepartie financière directe, des informations portant sur un crime, un délit, une violation du droit de l'UE ou une menace pour l'intérêt général.
- **Procédure de signalement** : interne (référent désigné obligatoire ≥ 50 salariés) ou externe (autorités compétentes : Défenseur des droits, AFA, ANSSI, etc.) ou, à titre subsidiaire, divulgation publique.
- **Mesures de représailles** interdites (L. 1132-3-3) : licenciement, sanction, mesure discriminatoire → nullité de la mesure ; réintégration possible.
- **Dommages et intérêts** : minimum 10 000 € depuis loi Waserman 2022 (sanction civile pour procédure abusive contre le lanceur d'alerte).
- **Protection préventive** : le lanceur d'alerte peut demander des mesures provisoires au juge.

Verdict `AnalyseLanceurAlerte` : `PROTECTION_FORTE` / `PROTECTION_PARTIELLE` / `HORS_CHAMP` + mesures recommandées.

## Cas d'erreur

- `caseFileId` hors workspace → 404.
- Domaine ≠ `DROIT_DU_TRAVAIL` → 422.
- `country` ≠ `FRANCE` → 422 (loi Waserman FR-only).
- Corps invalide → 400.

## Contrat API (figé — référence pour SF-212-26)

```
POST /api/v1/case-files/{caseFileId}/lanceur-alerte-protection
Request {
  natureSignalement: enum CRIME_DELIT|VIOLATION_DROIT_UE|MENACE_INTERET_GENERAL|AUTRE,
  contrepartieFinanciere: boolean,
  procedureSignalement: enum INTERNE|EXTERNE|DIVULGATION_PUBLIQUE,
  referentInterneSaisi: boolean|null,
  mesureRepresailleDetectee: boolean,
  natureMesureRepresaille: enum LICENCIEMENT|SANCTION|MESURE_DISCRIMINATOIRE|AUTRE|AUCUNE
}
Response 200 {
  ...inputs (snapshot),
  analyseLanceurAlerte: PROTECTION_FORTE|PROTECTION_PARTIELLE|HORS_CHAMP,
  scoreProtection: int,
  pointsAnalyse: [{code, libelle, fondement, conclusion}],
  nulliteMesureRepresaille: boolean,
  montantMinDommagesInterets: double,        // 10000 si PROTECTION_FORTE ou PARTIELLE
  basesJuridiques: [String],
  messages: [String],
  country: "FRANCE",
  calculatedAt: Instant
}
GET …/lanceur-alerte-protection → 200 | 204
```

`critereCode` F-IA-03 : `DT61_NATURE_SIGNALEMENT`, `DT61_CONTREPARTIE`, `DT61_PROCEDURE`, `DT61_MESURE_REPRESAILLE`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `lanceur_alerte_detail` :
`lanceurAlerteNatureSignalement`, `lanceurAlerteProcedure`, `lanceurAlerteMesureRepresaille`, `lanceurAlerteNatureMesure`.
Extension `LegalDomainPromptBuilder`.

## Critères d'acceptation

1. Signalement crime/délit + licenciement → `PROTECTION_FORTE`, `nulliteMesureRepresaille = true`, `montantMinDommagesInterets = 10000`.
2. Contrepartie financière → `HORS_CHAMP`.
3. Procédure interne non suivie (référent disponible mais pas saisi) → `PROTECTION_PARTIELLE`.
4. 422 hors `FRANCE`.
5. Isolation workspace → 404.
6. `tool_id=F-DT-61-lanceur-alerte-protection` dans `KNOWN_FRONTEND_TOOL_IDS`.

## Plan de test

- **UT `LanceurAlerteProtectionCalculatorTest`** : nature × procédure × représailles.
- **IT `LanceurAlerteProtectionControllerIT`**.

## Tables / endpoints / composants impactés

- **Nouvelle table** `lanceur_alerte_protection_analyses`.
- **Seed** : `tool_id=F-DT-61-lanceur-alerte-protection`, `trigger_field=lanceur_alerte_detecte`, `trigger_value=true`.
- Nouveaux fichiers + modifications standard.

## Hors périmètre

Frontend (→ SF-212-26). Signalement pénal (hors périmètre V1 droit du travail).
