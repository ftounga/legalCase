# Mini-spec — F-155 / SF-155-04-A3 — heures sup pré-fill IA + validation F-IA-03

## Identifiant

`F-155 / SF-155-04-A3`

## Feature parente

`F-155` — Audit cohérence composants décisionnels frontend + template canonique

## Statut

`ready`

## Date de création

2026-04-24

## Branche Git

`feat/SF-155-04-A3-heures-sup-prefill-ia`

---

## Objectif

Amener le composant frontend `HeuresSupSectionComponent` (F-DT-19-02) à la conformité du pattern canonique IA (`immigration-title-decision-section`) : pré-remplissage des champs depuis `TravailExtractedData.salaireBrutMensuel` et `TravailExtractedData.heuresSupMentionneesDansDossier`, badges de provenance "Pré-rempli depuis l'analyse", alertes de cohérence F-IA-03 (divergence taux horaire et heures sup saisies vs analyse IA), et branchement `aiData` + sources IA annexes dans le `TOOL_REGISTRY`.

---

## Comportement attendu

### Cas nominal

1. Le dossier droit du travail FR est analysé par le pipeline IA (SF-155-04-00-BE-travail mergée) → `TravailExtractedData` expose `salaireBrutMensuel`, `heuresSupMentionneesDansDossier: { totalDeclarees25pct, totalDeclarees50pct, horsContingent }`, `salaireEstDeduit`.
2. L'avocat ouvre le dossier, déplie l'outil "Rappel heures supplémentaires" (F-DT-19). Le composant reçoit `aiData` via le `TOOL_REGISTRY`.
3. Dans `ngOnInit()`, `prefillFromAi()` est invoqué si aucune analyse persistée en base n'existe (404 → fallback) :
   - `tauxHoraireBrut` ← `salaireBrutMensuel / 151.67` arrondi à 2 décimales (durée légale FR 35 h hebdo = 151.67 h/mois).
   - `heuresSupDeclarees25pct` ← `heuresSupMentionneesDansDossier.totalDeclarees25pct`.
   - `heuresSupDeclarees50pct` ← `heuresSupMentionneesDansDossier.totalDeclarees50pct`.
   - `heuresHorsContingent` ← `heuresSupMentionneesDansDossier.horsContingent`.
   - `provenanceTauxHoraire`, `provenanceHeures25`, `provenanceHeures50`, `provenanceHorsContingent` passent à `'IA'` pour chaque champ effectivement renseigné depuis l'IA.
4. À côté de chaque champ rempli, un badge `auto_awesome` "Pré-rempli depuis l'analyse" s'affiche.
5. Dès que l'avocat modifie manuellement un champ (handler `onXxxChange`), le signal `provenance*` correspondant revient à `null` → le badge disparaît.
6. Le computed `coherenceAlerts` expose dynamiquement les alertes :
   - **TAUX_HORAIRE** : si l'avocat saisit un taux horaire dont l'écart relatif avec le taux dérivé de `salaireBrutMensuel` excède 10 %.
   - **HEURES_SUP** : si la somme des heures sup saisies (25 + 50 + hors contingent) est >> la somme des heures sup détectées par l'IA (écart absolu ≥ 5 h ET l'écart relatif ≥ 50 %).
   - **SALAIRE_DEDUIT** (note info, pas alerte warning) : si `salaireEstDeduit === true`, un message précise que le taux horaire dérivé du brut déduit d'un net est moins fiable.
