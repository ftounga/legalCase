# SF-DT-14-02 — Frontend PSE (Plan de Sauvegarde de l'Emploi)

## Objectif (1 phrase)

Exposer côté Angular un outil décisionnel "PSE — critères de validité" (FR —
art. L.1233-24-1 + L.1233-30 + L.1233-57-2 + L.1233-61 + L.1235-7-1) consommant
l'API `/api/v1/case-files/{id}/pse-analysis` livrée par SF-DT-14-01, avec
pré-remplissage IA + alertes de cohérence F-IA-03, gate
`workspaceCountry === 'FRANCE'` (bannière info, pas masquage silencieux).

Branche : `feat/SF-DT-14-02-frontend-pse`.

---

## Contrat API (importé de SF-DT-14-01 backend, figé)

### Endpoint

`POST + GET /api/v1/case-files/{caseFileId}/pse-analysis`

### Body de requête

```ts
{
  tailleEntrepriseSalaries: number;            // ≥ 0
  nombreLicenciementsEnvisages: number;        // ≥ 0
  periodeJours: number;                        // ≥ 0 (typiquement 30)
  modeAdoption: 'ACCORD_COLLECTIF_MAJORITAIRE' | 'DOCUMENT_UNILATERAL';
  csaeConsulteAvis: 'FAVORABLE' | 'DEFAVORABLE' | 'NON_CONSULTE';
  dreetsStatut: 'VALIDE' | 'HOMOLOGUE' | 'REFUS' | 'EN_COURS';
  dateNotificationDreets?: string;             // YYYY-MM-DD (optionnel)
  contenuMesures: ContenuMesure[];             // 8 valeurs (cf. enum)
  dateProjet: string;                          // YYYY-MM-DD
}
```

`ContenuMesure` ∈ `'RECLASSEMENT_INTERNE' | 'RECLASSEMENT_EXTERNE' | 'FORMATION'
| 'AIDE_CREATION' | 'INDEMNITES_SUPRA' | 'CONGE_RECLASSEMENT' |
'CELLULE_RECLASSEMENT' | 'AUTRE'`.

### Réponse

```ts
{
  caseFileId: string;
  pseRequis: boolean;                          // déclenchement L.1233-24-1
  scoreConformite: number;                     // 0..100
  verdictValidite: 'VALIDE' | 'CONTESTABLE' | 'NUL';
  criteresRemplis: CritereValidite[];          // enum 5 valeurs
  criteresManquants: CritereValidite[];
  delaiContestationJours: number;              // toujours 60 (L.1235-7-1)
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: string;                             // 'FRANCE' attendu
}
```

`CritereValidite` ∈ `'CSE_CONSULTE' | 'DREETS_VALIDE_OU_HOMOLOGUE' |
'CONTENU_RECLASSEMENT' | 'MODE_DREETS_COHERENT' | 'MESURES_MULTIPLES'`.

### Codes erreurs

- 400 : body invalide (champs manquants, valeurs négatives, pays BE — backend
  rejette explicitement avec un message).
- 403 : accès dossier interdit (filtre workspace).
- 404 : `caseFileId` inconnu — utilisé silencieusement par le frontend pour
  fallback "pas encore d'analyse persistée".

---

## Comportement nominal

1. Le composant est instancié par le panel F-IA-04 dès que la règle
   `decision_tool_visibility_rules` retourne `F-DT-14-pse-validite`
   (`tool_id` figé par la migration 164). Outil ALWAYS_ON pour
   `DROIT_DU_TRAVAIL` × `FRANCE`.
2. Si `workspaceCountry !== 'FRANCE'` → bannière info "Outil français
   uniquement (PSE est un mécanisme propre au droit du travail français
   — l'équivalent BE congé-réorganisation Loi 1976/Loi Renault est traité
   en feature jumelle backlog)". Pas de POST, pas de form — pattern jumeau
   `refere-prudhomal-section` (FR-only).
