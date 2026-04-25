# Mini-spec — F-FA-11 / SF-FA-11-02 Désunion irrémédiable BE (art. 229 CC) — FRONTEND

## Objectif

Composant Angular `<app-divorce-desunion-be-section>` consommant l'API SF-FA-11-01
(POST + GET `/api/v1/case-files/{caseFileId}/desunion-irremediable-be`) pour
permettre à l'avocat (workspace BE) de simuler la recevabilité d'une demande de
divorce pour désunion irrémédiable (Code civil belge, art. 229) — le seul mode
de divorce contentieux possible en droit belge depuis la loi du 27/04/2007. Le
composant gère deux régimes : séparation consentue (≥ 6 mois) ou non consentue
(≥ 12 mois).

## Contrat API (importé de SF-FA-11-01-backend, mini-spec backend en parallèle)

- **POST + GET** `/api/v1/case-files/{caseFileId}/desunion-irremediable-be`
- **Request** :
  ```ts
  {
    dateSeparation: string;          // ISO YYYY-MM-DD
    separationConsentue: boolean;    // true → seuil 6 mois ; false → 12 mois
    preuvesSeparation: boolean;
    preuvesDocumentaires: boolean;
    tentativesReconciliation: boolean;
    dateAssignation?: string | null; // ISO YYYY-MM-DD (optionnel)
  }
  ```
- **Response** :
  ```ts
  {
    caseFileId: string;
    dateSeparation: string;
    separationConsentue: boolean;
    preuvesSeparation: boolean;
    preuvesDocumentaires: boolean;
    tentativesReconciliation: boolean;
    dateAssignation: string | null;
    dureeSeparationMois: number;
    seuilSeparationMois: number;     // 6 ou 12
    delaiObjectifOk: boolean;
    conditionsReunies: boolean;
    scoreGlobal: number;             // 0-100
    verdictProbabilite: 'ELEVEE' | 'MOYENNE' | 'FAIBLE';
    baseJuridique: string;
    formule: string;
    messages: string[];
    country: 'BELGIQUE';
  }
  ```
- **Codes erreur** : 400 si `dateAssignation < dateSeparation` ou date dans le
  futur ; 403 si dossier d'un autre workspace ; 404 sur GET si aucune analyse
  persistée.

## Form (6 champs)

Form réactif (FormsModule + signals) :
1. `dateSeparation` — `<input type="date">` (requis)
2. `separationConsentue` — `mat-slide-toggle` (label "Séparation consentue par
   les 2 époux"). `true` ⇒ seuil 6 mois ; `false` ⇒ seuil 12 mois.
3. `preuvesSeparation` — `mat-slide-toggle`
4. `preuvesDocumentaires` — `mat-slide-toggle`
5. `tentativesReconciliation` — `mat-slide-toggle`
6. `dateAssignation` — `<input type="date">` (optionnel)

Validators : `dateSeparation` requise, `dateAssignation` ≥ `dateSeparation` si
fournie. Submit désactivé tant que `formValid()` faux. Errors backend → snackbar
rouge.

## Affichage résultat

- **Bannière verdict** : navy (palette `--ok`) si `ELEVEE`, gold (`--warn`) si
  `MOYENNE`, rouge-classique (`--ko`) si `FAIBLE`. Icône check_circle / warning /
  error_outline. Score `<JetBrains Mono>` "Score X/100".
- **Carte "Délai objectif"** : `dureeSeparationMois` mois écoulés vs
  `seuilSeparationMois` (6 ou 12 selon `separationConsentue`), check si OK
  sinon cross.
- **Carte "Conditions"** : `conditionsReunies` check + libellé.
- **Liste `<ul>`** des `messages` rendus via `LegalCitationsPipe`.
- **`baseJuridique`** + **`formule`** en JetBrains Mono.
- Bouton "Modifier" (mat-stroked-button) → re-affiche le form.

## Pré-fill IA (OBLIGATOIRE — règle CLAUDE.md ligne 190)

`@Input() aiData?: Partial<FamilleExtractedData> | null` — type partagé
existant (`frontend/src/app/core/models/divorce-accepte.model.ts`) ; on
l'enrichit avec les champs nécessaires à la désunion BE :

```ts
interface FamilleExtractedData {
  // ... champs existants
  dateSeparation?: string | null;        // SF-FA-11-02 : pré-fill date séparation
  separationConsentue?: boolean | null;  // SF-FA-11-02 : pré-fill consentement
}
```

Tout champ inconnu de `aiData` est ignoré silencieusement (no-op gracieux). La
SF n'introduit pas de nouveau type partagé — elle réutilise l'existant.

Champs pré-remplis : `dateSeparation` → `dateSeparation`, `separationConsentue`
→ `separationConsentue`. Les booléens `preuves*` et `tentativesReconciliation`
n'ont pas de mapping IA natif — crochets prêts pour évolutions futures (signal
`provenance*` + handler `on*Change`).

