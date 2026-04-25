# Mini-spec — F-FA-21 / SF-FA-21-02 Frontend Séparation de corps + conversion divorce

## Identifiant

`F-FA-21 / SF-FA-21-02`

## Feature parente

`F-FA-21` — Séparation de corps + conversion en divorce (art. 296 et s. + 306 Cciv)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-FA-21-02-frontend-separation-corps`

---

## Objectif

Frontend Angular pour l'outil décisionnel "Séparation de corps art. 296 + conversion divorce art. 306 Cciv" (FRANCE uniquement) — saisie avocat, calcul + affichage verdict de conversion (POSSIBLE / PREMATUREE / RECONCILIATION_BLOQUE), avec pré-remplissage IA et validation F-IA-03.

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre le panel F-IA-04 sur un dossier `DROIT_FAMILLE` côté France.
2. La section `<app-separation-corps-section>` est rendue (collapsée par défaut).
3. Au déploiement (`!collapsed()`), la section :
   - charge `GET /api/v1/case-files/{caseFileId}/separation-corps` (404 attendu si pas d'analyse) ;
   - applique le pré-fill IA depuis `aiData?: Partial<FamilleExtractedData>` sur les champs détectables (`patrimoineCommun`, `dateJugementSeparationCorps` via `dateSeparation`) ;
   - affiche le formulaire avec les champs saisissables et les badges IA (`auto_awesome`).
4. L'avocat saisit les champs requis :
   - `mat-select modeProcedure` (4 valeurs : CONSENTEMENT_MUTUEL / ACCEPTATION_PRINCIPE / FAUTE / ALTERATION_DEFINITIVE) ;
   - 2 datepickers natifs `<input type="date">` nullable : `dateJugementSeparationCorps`, `dateRequeteConversion` ;
   - numériques : `dureeSeparationAnnees` (entier ≥ 0), `enfantsMineurs` (entier ≥ 0) ;
   - 3 slide-toggles : `consentementMutuelConversion`, `patrimoineCommun`, `demandeReconciliationFormulee`.
5. Au submit `Analyser la conversion`, `POST /api/v1/case-files/{caseFileId}/separation-corps` est émis avec le request body figé (cf. SF-FA-21-01).
6. Au retour 200, le formulaire bascule sur l'écran résultat affichant :
   - bannière verdict 3 valeurs (`POSSIBLE` / `PREMATUREE` / `RECONCILIATION_BLOQUE`) — palette navy / or / rouge classique (rouge réservé à `RECONCILIATION_BLOQUE` car le déclencheur d'art. 306 est annulé) ;
   - score `scoreEligibiliteConversion` (0-100) ;
   - 3 cartes : `dureeSeparationOk` / `delaiConversion2AnsAtteint` / `conversionAutomatiquePossible` ;
   - messages applicatifs ;
   - `formule` + `baseJuridique` en JetBrains Mono.
7. `CaseDashboardRefreshService.triggerRefresh()` est appelé après succès (panel F-IA-04 + dashboard F-IA-02).
8. Snack-bar succès vert ; bouton "Modifier" pour repasser en mode formulaire.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Champ obligatoire manquant | Bouton submit désactivé via `formValid()` |
| Workspace BELGIQUE | Bannière info "Séparation de corps = procédure française uniquement (art. 296 Cciv)", aucun appel HTTP |
| `GET` 404 | Reste en mode formulaire, applique pré-fill IA si `aiData` présent |
| `GET` autre erreur | Reste en mode formulaire, pas de snackbar (fail-open) |
| `POST` 400 (validation) | `MatSnackBar` rouge avec le message backend |
| `POST` autre erreur | `MatSnackBar` rouge "Erreur lors du calcul" |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : `pacs-dissolution-section` (template canonique 2026-04-25 — pattern mat-select + datepickers natifs + 5 fields F-IA-03) → réutilisé tel quel ; `divorce-accepte-section` (template canonique — 5 provenance signals + datepickers natifs) → réutilisé pour les datepickers ; `divorce-alteration-section` (template SF-FA-08-02 — pattern famille + builders d'alertes coherence) → réutilisé pour la structure des builders et helpers IA.
- [x] **Autres pays** : la séparation de corps + conversion en divorce est strictement française (art. 296 et s. + 306 Cciv). En Belgique, il n'existe pas d'équivalent direct (séparation de fait → désunion irrémédiable art. 229 CC déjà couverte par F-FA-11). La gate `workspaceCountry === 'FRANCE'` masque l'outil avec une bannière info pour BE.
- [x] **Autres domaines** : non applicable — un outil décisionnel = une situation métier (CLAUDE.md). Séparation de corps = couple marié refusant le divorce immédiat, scope `DROIT_FAMILLE` strict.
- [x] **Autres UI patterns** : pré-fill IA via `aiData?: Partial<FamilleExtractedData>` (réutilisé) + validation F-IA-03 via `CoherenceAlertBuilder` (réutilisé) + popover via `CoherencePopoverTriggerDirective` (réutilisé) + refresh dashboard via `CaseDashboardRefreshService` (réutilisé) + datepickers natifs (réutilisé) + palette navy/or/rouge (réutilisé). Aucun nouveau pattern partagé introduit.

### Classification

| Cible | Classification | Justification |
|-------|---------------|---------------|
| Désunion BE jumeau | **Non applicable** | F-FA-11 désunion irrémédiable BE existe déjà — la séparation de corps n'a pas d'équivalent strict en droit belge. |
| Pré-fill IA + F-IA-03 | **Intégré** | Pattern obligatoire CLAUDE.md — appliqué sur 2-3 fields (`patrimoineCommun`, `dateJugementSeparationCorps` via `dateSeparation`, `modeProcedure` éventuellement). |
| Composants existants | **Aucune harmonisation requise** | Le composant suit strictement les patterns canoniques `pacs-dissolution-section` + `divorce-accepte-section`. |

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
- **Droit de la famille** : oui — couple marié, séparation de corps art. 296, conversion automatique en divorce après 2 ans (art. 306). Pas de variation FR/BE dans cette SF (FRANCE uniquement, BE = désunion F-FA-11).
- **Droit immigration** : non applicable.

---

## Parité des domaines métier

Niveau de cet outil : **5 — Scoring / analyse validité** (`scoreEligibiliteConversion` 0-100 + `verdictConversion` 3 valeurs). Le pendant en immigration est `F-IM-05 / F-IM-06` (analyse validité titre + recours), en droit du travail `F-DT-08 / F-DT-10` (validité licenciement + rupture conventionnelle). Les 3 domaines disposent désormais de leurs scoring outils respectifs.

---

## Contrat API (importé de SF-FA-21-01)

**Endpoint** : `POST + GET /api/v1/case-files/{caseFileId}/separation-corps`

```typescript
export type ModeProcedureSep =
  | 'CONSENTEMENT_MUTUEL'
  | 'ACCEPTATION_PRINCIPE'
  | 'FAUTE'
  | 'ALTERATION_DEFINITIVE';

