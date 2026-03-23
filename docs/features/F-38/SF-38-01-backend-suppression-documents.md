# Mini-spec — F-38 / SF-38-01 Backend suppression de documents

## Identifiant
`F-38 / SF-38-01`

## Feature parente
`F-38` — Suppression de documents

## Statut
`ready`

## Date de création
2026-03-23

## Branche Git
`feat/SF-38-01-backend-suppression-documents`

---

## Objectif

Permettre la suppression d'un document via `DELETE /api/case-files/{id}/documents/{docId}`, avec cascade complète, mise à jour d'un timestamp `last_document_deleted_at` sur le dossier, et enregistrement dans une table `audit_logs`.

---

## Comportement attendu

### Cas nominal

1. `DELETE /api/case-files/{caseFileId}/documents/{docId}` (authentifié, workspace isolé)
2. Vérifier que le document appartient au dossier ET au workspace de l'utilisateur
3. Supprimer en cascade : `chunk_analyses` → `document_chunks` → `document_analyses` → `document_extractions` → `document` (dans cet ordre, FK oblige)
4. Mettre à jour `case_files.last_document_deleted_at = NOW()`
5. Insérer une ligne dans `audit_logs` : action=`DOCUMENT_DELETED`, metadata JSON = `{documentId, documentName, caseFileId, caseFileTitle}`
6. Répondre `204 No Content`

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Document inexistant | 404 Not Found | 404 |
| Document d'un autre workspace | 404 Not Found (ne pas révéler l'existence) | 404 |
| Document d'un autre dossier | 404 Not Found | 404 |
| Analyse en cours sur le dossier | 409 Conflict — suppression refusée pendant analyse | 409 |

---

## Critères d'acceptation

- [ ] `DELETE` retourne 204 et le document est absent de `SELECT * FROM documents`
- [ ] Toutes les tables dépendantes sont vidées (chunk_analyses, document_chunks, document_analyses, document_extractions)
- [ ] `case_files.last_document_deleted_at` est mis à jour
- [ ] Une ligne `audit_logs` est insérée avec action=`DOCUMENT_DELETED` et le bon metadata
- [ ] 404 si document d'un autre workspace
- [ ] 404 si docId ne correspond pas au caseFileId
- [ ] 409 si une analyse (PENDING ou PROCESSING) est en cours sur le dossier

---

## Périmètre

### Hors scope (explicite)

- Suppression du fichier physique dans le stockage S3 (géré séparément ou hors V1)
- Interface de consultation des audit_logs (SF-38-03 à planifier)
- Suppression en masse de plusieurs documents en une requête

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| DELETE | `/api/case-files/{caseFileId}/documents/{docId}` | Oui | LAWYER |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `documents` | DELETE | après cascade des dépendances |
| `document_extractions` | DELETE | cascade depuis document |
| `document_analyses` | DELETE | FK sur document_extractions |
| `document_chunks` | DELETE | FK sur document_extractions |
| `chunk_analyses` | DELETE | FK sur document_chunks |
| `case_files` | UPDATE | `last_document_deleted_at = NOW()` |
| `audit_logs` | INSERT | nouvelle table |

### Migration Liquibase

- [x] Oui — `027-document-deletion-audit.xml`
  - Ajouter `last_document_deleted_at TIMESTAMP WITH TIME ZONE` sur `case_files` (nullable)
  - Créer table `audit_logs` : id UUID PK, workspace_id UUID FK, user_id UUID FK, case_file_id UUID FK nullable, action VARCHAR(50), metadata JSONB, created_at TIMESTAMP WITH TIME ZONE

### Composants Java

- `DocumentDeleteService` — logique de suppression + audit
- `DocumentController` — nouveau endpoint DELETE
- `AuditLogRepository` — JPA repository
- `AuditLog` — entité JPA

---

## Plan de test

### Tests unitaires

- [ ] `DocumentDeleteService` — cas nominal : cascade, timestamp, audit log
- [ ] `DocumentDeleteService` — 404 si document autre workspace
- [ ] `DocumentDeleteService` — 409 si analyse en cours

### Tests d'intégration

- [ ] `DELETE /api/case-files/{id}/documents/{docId}` → 204 + document absent
- [ ] `DELETE /api/case-files/{id}/documents/{docId}` → 404 si autre workspace
- [ ] `DELETE /api/case-files/{id}/documents/{docId}` → 409 si analyse PENDING

### Isolation workspace

- [x] Applicable — un user du workspace A ne peut pas supprimer un document du workspace B

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Workspace context** — résolution du workspace pour vérifier la propriété du document

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `DocumentService` (upload) | Met à jour `last_document_added_at` — cohérence avec le nouveau `last_document_deleted_at` | Test unitaire du service |
| `CaseFileDetailComponent` | Recevra le nouveau champ `lastDocumentDeletedAt` — doit réafficher la liste | SF-38-02 frontend |

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — nouvelle fonctionnalité isolée, pas de modification de routing ni d'auth

---

## Dépendances

### Subfeatures bloquantes
- Aucune

### Questions ouvertes impactées
- Aucune

---

## Notes et décisions

- La suppression physique S3 est hors scope V1 : les fichiers sont orphelins mais n'ont pas d'impact fonctionnel. À nettoyer via un job de GC ultérieur.
- `audit_logs.metadata` est en JSONB pour flexibilité future (autres types d'actions).
- La vérification "analyse en cours" se fait via `analysis_jobs` — si PENDING ou PROCESSING sur ce dossier → 409.
- `last_document_deleted_at` est sur `case_files` (pas `workspaces`) car la périmation de synthèse est au niveau du dossier.
