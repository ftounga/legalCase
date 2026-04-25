# Mini-spec — F-DT-28 / SF-DT-28-02 Frontend avantages conventionnels BE

## Identifiant

`F-DT-28 / SF-DT-28-02`

## Feature parente

`F-DT-28` — Avantages conventionnels belges (pécule de vacances simple + double, prime de fin d'année, éco-chèques, chèques-repas).

## Statut

`ready`

## Date de création

2026-04-25

## Branche Git

`feat/SF-DT-28-02-frontend-avantages-conventionnels-be`

## Pattern de référence

- **Template canonique** : `harcelement-licenciement-nul-section` (F-DT-11-02), réf. skill `ai-skills/frontend-coherence-audit.md` §5.
- **Pattern BE-only** : `motif-grave-be-section` (SF-DT-27-02) — gate bannière info si `workspaceCountry !== 'BELGIQUE'`, structure form / résultat / hydratation GET 200/404, pré-fill IA + alertes F-IA-03 sur 2 fields (`SALAIRE`, `DATE_RUPTURE`).
- **Pattern sélecteur convention (mat-select)** : `indemnite-preavis-section` (SF-DT-25-02) — `MatSelectModule` pour le sélecteur de convention/commission paritaire, options enrichies par défauts.
- **Pattern toggles booléens** : `motif-grave-be-section` (BE) + composants à toggles `MatSlideToggleModule` (`indemnite-preavis-section` heures atypiques).
- **Helper partagé** : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).

## Contrat API IMPORTÉ (figé dans SF-DT-28-01)

`POST /api/v1/case-files/{caseFileId}/avantages-conventionnels-be`
`GET  /api/v1/case-files/{caseFileId}/avantages-conventionnels-be`

Request :

```typescript
export interface AvantagesConventionnelsBeRequest {
  salaireMensuelBrutEur: number;          // > 0
  joursTravaillesAnneePrecedente: number; // ≥ 0, ≤ 365 (généralement ≤ 251)
  anciennetteMois: number;                // ≥ 0
  commissionParitaire: string;            // CP_200 | CP_124 | CP_111 | CP_226 | CP_337 | …
  annee: number;                          // YYYY (ex. 2026)
  doublePeculeVacancesPercu: boolean;     // employeur a-t-il déjà versé le 2e pécule ?
  primeFinAnneePrevueCcCp: boolean;       // CCT/CP prévoit-elle la prime ?
  ecoChequesPrevuCcCp: boolean;
  ecoChequesUtilisationDansAn: boolean;
  chequesRepasPrevu: boolean;
  joursPrestesEffectifs: number;          // ≥ 0, base chèques-repas
}
```

Response :

```typescript
export interface AvantagesConventionnelsBeResponse {
  caseFileId: string;
  // Inputs ré-affichés
  salaireMensuelBrutEur: number;
  joursTravaillesAnneePrecedente: number;
  anciennetteMois: number;
  commissionParitaire: string;
  annee: number;
  doublePeculeVacancesPercu: boolean;
  primeFinAnneePrevueCcCp: boolean;
  ecoChequesPrevuCcCp: boolean;
  ecoChequesUtilisationDansAn: boolean;
  chequesRepasPrevu: boolean;
  joursPrestesEffectifs: number;
  // Outputs
  peculeVacancesSimpleEur: number;
  doublePeculeVacancesEur: number;
  primeFinAnneeEur: number;
  ecoChequesValeurAnnuelleEur: number;
  chequesRepasValeurAnnuelleEur: number;
  totalAvantagesAnnuelsEur: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'BELGIQUE';
}
```

Erreurs :

| Code | Cas |
|------|-----|
| 400 | `workspaceCountry !== BELGIQUE`, salaire ≤ 0, jours hors bornes, ancienneté < 0, CP inconnu, année non plausible |
| 403 | Workspace non autorisé |
| 404 | GET sans analyse persistée préalable |

---

## Objectif

