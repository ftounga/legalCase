# SF-FA-13-02 — Frontend Révisions post-divorce (FR)

> Subfeature **frontend** Angular consommant l'API SF-FA-13-01 (backend `revisions-post-divorce`).
> Domaine : Famille — France uniquement (équivalent BE F-FA-21 à venir).

---

## 1. Objectif (1 phrase)

Permettre à l'avocat de simuler la possibilité d'une **révision post-divorce** (pension alimentaire, prestation compensatoire, résidence enfants, droit de visite, déménagement parent) à partir d'un changement de circonstance, et d'obtenir un score, un verdict (`ELEVEE` / `MOYENNE` / `FAIBLE`), une formule et la base juridique applicable.

## 2. Contrat API (importé de SF-FA-13-01)

```
POST /api/v1/case-files/{caseFileId}/revisions-post-divorce
GET  /api/v1/case-files/{caseFileId}/revisions-post-divorce
```

### Request

```ts
type TypeRevisionPostDivorce =
  | 'PENSION_ALIMENTAIRE'
  | 'RESIDENCE'
  | 'DROIT_VISITE'
  | 'PRESTATION_COMPENSATOIRE'
  | 'DEMENAGEMENT_PARENT';

type ModeResidence = 'ALTERNEE' | 'EXCLUSIVE_MERE' | 'EXCLUSIVE_PERE' | 'LIBRE';

interface RevisionsPostDivorceRequest {
  typeRevision: TypeRevisionPostDivorce;
  dateDecisionInitiale: string;          // ISO YYYY-MM-DD
  changementCirconstance: string;         // ≥ 20 chars
  revenusInitialsDebiteurEur?: number | null;
  revenusActuelsDebiteurEur?: number | null;
  revenusInitialsCreancierEur?: number | null;
  revenusActuelsCreancierEur?: number | null;
  nbEnfantsACharge?: number | null;
  ageEnfants?: number[] | null;
  modeResidenceActuel?: ModeResidence | null;
  modeResidenceDemande?: ModeResidence | null;
}
```

### Response

```ts
interface RevisionsPostDivorceResponse {
  caseFileId: string;
  // input echo (mêmes champs que la request)
  typeRevision: TypeRevisionPostDivorce;
  dateDecisionInitiale: string;
  changementCirconstance: string;
  revenusInitialsDebiteurEur: number | null;
  revenusActuelsDebiteurEur: number | null;
  revenusInitialsCreancierEur: number | null;
  revenusActuelsCreancierEur: number | null;
  nbEnfantsACharge: number | null;
  ageEnfants: number[] | null;
  modeResidenceActuel: ModeResidence | null;
  modeResidenceDemande: ModeResidence | null;
  // calculs
  ecartRevenusPct: number | null;
  modificationSubstantielle: boolean;
  motivationSuffisante: boolean;
  scoreGlobal: number;
  verdictRevisionPossible: 'ELEVEE' | 'MOYENNE' | 'FAIBLE';
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}
```

## 3. Comportement nominal

1. Le composant `<app-revisions-post-divorce-section>` est intégré au panel F-IA-04 via le `TOOL_REGISTRY` (clé `F-FA-13-revisions-post-divorce`).
2. À l'ouverture, il appelle GET. Si 200 → mode "résultat persisté" (form masqué). Si 404 → mode "form" + tentative de pré-fill IA.
3. L'avocat sélectionne le `typeRevision` ; les champs additionnels affichent **conditionnellement** :
   - `PENSION_ALIMENTAIRE` ou `PRESTATION_COMPENSATOIRE` → 4 inputs revenus (€) + textarea changement.
   - `RESIDENCE` ou `DROIT_VISITE` → nbEnfants + chips ageEnfants + 2 mat-select modeResidence (actuel / demandé).
   - `DEMENAGEMENT_PARENT` → bloc d'information distance/impact (champ texte libre intégré au `changementCirconstance`).
4. POST envoyé → bannière verdict navy/or/rouge-classique, score, cartes "Modification substantielle" / "Motivation suffisante" / éventuellement "Écart revenus %", messages, baseJuridique + formule.
5. Bouton "Modifier" pour repasser en form mode.

## 4. Cas d'erreur

- Backend non disponible / 5xx → snackbar rouge "Erreur lors du calcul".
- Form invalide (typeRevision absent / dateDecisionInitiale absente / changement < 20 chars) → bouton "Analyser" disabled.
- `workspaceCountry !== 'FRANCE'` → bannière info redirigeant vers F-FA-21 (à venir).
- Backend mergé après frontend : tests utilisent un `HttpTestingController` mocké.

## 5. Critères d'acceptation

