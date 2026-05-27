# Mini-spec — F-219 / SF-219-01-backend Outil RCC métiers lourds — analyseur éligibilité 58+/35 ans

## Identifiant

`F-219 / SF-219-01-backend`

## Feature parente

`F-219` — P3 Travail BE — ~32 outils BE-only spécificité

## Statut

`ready`

## Date de création

2026-05-27

## Branche Git

`feat/SF-219-01-backend-rcc-be-metiers-lourds`

---

## Objectif

Analyser l'éligibilité à un **RCC métiers lourds** (régime de chômage avec complément d'entreprise, ex-prépension) selon les conditions âge ≥ 58 ans / carrière ≥ 35 ans dont au moins 5/7 ou 7/15 ans en métier lourd, selon **CCT 17 + AR 03/05/2007 art. 3** (régime spécifique métiers lourds). **BELGIQUE UNIQUEMENT** — aucun équivalent FR. Distinct du RCC général couvert par F-207 SF-207-06 (60+/40).

---

## Source juridique BE

- **CCT n° 17** du 19/12/1974 (Conseil National du Travail) — instaurant un régime d'indemnité complémentaire aux travailleurs âgés en cas de licenciement.
- **AR du 3 mai 2007** fixant le RCC, **art. 3** (métiers lourds 58+ / 35 ans carrière).
- **Liste des métiers lourds** (à vérifier par avocat BE) — annexes AR + arrêtés ministériels : travail en équipes successives avec prestations de nuit, travail en interruptions à horaires variables, travail pénible (manutention, construction, mines, etc.).
- **Conditions cumulatives** : âge ≥ 58 ans à la fin du contrat ; carrière professionnelle ≥ 35 ans ; au moins **5 ans en métier lourd dans les 10 dernières années** OU **7 ans dans les 15 dernières années**.
- **Licenciement obligatoire** : RCC n'est pas une démission — l'employeur doit licencier (préavis ou indemnité compensatoire).

---

## Comportement attendu

### Cas nominal

`POST /api/v1/case-files/{caseFileId}/decision-tools/rcc-be-metiers-lourds`

Inputs (body) :
- `ageFinContrat` (int, années) — âge du travailleur à la date prévue de fin de contrat — obligatoire.
- `anneesCarriereTotal` (int, années) — carrière professionnelle totale — obligatoire.
- `anneesMetierLourdRecent10` (int, années dans les 10 dernières) — obligatoire.
- `anneesMetierLourdRecent15` (int, années dans les 15 dernières) — obligatoire.
- `typeMetierLourd` (enum, `EQUIPES_SUCCESSIVES_NUIT` | `INTERRUPTIONS_HORAIRES_VARIABLES` | `TRAVAIL_PENIBLE` | `AUTRE`) — obligatoire.
- `licenciementEffectif` (boolean) — `true` si l'employeur licencie (vs démission) — obligatoire.

Logique (`RccBeMetiersLoursValidator`) :

| Condition | Verdict | Raison |
|---|---|---|
| `licenciementEffectif = false` | `INELIGIBLE` | `DEMISSION_NON_ELIGIBLE` — RCC réservé au licenciement |
| `ageFinContrat < 58` | `INELIGIBLE` | `AGE_INSUFFISANT` |
| `anneesCarriereTotal < 35` | `INELIGIBLE` | `CARRIERE_INSUFFISANTE` |
| `anneesMetierLourdRecent10 < 5 && anneesMetierLourdRecent15 < 7` | `INELIGIBLE` | `DUREE_METIER_LOURD_INSUFFISANTE` |
| Toutes conditions OK | `ELIGIBLE` | — |

Output (`RccBeMetiersLoursResponse`) :
```json
{
  "verdict": "ELIGIBLE" | "INELIGIBLE",
  "raisonIneligibilite": null | "DEMISSION_NON_ELIGIBLE" | "AGE_INSUFFISANT" | "CARRIERE_INSUFFISANTE" | "DUREE_METIER_LOURD_INSUFFISANTE",
  "synthese": "Le travailleur est éligible au RCC métiers lourds (CCT 17 + AR 03/05/2007 art. 3).",
  "baseJuridique": "CCT n°17 du 19/12/1974 ; AR du 03/05/2007 art. 3",
  "avertissement": "Liste des métiers lourds à vérifier annuellement — voir AR + arrêtés ministériels en vigueur."
}
```

Persistance : table `rcc_be_metiers_lourds_analyses` — 1 ligne par dossier (unique sur `case_file_id`, mise à jour à chaque POST). Inputs + verdict persistés en JSON (`result_data`).

`GET` du même path renvoie la dernière analyse ou 404.

### Cas d'erreur

| Situation | Code | Comportement |
|---|---|---|
| `workspaceCountry !== 'BELGIQUE'` | 404 | Isolation BE-only |
| `caseFileId` hors workspace | 404 | Isolation workspace standard |
| `ageFinContrat` < 18 ou > 80 | 400 | « Âge invalide » |
| `anneesCarriereTotal` < 0 ou > 60 | 400 | « Carrière invalide » |
| `typeMetierLourd` manquant | 400 | « typeMetierLourd obligatoire » |

---

## Critères d'acceptation

