# Mini-spec — SF-FA-19-02 Frontend autorité parentale — exercice

> Frontend de l'outil décisionnel F-FA-19 "Autorité parentale — exercice"
> (FRANCE uniquement, art. 372-373 / 373-2-10 Cciv).
> Contrat API importé de **SF-FA-19-01** (backend, parallèle).

---

## Identifiant

`F-FA-19 / SF-FA-19-02`

## Feature parente

`F-FA-19` — Autorité parentale (V7 backlog, droit famille FR).

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-FA-19-02-frontend-autorite-parentale-exercice`

## Subfeature parallèle

- `SF-FA-19-01` (backend) — contrat API figé importé ci-dessous (cf. §"Contrat API").

---

## Objectif

Exposer l'outil décisionnel F-FA-19 dans le panel des outils décisionnels du dossier (F-IA-04) :
formulaire d'analyse "exercice de l'autorité parentale" (régimes art. 372 Cciv, motifs de
changement art. 373/373-2-10 Cciv) → bannière verdict + cartes recommandations
(expertise psychiatrique, audition enfants ≥ 13 ans) + base juridique + formule.

---

## Comportement attendu

### Cas nominal

1. Le panel F-IA-04 affiche le composant `<app-autorite-parentale-section>` (FRANCE
   uniquement, gate pays) quand le tool_id `F-FA-19-autorite-parentale` est exposé.
2. Au mount, `GET /api/v1/case-files/{caseFileId}/autorite-parentale` :
   - 200 → result affiché (form masqué) ;
   - 404 → form affiché, pré-fill IA via `aiData?: FamilleExtractedData | null`.
3. L'avocat saisit régimes (actuel/demandé), motif, slide-toggles (danger, consentement,
   interférence vie enfant), preuves multi-select (8 options), âges enfants
   (input texte CSV "12, 8, 5") et date requête (`<input type="date">`).
4. Au submit, `POST /api/v1/case-files/{caseFileId}/autorite-parentale` :
   - 200 → result affiché (bannière verdict navy/or/rouge classique selon
     `verdictProbabiliteAcceptation` ELEVEE/MOYENNE/FAIBLE), score, 2 cartes
     recommandations (expertise psychiatrique conseillée, audition enfants ≥ 13 ans
     conseillée), liste messages, `baseJuridique` + `formule` JetBrains Mono ;
   - `triggerRefresh()` du dashboard ;
   - `MatSnackBar` succès "Analyse autorité parentale calculée".
5. F-IA-03 : pour chaque field clé pré-remplissable, alerte de cohérence inline
   si la saisie avocat diverge des sources IA (`aiData`, `procedureChecks`,
   `aiQuestions`, `piecesManquantes`) — multi-source via `CoherenceAlertBuilder`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| caseFileId workspace différent | Erreur backend 403/404 → snackbar "Fermer" rouge | 403/404 |
| Champs requis absents | `formValid()` faux → bouton submit disabled | n/a |
| Régime exercice actuel == régime demandé | Backend valide → 400 → snackbar rouge | 400 |
| Workspace pays = BELGIQUE | Bannière info "outil FRANCE uniquement, voir équivalent BE" — form masqué | n/a |
| Pas d'âges enfants saisis | Backend valide → 400 → snackbar rouge | 400 |
| Date requête vide ou non ISO | `formValid()` faux | n/a |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : pattern de référence canonique
  `harcelement-licenciement-nul-section` (template §5 audit). Composants similaires
  utilisés comme miroir : `divorce-faute-section` (mat-select multiple + 1 ISO date)
  et `divorce-accepte-section` (FamilleExtractedData + builder F-IA-03).
- [x] **Autres pays** : F-FA-19 = France uniquement (art. 372 Cciv). Belgique →
  futur F-FA-19-BE (cohabitation parentale art. 373 CC, hors scope).
- [x] **Autres domaines** : domaine FAMILLE (DROIT_FAMILLE). Pas d'impact
  immigration/travail.
- [x] **Autres UI patterns** : multi-select preuves, ages enfants en CSV (pas de
  chip-input encore introduit côté frontend → chemin pragmatique).
- [x] **Autres flows transversaux** : workspaceCountry, dashboard refresh.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : 5 fields audités —
  `REGIME_EXERCICE_ACTUEL`, `REGIME_EXERCICE_DEMANDE`, `MOTIF_CHANGEMENT`,
  `DANGER_CARACTERISE`, `AGE_ENFANTS`. Builder `CoherenceAlertBuilder` partagé.
- [x] **Refresh dashboard (F-IA-02)** : `CaseDashboardRefreshService.triggerRefresh()`
  appelé dans le `next:` du POST.
- [x] **Pré-remplissage IA** : 5 champs ouverts au pré-fill via
  `FamilleExtractedData` (`regimeExerciceActuel`, `dangerCaracterise`,
  `consentementAutreParent`, `interferenceVieEnfant`, `ageEnfants`).
- [x] **Persistance des inputs** : SF-FA-19-01 backend persiste tous les inputs
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
- [x] **Modèle `FamilleExtractedData`** : 5 nouveaux champs optionnels ajoutés —
  source de vérité unique côté frontend (pas de divergence). Réutilisable par
  les futurs outils famille.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pattern canonique `harcelement-licenciement-nul-section` | Oui | Template suivi via `divorce-accepte-section` (miroir famille) |
| Pré-fill IA + alertes F-IA-03 | Oui | Intégrés dans cette SF |
| Refresh dashboard F-IA-02 | Oui | Intégré dans cette SF |
| Belgique équivalent | Non (Belgique = système différent art. 373 CC) | Backlog futur F-FA-19-BE |
| Multi-domaine (immigration / travail) | Non applicable | Outil 100 % famille FR |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [x] Pas de SF parallèle nécessaire (Belgique = backlog)
- [x] Pas de backlog supplémentaire à ouvrir

---

## Contrat API (importé SF-FA-19-01)

```typescript
export type RegimeExercice =
  | 'CONJOINT'
  | 'EXCLUSIF_MERE'
  | 'EXCLUSIF_PERE'
  | 'DELEGATION_TIERS';

