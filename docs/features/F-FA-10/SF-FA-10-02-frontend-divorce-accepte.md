# Mini-spec — F-FA-10 / SF-FA-10-02 Divorce accepté FR — FRONTEND

## Identifiant
`F-FA-10 / SF-FA-10-02`

## Feature parente
`F-FA-10` — Divorce accepté / acceptation du principe de la rupture (art. 233 Cciv)

## Statut
`ready`

## Date de création
2026-04-25

## Branche Git
`feat/SF-FA-10-02-frontend-divorce-accepte`

---

## Objectif

Livrer le composant Angular `<app-divorce-accepte-section>` (FRANCE uniquement, droit famille) qui consomme l'API SF-FA-10-01 et affiche le verdict d'éligibilité, le délai de procédure prévisionnel (~10 mois) et la fourchette de prestation compensatoire art. 233-234 Cciv.

---

## Comportement attendu

### Cas nominal

1. Mount du composant dans le panel décisionnel F-IA-04 quand `tool_id=F-FA-10-divorce-accepte` est ALWAYS_ON pour FRANCE+DROIT_FAMILLE.
2. `ngOnInit()` lance `GET /api/v1/case-files/{id}/divorce-accepte`.
3. Si 200 : restaure les inputs persistés + le résultat (verdict ELEVEE/FAIBLE, délais, fourchette).
4. Si 404 : reste en mode formulaire, tente un pré-fill IA depuis `aiData` (FamilleExtractedData minimaliste).
5. L'avocat saisit (slide-toggles + numériques + dates), clique "Calculer".
6. `POST /api/v1/case-files/{id}/divorce-accepte` → réponse affichée + `CaseDashboardRefreshService.triggerRefresh()`.
7. Verdict binaire : bannière navy DISPONIBLE si `ELEVEE`, bannière rouge classique (`--danger`) si `FAIBLE`. Pas de palette dominante rouge (urgence non-critique).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `workspaceCountry !== 'FRANCE'` | Bannière info "outil FR uniquement, pour BE voir F-FA-11 à venir" + form masqué | n/a |
| Form invalide (champs requis manquants) | Bouton Calculer disabled + pas de POST | n/a |
| Backend 400 (validation) | MatSnackBar rouge avec message backend | 400 |
| Backend 404 sur GET | Reste en mode form, pas d'erreur visible | 404 |
| Backend 500 | MatSnackBar "Erreur lors du calcul" | 500 |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** : F-FA-08 alteration / F-FA-09 faute (autres divorces contentieux). Pattern outil = situation métier respecté — pas de fusion. Backend séparé déjà mergé.
- [x] **Autres pays** : Pas d'équivalent BE direct (procédures BE différentes, cf. F-FA-11 backlog). Bannière BE explicite.
- [x] **Autres domaines** : Pattern transversal des outils décisionnels famille — déjà partagé (CaseDashboardRefreshService, MatSnackBar, CoherencePopover).
- [x] **Autres UI patterns** : Même squelette que `harcelement-licenciement-nul-section` (template canonique 2026-04-24) + `inaptitude-section` (multi-fields + slide-toggles).
- [x] **Autres flows transversaux** : pas de touche auth / workspace / plans / navigation au-delà du contexte panel F-IA-04.

### Cas spécifique : outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : oui — alertes `coherenceAlerts` computed sur `dureeMariage`, `revenusEpoux1`, `revenusEpoux2`, `patrimoineCommun`, `dateAcceptationPV` (via `CoherenceAlertBuilder`). Trigger popover (`CoherencePopoverTriggerDirective`).
- [x] **Refresh dashboard (F-IA-02)** : oui — `dashboardRefresh?.triggerRefresh()` dans le `next:` du POST.
- [x] **Pré-remplissage IA** : oui — `prefillFromAi()` lit `aiData?: FamilleExtractedData | null` (champs ouverts par cette SF, no-op gracieux si absents).
- [x] **Persistance** : déjà gérée backend (table `divorce_accepte_analyses`).
- [x] **Masquage conditionnel** : tool_id ALWAYS_ON FR+DROIT_FAMILLE → masquage géré par F-IA-04 (panel). Côté front, gate supplémentaire workspaceCountry pour bannière BE.
- [x] **Alertes actives après calcul** : `coherenceAlerts` gate sur `!this.showForm()` (pas de `|| result()`).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-FA-08 / F-FA-09 (sections jumelles à venir) | Oui | SF parallèles déjà tracées (SF-FA-08-02 / SF-FA-09-02 dans le batch SPRINT-LONG) |
| F-FA-11 (équivalent BE divorce accepté) | Non — n'existe pas en droit BE | Bannière info BE qui mentionne F-FA-11 |
| Pré-fill IA `FamilleExtractedData` | Oui (nouveau type frontend) | Création d'une interface minimale dans `case-analysis.model.ts` (champs optionnels, no-op si absents) |
| Composant partagé `divorce-accepte-section` | Spécifique F-FA-10 | Pas de réutilisation prévue (pattern situation métier) |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [x] Subfeature(s) parallèle(s) tracée(s) : SF-FA-08-02 / SF-FA-09-02 (autres divorces FR)
- [x] Backlog : F-FA-11 BE divorce accepté équivalent (déjà tracé V8)