7. Les alertes sont rendues via la directive `CoherencePopoverTriggerDirective` sur les fields concernés (icône `warning`, popover au hover).
8. Si `workspaceCountry === 'BELGIQUE'` : une note discrète "Pré-fill IA heures sup disponible uniquement pour la France" est affichée et les champs BE restent vides ; aucun pré-fill, aucune alerte IA.
9. Au clic "Calculer", le flux POST existant est inchangé ; `CaseDashboardRefreshService.triggerRefresh()` est déjà appelé (inchangé).
10. `ngOnChanges(aiData)` : si `aiData` arrive après `ngOnInit` (pipeline fraîchement terminé) et qu'aucune analyse n'a encore été calculée (`showForm() === true && !result()`), `prefillFromAi()` est re-appelé. Les champs déjà modifiés manuellement (provenance `null`) ne sont PAS écrasés.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `aiData` absent (analyse non lancée) | Pas de pré-fill, pas de badge, pas d'alerte — form vierge | — |
| `heuresSupMentionneesDansDossier` partiellement rempli (ex. seul `totalDeclarees25pct`) | Seul ce champ est pré-rempli, badge IA uniquement sur lui | — |
| `salaireBrutMensuel` absent | `tauxHoraireBrut` non pré-rempli ; les heures sup peuvent l'être quand même | — |
| `salaireBrutMensuel === 0` ou négatif | `tauxHoraireBrut` non pré-rempli (garde-fou division) | — |
| `workspaceCountry === 'BELGIQUE'` | Note discrète affichée, aucun pré-fill IA, aucun coherence alert | — |
| Analyse existante déjà persistée (`GET 200`) | `loadExisting()` remplit depuis la réponse, `showForm` passe à `false`, aucun pré-fill IA (la persistance prime) | 200 |
| `aiData` arrive après `ngOnInit` mais `result()` déjà non-null | `prefillFromAi()` n'est PAS ré-invoqué (ne pas écraser un calcul existant) | — |
| Fixture malformée (`heuresSupMentionneesDansDossier` non-objet) | Garde-fou `typeof === 'object'` → pas de pré-fill heures sup, pas d'exception | — |
| Avocat modifie manuellement le taux horaire | `provenanceTauxHoraire.set(null)` → badge disparaît, l'alerte divergence reste si écart > 10 % | — |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : SF-155-04-A1 (`harcelement-licenciement-nul-section`) et SF-155-04-A2 (`inaptitude-section`) tournent en parallèle avec la même règle de pré-fill IA + coherenceAlerts ; SF-155-04-B1/B2 et C traitent les 3 composants immigration. Les outils antérieurs (F-DT-07, F-DT-08, F-DT-09, F-DT-10, F-IM-05, F-IM-06, F-IM-07, F-FA-05/06) ont déjà le pattern — pas de régression attendue.
- [x] **Autres pays** : FR uniquement pour le pré-fill (heures sup `heuresSupMentionneesDansDossier` est FR-only dans le record backend). BE → note discrète "pré-fill IA non disponible", champs laissés vides.
- [x] **Autres domaines** : Famille / Immigration non concernés (cette SF touche uniquement le composant heures sup travail).
- [x] **Autres UI patterns** : pattern `provenance<Field>` + `CoherencePopoverTriggerDirective` déjà canonique (cf. `immigration-title-decision-section`, `anciennete-section`) — aucun nouveau pattern introduit.
- [x] **Autres flows transversaux** : aucun. La SF ne touche ni auth, ni workspace, ni plans, ni routing.

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript** : `case-analysis.model.ts` déjà étendu par SF-155-04-00-BE-travail (champs `heuresSupMentionneesDansDossier`, `salaireBrutMensuel`, `salaireEstDeduit` présents sur `TravailExtractedData`). Aucune modification nécessaire.
- [x] **Record / DTO backend** : inchangé (backend palier 1 mergé).
- [x] **Service / logique métier** : calcul POST `/api/v1/case-files/{id}/heures-sup` inchangé.
- [x] **Entité JPA** : aucun impact.
- [x] **Tests existants** : `HeuresSupSectionComponent.spec` étendu pour les nouveaux scénarios IA ; autres specs (panel F-IA-04) non impactées car ajout d'inputs rétrocompatibles.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : implémentée — `coherenceAlerts` computed + `CoherencePopoverTriggerDirective`.
- [x] **Refresh dashboard (F-IA-02)** : déjà présent (`dashboardRefresh?.triggerRefresh()` dans `calculate()`).
- [x] **Pré-remplissage IA** : ajouté — `prefillFromAi()` + `provenance*` + badges UI.
- [x] **Persistance des inputs** : inchangée (GET/POST `/heures-sup` gère déjà la persistance via SF-DT-19-01).
- [x] **Masquage conditionnel selon type** : gate `workspaceCountry` existante ; le pré-fill IA est FR-only, BE reçoit une note.
- [x] **Alertes actives après calcul** : gate `showForm()` uniquement (pas `|| this.result()`), comme le pattern canonique.

