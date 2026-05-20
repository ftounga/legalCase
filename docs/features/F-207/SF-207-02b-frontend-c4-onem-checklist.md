# Mini-spec — F-207 / SF-207-02b-frontend Outil C4 ONEM checklist (UI)

## Identifiant

`F-207 / SF-207-02b-frontend` · Statut : `ready` · Date : 2026-05-20 · Branche : `feat/SF-207-02b-frontend-c4-onem-checklist`

## Cadrages amont

Étape 0 / 0 bis F-207 livrées dans #1119. Pas re-cadrés ici. Pattern canonique F-IA-04 : `immigration-title-decision-section` (réf. SF-207-01b).

## Objectif

Section frontend décisionnelle de la checklist C4 ONEM (consommant backend SF-207-02, PR #1123). Affiche un formulaire de 10 champs C4, calcule la conformité, affiche le verdict coloré + la liste des mentions manquantes + un encart « lettre rectificative » copiable. BE-only strict.

## Contrat API consommé (figé en SF-207-02 backend)

- `POST /api/v1/case-files/{caseFileId}/decision-tools/c4-onem-checklist`
- `GET` du même path
- Body POST :
  ```ts
  {
    raisonSocialeEmployeur?: string | null;
    numeroBce?: string | null;        // 10 chiffres si fourni
    nomSalarie: string;                // requis
    numeroNationalRegistre?: string | null;
    dateEntreeService: string;         // ISO date, requis
    dateSortieService: string;         // ISO date, requis, ≥ dateEntreeService
    categorieOnem?: string | null;
    motifExplicite?: string | null;
    fauteGraveMentionnee: boolean;     // requis
    preavisPresteJours?: number | null;
    dernierSalaireMensuelBrut?: number | null;
  }
  ```
- Réponse 200 :
  ```ts
  {
    verdict: 'CONFORME' | 'NON_CONFORME' | 'RISQUE_EXCLUSION_FAUTE_GRAVE';
    mentionsManquantes: string[]; // enum C4OnemChecklistMention
    fauteGraveDetectee: boolean;
    exclusionOnemRange: { minSemaines: number, maxSemaines: number } | null;
    lettreRectificativeProposee: string | null;
    baseJuridique: string;
    etapeSuivante: 'CONTESTATION_C4' | 'RECTIFICATION_AUPRES_EMPLOYEUR' | 'AUCUNE';
    // + inputs persistés dans la réponse pour pré-fill au GET
  }
  ```
- 404 si workspace FR ou case_file hors workspace.

## Comportement

Section composant `c4-onem-checklist-section.component` (sous `frontend/src/app/case-files/c4-onem-checklist-section/`) — pattern F-IA-04 :
- Inputs Angular : `caseFileId`, `workspaceCountry`, `aiData? : TravailExtractedData`, `procedureChecks?`, `aiQuestions?`, `piecesManquantes?`.
- **Pré-fill IA** via `prefillFromAi()` dans `ngOnInit()` + `ngOnChanges()`. Champs et sources :

| Champ formulaire | Source `aiData` | Provenance signal |
|---|---|---|
| `raisonSocialeEmployeur` | `aiData.raisonSocialeEmployeur` | `provenanceRaisonSociale` |
| `numeroBce` | `aiData.numeroBce` (10 chiffres) | `provenanceNumeroBce` |
| `nomSalarie` | nom du salarié si extrait, sinon vide | `provenanceNomSalarie` |
| `dateEntreeService` | `aiData.dateEntree` (champ existant FR/BE) | `provenanceDateEntree` |
| `dateSortieService` | `aiData.dateRuptureContrat` (livré SF-207-01) | `provenanceDateSortie` |
| `categorieOnem` | `aiData.categorieOnem` | `provenanceCategorieOnem` |
| `motifExplicite` | `aiData.motifExplicite` ou `aiData.motifRupture` | `provenanceMotif` |
| `fauteGraveMentionnee` | dérivé : `aiData.motifRupture` contient « faute grave » (substring case-insensitive, accents normalisés) | `provenanceFauteGrave` |
| `preavisPresteJours` | `aiData.preavisPresteJours` | `provenancePreavis` |
| `dernierSalaireMensuelBrut` | `aiData.dernierSalaireMensuelBrut` (BigDecimal côté backend → number côté frontend) | `provenanceSalaire` |

- Handler `onXxxChange()` par champ pré-rempli → remet la provenance à `null`.
- Badge `auto_awesome` « Pré-rempli depuis l'analyse » à côté de chaque champ pré-rempli.
- **Validation F-IA-03** : `coherenceAlerts = computed()` produit une alerte par champ quand la valeur diverge des 4 sources IA (`aiData`, `procedureChecks` F-96, `aiQuestions`, `piecesManquantes`). Directive `<app-coherence-popover-trigger>`. Helper partagé `CoherenceAlertBuilder`.
- Bouton « Vérifier la conformité du C4 » → `POST` puis affichage :
  - Badge verdict coloré : **vert** `CONFORME` / **ambre** `NON_CONFORME` / **rouge** `RISQUE_EXCLUSION_FAUTE_GRAVE`.
  - Si `mentionsManquantes` non vide : liste à puces des mentions humanisées (libellés FR par mention).
  - Si `RISQUE_EXCLUSION_FAUTE_GRAVE` : encart d'alerte rouge avec `exclusionOnemRange.minSemaines`-`maxSemaines` semaines + lien/texte « Contester le C4 → SF-207-03 ». Note : SF-207-03 pas encore livré, le lien renvoie pour l'instant vers l'outil prescription par sécurité (à mettre à jour quand SF-207-03 livré).
  - Si `lettreRectificativeProposee` non null : `<mat-card>` avec `<textarea readonly>` + bouton « Copier la lettre » (clipboard API).
  - `baseJuridique` en `JetBrains Mono`.
- `MatSnackBar` pour erreurs (pas d'`alert()`). `<input type="date">` (pas `MatDatepicker`).
- Refresh dashboard : `CaseDashboardRefreshService.triggerRefresh()` dans le `next:` du POST.

### Pré-fill rules — fichier `c4-onem-checklist-section-prefill-rules.ts`

`getPrefillCount(input)` static — pattern symétrique aux autres outils. Compte les champs effectivement pré-remplissables depuis `aiData`. Parité stricte avec `prefillFromAi()` runtime (tests obligatoires).

Mapping `fauteGraveMentionnee` :
- Si `aiData.motifRupture` (après upper-case + suppression accents) contient `'FAUTE GRAVE'` → `true`.
- Sinon → laissé indéterminé (pas de provenance). Le boolean form-control par défaut est `false` mais sans badge.

### Entrée TOOL_REGISTRY

Dans `decisional-tools-panel.component.ts` :
- `tool_id` : `c4-onem-checklist`
- Insertion après `prescription-be-litige-travail` (séquence métier Travail BE : prescription d'abord, C4 ensuite — cf. étape 0 bis F-207).
- `inputs:` standard : `caseFileId, workspaceCountry, aiData, procedureChecks, aiQuestions, piecesManquantes`.
- `TOOL_LABEL`, `TOOL_ICON`, `THEME_BY_TOOL_ID` (thème `DOCUMENTS` ou `RUPTURE` — à arbitrer en cohérence avec les autres outils Travail).
- `getPrefillCount` exposé statiquement sur le composant (badge panel).

### Visibility seed (migration backend incluse pour cohérence)

Migration `XXX-add-c4-onem-checklist-visibility.xml` (prochain numéro après 254) :
- INSERT `decision_tool_visibility_rules` : `tool_id='c4-onem-checklist'`, `country='BELGIQUE'`, `legal_domain='DROIT_DU_TRAVAIL'`, `layer='ALWAYS_ON'`, `trigger_field=NULL`, `trigger_value=NULL`.
- Justification ALWAYS_ON (vs CONTEXTUAL) : virtuellement chaque dossier de rupture BE Travail implique un C4 ; pas de trigger boolean disponible dans `TravailExtractedData` qui couvre précisément « C4 reçu ». ALWAYS_ON BE / DROIT_DU_TRAVAIL = visible par défaut pour les avocats BE Travail, pas pour les autres.

## Conformité F-IA-04 (auto-checklist)

- [x] Palette navy/or info, vert OK, rouge **réservé** `RISQUE_EXCLUSION_FAUTE_GRAVE` (= critique).
- [x] `<input type="date">` (PAS `MatDatepicker`).
- [x] `JetBrains Mono` pour `baseJuridique` et la `lettreRectificativeProposee`.
- [x] Gate `workspaceCountry === 'BELGIQUE'` — bannière info si mismatch.
- [x] Erreurs via `MatSnackBar`.
- [x] Refresh dashboard `CaseDashboardRefreshService.triggerRefresh()` dans `next:`.
- [x] Pré-fill IA + provenance + badges + handlers.
- [x] Validation F-IA-03 `coherenceAlerts` + popover trigger.
- [x] `getPrefillCount(input)` static, parité stricte.
- [x] Entrée TOOL_REGISTRY symétrique.

## Critères d'acceptation

- [ ] Section rend formulaire 10 champs + verdict ; gate `workspaceCountry === 'BELGIQUE'` strict.
- [ ] Pré-fill IA fonctionne sur les 10 champs pré-remplissables (badges visibles).
- [ ] Modification manuelle d'un champ pré-rempli → provenance → `null`, badge disparaît.
- [ ] `getPrefillCount` retourne 0, partial, et complet selon `aiData` — tests obligatoires.
- [ ] Validation F-IA-03 : valeur saisie divergente d'`aiData` → popover de divergence.
- [ ] Verdict `CONFORME` rend badge vert ; `NON_CONFORME` rend badge ambre + liste mentions manquantes ; `RISQUE_EXCLUSION_FAUTE_GRAVE` rend badge rouge + alerte 4-52 semaines + renvoi contestation.
- [ ] `lettreRectificativeProposee` non null → encart textarea readonly + bouton copier (clipboard API).
- [ ] `MatSnackBar` sur erreur réseau ; refresh dashboard appelé sur succès.
- [ ] `tool_id` ajouté à `TOOL_REGISTRY` après `prescription-be-litige-travail` ; `DecisionToolVisibilityIntegrityIT` reste vert.
- [ ] Migration backend visibility ALWAYS_ON / BELGIQUE / DROIT_DU_TRAVAIL appliquée.

## Périmètre / Hors scope

- Backend (livré #1123).
- Contestation C4 (SF-207-03 — outil distinct).
- Génération PDF/Word de la lettre (texte brut copiable suffit).
- Autres outils F-207 (vagues 3-8).

## Plan de test (Jest)

- [ ] `c4-onem-checklist-section-prefill-rules.spec.ts` — 0 champs / 1 partiel / 5 partiels / 10 complets / mapping faute grave avec/sans accents (5 tests min).
- [ ] `c4-onem-checklist-section.component.spec.ts` — rendu, pré-fill effectif, badges provenance, calcul → verdict (3 verdicts), liste mentions humanisées, lettre rectificative copiable, refresh dashboard, snackbar sur erreur (8+ tests).
- [ ] `DecisionToolVisibilityIntegrityIT` (backend) reste vert.

## Composants à créer / modifier

À créer sous `frontend/src/app/case-files/c4-onem-checklist-section/` :
- `c4-onem-checklist-section.component.{ts,html,scss,spec.ts}`
- `c4-onem-checklist-section-prefill-rules.{ts,spec.ts}`

Modèle :
- `frontend/src/app/core/models/c4-onem-checklist.model.ts` — DTO request/response/result + enums `Verdict`, `EtapeSuivante`, `Mention`.

Service :
- `frontend/src/app/core/services/c4-onem-checklist.service.ts` — POST + GET.

Modifications :
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` — entrée TOOL_REGISTRY après `prescription-be-litige-travail`.
- `frontend/src/app/core/models/case-analysis.model.ts` — `TravailExtractedData` étendu (déjà partiellement par SF-207-01b ; ajouter les 6 nouveaux champs `raisonSocialeEmployeur`, `numeroBce`, `categorieOnem`, `motifExplicite`, `preavisPresteJours`, `dernierSalaireMensuelBrut`).

Migration backend (bundled dans cette SF pour cohérence garde-fou) :
- `backend/src/main/resources/db/changelog/migrations/XXX-add-c4-onem-checklist-visibility.xml` (prochain numéro disponible).

## Dépendances

- SF-207-02-backend (#1123, mergé) — endpoint + extension `TravailExtractedData` + critereCode BE_C4_*.
- SF-207-01b (#1121, mergé) — pattern frontend canonique.
