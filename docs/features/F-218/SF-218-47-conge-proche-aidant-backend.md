# Mini-spec — F-218 / SF-218-47 — Congé de proche aidant — backend

## Identifiant

`F-218 / SF-218-47`

## Feature parente

`F-218d` — Temps de travail / congés FR-only (P3 Travail FR — différé signal terrain, réactivé)

## Statut

`ready`

## Date de création

2026-06-03

## Branche Git

`feat/SF-218-47-conge-proche-aidant-backend`

---

## Objectif

Déterminer l'**éligibilité au congé de proche aidant**, sa **durée maximale** et fournir une **estimation de l'AJPA** (art. L.3142-16 à L.3142-27 CT, loi n° 2020-220 du 06/03/2020) : congé permettant à un salarié de cesser temporairement son activité pour s'occuper d'une personne en situation de handicap ou de perte d'autonomie d'une particulière gravité, durée de 3 mois renouvelable dans la limite d'un an sur l'ensemble de la carrière, avec versement par la CAF de l'allocation journalière du proche aidant (AJPA) plafonnée à 66 jours indemnisés sur la carrière. **Analyseur de droit / éligibilité**. Aucun outil existant ne couvre le congé de proche aidant (vérifié — invariant « un outil = une situation » ; distinct du congé parental F-DT-78 et des congés pour évènements familiaux F-DT-76).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/conge-proche-aidant-analysis`
- Body :
  - `lienPersonneAidee` (enum, requis) ∈ { `CONJOINT`, `ASCENDANT`, `DESCENDANT`, `COLLATERAL`, `SANS_LIEN_RESIDENCE_COMMUNE` }
  - `personneAideeResideFrance` (boolean, requis) — la personne aidée réside en France (ou dans l'EEE)
  - `dureeSouhaiteeMois` (int, requis, > 0) — durée de congé souhaitée, en mois
  - `ajpaDemandee` (boolean, défaut false) — le salarié demande l'AJPA auprès de la CAF
- Analyzer `CongeProcheAidantAnalyzer` :
  - **Éligibilité** : le lien (y compris `SANS_LIEN_RESIDENCE_COMMUNE` : personne avec laquelle le salarié réside ou entretient des liens étroits et stables) ouvre droit au congé si `personneAideeResideFrance = true`. Si `personneAideeResideFrance = false` → `statut = NON_ELIGIBLE` + note « la personne aidée doit résider en France/EEE (L.3142-16) ».
  - **Durée maximale** : `dureeMaxMois = 12` (3 mois renouvelable, plafond d'un an sur l'ensemble de la carrière, L.3142-19). `dureeRetenueMois = min(dureeSouhaiteeMois, 12)` ; si `dureeSouhaiteeMois > 12` → note « durée plafonnée à un an sur la carrière ».
  - **Estimation AJPA** : si `ajpaDemandee = true` → `estimationAjpa` = `montantJournalierAjpa × min(joursOuvrables(dureeRetenueMois), 66)` avec `montantJournalierAjpa ≈ 64 €` (montant 2026, annoté « montant à vérifier ») et plafond de 66 jours indemnisés sur la carrière ; sinon `estimationAjpa = null` + note « AJPA non demandée ».
  - **Protection** : `protectionEmploi = true`, `nonImputableCongesPayes = true` — note « le congé n'est pas imputable sur les congés payés et le salarié retrouve son emploi ou un emploi similaire à l'issue ».
  - **Verdict** `statut` ∈ { `ELIGIBLE`, `NON_ELIGIBLE` } ; `dureeMaxMois` (int) ; `dureeRetenueMois` (int, nullable si non éligible) ; `estimationAjpa` (BigDecimal, nullable).
  - `baseJuridique` : art. L.3142-16 à L.3142-27 CT ; loi n° 2020-220 du 06/03/2020 — annoté `(à vérifier par avocat)`.
- Output persisté dans `conge_proche_aidant_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/conge-proche-aidant-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| un des champs requis absent (null) | 400 |
| `lienPersonneAidee` valeur inconnue | 400 |
| `dureeSouhaiteeMois` ≤ 0 | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **Art. L.3142-16 CT** — bénéficiaires du congé de proche aidant : personne aidée résidant en France de façon stable et régulière, lien avec le salarié (conjoint, ascendant, descendant, collatéral jusqu'au 4e degré, ou personne avec laquelle le salarié réside ou entretient des liens étroits et stables, à qui il vient en aide de manière régulière et fréquente à titre non professionnel).
- **Art. L.3142-19 CT** — durée : 3 mois, renouvelable, dans la limite d'un an pour l'ensemble de la carrière.
- **Art. L.3142-20 à L.3142-25 CT** — protection de l'emploi, non-imputation sur les congés payés, réintégration.
- **Loi n° 2020-220 du 06/03/2020** — création de l'allocation journalière du proche aidant (AJPA), versée par la CAF, plafonnée à 66 jours indemnisés sur l'ensemble de la carrière (montant journalier ≈ 64 € en 2026 — montant à vérifier).

(à vérifier par avocat)

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `lienPersonneAidee` | enum (String) | `lienProcheAidant` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| `dureeSouhaiteeMois` | entier | `dureeCongeProcheAidantMois` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Consolidation IA critique** : les nouveaux champs IA de cet outil sont ajoutés au **sous-record consolidé `Sf218dDetail`** (un seul sous-record `@JsonUnwrapped` partagé par les 9 outils de la vague F-218d, dans `TravailExtractedData` du record `CaseAnalysisResponse.java`) — **PAS** un sous-record dédié, afin de ne pas dépasser la limite JVM de 255 paramètres du constructeur canonical. Clés JSON HTTP inchangées (plates).

**Flag CONTEXTUAL pivot** : `conge_proche_aidant_detecte` (niveau 2, FR-only, default false) — nouveau flag `TravailExtractedData`. Bascule CONTEXTUAL quand l'IA détecte des signaux de congé de proche aidant (mentions « congé de proche aidant », « proche aidant », « AJPA », « allocation journalière du proche aidant », « aider un parent dépendant », « perte d'autonomie d'un proche »).

---

## Critères d'acceptation

- [ ] POST `lienPersonneAidee=ASCENDANT`, `personneAideeResideFrance=true`, `dureeSouhaiteeMois=3`, `ajpaDemandee=false` → `statut=ELIGIBLE`, `dureeMaxMois=12`, `dureeRetenueMois=3`, `estimationAjpa=null`
- [ ] POST `dureeSouhaiteeMois=18` → `dureeRetenueMois=12`, note plafond carrière
- [ ] POST `ajpaDemandee=true`, durée 3 mois → `estimationAjpa` calculée (≤ 66 jours × ~64 €), annotée « montant à vérifier »
- [ ] POST `personneAideeResideFrance=false` → `statut=NON_ELIGIBLE`, `dureeRetenueMois=null`
- [ ] POST `lienPersonneAidee=SANS_LIEN_RESIDENCE_COMMUNE`, France → `statut=ELIGIBLE`
- [ ] POST champ requis null → 400 ; `dureeSouhaiteeMois=0` → 400 ; `lienPersonneAidee` inconnu → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert ; isolation workspace (A ne lit pas B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`conge_proche_aidant_detecte`, trigger_value=`true`, FRANCE, DROIT_DU_TRAVAIL, priority 96
- [ ] `F-DT-79-conge-proche-aidant` ajouté à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Plan de test minimal

- **UT** `CongeProcheAidantAnalyzerTest` : ≥ 6 cas (éligible ascendant, durée plafonnée à 12, AJPA estimée plafond 66 jours, non éligible hors France, lien sans résidence commune éligible, protection non-imputable congés payés)
- **IT** `CongeProcheAidantControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `conge_proche_aidant_analyses`
- **Migrations** : `536-create-conge-proche-aidant-analyses.xml` (create) + `537-seed-conge-proche-aidant-visibility.xml` (seed visibility, priority 96)
- **Endpoint** `CongeProcheAidantController` (POST + GET)
- **Service** `CongeProcheAidantService` + **Analyzer** `CongeProcheAidantAnalyzer`
- **Extension** `TravailExtractedData` : champs `lienProcheAidant` + `dureeCongeProcheAidantMois` ajoutés au sous-record consolidé `Sf218dDetail` + flag `congeProcheAidantDetecte` + instruction `TRAVAIL_INSTRUCTION_PART41` dans `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-48)
- Calcul exact de l'AJPA par la CAF (estimation indicative seulement, montant journalier à vérifier)
- Congé de solidarité familiale (situation distincte)
- Congé parental d'éducation (F-DT-78, SF-218-45 — situation distincte)
- Congés pour évènements familiaux (F-DT-76, SF-218-43 — situation distincte)