### Cas spécifique : nouveau pattern UI ou service partagé

Aucun nouveau pattern introduit — réutilisation du pattern existant `CoherencePopoverTriggerDirective` + signals `provenance<Field>` déjà éprouvé par 10+ composants.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `harcelement-licenciement-nul-section` (F-DT-11-02) | Oui | **SF parallèle** SF-155-04-A1 — branche `feat/SF-155-04-A1-...` |
| `inaptitude-section` (F-DT-15-02) | Oui | **SF parallèle** SF-155-04-A2 |
| `oqtf-avec-delai-section` (F-IM-08-02) | Oui | **SF parallèle** SF-155-04-B1 |
| `oqtf-sans-delai-section` (F-IM-08-04) | Oui | **SF parallèle** SF-155-04-B2 |
| `annexe13-be-section` (F-IM-08-06) | Oui | **SF parallèle** SF-155-04-C |
| Autres composants décisionnels existants (F-DT-07, F-DT-08, etc.) | Non | Pattern déjà implémenté dans ces composants — pas de régression attendue |
| Panel F-IA-04 (`decisional-tools-panel`) | Oui — modification mineure | **Intégré** : ajout des 4 bindings IA (`aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`) sur l'entrée `F-DT-19-heures-sup` |

### Décision

- [x] Étendu à la cible primaire (composant `heures-sup-section`) + intégration `TOOL_REGISTRY`.
- [x] Subfeatures parallèles SF-155-04-A1/A2/B1/B2/C créées pour les 5 autres composants.
- [x] Non applicable aux outils décisionnels antérieurs (déjà IA-compliant).

---

## Impact par domaine métier

- **Droit du travail (FR)** : impact direct — pré-fill IA de 4 champs numériques + alerte de cohérence sur taux horaire et heures sup.
- **Droit du travail (BE)** : pas d'impact direct — note discrète, champs BE restent saisis manuellement. La cohérence heures sup BE n'a pas d'équivalent dans l'analyse IA (concept FR : contingent annuel + majorations 25/50 %).
- **Immigration / Famille** : hors scope (SF parallèles B1/B2/C pour OQTF/Annexe 13 ; Famille non concernée par heures sup).

---

## Parité des domaines métier

N/A — cette SF est une **harmonisation** (amener un composant existant F-DT-19 à la conformité du pattern canonique). Aucune nouvelle capacité métier livrée. Les composants frères (harcèlement, inaptitude, OQTF, Annexe 13) sont couverts par les SFs parallèles A1/A2/B1/B2/C.

---

## Critères d'acceptation

