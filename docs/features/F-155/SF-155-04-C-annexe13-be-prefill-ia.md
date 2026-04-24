# Mini-spec — F-155 / SF-155-04-C

## Identifiant

`F-155 / SF-155-04-C` — Annexe 13 BE : pré-fill IA + validation F-IA-03

## Feature parente

`F-155` — Audit cohérence composants décisionnels frontend + template canonique

## Statut

`draft`

## Date de création

2026-04-24

## Branche Git

`feat/SF-155-04-C-annexe13-be-prefill-ia`

---

## Objectif

Brancher le composant frontend `annexe13-be-section` (F-IM-08-annexe13-be) sur les 4 champs IA
backend `ImmigrationExtractedData.{dateNotificationAnnexe13, delaiDepartImposeJours,
motifOqtCodeBe, transfertImminentDetected}` livrés par SF-155-04-00-BE-immig-BE (PR #520), pour
que l'avocat reçoive un formulaire Annexe 13 pré-rempli depuis l'analyse IA + une alerte
critique "transfert imminent" + des alertes de cohérence par field, au même niveau de
conformité que le pattern canonique `immigration-title-decision-section`.

---

## Comportement attendu

### Cas nominal

1. Le panel F-IA-04 passe au composant 4 nouvelles entrées en plus du `caseFileId` /
   `workspaceCountry` existants : `aiData` (`ImmigrationExtractedData`), `procedureChecks`,
   `aiQuestions`, `piecesManquantes`.
2. Au `ngOnInit()` : si le dossier est BE, le composant appelle l'existant `load()` (GET
   Annexe 13). Si 404 → reste en mode formulaire → appelle `prefillFromAi()`. Si 200 →
   mode résultat, les champs persistés priment, pas de prefill (pattern cohérent avec
   `immigration-title-decision-section`).