export type VerdictConversion =
  | 'POSSIBLE'
  | 'PREMATUREE'
  | 'RECONCILIATION_BLOQUE';

export interface SeparationCorpsRequest {
  modeProcedure: ModeProcedureSep;
  dateJugementSeparationCorps: string | null;     // ISO YYYY-MM-DD ou null
  dateRequeteConversion: string | null;           // ISO YYYY-MM-DD ou null
  dureeSeparationAnnees: number;                  // entier ≥ 0
  consentementMutuelConversion: boolean;
  patrimoineCommun: boolean;
  enfantsMineurs: number;                         // entier ≥ 0
  demandeReconciliationFormulee: boolean;
}

export interface SeparationCorpsResponse {
  caseFileId: string;
  // input
  modeProcedure: ModeProcedureSep;
  dateJugementSeparationCorps: string | null;
  dateRequeteConversion: string | null;
  dureeSeparationAnnees: number;
  consentementMutuelConversion: boolean;
  patrimoineCommun: boolean;
  enfantsMineurs: number;
  demandeReconciliationFormulee: boolean;
  // output
  dureeSeparationOk: boolean;
  delaiConversion2AnsAtteint: boolean;
  conversionAutomatiquePossible: boolean;
  scoreEligibiliteConversion: number;             // 0-100
  verdictConversion: VerdictConversion;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}
