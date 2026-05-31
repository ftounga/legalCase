# Mini-spec — F-218 / SF-218-33 — Délégué syndical / RSS : désignation et protection — backend

## Identifiant

`F-218 / SF-218-33`

## Feature parente

`F-218c` — IRP / négociation collective FR-only (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-31

## Branche Git

`feat/SF-218-33-delegation-syndicale-backend`

---

## Objectif

Analyser le **statut de délégué syndical (DS) ou de représentant de section syndicale (RSS)** : conditions de désignation selon l'effectif (DS dès 50 salariés, RSS dans les entreprises sans syndicat représentatif), **monopole syndical** (désignation par une organisation syndicale représentative ayant ≥ 10 % des suffrages, candidat ayant lui-même obtenu ≥ 10 %), et **protection contre le licenciement** (statut de salarié protégé, autorisation préalable de l'inspecteur du travail — art. L.2143-1 et suivants, L.2411-3 CT). **Analyseur statut + protection**. Inclus partiellement dans F-DT-30 (statut protégé RP général) mais éclaté ici sur la spécificité syndicale (vérifié — invariant « un outil = une situation »).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/delegation-syndicale-analysis`
- Body :
  - `effectif` (int, requis) — effectif de l'entreprise (DS possible dès 50 salariés)
  - `typeMandat` (enum requis) — `DELEGUE_SYNDICAL` | `RSS` (représentant de section syndicale)
  - `syndicatRepresentatif` (boolean, requis) — l'organisation désignante est représentative (≥ 10 % des suffrages au 1er tour CSE)
  - `pourcentageScorePersonnel` (BigDecimal 0..100, optionnel, DS) — score personnel du candidat aux dernières élections (condition ≥ 10 % pour DS, L.2143-3)
  - `dateDesignation` (LocalDate, optionnel) — date de désignation
  - `licenciementEnvisage` (boolean, défaut false) — un licenciement est envisagé / engagé
  - `autorisationInspecteurTravail` (boolean, défaut false) — autorisation préalable de l'inspecteur du travail obtenue
- Analyzer `DelegationSyndicaleAnalyzer` :
  - **Conditions de désignation** : checklist `{ item, conforme, commentaire }` :
    1. Effectif suffisant : DS → `effectif ≥ 50` ; RSS → pas de seuil d'effectif spécifique mais absence de syndicat représentatif.
    2. Organisation représentative : DS → `syndicatRepresentatif=true` obligatoire ; RSS → `syndicatRepresentatif=false` attendu (le RSS existe précisément faute de représentativité).
    3. Score personnel (DS uniquement) : `pourcentageScorePersonnel ≥ 10` (sauf exceptions — annoté).
  - **Verdict de désignation** `statutDesignation` ∈ { `REGULIERE`, `IRREGULIERE`, `A_VERIFIER` } :
    - tous items conformes → `REGULIERE`.
    - item d'effectif/représentativité non conforme → `IRREGULIERE`.
    - DS sans `pourcentageScorePersonnel` renseigné → `A_VERIFIER` (score à confirmer).
  - **Protection contre le licenciement** : DS et RSS sont des **salariés protégés**. `statutProtege = OUI`. Si `licenciementEnvisage=true` :
    - `autorisationInspecteurTravail=false` → `risqueNulliteLicenciement = ELEVE` + note « licenciement d'un salarié protégé sans autorisation de l'inspecteur du travail = nullité + réintégration (L.2411-3) ».
    - `autorisationInspecteurTravail=true` → `risqueNulliteLicenciement = FAIBLE`.
    - sinon (pas de licenciement) → `risqueNulliteLicenciement = SANS_OBJET`.
  - `baseJuridique` : art. L.2143-1 et suivants CT ; L.2411-3 CT — annoté `(à vérifier par avocat)`.
- Output persisté dans `delegation_syndicale_analyses` (1:1 case_file, upsert).
- **GET** `/api/v1/case-files/{caseFileId}/delegation-syndicale-analysis` → 200 ou 404.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| workspace.country ≠ FRANCE | 400 |
| caseFile.legalDomain ≠ DROIT_DU_TRAVAIL | 400 |
| un des booléens requis absent (null) | 400 |
| `typeMandat` absent / invalide | 400 |
| effectif ≤ 0 | 400 |
| `pourcentageScorePersonnel` hors [0,100] | 400 |
| caseFile inaccessible (autre workspace) | 404 |

---

## Source juridique

- **Art. L.2143-1 et suivants CT** — désignation du délégué syndical : par une organisation syndicale représentative dans les entreprises d'au moins 50 salariés ; le DS doit en principe avoir recueilli au moins 10 % des suffrages exprimés au 1er tour des dernières élections (L.2143-3). Le représentant de section syndicale (RSS) est désigné par un syndicat non représentatif (L.2142-1-1).
- **Art. L.2411-3 CT** — protection du délégué syndical : le licenciement est soumis à l'autorisation préalable de l'inspecteur du travail. À défaut, nullité et droit à réintégration.

---

## Champs IA à extraire

| Champ | Type | Champ source `TravailExtractedData` | Extension |
|---|---|---|---|
| `effectif` | entier | `effectifEntreprise` (existant) | Réutiliser si présent |
| `typeMandat` | énum | `mandatSyndicalType` (nouveau) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

**Flag CONTEXTUAL pivot** : `delegation_syndicale_detectee` (niveau 2, FR-only, default false) — nouveau flag `TravailExtractedData`. Bascule CONTEXTUAL quand l'IA détecte des signaux de délégation syndicale (mentions « délégué syndical », « DS », « représentant de section syndicale », « RSS », « désignation syndicale », « monopole syndical », « salarié protégé syndical », « autorisation de l'inspecteur du travail »).

---

## Critères d'acceptation

- [ ] POST `typeMandat=DELEGUE_SYNDICAL`, `effectif=80`, `syndicatRepresentatif=true`, `pourcentageScorePersonnel=15` → `statutDesignation=REGULIERE`
- [ ] POST DS `effectif=30` → item effectif non conforme, `statutDesignation=IRREGULIERE`
- [ ] POST DS `syndicatRepresentatif=false` → item représentativité non conforme, `IRREGULIERE`
- [ ] POST DS sans `pourcentageScorePersonnel` → `statutDesignation=A_VERIFIER`
- [ ] POST `typeMandat=RSS`, `syndicatRepresentatif=false` → désignation REGULIERE (pas de seuil de score)
- [ ] POST `licenciementEnvisage=true`, `autorisationInspecteurTravail=false` → `risqueNulliteLicenciement=ELEVE` ; avec autorisation → `FAIBLE` ; sans licenciement → `SANS_OBJET`
- [ ] `statutProtege=OUI` toujours
- [ ] POST booléen requis null → 400 ; `typeMandat` invalide → 400 ; effectif=0 → 400 ; score=150 → 400
- [ ] POST workspace BE → 400 ; caseFile DROIT_IMMIGRATION → 400
- [ ] GET sans POST → 404 ; POST deux fois → upsert ; isolation workspace (A ne lit pas B → 404)
- [ ] Seed `decision_tool_visibility_rules` : layer CONTEXTUAL, trigger_field=`delegation_syndicale_detectee`, trigger_value=`true`, FRANCE, DROIT_DU_TRAVAIL
- [ ] `F-DT-69-delegation-syndicale-protection` ajouté à `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Plan de test minimal

