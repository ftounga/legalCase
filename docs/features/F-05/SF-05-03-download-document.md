# Mini-spec — F-05 / SF-05-03 — Téléchargement de document

> Statut : done (rétroactif — implémenté le 2026-03-17)

---

## Identifiant

`F-05 / SF-05-03`

## Feature parente

`F-05` — Upload de documents

## Statut

`done`

## Date de création

2026-03-17 (rétroactif)

## Branche Git

`feat/SF-05-03-download-document` *(non créée — commit direct sur master, écart de gouvernance corrigé rétroactivement)*

---

## Objectif

Permettre à un avocat de télécharger un document via une URL presignée temporaire (15 min) générée par MinIO.

---

## Comportement attendu

### Cas nominal

`GET /api/v1/case-files/{caseFileId}/documents/{docId}/download` vérifie l'isolation workspace, génère une URL presignée valide 15 minutes et retourne une redirection 302 avec `Location` header.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Utilisateur non authentifié | Refus | 401 |
| Document inconnu | Not found | 404 |
| Document appartenant à un autre case file | Not found | 404 |
| Case file d'un autre workspace | Not found | 404 |

---

## Critères d'acceptation

- [x] GET download doc existant → 302 avec header Location non vide
- [x] GET download sans auth → 401
- [x] GET download doc inconnu → 404
- [x] URL presignée valide 15 minutes
- [x] Isolation workspace respectée

---

## Périmètre

### Hors scope (explicite)

- URL de téléchargement permanent
- Téléchargement direct via streaming backend (proxy)
- Contrôle d'accès par rôle (V1 : tout membre authentifié du workspace)

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Description |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{caseFileId}/documents/{docId}/download` | Oui | Redirect presigned URL |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| documents | SELECT | Lecture storage_key pour presigning |

---

## Plan de test

### Tests d'intégration

- [x] GET download existant → 302 + Location header (`DocumentControllerIT`)
- [x] GET download sans auth → 401
- [x] GET download doc inconnu → 404

### Isolation workspace

- [x] Vérification case file appartient au workspace de l'utilisateur

---

## Dépendances

### Subfeatures bloquantes

- F-05 / SF-05-01 — upload document — statut : done

---

## Notes et décisions

- Approche redirect 302 plutôt que proxy streaming : moins de charge backend, MinIO sert le fichier directement
- Expiration presigned URL : 15 minutes (constante `PRESIGNED_URL_EXPIRATION_MINUTES`)
- Le frontend utilise `downloadUrl()` qui retourne la chaîne URL et redirige via `href` natif
