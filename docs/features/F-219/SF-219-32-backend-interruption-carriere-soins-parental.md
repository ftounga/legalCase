# Mini-spec — F-219 / SF-219-32-backend Outil interruption carriere soins parental — calculateur durée + allocation

## Identifiant

`F-219 / SF-219-32-backend`

## Feature parente

`F-219` — P3 Travail BE — ~32 outils BE-only spécificité

## Statut

`ready`

## Date de création

2026-05-27

## Branche Git

`feat/SF-219-32-backend-interruption-carriere-soins-parental`

---

## Objectif

Calculer le **droit au congé parental BE** (4 mois — équivalent temps plein, mi-temps ou 1/5e) + cumul allocations ONEM. **BELGIQUE UNIQUEMENT** — régime ONEM distinct du congé parental FR.

---

## Source juridique BE

- CCT n°64 ; Loi du 22/01/1985
- Détails et conditions spécifiques à vérifier par avocat BE avant seed production.
- **Pattern F-213 backend autonome** appliqué : pas de modification transverse (`TravailExtractedData`, `CritereCode`, `LegalDomainPromptBuilder`, `SYSTEM_PROMPT_TEMPLATEs`). L'outil est totalement autonome.

---

## Comportement attendu

### Cas nominal

`POST /api/v1/case-files/{caseFileId}/decision-tools/interruption-carriere-soins-parental`

Inputs (body) — voir détail logique métier ci-dessous.

Logique (`InterruptionCarriereSoinsParentalService` + `InterruptionCarriereSoinsParentalCalculator`) :

Calcule : éligibilité (12 mois ancienneté chez employeur, enfant < 12 ans / 21 ans handicap), formes (4 mois temps plein / 8 mois mi-temps / 20 mois 1/5e), allocation ONEM forfaitaire mensuelle (250-450 € selon forme), protection contre licenciement.

Output (`InterruptionCarriereSoinsParentalResponse`) :
```json
{
  "verdict": "<verdict métier>",
  "raison": "<code raison>",
  "synthese": "<phrase synthétique>",
  "baseJuridique": "CCT n°64",
  "avertissement": "<si applicable>"
}
```

Persistance : table `interruption_carriere_soins_parental_analyses` — 1 ligne par dossier (unique sur `case_file_id`, mise à jour à chaque POST). Inputs + verdict persistés en JSON (`result_data`).

`GET` du même path renvoie la dernière analyse ou 404.

### Cas d'erreur

| Situation | Code | Comportement |
|---|---|---|
| `workspaceCountry !== 'BELGIQUE'` | 404 | Isolation BE-only |
| `caseFileId` hors workspace | 404 | Isolation workspace standard |
| Inputs invalides (négatifs, hors enum, manquants) | 400 | Validation Bean |

---

## Critères d'acceptation

- [ ] `POST` retourne le verdict attendu pour chaque cas métier identifié.
- [ ] `POST` workspace France → 404.
- [ ] `GET` renvoie dernière analyse ou 404.
- [ ] `DashboardTileToolIdIntegrityIT` reste vert (tool_id ajouté à `KNOWN_NO_DASHBOARD_TILE_IDS` en SF-219-32b).
- [ ] Migration Liquibase reversible.

---

## Périmètre

### Hors scope

- Frontend (`interruption-carriere-soins-parental-section.component`) — SF-219-32b.
- Autres outils F-219.

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/decision-tools/interruption-carriere-soins-parental` | OIDC | LAWYER |
| GET  | `/api/v1/case-files/{caseFileId}/decision-tools/interruption-carriere-soins-parental` | OIDC | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|---|---|---|
| `interruption_carriere_soins_parental_analyses` | INSERT / UPDATE / SELECT | Unique sur `case_file_id`. Colonnes : `id` UUID, `case_file_id` FK CASCADE, `result_data` TEXT JSON, `created_at`, `updated_at`. |

### Migration Liquibase

`XXX-create-interruption_carriere_soins_parental-analyses.xml` — reversible (`<rollback><dropTable /></rollback>`). UUID pré-assigné par convention F-219.

### Composants backend (pattern F-213 autonome)

- `InterruptionCarriereSoinsParentalAnalysis.java` — entité JPA
- `InterruptionCarriereSoinsParentalRepository.java`
- `InterruptionCarriereSoinsParentalRequest.java` — DTO POST
- `InterruptionCarriereSoinsParentalResult.java` — record verdict
- `InterruptionCarriereSoinsParentalResponse.java` — DTO GET
- `InterruptionCarriereSoinsParentalService.java`
- `InterruptionCarriereSoinsParentalCalculator.java` — fonction pure (logique métier)
- `InterruptionCarriereSoinsParentalController.java`
- Enums spécifiques au domaine métier (ex. verdict, type, statut)
- `ToolBranchRegistry` entry pour `interruption-carriere-soins-parental`
- `ToolUsageContributor` entry pour comptage

**Pattern F-213 backend autonome** (`feedback_f213_backend_pattern`) — **pas de modif** `TravailExtractedData` / `CritereCode` / `LegalDomainPromptBuilder` / `SYSTEM_PROMPT_TEMPLATEs`. Saisie avocat manuelle V1 sur tous les champs métier spécifiques (pré-fill IA à consolider en feature dédiée ultérieurement).

---

## Plan de test

### Unitaires (`InterruptionCarriereSoinsParentalCalculatorTest`)

- [ ] Cas nominal `ELIGIBLE` / `VALIDE` / `CONFORME` → verdict positif + base juridique.
- [ ] Cas négatif principal → verdict négatif + raison.
- [ ] Cas limite (seuils, durées max, conditions cumulatives) → verdict cohérent.
- [ ] Tous les codes raison du domaine couverts.

### Intégration (`InterruptionCarriereSoinsParentalControllerIT`)

- [ ] `POST` workspace BE → 200 + persistance.
- [ ] `POST` workspace FR → 404.
- [ ] `POST` `caseFileId` autre workspace → 404.
- [ ] `GET` après POST → 200.
- [ ] `GET` sans POST → 404.
- [ ] Validation Bean : inputs invalides → 400.

### Isolation workspace

- [x] Applicable — standard.

---

## Analyse d'impact

### Préoccupations transversales

- [x] **Workspace context** — gate `workspaceCountry=BELGIQUE` strict.
- [x] **Outil décisionnel métier** — création d'un outil, invariant un outil = une situation métier respecté.
- Auth / Plans / Navigation — non touchés.

### Composants impactés (pattern F-213 — minimal)

| Composant | Impact | Test de non-régression |
|---|---|---|
| `ToolBranchRegistry` | Ajout entry `interruption-carriere-soins-parental` | Tests existants |
| `ToolUsageContributor` | Ajout entry comptage | Tests existants |
| `DecisionToolVisibilityIntegrityIT` | Nouveau tool_id seedé par SF-219-32b | `KNOWN_FRONTEND_TOOL_IDS` à jour côté frontend |
| `DashboardTileToolIdIntegrityIT` | tool_id ajouté à `KNOWN_NO_DASHBOARD_TILE_IDS` côté SF-219-32b | Critique sinon master-red |

---

## Dépendances

- Aucune SF F-219 bloquante. Peut démarrer indépendamment.
- F-207 + F-213 déjà mergées — fournissent l'infrastructure BE Travail (panel, gate, pattern). Non bloquant techniquement.
