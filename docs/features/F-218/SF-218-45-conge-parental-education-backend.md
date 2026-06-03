# Mini-spec — F-218 / SF-218-45 — Congé parental d'éducation — backend

## Identifiant

`F-218 / SF-218-45`

## Feature parente

`F-218d` — Temps de travail / congés FR-only (P3 Travail FR — différé signal terrain, réactivé)

## Statut

`ready`

## Date de création

2026-06-03

## Branche Git

`feat/SF-218-45-conge-parental-education-backend`

---

## Objectif

Déterminer l'**éligibilité au congé parental d'éducation** et sa **date de fin maximale** (art. L.1225-47 à L.1225-60 CT) : condition d'**un an d'ancienneté minimum** à la date de naissance ou d'arrivée de l'enfant adopté, durée du congé (total ou à temps partiel) **jusqu'au 3e anniversaire de l'enfant** (un an renouvelable deux fois ; règles spécifiques en cas d'adoption), et garantie de **réintégration dans le poste précédent ou un emploi similaire** assorti d'une rémunération au moins équivalente (L.1225-55). L'outil mentionne la PreParE versée par la CAF à titre informatif, sans calcul d'allocation. **Analyseur de droit / éligibilité**. Aucun outil existant ne couvre le congé parental d'éducation (vérifié — invariant « un outil = une situation »).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/conge-parental-education-analysis`
- Body :
  - `ancienneteMois` (int, requis, ≥ 0) — ancienneté du salarié à la date de naissance/adoption, en mois (≥ 12 requis, L.1225-47)
  - `modalite` (enum, requis) ∈ { `TEMPS_PLEIN`, `TEMPS_PARTIEL` }
  - `nombreEnfants` (int, requis, ≥ 1) — nombre d'enfants concernés (naissances/adoptions multiples)
  - `dateNaissanceOuAdoption` (LocalDate, requis) — date de naissance ou d'arrivée de l'enfant au foyer
- Analyzer `CongeParentalEducationAnalyzer` :
  - **Éligibilité** : `eligible = ancienneteMois >= 12`. Si `ancienneteMois < 12` → `statut = NON_ELIGIBLE` + note « un an d'ancienneté minimum à la date de naissance/adoption requis (L.1225-47) », pas de date de fin.
  - **Date de fin maximale** : `dateFinMax = dateNaissanceOuAdoption + 3 ans` (jusqu'au 3e anniversaire de l'enfant). Note : un an renouvelable deux fois ; en cas d'adoption d'un enfant de plus de 3 ans → durée maximale réduite (note « règles spécifiques d'adoption — à vérifier par avocat »).
  - **Protection / réintégration** : `protectionReintegration = true` — note « à l'issue du congé, le salarié retrouve son précédent emploi ou un emploi similaire assorti d'une rémunération au moins équivalente (L.1225-55) ».
  - **PreParE (information)** : `mentionPreparE = true` — note « le congé peut ouvrir droit à la prestation partagée d'éducation de l'enfant (PreParE) versée par la CAF — information, montant non calculé ici ».
  - **Verdict** `statut` ∈ { `ELIGIBLE`, `NON_ELIGIBLE` } ; `dateFinMax` (LocalDate, nullable si non éligible) ; `modaliteRetenue` (String) ; `protectionReintegration` (boolean).
  - `baseJuridique` : art. L.1225-47 à L.1225-60 CT — annoté `(à vérifier par avocat)`.
- Output persisté dans `conge_parental_education_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/conge-parental-education-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| un des champs requis absent (null) | 400 |
| `ancienneteMois` < 0 | 400 |
| `modalite` valeur inconnue | 400 |
| `nombreEnfants` < 1 | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **Art. L.1225-47 CT** — droit au congé parental d'éducation sous condition d'un an d'ancienneté minimum à la date de naissance ou de l'arrivée au foyer de l'enfant adopté de moins de 16 ans.
- **Art. L.1225-48 CT** — durée du congé : un an au plus, renouvelable deux fois, jusqu'au 3e anniversaire de l'enfant (règles spécifiques en cas d'adoption ou de naissances multiples).
- **Art. L.1225-55 CT** — à l'issue du congé, réintégration dans le précédent emploi ou un emploi similaire assorti d'une rémunération au moins équivalente.
- **Art. L.1225-47 à L.1225-60 CT** — modalités (temps plein / temps partiel), information de l'employeur, retour anticipé.

(à vérifier par avocat)

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `ancienneteMois` | entier | `ancienneteMois` (existant si présent) | Réutiliser si présent |
| `dateNaissanceOuAdoption` | date | `dateNaissanceOuAdoptionEnfant` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Consolidation IA critique** : les nouveaux champs IA de cet outil sont ajoutés au **sous-record consolidé `Sf218dDetail`** (un seul sous-record `@JsonUnwrapped` partagé par les 9 outils de la vague F-218d, dans `TravailExtractedData` du record `CaseAnalysisResponse.java`) — **PAS** un sous-record dédié, afin de ne pas dépasser la limite JVM de 255 paramètres du constructeur canonical. Clés JSON HTTP inchangées (plates).

**Flag CONTEXTUAL pivot** : `conge_parental_detecte` (niveau 2, FR-only, default false) — nouveau flag `TravailExtractedData`. Bascule CONTEXTUAL quand l'IA détecte des signaux de congé parental d'éducation (mentions « congé parental d'éducation », « congé parental », « PreParE », « réintégration après congé parental », « temps partiel pour élever un enfant »).

---

## Critères d'acceptation

- [ ] POST `ancienneteMois=18`, `modalite=TEMPS_PLEIN`, `nombreEnfants=1`, `dateNaissanceOuAdoption=2025-03-01` → `statut=ELIGIBLE`, `dateFinMax=2028-03-01`, `protectionReintegration=true`
- [ ] POST `ancienneteMois=8` → `statut=NON_ELIGIBLE`, `dateFinMax=null`, note ancienneté
- [ ] POST `ancienneteMois=12` (limite) → `statut=ELIGIBLE`
- [ ] POST `modalite=TEMPS_PARTIEL` → `modaliteRetenue=TEMPS_PARTIEL`, éligibilité inchangée
- [ ] POST réponse contient `mentionPreparE=true`
- [ ] POST champ requis null → 400 ; `ancienneteMois=-1` → 400 ; `nombreEnfants=0` → 400 ; `modalite` inconnu → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert ; isolation workspace (A ne lit pas B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`conge_parental_detecte`, trigger_value=`true`, FRANCE, DROIT_DU_TRAVAIL, priority 95
- [ ] `F-DT-78-conge-parental-education` ajouté à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Plan de test minimal

- **UT** `CongeParentalEducationAnalyzerTest` : ≥ 6 cas (éligible + dateFinMax = +3 ans, non éligible <12 mois, limite 12 mois, temps partiel, protection réintégration true, mention PreParE)
- **IT** `CongeParentalEducationControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `conge_parental_education_analyses`
- **Migrations** : `534-create-conge-parental-education-analyses.xml` (create) + `535-seed-conge-parental-education-visibility.xml` (seed visibility, priority 95)
- **Endpoint** `CongeParentalEducationController` (POST + GET)
- **Service** `CongeParentalEducationService` + **Analyzer** `CongeParentalEducationAnalyzer`
- **Extension** `TravailExtractedData` : champ `dateNaissanceOuAdoptionEnfant` ajouté au sous-record consolidé `Sf218dDetail` + flag `congeParentalDetecte` + instruction `TRAVAIL_INSTRUCTION_PART40` dans `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-46)
- Calcul du montant de la PreParE (compétence CAF, hors produit)
- Congé maternité / paternité (F-212, situation distincte)
- Congés pour évènements familiaux (F-DT-76, SF-218-43 — situation distincte)
- Règles fines d'adaptation de durée en cas d'adoption d'enfant de plus de 3 ans (signalées en note, pas calculées)