- [ ] `POST` retourne `INELIGIBLE` + `DEMISSION_NON_ELIGIBLE` si `licenciementEffectif=false`.
- [ ] `POST` retourne `INELIGIBLE` + `AGE_INSUFFISANT` si `ageFinContrat < 58`.
- [ ] `POST` retourne `INELIGIBLE` + `CARRIERE_INSUFFISANTE` si carrière < 35.
- [ ] `POST` retourne `INELIGIBLE` + `DUREE_METIER_LOURD_INSUFFISANTE` si ni 5/10 ni 7/15.
- [ ] `POST` retourne `ELIGIBLE` si toutes conditions remplies.
- [ ] `POST` workspace France → 404.
- [ ] `GET` renvoie dernière analyse ou 404.
- [ ] `DashboardTileToolIdIntegrityIT` reste vert (tool_id ajouté à `KNOWN_NO_DASHBOARD_TILE_IDS` en SF-219-01b).

---

## Périmètre

### Hors scope

- Frontend (`rcc-be-metiers-lourds-section.component`) — SF-219-01b.
- Calcul de l'indemnité complémentaire — couvert par F-207 SF-207-07 (`rcc-be-indemnite-complementaire`) qui s'applique à tous les régimes RCC.
- RCC longue carrière 59+/40 — SF-219-02.
- RCC entreprise difficulté — SF-219-03.
- Cumul RCC + ONEM — SF-219-04.
- Liste détaillée des fonctions/secteurs métiers lourds (référentiel à enrichir ultérieurement).

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/decision-tools/rcc-be-metiers-lourds` | OIDC | LAWYER |
| GET  | `/api/v1/case-files/{caseFileId}/decision-tools/rcc-be-metiers-lourds` | OIDC | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|---|---|---|
| `rcc_be_metiers_lourds_analyses` | INSERT / UPDATE / SELECT | Unique sur `case_file_id`. Colonnes : `id` UUID, `case_file_id` FK CASCADE, `result_data` TEXT JSON, `created_at`, `updated_at`. |

### Migration Liquibase

`XXX-create-rcc-be-metiers-lourds-analyses.xml` — reversible (`<rollback><dropTable /></rollback>`). UUID pré-assigné par convention F-219.

### Composants backend (pattern F-213 autonome)

- `RccBeMetiersLourdsAnalysis.java` — entité JPA
- `RccBeMetiersLourdsRepository.java`
- `RccBeMetiersLourdsTypeEnum.java` — 4 valeurs
- `RccBeMetiersLourdsVerdictEnum.java` — 2 valeurs
- `RccBeMetiersLourdsRequest.java` — DTO POST
- `RccBeMetiersLourdsResult.java` — record verdict
- `RccBeMetiersLourdsResponse.java` — DTO GET
- `RccBeMetiersLourdsService.java`
- `RccBeMetiersLourdsValidator.java` — fonction pure
- `RccBeMetiersLourdsController.java`
- `ToolBranchRegistry` entry pour `rcc-be-metiers-lourds`
- `ToolUsageContributor` entry pour comptage

**Pattern F-213 backend autonome** (`feedback_f213_backend_pattern`) — **pas de modif** `TravailExtractedData` / `CritereCode` / `LegalDomainPromptBuilder` / `SYSTEM_PROMPT_TEMPLATEs`. Saisie avocat manuelle V1 sur tous les champs (pré-fill IA à consolider en feature dédiée ultérieurement).

---

## Plan de test

### Unitaires (`RccBeMetiersLourdsValidatorTest`)

- [ ] Démission → `INELIGIBLE` / `DEMISSION_NON_ELIGIBLE`.
- [ ] Âge 57 ans → `INELIGIBLE` / `AGE_INSUFFISANT`.
- [ ] Carrière 34 ans → `INELIGIBLE` / `CARRIERE_INSUFFISANTE`.
- [ ] Métier lourd 4/10 ans ET 6/15 ans → `INELIGIBLE` / `DUREE_METIER_LOURD_INSUFFISANTE`.
- [ ] Métier lourd 5/10 ans → `ELIGIBLE`.
- [ ] Métier lourd 7/15 ans → `ELIGIBLE`.
- [ ] Toutes conditions OK → `ELIGIBLE`.

### Intégration (`RccBeMetiersLourdsControllerIT`)

- [ ] `POST` workspace BE → 200 + persistance.
- [ ] `POST` workspace FR → 404.
- [ ] `POST` `caseFileId` autre workspace → 404.
- [ ] `GET` après POST → 200.
- [ ] `GET` sans POST → 404.
- [ ] Validation Bean : `ageFinContrat` négatif → 400.

### Isolation workspace

- [x] Applicable — standard.

---

## Analyse d'impact

### Préoccupations transversales

- [x] **Workspace context** — gate `workspaceCountry=BELGIQUE` strict.
- [x] **Outil décisionnel métier** — création d'un outil, invariant un outil = une situation métier respecté (≠ F-207 RCC général). Audit §4.4 recommande explicitement de splitter RCC.
- Auth / Plans / Navigation — non touchés.

### Composants impactés (pattern F-213 — minimal)

| Composant | Impact | Test de non-régression |
|---|---|---|
| `ToolBranchRegistry` | Ajout entry `rcc-be-metiers-lourds` | Tests existants |
| `ToolUsageContributor` | Ajout entry comptage | Tests existants |
| `DecisionToolVisibilityIntegrityIT` | Nouveau tool_id seedé par SF-219-01b | `KNOWN_FRONTEND_TOOL_IDS` à jour côté frontend |
| `DashboardTileToolIdIntegrityIT` | tool_id ajouté à `KNOWN_NO_DASHBOARD_TILE_IDS` côté SF-219-01b | Critique sinon master-red |

---

## Dépendances

- Aucune SF F-219 bloquante. Peut démarrer indépendamment.
- F-207 SF-207-06/07 (RCC général + indemnité complémentaire) déjà mergées — fournissent le contexte fonctionnel mais ne sont pas bloquantes techniquement.
