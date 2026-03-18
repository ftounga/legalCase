# Mini-spec — F-16 / SF-16-03 — Gate upload document

## Identifiant
`F-16 / SF-16-03`

## Feature parente
`F-16` — Gestion des abonnements

## Statut
`in-progress`

## Date de création
`2026-03-18`

## Branche Git
`feat/SF-16-03-gate-upload-document`

---

## Objectif

Bloquer l'upload d'un document avec HTTP 402 si le dossier a atteint son quota de documents selon le plan du workspace (Starter : 5, Pro : 30).

---

## Comportement attendu

### Cas nominal

- Avant de persister un document, compter les documents existants du dossier.
- Si le compte est strictement inférieur à la limite du plan → upload autorisé (flux normal inchangé).
- La limite est lue depuis la `Subscription` active du workspace via `PlanLimitService`.

### Limites par plan

| Plan | Documents par dossier max |
|------|--------------------------|
| STARTER | 5 |
| PRO | 30 |

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Quota documents atteint | 402 avec message "Limite de documents atteinte pour votre plan." | 402 |
| Subscription absente | Upload autorisé (fail open) | 201 |

---

## Critères d'acceptation

- [ ] `PlanLimitService` expose `getMaxDocumentsPerCaseFile(String planCode)` et `getMaxDocumentsPerCaseFileForWorkspace(UUID workspaceId)`
- [ ] `DocumentService.upload` vérifie le quota avant le stockage et la persistance
- [ ] Retourne 402 si quota atteint
- [ ] Fail open si pas de subscription
- [ ] `DocumentRepository.countByCaseFileId` réutilisé (déjà présent)
- [ ] Tests unitaires : quota non atteint, quota atteint, pas de subscription
- [ ] 402 lancé AVANT le stockage S3 (pas de fichier orphelin)

---

## Périmètre

### Hors scope (explicite)

- Gate création dossier (SF-16-02)
- Gate re-analyse enrichie (SF-16-04)

---

## Technique

### Endpoint impacté

| Méthode | URL | Modification |
|---------|-----|-------------|
| POST | `/api/v1/case-files/{id}/documents` | Ajout contrôle quota avant stockage |

### Tables lues

| Table | Opération | Notes |
|-------|-----------|-------|
| `subscriptions` | SELECT | Via `PlanLimitService` |
| `documents` | COUNT | `countByCaseFileId` déjà existant |

### Migration Liquibase

- [x] Non applicable

---

## Plan de test

### Tests unitaires

- [ ] `PlanLimitService.getMaxDocumentsPerCaseFile("STARTER")` → 5
- [ ] `PlanLimitService.getMaxDocumentsPerCaseFile("PRO")` → 30
- [ ] `DocumentService.upload` — quota non atteint (4/5) → upload OK
- [ ] `DocumentService.upload` — quota atteint (5/5 Starter) → 402
- [ ] `DocumentService.upload` — pas de subscription → fail open

### Isolation workspace

- [ ] Applicable — le dossier est déjà résolu dans le contexte du workspace (`resolveCaseFile`)

---

## Dépendances

### Subfeatures bloquantes

- SF-16-01 — statut : done
- SF-16-02 — statut : done (`PlanLimitService` disponible)

---

## Notes et décisions

- Le 402 est levé AVANT l'appel à `storageService.upload` pour éviter tout fichier orphelin en object storage.
- `countByCaseFileId` est déjà dans `DocumentRepository` — aucune modification du repository nécessaire.