Pour chaque champ pré-rempli : signal `provenance<Field>: signal<'IA' | null>` +
badge `<mat-icon>auto_awesome</mat-icon> Pré-rempli depuis l'analyse`. Le badge
disparaît dès que l'avocat modifie manuellement (handler `on*Change()` reset le
signal à null). Pré-fill invoqué dans `ngOnInit()` ET `ngOnChanges()` (même
pattern `belgian-9bis-section` / `divorce-alteration-section`).

## Coherence F-IA-03 (OBLIGATOIRE — règle CLAUDE.md ligne 190)

`coherenceAlerts: computed<Partial<Record<DesUBeAlertField, CoherenceAlert>>>()`
basé sur `CoherenceAlertBuilder` partagé (`shared/coherence-popover/coherence-alert-builder.ts`).
Champs surveillés :

- `DATE_SEPARATION` : si IA `aiData.dateSeparation` ≠ saisie avocat (égalité
  stricte string ISO).
- `SEPARATION_CONSENTUE` : si IA `aiData.separationConsentue` ≠ saisie.
- `DATE_ASSIGNATION` : alerte sur sources externes uniquement (F-96 /
  QUESTION_IA / PIECE_MANQUANTE) — pas d'IA native.

Sources hiérarchisées (F-96 > QUESTION_IA > IA détection > Pièce manquante) :
- `procedureChecks` filtrés sur `critereCode` ∈ {`DESU_BE_DATE_SEPARATION`,
  `DESU_BE_CONSENTEE`, `DESU_BE_DATE_ASSIGNATION`} avec `statut === 'VERIFIED'`.
- `aiQuestions` réponses commençant par "oui" sur même `critereCode`.
- `aiData` (IA détection directe).
- `piecesManquantes` (contributor additionnel).

Multi-source `MULTI` produit automatiquement par le builder quand ≥ 2 sources
convergent sur la même `expectedDisplay`.

Gate masquage : alertes uniquement si `showForm()` (pattern anti-bug).

## Composants & dépendances

- `frontend/src/app/core/models/divorce-desunion-be.model.ts` — types Request /
  Response + alias `DesunionIrremediableBeVerdict`.
- `frontend/src/app/core/services/divorce-desunion-be.service.ts` — wrapper
  `HttpClient` (POST + GET).
- `frontend/src/app/case-files/divorce-desunion-be-section/` — composant
  standalone + spec.
- Modification minimale : ajout de 2 champs optionnels dans
  `FamilleExtractedData` (`divorce-accepte.model.ts`).

Imports composant : `CommonModule`, `FormsModule`, `MatButtonModule`,
`MatIconModule`, `MatFormFieldModule`, `MatInputModule`, `MatSlideToggleModule`,
`MatProgressSpinnerModule`, `LegalCitationsPipe`,
`CoherencePopoverTriggerDirective`, `DecimalPipe`.

## Gate workspaceCountry

`@Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE'`. Si FRANCE,
afficher une **bannière info** navy (icône info, pas masquage silencieux)
mentionnant les outils français équivalents (F-FA-08 altération définitive,
F-FA-10 divorce accepté). Form non rendu en FRANCE, aucun appel HTTP.

## Tests (≥ 12)

1. mount + collapsed initial
2. toggleCollapse, editMode helpers
3. formValid : vrai si dateSeparation OK ; faux sinon ; faux si dateAssignation
   < dateSeparation
4. GET 200 → form masqué + result rempli + valeurs hydratées
5. GET 404 → form visible + reste éditable
6. POST OK → result + showForm=false + snackbar succès + dashboardRefresh
7. POST erreur → snackbar 'snack-error' + calculating=false
8. POST ignoré si form invalide
9. workspaceCountry FRANCE → pas d'appel HTTP + isBelgiumGate=false
10. workspaceCountry BELGIQUE → GET émis
11. pré-fill IA → valeurs + provenance 'IA' (dateSeparation, separationConsentue)
12. pré-fill IA absent → valeurs nulles + provenance null
13. manual change → provenance reset à null
14. coherence alert DATE_SEPARATION si IA ≠ saisie
15. coherence alert MULTI quand F-96 + IA convergent vs saisie
16. alerts hidden lorsque showForm=false
17. ngOnChanges(aiData) post-mount applique le pré-fill si form vide
18. verdictLabel + verdictClass mappent ELEVEE/MOYENNE/FAIBLE

