# Mini-spec — F-27 / SF-27-01 — Domaine juridique workspace (backend)

## Identifiant

`F-27 / SF-27-01`

## Feature parente

`F-27` — Domaine juridique du workspace

## Statut

`ready`

## Date de création

2026-03-21

## Branche Git

`feat/SF-27-01-legal-domain-backend`

---

## Objectif

Renommer la constante `EMPLOYMENT_LAW` → `DROIT_DU_TRAVAIL` dans tout le backend, ajouter un champ `legal_domain` sur la table `workspaces`, et faire en sorte que les case files héritent du domaine de leur workspace.

---

## Comportement attendu

### Cas nominal

1. L'endpoint `POST /api/v1/workspaces` accepte un nouveau champ `legalDomain` (obligatoire).
2. Le workspace est créé avec ce domaine.
3. Lors de la création d'un case file (`POST /api/v1/case-files`), le `legalDomain` est automatiquement résolu depuis le workspace — il n'est plus fourni par le client.
4. `CaseFileService` valide que `workspace.legalDomain == 'DROIT_DU_TRAVAIL'` (seul domaine supporté en V1).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `legalDomain` absent à la création du workspace | 400 Bad Request | 400 |
| `legalDomain` avec valeur non supportée | 400 Bad Request | 400 |
| Case file créé dans un workspace sans domaine (legacy) | Fallback `DROIT_DU_TRAVAIL` | — |

---

## Critères d'acceptation

- [ ] La constante `EMPLOYMENT_LAW` n'apparaît plus dans le code backend (remplacée par `DROIT_DU_TRAVAIL`)
- [ ] La table `workspaces` possède une colonne `legal_domain VARCHAR(50) NOT NULL DEFAULT 'DROIT_DU_TRAVAIL'`
- [ ] Les workspaces existants ont `legal_domain = 'DROIT_DU_TRAVAIL'` (migration UPDATE)
- [ ] `POST /api/v1/workspaces` accepte `legalDomain` et le persiste
- [ ] La création d'un case file n'attend plus `legalDomain` du client — il est résolu depuis le workspace
- [ ] `CaseFileService` lit `workspace.legalDomain` pour la validation V1
- [ ] Tous les tests backend passent avec `DROIT_DU_TRAVAIL`

---

## Périmètre

### Hors scope (explicite)

- Implémentation de DROIT_IMMIGRATION ou DROIT_IMMOBILIER (backlog F-20 / F-21)
- Changement de domaine après création du workspace
- Frontend (SF-27-02)

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Valeurs autorisées | Notes |
|-------|-------------|-------------|-------------------|-------|
| `legalDomain` (workspace) | Oui | 50 | `DROIT_DU_TRAVAIL`, `DROIT_IMMIGRATION`, `DROIT_IMMOBILIER` | Seul `DROIT_DU_TRAVAIL` est opérationnel en V1 |

---

## Technique

### Endpoints impactés

| Méthode | URL | Changement |
|---------|-----|-----------|
| POST | `/api/v1/workspaces` | Ajout champ `legalDomain` dans le body |
| POST | `/api/v1/case-files` | Suppression champ `legalDomain` du body client — résolu depuis workspace |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `workspaces` | ALTER + UPDATE | Ajout colonne `legal_domain`, migration des données existantes |
| `case_files` | Aucun changement structurel | Valeur déjà stockée, migration data `EMPLOYMENT_LAW` → `DROIT_DU_TRAVAIL` |

### Migration Liquibase

- [x] Oui — deux changesets :
  1. `ALTER TABLE workspaces ADD COLUMN legal_domain VARCHAR(50) NOT NULL DEFAULT 'DROIT_DU_TRAVAIL'`
  2. `UPDATE case_files SET legal_domain = 'DROIT_DU_TRAVAIL' WHERE legal_domain = 'EMPLOYMENT_LAW'`

---

## Plan de test

### Tests unitaires

- [ ] `CaseFileServiceTest` — mise à jour de la constante + validation via workspace.legalDomain

### Tests d'intégration

- [ ] `POST /api/v1/workspaces` avec `legalDomain: DROIT_DU_TRAVAIL` → 201
- [ ] `POST /api/v1/workspaces` sans `legalDomain` → 400
- [ ] `POST /api/v1/case-files` sans `legalDomain` dans le body → 201 (résolu depuis workspace)
- [ ] Tous les ITs existants mis à jour : remplacer `EMPLOYMENT_LAW` → `DROIT_DU_TRAVAIL` dans les fixtures

### Isolation workspace

- [ ] Applicable — la résolution du domaine depuis le workspace respecte le filtre `workspace_id`

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] **Auth / Principal** — non
- [x] **Workspace context** — touche la création du workspace et la résolution du domaine dans CaseFileService
- [ ] **Plans / limites** — non
- [ ] **Navigation / routing frontend** — non

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `CaseFileService` | Lit désormais `workspace.legalDomain` au lieu d'une constante statique | IT CaseFileControllerIT mis à jour |
| `WorkspaceService.createWorkspace()` | Accepte un nouveau paramètre `legalDomain` | IT WorkspaceControllerIT |
| Tous les ITs backend | Référencent `EMPLOYMENT_LAW` dans les fixtures | Recherche/remplacement global |

### Smoke tests E2E concernés

- [x] `e2e/smoke/happy-path.spec.ts` — `login → créer dossier → vérifier présence` — création de dossier impactée (plus de legalDomain dans le body)

---

## Dépendances

### Subfeatures bloquantes

- Aucune

### Questions ouvertes impactées

- Aucune

---

## Notes et décisions

- Le champ `legalDomain` dans `POST /api/v1/case-files` est retiré du DTO client. Le backend le résout automatiquement depuis le workspace. Cela simplifie le formulaire de création et centralise la logique domaine au niveau workspace.
- En V1, si un workspace a `legal_domain = DROIT_IMMIGRATION` ou `DROIT_IMMOBILIER`, la création de case file retourne 400 (domaine non supporté). La modale d'onboarding (SF-27-02) empêche ce cas en ne proposant que DROIT_DU_TRAVAIL comme option cliquable.
