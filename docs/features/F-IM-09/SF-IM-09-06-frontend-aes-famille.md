# Mini-spec — F-IM-09 / SF-IM-09-06 AES Famille (L.435-1) — FRONTEND

## Identifiant
`F-IM-09 / SF-IM-09-06`

## Feature parente
`F-IM-09` — AES 4 motifs distincts (critique)

## Statut `draft` · Date `2026-04-25` · Branche `feat/SF-IM-09-06-frontend-aes-famille`

## Objectif

Livrer le frontend de l'outil décisionnel **AES voie familiale** (art. L.435-1 CESEDA + circulaire Valls 28/11/2012), en consommant l'API backend `SF-IM-09-02` mergée (PR #506). Outil **single-country FRANCE** ; bannière info pour la BELGIQUE. Mêmes patterns canoniques que `harcelement-licenciement-nul-section` (template canonique 2026-04-24) avec pré-fill IA + validation F-IA-03.

## Contrat API (importé de SF-IM-09-02-backend)

**Endpoint** : `POST | GET /api/v1/case-files/{caseFileId}/aes-famille`

**Request body** (POST) :
```json
{
  "dateEntreeFrance": "YYYY-MM-DD",
  "dureePresenceMois": 0,
  "conjointFrancaisOuRegulier": false,
  "enfantsScolarisesFrance": 0,
  "dureeScolaritePlusAncienEnfantAnnees": 0,
  "preuvesInsertion": false,
  "menaceOrdrePublic": false,
  "dateDepotDemande": "YYYY-MM-DD"  // nullable
}
```

**Response** :
```json
{
  "caseFileId": "uuid",
  "dateEntreeFrance": "YYYY-MM-DD",
  "dureePresenceMois": 0,
  "conjointFrancaisOuRegulier": false,
  "enfantsScolarisesFrance": 0,
  "dureeScolaritePlusAncienEnfantAnnees": 0,
  "preuvesInsertion": false,
  "menaceOrdrePublic": false,
  "dateDepotDemande": "YYYY-MM-DD",
  "country": "FRANCE",
  "presence5AnsOk": false,
  "presence10AnsOk": false,
  "liensFamiliauxOk": false,
  "insertionOk": false,
  "pasMenace": true,
  "scoreGlobal": 0,
  "verdictProbabiliteAcceptation": "FAIBLE | MOYENNE | ELEVEE",
  "criteresNonRemplis": ["..."],
  "dateExpirationInstructionSiDemande": "YYYY-MM-DD | null",
  "formule": "AES liens personnels et familiaux (L.435-1) : ...",
  "baseJuridique": "Art. L.435-1 CESEDA + Circulaire Valls 28/11/2012",
  "messages": ["..."]
}
```

**Codes d'erreur** :
- `400` validation (date future, dépôt antérieur entrée, négatifs)
- `400` workspaceCountry != FRANCE ou domaine != DROIT_IMMIGRATION
- `404` non-membre du workspace ou pas d'analyse persistée (GET)

## Form (champs frontend)

- `dateEntreeFrance` : `<input type="date">` required
- `dureePresenceMois` : number ≥ 0 required
- `conjointFrancaisOuRegulier` : `mat-slide-toggle`
- `enfantsScolarisesFrance` : number ≥ 0
- `dureeScolaritePlusAncienEnfantAnnees` : number ≥ 0
- `preuvesInsertion` : `mat-slide-toggle`
- `menaceOrdrePublic` : `mat-slide-toggle`
- `dateDepotDemande` : `<input type="date">` optional

## Affichage résultat

- **Bannière verdict** (palette navy/or classique — pas de rouge dominant : pas d'urgence < 72h)
  - `ELEVEE` (≥ 80) → fond or clair + icône `check_circle`
  - `MOYENNE` (50-79) → fond navy clair + icône `info_outline`
  - `FAIBLE` (< 50) → fond rouge classique (--danger) + icône `error`
- **Score** affiché en grand (X / 100)
- **Critères non remplis** rendus en chips
- **Carte "Délai d'instruction"** si `dateExpirationInstructionSiDemande` présent (silence vaut rejet) — JetBrains Mono pour la date
- **Formule** en JetBrains Mono
- **Base juridique** en JetBrains Mono (italic)
- **Messages** en `<ul>` avec citations `<code>` (CESEDA, circulaire) via `legalCitations` pipe

## Pré-fill IA

`@Input() aiData?: Partial<ImmigrationExtractedData>` — pré-remplit gracieusement (no-op si champs absents) :
- `dateEntreeFrance` ← `aiData.dateEntreeFrance` (nouveau champ à ajouter au model si absent — gracefully `(aiData as any).dateEntreeFrance`)
- `dureePresenceMois` ← calculé depuis `aiData.dateEntreeFrance` si présente

> Limite assumée : `ImmigrationExtractedData` actuel n'expose pas `dateEntreeFrance` typée — fallback gracieux via cast `any`. Le backend pourra étoffer plus tard (hors scope).

Provenance signals : `provenanceDateEntree`, `provenanceDureePresence`. Badge "Pré-rempli depuis l'analyse" + effacement au changement manuel via `onXxxChange()`.

## Cohérence F-IA-03

`coherenceAlerts` computed avec :
- Field `DATE_ENTREE_FRANCE` : divergence aiData vs saisie (alerte WARNING)
- Field `DUREE_PRESENCE` : divergence > 6 mois entre IA et saisie

Composant `<app-coherence-popover-trigger>` via `CoherencePopoverTriggerDirective`.

## Gate country

`@Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE'`. Si BELGIQUE, bannière info "Outil France uniquement — la Belgique applique l'art. 9bis loi 15/12/1980 (régularisation humanitaire), voir l'outil 9bis dédié." (pas de masquage silencieux).

## Composants

- `frontend/src/app/core/models/aes-famille.model.ts` (Request/Response/Verdict types)
- `frontend/src/app/core/services/aes-famille.service.ts` (HttpClient wrapper)
- `frontend/src/app/case-files/aes-famille-section/`
  - `aes-famille-section.component.ts`
  - `aes-famille-section.component.html`
  - `aes-famille-section.component.scss`
  - `aes-famille-section.component.spec.ts`

## Tests (≥ 10)

1. mount + GET 404 → mode form
2. GET 200 → résultat affiché, form masqué
3. form validators : dateEntreeFrance + dureePresenceMois requis
4. submit ok → POST + résultat + refresh dashboard
5. submit erreur 400 → snackbar
6. pré-fill IA `dateEntreeFrance` + provenance IA
7. badge IA effacé au changement manuel
8. coherence alert `DUREE_PRESENCE` si écart > 6 mois
9. gate BE → bannière info
10. gate FR → form visible
11. toggle collapsed
12. `criteresNonRemplis` rendus

## Design system

- Palette navy/or classique (var --ds-* + #C9973A or)
- JetBrains Mono pour `formule`, `baseJuridique`, dates ISO
- Inter pour le reste
- `<input type="date">` (pas de MatDatepicker)
- `MatSnackBar` pour erreurs (pas alert/confirm)
- `CaseDashboardRefreshService?.triggerRefresh()` après POST succès
- Pas d'emoji

## Pattern de référence

- Canonique : `harcelement-licenciement-nul-section` (F-DT-11-02)
- Pré-fill IA + cohérence : `immigration-title-decision-section` (F-IM-05-04) et `immigration-recours-section` (SF-155-04 immigration)

## TOOL_REGISTRY (à intégrer dans une SF parallèle SF-IA-04 : panel inclusion)

```typescript
import { AesFamilleSectionComponent } from '../aes-famille-section/aes-famille-section.component';

['F-IM-09-aes-famille', {
  component: AesFamilleSectionComponent,
  inputs: (ctx) => ({
    caseFileId: ctx.caseFileId,
    workspaceCountry: ctx.workspaceCountry,
    aiData: ctx.synthesis?.immigrationExtractedData,
    procedureChecks: ctx.procedureChecks,
    aiQuestions: ctx.aiQuestions,
    piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
  }),
}],
```

> Note gouvernance : la consigne actuelle interdit la modification de `decisional-tools-panel.component.ts` dans cette SF. Snippet documenté pour la SF d'intégration ultérieure (F-IA-04 / batch d'intégration AES).

## Hors scope

- Modification `decisional-tools-panel.component.ts` (SF d'intégration séparée)
- Modification `docs/PRODUCT_SPEC.md` (post-merge)
- Étoffer `ImmigrationExtractedData` (champs `dateEntreeFrance`, etc.) — fallback gracieux suffisant

## Impact par domaine métier

Outil sensible au domaine **DROIT_IMMIGRATION FR uniquement**. Hors scope DT/Famille/BE.

## Parité des domaines métier

Niveau 5 (scoring). Parité backend déjà validée par SF-IM-09-02 (équivalents F-DT-08 / F-FA-07 livrés). Cette SF est purement frontend donc l'invariant parité s'applique en amont.

## Analyse de cohérence transversale

Pattern strictement copié du canonique `harcelement-licenciement-nul-section` + `immigration-title-decision-section`. Aucun nouveau pattern UI ou service partagé introduit. Datepicker `<input type="date">`, MatSnackBar, palette navy/or classique, JetBrains Mono pour formule + baseJuridique, gate FRANCE avec bannière info BE.
