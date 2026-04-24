# Mini-spec — F-155 / SF-155-04-A2 Inaptitude : pré-fill IA + validation F-IA-03

## Identifiant

`F-155 / SF-155-04-A2`

## Feature parente

`F-155` — Audit cohérence composants décisionnels frontend + template canonique

## Statut

`ready`

## Date de création

2026-04-24

## Branche Git

`feat/SF-155-04-A2-inaptitude-prefill-ia`

---

## Objectif

Mettre en conformité le composant `inaptitude-section` (F-DT-15-02) avec le pattern canonique IA (`immigration-title-decision-section`, F-IM-05) en lui ajoutant le pré-remplissage IA de ses 5 champs (salaire, ancienneté, origine inaptitude, date avis médecin, reclassement respecté) + la validation F-IA-03 (badges provenance + alertes de cohérence au changement avocat).

---

## Contrat API (consommé, pas modifié)

- Source IA : `synthesis.travailExtractedData` (record backend `TravailExtractedData` étendu par SF-155-04-00-BE-travail, PR #518 mergée).
  - `salaireBrutMensuel?: number | null` → pré-fill `salaireMensuelReference`.
  - `dateEntree?: string | null` (YYYY-MM-DD historique) → calcul dérivé `ancienneteAnnees`.
  - `origineInaptitudePressentie?: 'ACCIDENT_TRAVAIL' | 'MALADIE_PROFESSIONNELLE' | 'MALADIE_ORDINAIRE' | null` → pré-fill `origineInaptitude` (mapping vers `PROFESSIONNELLE` / `NON_PROFESSIONNELLE`).
  - `avisMedecinTravailDate?: string | null` (YYYY-MM-DD) → pré-fill `avisMedecinTravailDate`.
  - `reclassementRespecteDetected?: DetectedAnswer | null` → pré-fill `reclassementRespecte` (OUI → true, NON → false, INCONNU → laisser défaut false).
  - `salaireEstDeduit?: boolean | null` → note info (IA a déduit salaire net × 1,30).
- Sources complémentaires optionnelles : `procedureChecks: ProcedureCheck[]`, `aiQuestions: AiQuestion[]`, `piecesManquantes: PieceManquanteEntry[]` (F-IA-03 compatible).

Endpoint `POST /api/v1/case-files/{id}/inaptitude` inchangé — payload strictement identique.

---

## Comportement attendu

### Cas nominal

1. Le panel F-IA-04 (`decisional-tools-panel`) passe au composant `aiData = ctx.synthesis?.travailExtractedData`, `procedureChecks`, `aiQuestions`, `piecesManquantes = ctx.synthesis?.piecesManquantesDetails`.
2. `ngOnInit()` appelle `loadExisting()` qui :
   - **Succès GET 200** (analyse déjà persistée) : prefill depuis la réponse backend (comportement actuel), pas d'écrasement IA. `showForm=false`, `result` affiché.
   - **Échec GET 404** (pas d'analyse) : appelle `prefillFromAi()`, reste en mode formulaire.
3. `ngOnChanges()` : si `aiData` change, `showForm=true` et pas de `result` persisté → re-applique `prefillFromAi()`.
4. `prefillFromAi()` remplit champs + positionne `provenance<Field>='IA'` pour ceux pré-remplis.
5. Chaque `onXxxChange()` manuel avocat remet la provenance correspondante à `null`.
6. `coherenceAlerts` computed signal produit une alerte par champ divergent (salaire / origine / date avis / reclassement).
7. `calculate()` POST : inchangé, mêmes champs envoyés que précédemment.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `aiData = null` | `prefillFromAi()` no-op, form vide | — |
| `aiData.origineInaptitudePressentie` hors enum FR (`ACCIDENT_TRAVAIL`/`MALADIE_PROFESSIONNELLE`/`MALADIE_ORDINAIRE`) | Skip mapping, `origineInaptitude` reste null, pas de badge IA | — |
| `aiData.avisMedecinTravailDate` format non ISO | Skip, `avisMedecinTravailDate` reste null | — |
| `aiData.reclassementRespecteDetected.reponse = 'INCONNU'` | Laisser défaut `reclassementRespecte=false`, pas de badge IA | — |
| Divergence salaire avocat vs IA > 10 % | Alerte cohérence inline (icône warning, popover explicatif) | — |
| Divergence origine inaptitude avocat vs IA | Alerte cohérence inline | — |
| Divergence date avis médecin | Alerte cohérence inline | — |
| Contradiction `reclassementRespecte=true` avocat mais `aiData.reclassementRespecteDetected.reponse='NON'` | Alerte cohérence inline (risque motif) | — |
| `aiData.salaireEstDeduit === true` | Note info sur champ salaire (pas une alerte, indicateur) | — |
| POST backend 4xx/5xx | Comportement actuel conservé (snackbar rouge) | 400/500 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : 5 autres SFs frontend F-155-04 tournent en parallèle (A1 harcèlement, A3 heures-sup, B1 oqtf-avec-delai, B2 oqtf-sans-delai, C annexe13-be). Chacune sur son propre composant isolé. Seul point de conflit potentiel : `decisional-tools-panel.component.ts` (TOOL_REGISTRY) — rebasable.
- [x] **Autres pays** : France uniquement. Dossiers BE : les 5 champs pré-fill restent `null` (voir note mini-spec backend SF-155-04-00-BE-travail §2.2). Le composant conserve son switch FR/BE existant pour les origines (`ORIGINES_FR` / `ORIGINES_BE`) — le prefill ne s'applique qu'aux origines FR.
- [x] **Autres domaines** : DROIT_FAMILLE et DROIT_IMMIGRATION non concernés (outil dédié droit du travail).
- [x] **Autres UI patterns** : pré-fill IA + F-IA-03 déjà établis par `immigration-title-decision-section` (F-IM-05 SF-IM-05-04), `licenciement-section` (F-DT-08), `rupture-conv-section` (F-DT-10), `indemnite-comparatif-section` (F-DT-09), `anciennete-section` (F-DT-07). Ce SF aligne un 6e outil sur le pattern.
- [x] **Autres flows transversaux** : aucun. Pas d'auth, pas de workspace, pas de navigation.

### Niveaux de vérification

- [x] **Modèle TypeScript** — `case-analysis.model.ts` déjà étendu (5 champs + `HeuresSupMentionnees`) par PR #518.
- [x] **Record / DTO backend** — `TravailExtractedData` déjà étendu (PR #518).
- [x] **Service / logique métier** — inchangé (POST/GET `/inaptitude`).
- [x] **Entité JPA + schéma DB** — inchangé.
- [x] **Tests existants** — `inaptitude-section.component.spec.ts` à étendre.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `harcelement-licenciement-nul-section` (SF A1) | Non | SF parallèle — branche séparée, pas de conflit fichier |
| `heures-sup-section` (SF A3) | Non | SF parallèle — branche séparée, pas de conflit fichier |
| `oqtf-avec-delai-section` (SF B1) | Non | SF parallèle — branche séparée, pas de conflit fichier |
| `oqtf-sans-delai-section` (SF B2) | Non | SF parallèle — branche séparée, pas de conflit fichier |
| `annexe13-be-section` (SF C) | Non | SF parallèle — branche séparée, pas de conflit fichier |
| `decisional-tools-panel.component.ts` (TOOL_REGISTRY) | Oui | Modifié dans cette SF — si conflit au rebase, conserver TOUS les bindings IA des 6 entrées F-155-04 |
| Pattern canonique `immigration-title-decision-section` | Oui | Référence — signals, computed alerts, handlers, directive, palette importés |
| Dossiers BE (origines BE existantes) | Oui | Préservé — le switch FR/BE existant reste, prefill no-op si workspaceCountry=BELGIQUE ou champ IA null |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (composant + tests + TOOL_REGISTRY).
- [x] Subfeature(s) parallèle(s) créée(s) — A1, A3, B1, B2, C en cours sur d'autres worktrees.
- [ ] Backlog — n/a.
- [x] Non applicable — autres composants hors scope (gérés par leur SF).

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : intégrée via `CoherencePopoverTriggerDirective` sur champs salaire/origine/date/reclassement.
- [x] **Refresh dashboard (F-IA-02)** : `CaseDashboardRefreshService.triggerRefresh()` déjà présent dans `calculate()` (inchangé).
- [x] **Pré-remplissage IA** : implémenté (5 champs + 1 dérivé).
- [x] **Persistance des inputs** : inchangée (déjà via endpoint existant qui re-lit via GET).
- [x] **Masquage conditionnel selon type** : géré par panel F-IA-04 (visibilité existante).
- [x] **Alertes actives après calcul** : `coherenceAlerts` gate sur `showForm()` uniquement (pas `|| this.result()` — bug SF-IA-03-12 évité).

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable. La SF **consomme** un pattern existant (`CoherencePopoverTriggerDirective`, signal `provenance<Field>`, computed `coherenceAlerts`). Aucun nouveau pattern UI ni service partagé n'est introduit.

---

## Impact par domaine métier

Le composant `inaptitude-section` est **spécifique au droit du travail FR**.
- **Droit du travail FR** : 5 champs pré-remplis, 4 alertes cohérence activées.
- **Droit du travail BE** : composant accessible (`workspaceCountry=BELGIQUE` + origines BE), mais `prefillFromAi()` ne mappe pas (l'enum `origineInaptitudePressentie` couvre uniquement FR — cohérent avec mini-spec backend §2.2). Les dossiers BE garderont un formulaire vierge jusqu'à extension future du prompt BE.
- **Droit immigration** : non concerné — outil dédié droit du travail.
- **Droit famille** : non concerné — outil dédié droit du travail.

Cette asymétrie BE est **tracée** dans la mini-spec backend SF-155-04-00-BE-travail et acceptable à ce stade (l'inaptitude belge n'a pas la même base juridique — article L.1226-14 FR, CCT BE différent).

## Parité des domaines métier

Niveau de l'outil : (3) Calculateur — niveau < 5. Parité ≥ niveau 5 non requise par la règle CLAUDE.md.

Pour information, les 3 calculateurs métier sont livrés en parallèle et chacun aura son propre outil :
- Droit du travail : F-DT-15 inaptitude (cet outil) + F-DT-11 harcèlement + F-DT-19 heures sup.
- Droit famille : F-FA-05 partage immobilier.
- Droit immigration : F-IM-08 OQTF / Annexe 13.

---

## Critères d'acceptation

- [ ] `InaptitudeSectionComponent` expose `@Input() aiData?: TravailExtractedData | null`, `@Input() procedureChecks?: ProcedureCheck[] | null`, `@Input() aiQuestions?: AiQuestion[] | null`, `@Input() piecesManquantes?: PieceManquanteEntry[] | null`.
- [ ] 5 signals `provenance<Field>` : `provenanceSalaire`, `provenanceAnciennete`, `provenanceOrigineInaptitude`, `provenanceAvisMedecinDate`, `provenanceReclassement` (type `signal<'IA' | null>(null)`).
- [ ] Méthode privée `prefillFromAi()` :
  - `salaireMensuelReference` ← `aiData?.salaireBrutMensuel` si présent (> 0) et courant null.
  - `ancienneteAnnees` ← calcul depuis `aiData?.dateEntree` jusqu'à `new Date()`, arrondi entier inférieur, ≥ 0.
  - `origineInaptitude` ← mapping `origineInaptitudePressentie` : `ACCIDENT_TRAVAIL` | `MALADIE_PROFESSIONNELLE` → `PROFESSIONNELLE`, `MALADIE_ORDINAIRE` → `NON_PROFESSIONNELLE`. Autres / null → skip.
  - `avisMedecinTravailDate` ← `aiData?.avisMedecinTravailDate` si format YYYY-MM-DD valide.
  - `reclassementRespecte` ← `aiData?.reclassementRespecteDetected?.reponse === 'OUI'` ? true : `=== 'NON'` ? false : laisser actuel.
- [ ] Méthode invoquée dans `ngOnInit()` uniquement si `load()` renvoie 404 (pas d'écrasement si analyse persistée) ET dans `ngOnChanges(changes.aiData)` si `showForm() && !result()`.
- [ ] 5 handlers `onSalaireChange()`, `onAncienneteChange()`, `onOrigineChange()`, `onAvisMedecinDateChange()`, `onReclassementChange()` remettent la provenance correspondante à `null`.
- [ ] Template : badge `<mat-icon>auto_awesome</mat-icon> Pré-rempli depuis l'analyse` affiché à côté de chaque champ avec provenance=IA (classe `.prefilled-from-ai` ou `.provenance-note`).
- [ ] Computed signal `coherenceAlerts` exposant `salaire | origine | avisDate | reclassement` (type `Partial<Record<InaptitudeAlertField, InaptitudeCoherenceAlert>>`).
- [ ] Directive `CoherencePopoverTriggerDirective` appliquée sur les badges d'alerte (champs salaire, origine, date avis, reclassement).
- [ ] Alertes cohérence couvertes :
  - divergence salaire IA vs saisi > 10 %.
  - divergence `origineInaptitudePressentie` mappée vs valeur avocat.
  - divergence `avisMedecinTravailDate` IA vs saisi.
  - contradiction `reclassementRespecte=true` avocat vs `reclassementRespecteDetected.reponse='NON'`.
  - note info si `salaireEstDeduit=true` (badge info, pas une alerte bloquante).
- [ ] `TOOL_REGISTRY` entrée `F-DT-15-inaptitude` mise à jour avec `aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`.
- [ ] ≥ 15 tests Jest passants couvrant : prefill complet, prefill partiel, prefill ignore enum hors whitelist, prefill skip si GET 200 succès (pas d'écrasement), onChange manuel clear badge, divergence salaire > 10 %, pas d'alerte salaire si écart < 10 %, divergence origine, divergence date, contradiction reclassement, `salaireEstDeduit=true` affiche note info, fixture sans aiData, re-prefill sur ngOnChanges, calcul ancienneté depuis dateEntree, origine hors mapping ignorée.
- [ ] Builds verts : `npm run build`, `npx tsc --noEmit -p tsconfig.app.json`, `npx jest inaptitude-section`.

---

## Périmètre

### Hors scope (explicite)

- Aucune modification backend (record, prompt, service métier, endpoint).
- Aucune modification des 5 autres composants SF-155-04 (A1, A3, B1, B2, C).
- Aucune modification de `inaptitude.model.ts` (types).
- Aucune extension de l'enum BE `origineInaptitudePressentie` (hors scope — géré hors F-155).
- Aucune migration Liquibase.

---

## Valeurs initiales

N/A. La SF ne crée aucune entité. Les signals par défaut : `provenance*` = `null`.

---

## Contraintes de validation

Aucun nouveau champ utilisateur. Contraintes héritées de la SF d'origine (F-DT-15-02) :
- `salaireMensuelReference` > 0.
- `ancienneteAnnees` ∈ entier ≥ 0.
- `origineInaptitude` ∈ enum `OrigineInaptitude`.
- `avisMedecinTravailDate` optional, YYYY-MM-DD.

---

## Technique

### Endpoint(s)

Inchangé — `POST/GET /api/v1/case-files/{id}/inaptitude`.

### Tables impactées

Aucune.

### Migration Liquibase

- [ ] Oui
- [x] Non applicable

### Composants Angular

- `InaptitudeSectionComponent` — composant principal (ajout @Inputs + signals + méthodes + template).
- `DecisionToolsPanelComponent` — mise à jour 1 entrée TOOL_REGISTRY.

---

## Plan de test

### Tests unitaires (Jest / Karma)

1. **prefill complet FRANCE** : `aiData` avec 5 champs remplis → champs pré-remplis + 5 signals provenance = IA.
2. **prefill partiel** : `aiData` avec seulement `salaireBrutMensuel` et `origineInaptitudePressentie` → seuls ces 2 pré-remplis, autres provenance null.
3. **prefill pas d'écrasement si GET 200** : `load()` succès → pas de `prefillFromAi()`, aucun badge IA.
4. **origine hors enum whitelist ignorée** : `origineInaptitudePressentie = 'FOO_BAR'` → origine reste null, pas de badge.
5. **prefill ignore si salaireBrutMensuel ≤ 0** : `salaireBrutMensuel = 0` → pas de prefill salaire.
6. **modif manuelle salaire clear badge** : prefill IA → `onSalaireChange()` → `provenanceSalaire = null`.
7. **idem pour les 4 autres champs** (1 test chacun = 4 tests) : `onOrigineChange`, `onAvisMedecinDateChange`, `onReclassementChange`, `onAncienneteChange`.
8. **divergence salaire > 10 %** : IA=3000, avocat=3400 (~13 %) → `coherenceAlerts().salaire` défini.
9. **pas d'alerte salaire si écart ≤ 10 %** : IA=3000, avocat=3200 → pas d'alerte.
10. **divergence origine inaptitude** : IA → PROFESSIONNELLE, avocat saisit NON_PROFESSIONNELLE → alerte origine.
11. **divergence date avis** : IA=2026-01-10, avocat=2026-02-01 → alerte avisDate.
12. **contradiction reclassement** : `reclassementRespecteDetected.reponse='NON'`, avocat `reclassementRespecte=true` → alerte reclassement.
13. **salaireEstDeduit=true** : aiData avec `salaireEstDeduit=true` → signal/computed expose la note info.
14. **fixture sans aiData** : aiData null + GET 404 → form vide, pas de provenance, pas d'alertes.
15. **re-prefill sur ngOnChanges** : simulate 2e tick avec aiData changé → re-prefill effectue.
16. **calcul ancienneté depuis dateEntree** : `dateEntree='2020-01-01'`, today 2026-04-24 → `ancienneteAnnees=6`.

### Tests d'intégration

Non applicable (SF frontend pure, endpoint inchangé). Les tests backend `InaptitudeCalculatorIT` restent verts.

### Isolation workspace

- [ ] Applicable
- [x] Non applicable — composant frontend, isolation gérée par l'endpoint existant.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — SF frontend isolée au composant inaptitude.

### Composants / endpoints potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `DecisionToolsPanelComponent` | Ajout bindings dans entrée TOOL_REGISTRY (1 entrée sur 21) | Tests existants `decisional-tools-panel.component.spec.ts` doivent rester verts |
| Tests existants `inaptitude-section.component.spec.ts` | 9 tests conservés — non cassés par les ajouts | Relance `npx jest inaptitude-section` |

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné (SF isolée, pas d'auth / workspace / navigation touchés).

---

## Dépendances

### Subfeatures bloquantes

- SF-155-04-00-BE-travail — `done` (PR #518 mergée, champs backend disponibles en synthesis).

### Subfeatures débloquées par celle-ci

- Aucune.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Choix 1** : le mapping `origineInaptitudePressentie` backend est **tripartite** (AT/MP/MO) mais l'enum frontend FR est **binaire** (PROFESSIONNELLE/NON_PROFESSIONNELLE). `ACCIDENT_TRAVAIL` ET `MALADIE_PROFESSIONNELLE` mappent vers `PROFESSIONNELLE` (article L.1226-14 couvre les deux), `MALADIE_ORDINAIRE` vers `NON_PROFESSIONNELLE`.
- **Choix 2** : `ancienneteAnnees` est calculée depuis `dateEntree` (déjà présent dans `TravailExtractedData` depuis F-DT-07), arrondi entier inférieur. Pas de pré-fill si `dateEntree` absent ou date future.
- **Choix 3** : la convention date `<input type="date">` existante est conservée (pas de changement de picker). Input valide seulement si format YYYY-MM-DD strict.
- **Choix 4** : seuil divergence salaire 10 % cohérent avec F-DT-07 SF-DT-07-04 (anciennete-section) et F-DT-08 (licenciement-section).
- **Choix 5** : `salaireEstDeduit=true` expose une **note info** (pas une alerte) car c'est une précision sur la méthode IA, pas une divergence.
- **Choix 6** : `reclassementRespecteDetected.reponse='INCONNU'` ne déclenche PAS de prefill (on laisse la valeur avocat inchangée) et PAS d'alerte (on ne sait rien).
- **Choix 7** : TOOL_REGISTRY : les 4 inputs (`aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`) sont ajoutés symétriquement aux autres outils travail (F-DT-08, F-DT-09, F-DT-10).
