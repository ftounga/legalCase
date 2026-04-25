# Mini-spec — F-DT-26 / SF-DT-26-02 Frontend indemnité compensatrice congés payés FR

## Identifiant

`F-DT-26 / SF-DT-26-02`

## Feature parente

`F-DT-26` — Indemnité compensatrice de congés payés (art. L.3141-26 Code du travail)

## Statut

`ready`

## Date de création

2026-04-25

## Branche Git

`feat/SF-DT-26-02-frontend-conges-payes`

## Pattern de référence

- **Template canonique** : `harcelement-licenciement-nul-section` (F-DT-11-02), réf. skill `ai-skills/frontend-coherence-audit.md` §5.
- **Pattern structure form + résultat** : `indemnite-precarite-cdd-section` (SF-DT-17-02) — calque structure pour outil FR-only consommant total + tauxRadio + casExclusion.
- **Pattern IA** : `immigration-title-decision-section` (F-IM-05) pour `prefillFromAi()` + signals provenance + `coherenceAlerts` ; `motif-grave-be-section` (SF-DT-27-02) pour le pattern datepicker + salaire.
- **Helper partagé** : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
- **Contrat API importé de SF-DT-26-01 backend** (parallélisation autorisée — contrat figé dans la mission).

---

## Contrat API (importé de SF-DT-26-01)

### Endpoint

```
POST + GET /api/v1/case-files/{caseFileId}/conges-payes-indemnite
```

### Request

```typescript
export type MethodeCpForcee = 'DIX_POURCENT' | 'MAINTIEN';

export interface CongesPayesIndemniteRequest {
  totalRemunerationPeriodeEur: number;
  joursAcquisAnnee: number;
  joursPris: number;
  salaireMensuelBrutEur: number;
  dateRupture: string;          // YYYY-MM-DD
  methodeForcee?: MethodeCpForcee | null;
}
```

### Response

```typescript
export interface CongesPayesIndemniteResponse {
  caseFileId: string;
  totalRemunerationPeriodeEur: number;
  joursAcquisAnnee: number;
  joursPris: number;
  salaireMensuelBrutEur: number;
  dateRupture: string;
  methodeForcee: MethodeCpForcee | null;
  joursDus: number;
  montantMethodeDixPourcentEur: number;
  montantMethodeMaintienEur: number;
  methodeRetenue: MethodeCpForcee;
  montantIndemniteEur: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}
```

---

## Objectif