- [ ] `HeuresSupSectionComponent` expose `@Input() aiData?: TravailExtractedData | null`, `@Input() procedureChecks?: ProcedureCheck[] | null`, `@Input() aiQuestions?: AiQuestion[] | null`, `@Input() piecesManquantes?: PieceManquanteEntry[] | null`.
- [ ] Une méthode privée `prefillFromAi()` est invoquée dans `ngOnInit()` (fallback 404) ET dans `ngOnChanges(aiData)` (si `showForm() && !result()`).
- [ ] `prefillFromAi()` calcule `tauxHoraireBrut = salaireBrutMensuel / 151.67` (FR uniquement) uniquement si `salaireBrutMensuel > 0`, arrondi à 2 décimales.
- [ ] `prefillFromAi()` affecte `heuresSupDeclarees25pct`, `heuresSupDeclarees50pct`, `heuresHorsContingent` depuis `heuresSupMentionneesDansDossier` (garde-fou `typeof === 'object'`).
- [ ] 4 signals `provenanceTauxHoraire`, `provenanceHeures25`, `provenanceHeures50`, `provenanceHorsContingent` existent, type `signal<'IA' | null>(null)`.
- [ ] 4 handlers `onTauxHoraireChange`, `onHeures25Change`, `onHeures50Change`, `onHorsContingentChange` remettent la provenance correspondante à `null`. Ils sont branchés dans le template.
- [ ] Template : badge `<span class="provenance-note"><mat-icon>auto_awesome</mat-icon> Pré-rempli depuis l'analyse</span>` à côté de chaque champ quand la provenance IA est active.
- [ ] Computed `coherenceAlerts` expose les clés `TAUX_HORAIRE`, `HEURES_SUP`, `SALAIRE_DEDUIT` avec détection décrite ci-dessus.
- [ ] Computed `alertsSummary` expose `{ total, blockers: 0 }`.
- [ ] Directive `CoherencePopoverTriggerDirective` utilisée sur les badges d'alerte des champs concernés.
- [ ] Dossier BE : note "Pré-fill IA heures sup disponible uniquement pour la France" visible au-dessus du form ; aucune valeur pré-remplie ; aucune alerte IA.
- [ ] `TOOL_REGISTRY` entrée `F-DT-19-heures-sup` étendue : `aiData: ctx.synthesis?.travailExtractedData`, `procedureChecks: ctx.procedureChecks`, `aiQuestions: ctx.aiQuestions`, `piecesManquantes: ctx.synthesis?.piecesManquantesDetails`.
- [ ] Tests unitaires : ≥ 15 tests dans `heures-sup-section.component.spec.ts` couvrant pré-fill complet FR, pré-fill null BE, calcul taux horaire, effacement badge au changement manuel, divergence heures sup, divergence taux horaire, note `salaireEstDeduit`, pré-fill partiel (1 champ IA), ngOnChanges re-prefill sans écraser manuel, loadExisting priorise persistance, fixture malformée graceful.
- [ ] `npm run build` vert, `npx tsc --noEmit` vert, `npx jest heures-sup-section` 100 % passant.

---

## Périmètre

### Hors scope (explicite)

- Aucune modification des SF parallèles A1/A2/B1/B2/C (composants distincts).
- Aucune modification backend (record, prompt, extraction) — tout est mergé en palier 1.
- Aucune modification des endpoints `/heures-sup` (POST/GET inchangés).
- Aucune modification de `CaseAnalysisResult` ou autre DTO (les champs consommés existent déjà).
- Pas de nouveau design system (badge + directive existants réutilisés).
- Aucune refonte de la logique de validation `formValid()` existante.
- Aucune modification des 10 autres composants décisionnels déjà IA-compliants.

---

## Valeurs initiales

N/A — la SF modifie un composant frontend existant, pas de création d'entité.

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `tauxHoraireBrut` (pré-fill) | Non | — | nombre > 0, arrondi à 2 décimales via `Math.round(x * 100) / 100` | Non | Appliquée uniquement si `salaireBrutMensuel > 0` |
| `heuresSupDeclarees25pct` (pré-fill) | Non | — | entier ≥ 0 | Non | Garde-fou `typeof === 'number'` |
| `heuresSupDeclarees50pct` (pré-fill) | Non | — | entier ≥ 0 | Non | idem |
| `heuresHorsContingent` (pré-fill) | Non | — | entier ≥ 0 | Non | idem |

Notes :
- La constante `HEURES_MOIS_FR = 151.67` correspond à la durée légale française mensualisée (35 h × 52 semaines / 12 mois).
- Les seuils de détection d'alerte (10 % pour taux horaire, 5 h absolues + 50 % relatif pour heures sup) sont inspirés de `anciennete-section` (pattern similaire).

---

## Technique

### Endpoint(s)

Aucun endpoint modifié. La SF étend uniquement le composant frontend et l'entrée `TOOL_REGISTRY` du panel.

### Tables impactées

Aucune.

### Migration Liquibase

- [ ] Non applicable — SF frontend pure.

### Composants Angular

- `HeuresSupSectionComponent` (`frontend/src/app/case-files/heures-sup-section/`) — étendu avec 4 Inputs IA + `prefillFromAi()` + 4 signals provenance + 4 handlers onChange + coherenceAlerts computed + template badges + scss provenance/coherence.
- `DecisionToolsPanelComponent` (`frontend/src/app/case-files/decisional-tools-panel/`) — entrée `F-DT-19-heures-sup` étendue (ajout 4 bindings).

### Pattern de référence

**Pattern canonique IA** : `immigration-title-decision-section.component.ts` — `prefillFromAi()` + signals `provenance<Field>` + handlers `onXxxChange()` + `coherenceAlerts` computed + template badges `auto_awesome` + directive `CoherencePopoverTriggerDirective`.

