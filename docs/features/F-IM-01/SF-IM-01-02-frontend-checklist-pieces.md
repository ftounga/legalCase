# Mini-spec — F-IM-01 / SF-IM-01-02 Composant Angular checklist pièces

## Identifiant

`F-IM-01 / SF-IM-01-02`

## Feature parente

`F-IM-01` — Checklist pièces par type de titre de séjour

## Statut

`in-progress`

## Date de création

2026-04-04

## Branche Git

`feat/SF-IM-01-02-frontend-checklist-pieces`

---

## Objectif

Afficher dans la page dossier une section repliable `ImmigrationChecklistSectionComponent` permettant à l'avocat de sélectionner un type de titre de séjour et un pays, de consulter la liste des pièces requises et de cocher le statut de chaque pièce (PRESENT / ABSENT / INCONNU). La section n'est visible que pour les dossiers `DROIT_IMMIGRATION`.

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre un dossier `DROIT_IMMIGRATION`.
2. La section "Checklist pièces immigration" est visible, repliée par défaut.
3. L'avocat déplie la section — un sélecteur de type de titre (mat-select) et de pays (mat-select) apparaît.
4. À chaque changement de sélecteur, `GET /immigration-checklist?titreType=...&country=...` est appelé.
5. La liste des pièces s'affiche avec le statut actuel (badge coloré : vert PRESENT, rouge ABSENT, gris INCONNU).
6. L'avocat clique sur le statut d'une pièce → cycle INCONNU → PRESENT → ABSENT → INCONNU.
7. Un bouton "Enregistrer" appelle `PUT /immigration-checklist` avec la liste complète.
8. Confirmation via `MatSnackBar`.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Erreur réseau GET | Snackbar d'erreur, liste vide affichée |
| Erreur réseau PUT | Snackbar d'erreur, statuts locaux préservés |
| Dossier non DROIT_IMMIGRATION | Composant non rendu (condition dans CaseFileDetailComponent) |

---

## Critères d'acceptation

- [ ] Section visible uniquement si `caseFile().legalDomain === 'DROIT_IMMIGRATION'`
- [ ] Section repliée par défaut, dépliable via clic sur header
- [ ] Badge compteur dans le header affiche le nombre de pièces PRESENT
- [ ] Sélecteurs type de titre + pays déclenchent le chargement de la liste
- [ ] Statuts affichés avec code couleur (vert / rouge / gris)
- [ ] Clic sur statut → cycle des 3 valeurs
- [ ] Bouton Enregistrer appelle PUT et affiche snackbar succès
- [ ] Erreur PUT → snackbar erreur
- [ ] Bouton Enregistrer désactivé pendant la sauvegarde (`saving` signal)

---

## Périmètre

### Hors scope

- Export PDF (SF-IM-01-03)
- Détection automatique du type de titre depuis l'IA
- Tri ou filtrage des pièces
- Ajout de pièces hors référentiel

---

## Technique

### Endpoints consommés

| Méthode | URL |
|---------|-----|
| GET | `/api/v1/case-files/{id}/immigration-checklist?titreType=...&country=...` |
| PUT | `/api/v1/case-files/{id}/immigration-checklist` |

### Modèles Angular

`frontend/src/app/core/models/immigration-checklist.model.ts`
```typescript
export interface ImmigrationPieceItem { label: string; statut: 'PRESENT' | 'ABSENT' | 'INCONNU'; }
export interface ImmigrationChecklist { caseFileId: string; titreType: string; country: string; pieces: ImmigrationPieceItem[]; }
```

### Service Angular

`frontend/src/app/core/services/immigration-checklist.service.ts`
- `get(caseFileId, titreType, country): Observable<ImmigrationChecklist>`
- `upsert(caseFileId, body): Observable<ImmigrationChecklist>`

### Composant

`frontend/src/app/case-files/immigration-checklist-section/immigration-checklist-section.component.ts`
- `@Input() caseFileId!: string`
- Signals : `collapsed`, `saving`, `checklist`, `titreType`, `country`
- Méthodes : `load()`, `cycleStatut(piece)`, `save()`

### Intégration

`case-file-detail.component.html` : `@if (caseFile()!.legalDomain === 'DROIT_IMMIGRATION')` wrappant le composant.

---

## Plan de test

### Tests unitaires

- [ ] `ImmigrationChecklistSectionComponent` — init avec titreType/country → appel GET
- [ ] `ImmigrationChecklistSectionComponent` — cycleStatut INCONNU → PRESENT → ABSENT → INCONNU
- [ ] `ImmigrationChecklistSectionComponent` — save() → appel PUT → snackbar succès
- [ ] `ImmigrationChecklistSectionComponent` — erreur GET → snackbar erreur
- [ ] `ImmigrationChecklistSectionComponent` — badge = nb pièces PRESENT
- [ ] `CaseFileDetailComponent` — section absente si legalDomain ≠ DROIT_IMMIGRATION
- [ ] `CaseFileDetailComponent` — section présente si legalDomain = DROIT_IMMIGRATION

### Tests d'intégration

Non applicable — composant frontend, pas de nouvel endpoint.

### Isolation workspace

Non applicable — gérée par le backend.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Workspace context** — touche `CaseFileDetailComponent` (ajout d'une section conditionnelle)

### Composants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|-----------------|------------------------------|
| `CaseFileDetailComponent` | Ajout du composant immigration-checklist-section | Test : section absente si DROIT_DU_TRAVAIL |

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — pas de route ni guard modifié

---

## Dépendances

### Subfeatures bloquantes

- SF-IM-01-01 — statut : done ✅

---

## Notes et décisions

- Pattern identique à `CaseDeadlinesSectionComponent` : signals, collapsed, snackbar.
- Les 3 valeurs de statut cyclent au clic (pas de select par pièce) pour une UX rapide.
- Le sélecteur de type de titre est initialisé à `VISA_ETUDIANT` + `FRANCE` par défaut.
- Valeurs des sélecteurs hardcodées dans le composant (cohérent avec le référentiel statique Java).