export type MotifChangementAutorite =
  | 'MISE_EN_DANGER'
  | 'DESINTERET_PROLONGE'
  | 'IMPOSSIBILITE_FAIT'
  | 'CONDAMNATION_PENALE'
  | 'ALIENATION_PARENTALE'
  | 'AUTRE';

export type PreuveAutorite =
  | 'JUGEMENT_PENAL'
  | 'MAINS_COURANTES'
  | 'CERTIFICAT_MEDICAL'
  | 'TEMOIGNAGES_ECOLE'
  | 'TEMOIGNAGES_PROCHES'
  | 'RAPPORT_AED'
  | 'EXPERTISE_PSYCHIATRIQUE'
  | 'AUTRE';

export interface AutoriteParentaleRequest {
  regimeExerciceActuel: RegimeExercice;
  regimeExerciceDemande: RegimeExercice;
  motifChangement: MotifChangementAutorite;
  dangerCaracterise: boolean;
  preuvesProduites: PreuveAutorite[];
  ageEnfants: number[];
  consentementAutreParent: boolean;
  interferenceVieEnfant: boolean;
  dateRequete: string;
}

export interface AutoriteParentaleResponse {
  caseFileId: string;
  regimeExerciceActuel: RegimeExercice;
  regimeExerciceDemande: RegimeExercice;
  motifChangement: MotifChangementAutorite;
  dangerCaracterise: boolean;
  preuvesProduites: PreuveAutorite[];
  ageEnfants: number[];
  consentementAutreParent: boolean;
  interferenceVieEnfant: boolean;
  dateRequete: string;
  scoreEligibilite: number;
  verdictProbabiliteAcceptation: 'ELEVEE' | 'MOYENNE' | 'FAIBLE';
  expertiseRecommandee: boolean;
  auditionEnfantsRecommandee: boolean;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}
