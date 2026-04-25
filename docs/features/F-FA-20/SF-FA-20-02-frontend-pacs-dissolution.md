# Mini-spec — F-FA-20 / SF-FA-20-02 Frontend Dissolution PACS art. 515-7

## Identifiant

`F-FA-20 / SF-FA-20-02`

## Feature parente

`F-FA-20` — PACS / cohabitation légale BE — dissolution

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-FA-20-02-frontend-pacs-dissolution`

---

## Objectif

Frontend Angular pour l'outil décisionnel "Dissolution PACS art. 515-7 Cciv" (FRANCE uniquement) — saisie avocat, calcul + affichage verdict / scoring créances probables, pré-remplissage IA et validation F-IA-03.

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre le panel F-IA-04 sur un dossier `DROIT_FAMILLE` côté France.
2. La section `<app-pacs-dissolution-section>` est rendue (collapsée par défaut, comme les autres sections décisionnelles).
3. Au déploiement (`!collapsed()`), la section :
   - charge `GET /api/v1/case-files/{caseFileId}/pacs-dissolution` (404 attendu si pas d'analyse) ;
   - applique le pré-fill IA depuis `aiData?: FamilleExtractedData` sur les 4-5 champs détectables (date PACS, mode dissolution, régime biens, créances alléguées, patrimoine commun) ;
   - affiche le formulaire avec les champs saisissables et les badges IA (`auto_awesome`).
4. L'avocat saisit / corrige les champs requis :
   - 3 datepickers natifs `<input type="date">` : datePacs, dateDissolution, dateNotificationPartenaire ;
   - mat-select `modeDissolution` (5 valeurs) ;
   - mat-select `regimeBiens` (3 valeurs) ;
   - mat-select multiple `creancesAlleguees` (5 valeurs, dont AUCUNE) ;
   - numériques : `dureeUnionAnnees` (entier ≥ 0) + `enfantsCommuns` (entier ≥ 0) ;
   - slide-toggle `patrimoineCommunSignificatif`.
5. Au submit `Analyser la dissolution`, `POST /api/v1/case-files/{caseFileId}/pacs-dissolution` est émis avec le request body figé (cf. SF-FA-20-01).
6. Au retour 200, le formulaire bascule sur l'écran résultat affichant :
   - bannière verdict 4 valeurs (`LIQUIDATION_AMIABLE` / `MEDIATION_OU_JAF` / `CONTENTIEUX_INEVITABLE` / `RIEN_A_FAIRE`) — palette navy/or/rouge classique ;
   - score `scoreCreancesProbables` (0-100) ;
   - 3 cartes : `dissolutionValide` / `delaiNotificationOk` / `dureeUnionEligibleCreances` ;
   - liste de chips créances `creancesPotentielleVisibles` ;
   - délai prescription `delaiPrescriptionAnnees` en JetBrains Mono ;
   - messages applicatifs ;
   - formule + base juridique en JetBrains Mono.
7. `CaseDashboardRefreshService.triggerRefresh()` est appelé après succès (panel F-IA-04 + dashboard F-IA-02).
8. Snack-bar succès vert ; bouton "Modifier" pour repasser en mode formulaire.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Champ obligatoire manquant | Bouton submit désactivé via `formValid()` |
| Workspace BELGIQUE | Bannière info "PACS = procédure française uniquement", aucun appel HTTP |
| `GET` 404 | Reste en mode formulaire, applique pré-fill IA si `aiData` présent |
| `GET` autre erreur | Reste en mode formulaire, pas de snackbar (fail-open) |
| `POST` 400 (validation) | `MatSnackBar` rouge avec le message backend |
| `POST` autre erreur | `MatSnackBar` rouge "Erreur lors du calcul" |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : `desaccords-parentaux-section` (template canonique récent — pattern multi-select F-IA-03 + 5 fields pré-fillables) → réutilisé tel quel ; `recompenses-section` (template SF-FA-15-02 — pattern régime matrimonial + builder F-IA-03) → réutilisé pour le mat-select `regimeBiens` ; `divorce-accepte-section` (template canonique) → datepickers natifs `<input type="date">` + 5 provenance signals.
- [x] **Autres pays** : la dissolution PACS est un mécanisme strictement français — la cohabitation légale BE relève d'une feature jumelle (loi belge 23/11/1998, art. 1476/1477 CC) **hors périmètre SF-FA-20-02** ; le verdict `country: 'FRANCE'` est figé dans la response. La gate `workspaceCountry === 'FRANCE'` masque l'outil avec une bannière info pour BE.
- [x] **Autres domaines** : non applicable — un outil décisionnel = une situation métier (CLAUDE.md). PACS = couple non marié, scope `DROIT_FAMILLE` strict. Aucun mécanisme générique réutilisable hors famille.
- [x] **Autres UI patterns** : pré-fill IA via `aiData?: Partial<FamilleExtractedData>` (réutilisé) + validation F-IA-03 via `CoherenceAlertBuilder` (réutilisé) + popover via `CoherencePopoverTriggerDirective` (réutilisé) + refresh dashboard via `CaseDashboardRefreshService` (réutilisé) + datepickers natifs (réutilisé) + palette navy/or/rouge (réutilisé). Aucun nouveau pattern partagé introduit.

### Classification

| Cible | Classification | Justification |
|-------|---------------|---------------|
| Cohabitation légale BE (jumeau) | **Backlog F-FA-20 SF-03** | Mécanisme distinct (loi belge 1998, art. 1476/1477 CC), backend séparé prévu — hors scope SF-02 par décision SF-FA-20-01. |
| Pré-fill IA + F-IA-03 | **Intégré** | Pattern obligatoire CLAUDE.md — appliqué sur 5 fields. |
| Composants existants | **Aucune harmonisation requise** | Le composant suit strictement les patterns canoniques ; aucun composant existant n'est modifié. |

---

## Nouveau pattern UI ou service partagé

Aucun nouveau composant/directive/service partagé. Le composant réutilise :
- `CoherenceAlertBuilder` (chemin `frontend/src/app/shared/coherence-popover/coherence-alert-builder.ts`) ;
- `CoherencePopoverTriggerDirective` ;
- `CaseDashboardRefreshService` ;
- `LegalCitationsPipe` (pour le rendu des messages) ;
- `SourceExplanationService` (popover F-IA-03-15c, fail-open).

---

## Impact par domaine métier

- **Droit du travail** : non applicable (scope famille).
- **Droit de la famille** : oui — couple PACS non marié, dissolution unilatérale / conjointe / mariage / décès, scoring créances 1437/1469. Pas de variation FR/BE dans cette SF (FRANCE uniquement, BE = SF future).
- **Droit immigration** : non applicable.

Cohabitation légale BE = jumeau métier déjà tracé dans la note F-FA-20 du PRODUCT_SPEC (~2-3 SF total). Cette SF n'introduit pas d'asymétrie nouvelle (le backend SF-FA-20-01 figé est lui-même FR uniquement).

---

## Parité des domaines métier

Niveau de cet outil : **5 — Scoring / analyse validité** (`scoreCreancesProbables` 0-100 + `verdictRecommandation` 4 valeurs). Le pendant en immigration est `F-IM-05 / F-IM-06` (analyse validité titre + recours), en droit du travail `F-DT-08 / F-DT-10` (validité licenciement + rupture conventionnelle). Ces 3 domaines disposent désormais de leurs scoring outils respectifs — F-FA-20 ferme la parité famille.

---

## Contrat API (importé de SF-FA-20-01)

**Endpoint** : `POST + GET /api/v1/case-files/{caseFileId}/pacs-dissolution`

```typescript
export type ModeDissolutionPacs =
  | 'DECLARATION_UNILATERALE'
  | 'DECLARATION_CONJOINTE'
  | 'MARIAGE_PARTENAIRES'
  | 'MARIAGE_TIERS'
  | 'DECES';

