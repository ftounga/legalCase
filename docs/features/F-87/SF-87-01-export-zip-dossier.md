# Mini-spec — F-87 / SF-87-01 Export complet d'un dossier (ZIP)

---

## Identifiant

`F-87 / SF-87-01`

## Feature parente

`F-87` — Export complet d'un dossier (ZIP)

## Statut

`ready`

## Date de création

2026-03-31

## Branche Git

`feat/SF-87-01-export-zip-dossier`

---

## Objectif

Permettre à un membre du workspace de télécharger l'intégralité d'un dossier sous forme d'archive ZIP contenant la synthèse (JSON), la liste des documents, les notes et les délais légaux.

---

## Comportement attendu

### Cas nominal

1. L'utilisateur clique sur un bouton "Exporter" dans la page dossier
2. Le frontend appelle `GET /api/v1/case-files/{id}/export`
3. Le backend vérifie que le dossier appartient au workspace de l'utilisateur
4. Il génère en mémoire un ZIP contenant :
   - `synthese.json` — la dernière analyse STANDARD ou ENRICHED disponible (null si aucune)
   - `documents.csv` — liste des documents (nom, taille, uploadedAt, status)
   - `notes.txt` — toutes les notes internes du dossier (une par ligne avec date)
   - `delais.txt` — tous les délais légaux (titre, date, type, statut J-X)
   - `dossier.json` — métadonnées du dossier (id, title, legalDomain, status, createdAt)
5. Le backend retourne le ZIP avec `Content-Disposition: attachment; filename="dossier-{title}.zip"`
6. Le frontend déclenche le téléchargement navigateur

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Dossier inexistant | 404 Not Found | 404 |
| Dossier dans un autre workspace | 403 Forbidden | 403 |
| Dossier soft-deleted | 404 Not Found | 404 |
| Aucune analyse disponible | ZIP généré sans synthese.json (pas d'erreur) | 200 |

---

## Critères d'acceptation

- [ ] `GET /api/v1/case-files/{id}/export` → 200 + `application/zip` + fichier téléchargeable
- [ ] ZIP contient `dossier.json`, `documents.csv`, `notes.txt`, `delais.txt`
- [ ] ZIP contient `synthese.json` si une analyse est disponible
- [ ] ZIP ne contient pas `synthese.json` si aucune analyse (pas d'erreur)
- [ ] Dossier d'un autre workspace → 403
- [ ] Dossier deleted → 404
- [ ] Nom de fichier ZIP : `dossier-{title-sanitisé}.zip`
- [ ] Bouton "Exporter" visible dans la page dossier
- [ ] Téléchargement navigateur déclenché côté frontend

---

## Périmètre

### Hors scope

- Export des fichiers binaires des documents (trop volumineux — uniquement la liste CSV)
- Export en PDF (couvert par F-40 pour la synthèse uniquement)
- Génération asynchrone / job — export synchrone en mémoire suffisant
- Export multiple dossiers (zip de zips)

---

## Contraintes de validation

Aucune contrainte de saisie — endpoint GET sans body.

---

## Technique

### Endpoint

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{id}/export` | Oui | MEMBER |

**Response :**
- Status : 200
- Content-Type : `application/zip`
- Content-Disposition : `attachment; filename="dossier-{sanitized-title}.zip"`
- Body : flux ZIP binaire

### Contenu du ZIP

| Fichier | Source | Format |
|---------|--------|--------|
| `dossier.json` | CaseFile | JSON : id, title, legalDomain, status, createdAt |
| `documents.csv` | Document[] | CSV : name, sizeBytes, uploadedAt, processingStatus |
| `notes.txt` | CaseNote[] | Texte : une note par bloc (date + contenu) |
| `delais.txt` | CaseDeadline[] | Texte : un délai par ligne (titre, date, type) |
| `synthese.json` | CaseAnalysis (dernière) | JSON brut du résultat d'analyse (si présente) |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| case_files | SELECT | vérification workspace |
| documents | SELECT | liste pour CSV |
| case_notes | SELECT | notes pour TXT |
| case_deadlines | SELECT | délais pour TXT |
| case_analyses | SELECT | dernière analyse disponible |

### Migration Liquibase

- [x] Non applicable — lecture seule sur tables existantes

### Composants Angular

- `CaseFileDetailComponent` (existant) — ajout bouton "Exporter" déclenchant un `GET` via `HttpClient` avec `responseType: 'blob'`, puis création d'un lien `<a>` temporaire pour déclencher le téléchargement

---

## Plan de test

### Tests unitaires

- [ ] `CaseFileExportService.export()` — ZIP contient les 5 entrées attendues
- [ ] `CaseFileExportService.export()` — ZIP sans synthese.json si aucune analyse
- [ ] `CaseFileExportService.export()` — dossier autre workspace → 403
- [ ] `CaseFileExportService.export()` — dossier deleted → 404
- [ ] Sanitisation du nom de fichier (espaces → tirets, caractères spéciaux supprimés)

### Tests d'intégration

- [ ] `GET /api/v1/case-files/{id}/export` → 200 + Content-Type application/zip
- [ ] `GET /api/v1/case-files/{id}/export` → 403 dossier autre workspace
- [ ] `GET /api/v1/case-files/{id}/export` → 404 dossier inexistant
- [ ] Content-Disposition header contient le nom du dossier

### Tests frontend

- [ ] Clic bouton "Exporter" → appel HTTP déclenché
- [ ] Réponse blob → téléchargement navigateur déclenché
- [ ] Erreur API → snackbar d'erreur

### Isolation workspace

- [x] Applicable — test : GET export depuis utilisateur workspace B sur dossier workspace A → 403

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — nouvel endpoint lecture seule, pas de changement de routing, pas de gate plan, pattern auth identique aux autres endpoints

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- Export synchrone en mémoire : les dossiers ne contiennent pas de binaires → taille ZIP maîtrisée
- `java.util.zip.ZipOutputStream` (stdlib Java) — pas de dépendance externe
- Sanitisation du titre pour le nom de fichier : `title.replaceAll("[^a-zA-Z0-9\\-_]", "-").toLowerCase()`
- La dernière analyse est déterminée par `createdAt DESC LIMIT 1` sur `case_analyses` avec status = DONE
- Le frontend utilise `responseType: 'blob'` sur `HttpClient.get()` + `URL.createObjectURL()` pour déclencher le téléchargement
