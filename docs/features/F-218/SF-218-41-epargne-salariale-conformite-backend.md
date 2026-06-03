# Mini-spec — F-218 / SF-218-41 — Intéressement / participation : conformité et obligation — backend

## Identifiant

`F-218 / SF-218-41`

## Feature parente

`F-218d` — Temps de travail / congés FR-only (P3 Travail FR — différé signal terrain, réactivé)

## Statut

`ready`

## Date de création

2026-06-03

## Branche Git

`feat/SF-218-41-epargne-salariale-conformite-backend`

---

## Objectif

Analyser la **conformité aux obligations d'épargne salariale** (intéressement, participation, dispositif de partage de la valeur) : vérifier le caractère **obligatoire de la participation** dans les entreprises d'au moins 50 salariés (art. L.3322-2 CT), et l'**obligation issue de la loi n° 2023-1107 du 29/11/2023** de mettre en place un dispositif de partage de la valeur pour les entreprises de 11 à 49 salariés rentables (bénéfice net fiscal ≥ 1 % du chiffre d'affaires pendant 3 années consécutives), applicable à compter des exercices ouverts en 2025. Produit une **checklist de conformité**. **Analyseur conformité**. Aucun outil existant ne couvre l'obligation d'épargne salariale (vérifié — invariant « un outil = une situation » ; distinct de F-DT-52 PPV qui porte sur l'exonération d'une prime versée).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/epargne-salariale-conformite-analysis`
- Body :
  - `effectif` (int, requis, > 0) — effectif de l'entreprise
  - `accordParticipationPresent` (boolean, requis) — un accord de participation est en place
  - `accordInteressementPresent` (boolean, requis) — un accord d'intéressement est en place
  - `beneficeNetFiscalPositif3Ans` (boolean, requis) — bénéfice net fiscal ≥ 1 % du CA sur les 3 derniers exercices
  - `entreprise11a49` (boolean, requis) — l'entreprise compte entre 11 et 49 salariés
- Analyzer `EpargneSalarialeConformiteAnalyzer` :
  - **Obligation participation (≥ 50 salariés)** : si `effectif >= 50` → la participation est obligatoire (L.3322-2). Item `{ obligationParticipation, conforme = accordParticipationPresent, type=OBLIGATION }`.
  - **Obligation partage de la valeur (11–49 salariés rentables)** : si `entreprise11a49 = true` ET `beneficeNetFiscalPositif3Ans = true` → obligation de mettre en place un dispositif de partage de la valeur (participation, intéressement, PPV ou abondement) à compter de 2025. Item `{ obligationPartageValeur, conforme = (accordParticipationPresent OU accordInteressementPresent), type=OBLIGATION }`.
  - **Checklist conformité** : liste de `{ item, conforme, type, commentaire }` couvrant participation obligatoire (si applicable) et dispositif de partage de la valeur (si applicable). Pour les entreprises < 11 salariés ou non concernées → items `type=INFORMATION` (facultatif), conformes par défaut.
  - **Verdict** `statut` ∈ { `CONFORME`, `OBLIGATION_NON_REMPLIE`, `NON_REQUIS` } :
    - aucune obligation applicable (effectif < 11, ou 11–49 non rentable) → `NON_REQUIS`.
    - toutes les obligations applicables remplies → `CONFORME`.
    - au moins une obligation applicable non remplie → `OBLIGATION_NON_REMPLIE`.
  - `obligationsApplicables` (int) ; `obligationsNonRemplies` (int).
  - `baseJuridique` : art. L.3311-1 et suivants, L.3321-1 et suivants, L.3322-2 CT ; loi n° 2023-1107 du 29/11/2023 — annoté `(à vérifier par avocat)`.
- Output persisté dans `epargne_salariale_conformite_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/epargne-salariale-conformite-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| un des booléens requis absent (null) | 400 |
| `effectif` ≤ 0 | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **Art. L.3311-1 et suivants CT** — intéressement (dispositif facultatif d'association des salariés aux résultats).
- **Art. L.3321-1 et suivants CT** — participation des salariés aux résultats de l'entreprise.
- **Art. L.3322-2 CT** — participation obligatoire dans les entreprises employant habituellement au moins 50 salariés.
- **Loi n° 2023-1107 du 29/11/2023 (partage de la valeur)** — obligation, à titre expérimental (exercices ouverts à compter de 2025), pour les entreprises de 11 à 49 salariés dont le bénéfice net fiscal est au moins égal à 1 % du chiffre d'affaires pendant 3 exercices consécutifs, de mettre en place un dispositif de partage de la valeur.

(à vérifier par avocat)

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `effectif` | entier | `effectifEntreprise` (existant) | Réutiliser si présent |
| `accordParticipationPresent` | booléen | `accordParticipationPresent` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Consolidation IA critique** : les nouveaux champs IA de cet outil sont ajoutés au **sous-record consolidé `Sf218dDetail`** (un seul sous-record `@JsonUnwrapped` partagé par les 9 outils de la vague F-218d, dans `TravailExtractedData` du record `CaseAnalysisResponse.java`) — **PAS** un sous-record dédié, afin de ne pas dépasser la limite JVM de 255 paramètres du constructeur canonical. Clés JSON HTTP inchangées (plates). Note : `accordInteressementPresent` est mutualisé avec SF-218-39 (F-DT-52) — un seul champ dans `Sf218dDetail`.

**Flag CONTEXTUAL pivot** : `epargne_salariale_detectee` (niveau 2, FR-only, default false) — nouveau flag `TravailExtractedData`. Bascule CONTEXTUAL quand l'IA détecte des signaux d'épargne salariale (mentions « accord de participation », « accord d'intéressement », « participation aux résultats », « dispositif de partage de la valeur », « réserve spéciale de participation », « PEE/PERCO »).

---

## Critères d'acceptation

- [ ] POST `effectif=80`, `accordParticipationPresent=true` → obligation participation conforme, `statut=CONFORME`
- [ ] POST `effectif=80`, `accordParticipationPresent=false` → `statut=OBLIGATION_NON_REMPLIE`, `obligationsNonRemplies ≥ 1`
- [ ] POST `entreprise11a49=true`, `beneficeNetFiscalPositif3Ans=true`, ni participation ni intéressement → `statut=OBLIGATION_NON_REMPLIE`
- [ ] POST `entreprise11a49=true`, `beneficeNetFiscalPositif3Ans=true`, `accordInteressementPresent=true` → obligation partage valeur conforme, `statut=CONFORME`
- [ ] POST `effectif=8`, `entreprise11a49=false`, `beneficeNetFiscalPositif3Ans=false` → `statut=NON_REQUIS`
- [ ] POST booléen requis null → 400 ; `effectif=0` → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert ; isolation workspace (A ne lit pas B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`epargne_salariale_detectee`, trigger_value=`true`, FRANCE, DROIT_DU_TRAVAIL, priority 93
- [ ] `F-DT-53-epargne-salariale-conformite` ajouté à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Plan de test minimal

- **UT** `EpargneSalarialeConformiteAnalyzerTest` : ≥ 6 cas (≥50 + participation → CONFORME, ≥50 sans participation → OBLIGATION_NON_REMPLIE, 11-49 rentable sans dispositif → OBLIGATION_NON_REMPLIE, 11-49 rentable avec intéressement → CONFORME, <11 non concerné → NON_REQUIS, comptage obligationsApplicables/obligationsNonRemplies)
- **IT** `EpargneSalarialeConformiteControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `epargne_salariale_conformite_analyses`
- **Migrations** : `530-create-epargne-salariale-conformite-analyses.xml` (create) + `531-seed-epargne-salariale-conformite-visibility.xml` (seed visibility, priority 93)
- **Endpoint** `EpargneSalarialeConformiteController` (POST + GET)
- **Service** `EpargneSalarialeConformiteService` + **Analyzer** `EpargneSalarialeConformiteAnalyzer`
- **Extension** `TravailExtractedData` : champ `accordParticipationPresent` ajouté au sous-record consolidé `Sf218dDetail` (réutilise `accordInteressementPresent` de SF-218-39) + flag `epargneSalarialeDetectee` + instruction `TRAVAIL_INSTRUCTION_PART38` dans `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-42)
- Exonération d'une PPV versée (F-DT-52, SF-218-39 — situation distincte)
- Calcul de la réserve spéciale de participation (formule légale détaillée)
- Régime fiscal des plans d'épargne (PEE/PERCO/PER)
