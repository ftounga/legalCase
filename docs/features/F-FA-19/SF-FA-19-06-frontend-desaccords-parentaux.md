# Mini-spec — SF-FA-19-06 Frontend désaccords parentaux art. 373-2-10

> Frontend de l'outil décisionnel F-FA-19 "Désaccords parentaux" (FRANCE
> uniquement, art. 373-2-10 Cciv — saisine du JAF en cas de désaccord
> sur l'exercice de l'autorité parentale).
> Contrat API importé de **SF-FA-19-05** (backend, parallèle).

---

## Identifiant

`F-FA-19 / SF-FA-19-06`

## Feature parente

`F-FA-19` — Autorité parentale (V7 backlog, droit famille FR).

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-FA-19-06-frontend-desaccords-parentaux`

## Subfeature parallèle

- `SF-FA-19-05` (backend) — contrat API figé importé ci-dessous (cf. §"Contrat API").

---

## Objectif

Exposer l'outil décisionnel "Désaccords parentaux art. 373-2-10" dans le panel
des outils décisionnels (F-IA-04) : formulaire d'analyse des désaccords entre
parents (scolarité, santé, religion, loisirs, choix éducatifs, déménagement,
autre) avec scoring d'éligibilité de la saisine JAF + recommandations
(médiation préalable, audition enfants, expertise psy) + délai prévisionnel.

---

## Comportement attendu

### Cas nominal

1. Le panel F-IA-04 affiche `<app-desaccords-parentaux-section>` (FRANCE
   uniquement, gate pays) quand le tool_id `F-FA-19-desaccords-parentaux` est
   exposé.
2. Au mount, `GET /api/v1/case-files/{caseFileId}/desaccords-parentaux` :
   - 200 → result affiché (form masqué) ;
   - 404 → form affiché, pré-fill IA via `aiData?: FamilleExtractedData | null`.
3. L'avocat saisit :
   - `domaineDesaccord` (mat-select 7 options : SCOLARITE, SANTE, RELIGION,
     LOISIRS_SPORTS, CHOIX_EDUCATIFS, DEMENAGEMENT, AUTRE) ;
   - `intensiteDesaccord` (mat-select 3 options : MAJEUR, MOYEN, MINEUR) ;
   - `tentativesMediation` (mat-select multiple 5 options : MEDIATION_FAMILIALE,
     MEDIATION_JUDICIAIRE, DISCUSSIONS_DIRECTES, THERAPIE_FAMILIALE, AUCUNE) ;
   - `ageEnfantsConcernes` (input texte CSV, parsé) ;
   - 3 slide-toggles : `interetSuperieurInvoque`, `expertiseDejaRealisee`,
     `urgence` ;
   - `dateRequete` (`<input type="date">`).
4. Au submit, `POST /api/v1/case-files/{caseFileId}/desaccords-parentaux` :
   - 200 → result affiché (bannière verdict navy/or/rouge selon
     `verdictProbabiliteAcceptation` ELEVEE/MOYENNE/FAIBLE), score, 3 cartes
     recommandations (médiation préalable, audition enfants, expertise psy),
     1 carte délai prévisionnel (90j ou 30j si urgence), liste messages,
     `baseJuridique` + `formule` JetBrains Mono ;
   - `triggerRefresh()` du dashboard ;
   - `MatSnackBar` succès "Analyse désaccords parentaux calculée".
5. F-IA-03 : pour chaque field clé pré-remplissable, alerte de cohérence inline
   si la saisie avocat diverge des sources IA (`aiData`, `procedureChecks`,
   `aiQuestions`, `piecesManquantes`) — multi-source via `CoherenceAlertBuilder`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| caseFileId workspace différent | Erreur backend 403/404 → snackbar "Fermer" rouge | 403/404 |
| Champs requis absents | `formValid()` faux → bouton submit disabled | n/a |
| Pas d'âges enfants saisis | `formValid()` faux | n/a |
| Workspace pays = BELGIQUE | Bannière info "outil FRANCE uniquement" — form masqué | n/a |
| Date requête vide ou non ISO | `formValid()` faux | n/a |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : pattern de référence canonique
  `harcelement-licenciement-nul-section` (template §5 audit). Composants
  miroirs : `autorite-parentale-section` (jumeau F-FA-19, FamilleExtractedData,
  CSV âges, builder F-IA-03) et `divorce-faute-section` (mat-select multiple).
- [x] **Autres pays** : F-FA-19 désaccords = France uniquement (art. 373-2-10
  Cciv). Belgique non pertinent (système de cohabitation parentale différent —
  art. 373 CC).
- [x] **Autres domaines** : domaine FAMILLE uniquement. Pas d'impact
  immigration/travail.
- [x] **Autres UI patterns** : multi-select tentativesMediation, ages enfants
  CSV (cohérent avec autorite-parentale-section).
- [x] **Autres flows transversaux** : workspaceCountry, dashboard refresh.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : 5 fields audités —
  `DOMAINE_DESACCORD`, `INTENSITE_DESACCORD`, `TENTATIVES_MEDIATION`,
  `AGE_ENFANTS_CONCERNES`, `URGENCE`. Builder `CoherenceAlertBuilder` partagé.
- [x] **Refresh dashboard (F-IA-02)** : `CaseDashboardRefreshService.triggerRefresh()`
  appelé dans le `next:` du POST.
- [x] **Pré-remplissage IA** : 5 champs ouverts via `FamilleExtractedData`
  (nouveaux champs optionnels : `domaineDesaccordDetecte`,
  `intensiteDesaccordDetecte`, `tentativesMediationDetectees`,
  `ageEnfantsConcernes` — réutilisé depuis `ageEnfants` existant ou nouveau
  `ageEnfantsConcernes`, voir §Notes), `urgenceDetectee`).
- [x] **Persistance des inputs** : SF-FA-19-05 backend persiste tous les inputs
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
- [x] **Modèle `FamilleExtractedData`** : 4 nouveaux champs optionnels ajoutés
  (`domaineDesaccordDetecte`, `intensiteDesaccordDetecte`,
  `tentativesMediationDetectees`, `urgenceDetectee`). `ageEnfantsConcernes`
  réutilise `ageEnfants` existant pour ne pas dupliquer la même donnée.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pattern canonique `harcelement-licenciement-nul-section` | Oui | Template suivi via `autorite-parentale-section` (jumeau F-FA-19) |
| Pré-fill IA + alertes F-IA-03 | Oui | Intégrés dans cette SF |
| Refresh dashboard F-IA-02 | Oui | Intégré dans cette SF |
| Belgique équivalent | Non (système différent) | Hors scope V1 |
| Multi-domaine (immigration / travail) | Non applicable | Outil 100 % famille FR |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [x] Pas de SF parallèle nécessaire
- [x] Pas de backlog supplémentaire à ouvrir

---

## Contrat API (importé SF-FA-19-05)

```typescript
export type DomaineDesaccord =
  | 'SCOLARITE'
  | 'SANTE'
  | 'RELIGION'
  | 'LOISIRS_SPORTS'
  | 'CHOIX_EDUCATIFS'
  | 'DEMENAGEMENT'
  | 'AUTRE';

