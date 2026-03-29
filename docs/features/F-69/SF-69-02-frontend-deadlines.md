# Mini-spec — F-69 / SF-69-02 Frontend section Délais légaux

---

## Identifiant

`F-69 / SF-69-02`

## Feature parente

`F-69` — Suivi des délais légaux

## Statut

`ready`

## Date de création

2026-03-29

## Branche Git

`feat/SF-69-02-frontend-deadlines`

---

## Objectif

Afficher la section "Délais légaux" dans la page dossier : liste des délais avec indicateur J-X coloré, formulaire d'ajout et actions edit/delete.

---

## Comportement attendu

### Cas nominal

- Section "Délais légaux" visible dans `case-file-detail` sous la section Notes
- Chaque délai affiche : label, date (dd/MM/yyyy), indicateur J-X
  - Passé (J < 0) → rouge `#C0392B`
  - Dans les 15 jours (0 ≤ J ≤ 15) → or `#C9973A`
  - Plus de 15 jours → vert `#27AE60`
- Bouton "Ajouter" ouvre un formulaire inline : label (input) + date (date picker)
- "Ajouter" désactivé si label vide ou date absente
- Tout membre peut modifier ou supprimer n'importe quel délai (pas de restriction auteur)
- Édition inline : formulaire avec champs pré-remplis, boutons Sauvegarder / Annuler
- Suppression directe (pas de confirmation dialog — délai est non-destructif)

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Erreur chargement | SnackBar erreur |
| Erreur création | SnackBar erreur |
| Erreur mise à jour | SnackBar erreur |
| Erreur suppression | SnackBar erreur |

---

## Critères d'acceptation

- [ ] Section "Délais légaux" visible dans la page dossier
- [ ] Délais affichés triés par due_date ASC (backend garantit l'ordre)
- [ ] Indicateur J-X correct : valeur et couleur selon règle des 15 jours
- [ ] Formulaire ajout : désactivé si label vide ou date absente
- [ ] Tout membre peut éditer/supprimer tout délai
- [ ] Édition inline fonctionnelle avec pré-remplissage
- [ ] Snackbars success/error sur toutes les opérations

---

## Périmètre

### Hors scope (explicite)

- Alertes email J-15/J-7 (SF-69-03)
- Confirmation dialog pour suppression (pas jugé nécessaire — opération récupérable)
- Pagination (nombre de délais par dossier reste limité en pratique)

---

## Technique

### Endpoint(s) consommés

| Méthode | URL |
|---------|-----|
| GET | `/api/v1/case-files/{caseFileId}/deadlines` |
| POST | `/api/v1/case-files/{caseFileId}/deadlines` |
| PUT | `/api/v1/case-files/{caseFileId}/deadlines/{deadlineId}` |
| DELETE | `/api/v1/case-files/{caseFileId}/deadlines/{deadlineId}` |

### Composants Angular

- `CaseDeadline` (model) — `frontend/src/app/core/models/case-deadline.model.ts`
- `CaseDeadlineService` — `frontend/src/app/core/services/case-deadline.service.ts`
- `CaseDeadlinesSectionComponent` — `frontend/src/app/case-files/case-deadlines-section/`
- Intégration dans `CaseFileDetailComponent`

---

## Plan de test

### Tests unitaires / composant

- [ ] U-01 : état vide → message "Aucun délai"
- [ ] U-02 : délai avec due_date passée → classe CSS `deadline--past`
- [ ] U-03 : délai dans les 15 jours → classe CSS `deadline--soon`
- [ ] U-04 : bouton "Ajouter" désactivé si label vide
- [ ] U-05 : saveDeadline appelle deadlineService.create avec les bons params

### Tests service

- [ ] GET /deadlines retourne la liste
- [ ] POST /deadlines envoie label + dueDate
- [ ] PUT /deadlines/{id} envoie label + dueDate
- [ ] DELETE /deadlines/{id}

### Isolation workspace

- [ ] Non applicable — frontend uniquement, isolation garantie par le backend

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné

---

## Dépendances

### Subfeatures bloquantes

- SF-69-01 (done) — backend CRUD disponible

---

## Notes et décisions

- Indicateur J-X : `Math.ceil((dueDate - today) / 86400000)` — valeur en jours entiers
- `dueDate` reçu du backend en format `YYYY-MM-DD` (LocalDate) — parsé via `new Date(dueDate + 'T12:00:00')`
  pour éviter les problèmes de timezone (minuit UTC = veille en locale Paris)
- Pas de `MatDatepicker` custom — input `type="date"` natif pour la simplicité
