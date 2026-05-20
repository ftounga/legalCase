# Mini-spec — F-214 / SF-214-23 — Carte de résident L. 426-1 — backend

## Identifiant

`F-214 / SF-214-23`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Analyser l'éligibilité à la carte de résident 10 ans (L. 426-1 CESEDA) selon les critères de durée de séjour régulier, d'intégration et de ressources.

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/carte-resident-analysis`
- Body : `dureeSejourRegulierAnnees` (int), `typesTitresAnterieurs` (string, optionnel), `niveauIntegration` (enum `FORT` | `MOYEN` | `FAIBLE`), `ressourcesMensuellesNettes` (double), `condamnationsPenalesGraves` (boolean)
- Analyzer `CarteResidentAnalyzer` :
  - Critères L. 426-1 : 5 ans de séjour régulier + intégration républicaine (langue, valeurs) + ressources ≥ SMIC
  - `verdict` ∈ {`ELIGIBLE`, `ELIGIBLE_SOUS_RESERVE`, `NON_ELIGIBLE_DELAI`, `NON_ELIGIBLE_INTEGRATION`, `NON_ELIGIBLE_RESSOURCES`, `INADMISSIBLE` (condamnation)}
  - `chipsCriteresNonRemplis` : liste critères
  - `atouts` : liste éléments favorables à valoriser dans le dossier
  - `baseJuridique` : L. 426-1 + R. 426-1+ CESEDA
- Output persisté dans `carte_resident_analyses` (1:1 case_file)
- **GET** `/api/v1/case-files/{caseFileId}/carte-resident-analysis` → 200 ou 404

---

## Source juridique

- **L. 426-1 à L. 426-12 CESEDA** (recodification 2021, anciens L. 314-1+) — carte résident 10 ans.
- **R. 426-1 à R. 426-6 CESEDA** — conditions et procédure.
- **L. 426-3 CESEDA** — carte de résident permanent (durée illimitée).
- **CE 27 juillet 2005, n° 268600** (à vérifier) — critères intégration.
- **Loi 26/01/2024** : renforcement exigences intégration (test de langue).

---

## Champs IA à extraire

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|---|
| `dureeSejourRegulierAnnees` | int | `aesDureePresenceMois` / 12 (proxy) | Dériver |
| `ressourcesMensuellesNettes` | double | Absent | Extension record + prompt (`carteResidentRessources`) |

**Nouveau flag CONTEXTUAL** : `carteResidentEnvisagee` (boolean) — extraction : mentions "carte de résident", "L.426-1", "séjour de 10 ans", "titre 10 ans", "résidence permanente", durée présence ≥ 5 ans + titre actuel VPF. Ajouté dans `ImmigrationExtractedData` + prompt.

---

## Critères d'acceptation

- [x] POST ELIGIBLE retourne verdict, atouts
- [x] POST NON_ELIGIBLE_DELAI (4 ans) retourne chipsCriteresNonRemplis SEJOUR_INSUFFISANT
- [x] POST INADMISSIBLE si condamnationsPenalesGraves → verdict INADMISSIBLE
- [x] POST workspace BE → 400
- [x] GET sans POST → 404
- [x] Isolation workspace
- [x] `F-IM-36-carte-resident-l4261-fr` dans KNOWN_FRONTEND_TOOL_IDS
- [x] Seed : CONTEXTUAL, trigger_field=`carte_resident_envisagee`

## Plan de test minimal

- **UT** `CarteResidentAnalyzerTest` : 6+ cas
- **IT** `CarteResidentControllerIT` : 5+ cas

## Tables / endpoints / composants impactés

- **Nouvelle table** `carte_resident_analyses`
- **Migration Liquibase** + seed visibility rules
- **Extension** `ImmigrationExtractedData` : flag `carteResidentEnvisagee` + champ `carteResidentRessources`
- **Endpoint** `CarteResidentController`

## Hors périmètre

- Composant Angular (SF-214-24)
- Carte résident conjoint Français 3 ans et parent enfant français (L. 426-5) → P3 F-220
