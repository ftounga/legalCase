# Mini-spec — F-101 / SF-101-01 — Stepper d'état du dossier

## Identifiant
`F-101 / SF-101-01`

## Feature parente
`F-101` — Tableau de bord dossier enrichi

## Statut
`ready`

## Date de création
2026-04-01

## Branche Git
`feat/SF-101-01-case-dashboard-stepper`

---

## Objectif

Afficher en haut de la page dossier un stepper horizontal à 5 étapes colorées indiquant l'état d'avancement du dossier. Chaque étape incomplète est cliquable et navigue vers la section concernée.

---

## Comportement attendu

### Composant

Nouveau composant standalone `CaseDashboardStepperComponent` inséré dans `CaseFileDetailComponent`, juste après le header du dossier (titre + badges) et avant les sections Documents/Analyse.

### Les 5 étapes

| # | Label | Condition `done` | Condition `in_progress` | Détail affiché | Action au clic si non done |
|---|-------|-----------------|------------------------|----------------|---------------------------|
| 1 | Documents | `documents.length > 0` | `uploading` | `"N document(s)"` | Scroll vers `#section-documents` |
| 2 | Analyse IA | `synthesis !== null` | `fullAnalysisRunning` | `"Analyse terminée"` / `"En cours…"` | Scroll vers `#section-analyse` |
| 3 | Questions IA | `pendingCount === 0 && questions.length > 0` | — | `"N question(s) en attente"` | Naviguer vers `/case-files/:id/synthesis` |
| 4 | Délais légaux | `pendingAiDeadlines === 0` | — | `"N proposition(s) IA en attente"` | Scroll vers `#section-deadlines` |
| 5 | Pièces manquantes | `piecesManquantes.length === 0` | — | `"N pièce(s) identifiée(s)"` | Naviguer vers `/case-files/:id/synthesis` |

### Statuts visuels

| Statut | Couleur icône | Couleur label |
|--------|--------------|---------------|
| `done` | Vert `#27AE60` — icône `check_circle` | `#27AE60` |
| `in_progress` | Or `#C9973A` — spinner ou icône `pending` | `#C9973A` |
| `pending` | Gris `#6B7A8D` — icône `radio_button_unchecked` | `#6B7A8D` |

Connecteurs horizontaux entre étapes : trait `1px solid #E0E4EA`.

### Étapes conditionnelles

- Étapes 3 et 5 : statut `pending` si `synthesis === null` (pas encore d'analyse).
- Étape 4 : statut `done` si aucun délai AI avec `aiStatus === 'PENDING'` (zéro proposition en attente, ou aucun délai AI du tout).

### Données dans le parent

`CaseFileDetailComponent` charge déjà : `documents`, `synthesis`, `questions`, `analysisJobs`.

**Nouveau** : charger les délais via `CaseDeadlineService.list()` dans `ngOnInit` du parent → signal `deadlines`.

Le parent calcule un computed `dashboardSteps` de type `DashboardStep[]` et le passe en `@Input()` au stepper.

### Interface

```typescript
export interface DashboardStep {
  id: string;               // 'documents' | 'analyse' | 'questions' | 'delais' | 'pieces'
  label: string;
  status: 'done' | 'in_progress' | 'pending';
  detail: string | null;
  anchorId: string | null;  // null → navigation vers synthesis
}
```

---

## Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Dossier vide (0 doc) | Étape 1 en `pending`, étapes 2-5 en `pending` |
| Chargement délais échoue | Étape 4 reste `pending`, fail-open silencieux |
| `questions` vide mais synthesis existe | Étape 3 en `pending` (pas de questions générées) |
| Clic sur étape `done` | Aucune action |
| Analyse en cours | Étape 2 en `in_progress` |

---

## Critères d'acceptation

- [ ] Stepper visible en haut de la page dossier, avant la section Documents
- [ ] Étape 1 verte dès qu'au moins 1 document est uploadé
- [ ] Étape 2 en `in_progress` pendant l'analyse, verte après
- [ ] Étape 3 affiche le nombre de questions en attente, verte si toutes répondues
- [ ] Étape 4 affiche les propositions IA en attente, verte si aucune
- [ ] Étape 5 affiche le nombre de pièces manquantes, verte si zéro
- [ ] Clic sur étape 1 (incomplète) scroll vers section Documents
- [ ] Clic sur étape 2 (incomplète) scroll vers section Analyse
- [ ] Clic sur étape 4 (incomplète) scroll vers section Délais
- [ ] Clic sur étape 3 ou 5 (incomplète) navigue vers `/synthesis`
- [ ] Aucune action sur clic d'une étape `done`
- [ ] Responsive mobile : stepper en colonne (vertical) sous 600px

---

## Périmètre

### Dans le scope
- `CaseDashboardStepperComponent` nouveau composant standalone
- Modification `CaseFileDetailComponent` : chargement délais + computed `dashboardSteps`
- Insertion du stepper dans le template HTML parent

### Hors scope
- Modification de la page Synthèse
- Aucun endpoint backend (tout est déjà disponible)
- Modification de `CaseDeadlinesSectionComponent`

---

## Technique

### Nouveaux fichiers
- `frontend/src/app/case-files/case-dashboard-stepper/case-dashboard-stepper.component.ts`
- `frontend/src/app/case-files/case-dashboard-stepper/case-dashboard-stepper.component.html`
- `frontend/src/app/case-files/case-dashboard-stepper/case-dashboard-stepper.component.scss`
- `frontend/src/app/case-files/case-dashboard-stepper/case-dashboard-stepper.component.spec.ts`

### Fichiers modifiés
- `frontend/src/app/case-files/case-file-detail/case-file-detail.component.ts` — `deadlines` signal + `dashboardSteps` computed + import stepper
- `frontend/src/app/case-files/case-file-detail/case-file-detail.component.html` — insertion du stepper
- `frontend/src/app/case-files/case-file-detail/case-file-detail.component.spec.ts` — nouveaux tests

### Ancres HTML
Ajouter `id="section-documents"`, `id="section-analyse"`, `id="section-deadlines"` aux sections existantes du template parent.

---

## Plan de test

### Tests unitaires — `CaseDashboardStepperComponent`
- [ ] Rendu : 5 étapes affichées
- [ ] Icône `check_circle` si `status === 'done'`
- [ ] Icône `radio_button_unchecked` si `status === 'pending'`
- [ ] Clic étape `pending` avec `anchorId` → scroll appelé
- [ ] Clic étape `pending` sans `anchorId` → navigate appelé
- [ ] Clic étape `done` → aucune action

### Tests computed — `CaseFileDetailComponent`
- [ ] Étape 1 `done` si `documents.length > 0`
- [ ] Étape 2 `in_progress` si `fullAnalysisRunning() === true`
- [ ] Étape 2 `done` si `synthesis !== null`
- [ ] Étape 3 `pending` si `synthesis === null`
- [ ] Étape 3 `done` si `questions.length > 0 && pendingCount === 0`
- [ ] Étape 4 `done` si aucun délai AI avec `aiStatus === 'PENDING'`
- [ ] Étape 5 `done` si `synthesis.piecesManquantes.length === 0`

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — ajout d'un composant d'affichage dans une page existante, lecture seule, pas de modification de routing ni d'auth.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné

---

## Dépendances

### Subfeatures bloquantes
Aucune — toutes les données sont déjà disponibles.
