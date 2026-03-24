# Mini-spec — F-38 / SF-38-04 Écran dédié Journal d'actions

## Identifiant
`F-38 / SF-38-04`

## Feature parente
`F-38` — Suppression de documents

## Statut
`ready`

## Date de création
2026-03-24

## Branche Git
`feat/SF-38-04-audit-log-screen`

---

## Objectif

Déplacer le journal d'actions dans un écran dédié `/workspace/audit-logs`, accessible depuis Administration, avec recherche/filtre par utilisateur, action et dossier.

---

## Comportement attendu

### Backend
- Endpoint existant `GET /api/v1/admin/audit-logs` inchangé.

### Frontend
- Nouvelle route `/workspace/audit-logs`
- Bouton "Voir le journal" dans `WorkspaceAdminComponent` → navigue vers `/workspace/audit-logs`
- Section journal supprimée de `WorkspaceAdminComponent`
- `AuditLogScreenComponent` :
  - Champ recherche texte libre (filtre client sur userEmail + caseFileTitle + documentName)
  - Filtre "Action" : select — toutes / DOCUMENT_DELETED
  - Table : Date / Action / Utilisateur / Dossier / Document
  - Liste vide → "Aucune action enregistrée."
  - 403 → message accès refusé
  - 500 → snackbar erreur

### Cas d'erreur

| Situation | Comportement |
|-----------|-------------|
| 403 | Message "Accès réservé aux OWNER et ADMIN" |
| 500 | Snackbar "Erreur lors du chargement du journal." |
| Liste vide | "Aucune action enregistrée." |

---

## Critères d'acceptation

- [ ] Route `/workspace/audit-logs` accessible OWNER/ADMIN, message accès refusé sinon
- [ ] Bouton dans Administration navigue vers l'écran
- [ ] Section journal supprimée de `WorkspaceAdminComponent`
- [ ] Filtre texte libre fonctionne (email, dossier, document)
- [ ] Filtre "Action" fonctionne
- [ ] Table Date/Action/Utilisateur/Dossier/Document

---

## Périmètre

### Hors scope
- Pagination
- Export CSV
- Filtre par plage de dates
- Nouveaux types d'action

---

## Technique

### Route
| Chemin | Composant | Guard |
|--------|-----------|-------|
| `/workspace/audit-logs` | `AuditLogScreenComponent` | authGuard (existant) |

### Composants Angular
- `AuditLogScreenComponent` (nouveau)
- `WorkspaceAdminComponent` — supprime section journal, ajoute bouton lien

### Migration Liquibase
Non applicable.

---

## Plan de test

### Tests unitaires (frontend Karma)
- [ ] Filtre texte → affiche uniquement les lignes correspondantes
- [ ] Filtre action "DOCUMENT_DELETED" → filtre correct
- [ ] Liste vide → message "Aucune action enregistrée."
- [ ] Erreur 403 → message accès refusé
- [ ] `WorkspaceAdminComponent` — section journal absente, bouton "Voir le journal" présent

### Isolation workspace
- [x] Non applicable — filtrage workspace géré par le backend (SF-38-03)

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] **Navigation / routing** — nouvelle route `/workspace/audit-logs`
  - Composants impactés : `app.routes.ts`, `WorkspaceAdminComponent`

---

## Dépendances
- SF-38-03 — statut : done ✓