- **UT** `DelegationSyndicaleAnalyzerTest` : ≥ 6 cas (DS régulier → REGULIERE, DS effectif insuffisant → IRREGULIERE, DS non représentatif → IRREGULIERE, DS score absent → A_VERIFIER, RSS régulier, protection licenciement ELEVE/FAIBLE/SANS_OBJET)
- **IT** `DelegationSyndicaleControllerIT` : ≥ 5 cas (200 nominal, 400 country BE, 400 domaine, 404 isolation, upsert GET)

## Tables / endpoints / composants impactés

- **Nouvelle table** `delegation_syndicale_analyses`
- **Migrations** : `create-delegation-syndicale-analyses.xml` + `seed-delegation-syndicale-visibility.xml` (reconfirmer les numéros libres dans le worktree)
- **Endpoint** `DelegationSyndicaleController` (POST + GET)
- **Service** `DelegationSyndicaleService` + **Analyzer** `DelegationSyndicaleAnalyzer`
- **Extension** `TravailExtractedData` : champ `mandatSyndicalType` + flag `delegationSyndicaleDetectee` + prompt `LegalDomainPromptBuilder`
- **Test** `DashboardTileToolIdIntegrityIT.KNOWN_NO_DASHBOARD_TILE_IDS`

## Hors périmètre

- Composant Angular (SF-218-34)
- Statut protégé RP général (F-DT-30, situation distincte plus large)
- Procédure d'autorisation détaillée devant l'inspecteur du travail / recours
- Élections CSE / représentativité (F-DT-65, situation distincte)
