# Mini-spec — F-DT-24 / SF-DT-24-02 Frontend clause non-concurrence (FR)

## Identifiant

`F-DT-24 / SF-DT-24-02`

## Feature parente

`F-DT-24` — Clause de non-concurrence (Cass. soc. 10/07/2002, art. L.1121-1 Code du travail) — droit du travail FR.

## Statut

`ready`

## Date de création

2026-04-25

## Branche Git

`feat/SF-DT-24-02-frontend-non-concurrence`

---

## Objectif

Livrer le composant Angular `non-concurrence-section` qui consomme l'API SF-DT-24-01 (POST/GET `/api/v1/case-files/{id}/non-concurrence`) afin que l'avocat français puisse instruire la validité d'une clause de non-concurrence selon les 4 critères cumulatifs Cass. soc. 10/07/2002 — territoire défini, durée définie, objet défini, contrepartie financière proportionnée — et obtenir un score de validité, un verdict (`VALIDE` / `RISQUE_NULLITE_PARTIELLE` / `NULLE`), le ratio contrepartie / salaire en %, le montant d'indemnité contrepartie due et le potentiel d'indemnité de nullité.

---

## Comportement attendu

### Cas nominal

1. L'utilisateur ouvre un dossier de droit du travail FRANCE. Le panel F-IA-04 affiche la section "CLAUSE DE NON-CONCURRENCE" (tool_id `F-DT-24-non-concurrence`, règle ALWAYS_ON FR + travail seed migration backend SF-DT-24-01).
2. Section repliée par défaut (collapsed). Click/Enter → expand.
3. `ngOnInit` :
   - Si `workspaceCountry !== 'FRANCE'` → bannière info BE (régime distinct loi du 03/07/1978 art. 65 — clause de non-concurrence belge à modéliser séparément), pas de GET.
   - Sinon GET `/api/v1/case-files/{id}/non-concurrence`.
     - GET 200 → mode lecture (form masqué, valeurs hydratées, bouton "Modifier").
     - GET 404 → mode formulaire vide ; `prefillFromAi()` depuis `aiData.salaireBrutMensuel`.
4. L'avocat saisit :
   - `clausePresenteContrat` (mat-slide-toggle, défaut `true`).
   - 4 paires de critères Cass. soc. 10/07/2002, chacune avec un toggle "limite définie" + un input descriptif/numérique :
     - **Territoire** : `limiteTerritoireDefini` (toggle) + `territoireDescription` (textarea, requis si toggle on).
     - **Durée** : `limiteDureeDefinie` (toggle) + `dureeMois` (number, ≥ 0, requis si toggle on).
     - **Objet** : `limiteObjetDefini` (toggle) + `objetDescription` (textarea, requis si toggle on).
     - **Contrepartie financière** : `contrepartieFinancierePresente` (toggle) + `contrepartieMontantMensuelEur` (number, ≥ 0, requis si toggle on).
   - `salaireMensuelBrutEur` (input number, > 0, step 0.01) — **pré-fill IA** depuis `aiData.salaireBrutMensuel`.
   - `secteurActivite` (mat-select, 5 options : `INFORMATIQUE`, `COMMERCE`, `INDUSTRIE`, `SERVICES`, `AUTRE`).
   - `datePriseEffet` (`<input type="date">`, ISO YYYY-MM-DD).
5. Submit → POST avec body conforme au contrat figé SF-DT-24-01.
6. Réponse 200 :
   - Bannière verdict (validité : `VALIDE` navy / `RISQUE_NULLITE_PARTIELLE` or / `NULLE` rouge alerte) avec score 0-100 et pictogramme (`gavel` rouge / `balance` or / `verified` navy).
   - **4 cartes critères** (✓ navy / ✗ rouge) avec article cité — Critère 1 Territoire / Critère 2 Durée / Critère 3 Objet / Critère 4 Contrepartie financière.
   - Ratio contrepartie / salaire en % (`ratioContrepartiePct`) en JetBrains Mono dans une mini-carte dédiée (visible uniquement si `contrepartieFinancierePresente=true`).
   - 2 cartes montants en JetBrains Mono : `indemniteContrepartieDueEur` (montant que l'employeur doit verser tant que la clause s'applique) + `indemnitePotentielleNulliteEur` (montant indemnitaire potentiel en cas de nullité judiciaire).
   - Liste messages avec rappel jurisprudentiel Cass. soc. 10/07/2002 + références juridiques en `<code>` JetBrains Mono via pipe `legalCitations`.
   - `baseJuridique` + `formule` en JetBrains Mono.
   - Bouton "Modifier" → retour formulaire avec valeurs pré-remplies.
   - `MatSnackBar` succès, `CaseDashboardRefreshService.triggerRefresh()` appelé.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `salaireMensuelBrutEur` ≤ 0 | Submit désactivé (form invalide) | — |