**Pattern secondaire** : `anciennete-section.component.ts` — seuils de divergence relative (`percentDiff`) et gestion `aiDataSignal` + `ngOnChanges`.

---

## Plan de test

### Tests unitaires (Jest / Jasmine)

1. `charge l'analyse existante FR si présente (GET 200)` — inchangé, doit passer.
2. `reste en mode formulaire si GET 404` — inchangé, doit passer.
3. `FR : formValid false si taux ≤ 0 ou aucune heure saisie` — inchangé.
4. `FR : formValid false si tauxMajoration hors [10, 50]` — inchangé.
5. `BE : formValid true si taux > 0 et au moins une heure > 0` — inchangé.
6. `FR calculate() POST + affiche résultat + snackbar succès` — inchangé.
7. `BE calculate() POST envoie uniquement les champs BE` — inchangé.
8. `calculate() erreur backend → snackbar rouge` — inchangé.
9. `calculate() ignoré si form invalide (pas d'appel HTTP)` — inchangé.
10. **[NEW] FR prefillFromAi complet** — `aiData = { salaireBrutMensuel: 3034, heuresSupMentionneesDansDossier: { totalDeclarees25pct: 10, totalDeclarees50pct: 5, horsContingent: 2 } }` → `tauxHoraireBrut ≈ 20`, `heures25pct = 10`, `heures50pct = 5`, `horsContingent = 2`, les 4 signals provenance = `'IA'`.
11. **[NEW] FR prefillFromAi partiel** — seul `heuresSupMentionneesDansDossier.totalDeclarees25pct = 8` renseigné → uniquement `heures25pct = 8` + `provenanceHeures25 = 'IA'`, autres provenance null.
12. **[NEW] FR calcul tauxHoraireBrut correct** — `salaireBrutMensuel = 1821.04` → `tauxHoraireBrut = 12.00` (arrondi 2 décimales).
13. **[NEW] FR prefillFromAi — salaireBrutMensuel absent** — pas de `tauxHoraireBrut` pré-rempli, `provenanceTauxHoraire` null.
14. **[NEW] BE prefillFromAi null** — `workspaceCountry = 'BELGIQUE'`, même `aiData` complet → aucun pré-fill, toutes provenance null.
15. **[NEW] Modification manuelle → badge disparaît** — après prefill, `onTauxHoraireChange()` passe `provenanceTauxHoraire` à null.
16. **[NEW] coherenceAlerts — divergence taux horaire** — avocat saisit `tauxHoraireBrut = 30`, IA dérive 20 (écart 50 %) → `coherenceAlerts().TAUX_HORAIRE` défini.
17. **[NEW] coherenceAlerts — divergence heures sup** — IA = 10h total, avocat saisit 40+10+0 = 50h → `coherenceAlerts().HEURES_SUP` défini.
18. **[NEW] coherenceAlerts — salaireEstDeduit=true produit note info** — `aiData.salaireEstDeduit === true` + `salaireBrutMensuel` présent → `coherenceAlerts().SALAIRE_DEDUIT` présent.
19. **[NEW] ngOnChanges re-prefill mais pas d'écrasement manuel** — après modification manuelle de `tauxHoraireBrut`, réception d'un nouveau `aiData` ne doit PAS ré-écraser la valeur saisie (car `showForm() && !result()` mais provenance null → skip pour ce champ spécifique).
20. **[NEW] loadExisting priorise persistance** — si GET 200 retourne un résultat, `prefillFromAi` n'est pas appelé.
21. **[NEW] Fixture malformée graceful** — `heuresSupMentionneesDansDossier = 42` (non-objet) → pas d'exception, aucun champ pré-rempli.

### Tests d'intégration

N/A — SF frontend pure ; tests backend déjà couverts par SF-155-04-00-BE-travail.

### Isolation workspace

