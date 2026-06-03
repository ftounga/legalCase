# Mini-spec — F-218 / SF-218-49 — RTT : acquisition selon accord d'aménagement — backend

## Identifiant

`F-218 / SF-218-49`

## Feature parente

`F-218d` — Temps de travail / congés FR-only (P3 Travail FR — différé signal terrain, réactivé)

## Statut

`ready`

## Date de création

2026-06-03

## Branche Git

`feat/SF-218-49-rtt-acquisition-backend`

---

## Objectif

Calculer le **nombre théorique de jours de réduction du temps de travail (JRTT) acquis** dans le cadre d'un accord d'aménagement du temps de travail sur l'année (art. L.3121-41 à L.3121-44 CT) : lorsqu'un accord collectif fixe un horaire hebdomadaire supérieur à 35 heures (ex. 37 h ou 39 h), les heures effectuées entre 35 h et l'horaire collectif sont compensées par l'attribution de jours de repos (JRTT) **sans majoration**. À défaut d'accord d'aménagement, l'outil renvoie au régime des heures supplémentaires. **Calculateur de droit à repos**. **DISTINCT de F-DT-19 (heures supplémentaires) et de F-DT-51 (monétisation de RTT)** : ici on calcule l'acquisition de JRTT, pas leur paiement ni leur majoration (vérifié — invariant « un outil = une situation »).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/rtt-acquisition-analysis`
- Body :
  - `horaireHebdomadaireCollectif` (Double, requis, > 35) — horaire hebdomadaire fixé par l'accord (ex. 37, 39)
  - `accordCollectifPresent` (boolean, requis) — un accord d'aménagement du temps de travail sur l'année existe
  - `semainesTravailleesAn` (int, optionnel, défaut `47`) — nombre de semaines effectivement travaillées dans l'année (hors congés)
- Analyzer `RttAcquisitionAnalyzer` :
  - **Renvoi si pas d'accord** : si `accordCollectifPresent = false` → `statut = RENVOI_HEURES_SUP`, `nombreJrttTheorique = null` + note « à défaut d'accord d'aménagement, les heures effectuées au-delà de 35 h relèvent du régime des heures supplémentaires (voir l'outil dédié — F-DT-19) ».
  - **Calcul JRTT** (si `accordCollectifPresent = true`) :
    `heuresAuDela = horaireHebdomadaireCollectif − 35`
    `heuresAnnuelles = heuresAuDela × semainesTravailleesAn`
    `dureeJourEnHeures = horaireHebdomadaireCollectif / 5`
    `nombreJrttTheorique = heuresAnnuelles / dureeJourEnHeures` (arrondi, ex. 37 h → ~12 JRTT/an, 39 h → ~23 JRTT/an).
  - **Verdict** `statut` ∈ { `CALCULE`, `RENVOI_HEURES_SUP` } ; `nombreJrttTheorique` (Double, nullable) ; `base` (String, ex. « horaire collectif 37 h, 47 semaines travaillées, JRTT sans majoration »).
  - Note systématique : « les JRTT compensent les heures effectuées entre 35 h et l'horaire collectif et ne donnent lieu à aucune majoration ».
  - `baseJuridique` : art. L.3121-41 à L.3121-44 CT (aménagement du temps de travail sur une période supérieure à la semaine) ; accord d'entreprise / CCN — annoté `(à vérifier par avocat)`.
- Output persisté dans `rtt_acquisition_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/rtt-acquisition-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| un des champs requis absent (null) | 400 |
| `horaireHebdomadaireCollectif` ≤ 35 | 400 |
| `semainesTravailleesAn` ≤ 0 (si fourni) | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **Art. L.3121-41 à L.3121-44 CT** — aménagement du temps de travail sur une période supérieure à la semaine (au plus égale à l'année) par accord collectif : la durée du travail peut être répartie de sorte que les heures effectuées au-delà de 35 h en moyenne soient compensées par des jours de repos (JRTT), sans constituer des heures supplémentaires tant que la moyenne reste dans les limites de l'accord.
- **Principe** : les JRTT compensent les heures effectuées entre 35 h et l'horaire collectif, sans majoration (à la différence des heures supplémentaires).
- À défaut d'accord d'aménagement, les heures au-delà de 35 h relèvent du régime des heures supplémentaires (renvoi à l'outil dédié).

(à vérifier par avocat)

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `horaireHebdomadaireCollectif` | décimal | `horaireHebdomadaireCollectif` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| `accordCollectifPresent` | booléen | `accordAmenagementTempsPresent` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Consolidation IA critique** : les nouveaux champs IA de cet outil sont ajoutés au **sous-record consolidé `Sf218dDetail`** (un seul sous-record `@JsonUnwrapped` partagé par les 9 outils de la vague F-218d, dans `TravailExtractedData` du record `CaseAnalysisResponse.java`) — **PAS** un sous-record dédié, afin de ne pas dépasser la limite JVM de 255 paramètres du constructeur canonical. Clés JSON HTTP inchangées (plates).

**Flag CONTEXTUAL pivot** : `rtt_acquisition_detectee` (niveau 2, FR-only, default false) — nouveau flag `TravailExtractedData`. Bascule CONTEXTUAL quand l'IA détecte des signaux d'acquisition de JRTT (mentions « jours de RTT », « JRTT », « accord d'aménagement du temps de travail », « horaire collectif 37 heures / 39 heures », « jours de repos compensateurs de réduction du temps de travail »).

---

## Critères d'acceptation

- [ ] POST `horaireHebdomadaireCollectif=37`, `accordCollectifPresent=true`, `semainesTravailleesAn=47` → `statut=CALCULE`, `nombreJrttTheorique` ≈ 12,7 (94 / 7,4)
- [ ] POST `horaireHebdomadaireCollectif=39`, accord présent, 47 semaines → `nombreJrttTheorique` ≈ 23,6
- [ ] POST `accordCollectifPresent=false` → `statut=RENVOI_HEURES_SUP`, `nombreJrttTheorique=null`, note renvoi heures sup
- [ ] POST `semainesTravailleesAn` non fourni → défaut 47 appliqué
- [ ] POST réponse contient note « JRTT sans majoration »
- [ ] POST champ requis null → 400 ; `horaireHebdomadaireCollectif=35` → 400 ; `semainesTravailleesAn=0` → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert ; isolation workspace (A ne lit pas B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`rtt_acquisition_detectee`, trigger_value=`true`, FRANCE, DROIT_DU_TRAVAIL, priority 97
- [ ] `F-DT-80-rtt-acquisition` ajouté à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Plan de test minimal

- **UT** `RttAcquisitionAnalyzerTest` : ≥ 6 cas (37 h → ~12,7 JRTT, 39 h → ~23,6 JRTT, défaut 47 semaines, semaines réduites → moins de JRTT, pas d'accord → RENVOI_HEURES_SUP sans calcul, note sans majoration)
- **IT** `RttAcquisitionControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `rtt_acquisition_analyses`
- **Migrations** : `538-create-rtt-acquisition-analyses.xml` (create) + `539-seed-rtt-acquisition-visibility.xml` (seed visibility, priority 97)
- **Endpoint** `RttAcquisitionController` (POST + GET)
- **Service** `RttAcquisitionService` + **Analyzer** `RttAcquisitionAnalyzer`
- **Extension** `TravailExtractedData` : champs `horaireHebdomadaireCollectif` + `accordAmenagementTempsPresent` ajoutés au sous-record consolidé `Sf218dDetail` + flag `rttAcquisitionDetectee` + instruction `TRAVAIL_INSTRUCTION_PART42` dans `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-50)
- Heures supplémentaires et leur majoration (F-DT-19, situation distincte — renvoi)
- Monétisation / rachat de jours de RTT (F-DT-51, SF-218-37 — situation distincte)
- Forfait annuel en jours (régime spécifique, situation distincte)
- Régularisation des heures en fin de période d'aménagement (compteur réel, non l'objet du calcul théorique)