```

Endpoints (consommés) :
- `POST /api/v1/case-files/{caseFileId}/autorite-parentale` (calcul + persistance)
- `GET  /api/v1/case-files/{caseFileId}/autorite-parentale` (récupération)

---

## Critères d'acceptation

- [ ] Composant standalone `AutoriteParentaleSectionComponent` (3 fichiers `.ts/.html/.scss`).
- [ ] Service `AutoriteParentaleService` (POST + GET) injecté.
- [ ] Modèle TS `autorite-parentale.model.ts` (3 enums + 2 interfaces + 3 listes labels).
- [ ] `@Input() caseFileId!: string`, `@Input() workspaceCountry`, `@Input() aiData?`,
  `@Input() procedureChecks?`, `@Input() aiQuestions?`, `@Input() piecesManquantes?`.
- [ ] Gate FRANCE : bannière info pour BE (`Cet outil s'applique à la France uniquement`)
  — pas masquage silencieux.
- [ ] Form : 2 mat-select RegimeExercice (4 options), 1 mat-select MotifChangement
  (6 options), 3 slide-toggles (danger / consentement / interférence), 1 mat-select
  multiple PreuveAutorite (8 options), input ages CSV, `<input type="date">` requête.
- [ ] Pré-fill IA via `FamilleExtractedData` (5 champs : régime actuel, danger,
  consentement, interférence, âges) + signal `provenance<Field>` + badge
  "Pré-rempli depuis l'analyse" (icône `auto_awesome`).
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

- Backend (couvert par SF-FA-19-01).
- Belgique (système différent, futur F-FA-19-BE).
- Intégration TOOL_REGISTRY décisionnelle dans le panel : sera ajoutée à la merge
  globale F-IA-04 (l'orchestration backend `decision_tool_visibility_rules`
  dépend de la persistance backend SF-FA-19-01).
  Toutefois, l'entrée `TOOL_REGISTRY` est ajoutée maintenant pour symétrie avec
  les autres outils famille (forward-compat F-IA-04).
- Implémentation des 4 autres SF F-FA-19 (changement résidence, désaccord parental,
  visite médiatisée, relations tiers) → backlog F-FA-19.
- Génération de pièces (assignation, requête JAF) → futur F-FA-19-XX.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `regimeExerciceActuel` | `null` | requis avant submit |
| `regimeExerciceDemande` | `null` | requis avant submit |
| `motifChangement` | `null` | requis avant submit |
| `dangerCaracterise` | `false` | défaut |
| `preuvesProduites` | `[]` | optionnel |
| `ageEnfants` | `[]` | requis ≥ 1 enfant pour formValid |
| `consentementAutreParent` | `false` | défaut |
| `interferenceVieEnfant` | `false` | défaut |
| `dateRequete` | `null` | requis avant submit, ISO YYYY-MM-DD |

---

## Contraintes de validation

| Champ | Obligatoire | Format | Notes |
|-------|-------------|--------|-------|
| regimeExerciceActuel | Oui | enum 4 valeurs | différent du regimeExerciceDemande (validé backend) |
| regimeExerciceDemande | Oui | enum 4 valeurs | |
| motifChangement | Oui | enum 6 valeurs | |
| preuvesProduites | Non | array enum 8 valeurs | peut être vide |
| ageEnfants | Oui (≥ 1) | array d'entiers ≥ 0 et ≤ 30 (limite raisonnable) | parsé depuis CSV |
| dateRequete | Oui | ISO YYYY-MM-DD | |

---

## Plan de test

### Tests unitaires Jest