export type RegimeBiensPacs =
  | 'SEPARATION_BIENS'
  | 'INDIVISION_AMENAGEE'
  | 'INDIVISION_PAR_DEFAUT';

export type CreanceAlleguee =
  | 'CONTRIBUTION_DESEQUILIBRE'
  | 'INVESTISSEMENT_BIEN_PROPRE'
  | 'ENRICHISSEMENT_INJUSTE'
  | 'PRESTATION_TRAVAIL_NON_REMUNEREE'
  | 'AUCUNE';

export type VerdictPacs =
  | 'LIQUIDATION_AMIABLE'
  | 'MEDIATION_OU_JAF'
  | 'CONTENTIEUX_INEVITABLE'
  | 'RIEN_A_FAIRE';

export interface PacsDissolutionRequest {
  dateConclusionPacs: string;          // ISO YYYY-MM-DD
  modeDissolution: ModeDissolutionPacs;
  dateDissolution: string;             // ISO YYYY-MM-DD
  dureeUnionAnnees: number;            // entier ≥ 0
  regimeBiens: RegimeBiensPacs;
  patrimoineCommunSignificatif: boolean;
  creancesAlleguees: CreanceAlleguee[]; // peut contenir AUCUNE
  enfantsCommuns: number;              // entier ≥ 0
  dateNotificationPartenaire: string;  // ISO YYYY-MM-DD
}

