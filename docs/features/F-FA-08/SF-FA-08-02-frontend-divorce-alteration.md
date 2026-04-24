# Mini-spec — F-FA-08 / SF-FA-08-02 Divorce altération définitive lien conjugal FR — FRONTEND

## Objectif

Composant Angular `<app-divorce-alteration-section>` consommant l'API SF-FA-08-01
(POST + GET `/api/v1/case-files/{caseFileId}/divorce-alteration`) pour permettre
à l'avocat de simuler la recevabilité d'un divorce pour altération définitive
du lien conjugal (art. 237 Cciv) et obtenir une fourchette indicative de
prestation compensatoire.

## Contrat API (importé de SF-FA-08-01-backend)

- **POST + GET** `/api/v1/case-files/{caseFileId}/divorce-alteration`
- **Request** :
  ```ts
  {
    dateCessationVieCommune: string;        // ISO YYYY-MM-DD
    preuvesSeparationDocumentaires: boolean;
    tentativesReconciliation: boolean;
    dureeMariageAnnees: number;
    revenusAnnuelsEpoux1Eur: number;
    revenusAnnuelsEpoux2Eur: number;
    patrimoineCommunSignificatif: boolean;
    dateAssignation?: string | null;        // ISO YYYY-MM-DD
  }
  ```
- **Response** :
  ```ts
  {
    caseFileId: string;
    dateCessationVieCommune: string;
    preuvesSeparationDocumentaires: boolean;
    tentativesReconciliation: boolean;
    dureeMariageAnnees: number;
    revenusAnnuelsEpoux1Eur: number;
    revenusAnnuelsEpoux2Eur: number;
    patrimoineCommunSignificatif: boolean;
    dateAssignation: string | null;
    country: 'FRANCE';
    dureeSeparationAnnees: number;
    delaiObjectifOk: boolean;
    absencePreuveReconciliation: boolean;
    conditionsReunies: boolean;
    scoreGlobal: number;                    // 0-100
    verdictProbabilite: 'ELEVEE' | 'MOYENNE' | 'FAIBLE';
    criteresNonRemplis: string[];
    prestationCompensatoireFourchetteMin: number;
    prestationCompensatoireFourchetteMax: number;
    formule: string;
    baseJuridique: string;
    messages: string[];
  }
  ```

## Form (8 champs)

Form réactif (FormsModule + signals) :
1. `dateCessationVieCommune` — `<input type="date">` (requis)
2. `dateAssignation` — `<input type="date">` (optionnel)
3. `dureeMariageAnnees` — input number (requis, ≥ 0)
4. `preuvesSeparationDocumentaires` — `mat-slide-toggle`
5. `tentativesReconciliation` — `mat-slide-toggle`
6. `revenusAnnuelsEpoux1Eur` — input number (requis, ≥ 0)
7. `revenusAnnuelsEpoux2Eur` — input number (requis, ≥ 0)
8. `patrimoineCommunSignificatif` — `mat-slide-toggle`

Validators : tous les nombres positifs ou nuls, dateCessation requise, dateAssignation
ne doit pas être antérieure à dateCessation (sinon backend renvoie 400 → snackbar).

## Affichage résultat

- **Bannière verdict** : navy (DISPONIBLE) si `ELEVEE`, gold si `MOYENNE`,
  rouge-classique si `FAIBLE`. Palette standard (cf. coherence-audit).
- **Carte "Délai objectif : 1 an de séparation"** : `dureeSeparationAnnees`
  formaté + check (si `delaiObjectifOk`) ou cross.
- **Carte "Prestation compensatoire indicative"** : min € — max € (JetBrains
  Mono pour les chiffres, formaté FR).
- **Liste `<ul>` messages** rendus via `LegalCitationsPipe`.
- **`baseJuridique`** + **`formule`** en JetBrains Mono.
- **Liste `criteresNonRemplis`** en bloc info si non vide.
- Bouton "Modifier" → re-affiche le formulaire.

## Pré-fill IA (OBLIGATOIRE)

`@Input() aiData?: FamilleAiData | null` — interface locale typée :
```ts
interface FamilleAiData {
  dateCessationVieCommune?: string | null;
  dureeMariageAnnees?: number | null;
  revenusAnnuelsEpoux1Eur?: number | null;
  revenusAnnuelsEpoux2Eur?: number | null;
  patrimoineCommunSignificatif?: boolean | null;
}
```

`FamilleExtractedData` n'existe pas encore dans `case-analysis.model.ts`. La
SF-FA-08-02 ne crée PAS ce type partagé (out-of-scope). Le composant définit
une interface locale `FamilleAiData` minimaliste qui sera remplacée par le
vrai type au moment où le pipeline IA Famille étendra `case-analysis.model.ts`
(feature future). Tout champ inconnu de `aiData` est ignoré silencieusement.

Champs pré-remplis si non null/positif : `dateCessationVieCommune`,
`dureeMariageAnnees`, `revenusAnnuelsEpoux1Eur`, `revenusAnnuelsEpoux2Eur`,
`patrimoineCommunSignificatif`.

Pour chaque champ pré-rempli : signal `provenance<Field>: 'IA' | null` +
badge `<mat-icon>auto_awesome</mat-icon> Pré-rempli depuis l'analyse`.
Le badge disparaît dès que l'avocat modifie manuellement (handler
`on<Field>Change()` reset le signal à null).

## Coherence F-IA-03

