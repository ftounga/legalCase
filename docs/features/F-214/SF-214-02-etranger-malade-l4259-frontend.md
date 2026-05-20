# Mini-spec — F-214 / SF-214-02 — Étranger malade L. 425-9 — frontend

## Identifiant

`F-214 / SF-214-02`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-214-02-etranger-malade-l4259-frontend`

---

## Objectif

Livrer le composant Angular `<app-etranger-malade-section>` conforme au pattern canonique F-IA-04 pour l'outil `F-IM-25-etranger-malade-l4259-fr`, avec pré-fill IA depuis `ImmigrationExtractedData`, validation F-IA-03, entrée TOOL_REGISTRY et badge dashboard.

---

## Comportement attendu

### Cas nominal

- Composant `EtrangerMaladeSectionComponent` : standalone, OnPush, consomme l'endpoint POST/GET SF-214-01.
- Formulaire : `pathologiePrincipale` (textarea), `paysOrigine` (text), `traitementDisponiblePaysOrigine` (checkbox), `avisOFII` (select FAVORABLE/DEFAVORABLE/EN_ATTENTE), `dateAvisOFII` (date input, conditionnel si DEFAVORABLE).
- `prefillFromAi()` depuis `ImmigrationExtractedData` (champs SF-214-01).
- `coherenceAlerts` computed + `<app-coherence-popover-trigger>` pour chaque champ pré-rempli.
- Résultat affiché : verdict (chip couleur), chipsCriteresNonRemplis, delaiRecoursTA (JetBrains Mono), motifRecours (si DEFAVORABLE).
- Gate `workspaceCountry === 'FRANCE'` : bannière info si non FR.
- `CaseDashboardRefreshService.triggerRefresh()` dans `next:` du POST.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| POST échoue (400/404/500) | `MatSnackBar` message erreur |
| `aiData` absent ou null | Formulaire vide, badge pré-fill absent |

---

## Conformité F-IA-04

### 1. Cohérence visuelle

- [x] Palette statut : navy/or info, vert ELIGIBLE_PROBABLE, orange SOUS_RESERVE, rouge NON_ELIGIBLE
- [x] Datepicker : `<input type="date">`
- [x] Typographie : JetBrains Mono pour baseJuridique + délai TA ; Inter pour le reste
- [x] Gate workspaceCountry : bannière si ≠ FRANCE
- [x] Erreurs : MatSnackBar
- [x] Refresh dashboard : triggerRefresh() dans next:

### 2. Pré-fill IA

- [x] `@Input() aiData?: ImmigrationExtractedData`
- [x] `prefillFromAi()` dans ngOnInit() + ngOnChanges()
- [x] Signals provenance par champ pré-rempli
- [x] Badge auto_awesome par champ pré-rempli
- [x] Handlers onXxxChange() remettent provenance à null

### 3. Validation F-IA-03

- [x] `coherenceAlerts` computed
- [x] `CoherenceAlertBuilder` partagé
- [x] `<app-coherence-popover-trigger>` sur chaque champ
- [x] Hiérarchie sources respectée (F-96 > Question IA > IA détection > Pièce manquante)

### 4. TOOL_REGISTRY + getPrefillCount

- [x] Entrée `F-IM-25-etranger-malade-l4259-fr` dans TOOL_REGISTRY
- [x] `static getPrefillCount(input)` cohérent avec `prefillFromAi()` runtime
- [x] Tests Jest : 0 champs / cas partiel / cas nominal

### 5. Parité domaines

- Niveau de l'outil : 5 (scoring/analyse de validité)
- Travail FR : non pertinent (L. 425-9 est immigration)
- Famille FR : non pertinent
- Immigration BE : régime distinct — F-220 P3

---

## Champs IA à extraire

Identiques à SF-214-01 (voir tableau champs IA).

---

## Critères d'acceptation

- [x] Composant compile, BUILD SUCCESS, 0 erreur TypeScript
- [x] Formulaire pré-rempli depuis ImmigrationExtractedData (champs SF-214-01)
- [x] POST nominal → verdict affiché + dashboard refresh
- [x] POST 400 → MatSnackBar
- [x] Gate country France affiche bannière si workspace BE
- [x] coherenceAlerts affiché via popover-trigger
- [x] Tests Jest ≥ 15 (composant + service + prefill-rules + getPrefillCount)
- [x] `F-IM-25-etranger-malade-l4259-fr` dans TOOL_REGISTRY + KNOWN_FRONTEND_TOOL_IDS

## Plan de test minimal

- **Jest** : `etranger-malade-section.component.spec.ts` (≥ 10 tests), `etranger-malade.service.spec.ts` (≥ 5 tests), `etranger-malade-prefill-rules.spec.ts` (≥ 3 tests getPrefillCount)
- **CI** : npm run build BUILD SUCCESS

## Tables / endpoints / composants impactés

- **Nouveau composant** `EtrangerMaladeSectionComponent` (standalone, OnPush)
- **Nouveau service** `EtrangerMaladeService`
- **Nouveau modèle** `EtrangerMaladeAnalysis` (DTO frontend)
- **Nouveau fichier** `etranger-malade-prefill-rules.ts`
- **Modification** `decisional-tools-panel.component.ts` : ajout entrée TOOL_REGISTRY `F-IM-25-etranger-malade-l4259-fr`

## Hors périmètre

- Backend (SF-214-01)
- Génération automatique de requête TA (F-IM-06 existant)

## Dépendances

- SF-214-01 : statut `done` (backend + migration + `KNOWN_FRONTEND_TOOL_IDS`)
- F-246 SF-246-04/18 : champs `ImmigrationExtractedData` pré-fill étranger malade livrés
