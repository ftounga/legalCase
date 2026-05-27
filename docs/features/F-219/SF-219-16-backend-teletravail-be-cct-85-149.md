# Mini-spec — F-219 / SF-219-16-backend Outil teletravail BE cct 85 149 — checklist de conformité

## Identifiant

`F-219 / SF-219-16-backend`

## Feature parente

`F-219` — P3 Travail BE — ~32 outils BE-only spécificité

## Statut

`ready`

## Date de création

2026-05-27

## Branche Git

`feat/SF-219-16-backend-teletravail-be-cct-85-149`

---

## Objectif

Vérifier la conformité d'un **accord de télétravail BE** (CCT 85 structurel ou CCT 149 occasionnel), calculer l'indemnité forfaitaire frais. **BELGIQUE UNIQUEMENT** — distinct de l'accord télétravail FR.

---

## Source juridique BE

- CCT n°85 (télétravail structurel) ; CCT n°149 (télétravail occasionnel)
- Détails et conditions spécifiques à vérifier par avocat BE avant seed production.
- **Pattern F-213 backend autonome** appliqué : pas de modification transverse (`TravailExtractedData`, `CritereCode`, `LegalDomainPromptBuilder`, `SYSTEM_PROMPT_TEMPLATEs`). L'outil est totalement autonome.

---

## Comportement attendu

### Cas nominal

`POST /api/v1/case-files/{caseFileId}/decision-tools/teletravail-be-cct-85-149`

Inputs (body) — voir détail logique métier ci-dessous.

Logique (`TeletravailBeCct85149Service` + `TeletravailBeCct85149Checker`) :

Vérifie : type télétravail (structurel régulier CCT 85 / occasionnel CCT 149), accord écrit obligatoire, indemnité forfaitaire frais (max 154,74 €/mois 2024 — internet/électricité/ergonomie), fourniture matériel, droit déconnexion (lien outil 19).

Output (`TeletravailBeCct85149Response`) :
```json
{
  "verdict": "<verdict métier>",
  "raison": "<code raison>",
  "synthese": "<phrase synthétique>",
  "baseJuridique": "CCT n°85 (télétravail structurel)",
  "avertissement": "<si applicable>"
}
```

Persistance : table `teletravail_be_cct_85_149_analyses` — 1 ligne par dossier (unique sur `case_file_id`, mise à jour à chaque POST). Inputs + verdict persistés en JSON (`result_data`).

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
- [ ] `DashboardTileToolIdIntegrityIT` reste vert (tool_id ajouté à `KNOWN_NO_DASHBOARD_TILE_IDS` en SF-219-16b).
- [ ] Migration Liquibase reversible.

---

## Périmètre

### Hors scope

- Frontend (`teletravail-be-cct-85-149-section.component`) — SF-219-16b.
- Autres outils F-219.

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/decision-tools/teletravail-be-cct-85-149` | OIDC | LAWYER |
| GET  | `/api/v1/case-files/{caseFileId}/decision-tools/teletravail-be-cct-85-149` | OIDC | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|---|---|---|
| `teletravail_be_cct_85_149_analyses` | INSERT / UPDATE / SELECT | Unique sur `case_file_id`. Colonnes : `id` UUID, `case_file_id` FK CASCADE, `result_data` TEXT JSON, `created_at`, `updated_at`. |

### Migration Liquibase

`XXX-create-teletravail_be_cct_85_149-analyses.xml` — reversible (`<rollback><dropTable /></rollback>`). UUID pré-assigné par convention F-219.

### Composants backend (pattern F-213 autonome)

- `TeletravailBeCct85149Analysis.java` — entité JPA
- `TeletravailBeCct85149Repository.java`
- `TeletravailBeCct85149Request.java` — DTO POST
- `TeletravailBeCct85149Result.java` — record verdict
- `TeletravailBeCct85149Response.java` — DTO GET
- `TeletravailBeCct85149Service.java`
- `TeletravailBeCct85149Checker.java` — fonction pure (logique métier)
- `TeletravailBeCct85149Controller.java`
- Enums spécifiques au domaine métier (ex. verdict, type, statut)
- `ToolBranchRegistry` entry pour `teletravail-be-cct-85-149`
- `ToolUsageContributor` entry pour comptage

**Pattern F-213 backend autonome** (`feedback_f213_backend_pattern`) — **pas de modif** `TravailExtractedData` / `CritereCode` / `LegalDomainPromptBuilder` / `SYSTEM_PROMPT_TEMPLATEs`. Saisie avocat manuelle V1 sur tous les champs métier spécifiques (pré-fill IA à consolider en feature dédiée ultérieurement).

---

## Plan de test

### Unitaires (`TeletravailBeCct85149CheckerTest`)

- [ ] Cas nominal `ELIGIBLE` / `VALIDE` / `CONFORME` → verdict positif + base juridique.
- [ ] Cas négatif principal → verdict négatif + raison.
- [ ] Cas limite (seuils, durées max, conditions cumulatives) → verdict cohérent.
- [ ] Tous les codes raison du domaine couverts.

### Intégration (`TeletravailBeCct85149ControllerIT`)

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
| `ToolBranchRegistry` | Ajout entry `teletravail-be-cct-85-149` | Tests existants |
| `ToolUsageContributor` | Ajout entry comptage | Tests existants |
| `DecisionToolVisibilityIntegrityIT` | Nouveau tool_id seedé par SF-219-16b | `KNOWN_FRONTEND_TOOL_IDS` à jour côté frontend |
| `DashboardTileToolIdIntegrityIT` | tool_id ajouté à `KNOWN_NO_DASHBOARD_TILE_IDS` côté SF-219-16b | Critique sinon master-red |

---

## Dépendances

- Aucune SF F-219 bloquante. Peut démarrer indépendamment.
- F-207 + F-213 déjà mergées — fournissent l'infrastructure BE Travail (panel, gate, pattern). Non bloquant techniquement.