- [x] Non applicable — la SF ne touche pas aux requêtes HTTP, seulement aux bindings et à la logique de pré-fill dans le composant. L'isolation workspace est déjà garantie par les endpoints `/api/v1/case-files/{id}/heures-sup` (GET/POST) inchangés.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — SF frontend limitée à un composant + 1 ligne dans le panel.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `HeuresSupSectionComponent` (lui-même) | Extension Inputs, ajout pattern IA | Spec complète (21 tests) |
| `DecisionToolsPanelComponent` | 1 entrée `TOOL_REGISTRY` étendue avec 4 bindings additionnels | Spec existante doit rester verte |
| Autres composants décisionnels | Aucun — SFs parallèles A1/A2/B1/B2/C touchent les leurs | N/A |

### Smoke tests E2E concernés

- [x] Aucun smoke test E2E concerné — SF frontend pure sans impact auth/workspace/navigation.

---

## Dépendances

### Subfeatures bloquantes

- **SF-155-04-00-BE-travail** (backend palier 1 — PR #518 mergée) — expose `heuresSupMentionneesDansDossier` + `salaireBrutMensuel` + `salaireEstDeduit` dans `TravailExtractedData`. **Done.**

### Subfeatures parallèles (pas bloquantes mais synchronisées)

- SF-155-04-A1 (harcèlement), SF-155-04-A2 (inaptitude), SF-155-04-B1 (OQTF avec délai), SF-155-04-B2 (OQTF sans délai), SF-155-04-C (Annexe 13 BE) : chaque SF sur sa propre branche + composant distinct. Le seul point de conflit potentiel est le `TOOL_REGISTRY` de `decisional-tools-panel` ; en cas de conflit au push, rebase et garder tous les bindings IA.

### Subfeatures débloquées par celle-ci

- F-155 palier 2 (frontend) terminé quand A1/A2/A3/B1/B2/C sont toutes mergées.

### Questions ouvertes impactées

- [ ] Aucune question de `docs/OPEN_QUESTIONS.md` tranchée ou impactée.

---

## Notes et décisions

- **Choix 1** — Constante `HEURES_MOIS_FR = 151.67` codée en dur (cohérente avec le code du travail français : L.3121-27 durée légale 35 h/semaine × 52 / 12). Pas de config externe nécessaire pour V1.
- **Choix 2** — Arrondi `tauxHoraireBrut` à 2 décimales (centime d'euro) via `Math.round(x * 100) / 100` — aligné sur le format des rémunérations dans les bulletins de paie.
- **Choix 3** — Seuil divergence taux horaire **10 %** : plus laxe que `anciennete-section` (5 %) car le brut déduit d'un net peut varier selon le coefficient (30 %, mais peut être 33 % selon cotisations). Évite trop de faux positifs.
- **Choix 4** — Seuil divergence heures sup : **écart absolu ≥ 5 h ET écart relatif ≥ 50 %** — double garde-fou pour éviter un warning sur 2 h de différence (bruit typique de lecture bulletin).
- **Choix 5** — `SALAIRE_DEDUIT` est une **note info** (pas une alerte warning) — le taux horaire dérivé reste valide, juste moins fiable. Rendu avec icône `info_outline` + couleur navy (pas or).
- **Choix 6** — BE : pas de pré-fill ni coherence alert IA pour les heures sup, car le backend ne remplit pas ces champs pour des dossiers BE (concept FR-specific : contingent annuel 220 h + majorations 25/50 %). La note discrète prévient l'avocat et évite confusion.
- **Choix 7** — Handlers `onXxxChange` : un par champ IA (4 total). Ils sont branchés via `(ngModelChange)` dans le template en complément de `.set($event)` existant, ex. `(ngModelChange)="tauxHoraireBrut.set($event); onTauxHoraireChange()"`. Alternative : regrouper dans un seul handler générique `onFieldChange(field)` — rejetée car moins explicite lors de review.
- **Choix 8** — `ngOnChanges` : guard `if (this.showForm() && !this.result())` évite d'écraser un calcul persisté. Après modification manuelle d'un champ, la prochaine arrivée d'`aiData` **n'écrase pas** ce champ (les provenance nulls signalent les modifications utilisateur).
- **Choix 9** — Pattern `aiDataSignal` interne (comme `anciennete-section`) pour exposer les valeurs IA dans les `computed` sans dépendre du `@Input` directement (qui ne déclenche pas `computed`).