export type IntensiteDesaccord = 'MAJEUR' | 'MOYEN' | 'MINEUR';

export type TentativeMediation =
  | 'MEDIATION_FAMILIALE'
  | 'MEDIATION_JUDICIAIRE'
  | 'DISCUSSIONS_DIRECTES'
  | 'THERAPIE_FAMILIALE'
  | 'AUCUNE';

export interface DesaccordsParentauxRequest {
  domaineDesaccord: DomaineDesaccord;
  intensiteDesaccord: IntensiteDesaccord;
  tentativesMediation: TentativeMediation[];
  ageEnfantsConcernes: number[];
  interetSuperieurInvoque: boolean;
  expertiseDejaRealisee: boolean;
  urgence: boolean;
  /** ISO date YYYY-MM-DD. */
  dateRequete: string;
}

export interface DesaccordsParentauxResponse {
  caseFileId: string;
  domaineDesaccord: DomaineDesaccord;
  intensiteDesaccord: IntensiteDesaccord;
  tentativesMediation: TentativeMediation[];
  ageEnfantsConcernes: number[];
  interetSuperieurInvoque: boolean;
  expertiseDejaRealisee: boolean;
  urgence: boolean;
  dateRequete: string;
  scoreEligibiliteJaf: number;
  verdictProbabiliteAcceptation: 'ELEVEE' | 'MOYENNE' | 'FAIBLE';
  mediationPrealableRecommandee: boolean;
  auditionEnfantsRecommandee: boolean;
  expertisePsyRecommandee: boolean;
  delaiTraitementJoursPrevisionnel: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}