| `secteurActivite` non sélectionné | Submit désactivé | — |
| `datePriseEffet` vide ou non ISO | Submit désactivé | — |
| `limiteTerritoireDefini=true` sans `territoireDescription` | Submit désactivé | — |
| `limiteDureeDefinie=true` avec `dureeMois` ≤ 0 | Submit désactivé | — |
| `limiteObjetDefini=true` sans `objetDescription` | Submit désactivé | — |
| `contrepartieFinancierePresente=true` avec `contrepartieMontantMensuelEur` ≤ 0 | Submit désactivé | — |
| Backend 400 (validation) | `MatSnackBar` rouge, message backend remonté | 400 |
| Dossier hors workspace ou hors travail | `MatSnackBar` erreur | 400/404 |
| GET inexistant | Reste en mode formulaire (404 attendu — pas de snackbar) | 404 |
| Erreur réseau POST | `MatSnackBar` rouge | 5xx |
| `workspaceCountry !== 'FRANCE'` | Bannière info BE — pas de GET, pas de form | — |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier (droit du travail)** : F-DT-23 (requalification intérim → CDI) jumeau récent — palette navy/or/rouge identique, scoring 0-100, mêmes patterns pré-fill IA salaire + alertes F-IA-03. F-DT-22 (requalification CDD → CDI) palette identique. F-DT-11 (harcèlement licenciement nul) template canonique. **Pattern intégré** : structure héritée de F-DT-23 + adaptation 4 critères Cass. soc. 10/07/2002 (cartes critères ✓/✗).
- [x] **Pattern de référence canonique** : `harcelement-licenciement-nul-section` (F-DT-11-02, identifié dans `ai-skills/frontend-coherence-audit.md` §5). Pattern jumeau structurel verdict + multi-cartes : `divorce-faute-section` (F-FA-09-02). Pattern jumeau direct salaire + scoring : `requalification-interim-cdi-section` (F-DT-23-02).
- [x] **Autres pays** : FRANCE only. Belgique : régime distinct (loi du 03/07/1978 art. 65, AR 24/12/1969 — durée max 12 mois, indemnité forfaitaire ½ rémunération brute correspondant à la durée d'application) — bannière info renvoyant vers un futur outil BE (F-DT-24-BE backlog hypothétique).
- [x] **Autres domaines** : non applicable — clause de non-concurrence est une notion strictement de droit du travail. Pas d'équivalent Immigration / Famille.
- [x] **Autres UI patterns** : pré-fill IA (SF-155-04), alertes cohérence F-IA-03 via `CoherenceAlertBuilder` (SF-155-05/06), refresh dashboard (SF-IA-02-03), pipe `legalCitations` pour rendu références juridiques — tous réutilisés. **Aucun nouveau pattern partagé introduit**.

### Niveaux de vérification

- [x] Modèle TypeScript + interface contrat API (importé SF-DT-24-01)
- [x] Service Angular wrapping HttpClient (POST + GET)
- [x] Composant Angular consommateur avec pré-fill IA + validation F-IA-03
- [x] Spec Jest ≥ 15 tests couvrant mount + form valid + POST + erreur + IA + alertes
- [x] Entrée TOOL_REGISTRY symétrique (`aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`)

---

## Impact par domaine métier

- **Droit du travail (FR)** : feature centrale (objet de la SF). Spécifique à la clause de non-concurrence française et la jurisprudence Cass. soc. 10/07/2002.
- **Droit du travail (BE)** : non couvert — bannière info uniquement. Le régime belge (loi 03/07/1978 art. 65, AR 24/12/1969) prévoit un mécanisme distinct (durée plafond 12 mois, indemnité forfaitaire ½ rémunération brute). Backlog : F-DT-24-BE jumeau hypothétique à ouvrir si demande utilisateur.
- **Droit de l'immigration** : non applicable.
- **Droit de la famille** : non applicable.

---

## Parité des domaines métier (outil décisionnel niveau ≥ 5 — scoring + analyse validité)

L'outil produit un `scoreValidite` (0-100) + `verdictValidite` (`VALIDE` / `RISQUE_NULLITE_PARTIELLE` / `NULLE`) → niveau **5 (scoring / analyse validité)**.

| Domaine | Équivalent existant ? |
|---|---|
| Travail FR | **Oui** — c'est cette SF (F-DT-24). |
| Travail BE | Non — pas de mécanisme miroir direct. Régime non-concurrence belge à modéliser séparément si feature jumelle ouverte au backlog (F-DT-24-BE potentiel). |
| Immigration | Non applicable — concept de clause de non-concurrence strictement contractuel salarial. |
| Famille | Non applicable. |

**Conclusion** : pas d'asymétrie nouvelle créée. La parité est traitée par la convention "outil = situation métier" (cf. `feedback_decision_tools_one_per_situation.md`). Le concept "clause de non-concurrence" est strictement français pour l'instant.

---

## Nouveau pattern UI ou service partagé

Aucun. Le composant ne crée ni service partagé, ni directive transversale, ni DTO réutilisable, ni composant générique. Il consomme :
- Le `CoherenceAlertBuilder` partagé (`shared/coherence-popover/coherence-alert-builder.ts`) — existant SF-155-05.
- La directive `CoherencePopoverTriggerDirective` — existant SF-IA-03-15b.
- Le pipe `LegalCitationsPipe` — existant.
- Les modèles `TravailExtractedData`, `ProcedureCheck`, `AiQuestion`, `PieceManquanteEntry` — existants.

---

## Critères d'acceptation

1. Composant Angular `NonConcurrenceSectionComponent` standalone publié dans `frontend/src/app/case-files/non-concurrence-section/`.
2. Modèle TypeScript dans `frontend/src/app/core/models/non-concurrence.model.ts` avec types `SecteurActivite`, `VerdictValiditeNc`, `NonConcurrenceRequest`, `NonConcurrenceResponse`, et `SECTEUR_ACTIVITE_OPTIONS`.
3. Service `NonConcurrenceService` avec méthodes `calculate(caseFileId, request)` (POST) et `get(caseFileId)` (GET).
4. Form valide quand `salaireMensuelBrutEur > 0`, `secteurActivite` ≠ null, `datePriseEffet` non vide, et conditions cumulatives sur les 4 paires (toggle on ⇒ champ correspondant rempli/positif).
5. POST envoie le body au schéma exact figé par SF-DT-24-01.
6. Affichage résultat : bannière verdict colorée (palette navy/or/rouge), score 0-100, **4 cartes critères ✓/✗** avec article, mini-carte ratio contrepartie en %, 2 cartes montants en JetBrains Mono, messages, baseJuridique, formule, bouton Modifier.
7. **Pré-fill IA fonctionnel** : `aiData.salaireBrutMensuel` pré-remplit `salaireMensuelBrutEur` avec badge "Pré-rempli depuis l'analyse" (icône `auto_awesome`). Saisie manuelle efface le badge.
8. **Validation F-IA-03 fonctionnelle** : alerte de cohérence sur `salaireMensuelBrutEur` quand divergence > 10 % vs `aiData`. Multi-sources `IA` / `F96` / `QUESTION_IA` / `PIECE_MANQUANTE` consolidées via `CoherenceAlertBuilder`.
9. Gate `workspaceCountry`: bannière info si BE (jamais masquage silencieux).
10. `CaseDashboardRefreshService.triggerRefresh()` appelé après POST 200.
11. `MatSnackBar` pour erreurs HTTP, jamais `alert`/`confirm`.
12. JetBrains Mono pour `baseJuridique`, `formule`, montants, dates ISO. Inter pour le reste.
13. Entrée TOOL_REGISTRY ajoutée (`F-DT-24-non-concurrence`), avec inputs symétriques aux autres outils du panel.
14. Spec Jest avec ≥ 15 tests : mount, form validators, GET 200/404, POST succès/erreur, pré-fill, alertes IA-03, gate BE, paires toggles, edit mode, toggle collapse, mapping verdictBannerClass.
15. `tsc --noEmit -p tsconfig.app.json` et `npx jest --testPathPattern=non-concurrence` passent verts.

---

## Plan de test minimal

### Unitaires (Jest, ≥ 15 — livré 36)

1. `mount sans erreur (FRANCE)` + 5 secteurActivite options exposées.
2. `formValid faux si salaireMensuelBrutEur null/0/négatif`.
3. `formValid faux si secteurActivite null`.
4. `formValid faux si datePriseEffet vide`.
5. `formValid faux si limiteTerritoireDefini=true sans territoireDescription`.
6. `formValid faux si limiteDureeDefinie=true avec dureeMois ≤ 0/null`.
7. `formValid faux si limiteObjetDefini=true sans objetDescription`.
8. `formValid faux si contrepartieFinancierePresente=true avec montant ≤ 0/null`.
9. `formValid vrai sur cas nominal complet sans toggles définis`.
10. `GET 200 → form masqué, valeurs hydratées, pas de badge IA`.
11. `GET 404 → reste en mode formulaire ; pré-fill IA appliqué`.
12. `calculate() POST → résultat affiché + snackbar succès + dashboardRefresh`.
13. `calculate() erreur 400 → snackbar rouge, pas de refresh`.
14. `calculate() ignoré si form invalide`.
15. `pré-fill IA salaireMensuelBrutEur si aiData.salaireBrutMensuel > 0`.
16. `aiData.salaireBrutMensuel = 0 → pas de pré-fill`.
17. `aiData null → pas de badge IA, pas de pré-fill`.
18. `onSalaireChange manuel efface le badge IA`.
19. `ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide`.
20. `ngOnChanges(aiData) après saisie manuelle n'écrase pas`.
21. `coherenceAlerts.SALAIRE_MENSUEL présent si écart > 10 % vs IA`.
22. `coherenceAlerts absent si écart ≤ 10 %`.
23. `alertes masquées après showForm=false (anti-bug SF-IA-03-12)`.
24. `alertBadgeLabel et alertTooltip exposent un texte pertinent`.
25. `toggle limiteTerritoireDefini=false vide la description`.
26. `toggle limiteDureeDefinie=false remet dureeMois à null`.
27. `toggle limiteObjetDefini=false vide la description`.
28. `toggle contrepartieFinancierePresente=false remet montant à null`.
29. `POST envoie tous les champs y compris ceux issus des paires off`.
30. `workspaceCountry BELGIQUE → bannière info, pas de GET`.
31. `toggleCollapse fonctionne`.
32. `editMode ré-affiche le form`.
33. `verdictBannerClass mappe NULLE→danger, RISQUE_NULLITE_PARTIELLE→warn, VALIDE→info`.
34. `verdictIcon mappe NULLE→gavel, RISQUE_NULLITE_PARTIELLE→balance, VALIDE→verified`.
35. `verdictValiditeNcLabel produit un label francisé pour chaque valeur`.
36. `secteurActiviteLabel résout les libellés pour les codes connus`.

### Intégration

Smoke test via tests Jest avec `HttpClientTestingModule` mocké couvrant le flux complet GET → form → POST → résultat. Pas de e2e dédié (couvert par les e2e existants `case-detail.spec.ts` du panel).

### Isolation workspace

Non applicable côté frontend (le backend filtre `workspace_id`). Le composant respecte la règle en passant `caseFileId` opaque ; aucune fuite cross-workspace possible côté UI.

---

## Tables / endpoints / composants impactés

### Endpoints consommés (figés SF-DT-24-01)

- `POST /api/v1/case-files/{caseFileId}/non-concurrence` → `NonConcurrenceResponse`.
- `GET /api/v1/case-files/{caseFileId}/non-concurrence` → `NonConcurrenceResponse` (404 si absent).

### Fichiers créés

- `frontend/src/app/core/models/non-concurrence.model.ts`
- `frontend/src/app/core/services/non-concurrence.service.ts`
- `frontend/src/app/case-files/non-concurrence-section/non-concurrence-section.component.ts`
- `frontend/src/app/case-files/non-concurrence-section/non-concurrence-section.component.html`
- `frontend/src/app/case-files/non-concurrence-section/non-concurrence-section.component.scss`
- `frontend/src/app/case-files/non-concurrence-section/non-concurrence-section.component.spec.ts`
- `docs/features/F-DT-24/SF-DT-24-02-frontend-non-concurrence.md`

### Fichiers modifiés

- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` (+ entrée TOOL_REGISTRY).

---

## Hors périmètre

- Backend SF-DT-24-01 (déjà figé, contrat importé).
- Régime BE (loi 03/07/1978 art. 65, AR 24/12/1969) — bannière info uniquement.
- Détection IA automatique du `secteurActivite` ou de la présence de la clause — backlog futur (extraction enrichie LLM via F-IA-01).
- Calculateur indemnité jurisprudentielle au-delà du minimum forfaitaire — couvert par F-DT-09 comparateur si étendu.
- Génération PDF de la fiche d'analyse de validité — couvert par F-DT-04 (fiche prudhomale) si étendu.
- Action en levée judiciaire de la clause par l'employeur (renonciation tardive) — pas couvert dans cet outil ; backlog si demande.

---

## Contrat API (importé de SF-DT-24-01)

```typescript
export type VerdictValiditeNc = 'VALIDE' | 'RISQUE_NULLITE_PARTIELLE' | 'NULLE';
export type SecteurActivite = 'INFORMATIQUE' | 'COMMERCE' | 'INDUSTRIE' | 'SERVICES' | 'AUTRE';

export interface NonConcurrenceRequest {
  clausePresenteContrat: boolean;
  limiteTerritoireDefini: boolean;
  territoireDescription: string;
  limiteDureeDefinie: boolean;
  dureeMois: number;
  limiteObjetDefini: boolean;
  objetDescription: string;
  contrepartieFinancierePresente: boolean;
  contrepartieMontantMensuelEur: number;
  salaireMensuelBrutEur: number;
  secteurActivite: SecteurActivite;
  datePriseEffet: string; // ISO YYYY-MM-DD
}

export interface NonConcurrenceResponse {
  caseFileId: string;
  clausePresenteContrat: boolean;
  limiteTerritoireDefini: boolean;
  territoireDescription: string;
  limiteDureeDefinie: boolean;
  dureeMois: number;
  limiteObjetDefini: boolean;
  objetDescription: string;
  contrepartieFinancierePresente: boolean;
  contrepartieMontantMensuelEur: number;
  salaireMensuelBrutEur: number;
  secteurActivite: SecteurActivite;
  datePriseEffet: string;
  critere1TerritoireOk: boolean;
  critere2DureeOk: boolean;
  critere3ObjetOk: boolean;
  critere4ContrepartieOk: boolean;
  ratioContrepartiePct: number;
  scoreValidite: number;
  verdictValidite: VerdictValiditeNc;
  indemniteContrepartieDueEur: number;
  indemnitePotentielleNulliteEur: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}
```

---

## Références

- Pattern canonique : `ai-skills/frontend-coherence-audit.md` §5.
- Pattern jumeau direct : `frontend/src/app/case-files/requalification-interim-cdi-section/` (F-DT-23-02).
- Pattern jumeau verdict + multi-cartes : `frontend/src/app/case-files/divorce-faute-section/` (F-FA-09-02).
- Pré-fill IA : `frontend/src/app/case-files/immigration-title-decision-section/` (F-IM-05-03).
- Builder F-IA-03 : `frontend/src/app/shared/coherence-popover/coherence-alert-builder.ts` (SF-155-05).
- Backend contrat figé : `docs/features/F-DT-24/SF-DT-24-01-backend-non-concurrence.md` (à mergé en parallèle).
