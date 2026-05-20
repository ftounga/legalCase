# Mini-spec — F-217 / SF-217-13 — Frontend : sections décisionnelles Vague 3 successions BE (dévolution-réserve + acceptation-renonciation)

## Identifiant
`F-217 / SF-217-13`

## Feature parente
`F-217` — P2 Famille BE — outils décisionnels de fréquence haute (Vague 3 — Successions / protection / international)

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-217-13-frontend-successions-be-bundle`

## Contrat API
Importé de **SF-217-11-backend** (`succession-be-devolution-reserve`, POST/GET figés)
et **SF-217-12-backend** (`succession-be-acceptation-renonciation`, POST/GET figés).
Tests Jest sur **mock des services** — pas de dépendance au backend mergé.

---

## Objectif

Ajouter dans le panel des outils décisionnels du dossier les 2 sections successions BE
de la Vague 3 — « Dévolution et réserve (Belgique) » et « Acceptation / renonciation
(Belgique) » — chacune étant un formulaire de saisie qui appelle son endpoint et affiche
un verdict décisionnel, et seeder leur visibilité dans `decision_tool_visibility_rules`.

---

## Comportement attendu

### Cas nominal

1. Sur un dossier Famille **BE**, les 2 sections apparaissent dans
   `app-decisional-tools-panel` en mode `ALWAYS_ON` (visibilité gérée par F-IA-04 —
   seed migration livré par cette SF).
2. **Dévolution et réserve** : l'avocat renseigne la date du décès, l'état civil du
   défunt, son régime matrimonial le cas échéant, le nombre d'enfants vivants /
   prédécédés avec descendants, la présence de parents / collatéraux, la masse
   successorale et les libéralités déjà consenties. Clic « Analyser » → `POST` →
   affichage du verdict (`DEVOLUTION_ETABLIE` / `RESERVE_RESPECTEE` /
   `QUOTITE_DEPASSEE` / `QUALIFICATION_INCOMPLETE`), de la liste des héritiers avec leurs
   quotes-parts (fraction + euros), de la réserve globale, de la quotité disponible
   et des bases juridiques.
3. **Acceptation / renonciation** : l'avocat renseigne la date du décès, la qualité
   d'héritier, l'état du patrimoine (`SOLVABLE` / `DOUTEUX` / `INSOLVABLE` / `INCONNU`),
   les actes éventuellement déjà accomplis (liste à cocher), la volonté du client et
   l'éventuelle mise en demeure d'un créancier. Clic « Analyser » → `POST` → affichage
   du verdict (7 niveaux), de l'option recommandée, du délai (date limite + jours
   restants + statut), des risques avec sévérité, des actions concrètes à poser et des
   bases juridiques.
4. À la réouverture du dossier, le dernier résultat de chaque outil (`GET`) est
   rechargé ; l'avocat peut ré-éditer et recalculer (la Response ré-expose les inputs —
   leçon F-DT-36).
5. Après calcul, `CaseDashboardRefreshService.triggerRefresh()` est invoqué dans le
   `next:` du POST.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Échec `GET` au chargement | Section en formulaire vierge, pas de crash |
| Échec `POST` (400 / 422 / 4xx) | `MatSnackBar` avec le message backend, le formulaire reste éditable |
| Composant monté sur un workspace FR | Bannière info explicite « Outil propre au droit belge » — pas de masquage silencieux, pas d'appel `POST` |

---

## Analyse de cohérence transversale

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Outils décisionnels successions BE | Oui | Aucun outil BE équivalent (F-FA-24-* sont FR-only). Les 2 outils sont des situations distinctes (quantification dévolution+réserve vs arbre options post-décès). Cf. `SF-217-00-coherence.md` |
| Outils Famille BE F-211 (`pacte-successoral-be-2018`) | Oui | Outil distinct (pacte anticipé pré-décès) — `pacte-successoral-be-2018` couvre le pacte du vivant, les 2 outils SF-217-11/12 couvrent la phase post-décès. Aucune duplication |
| Pré-fill IA | Oui | Aucun flag pivot dédié extrait par le pipeline V1 pour les successions BE (date du décès, masse, options) → `getPrefillCount()` = 0, `PREFILL_COUNT_ALWAYS_ZERO = true` documenté |
| F-IA-03 cohérence | Oui | Câblage `coherenceAlerts` sur les champs croisables : `dateDeces` (potentiellement extractible à terme) ; en V1 aucun champ croisable confirmé → `coherenceAlerts` retourne `{}` mais le câblage est présent |
| Refresh dashboard F-IA-02 | Oui | `triggerRefresh()` dans le `next:` du POST des 2 sections |
| Gate `workspaceCountry` | Oui | Bannière info si workspace FR sur les 2 sections — outils BE-only |

### Décision
- [x] Étendu à toutes les cibles applicables dans cette SF.

---

## Conformité F-IA-04

SF frontend décisionnelle livrant 2 sections — les 5 blocs s'appliquent aux deux.

### 1. Cohérence visuelle
- [x] Palette navy/or pour info, vert pour OK, rouge **réservé** aux verdicts critiques
  (`QUOTITE_DEPASSEE`, `ACCEPTATION_TACITE_PROBABLE`, `DELAI_CRITIQUE`, `DELAI_DEPASSE`)
  et au statut de délai `DEPASSE` / `CRITIQUE`. `QUALIFICATION_INCOMPLETE` = couleur
  navy avec icône info (état neutre).
- [x] `<input type="date">` pour les dates (`dateDeces`, `dateMiseEnDemeureCreancier`) —
  pas `MatDatepicker`.
- [x] `JetBrains Mono` pour `fondement` / `basesJuridiques` ; `Inter` pour le reste.
- [x] Gate `workspaceCountry` : les 2 outils sont BE-only → **bannière info** explicite
  si workspace FR (pas de masquage silencieux côté composant).
- [x] `MatSnackBar` pour les erreurs — pas d'`alert()` / `confirm()`.
- [x] `CaseDashboardRefreshService.triggerRefresh()` dans le `next:` du POST des 2
  sections.

### 2. Pré-fill IA
- [x] `@Input() aiData?: FamilleExtractedData` typé sur les 2 sections.
- [x] `prefillFromAi()` invoqué dans `ngOnInit()` ET `ngOnChanges()`.
- [x] V1 : aucun flag pivot dédié (date du décès, masse successorale, état du
  patrimoine, actes d'héritier) n'est extrait par le pipeline IA → aucun champ
  pré-rempli, `getPrefillCount()` retourne 0 pour les 2 outils.
  `PREFILL_COUNT_ALWAYS_ZERO = true` documenté (factuel — pas un oubli, cf. SF-211-05
  / SF-217-03 / SF-217-05). Extension IA = SF ultérieure si signal terrain (à brancher
  éventuellement à un `succession_be_detection` du pipeline V2).
- [x] **Aucun champ ajouté à `FamilleExtractedData`** dans cette SF — la liste reste
  identique à la Vague 2.

### 3. Validation F-IA-03
- [x] `coherenceAlerts = computed<Partial<Record<FieldName, CoherenceAlert>>>()` sur
  chaque section :
  - Dévolution-réserve : `dateDeces` (potentiellement extractible à terme — câblage
    présent mais alerte effective uniquement si pipeline V2 ajoute la détection).
  - Acceptation-renonciation : `dateDeces` (idem) ; les autres champs (état patrimoine,
    actes d'héritier) sont qualifiés à l'audience par l'avocat et ne sont pas croisables.
- [x] Hiérarchie de sources respectée : **F-96 > Question IA > IA détection > Pièce
  manquante** ; source `'MULTI'` en cas de convergence.
- [x] `<app-coherence-popover-trigger>` câblé sur les champs concernés (même si en V1
  l'alerte est rarement produite faute de signal IA).
- [x] Helper partagé `CoherenceAlertBuilder` —
  pas de définition locale ad hoc.
- [x] Le gate du `coherenceAlerts` computed n'inclut que `!this.showForm()` (pas
  `|| this.result()` — bug SF-IA-03-12).

### 4. TOOL_REGISTRY symétrique + `getPrefillCount()`
- [x] 2 entrées ajoutées dans `TOOL_REGISTRY`
  (`frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts`) :
  - `succession-be-devolution-reserve` →
    `SuccessionBeDevolutionReserveSectionComponent`, `displayLabel` = « Dévolution et
    réserve héréditaire (Belgique) ».
  - `succession-be-acceptation-renonciation` →
    `SuccessionBeAcceptationRenonciationSectionComponent`, `displayLabel` =
    « Acceptation / renonciation à succession (Belgique) ».
  - `inputs: (ctx) => ({ caseFileId, workspaceCountry, aiData:
    ctx.synthesis?.familleExtractedData, procedureChecks, aiQuestions, piecesManquantes:
    ctx.synthesis?.piecesManquantesDetails, standaloneMode: ctx.standaloneMode ?? false })`
    — symétrique aux entrées Vague 1+2 (`regime-mat-be-communaute-legale`, etc.).
- [x] 2 mappings `THEME_BY_TOOL_ID` :
  - `succession-be-devolution-reserve` → `VALIDITE` (quantification / qualification
    de la dévolution).
  - `succession-be-acceptation-renonciation` → `DELAIS` (outil à délais — délai 4 mois
    impératif, jours restants, verdict de criticité).
- [x] `static getPrefillCount()` retourne 0 sur les 2 composants — parité stricte avec
  `prefillFromAi()` (V1 = 0 champ). `PREFILL_COUNT_ALWAYS_ZERO = true` exposé → branche
  d'exemption du test `prefill-count-integrity.spec.ts`.
- [x] Les 2 `tool_id` exposés (`succession-be-devolution-reserve`,
  `succession-be-acceptation-renonciation`) sont couverts par le seed
  `decision_tool_visibility_rules` de cette SF, et `DecisionToolVisibilityIntegrityIT`
  (extraction dynamique du `TOOL_REGISTRY`) doit passer : seed + entrées frontend
  livrés **ensemble dans cette SF** (cf. `feedback_pre_merge_visibility_seed_check`,
  précédent SF-211-05 / SF-DT-36-02 / SF-217-03).
- [x] `DecisionToolDisplayLabelIntegrityIT` : 2 `displayLabel` humains non vides, non
  auto-référençants.

### 5. Parité des domaines (niveau ≥ 5)
- [x] Niveau des 2 tools : **5** (quantification dévolution + analyse de validité de
  la réserve ; arbre décisionnel avec délais critiques et risques).
- Parité par domaine :

| Domaine | Équivalent existe ? | Action |
|---------|---------------------|--------|
| Famille | C'est F-217 lui-même (le périmètre est BE-only par construction — cf. `feedback_belgique_never_forget`) | — |
| Droit du travail | Non | Non pertinent — successions = droit de la famille. |
| Immigration | Non | Non pertinent — successions = droit de la famille. |

> Note parité FR/BE : il n'existe **pas** d'attente de parité FR pour F-217. Le
> périmètre est BE-only assumé — l'objectif est de combler le déséquilibre FR/BE
> dénoncé par `feedback_belgique_never_forget` ; les outils FR (F-FA-24-*) reposent
> sur des mécanismes juridiques distincts (réserve 1/2-2/3-3/4 vs 1/2 fixe BE) et
> ne sont pas réutilisés.

---

## Critères d'acceptation

- [ ] Les 2 sections apparaissent dans `app-decisional-tools-panel` sur un dossier
      Famille BE (mode `ALWAYS_ON`).
- [ ] Sur un workspace FR, aucune des 2 sections n'apparaît (gate `workspaceCountry`
      côté visibilité) ; si un composant est néanmoins monté, il affiche une bannière
      info et n'appelle pas `POST`.
- [ ] Section dévolution-réserve : formulaire complet ; « Analyser » → `POST` →
      verdict + liste des héritiers (fraction + €) + réserve globale + quotité
      disponible + libéralités consenties + dépassement éventuel + bases juridiques.
- [ ] Section acceptation-renonciation : formulaire complet ; « Analyser » → `POST` →
      verdict + option recommandée + date limite + jours restants + délai statut
      coloré + risques avec sévérité + actions concrètes + bases juridiques.
- [ ] Le formulaire de chaque section couvre les champs du contrat API SF-217-11 /
      SF-217-12.
- [ ] Le dernier résultat de chaque outil est rechargé à l'ouverture (`GET`) et le
      formulaire est ré-éditable (inputs ré-exposés par la Response).
- [ ] Erreur backend → `MatSnackBar`, formulaire conservé.
- [ ] `triggerRefresh()` appelé après chaque calcul.
- [ ] Migration `273-seed-f217-vague3-successions-visibility-rules.xml` ajoute 2 INSERT
      dans `decision_tool_visibility_rules` (`ALWAYS_ON`, `DROIT_FAMILLE` / `BELGIQUE`).
- [ ] `getPrefillCount()` retourne 0 pour les 2 outils.
- [ ] Tests Jest verts (services mockés). `DecisionToolVisibilityIntegrityIT`,
      `DecisionToolDisplayLabelIntegrityIT`, `prefill-count-integrity.spec.ts` passent.

---

## Périmètre

### Hors scope
- Endpoints backend (SF-217-11 / SF-217-12).
- Pré-fill IA effectif (V1 = 0 champ pour les 2 outils).
- Les 6 autres SF de la Vague 3 F-217 (SF-217-14 à 19 — protection majeur, mariage
  étranger, contestation filiation).
- Génération de la déclaration d'option (acte) — non couvert en V1.
- Génération de l'acte de notoriété — non couvert.

---

## Technique

### Composants Angular
- `SuccessionBeDevolutionReserveSectionComponent`
  (`frontend/src/app/case-files/succession-be-devolution-reserve-section/`) —
  standalone, OnPush, signals.
- `SuccessionBeAcceptationRenonciationSectionComponent`
  (`frontend/src/app/case-files/succession-be-acceptation-renonciation-section/`) —
  standalone, OnPush, signals.
- `SuccessionBeDevolutionReserveService` et
  `SuccessionBeAcceptationRenonciationService` (`core/services/`) — `calculate()`,
  `get()`.
- `succession-be-devolution-reserve-section-prefill-rules.ts` et
  `succession-be-acceptation-renonciation-section-prefill-rules.ts` — helpers
  `getPrefillCount()` (return 0).
- 2 entrées `TOOL_REGISTRY` + 2 entrées `THEME_BY_TOOL_ID` dans
  `decisional-tools-panel.component.ts`.
- 2 modèles `core/models/succession-be-devolution-reserve.model.ts` et
  `core/models/succession-be-acceptation-renonciation.model.ts`.

### Migration Liquibase
- [x] Oui — `273-seed-f217-vague3-successions-visibility-rules.xml` — 2 INSERT dans
  `decision_tool_visibility_rules` :
  - `succession-be-devolution-reserve` : `layer = ALWAYS_ON`, `legal_domain =
    DROIT_FAMILLE`, `country = BELGIQUE`, `trigger_field = NULL`, `trigger_value = NULL` —
    toute analyse de dossier successoral belge appelle la qualification de la dévolution
    et de la réserve (cf. `SF-217-00-coherence.md` : situation toujours pertinente).
  - `succession-be-acceptation-renonciation` : `layer = ALWAYS_ON`, `legal_domain =
    DROIT_FAMILLE`, `country = BELGIQUE`, `trigger_field = NULL`, `trigger_value = NULL` —
    confirmé `ALWAYS_ON` : tout héritier appelé doit opter dans les délais ; l'outil
    est pertinent dès qu'un dossier successoral BE est ouvert.
  - Numéro `273` = prochain libre après `272` (SF-217-12). À renuméroter si conflit
    au merge. UUID namespace dédié F-217 Vague 3 : `f1a04003-0000-0000-0000-eeee21703XXX`.
  - Rollback : `DELETE FROM decision_tool_visibility_rules WHERE id LIKE
    'f1a04003-0000-0000-0000-eeee21703%'`.

---

## Plan de test

### Tests unitaires (Jest, services mockés)
- [ ] Chargement : rechargement du dernier résultat via `GET` pour chaque section.
- [ ] Dévolution-réserve : `POST` appelé avec le bon payload ; affichage des héritiers
      + quotient + réserve.
- [ ] Acceptation-renonciation : `POST` appelé avec le bon payload ; affichage du
      verdict, de l'option recommandée, des risques colorés par sévérité.
- [ ] Affichage des verdicts (couleur correcte ; rouge réservé à `QUOTITE_DEPASSEE`,
      `ACCEPTATION_TACITE_PROBABLE`, `DELAI_CRITIQUE`, `DELAI_DEPASSE`).
- [ ] Erreur `POST` → `MatSnackBar`, formulaire conservé.
- [ ] Bannière info si workspace FR ; pas d'appel `POST` ni `GET`.
- [ ] `getPrefillCount()` → 0 pour les 2 composants.
- [ ] `coherenceAlerts` : pas d'alerte en `standaloneMode`.

### Isolation workspace
- [x] Non applicable — garantie côté backend (SF-217-11 / SF-217-12).

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Outil décisionnel métier** — 2 nouvelles sections décisionnelles. Scan fait
  (cf. analyse de cohérence transversale + `SF-217-00-coherence.md`). Un outil = une
  situation : quantification dévolution+réserve ≠ arbre options post-décès. Aucune
  duplication avec `pacte-successoral-be-2018` (pacte pré-décès).
- [x] Aucune autre préoccupation transversale (auth/workspace/plans/navigation non
  modifiés — ajout de sections dans un panel existant).

### Smoke tests E2E
- [x] Aucun concerné — feature additive (sections dans un panel existant, pas de
  modification de route / guard / auth).

---

## Dépendances
- `SF-217-11` et `SF-217-12` — contrats API figés (importés). Dev frontend
  parallélisable (tests sur mocks de service). Merge frontend **après** les 2 backends
  (cf. `feedback_pre_merge_endpoint_check` — vérifier que les endpoints
  `succession-be-devolution-reserve` et `succession-be-acceptation-renonciation`
  existent côté backend avant `gh pr merge`).

---

## Notes et décisions
- `getPrefillCount() = 0` en V1 assumé pour les 2 outils : le pipeline IA n'extrait
  pas de flags dédiés aux successions BE en V1. `PREFILL_COUNT_ALWAYS_ZERO = true`
  documenté pour le test d'intégrité — factuel, pas une dette masquée (cf.
  `SF-217-00-coherence.md` ajustement n° 2). **Aucun nouveau champ ajouté à
  `FamilleExtractedData`** dans cette SF — l'extension IA sera tranchée à une SF
  ultérieure (potentiel `succession_be_detection` pipeline V2).
- Visibilité `ALWAYS_ON` confirmée pour les 2 outils : toute analyse d'un dossier
  successoral belge mobilise la qualification de la dévolution et l'arbitrage
  acceptation/renonciation — ce sont des situations *toujours pertinentes*, pas des
  situations à détecter (cf. `SF-217-00-coherence.md` ajustement n° 1).
- OnPush + signals : `markForCheck()` (via `ChangeDetectorRef`) dans le `next:` /
  `error:` des `subscribe()` (cf. `feedback_onpush_subscribe_markforcheck`).
- Le seed `decision_tool_visibility_rules` est livré dans cette SF frontend (couplé
  aux entrées `TOOL_REGISTRY`) — un seed sans entrée frontend ferait échouer
  `DecisionToolVisibilityIntegrityIT` ; une entrée frontend sans seed rendrait les
  outils silencieusement invisibles. Les deux vont ensemble (précédent SF-211-05 /
  SF-DT-36-02 / SF-217-03).
- Bundle des 2 sections successions dans **une seule SF frontend** (pattern SF-217-03
  qui bundle régime + liquidation) : cohérence visuelle « bloc successions », unique
  migration de seed, unique passage en review — réduction du coût de merge.
