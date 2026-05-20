# SF-212-01 — Backend : outil décisionnel « licenciement pour faute grave / faute lourde »

> Feature F-212 — P2 Travail FR — 22 outils fréquence haute. Cadrages : `SF-212-00-coherence.md` (GO), `SF-212-00b-ux-coherence.md` (GO).
> Outil : `F-DT-36-licenciement-faute-grave-lourde`. Fondement : L. 1234-1 CT ; L. 1234-9 CT ; jurisprudence Cass. soc. (faute grave : privation préavis + IL ; faute lourde : intention de nuire + dommages et intérêts au-delà).

## Objectif

Fournir le moteur backend qui analyse la qualification d'une faute disciplinaire (faute simple / grave / lourde) et calcule l'impact financier sur les indemnités de rupture — distinct de F-DT-08 qui valide la cause réelle et sérieuse générique.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/licenciement-faute-grave-lourde` reçoit la saisie, calcule via un `Calculator` stateless, persiste un snapshot et renvoie le verdict. `GET` renvoie le dernier snapshot.

L'analyseur évalue :
- **Qualification** : FAUTE_SIMPLE / FAUTE_GRAVE / FAUTE_LOURDE selon les faits décrits.
- **Faute grave** (L. 1234-1) : impossibilité de maintenir dans l'entreprise pendant le préavis → privation indemnité compensatrice de préavis (L. 1234-5) + privation indemnité légale de licenciement (L. 1234-9). Maintien du DIF/CPF.
- **Faute lourde** : intention de nuire à l'employeur (Cass. soc.) → privation des mêmes indemnités + possibilité dommages-intérêts au profit de l'employeur. Attention : depuis Cass. soc. 18/06/2013, la faute lourde ne prive plus le salarié des CP acquis.
- **Procédure** : délai de prescription de la faute (2 mois L. 1332-4), convocation entretien préalable, lettre de licenciement (L. 1232-6).

Verdict `QualificationFaute` : `FAUTE_SIMPLE` / `FAUTE_GRAVE` / `FAUTE_LOURDE` avec justification. Score de solidité de la qualification 0-100. Liste des `FacteurQualification(code, libelle, fondement, poids, explication)`.

Impact financier calculé selon la qualification :
- Faute simple : préavis + IL légale + CP.
- Faute grave : 0 préavis + 0 IL + CP (post-Cass. 18/06/2013).
- Faute lourde : 0 préavis + 0 IL + CP (post-Cass. 18/06/2013).

## Cas d'erreur

- `caseFileId` inexistant ou hors workspace → 404.
- Domaine ≠ `DROIT_DU_TRAVAIL` → 422.
- `country` ≠ `FRANCE` → 422.
- Corps invalide (enum inconnu, ancienneté négative) → 400.

## Contrat API (figé — référence pour SF-212-02)

```
POST /api/v1/case-files/{caseFileId}/licenciement-faute-grave-lourde
Request {
  faitsReproches: String,                   // description des faits (texte libre)
  datesFaits: [LocalDate],                  // dates des faits reprochés
  dateSaisineEmployeur: LocalDate|null,     // date à laquelle l'employeur a eu connaissance
  prescriptionFauteVerifiee: boolean,       // faits > 2 mois avant engagement procédure ?
  ancienneteMois: int,
  salaireMensuelBrutEuros: double,
  qualificationEmployeur: enum FAUTE_GRAVE|FAUTE_LOURDE|FAUTE_SIMPLE,
  intentionNuireAlleeguee: boolean,
  preuveIntentionNuire: String|null,
  attenteConvocation: boolean,              // entretien préalable tenu ?
  motivationLettreAdequate: boolean
}
Response 200 {
  ...inputs (snapshot),
  qualificationRetenue: FAUTE_SIMPLE|FAUTE_GRAVE|FAUTE_LOURDE,
  scoreQualification: int,                  // 0-100 solidité de la qualification
  facteursQualification: [{code, libelle, fondement, poids, explication}],
  indemnitePreavisEuros: double,
  indemniteLegaleEuros: double,
  indemnitesCongesPayesEuros: double,
  totalIndemnitesDuesEuros: double,
  alertePrescriptionFaute: boolean,         // true si faits > 2 mois
  basesJuridiques: [String],
  messages: [String],
  country: "FRANCE",
  calculatedAt: Instant
}
GET …/licenciement-faute-grave-lourde → 200 dernier snapshot | 204 si absent
```

`critereCode` F-IA-03 : `DT36_QUALIFICATION_FAUTE`, `DT36_PRESCRIPTION_FAUTE`, `DT36_INTENTION_NUIRE`, `DT36_PROCEDURE_DISCIPLINAIRE`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `faute_grave_detail` :
`fauteGraveFaitsReproches`, `fauteGraveDatesFaits`, `fauteGraveQualificationEmployeur`, `fauteGraveIntentionNuireAlleeguee`, `fauteGraveAncienneteMois`, `fauteGraveSalaireMensuelBrut`.
Extension `LegalDomainPromptBuilder` bloc `DROIT_DU_TRAVAIL` pour extraire ces champs + `critereCode` ci-dessus.

## Critères d'acceptation

1. Faute grave déclarée par l'employeur + pas d'intention de nuire → `qualificationRetenue = FAUTE_GRAVE`, `indemnitePreavisEuros = 0`, `indemniteLegaleEuros = 0`, `indemnitesCongesPayesEuros > 0`.
2. Faute lourde + intention de nuire prouvée → `qualificationRetenue = FAUTE_LOURDE`, mêmes impacts financiers que faute grave (règle Cass. 18/06/2013 sur les CP).
3. Faits > 2 mois avant engagement de procédure → `alertePrescriptionFaute = true`, facteur `DT36_PRESCRIPTION_FAUTE` détecté.
4. Faute simple → préavis + IL calculés normalement.
5. Outil renvoyé en 422 hors `DROIT_DU_TRAVAIL` / hors `FRANCE`.
6. Isolation workspace : cross-workspace → 404.
7. Pré-remplissage : les 6 champs `faute_grave_detail` extraits par le prompt.
8. `tool_id=F-DT-36-licenciement-faute-grave-lourde` présent dans `KNOWN_FRONTEND_TOOL_IDS`.

## Plan de test

- **UT `LicenciementFauteGraveLourdeCalculatorTest`** : chaque qualification (simple/grave/lourde) ; prescription faute ; intention de nuire ; CP post-Cass. 18/06/2013 ; cumul facteurs.
- **IT `LicenciementFauteGraveLourdeControllerIT`** : POST + GET, droits, domaine, pays, isolation workspace, 400 corps invalide.
- **IT visibilité** : `DecisionToolVisibilityIntegrityIT` reste vert.

## Tables / endpoints / composants impactés

- **Nouvelle table** `licenciement_faute_grave_lourde_analyses` (id, case_file_id, country, snapshot_data JSONB, calculated_at) — migration Liquibase.
- **Seed** `decision_tool_visibility_rules` : `tool_id=F-DT-36-licenciement-faute-grave-lourde`, `legal_domain=DROIT_DU_TRAVAIL`, `country=FRANCE`, `layer=CONTEXTUAL`, `trigger_field=motif_faute_grave_pressenti`, `trigger_value=true`.
- **Nouveaux fichiers** : `LicenciementFauteGraveLourdeCalculator`, `…Request`, `…Response`, `…Repository`, `…Service`, `…Controller`.
- **Modifiés** : `CaseAnalysisResponse.java`, `LegalDomainPromptBuilder.java`, `CaseFileDashboardService.java`.

## Préoccupations transversales

**Outil décisionnel métier** — création d'un analyseur distinct de F-DT-08. Invariant « un outil = une situation » respecté (situation = qualification de la faute disciplinaire avec impact financier). **Auth / workspace** : pattern existant. Smoke E2E non impacté.

## Hors périmètre

- Frontend (→ SF-212-02).
- Génération de la lettre de licenciement (couvert F-98).
- Calcul barème Macron si licenciement contesté ultérieurement (couvert F-DT-09).