```

Endpoints (consommés) :
- `POST /api/v1/case-files/{caseFileId}/desaccords-parentaux` (calcul + persistance)
- `GET  /api/v1/case-files/{caseFileId}/desaccords-parentaux` (récupération)

---

## Critères d'acceptation

- [ ] Composant standalone `DesaccordsParentauxSectionComponent` (3 fichiers `.ts/.html/.scss`).
- [ ] Service `DesaccordsParentauxService` (POST + GET).
- [ ] Modèle TS `desaccords-parentaux.model.ts` (3 enums + 2 interfaces + listes labels).
- [ ] `@Input() caseFileId!: string`, `@Input() workspaceCountry`, `@Input() aiData?`,
  `@Input() procedureChecks?`, `@Input() aiQuestions?`, `@Input() piecesManquantes?`.
- [ ] Gate FRANCE : bannière info pour BE — pas de masquage silencieux.
- [ ] Form : 1 mat-select DomaineDesaccord (7), 1 mat-select IntensiteDesaccord (3),
  1 mat-select multiple TentativeMediation (5), 1 input texte CSV ages,
  3 slide-toggles, 1 `<input type="date">` requête.
- [ ] Pré-fill IA via `FamilleExtractedData` (5 champs : `domaineDesaccordDetecte`,
  `intensiteDesaccordDetecte`, `tentativesMediationDetectees`, `ageEnfants` →
  `ageEnfantsConcernes`, `urgenceDetectee`) + signal `provenance<Field>` +
  badge "Pré-rempli depuis l'analyse" (icône `auto_awesome`).
- [ ] Alertes F-IA-03 sur 5 fields via `CoherenceAlertBuilder` + popover.
  Builder partagé — pas d'interface locale `CoherenceAlert`.
- [ ] `CaseDashboardRefreshService.triggerRefresh()` après POST succès.
- [ ] `MatSnackBar` pour les erreurs (pas alert/confirm).
- [ ] JetBrains Mono pour `baseJuridique` et `formule` (Inter sinon).
- [ ] Bannière verdict navy clair (ELEVEE) / or (MOYENNE) / rouge classique
  (FAIBLE), pas de gradation rouge dominante.
- [ ] Affichage 3 cartes recommandations + 1 carte délai (couleur or si urgence
  réduit le délai à 30j).
- [ ] Tests Jest ≥ 12 (mount, gate FR/BE, GET 200/404, POST 200/erreur,
  pré-fill IA, alertes F-IA-03, handlers manuel, ngOnChanges).
- [ ] Patch `decisional-tools-panel.component.ts` : entrée `TOOL_REGISTRY`
  pour `F-FA-19-desaccords-parentaux`.
- [ ] tsc clean + jest pass.

---

## Périmètre

### Hors scope (explicite)

- Backend (couvert par SF-FA-19-05).
- Belgique (système différent — hors scope V1).
- Autres SF F-FA-19 (changement résidence — SF-FA-19-03/04 ; visite médiatisée
  et relations tiers — backlog).
- Génération de pièces (assignation, requête JAF) → futur F-FA-19-XX.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `domaineDesaccord` | `null` | requis avant submit |
| `intensiteDesaccord` | `null` | requis avant submit |
| `tentativesMediation` | `[]` | requis ≥ 1 (peut être `['AUCUNE']`) |
| `ageEnfantsConcernes` | `[]` | requis ≥ 1 enfant |
| `interetSuperieurInvoque` | `false` | défaut |
| `expertiseDejaRealisee` | `false` | défaut |
| `urgence` | `false` | défaut |
| `dateRequete` | `null` | requis avant submit, ISO YYYY-MM-DD |

---

## Contraintes de validation

| Champ | Obligatoire | Format | Notes |
|-------|-------------|--------|-------|
| domaineDesaccord | Oui | enum 7 valeurs | |
| intensiteDesaccord | Oui | enum 3 valeurs | |
| tentativesMediation | Oui (≥ 1) | array enum 5 valeurs | peut être `['AUCUNE']` |
| ageEnfantsConcernes | Oui (≥ 1) | array d'entiers ≥ 0 et ≤ 30 | parsé depuis CSV |
| dateRequete | Oui | ISO YYYY-MM-DD | |

---

## Plan de test

### Tests unitaires Jest

1. mount sans erreur (FRANCE) — composant créé, options enums chargées.
2. `formValid` faux si domaineDesaccord null.
3. `formValid` faux si intensiteDesaccord null.
4. `formValid` faux si tentativesMediation vide.
5. `formValid` faux si ageEnfantsConcernes vide.
6. `formValid` vrai sur saisie minimale complète.
7. GET 200 → form masqué + valeurs persistées + pas de badge IA.
8. GET 404 → form affiché + pré-fill IA appliqué (5 fields).
9. POST 200 → result affiché + snackbar succès + dashboardRefresh.
10. POST erreur 400 → snackbar rouge + reset calculating.
11. handler manuel `onDomaineChange` efface badge IA.
12. coherenceAlert DOMAINE_DESACCORD si IA dit autre chose.
13. coherenceAlert AGE_ENFANTS_CONCERNES multi-source IA + F96 + PIECE_MANQUANTE.
14. alertes masquées après résultat (showForm=false).
15. ngOnChanges(aiData) post-mount rafraîchit le pré-fill.
16. gate BELGIQUE → form non rendu, GET non appelé.
17. parsing ageEnfants CSV "12, 8, 5" → `[12, 8, 5]`.
18. carte délai affichée 30j si urgence true.

### Tests d'intégration

Couverts par SF-FA-19-05 backend.

### Isolation workspace

Couvert par SF-FA-19-05 (le service ne fait que POST/GET avec caseFileId).

---

## Technique

### Endpoints consommés

| Méthode | URL | Auth | Source |
|---------|-----|------|--------|
| POST | `/api/v1/case-files/{caseFileId}/desaccords-parentaux` | Oui | SF-FA-19-05 |
| GET | `/api/v1/case-files/{caseFileId}/desaccords-parentaux` | Oui | SF-FA-19-05 |

### Composants Angular créés

- `DesaccordsParentauxSectionComponent` — composant standalone, panel F-IA-04.
- `DesaccordsParentauxService` — wrapper HttpClient.
- Modèle `desaccords-parentaux.model.ts` (3 enums + 2 interfaces + listes labels).
- Patch `divorce-accepte.model.ts::FamilleExtractedData` : ajout 4 champs
  optionnels (`domaineDesaccordDetecte`, `intensiteDesaccordDetecte`,
  `tentativesMediationDetectees`, `urgenceDetectee`).
- Patch `decisional-tools-panel.component.ts` : entrée `TOOL_REGISTRY` pour
  `F-FA-19-desaccords-parentaux`.

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
- [ ] Navigation / routing — non touché.
- [x] **Aucune préoccupation transversale au sens fort** — composant isolé.

### Impact par domaine métier

Sensible au domaine **DROIT_FAMILLE** (FRANCE uniquement). Pas d'équivalent
immigration ni droit du travail. Belgique = système différent (hors scope V1).

### Parité des domaines métier (outil ≥ niveau 5)

Niveau **5 (scoring)** + **6 (recommandations / fourchettes)** :
- Famille : SF-FA-19-06 livre cet outil (scoring `scoreEligibiliteJaf` +
  recommandations médiation / audition / expertise psy).
- Travail : pas pertinent (concept "désaccord parental" intrinsèquement famille).
- Immigration : pas pertinent.
**Justification** : la notion de désaccord parental art. 373-2-10 est
strictement famille FR.

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — composant frontend isolé.
- [ ] Frontend tsc + Jest doivent passer.

---

## Dépendances

### Subfeatures bloquantes

- `SF-FA-19-05` (backend) — figé (contrat API utilisé).

### Questions ouvertes impactées

- Aucune.

---

## Notes et décisions

- **Pré-fill IA** : 4 champs nouveaux dans `FamilleExtractedData`
  (`domaineDesaccordDetecte`, `intensiteDesaccordDetecte`,
  `tentativesMediationDetectees`, `urgenceDetectee`) + réutilisation de
  `ageEnfants` existant pour `ageEnfantsConcernes` (même donnée).
  `interetSuperieurInvoque` et `expertiseDejaRealisee` sont des décisions
  procédurales avocat, pas pré-rempli IA.
- **Saisie âges enfants** : input texte CSV ("12, 8, 5") parsé via regex —
  cohérent avec `autorite-parentale-section`.
- **Verdict 3 paliers** : ELEVEE (navy clair), MOYENNE (or accent), FAIBLE
  (rouge classique). Palette identique à `autorite-parentale-section`.
- **Urgence et délai** : la carte délai affiche `delaiTraitementJoursPrevisionnel`
  (90j ou 30j si urgence). Couleur or si délai réduit (urgence active),
  navy clair sinon.
- **Pattern miroir** : `autorite-parentale-section` (jumeau F-FA-19, même
  domaine FAMILLE FR, même builder, même structure).