`coherenceAlerts: computed<Partial<Record<DAAlertField, CoherenceAlert>>>()`
basé sur `CoherenceAlertBuilder` partagé (SF-155-05/06). Champs surveillés :
- `DUREE_MARIAGE` : si IA donne `dureeMariageAnnees` et que la valeur saisie
  diffère de plus de 1 an.
- `REVENUS_EPOUX1` / `REVENUS_EPOUX2` : si écart relatif > 10 % avec la valeur IA.

Sources : IA + `procedureChecks` (critereCode `DA_DUREE_MARIAGE`,
`DA_REVENUS_EPOUX1`, `DA_REVENUS_EPOUX2`) + `aiQuestions` (réponse "oui"
sur même critereCode) + `piecesManquantes`.

Gate masquage : alertes uniquement si `showForm()` (pattern anti-bug
SF-IA-03-12).

## Composants & dépendances

- `frontend/src/app/core/models/divorce-alteration.model.ts` — types Request /
  Response + interface locale `FamilleAiData`.
- `frontend/src/app/core/services/divorce-alteration.service.ts` — wrapper
  `HttpClient` (POST + GET).
- `frontend/src/app/case-files/divorce-alteration-section/` —
  composant standalone + spec.

Imports : `CommonModule`, `FormsModule`, `MatButtonModule`, `MatIconModule`,
`MatFormFieldModule`, `MatInputModule`, `MatSelectModule`,
`MatProgressSpinnerModule`, `MatSlideToggleModule`, `LegalCitationsPipe`,
`CoherencePopoverTriggerDirective`. `DecimalPipe`.

## Gate workspaceCountry

`@Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE'`. Si BELGIQUE,
afficher une **bannière info** (palette navy avec icône info, pas masquage
silencieux) renvoyant vers F-FA-11 (désunion irrémédiable BE, à venir).
Form non rendu en BELGIQUE.

## Tests (≥ 10)

1. mount + collapsed initial
2. formValid : vrai si tous les champs requis OK ; faux sinon
3. submit OK → POST + display result + `triggerRefresh()` + snackbar succès
4. submit erreur → snackbar `panelClass: 'snack-error'`
5. GET 200 au mount → form masqué + result rempli
6. GET 404 → form visible
7. pré-fill IA avec aiData → valeurs + badges 'IA'
8. handler change manuel → badge effacé
9. coherence alert REVENUS_EPOUX1 sur écart > 10 %
10. workspaceCountry BELGIQUE → bannière info + form caché
11. toggleCollapse fonctionne
12. editMode ré-affiche le form

## Design system

- Palette navy (#1A3A5C) / or (#C9973A) / texte primaire / rouge-classique
  réservé au verdict FAIBLE — pas de rouge dominant.
- Typo Inter partout, JetBrains Mono pour `baseJuridique`, `formule`,
  fourchette prestation et dates ISO.
- Datepicker `<input type="date">` natif (pas MatDatepicker) — convention
  `frontend-coherence-audit`.

## Pattern de référence

`harcelement-licenciement-nul-section` (canonique palette navy/or — F-DT-11) +
`immigration-title-decision-section` (pattern prefillFromAi + coherenceAlerts
multi-sources F-IM-05).

## TOOL_REGISTRY (out of scope SF — sera ajouté en PR orchestrateur)

```ts
['F-FA-08-divorce-alteration', {
  component: DivorceAlterationSectionComponent,
  inputs: (ctx) => ({
    caseFileId: ctx.caseFileId,
    workspaceCountry: ctx.workspaceCountry,
    aiData: null, // FamilleExtractedData pas encore exposée par la synthèse
    procedureChecks: ctx.procedureChecks,
    aiQuestions: ctx.aiQuestions,
    piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
  }),
}],
```

## Hors scope

- Type `FamilleExtractedData` partagé dans `case-analysis.model.ts`
- Entrée `TOOL_REGISTRY` dans `decisional-tools-panel.component.ts`
- Mise à jour `docs/PRODUCT_SPEC.md`
- Compteur temps réel "il reste X jours avant 1 an"
- Gestion enfants mineurs / mesures provisoires (= F-FA-12)
- Cas violence conjugale (= F-FA-15)
- Procédure BE (= F-FA-11)

## Analyse de cohérence transversale

- **Outils décisionnels FR famille déjà livrés** : F-152 (consentement scoring),
  F-FA-04 (pension alimentaire), F-FA-05 (partage immo), F-FA-06 (calendrier
  garde), F-FA-07 (checklist divorce). Aucun ne traite l'altération du lien
  conjugal art. 237 — pas de doublon.
- **Pattern UI** : aligné sur le canonique
  `harcelement-licenciement-nul-section`. Aucun nouveau composant partagé
  introduit.
- **Pré-fill IA Famille** : pas de pipeline IA `FamilleExtractedData` aujourd'hui ;
  la SF expose `aiData` typé localement et fail-open si absent. À harmoniser
  quand le pipeline IA Famille sera étendu (feature future).

## Impact par domaine métier

- **DROIT_FAMILLE FR uniquement.** Outil décisionnel niveau 5 (scoring) FR.
- BE équivalent = F-FA-11 (désunion irrémédiable, art. 229 CC belge — backlog).
- Travail / immigration : non concernés.

## Parité des domaines métier

Outil de niveau 5 (scoring 0-100). Domaines :
- DROIT_FAMILLE FR : couvert ici (SF-FA-08-02).
- DROIT_FAMILLE BE : F-FA-11 (backlog, déjà identifié dans
  `DivorceAlterationCalculator` comme "outil single-country FR").
- DROIT_TRAVAIL : non applicable (concept divorce).
- IMMIGRATION : non applicable.
