# Mini-spec — F-110 / SF-110-05 : Signalement d'anomalie par les MEMBER

## Identifiant
`F-110 / SF-110-05`

## Feature parente
`F-110` — Guides & barèmes métier par domaine

## Statut
`done`

## Date de création
`2026-04-04`

## Branche Git
`feat/SF-110-05-referentials-report-anomaly`

---

## Objectif
Un MEMBER peut signaler une valeur de référentiel qui lui semble incorrecte. Le signalement crée une notification email vers tous les OWNER/ADMIN du workspace avec le champ concerné et un commentaire libre. Le MEMBER ne peut pas modifier la valeur.

---

## Comportement attendu

### Cas nominal
1. Le MEMBER voit un bouton "Signaler une anomalie" (icône `flag`) sur chaque entrée. Absent pour OWNER/ADMIN.
2. Il clique → dialog avec le libellé de l'entrée (lecture seule) + champ commentaire libre (max 500 chars, obligatoire).
3. Il soumet → `POST /api/v1/referentials/{entryId}/reports` body `{ comment }`.
4. Backend crée un `ReferentialReport` (status=OPEN) et envoie un email à tous les OWNER/ADMIN du workspace.
5. Réponse `201` → snackbar "Signalement envoyé."
6. Doublon (même userId + entryId, status=OPEN) → 409 "Vous avez déjà signalé cette valeur."

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| OWNER ou ADMIN appelle le endpoint | 403 | 403 |
| Entrée introuvable | 404 | 404 |
| Comment vide ou manquant | 400 | 400 |
| Doublon (même userId + entryId OPEN) | 409 | 409 |

---

## Critères d'acceptation

- [x] Bouton "Signaler" visible uniquement pour les MEMBER
- [x] Bouton absent pour OWNER/ADMIN
- [x] Dialog : libellé entrée en lecture seule + champ commentaire requis (max 500 chars)
- [x] `POST /api/v1/referentials/{entryId}/reports` — 201, crée ReferentialReport, email OWNER/ADMIN
- [x] Email contient : nom entrée, valeur actuelle, commentaire, email reporter, lien /referentials
- [x] Doublon → 409
- [x] Comment vide → 400 (validation Bean Validation @NotBlank)
- [x] Snackbar "Signalement envoyé." après succès, message "déjà signalé" si 409

---

## Périmètre

### Hors scope
- Interface de gestion des signalements (liste, résolution)
- Statut RESOLVED/CLOSED
- Notification in-app
- Signalement par OWNER/ADMIN

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle |
|---------|-----|------|------|
| POST | `/api/v1/referentials/{entryId}/reports` | Oui | MEMBER uniquement |

### Nouvelles classes backend
- `ReferentialReport` (entité JPA)
- `ReferentialReportRepository`
- `ReferentialReportService`
- `ReferentialReportRequest` (record `@NotBlank @Size(max=500) comment`)
- `ReferentialReportController`
- Migration `051-create-referential-reports.xml`
- `sendReferentialReport()` dans `EmailService`
- `findByWorkspace_IdAndMemberRoleIn()` dans `WorkspaceMemberRepository`

### Composant Angular
- `ReferentialReportDialogComponent` (dialog commentaire)
- `ReferentialsComponent` : signal `canReport`, bouton `.report-btn`, `openReportDialog()`

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| referential_reports | CREATE | Nouvelle table (migration 051), UQ entry_id+reporter_id+status |

---

## Plan de test

### Backend — unitaires (RPT)
- [x] RPT-01 : signalement normal → report créé + email envoyé OWNER/ADMIN
- [x] RPT-02 : doublon → 409
- [x] RPT-03 : entrée introuvable → 404

### Backend — intégration (IT-RPT)
- [x] IT-RPT-01 : OWNER → 403
- [x] IT-RPT-02 : MEMBER → 201
- [x] IT-RPT-03 : doublon MEMBER → 409
- [x] IT-RPT-04 : commentaire vide → 400

### Frontend — unitaires (REF-UI)
- [x] REF-UI-09 : bouton "Signaler" absent pour OWNER
- [x] REF-UI-10 : bouton "Signaler" présent pour MEMBER
- [x] REF-UI-11 : canReport = true pour MEMBER

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Auth / Principal** — POST reports vérifie rôle MEMBER exclusivement
  - Composants impactés : nouveau `ReferentialReportController`
- [x] **Workspace context** — `findByWorkspace_IdAndMemberRoleIn` filtre les destinataires par workspace

| Composant | Impact potentiel | Non-régression |
|-----------|-----------------|----------------|
| `ReferentialController` | non impacté | GET/PUT inchangés |
| `ReferentialsComponent` | bouton conditionnel MEMBER | OWNER : bouton Modifier inchangé, 11 tests verts |

---

## Dépendances
- SF-110-01/02/03/04 — Done
