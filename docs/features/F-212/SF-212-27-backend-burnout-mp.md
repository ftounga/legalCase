# SF-212-27 — Backend : outil décisionnel « burn-out — reconnaissance maladie professionnelle hors tableau »

> Feature F-212. Outil : `F-DT-64-burnout-reconnaissance`. Fondement : L. 461-1 CSS ; tableau 57 maladies professionnelles (affections péri-articulaires, non le burn-out) ; comité régional de reconnaissance des maladies professionnelles (CRRMP) ; circulaire DGT 2016/01.

## Objectif

Fournir le moteur backend qui évalue les chances de reconnaissance du burn-out comme maladie professionnelle via la procédure CRRMP (comité régional), lorsque la pathologie n'est pas inscrite aux tableaux officiels.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/burnout-reconnaissance-mp` + `GET`.

L'analyseur évalue la **procédure hors tableau** (L. 461-1 al. 4 et 5 CSS) :
- Conditions : la maladie doit être directement causée par le travail + incapacité permanente ≥ 25 % OU décès du salarié.
- **Lien direct** entre le travail et la pathologie : rapport médical, témoignages, conditions de travail (surcharge, manquements à l'obligation sécurité L. 4121-1, harcèlement).
- **Instruction CRRMP** : 3 membres (médecin conseil CPAM, médecin inspecteur régional du travail, médecin spécialiste).
- **Taux IPP** : si reconnu, constitution d'une rente + possibilité d'action en faute inexcusable (F-DT-91).
- Délai d'instruction : 4 à 6 mois.

Verdict `AnalyseBurnoutMp` : `CHANCES_IMPORTANTES` / `CHANCES_MODEREES` / `CHANCES_FAIBLES` + `FicheSommaire` pour le dossier CRRMP.

## Cas d'erreur

- `caseFileId` hors workspace → 404.
- Domaine ≠ `DROIT_DU_TRAVAIL` → 422.
- `country` ≠ `FRANCE` → 422 (CRRMP FR-only).
- Corps invalide → 400.

## Contrat API (figé — référence pour SF-212-28)

```
POST /api/v1/case-files/{caseFileId}/burnout-reconnaissance-mp
Request {
  diagnosticBurnoutPose: boolean,
  tauxIPPEstime: double,                     // % — doit être >= 25 pour CRRMP
  anneesExpositionProfessionnelle: int,
  surchargeChargeDocumentee: boolean,
  manquementsSecuriteDocumentes: boolean,
  harcelementConcomitant: boolean,
  arretsMaladieMultiples: boolean,
  lienCausalDirectEtabli: boolean
}
Response 200 {
  ...inputs (snapshot),
  analyseChancesCRRMP: CHANCES_IMPORTANTES|CHANCES_MODEREES|CHANCES_FAIBLES,
  scoreChances: int,
  conditionIPPRemplie: boolean,              // true si tauxIPP >= 25
  facteursDossier: [{code, libelle, fondement, poids}],
  alerteIPPInsuffisante: boolean,            // true si tauxIPP < 25
  delaiInstructionMois: int,                 // 4-6
  basesJuridiques: [String],
  messages: [String],
  country: "FRANCE",
  calculatedAt: Instant
}
GET …/burnout-reconnaissance-mp → 200 | 204
```

`critereCode` F-IA-03 : `DT64_DIAGNOSTIC_POSE`, `DT64_TAUX_IPP`, `DT64_LIEN_CAUSAL`, `DT64_SURCHARGE_DOCUMENTEE`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `burnout_detail` :
`burnoutDiagnostic`, `burnoutTauxIPP`, `burnoutSurchargeDocumentee`, `burnoutArretsMaladie`.
Extension `LegalDomainPromptBuilder`.

## Critères d'acceptation

1. Taux IPP < 25 % → `alerteIPPInsuffisante = true`, `CHANCES_FAIBLES`.
2. Diagnostic + lien causal + surcharge documentée → `CHANCES_IMPORTANTES`.
3. `conditionIPPRemplie` correct (seuil 25 %).
4. `delaiInstructionMois = 4 à 6` toujours présent.
5. 422 hors `FRANCE`.
6. Isolation workspace → 404.
7. `tool_id=F-DT-64-burnout-reconnaissance-mp` dans `KNOWN_FRONTEND_TOOL_IDS`.

## Plan de test

- **UT `BurnoutReconnaissanceMpCalculatorTest`** : seuil IPP ; lien causal ; combinaisons.
- **IT `BurnoutReconnaissanceMpControllerIT`**.

## Tables / endpoints / composants impactés

- **Nouvelle table** `burnout_reconnaissance_mp_analyses`.
- **Seed** : `tool_id=F-DT-64-burnout-reconnaissance-mp`, `trigger_field=burnout_detecte`, `trigger_value=true`.
- Nouveaux fichiers + modifications standard.

## Hors périmètre

Frontend (→ SF-212-28). Faute inexcusable consécutive à reconnaissance MP (couvert SF-212-09).
