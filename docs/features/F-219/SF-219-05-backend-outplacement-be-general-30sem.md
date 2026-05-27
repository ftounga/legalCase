# Mini-spec — F-219 / SF-219-05-backend Outil outplacement BE general 30sem — checklist de conformité

## Identifiant

`F-219 / SF-219-05-backend`

## Feature parente

`F-219` — P3 Travail BE — ~32 outils BE-only spécificité

## Statut

`ready`

## Date de création

2026-05-27

## Branche Git

`feat/SF-219-05-backend-outplacement-be-general-30sem`

---

## Objectif

Vérifier la conformité de l'**outplacement général régime préavis ≥ 30 semaines** (procédure, durée, coût, sanctions). **BELGIQUE UNIQUEMENT** — distinct de l'outplacement obligatoire 45+ ans (F-207 SF-207-08) qui s'applique quelle que soit la durée du préavis.

---

## Source juridique BE

- Loi du 05/09/2001 art. 11 ; AR du 21/10/2007
- Détails et conditions spécifiques à vérifier par avocat BE avant seed production.
- **Pattern F-213 backend autonome** appliqué : pas de modification transverse (`TravailExtractedData`, `CritereCode`, `LegalDomainPromptBuilder`, `SYSTEM_PROMPT_TEMPLATEs`). L'outil est totalement autonome.

---

## Comportement attendu

### Cas nominal

`POST /api/v1/case-files/{caseFileId}/decision-tools/outplacement-be-general-30sem`

Inputs (body) — voir détail logique métier ci-dessous.

Logique (`OutplacementBeGeneral30semService` + `OutplacementBeGeneral30semChecker`) :

Conditions : préavis ≥ 30 semaines (ou indemnité compensatoire équivalente) ; offre d'outplacement obligatoire dans les 15 jours ; durée 60h sur 12 mois ; sanction = perte allocations chômage pour le travailleur, amende employeur.

Output (`OutplacementBeGeneral30semResponse`) :
```json
{
  "verdict": "<verdict métier>",
  "raison": "<code raison>",
  "synthese": "<phrase synthétique>",
  "baseJuridique": "Loi du 05/09/2001 art. 11",
  "avertissement": "<si applicable>"
}
```

Persistance : table `outplacement_be_general_30sem_analyses` — 1 ligne par dossier (unique sur `case_file_id`, mise à jour à chaque POST). Inputs + verdict persistés en JSON (`result_data`).

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
- [ ] `DashboardTileToolIdIntegrityIT` reste vert (tool_id ajouté à `KNOWN_NO_DASHBOARD_TILE_IDS` en SF-219-05b).
- [ ] Migration Liquibase reversible.

---

## Périmètre

### Hors scope

- Frontend (`outplacement-be-general-30sem-section.component`) — SF-219-05b.
- Autres outils F-219.

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/decision-tools/outplacement-be-general-30sem` | OIDC | LAWYER |
| GET  | `/api/v1/case-files/{caseFileId}/decision-tools/outplacement-be-general-30sem` | OIDC | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|---|---|---|
| `outplacement_be_general_30sem_analyses` | INSERT / UPDATE / SELECT | Unique sur `case_file_id`. Colonnes : `id` UUID, `case_file_id` FK CASCADE, `result_data` TEXT JSON, `created_at`, `updated_at`. |

### Migration Liquibase

`XXX-create-outplacement_be_general_30sem-analyses.xml` — reversible (`<rollback><dropTable /></rollback>`). UUID pré-assigné par convention F-219.

### Composants backend (pattern F-213 autonome)

- `OutplacementBeGeneral30semAnalysis.java` — entité JPA
- `OutplacementBeGeneral30semRepository.java`
- `OutplacementBeGeneral30semRequest.java` — DTO POST
- `OutplacementBeGeneral30semResult.java` — record verdict
- `OutplacementBeGeneral30semResponse.java` — DTO GET
- `OutplacementBeGeneral30semService.java`
- `OutplacementBeGeneral30semChecker.java` — fonction pure (logique métier)
- `OutplacementBeGeneral30semController.java`
- Enums spécifiques au domaine métier (ex. verdict, type, statut)
- `ToolBranchRegistry` entry pour `outplacement-be-general-30sem`
- `ToolUsageContributor` entry pour comptage

**Pattern F-213 backend autonome** (`feedback_f213_backend_pattern`) — **pas de modif** `TravailExtractedData` / `CritereCode` / `LegalDomainPromptBuilder` / `SYSTEM_PROMPT_TEMPLATEs`. Saisie avocat manuelle V1 sur tous les champs métier spécifiques (pré-fill IA à consolider en feature dédiée ultérieurement).

---

## Plan de test

### Unitaires (`OutplacementBeGeneral30semCheckerTest`)

- [ ] Cas nominal `ELIGIBLE` / `VALIDE` / `CONFORME` → verdict positif + base juridique.
- [ ] Cas négatif principal → verdict négatif + raison.
- [ ] Cas limite (seuils, durées max, conditions cumulatives) → verdict cohérent.
- [ ] Tous les codes raison du domaine couverts.

### Intégration (`OutplacementBeGeneral30semControllerIT`)

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
| `ToolBranchRegistry` | Ajout entry `outplacement-be-general-30sem` | Tests existants |
| `ToolUsageContributor` | Ajout entry comptage | Tests existants |
| `DecisionToolVisibilityIntegrityIT` | Nouveau tool_id seedé par SF-219-05b | `KNOWN_FRONTEND_TOOL_IDS` à jour côté frontend |
| `DashboardTileToolIdIntegrityIT` | tool_id ajouté à `KNOWN_NO_DASHBOARD_TILE_IDS` côté SF-219-05b | Critique sinon master-red |

---

## Dépendances

- Aucune SF F-219 bloquante. Peut démarrer indépendamment.
- F-207 + F-213 déjà mergées — fournissent l'infrastructure BE Travail (panel, gate, pattern). Non bloquant techniquement.