---

## Critères d'acceptation

- [x] Composant `app-divorce-accepte-section` standalone, intégré au panel F-IA-04 via TOOL_REGISTRY (entrée documentée ci-dessous).
- [x] `GET` au mount : 200 → restaure form+résultat ; 404 → form vide + tentative pré-fill IA.
- [x] Form valid si `acceptationPrincipeSignee=true` (sinon UX info), `dureeMariageAnnees ≥ 0`, `revenusAnnuels{1,2}Eur ≥ 0`. Dates optionnelles.
- [x] `POST` envoie le payload exact attendu par DivorceAccepteRequest et restitue la réponse.
- [x] Verdict ELEVEE → bannière navy `DISPONIBLE`, FAIBLE → bannière rouge classique `--danger` (palette standard, pas dominante).
- [x] Délai procédure ~10 mois en JetBrains Mono. Prestation compensatoire min-max formattée. Formule + baseJuridique en JetBrains Mono.
- [x] `messages` rendus en `<ul>` avec `LegalCitationsPipe` (articles cités en `<code>`).
- [x] Pré-fill IA via `aiData?: FamilleExtractedData | null` : `dureeMariageAnnees`, `revenusAnnuelsEpoux1Eur`, `revenusAnnuelsEpoux2Eur`, `patrimoineCommun`, `dateAcceptationPV`. No-op gracieux si champ absent. Badge "Pré-rempli depuis l'analyse" + signal `provenance<Field>`. Effacement au `onXxxChange()`.
- [x] Coherence alerts F-IA-03 sur `dureeMariage`, `revenusEpoux1`, `revenusEpoux2`, `patrimoineCommun`, `dateAcceptationPV` (computed `coherenceAlerts` + `CoherencePopoverTriggerDirective`). Gate `!showForm()`.
- [x] Gate FR : si `workspaceCountry !== 'FRANCE'`, bannière info "Outil FR uniquement — voir F-FA-11 (BE) à venir" + form masqué.
- [x] `CaseDashboardRefreshService.triggerRefresh()` après POST succès.
- [x] MatSnackBar pour erreurs (panel rouge `snack-error`).
- [x] Ne modifie pas `decisional-tools-panel.component.ts` ni `docs/PRODUCT_SPEC.md` (livré dans une SF parallèle d'intégration).
- [x] Tests Karma ≥ 10 : mount, form valid, POST ok, POST error snackbar, GET 200 restore, GET 404 form, prefill IA, onChange efface badge IA, coherence alert > 0, gate BE bannière, gate FR form visible, toggle collapse, verdict ELEVEE banner, verdict FAIBLE banner.

---

## Périmètre

### Hors scope

- Modification de `decisional-tools-panel.component.ts` (TOOL_REGISTRY) — sera intégrée dans une SF d'agrégation F-FA (cf. note ci-dessous).
- Modification de `docs/PRODUCT_SPEC.md` — sera faite post-merge.
- Backend (déjà livré PR #514).
- Type backend `FamilleExtractedData` (frontend-only minimal interface, sera enrichie quand le backend famille sera étendu).
- Pré-fill `dateAssignation` (champ avocat, pas IA).

---

## Contrat API (importé de SF-FA-10-01)

POST + GET `/api/v1/case-files/{caseFileId}/divorce-accepte`

Request : `{ acceptationPrincipeSignee: boolean, dateAcceptationPV?: ISO date, dureeMariageAnnees: number, revenusAnnuelsEpoux1Eur: number, revenusAnnuelsEpoux2Eur: number, patrimoineCommun: boolean, dateAssignation?: ISO date }`

Response : voir `DivorceAccepteResponse.java` lignes 8-30.

---

## Technique

### Composants Angular

- `frontend/src/app/core/models/divorce-accepte.model.ts` — types Request/Response + interface `FamilleExtractedData` minimale.
- `frontend/src/app/core/services/divorce-accepte.service.ts` — wrapper HTTP `calculate()` + `get()`.
- `frontend/src/app/case-files/divorce-accepte-section/` — composant standalone + spec ≥ 10 tests.

### TOOL_REGISTRY (à documenter — pas modifié dans cette SF)

```typescript
// Ajouter dans decisional-tools-panel.component.ts (SF d'agrégation parallèle ou suivante) :
import { DivorceAccepteSectionComponent } from '../divorce-accepte-section/divorce-accepte-section.component';
// dans TOOL_REGISTRY :
['F-FA-10-divorce-accepte', {
  component: DivorceAccepteSectionComponent,
  inputs: (ctx) => ({
    caseFileId: ctx.caseFileId,
    workspaceCountry: ctx.workspaceCountry,
    aiData: ctx.synthesis?.familleExtractedData,
    procedureChecks: ctx.procedureChecks,
    aiQuestions: ctx.aiQuestions,
    piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
  }),
}],
```

---

## Plan de test

### Tests unitaires (Jasmine + HttpClientTestingModule)

1. mount + workspaceCountry FRANCE par défaut.
2. GET 200 → restore inputs + result + showForm=false.
3. GET 404 → reste en form + prefill IA si aiData fourni.
4. formValid : exige acceptationPrincipeSignee=true + dureeMariageAnnees ≥ 0 + revenus ≥ 0.
5. POST OK → result rendu + snackbar succès + triggerRefresh appelé.
6. POST 400 → snackbar erreur rouge.
7. prefillFromAi : aiData complet → 5 fields pré-remplis + badges IA.
8. onDureeMariageChange efface badge IA.
9. coherenceAlerts.DUREE_MARIAGE présent si divergence > 1 an entre IA et avocat.
10. workspaceCountry=BELGIQUE → bannière BE + form masqué (pas d'appel HTTP).
11. workspaceCountry=FRANCE → form visible.
12. toggleCollapse / editMode (non-régression).
13. Verdict ELEVEE/FAIBLE : classes CSS appliquées sur la bannière.

### Isolation workspace

- Non applicable côté frontend — l'isolation est portée par le backend (déjà couverte SF-FA-10-01).

---

## Analyse d'impact

### Préoccupations transversales

- [x] **Aucune préoccupation transversale** — outil décisionnel isolé, pas de modification d'auth / workspace / plans / routing.

### Smoke tests E2E

- [x] Aucun smoke test concerné — l'outil est un nouveau composant non monté par défaut (mounting via panel F-IA-04 selon visibility rule ALWAYS_ON FR DROIT_FAMILLE — déjà couvert par les tests d'intégration backend).

---

## Dépendances

### Subfeatures bloquantes

- SF-FA-10-01 — `done` (PR #514 mergée 2026-04-24)

### Questions ouvertes impactées

- Aucune.

---

## Notes et décisions

- Pattern de référence : `harcelement-licenciement-nul-section` (template canonique 2026-04-24, cf. `ai-skills/frontend-coherence-audit.md`).
- Pré-fill IA via interface frontend-only `FamilleExtractedData` minimaliste : permet à la SF d'être livrée sans dépendre d'une extension backend du pipeline IA. No-op gracieux quand champs absents.
- Verdict binaire ELEVEE/FAIBLE (pas de MOYENNE) — palette standard navy/rouge classique (pas de gradation `--danger-medium/-strong/-dark` car pas d'urgence < 72h).
- L'entrée TOOL_REGISTRY est documentée ici mais non appliquée — sera intégrée par la SF F-FA-XX-XX d'agrégation panel (en attente du regroupement des composants F-FA-08/09/10 frontend).
