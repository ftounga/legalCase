# Mini-spec — F-55 / SF-55-01 — Backend multi-domaines et pays

> Ce document doit être validé AVANT de démarrer le dev.

---

## Identifiant

`F-55 / SF-55-01`

## Feature parente

`F-55` — Multi-domaines et pays workspace

## Statut

`draft`

## Date de création

2026-03-26

## Branche Git

`feat/SF-55-01-backend-multi-domaines-pays`

---

## Objectif

Ajouter le champ `country` (FRANCE / BELGIQUE) au workspace, activer les domaines DROIT_IMMIGRATION et DROIT_FAMILLE (DROIT_IMMOBILIER retiré), et rendre les system prompts IA dynamiques selon le domaine et le pays.

---

## Comportement attendu

### Migration schéma

- Ajout colonne `country VARCHAR(10) NOT NULL DEFAULT 'FRANCE'` sur `workspaces`
- Backfill : tous les workspaces existants → `country = 'FRANCE'`

### Endpoint `POST /api/v1/workspaces`

- Champ `country` ajouté au `CreateWorkspaceRequest` (obligatoire, valeurs : `FRANCE` | `BELGIQUE`)
- Champ `legalDomain` : valeurs acceptées désormais `DROIT_DU_TRAVAIL` | `DROIT_IMMIGRATION` | `DROIT_FAMILLE` — `DROIT_IMMOBILIER` rejeté avec 400
- `WorkspaceResponse` enrichi : expose `legalDomain` et `country`

### Endpoint `GET /api/v1/workspaces/current` et `GET /api/v1/workspaces`

- `WorkspaceResponse` expose désormais `legalDomain` et `country`

### `createDefaultWorkspace()`

- Défaut : `legalDomain = "DROIT_DU_TRAVAIL"`, `country = "FRANCE"`

### System prompts dynamiques

Les 6 system prompts (AnthropicService, ChunkAnalysisService, DocumentAnalysisService, CaseAnalysisService, EnrichedAnalysisService, AiQuestionService) deviennent dynamiques.

| Domaine | France | Belgique |
|---------|--------|----------|
| DROIT_DU_TRAVAIL | droit du travail français | droit du travail belge |
| DROIT_IMMIGRATION | droit de l'immigration française | droit de l'immigration belge |
| DROIT_FAMILLE | droit de la famille français | droit de la famille belge |

Le workspace (et donc son `legalDomain` + `country`) est passé en contexte à chaque appel IA. Les services IA reçoivent ces deux paramètres pour construire le prompt dynamiquement.

### Cas d'erreur

| Situation | Code HTTP |
|-----------|-----------|
| `legalDomain` invalide (ex: DROIT_IMMOBILIER) | 400 |
| `country` invalide (hors FRANCE/BELGIQUE) | 400 |
| `country` absent | 400 |

---

## Critères d'acceptation

- [ ] `POST /workspaces` avec `country=BELGIQUE` → workspace créé avec `country=BELGIQUE`
- [ ] `POST /workspaces` avec `legalDomain=DROIT_FAMILLE` → workspace créé avec ce domaine
- [ ] `POST /workspaces` avec `legalDomain=DROIT_IMMOBILIER` → 400
- [ ] `POST /workspaces` sans `country` → 400
- [ ] `GET /workspaces/current` → réponse inclut `legalDomain` et `country`
- [ ] Workspaces existants en DB → `country = 'FRANCE'` après migration
- [ ] Analyse d'un dossier workspace `DROIT_IMMIGRATION` + `BELGIQUE` → prompt contient "droit de l'immigration belge"
- [ ] Analyse d'un dossier workspace `DROIT_FAMILLE` + `FRANCE` → prompt contient "droit de la famille français"

---

## Périmètre

### Hors scope

- UI (SF-55-02)
- Changement de pays/domaine d'un workspace existant (pas d'endpoint PATCH)
- Contenu IA spécifique par pays (jurisprudence, articles de loi) — le prompt adapte le référentiel, pas le contenu détaillé

---

## Technique

### Migration Liquibase

`029-add-country-to-workspaces.xml`
- ADD COLUMN `country VARCHAR(10) NOT NULL DEFAULT 'FRANCE'`
- UPDATE backfill : `UPDATE workspaces SET country = 'FRANCE'`

### Composants modifiés

| Composant | Modification |
|-----------|-------------|
| `Workspace` | Ajout champ `country` |
| `CreateWorkspaceRequest` | Ajout `country` + validation |
| `WorkspaceResponse` | Ajout `legalDomain` + `country` |
| `WorkspaceService` | Passer `country` à la création, default dans `createDefaultWorkspace` |
| `AnthropicService` | `SYSTEM_PROMPT` devient méthode `buildSystemPrompt(domain, country)` |
| `ChunkAnalysisService` | Idem |
| `DocumentAnalysisService` | Idem |
| `CaseAnalysisService` | Idem — reçoit le workspace en contexte |
| `EnrichedAnalysisService` | Idem |
| `AiQuestionService` | Idem |
| `CaseAnalysisService` | Résout le workspace via `caseFile.getWorkspace()` pour passer domain+country |

### Tables impactées

- `workspaces` : ajout colonne `country`

---

## Plan de test

### Tests unitaires

- [ ] `buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE")` → contient "droit du travail français"
- [ ] `buildSystemPrompt("DROIT_DU_TRAVAIL", "BELGIQUE")` → contient "droit du travail belge"
- [ ] `buildSystemPrompt("DROIT_IMMIGRATION", "FRANCE")` → contient "droit de l'immigration française"
- [ ] `buildSystemPrompt("DROIT_FAMILLE", "BELGIQUE")` → contient "droit de la famille belge"

### Tests d'intégration — `WorkspaceControllerIT`

- [ ] I-01 POST /workspaces avec country=BELGIQUE → 201, country=BELGIQUE dans réponse
- [ ] I-02 POST /workspaces avec legalDomain=DROIT_FAMILLE → 201, legalDomain dans réponse
- [ ] I-03 POST /workspaces avec legalDomain=DROIT_IMMOBILIER → 400
- [ ] I-04 POST /workspaces sans country → 400
- [ ] I-05 GET /workspaces/current → legalDomain et country présents

### Isolation workspace

- Non applicable (modification de la création, pas d'accès cross-workspace)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] **Workspace context** — `WorkspaceResponse` enrichi : vérifier que tous les consommateurs du modèle Angular/Java restent compatibles (ajout de champs optionnels)
- [ ] **Plans / limites** — non impacté

### Composants existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|-----------|--------|----------------------|
| `WorkspaceControllerIT` | Ajout country dans les requêtes de création | Tests existants mis à jour |
| Tests IT créant des workspaces | `country` devient obligatoire dans le body | Mise à jour des fixtures |

---

## Dépendances

- Aucune subfeature bloquante
