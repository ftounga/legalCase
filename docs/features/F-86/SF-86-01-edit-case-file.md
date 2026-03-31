# Mini-spec — F-86 / SF-86-01 Renommage et modification d'un dossier

---

## Identifiant

`F-86 / SF-86-01`

## Feature parente

`F-86` — Renommage et modification d'un dossier

## Statut

`ready`

## Date de création

2026-03-31

## Branche Git

`feat/SF-86-01-edit-case-file`

---

## Objectif

Permettre à un membre du workspace de modifier le titre et la description d'un dossier existant via un endpoint PATCH et un dialog Angular dans la page dossier.

---

## Comportement attendu

### Cas nominal

1. L'utilisateur clique sur un bouton "Modifier" dans la page dossier (`/case-files/{id}`)
2. Un dialog Angular s'ouvre, pré-rempli avec le titre et la description actuels
3. L'utilisateur modifie les champs et valide
4. `PATCH /api/v1/case-files/{id}` est appelé avec `{ title, description }`
5. Le backend vérifie que le dossier appartient au workspace de l'utilisateur
6. Il met à jour `title`, `description` et `updatedAt` dans `case_files`
7. Il trace `CASE_FILE_UPDATED` dans `audit_logs`
8. Il retourne le `CaseFileResponse` mis à jour (200)
9. Le frontend referme le dialog, met à jour le titre affiché et affiche un snackbar de succès

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Titre vide après trim | 400 Bad Request | 400 |
| Titre > 255 caractères | 400 Bad Request | 400 |
| Description > 2000 caractères | 400 Bad Request | 400 |
| Dossier inexistant | 404 Not Found | 404 |
| Dossier dans un autre workspace | 403 Forbidden | 403 |
| Dossier soft-deleted (`deletedAt` non null) | 404 Not Found | 404 |

---

## Critères d'acceptation

- [ ] `PATCH /api/v1/case-files/{id}` avec titre valide → 200 + champs mis à jour dans la réponse
- [ ] `updatedAt` mis à jour en base
- [ ] `CASE_FILE_UPDATED` tracé dans `audit_logs`
- [ ] Titre vide → 400
- [ ] Dossier d'un autre workspace → 403
- [ ] Dossier supprimé (deletedAt non null) → 404
- [ ] Dialog pré-rempli avec les valeurs actuelles
- [ ] Snackbar "Dossier modifié avec succès" après validation
- [ ] Titre affiché dans le header de la page dossier mis à jour sans rechargement complet

---

## Périmètre

### Hors scope

- Modification du domaine juridique (legalDomain) — impacte les prompts IA, à traiter séparément
- Modification du statut (OPEN/CLOSED) — géré par F-53
- Modification de la date de création
- Accès restreint au seul OWNER/ADMIN (tout membre peut modifier)

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format | Unicité | Normalisation |
|-------|-------------|-------------|--------|---------|---------------|
| title | Oui | 255 | non vide après trim | Non | trim() |
| description | Non | 2000 | texte libre | Non | — |

---

## Technique

### Endpoint

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| PATCH | `/api/v1/case-files/{id}` | Oui | MEMBER (tout membre du workspace) |

**Request body :**
```json
{ "title": "Nouveau titre", "description": "Description optionnelle" }
```

**Response (200) :** `CaseFileResponse` existant (inchangé structurellement)

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| case_files | UPDATE | title, description, updated_at |
| audit_logs | INSERT | action = CASE_FILE_UPDATED |

### Migration Liquibase

- [x] Non applicable — colonnes title et description existent déjà

### Composants Angular

- `CaseFileEditDialogComponent` (nouveau) — dialog avec 2 champs (titre, description), validation réactive
- `CaseFileDetailComponent` (existant) — ajout bouton "Modifier", subscription résultat dialog, mise à jour locale du titre

---

## Plan de test

### Tests unitaires

- [ ] `CaseFileService.update()` — cas nominal : title + description mis à jour, updatedAt rechargé
- [ ] `CaseFileService.update()` — dossier autre workspace → 403
- [ ] `CaseFileService.update()` — dossier deleted → 404
- [ ] `CaseFileService.update()` — audit log CASE_FILE_UPDATED enregistré

### Tests d'intégration

- [ ] `PATCH /api/v1/case-files/{id}` → 200 avec payload valide
- [ ] `PATCH /api/v1/case-files/{id}` → 400 titre vide
- [ ] `PATCH /api/v1/case-files/{id}` → 403 dossier autre workspace
- [ ] `PATCH /api/v1/case-files/{id}` → 404 dossier inexistant
- [ ] `PATCH /api/v1/case-files/{id}` → 404 dossier soft-deleted

### Tests frontend

- [ ] Dialog s'ouvre pré-rempli avec les valeurs actuelles
- [ ] Validation réactive : titre vide → bouton désactivé
- [ ] Succès API → dialog fermé + snackbar + titre mis à jour
- [ ] Erreur API → snackbar d'erreur

### Isolation workspace

- [x] Applicable — test : PATCH depuis un utilisateur d'un workspace B sur un dossier du workspace A → 403

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — utilise `@AuthenticationPrincipal` existant, pattern identique aux autres endpoints
- [x] **Aucune préoccupation transversale** — même pattern que `CaseFileStatusService`, pas de changement de routing, pas de gate plan

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — pas de changement de routing ni d'auth

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- `PATCH` partiel : seuls `title` et `description` sont modifiables. Pas de `PUT` complet.
- L'audit log réutilise le pattern existant de `CaseFileStatusService.saveAuditLog()`
- Le frontend met à jour le titre localement (pas de reload complet) via la réponse 200
