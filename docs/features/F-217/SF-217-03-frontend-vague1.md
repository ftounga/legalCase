# Mini-spec — F-217 / SF-217-03 — Frontend : sections décisionnelles Vague 1 Famille BE (régime communauté légale + liquidation-partage)

## Identifiant
`F-217 / SF-217-03`

## Feature parente
`F-217` — P2 Famille BE — ~10 outils décisionnels de fréquence haute (Vague 1 — Patrimoine du couple)

## Statut
`ready`

## Date de création
2026-05-17

## Branche Git
`feat/SF-217-03-frontend-vague1`

## Contrat API
Importé de **SF-217-01-backend** (`regime-mat-be-communaute-legale`, POST/GET figés) et **SF-217-02-backend** (`liquidation-partage-be`, POST/GET figés). Tests Jest sur **mock des services** — pas de dépendance au backend mergé.

---

## Objectif

Ajouter dans le panel des outils décisionnels du dossier les 2 sections de la Vague 1 de F-217 — « Régime de communauté légale (Belgique) » et « Liquidation-partage post-divorce (Belgique) » — chacune étant un formulaire de saisie qui appelle son endpoint et affiche un verdict décisionnel, et seeder leur visibilité dans `decision_tool_visibility_rules`.

---

## Comportement attendu

### Cas nominal

1. Sur un dossier Famille **BE**, les 2 sections apparaissent dans `app-decisional-tools-panel` en mode `ALWAYS_ON` (visibilité gérée par F-IA-04 — seed migration 235 livré par cette SF).
2. **Régime de communauté légale** : l'avocat renseigne la date de mariage, la présence d'un contrat de mariage, et ajoute les biens / dettes via des listes dynamiques (ajout / suppression de lignes, chaque ligne portant ses critères de qualification). Clic « Analyser » → `POST` → affichage du verdict (`COMMUNAUTE_LEGALE_APPLICABLE` / `REGIME_CONVENTIONNEL_DETECTE` / `QUALIFICATION_INCOMPLETE`), de la liste des biens qualifiés (qualification + mode de gestion + fondement + explication), des dettes qualifiées et de la synthèse de composition.
3. **Liquidation-partage** : l'avocat renseigne l'avancement des 8 étapes (cases + dates). Clic « Analyser » → `POST` → affichage du verdict (`PROCEDURE_NON_ENGAGEE` / `EN_COURS` / `DELAI_CONTREDITS_CRITIQUE` / `EN_ATTENTE_HOMOLOGATION` / `CLOTUREE`), de la checklist d'étapes (statut + fondement), des délais (échéance + jours restants + statut) et de la prochaine étape.
4. À la réouverture du dossier, le dernier résultat de chaque outil (`GET`) est rechargé ; l'avocat peut ré-éditer et recalculer (la Response ré-expose les inputs — leçon F-DT-36).
5. Après calcul, `CaseDashboardRefreshService.triggerRefresh()` est invoqué dans le `next:` du POST.

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
| Outils décisionnels régime matrimonial / patrimoine BE | Oui | Aucun outil BE équivalent — F-FA-15/16/17 sont FR-only. Les 2 outils sont des situations distinctes (composition du patrimoine vs procédure de liquidation). Cf. `SF-217-00-coherence.md` |
| Outils Famille BE F-211 (`divorce-dc-be`, etc.) | Oui | Pattern de section / wrapper aligné. Les 2 outils Vague 1 prolongent le pilier « Couple/patrimoine » après la dissolution couverte par F-211 |
| Pré-fill IA | Oui | Aucun flag pivot dédié extrait par le pipeline V1 pour le régime matrimonial / la procédure de liquidation → `getPrefillCount()` = 0, `PREFILL_COUNT_ALWAYS_ZERO = true` documenté. Extension IA = SF ultérieure si signal |
| F-IA-03 cohérence | Oui | Câblage `coherenceAlerts` sur les champs croisables : `dateMariage` (régime) et `dateNotificationProjet` / étapes (liquidation) vs F-96, questions IA, pièces manquantes |
| Refresh dashboard F-IA-02 | Oui | `triggerRefresh()` dans le `next:` du POST des 2 sections |
| Gate `workspaceCountry` | Oui | Bannière info si workspace FR sur les 2 sections — outils BE-only |

### Décision
- [x] Étendu à toutes les cibles applicables dans cette SF.

---

## Conformité F-IA-04

SF frontend décisionnelle livrant 2 sections — les 5 blocs s'appliquent aux deux.

