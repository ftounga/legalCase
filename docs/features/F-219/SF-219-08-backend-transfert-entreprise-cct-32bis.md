# Mini-spec — F-219 / SF-219-08-backend Outil transfert entreprise cct 32bis — checklist de conformité

## Identifiant

`F-219 / SF-219-08-backend`

## Feature parente

`F-219` — P3 Travail BE — ~32 outils BE-only spécificité

## Statut

`ready`

## Date de création

2026-05-27

## Branche Git

`feat/SF-219-08-backend-transfert-entreprise-cct-32bis`

---

## Objectif

Vérifier la conformité d'un **transfert d'entreprise CCT 32bis** (maintien des droits, info-consult préalable, reprise des contrats). **BELGIQUE UNIQUEMENT** — régime BE plus formaliste que L. 1224-1 FR.

---

## Source juridique BE

- CCT n°32bis du 07/06/1985 ; CCT n°32ter ; Directive 2001/23/CE
- Détails et conditions spécifiques à vérifier par avocat BE avant seed production.
- **Pattern F-213 backend autonome** appliqué : pas de modification transverse (`TravailExtractedData`, `CritereCode`, `LegalDomainPromptBuilder`, `SYSTEM_PROMPT_TEMPLATEs`). L'outil est totalement autonome.

---

## Comportement attendu

### Cas nominal

`POST /api/v1/case-files/{caseFileId}/decision-tools/transfert-entreprise-cct-32bis`

Inputs (body) — voir détail logique métier ci-dessous.

Logique (`TransfertEntrepriseCct32bisService` + `TransfertEntrepriseCct32bisChecker`) :

Vérifie : qualification du transfert (entité économique gardant son identité), info-consult préalable des représentants des travailleurs, maintien automatique des contrats individuels, maintien temporaire des conventions collectives, responsabilité solidaire 1 an cédant/cessionnaire.

Output (`TransfertEntrepriseCct32bisResponse`) :
```json
{
  "verdict": "<verdict métier>",
  "raison": "<code raison>",
  "synthese": "<phrase synthétique>",
  "baseJuridique": "CCT n°32bis du 07/06/1985",
  "avertissement": "<si applicable>"
}
```

Persistance : table `transfert_entreprise_cct_32bis_analyses` — 1 ligne par dossier (unique sur `case_file_id`, mise à jour à chaque POST). Inputs + verdict persistés en JSON (`result_data`).

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
- [ ] `DashboardTileToolIdIntegrityIT` reste vert (tool_id ajouté à `KNOWN_NO_DASHBOARD_TILE_IDS` en SF-219-08b).
- [ ] Migration Liquibase reversible.

---

## Périmètre

### Hors scope

- Frontend (`transfert-entreprise-cct-32bis-section.component`) — SF-219-08b.
- Autres outils F-219.

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/decision-tools/transfert-entreprise-cct-32bis` | OIDC | LAWYER |
| GET  | `/api/v1/case-files/{caseFileId}/decision-tools/transfert-entreprise-cct-32bis` | OIDC | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|---|---|---|
| `transfert_entreprise_cct_32bis_analyses` | INSERT / UPDATE / SELECT | Unique sur `case_file_id`. Colonnes : `id` UUID, `case_file_id` FK CASCADE, `result_data` TEXT JSON, `created_at`, `updated_at`. |

### Migration Liquibase

`XXX-create-transfert_entreprise_cct_32bis-analyses.xml` — reversible (`<rollback><dropTable /></rollback>`). UUID pré-assigné par convention F-219.

### Composants backend (pattern F-213 autonome)

- `TransfertEntrepriseCct32bisAnalysis.java` — entité JPA
- `TransfertEntrepriseCct32bisRepository.java`
- `TransfertEntrepriseCct32bisRequest.java` — DTO POST
- `TransfertEntrepriseCct32bisResult.java` — record verdict
- `TransfertEntrepriseCct32bisResponse.java` — DTO GET
- `TransfertEntrepriseCct32bisService.java`
- `TransfertEntrepriseCct32bisChecker.java` — fonction pure (logique métier)
- `TransfertEntrepriseCct32bisController.java`
- Enums spécifiques au domaine métier (ex. verdict, type, statut)
- `ToolBranchRegistry` entry pour `transfert-entreprise-cct-32bis`
- `ToolUsageContributor` entry pour comptage

**Pattern F-213 backend autonome** (`feedback_f213_backend_pattern`) — **pas de modif** `TravailExtractedData` / `CritereCode` / `LegalDomainPromptBuilder` / `SYSTEM_PROMPT_TEMPLATEs`. Saisie avocat manuelle V1 sur tous les champs métier spécifiques (pré-fill IA à consolider en feature dédiée ultérieurement).

---

## Plan de test

### Unitaires (`TransfertEntrepriseCct32bisCheckerTest`)

- [ ] Cas nominal `ELIGIBLE` / `VALIDE` / `CONFORME` → verdict positif + base juridique.
- [ ] Cas négatif principal → verdict négatif + raison.
- [ ] Cas limite (seuils, durées max, conditions cumulatives) → verdict cohérent.
- [ ] Tous les codes raison du domaine couverts.

### Intégration (`TransfertEntrepriseCct32bisControllerIT`)

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
| `ToolBranchRegistry` | Ajout entry `transfert-entreprise-cct-32bis` | Tests existants |
| `ToolUsageContributor` | Ajout entry comptage | Tests existants |
| `DecisionToolVisibilityIntegrityIT` | Nouveau tool_id seedé par SF-219-08b | `KNOWN_FRONTEND_TOOL_IDS` à jour côté frontend |
| `DashboardTileToolIdIntegrityIT` | tool_id ajouté à `KNOWN_NO_DASHBOARD_TILE_IDS` côté SF-219-08b | Critique sinon master-red |

---

## Dépendances

- Aucune SF F-219 bloquante. Peut démarrer indépendamment.
- F-207 + F-213 déjà mergées — fournissent l'infrastructure BE Travail (panel, gate, pattern). Non bloquant techniquement.
