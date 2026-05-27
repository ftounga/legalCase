# Mini-spec — F-219 / SF-219-13-backend Outil etudiant jobiste be — calculateur quota + checklist

## Identifiant

`F-219 / SF-219-13-backend`

## Feature parente

`F-219` — P3 Travail BE — ~32 outils BE-only spécificité

## Statut

`ready`

## Date de création

2026-05-27

## Branche Git

`feat/SF-219-13-backend-etudiant-jobiste-be`

---

## Objectif

Calculer le **quota 600h/an étudiant jobiste** + vérifier la conformité du contrat (ONSS réduit, Dimona spécifique). **BELGIQUE UNIQUEMENT** — aucun équivalent FR direct.

---

## Source juridique BE

- Loi du 03/07/1978 art. 120bis ; Loi du 30/04/1999 (charte étudiant)
- Détails et conditions spécifiques à vérifier par avocat BE avant seed production.
- **Pattern F-213 backend autonome** appliqué : pas de modification transverse (`TravailExtractedData`, `CritereCode`, `LegalDomainPromptBuilder`, `SYSTEM_PROMPT_TEMPLATEs`). L'outil est totalement autonome.

---

## Comportement attendu

### Cas nominal

`POST /api/v1/case-files/{caseFileId}/decision-tools/etudiant-jobiste-be`

Inputs (body) — voir détail logique métier ci-dessous.

Logique (`EtudiantJobisteBeService` + `EtudiantJobisteBeCalculator`) :

Calcule heures restantes du quota 600h/an, vérifie : conditions d'étudiant (max 12 mois pause études), contrat occupation étudiant écrit, Dimona type STU, taux ONSS réduit (5,42 % employeur + 2,71 % travailleur). Dépassement = cotisations normales rétroactives.

Output (`EtudiantJobisteBeResponse`) :
```json
{
  "verdict": "<verdict métier>",
  "raison": "<code raison>",
  "synthese": "<phrase synthétique>",
  "baseJuridique": "Loi du 03/07/1978 art. 120bis",
  "avertissement": "<si applicable>"
}
```

Persistance : table `etudiant_jobiste_be_analyses` — 1 ligne par dossier (unique sur `case_file_id`, mise à jour à chaque POST). Inputs + verdict persistés en JSON (`result_data`).

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
- [ ] `DashboardTileToolIdIntegrityIT` reste vert (tool_id ajouté à `KNOWN_NO_DASHBOARD_TILE_IDS` en SF-219-13b).
- [ ] Migration Liquibase reversible.

---

## Périmètre

### Hors scope

- Frontend (`etudiant-jobiste-be-section.component`) — SF-219-13b.
- Autres outils F-219.

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/decision-tools/etudiant-jobiste-be` | OIDC | LAWYER |
| GET  | `/api/v1/case-files/{caseFileId}/decision-tools/etudiant-jobiste-be` | OIDC | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|---|---|---|
| `etudiant_jobiste_be_analyses` | INSERT / UPDATE / SELECT | Unique sur `case_file_id`. Colonnes : `id` UUID, `case_file_id` FK CASCADE, `result_data` TEXT JSON, `created_at`, `updated_at`. |

### Migration Liquibase

`XXX-create-etudiant_jobiste_be-analyses.xml` — reversible (`<rollback><dropTable /></rollback>`). UUID pré-assigné par convention F-219.

### Composants backend (pattern F-213 autonome)

- `EtudiantJobisteBeAnalysis.java` — entité JPA
- `EtudiantJobisteBeRepository.java`
- `EtudiantJobisteBeRequest.java` — DTO POST
- `EtudiantJobisteBeResult.java` — record verdict
- `EtudiantJobisteBeResponse.java` — DTO GET
- `EtudiantJobisteBeService.java`
- `EtudiantJobisteBeCalculator.java` — fonction pure (logique métier)
- `EtudiantJobisteBeController.java`
- Enums spécifiques au domaine métier (ex. verdict, type, statut)
- `ToolBranchRegistry` entry pour `etudiant-jobiste-be`
- `ToolUsageContributor` entry pour comptage

**Pattern F-213 backend autonome** (`feedback_f213_backend_pattern`) — **pas de modif** `TravailExtractedData` / `CritereCode` / `LegalDomainPromptBuilder` / `SYSTEM_PROMPT_TEMPLATEs`. Saisie avocat manuelle V1 sur tous les champs métier spécifiques (pré-fill IA à consolider en feature dédiée ultérieurement).

---

## Plan de test

### Unitaires (`EtudiantJobisteBeCalculatorTest`)

- [ ] Cas nominal `ELIGIBLE` / `VALIDE` / `CONFORME` → verdict positif + base juridique.
- [ ] Cas négatif principal → verdict négatif + raison.
- [ ] Cas limite (seuils, durées max, conditions cumulatives) → verdict cohérent.
- [ ] Tous les codes raison du domaine couverts.

### Intégration (`EtudiantJobisteBeControllerIT`)

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
| `ToolBranchRegistry` | Ajout entry `etudiant-jobiste-be` | Tests existants |
| `ToolUsageContributor` | Ajout entry comptage | Tests existants |
| `DecisionToolVisibilityIntegrityIT` | Nouveau tool_id seedé par SF-219-13b | `KNOWN_FRONTEND_TOOL_IDS` à jour côté frontend |
| `DashboardTileToolIdIntegrityIT` | tool_id ajouté à `KNOWN_NO_DASHBOARD_TILE_IDS` côté SF-219-13b | Critique sinon master-red |

---

## Dépendances

- Aucune SF F-219 bloquante. Peut démarrer indépendamment.
- F-207 + F-213 déjà mergées — fournissent l'infrastructure BE Travail (panel, gate, pattern). Non bloquant techniquement.
