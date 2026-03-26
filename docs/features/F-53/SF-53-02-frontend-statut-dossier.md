# Mini-spec — F-53 / SF-53-02 — Frontend gestion statut des dossiers

> Ce document doit être validé AVANT de démarrer le dev.

---

## Identifiant

`F-53 / SF-53-02`

## Feature parente

`F-53` — Gestion du statut des dossiers

## Statut

`draft`

## Date de création

2026-03-26

## Branche Git

`feat/SF-53-02-frontend-statut-dossier`

---

## Objectif

Exposer dans l'interface les actions de clôture, réouverture et suppression d'un dossier, avec visibilité conditionnelle selon le rôle de l'utilisateur, gestion des erreurs métier (402, 409) et badge statut `CLOSED`.

---

## Comportement attendu

### Cas nominal

**Badge statut `CLOSED`** :
- Dans `CaseFilesListComponent` : `statusLabel('CLOSED') = 'Clôturé'`, `statusClass('CLOSED') = 'badge--neutral'`
- Dans `CaseFileDetailComponent` : même logique — le badge existant `statusLabel()` doit gérer `CLOSED`

**Bouton Clôturer** (`PATCH /close`) :
- Visible pour tous les membres si `status === 'OPEN'`
- Appel → snackbar "Dossier clôturé" + rechargement du dossier

**Bouton Réouvrir** (`PATCH /reopen`) :
- Visible pour OWNER et ADMIN uniquement si `status === 'CLOSED'`
- Appel → snackbar "Dossier réouvert" + rechargement du dossier
- Erreur 402 → snackbar "Limite de dossiers actifs atteinte. Passez à un plan supérieur."

**Bouton Supprimer** (`DELETE /{id}`) :
- Visible pour OWNER uniquement, quelle que soit le statut
- Désactivé si `fullAnalysisRunning()` (analyse PENDING ou PROCESSING détectée)
- Clic → ouvre `CaseFileDeleteDialogComponent` (confirmation MatDialog)
- Confirmation → DELETE → navigation vers `/case-files`
- Erreur 409 → snackbar "Impossible de supprimer un dossier avec une analyse en cours."

**Résolution du rôle courant** :
- Au chargement de `CaseFileDetailComponent`, appel `WorkspaceMemberService.getMembers()` puis filtre par `authService.currentUser().id` → `currentMemberRole` signal
- `canReopen` : computed = `currentMemberRole() === 'OWNER' || currentMemberRole() === 'ADMIN'`
- `canDelete` : computed = `currentMemberRole() === 'OWNER'`

### Cas d'erreur

| Situation | Comportement | Code HTTP reçu |
|-----------|-------------|----------------|
| Réouverture quota atteint | Snackbar "Limite de dossiers actifs atteinte. Passez à un plan supérieur." | 402 |
| Suppression analyse en cours | Snackbar "Impossible de supprimer un dossier avec une analyse en cours." | 409 |
| Erreur réseau / 5xx | Snackbar "Une erreur est survenue. Veuillez réessayer." | 5xx |

---

## Critères d'acceptation

- [ ] Badge `CLOSED` affiche "Clôturé" dans la liste et dans le détail
- [ ] Bouton Clôturer visible si `status === 'OPEN'`, accessible à tous les membres
- [ ] Bouton Réouvrir visible si `status === 'CLOSED'` et rôle OWNER ou ADMIN
- [ ] Bouton Réouvrir absent si rôle LAWYER ou MEMBER
- [ ] Bouton Supprimer visible uniquement si rôle OWNER
- [ ] Bouton Supprimer désactivé si analyse en cours (`fullAnalysisRunning()`)
- [ ] Clic Supprimer ouvre une dialog de confirmation (MatDialog)
- [ ] Confirmation suppression → navigation vers `/case-files`
- [ ] Erreur 402 → snackbar message upgrade plan
- [ ] Erreur 409 → snackbar message analyse en cours

---

## Périmètre

### Hors scope

- Blocage du lancement d'analyse sur un dossier `CLOSED` (hors scope F-53)
- Affichage de l'historique des actions de statut
- Modification du design system (utilise variables existantes)

---

## Technique

### Endpoints consommés

| Méthode | URL | Rôle minimum |
|---------|-----|-------------|
| PATCH | `/api/v1/case-files/{id}/close` | MEMBER |
| PATCH | `/api/v1/case-files/{id}/reopen` | ADMIN |
| DELETE | `/api/v1/case-files/{id}` | OWNER |

### Composants Angular

| Composant | Modification |
|-----------|-------------|
| `CaseFilesListComponent` | `statusLabel()` : ajout cas `CLOSED` → `'Clôturé'` |
| `CaseFileDetailComponent` | Chargement rôle courant, boutons conditionnels, handlers close/reopen/delete |
| `CaseFileDeleteDialogComponent` (NOUVEAU) | Dialog confirmation suppression, même pattern que `DocumentDeleteDialogComponent` |
| `CaseFileStatusService` (NOUVEAU) | `close()`, `reopen()`, `delete()` — wrapping HTTP calls |

### Migration Liquibase

- Non applicable (frontend uniquement)

---

## Plan de test

### Tests unitaires

- [ ] `CaseFileStatusService` — `close()` appelle `PATCH /api/v1/case-files/{id}/close`
- [ ] `CaseFileStatusService` — `reopen()` appelle `PATCH /api/v1/case-files/{id}/reopen`
- [ ] `CaseFileStatusService` — `delete()` appelle `DELETE /api/v1/case-files/{id}`
- [ ] `CaseFileDetailComponent` — `canReopen()` true si OWNER, true si ADMIN, false si LAWYER
- [ ] `CaseFileDetailComponent` — `canDelete()` true si OWNER, false si ADMIN
- [ ] `CaseFileDetailComponent` — bouton Supprimer désactivé si `fullAnalysisRunning()` true

### Tests d'intégration

- Non applicables (couverts par SF-53-01)

### Isolation workspace

- Non applicable — pas de filtre workspace côté frontend (résolu par le backend)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Workspace context** — résolution du rôle membre courant via `WorkspaceMemberService`
- [ ] **Auth / Principal** — pas de changement
- [ ] **Plans / limites** — le 402 est géré en lecture (snackbar), pas de nouveau gate frontend
- [ ] **Navigation / routing frontend** — une nouvelle route de sortie (navigate vers `/case-files` après delete), mais aucun guard modifié

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|-----------------|------------------------------|
| `CaseFilesListComponent` | Modification `statusLabel()` — risque faible (ajout cas) | Test unitaire du badge |
| `CaseFileDetailComponent` | Ajout chargement membre — risque faible (nouveau signal) | Test unitaire composant |

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné (les smoke tests existants ne couvrent pas le détail d'un dossier)

---

## Dépendances

### Subfeatures bloquantes

- `SF-53-01` — statut : done (mergé PR #114)

### Questions ouvertes impactées

- Aucune

---

## Notes et décisions

- Résolution du rôle courant : via `WorkspaceMemberService.getMembers()` filtré par `authService.currentUser()?.id` — pas de nouvel endpoint backend, réutilise l'existant
- `CaseFileDeleteDialogComponent` : composant dédié (pas de réutilisation de `DocumentDeleteDialogComponent`) pour un message de confirmation adapté au dossier
- 402 traité comme erreur HTTP → `error.status === 402` dans le handler
- Bouton Supprimer toujours visible (OWNER) mais désactivé si `fullAnalysisRunning()` — l'erreur 409 reste gérée en fallback au cas où
