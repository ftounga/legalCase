# Mini-spec — SF-FA-19-04 Frontend changement de résidence (art. 373-2 Cciv)

> Frontend de l'outil décisionnel F-FA-19 "Changement de résidence" (FRANCE
> uniquement, art. 373-2 Cciv — obligation d'information préalable, délai de
> préavis raisonnable, modification éventuelle des modalités de la DVH).
> Contrat API importé de **SF-FA-19-03** (backend, parallèle).

---

## Identifiant

`F-FA-19 / SF-FA-19-04`

## Feature parente

`F-FA-19` — Autorité parentale (V7 backlog, droit famille FR).

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-FA-19-04-frontend-changement-residence`

## Subfeature parallèle

- `SF-FA-19-03` (backend) — contrat API figé importé ci-dessous (cf. §"Contrat API").

---

## Objectif

Exposer l'outil décisionnel F-FA-19 "Changement de résidence" dans le panel
des outils décisionnels du dossier (F-IA-04) : formulaire d'analyse
d'acceptabilité d'un projet de déménagement par le parent gardien (art. 373-2
Cciv) → bannière verdict + score d'acceptabilité + 3 cartes recommandations
(obligation d'information respectée, expertise psy enfant recommandée, délai de
préavis légal OK) + base juridique + formule.

---

## Comportement attendu

### Cas nominal

1. Le panel F-IA-04 affiche le composant `<app-changement-residence-section>`
   (FRANCE uniquement, gate pays) quand le tool_id `F-FA-19-changement-residence`
   est exposé.
2. Au mount, `GET /api/v1/case-files/{caseFileId}/changement-residence` :
   - 200 → result affiché (form masqué) ;
   - 404 → form affiché, pré-fill IA via `aiData?: FamilleExtractedData | null`.
3. L'avocat saisit la date prévue (`<input type="date">`), distance (km),
   raison (5 options), 4 slide-toggles bool, délai d'information préalable
   (jours), mode résidence actuel (3 options), âges enfants (CSV).
4. Au submit, `POST /api/v1/case-files/{caseFileId}/changement-residence` :
   - 200 → result affiché (bannière verdict navy/or/rouge classique selon
     `verdictProbabiliteAcceptation` ELEVEE/MOYENNE/FAIBLE), score, 3 cartes
     recommandations (obligation information respectée, expertise psy
     enfant recommandée, délai préavis OK), liste messages,
     `baseJuridique` + `formule` JetBrains Mono ;
   - `triggerRefresh()` du dashboard ;
   - `MatSnackBar` succès "Analyse changement de résidence calculée".
5. F-IA-03 : pour chaque field clé pré-remplissable, alerte de cohérence inline
   si la saisie avocat diverge des sources IA (`aiData`, `procedureChecks`,
   `aiQuestions`, `piecesManquantes`) — multi-source via `CoherenceAlertBuilder`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| caseFileId workspace différent | Erreur backend 403/404 → snackbar "Fermer" rouge | 403/404 |
| Champs requis absents | `formValid()` faux → bouton submit disabled | n/a |
| Distance ≤ 0 | `formValid()` faux | n/a |
| Date prévue dans le passé | Backend valide (info pure) → 200 | n/a |
| Workspace pays = BELGIQUE | Bannière info "outil FRANCE uniquement" — form masqué | n/a |
| Pas d'âges enfants saisis | Backend valide → 400 → snackbar rouge | 400 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : pattern de référence canonique
  `harcelement-licenciement-nul-section` (template §5 audit). Composant miroir
  direct : `autorite-parentale-section` (SF-FA-19-02, même module F-FA-19,
  FamilleExtractedData + CoherenceAlertBuilder + 5 fields, parsing CSV âges).
- [x] **Autres pays** : F-FA-19 = France uniquement (art. 373-2 Cciv).
  Belgique → futur F-FA-19-BE (changement résidence régi par art. 374 CC).
- [x] **Autres domaines** : domaine FAMILLE (DROIT_FAMILLE). Pas d'impact
  immigration/travail.
- [x] **Autres UI patterns** : aucun nouveau pattern — réutilise mat-select
  enum + slide-toggle + input numérique + input texte CSV âges + `<input
  type="date">` (déjà introduits par SF-FA-19-02).
- [x] **Autres flows transversaux** : workspaceCountry, dashboard refresh.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : 5 fields audités —
  `RAISON_CHANGEMENT`, `CONSENTEMENT_AUTRE_PARENT`, `INFORME_PREALABLEMENT`,
  `MODE_RESIDENCE_ACTUEL`, `AGE_ENFANTS`. Builder `CoherenceAlertBuilder` partagé.
- [x] **Refresh dashboard (F-IA-02)** : `CaseDashboardRefreshService.triggerRefresh()`
  appelé dans le `next:` du POST.
- [x] **Pré-remplissage IA** : 5 champs ouverts au pré-fill via
  `FamilleExtractedData` (`raisonChangementDetectee`, `consentementAutreParent`,
  `informePrealablement`, `modeResidenceActuel`, `ageEnfants`).
- [x] **Persistance des inputs** : SF-FA-19-03 backend persiste tous les inputs
  + sortie. Reload → GET 200 → form masqué + valeurs persistées.
- [x] **Masquage conditionnel** : gate `workspaceCountry === 'FRANCE'`. Bannière
  info pour BE (pas masquage silencieux).
- [x] **Alertes actives après calcul** : gate strict `!showForm()` dans
  `coherenceAlerts` computed (anti-bug SF-IA-03-12).

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Composant standalone non partagé** — pas de composant shared introduit.
- [x] **Pas de nouveau service** — réutilise `CoherenceAlertBuilder`,
  `CoherencePopoverTriggerDirective`, `LegalCitationsPipe`,
  `SourceExplanationService`, `CaseDashboardRefreshService`.
- [x] **Modèle `FamilleExtractedData`** : 4 nouveaux champs optionnels ajoutés —
  `raisonChangementDetectee`, `informePrealablement`, `modeResidenceActuel`
  (nouveau, distinct du `regimeExerciceActuel` SF-FA-19-02 — concept différent :
  *exercice* parental vs *résidence physique* enfant). `consentementAutreParent`
  et `ageEnfants` sont déjà présents (réutilisés depuis SF-FA-19-02).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pattern canonique `harcelement-licenciement-nul-section` | Oui | Template suivi via `autorite-parentale-section` (miroir module F-FA-19) |
| Pré-fill IA + alertes F-IA-03 | Oui | Intégrés dans cette SF |
| Refresh dashboard F-IA-02 | Oui | Intégré dans cette SF |
| Belgique équivalent | Non (Belgique = système différent art. 374 CC) | Backlog futur F-FA-19-BE |
| Multi-domaine (immigration / travail) | Non applicable | Outil 100 % famille FR |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [x] Pas de SF parallèle nécessaire (Belgique = backlog)
- [x] Pas de backlog supplémentaire à ouvrir

---

## Contrat API (importé SF-FA-19-03)

```typescript
export type RaisonChangement =
  | 'TRAVAIL'
  | 'FAMILLE'
  | 'LOGEMENT'
  | 'RAPPROCHEMENT_FAMILIAL'
  | 'AUTRE';