3. À l'ouverture (FR) :
   - GET `/api/v1/case-files/{id}/pse-analysis` (silencieux 404)
     → si 200, valeurs persistées affichées et résultat rendu.
   - Si 404 et `aiData` présent → `prefillFromAi()` rempli les champs
     pré-remplissables (`dateProjet`) avec badge IA.
4. L'avocat complète le formulaire en 4 sections collapsibles (axes
   PSE) :
   - **Déclenchement** : `tailleEntrepriseSalaries`,
     `nombreLicenciementsEnvisages`, `periodeJours` (défaut 30),
     `dateProjet` — affiche live le badge `pseRequis`.
   - **Mode + DREETS** : `modeAdoption` (radio), `dreetsStatut` (radio),
     `dateNotificationDreets` (date optionnelle).
   - **CSE** : `csaeConsulteAvis` (radio).
   - **Contenu** : `contenuMesures` (checkboxes multi-select 8 options).
5. Submit → POST → refresh `CaseDashboardRefreshService.triggerRefresh()` +
   snackbar succès. Le résultat affiche bandeau verdict :
   - `VALIDE` → navy/info.
   - `CONTESTABLE` → or/warning.
   - `NUL` → rouge/critical (palette urgence — bloquant procédural).
   Avec score, critères remplis/manquants, base juridique JetBrains Mono
   italique, formule JetBrains Mono, messages F-IA-03 via
   `LegalCitationsPipe`.
6. Bouton "Modifier" → revenir au formulaire (`showForm = true`).

## Cas d'erreur

- POST 400 → snackbar rouge `panelClass: 'snack-error'` avec message backend
  (ou fallback "Erreur lors du calcul").
- POST 403/404/500 → snackbar rouge.
- GET 404 → fallback gracieux : reste en mode formulaire, applique pré-fill
  IA.

---

## Pré-remplissage IA (RÈGLE FONDAMENTALE — FAIL si absent)

`@Input() aiData?: TravailExtractedData | null` (type existant).

### Champs pré-remplissables

| Champ frontend | Source IA | Règle |
|---|---|---|
| `dateProjet` | `aiData.dateLicenciement` (YYYY-MM-DD) | Si format valide, recopie tel quel comme date du projet (la date de notification du licenciement reflète à 1ʳᵉ approximation la date du projet PSE). |

> Note : `tailleEntrepriseSalaries` et `nombreLicenciementsEnvisages` ne sont
> pas extraits par le pipeline IA travail actuel. Le composant pourra les
> brancher ultérieurement (no-op gracieux). Cette SF se concentre sur ce qui
> est extractible aujourd'hui pour ne pas créer de signal fantôme.

### Pattern obligatoire

- Méthode privée `prefillFromAi()` invoquée dans `ngOnInit()` (après GET 404)
  ET dans `ngOnChanges()` si `aiData` change avant première résolution.
- Signal `provenanceDateProjet = signal<'IA' | null>(null)`.
- Badge UI `<mat-icon>auto_awesome</mat-icon> Pré-rempli depuis l'analyse`
  affiché si `provenanceDateProjet() === 'IA'`.
- Handler `onDateProjetChange()` qui remet le signal à `null` au changement
  manuel.

---

## Validation IA F-IA-03 (RÈGLE FONDAMENTALE — FAIL si absent)

`coherenceAlerts = computed<Partial<Record<PseAlertField, ...>>>()` construit
avec `CoherenceAlertBuilder` (helper partagé
`frontend/src/app/shared/coherence-popover/coherence-alert-builder.ts`).

### Field audité

