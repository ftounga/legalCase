# Mini-spec — F-FA-09 / SF-FA-09-02 Divorce pour faute FR — FRONTEND

## Objectif

Composant Angular standalone `divorce-faute-section` (FR uniquement, art. 242 Cciv) consommant l'API `POST + GET /api/v1/case-files/{caseFileId}/divorce-faute` exposée par SF-FA-09-01. Affiche un verdict sur la probabilité du divorce pour faute, le tort estimé, des fourchettes de dommages-intérêts (art. 266) et de prestation compensatoire (art. 270), avec pré-fill IA et alertes de cohérence F-IA-03.

## Contrat API (importé de SF-FA-09-01)

- **POST** `/api/v1/case-files/{caseFileId}/divorce-faute`
- **GET** `/api/v1/case-files/{caseFileId}/divorce-faute`

### Request body
```json
{
  "fautesInvoquees": ["ADULTERE", "VIOLENCES"],
  "preuvesDocumentaires": true,
  "tortsAdverseInvoques": false,
  "dureeMariageAnnees": 12,
  "revenusAnnuelsDemandeurEur": 24000,
  "revenusAnnuelsDefendeurEur": 60000,
  "dateDepotAssignation": "2026-04-25"
}
```

Codes faute (8) : `ADULTERE`, `VIOLENCES`, `ABANDON`, `OUTRAGES`, `DEVOIR_ASSISTANCE`, `DEVOIR_FIDELITE`, `DEVOIR_COMMUNAUTE_VIE`, `AUTRE`.

### Response
```json
{
  "caseFileId": "uuid",
  "fautesInvoquees": ["ADULTERE", "VIOLENCES"],
  "preuvesDocumentaires": true,
  "tortsAdverseInvoques": false,
  "dureeMariageAnnees": 12,
  "revenusAnnuelsDemandeurEur": 24000,
  "revenusAnnuelsDefendeurEur": 60000,
  "dateDepotAssignation": "2026-04-25",
  "country": "FRANCE",
  "nombreFautesInvoquees": 2,
  "solidariteeFautesOk": true,
  "risqueTortsPartages": false,
  "scoreGlobal": 72,
  "verdictProbabiliteDivorceFaute": "ELEVEE | MOYENNE | FAIBLE",
  "verdictTortsEstimes": "EXCLUSIF_DEFENDEUR | PARTAGES | IMPREDICTIBLE",
  "damagesInteretsArt266FourchetteMin": 2000,
  "damagesInteretsArt266FourchetteMax": 8000,
  "prestationCompensatoireFourchetteMin": 24000,
  "prestationCompensatoireFourchetteMax": 60000,
  "criteresNonRemplis": [],
  "formule": "...",
  "baseJuridique": "Art. 242-246 + 266 + 270 Cciv",
  "messages": ["..."]
}
```

### Codes erreur
- 400 si `fautesInvoquees` vide ou champs requis manquants
- 404 GET si aucune analyse persistée → form mode
- 409 si dossier non FR/DROIT_FAMILLE (gate backend)

## Form (FR uniquement)

| Champ | UI | Required |
|---|---|---|
| `fautesInvoquees` | `mat-select multiple` (8 options : "Adultère", "Violences conjugales", "Abandon du domicile", "Outrages / injures", "Devoir d'assistance", "Devoir de fidélité", "Devoir communauté de vie", "Autre") | Oui — au moins 1 |
| `preuvesDocumentaires` | `mat-slide-toggle` (constats huissier, témoignages, mains courantes, jugements antérieurs) | Oui (boolean default false) |
| `tortsAdverseInvoques` | `mat-slide-toggle` (demande reconventionnelle possible) | Oui (boolean default false) |
| `dureeMariageAnnees` | `mat-input` type number, min=0, step=1 | Oui (≥0) |
| `revenusAnnuelsDemandeurEur` | `mat-input` type number, min=0, step=100 | Oui (≥0) |
| `revenusAnnuelsDefendeurEur` | `mat-input` type number, min=0, step=100 | Oui (≥0) |
| `dateDepotAssignation` | `<input type="date">` (pas MatDatepicker — convention canonique) | Non |

`formValid` = au moins 1 faute + revenus ≥ 0 + durée mariage ≥ 0.

## Affichage résultat

1. **Bannière verdict** (`verdictProbabiliteDivorceFaute`) :
   - `ELEVEE` → palette navy + icône `gavel`
   - `MOYENNE` → palette or (accent gold)
   - `FAIBLE` → palette rouge classique (réservé alerte critique uniquement)