```

Source : SF-FA-21-01 backend (parallèle, branche `feat/SF-FA-21-01-separation-corps-backend`).

---

## Critères d'acceptation

1. Le composant se monte sans erreur en `FRANCE` et en `BELGIQUE` (gate info visible).
2. `GET` 200 → form masqué, valeurs persistées, badges IA absents.
3. `GET` 404 → reste en form, pré-fill IA appliqué si `aiData` présent.
4. `formValid()` exige : modeProcedure non null + dureeSeparationAnnees ≥ 0 entier + enfantsMineurs ≥ 0 entier (les booléens et dates sont nullable).
5. `calculate()` envoie un POST avec body conforme au contrat (dates `null` si non saisies) ; succès → bannière verdict + cartes + messages ; échec → MatSnackBar rouge.
6. `dashboardRefresh.triggerRefresh()` est invoqué après POST 200.
7. Le badge IA disparaît au moindre changement manuel (handlers `on*Change`).
8. Les alertes F-IA-03 sont calculées via `CoherenceAlertBuilder`, gate `showForm()` strict.
9. La gate BELGIQUE n'émet aucune requête HTTP.
10. Tous les textes affichés dans la palette `DESIGN_SYSTEM.md` (navy/or, rouge réservé alerte critique). `formule` et `baseJuridique` en JetBrains Mono, reste en Inter.

---

## Plan de test

### Unitaires Jest (≥ 18 tests)

- mount sans erreur (FRANCE / BELGIQUE)
- `formValid()` × 4 cas (chaque champ obligatoire absent + cas nominal)
- `GET` 200 → form masqué + valeurs persistées + provenance null
- `GET` 404 → form + pré-fill IA appliqué
- `GET` 404 + `aiData = null` → no-op
- `calculate()` 200 → POST body exact + result + snackbar succès + dashboardRefresh
- `calculate()` 400 → snackbar rouge
- `calculate()` ignoré si form invalide
- handlers `on*Change` effacent badge IA
- `coherenceAlerts.PATRIMOINE_COMMUN` quand IA dit autre chose
- alertes masquées après résultat (`showForm=false`)
- `alertBadgeLabel` IA / MULTI
- `explanationFor` retourne `[]` (fail-open)
- `ngOnChanges(aiData)` post-mount rafraîchit le pré-fill
- gate BELGIQUE → aucun HTTP
- verdict UI helpers

### Intégration

Le frontend consomme le mock backend SF-FA-21-01 figé. L'intégration end-to-end réelle est validée après merge des deux PRs.

### Isolation workspace

Aucune table créée par cette SF. L'isolation passe par l'API backend (cf. SF-FA-21-01).

---

## Tables / endpoints / composants impactés

- **Modèle TS créé** : `frontend/src/app/core/models/separation-corps.model.ts`
- **Service créé** : `frontend/src/app/core/services/separation-corps.service.ts`
- **Composant créé** : `frontend/src/app/case-files/separation-corps-section/`
  - `separation-corps-section.component.ts`
  - `separation-corps-section.component.html`
  - `separation-corps-section.component.scss`
  - `separation-corps-section.component.spec.ts`
- **TOOL_REGISTRY mis à jour** : `decisional-tools-panel.component.ts` — entrée `'F-FA-21-separation-corps'`.

---

## Hors périmètre

- Backend (livré par SF-FA-21-01 en parallèle).
- Génération de PDF / export → aucun générateur lié à cette SF.
- Calcul détaillé de la conversion automatique avec relevé date par date — l'outil renvoie un verdict + score, pas un calendrier détaillé.
- Tests E2E (couverts en intégration manuelle après merge).
