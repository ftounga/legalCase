# Mini-spec — F-207 / SF-207-08-backend Outil outplacement obligatoire 45+ BE

## Identifiant

`F-207 / SF-207-08-backend` · Statut : `ready` · Date : 2026-05-20 · Branche : `feat/SF-207-08-backend-outplacement-be-obligatoire-45`

## Cadrages amont

Étape 0 / 0 bis F-207 livrées #1119. Pattern source : `C4OnemChecklist*` (SF-207-02, checklist + verdict + sanction) ; pattern BE workspace gate de SF-207-01..07.

## Objectif

Analyseur de conformité de l'**outplacement obligatoire pour salariés 45+** (CCT 82 ; CCT 82 bis ; Loi du 5 septembre 2001 art. 13) — vérifie les conditions d'obligation employeur (âge, ancienneté, motif, type contrat), évalue si l'offre a été faite et est conforme (60 h sur 12 mois dans les 15 j de la rupture), quantifie la **sanction employeur** (1 800 €) ou la **sanction salarié** (exclusion ONEM 4-52 sem) selon le cas. Outil BE-only.

## Substance juridique (BE strict)

- **CCT 82** (CCT 82 bis pour temps partiel) — outplacement obligatoire pour salariés ≥ 45 ans, ≥ 1 an d'ancienneté, licenciés (hors faute grave).
- **Loi du 5 septembre 2001 art. 13** — base légale, sanction administrative.
- **Offre par écrit** dans les **15 jours** du licenciement, programme de **60 heures sur 12 mois maximum**.
- **Sanction employeur** non-offre/non-conformité : 1 800 € (amende administrative, AR 30/05/2018).
- **Sanction salarié** refus offre conforme : exclusion allocations chômage 4-52 semaines (AR 25/11/1991 art. 154).

## Contrat API

`POST /api/v1/case-files/{caseFileId}/decision-tools/outplacement-be-obligatoire-45`

Inputs (`OutplacementBeRequest`) :
```json
{
  "dateLicenciement": "2026-04-15",                 // requis
  "dateNaissanceSalarie": "1972-03-10",              // requis (réutilisé SF-207-06)
  "ancienneteAnnees": 12.5,                          // requis (≥ 0, BigDecimal en années avec décimales possibles)
  "motifLicenciement": "LICENCIEMENT_ECONOMIQUE",    // enum : LICENCIEMENT_ECONOMIQUE | LICENCIEMENT_AUTRE | FAUTE_GRAVE | DEMISSION
  "contratTempsPlein": true,                         // boolean — false = CCT 82 bis applicable
  "offreOutplacementRecue": false,                   // boolean — offre formelle reçue
  "dateOffreOutplacement": null,                     // optionnel ; requis si offreOutplacementRecue=true
  "offreConformeCCT82": null,                        // optionnel — true/false ; requis si offreRecue=true
  "salarieAcceptantOffre": null                       // optionnel — true/false/null
}
```

Réponse 200 :
```json
{
  "verdict": "OUTPLACEMENT_NON_DU"
           | "OFFRE_CONFORME_RESPECTEE"
           | "OFFRE_NON_CONFORME_SANCTION_EMPLOYEUR"
           | "OFFRE_NON_FAITE_SANCTION_EMPLOYEUR"
           | "OFFRE_REFUSEE_SANCTION_SALARIE",
  "ageALaDateLicenciement": 54,
  "obligationEmployeurApplicable": true,
  "raisonNonObligation": null,                       // ou enum : AGE_INFERIEUR_45 | ANCIENNETE_INFERIEURE_1AN | FAUTE_GRAVE | DEMISSION
  "sanctionEmployeurEuros": 1800.00,                  // 0 si OUTPLACEMENT_NON_DU ou OFFRE_CONFORME
  "sanctionSalarieRange": { "minSemaines": 4, "maxSemaines": 52 } | null,
  "dateLimiteOffre": "2026-04-30",                    // dateLicenciement + 15 jours
  "delaiOffreRespecte": false,                        // true si dateOffre <= dateLimiteOffre, null si pas d'offre
  "etapeSuivante": "PLAINTE_INSPECTION_LOIS_SOCIALES" | "PRESENTATION_C4_ONEM" | "AUCUNE",
  "baseJuridique": "CCT 82 ; CCT 82 bis ; loi du 5 septembre 2001 art. 13 ; AR 30/05/2018 (sanction administrative) ; AR 25/11/1991 art. 154 (sanction salarié)",
  "formuleCalcul": "Salarié 54 ans (≥ 45 ✓), ancienneté 12,5 ans (≥ 1 ✓), licenciement économique (≠ faute grave ✓) → outplacement obligatoire. Offre non reçue → sanction employeur 1 800 € (AR 30/05/2018)."
}
```

## Logique (`OutplacementBeCalculator`)

1. **Calcul âge** : `dateLicenciement - dateNaissanceSalarie` en années pleines.
2. **Test obligation** :
   - Si âge < 45 → `OUTPLACEMENT_NON_DU` + `raisonNonObligation=AGE_INFERIEUR_45`.
   - Si ancienneté < 1 an → `OUTPLACEMENT_NON_DU` + `ANCIENNETE_INFERIEURE_1AN`.
   - Si motif = `FAUTE_GRAVE` → `OUTPLACEMENT_NON_DU` + `FAUTE_GRAVE`.
   - Si motif = `DEMISSION` → `OUTPLACEMENT_NON_DU` + `DEMISSION`.
