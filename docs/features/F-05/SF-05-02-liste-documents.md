# Mini-spec — F-05 / SF-05-02 — Liste des documents

> Statut : done (rétroactif — implémenté le 2026-03-17)

---

## Identifiant

`F-05 / SF-05-02`

## Feature parente

`F-05` — Upload de documents

## Statut

`done`

## Date de création

2026-03-17 (rétroactif)

## Branche Git

`feat/SF-05-02-liste-documents` *(non créée — commit direct sur master, écart de gouvernance corrigé rétroactivement)*

---

## Objectif

Permettre à un avocat de consulter la liste des documents d'un dossier, triée par date d'ajout décroissante, avec affichage dans le frontend.

---

## Comportement attendu

### Cas nominal

`GET /api/v1/case-files/{id}/documents` retourne un tableau JSON des documents du dossier, ordonnés par `created_at DESC`. Le composant Angular affiche la liste dans une `mat-table`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Utilisateur non authentifié | Refus | 401 |
| Case file inconnu | Not found | 404 |
| Case file appartenant à un autre workspace | Not found | 404 |
| Aucun document | Tableau vide `[]` | 200 |

---

## Critères d'acceptation

- [x] GET liste → 200 avec tableau des documents du dossier
- [x] GET liste dossier vide → 200 avec `[]`
- [x] GET sans auth → 401
- [x] GET case file autre workspace → 404 (isolation)
- [x] Frontend affiche la liste dans mat-table avec colonnes : nom, type, taille, date, actions

---

## Périmètre

### Hors scope (explicite)

- Pagination (tous les documents d'un dossier en une seule réponse — V1)
- Filtrage, tri côté client
- Statut d'extraction (F-06)

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Description |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{id}/documents` | Oui | Liste des documents |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| documents | SELECT | Filtré par case_file_id + isolation workspace |

### Composants Angular

- `CaseFileDetailComponent` — section Documents avec mat-table (colonnes : nom, type uppercase, taille formatée, date, télécharger)

---

## Plan de test

### Tests d'intégration

- [x] GET liste vide → 200 `[]` (`DocumentControllerIT`)
- [x] GET liste avec docs → 200 avec items
- [x] GET sans auth → 401
- [x] GET autre workspace → 404 (isolation)

### Tests frontend (Karma)

- [x] CaseFileDetailComponent — should create
- [x] Affichage section documents

### Isolation workspace

- [x] Testée — un utilisateur ne voit que les documents de son workspace

---

## Dépendances

### Subfeatures bloquantes

- F-05 / SF-05-01 — upload document — statut : done

---

## Notes et décisions

- Tri côté backend (`ORDER BY created_at DESC`) pour éviter le tri côté Angular
- `formatSize()` implémentée dans le composant (o / Ko / Mo)
- `UpperCasePipe` nécessaire pour afficher le content-type — importée explicitement (composant standalone)