export type ModeResidenceCh =
  | 'ALTERNEE'
  | 'EXCLUSIVE_DEMANDEUR'
  | 'EXCLUSIVE_DEFENDEUR';

export interface ChangementResidenceRequest {
  /** ISO date YYYY-MM-DD (date prévue du changement). */
  dateChangementPrevu: string;
  distanceKm: number;
  raisonChangement: RaisonChangement;
  consentementAutreParent: boolean;
  informePrealablement: boolean;
  delaiInformationJours: number;
  modeResidenceActuel: ModeResidenceCh;
  ageEnfants: number[];
  scolariteImpactee: boolean;
  modificationDvhDemandee: boolean;
}

export interface ChangementResidenceResponse {
  caseFileId: string;
  dateChangementPrevu: string;
  distanceKm: number;
  raisonChangement: RaisonChangement;
  consentementAutreParent: boolean;
  informePrealablement: boolean;
  delaiInformationJours: number;
  modeResidenceActuel: ModeResidenceCh;
  ageEnfants: number[];
  scolariteImpactee: boolean;
  modificationDvhDemandee: boolean;
  scoreAcceptabilite: number;
  verdictProbabiliteAcceptation: 'ELEVEE' | 'MOYENNE' | 'FAIBLE';
  obligationInformationRespectee: boolean;
  expertisePsyEnfantRecommandee: boolean;
  delaiPreavisLegalOk: boolean;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}
```

Endpoints (consommés) :
- `POST /api/v1/case-files/{caseFileId}/changement-residence` (calcul + persistance)
- `GET  /api/v1/case-files/{caseFileId}/changement-residence` (récupération)

---

## Critères d'acceptation

- [ ] Composant standalone `ChangementResidenceSectionComponent` (3 fichiers `.ts/.html/.scss`).
- [ ] Service `ChangementResidenceService` (POST + GET) injecté.
- [ ] Modèle TS `changement-residence.model.ts` (2 enums + 2 interfaces + 2 listes labels).
- [ ] `@Input() caseFileId!: string`, `@Input() workspaceCountry`, `@Input() aiData?`,
  `@Input() procedureChecks?`, `@Input() aiQuestions?`, `@Input() piecesManquantes?`.
- [ ] Gate FRANCE : bannière info pour BE (`Cet outil s'applique à la France uniquement`)
  — pas masquage silencieux.
