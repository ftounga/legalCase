# Mini-spec — F-214 / SF-214-29 — Recours refus naturalisation TJ — backend

## Identifiant

`F-214 / SF-214-29`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Calculer les délais de recours devant le Tribunal judiciaire contre un refus de déclaration de nationalité (voies Cciv 21-2 mariage, 21-13 ascendant, 21-14 mineur) — procédure judiciaire distincte du recours administratif (TA Nantes, SF-214-31).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/naturalisation-recours-tj-analysis`
- Body : `voieNaturalisation` (enum `MARIAGE` | `ASCENDANT` | `MINEUR_22_1`), `dateRefusDeclaration` (LocalDate, requis), `typeRefus` (enum `REFUS_ENREGISTREMENT` | `CONTESTATION_NATIONALITE`)
- Calculator `NaturalisationRecoursTjCalculator` :
  - `dateEcheanceRecoursJudicaire` = dateRefusDeclaration + 6 mois (Cciv 26-3 — délai recours tribunal)
  - `joursRestants` = dateEcheanceRecoursJudicaire - today
  - `tribunalCompetent` : TJ du lieu de résidence (juridiction civile — DISTINCT du TA Nantes)
  - `basesJuridiques` : Cciv 26-3 + Cciv 21-2/21-13/22-1 selon voie
  - `motifsRecoursDisponibles` : liste selon voie (ex. mariage : erreur sur communauté de vie, erreur sur durée, vice de forme)
  - `statut` ∈ {`RECOURS_POSSIBLE`, `URGENT` (< 30 j), `PRESCRIT`}
- Output persisté dans `naturalisation_recours_tj_analyses` (1:1 case_file)
- **GET** `/api/v1/case-files/{caseFileId}/naturalisation-recours-tj-analysis` → 200 ou 404

---

## Source juridique

- **Cciv 26-3** — recours contre refus d'enregistrement de déclaration de nationalité, délai 6 mois devant TJ.
- **Cciv 21-2** — nationalité par mariage.
- **Cciv 21-13** — nationalité par ascendance française.
- **Cciv 22-1** — nationalité du mineur.
- **CPC art. 1043** (à vérifier) — procédure matières de nationalité devant TJ.

**Distinction TJ vs TA Nantes** : Le recours TJ (cet outil SF-214-29) concerne les DÉCLARATIONS de nationalité (mariage, ascendant, mineur). Le recours TA Nantes (SF-214-31) concerne le DÉCRET de naturalisation (voie DECRET_21_15). Ce sont deux procédures distinctes avec des délais et tribunaux différents.

---

## Champs IA à extraire

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|---|
| `voieNaturalisation` (proxy) | texte | Absent | Extension record + prompt (`naturalisationVoie`) |
| `dateRefusDeclaration` | date | Absent | Extension record + prompt (`naturalisationDateRefus`) |

**Trigger CONTEXTUAL** : `naturalisationEnvisageeDetectee` (existant F-201).

---

## Critères d'acceptation

- [x] POST voie MARIAGE → motifsRecoursDisponibles mariage, tribunalCompetent TJ
- [x] POST statut PRESCRIT si > 6 mois → motifsRecoursDisponibles vide + message prescription
- [x] POST URGENT < 30 j → statut URGENT
- [x] POST workspace BE → 400
- [x] GET sans POST → 404
- [x] Isolation workspace
- [x] `F-IM-39-naturalisation-recours-tj-fr` dans KNOWN_FRONTEND_TOOL_IDS
- [x] Seed : CONTEXTUAL, trigger_field=`naturalisation_envisagee_detectee`

## Plan de test minimal

- **UT** `NaturalisationRecoursTjCalculatorTest` : 6+ cas
- **IT** `NaturalisationRecoursTjControllerIT` : 5+ cas

## Tables / endpoints / composants impactés

- **Nouvelle table** `naturalisation_recours_tj_analyses`
- **Migration Liquibase** + seed visibility rules
- **Extension** `ImmigrationExtractedData` : champs `naturalisationVoie` + `naturalisationDateRefus`
- **Endpoint** `NaturalisationRecoursTjController`

## Hors périmètre

- Composant Angular (SF-214-30)
- Recours TA Nantes pour refus décret (SF-214-31)
