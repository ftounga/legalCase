# Mini-spec — F-176 / SF-176-02 Frontend bloc transversal "Pistes stratégiques"

## Identifiant

`F-176 / SF-176-02`

## Feature parente

`F-176` — Bloc transversal "Pistes stratégiques" — séparer options stratégiques de la checklist procédurale F-96

## Statut

`ready`

## Date de création

2026-04-30

## Branche Git

`feat/SF-176-02-frontend-pistes-strategiques`

---

## Objectif

Côté frontend, afficher dans `SynthesisComponent` un nouveau bloc accordéon `Pistes stratégiques` (en miroir de `Checklist procédurale` F-96) qui consomme les endpoints de SF-176-01, présente les pistes triées par statut (À étudier / Retenues / Écartées) avec actions de mise à jour de statut + saisie d'une raison écartée, et propose un export PDF léger.

---

## Comportement attendu

### Cas nominal

1. **Chargement** : à `ngOnInit` puis à chaque changement de version, après `loadVersions()`, le composant invoque `strategicOptionService.list(caseFileId, analysisId)` et stocke le résultat dans le signal `strategicOptions`.
2. **Affichage du bloc** : un `mat-expansion-panel` ouvert par défaut (id="section-pistes") apparaît si `strategicOptions().length > 0`. Sinon le bloc est masqué entièrement.
3. **Tri** : les pistes sont affichées en 3 sous-sections (toutes optionnelles, n'apparaissent que si non vides) :
   - **À étudier** (`statut === 'TO_STUDY'`) — fond beige clair, ordre = `ordre` ascendant
   - **Retenues** (`statut === 'RETAINED'`) — bordure verte, ordre = `ordre` ascendant
   - **Écartées** (`statut === 'DISCARDED'`) — fond grisé + texte légèrement atténué, raisonDiscard affichée en dessous
4. **Affichage par piste** :
   - `texte` (gras)
   - Si `baseJuridique` : ligne dédiée police monospace JetBrains (cohérence design system) avec icône `gavel`
   - Si `horizonTemporel` : badge gris discret
   - Si `conditions.length > 0` : liste à puces "Conditions :"
   - Si `source` : ligne italique grise "Source : …"
5. **Actions par piste** : 3 boutons miroir F-96 (`status-btn--retained` / `status-btn--discarded` / `status-btn--to-study`), bouton actif si statut courant. Le clic appelle `strategicOptionService.updateStatus(option.id, statut)`. Pendant l'update : `<mat-spinner diameter="20">` en lieu et place des boutons.
6. **Saisie raison écartée** : si l'utilisateur clique sur **Écarter**, un dialog `MatDialog` simple s'ouvre avec un `<textarea>` "Raison (facultatif)" + boutons Annuler / Confirmer. Sur Confirmer, l'appel `updateStatus(id, 'DISCARDED', raisonDiscard)` est invoqué. Si l'utilisateur clique **Écarter** alors que la piste est déjà DISCARDED, le dialog s'ouvre en édition de la raison existante.
7. **Édition raison** : sur une piste DISCARDED, un bouton `<mat-icon>edit</mat-icon>` à côté de la raison ouvre le même dialog en édition.
8. **Export PDF** : un bouton `Exporter PDF` dans la barre du bloc déclenche un export simple via `pdfExportService.exportStrategicOptions(caseFile, options)` (méthode à ajouter — section dédiée listant les 3 sous-sections).
9. **Refresh dashboard** : aucun (pas un outil F-IA-04, pas de KPI dashboard).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Backend renvoie 200 + `[]` | Bloc masqué (length 0 = absence) |
| Backend renvoie 4xx/5xx au GET | `strategicOptions = []`, snackbar muet (cohérent F-96 — pas de blocage) |
| Backend renvoie 4xx/5xx au PATCH | Snackbar erreur "Erreur lors de la mise à jour", spinner cleared, état UI inchangé |
| Backend renvoie 403 | Snackbar erreur "Accès refusé", workspace mismatch |
| Backend renvoie 404 | Snackbar erreur "Piste introuvable" |
| Réseau down (timeout) | Snackbar erreur générique, retry manuel par l'utilisateur |
| Saisie raison vide au PATCH DISCARDED | Accepté, `raisonDiscard = null` envoyé |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** : F-96 (procedure_checks) — pattern miroir UX strict (mêmes boutons status, même expansion-panel, même structure HTML/SCSS). F-IA-04 panneau outils décisionnels — non concerné (les pistes ne sont pas un outil décisionnel).
- [x] **Autres pays** : F-176 transversale, pas de variation pays côté frontend (pas de `workspaceCountry` gate).
- [x] **Autres domaines** : F-176 transversale, pas de différenciation domaine côté frontend.
- [x] **Autres UI patterns** : barre d'export miroir `checklist-export-bar` ; statuts miroir `status-btn--*` (verified/non-compliant/to-check) → renommés `status-btn--retained / discarded / to-study`.
- [x] **Autres flows** : pas de routing nouveau, pas d'auth nouvelle, pas de quota.

### Niveaux de vérification

- [x] **Modèle TypeScript** : `StrategicOption` interface importée depuis le contrat figé SF-176-01.
- [x] **Service Angular** : `StrategicOptionService` (nouveau, miroir `ProcedureCheckService`).
- [x] **Composant Angular** : pas de nouveau composant, le bloc est intégré directement dans `SynthesisComponent` (pattern F-96 actuel — éviter prolifération de composants pour des sections simples).
- [x] **Tests existants** : `synthesis.component.spec.ts` à étendre.

### Nouveau pattern UI ou service partagé

- [x] **Nouveau service `StrategicOptionService`** — miroir strict de `ProcedureCheckService` (3 méthodes : list, updateStatus, [optionnel] updateStatusWithReason). Pas de divergence.
- [x] **Nouveau dialog raison écartée** — un dialog simple avec `MatDialog` + textarea, pas de composant partagé à créer (réutilise `MatDialogModule` standard). Si dans 3+ outils on retrouve "demander une raison à l'avocat", on factorisera. À ce stade, c'est isolé F-176.
- [x] **Aucune divergence visuelle introduite** — palette navy/or, JetBrains Mono pour `baseJuridique`, snackbar pour erreurs, spinner intégré. Conforme `DESIGN_SYSTEM.md`.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-96 frontend (checklist procédurale UI) | Oui — pattern de référence | Pattern miroir HTML + SCSS + TS strict |
| F-IA-03 (cohérence IA) | Non — exclu par design F-176 | Pas de `coherenceAlerts` ni `<app-coherence-popover-trigger>` |
| F-IA-04 (panel outils décisionnels) | Non | Les pistes ne sont pas un outil ; pas d'entrée `TOOL_REGISTRY` |
| Pré-fill IA pattern | Non | Les pistes SONT le pré-fill IA elles-mêmes (le contenu vient du JSON IA) |
| Export PDF dossier (F-95) | À étendre | Méthode `exportStrategicOptions` ajoutée à `PdfExportService` |
| Smoke tests E2E | Non concerné | Pas de routing/auth touché |

### Décision

- [x] Pattern miroir F-96 strict appliqué.
- [x] Pas de F-IA-03 (volonté backlog F-176).
- [x] Pas de pré-fill IA "classique" (les pistes EUX sont le pré-fill).

---

## Impact par domaine métier

| Domaine | Effet |
|---------|-------|
| **Droit du travail (FR + BE)** | Le bloc affiche les pistes générées par l'IA pour les dossiers de travail. Pas de différenciation UI. |
| **Droit de la famille (FR + BE)** | Idem — bloc générique. Pas de différenciation UI. |
| **Droit de l'immigration (FR + BE)** | **Cible principale** — bloc le plus utilisé en immigration (cas usage initial dossier Chen 2). Pas de différenciation UI. |

Pas de différenciation par domaine côté frontend (le contenu est piloté par le prompt IA dans SF-176-01).

---

## Parité des domaines métier

(N/A — F-176 est transversale par construction, pas un outil décisionnel niveau ≥ 5.)

---

## Critères d'acceptation

- [ ] Modèle TypeScript `StrategicOption` + enum `StrategicOptionStatus` créés dans `frontend/src/app/core/models/strategic-option.model.ts`.
- [ ] Service `StrategicOptionService` créé dans `frontend/src/app/core/services/strategic-option.service.ts` avec :
  - `list(caseFileId: string, analysisId: string): Observable<StrategicOption[]>`
  - `updateStatus(optionId: string, statut: StrategicOptionStatus, raisonDiscard?: string): Observable<StrategicOption>`
- [ ] `SynthesisComponent.ts` enrichi :
  - Signal `strategicOptions = signal<StrategicOption[]>([])` + `updatingOptionId = signal<string | null>(null)`
  - Computed signals `optionsToStudy()`, `optionsRetained()`, `optionsDiscarded()` qui filtrent par statut
  - Méthode privée `loadStrategicOptionsForVersion(caseFileId, analysisId)` invoquée dans `loadVersions().next` ET `onVersionChange()` (cohérence F-96)
  - Méthode publique `updateOptionStatus(option, statut)` qui invoque le service ; si statut === 'DISCARDED', ouvre `MatDialog` pour saisir la raison avant l'appel
  - Méthode `editDiscardReason(option)` qui ouvre `MatDialog` en édition
  - Reset `strategicOptions.set([])` dans `onVersionChange`
- [ ] Template `synthesis.component.html` enrichi :
  - Nouveau `<mat-expansion-panel expanded id="section-pistes">` placé après le panneau `section-checklist` (ordre logique : F-96 puis F-176)
  - Header avec `<mat-icon>lightbulb</mat-icon>` + titre "Pistes stratégiques" + description "{{ strategicOptions().length }} piste(s) — {{ optionsRetained().length }} retenue(s)"
  - 3 sous-sections (TO_STUDY, RETAINED, DISCARDED) avec headers clairs
  - Pour chaque piste : structure HTML miroir F-96 + champs additionnels (`baseJuridique`, `horizonTemporel`, `conditions`, `source`, `raisonDiscard`)
  - Boutons d'action `Retenir` / `Écarter` / `À étudier`
- [ ] SCSS `synthesis.component.scss` enrichi avec classes :
  - `.pistes-list`, `.pistes-section` (3 variantes par statut), `.piste-item`, `.piste-texte`, `.piste-base-juridique` (JetBrains Mono), `.piste-conditions`, `.piste-raison-discard`
  - `.status-btn--retained`, `.status-btn--discarded`, `.status-btn--to-study` (palette navy/or, vert pour retenue, gris pour écartée)
- [ ] `MatDialog` simple (composant `DiscardReasonDialogComponent` standalone inline ou un dialog template literal) pour saisir la raison.
- [ ] `PdfExportService.exportStrategicOptions(caseFile, options)` ajouté (méthode similaire à `exportChecklist`).
- [ ] Tests Jest `synthesis.component.spec.ts` :
  - `should load strategic options on init`
  - `should display 3 sections by status`
  - `should call updateStatus on click Retenir`
  - `should open dialog on click Écarter and call updateStatus with raison`
  - `should reset strategicOptions on version change`
- [ ] Service mocked dans tests (pas d'appel réel au backend).
- [ ] Compilation TypeScript stricte sans erreur.
- [ ] `ng test` vert localement.

---

## Périmètre

### Hors scope

- Drag-and-drop de réorganisation (V2)
- Commentaires libres avocat sur chaque piste (V2)
- Filtres avancés (V2)
- Recherche full-text dans les pistes (V2)
- Export DOCX dédié (utilise l'export DOCX synthèse global)
- Pré-fill IA "classique" + F-IA-03 (volontairement exclu — voir backlog F-176)
- Tests d'intégration E2E (non requis pour SF UI simple)

---

## Valeurs initiales

| Champ UI | Valeur initiale |
|----------|----------------|
| `strategicOptions` | `[]` |
| `updatingOptionId` | `null` |
| Statut visuel TO_STUDY | beige clair (`#FAF7E8`) |
| Statut visuel RETAINED | bordure verte (`#3F7B3F`) |
| Statut visuel DISCARDED | fond gris (`#F0F0F0`), texte 70% opacité |

---

## Contraintes de validation

(N/A côté frontend — la validation est côté backend SF-176-01. Le frontend respecte le contrat figé et n'envoie pas de statut non listé.)

---

## Technique

### Contrat API (importé de SF-176-01)

Voir `docs/features/F-176/SF-176-01-backend-pistes-strategiques.md` section "Contrat API" pour le détail.

```typescript
// frontend/src/app/core/models/strategic-option.model.ts
export type StrategicOptionStatus = 'TO_STUDY' | 'RETAINED' | 'DISCARDED';

export interface StrategicOption {
  id: string;
  texte: string;
  baseJuridique: string | null;
  horizonTemporel: string | null;
  conditions: string[];
  source: string | null;
  statut: StrategicOptionStatus;
  raisonDiscard: string | null;
  ordre: number;
  createdAt: string;
  updatedAt: string;
}
```

### Composants Angular

| Composant | Action |
|-----------|--------|
| `SynthesisComponent` | Enrichi (signal + 3 computed + 3 méthodes + template + SCSS) |
| `DiscardReasonDialogComponent` | Nouveau — composant standalone simple (template inline si possible) |
| `StrategicOptionService` | Nouveau — singleton `providedIn: 'root'` |
| `PdfExportService` | Étendu — méthode `exportStrategicOptions` |

### Endpoints consommés

- `GET /api/v1/case-files/{caseFileId}/analyses/{analysisId}/strategic-options`
- `PATCH /api/v1/strategic-options/{optionId}` (body `{statut, raisonDiscard?}`)

---

## Plan de test

### Tests unitaires Jest

- [ ] `StrategicOptionService.list calls correct URL`
- [ ] `StrategicOptionService.updateStatus calls correct URL with body`
- [ ] `SynthesisComponent.loadStrategicOptionsForVersion sets signal on success`
- [ ] `SynthesisComponent.loadStrategicOptionsForVersion clears signal on error`
- [ ] `SynthesisComponent.optionsToStudy/Retained/Discarded computed filter correctly`
- [ ] `SynthesisComponent.updateOptionStatus to RETAINED calls service`
- [ ] `SynthesisComponent.updateOptionStatus to DISCARDED opens dialog`
- [ ] `SynthesisComponent reset signal on version change`

### Tests E2E

(N/A — SF UI simple, couvert par tests Jest.)

### Smoke tests E2E concernés

- [ ] Aucun (pas de modification de routing / auth / workspace / navigation).

### Self-check grep pré-commit

Avant de pousser, exécuter et vérifier 0 résultat :

```bash
# Le bloc Pistes ne doit jamais utiliser MatDatepicker (pattern interdit projet)
grep -rn "MatDatepickerModule\|<mat-datepicker" frontend/src/app/case-files/synthesis/

# Aucun appel direct alert/confirm browser
grep -rnE "\\balert\\(|\\bconfirm\\(" frontend/src/app/case-files/synthesis/synthesis.component.ts

# Le service doit être providedIn: 'root'
grep -n "providedIn: 'root'" frontend/src/app/core/services/strategic-option.service.ts

# Le statut DISCARDED doit avoir le dialog raison
grep -n "DISCARDED" frontend/src/app/case-files/synthesis/synthesis.component.ts
```

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — la SF étend un composant existant sans modifier l'auth, le routing, ou les quotas.

### Smoke tests E2E concernés

- [x] Aucun smoke test E2E impacté.

---

## Dépendances

### Subfeatures bloquantes

- **SF-176-01** (backend pistes stratégiques) — Done (PR #709 mergée 2026-04-30, commit `0221a51b`). Le contrat API est figé.

### Subfeatures parallèles

(N/A — SF-176-01 backend est mergée avant SF-176-02.)

### Subfeatures débloquées

- **F-IM-21** SF-IM-21-02 — l'extension prompt critères binaires immigration peut suivre.

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` n'est tranchée par cette SF.

---

## Notes et décisions

- **Pourquoi pas un composant Angular dédié `app-strategic-options-section` ?** Cohérence F-96 — la checklist procédurale est aussi inline dans `SynthesisComponent`. Si dans 3+ vues on retrouve le bloc, on extraira en composant. À ce stade : une seule vue (synthèse).
- **Pourquoi `MatDialog` plutôt qu'un input inline pour la raison ?** UX plus claire — `confirm()` browser est interdit (CLAUDE.md règle frontend). Un input inline rendrait la liste visuellement bruyante, surtout quand 5+ pistes coexistent.
- **Pourquoi pas de `MatTabGroup` pour séparer les 3 statuts ?** Visibilité — l'avocat doit voir d'un coup d'œil ses pistes retenues vs écartées vs à étudier. Les tabs forceraient un clic supplémentaire.
- **Pourquoi le bloc est ouvert par défaut (`expanded`) ?** Pattern F-96 — la checklist procédurale est aussi `expanded`. Le contenu est important, on ne le cache pas par défaut.
- **Pas de F-IA-03** — décision F-176 backlog (les pistes sont par essence ouvertes).
- **Pourquoi l'icône `lightbulb` ?** Sémantique correcte (idée / option / opportunité) ; cohérent design system (existing icons).
