# Mini-spec — F-219 / SF-219-21-backend Outil eco cheques cheques repas be — calculateur d'exonération

## Identifiant

`F-219 / SF-219-21-backend`

## Feature parente

`F-219` — P3 Travail BE — ~32 outils BE-only spécificité

## Statut

`ready`

## Date de création

2026-05-27

## Branche Git

`feat/SF-219-21-backend-eco-cheques-cheques-repas-be`

---

## Objectif

Calculer les **éco-chèques + chèques-repas BE** (montants exonérés ONSS, conditions). **BELGIQUE UNIQUEMENT** — avantages extra-légaux fréquents.

---

## Source juridique BE

- AR du 14/01/2013 (éco-chèques) ; AR du 12/10/2010 (chèques-repas)
- Détails et conditions spécifiques à vérifier par avocat BE avant seed production.
- **Pattern F-213 backend autonome** appliqué : pas de modification transverse (`TravailExtractedData`, `CritereCode`, `LegalDomainPromptBuilder`, `SYSTEM_PROMPT_TEMPLATEs`). L'outil est totalement autonome.

---

## Comportement attendu

### Cas nominal

`POST /api/v1/case-files/{caseFileId}/decision-tools/eco-cheques-cheques-repas-be`

Inputs (body) — voir détail logique métier ci-dessous.

Logique (`EcoChequesChequesRepasBeService` + `EcoChequesChequesRepasBeCalculator`) :

Calcule : éco-chèques max 250 €/an exonérés ONSS, chèques-repas max 8 €/jour ouvré (6,91 € exonérés + 1,09 € employeur min), conditions cumulatives (jour effectivement presté, pas double avec frais de bouche), sanction = requalification en rémunération + cotisations rétroactives.

Output (`EcoChequesChequesRepasBeResponse`) :
```json
{
  "verdict": "<verdict métier>",
  "raison": "<code raison>",
  "synthese": "<phrase synthétique>",
  "baseJuridique": "AR du 14/01/2013 (éco-chèques)",
  "avertissement": "<si applicable>"
}
```

Persistance : table `eco_cheques_cheques_repas_be_analyses` — 1 ligne par dossier (unique sur `case_file_id`, mise à jour à chaque POST). Inputs + verdict persistés en JSON (`result_data`).

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
- [ ] `DashboardTileToolIdIntegrityIT` reste vert (tool_id ajouté à `KNOWN_NO_DASHBOARD_TILE_IDS` en SF-219-21b).
- [ ] Migration Liquibase reversible.

---

## Périmètre

### Hors scope

- Frontend (`eco-cheques-cheques-repas-be-section.component`) — SF-219-21b.
- Autres outils F-219.

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/decision-tools/eco-cheques-cheques-repas-be` | OIDC | LAWYER |
| GET  | `/api/v1/case-files/{caseFileId}/decision-tools/eco-cheques-cheques-repas-be` | OIDC | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|---|---|---|
| `eco_cheques_cheques_repas_be_analyses` | INSERT / UPDATE / SELECT | Unique sur `case_file_id`. Colonnes : `id` UUID, `case_file_id` FK CASCADE, `result_data` TEXT JSON, `created_at`, `updated_at`. |

### Migration Liquibase

`XXX-create-eco_cheques_cheques_repas_be-analyses.xml` — reversible (`<rollback><dropTable /></rollback>`). UUID pré-assigné par convention F-219.

### Composants backend (pattern F-213 autonome)

- `EcoChequesChequesRepasBeAnalysis.java` — entité JPA
- `EcoChequesChequesRepasBeRepository.java`
- `EcoChequesChequesRepasBeRequest.java` — DTO POST
- `EcoChequesChequesRepasBeResult.java` — record verdict
- `EcoChequesChequesRepasBeResponse.java` — DTO GET
- `EcoChequesChequesRepasBeService.java`
- `EcoChequesChequesRepasBeCalculator.java` — fonction pure (logique métier)
- `EcoChequesChequesRepasBeController.java`
- Enums spécifiques au domaine métier (ex. verdict, type, statut)
- `ToolBranchRegistry` entry pour `eco-cheques-cheques-repas-be`
- `ToolUsageContributor` entry pour comptage

**Pattern F-213 backend autonome** (`feedback_f213_backend_pattern`) — **pas de modif** `TravailExtractedData` / `CritereCode` / `LegalDomainPromptBuilder` / `SYSTEM_PROMPT_TEMPLATEs`. Saisie avocat manuelle V1 sur tous les champs métier spécifiques (pré-fill IA à consolider en feature dédiée ultérieurement).

---

## Plan de test

### Unitaires (`EcoChequesChequesRepasBeCalculatorTest`)

- [ ] Cas nominal `ELIGIBLE` / `VALIDE` / `CONFORME` → verdict positif + base juridique.
- [ ] Cas négatif principal → verdict négatif + raison.
- [ ] Cas limite (seuils, durées max, conditions cumulatives) → verdict cohérent.
- [ ] Tous les codes raison du domaine couverts.

### Intégration (`EcoChequesChequesRepasBeControllerIT`)

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
| `ToolBranchRegistry` | Ajout entry `eco-cheques-cheques-repas-be` | Tests existants |
| `ToolUsageContributor` | Ajout entry comptage | Tests existants |
| `DecisionToolVisibilityIntegrityIT` | Nouveau tool_id seedé par SF-219-21b | `KNOWN_FRONTEND_TOOL_IDS` à jour côté frontend |
| `DashboardTileToolIdIntegrityIT` | tool_id ajouté à `KNOWN_NO_DASHBOARD_TILE_IDS` côté SF-219-21b | Critique sinon master-red |

---

## Dépendances

- Aucune SF F-219 bloquante. Peut démarrer indépendamment.
- F-207 + F-213 déjà mergées — fournissent l'infrastructure BE Travail (panel, gate, pattern). Non bloquant techniquement.
