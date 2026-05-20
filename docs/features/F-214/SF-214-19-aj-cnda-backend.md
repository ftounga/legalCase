# Mini-spec — F-214 / SF-214-19 — AJ CNDA procédure — backend

## Identifiant

`F-214 / SF-214-19`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Vérifier l'éligibilité à l'aide juridictionnelle (AJ) devant la Cour nationale du droit d'asile (CNDA) et calculer les délais de demande, avec les conditions de ressources et de procédure.

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/aj-cnda-analysis`
- Body : `dateDecisionOFPRA` (LocalDate, requis), `ressourcesMensuellesNettes` (double), `procedureAcceleree` (boolean), `demandeAJDeposee` (boolean), `dateDepotAJ` (LocalDate, optionnel)
- Analyzer `AjCndaAnalyzer` :
  - Seuil AJ : ressourcesMensuellesNettes ≤ plafond AJ en vigueur (base 2024 : ~1 082 €/mois pour personne seule)
  - `dateEcheanceRecoursCNDA` = dateDecisionOFPRA + 1 mois (ou + 15 j si procédure accélérée)
  - `dateEcheanceDemandeAJ` = dateDecisionOFPRA + 15 j (demande AJ doit précéder le recours)
  - `eligibleAJ` : boolean (ressources)
  - `procedureAccelereeDureeReduite` : boolean — délai 15 j si procédure accélérée
  - `statut` ∈ {`AJ_A_DEMANDER`, `AJ_DEPOSEE`, `HORS_DELAI_AJ`, `NON_ELIGIBLE_RESSOURCES`}
  - `piecesAJ` : liste pièces pour demande AJ CNDA (formulaire Cerfa, justificatif ressources, notification OFPRA)
- Output persisté dans `aj_cnda_analyses` (1:1 case_file)
- **GET** `/api/v1/case-files/{caseFileId}/aj-cnda-analysis` → 200 ou 404

---

## Source juridique

- **Loi n° 91-647 du 10/07/1991** — aide juridictionnelle (articles 2, 4, 9-12).
- **Décret n° 2020-1717 du 28/12/2020** — plafonds AJ (art. 2 annexe I).
- **L. 532-1 à L. 532-35 CESEDA** — procédure CNDA.
- **L. 532-4 CESEDA** — délai de recours CNDA (1 mois ordinaire, 15 j procédure accélérée).
- **R. 532-1 à R. 532-7 CESEDA** — modalités AJ devant CNDA.

---

## Champs IA à extraire

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|---|
| `dateDecisionOFPRA` | date | `asileDateDecisionAnterieure` (proxy) | Réutiliser |
| `procedureAcceleree` | boolean | Absent | Extension record + prompt (`asileProcedureeAccelereee`) |

**Trigger CONTEXTUAL** : `procedureAsileDetectee` (existant F-201).

---

## Critères d'acceptation

- [x] POST ELIGIBLE_AJ retourne piecesAJ, dateEcheanceDemandeAJ
- [x] POST NON_ELIGIBLE_RESSOURCES retourne statut + message
- [x] POST procedureAcceleree=true → délai 15 j
- [x] POST workspace BE → 400
- [x] GET sans POST → 404
- [x] Isolation workspace
- [x] `F-IM-34-aj-cnda-fr` dans KNOWN_FRONTEND_TOOL_IDS
- [x] Seed : CONTEXTUAL, trigger_field=`procedure_asile_detectee`

## Plan de test minimal

- **UT** `AjCndaAnalyzerTest` : 6+ cas
- **IT** `AjCndaControllerIT` : 5+ cas

## Tables / endpoints / composants impactés

- **Nouvelle table** `aj_cnda_analyses`
- **Migration Liquibase** + seed visibility rules
- **Extension** `ImmigrationExtractedData` : champ `asileProcedureeAccelereee` (boolean)
- **Endpoint** `AjCndaController`

## Hors périmètre

- Composant Angular (SF-214-20)
