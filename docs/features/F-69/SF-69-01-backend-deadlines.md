# Mini-spec — F-69 / SF-69-01 Backend CRUD délais légaux

---

## Identifiant

`F-69 / SF-69-01`

## Feature parente

`F-69` — Suivi des délais légaux

## Statut

`ready`

## Date de création

2026-03-29

## Branche Git

`feat/SF-69-01-backend-deadlines`

---

## Objectif

Exposer un CRUD REST des délais légaux associés à un dossier (`case_deadlines`), avec isolation workspace et validation.

---

## Comportement attendu

### Cas nominal

- `GET /api/v1/case-files/{caseFileId}/deadlines` — retourne la liste triée par `due_date` ASC
- `POST /api/v1/case-files/{caseFileId}/deadlines` — crée un délai (label + due_date), retourne 201
- `PUT /api/v1/case-files/{caseFileId}/deadlines/{deadlineId}` — modifie label et/ou due_date, retourne 200
- `DELETE /api/v1/case-files/{caseFileId}/deadlines/{deadlineId}` — supprime, retourne 204

Tout membre du workspace peut créer, modifier ou supprimer un délai (non restreint à l'auteur — les délais sont des données d'équipe).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| label absent ou vide | 400 Bad Request | 400 |
| due_date absent | 400 Bad Request | 400 |
| label > 255 caractères | 400 Bad Request | 400 |
| caseFileId inexistant dans le workspace | 404 Not Found | 404 |
| caseFileId appartenant à un autre workspace | 404 Not Found | 404 |
| deadlineId inexistant | 404 Not Found | 404 |
| Requête non authentifiée | 401 Unauthorized | 401 |

---

## Critères d'acceptation

- [ ] GET retourne la liste des délais triés par due_date ASC
- [ ] POST crée un délai, retourne 201 avec le délai créé
- [ ] PUT met à jour label et/ou due_date, retourne le délai mis à jour
- [ ] DELETE supprime le délai, retourne 204
- [ ] label vide → 400
- [ ] due_date absent → 400
- [ ] Dossier d'un autre workspace → 404
- [ ] 401 si non authentifié
- [ ] Isolation workspace : un membre du workspace A ne voit pas les délais du workspace B

---

## Périmètre

### Hors scope (explicite)

- Alertes email J-15/J-7 (SF-69-03)
- Affichage frontend (SF-69-02)
- Restriction à l'auteur — tout membre peut modifier/supprimer (délais = données d'équipe)
- Notification lors de la création d'un délai

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| created_at | `now()` | `@PrePersist` |
| updated_at | `now()` | `@PrePersist` + `@PreUpdate` |

Comportements à la création :
- `created_at` / `updated_at` renseignés automatiquement
- `workspace_id` résolu via le `case_file` associé (isolation garantie)

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| label | Oui | 255 | texte libre, non vide après trim | Non | trim() |
| due_date | Oui | — | ISO 8601 date (LocalDate) | Non | — |

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{caseFileId}/deadlines` | Oui | MEMBER |
| POST | `/api/v1/case-files/{caseFileId}/deadlines` | Oui | MEMBER |
| PUT | `/api/v1/case-files/{caseFileId}/deadlines/{deadlineId}` | Oui | MEMBER |
| DELETE | `/api/v1/case-files/{caseFileId}/deadlines/{deadlineId}` | Oui | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| case_deadlines | CREATE TABLE | nouvelle table |
| case_files | SELECT | résolution workspace |

### Migration Liquibase

- [x] Oui — `037-create-case-deadlines.xml`

Colonnes :
- `id` UUID PK
- `case_file_id` UUID FK → `case_files(id)` ON DELETE CASCADE
- `label` VARCHAR(255) NOT NULL
- `due_date` DATE NOT NULL
- `created_at` TIMESTAMPTZ NOT NULL DEFAULT NOW()
- `updated_at` TIMESTAMPTZ NOT NULL DEFAULT NOW()

Index : `idx_case_deadlines_case_file_id` sur `case_file_id`

### Composants Angular (si applicable)

Aucun — backend only.

---

## Plan de test

### Tests unitaires

Aucun service métier complexe nécessitant des mocks — logique directe delegée aux ITs.

### Tests d'intégration (`CaseDeadlineControllerIT`)

- [ ] IT-01 : GET → 200, liste vide si aucun délai
- [ ] IT-02 : POST → 201, délai créé avec label et due_date corrects
- [ ] IT-03 : POST → 400 si label vide
- [ ] IT-04 : POST → 400 si due_date absent
- [ ] IT-05 : GET → retourne les délais triés par due_date ASC
- [ ] IT-06 : PUT → 200, délai mis à jour
- [ ] IT-07 : DELETE → 204, délai supprimé
- [ ] IT-08 : GET → 404 si dossier d'un autre workspace
- [ ] IT-09 : GET → 401 si non authentifié

### Isolation workspace

- [x] Applicable — IT-08 : utilisateur du workspace A → 404 sur les délais du workspace B

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée, impact limité à son périmètre

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné (nouvelle table, pas de modification de flux existant)

---

## Dépendances

### Subfeatures bloquantes

- SF-70-01 (done) — pattern CRUD notes réutilisé pour la structure

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- Pattern identique à `case_notes` (SF-70-01) : isolation via `resolveCaseFileForUser()`, `@PrePersist`/`@PreUpdate`
- Pas de restriction auteur : les délais sont des données partagées de l'équipe (contrairement aux notes internes)
- `due_date` est un `LocalDate` (pas de timezone — une date juridique n'a pas d'heure)
- Réponse DTO : `CaseDeadlineResponse` (id, label, dueDate, createdAt, updatedAt)