Livrer le composant Angular `<app-avantages-conventionnels-be-section>` qui consomme l'endpoint `POST/GET /api/v1/case-files/{id}/avantages-conventionnels-be` (SF-DT-28-01) pour calculer 5 avantages conventionnels belges (pécule de vacances simple + double pécule, prime de fin d'année, éco-chèques, chèques-repas) + total annuel, avec affichage en cartes, formule + base juridique + messages.

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre un dossier BE (DROIT_DU_TRAVAIL, workspace `country = 'BELGIQUE'`).
2. Le panel décisionnel F-IA-04 affiche l'outil `F-DT-28-avantages-conventionnels-be` (visibility règle injectée par SF-DT-28-01, ALWAYS_ON BE).
3. Le composant collapsible affiche en header `AVANTAGES CONVENTIONNELS BE` + chip statut `Calculé` + total annuel formaté en JetBrains Mono si résultat persisté.
4. À l'ouverture (dépli) : GET → si 200 résultat précédent, mode résultat ; si 404, formulaire + pré-fill IA depuis `synthesis.travailExtractedData`.
5. Formulaire :
   - `salaireMensuelBrutEur` (`<input type="number">`, > 0, obligatoire) — pré-rempli depuis `aiData.salaireBrutMensuel`
   - `joursTravaillesAnneePrecedente` (`<input type="number">`, 0–365, obligatoire)
   - `anciennetteMois` (`<input type="number">`, ≥ 0, obligatoire)
   - `commissionParitaire` (`<mat-select>`, obligatoire) — CP_200 (employés défaut), CP_124 (construction), CP_111 (métal), CP_226 (commerce intl.), CP_337 (non-marchand)
   - `annee` (`<input type="number">`, 2020–2030, obligatoire) — pré-rempli année courante
   - `doublePeculeVacancesPercu` (`<mat-slide-toggle>`)
   - `primeFinAnneePrevueCcCp` (`<mat-slide-toggle>`)
   - `ecoChequesPrevuCcCp` (`<mat-slide-toggle>`)
   - `ecoChequesUtilisationDansAn` (`<mat-slide-toggle>`)
   - `chequesRepasPrevu` (`<mat-slide-toggle>`)
   - `joursPrestesEffectifs` (`<input type="number">`, ≥ 0, obligatoire)
