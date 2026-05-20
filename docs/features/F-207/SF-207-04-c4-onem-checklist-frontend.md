# Mini-spec — F-207 / SF-207-04 C4 ONEM — Checklist conformité — FRONTEND

## Identifiant
`F-207 / SF-207-04`

## Feature parente
`F-207` — P1 Travail BE — 8 outils urgences BE-only

## Statut `ready` · Date `2026-05-20` · Branche `feat/SF-207-04-c4-onem-checklist-frontend`

## Pattern de référence
- **Template canonique** : `prescription-be-litige-travail-section` (SF-207-01b) — signal-based, gate strict BE, pré-fill IA + F-IA-03, refresh dashboard.
- **Pattern checklist multi-cases** : `documents-fin-contrat-section` (F-DT-32-02) — mentions documentaires.

> **Réalignement contrat backend (2026-05-20)** — La SF-207-02-backend (PR #1123, mergée)
> livre un contrat différent de la version textuelle de cette mini-spec :
> - 10 mentions obligatoires (au lieu de 8) — cf. enum `C4OnemChecklistMention` backend
> - 3 verdicts `CONFORME` / `NON_CONFORME` / `RISQUE_EXCLUSION_FAUTE_GRAVE`
> - Champs C4 directs (`raisonSocialeEmployeur`, `numeroBce`, `nomSalarie`,
>   `numeroNationalRegistre`, `dateEntreeService`, `dateSortieService`,
>   `categorieOnem`, `motifExplicite`, `fauteGraveMentionnee`,
>   `preavisPresteJours`, `dernierSalaireMensuelBrut`).
> - Endpoint `/api/v1/case-files/{id}/decision-tools/c4-onem-checklist`.
> Ce frontend consomme exactement le contrat backend réel (mémoire
> `feedback_pre_merge_endpoint_check`).

---

## Objectif

Livrer le composant Angular `<app-c4-onem-checklist-section>` qui consomme l'endpoint `POST/GET /api/v1/case-files/{id}/c4-onem-checklist` (SF-207-03 backend) pour vérifier les **8 mentions obligatoires du C4 ONEM** (AR 25/11/1991 art. 92), détecter le risque d'**exclusion ONEM motif faute grave** et rendre un verdict `CONFORME / À_RECTIFIER / À_CONTESTER`.

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre un dossier BE travail. Le panel F-IA-04 affiche l'outil `F-207-c4-onem-checklist` (ALWAYS_ON BELGIQUE priority 46).
2. Header `C4 ONEM — CHECKLIST CONFORMITÉ (BE)` + chip statut (`CONFORME` navy / `A_RECTIFIER` or / `A_CONTESTER` rouge).
3. GET au `ngOnInit` ; 200 → mode résultat ; 404 → formulaire + pré-fill IA.
4. Formulaire (12 champs) :
   - `c4Recu` (`<mat-slide-toggle>`, obligatoire) — défaut depuis `aiData.c4OnemRecuDetecte`.
   - **Si `c4Recu=true`** :
     - `dateEmissionC4` (`<input type="date">`, obligatoire ≤ J+1) — pré-rempli depuis `aiData.dateEmissionC4Onem` ; à défaut `aiData.dateLicenciement + 7j` (dérivation frontend).
     - `motifRuptureC4` (`<select>`, obligatoire) — défaut `MOTIF_GRAVE_EXCLUSIF` si flag `aiData.c4OnemMotifFauteGraveDetecte`, sinon `LICENCIEMENT_ORDINAIRE`.
     - **Bloc « 8 mentions obligatoires »** : 8 `<mat-checkbox>` initialisées par dérivation depuis `aiData` (cf. §Pré-fill IA).
5. POST à la soumission → mode résultat + `MatSnackBar` + `CaseDashboardRefreshService.triggerRefresh()`.
6. Mode résultat :
   - Bandeau navy `CONFORME`, or `A_RECTIFIER`, **rouge** `A_CONTESTER` (risque d'exclusion ONEM = urgence absolue).
   - `mentionsManquantes` listées en liste à puces (Inter).
   - Si `verdict=A_CONTESTER` : message orienté SF-207-06 (« Ouvrez l'outil « Contestation décision ONEM » pour préparer le recours sous 1 mois »).
   - `dateButoirRectification` (JetBrains Mono) + `delaiContestation=30` jours.
   - `formule`, `baseJuridique` JetBrains Mono.
   - `messages[]` rendus via `LegalCitationsPipe`.
   - Mention « Échéance rectification suivie dans l'onglet Suivi » (ajustement #3 étape 0bis).
   - Bouton « Modifier ».

### Gate pays

- `workspaceCountry !== 'BELGIQUE'` → bannière info navy : « Le C4 ONEM est un document belge — pas d'équivalent FR. ». Pas d'appel HTTP.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| GET 404 | Mode formulaire + pré-fill IA |
| GET 5xx | `MatSnackBar` + form vide |
| POST 400 `c4Recu=false` + autres champs | snackbar message backend |
| POST 400 date future | snackbar |
| POST 403/404 | snackbar générique |

---

## Contrat consommé (figé par SF-207-03)

**POST /api/v1/case-files/{caseFileId}/c4-onem-checklist**

Request :
```ts
{
  c4Recu: boolean;
  dateEmissionC4?: string;
  motifRuptureC4?: 'LICENCIEMENT_ORDINAIRE' | 'MOTIF_GRAVE_EXCLUSIF' | 'DEMISSION' | 'RUPTURE_AMIABLE' | 'FIN_CDD' | 'AUTRE';
  mentionsObligatoires?: {
    identiteSalarie: boolean; identiteEmployeur: boolean; dateRupture: boolean;
    motifRupture: boolean; dureeOccupation: boolean; regimeTravail: boolean;
    remunerationDerniere: boolean; causeChomage: boolean;
  };
}
```

Response 200 (champs principaux) :
```ts
{
  caseFileId; c4Recu; dateEmissionC4; motifRuptureC4; mentionsObligatoires;
  mentionsManquantes: string[];
  risqueExclusionOnem: boolean;
  delaiContestation: number;          // 30
  dateButoirRectification: string;
  verdict: 'CONFORME' | 'A_RECTIFIER' | 'A_CONTESTER';
  baseJuridique; formule; messages: string[];
  createdAt; updatedAt;
}
```

---

## Pré-fill IA (RÈGLE FONDAMENTALE)

`@Input() aiData?: TravailExtractedData | null`.

| Champ formulaire | Source `TravailExtractedData` | Règle |
|---|---|---|
| `c4Recu` | `aiData.c4OnemRecuDetecte` | Toggle pré-coché si flag `true`. Signal `provenanceC4Recu`. |
| `dateEmissionC4` | `aiData.dateEmissionC4Onem` (livré SF-207-03) — fallback `aiData.dateLicenciement + 7j` (dérivation frontend) | Signal `provenanceDateEmission`. Si fallback utilisé → provenance = `IA_DERIVE` (badge dédié « Estimation à partir de la date de rupture »). |
| `motifRuptureC4` | dérivé : `aiData.c4OnemMotifFauteGraveDetecte=true` → `MOTIF_GRAVE_EXCLUSIF` ; sinon `LICENCIEMENT_ORDINAIRE` | Signal `provenanceMotif`. |
| `mentionsObligatoires.identiteSalarie` | `aiData.nomSalarie != null` | Dérivation frontend. |
| `mentionsObligatoires.identiteEmployeur` | `aiData.nomEmployeur != null` | Dérivation. |
| `mentionsObligatoires.dateRupture` | `aiData.dateLicenciement != null` | Dérivation. |
| `mentionsObligatoires.motifRupture` | `aiData.motifLicenciement != null` | Dérivation. |
| `mentionsObligatoires.dureeOccupation` | `aiData.dateEntree != null && aiData.dateLicenciement != null` | Dérivation. |
| `mentionsObligatoires.regimeTravail` | `aiData.typeContrat != null` | Dérivation. |
| `mentionsObligatoires.remunerationDerniere` | `aiData.salaireBrutMensuel != null` | Dérivation. |
| `mentionsObligatoires.causeChomage` | `aiData.motifLicenciement != null` | Dérivation. |

**Toutes dérivations sont pré-cochées par l'IA**. L'avocat décoche manuellement si l'IA s'est trompée (l'inverse est rarement nécessaire — F-IA-03 alerte si décochage entre en conflit avec un champ IA non null).

`getPrefillCount(input): number` : compte 3 (c4Recu + dateEmission + motif si flags présents) + 8 dérivations possibles ; max 11 si tout est extrait.

---

## Validation F-IA-03 (RÈGLE FONDAMENTALE)

`coherenceAlerts = computed<Partial<Record<C4OnemAlertField, CoherenceAlert>>>()`.

### Fields audités

- `C4_RECU` — divergence si `aiData.c4OnemRecuDetecte=true` mais toggle décoché (l'avocat affirme ne pas avoir reçu de C4 alors que l'IA en a détecté un).
- `MOTIF_RUPTURE` — divergence si `aiData.c4OnemMotifFauteGraveDetecte=true` mais `motifRuptureC4 !== 'MOTIF_GRAVE_EXCLUSIF'` (l'avocat conteste le flag faute grave de l'IA).
- `DATE_EMISSION` — divergence si `aiData.dateEmissionC4Onem` présent et `dateEmissionC4` saisie diverge.

Sources : `IA` (principale) ; F-96 / QUESTION_IA / PIECE_MANQUANTE non encore définies pour C4 — laisser builder graceful (return alerte sans contributor F96 si absent).

Gate : `!showForm()` → `{}`.

---

## TOOL_REGISTRY symétrique

- Entrée :
  ```ts
  ['F-207-c4-onem-checklist', {
    displayLabel: 'C4 ONEM — checklist conformité (BE)',
    component: C4OnemChecklistSectionComponent,
    inputs: (ctx) => ({
      caseFileId: ctx.caseFileId, workspaceCountry: ctx.workspaceCountry,
      aiData: ctx.synthesis?.travailExtractedData,
      procedureChecks: ctx.procedureChecks, aiQuestions: ctx.aiQuestions,
      piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
      standaloneMode: ctx.standaloneMode ?? false,
    }),
  }]
  ```
- `THEME_BY_TOOL_ID` : `['F-207-c4-onem-checklist', 'DOCUMENTS']`.
- `displayLabel` finissant par `(BE)` ✓ (invariant #10 étape 0bis).
- `getPrefillCount` exposé (cas 0 / 3 / 11).

---

## Mapping DashboardTile (F-167)

| Champ | Valeur |
|---|---|
| `theme` | `DOCUMENTS` |
| `toolId` | `F-207-c4-onem-checklist` |
| `title` | « C4 ONEM (BE) » |
| `value` | `CONFORME` / `À RECTIFIER` / `À CONTESTER` |
| `alertLevel` | `OK` / `WARN` / `CRITICAL` |
| `subtitle` | « AR 25/11/1991 art. 92 » + nb mentions manquantes si > 0 |

---

## Critères d'acceptation

- [ ] Composant Angular standalone `<app-c4-onem-checklist-section>` sous `frontend/src/app/case-files/c4-onem-checklist-section/`.
- [ ] Gate pays BE-only avec bannière info.
- [ ] GET ngOnInit ; 200 → résultat ; 404 → form + pré-fill.
- [ ] Pré-fill IA des 3 champs (c4Recu/dateEmission/motif) + 8 dérivations cases obligatoires.
- [ ] Badge IA `auto_awesome` + badge dédié `IA_DERIVE` pour `dateEmissionC4` fallback.
- [ ] `coherenceAlerts.C4_RECU`, `MOTIF_RUPTURE`, `DATE_EMISSION` selon §Validation F-IA-03.
- [ ] `formValid` : `c4Recu=true` → tous les autres champs requis ; `c4Recu=false` → form OK sans détails.
- [ ] POST succès → mode résultat + snackbar + `triggerRefresh()` + `markForCheck()`.
- [ ] Mode résultat : navy CONFORME, or A_RECTIFIER, rouge A_CONTESTER. `mentionsManquantes` listées. `dateButoirRectification` JetBrains Mono.
- [ ] Si `verdict=A_CONTESTER` → message d'amorce vers SF-207-06 visible.
- [ ] Mention « Échéance rectification suivie dans l'onglet Suivi » présente (ajustement #3).
- [ ] Entrée TOOL_REGISTRY + THEME_BY_TOOL_ID + displayLabel `(BE)`.
- [ ] `DecisionToolVisibilityIntegrityIT` vert ; mapper `DashboardTile` DOCUMENTS livré + `DashboardTileToolIdIntegrityIT` vert.
- [ ] Self-check grep pré-commit : (a) tool_id présent dans 2 maps frontend, (b) mapper backend, (c) displayLabel finissant `(BE)`.
- [ ] ≥ 14 tests Jest verts.

---

## Périmètre

### Hors scope

- Backend (SF-207-03).
- OCR du PDF C4 pour pré-cocher automatiquement les 8 mentions visuelles — couvert F-122 (extraction enrichie).
- Génération d'une lettre rectificative employeur — couvert par template Markdown copiable côté SF-207-06 (contestation), pas par cette SF.

---

## Valeurs initiales

| Champ | Valeur initiale |
|---|---|
| `collapsed` | `true` |
| `showForm` | `true` |
| `c4Recu` | dérivé `aiData.c4OnemRecuDetecte`, défaut `false` |
| `motifRuptureC4` | dérivé (cf. §Pré-fill) |
| `mentionsObligatoires.*` | dérivations IA (cf. §Pré-fill) |

---

## Contraintes de validation (UI)

| Champ | Obligatoire | Format |
|---|---|---|
| `c4Recu` | Oui | boolean |
| `dateEmissionC4` | Oui si `c4Recu=true` | YYYY-MM-DD ≤ J+1 |
| `motifRuptureC4` | Oui si `c4Recu=true` | enum 6 |
| `mentionsObligatoires.*` | Oui si `c4Recu=true` | 8 booléens |

---

## Technique

### Endpoint consommé

| Méthode | URL | Backend SF |
|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/c4-onem-checklist` | SF-207-03 |
| GET | idem | SF-207-03 |

### Composants Angular créés

- `c4-onem-checklist.model.ts`
- `c4-onem-checklist.service.ts`
- `c4-onem-checklist-section.component.{ts,html,scss,spec.ts}`

### Modifications

- `decisional-tools-panel.component.ts` : +1 TOOL_REGISTRY + 1 THEME.
- `CaseFileDashboardService.java` : +1 mapper DOCUMENTS.

---

## Plan de test (≥ 14 Jest)

1. BE → GET appelé.
2. FR → bannière info, pas d'appel.
3. GET 200 → mode résultat hydraté.
4. GET 404 → form + pré-fill.
5. Pré-fill `c4Recu` depuis flag IA + badge.
6. Pré-fill `motifRuptureC4=MOTIF_GRAVE_EXCLUSIF` si flag IA.
7. Dérivation 8 mentions : `nomSalarie != null` → `identiteSalarie=true`.
8. Fallback `dateEmissionC4 = dateLicenciement + 7j` quand IA n'a pas extrait — badge `IA_DERIVE`.
9. `onC4RecuChange` efface provenance + dévalide les champs détaillés si `false`.
10. `coherenceAlerts.MOTIF_RUPTURE` présent si flag IA != saisie.
11. `coherenceAlerts.C4_RECU` présent si flag IA=true mais toggle décoché.
12. `formValid` OK si `c4Recu=false` (autres champs ignorés).
13. `formValid` faux si `c4Recu=true` + 1 champ requis manquant.
14. POST succès → mode résultat + snackbar + `triggerRefresh()`.
15. POST `A_CONTESTER` → bandeau rouge + message orientant SF-207-06.
16. POST 400 → snackbar erreur backend.
17. `getPrefillCount` retourne 0 / 3 / 11 selon `aiData`.
18. TOOL_REGISTRY+THEME : entrée présente, `displayLabel='C4 ONEM — checklist conformité (BE)'`.

### Isolation workspace : non applicable côté composant.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Outil décisionnel métier** — nouvel outil checklist niveau 1. Invariant respecté (pas de C4 en FR, BE-only structurel).

### Composants impactés

| Composant | Impact | Test |
|---|---|---|
| `decisional-tools-panel.component.ts` | +TOOL_REGISTRY + THEME | `DecisionToolVisibilityIntegrityIT` |
| `CaseFileDashboardService.java` | +mapper DOCUMENTS | `DashboardTileToolIdIntegrityIT` |

### Smoke E2E

- [x] `cd e2e && npm test`.

---

## Dépendances

### Bloquantes

- **SF-207-01** (livre `c4OnemRecuDetecte`, `c4OnemMotifFauteGraveDetecte`, extension `TravailExtractedData`).
- **SF-207-03** (livre `dateEmissionC4Onem` + endpoint).

### Aval

- **SF-207-06** consomme un message d'amorce visuel quand `verdict=A_CONTESTER` (purement cosmétique frontend).

---

## Notes et décisions

- **Rouge pour `A_CONTESTER`** justifié — risque d'exclusion ONEM 4 à 52 sem = urgence absolue pour le client.
- **Pas de pré-fill `motifRuptureC4=AUTRE`** — défaut `LICENCIEMENT_ORDINAIRE` est plus utile (cas majoritaire).
- **Niveau outil** : 1 (checklist documentaire) — non applicable parité ≥ 5.
- **Référence audit** : `docs/features/F-191/audit-be-travail-exhaustif.md` §3.3.
