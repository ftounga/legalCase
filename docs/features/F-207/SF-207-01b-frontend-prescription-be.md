# Mini-spec — F-207 / SF-207-01b-frontend Outil prescription Travail BE (UI)

## Identifiant

`F-207 / SF-207-01b-frontend`

## Feature parente

`F-207` — P1 Travail BE — 8 outils urgences BE-only

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-207-01b-frontend-prescription-be`

## Cadrages amont (étapes 0 et 0 bis)

Produits dans la PR backend (#1119) — `docs/features/F-207/SF-207-00-coherence.md` (GO) et `docs/features/F-207/SF-207-00b-ux-coherence.md` (GO avec ajustements). Pas re-cadrés ici.

---

## Objectif

Livrer la section frontend décisionnelle du calculateur de prescription Travail BE (consommant le backend SF-207-01 livré). Conforme au pattern canonique F-IA-04 (`immigration-title-decision-section`), exposée **uniquement aux workspaces BELGIQUE / DROIT_DU_TRAVAIL**, visibility **ALWAYS_ON**.

---

## Contrat API consommé (figé en SF-207-01 backend)

- `POST /api/v1/case-files/{caseFileId}/decision-tools/prescription-be-litige-travail`
- `GET` du même path
- Body : `{ dateRupture: string ISO, typeCreance: enum, dateActionEnvisagee?: string ISO }`
- Réponse 200 : `{ verdict: 'PRESCRIT'|'IMMINENT'|'NON_PRESCRIT', dateLimitePrescription: string, joursRestants: number, regleAppliquee: string, baseJuridique: string, formuleCalcul: string }`
- 404 si workspace FR ou case_file hors workspace.

`typeCreance` enum : `EX_CONTRAT_GENERAL` | `EX_CONTRAT_CCT_109` | `PENDANT_CONTRAT` | `ARRIERES_SALAIRE`.

---

## Comportement attendu

### Section composant

`prescription-be-litige-travail-section.component` (sous `frontend/src/app/case-files/`) — formulaire + verdict, conforme au pattern `immigration-title-decision-section` :
- `@Input() caseFileId`, `workspaceCountry`, `aiData?: TravailExtractedData`, `procedureChecks?`, `aiQuestions?`, `piecesManquantes?`.
- Pré-fill IA via `prefillFromAi()` invoqué `ngOnInit()` + `ngOnChanges()` — signals `provenanceDateRupture`, `provenanceTypeCreance` à `'IA'` ou `null`.
- Badge `auto_awesome` « Pré-rempli depuis l'analyse » à côté de chaque champ pré-rempli.
- Handlers `onDateRuptureChange()` / `onTypeCreanceChange()` remettent la provenance à `null` à la modification.
- Validation F-IA-03 — `coherenceAlerts = computed()` qui produit une alerte par champ quand la valeur diverge des 4 sources IA (`aiData`, `procedureChecks` F-96, `aiQuestions`, `piecesManquantes`). Directive `<app-coherence-popover-trigger>`. Helper partagé `CoherenceAlertBuilder`.
- Bouton « Calculer la prescription » → `POST` puis affichage du verdict (badge couleur : rouge `PRESCRIT`, ambre `IMMINENT`, vert `NON_PRESCRIT`) + `dateLimitePrescription` + `joursRestants` + `regleAppliquee` + `baseJuridique` (en `JetBrains Mono`) + `formuleCalcul`.
- `MatSnackBar` pour erreurs (pas d'`alert()`). `MatDatepicker` interdit — utiliser `<input type="date">`.
- Refresh dashboard décisionnel : `CaseDashboardRefreshService.triggerRefresh()` dans le `next:` du POST (pattern SF-IA-02-03).

### Pré-fill rules (`prescription-be-litige-travail-section-prefill-rules.ts`)

Pattern symétrique aux autres outils. Champs et sources :

| Champ | Source pré-fill IA | Règle |
|---|---|---|
| `dateRupture` | `aiData.dateRuptureContrat` (ajouté par SF-207-01 backend dans `TravailExtractedData`) | `isoDateOrNull(value)` |
| `typeCreance` | `aiData.motifRupture` (mapping) | si `motifRupture` ∈ {`LICENCIEMENT`, `DEMISSION`, `FAUTE_GRAVE`, `RCC`, `RUPTURE_AMIABLE`} → `EX_CONTRAT_GENERAL` ; sinon `null` |

`getPrefillCount(input)` retourne 0, 1 ou 2 selon les champs pré-remplissables — garantir **parité stricte** avec `prefillFromAi()` runtime (test obligatoire).

### Entrée TOOL_REGISTRY

Dans `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` (ou le fichier où est défini `TOOL_REGISTRY` — l'agent vérifie) :
- `tool_id` : `prescription-be-litige-travail`.
- `inputs: (ctx) => ({ caseFileId: ctx.caseFileId, workspaceCountry: ctx.workspaceCountry, aiData: ctx.aiData, procedureChecks: ctx.procedureChecks, aiQuestions: ctx.aiQuestions, piecesManquantes: ctx.piecesManquantes })`.
- Constantes `TOOL_LABEL` / `TOOL_ICON` symétriques aux autres outils.
- Ordre : **en tête de la séquence Travail BE** (transversal P1, cf. étape 0 bis).
- `KNOWN_FRONTEND_TOOL_IDS` (test d'intégrité F-164) : ajouter `prescription-be-litige-travail` à la liste pour que `DecisionToolVisibilityIntegrityIT` reste vert.

### Visibility seed (migration backend incluse dans cette SF)

Migration Liquibase `XXX-add-prescription-be-litige-travail-visibility.xml` (prochain numéro disponible) :
- INSERT dans `decision_tool_visibility_rules` :
  - `tool_id = 'prescription-be-litige-travail'`
  - `country = 'BELGIQUE'`
  - `legal_domain = 'DROIT_DU_TRAVAIL'`
  - `visibility = 'ALWAYS_ON'` (transversal P1)
  - `trigger_field = NULL`
  - `trigger_value = NULL`

**Note** : la migration backend est incluse dans cette SF frontend pour respecter la mémoire `feedback_pre_merge_visibility_seed_check` — TOOL_REGISTRY frontend, `KNOWN_FRONTEND_TOOL_IDS` et `decision_tool_visibility_rules` doivent **lander en un seul commit** pour que le garde-fou d'intégrité ne casse jamais.

### Cas d'erreur

| Situation | UI |
|---|---|
| 404 (workspace FR) | Outil masqué via gate `workspaceCountry === 'BELGIQUE'` côté composant — ne devrait pas arriver. Si arrive : `MatSnackBar` « Outil indisponible pour ce workspace » |
| 404 (case_file autre workspace) | `MatSnackBar` « Dossier introuvable » |
| 400 (validation Bean) | Erreur affichée sous le champ concerné via `mat-error` |
| Réseau / 500 | `MatSnackBar` rouge « Une erreur est survenue. Veuillez réessayer. » |

---

## Conformité F-IA-04 — auto-checklist

- [x] Palette : navy/or info, vert OK, rouge **réservé** `PRESCRIT` (= critique).
- [x] `<input type="date">` pour `dateRupture` et `dateActionEnvisagee` (PAS `MatDatepicker`).
- [x] `JetBrains Mono` pour `baseJuridique` et `formuleCalcul`. `Inter` pour le reste.
- [x] Gate `workspaceCountry === 'BELGIQUE'` — si mismatch, bannière info explicite, pas de masquage silencieux. (Ici en pratique : la visibility ALWAYS_ON BELGIQUE filtre déjà le rendu.)
- [x] Erreurs via `MatSnackBar` — pas d'`alert()`.
- [x] Refresh dashboard `CaseDashboardRefreshService.triggerRefresh()` dans `next:` du POST de validation.
- [x] Pré-fill IA (`prefillFromAi()` + signals provenance + badges).
- [x] Validation F-IA-03 (`coherenceAlerts` + `<app-coherence-popover-trigger>` + `CoherenceAlertBuilder` partagé).
- [x] `getPrefillCount(input)` static, parité stricte avec `prefillFromAi()`.
- [x] `tool_id` ajouté à `KNOWN_FRONTEND_TOOL_IDS` du test d'intégrité.

---

## Critères d'acceptation

- [ ] Section composant rend formulaire + verdict ; gate `workspaceCountry === 'BELGIQUE'` strict.
- [ ] Pré-fill IA fonctionne : `dateRuptureContrat` + mapping `motifRupture → typeCreance` renseignent les champs, badge « Pré-rempli depuis l'analyse » visible.
- [ ] Modification manuelle d'un champ pré-rempli → provenance → `null`, badge disparaît.
- [ ] `getPrefillCount(input)` retourne le bon nombre (tests : 0 champs / 1 champ partiel / 2 champs cas nominal).
- [ ] Validation F-IA-03 : si l'avocat saisit une `dateRupture` différente de `aiData.dateRuptureContrat` → alerte de divergence affichée.
- [ ] Bouton « Calculer » → POST 200 → verdict affiché avec badge couleur correct (PRESCRIT rouge / IMMINENT ambre / NON_PRESCRIT vert).
- [ ] `MatSnackBar` sur erreur réseau.
- [ ] `CaseDashboardRefreshService.triggerRefresh()` appelé sur succès POST.
- [ ] Entrée `TOOL_REGISTRY` ordonnée en tête de la séquence Travail BE.
- [ ] `KNOWN_FRONTEND_TOOL_IDS` mis à jour — `DecisionToolVisibilityIntegrityIT` reste vert.
- [ ] Migration backend visibility ALWAYS_ON / BELGIQUE / DROIT_DU_TRAVAIL appliquée.

---

## Périmètre

### Hors scope

- Backend (livré par SF-207-01).
- Autres outils F-207 (vagues 2-8).
- Gating CONTEXTUAL — cet outil est ALWAYS_ON (transversal P1).

---

## Plan de test (Jest)

- [ ] `prescription-be-litige-travail-section-prefill-rules.spec.ts` — 5 tests : 0 champs / 1 partiel / 2 nominal / mapping `motifRupture` complet / mapping motif inconnu.
- [ ] `prescription-be-litige-travail-section.component.spec.ts` — au moins : rendu sans `workspaceCountry=BELGIQUE`, pré-fill effectif, badge provenance, calcul → verdict affiché, refresh dashboard appelé, snackbar sur erreur.
- [ ] `DecisionToolVisibilityIntegrityIT` (backend) reste vert.

---

## Technique

### Composants Angular

- `frontend/src/app/case-files/prescription-be-litige-travail-section/prescription-be-litige-travail-section.component.{ts,html,scss,spec.ts}`
- `frontend/src/app/case-files/prescription-be-litige-travail-section/prescription-be-litige-travail-section-prefill-rules.{ts,spec.ts}`

### Backend (migration visibility uniquement)

- `backend/src/main/resources/db/changelog/migrations/XXX-add-prescription-be-litige-travail-visibility.xml` (prochain numéro)

### Modèle

- `frontend/src/app/core/models/prescription-be-litige-travail.model.ts` (DTO request/response/result + enum `TypeCreance`).

---

## Analyse d'impact

### Préoccupations transversales

- [x] **Outil décisionnel métier** — création d'un outil ; invariant « un outil = une situation métier » respecté.
- [x] Auth / Workspace / Plans / Navigation — non touchés.

### Composants impactés

| Composant | Impact | Test de non-régression |
|---|---|---|
| `decisional-tools-panel` `TOOL_REGISTRY` | Ajout d'une entrée | Spec panel : nouvel outil visible BE+TRAVAIL |
| `KNOWN_FRONTEND_TOOL_IDS` (test backend) | Ajout de `prescription-be-litige-travail` | `DecisionToolVisibilityIntegrityIT` vert |
| `decision_tool_visibility_rules` (migration) | INSERT 1 ligne | `DecisionToolVisibilityIntegrityIT` vert |

---

## Dépendances

- SF-207-01 backend — mergée (PR #1119, master `660e6579`).
