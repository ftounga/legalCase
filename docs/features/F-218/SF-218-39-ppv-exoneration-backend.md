# Mini-spec — F-218 / SF-218-39 — Prime de partage de la valeur (PPV) : exonération — backend

## Identifiant

`F-218 / SF-218-39`

## Feature parente

`F-218d` — Temps de travail / congés FR-only (P3 Travail FR — différé signal terrain, réactivé)

## Statut

`ready`

## Date de création

2026-06-03

## Branche Git

`feat/SF-218-39-ppv-exoneration-backend`

---

## Objectif

Analyser la **conformité et les exonérations de la prime de partage de la valeur (PPV)** (loi n° 2022-1158 du 16/08/2022, loi n° 2023-1107 du 29/11/2023 sur le partage de la valeur) : vérifier le respect du **plafond d'exonération sociale** (3 000 € par bénéficiaire et par année civile, porté à 6 000 € en présence d'un accord d'intéressement OU pour les entreprises de moins de 50 salariés dotées d'un dispositif de partage de la valeur), et déterminer l'**exonération fiscale (IR)** conditionnelle (rémunération inférieure à 3 SMIC, effectif < 50, jusqu'au 31/12/2026), distinguer la part `montantExonere` de la part `montantImposable`. **Calculateur d'indemnité / exonération**. Aucun outil existant ne couvre la PPV (vérifié — invariant « un outil = une situation »).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/ppv-exoneration-analysis`
- Body :
  - `montantPrime` (BigDecimal, requis, > 0) — montant de la PPV versée au bénéficiaire sur l'année civile
  - `accordInteressementPresent` (boolean, requis) — un accord d'intéressement existe dans l'entreprise
  - `remunerationAnnuelleBrute` (BigDecimal, requis, > 0) — rémunération annuelle brute du bénéficiaire (base du test « < 3 SMIC »)
  - `effectifMoins50` (boolean, requis) — l'entreprise compte moins de 50 salariés
  - `versementPlanEpargne` (boolean, défaut false) — la prime (ou une partie) a été affectée à un plan d'épargne salariale