6. À la soumission (POST) : appel service, puis affichage mode résultat + `MatSnackBar` succès + `CaseDashboardRefreshService.triggerRefresh()`.
7. Mode résultat :
   - 5 cartes (pécule simple, double pécule, prime fin d'année, éco-chèques, chèques-repas) avec montants en JetBrains Mono, et label spécifique chaque carte.
   - **Carte total annuel** mise en avant (taille augmentée, accent navy/or selon montant > 0).
   - `formule` en JetBrains Mono dans bloc dédié.
   - `baseJuridique` en JetBrains Mono italique (footer).
   - `messages[]` rendus via `LegalCitationsPipe` (citations art. CCT 109 / loi 28/06/1971 / CCT 90 etc. en JetBrains Mono inline).
   - Bouton "Modifier" pour ré-éditer.

### Gate pays (pattern motif-grave-be-section)

- Si `workspaceCountry !== 'BELGIQUE'` → **bannière info** navy (`info_outline`) : "Cet outil s'applique à la Belgique uniquement. En France, les équivalents (13e mois, prime conventionnelle, tickets-restaurant) sont gérés via la convention collective applicable et la convention de l'entreprise — pas d'outil dédié actuellement." Pas d'appel HTTP, pas de form.
- Si `workspaceCountry === 'BELGIQUE'` → formulaire + GET hydrate.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| GET 404 | Mode formulaire + pré-fill IA |
| GET autre erreur | `MatSnackBar` "Impossible de charger l'analyse" + reste en mode formulaire vide |
| POST 400 (validation) | `MatSnackBar` avec message backend |
| POST 403/404 | `MatSnackBar` générique |
| Backend down | `MatSnackBar` "Erreur lors de l'analyse" |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- **Autres outils métier décisionnels** : `motif-grave-be-section` (BE), `indemnite-preavis-section` (FR sélecteur convention), `belgian-9bis-section` (BE), `annexe13-be-section` (BE). Cet outil réutilise les mêmes patterns. Pas de pattern nouveau introduit.
- **Autres pays** : FR non applicable. BE only — gate UI bannière info symétrique aux autres outils BE-only.
- **Autres domaines** : DROIT_DU_TRAVAIL BELGIQUE uniquement. Immigration/Famille non applicables.
- **Autres UI patterns** : aucun pattern nouveau. Réutilise `CoherencePopoverTriggerDirective`, `CoherenceAlertBuilder`, `LegalCitationsPipe`, `CaseDashboardRefreshService`, `MatSelectModule` (déjà utilisé par `indemnite-preavis-section`), `MatSlideToggleModule` (idem).
- **Auth / workspace / plans / navigation** : aucun impact — composant isolé.

### Niveaux de vérification

- **Modèle TypeScript** : `avantages-conventionnels-be.model.ts` (Request / Response + enum `CommissionParitaireBe`) calqué sur le contrat figé SF-DT-28-01.
- **DTO backend** : Request 11 champs + Response 18 champs. Tous mappés côté frontend.
- **Service / logique métier** : `AvantagesConventionnelsBeService` côté frontend = wrapper HttpClient POST/GET. Pas de logique additionnelle.
- **Tests existants** : frontend à créer (≥ 18 tests). Backend SF-DT-28-01 hors scope frontend.

### Cas spécifique : outil décisionnel

- **Cohérence IA (F-IA-03)** : alerte de cohérence **`SALAIRE`** → divergence relative > 10 % entre `aiData.salaireBrutMensuel` et saisie. Les autres champs (jours travaillés, ancienneté en mois, commission paritaire, année, toggles) ne sont pas extraits par l'IA actuelle — pas d'alerte pour eux.
- **Refresh dashboard (F-IA-02)** : `triggerRefresh()` appelé après POST succès.
- **Pré-remplissage IA** : `prefillFromAi()` branche `aiData.salaireBrutMensuel` → `salaireMensuelBrutEur`. Année courante pré-remplie par défaut. Badge "Pré-rempli depuis l'analyse" sous le field salaire avec signal provenance effacé au `onSalaireChange()`.
- **Persistance des inputs** : tous les inputs sont supposés persistés côté backend (cf. contrat). OK au reload.
- **Masquage conditionnel** : orchestré par le panel F-IA-04 via `TOOL_REGISTRY` + règle visibility ALWAYS_ON BELGIQUE (configurée backend SF-DT-28-01).
- **Alertes actives après calcul** : `coherenceAlerts = computed(() => { if (!this.showForm()) return {}; ... })` — gate correct (pattern canonique).

### Cas spécifique : nouveau pattern UI / service partagé

Aucun. Le composant réutilise intégralement les patterns établis. Les 5 "cartes par avantage" + "carte total en grand" sont une simple variation visuelle locale (style SCSS), pas un composant partagé.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pattern canonique (harcelement-licenciement-nul-section) | Oui | Intégré — structure .ts / .html / .scss / .spec.ts calquée |
| Pattern BE-only (motif-grave-be-section) | Oui | Bannière info FR, gate `isBelgium()`, hydratation GET 200/404 |
| Pattern sélecteur convention (indemnite-preavis-section) | Oui | `MatSelectModule` + options enum `CommissionParitaireBe` |
| Pattern toggles (slide-toggle) | Oui | 5 toggles booléens |
| CoherenceAlertBuilder / CoherenceAlert<F> partagés (SF-155-05) | Oui | Utilisé pour l'alerte F-IA-03 SALAIRE |
| LegalCitationsPipe (SF-155-01) | Oui | Utilisé pour `baseJuridique` + `messages[]` |
| Autres domaines (DROIT_FAMILLE / DROIT_IMMIGRATION) | Non | Avantages conventionnels = concept BE droit du travail spécifique |
| Autres pays (FR) | Non | Outil BE-only — équivalents FR gérés directement par convention de l'entreprise |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [ ] Subfeature(s) parallèle(s) — aucune
- [ ] Backlog — aucun
- [x] Non applicable aux autres cibles (FR, DROIT_FAMILLE, DROIT_IMMIGRATION) — justifié

---

## Impact par domaine métier

- **DROIT_DU_TRAVAIL BELGIQUE uniquement** — les avantages conventionnels (pécule, double pécule, prime fin année, éco-chèques, chèques-repas) sont des concepts spécifiques au droit du travail belge, encadrés par la loi du 28/06/1971 (pécule), CCT 90 (avantages non récurrents), CCT 98 (éco-chèques) et la convention collective de la commission paritaire compétente.
- **DROIT_DU_TRAVAIL FRANCE** : non applicable. Les équivalents FR (13e mois, prime conventionnelle, tickets-restaurant) sont gérés directement par la convention collective ou la convention d'entreprise — pas d'outil dédié actuellement (backlog F-DT- avantages FR à envisager si demande).
- **DROIT_IMMIGRATION / DROIT_FAMILLE** : non applicable. Pas de concept équivalent.

---

## Parité des domaines métier

Non applicable — outil de **niveau 3 (calculateur)**, pas niveau ≥ 5. La règle de parité concerne les outils de niveau ≥ 5.

Note : si une feature jumelle FR "avantages conventionnels FR" est ajoutée au backlog, elle sera classée niveau 3 également et ne nécessitera pas de feature jumelle de parité au sens strict (chaque outil = une situation métier, conformément à l'invariant "un outil décisionnel = une situation métier").

---

## Critères d'acceptation

- [ ] Composant Angular standalone `<app-avantages-conventionnels-be-section>` créé avec inputs `caseFileId` (required), `workspaceCountry`, `aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`.
- [ ] Gate pays : si `workspaceCountry !== 'BELGIQUE'` → bannière info navy, pas d'appel HTTP.
- [ ] GET `/api/v1/case-files/{id}/avantages-conventionnels-be` au `ngOnInit` si BELGIQUE. 200 → mode résultat ; 404 → mode formulaire + pré-fill IA.
- [ ] Pré-fill IA : `aiData.salaireBrutMensuel` → `salaireMensuelBrutEur`. Année courante par défaut. Badge "Pré-rempli depuis l'analyse" sous le field salaire. Signal `provenanceSalaire` effacé au change manuel.
- [ ] `coherenceAlerts` computed retourne `{}` quand `!showForm()`. Alerte F-IA-03 unique : `SALAIRE` (divergence relative > 10 %). Construite via `CoherenceAlertBuilder`.
- [ ] Formulaire valide uniquement si tous les champs numériques renseignés + commissionParitaire choisie + plages cohérentes (salaire > 0, jours 0–365, ancienneté ≥ 0, année 2020–2030, joursPrestesEffectifs ≥ 0).
- [ ] `mat-select` commissionParitaire propose au moins CP_200, CP_124, CP_111, CP_226, CP_337 avec libellés humains.
- [ ] 5 toggles `<mat-slide-toggle>` pour les 4 booléens CCT/CP + 1 chèques-repas.
- [ ] POST `/api/v1/case-files/{id}/avantages-conventionnels-be` à la soumission. Succès → résultat persisté affiché + `MatSnackBar` + `CaseDashboardRefreshService.triggerRefresh()`.
- [ ] Mode résultat : 5 cartes par avantage avec montants en JetBrains Mono + carte total annuel mise en avant (taille augmentée, navy/or accent).
- [ ] Typographie : `baseJuridique`, `formule`, montants des 6 cartes en JetBrains Mono. Labels, titres, libellés en Inter.
- [ ] `LegalCitationsPipe` appliqué sur `baseJuridique` + `messages[]` pour rendre `art. 38 loi 28/06/1971`, `CCT 98`, etc. en JetBrains Mono inline.
- [ ] Erreurs HTTP : `MatSnackBar` (jamais `alert/confirm`).
- [ ] Entrée `TOOL_REGISTRY` pour `F-DT-28-avantages-conventionnels-be` ajoutée dans `decisional-tools-panel.component.ts` — inputs canoniques : `caseFileId`, `workspaceCountry`, `aiData: synthesis?.travailExtractedData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`.
- [ ] Couverture tests ≥ 18 (cf. plan de test).
- [ ] `tsc --noEmit` vert. Tests Jest ciblés verts (`avantages-conventionnels-be-section`).

---

## Périmètre

### Hors scope (explicite)

- **Backend** : SF-DT-28-01, hors scope ici (frontend uniquement).
- **Pré-fill IA des autres champs** (jours travaillés, ancienneté en mois, commission paritaire, toggles, joursPrestesEffectifs) : non extraits par le prompt IA travail actuel — l'avocat les saisit manuellement. Une SF backend ultérieure pourrait étendre le prompt pour pré-remplir `commissionParitaire` (issue de `conventionCollective` extraite) et `joursPrestesEffectifs` — mais hors scope ici.
- **Templates PDF / exports** : hors scope V8.
- **Sélecteur convention enrichi via `ConventionReferentialService`** : conserver une liste statique des 5 CP au minimum dans le composant frontend ; la dynamisation depuis le référentiel CCT (issue F-DT-28 description backlog) est hors scope SF-DT-28-02 — SF future à envisager si la liste s'allonge.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `collapsed` | `true` | Replié par défaut (pattern canonique) |
| `showForm` | `true` | Form affiché tant que résultat non chargé |
| `provenanceSalaire` | `null` | Badge IA effacé au démarrage ; set à 'IA' par prefillFromAi |
| `annee` | année courante (`new Date().getFullYear()`) | Pré-rempli pour confort de saisie |
| `doublePeculeVacancesPercu` | `false` | Toggle off par défaut |
| `primeFinAnneePrevueCcCp` | `false` | Idem |
| `ecoChequesPrevuCcCp` | `false` | Idem |
| `ecoChequesUtilisationDansAn` | `false` | Idem |
| `chequesRepasPrevu` | `false` | Idem |

---

## Contraintes de validation

| Champ | Obligatoire | Format | Normalisation |
|-------|-------------|--------|---------------|
| salaireMensuelBrutEur | Oui | > 0 | - |
| joursTravaillesAnneePrecedente | Oui | Integer 0–365 | - |
| anciennetteMois | Oui | Integer ≥ 0 | - |
| commissionParitaire | Oui | enum CP_xxx | - |
| annee | Oui | Integer 2020–2030 | - |
| doublePeculeVacancesPercu | Oui | boolean | par défaut `false` |
| primeFinAnneePrevueCcCp | Oui | boolean | par défaut `false` |
| ecoChequesPrevuCcCp | Oui | boolean | par défaut `false` |
| ecoChequesUtilisationDansAn | Oui | boolean | par défaut `false` |
| chequesRepasPrevu | Oui | boolean | par défaut `false` |
| joursPrestesEffectifs | Oui | Integer ≥ 0 | - |

---

## Technique

### Endpoint(s) consommés

| Méthode | URL | Backend SF |
|---------|-----|------------|
| POST | `/api/v1/case-files/{caseFileId}/avantages-conventionnels-be` | SF-DT-28-01 |
| GET | `/api/v1/case-files/{caseFileId}/avantages-conventionnels-be` | SF-DT-28-01 |

### Composants Angular créés

- `avantages-conventionnels-be.model.ts` — types `AvantagesConventionnelsBeRequest`, `AvantagesConventionnelsBeResponse`, enum `CommissionParitaireBe` + table de libellés humains.
- `avantages-conventionnels-be.service.ts` — `AvantagesConventionnelsBeService` HttpClient wrapper.
- `avantages-conventionnels-be-section.component.{ts,html,scss,spec.ts}` — composant section intégré au panel F-IA-04.

### Modifications

- `decisional-tools-panel.component.ts` — ajout entrée `F-DT-28-avantages-conventionnels-be` dans `TOOL_REGISTRY`.

---

## Plan de test (≥ 18)

### Tests unitaires composant (Jest)

1. BELGIQUE → GET appelé au ngOnInit.
2. FRANCE → aucun appel HTTP + bannière info rendue.
3. Charge l'analyse existante si GET 200 (mode résultat, valeurs hydratées).
4. Reste en formulaire si GET 404.
5. `formValid()` false si salaire ≤ 0.
6. `formValid()` false si jours travaillés < 0 ou > 365.
7. `formValid()` false si ancienneté en mois < 0.
8. `formValid()` false si commission paritaire absente.
9. `formValid()` false si année < 2020 ou > 2030.
10. `formValid()` true avec valeurs canoniques.
11. POST → succès hydrate le résultat + snackbar + triggerRefresh().
12. POST → erreur backend (400) → snackbar message erreur.
13. POST ignoré si form invalide (pas d'appel HTTP).
14. Pré-fill IA : `salaireBrutMensuel` → `salaireMensuelBrutEur` + badge IA.
15. Pré-fill IA : `aiData` absent → aucun pré-fill, pas d'erreur.
16. `onSalaireChange()` efface `provenanceSalaire`.
17. `coherenceAlerts.SALAIRE` si divergence > 10 %.
18. `coherenceAlerts.SALAIRE` absent si écart ≤ 10 %.
19. `coherenceAlerts` retourne `{}` en mode résultat (showForm=false).
20. `editMode()` → repasse en formulaire.
21. `toggleCollapse()` inverse l'état collapsed.
22. `commissionParitaireOptions` expose au moins CP_200, CP_124, CP_111, CP_226, CP_337.
23. `bannerClass(total)` : navy si total > 0, neutre si total = 0.

### Isolation workspace

Non applicable ici — l'isolation est côté backend. Côté frontend, le composant consomme l'endpoint qui gère l'isolation (cf. service SF-DT-28-01).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée, impact limité à son périmètre (nouveau composant + ajout TOOL_REGISTRY).

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — justification : composant isolé visible uniquement en contexte BE + DROIT_DU_TRAVAIL. Pas d'impact sur auth, navigation ou workspace.

---

## Dépendances

### Subfeatures bloquantes

- SF-DT-28-01 — backend (contrat API figé, fourni par mission ; SF parallèle ou mergée selon planning).
- SF-155-05 (CoherenceAlertBuilder) — statut : done.
- SF-155-01 (LegalCitationsPipe) — statut : done.

### Questions ouvertes

Aucune.

---

## Notes et décisions

- **Pas de rouge** dans la palette : un total à 0 n'est pas une "alerte critique" (juste l'indication que les avantages ne sont pas dus dans la situation décrite). Palette navy/or — aligné avec `motif-grave-be-section`.
- **Carte total annuel en grand** : taille `font-size: 24px`, accent navy, mise en avant visuelle (border-left 4px navy, padding renforcé). Pattern visuel local (ne devient pas composant partagé).
- **Liste CP statique** : on conserve les 5 CP les plus fréquentes. Une dynamisation via `ConventionReferentialService` (SF future) pourrait remonter toutes les CP du référentiel — mais hors scope SF-DT-28-02 pour ne pas créer un couplage prématuré.
- **Alertes F-IA-03** : limitées au seul field `SALAIRE` correspondant au seul field pré-rempli depuis `aiData`. Les 10 autres champs n'ont pas de source IA → pas d'alerte possible à ce jour. Si l'extraction IA est étendue plus tard (ex. `commissionParitaire` à partir de `conventionCollective`), une SF future ajoutera d'autres alertes.
- **TOOL_REGISTRY ordre** : à placer après l'entrée `F-DT-27-motif-grave-be` pour rester regroupée avec les outils BE-only.