## Design system

- Palette navy (#1A3A5C) / or (#C9973A) / rouge-classique (#D33A2C) réservé au
  verdict FAIBLE — **pas de rouge dominant**.
- Typo Inter partout, JetBrains Mono pour `baseJuridique`, `formule`, score,
  durée mois, dates ISO.
- Datepicker `<input type="date">` natif (pas MatDatepicker) — convention
  `frontend-coherence-audit` §6.
- Snackbar pour erreurs (pas alert/confirm).

## Pattern de référence

- `divorce-alteration-section` (F-FA-08, FR — structure très proche, même
  layout résultat verdict + carte délai)
- `belgian-9bis-section` (F-IM-14, BE — gate `isBelgium`, pré-fill IA,
  coherenceAlerts via builder partagé)
- `harcelement-licenciement-nul-section` (canonique palette navy/or — F-DT-11)

## TOOL_REGISTRY (HORS SCOPE — ajout panel out-of-scope ici)

À brancher dans une SF orchestrateur ultérieure dans
`decisional-tools-panel.component.ts` :

```ts
['F-FA-11-desunion-be', {
  component: DivorceDesunionBeSectionComponent,
  inputs: (ctx) => ({
    caseFileId: ctx.caseFileId,
    workspaceCountry: ctx.workspaceCountry,
    aiData: ctx.synthesis?.familleExtracted ?? null,
    procedureChecks: ctx.procedureChecks,
    aiQuestions: ctx.aiQuestions,
    piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
  }),
}],
```

## Hors scope

- Modification de `decisional-tools-panel.component.ts` (TOOL_REGISTRY) — sera
  câblé en SF orchestrateur ultérieure.
- Mise à jour de `docs/PRODUCT_SPEC.md` (post-merge).
- Génération du document d'assignation BE (F-FA-12 BE futur).
- Calcul de la pension alimentaire / liquidation patrimoine (F-FA-04 / F-FA-05
  ne s'étendent pas à BE ici).
- Mesures provisoires JP belge (autre feature).

## Analyse de cohérence transversale

- **Outils décisionnels famille BE déjà livrés** : aucun. F-FA-08/10/13/14 sont
  FR-only et redirigent vers BE depuis leurs bannières de gate. Pas de doublon.
- **Pattern UI** : aligné sur le canonique `harcelement-licenciement-nul-section`
  + `belgian-9bis-section` (gate BE). Aucun nouveau composant partagé
  introduit. Le composant **réutilise** :
  - `CoherenceAlertBuilder` (shared/coherence-popover/) — pattern SF-155-05.
  - `CoherencePopoverTriggerDirective` — pattern SF-155-05.
  - `LegalCitationsPipe` (shared/pipes/).
  - `CaseDashboardRefreshService.triggerRefresh()` — pattern obligatoire.
- **Pré-fill IA Famille** : `FamilleExtractedData` étendu de 2 champs
  optionnels (`dateSeparation`, `separationConsentue`) — extension non
  destructive, no-op gracieux pour les composants existants qui ne les lisent
  pas. Pas d'impact sur F-FA-08, F-FA-10, F-FA-04, F-FA-05.

## Nouveau pattern UI ou service partagé

Aucun. La SF consomme uniquement l'écosystème existant (`CoherenceAlertBuilder`,
`CoherencePopoverTriggerDirective`, `LegalCitationsPipe`,
`CaseDashboardRefreshService`). Les 2 champs ajoutés à `FamilleExtractedData`
sont **optionnels** et non destructifs — pas de divergence.

## Impact par domaine métier

- **DROIT_FAMILLE BELGIQUE uniquement.** Outil décisionnel niveau 5 (scoring
  0-100). Le seul mode de divorce contentieux belge depuis 2007 — couvre 100 %
  des cas non-mutuel-consentement BE.
- DROIT_FAMILLE FR : équivalent = F-FA-08 (altération art. 237) + F-FA-10
  (accepté art. 233) — déjà livrés.
- DROIT_TRAVAIL / IMMIGRATION : non applicables (concept divorce).

## Parité des domaines métier

Outil niveau 5 (scoring). Symétrie BE :
- DROIT_FAMILLE FR : couvert par F-FA-08 + F-FA-10 (livré).
- DROIT_FAMILLE BE : couvert ici (SF-FA-11-02).
- DROIT_TRAVAIL : non applicable.
- IMMIGRATION : non applicable.

Cette SF **comble** l'asymétrie FR/BE famille héritée de F-FA-08/10 livrés
FR-only.
