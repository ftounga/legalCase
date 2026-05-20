# Mini-spec — F-214 / SF-214-03 — Regroupement familial éligibilité + ressources — backend

## Identifiant

`F-214 / SF-214-03`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-214-03-regroupement-familial-backend`

---

## Objectif

Analyser l'éligibilité au regroupement familial (L. 434-1+ CESEDA) incluant le calcul des ressources du regroupant (seuil SMIC), les conditions de logement et de durée de séjour, et persister l'analyse 1:1 par dossier.

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/regroupement-familial-analysis`
- Body : `dureeSejourRegulierMois` (int), `ressourcesMensuellesNettes` (double), `tailleLogementM2` (int), `nombrePersonnesFoyer` (int), `typeRegroupement` (enum `CONJOINT` | `ENFANT_MINEUR` | `AUTRE`), `membresFamilleARegrouper` (int, 1-6)
- Analyzer `RegroupementFamilialAnalyzer` :
  - Critères L. 434-1 : durée séjour régulier ≥ 18 mois (R. 434-4)
  - Calcul seuil SMIC mensuel net × membres : ressources ≥ SMIC × 1.0 + 0.2 × (membres - 1) (référence R. 434-30)
  - Calcul surface habitable minimale (R. 434-20) : 9 m² par personne supplémentaire au-delà de la 1ʳᵉ
  - `verdict` ∈ {`ELIGIBLE`, `ELIGIBLE_SOUS_RESERVE`, `NON_ELIGIBLE_DELAI`, `NON_ELIGIBLE_RESSOURCES`, `NON_ELIGIBLE_LOGEMENT`}
  - `chipsCriteresNonRemplis` : liste critères non satisfaits
  - `ressourcesRequises` : montant calculé
  - `surfaceRequise` : m² calculés
- Output persisté dans `regroupement_familial_analyses` (1:1 case_file)
- **GET** `/api/v1/case-files/{caseFileId}/regroupement-familial-analysis` → 200 ou 404

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_IMMIGRATION | 400 |
| dureeSejourRegulierMois < 0 | 400 |
| ressourcesMensuellesNettes ≤ 0 | 400 |
| tailleLogementM2 ≤ 0 | 400 |
| membresFamilleARegrouper hors [1-6] | 400 |
| caseFile inaccessible au workspace | 404 |

---

## Source juridique

- **L. 434-1 à L. 434-13 CESEDA** — regroupement familial (recodification 2021, anciens L. 411-1+).
- **R. 434-1 à R. 434-35 CESEDA** — conditions de ressources, logement, durée séjour.
- **R. 434-4** : durée séjour régulier ≥ 18 mois.
- **R. 434-20** : surface habitable (9 m² par personne).
- **R. 434-30** : ressources (SMIC mensuel net hors allocations familiales, APL, RSA).
- **CE 22 mars 2010, n° 301750** — exclusion APL du calcul des ressources.
- **Loi 26/01/2024** (Darmanin) : durcissement conditions (à vérifier impact R. 434-30).

---

## Champs IA à extraire

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|---|
| `dureeSejourRegulierMois` | int | `aesDureePresenceMois` (proxy) | Réutiliser |
| `ressourcesMensuellesNettes` | double | Absent | Extension record + prompt (`regroupementRessourcesMensuelles`) |
| `typeRegroupement` | enum | Absent | Extension record + prompt (`regroupementType`) |

**Nouveau flag CONTEXTUAL** : `regroupementFamilialEnvisage` (boolean) — extraction : mentions "regroupement familial", "OFII", "membre de famille", "rejoindre en France", "visa long séjour famille". Ajouté dans `ImmigrationExtractedData` + prompt.

---

## Critères d'acceptation

- [x] POST ELIGIBLE retourne 200 avec verdict, ressourcesRequises, surfaceRequise
- [x] POST NON_ELIGIBLE_RESSOURCES retourne chipsCriteresNonRemplis avec code RESSOURCES_INSUFFISANTES
- [x] POST NON_ELIGIBLE_DELAI retourne chipsCriteresNonRemplis avec code SEJOUR_INSUFFISANT
- [x] POST workspace BE → 400
- [x] POST domaine travail → 400
- [x] GET sans POST préalable → 404
- [x] POST upsert → 200 (remplacement)
- [x] Isolation workspace
- [x] `F-IM-26-regroupement-familial-fr` dans KNOWN_FRONTEND_TOOL_IDS
- [x] Seed `decision_tool_visibility_rules` : CONTEXTUAL, trigger_field=`regroupement_familial_envisage`

## Plan de test minimal

- **UT** `RegroupementFamilialAnalyzerTest` : 8+ cas (ELIGIBLE, SOUS_RESERVE, NON_ELIGIBLE_DELAI, NON_ELIGIBLE_RESSOURCES, NON_ELIGIBLE_LOGEMENT, calcul SMIC, calcul surface, exclusion APL)
- **IT** `RegroupementFamilialControllerIT` : 6+ cas

## Tables / endpoints / composants impactés

- **Nouvelle table** `regroupement_familial_analyses` (standard pattern — id UUID, case_file_id UUID UNIQUE, champs input, country, result_data TEXT, timestamps)
- **Migration Liquibase** + seed `decision_tool_visibility_rules`
- **Extension** `ImmigrationExtractedData` + prompt : flag `regroupementFamilialEnvisage` + champs ressources + type
- **Endpoint** `RegroupementFamilialController` (POST, GET)

## Hors périmètre

- Composant Angular (SF-214-04)
- Procédure OFII de traitement (checklist pièces → F-IM-01 existant)
- Cas regroupement sur place (dérogation R. 434-7) — reporté F-220
