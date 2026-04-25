# Mini-spec — F-IM-09 / SF-IM-09-07 AES Humanitaire (L.435-2) — FRONTEND

## Identifiant
`F-IM-09 / SF-IM-09-07`

## Feature parente
`F-IM-09` — AES 4 motifs distincts (critique)

## Statut `draft` · Date `2026-04-25` · Branche `feat/SF-IM-09-07-frontend-aes-humanitaire`

## Objectif

Livrer le frontend de l'outil décisionnel **AES voie humanitaire** (art. L.435-2 CESEDA + L.432-14 commission du titre de séjour), en consommant l'API backend `SF-IM-09-03` mergée (PR #507). Outil **single-country FRANCE** ; bannière info pour la BELGIQUE (pas d'équivalent direct — la régularisation humanitaire BE relève de l'art. 9bis loi 15/12/1980, déjà couvert par l'outil 9bis dédié). Patterns canoniques : `harcelement-licenciement-nul-section` (F-DT-11-02) + `aes-famille-section` (SF-IM-09-06) et `aes-metiers-tension-section` (SF-IM-09-05) pour pré-fill IA + cohérence F-IA-03.

## Contrat API (importé de SF-IM-09-03-backend, PR #507)

**Endpoint** : `POST | GET /api/v1/case-files/{caseFileId}/aes-humanitaire`

**Request body** (POST) :
```json
{
  "dateEntreeFrance": "YYYY-MM-DD",
  "motifHumanitaireDominant": "RISQUES_AU_RETOUR | ISOLEMENT_TOTAL | VICTIME_VIOLENCES | VICTIME_TRAITE | SITUATION_MEDICALE_PRECAIRE_HORS_L425_9 | AUTRE_HUMANITAIRE",
  "preuvesMedicales": false,
  "preuvesViolencesOuTraite": false,
  "demandeAsileDeposeeEtRejetee": false,
  "commissionTitreSejourSaisie": false,
  "menaceOrdrePublic": false,
  "dateDepotDemande": "YYYY-MM-DD"  // nullable
}
```

**Response** :
```json
{
  "caseFileId": "uuid",
  "dateEntreeFrance": "YYYY-MM-DD",
  "motifHumanitaireDominant": "...",
  "preuvesMedicales": false,
  "preuvesViolencesOuTraite": false,
  "demandeAsileDeposeeEtRejetee": false,
  "commissionTitreSejourSaisie": false,
  "menaceOrdrePublic": false,
  "dateDepotDemande": "YYYY-MM-DD | null",
  "country": "FRANCE",
  "motifEligible": false,
  "preuvesAdaptees": false,
  "commissionRequise": false,
  "pasMenace": true,
  "scoreGlobal": 0,
  "verdictProbabiliteAcceptation": "FAIBLE | MOYENNE | ELEVEE",
  "criteresNonRemplis": ["..."],
  "dateExpirationInstruction": "YYYY-MM-DD | null",
  "formule": "AES voie humanitaire (L.435-2) — motif « XXX » : score N/100, probabilité d'acceptation YYY — délai d'instruction jusqu'au …",
  "baseJuridique": "Art. L.435-2 CESEDA + L.432-14 (commission titre séjour)",
  "messages": ["..."]
}
```

**Codes d'erreur** :
- `400` validation (date d'entrée future, dépôt antérieur entrée, motif manquant)
- `400` workspaceCountry != FRANCE ou domaine != DROIT_IMMIGRATION
- `404` non-membre du workspace ou pas d'analyse persistée (GET)

## Form (champs frontend)

- `dateEntreeFrance` : `<input type="date">` required (max = aujourd'hui)
- `motifHumanitaireDominant` : `mat-select` 6 options required (libellés humains, code enum interne)
- `preuvesMedicales` : `mat-slide-toggle`
- `preuvesViolencesOuTraite` : `mat-slide-toggle`
- `demandeAsileDeposeeEtRejetee` : `mat-slide-toggle`
- `commissionTitreSejourSaisie` : `mat-slide-toggle`
- `menaceOrdrePublic` : `mat-slide-toggle`
- `dateDepotDemande` : `<input type="date">` optional (≥ dateEntreeFrance)

## Affichage résultat

- **Bannière verdict** (palette navy/or classique — pas de rouge dominant : pas d'urgence < 72h)
  - `ELEVEE` (≥ 70) → fond vert + icône `check_circle`
  - `MOYENNE` (45-69) → fond navy clair + icône `info_outline`
  - `FAIBLE` (< 45 ou menace ordre public) → fond rouge classique (--danger) + icône `error`
- **Score** affiché en grand (X / 100)
- **Critères non remplis** rendus en chips
- **Carte "Délai d'instruction"** si `dateExpirationInstruction` présent (silence vaut rejet à 6 mois)
- **Formule** + **Base juridique** en JetBrains Mono
- **Messages** en `<ul>` avec citations `<code>` (CESEDA, articles) via `legalCitations` pipe

## Pré-fill IA (OBLIGATOIRE — règle CLAUDE.md)

`@Input() aiData?: ImmigrationExtractedData | null` :
- `dateEntreeFrance` ← `(aiData as any).dateEntreeFrance` (champ non typé natif sur `ImmigrationExtractedData` — fallback gracieux pattern SF-IM-09-06)
- `dateDepotDemande` ← `aiData.dateDepotProcedure` si présente et postérieure à `dateEntreeFrance` (sinon backend rejet)
- `motifHumanitaireDominant` : pas de champ IA dédié à ce stade — no-op gracieux (l'avocat sélectionne)

> Limite assumée : `ImmigrationExtractedData` actuel n'expose pas `dateEntreeFrance` typée — fallback gracieux via cast `any`. Le backend pourra étoffer plus tard (hors scope).

Provenance signals : `provenanceDateEntree`, `provenanceMotif`, `provenanceDateDepot`. Badge `auto_awesome` "Pré-rempli depuis l'analyse" + effacement au changement manuel via `onXxxChange()`.

`prefillFromAi()` invoquée dans `ngOnInit()` ET `ngOnChanges()` (réactivité post-mount).

## Cohérence F-IA-03 (OBLIGATOIRE — règle CLAUDE.md)

`coherenceAlerts` computed via `CoherenceAlertBuilder` (helper partagé `frontend/src/app/shared/coherence-popover/coherence-alert-builder.ts`) :
- Field `DATE_ENTREE_FRANCE` : divergence aiData / F96 (`IM09H_DATE_ENTREE_FRANCE`) / pièce manquante (passeport, justificatif domicile)
- Field `MOTIF_HUMANITAIRE` : divergence F96 (`IM09H_MOTIF_HUMANITAIRE`) / QUESTION_IA / pièce manquante (certificats médicaux, plaintes)
- Field `DATE_DEPOT_DEMANDE` : divergence IA (`dateDepotProcedure`) / pièce manquante (`IM09H_DATE_DEPOT_DEMANDE`)

Hiérarchie sources `F96 > QUESTION_IA > IA > PIECE_MANQUANTE` consolidée par le builder (source `MULTI` si convergence). Directive `[appCoherencePopover]` câblée sur les 3 fields concernés (popover affichant la divergence avec source, pas de blocage technique).

`alertsSummary` computed exposé pour la bannière "N incohérence(s) avec l'analyse".

## Gate country

`@Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE'`. Si BELGIQUE, bannière info :

> "Outil France uniquement — la Belgique n'a pas d'équivalent standard à l'art. L.435-2 CESEDA ; la régularisation humanitaire BE relève de l'art. 9bis loi 15/12/1980."

Pas de masquage silencieux. Pas de GET ni POST déclenchés.

## Composants

- `frontend/src/app/core/models/aes-humanitaire.model.ts` (Request / Response / Verdict / MotifHumanitaire types)
- `frontend/src/app/core/services/aes-humanitaire.service.ts` (HttpClient wrapper)
- `frontend/src/app/case-files/aes-humanitaire-section/`
  - `aes-humanitaire-section.component.ts`
  - `aes-humanitaire-section.component.html`
  - `aes-humanitaire-section.component.scss`
  - `aes-humanitaire-section.component.spec.ts`

## Tests (≥ 12)

1. mount + GET 404 → mode form
2. GET 200 → résultat affiché, form masqué (récupération + applyResultToForm)
3. formValid : exige dateEntreeFrance + motifHumanitaireDominant
4. formValid : refuse date d'entrée future
5. formValid : refuse dateDepotDemande antérieure à dateEntreeFrance
6. submit ok → POST + résultat + snackbar succès + refreshDashboard
7. submit erreur 400 → snackbar rouge
8. pré-fill IA `dateEntreeFrance` + `dateDepotDemande` + provenance IA
9. pré-fill no-op si aiData absent
10. handlers (date entrée / motif / date dépôt) effacent les badges IA
11. coherence alert DATE_ENTREE_FRANCE si divergence avec IA
12. coherence alert MOTIF_HUMANITAIRE consolidé F96 + QUESTION_IA → MULTI
13. coherence alert DATE_DEPOT_DEMANDE si IA divergent
14. gate BE → bannière info + pas de GET/POST
15. gate FR → GET déclenché
16. toggle collapsed
17. verdict ELEVEE → banner success
18. verdict FAIBLE (menace ordre public) → banner danger + criteres
19. ngOnChanges(aiData) post-mount rafraîchit le pré-fill
20. alertes masquées si form caché (showForm=false)
21. editMode ré-affiche le form
22. labelForMotif retourne libellé humain ou fallback code

## Design system

- Palette navy/or classique (var --ds-* + #C9973A or)
- JetBrains Mono pour `formule`, `baseJuridique`, dates ISO
- Inter pour le reste
- `<input type="date">` (pas de MatDatepicker)
- `MatSnackBar` pour erreurs (pas alert/confirm)
- `CaseDashboardRefreshService?.triggerRefresh()` après POST succès
- Pas d'emoji
- Pas de gradation rouge `--danger-medium/-strong/-dark` (instruction = 6 mois — pas d'urgence < 72h)

## Pattern de référence

- Canonique : `harcelement-licenciement-nul-section` (F-DT-11-02)
- Pré-fill IA + cohérence : `immigration-title-decision-section` (F-IM-05-04)
- Patterns directs (vague 10) : `aes-famille-section` (SF-IM-09-06), `aes-metiers-tension-section` (SF-IM-09-05)

## TOOL_REGISTRY (à intégrer dans une SF parallèle SF-IA-04 : panel inclusion)

```typescript
import { AesHumanitaireSectionComponent } from '../aes-humanitaire-section/aes-humanitaire-section.component';

['F-IM-09-aes-humanitaire', {
  component: AesHumanitaireSectionComponent,
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
- Modification `docs/features/F-IM-09/CLAUDE.md` (post-merge)
- Étoffer `ImmigrationExtractedData` (champ `dateEntreeFrance` typé) — fallback gracieux suffisant à ce stade

## Impact par domaine métier

Outil sensible au domaine **DROIT_IMMIGRATION FR uniquement**. Hors scope DT/Famille/BE.
- DT : non applicable (régime AES = droit des étrangers).
- Famille : non applicable (la dimension humanitaire familiale relève de L.435-1, déjà couvert par SF-IM-09-06).
- BE : pas d'équivalent direct (régularisation humanitaire = 9bis loi 15/12/1980, déjà couvert par l'outil 9bis dédié) → bannière info.

## Parité des domaines métier

Niveau 5 (scoring). La parité backend est déjà validée par SF-IM-09-03 (équivalents F-DT-08 / F-FA-07 livrés en mars 2026). Cette SF est purement frontend — l'invariant parité s'applique en amont au backend.

## Analyse de cohérence transversale

Pattern strictement copié des canoniques `harcelement-licenciement-nul-section` + `immigration-title-decision-section` + `aes-famille-section` (SF-IM-09-06) + `aes-metiers-tension-section` (SF-IM-09-05). Aucun nouveau pattern UI ou service partagé introduit. Datepicker `<input type="date">`, `MatSnackBar`, palette navy/or classique, JetBrains Mono pour `formule` + `baseJuridique`, gate FRANCE avec bannière info BE, `CoherenceAlertBuilder` partagé + directive `[appCoherencePopover]` sur 3 fields clés.

Self-check pré-commit (≥ seuils CLAUDE.md ligne 190) :

| Check | Seuil | Valeur attendue |
|---|---|---|
| `CoherenceAlertBuilder` (occurrences `.ts`) | ≥ 2 | OK |
| import `coherence-alert-builder` | ≥ 1 | OK |
| `[appCoherencePopover]` (HTML) | ≥ 3 | OK (3 fields) |
| `prefillFromAi` / `prefill(` | ≥ 2 | OK (ngOnInit + load + ngOnChanges) |
| `auto_awesome` (HTML) | ≥ 2 | OK (3 badges) |
| `provenance` (.ts) | ≥ 6 | OK (3 signals + 3 setters + handlers) |
| `coherenceAlerts` (.ts) | ≥ 1 | OK |
| handlers `on<X>Change` | ≥ 2 | OK (8 handlers) |
| pas d'`interface CoherenceAlert` locale | == 0 | OK (réutilisation `shared/`) |
