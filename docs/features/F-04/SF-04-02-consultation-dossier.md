# Mini-spec — F-04 / SF-04-02 — Consultation d'un dossier

> Statut : done (rétroactif — implémenté le 2026-03-17, PR #11)

---

## Identifiant

`F-04 / SF-04-02`

## Feature parente

`F-04` — Liste & consultation des dossiers

## Statut

`done`

## Date de création

2026-03-17 (rétroactif)

## Branche Git

`feature/SF-04-02-get-case-file` *(PR #11 mergée sur master)*

---

## Objectif

Permettre à un avocat authentifié de consulter le détail d'un dossier par son identifiant via `GET /api/v1/case-files/{id}`, avec isolation workspace.

---

## Comportement attendu

### Cas nominal

`GET /api/v1/case-files/{id}` retourne le `CaseFileResponse` du dossier si celui-ci appartient au workspace de l'utilisateur connecté.

Réponse attendue :
```json
{
  "id": "uuid",
  "title": "Licenciement M. Dupont",
  "legalDomain": "EMPLOYMENT_LAW",
  "description": "Contestation licenciement abusif",
  "status": "OPEN",
  "createdAt": "2026-03-17T10:00:00Z"
}
```

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Utilisateur non authentifié | Refus | 401 |
| Dossier inconnu (UUID inexistant) | Not found | 404 |
| Dossier appartenant à un autre workspace | Not found — pas d'information sur l'existence | 404 |
| Utilisateur sans workspace | Not found | 404 |

---

## Critères d'acceptation

- [x] GET `/{id}` dossier existant → 200 avec id, title, legalDomain, status, createdAt
- [x] GET `/{id}` UUID inexistant → 404
- [x] GET `/{id}` sans auth → 401
- [x] GET `/{id}` dossier d'un autre workspace → 404 (isolation — pas de 403 pour éviter l'énumération)

---

## Périmètre

### Hors scope (explicite)

- Modification d'un dossier
- Changement de statut
- Liste des documents du dossier (F-05 / SF-05-02)
- Frontend détail dossier (implémenté dans PR #12)

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Description |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{id}` | Oui | Consultation par UUID |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| case_files | SELECT | Par id, puis vérification workspace_id |
| workspace_members | SELECT | Résolution workspace de l'utilisateur |
| auth_accounts | SELECT | Résolution user depuis OidcUser |

### Migration Liquibase

- [x] Non applicable — table `case_files` déjà créée en SF-03-01

---

## Plan de test

### Tests d'intégration

- [x] GET `/{id}` existant → 200 avec données correctes (`CaseFileControllerIT`)
- [x] GET `/{id}` UUID inexistant → 404
- [x] GET `/{id}` sans auth → 401

### Isolation workspace

- [x] Vérification dans le service : `caseFile.getWorkspace().getId().equals(workspace.getId())` → 404 si différent
- [ ] ⚠️ Non testée explicitement avec deux workspaces distincts dans `CaseFileControllerIT` — dette technique à corriger

---

## Dépendances

### Subfeatures bloquantes

- F-04 / SF-04-01 — liste dossiers — statut : done
- F-03 / SF-03-01 — création dossier — statut : done

---

## Notes et décisions

- Le 404 est retourné aussi pour les dossiers d'un autre workspace (et non un 403) : évite l'énumération d'IDs — décision de sécurité explicite
- L'isolation est vérifiée au niveau service après la lecture en base : `findById()` puis comparaison `workspace_id`
- Le frontend (CaseFileDetailComponent) est couvert par PR #12 séparée