### 1. Cohérence visuelle
- [x] Palette navy/or pour info, vert pour OK, rouge **réservé** aux verdicts critiques (`QUALIFICATION_INCOMPLETE`, `DELAI_CONTREDITS_CRITIQUE`) et au statut de délai `DEPASSE` / `CRITIQUE`.
- [x] `<input type="date">` pour les dates (`dateMariage`, dates de procédure) — pas `MatDatepicker`.
- [x] `JetBrains Mono` pour `fondement` / `basesJuridiques` ; `Inter` pour le reste.
- [x] Gate `workspaceCountry` : les 2 outils sont BE-only → **bannière info** explicite si workspace FR (pas de masquage silencieux côté composant).
- [x] `MatSnackBar` pour les erreurs — pas d'`alert()` / `confirm()`.
- [x] `CaseDashboardRefreshService.triggerRefresh()` dans le `next:` du POST des 2 sections.

### 2. Pré-fill IA
- [x] `@Input() aiData?: FamilleExtractedData` typé sur les 2 sections.
- [x] `prefillFromAi()` invoqué dans `ngOnInit()` ET `ngOnChanges()`.
- [x] V1 : aucun flag pivot dédié (composition du patrimoine, avancement de procédure de liquidation) n'est extrait par le pipeline IA → aucun champ pré-rempli, `getPrefillCount()` retourne 0 pour les 2 outils. `PREFILL_COUNT_ALWAYS_ZERO = true` documenté (factuel — pas un oubli, cf. SF-DT-36-02 / SF-211-05). Extension IA = SF ultérieure si signal.

### 3. Validation F-IA-03
- [x] `coherenceAlerts = computed<Partial<Record<FieldName, CoherenceAlert>>>()` sur chaque section :
  - Régime : `dateMariage` croisée avec `aiData` / questions IA / pièces (la date de mariage est souvent extraite par le pipeline famille).
  - Liquidation : `dateNotificationProjet` et les cases d'étape croisées avec `procedureChecks` (F-96), questions IA, pièces manquantes.
- [x] Hiérarchie de sources respectée : **F-96 > Question IA > IA détection > Pièce manquante** ; source `'MULTI'` en cas de convergence.
- [x] `<app-coherence-popover-trigger>` câblé sur les champs concernés.
- [x] Helper partagé `CoherenceAlertBuilder` (`frontend/src/app/shared/coherence-popover/coherence-alert-builder.ts`) — pas de définition locale ad hoc.
- [x] Le gate du `coherenceAlerts` computed n'inclut que `!this.showForm()` (pas `|| this.result()` — bug SF-IA-03-12).

### 4. TOOL_REGISTRY symétrique + `getPrefillCount()`
- [x] 2 entrées ajoutées dans `TOOL_REGISTRY` (`frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts`) :
  - `regime-mat-be-communaute-legale` → `RegimeCommunauteLegaleBeSectionComponent`, `displayLabel` = « Régime de communauté légale (Belgique) ».
  - `liquidation-partage-be` → `LiquidationPartageBeSectionComponent`, `displayLabel` = « Liquidation-partage post-divorce (Belgique) ».
  - `inputs: (ctx) => ({ caseFileId, workspaceCountry, aiData: ctx.synthesis?.familleExtractedData, procedureChecks, aiQuestions, piecesManquantes: ctx.synthesis?.piecesManquantesDetails, standaloneMode: ctx.standaloneMode ?? false })` — symétrique aux entrées `divorce-dc-be` et `F-DT-36-procedure-nullite-licenciement`.
- [x] 2 mappings `THEME_BY_TOOL_ID` : `regime-mat-be-communaute-legale` → `VALIDITE` (qualification de la composition) ; `liquidation-partage-be` → `DELAIS` (procédure à délais — cohérent avec `tribunal-famille-be-mesures-prov` → `DELAIS`).
- [x] `static getPrefillCount()` retourne 0 sur les 2 composants — parité stricte avec `prefillFromAi()` (V1 = 0 champ). `PREFILL_COUNT_ALWAYS_ZERO = true` exposé → branche d'exemption du test `prefill-count-integrity.spec.ts`.
- [x] Les 2 `tool_id` exposés (`regime-mat-be-communaute-legale`, `liquidation-partage-be`) sont couverts par le seed `decision_tool_visibility_rules` de cette SF, et `DecisionToolVisibilityIntegrityIT` (extraction dynamique du `TOOL_REGISTRY`) doit passer : seed + entrées frontend livrés **ensemble dans cette SF** (cf. `feedback_pre_merge_visibility_seed_check`, précédent SF-211-05 / SF-DT-36-02).
- [x] `DecisionToolDisplayLabelIntegrityIT` : 2 `displayLabel` humains non vides, non auto-référençants.