export interface PacsDissolutionResponse {
  caseFileId: string;
  // input
  dateConclusionPacs: string;
  modeDissolution: ModeDissolutionPacs;
  dateDissolution: string;
  dureeUnionAnnees: number;
  regimeBiens: RegimeBiensPacs;
  patrimoineCommunSignificatif: boolean;
  creancesAlleguees: CreanceAlleguee[];
  enfantsCommuns: number;
  dateNotificationPartenaire: string;
  // output
  dissolutionValide: boolean;
  delaiNotificationOk: boolean;
  dureeUnionEligibleCreances: boolean;
  scoreCreancesProbables: number;
  verdictRecommandation: VerdictPacs;
  creancesPotentielleVisibles: CreanceAlleguee[];
  delaiPrescriptionAnnees: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}
```

Source : SF-FA-20-01 backend (parallèle, branche `feat/SF-FA-20-01-backend-pacs-dissolution`).

---

## Critères d'acceptation

1. Le composant se monte sans erreur en `FRANCE` et en `BELGIQUE` (gate visible).
2. `GET` 200 → form masqué, valeurs persistées, badges IA absents.
3. `GET` 404 → reste en form, pré-fill IA appliqué sur 5 fields max si `aiData` présent.
4. `formValid()` exige : 3 dates ISO valides + modeDissolution + regimeBiens + dureeUnionAnnees ≥ 0 entier + enfantsCommuns ≥ 0 entier + creancesAlleguees non vide.
5. `calculate()` envoie un POST avec body conforme au contrat ; succès → bannière verdict + cartes + chips ; échec → MatSnackBar rouge.
6. `dashboardRefresh.triggerRefresh()` est invoqué après POST 200.
7. Le badge IA disparaît au moindre changement manuel (handlers `on*Change`).
8. Les alertes F-IA-03 sont calculées via `CoherenceAlertBuilder` (5 fields max), gate `showForm()` strict.
9. La gate BELGIQUE n'émet aucune requête HTTP.
10. Tous les textes affichés dans la palette de couleurs `DESIGN_SYSTEM.md` (navy/or, rouge réservé alerte critique).

---

## Plan de test

### Unitaires Jest (≥ 18 tests)

- mount sans erreur (FRANCE / BELGIQUE)
- `formValid()` × 6 cas (chaque champ obligatoire absent + cas nominal)
- `GET` 200 → form masqué + valeurs persistées + provenance null
- `GET` 404 → form + pré-fill IA appliqué (5 fields)
- `GET` 404 + `aiData = null` → no-op
- `calculate()` 200 → POST body exact + result + snackbar succès + dashboardRefresh
- `calculate()` 400 → snackbar rouge
- `calculate()` ignoré si form invalide
- `onModeDissolutionChange / onRegimeBiensChange / onCreancesChange` effacent badge IA
- `coherenceAlerts.MODE_DISSOLUTION` quand IA dit autre chose
- `coherenceAlerts.CREANCES_ALLEGUEES` divergence ensembliste
- alertes masquées après résultat (`showForm=false`)
- `alertBadgeLabel` IA / MULTI
- `explanationFor` retourne `[]` (fail-open)
- `ngOnChanges(aiData)` post-mount rafraîchit le pré-fill
- gate BELGIQUE → aucun HTTP

### Intégration

Le frontend consomme le mock backend SF-FA-20-01 figé. L'intégration end-to-end réelle est validée après merge des deux PRs.

### Isolation workspace

Aucune table créée par cette SF. L'isolation passe par l'API backend (cf. SF-FA-20-01).

---

## Tables / endpoints / composants impactés

- **Modèle TS créé** : `frontend/src/app/core/models/pacs-dissolution.model.ts`
- **Service créé** : `frontend/src/app/core/services/pacs-dissolution.service.ts`
- **Composant créé** : `frontend/src/app/case-files/pacs-dissolution-section/`
  - `pacs-dissolution-section.component.ts`
  - `pacs-dissolution-section.component.html`
  - `pacs-dissolution-section.component.scss`
  - `pacs-dissolution-section.component.spec.ts`
- **TOOL_REGISTRY mis à jour** : `decisional-tools-panel.component.ts` — entrée `'F-FA-20-pacs-dissolution'`.

---

## Hors périmètre

- Cohabitation légale belge (loi 23/11/1998 art. 1476/1477 CC) — feature jumelle prévue F-FA-20 SF-03+.
- Édition / récalcul partiel d'une dissolution déjà persistée (out of scope MVP).
- Génération de PDF / export → aucun générateur lié à cette SF (mécanisme F-105 hors scope).
- Liquidation détaillée des créances avec calcul ligne par ligne — relève de F-FA-15 (récompenses) côté communauté seulement, pas du PACS.