2. **Carte "Torts estimés"** (`verdictTortsEstimes`) : EXCLUSIF_DEFENDEUR / PARTAGES / IMPREDICTIBLE en libellé humain.
3. **Carte "Nombre de fautes invoquées"** : nombre + chip listant les codes (libellés humains).
4. **Carte "Dommages-intérêts art. 266"** : min – max en €.
5. **Carte "Prestation compensatoire (art. 270)"** : min – max en €.
6. **Liste `<ul>` `messages`** rendue avec `LegalCitationsPipe`.
7. **`baseJuridique` + `formule`** en `JetBrains Mono`.

Bouton "Modifier" pour réafficher le form.

## Pré-fill IA

Via `@Input() aiData?: TravailExtractedData | null` — null-safe.

Champs pré-remplis depuis `aiData` :
- `dureeMariageAnnees` (si > 0)
- `revenusAnnuelsDemandeurEur` (si > 0)
- `revenusAnnuelsDefendeurEur` (si > 0)
- `dateDepotAssignation` (ISO YYYY-MM-DD)
- `fautesInvoquees` (liste code) si `aiData.fautesDetectees?: string[]` exposée par le pipeline IA

> Nouveau champ optionnel `fautesDetectees?: string[]` ajouté à `TravailExtractedData` — extraction non implémentée par le pipeline IA aujourd'hui (no-op gracieux). Backlog backend pour brancher la détection (constats huissier, mains courantes).

`provenance<Field>` signals + badges `<mat-icon>auto_awesome</mat-icon> Pré-rempli depuis l'analyse`. Toute modif manuelle (`onXxxChange`) efface le badge.

## F-IA-03 — alertes de cohérence

`coherenceAlerts` (computed) :
- `REVENUS_DEMANDEUR` : divergence > 10 % entre IA et saisie avocat (mêmes seuils canoniques que F-DT-09).
- `REVENUS_DEFENDEUR` : idem.
- `DUREE_MARIAGE` : divergence > 1 an (entier — ratio non significatif sur petites valeurs).
- `FAUTES_INVOQUEES` : `aiData.fautesDetectees` non vide et avocat n'a sélectionné aucune faute IA, ou ensemble de fautes IA ≠ avocat.

Affichage badge or (palette F-IA-03 standard) + popover via `CoherencePopoverTriggerDirective` (clé `source-explanations` à mapper si dispo).

## Gate workspaceCountry

- `FRANCE` → form actif.
- `BELGIQUE` → bannière info "Divorce pour faute — procédure française uniquement (art. 242 Cciv). Pour la Belgique, voir F-FA-11 (à venir)" — **pas masquage silencieux** (règle CLAUDE.md, leçon F-155).

## Composants impactés

### Nouveaux fichiers
- `frontend/src/app/core/models/divorce-faute.model.ts`
- `frontend/src/app/core/services/divorce-faute.service.ts`
- `frontend/src/app/case-files/divorce-faute-section/divorce-faute-section.component.ts`
- `frontend/src/app/case-files/divorce-faute-section/divorce-faute-section.component.html`
- `frontend/src/app/case-files/divorce-faute-section/divorce-faute-section.component.scss`
- `frontend/src/app/case-files/divorce-faute-section/divorce-faute-section.component.spec.ts`
- `docs/features/F-FA-09/SF-FA-09-02-frontend-divorce-faute.md`

### Modifications
- `frontend/src/app/core/models/case-analysis.model.ts` : ajout `fautesDetectees?: string[]` à `TravailExtractedData`.

### Hors scope (NE PAS modifier)
- `decisional-tools-panel.component.ts` (entrée TOOL_REGISTRY documentée ci-dessous, intégration dans une SF ultérieure ou par F-IA-04 panel maintainer).
- `docs/PRODUCT_SPEC.md` (mise à jour post-merge).

## TOOL_REGISTRY entry (à documenter, intégration ultérieure)

```typescript
// import à ajouter en tête :
import { DivorceFauteSectionComponent } from '../divorce-faute-section/divorce-faute-section.component';

// entry à ajouter dans le Map :
['F-FA-09-divorce-faute', {
  component: DivorceFauteSectionComponent,
  inputs: (ctx) => ({
    caseFileId: ctx.caseFileId,
    workspaceCountry: ctx.workspaceCountry,
    aiData: ctx.synthesis?.travailExtractedData,
  }),
}],
```

## Tests (≥ 10)

