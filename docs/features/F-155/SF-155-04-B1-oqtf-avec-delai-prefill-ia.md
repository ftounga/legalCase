# Mini-spec — SF-155-04-B1 Pré-fill IA + validation F-IA-03 pour `oqtf-avec-delai-section` (F-IM-08-02 FR)

## Identifiant

`F-155 / SF-155-04-B1`

## Feature parente

`F-155` — Harmonisation cohérence frontend + pré-fill IA des outils décisionnels (batch 2026-04-24)

## Statut

`ready`

## Date de création

2026-04-24

## Branche Git

`feat/SF-155-04-B1-oqtf-avec-delai-prefill-ia`

---

## Objectif

Brancher l'outil décisionnel `oqtf-avec-delai-section` (F-IM-08-02 FR) sur les champs IA désormais
exposés par le palier backend (SF-155-04-00-BE-immig-FR, PR #519) : pré-remplissage automatique des
champs de notification, motif et recours, plus alertes de cohérence F-IA-03 si l'avocat saisit une
valeur divergente — en particulier l'alerte CRITIQUE qui signale un recours déjà formé mais oublié.

---

## Comportement attendu

### Cas nominal

1. Ouverture du panneau décisionnel pour un dossier FR immigration dont l'analyse IA a détecté
   les champs OQTF — le panel F-IA-04 (`decisional-tools-panel`) instancie
   `OqtfAvecDelaiSectionComponent` avec les inputs : `caseFileId`, `workspaceCountry`,
   `aiData=ctx.synthesis.immigrationExtractedData`, `procedureChecks`, `aiQuestions`,
   `piecesManquantes`.
2. `ngOnInit()` : (a) si `isFrance()`, appelle `load()` (GET analyse existante) ; (b) si 404,
   reste en mode formulaire, déclenche `prefillFromAi()` qui remplit :
   - `dateNotificationOqtf` ← `aiData.dateNotificationOqtf` (YYYY-MM-DD tel quel) si non vide et
     non futur,
   - `motifOqtf` ← `aiData.motifOqtfCode` seulement si la valeur appartient à l'enum front
     `MotifOqtf` (sinon le champ reste nul — pas de fallback implicite),
   - `recoursForme` ← dérivé de `aiData.recoursFormeDetected.reponse` : `OUI` → `true`, `NON` →
     `false`, `INCONNU` (ou absent) → pas de pré-fill (reste à `false` défaut).
3. Chaque champ pré-rempli affiche un badge `auto_awesome` + texte "Pré-rempli depuis l'analyse"
   (signaux `provenanceDateNotification`, `provenanceMotifOqtf`, `provenanceRecoursForme`).
4. Si `ngOnChanges` reçoit un nouveau `aiData` (pipeline IA terminé après ouverture), le
   pré-remplissage est ré-appliqué uniquement si `showForm()` et `result() === null`
   — on n'écrase jamais une analyse persistée ni une saisie avocat en cours.
5. Le handler de changement manuel efface le badge du champ concerné
   (`onDateNotificationChange`, `onMotifOqtfChange`, `onRecoursFormeChange`).
6. `coherenceAlerts` (computed) expose 3 alertes possibles :
   - `DATE_NOTIFICATION` : divergence entre valeur avocat et `aiData.dateNotificationOqtf`
     (sévérité WARNING),
   - `MOTIF_OQTF` : divergence entre `motifOqtf` avocat et `aiData.motifOqtfCode` (WARNING),
   - `RECOURS_FORME` : **contradiction critique** — avocat coche "recours non formé"
     (`recoursForme=false`) alors que `aiData.recoursFormeDetected.reponse === 'OUI'` (sévérité
     CRITICAL — l'avocat a potentiellement oublié un recours déjà déposé, risque
     d'irrecevabilité).
7. Si `workspaceCountry === 'BELGIQUE'` : bannière info "OQTF — procédure française uniquement"
   (comportement existant conservé, pattern F-IM-08-02 initial).
8. Mise à jour du `TOOL_REGISTRY` (`F-IM-08-oqtf-avec-delai-fr`) : ajouter `aiData`,
   `procedureChecks`, `aiQuestions`, `piecesManquantes` dans `inputs(ctx)`.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `aiData.motifOqtfCode` hors enum `MotifOqtf` (ex : code backend inattendu) | Le champ reste nul, aucun badge, aucune alerte — skip silencieux (garde-fou) |
| `aiData.dateNotificationOqtf` malformée (pas ISO) | Le champ reste nul ; pas de crash (on n'applique que si string ISO valide) |
| `aiData.recoursFormeDetected.reponse === 'INCONNU'` | Pas de pré-fill ; valeur reste `false` défaut ; pas de badge ; pas d'alerte |
| GET 200 (analyse persistée) | `prefillFromAi()` n'est PAS appelé (on priorise l'analyse persistée — pas d'écrasement) |
| `aiData === null` et `aiData === undefined` | `prefillFromAi()` no-op, aucun badge, aucune alerte |
| `workspaceCountry === 'BELGIQUE'` | Pas de pré-fill ni d'alerte ; bannière info FR-only conservée |
| Modification manuelle d'un champ pré-rempli | Badge disparaît, alerte de divergence peut apparaître si l'écart subsiste |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : cf. tableau ci-dessous — les 5 autres composants du batch
  2026-04-24 sont tous en dette du même pattern (F-155 SF-155-04 A/B/C).
- [x] **Autres pays** : FR only — pour BE, l'outil équivalent est `annexe13-be-section`
  (F-IM-08-06) traité en SF-155-04-C parallèle.
- [x] **Autres domaines** : Immigration uniquement. Droit du travail traité par A1/A2/A3,
  Famille hors scope F-155 (outils divorce déjà conformes au canonique F-IM-05).
- [x] **Autres UI patterns** : pattern `immigration-title-decision-section` réutilisé tel quel
  (signals `provenanceXxx`, `coherenceAlerts` computed, badges `auto_awesome`, directive
  `CoherencePopoverTriggerDirective`).
- [x] **Autres flows transversaux** : pas de changement auth/workspace/navigation/plans.

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript** : `ImmigrationExtractedData` (case-analysis.model.ts) déjà enrichi
  par SF-155-04-00-BE-immig-FR — champs `dateNotificationOqtf`, `motifOqtfCode`,
  `recoursFormeDetected` présents. Vérifié ligne 246-251.
- [x] **Record backend** : mergé (PR #519 SF-155-04-00-BE-immig-FR).
- [x] **Service / logique métier** : pas de changement backend côté OQTF calculator — on
  consomme uniquement l'analyse IA.
- [x] **Entité JPA / DB** : non applicable (pas de nouvelle persistance).
- [x] **Tests existants** : spec `oqtf-avec-delai-section` actuelle (17 tests) — on ajoute
  ≥ 15 nouveaux tests sans casser les existants.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : oui, sources `aiData` + `procedureChecks` F-96 + `aiQuestions`
  + `piecesManquantes`. Implémentation minimale sur ce composant — on commence par les champs
  directs `aiData` (3 alertes). Extension F96/QUESTION_IA/PIECE_MANQUANTE possible en V2 si
  référentiels `OQTF_DATE`, `OQTF_MOTIF`, `OQTF_RECOURS_FORME` disponibles (non prévus par
  F-96 à ce jour — backlog).
- [x] **Refresh dashboard (F-IA-02)** : déjà appelé dans `analyze()` via `dashboardRefresh?.triggerRefresh()`.
- [x] **Pré-remplissage IA** : objet central de cette SF.
- [x] **Persistance des inputs** : déjà en place via `OqtfAvecDelaiResponse.dateNotificationOqtf`,
  `motifOqtf`, `recoursForme`, `dateRecours` (SF-IM-08-01).
- [x] **Masquage conditionnel selon pays** : `workspaceCountry !== 'FRANCE'` → bannière info
  (conservé).
- [x] **Alertes actives après calcul** : gate `showForm()` utilisé — pas de `|| this.result()`
  (bug SF-IA-03-12 évité).

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — on réutilise directement :
- `CoherencePopoverTriggerDirective` (déjà partagé).
- Signal pattern `provenanceXxx` (canonique `immigration-title-decision-section`).
- Badge `auto_awesome` inline + classe `.provenance-note` — à ajouter localement au SCSS du
  composant (pas de service ni directive nouvelle).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `harcelement-licenciement-nul-section` (F-155-04-A1) | Oui | SF parallèle — branche `feat/SF-155-04-A1-...` |
| `inaptitude-section` (F-155-04-A2) | Oui | SF parallèle |
| `heures-sup-section` (F-155-04-A3) | Oui | SF parallèle |
| `oqtf-sans-delai-section` (F-155-04-B2) | Oui | SF parallèle — très proche, mais champs différents (datetime-local + CRA) |
| `annexe13-be-section` (F-155-04-C) | Oui | SF parallèle BE |
| `immigration-title-decision-section` (F-IM-05) | Non | Canonique — déjà conforme |
| Autres composants Famille / droit du travail antérieurs | Non | Déjà conformes au pattern |
| Extension F96 OQTF_MOTIF | Non | Pas de critère F-96 existant pour OQTF — backlog si besoin |

### Décision

- [x] Étendu à `oqtf-avec-delai-section` uniquement dans cette SF (scope B1).
- [x] SF parallèles créées pour les 5 autres composants (A1/A2/A3/B2/C).
- [x] Backlog : extension F96/QUESTION_IA/PIECE_MANQUANTE pour OQTF si referentiel F-96 OQTF
  jamais introduit.

---

## Impact par domaine métier

- **Droit du travail** : non concerné par cette SF (géré par A1/A2/A3).
- **Immigration FR** : concerné directement — outil spécifique OQTF avec délai FR, alimenté par
  `ImmigrationExtractedData` FR (champs `dateNotificationOqtf`, `motifOqtfCode`,
  `recoursFormeDetected`).
- **Immigration BE** : non concerné — outil équivalent `annexe13-be-section` (SF-155-04-C).
- **Famille** : non concerné — pas d'équivalent OQTF en droit famille.

Parité des domaines : l'outil OQTF est FR-only par construction juridique (l'équivalent belge
est l'Annexe 13, outil distinct). Pas d'asymétrie créée — le pattern pré-fill IA est déjà
appliqué en droit famille (F-FA-05/06/07) et droit du travail (F-DT-07) et sera uniformisé sur
le batch 2026-04-24 via les 6 SFs F-155-04 parallèles.

Sensibilité domaine : **Immigration FR uniquement** — le composant reste bloqué par le gate
`workspaceCountry === 'FRANCE'` (bannière info sinon).

---

## Critères d'acceptation

- [ ] Le composant déclare `@Input() aiData?`, `procedureChecks?`, `aiQuestions?`, `piecesManquantes?`.
- [ ] `prefillFromAi()` invoqué dans `ngOnInit()` (post-404) et `ngOnChanges()` (quand `aiData`
  change, uniquement si `showForm()` et `!result()`).
- [ ] `prefillFromAi()` alimente `dateNotificationOqtf` depuis `aiData.dateNotificationOqtf`.
- [ ] `prefillFromAi()` alimente `motifOqtf` depuis `aiData.motifOqtfCode` uniquement si valeur
  dans l'enum `MotifOqtf` ; sinon skip silencieux.
- [ ] `prefillFromAi()` convertit `aiData.recoursFormeDetected.reponse` : `OUI` → `true`,
  `NON` → `false`, `INCONNU` → no-op.
- [ ] Signaux `provenanceDateNotification`, `provenanceMotifOqtf`, `provenanceRecoursForme` de
  type `'IA' | null`, valeur `'IA'` dès pré-fill.
- [ ] Badge template `<span class="provenance-note"><mat-icon>auto_awesome</mat-icon>
  Pré-rempli depuis l'analyse</span>` affiché conditionnellement.
- [ ] Handlers `onDateNotificationChange`, `onMotifOqtfChange`, `onRecoursFormeChange` remettent
  la provenance à `null`.
- [ ] Computed `coherenceAlerts` produit 3 alertes conditionnelles (DATE_NOTIFICATION,
  MOTIF_OQTF, RECOURS_FORME-critique).
- [ ] Template affiche les alertes via `CoherencePopoverTriggerDirective` sur les champs
  concernés.
- [ ] L'alerte `RECOURS_FORME` est marquée `CRITICAL` (sévérité distincte WARNING) avec
  libellé "Risque d'irrecevabilité — recours déjà formé détecté".
- [ ] `TOOL_REGISTRY['F-IM-08-oqtf-avec-delai-fr']` expose `aiData`, `procedureChecks`,
  `aiQuestions`, `piecesManquantes`.
- [ ] Pas d'écrasement de l'analyse persistée (GET 200) par le pré-fill.
- [ ] Pas d'écrasement de la saisie avocat par un `ngOnChanges` tardif (`!result()` gate).
- [ ] Gate `workspaceCountry === 'BELGIQUE'` : bannière info — pas de pré-fill.
- [ ] Tests spec ≥ 15 nouveaux couvrant les scénarios ci-dessus.
- [ ] `ng build` et `ng test --watch=false --browsers=ChromeHeadless` verts.

---

## Périmètre

### Hors scope (explicite)

- Ne pas toucher aux autres composants (harcèlement / inaptitude / heures-sup / oqtf-sans-delai /
  annexe13-be) — chacun est traité dans sa propre SF parallèle.
- Ne pas modifier le backend (endpoint OQTF, prompt IA, record Java).
- Ne pas ajouter de critère F-96 OQTF_DATE / OQTF_MOTIF / OQTF_RECOURS_FORME (pas de source
  référentielle existante — possible backlog V2).
- Ne pas afficher de popover riche (SourceExplanation) sur les champs OQTF — simple badge
  `CoherencePopoverTriggerDirective` avec reason textuelle.
- Ne pas toucher `dateRecours` par pré-fill — pas de champ `dateRecoursDetected` dans
  `ImmigrationExtractedData` (si recoursForme pré-rempli `true`, l'avocat saisit la date
  manuellement — pas d'écrasement).

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs autorisées | Normalisation |
|-------|-------------|-----------------------------|---------------|
| `dateNotificationOqtf` | Oui (pour analyse) | YYYY-MM-DD, non futur | Pré-fill seulement si string passe `/^\d{4}-\d{2}-\d{2}$/` et ≤ today |
| `motifOqtf` | Oui (pour analyse) | Enum `MotifOqtf` : REFUS_TITRE, EXPIRATION_TITRE, SEJOUR_IRREGULIER, RETRAIT_TITRE, AUTRE | Pré-fill seulement si `aiData.motifOqtfCode` dans cet ensemble |
| `recoursForme` | Oui | boolean | Pré-fill via mapping OUI/NON/INCONNU |
| `dateRecours` | Oui si `recoursForme=true` | YYYY-MM-DD, ≥ dateNotification | Pas pré-rempli |

---

## Technique

### Endpoints

Aucun nouvel endpoint. Consomme le contrat SF-IM-08-01 existant (`GET/POST /api/v1/case-files/{id}/oqtf-avec-delai`).

### Tables impactées

Aucune.

### Migration Liquibase

- [ ] Oui
- [x] Non applicable

### Composants Angular

- `OqtfAvecDelaiSectionComponent` — inputs étendus + `prefillFromAi()` + `coherenceAlerts`
  + badges provenance + handlers.
- `DecisionToolsPanelComponent` — mise à jour `TOOL_REGISTRY['F-IM-08-oqtf-avec-delai-fr']`.

### Contrat API

Pas de nouveau contrat. Contrat importé de SF-155-04-00-BE-immig-FR (`ImmigrationExtractedData.dateNotificationOqtf`,
`.motifOqtfCode`, `.recoursFormeDetected: DetectedAnswer | null`).

---

## Plan de test

### Tests unitaires (≥ 15 nouveaux)

1. `prefillFromAi` : aiData complet (date + motif valide + recoursFormeDetected OUI) →
   les 3 champs remplis + les 3 badges `'IA'`.
2. `prefillFromAi` : `motifOqtfCode='INCONNU_XYZ'` → champ `motifOqtf` reste `null`, pas de
   badge.
3. `prefillFromAi` : `recoursFormeDetected.reponse='OUI'` → `recoursForme=true`, badge.
4. `prefillFromAi` : `recoursFormeDetected.reponse='NON'` → `recoursForme=false`, badge.
5. `prefillFromAi` : `recoursFormeDetected.reponse='INCONNU'` → pas de pré-fill recours,
   pas de badge.
6. `prefillFromAi` : `dateNotificationOqtf` malformée (`"31/03/2026"`) → champ reste `null`.
7. `prefillFromAi` : `dateNotificationOqtf` dans le futur → champ reste `null`.
8. Modification manuelle (handler) : `onMotifOqtfChange` → `provenanceMotifOqtf` devient
   `null`.
9. `coherenceAlerts` — divergence date notification : aiData='2026-04-01',
   avocat='2026-04-03' → alerte `DATE_NOTIFICATION` présente.
10. `coherenceAlerts` — divergence motif : aiData='EXPIRATION_TITRE',
    avocat='REFUS_TITRE' → alerte `MOTIF_OQTF`.
11. `coherenceAlerts` — contradiction critique : `recoursFormeDetected.reponse='OUI'`
    + `recoursForme=false` → alerte `RECOURS_FORME` sévérité `CRITICAL`.
12. `coherenceAlerts` — pas de contradiction : `recoursFormeDetected.reponse='OUI'` +
    `recoursForme=true` → pas d'alerte RECOURS_FORME.
13. `ngOnChanges` : nouveau aiData reçu + showForm()=true + !result() → re-prefill.
14. `ngOnChanges` : nouveau aiData reçu mais result() persisté → pas de re-prefill
    (pas d'écrasement).
15. `loadExisting` (GET 200) : l'analyse persistée est affichée, `prefillFromAi` pas appelé,
    pas de badge provenance.
16. Gate pays : `workspaceCountry='BELGIQUE'` → pas de pré-fill, pas d'alerte, bannière info.
17. Fixture aiData malformée (`recoursFormeDetected: { reponse: 'FOO' }`) → no-op.

### Tests d'intégration

Non applicables — pas de nouvel endpoint.

### Isolation workspace

Non applicable — pas d'accès DB côté frontend ; isolation garantie au backend (SF-IM-08-01).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing
- [x] **Outil décisionnel métier** — modification de `oqtf-avec-delai-section` + TOOL_REGISTRY

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|-----------------|------------------------------|
| `OqtfAvecDelaiSectionComponent` | Ajout signals/inputs — les tests existants doivent passer | Les 17 tests spec existants inchangés |
| `DecisionToolsPanelComponent` | Ajout 4 inputs au mapping `F-IM-08-oqtf-avec-delai-fr` | Test mapping si existant ; build TypeScript strict |
| Autres composants décisionnels | Aucun impact (modification ciblée `TOOL_REGISTRY`) | Non applicable |

Scan des autres outils décisionnels : F-DT-07/08/09/10, F-FA-05/06/07, F-IM-05/06/07 — chacun
a son propre mapping dans `TOOL_REGISTRY` et ses propres `@Input()`. Aucune mutualisation
potentielle — chacun aligne ses champs aiData sur son DTO backend.

### Smoke tests E2E concernés

- [ ] `e2e/smoke/navigation.spec.ts` — aucune route touchée
- [x] Aucun smoke test concerné — changements localisés à un composant frontend, pas de
  modification auth/workspace/navigation.

---

## Dépendances

### Subfeatures bloquantes

- `SF-155-04-00-BE-immig-FR` — statut : **done** (PR #519 mergée).

### Questions ouvertes impactées

- [ ] Aucune question de `docs/OPEN_QUESTIONS.md` impactée.

---

## Notes et décisions

- **Pattern canonique** : `immigration-title-decision-section` (F-IM-05) — même domaine
  Immigration, même structure (signals + computed alerts + CoherencePopoverTriggerDirective).
- **Pas de SourceExplanationService ici** : on évite la dépendance supplémentaire, les alertes
  OQTF n'ont pas de `source_explanations` en DB (pas de critère F-96 OQTF référencé). L'alerte
  expose simplement un `reason` textuel via `[appCoherencePopoverReason]`.
- **Alerte CRITICAL vs WARNING** : distinction portée par une propriété `severity` sur
  l'interface `OqtfCoherenceAlert`. Les deux utilisent `CoherencePopoverTriggerDirective`
  mais le label/couleur diffère (sévérité CRITICAL rend une bordure rouge, réservée aux cas
  bloquants par DESIGN_SYSTEM.md — ici risque d'irrecevabilité = cas critique légitime).
- **Pas d'écrasement des valeurs chargées** : gate `!result()` dans `ngOnChanges` — priorise
  toujours l'analyse persistée.
- **Conflit TOOL_REGISTRY** : les 6 SFs parallèles modifient toutes ce tableau. Rebase post-
  merge si conflit — garder TOUS les bindings IA des autres SFs déjà mergées.
