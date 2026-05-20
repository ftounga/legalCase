# SF-212-03 — Backend : outil décisionnel « forfait jours — validité et rappel heures supplémentaires »

> Feature F-212. Outil : `F-DT-50-forfait-jours-validite`. Fondement : L. 3121-58 à L. 3121-66 CT ; Cass. soc. 29/06/2011 (Syntec) ; jurisprudence Cass. soc. sur accord collectif insuffisant.

## Objectif

Fournir le moteur backend qui analyse la validité d'une convention de forfait jours et, si le forfait est nul, calcule le rappel d'heures supplémentaires sur 3 ans (L. 3245-1).

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/forfait-jours-validite` + `GET`.

L'analyseur vérifie :
- **Accord collectif** : existence d'un accord d'entreprise ou de branche autorisant le forfait (L. 3121-63). Synthèse exigeante post-Cass. 29/06/2011 : l'accord doit garantir le respect de la santé et du repos du salarié (mécanisme d'alerte, entretien annuel, suivi charge travail).
- **Entretien annuel de charge** : réalisé et formalisé (L. 3121-65) ; absence = nullité.
- **Suivi jours travaillés** : existence d'un document de contrôle mensuel (L. 3121-66).
- **Catégorie cadre autonome** : le salarié remplit-il les conditions (cadre autonome ou ETAM ayant la maîtrise de son temps, L. 3121-58).
- **Nombre de jours** : ≤ 218 j/an (L. 3121-64, sauf accord supérieur à 235 j max).

Verdict `ValiditeForfait` : `VALIDE` / `PARTIELLEMENT_NULE` / `NULLE` + justification. Si nulle : calcul du rappel d'heures supplémentaires estimé sur 3 ans (hypothèse 10 h HS/semaine × 45 sem × 3 ans, modulable par l'avocat) avec application des majorations 25 % / 50 %.

## Cas d'erreur

- `caseFileId` inexistant ou hors workspace → 404.
- Domaine ≠ `DROIT_DU_TRAVAIL` → 422.
- `country` ≠ `FRANCE` → 422 (forfait jours FR-only).
- Corps invalide → 400.

## Contrat API (figé — référence pour SF-212-04)

```
POST /api/v1/case-files/{caseFileId}/forfait-jours-validite
Request {
  accordCollectifExiste: boolean,
  accordGarantitSuiviCharge: boolean,
  entretienAnnuelRealise: boolean,
  documentControleMensuelExiste: boolean,
  categorieAutonomeConfirmee: boolean,
  nbJoursForfait: int,                         // ex. 218
  ancienneteMois: int,
  salaireMensuelBrutEuros: double,
  hsEstimeesParSemaine: int,                   // estimation HS/sem pour le rappel
  nbSemainesParAn: int                         // défaut 45
}
Response 200 {
  ...inputs (snapshot),
  validiteForfait: VALIDE|PARTIELLEMENT_NULLE|NULLE,
  scoreValidite: int,
  facteursInvalidite: [{code, libelle, fondement, poids, explication}],
  rappelHsEstimeEuros: double|null,            // null si VALIDE
  prescriptionRappelAns: int,                  // 3
  basesJuridiques: [String],
  messages: [String],
  country: "FRANCE",
  calculatedAt: Instant
}
GET …/forfait-jours-validite → 200 | 204
```

`critereCode` F-IA-03 : `DT50_ACCORD_COLLECTIF`, `DT50_ENTRETIEN_ANNUEL`, `DT50_SUIVI_CHARGE`, `DT50_CATEGORIE_AUTONOME`, `DT50_NB_JOURS`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `forfait_jours_detail` :
`forfaitJoursAccordCollectifExiste`, `forfaitJoursEntretienAnnuelRealise`, `forfaitJoursDocumentControle`, `forfaitJoursCategorieAutonome`, `forfaitJoursNbJours`.
Extension `LegalDomainPromptBuilder`.

## Critères d'acceptation

1. Accord collectif absent → `validiteForfait = NULLE`, facteur `DT50_ACCORD_COLLECTIF`.
2. Accord présent mais sans garantie suivi charge (post-Cass. 29/06/2011) → `PARTIELLEMENT_NULLE`.
3. Entretien annuel absent → facteur `DT50_ENTRETIEN_ANNUEL`, invalide.
4. Nb jours > 218 sans accord majoré → facteur `DT50_NB_JOURS`.
5. Forfait nul → `rappelHsEstimeEuros` calculé (HS × majorations × 3 ans).
6. Forfait valide → `rappelHsEstimeEuros = null`.
7. 422 hors `DROIT_DU_TRAVAIL` / hors `FRANCE`.
8. Isolation workspace → 404.
9. `tool_id=F-DT-50-forfait-jours-validite` dans `KNOWN_FRONTEND_TOOL_IDS`.

## Plan de test

- **UT `ForfaitJoursValiditeCalculatorTest`** : accord absent ; accord présent sans garantie ; entretien absent ; nb jours > 218 ; combinaisons ; rappel HS.
- **IT `ForfaitJoursValiditeControllerIT`** : POST + GET, domaine, pays, workspace.

## Tables / endpoints / composants impactés

- **Nouvelle table** `forfait_jours_validite_analyses` (id, case_file_id, country, snapshot_data JSONB, calculated_at).
- **Seed** `decision_tool_visibility_rules` : `tool_id=F-DT-50-forfait-jours-validite`, `layer=CONTEXTUAL`, `trigger_field=forfait_jours_detecte`, `trigger_value=true`.
- **Nouveaux fichiers** : `ForfaitJoursValiditeCalculator`, `…Request`, `…Response`, `…Repository`, `…Service`, `…Controller`.
- **Modifiés** : `CaseAnalysisResponse.java`, `LegalDomainPromptBuilder.java`, `CaseFileDashboardService.java`.

## Hors périmètre

- Frontend (→ SF-212-04).
- Cas Syntec spécifique par CCN (le calcul est générique ; la CCN Syntec est un paramétrage documenté mais non seedé dans cette SF).