3. **Obligation applicable** :
   - `dateLimiteOffre = dateLicenciement + 15 jours` calendaires.
   - Si `offreOutplacementRecue=false` → `OFFRE_NON_FAITE_SANCTION_EMPLOYEUR`, `sanctionEmployeurEuros=1800`, `etapeSuivante=PLAINTE_INSPECTION_LOIS_SOCIALES`.
   - Si `offreOutplacementRecue=true` :
     - Si `offreConformeCCT82=false` ou `dateOffre > dateLimiteOffre` → `OFFRE_NON_CONFORME_SANCTION_EMPLOYEUR`, `sanctionEmployeurEuros=1800`, `etapeSuivante=PLAINTE_INSPECTION_LOIS_SOCIALES`.
     - Si `offreConformeCCT82=true` + `salarieAcceptantOffre=false` → `OFFRE_REFUSEE_SANCTION_SALARIE`, `sanctionSalarieRange={4,52}`, `etapeSuivante=PRESENTATION_C4_ONEM` (info pour l'avocat — la sanction sera visible sur le C4).
     - Sinon (`salarieAcceptantOffre=true` ou null) → `OFFRE_CONFORME_RESPECTEE`, `etapeSuivante=AUCUNE`.

BigDecimal pour `sanctionEmployeurEuros` (HALF_EVEN 2 décimales).

## Cas d'erreur

| Situation | Code |
|---|---|
| `workspaceCountry !== BELGIQUE` | 404 |
| `caseFileId` autre workspace | 404 |
| `dateLicenciement` futur | 400 |
| `dateNaissanceSalarie` futur ou > 100 ans | 400 |
| `dateLicenciement < dateNaissance + 18 ans` | 400 |
| `motifLicenciement` invalide / manquant | 400 |
| `offreOutplacementRecue=true` mais `dateOffreOutplacement` ou `offreConformeCCT82` manquant | 400 |

## Composants à créer (pattern `C4OnemChecklist*`)

Sous `backend/src/main/java/fr/ailegalcase/casefile/` :
- `OutplacementBeAnalysis.java` (entité JPA unique sur `case_file_id`)
- `OutplacementBeRepository.java`
- `OutplacementBeMotifLicenciement.java` (enum 4)
- `OutplacementBeRaisonNonObligation.java` (enum 4)
- `OutplacementBeRequest.java` (Bean Validation : `@NotNull dateLicenciement`, `@NotNull dateNaissanceSalarie`, `@NotNull @DecimalMin("0") ancienneteAnnees`, `@NotNull motifLicenciement`, `@NotNull contratTempsPlein`, `@NotNull offreOutplacementRecue` ; validations conditionnelles côté service)
- `OutplacementBeResult.java` (record + enums `Verdict` 5 valeurs, `EtapeSuivante` 3 valeurs, record `SanctionSalarieRange`)
- `OutplacementBeResponse.java`
- `OutplacementBeCalculator.java` (logique conditionnelle 5 verdicts, BigDecimal)
- `OutplacementBeService.java` (gate `BELGIQUE`, validation, persistance)
- `OutplacementBeController.java` (POST + GET)

Migration `XXX-create-outplacement-be-analyses.xml` (prochain après 265). Table standard. Rollback.

Extensions :
- `LegalDomainPromptBuilder` BE Travail : ajout 3 champs IA `ancienneteSalarie` (Double, années), `motifLicenciementDetecte` (String enum), `offreOutplacementMentionnee` (Boolean). `dateNaissanceSalarie`, `dateLicenciement` déjà extraits. 3 `critereCode` `BE_OUTPLACEMENT_*`.
- `CaseAnalysisResponse.TravailExtractedData` : ajout 3 fields (rétrocompat Builder).

## Critères d'acceptation

- [ ] Âge < 45 → `OUTPLACEMENT_NON_DU` + `raisonNonObligation=AGE_INFERIEUR_45`.
- [ ] Ancienneté < 1 → `OUTPLACEMENT_NON_DU` + `ANCIENNETE_INFERIEURE_1AN`.
- [ ] Motif FAUTE_GRAVE → `OUTPLACEMENT_NON_DU` + `FAUTE_GRAVE`.
- [ ] Conditions remplies + `offreRecue=false` → `OFFRE_NON_FAITE_SANCTION_EMPLOYEUR` + 1800 €.
- [ ] Conditions + `offreRecue=true` + `dateOffre > dateLimite (J+15)` → `OFFRE_NON_CONFORME_SANCTION_EMPLOYEUR` + 1800 €.
- [ ] Conditions + offre conforme + `salarieRefuse=true` → `OFFRE_REFUSEE_SANCTION_SALARIE` + range 4-52 semaines.
- [ ] Conditions + offre conforme + accepte/null → `OFFRE_CONFORME_RESPECTEE` + 0 €.
- [ ] Workspace FR → 404 ; autre workspace → 404.
- [ ] `offreRecue=true` sans `dateOffre`/`offreConformeCCT82` → 400.
- [ ] `critereCode` BE_OUTPLACEMENT_* émis ; `CritereCodeIntegrityIT` vert.

## Hors scope

- Frontend (SF-207-08b).
- Calcul détaillé du coût de l'outplacement (compétence cabinet outplacement).
- Outplacement volontaire (non obligatoire).
- Outplacement collectif/restructuration (Loi Renault — autre régime).

## Plan de test

`OutplacementBeCalculatorTest` (12+ tests : 5 verdicts × edge cases + 4 raisons non-obligation + délai 15 j bornes + temps partiel CCT 82 bis).
`OutplacementBeControllerIT` (5+ tests : BE OK, FR 404, autre workspace 404, validation conditionnelle 400, GET 404).

## Dépendances

- Pattern `C4OnemChecklist*` (SF-207-02).
- `dateNaissanceSalarie` déjà dans `TravailExtractedData` (SF-207-06).
