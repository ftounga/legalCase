# SF-70-01 — Backend notes internes sur un dossier

## Objectif
Permettre à l'avocat d'ajouter, modifier et supprimer des notes libres sur un dossier. Notes non visibles par le client (hors du share public). Scoped au workspace.

---

## Comportement nominal

| Endpoint | Description |
|----------|-------------|
| `GET /api/v1/case-files/:id/notes` | Liste toutes les notes du dossier, ordre `created_at DESC` |
| `POST /api/v1/case-files/:id/notes` | Crée une note (`content` max 5000 chars) |
| `PUT /api/v1/case-files/:id/notes/:noteId` | Modifie le contenu d'une note |
| `DELETE /api/v1/case-files/:id/notes/:noteId` | Supprime une note |

- Isolation workspace : l'utilisateur doit appartenir au même workspace que le dossier
- `created_by` = utilisateur courant
- Seul l'auteur peut modifier ou supprimer sa note

---

## Cas d'erreur

| Cas | Code |
|-----|------|
| Dossier inexistant ou hors workspace | 404 |
| `content` vide ou > 5000 chars | 400 |
| Modifier/supprimer une note d'un autre utilisateur | 403 |
| Sans authentification | 401 |

---

## Critères d'acceptation

- [ ] `GET` retourne la liste des notes du dossier (vide si aucune)
- [ ] `POST` crée une note et retourne 201 avec la note créée
- [ ] `PUT` modifie le contenu, retourne 200
- [ ] `DELETE` supprime, retourne 204
- [ ] Un utilisateur d'un autre workspace ne peut pas accéder aux notes → 404
- [ ] Un utilisateur ne peut pas modifier/supprimer la note d'un autre → 403
- [ ] `content` vide → 400 | `content` > 5000 chars → 400

---

## Plan de test

### Unitaires
- U-01 : `create` → note persistée, 201
- U-02 : `content` vide → 400
- U-03 : `content` > 5000 chars → 400

### Intégration
- I-01 : GET liste → 200 avec notes dans l'ordre décroissant
- I-02 : POST → 201 + note retournée
- I-03 : PUT auteur → 200
- I-04 : DELETE auteur → 204
- I-05 : PUT autre utilisateur → 403
- I-06 : DELETE autre utilisateur → 403
- I-07 : dossier hors workspace → 404
- I-08 : sans auth → 401

---

## Tables / migrations

**Nouvelle table `case_notes` (migration 036) :**
- `id` UUID PK
- `case_file_id` UUID FK → `case_files(id)`
- `created_by_user_id` UUID FK → `users(id)`
- `content` TEXT NOT NULL
- `created_at` TIMESTAMPTZ NOT NULL
- `updated_at` TIMESTAMPTZ NOT NULL

Index : `idx_case_notes_case_file_id`

---

## Composants impactés

| Fichier | Action |
|---------|--------|
| `036-create-case-notes.xml` | Nouveau |
| `casefile/CaseNote.java` | Nouveau |
| `casefile/CaseNoteRepository.java` | Nouveau |
| `casefile/CaseNoteService.java` | Nouveau |
| `casefile/CaseNoteController.java` | Nouveau |
| `casefile/CaseNoteResponse.java` | Nouveau |
| `casefile/CaseNoteRequest.java` | Nouveau |
| `CaseNoteServiceTest.java` | Nouveau |
| `CaseNoteControllerIT.java` | Nouveau |

---

## Hors périmètre
- Notes visibles par le client (share public) — hors scope
- Mentions @utilisateur — V3
- Pièces jointes sur une note — V3