- [ ] Form : `<input type="date">` dateChangementPrevu, `<input type="number">`
  distanceKm, mat-select RaisonChangement (5 options), 4 slide-toggles
  (consentement / informePrealablement / scolariteImpactee / modificationDvhDemandee),
  `<input type="number">` delaiInformationJours, mat-select ModeResidenceCh
  (3 options), input texte CSV ages.
- [ ] Pré-fill IA via `FamilleExtractedData` (5 champs : `raisonChangementDetectee`,
  `consentementAutreParent`, `informePrealablement`, `modeResidenceActuel`,
  `ageEnfants`) + signal `provenance<Field>` + badge "Pré-rempli depuis l'analyse"
  (icône `auto_awesome`).
- [ ] Alertes F-IA-03 sur 5 fields via `CoherenceAlertBuilder` + popover
  `[appCoherencePopover]`. Builder partagé — pas d'interface locale `CoherenceAlert`.
- [ ] `CaseDashboardRefreshService.triggerRefresh()` après POST succès.
- [ ] `MatSnackBar` pour les erreurs (pas alert/confirm).
- [ ] JetBrains Mono pour `baseJuridique` et `formule` (Inter sinon).
- [ ] Bannière verdict navy clair (ELEVEE) / or (MOYENNE) / rouge classique (FAIBLE),
  pas de gradation rouge dominante.
- [ ] Tests Jest ≥ 12 (mount, gate FR/BE, GET 200/404, POST 200/erreur,
  pré-fill IA, alertes F-IA-03, handlers manuel, ngOnChanges).
- [ ] Self-check pre-commit OK (CoherenceAlertBuilder ≥ 2, popover ≥ 3,
  prefill ≥ 2, auto_awesome ≥ 2, provenance ≥ 6, coherenceAlerts ≥ 1,
  handlers ≥ 2, interface CoherenceAlert locale = 0).
- [ ] tsc clean + jest pass.

---

## Périmètre

### Hors scope (explicite)

- Backend (couvert par SF-FA-19-03).
- Belgique (système différent, futur F-FA-19-BE).
- Intégration TOOL_REGISTRY décisionnelle dans le panel : entrée
  `F-FA-19-changement-residence` ajoutée dans cette SF (forward-compat F-IA-04 ;
  l'orchestration backend `decision_tool_visibility_rules` dépend de la
  persistance backend SF-FA-19-03).
- Implémentation des 3 autres SF F-FA-19 (désaccord parental, visite médiatisée,
  relations tiers) → backlog F-FA-19.
- Génération de pièces (assignation / requête JAF) → futur F-FA-19-XX.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `dateChangementPrevu` | `null` | requis avant submit, ISO YYYY-MM-DD |
| `distanceKm` | `null` | requis avant submit, > 0 |
| `raisonChangement` | `null` | requis avant submit |
| `consentementAutreParent` | `false` | défaut |
| `informePrealablement` | `false` | défaut |
| `delaiInformationJours` | `0` | requis ≥ 0 |
| `modeResidenceActuel` | `null` | requis avant submit |
| `ageEnfants` | `[]` | requis ≥ 1 enfant pour formValid |
| `scolariteImpactee` | `false` | défaut |
| `modificationDvhDemandee` | `false` | défaut |

---

## Contraintes de validation

| Champ | Obligatoire | Format | Notes |
|-------|-------------|--------|-------|
| dateChangementPrevu | Oui | ISO YYYY-MM-DD | |
| distanceKm | Oui | nombre > 0 | |
| raisonChangement | Oui | enum 5 valeurs | |
| delaiInformationJours | Oui | entier ≥ 0 | |
| modeResidenceActuel | Oui | enum 3 valeurs | |
| ageEnfants | Oui (≥ 1) | array d'entiers ≥ 0 et ≤ 30 | parsé depuis CSV |

---

## Plan de test

### Tests unitaires Jest

1. mount sans erreur (FRANCE) — composant créé, options enums chargées.
2. `formValid` faux si dateChangementPrevu null.
3. `formValid` faux si distanceKm ≤ 0.
4. `formValid` faux si modeResidenceActuel null.
5. `formValid` faux si ageEnfants vide.
6. `formValid` vrai sur saisie minimale complète.
7. GET 200 → form masqué + valeurs persistées + pas de badge IA.
8. GET 404 → form affiché + pré-fill IA appliqué.
9. POST 200 → result affiché + snackbar succès + dashboardRefresh.
10. POST erreur 400 → snackbar rouge + reset calculating.
11. `onRaisonChange` efface badge IA.
12. coherenceAlert RAISON_CHANGEMENT si IA dit autre chose.
13. coherenceAlert AGE_ENFANTS multi-source IA + F96 + PIECE_MANQUANTE.
14. alertes masquées après résultat (showForm=false).
15. ngOnChanges(aiData) post-mount rafraîchit le pré-fill.
16. gate BELGIQUE → form non rendu, GET non appelé.
17. parsing ageEnfants CSV "12, 8, 5" → `[12, 8, 5]`.

