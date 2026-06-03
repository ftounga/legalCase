# Mini-spec — F-218 / SF-218-37 — RTT : monétisation (rachat de jours de RTT) — backend

## Identifiant

`F-218 / SF-218-37`

## Feature parente

`F-218d` — Temps de travail / congés FR-only (P3 Travail FR — différé signal terrain, réactivé)

## Statut

`ready`

## Date de création

2026-06-03

## Branche Git

`feat/SF-218-37-rtt-monetisation-backend`

---

## Objectif

Analyser l'**éligibilité et le montant de la monétisation de jours de RTT** (rachat de jours de réduction du temps de travail) dans le cadre du dispositif de la loi de finances rectificative pour 2022 (loi n° 2022-1157 du 16/08/2022, art. 5), **prolongé jusqu'au 31/12/2026** : sur demande du salarié et avec accord de l'employeur, les jours ou demi-journées de RTT acquis entre le 01/01/2022 et le 31/12/2026 peuvent être renoncés contre une rémunération majorée, bénéficiant du régime social et fiscal des heures supplémentaires (exonération dans plafond). **Calculateur d'indemnité**. Aucun outil existant ne couvre la monétisation de RTT (vérifié — invariant « un outil = une situation » ; distinct de F-DT-19 heures supplémentaires et de F-DT-80 acquisition de JRTT).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/rtt-monetisation-analysis`
- Body :
  - `nombreJoursRttRenonces` (int, requis, > 0) — nombre de jours de RTT auxquels le salarié renonce
  - `salaireJournalierBrut` (BigDecimal, requis, > 0) — salaire journalier brut de référence
  - `tauxMajorationConventionnel` (Double, optionnel, défaut `25`, borné 10–25) — taux de majoration applicable (≥ taux de la 1re heure supplémentaire, 10–25 %)
  - `joursAcquisDansFenetre` (boolean, requis) — les jours sont acquis entre le 01/01/2022 et le 31/12/2026 (fenêtre du dispositif)
- Analyzer `RttMonetisationAnalyzer` :
  - **Applicabilité** : si `joursAcquisDansFenetre = false` → `statut = NON_ELIGIBLE` + note « jours hors de la fenêtre du dispositif de monétisation (01/01/2022 → 31/12/2026, loi LFR 2022) », pas de calcul de montant.
  - **Borne taux** : `tauxMajorationConventionnel` ramené dans [10, 25] (un taux < 10 est porté à 10 ; un taux > 25 est porté à 25 avec note « majoration plafonnée au régime des heures supplémentaires »).
  - **Calcul montant** : `montantBrut = nombreJoursRttRenonces × salaireJournalierBrut × (1 + tauxMajorationConventionnel / 100)`.
  - **Régime** : `regimeSocialFiscal = "ALIGNE_HEURES_SUPPLEMENTAIRES"` — note « exonération de cotisations salariales et d'impôt sur le revenu dans le plafond applicable aux heures supplémentaires (à vérifier par avocat) ».
  - **Verdict** `statut` ∈ { `ELIGIBLE`, `NON_ELIGIBLE` } : `ELIGIBLE` si `joursAcquisDansFenetre = true`.
  - `montantBrut` (BigDecimal, 2 décimales) ; `tauxApplique` (Double) ; `regimeSocialFiscal` (String).
  - `baseJuridique` : loi n° 2022-1157 du 16/08/2022 (LFR 2022) art. 5, dispositif prolongé jusqu'au 31/12/2026 — annoté `(à vérifier par avocat)`.
- Output persisté dans `rtt_monetisation_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/rtt-monetisation-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| un des champs requis absent (null) | 400 |
| `nombreJoursRttRenonces` ≤ 0 | 400 |
| `salaireJournalierBrut` ≤ 0 | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **Loi n° 2022-1157 du 16/08/2022 de finances rectificative pour 2022, art. 5** — monétisation des jours de RTT : renonciation à des jours/demi-journées de RTT acquis entre le 01/01/2022 et le 31/12/2026, sur demande du salarié et avec accord de l'employeur, contre rémunération majorée.
- **Taux de majoration** : au moins égal au taux de majoration de la première heure supplémentaire applicable dans l'entreprise (10–25 %).
- **Régime social et fiscal** : aligné sur celui des heures supplémentaires (exonération de cotisations salariales et d'impôt sur le revenu dans le plafond applicable).
- Dispositif initialement temporaire, **prolongé jusqu'au 31/12/2026**.

(à vérifier par avocat)

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `nombreJoursRttRenonces` | entier | `nombreJoursRttRenonces` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| `salaireJournalierBrut` | décimal | `salaireJournalierBrut` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Consolidation IA critique** : les nouveaux champs IA de cet outil sont ajoutés au **sous-record consolidé `Sf218dDetail`** (un seul sous-record `@JsonUnwrapped` partagé par les 9 outils de la vague F-218d, dans `TravailExtractedData` du record `CaseAnalysisResponse.java`) — **PAS** un sous-record dédié, afin de ne pas dépasser la limite JVM de 255 paramètres du constructeur canonical. Clés JSON HTTP inchangées (plates).

**Flag CONTEXTUAL pivot** : `rtt_monetisation_detectee` (niveau 2, FR-only, default false) — nouveau flag `TravailExtractedData`. Bascule CONTEXTUAL quand l'IA détecte des signaux de monétisation de RTT (mentions « rachat de RTT », « monétisation des RTT », « renonciation à des jours de RTT », « jours de RTT payés », « rémunération majorée des RTT »).

---

## Critères d'acceptation

- [ ] POST `nombreJoursRttRenonces=5`, `salaireJournalierBrut=200`, `joursAcquisDansFenetre=true`, taux défaut → `statut=ELIGIBLE`, `montantBrut=1250.00` (5 × 200 × 1.25)
- [ ] POST `tauxMajorationConventionnel=10` → `tauxApplique=10`, montant recalculé
- [ ] POST `tauxMajorationConventionnel=40` → borné à 25, note plafonnement
- [ ] POST `joursAcquisDansFenetre=false` → `statut=NON_ELIGIBLE`, pas de montant, note hors fenêtre
- [ ] POST champ requis null → 400 ; `nombreJoursRttRenonces=0` → 400 ; `salaireJournalierBrut=0` → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert ; isolation workspace (A ne lit pas B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`rtt_monetisation_detectee`, trigger_value=`true`, FRANCE, DROIT_DU_TRAVAIL, priority 91
- [ ] `F-DT-51-rtt-monetisation` ajouté à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Plan de test minimal

- **UT** `RttMonetisationAnalyzerTest` : ≥ 6 cas (nominal ELIGIBLE + montant exact, taux par défaut 25, taux borné bas 10, taux borné haut → 25, hors fenêtre → NON_ELIGIBLE sans montant, régime aligné heures sup)
- **IT** `RttMonetisationControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `rtt_monetisation_analyses`
- **Migrations** : `526-create-rtt-monetisation-analyses.xml` (create) + `527-seed-rtt-monetisation-visibility.xml` (seed visibility, priority 91)
- **Endpoint** `RttMonetisationController` (POST + GET)
- **Service** `RttMonetisationService` + **Analyzer** `RttMonetisationAnalyzer`
- **Extension** `TravailExtractedData` : champs `nombreJoursRttRenonces` + `salaireJournalierBrut` ajoutés au sous-record consolidé `Sf218dDetail` + flag `rttMonetisationDetectee` + instruction `TRAVAIL_INSTRUCTION_PART36` dans `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-38)
- Acquisition des JRTT selon accord d'aménagement (F-DT-80, SF-218-49 — situation distincte)
- Heures supplémentaires (F-DT-19, situation distincte)
- Calcul détaillé du plafond d'exonération sociale/fiscale (renvoi au régime heures sup, pas recalculé ici)