1. mount sans erreur (FRANCE) — composant créé, options enums chargées.
2. `formValid` faux si régime actuel null.
3. `formValid` faux si ageEnfants vide.
4. `formValid` vrai sur saisie minimale complète.
5. GET 200 → form masqué + valeurs persistées + pas de badge IA.
6. GET 404 → form affiché + pré-fill IA appliqué.
7. POST 200 → result affiché + snackbar succès + dashboardRefresh.
8. POST erreur 400 → snackbar rouge + reset calculating.
9. `onRegimeActuelChange` efface badge IA.
10. coherenceAlert REGIME_EXERCICE_ACTUEL si IA dit autre chose.
11. coherenceAlert AGE_ENFANTS multi-source IA + F96 + PIECE_MANQUANTE.
12. alertes masquées après résultat (showForm=false).
13. ngOnChanges(aiData) post-mount rafraîchit le pré-fill.
14. gate BELGIQUE → form non rendu, GET non appelé.
15. parsing ageEnfants CSV "12, 8, 5" → `[12, 8, 5]`.

### Tests d'intégration

Couverts par SF-FA-19-01 backend (POST/GET, 403, 400, persistance).

### Isolation workspace

Couvert par SF-FA-19-01 (le service ne fait que POST/GET avec caseFileId, l'authentification
est portée par le contexte global).

---

## Technique

### Endpoints consommés

| Méthode | URL | Auth | Source |
|---------|-----|------|--------|
| POST | `/api/v1/case-files/{caseFileId}/autorite-parentale` | Oui | SF-FA-19-01 |
| GET | `/api/v1/case-files/{caseFileId}/autorite-parentale` | Oui | SF-FA-19-01 |

### Composants Angular créés

- `AutoriteParentaleSectionComponent` — composant standalone, panel F-IA-04.
- `AutoriteParentaleService` — wrapper HttpClient.
- Modèle `autorite-parentale.model.ts` (3 enums + 2 interfaces + listes labels).
- Patch `divorce-accepte.model.ts::FamilleExtractedData` : ajout 5 champs optionnels
  (`regimeExerciceActuel`, `dangerCaracterise`, `consentementAutreParent`,
  `interferenceVieEnfant`, `ageEnfants`).
- Patch `decisional-tools-panel.component.ts` : entrée `TOOL_REGISTRY` pour
  `F-FA-19-autorite-parentale`.

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
- Famille : SF-FA-19-02 livre cet outil (scoring `scoreEligibilite` + recommandations
  `expertiseRecommandee` + `auditionEnfantsRecommandee`).
- Travail : pas pertinent (le concept "autorité parentale" est strictement famille).
- Immigration : pas pertinent.
**Justification** : la notion d'autorité parentale est intrinsèquement famille,
les autres domaines n'ont pas d'analogue (l'attribution garde enfants en immigration
est traitée dans les regroupements familiaux F-IM-XX, hors scope F-FA-19).

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — composant frontend isolé.
- [ ] Frontend tsc + Jest doivent passer.

---

## Dépendances

### Subfeatures bloquantes

- `SF-FA-19-01` (backend) — figé (contrat API utilisé).

### Questions ouvertes impactées

- Aucune.

---

## Notes et décisions

- **Pré-fill IA** sur 5 champs (les 4 booléens + `ageEnfants`). `motifChangement`
  et `regimeExerciceDemande` ne sont pas pré-remplis (décisions juridiques pures
  réservées à l'avocat). `dateRequete` est saisie utilisateur.
- **Saisie âges enfants** : input texte CSV ("12, 8, 5") parsé via regex —
  pragmatique car aucun chip-input introduit dans le frontend à ce jour.
- **Verdict 3 paliers** : ELEVEE (navy clair), MOYENNE (or accent), FAIBLE
  (rouge classique). Pas de gradation rouge dominante (urgence non absolue).
- **Audition enfants ≥ 13 ans** : la recommandation est portée par le backend
  via `auditionEnfantsRecommandee`. Frontend affiche la carte recommandation
  uniquement si `true`.
- **Pattern miroir** : `divorce-accepte-section` (FamilleExtractedData + builder
  F-IA-03 + 5 fields). Adapté pour F-FA-19 avec mat-select multiple preuves
  + parsing CSV âges.
