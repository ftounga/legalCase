# Mini-spec — F-38 / SF-38-02 Frontend suppression de documents

## Identifiant
`F-38 / SF-38-02`

## Feature parente
`F-38` — Suppression de documents

## Statut
`ready`

## Date de création
2026-03-23

## Branche Git
`feat/SF-38-02-frontend-suppression-documents`

---

## Objectif

Permettre à l'avocat de supprimer un document depuis l'écran dossier, avec confirmation par dialog, et afficher un message adaptatif sur la synthèse si des documents ont été ajoutés et/ou supprimés depuis la dernière analyse.

---

## Comportement attendu

### Cas nominal

1. Bouton poubelle (`delete`) dans la colonne actions de chaque ligne document
2. Clic → `MatDialog` de confirmation : "Supprimer le document `<nom>` ?"
3. Confirmation → `DELETE /api/v1/case-files/{id}/documents/{docId}` → 204
4. Document retiré de la liste localement
5. `caseFile.lastDocumentDeletedAt` mis à jour localement (date courante)
6. Snackbar "Document supprimé"

**Message adaptatif sur la synthèse :**
- Additions seulement → message existant ("ne prend pas en compte X document(s) récent(s)")
- Suppressions seulement → "Des documents ont été supprimés depuis la dernière synthèse. Relancez une analyse pour la mettre à jour."
- Les deux → "Des documents ont été ajoutés et/ou supprimés depuis la dernière synthèse. Relancez une analyse pour la mettre à jour."

### Cas d'erreur

| Situation | Comportement |
|-----------|-------------|
| 409 — analyse en cours | Snackbar "Suppression impossible : une analyse est en cours." |
| 404 — document inconnu | Snackbar "Document introuvable." |
| Autre erreur | Snackbar "Erreur lors de la suppression." |
| Analyse en cours (frontend) | Bouton poubelle désactivé |

---

## Critères d'acceptation

- [ ] Bouton poubelle visible sur chaque ligne document
- [ ] MatDialog de confirmation avant suppression (Annuler + Supprimer warn)
- [ ] Suppression → document retiré de la liste + snackbar "Document supprimé"
- [ ] Bouton désactivé pendant fullAnalysisRunning / enrichedAnalysisRunning / docAnalysisRunning
- [ ] `deletedSinceLastAnalysis()` : true si `lastDocumentDeletedAt > synthesis.updatedAt`
- [ ] Message adaptatif : 3 cas couverts
- [ ] 409 → snackbar dédiée

---

## Périmètre

### Hors scope

- Suppression en masse
- Historique consultable des suppressions (SF-38-03)
- Re-fetch du caseFile après suppression (mise à jour locale suffisante)

---

## Technique

### Composants impactés

- `CaseFile` model — `+ lastDocumentDeletedAt: string | null`
- `DocumentService` — `+ delete(caseFileId, documentId): Observable<void>`
- `CaseFileDetailComponent` — bouton suppression, `deletedSinceLastAnalysis` computed, message adaptatif
- `DocumentDeleteDialogComponent` (nouveau, inline dans case-file-detail) — dialog confirmation

### Endpoint(s)

| Méthode | URL | Auth |
|---------|-----|------|
| DELETE | `/api/v1/case-files/{caseFileId}/documents/{documentId}` | Oui |

### Migration Liquibase
Non applicable.

---

## Plan de test

### Tests unitaires (Karma)

- [ ] Bouton delete présent dans le DOM quand `documents.length > 0`
- [ ] `deletedSinceLastAnalysis()` : true si `lastDocumentDeletedAt > synthesis.updatedAt`
- [ ] `deletedSinceLastAnalysis()` : false si `lastDocumentDeletedAt` est null
- [ ] Message adaptatif : additions + suppressions → message combiné
- [ ] Bouton désactivé quand `fullAnalysisRunning()` est true

### Isolation workspace
Non applicable (isolation côté backend, SF-38-01).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — ajout bouton + computed sur composant existant, pas de routing, pas d'auth, pas de plan gate

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné

---

## Dépendances

### Subfeatures bloquantes

- SF-38-01 — statut : done ✓

### Questions ouvertes impactées

- Aucune

---

## Notes et décisions

- `lastDocumentDeletedAt` mis à jour localement après suppression (évite un re-fetch du dossier).
- Dialog de confirmation créé dans le dossier `case-file-detail/` (usage unique, pas de composant partagé).
- Bouton désactivé pendant toute analyse en cours (cohérent avec le guard 409 côté backend).