### 5. Parité des domaines (niveau ≥ 5)
- [x] Niveau des 2 tools : **5** (analyse de validité / verdict décisionnel — qualification du patrimoine ; positionnement sur séquence procédurale + verdict d'avancement).
- Parité par domaine :

| Domaine | Équivalent existe ? | Action |
|---------|---------------------|--------|
| Famille | C'est F-217 lui-même (le périmètre est BE-only par construction — cf. `feedback_belgique_never_forget`) | — |
| Droit du travail | Non | Non pertinent — le régime matrimonial et la liquidation-partage post-divorce sont propres au droit de la famille. |
| Immigration | Non | Non pertinent — concepts propres au droit de la famille. |

> Note parité FR/BE : il n'existe **pas** d'attente de parité FR pour F-217. Le périmètre est BE-only assumé — l'objectif est de combler le déséquilibre FR/BE dénoncé par `feedback_belgique_never_forget` ; les outils FR (F-FA-15/16/17) reposent sur des mécanismes juridiques distincts et ne sont pas réutilisés.

---

## Critères d'acceptation

- [ ] Les 2 sections apparaissent dans `app-decisional-tools-panel` sur un dossier Famille BE (mode `ALWAYS_ON`).
- [ ] Sur un workspace FR, aucune des 2 sections n'apparaît (gate `workspaceCountry` côté visibilité) ; si un composant est néanmoins monté, il affiche une bannière info et n'appelle pas `POST`.
- [ ] Section régime : listes dynamiques de biens / dettes (ajout / suppression de lignes) ; « Analyser » → `POST` → verdict + biens qualifiés + dettes qualifiées + synthèse de composition.
- [ ] Section liquidation : 8 étapes saisissables (cases + dates) ; « Analyser » → `POST` → verdict + checklist d'étapes + délais + prochaine étape.
- [ ] Le formulaire de chaque section couvre les champs du contrat API SF-217-01 / SF-217-02.
- [ ] Le dernier résultat de chaque outil est rechargé à l'ouverture (`GET`) et le formulaire est ré-éditable (inputs ré-exposés par la Response).
- [ ] Erreur backend → `MatSnackBar`, formulaire conservé.
- [ ] `triggerRefresh()` appelé après chaque calcul.
- [ ] Migration `235-seed-f217-vague1-visibility-rules.xml` ajoute 2 INSERT dans `decision_tool_visibility_rules` (`ALWAYS_ON`, `DROIT_FAMILLE` / `BELGIQUE`).
- [ ] `getPrefillCount()` retourne 0 pour les 2 outils.
- [ ] Tests Jest verts (services mockés). `DecisionToolVisibilityIntegrityIT`, `DecisionToolDisplayLabelIntegrityIT`, `prefill-count-integrity.spec.ts` passent.

---

## Périmètre

### Hors scope
- Endpoints backend (SF-217-01 / SF-217-02).
- Pré-fill IA effectif (V1 = 0 champ pour les 2 outils).
- Les 8 autres outils de F-217 (vagues 2 et 3).
- Génération de document (état liquidatif) — non couvert en V1.

---

## Technique

### Composants Angular
- `RegimeCommunauteLegaleBeSectionComponent` (`frontend/src/app/case-files/regime-communaute-legale-be-section/`) — standalone, OnPush, signals.
- `LiquidationPartageBeSectionComponent` (`frontend/src/app/case-files/liquidation-partage-be-section/`) — standalone, OnPush, signals.
- `RegimeCommunauteLegaleBeService` et `LiquidationPartageBeService` (`core/services/`) — `calculate()`, `get()`.
- `regime-communaute-legale-be-section-prefill-rules.ts` et `liquidation-partage-be-section-prefill-rules.ts` — helpers `getPrefillCount()` (return 0).
- 2 entrées `TOOL_REGISTRY` + 2 entrées `THEME_BY_TOOL_ID` dans `decisional-tools-panel.component.ts`.

### Migration Liquibase
- [x] Oui — `235-seed-f217-vague1-visibility-rules.xml` — 2 INSERT dans `decision_tool_visibility_rules` :
  - `regime-mat-be-communaute-legale` : `layer = ALWAYS_ON`, `legal_domain = DROIT_FAMILLE`, `country = BELGIQUE`, `trigger_field = NULL`, `trigger_value = NULL` — toute analyse de dossier de couple marié belge porte une qualification de régime (cf. `SF-217-00-coherence.md` : situation toujours pertinente, pas de flag IA requis).
  - `liquidation-partage-be` : `layer = ALWAYS_ON`, `legal_domain = DROIT_FAMILLE`, `country = BELGIQUE`, `trigger_field = NULL`, `trigger_value = NULL` — **confirmé `ALWAYS_ON`** : toute dissolution du couple appelle une liquidation-partage du patrimoine, l'outil est pertinent dès qu'un dossier Famille BE est ouvert (cohérent avec `tribunal-famille-be-mesures-prov` `ALWAYS_ON` de F-211).
  - Numéro `235` = numéro libre suivant (`232`=F-240, `233`=SF-217-01, `234`=SF-217-02 déjà pris). UUID namespace dédié F-217 : `f1a04001-0000-0000-0000-eeee21700XXX`.
  - Rollback : `DELETE FROM decision_tool_visibility_rules WHERE id LIKE 'f1a04001-0000-0000-0000-eeee21700%'`.

---

## Plan de test

### Tests unitaires (Jest, services mockés)
- [ ] Chargement : rechargement du dernier résultat via `GET` pour chaque section.
- [ ] Régime : ajout / suppression de lignes de biens et dettes ; `POST` appelé avec le bon payload ; affichage des biens qualifiés + synthèse.
- [ ] Liquidation : saisie des 8 étapes ; `POST` appelé avec le bon payload ; affichage de la checklist + délais.
- [ ] Affichage des verdicts (couleur correcte ; rouge réservé à `QUALIFICATION_INCOMPLETE` / `DELAI_CONTREDITS_CRITIQUE`).
- [ ] Erreur `POST` → `MatSnackBar`, formulaire conservé.
- [ ] Bannière info si workspace FR ; pas d'appel `POST`.
- [ ] `getPrefillCount()` → 0 pour les 2 composants.
- [ ] `coherenceAlerts` : pas d'alerte en `standaloneMode`.

### Isolation workspace
- [x] Non applicable — garantie côté backend (SF-217-01 / SF-217-02).

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Outil décisionnel métier** — 2 nouvelles sections décisionnelles. Scan fait (cf. analyse de cohérence transversale + `SF-217-00-coherence.md`). Un outil = une situation : composition du patrimoine ≠ procédure de liquidation.
- [x] Aucune autre préoccupation transversale (auth/workspace/plans/navigation non modifiés — ajout de sections dans un panel existant).

### Smoke tests E2E
- [x] Aucun concerné — feature additive (sections dans un panel existant, pas de modification de route / guard / auth).

---

## Dépendances
- `SF-217-01` et `SF-217-02` — contrats API figés (importés). Dev frontend parallélisable (tests sur mocks de service). Merge frontend **après** les 2 backends (cf. `feedback_pre_merge_endpoint_check` — vérifier que les endpoints `regime-mat-be-communaute-legale` et `liquidation-partage-be` existent côté backend avant `gh pr merge`).

---

## Notes et décisions
- `getPrefillCount() = 0` en V1 assumé pour les 2 outils : le pipeline IA n'extrait pas de flags dédiés à la composition du patrimoine ni à l'avancement de la liquidation. `PREFILL_COUNT_ALWAYS_ZERO = true` documenté pour le test d'intégrité — factuel, pas une dette masquée (cf. `SF-217-00-coherence.md` ajustement n° 2).
- Visibilité `ALWAYS_ON` confirmée pour les 2 outils : toute analyse d'un dossier de couple marié belge mobilise la qualification du régime, et toute dissolution appelle une liquidation-partage — ce sont des situations *toujours pertinentes*, pas des situations à détecter (cf. `SF-217-00-coherence.md` ajustement n° 1, pattern `F-FA-06` / `tribunal-famille-be-mesures-prov`).
- OnPush + signals : `markForCheck()` (via `ChangeDetectorRef`) dans le `next:` / `error:` des `subscribe()` (cf. `feedback_onpush_subscribe_markforcheck`).
- Le seed `decision_tool_visibility_rules` est livré dans cette SF frontend (couplé aux entrées `TOOL_REGISTRY`) — un seed sans entrée frontend ferait échouer `DecisionToolVisibilityIntegrityIT` ; une entrée frontend sans seed rendrait les outils silencieusement invisibles. Les deux vont ensemble (précédent SF-211-05 / SF-DT-36-02).
</content>
