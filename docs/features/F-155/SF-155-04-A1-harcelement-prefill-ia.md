# Mini-spec — F-155 / SF-155-04-A1 — Pré-fill IA + validation F-IA-03 harcèlement (template canonique)

## Identifiant

`F-155 / SF-155-04-A1`

## Feature parente

`F-155` — Audit cohérence composants décisionnels frontend + template canonique

## Statut

`ready`

## Date de création

2026-04-24

## Branche Git

`feat/SF-155-04-A1-harcelement-prefill-ia`

---

## Objectif

Brancher le composant `harcelement-licenciement-nul-section` (F-DT-11) sur les sorties IA `TravailExtractedData` étendues par le palier 1 backend (PR #518) afin de pré-remplir les 2 champs du form (salaire + motif) depuis l'analyse du dossier et de signaler toute divergence avec la saisie manuelle — ce composant servant de **template canonique IA-compliant** pour les 5 SFs frontend jumelles (A2/A3/B1/B2/C).

---

## Pattern de référence

**Composant canonique IA-compliant** : `frontend/src/app/case-files/immigration-title-decision-section/immigration-title-decision-section.component.{ts,html,scss}` (F-IM-05).

Pattern emprunté intégralement :
- Signals `provenance<Field>` (type `'IA' | null`) par champ pré-remplissable.
- Méthode privée `prefillFromAi()` invoquée dans `ngOnInit()` (fallback quand GET 404 — pas d'existant persisté) **et** `ngOnChanges()` (réaction aux changements `aiData` avant première résolution).
- Garde anti-écrasement : `prefillFromAi()` ne s'exécute **pas** quand le form est déjà masqué (`!showForm()`) ou qu'un résultat persisté est présent (`result()`).
- Handlers `on<Field>Change()` qui remettent le signal provenance à `null` dès modification manuelle (effacement badge IA).
- Computed signal `coherenceAlerts` exposant un `Partial<Record<FieldName, CoherenceAlert>>`, avec un `alertsSummary` cumulé pour la bannière.
- Directive `CoherencePopoverTriggerDirective` appliquée sur chaque field avec alerte, alimentée par `explanationFor(field)` depuis `SourceExplanationService` (F-IA-03-15c).
- Badge HTML : `<mat-icon>auto_awesome</mat-icon>` + libellé "Pré-rempli depuis l'analyse" visible uniquement si `provenance<Field>() === 'IA'`.

---

## Comportement attendu

### Cas nominal

1. Le panel F-IA-04 (`decisional-tools-panel`) instancie `<app-harcelement-licenciement-nul-section>` via `TOOL_REGISTRY['F-DT-11-harcelement-licenciement-nul']` en passant `aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`.
2. `ngOnInit()` appelle `load()` (GET existant).
   - Si une analyse persistée existe (GET 200) → `result` rempli, form masqué, pas de pré-fill IA (le form reste sur les valeurs persistées = "source de vérité avocat").
   - Si 404 → on retombe en mode formulaire et `prefillFromAi()` est invoqué (fallback).
3. `prefillFromAi()` :
   - Si `aiData?.salaireBrutMensuel > 0` → `salaireMensuelReference.set(...)` + `provenanceSalaire.set('IA')`.
   - Si `aiData?.motifNullitePressenti` est mappable vers un `MotifNullite` (selon `workspaceCountry` courant et table de mapping documentée) → `motifNullite.set(...)` + `provenanceMotifNullite.set('IA')`.
   - Aucun écrasement si la valeur IA est absente ou invalide (fail-open).
4. Quand l'avocat modifie un champ, `onSalaireChange()` / `onMotifNulliteChange()` remet la provenance IA à `null` → le badge disparaît.
5. Le computed `coherenceAlerts` évalue :
   - `SALAIRE` : divergence `aiData.salaireBrutMensuel` vs `salaireMensuelReference()` si écart relatif > 10 %.
   - `MOTIF_NULLITE` : divergence `aiData.motifNullitePressenti` (après mapping) vs `motifNullite()`.
6. Si `aiData.salaireEstDeduit === true` → note discrète sous le champ salaire ("Salaire déduit d'un montant net × 1,30").
7. Le POST `calculate()` reste inchangé ; après succès, `CaseDashboardRefreshService.triggerRefresh()` est appelé (pattern déjà présent).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `aiData` absent (null/undefined) | Pas de pré-fill, pas de badge, pas d'alerte. Form vide comportement actuel. | N/A |
| `aiData.motifNullitePressenti` non mappable (hors enum du domaine) | Pas de pré-fill sur le motif, reste null, pas de crash. | N/A |
| `aiData.motifNullitePressenti` mappable mais inconnu côté BE (dossier FR) / workspace_country switch | Pas de pré-fill pour un code incompatible avec le pays courant. | N/A |
| `aiData.salaireBrutMensuel` ≤ 0 ou non numérique | Pas de pré-fill. | N/A |
| `procedureChecks` / `aiQuestions` / `piecesManquantes` absents | Composant se comporte normalement (sources multiples optionnelles). | N/A |
| Analyse persistée (GET 200) puis avocat clique "Modifier" (editMode) | Le form réapparaît avec les valeurs persistées — pas de ré-écrasement IA (provenance reste null car saisie avocat). | N/A |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : les 5 autres SFs frontend du palier 2 (A2 inaptitude, A3 heures-sup, B1 OQTF avec délai, B2 OQTF sans délai, C Annexe13 BE) sont **jumelles** — chacune copiera ce pattern. Les outils antérieurs (F-DT-07/08/09/10, F-IM-05/06/07, F-FA-05/06) sont déjà IA-compliant (cf. `audit-prefill-ia-2026-04-24.md` §8.2) et ne sont pas touchés.
- [x] **Autres pays** : `workspaceCountry` FR et BE supportés. Le DTO backend `motifNullitePressenti` n'est rempli que sur dossiers FR (cf. SF-155-04-00-BE-travail §C — "Dossiers BE : les 5 champs restent `null`"). Le pré-fill ne se déclenche donc que si `workspaceCountry === 'FRANCE'` ET que le motif est mappable vers un `MotifNulliteFr`. Pas de cas BE car l'IA ne remplit pas le champ pour BE.
- [x] **Autres domaines** : DROIT_IMMIGRATION et DROIT_FAMILLE non concernés (outils dédiés, SFs séparées).
- [x] **Autres UI patterns** : provenance, coherenceAlerts, CoherencePopoverTrigger → patterns **déjà existants** via `immigration-title-decision-section` (canonique). Cette SF ne crée pas de pattern nouveau — elle applique le canonique à un composant jusque-là non conforme.
- [x] **Autres flows transversaux** : aucun. Pas de modification auth, workspace, plans, routing.

### Niveaux de vérification

- [x] **Modèle TypeScript** — `TravailExtractedData` (case-analysis.model.ts) déjà étendu par SF-155-04-00-BE-travail (merge PR #518). Champs `motifNullitePressenti`, `salaireBrutMensuel`, `salaireEstDeduit` consommés.
- [x] **Record / DTO backend** — inchangé (palier 1 mergé).
- [x] **Service / logique métier** — aucun changement dans `HarcelementNulliteService` ni backend.
- [x] **Entité JPA + schéma DB** — aucun impact.
- [x] **Tests existants** — spec du composant (7 tests) étendu à 17+ tests couvrant les nouveaux comportements sans régression.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `inaptitude-section` (F-DT-15) | Oui | SF parallèle SF-155-04-A2 (à créer) — pattern identique, 5 champs à pré-remplir. |
| `heures-sup-section` (F-DT-19) | Oui | SF parallèle SF-155-04-A3 (à créer) — pattern identique, agrégat `heuresSupMentionneesDansDossier`. |
| `oqtf-avec-delai-section` (F-IM-08-02) | Oui | SF parallèle SF-155-04-B1 (à créer) — pattern identique, `dateNotificationOqtf` + `motifOqtfCode`. |
| `oqtf-sans-delai-section` (F-IM-08-04) | Oui | SF parallèle SF-155-04-B2 (à créer) — pattern identique + alerte critique 48h. |
| `annexe13-be-section` (F-IM-08-06) | Oui | SF parallèle SF-155-04-C (à créer) — pattern identique, 4 champs BE. |
| `immigration-title-decision-section` | Non | Déjà canonique / IA-compliant, pas touché. |
| Pattern `provenance + coherenceAlerts + CoherencePopoverTrigger` | Existant | Imitation, pas de nouveau pattern. |

### Décision

- [x] Étendu à la cible de cette SF (`harcelement-licenciement-nul-section`).
- [x] Subfeatures parallèles créées/à créer pour les 5 autres composants (A2/A3/B1/B2/C).
- [ ] Backlog — n/a.
- [x] Non applicable aux autres cibles (canoniques déjà conformes, ou traitées par autres SFs).

---

## Critères d'acceptation

1. [ ] Le composant expose 4 nouveaux inputs : `@Input() aiData?: TravailExtractedData | null`, `@Input() procedureChecks?: ProcedureCheck[] | null`, `@Input() aiQuestions?: AiQuestion[] | null`, `@Input() piecesManquantes?: PieceManquanteEntry[] | null`.
2. [ ] Le composant expose 2 signals `provenanceSalaire` et `provenanceMotifNullite` de type `signal<'IA' | null>(null)`.
3. [ ] `prefillFromAi()` est invoqué dans `ngOnInit()` uniquement si le GET initial retourne 404 (fallback — pas d'écrasement de valeurs persistées) et dans `ngOnChanges()` quand `aiData` change avant première résolution.
4. [ ] Le pré-fill salaire ne s'applique que si `aiData?.salaireBrutMensuel` est un nombre > 0.
5. [ ] Le pré-fill motif ne s'applique que si `aiData?.motifNullitePressenti` est mappable vers un `MotifNulliteFr` (via table `AI_MOTIF_TO_MOTIF_NULLITE_FR`) ET que `workspaceCountry === 'FRANCE'`.
6. [ ] Le mapping motif documenté est : `DISCRIMINATION → DISCRIMINATION`, `HARCELEMENT_MORAL → HARCELEMENT_MORAL`, `HARCELEMENT_SEXUEL → HARCELEMENT_SEXUEL`, `MATERNITE_PATERNITE → GROSSESSE`. Les valeurs IA `RETORSION`, `SYNDICAL`, `ACCIDENT_MP` ne sont pas mappées (pas d'équivalent direct) → pas de pré-fill.
7. [ ] `onSalaireChange()` et `onMotifNulliteChange()` remettent respectivement `provenanceSalaire` et `provenanceMotifNullite` à `null`.
8. [ ] Les badges `<mat-icon>auto_awesome</mat-icon> Pré-rempli depuis l'analyse` sont affichés sous chaque champ uniquement si le signal provenance correspondant vaut `'IA'`.
9. [ ] Le computed `coherenceAlerts` produit :
    - `SALAIRE` si `aiData.salaireBrutMensuel` > 0 et saisie > 0 et écart relatif `> 10 %` ;
    - `MOTIF_NULLITE` si `aiData.motifNullitePressenti` est mappable et diffère de la saisie de l'avocat.
10. [ ] La directive `CoherencePopoverTriggerDirective` est appliquée sur le badge d'alerte de chaque champ concerné (pattern `immigration-title-decision-section`).
11. [ ] Une note discrète "Salaire déduit d'un montant net × 1,30" apparaît sous le champ salaire si `aiData.salaireEstDeduit === true`.
12. [ ] L'entrée `TOOL_REGISTRY['F-DT-11-harcelement-licenciement-nul']` est mise à jour pour injecter les 4 nouveaux inputs depuis le contexte.
13. [ ] Le composant reste entièrement fonctionnel sans `aiData` fourni (null-safe partout, pas de régression des 7 tests existants).
14. [ ] Spec jest : ≥ 15 tests au total (les 7 existants passant toujours + 10+ nouveaux couvrant pré-fill, badges, handlers, alertes, edge cases).
15. [ ] `npm run build` vert, `tsc --noEmit -p tsconfig.app.json` vert.

---

## Périmètre

### Hors scope (explicite)

- Pas de modification backend (palier 1 déjà mergé).
- Pas de modification des 5 autres composants frontend décisionnels (chacun dans sa SF jumelle).
- Pas de modification du service `HarcelementNulliteService`, ni de l'endpoint POST/GET existant.
- Pas de modification du calcul `indemniteMinimumNullite` ni de la `formule`.
- Pas de modification du refresh dashboard (déjà en place via `CaseDashboardRefreshService`).

---

## Contrat des Inputs

| Input | Type | Obligatoire | Source (panel F-IA-04) |
|-------|------|------------|------------------------|
| `caseFileId` | `string` | Oui | `ctx.caseFileId` |
| `workspaceCountry` | `'FRANCE' \| 'BELGIQUE'` | Non (défaut FRANCE) | `ctx.workspaceCountry` |
| `aiData` | `TravailExtractedData \| null \| undefined` | Non | `ctx.synthesis?.travailExtractedData` |
| `procedureChecks` | `ProcedureCheck[] \| null \| undefined` | Non | `ctx.procedureChecks` |
| `aiQuestions` | `AiQuestion[] \| null \| undefined` | Non | `ctx.aiQuestions` |
| `piecesManquantes` | `PieceManquanteEntry[] \| null \| undefined` | Non | `ctx.synthesis?.piecesManquantesDetails` |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| Seuil écart salaire | N/A | — | 10 % (`SALAIRE_DIVERGENCE_RATIO = 0.10`) | — | Constante privée |
| Motifs mappables IA→FR | N/A | — | 4 entries (DISCRIMINATION, HARCELEMENT_MORAL, HARCELEMENT_SEXUEL, MATERNITE_PATERNITE) | — | Uppercase |
| `workspaceCountry` pour pré-fill motif | Oui | — | `'FRANCE'` uniquement | — | Comparaison stricte |

---

## Technique

### Endpoint(s)

Aucun endpoint nouveau. Consommation uniquement des champs déjà exposés par `GET /api/v1/case-files/{id}/analysis` (via `aiData` reçu du panel). Endpoints existants `GET/POST /api/v1/case-files/{id}/harcelement-licenciement-nul` inchangés.

### Tables impactées

Aucune.

### Migration Liquibase

- [ ] Oui — n/a
- [x] Non applicable — SF frontend pure, pas de persistance nouvelle.

### Composants Angular

- `HarcelementLicenciementNulSectionComponent` — ajout inputs, signals, computed, handlers, prefillFromAi. Pas de changement de selector ni de dépendances publiques.
- `DecisionToolsPanelComponent` — une entrée `TOOL_REGISTRY` mise à jour pour binder les nouveaux inputs.

---

## Plan de test

### Tests spec (jest, style jasmine — alignement repo)

Tests existants (conservés, non-régression) :
1. [x] FRANCE → 8 motifs disponibles.
2. [x] BELGIQUE → 4 motifs disponibles.
3. [x] `load()` charge l'analyse existante (GET 200).
4. [x] `load()` reste en mode formulaire si GET 404.
5. [x] `formValid()` combinaisons valides/invalides.
6. [x] `calculate()` POST + snackbar succès.
7. [x] `calculate()` erreur backend → snackbar rouge.
8. [x] `calculate()` ignoré si form invalide.

Nouveaux tests (SF-155-04-A1) :
9. [ ] `prefillFromAi` : `aiData` complet (salaire + motif mappable) + GET 404 → salaire rempli, motif rempli, `provenanceSalaire()==='IA'`, `provenanceMotifNullite()==='IA'`.
10. [ ] `prefillFromAi` : `aiData` absent → rien rempli, aucun signal provenance.
11. [ ] `prefillFromAi` : `aiData.motifNullitePressenti='RETORSION'` (hors mapping) → motif non rempli, provenance null.
12. [ ] `prefillFromAi` : `aiData.salaireBrutMensuel=0` ou négatif → salaire non pré-rempli.
13. [ ] `onSalaireChange` : provenance IA set → appel handler → provenance remise à null (badge disparaît).
14. [ ] `onMotifNulliteChange` : idem pour motif.
15. [ ] `loadExisting` (GET 200) : valeurs persistées prioritaires sur pré-fill IA — aucune provenance IA après load.
16. [ ] `coherenceAlerts.SALAIRE` : divergence > 10 % détectée → alerte présente avec expectedDisplay.
17. [ ] `coherenceAlerts.SALAIRE` : écart ≤ 10 % → aucune alerte.
18. [ ] `coherenceAlerts.MOTIF_NULLITE` : divergence détectée entre mapping IA et saisie → alerte présente.
19. [ ] `ngOnChanges(aiData)` avec form vide et pas encore résolu : ré-applique le pré-fill.
20. [ ] `ngOnChanges(aiData)` après modification manuelle (provenance déjà nulle) : n'écrase pas la saisie avocat.
21. [ ] Pré-fill motif bloqué si `workspaceCountry='BELGIQUE'` même avec `motifNullitePressenti` valide.
22. [ ] `salaireEstDeduit=true` → template affiche la note déduction (inspection via debugElement/NativeElement optionnelle).

### Isolation workspace

- [x] Non applicable — SF frontend pure, les données viennent des inputs ; isolation garantie côté backend (endpoints existants inchangés).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — extension d'un composant frontend existant + ajustement registry (injection de 4 inputs supplémentaires, symétrique aux autres entrées registry).

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `DecisionToolsPanelComponent` | Entrée registry modifiée (4 nouveaux bindings) | Spec dédié du panel vérifie résolution composant — 1 test ajouté. |
| `HarcelementLicenciementNulSectionComponent` | Structure étendue (signals, inputs) — comportement actuel préservé | 7 tests existants restent verts + 15 nouveaux tests. |

### Smoke tests E2E concernés

- [x] Aucun smoke test touché — la SF ne modifie ni auth, ni workspace, ni navigation, ni accès backend.

---

## Dépendances

### Subfeatures bloquantes

- `SF-155-04-00-BE-travail` — statut : **Done** (PR #518 mergée). Les champs `motifNullitePressenti`, `salaireEstDeduit`, `salaireBrutMensuel` sont présents dans le DTO.

### Subfeatures débloquées par celle-ci

- `SF-155-04-A2` (inaptitude frontend) — pattern à copier depuis ce composant canonique.
- `SF-155-04-A3` (heures-sup frontend) — idem.
- `SF-155-04-B1`, `SF-155-04-B2`, `SF-155-04-C` (immigration frontend) — idem, avec adaptation DTO.

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` impactée.

---

## Impact par domaine métier

- **Droit du travail (FR)** : concerné directement — pré-fill salaire + motif depuis analyse IA. Mapping des 4 motifs prévus (DISCRIMINATION, HARCELEMENT_MORAL, HARCELEMENT_SEXUEL, MATERNITE_PATERNITE).
- **Droit du travail (BE)** : `motifNullitePressenti` reste null côté IA (cf. SF backend), donc pas de pré-fill motif. Le composant reste fonctionnel (4 motifs BE affichés comme avant, saisie manuelle).
- **Droit immigration** : non applicable — outil droit du travail uniquement.
- **Droit famille** : non applicable.

Cette SF est **sensible au domaine** droit du travail FR. La non-parité avec le droit immigration / famille est cohérente avec l'outil (F-DT-11 est un outil droit du travail FR+BE).

---

## Notes et décisions

- **Choix 1 — mapping enum IA → enum UI** : les enums ne sont pas identiques. Le DTO backend a 7 valeurs (`DISCRIMINATION, HARCELEMENT_MORAL, HARCELEMENT_SEXUEL, RETORSION, SYNDICAL, MATERNITE_PATERNITE, ACCIDENT_MP`), l'enum UI `MotifNulliteFr` a 8 valeurs (`HARCELEMENT_MORAL, HARCELEMENT_SEXUEL, DISCRIMINATION, GROSSESSE, SALARIE_PROTEGE, LIBERTE_FONDAMENTALE, ACTION_JUSTICE, ALERTE_ETHIQUE`). Mapping partiel et **explicite** via une constante `AI_MOTIF_TO_MOTIF_NULLITE_FR: Record<string, MotifNulliteFr>` : 4 entries (équivalents clairs). Les valeurs IA sans équivalent UI → pas de pré-fill (graceful). Rationalisation documentée dans ce fichier.
- **Choix 2 — garde anti-écrasement** : `prefillFromAi()` ne s'exécute que quand aucun résultat persisté n'existe (le GET 200 initial stoppe la chaîne par `load()` qui renseigne les signals depuis le backend). Cohérent avec `immigration-title-decision-section.loadExisting()`.
- **Choix 3 — seuil écart salaire** : 10 % (`SALAIRE_DIVERGENCE_RATIO = 0.10`). Choix aligné avec le seuil utilisé dans `F-DT-09-comparateur-indemnites` (le salaire de référence bascule l'indemnité d'un plafond à l'autre au seuil 10 %). Peut être ajusté post-déploiement via constante.
- **Choix 4 — pas de badge provenance pour `workspaceCountry`** : le gate country est une entrée, pas un champ form avec provenance. Conservation de la structure existante.
- **Choix 5 — BELGIQUE explicite** : le pré-fill motif est **bloqué** si `workspaceCountry === 'BELGIQUE'` pour éviter de mapper des codes IA FR vers des codes BE (ex. `HARCELEMENT_MORAL` vs `HARCELEMENT_MORAL_BE`). L'IA ne remplit pas ce champ pour BE côté backend (cf. SF-155-04-00-BE-travail §C), donc le code défensif empêche une régression future si le backend changeait.
- **Choix 6 — alerte cohérence "source"** : pour rester minimaliste sur cette SF canonique, les alertes se concentrent sur `aiData` (pas de prise en compte active de `procedureChecks/aiQuestions/piecesManquantes` — juste les inputs exposés pour symétrie avec `immigration-title-decision-section`). Les SFs jumelles (A2/A3/B1/B2/C) exploiteront davantage ces sources selon leur métier.
- **Choix 7 — style tests** : `jasmine.createSpyObj` conservé (convention repo, mix jest+jasmine shim).
