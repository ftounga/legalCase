# Mini-spec — F-DT-36 / SF-DT-36-02 — Frontend : section décisionnelle nullités de procédure

## Identifiant
`F-DT-36 / SF-DT-36-02`

## Feature parente
`F-DT-36` — Analyse des nullités de procédure de licenciement

## Statut
`ready`

## Date de création
2026-05-17

## Branche Git
`feat/SF-DT-36-02-frontend`

## Contrat API
Importé de **SF-DT-36-01-backend** (endpoints POST/GET figés). Tests Jest sur **mock du service** — pas de dépendance au backend mergé.

---

## Objectif

Ajouter dans le panel des outils décisionnels du dossier la section « Nullité de procédure de licenciement » : un formulaire de saisie des éléments de procédure, un calcul, et l'affichage du verdict de nullité.

---

## Comportement attendu

### Cas nominal
1. La section apparaît dans `app-decisional-tools-panel` quand un licenciement est détecté (visibilité contextuelle gérée par F-IA-04 — seed backend SF-DT-36-01).
2. L'avocat renseigne le formulaire (dates de convocation/entretien/notification, présence et motivation de la lettre, motif grave, licenciement collectif, convention collective).
3. Clic « Analyser » → `POST` → affichage du verdict (`NULLITE_AVEREE` / `NULLITE_PROBABLE` / `PROCEDURE_REGULIERE`), du score, et de la liste des vices détectés (libellé + fondement légal + explication).
4. À la réouverture du dossier, le dernier résultat (`GET`) est rechargé ; l'avocat peut ré-éditer et recalculer.
5. Après calcul, `CaseDashboardRefreshService.triggerRefresh()` est invoqué.

### Cas d'erreur
| Situation | Comportement |
|-----------|--------------|
| Échec `GET` au chargement | Section en formulaire vierge, pas de crash |
| Échec `POST` (400/4xx) | `MatSnackBar` avec le message backend, le formulaire reste éditable |

---

## Analyse de cohérence transversale

### Résultat du scan
| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Outils décisionnels licenciement (F-DT-08, F-DT-16) | Oui | F-DT-36 = situation distincte (vices de procédure). Pas de duplication — cf. `SF-DT-36-00-coherence.md` |
| Pré-fill IA | Partiel | Pas de flag procédural dédié extrait par le pipeline V1 → `getPrefillCount` = 0 en V1, `PREFILL_COUNT_ALWAYS_ZERO` documenté. Extension IA = SF ultérieure |
| F-IA-03 cohérence | Oui | Câblage `coherenceAlerts` sur les champs croisables avec F-96 / questions IA / pièces manquantes (délai convocation, motivation, entretien) |
| Refresh dashboard F-IA-02 | Oui | `triggerRefresh()` dans le `next:` du POST |

### Décision
- [x] Étendu à toutes les cibles applicables dans cette SF.

---

## Conformité F-IA-04

Section frontend décisionnelle — les 5 blocs s'appliquent.

### 1. Cohérence visuelle
- [x] Palette navy/or pour info, vert OK, rouge réservé au verdict `NULLITE_AVEREE`.
- [x] `<input type="date">` pour les dates (pas `MatDatepicker`).
- [x] `JetBrains Mono` pour `fondement` / `baseJuridique` ; `Inter` pour le reste.
- [x] Gate `workspaceCountry` : F-DT-36 est FR uniquement → bannière info si workspace BE (pas de masquage silencieux).
- [x] `MatSnackBar` pour les erreurs.
- [x] `CaseDashboardRefreshService.triggerRefresh()` dans le `next:` du POST.

### 2. Pré-fill IA
- [x] `@Input() aiData?: TravailExtractedData` typé.
- [x] `prefillFromAi()` invoqué dans `ngOnInit()` ET `ngOnChanges()`.
- [x] V1 : aucun flag procédural dédié dans `TravailExtractedData` → aucun champ pré-rempli, `getPrefillCount()` retourne 0. `PREFILL_COUNT_ALWAYS_ZERO = true` documenté (factuel — pas un oubli). Extension IA (flags `delaiConvocationNonRespecte`, etc.) = SF ultérieure si signal.

### 3. Validation F-IA-03
- [x] `coherenceAlerts = computed()` sur les champs croisables : `dateEntretienPrealable` / `motivationSuffisante` / `entretienTenu` vs F-96 (checklist procédure), questions IA, pièces manquantes.
- [x] Hiérarchie F-96 > Question IA > IA détection > Pièce manquante.
- [x] `<app-coherence-popover-trigger>` sur les champs concernés.
- [x] Helper partagé `CoherenceAlertBuilder` (`frontend/src/app/shared/coherence-popover/coherence-alert-builder.ts`) — pas de définition locale.

