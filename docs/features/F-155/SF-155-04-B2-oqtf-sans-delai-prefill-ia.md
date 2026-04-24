# Mini-spec — F-155 / SF-155-04-B2 — OQTF sans délai pré-fill IA + validation F-IA-03 (urgence 48h)

## Identifiant

`F-155 / SF-155-04-B2`

## Feature parente

`F-155` — Audit cohérence composants décisionnels frontend + template canonique

## Statut

`ready`

## Date de création

2026-04-24

## Branche Git

`feat/SF-155-04-B2-oqtf-sans-delai-prefill-ia`

---

## Objectif

Brancher l'outil décisionnel `oqtf-sans-delai-section` (F-IM-08-04, urgence **48h JLD**) sur la synthèse IA — pré-remplissage automatique des champs date/heure de notification, motif sans délai, placement CRA et recours formé depuis `ImmigrationExtractedData` (étendue par SF-155-04-00-BE-immig-FR), avec deux alertes critiques de cohérence F-IA-03 pour éviter qu'un avocat laisse expirer silencieusement le délai de 48h.

---

## Contexte — criticité

Cet outil est identifié dans `audit-prefill-ia-2026-04-24.md` §3.5 comme **le cas le plus dangereux des 6** : un recours oublié = conséquences directes client (impossibilité de contester l'éloignement, placement en rétention sans recours suspensif). Le pré-fill IA + alerte cohérence sur le délai 48h sont donc la **priorité absolue** du palier 2 F-155.

---

## Comportement attendu

### Cas nominal

1. Le composant reçoit via le panel F-IA-04 `decisional-tools-panel` :
   - `aiData?: ImmigrationExtractedData | null` — synthèse IA immigration.
   - `procedureChecks?: ProcedureCheck[] | null` — checklist procédurale F-96.
   - `aiQuestions?: AiQuestion[] | null` — questions complémentaires F-IA-02.
   - `piecesManquantes?: PieceManquanteEntry[] | null` — pièces manquantes F-145.
2. Au `ngOnInit()` (et au `ngOnChanges()` si `aiData` change avant la première résolution), le composant invoque `prefillFromAi()` :
   - `dateHeureNotificationOqtf` ← `aiData.dateHeureNotificationOqtfSansDelai` (ISO datetime `YYYY-MM-DDTHH:mm[:ss]` — normalisé en `YYYY-MM-DDTHH:mm` pour l'input `datetime-local`).
   - `motifSansDelai` ← `aiData.motifOqtfCode` **si** la valeur est dans l'enum front `MotifSansDelai` (`RISQUE_FUITE`, `TROUBLE_ORDRE_PUBLIC`, `OQTF_PRECEDENTE_INEXECUTEE`, `AUTRE`). L'enum backend `motifOqtfCode` (avec délai : `REFUS_TITRE`, `EXPIRATION_TITRE`, `SEJOUR_IRREGULIER`, `RETRAIT_TITRE`, `AUTRE`) n'intersecte qu'avec `AUTRE` — en pratique seul `AUTRE` sera pré-rempli, les autres valeurs seront ignorées (skip silencieux, documenté dans `audit-prefill-ia-2026-04-24.md` §3.5 et dans SF-155-04-00-BE-immig-FR §Choix 3).
   - `placementCra` ← `aiData.placementCraDetected` si `true|false` (boolean).
   - `recoursForme` ← `aiData.recoursFormeDetected?.reponse` (OUI → `true`, NON → `false`, INCONNU → laisse la valeur défaut `false`, sans signal provenance).
3. Pour chaque champ effectivement pré-rempli, un signal `provenance<Field>` passe à `'IA'` et un badge UI "Pré-rempli depuis l'analyse" (icône `auto_awesome`) s'affiche à côté du champ.
4. Au changement manuel (`onXxxChange`) d'un champ, le signal `provenance` correspondant retombe à `null` et le badge disparaît.
5. Un computed signal `coherenceAlerts` produit des alertes par champ clé quand la valeur saisie par l'avocat diverge de l'information IA. Trois types :
   - **ALERTE CRITIQUE — délai 48h expiré probable** : si `aiData.dateHeureNotificationOqtfSansDelai` est **strictement plus ancien que 48h** à l'instant courant ET `recoursForme === false` → alerte field `RECOURS_FORME`, message "Recours hors délai probable : notification > 48h, aucun recours formé".
   - **ALERTE CRITIQUE — CRA détecté** : si `aiData.placementCraDetected === true` ET avocat coche `placementCra === false` → alerte field `PLACEMENT_CRA`, message "L'analyse a détecté un placement CRA mentionné dans les pièces — vérifier".
   - **Divergence date/heure** : si `aiData.dateHeureNotificationOqtfSansDelai` diffère de la valeur saisie de **plus d'une heure** (comparaison `Math.abs(delta) > 3_600_000 ms`) → alerte field `DATE_HEURE_NOTIFICATION`, message "Date IA : {valeur} — écart > 1 h avec saisie".
   - **Contradiction recours formé** : si `aiData.recoursFormeDetected?.reponse` ∈ {OUI, NON} ET diverge de la valeur saisie → alerte field `RECOURS_FORME`, message "Analyse : {OUI/NON} pour un recours déjà formé".
6. Chaque alerte est liée à la directive `CoherencePopoverTriggerDirective` sur le champ concerné (accessible depuis le badge warning).
7. Gate `workspaceCountry !== 'FRANCE'` : bannière d'info déjà présente (pattern conforme skill §5) — aucun changement.
8. Entrée `TOOL_REGISTRY.F-IM-08-oqtf-sans-delai-fr` étendue pour binder `aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes`.

### Cas d'erreur / edge cases

| Situation | Comportement attendu |
|-----------|---------------------|
| `aiData === null` ou `undefined` | `prefillFromAi()` no-op, aucun champ modifié, aucun badge provenance affiché |
| `aiData.dateHeureNotificationOqtfSansDelai === null` | Champ date/heure reste tel quel, pas de provenance IA |
| `aiData.dateHeureNotificationOqtfSansDelai` avec secondes (`YYYY-MM-DDTHH:mm:ss`) | Normalisé à `YYYY-MM-DDTHH:mm` pour l'input `datetime-local` (compatible `toLocalInputValue()` existant) |
| `aiData.motifOqtfCode` hors enum `MotifSansDelai` (ex. `EXPIRATION_TITRE`) | Champ motif non pré-rempli, pas de provenance IA (skip silencieux) |
| `aiData.placementCraDetected === null` | Champ `placementCra` reste `false` (défaut), pas de provenance IA |
| `aiData.recoursFormeDetected?.reponse === 'INCONNU'` | Champ `recoursForme` non modifié, pas de provenance IA |
| Avocat modifie manuellement un champ pré-rempli | Signal `provenance<Field>` → `null`, badge disparaît, les alertes cohérence recalculées si divergence |
| Date IA dans le futur (IA bruyante) | Le calcul 48h reste valide arithmétiquement ; pas d'alerte spéciale — le gate métier `formValid()` existant interdit déjà une date future |
| Saisie avocat avant minuit / fuseau horaire local | `dateHeureNotificationOqtf` est stocké au format local `YYYY-MM-DDTHH:mm` ; le calcul delta IA se fait via `new Date(<local>)` et `new Date(<iso ia>)` — tolérance d'une heure couvre les petites dérives TZ. Pas de conversion timezone explicite (cohérent avec la convention datetime-local côté F-IM-08-04 backend). |
| `workspaceCountry === 'BELGIQUE'` | Bannière info existante, aucun pré-fill, aucune alerte (no-op) |
| Pas de `CoherencePopoverTriggerDirective` contextuelle (pas d'explication servie) | Le badge s'affiche quand même, popover affiche `reason` brut |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils décisionnels** : 5 SF frontend tournent en parallèle dans ce palier 2 F-155 (A1/A2/A3 — droit du travail, B1 — OQTF avec délai FR, C — Annexe 13 BE). Chaque SF touche **uniquement son propre composant** (règle de coordination du prompt : "NE PAS toucher autres composants"). Les conflits sont limités à `decisional-tools-panel.component.ts` (TOOL_REGISTRY) + éventuel rebase mécanique. Les outils décisionnels antérieurs (F-DT-07, F-DT-08, F-IM-05/06/07, F-FA-05/06/07) sont déjà IA-compliants (cf. audit initial `audit-prefill-ia-2026-04-24.md`).
- [x] **Autres pays** : composant FR uniquement, gate bannière info déjà en place pour BELGIQUE. Annexe 13 BE est traité par SF-155-04-C en parallèle.
- [x] **Autres domaines** : droit du travail traité par SF-155-04-A1/A2/A3. Famille : non concerné par F-IM-08.
- [x] **Autres UI patterns** : la directive `CoherencePopoverTriggerDirective` + badge warning + note de provenance `<span class="provenance-note">` sont les patterns canoniques de `immigration-title-decision-section` (F-IM-05-04). Aucun nouveau pattern introduit, réutilisation stricte.
- [x] **Autres flows transversaux** : aucun. Pas d'auth, pas de workspace context modifié, pas de plans/routing.

### Niveaux de vérification

- [x] **Modèle TypeScript** — `ImmigrationExtractedData` déjà étendu par SF-155-04-00-BE-immig-FR merged #519 (champs `dateHeureNotificationOqtfSansDelai`, `placementCraDetected`, `motifOqtfCode`, `recoursFormeDetected`). Aucune modification.
- [x] **Record / DTO backend** — aucun changement backend (SF frontend pure).
- [x] **Service / logique métier** — `OqtfSansDelaiService` inchangé. Le calculateur backend reste la source de vérité du décompte 48h officiel ; le pré-fill frontend + alertes visuelles ne dupliquent pas la logique mais **attirent l'attention** de l'avocat sur des cas où l'analyse IA et la saisie divergent.
- [x] **Entité JPA + schéma DB** — aucun impact, pas de persistance nouvelle.
- [x] **Tests existants** — `oqtf-sans-delai-section.component.spec.ts` actuel (19 tests) doit rester vert. Les nouveaux tests seront ajoutés en supplément.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| A1 harcèlement-licenciement-nul pré-fill IA | Oui | SF parallèle `SF-155-04-A1` |
| A2 inaptitude pré-fill IA | Oui | SF parallèle `SF-155-04-A2` |
| A3 heures-sup pré-fill IA | Oui | SF parallèle `SF-155-04-A3` |
| B1 OQTF avec délai pré-fill IA | Oui | SF parallèle `SF-155-04-B1` |
| C Annexe 13 BE pré-fill IA | Oui | SF parallèle `SF-155-04-C` |
| Pattern `CoherencePopoverTriggerDirective` déjà utilisé ailleurs | Oui | Réutilisation stricte depuis `immigration-title-decision-section` |
| `TOOL_REGISTRY` — inputs `procedureChecks / aiQuestions / piecesManquantes` | Oui | Intégré — déjà utilisé par F-IM-05/06/07 |
| Bannière gate `workspaceCountry !== FRANCE` | Non modifiée | Pattern déjà conforme skill §5 |
| Autres outils décisionnels antérieurs | Non applicable | Déjà IA-compliants |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (composant `oqtf-sans-delai-section` + TOOL_REGISTRY).
- [x] Subfeature(s) parallèle(s) créée(s) : SF-155-04-A1/A2/A3/B1/C — chacune isolée sur son propre composant.
- [x] Non applicable aux autres cibles — justifications ci-dessus.

### Cas spécifique : nouveau pattern UI ou service partagé

N/A — aucun nouveau pattern UI introduit. Réutilisation stricte des patterns `provenance-note`, `coherence-badge`, `CoherencePopoverTriggerDirective`, `alertTooltip`, `alertBadgeLabel` issus de `immigration-title-decision-section`.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** — cœur de cette SF. Alertes cohérence critiques sur le délai 48h et le placement CRA.
- [x] **Refresh dashboard (F-IA-02)** — déjà en place (`dashboardRefresh?.triggerRefresh()` après analyze). Non modifié.
- [x] **Pré-remplissage IA** — cœur de cette SF.
- [x] **Persistance des inputs** — inchangé (l'endpoint POST persiste déjà tout le form).
- [x] **Masquage conditionnel selon type** — déjà géré par le panel F-IA-04 (visibility rules).
- [x] **Alertes actives après calcul** — gate `showForm()` — les alertes ne s'affichent pas après calcul (pattern canonique section 5 skill).

---

## Impact par domaine métier

- **Droit du travail** : non applicable — F-IM-08-04 est immigration FR.
- **Droit de l'immigration (FR)** : directement concerné. Urgence absolue 48h = criticité maximale du dossier immigration FR (aucun autre outil FR n'a de délai aussi court).
- **Droit de l'immigration (BE)** : non applicable — Annexe 13 BE a ses propres délais (30 jours typ.) et son propre outil.
- **Droit de la famille** : non applicable.

Cette SF est **domaine-spécifique immigration FR**.

---

## Parité des domaines métier

Non applicable — cette SF ne livre **pas** un outil décisionnel de niveau ≥ 5. Elle branche l'IA sur un outil existant de **niveau 3** (calculateur de délai 48h). Les équivalents jumeaux :
- Famille : aucun équivalent (pas de procédure à 48h structurée en Famille).
- Droit du travail : aucun équivalent (les délais de contestation travail sont en mois/années).

---

## Critères d'acceptation

- [ ] `oqtf-sans-delai-section.component.ts` déclare les 4 `@Input()` : `aiData?: ImmigrationExtractedData | null`, `procedureChecks?: ProcedureCheck[] | null`, `aiQuestions?: AiQuestion[] | null`, `piecesManquantes?: PieceManquanteEntry[] | null`.
- [ ] Méthode privée `prefillFromAi()` invoquée dans `ngOnInit()` **et** dans `ngOnChanges()` quand `aiData` change avant la première résolution (équivalent du pattern `immigration-title-decision-section`).
- [ ] Signals `provenanceDateHeure`, `provenanceMotifSansDelai`, `provenancePlacementCra`, `provenanceRecoursForme` (type `'IA' | null`), initialisés à `null`, remplis par `prefillFromAi()` quand la valeur correspondante est effectivement pré-remplie.
- [ ] Handlers `onDateHeureNotificationChange`, `onMotifSansDelaiChange`, `onPlacementCraChange`, `onRecoursFormeChange` remettent le signal provenance correspondant à `null`.
- [ ] Computed signal `coherenceAlerts` expose les alertes par field (`DATE_HEURE_NOTIFICATION`, `PLACEMENT_CRA`, `RECOURS_FORME`, `MOTIF_SANS_DELAI` si pertinent) **uniquement quand `showForm() === true`** (gate conforme SF-IA-03-12 : ne pas réafficher les alertes après calcul).
- [ ] Template : badge UI "Pré-rempli depuis l'analyse" (icône `auto_awesome`, classe `provenance-note`, coloration **navy + or** conforme pattern canonique — **pas rouge**, pour distinguer l'info IA de l'alerte critique d'urgence 48h).
- [ ] Template : directive `CoherencePopoverTriggerDirective` appliquée sur chaque champ disposant d'une alerte cohérence potentielle.
- [ ] TOOL_REGISTRY `F-IM-08-oqtf-sans-delai-fr` étendu avec `aiData: ctx.synthesis?.immigrationExtractedData`, `procedureChecks: ctx.procedureChecks`, `aiQuestions: ctx.aiQuestions`, `piecesManquantes: ctx.synthesis?.piecesManquantesDetails`.
- [ ] Tests Jest ≥ 18 tests (criticité oblige). Couverture minimale :
  - Mount + pas d'`aiData` → no-op.
  - `aiData.dateHeureNotificationOqtfSansDelai` présente → champ pré-rempli + badge provenance.
  - `aiData.placementCraDetected === true` → toggle pré-rempli + badge provenance.
  - `aiData.recoursFormeDetected?.reponse === 'OUI'` → toggle recours pré-rempli.
  - `aiData.motifOqtfCode === 'AUTRE'` → champ motif pré-rempli ; `aiData.motifOqtfCode === 'EXPIRATION_TITRE'` → skip.
  - Datetime IA avec secondes `YYYY-MM-DDTHH:mm:ss` → normalisé à `YYYY-MM-DDTHH:mm`.
  - `onDateHeureNotificationChange` → `provenanceDateHeure() === null`.
  - **ALERTE CRITIQUE 48h** : notif IA > 48h + `recoursForme === false` → alerte `RECOURS_FORME` présente.
  - **ALERTE CRITIQUE CRA** : `placementCraDetected === true` + avocat `placementCra === false` → alerte `PLACEMENT_CRA` présente.
  - Divergence date/heure > 1h → alerte `DATE_HEURE_NOTIFICATION` présente.
  - Divergence date/heure < 1h (ex. 30 min) → alerte absente.
  - `aiData.recoursFormeDetected?.reponse === 'OUI'` + avocat `recoursForme === false` → alerte `RECOURS_FORME` (contradiction, pas hors délai).
  - Edge case minuit : notif `2026-04-23T00:00`, courant `2026-04-25T00:01` → alerte 48h déclenchée.
  - `workspaceCountry === 'BELGIQUE'` → bannière info + pas de pré-fill ni alerte.
  - `ngOnChanges` avec nouvel `aiData` avant résolution → re-invocation `prefillFromAi`.
  - `ngOnChanges` avec nouvel `aiData` après `result()` non null → pas de re-invocation (`showForm === false`).
  - Alertes **non affichées** quand `showForm() === false` (post-analyse).
  - `alertTooltip` / `alertBadgeLabel` renvoient des chaînes exploitables (smoke test directive).
- [ ] Tests existants du composant (19 tests) restent verts.
- [ ] `tsc --noEmit` vert (aucune erreur de typage).
- [ ] Build `npm run build` vert.
- [ ] `npm test` vert.

---

## Périmètre

### Hors scope (explicite)

- Ne pas modifier la logique de calcul backend `OqtfSansDelaiService` ni l'endpoint.
- Ne pas toucher aux 5 autres composants du batch F-155 (A1/A2/A3/B1/C).
- Ne pas modifier `ImmigrationExtractedData` (déjà étendu par SF-155-04-00-BE-immig-FR #519).
- Ne pas modifier la bannière `workspaceCountry !== FRANCE` (pattern déjà conforme).
- Ne pas introduire de nouveau pattern UI (provenance-note, coherence-badge, directive déjà réutilisés depuis canonical).
- Ne pas ajouter de nouvelle route ou guard.
- Ne pas ajouter de test E2E (la non-régression des smoke tests existants suffit).
- Ne pas implémenter de popover expliquant les sources (déjà fait par `CoherencePopoverTriggerDirective` existante).

---

## Valeurs initiales

| Signal | Valeur initiale | Règle |
|--------|-----------------|-------|
| `provenanceDateHeure` | `null` | Passe à `'IA'` si `aiData.dateHeureNotificationOqtfSansDelai` renseigne le champ. |
| `provenanceMotifSansDelai` | `null` | Passe à `'IA'` si `aiData.motifOqtfCode` est dans l'enum `MotifSansDelai`. |
| `provenancePlacementCra` | `null` | Passe à `'IA'` si `aiData.placementCraDetected` est un boolean. |
| `provenanceRecoursForme` | `null` | Passe à `'IA'` si `aiData.recoursFormeDetected?.reponse` ∈ {OUI, NON}. |

---

## Contraintes de validation

Aucune nouvelle contrainte backend. Les contraintes existantes (`formValid()`, enum `MotifSansDelai`, regex datetime backend SF-155-04-00-BE-immig-FR) restent inchangées.

---

## Technique

### Endpoint(s)

Aucun endpoint modifié. Le composant utilise l'existant `GET/POST /api/v1/case-files/{id}/oqtf-sans-delai` via `OqtfSansDelaiService`.

### Tables impactées

Aucune.

### Migration Liquibase

- [ ] Oui
- [x] Non applicable — SF frontend pure.

### Composants Angular

- `oqtf-sans-delai-section.component.ts` — extension pattern pré-fill IA + alertes cohérence.
- `oqtf-sans-delai-section.component.html` — ajout badges provenance + directives cohérence + alertes critiques.
- `oqtf-sans-delai-section.component.scss` — ajout (classes `provenance-note`, `coherence-badge`, `coherence-critical`) alignées pattern canonique.
- `oqtf-sans-delai-section.component.spec.ts` — ≥ 18 tests supplémentaires.
- `decisional-tools-panel.component.ts` — binding TOOL_REGISTRY enrichi.

### Référentiel métier

Aucun impact (pas de classe `*Referentiel.java`, pas d'enum DB).

---

## Plan de test

### Tests unitaires (composant Jest)

1. `prefillFromAi` — no-op quand `aiData === null`.
2. `prefillFromAi` — date/heure pré-remplie depuis `dateHeureNotificationOqtfSansDelai`.
3. `prefillFromAi` — date/heure normalisation secondes → `YYYY-MM-DDTHH:mm`.
4. `prefillFromAi` — `placementCra` pré-rempli depuis `placementCraDetected === true`.
5. `prefillFromAi` — `placementCra` non pré-rempli si `placementCraDetected === null`.
6. `prefillFromAi` — `recoursForme` pré-rempli depuis `recoursFormeDetected.reponse === 'OUI'`.
7. `prefillFromAi` — `recoursForme` non pré-rempli si `reponse === 'INCONNU'`.
8. `prefillFromAi` — `motifSansDelai` pré-rempli si `motifOqtfCode === 'AUTRE'`.
9. `prefillFromAi` — `motifSansDelai` non pré-rempli si `motifOqtfCode === 'EXPIRATION_TITRE'` (hors enum).
10. `onDateHeureNotificationChange` efface `provenanceDateHeure`.
11. `onPlacementCraChange` efface `provenancePlacementCra`.
12. `onRecoursFormeChange` efface `provenanceRecoursForme`.
13. `onMotifSansDelaiChange` efface `provenanceMotifSansDelai`.
14. `coherenceAlerts` — ALERTE CRITIQUE 48h : notif IA > 48h + `recoursForme === false` → alerte `RECOURS_FORME` présente.
15. `coherenceAlerts` — ALERTE CRITIQUE CRA : `placementCraDetected === true` + avocat `placementCra === false` → alerte `PLACEMENT_CRA` présente.
16. `coherenceAlerts` — divergence date/heure > 1h → alerte `DATE_HEURE_NOTIFICATION` présente.
17. `coherenceAlerts` — divergence date/heure < 1h → alerte absente.
18. `coherenceAlerts` — contradiction `recoursFormeDetected.OUI` vs avocat `false` → alerte `RECOURS_FORME` (message contradiction).
19. `coherenceAlerts` — edge case minuit / 48h exact → alerte déclenchée.
20. `coherenceAlerts` — toutes alertes absentes quand `showForm() === false` (post-calcul).
21. `workspaceCountry === 'BELGIQUE'` → aucun pré-fill, aucune alerte (gate existant).
22. `ngOnChanges(aiData)` avant résolution → re-invocation `prefillFromAi`.
23. `ngOnChanges(aiData)` après `result()` non null → pas de re-invocation.
24. TOOL_REGISTRY — smoke test : les 4 inputs sont passés au composant (test à écrire dans `decisional-tools-panel.component.spec.ts` — léger).

### Tests d'intégration

- [x] Non applicable — SF frontend pure, pas d'endpoint nouveau. Les tests d'intégration existants de l'endpoint OQTF sans délai restent verts par non-régression.

### Isolation workspace

- [x] Non applicable — le composant consomme les inputs passés par le panel déjà scopé par workspace via le service d'analyse.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Outil décisionnel métier** — branchement IA sur un outil existant (F-IM-08-04 OQTF sans délai FR).

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `oqtf-sans-delai-section.component.ts` | Extension par 4 Inputs + computed alerts. Le chemin actuel (sans aiData) reste intact via `if (!aiData) return;` dans `prefillFromAi` | 19 tests existants restent verts |
| `decisional-tools-panel.component.ts` | TOOL_REGISTRY binding étendu | Les autres entrées non touchées restent lisibles via test visuel de smoke (`panel.component.spec.ts` inchangé) |
| Endpoint `/api/v1/case-files/{id}/oqtf-sans-delai` | Aucun impact | Tests existants restent verts |

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — pas d'auth/workspace/routing touché.

---

## Dépendances

### Subfeatures bloquantes

- `SF-155-04-00-BE-immig-FR` (PR #519) — **mergée** le 2026-04-24. Les champs IA `dateHeureNotificationOqtfSansDelai`, `placementCraDetected`, `motifOqtfCode`, `recoursFormeDetected` sont déjà exposés dans `ImmigrationExtractedData` (DTO frontend + backend).

### Subfeatures débloquées par celle-ci

Aucune directe. Cette SF **clôt la dette de convergence IA** identifiée pour F-IM-08-04 (cf. `audit-prefill-ia-2026-04-24.md` §3.5).

### Questions ouvertes impactées

- [ ] Aucune question de `docs/OPEN_QUESTIONS.md` tranchée ou impactée.

---

## Notes et décisions

- **Choix 1 — Pattern canonique** : strict copier-adapter depuis `immigration-title-decision-section` (computed `coherenceAlerts`, `alertsSummary`, `buildXxxAlert`, directive `CoherencePopoverTrigger`, note de provenance). Aucun nouveau pattern inventé.
- **Choix 2 — Badges provenance navy+or, pas rouge** : le composant a une palette rouge dominante documentée (urgence 48h, skill §5). Le badge provenance "Pré-rempli depuis l'analyse" reste **navy + or** (pattern canonique `.provenance-note`) pour distinguer visuellement l'**information IA** (pré-fill neutre) de l'**alerte critique d'urgence** (rouge). Les badges d'alerte cohérence critique peuvent quant à eux utiliser une palette rouge pour se démarquer.
- **Choix 3 — Enum `MotifSansDelai` vs `motifOqtfCode`** : le backend IA n'extrait que les codes OQTF avec délai (cf. SF-155-04-00-BE-immig-FR §Choix 3 : « le motif sans délai implique une qualification administrative mélange subjectif et réglementaire, peu fiable en détection auto »). Le pré-fill motif utilisera donc `motifOqtfCode` en cross-check avec l'enum `MotifSansDelai` — seule la valeur `AUTRE` est commune, les 4 autres (`REFUS_TITRE`, `EXPIRATION_TITRE`, `SEJOUR_IRREGULIER`, `RETRAIT_TITRE`) seront ignorées silencieusement. L'avocat devra saisir manuellement le motif sans délai dans 95 % des cas — mais l'infra est en place pour le jour où une SF future étendra le backend avec un champ `motifSansDelaiCode` dédié.
- **Choix 4 — Seuil divergence date/heure 1h** : dans une procédure urgente 48h, une divergence de 15 min peut déjà avoir un impact (ex. recours déposé à la minute près). Seuil strict — 1h est une marge "de confort" qui évite l'alerte bruyante pour des dérives de TZ / d'arrondi. Au-delà d'1h, le message `"Date IA : {iso} — écart > 1 h avec saisie"` invite à vérifier.
- **Choix 5 — Calcul 48h basé sur `new Date()` au moment courant** : non mocké en production. Pour les tests, les fixtures utilisent des dates relatives (notif = `Date.now() - 49h`) pour déclencher/éviter l'alerte. Cohérent avec la pratique du composant actuel (`buildNowLocalIso()`).
- **Choix 6 — Alertes uniquement quand `showForm() === true`** : conforme à la règle SF-IA-03-12 (ne pas réafficher les alertes après calcul, seule le verdict importe). Le computed intègre ce gate dès le début.
- **Choix 7 — `recoursFormeDetected.reponse === 'INCONNU'` = pas de provenance** : on ne veut pas afficher "Pré-rempli depuis l'analyse" si l'IA répond "Je ne sais pas" — ça donnerait une fausse confiance.
- **Choix 8 — Pas de `SourceExplanationService`** : le composant canonique `immigration-title-decision-section` branche aussi un `SourceExplanationService` pour enrichir le popover. Pour cette SF, **on ne branche pas** ce service (surcharge non justifiée — l'urgence 48h privilégie la lisibilité + clarté des alertes plutôt qu'un popover riche). Le popover affichera directement `reason` (texte généré par le composant). Un futur enrichissement (SF distincte) pourra ajouter le branchement si besoin.