1. Mount sans erreur (FRANCE).
2. `formValid` faux si `fautesInvoquees` vide ou tous revenus < 0.
3. `formValid` vrai si au moins 1 faute + revenus ≥ 0.
4. `calculate()` POST → résultat affiché + snackbar succès + `dashboardRefresh.triggerRefresh()` appelé.
5. `calculate()` erreur 400 → snackbar `panelClass: 'snack-error'`.
6. GET 200 au mount → form masqué, valeurs persistées affichées, pas de badge IA.
7. GET 404 → reste en mode formulaire ; pré-fill IA appliqué si `aiData` présent.
8. Pré-fill IA : `aiData` rempli → champs renseignés + badges IA.
9. `onRevenusDemandeurChange` efface le badge IA.
10. Coherence alert : divergence revenus > 10 % → alerte présente.
11. Gate `workspaceCountry='BELGIQUE'` → bannière info, form non rendu.
12. `toggleCollapse` ouvre/ferme la section.
13. Verdict `ELEVEE` rendu en bannière navy ; verdict `FAIBLE` rendu en bannière rouge.

## Design system

- Standalone component, palette navy/or — rouge classique uniquement pour alerte critique (verdict FAIBLE).
- `Inter` pour le corps du texte, `JetBrains Mono` pour `formule` + `baseJuridique`.
- Datepicker : `<input type="date">` (convention canonique — pas `MatDatepicker`).
- Citations juridiques rendues via `LegalCitationsPipe` (consistance F-155).
- `MatSnackBar` pour erreurs (pas d'`alert()` ni `confirm()`).

## Pattern de référence

- **Canonical** : `harcelement-licenciement-nul-section` (HLN, F-DT-11) — structure complète : computed motifs, prefillFromAi, coherenceAlerts builder, sections form/result, popover F-IA-03, gating ngOnChanges.
- **Multi-source coherence + pays** : `immigration-title-decision-section` (F-IM-05).
- **Gate FR-only avec bannière info** : `oqtf-sans-delai-section` (F-IM-08-04).

## Analyse de cohérence transversale

- Outils décisionnels (niveau 5 – scoring) du même domaine famille FR :
  - `divorce-checklist-section` : checklist procédurale (niveau 1) — non concerné.
  - `divorce-consentement-scoring-section` (F-152) : scoring dédié à un autre régime de divorce — déjà séparé. Pas de fusion.
  - `divorce-accepte-*` (backend) : régime distinct — frontend séparé hors scope.
  - `divorce-alteration-*` (backend) : régime distinct — frontend séparé hors scope.
- Invariant respecté : un outil = une situation métier (art. 242 Cciv).

## Parité des domaines métier (niveau 5 — scoring)

- DROIT_TRAVAIL : scoring équivalent existe (F-DT-08, F-DT-12, etc.).
- IMMIGRATION : F-IM-05 / F-IM-06 / F-IM-07 (scoring équivalents).
- FAMILLE : F-FA-09 (cette SF) — divorce pour faute. Régimes voisins : F-FA-11 (BE — backlog), F-150 (consentement), F-151 (accepté), F-152 (scoring consentement), F-153 (altération). Couverture acceptable.

## Impact par domaine métier

- DROIT_TRAVAIL : non applicable (régime du divorce — droit famille pur).
- IMMIGRATION : non applicable.
- FAMILLE : OUI, FR uniquement (art. 242 Cciv). BE → bannière redirection F-FA-11 (backlog).

## Nouveau pattern UI ou service partagé

Aucun nouveau pattern partagé introduit — ce composant suit strictement le template HLN canonique. La nouvelle entrée `fautesDetectees?: string[]` sur `TravailExtractedData` est consommée par cet outil seul ; à généraliser à F-FA-11 (BE) lorsqu'elle sera implémentée.

## Hors scope

- Détection IA des fautes (`fautesDetectees`) : pipeline backend en backlog — l'attribut est ajouté à `TravailExtractedData` mais aucune extraction LLM n'est connectée dans cette SF.
- Évaluation détaillée par faute (preuves individuelles) : seul le juge se prononce.
- Sélection torts détaillée (exclusif d'un côté ou de l'autre) : produit une estimation indicative.
- Modification de `decisional-tools-panel.component.ts` : à intégrer ultérieurement (entrée documentée ci-dessus).
- Mise à jour `docs/PRODUCT_SPEC.md` : post-merge uniquement.
- Implémentation BE (F-FA-11) : backlog distinct.