### 4. TOOL_REGISTRY + getPrefillCount
- [x] Entrée `F-DT-36-procedure-nullite-licenciement` dans `TOOL_REGISTRY` (`decisional-tools-panel.component.ts`), `inputs` passant `caseFileId, workspaceCountry, aiData, procedureChecks, aiQuestions, piecesManquantes`.
- [x] `TOOL_LABEL` + `TOOL_ICON` symétriques.
- [x] `static getPrefillCount()` retourne 0 (V1) — parité stricte avec `prefillFromAi()`.
- [x] Tests Jest : cas 0 champ (return 0).
- [x] Le seed `decision_tool_visibility_rules` (migration `231-seed-f-dt-36-visibility-rules.xml`) est porté par **cette SF frontend** — couplé à l'entrée `TOOL_REGISTRY` dans le même lot, conformément au précédent SF-211-05. Le garde-fou `DecisionToolVisibilityIntegrityIT` extrait dynamiquement `TOOL_REGISTRY` (plus de liste hardcodée `KNOWN_FRONTEND_TOOL_IDS`) : seed + entrée frontend doivent être livrés ensemble (cf. mémoire `feedback_pre_merge_visibility_seed_check`).

### 5. Parité des domaines (niveau ≥ 5)
- [x] Niveau du tool : **5** (scoring / analyse de validité — verdict de nullité).
- Parité par domaine :

| Domaine | Équivalent existe ? | Action |
|---------|---------------------|--------|
| Droit du travail | C'est F-DT-36 lui-même | — |
| Immigration | Non | Non pertinent — la « nullité de procédure de licenciement » est propre au droit du travail. Les vices de procédure immigration (OQTF, refus de titre) relèvent d'outils immigration distincts, hors périmètre. |
| Famille | Non | Non pertinent — concept propre au licenciement. |

---

## Critères d'acceptation
- [ ] La section apparaît dans le panel uniquement si un licenciement est détecté.
- [ ] Le formulaire couvre les champs du contrat API SF-DT-36-01.
- [ ] « Analyser » appelle `POST` et affiche verdict + score + liste des vices (libellé, fondement, explication).
- [ ] Le dernier résultat est rechargé à l'ouverture (`GET`).
- [ ] Erreur backend → `MatSnackBar`, formulaire conservé.
- [ ] Bannière info si `workspaceCountry` = BELGIQUE.
- [ ] `triggerRefresh()` appelé après calcul.
- [ ] Tests Jest verts (service mocké).

---

## Périmètre

### Hors scope
- Endpoints backend (SF-DT-36-01).
- Pré-fill IA effectif (V1 = 0 champ).
- Outil jumeau Belgique.

---

## Technique

### Composants Angular
- `ProcedureNulliteLicenciementSectionComponent` (standalone, OnPush, signals).
- `ProcedureNulliteLicenciementService` (`core/services/`) — `calculate()`, `get()`.
- `procedure-nullite-licenciement-section-prefill-rules.ts` — helper `getPrefillCount()`.
- Entrée `TOOL_REGISTRY` dans `decisional-tools-panel.component.ts`.
- Migration Liquibase `231-seed-f-dt-36-visibility-rules.xml` — seed `decision_tool_visibility_rules` (CONTEXTUAL, DROIT_DU_TRAVAIL / FRANCE, trigger `type_rupture`).

---

## Plan de test

### Tests unitaires (Jest, service mocké)
- [ ] Chargement : rechargement du dernier résultat via `GET`.
- [ ] Calcul : `POST` appelé avec le bon payload, verdict affiché.
- [ ] Affichage des 3 verdicts (couleur correcte, rouge réservé à `NULLITE_AVEREE`).
- [ ] Erreur `POST` → `MatSnackBar`, formulaire conservé.
- [ ] Bannière info si workspace BE.
- [ ] `getPrefillCount()` → 0.
- [ ] `coherenceAlerts` : pas d'alerte en `standaloneMode`.

### Isolation workspace
- [x] Non applicable — garantie côté backend (SF-DT-36-01).

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Outil décisionnel métier** — nouvelle section décisionnelle. Scan fait (cf. cohérence transversale + `SF-DT-36-00-coherence.md`).
- [x] Aucune autre (auth/workspace/plans/navigation non modifiés).

### Smoke tests E2E
- [x] Aucun concerné.

---

## Dépendances
- `SF-DT-36-01` — contrat API figé (importé). Dev parallèle possible (tests sur mock). Merge frontend **après** backend (cf. `feedback_pre_merge_endpoint_check`).

---

## Notes et décisions
- `getPrefillCount() = 0` en V1 assumé : le pipeline IA n'extrait pas encore de flags procéduraux dédiés. `PREFILL_COUNT_ALWAYS_ZERO` documenté pour le test d'intégrité — c'est factuel, pas une dette masquée.
- OnPush + signals : `markForCheck()` dans `next`/`error` des `subscribe()` (cf. `feedback_onpush_subscribe_markforcheck`).