- Analyzer `PpvExonerationAnalyzer` :
  - **Plafond social** : `plafondSocial = 6000` si `accordInteressementPresent = true` OU (`effectifMoins50 = true` avec dispositif de partage de la valeur), sinon `3000`.
  - **Test plafond** : si `montantPrime > plafondSocial` → `montantExonere = plafondSocial`, `montantImposable` (part dépassant) = `montantPrime − plafondSocial`, `statut = PLAFOND_DEPASSE`. Sinon `montantExonere = montantPrime`, `montantImposable = 0`, `statut = CONFORME`.
  - **Exonération fiscale (IR)** : `exonerationFiscaleIr = true` si `effectifMoins50 = true` ET `remunerationAnnuelleBrute < 3 × SMIC annuel` (jusqu'au 31/12/2026) — note « exonération d'impôt sur le revenu dans la limite du plafond social applicable » ; sinon `exonerationFiscaleIr = false` (la part exonérée socialement reste imposable à l'IR sauf affectation à un plan d'épargne).
  - **Note plan d'épargne** : si `versementPlanEpargne = true` → note « la fraction affectée à un plan d'épargne salariale bénéficie d'une exonération d'IR (à vérifier par avocat) ».
  - **Verdict** `statut` ∈ { `CONFORME`, `PLAFOND_DEPASSE` }.
  - `plafondSocialApplique` (BigDecimal) ; `montantExonere` (BigDecimal) ; `montantImposable` (BigDecimal) ; `exonerationFiscaleIr` (boolean).
  - `baseJuridique` : loi n° 2022-1158 du 16/08/2022 + loi n° 2023-1107 du 29/11/2023 (partage de la valeur) — annoté `(à vérifier par avocat)`.
- Output persisté dans `ppv_exoneration_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/ppv-exoneration-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| un des champs requis absent (null) | 400 |
| `montantPrime` ≤ 0 | 400 |
| `remunerationAnnuelleBrute` ≤ 0 | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **Loi n° 2022-1158 du 16/08/2022, art. 1** — création de la prime de partage de la valeur (PPV) : exonération de cotisations sociales dans la limite de 3 000 € par bénéficiaire et par année civile, portée à 6 000 € en présence d'un accord d'intéressement (ou de participation à titre volontaire).
- **Loi n° 2023-1107 du 29/11/2023 sur le partage de la valeur** — pérennisation et aménagements : possibilité de deux primes par an, affectation à un plan d'épargne salariale, exonération d'IR maintenue jusqu'au 31/12/2026 pour les salariés des entreprises de moins de 50 salariés dont la rémunération est inférieure à 3 SMIC.
- **Exonération fiscale (IR)** : conditionnelle (effectif < 50, rémunération < 3 SMIC, jusqu'au 31/12/2026) ; la fraction affectée à un plan d'épargne est exonérée d'IR.

(à vérifier par avocat)

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `montantPrime` | décimal | `montantPpv` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| `accordInteressementPresent` | booléen | `accordInteressementPresent` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Consolidation IA critique** : les nouveaux champs IA de cet outil sont ajoutés au **sous-record consolidé `Sf218dDetail`** (un seul sous-record `@JsonUnwrapped` partagé par les 9 outils de la vague F-218d, dans `TravailExtractedData` du record `CaseAnalysisResponse.java`) — **PAS** un sous-record dédié, afin de ne pas dépasser la limite JVM de 255 paramètres du constructeur canonical. Clés JSON HTTP inchangées (plates). Note : le champ `accordInteressementPresent` est partagé avec SF-218-41 (F-DT-53) — un seul champ dans `Sf218dDetail`.

**Flag CONTEXTUAL pivot** : `ppv_detectee` (niveau 2, FR-only, default false) — nouveau flag `TravailExtractedData`. Bascule CONTEXTUAL quand l'IA détecte des signaux de PPV (mentions « prime de partage de la valeur », « PPV », « prime Macron », « prime exceptionnelle de pouvoir d'achat », « prime PEPA »).

---

## Critères d'acceptation

- [ ] POST `montantPrime=2500`, `accordInteressementPresent=false`, `effectifMoins50=false` → `plafondSocialApplique=3000`, `montantExonere=2500`, `montantImposable=0`, `statut=CONFORME`
- [ ] POST `montantPrime=4500`, `accordInteressementPresent=true` → `plafondSocialApplique=6000`, `montantExonere=4500`, `statut=CONFORME`
- [ ] POST `montantPrime=4000`, `accordInteressementPresent=false`, `effectifMoins50=false` → `montantExonere=3000`, `montantImposable=1000`, `statut=PLAFOND_DEPASSE`
- [ ] POST `effectifMoins50=true`, `remunerationAnnuelleBrute` < 3 SMIC → `exonerationFiscaleIr=true`
- [ ] POST `effectifMoins50=false`, rémunération ≥ 3 SMIC → `exonerationFiscaleIr=false`
- [ ] POST champ requis null → 400 ; `montantPrime=0` → 400 ; `remunerationAnnuelleBrute=0` → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert ; isolation workspace (A ne lit pas B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`ppv_detectee`, trigger_value=`true`, FRANCE, DROIT_DU_TRAVAIL, priority 92
- [ ] `F-DT-52-ppv-exoneration` ajouté à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Plan de test minimal

- **UT** `PpvExonerationAnalyzerTest` : ≥ 6 cas (sous plafond 3000 → CONFORME, plafond 6000 via accord intéressement, dépassement → PLAFOND_DEPASSE + montantImposable, exonération IR effectif<50 + <3 SMIC, pas d'exonération IR, plan d'épargne note)
- **IT** `PpvExonerationControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `ppv_exoneration_analyses`
- **Migrations** : `528-create-ppv-exoneration-analyses.xml` (create) + `529-seed-ppv-exoneration-visibility.xml` (seed visibility, priority 92)
- **Endpoint** `PpvExonerationController` (POST + GET)
- **Service** `PpvExonerationService` + **Analyzer** `PpvExonerationAnalyzer`
- **Extension** `TravailExtractedData` : champs `montantPpv` + `accordInteressementPresent` ajoutés au sous-record consolidé `Sf218dDetail` + flag `ppvDetectee` + instruction `TRAVAIL_INSTRUCTION_PART37` dans `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-40)
- Conformité du dispositif d'intéressement / participation (F-DT-53, SF-218-41 — situation distincte)
- Calcul exact du SMIC annuel de référence (valeur paramétrée / vérifiée, pas l'objet de l'outil)
- Régime de l'épargne salariale (PEE/PERCO) en tant que tel