### Tests d'intégration

Couverts par SF-FA-19-03 backend (POST/GET, 403, 400, persistance).

### Isolation workspace

Couvert par SF-FA-19-03 (le service ne fait que POST/GET avec caseFileId, l'authentification
est portée par le contexte global).

---

## Technique

### Endpoints consommés

| Méthode | URL | Auth | Source |
|---------|-----|------|--------|
| POST | `/api/v1/case-files/{caseFileId}/changement-residence` | Oui | SF-FA-19-03 |
| GET | `/api/v1/case-files/{caseFileId}/changement-residence` | Oui | SF-FA-19-03 |

### Composants Angular créés

- `ChangementResidenceSectionComponent` — composant standalone, panel F-IA-04.
- `ChangementResidenceService` — wrapper HttpClient.
- Modèle `changement-residence.model.ts` (2 enums + 2 interfaces + listes labels).
- Patch `divorce-accepte.model.ts::FamilleExtractedData` : ajout 3 champs
  optionnels (`raisonChangementDetectee`, `informePrealablement`,
  `modeResidenceActuel`). `consentementAutreParent` et `ageEnfants` déjà présents.
- Patch `decisional-tools-panel.component.ts` : entrée `TOOL_REGISTRY` pour
  `F-FA-19-changement-residence`.

### Tables impactées

Aucune (frontend uniquement).

### Migration Liquibase

- [ ] Non applicable (SF frontend).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — non touché.
- [ ] Workspace context — utilise `workspaceCountry` (input) sans modification.
- [ ] Plans / limites — non touché.
- [ ] Navigation / routing — non touché (intégration via `decisional-tools-panel`).
- [x] **Aucune préoccupation transversale au sens fort** — composant isolé qui
  s'intègre via le pattern existant.

### Impact par domaine métier

Sensible au domaine **DROIT_FAMILLE** (FRANCE uniquement). Pas d'équivalent
immigration ni droit du travail. Belgique = système différent (futur F-FA-19-BE).

### Parité des domaines métier (outil ≥ niveau 5)

Niveau **5 (scoring)** + **6 (comparateur fourchettes recommandations)** :
- Famille : SF-FA-19-04 livre cet outil (scoring `scoreAcceptabilite` +
  recommandations `obligationInformationRespectee` + `expertisePsyEnfantRecommandee`
  + `delaiPreavisLegalOk`).
- Travail : pas pertinent (le concept "changement de résidence d'un enfant
  affectant la DVH" est strictement famille).
- Immigration : pas pertinent (les déménagements en immigration relèvent du
  changement d'adresse au préfet, hors scope F-FA-19).
**Justification** : la notion de changement de résidence d'un enfant déclenchant
l'obligation d'information art. 373-2 Cciv est intrinsèquement famille.

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — composant frontend isolé.
- [ ] Frontend tsc + Jest doivent passer.

---

## Dépendances

### Subfeatures bloquantes

- `SF-FA-19-03` (backend) — figé (contrat API utilisé).

### Questions ouvertes impactées

- Aucune.

---

## Notes et décisions

- **Pré-fill IA** sur 5 champs (`raisonChangement`, `consentementAutreParent`,
  `informePrealablement`, `modeResidenceActuel`, `ageEnfants`). `dateChangementPrevu`,
  `distanceKm`, `delaiInformationJours`, `scolariteImpactee`, `modificationDvhDemandee`
  ne sont pas pré-remplis (saisies projet pures réservées à l'avocat).
- **Saisie âges enfants** : input texte CSV ("12, 8, 5") parsé via regex —
  pragmatique car aucun chip-input introduit dans le frontend à ce jour
  (cohérence avec autorite-parentale-section SF-FA-19-02).
- **Verdict 3 paliers** : ELEVEE (navy clair), MOYENNE (or accent), FAIBLE
  (rouge classique). Pas de gradation rouge dominante (urgence non absolue).
- **Pattern miroir** : `autorite-parentale-section` (FamilleExtractedData +
  builder F-IA-03 + 5 fields + parsing CSV âges + 4 slide-toggles).
- **Distance et délai** : les bornes (préavis légal raisonnable, distance
  significative ≥ 50km / ≥ 100km) sont portées par le backend via
  `delaiPreavisLegalOk` et la formule. Frontend n'effectue pas de validation
  métier — seulement la validation de format (> 0, entier).