3. `prefillFromAi()` remplit :
   - `dateNotificationAnnexe13.set(aiData.dateNotificationAnnexe13)` si présent (format
     YYYY-MM-DD — pas de parsing, passthrough).
   - `delaiDepartImposeJours.set(aiData.delaiDepartImposeJours)` si présent et entier ≥ 0.
     Sinon garde la valeur par défaut 30 (sans provenance IA).
   - `motifOqt.set(aiData.motifOqtCodeBe)` **uniquement** si la valeur est dans la whitelist
     front `MOTIFS_OQT` (4 codes : `SEJOUR_IRREGULIER_ART_7`, `REFUS_SEJOUR_APRES_DEMANDE`,
     `FIN_SEJOUR_REGULIER`, `AUTRE`). Hors-liste → skip (pas de provenance, pas d'erreur).
   - `transfertImminent.set(aiData.transfertImminentDetected)` **uniquement** si boolean
     strict (`=== true` ou `=== false`). Null/undefined → skip.
   - Chaque champ pré-rempli met son signal `provenance<Field>` à `'IA'`.
4. Badge UI "Pré-rempli depuis l'analyse" (`auto_awesome`) à côté de chaque champ dont
   `provenance<Field>() === 'IA'`.
5. Dès que l'avocat modifie manuellement un champ (existing `(ngModelChange)`), un handler
   `onXxxChange()` remet la provenance à `null` → badge disparaît.
6. Alertes de cohérence exposées via `coherenceAlerts` computed signal :
   - **`TRANSFERT_IMMINENT` (CRITIQUE)** : si `aiData.transfertImminentDetected === true`
     ET `transfertImminent() === false` → alerte rouge "IA a détecté un risque de transfert
     imminent (centre fermé, escorte, vol programmé) — à vérifier". `blocker: true`.
   - `DATE_NOTIFICATION_ANNEXE13` : divergence non nulle entre IA date et saisie avocat
     (écart ≥ 1 jour). Warning.
   - `DELAI_DEPART` : divergence entière entre IA délai et saisie avocat (IA fournit 30,
     avocat saisit 7). Warning.
   - `MOTIF_OQT` : IA propose motif X dans la whitelist et avocat saisit motif Y ≠ X.
     Warning.
7. Directive `CoherencePopoverTriggerDirective` appliquée sur chaque field porteur d'alerte
   — hover → popover enrichi via `sourceExplanations` (map générique
   `SourceExplanationService.getForCaseFile`).
8. Gate `workspaceCountry` : comportement existant préservé (bannière info "BE uniquement"
   si FR). Les 4 champs IA ne sont même pas lus côté FR (gate dans `prefillFromAi`).
9. `TOOL_REGISTRY['F-IM-08-annexe13-be'].inputs(ctx)` étendu pour injecter les 4 nouvelles
   entrées du composant (sans casser les 2 existantes).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `aiData` null/undefined | `prefillFromAi()` no-op, pas de provenance, formulaire vierge |
| `aiData.motifOqtCodeBe` hors whitelist front (ex "INCONNU") | Motif non pré-rempli, pas d'alerte, pas d'erreur |
| `aiData.delaiDepartImposeJours` < 0 ou non-nombre | Délai non pré-rempli, valeur par défaut 30 reste |
| `aiData.transfertImminentDetected` = null | Pas de prefill, pas d'alerte transfert |
| `aiData.dateNotificationAnnexe13` chaîne arbitraire (ex "2026-04-") | Passthrough (cohérent avec autres dates) — le validateur template `type="date"` bloquera la soumission |
| FR workspace | Bannière info existante, pas de prefill tenté |
| Déjà résultat persisté (GET 200) | Pas de prefill (évite écrasement des valeurs saisies par l'avocat précédemment) |
| `aiData` change via `ngOnChanges` alors que `showForm() === false` (résultat déjà affiché) | Pas de re-prefill |
| `procedureChecks` / `aiQuestions` / `piecesManquantes` absents | `coherenceAlerts` renvoie `{}` ou uniquement alertes basées sur `aiData` |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils du même domaine (immigration)** : F-IM-05/06/07 déjà IA-compliant
  depuis F-150/F-151. F-IM-08-oqtf-avec-delai-fr (SF-155-04-B1) et F-IM-08-oqtf-sans-delai-fr
  (SF-155-04-B2) sont **SF parallèles** en cours de livraison (consomment les champs FR du
  record). Chacune sur son propre composant → **pas de conflit**. Périmètre : composant
  annexe13-be uniquement.
- [x] **Autres pays** : composant BE only. Gate `workspaceCountry === 'BELGIQUE'` déjà en
  place (bannière info si FR). L'OQTF FR est traitée par les 2 SFs jumelles B1/B2 — hors
  scope ici.
- [x] **Autres domaines métier (travail / famille)** : non applicable. Annexe 13 est un
  outil purement immigration BE. SF-155-04-A1/A2/A3 sont les SFs jumelles travail (6
  composants au total, 3 domaines, 2 pays).
- [x] **Autres UI patterns** : le pattern "provenance IA + badge + onChange clear +
  coherenceAlerts + CoherencePopoverTriggerDirective" est le **pattern canonique**
  formalisé dans `ai-skills/frontend-coherence-audit.md` section 5. Référence :
  `immigration-title-decision-section`. Pas de nouveau pattern partagé introduit — copie
  stricte du canonique.
- [x] **Autres flows transversaux** : pas d'auth, pas de workspace context changé, pas de
  plan/quota, pas de nouvelle route, pas de nouveau guard. Juste des bindings panel →
  composant.

### Niveaux de vérification

- [x] **Modèle TypeScript** — DTO `ImmigrationExtractedData` (case-analysis.model.ts) déjà
  étendu avec les 4 champs BE par SF-155-04-00-BE-immig-BE. Consommé tel quel.
- [x] **Service / logique métier** — aucune modification de `Annexe13BeService` frontend
  ni backend.
- [x] **Entité JPA + schéma DB** — aucun impact (pas de modification backend).
- [x] **Tests existants** — les 22 tests existants du spec doivent rester verts. 15+
  nouveaux tests ajoutés (cf. plan de test).

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable. La SF n'introduit pas de nouveau pattern. Elle **applique** un pattern
existant (canonique `immigration-title-decision-section`) à un composant qui en manquait.
La directive `CoherencePopoverTriggerDirective` et le service `SourceExplanationService`
sont réutilisés sans modification.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| SF-155-04-A1/A2/A3 (travail FR) | Non — SF jumelles en parallèle, composants différents | SF parallèle (non-conflictuelle) |
| SF-155-04-B1/B2 (OQTF FR) | Non — SF jumelles en parallèle, composants différents | SF parallèle (non-conflictuelle) |
| F-IM-05/06/07 (immigration déjà IA-compliant) | Non | Déjà conforme, aucune régression attendue |
| `decisional-tools-panel.component.ts` (TOOL_REGISTRY) | Oui | Intégré dans la SF — extension du binding `F-IM-08-annexe13-be` |
| `immigration-title-decision-section` (pattern canonique) | Oui | Référence lecture seule, imitation stricte |
| Directive `CoherencePopoverTriggerDirective` | Oui | Réutilisée sans modification |
| `SourceExplanationService` | Oui | Réutilisée sans modification |
| Risque conflit TOOL_REGISTRY avec SF parallèles | Oui | **Rebase post-merge** : chaque SF touche une entrée différente de la Map ; conflit mécanique résolu en gardant TOUTES les extensions |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature.
- [x] SFs parallèles (A1/A2/A3/B1/B2) tournent chacune sur leur propre composant — pas de
  duplication.
- [x] Pas d'item backlog à créer (SF-155-04-C termine le palier 2 frontend F-155).
- [x] Non applicable aux autres cibles — justifications ci-dessus.

---

## Critères d'acceptation

- [ ] `Annexe13BeSectionComponent` implémente `OnInit` ET `OnChanges`, expose 4 nouveaux
  `@Input()` : `aiData?: ImmigrationExtractedData | null`, `procedureChecks?: ProcedureCheck[] | null`,
  `aiQuestions?: AiQuestion[] | null`, `piecesManquantes?: PieceManquanteEntry[] | null`.
- [ ] Signals privés `aiDataSignal`, `procedureChecksSignal`, `aiQuestionsSignal`,
  `piecesManquantesSignal` mis à jour depuis les inputs dans `ngOnInit` et `ngOnChanges`
  (pattern canonique).
- [ ] Signals publics `provenanceDateNotification`, `provenanceDelaiDepart`,
  `provenanceMotifOqt`, `provenanceTransfertImminent` de type `'IA' | null`.
- [ ] Méthode privée `prefillFromAi()` invoquée :
    1. dans `ngOnInit()` **après** `this.service.get()` seulement **si GET 404** (logique
       existante `load()`) — pour ne pas écraser des valeurs persistées côté backend ;
    2. dans `ngOnChanges()` si `aiData` change ET `showForm() === true` ET `result() === null`.
- [ ] `prefillFromAi()` respecte les règles de whitelist / validation décrites en
  "Comportement attendu" section 3.
- [ ] Handlers `onDateNotificationChange()`, `onDelaiDepartChange()`, `onMotifOqtChange()`,
  `onTransfertImminentChange()` remettent la provenance correspondante à `null`. Intégrés
  au template via `(ngModelChange)` (en plus du `.set($event)` existant).
- [ ] Badges `<span class="provenance-note">` avec `<mat-icon>auto_awesome</mat-icon>` +
  label "Pré-rempli depuis l'analyse" affichés conditionnellement à côté des 4 champs
  lorsque leur `provenance<Field>()` vaut `'IA'`.
- [ ] `coherenceAlerts` computed signal expose un objet partiel typé `Partial<Record<AlertField, Alert>>`
  avec 4 fields possibles (`TRANSFERT_IMMINENT`, `DATE_NOTIFICATION`, `DELAI_DEPART`,
  `MOTIF_OQT`) — renvoie `{}` si `showForm() === false` (cohérent avec canonique).
- [ ] `TRANSFERT_IMMINENT` a `blocker: true` et classe CSS rouge (alerte critique —
  design system autorise le rouge pour les urgences). Les 3 autres alertes utilisent la
  classe `coherence-warning` navy/or.
- [ ] Directive `CoherencePopoverTriggerDirective` appliquée sur les 4 fields concernés
  dans le template, avec explications routées via `SourceExplanationService.getForCaseFile`
  (fail-open).
- [ ] Gate `workspaceCountry` : bannière info existante préservée (pattern `isBelgium()`
  conservé — pas de masquage silencieux).
- [ ] `TOOL_REGISTRY['F-IM-08-annexe13-be'].inputs(ctx)` étendu pour injecter
  `aiData: ctx.synthesis?.immigrationExtractedData`, `procedureChecks: ctx.procedureChecks`,
  `aiQuestions: ctx.aiQuestions`, `piecesManquantes: ctx.synthesis?.piecesManquantesDetails`
  en plus de `caseFileId` et `workspaceCountry`.
- [ ] Tous les tests existants du spec (22) restent verts.
- [ ] ≥ 15 nouveaux tests ajoutés (cf. plan de test) — tous verts.
- [ ] Build frontend vert (`ng build --configuration=production`), `tsc --noEmit` vert,
  lint vert.

---

## Périmètre

### Hors scope (explicite)

- Pas d'extension du DTO TypeScript `ImmigrationExtractedData` (déjà fait par
  SF-155-04-00-BE-immig-BE).
- Pas de modification du backend (calculateur, service, controller Annexe 13 inchangés).
- Pas de modification du template canonique `immigration-title-decision-section`.
- Pas de refonte des 10 divergences visuelles/comportementales déjà tracées dans
  `audit-2026-04-24.md` (hors scope).
- Pas de modification des SFs jumelles A1/A2/A3/B1/B2 (composants différents).
- Pas de nouveau composant partagé — imitation stricte du pattern canonique.
- Pas de modification du prompt système / des tests backend IA.

---

## Contraintes de validation

| Champ | Pré-fill IA si | Normalisation |
|-------|---------------|---------------|
| `dateNotificationAnnexe13` | `typeof aiData.dateNotificationAnnexe13 === 'string'` | passthrough |
| `delaiDepartImposeJours` | `typeof aiData.delaiDepartImposeJours === 'number'` ET `>= 0` ET entier | passthrough |
| `motifOqt` | `aiData.motifOqtCodeBe` ∈ whitelist `MOTIFS_OQT` codes | passthrough |
| `transfertImminent` | `typeof aiData.transfertImminentDetected === 'boolean'` | passthrough |

Whitelist `MOTIFS_OQT` :`SEJOUR_IRREGULIER_ART_7`, `REFUS_SEJOUR_APRES_DEMANDE`,
`FIN_SEJOUR_REGULIER`, `AUTRE` (4 codes alignés strictement sur
`Annexe13BeCalculator.MOTIFS_VALIDES` et `annexe13-be.model.ts` front).

---

## Technique

### Endpoint(s)

Aucun endpoint nouveau. Le composant consomme `Annexe13BeService.analyze()` et `get()`
existants (pas modifiés).

### Tables impactées

Aucune.

### Migration Liquibase

Non applicable.

### Composants Angular

| Composant | Opération |
|-----------|-----------|
| `annexe13-be-section.component.ts` | modifié (ajout inputs + prefill + provenance + coherenceAlerts + handlers) |
| `annexe13-be-section.component.html` | modifié (badges + directive popover + handlers onChange) |
| `annexe13-be-section.component.scss` | modifié (styles `.provenance-note`, `.coherence-badge`, `.coherence-alert-critical`) |
| `annexe13-be-section.component.spec.ts` | modifié (+15 tests) |
| `decisional-tools-panel.component.ts` | modifié (extension TOOL_REGISTRY F-IM-08-annexe13-be) |

### Référentiel métier

Non applicable.

---

## Impact par domaine métier

Cette SF est **sensible au domaine** : immigration BE uniquement (Annexe 13 / OQT belge,
art. 7 Loi 15/12/1980). Elle **ne s'applique pas** aux deux autres domaines (travail,
famille). L'équivalent FR (OQTF) est couvert par les SFs jumelles **SF-155-04-B1**
(avec délai, art. L.614-5 CESEDA) et **SF-155-04-B2** (sans délai, art. L.731-1 CESEDA).
Le droit du travail et la famille n'ont pas d'équivalent (pas d'outil "ordre de quitter
le territoire" hors immigration).

Parité des 3 domaines sur l'**axe pré-fill IA** pour les outils décisionnels livrés
2026-04-24 :
- Travail FR → SF-155-04-A1/A2/A3 (en parallèle).
- Immigration FR → SF-155-04-B1/B2 (en parallèle).
- Immigration BE → SF-155-04-C (cette SF).
- Famille → déjà conforme (F-152/F-153 livrés 2026-04-23 incluaient le pré-fill IA dès
  la conception).

---

## Plan de test

### Tests unitaires (≥ 15 nouveaux)

1. `prefillFromAi() — aiData complet BE → 4 champs + 4 provenance 'IA'`
2. `prefillFromAi() — aiData partiel (seule date) → date pré-remplie, autres sans provenance`
3. `prefillFromAi() — motifOqtCodeBe hors whitelist ("INCONNU") → motif non pré-rempli, pas d'erreur`
4. `prefillFromAi() — delaiDepartImposeJours négatif (-5) → délai reste 30 (défaut), pas de provenance`
5. `prefillFromAi() — delaiDepartImposeJours = 0 (cas OQT urgence) → délai pré-rempli 0 + provenance`
6. `prefillFromAi() — transfertImminentDetected null → pas de provenance, transfert reste false`
7. `prefillFromAi() — transfertImminentDetected false explicite → transfert = false + provenance IA`
8. `onXxxChange() — modifier manuellement date/delai/motif/transfert → provenance correspondante à null`
9. `coherenceAlerts — aiData.transfertImminentDetected=true + avocat transfertImminent=false → alerte CRITIQUE blocker=true`
10. `coherenceAlerts — IA délai=30 + avocat délai=7 → alerte DELAI_DEPART warning`
11. `coherenceAlerts — IA motif=SEJOUR_IRREGULIER_ART_7 + avocat motif=AUTRE → alerte MOTIF_OQT`
12. `coherenceAlerts — IA date='2026-04-01' + avocat date='2026-04-02' → alerte DATE_NOTIFICATION`
13. `coherenceAlerts — showForm=false → retourne {}`
14. `ngOnInit() — workspace FRANCE → prefillFromAi() pas appelé (gate isBelgium)`
15. `ngOnChanges() — aiData changé alors que result existe (showForm=false) → pas de re-prefill`
16. `loadExisting() GET 200 → résultat affiché, prefillFromAi non appelé (pas d'écrasement)`
17. `loadExisting() GET 404 → prefillFromAi appelé si aiData présent`
18. `fixture malformée (aiData.motifOqtCodeBe = 123 numeric) → prefill graceful skip`
19. `TOOL_REGISTRY['F-IM-08-annexe13-be'].inputs(ctx) — retourne aiData + procedureChecks + aiQuestions + piecesManquantes + caseFileId + workspaceCountry`
20. `délai négatif côté IA rappelle que le backend écarte aussi (test sentinelle doc — vérifie le champ reste null côté modèle)`

### Tests d'intégration

Non applicable (composant frontend, pas d'endpoint nouveau).

### Isolation workspace

Non applicable (le composant ne requête ni ne persiste aucune donnée workspace nouvelle —
il consomme `Annexe13BeService` existant qui filtre déjà par `caseFileId`).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Outil décisionnel métier** — modification de `annexe13-be-section` (F-IM-08-annexe13-be)
  pour ajout mécanismes IA (pré-fill + validation cohérence). Conforme à l'invariant "un
  outil = une situation métier" (Annexe 13 BE seule). Les autres outils décisionnels
  immigration (F-IM-05/06/07) sont déjà IA-compliant, aucun scan complémentaire
  nécessaire au-delà de celui fait en section "Analyse de cohérence transversale".
- [x] **Préoccupation transversale "Outil décisionnel métier"** — scan effectué :
  | Outil | Statut pré-fill IA | Action |
  |-------|-------------------|--------|
  | F-DT-07/08/09/10 | Déjà IA-compliant | Aucune |
  | F-DT-11/15/19 | FAIL (A1/A2/A3 en parallèle) | Hors scope (SF jumelles) |
  | F-IM-01/05/06/07 | Déjà IA-compliant | Aucune |
  | F-IM-08-oqtf-avec-delai-fr | FAIL (B1 en parallèle) | Hors scope (SF jumelle) |
  | F-IM-08-oqtf-sans-delai-fr | FAIL (B2 en parallèle) | Hors scope (SF jumelle) |
  | F-IM-08-annexe13-be | FAIL → corrigé ici | **Cette SF** |
  | F-FA-05/06/07, F-152/F-153 | Déjà IA-compliant | Aucune |

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact | Test de non-régression |
|----------------------|--------|------------------------|
| `decisional-tools-panel.component.ts` | Extension entrée Map (non-breaking — 2 inputs → 6 inputs) | Spec panel doit rester vert (pas de test attendu sur les clés de `inputs`) |
| `Annexe13BeService` | Aucun impact (méthode `analyze()`/`get()` non modifiées) | Tests existants verts |
| `SourceExplanationService` | Aucun (réutilisé) | Tests existants verts |
| `CoherencePopoverTriggerDirective` | Aucun (réutilisé) | Tests existants verts |

### Smoke tests E2E concernés

- [x] Aucun. La SF ne touche ni auth, ni workspace switch, ni navigation.

---

## Dépendances

### Subfeatures bloquantes

- `SF-155-04-00-BE-immig-BE` (mergée dans PR #520) — 4 champs BE dans DTO
  `ImmigrationExtractedData` + parsing backend + prompt IA.

### Subfeatures parallèles (non-conflictuelles)

- `SF-155-04-A1`, `SF-155-04-A2`, `SF-155-04-A3` (travail FR, composants différents).
- `SF-155-04-B1`, `SF-155-04-B2` (OQTF FR, composants différents).

Coordination : conflit unique possible au `push` dans `decisional-tools-panel.component.ts`
sur les entrées de la Map `TOOL_REGISTRY`. Résolution mécanique post-rebase (garder
**toutes** les extensions).

### Subfeatures débloquées

- Aucune directe. La complétion de SF-155-04-C + A1/A2/A3/B1/B2 termine F-155 palier 2
  frontend → F-155 passe `Terminée`.

### Questions ouvertes impactées

- [x] Aucune.

---

## Contrat API

Non applicable. La SF consomme des données déjà disponibles via `CaseAnalysisResult`
(GET `/api/v1/case-files/{id}/analysis`) et `Annexe13BeService` existant — aucun
endpoint nouveau, aucune modification de contrat.

---

## Notes et décisions

- **Choix 1 — 4 codes motif et pas 6** : le brief initial proposait 6 codes mais le
  calculateur backend `Annexe13BeCalculator.MOTIFS_VALIDES` n'accepte que 4 codes. La
  SF-155-04-00-BE-immig-BE (palier 1) a tranché en alignant l'IA sur les 4 codes. Cette
  SF frontend reprend strictement cette whitelist (variable `MOTIFS_OQT` du modèle
  annexe13-be.model.ts). Tout code IA hors-liste → pré-fill skipé silencieusement.
- **Choix 2 — ordre d'appel de `prefillFromAi()`** : après `load()` et uniquement si GET
  renvoie 404. Cohérent avec `immigration-title-decision-section` : si le backend a
  déjà un résultat persisté, on ne l'écrase jamais, l'avocat voit le résultat tel qu'il
  l'a validé auparavant. En mode "edit" (`editMode()`), le composant repasse en
  `showForm=true` mais le prefill n'est PAS réappelé (les valeurs existantes sont
  préservées — seul `ngOnChanges` avec nouveau `aiData` pourrait déclencher un prefill
  et encore uniquement si `!result()`).
- **Choix 3 — alerte `TRANSFERT_IMMINENT` en rouge** : `DESIGN_SYSTEM.md` autorise le
  rouge pour les urgences. L'Annexe 13 est la seule procédure avec placement en centre
  fermé et escorte — c'est le contexte d'urgence le plus critique du domaine immigration.
  La divergence "IA détecte transfert imminent, avocat n'a pas coché" est donc traitée
  comme un blocker visuel (classe `.coherence-alert-critical` rouge + `blocker: true`).
- **Choix 4 — provenance sur 4 signals (et non un objet)** : pattern canonique
  `immigration-title-decision-section` utilise 3 signals séparés (`provenanceMotif`,
  `provenanceSituationFamiliale`, `provenanceNationaliteUe`). On reproduit le même
  pattern avec 4 signals ici.
- **Choix 5 — handlers `onXxxChange()` en plus de `(ngModelChange)` existants** : le
  template actuel utilise `(ngModelChange)="dateNotificationAnnexe13.set($event)"`.
  On refactor minimalement en ajoutant un `; onDateNotificationChange()` ou en
  transformant en méthode dédiée.
- **Choix 6 — `SourceExplanationService` utilisé même si aucune clé métier Annexe 13
  n'existe encore côté backend** : fail-open — si la Map est vide, les popovers
  affichent une navigation par défaut vers la synthèse. Cohérent avec pattern canonique.
  Les clés spécifiques (`IM08_TRANSFERT`, `IM08_MOTIF_OQT`) pourront être ajoutées en
  backlog ultérieurement si nécessaire — hors scope ici.