- [ ] Composant standalone `RevisionsPostDivorceSectionComponent`.
- [ ] Inputs : `caseFileId`, `workspaceCountry`, `aiData?: Partial<FamilleExtractedData>`, `procedureChecks`, `aiQuestions`, `piecesManquantes`.
- [ ] Form : mat-select typeRevision + datepicker date + textarea ≥ 20 + champs conditionnels.
- [ ] Pré-fill IA depuis `aiData.revenusAnnuelsEpoux1Eur` / `revenusAnnuelsEpoux2Eur` (mappés à `revenusActuelsCreancier/Debiteur` selon le typeRevision sélectionné).
- [ ] Validation IA F-IA-03 : `coherenceAlerts` computed sur revenus + nbEnfants ; popover via `CoherencePopoverTriggerDirective`.
- [ ] POST + GET via `RevisionsPostDivorceService`.
- [ ] `CaseDashboardRefreshService.triggerRefresh()` après POST succès.
- [ ] `MatSnackBar` pour erreurs (pas d'alert).
- [ ] Gate `workspaceCountry !== 'FRANCE'` avec bannière info.
- [ ] JetBrains Mono pour `baseJuridique`, `formule`, écart %.
- [ ] Entrée `TOOL_REGISTRY` `F-FA-13-revisions-post-divorce`.
- [ ] Tests Jest ≥ 12 cas (mount, form valid, conditionnels, POST, GET 200, GET 404, pré-fill IA, alertes F-IA-03, gate BE, verdict mapping).
- [ ] Self-check pre-commit passe les seuils.

## 6. Plan de test (Jest, HttpTestingController)

| Test | Scénario |
|---|---|
| 1 | Mount default collapsed=true, isFranceGate true |
| 2 | toggleCollapse switches collapsed |
| 3 | typeRevision PENSION_ALIMENTAIRE → champs revenus visibles, residence/age masqués |
| 4 | typeRevision RESIDENCE → champs nbEnfants/age/mode visibles, revenus masqués |
| 5 | formValid : typeRevision + date + changement ≥ 20 chars |
| 6 | GET 200 → form hidden + result populated |
| 7 | GET 404 → form mode + prefillFromAi |
| 8 | calculate POST → result + success snackbar + refresh |
| 9 | calculate error → red snackbar |
| 10 | pré-fill IA depuis aiData (revenus mappés selon typeRevision) |
| 11 | manual change clears IA badge |
| 12 | coherence alert REVENUS si écart ≥ 10 % vs IA |
| 13 | alerts hidden once result rendered (showForm=false) |
| 14 | workspaceCountry BELGIQUE → no HTTP + isFranceGate false |
| 15 | verdict mapping ELEVEE/MOYENNE/FAIBLE (label + class) |
| 16 | ngOnChanges(aiData) post-mount refreshes pré-fill |

## 7. Hors périmètre

- Génération de la requête juridique (futur F-FA-13-03 ou repris dans F-133).
- Liaison F-145 piecesManquantes spécifique aux décisions JAF antérieures (out-of-scope, deviendra `DECISION_JAF_ORIGINALE`).
- Équivalent Belgique (F-FA-21 — à créer).

## 8. Tables / endpoints / composants impactés

- **Endpoint** : POST/GET `/api/v1/case-files/{caseFileId}/revisions-post-divorce` (consommé).
- **Composants nouveaux** :
  - `frontend/src/app/core/models/revisions-post-divorce.model.ts`
  - `frontend/src/app/core/services/revisions-post-divorce.service.ts`
  - `frontend/src/app/case-files/revisions-post-divorce-section/*` (4 fichiers)
- **Composants modifiés** :
  - `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` (ajout entrée TOOL_REGISTRY).

## 9. Pattern de référence

- Canonique `harcelement-licenciement-nul-section` (`ai-skills/frontend-coherence-audit.md` §5).
- Miroir Famille : `divorce-faute-section` (mat-select + scoring) + `divorce-alteration-section` (verdict probabilité).
- Builder partagé `CoherenceAlertBuilder` (SF-155-05).

## 10. Analyse de cohérence transversale

- **Auth / Principal** : aucun changement.
- **Workspace context** : utilise `workspaceCountry` standard.
- **Plans / limites** : aucun gate plan.
- **Navigation / routing** : aucun changement.
- **Outil décisionnel métier** :
  - Composants jumeaux scannés : F-FA-08 (altération), F-FA-09 (faute), F-FA-10 (accepté), F-FA-05 (partage immo). Tous indépendants ; pas d'overlap fonctionnel.
  - Pattern conditionnel sur typeRevision : équivalent à `immigration-title-decision-section` (mat-select MOTIF → champs visibles selon le motif). Aucune divergence d'invariant.
  - Equivalent FR : F-FA-13. Equivalent BE : F-FA-21 (à ouvrir au backlog avant clôture F-FA-13).

## 11. Impact par domaine métier

- **Droit du travail** : non applicable.
- **Immigration** : non applicable.
- **Famille** : SF spécifique au domaine **Famille**. Le concept de "révision" s'applique également BE (mais au sein de l'Enquête sociale + tribunal famille — F-FA-21).
- **Pays** : France uniquement V1 (gate workspaceCountry === 'FRANCE'). Belgique → ouverture F-FA-21 au backlog.

## 12. Parité des domaines métier (niveau ≥ 5)

Cet outil est de **niveau 5 (scoring)** :
- Famille FR : F-FA-13 (cette SF).
- Famille BE : F-FA-21 (à ajouter au backlog — révision décision tribunal famille BE).
- Droit du travail : non pertinent (pas de "révision post-rupture" en droit du travail dans le même sens).
- Immigration : non pertinent (pas de "révision de décision" dans le même sens — recours → autre outil F-IM-09/10).

→ Une seule feature jumelle à ouvrir : F-FA-21 (Famille BE révision tribunal famille). Sera ajoutée au backlog après merge SF-FA-13-02.

## 13. Nouveau pattern UI ou service partagé

- Pas de nouveau service partagé (réutilise `CoherenceAlertBuilder`, `CoherencePopoverTriggerDirective`, `CaseDashboardRefreshService`, `MatSnackBar`).
- Pas de nouveau composant transverse.
- Pattern "form conditionnel selon enum" déjà établi par `immigration-title-decision-section` — réutilisé tel quel.
