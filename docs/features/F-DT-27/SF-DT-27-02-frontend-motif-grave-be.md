# Mini-spec — F-DT-27 / SF-DT-27-02 Frontend motif grave BE

## Identifiant

`F-DT-27 / SF-DT-27-02`

## Feature parente

`F-DT-27` — Licenciement pour motif grave BE (art. 35 Loi 03/07/1978)

## Statut

`ready`

## Date de création

2026-04-24

## Branche Git

`feat/SF-DT-27-02-frontend-motif-grave-be`

## Pattern de référence

- **Template canonique** : `harcelement-licenciement-nul-section` (F-DT-11-02), réf. skill `ai-skills/frontend-coherence-audit.md` §5.
- **Pattern IA** : `immigration-title-decision-section` (F-IM-05) pour `prefillFromAi()` + signals provenance + `coherenceAlerts`.
- **Pattern gate BE-only** : `annexe13-be-section` (F-IM-08-06) — bannière info si `workspaceCountry !== 'BELGIQUE'`, pas de masquage silencieux.
- **Helper partagé** : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05/06).

---

## Objectif

Livrer le composant Angular `<app-motif-grave-be-section>` qui consomme l'endpoint `POST/GET /api/v1/case-files/{id}/motif-grave-be` (déjà mergé SF-DT-27-01, PR #497) pour valider les deux délais de 3 jours ouvrables de l'art. 35 Loi 03/07/1978 (notification rupture + motifs recommandé) et afficher la conséquence indemnitaire (préavis + CCT 109 si invalide).

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre un dossier BE (DROIT_DU_TRAVAIL, workspace `country = 'BELGIQUE'`).
2. Le panel décisionnel F-IA-04 affiche l'outil `F-DT-27-motif-grave-be` (règle `ALWAYS_ON` + `BELGIQUE` migrée en 115).
3. Le composant collapsible affiche en header `MOTIF GRAVE BE (ART. 35 LOI 03/07/1978)` + chip statut (`Valable` navy / `Invalide` or) si résultat persisté.
4. À l'ouverture (dépli) : GET → si 200 résultat précédent, affichage du mode résultat ; si 404, formulaire + pré-fill IA depuis `synthesis.travailExtractedData`.
5. Formulaire :
   - `dateConnaissanceFait` (`<input type="date">`, obligatoire, ≤ aujourd'hui)
   - `dateNotificationRupture` (`<input type="date">`, obligatoire, ≥ `dateConnaissanceFait`, ≤ aujourd'hui) — pré-rempli depuis `aiData.dateLicenciement` si disponible
   - `dateNotificationMotifs` (`<input type="date">`, obligatoire, ≥ `dateNotificationRupture`, ≤ aujourd'hui)
   - `anciennetteAnnees` (`<input type="number">`, entier ≥ 0, obligatoire)
   - `salaireMensuelReference` (`<input type="number">`, > 0, obligatoire) — pré-rempli depuis `aiData.salaireBrutMensuel` si disponible
6. À la soumission (POST) : appel service, puis affichage mode résultat + `MatSnackBar` succès + `CaseDashboardRefreshService.triggerRefresh()`.
7. Mode résultat :
   - Bandeau statut (navy "Motif grave procéduralement valide" ou or "Motif grave invalide" — **pas de rouge**, motif grave est une qualification juridique pas une urgence temporelle).
   - Breakdown (délais en jours ouvrables, indemnité préavis, fourchette CCT 109 min/max).
   - `formule` en JetBrains Mono, `baseJuridique` en JetBrains Mono italique.
   - `messages[]` rendus via `LegalCitationsPipe`.
   - Bouton "Modifier" pour ré-éditer.

### Gate pays (pattern annexe13-be-section)

- Si `workspaceCountry !== 'BELGIQUE'` → **bannière info** navy (`info_outline`) : "Cet outil s'applique à la Belgique uniquement. Pour la France, voir F-DT-08 (validité disciplinaire) et F-DT-01 (licenciement simple)." Pas d'appel HTTP, pas de form.
- Si `workspaceCountry === 'BELGIQUE'` → formulaire + charge GET.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| GET 404 | Mode formulaire + pré-fill IA |
| GET autre erreur | `MatSnackBar` "Impossible de charger l'analyse" + reste en mode formulaire vide |
| POST 400 (ex. `dateNotificationRupture < dateConnaissanceFait` côté backend) | `MatSnackBar` avec message backend |
| POST 403/404 | `MatSnackBar` générique |
| Backend down | `MatSnackBar` "Erreur lors de l'analyse" |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- **Autres outils métier** : tous les outils décisionnels frontend livrés 2026-04-24 (harcelement, inaptitude, heures-sup, annexe13-be, oqtf-sans-delai, oqtf-avec-delai, immigration-title-decision, etc.). Cet outil réutilise les mêmes patterns : pas de pattern nouveau.
- **Autres pays** : FR non applicable (backend 400 explicite renvoyant vers F-DT-08 / F-DT-01). BE only — gate UI bannière info symétrique à annexe13-be-section.
- **Autres domaines** : DROIT_DU_TRAVAIL BELGIQUE uniquement. Immigration / Famille non applicables.
- **Autres UI patterns** : aucune introduction de nouveau pattern shared. Réutilise `CoherencePopoverTriggerDirective`, `CoherenceAlertBuilder`, `LegalCitationsPipe`, `DecisionalHeaderFlagComponent` (optionnel), `CaseDashboardRefreshService`.
- **Auth / workspace / plans / navigation** : aucun impact — composant isolé.

### Niveaux de vérification

- **Modèle TypeScript** : `motif-grave-be.model.ts` (Request / Response) calqué sur `MotifGraveBeRequest` / `MotifGraveBeResponse` backend.
- **Record / DTO backend** : `MotifGraveBeResponse` (15 champs). Tous mappés côté frontend.
- **Service / logique métier** : `MotifGraveBeService` côté frontend = wrapper HttpClient POST/GET. Pas de logique additionnelle.
- **Entité JPA + schéma DB** : aucune modification (backend déjà mergé).
- **Tests existants** : backend 17 UT + 19 utility + 10 IT verts. Frontend à créer (15+ tests).

### Cas spécifique : outil décisionnel

- **Cohérence IA (F-IA-03)** : alerte de cohérence **`dateRuptureNotification`** → divergence `aiData.dateLicenciement` vs saisie avocat (WARNING). Les autres champs (`dateConnaissanceFait`, `dateNotificationMotifs`, `anciennetteAnnees`) ne sont pas extraits par l'IA actuelle — pas d'alerte pour eux. Alerte **`salaireReference`** → divergence relative > 10 % entre `aiData.salaireBrutMensuel` et saisie.
- **Refresh dashboard (F-IA-02)** : `triggerRefresh()` appelé après POST succès.
- **Pré-remplissage IA** : `prefillFromAi()` branche `aiData.dateLicenciement` → `dateNotificationRupture` + `aiData.salaireBrutMensuel` → `salaireMensuelReference`. Badges "Pré-rempli depuis l'analyse" avec signal provenance effacé au `onXxxChange()`.
- **Persistance des inputs** : tous les inputs persistés côté backend (colonnes dédiées) — confirmé par `MotifGraveBeAnalysis`. OK au reload.
- **Masquage conditionnel** : orchestré par le panel F-IA-04 via `TOOL_REGISTRY` + règle visibility `ALWAYS_ON` BELGIQUE.
- **Alertes actives après calcul** : `coherenceAlerts = computed(() => { if (!this.showForm()) return {}; ... })` — gate correct (pattern canonique).

### Cas spécifique : nouveau pattern UI / service partagé

Aucun. Le composant réutilise intégralement les patterns établis (CoherenceAlertBuilder, CoherencePopoverTriggerDirective, LegalCitationsPipe).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pattern canonique (harcelement-licenciement-nul-section) | Oui | Intégré — structure .ts / .html / .scss / .spec.ts calquée |
| Pattern IA (immigration-title-decision-section) | Oui | `prefillFromAi()` + signals provenance + `coherenceAlerts` + handlers onXxxChange |
| Pattern gate BE-only (annexe13-be-section) | Oui | Bannière info si workspaceCountry !== 'BELGIQUE' |
| CoherenceAlertBuilder / CoherenceAlert<F> partagés (SF-155-05) | Oui | Utilisé pour les 2 alertes F-IA-03 |
| LegalCitationsPipe (SF-155-01) | Oui | Utilisé pour `baseJuridique` + `messages[]` |
| Autres domaines (DROIT_FAMILLE / DROIT_IMMIGRATION) | Non | Motif grave est un concept BE spécifique au droit du travail |
| Autres pays (FR) | Non | Backend rejette FR avec 400 explicite — F-DT-08 / F-DT-01 prennent le relais |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [ ] Subfeature(s) parallèle(s) — aucune
- [ ] Backlog — aucun
- [x] Non applicable aux autres cibles (FR, DROIT_FAMILLE, DROIT_IMMIGRATION) — justifié

---

## Impact par domaine métier

- **DROIT_DU_TRAVAIL BELGIQUE uniquement** — le motif grave est un concept spécifique au droit du travail belge (art. 35 Loi 03/07/1978).
- **DROIT_DU_TRAVAIL FRANCE** : non applicable. L'équivalent FR (faute grave disciplinaire) est géré par F-DT-08 (validité licenciement disciplinaire) et F-DT-01 (licenciement simple). Le backend renvoie 400 sur dossiers FR.
- **DROIT_IMMIGRATION / DROIT_FAMILLE** : non applicable. Pas de concept équivalent.

---

## Parité des domaines métier

Non applicable — outil de **niveau 4 (arbre décisionnel)**, pas niveau ≥ 5 (scoring/comparateur/événement). La règle de parité concerne les outils de niveau ≥ 5.

Note : le backend (SF-DT-27-01) a été classé comme outil validation délai + calcul indemnitaire (hybride niveau 3/4). Aucune feature jumelle requise.

---

## Critères d'acceptation

- [x] Composant Angular standalone `<app-motif-grave-be-section>` créé avec inputs `caseFileId` (required), `workspaceCountry`, `aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`.
- [x] Gate pays : si `workspaceCountry !== 'BELGIQUE'` → bannière info navy, pas d'appel HTTP.
- [x] GET `/api/v1/case-files/{id}/motif-grave-be` au `ngOnInit` si BELGIQUE. 200 → mode résultat ; 404 → mode formulaire + pré-fill IA.
- [x] Pré-fill IA : `aiData.dateLicenciement` → `dateNotificationRupture` + `aiData.salaireBrutMensuel` → `salaireMensuelReference`. Badge "Pré-rempli depuis l'analyse" sous chaque field pré-rempli. Signal `provenance<Field>` effacé au change manuel.
- [x] `coherenceAlerts` computed retourne `{}` quand `!showForm()`. Alertes F-IA-03 : `DATE_RUPTURE` (divergence aiData.dateLicenciement vs saisie) + `SALAIRE` (divergence relative > 10 %). Construites via `CoherenceAlertBuilder`.
- [x] Formulaire valide uniquement si toutes dates présentes + `dateNotificationRupture >= dateConnaissanceFait` + `dateNotificationMotifs >= dateNotificationRupture` + dates <= aujourd'hui + ancienneté entier ≥ 0 + salaire > 0.
- [x] POST `/api/v1/case-files/{id}/motif-grave-be` à la soumission. Succès → résultat persisté affiché + `MatSnackBar` + `CaseDashboardRefreshService.triggerRefresh()`.
- [x] Mode résultat : bandeau navy si `motifGraveProceduralementValide=true`, or si `=false`. **Pas de rouge** (motif grave = qualification juridique, pas urgence).
- [x] Typographie : `baseJuridique`, `formule`, `delaiRuptureJoursOuvrables`, `delaiMotifsJoursOuvrables`, montants indemnités en JetBrains Mono. Labels, titres, messages en Inter.
- [x] `LegalCitationsPipe` appliqué sur `baseJuridique` + `messages[]` pour rendre `art. 35`, `CCT 109`, `loi 26/12/2013` en JetBrains Mono inline.
- [x] Erreurs HTTP : `MatSnackBar` (jamais `alert/confirm`).
- [x] Entrée `TOOL_REGISTRY` pour `F-DT-27-motif-grave-be` ajoutée dans `decisional-tools-panel.component.ts` — inputs canoniques : `caseFileId`, `workspaceCountry`, `aiData: synthesis?.travailExtractedData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`.
- [x] Couverture tests ≥ 15 (cf. plan de test).
- [x] `tsc --noEmit` vert. Tests Jest ciblés verts (`motif-grave-be-section` + `decisional-tools-panel`).

---

## Périmètre

### Hors scope (explicite)

- **Backend** : déjà mergé (SF-DT-27-01, PR #497). Aucune modification.
- **Extraction IA de `dateMotifsNotification` (2e recommandé)** : champ non extrait actuellement dans `TravailExtractedData`. Skipé gracefully — à envisager en SF ultérieure backend si besoin (extraction IA enrichie).
- **Extraction IA de `dateConnaissanceFait`** : pas d'équivalent direct. L'avocat saisit manuellement.
- **F-DT-08 / F-DT-01** (équivalents FR) : hors scope — déjà couverts.
- **Templates PDF / exports** : hors scope — pas demandé en V7.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `collapsed` | `true` | Replié par défaut (pattern canonique) |
| `showForm` | `true` | Form affiché tant que résultat non chargé |
| `provenance<Field>` | `null` | Badge IA effacé au démarrage ; set à 'IA' par prefillFromAi |

---

## Contraintes de validation

| Champ | Obligatoire | Format | Normalisation |
|-------|-------------|--------|---------------|
| dateConnaissanceFait | Oui | YYYY-MM-DD, ≤ aujourd'hui | - |
| dateNotificationRupture | Oui | YYYY-MM-DD, ≥ dateConnaissanceFait, ≤ aujourd'hui | - |
| dateNotificationMotifs | Oui | YYYY-MM-DD, ≥ dateNotificationRupture, ≤ aujourd'hui | - |
| anciennetteAnnees | Oui | Integer ≥ 0 | - |
| salaireMensuelReference | Oui | > 0 | - |

---

## Technique

### Endpoint(s) consommés

| Méthode | URL | Backend SF |
|---------|-----|------------|
| POST | `/api/v1/case-files/{caseFileId}/motif-grave-be` | SF-DT-27-01 |
| GET | `/api/v1/case-files/{caseFileId}/motif-grave-be` | SF-DT-27-01 |

### Composants Angular créés

- `motif-grave-be.model.ts` — types `MotifGraveBeRequest`, `MotifGraveBeResponse`.
- `motif-grave-be.service.ts` — `MotifGraveBeService` HttpClient wrapper.
- `motif-grave-be-section.component.{ts,html,scss,spec.ts}` — composant section intégré au panel F-IA-04.

### Modifications

- `decisional-tools-panel.component.ts` — ajout entrée `F-DT-27-motif-grave-be` dans `TOOL_REGISTRY`.

---

## Plan de test (≥ 15)

### Tests unitaires composant (Jest)

1. BELGIQUE → GET appelé au ngOnInit.
2. FRANCE → aucun appel HTTP + bannière info rendue.
3. Charge l'analyse existante si GET 200 (mode résultat, valeurs hydratées).
4. Reste en formulaire si GET 404.
5. `formValid()` false si date manquante, négative ou future.
6. `formValid()` false si `dateNotificationRupture < dateConnaissanceFait` (UI-side gate).
7. `formValid()` false si `dateNotificationMotifs < dateNotificationRupture`.
8. `formValid()` false si ancienneté négative ou salaire ≤ 0.
9. POST → succès hydrate le résultat + snackbar + triggerRefresh().
10. POST → erreur backend snackbar message erreur.
11. Pré-fill IA : `dateLicenciement` → `dateNotificationRupture` + badge IA.
12. Pré-fill IA : `salaireBrutMensuel` → `salaireMensuelReference` + badge IA.
13. `onDateRuptureChange()` efface `provenanceDateRupture`.
14. `onSalaireChange()` efface `provenanceSalaire`.
15. `coherenceAlerts` : alerte DATE_RUPTURE si `aiData.dateLicenciement !== saisie`.
16. `coherenceAlerts` : alerte SALAIRE si divergence > 10 %.
17. `coherenceAlerts` retourne `{}` en mode résultat.
18. Résultat valide (ruptureOk + motifsOk) → bandeau navy "valide".
19. Résultat invalide → bandeau or "invalide" + fourchette CCT 109 affichée.
20. `editMode()` → repasse en formulaire.

### Tests TOOL_REGISTRY (decisional-tools-panel)

21. `TOOL_REGISTRY.get('F-DT-27-motif-grave-be')` retourne `MotifGraveBeSectionComponent` avec inputs canoniques.

### Isolation workspace

Non applicable ici — l'isolation est côté backend. Côté frontend, le composant consomme l'endpoint qui gère l'isolation (cf. service SF-DT-27-01).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée, impact limité à son périmètre (nouveau composant + ajout TOOL_REGISTRY).

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — justification : composant isolé visible uniquement en contexte BE + DROIT_DU_TRAVAIL. Pas d'impact sur auth, navigation ou workspace.

---

## Dépendances

### Subfeatures bloquantes

- SF-DT-27-01 — statut : done (PR #497 mergée 2026-04-24).
- SF-155-05 (CoherenceAlertBuilder) — statut : done.

### Questions ouvertes

Aucune.

---

## Notes et décisions

- **Pas de rouge** dans la palette : même si l'analyse conclut à une invalidité procédurale, cela relève d'une qualification juridique (conséquence indemnitaire), pas d'une urgence temporelle critique. Palette navy (valide) / or (invalide), pattern cohérent avec licenciement-section FR.
- **Pas de pré-fill pour `dateConnaissanceFait` et `dateNotificationMotifs`** : ces champs ne sont pas extraits par le prompt IA travail actuel. L'avocat les saisit manuellement. `motifLicenciement` (enum vague côté IA) n'a pas d'équivalent enum backend ici — pas de pré-fill pour éviter une dette de mapping incertaine.
- **Alertes F-IA-03** : limitées à 2 fields (`DATE_RUPTURE`, `SALAIRE`) correspondant aux 2 seuls fields pré-remplis depuis `aiData`. Les 3 autres champs du form n'ont pas de source IA → pas d'alerte possible à ce jour.
- **TOOL_REGISTRY ordre** : placée après `F-IM-08-annexe13-be` (dernière entrée au HEAD), pour rester regroupée avec les outils BE. Priority 56 déjà définie côté DB (migration 115).