Livrer le composant Angular `<app-conges-payes-section>` qui consomme l'endpoint `POST/GET /api/v1/case-files/{id}/conges-payes-indemnite` (SF-DT-26-01) pour calculer et **comparer les 2 méthodes** d'indemnité compensatrice de congés payés (méthode 10 % L.3141-24 vs méthode du maintien L.3141-22) et retenir la plus favorable au salarié (sauf surcharge `methodeForcee`). FR uniquement (concept BE distinct géré séparément dans le bloc travail BE — pécule de vacances).

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre un dossier FR (DROIT_DU_TRAVAIL, workspace `country = 'FRANCE'`).
2. Le panel décisionnel F-IA-04 affiche l'outil `F-DT-26-conges-payes-indemnite` (règle `ALWAYS_ON` FR + travail).
3. Le composant collapsible affiche en header `INDEMNITÉ COMPENSATRICE DE CONGÉS PAYÉS` + chip montant retenu si résultat persisté.
4. À l'ouverture (dépli) : GET → si 200 résultat précédent, affichage du mode résultat ; si 404, formulaire + pré-fill IA depuis `synthesis.travailExtractedData`.
5. Formulaire :
   - `totalRemunerationPeriodeEur` (`<input type="number">`, > 0, obligatoire)
   - `joursAcquisAnnee` (`<input type="number">`, ≥ 0, obligatoire)
   - `joursPris` (`<input type="number">`, ≥ 0, obligatoire ; ≤ `joursAcquisAnnee` UI-side)
   - `salaireMensuelBrutEur` (`<input type="number">`, > 0, obligatoire) — pré-rempli depuis `aiData.salaireBrutMensuel` si disponible
   - `dateRupture` (`<input type="date">`, obligatoire, ≤ aujourd'hui) — pré-remplie depuis `aiData.dateLicenciement` si disponible
   - `methodeForcee` (`mat-radio-group`, 3 options : "Auto (le plus favorable)" = `null`, "Méthode 10 %" = `'DIX_POURCENT'`, "Méthode du maintien" = `'MAINTIEN'`). Défaut : `null` (Auto).
6. À la soumission (POST) : appel service, puis affichage mode résultat + `MatSnackBar` succès + `CaseDashboardRefreshService.triggerRefresh()`.
7. Mode résultat — **comparateur visuel des 2 méthodes** :
   - Bandeau navy "Méthode retenue : X" en grand.
   - Carte côte-à-côte 2 colonnes : "Méthode 10 %" (montant `montantMethodeDixPourcentEur`) et "Méthode du maintien" (montant `montantMethodeMaintienEur`). Badge "Le plus favorable" sur la carte correspondant à `methodeRetenue`.
   - Montant final `montantIndemniteEur` en grand (32 px JetBrains Mono).
   - `joursDus` indiqué.
   - `formule` en JetBrains Mono.
   - `baseJuridique` en JetBrains Mono italique via `LegalCitationsPipe`.
   - `messages[]` rendus via `LegalCitationsPipe` dans `<ul>`.
   - Bouton "Modifier" pour ré-éditer.

### Gate pays (pattern indemnite-precarite-cdd-section)

- Si `workspaceCountry !== 'FRANCE'` → **bannière info** navy (`info_outline`) : "Cet outil s'applique à la France uniquement. Pour la Belgique, voir le pécule de vacances (régime distinct)." Pas d'appel HTTP, pas de form.
- Si `workspaceCountry === 'FRANCE'` → formulaire + charge GET.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| GET 404 | Mode formulaire + pré-fill IA |
| GET autre erreur | Reste en mode formulaire vide |
| POST 400 (ex. `joursPris > joursAcquisAnnee` côté backend) | `MatSnackBar` avec message backend |
| POST 403/404 | `MatSnackBar` générique |
| Backend down | `MatSnackBar` "Erreur lors du calcul" |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- **Autres outils métier** : tous les outils décisionnels frontend droit du travail FR (anciennete, licenciement, rupture-conv, harcelement, discrimination, inaptitude, heures-sup, indemnite-precarite-cdd, travail-dissimule). Cet outil réutilise les mêmes patterns : pas de pattern nouveau.
- **Autres pays** : FR-only ici. La BE (pécule de vacances) sera traitée séparément dans le backlog si pertinent — concept distinct (lié à l'année antérieure, calcul différent ONSS), pas un simple mapping. Bannière info en gate.
- **Autres domaines** : DROIT_DU_TRAVAIL FRANCE uniquement. Immigration / Famille non applicables.
- **Autres UI patterns** : aucune introduction de nouveau pattern shared. Réutilise `CoherencePopoverTriggerDirective`, `CoherenceAlertBuilder`, `LegalCitationsPipe`, `CaseDashboardRefreshService`.
- **Auth / workspace / plans / navigation** : aucun impact — composant isolé.

### Niveaux de vérification

- **Modèle TypeScript** : `conges-payes-indemnite.model.ts` (Request / Response) calqué strictement sur le contrat figé.
- **Record / DTO backend** : importé tel quel — 16 champs Response.
- **Service / logique métier** : `CongesPayesIndemniteService` côté frontend = wrapper HttpClient POST/GET. Pas de logique additionnelle.
- **Entité JPA + schéma DB** : aucune modification (backend SF-DT-26-01 en parallèle).
- **Tests existants** : frontend à créer (≥ 15 tests).

### Cas spécifique : outil décisionnel

- **Cohérence IA (F-IA-03)** : alerte de cohérence **`SALAIRE_MENSUEL`** → divergence relative > 10 % entre `aiData.salaireBrutMensuel` et saisie. Alerte **`DATE_RUPTURE`** → divergence `aiData.dateLicenciement` vs `dateRupture` saisie. Les autres champs (`totalRemunerationPeriodeEur`, `joursAcquisAnnee`, `joursPris`) ne sont pas extraits par l'IA actuelle — pas d'alerte pour eux.
- **Refresh dashboard (F-IA-02)** : `triggerRefresh()` appelé après POST succès.
- **Pré-remplissage IA** : `prefillFromAi()` branche `aiData.salaireBrutMensuel` → `salaireMensuelBrutEur` + `aiData.dateLicenciement` → `dateRupture`. Badges "Pré-rempli depuis l'analyse" avec signal provenance effacé au `onXxxChange()`.
- **Persistance des inputs** : tous les inputs persistés côté backend (confirmé par contrat API). OK au reload.
- **Masquage conditionnel** : orchestré par le panel F-IA-04 via `TOOL_REGISTRY` + règle visibility `ALWAYS_ON` FRANCE.
- **Alertes actives après calcul** : `coherenceAlerts = computed(() => { if (!this.showForm()) return {}; ... })` — gate correct (pattern canonique anti-bug SF-IA-03-12).

### Cas spécifique : nouveau pattern UI / service partagé

Aucun. Le composant réutilise intégralement les patterns établis. Le **comparateur 2 méthodes** côte-à-côte est une variante visuelle locale (pas un nouveau pattern shared) inspirée du `comparatif-section` mais simplifiée à 2 cartes statiques avec badge "Le plus favorable".

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pattern canonique (harcelement-licenciement-nul-section) | Oui | Intégré — structure .ts / .html / .scss / .spec.ts calquée |
| Pattern structure CDD (indemnite-precarite-cdd-section) | Oui | Calque pour radio méthode + total montant |
| Pattern IA (immigration-title-decision-section / motif-grave-be-section) | Oui | `prefillFromAi()` + signals provenance + `coherenceAlerts` + handlers onXxxChange |
| CoherenceAlertBuilder / CoherenceAlert<F> partagés (SF-155-05) | Oui | Utilisé pour les 2 alertes F-IA-03 |
| LegalCitationsPipe (SF-155-01) | Oui | Utilisé pour `baseJuridique` + `messages[]` |
| Autres domaines (DROIT_FAMILLE / DROIT_IMMIGRATION) | Non | Indemnité de CP est un concept droit du travail FR |
| Autres pays (BE) | Non | Pécule de vacances BE = régime distinct (à traiter séparément backlog) |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [ ] Subfeature(s) parallèle(s) — aucune (BE pécule de vacances reste backlog F-DT distinct)
- [x] Backlog — backlog BE pécule à conserver séparément
- [x] Non applicable aux autres cibles (BE direct, DROIT_FAMILLE, DROIT_IMMIGRATION) — justifié

---

## Impact par domaine métier

- **DROIT_DU_TRAVAIL FRANCE uniquement** — l'indemnité compensatrice de CP est un concept spécifique au droit du travail FR (art. L.3141-22 et L.3141-24).
- **DROIT_DU_TRAVAIL BELGIQUE** : non applicable. L'équivalent BE (pécule de vacances) suit un régime distinct ONSS — backlog séparé pour préserver la séparation un-outil-une-situation (règle CLAUDE.md).
- **DROIT_IMMIGRATION / DROIT_FAMILLE** : non applicable. Pas de concept équivalent.

---

## Parité des domaines métier

Outil de **niveau 6 (comparateur)** — 2 méthodes côte-à-côte avec choix du plus favorable.

| Domaine | Équivalent existant ? | Décision |
|---------|----------------------|----------|
| DROIT_DU_TRAVAIL FR | Cette feature (F-DT-26) | OK |
| DROIT_DU_TRAVAIL BE | Pécule de vacances ONSS — concept distinct, calcul ≠ | Backlog séparé (un-outil-une-situation, règle CLAUDE.md) |
| DROIT_IMMIGRATION FR / BE | N/A | Concept non transposable |
| DROIT_FAMILLE FR / BE | N/A | Concept non transposable |

Le pattern "comparateur 2 méthodes au plus favorable" est déjà présent dans F-DT-09 (comparateur indemnités licenciement) — réutilisation cohérente, pas d'asymétrie introduite.

---

## Critères d'acceptation

- [x] Composant Angular standalone `<app-conges-payes-section>` créé avec inputs `caseFileId` (required), `workspaceCountry`, `aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`.
- [x] Gate pays : si `workspaceCountry !== 'FRANCE'` → bannière info navy, pas d'appel HTTP.
- [x] GET `/api/v1/case-files/{id}/conges-payes-indemnite` au `ngOnInit` si FRANCE. 200 → mode résultat ; 404 → mode formulaire + pré-fill IA.
- [x] Pré-fill IA : `aiData.salaireBrutMensuel` → `salaireMensuelBrutEur` + `aiData.dateLicenciement` → `dateRupture`. Badge "Pré-rempli depuis l'analyse" sous chaque field pré-rempli. Signal `provenance<Field>` effacé au change manuel.
- [x] `coherenceAlerts` computed retourne `{}` quand `!showForm()`. Alertes F-IA-03 : `SALAIRE_MENSUEL` (divergence relative > 10 %) + `DATE_RUPTURE` (divergence aiData.dateLicenciement vs saisie). Construites via `CoherenceAlertBuilder`.
- [x] Formulaire valide uniquement si `totalRemunerationPeriodeEur > 0` + `joursAcquisAnnee >= 0` + `joursPris >= 0` (et UI ≤ joursAcquisAnnee) + `salaireMensuelBrutEur > 0` + `dateRupture` non vide ≤ aujourd'hui.
- [x] Radio méthodeForcee : 3 options (Auto null / DIX_POURCENT / MAINTIEN). Auto par défaut.
- [x] POST `/api/v1/case-files/{id}/conges-payes-indemnite` à la soumission. Succès → résultat persisté affiché + `MatSnackBar` + `CaseDashboardRefreshService.triggerRefresh()`.
- [x] Mode résultat : carte "Méthode retenue : X" en navy, **comparateur 2 méthodes côte-à-côte** avec badge "Le plus favorable" sur la méthode retenue, montant final en grand.
- [x] Typographie : `baseJuridique`, `formule`, montants en JetBrains Mono. Labels, titres, messages en Inter.
- [x] `LegalCitationsPipe` appliqué sur `baseJuridique` + `messages[]`.
- [x] Erreurs HTTP : `MatSnackBar` (jamais `alert/confirm`).
- [x] Entrée `TOOL_REGISTRY` pour `F-DT-26-conges-payes-indemnite` ajoutée dans `decisional-tools-panel.component.ts` — inputs canoniques : `caseFileId`, `workspaceCountry`, `aiData: synthesis?.travailExtractedData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`.
- [x] Couverture tests ≥ 15.
- [x] `tsc --noEmit` vert. Tests Jest ciblés verts (`conges-payes-section`).

---

## Périmètre

### Hors scope (explicite)

- **Backend** : développé en parallèle (SF-DT-26-01) — pas de modification depuis cette branche.
- **Pécule de vacances BE** : régime distinct, hors scope F-DT-26.
- **Templates PDF / exports** : hors scope.
- **Extraction IA enrichie** (joursAcquis / joursPris) : hors scope. À envisager dans une SF backend ultérieure si pertinent.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `collapsed` | `true` | Replié par défaut |
| `showForm` | `true` | Form affiché tant que résultat non chargé |
| `methodeForcee` | `null` | Auto = backend choisit le plus favorable |
| `provenance<Field>` | `null` | Badge IA effacé au démarrage ; set à 'IA' par prefillFromAi |

---

## Contraintes de validation

| Champ | Obligatoire | Format | Normalisation |
|-------|-------------|--------|---------------|
| totalRemunerationPeriodeEur | Oui | > 0 | - |
| joursAcquisAnnee | Oui | ≥ 0 | - |
| joursPris | Oui | ≥ 0 ; ≤ joursAcquisAnnee | - |
| salaireMensuelBrutEur | Oui | > 0 | - |
| dateRupture | Oui | YYYY-MM-DD, ≤ aujourd'hui | - |
| methodeForcee | Non | null \| DIX_POURCENT \| MAINTIEN | - |

---

## Technique

### Endpoint(s) consommés

| Méthode | URL | Backend SF |
|---------|-----|------------|
| POST | `/api/v1/case-files/{caseFileId}/conges-payes-indemnite` | SF-DT-26-01 |
| GET | `/api/v1/case-files/{caseFileId}/conges-payes-indemnite` | SF-DT-26-01 |

### Composants Angular créés

- `conges-payes-indemnite.model.ts` — types `MethodeCpForcee`, `CongesPayesIndemniteRequest`, `CongesPayesIndemniteResponse`.
- `conges-payes-indemnite.service.ts` — `CongesPayesIndemniteService` HttpClient wrapper.
- `conges-payes-section.component.{ts,html,scss,spec.ts}` — composant section intégré au panel F-IA-04.

### Modifications

- `decisional-tools-panel.component.ts` — ajout entrée `F-DT-26-conges-payes-indemnite` dans `TOOL_REGISTRY`.

---

## Plan de test (≥ 15)

### Tests unitaires composant (Jest)

1. FRANCE → GET appelé au ngOnInit.
2. BELGIQUE → aucun appel HTTP + bannière info rendue.
3. Charge l'analyse existante si GET 200 (mode résultat, valeurs hydratées).
4. Reste en formulaire si GET 404.
5. `formValid()` false si `totalRemunerationPeriodeEur` ≤ 0.
6. `formValid()` false si `salaireMensuelBrutEur` ≤ 0.
7. `formValid()` false si `dateRupture` vide ou future.
8. `formValid()` false si `joursPris > joursAcquisAnnee` (UI-side gate).
9. POST → succès hydrate le résultat + snackbar + triggerRefresh().
10. POST → erreur backend snackbar message erreur.
11. Pré-fill IA : `salaireBrutMensuel` → `salaireMensuelBrutEur` + badge IA.
12. Pré-fill IA : `dateLicenciement` → `dateRupture` + badge IA.
13. `onSalaireChange()` efface `provenanceSalaire`.
14. `onDateRuptureChange()` efface `provenanceDateRupture`.
15. `coherenceAlerts` : alerte SALAIRE_MENSUEL si divergence > 10 %.
16. `coherenceAlerts` : alerte DATE_RUPTURE si `aiData.dateLicenciement !== saisie`.
17. `coherenceAlerts` retourne `{}` en mode résultat (anti-bug SF-IA-03-12).
18. Méthode forcée DIX_POURCENT envoyée dans le POST.
19. Méthode forcée MAINTIEN envoyée dans le POST.
20. Méthode forcée Auto (null) envoyée dans le POST.
21. `editMode()` → repasse en formulaire.

### Isolation workspace

Non applicable ici — l'isolation est côté backend.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée, impact limité à son périmètre (nouveau composant + ajout TOOL_REGISTRY).

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — composant isolé visible uniquement en contexte FR + DROIT_DU_TRAVAIL.

---

## Dépendances

### Subfeatures bloquantes

- SF-DT-26-01 (backend) — en parallèle (contrat figé). Cette SF (frontend) peut être mergée indépendamment ; intégration runtime opérationnelle après merge des deux.
- SF-155-05 (CoherenceAlertBuilder) — statut : done.

### Questions ouvertes

Aucune.

---

## Notes et décisions

- **Comparateur 2 méthodes** : différenciateur produit par rapport à un simple calculateur. Affiche les 2 montants côte-à-côte pour transparence — l'avocat voit explicitement quelle méthode l'IA / le moteur a retenue et peut surcharger via le radio.
- **Pas de rouge dans la palette** : navy/or standard. Le résultat n'est pas une urgence temporelle.
- **Pas de pré-fill pour `totalRemunerationPeriodeEur`, `joursAcquisAnnee`, `joursPris`** : ces champs ne sont pas extraits par le prompt IA travail actuel. L'avocat les saisit manuellement.
- **Alertes F-IA-03** : limitées aux 2 fields pré-remplis (SALAIRE_MENSUEL, DATE_RUPTURE).
- **TOOL_REGISTRY ordre** : ajouté après `F-DT-21-travail-dissimule` pour rester regroupé avec les outils FR + travail.