- `DATE_PROJET` — divergence si l'IA a `dateLicenciement` et l'avocat saisit
  une date éloignée (> 30 jours d'écart absolu).

### Sources

- `IA` — analyse du dossier (`aiData.dateLicenciement`).
- `F96` — `procedureChecks` avec `critereCode === 'PSE_DATE_PROJET'`.
- `QUESTION_IA` — `aiQuestions` avec même critère et réponse "oui".
- `PIECE_MANQUANTE` — `piecesManquantes` avec mêmes codes (contributor
  enrichissant — pas accroche solo).

### Directive

Le field `DATE_PROJET` porte `<app-coherence-popover-trigger>`
(`appCoherencePopover`) câblé sur l'alerte computed.

### Hiérarchie sources F-IA-03

F96 > QUESTION_IA > IA > PIECE_MANQUANTE (règle F-IA-03 ; first-source-wins
dans le builder).

---

## Critères d'acceptation vérifiables

1. Composant `PseSectionComponent` standalone, créé sous
   `frontend/src/app/case-files/pse-section/`.
2. `@Input() caseFileId: string` requis + `@Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE'`.
3. Bannière info si `workspaceCountry !== 'FRANCE'` (pas de masquage).
4. Pré-fill IA fonctionnel (signal + badge + handler reset).
5. Validation F-IA-03 fonctionnelle (`coherenceAlerts.DATE_PROJET` +
   popover trigger).
6. Service `PseService` (POST + GET) avec URL
   `/api/v1/case-files/{id}/pse-analysis`.
7. Modèles TS dans `frontend/src/app/core/models/pse.model.ts`.
8. Entrée `TOOL_REGISTRY` `'F-DT-14-pse-validite'` (alignée migration 164)
   ajoutée au panel.
9. Bandeau verdict :
   - `VALIDE` : navy/info ;
   - `CONTESTABLE` : or/warning ;
   - `NUL` : rouge/critical.
10. ≥ 10 tests Jest qui passent.
11. Self-check grep pré-commit : 5 patterns OK.
12. SCSS aligné palette navy/or, JetBrains Mono pour `baseJuridique` et
    `formule`, Inter pour le reste, datepicker `<input type="date">`.

---

## Plan de test minimal

### Unitaires (Jest, ≥ 10)

1. `FRANCE` → form rendu, GET appelé.
2. `BELGIQUE` → bannière info, pas de form, pas d'appel HTTP.
3. GET 200 → résultat affiché, showForm=false.
4. GET 404 → reste en formulaire.
5. Pré-fill IA `dateProjet` depuis `aiData.dateLicenciement`.
6. `onDateProjetChange` efface badge IA.
7. `formValid()` exhaustif (tous les champs requis présents).
8. POST → snackbar succès + `triggerRefresh()`.
9. POST 400 → snackbar erreur + `panelClass: 'snack-error'`.
10. `coherenceAlerts.DATE_PROJET` présent si écart IA > 30 jours.
11. `coherenceAlerts.DATE_PROJET` absent si écart ≤ 30 jours.
12. `bannerClass` mappe verdict NUL → critical, CONTESTABLE → warning,
    VALIDE → info.
13. `pseRequis` calculé en live depuis le form (preview UX).
14. `ngOnChanges(aiData)` post-mount rafraîchit le pré-fill si form vide.
15. `toggleCollapse` fonctionne.

### Intégration

Pas de test backend dans cette SF (frontend-only). Le contrat est figé par
SF-DT-14-01 mergée hier (PR #623).

### Isolation workspace

Le filtre workspace est appliqué côté backend. Le frontend ne fait que
consommer l'endpoint avec le `caseFileId`. Pas de leak côté UI (la bannière
country n'est qu'une UX, pas une garantie d'isolation).

---

## Tables / endpoints / composants impactés

### Endpoints

- `POST /api/v1/case-files/{id}/pse-analysis` — consommé.
- `GET /api/v1/case-files/{id}/pse-analysis` — consommé (silencieux 404).

### Composants

- **Nouveau** : `PseSectionComponent` (+html/scss/spec).
- **Modifié** : `decisional-tools-panel.component.ts` (entrée TOOL_REGISTRY +
  import).

### Services

- **Nouveau** : `PseService` (HttpClient wrapper).

### Modèles

- **Nouveau** : `pse.model.ts` (Request + Response + 5 enums :
  `ModeAdoption`, `AvisCse`, `StatutDreets`, `ContenuMesure`,
  `VerdictValidite`, `CritereValidite`).

---

## Hors périmètre

- Backend SF-DT-14-01 (mergé indépendamment hier — PR #623).
- Extraction IA `tailleEntrepriseSalaries` /
  `nombreLicenciementsEnvisages` (le pipeline backend les extrait dans
  une SF ultérieure si pertinent — no-op gracieux côté frontend).
- Tests E2E.
- Equivalent BE congé-réorganisation Loi 1976 / Loi Renault (feature
  jumelle backlog dédiée).

---

## Analyse de cohérence transversale (RÈGLE CLAUDE.md)

### Autres outils décisionnels FR/BE

- F-DT-08 (Licenciement validity) — séparé FR/BE.
- F-DT-10 (Rupture conv FR) / F-DT-27 (Motif grave BE) — séparés.
- F-DT-13 (Licenciement économique FR) — couvre l'individuel/petit collectif,
  ne déclenche pas la procédure PSE. F-DT-14 couvre **uniquement** le grand
  collectif (≥ 10 / 30 jours / ≥ 50 salariés). Périmètres séparés.
- F-DT-29 (Crédit-temps BE) — pattern jumeau (calculator BE-only) utilisé
  comme template canonique.
- F-DT-34 (Référé prud'homal FR) — pattern jumeau (calculator FR-only)
  également utilisé comme référence pour le gate FR.

### Autres pays/domaines

- Belgique : équivalent connu sous le nom de "congé-réorganisation Loi
  1976 / Loi Renault" — backlog feature jumelle dédiée. Hors scope V8.
- Famille / Immigration : non applicable (mécanisme employeur/employé).

### Nouveau pattern UI ou service partagé

Aucun. Cette SF réutilise :
- `CoherenceAlertBuilder` (SF-155-05 partagé).
- `CoherencePopoverTriggerDirective` (F-IA-03-15c).
- `CaseDashboardRefreshService`, `MatSnackBar`, `LegalCitationsPipe`.
- Pattern canonique `credit-temps-be-section` (PR #624 jumeau BE-only —
  ce pattern est adapté pour FR-only).

---

## Impact par domaine métier (RÈGLE CLAUDE.md)

- **Droit du travail** : OUI, sensible. Outil décisionnel travail FR.
- **Famille** : non applicable (mécanisme employeur/employé).
- **Immigration** : non applicable.
- Pays : FR uniquement (équivalent BE laissé à feature jumelle backlog).

Pas de risque d'asymétrie métier — concept FR-only documenté en backlog
PRODUCT_SPEC F-DT-14.

---

## Parité des domaines métier (RÈGLE CLAUDE.md)

Niveau 5 (scoring `verdictValidite` VALIDE/CONTESTABLE/NUL).

- Famille : non applicable (mécanisme employeur/employé).
- Immigration : non applicable.
- Droit du travail BE : équivalent congé-réorganisation Loi 1976 / Loi
  Renault — feature jumelle backlog dédiée (hors scope V8).

Pas d'ouverture de feature jumelle nécessaire dans cette SF — déjà au
backlog côté Belgique.

---

## Préoccupations transversales (RÈGLE CLAUDE.md)

| Préoccupation | Impacté ? | Action |
|--------------|-----------|--------|
| Auth / Principal | Non — l'auth est gérée par les guards Angular existants ; le composant ne touche pas au Principal. | Aucune. |
| Workspace context | Non — le filtre workspace est appliqué côté backend (SF-DT-14-01). Le frontend passe simplement le `caseFileId`. | Aucune. |
| Plans / limites | Non — pas de quota PSE. | Aucune. |
| Navigation / routing | Non — le composant est consommé par le panel F-IA-04 existant, pas de nouvelle route. | Aucune. |
| Outil décisionnel métier | Oui — création d'un outil décisionnel travail FR. Périmètre **un outil = une situation** respecté (PSE FR uniquement, pas de switch implicit FR/BE). | Outil isolé, pattern jumeau `credit-temps-be-section` (BE) et `refere-prudhomal-section` (FR) servent de référence. Pas d'autre outil décisionnel modifié. |
