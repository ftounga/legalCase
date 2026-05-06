# Mini-spec — F-193 / SF-193-02 Frontend — Sortie outils enrichie alignement procedure_checks + tile dashboard

## Identifiant

`F-193 / SF-193-02`

## Feature parente

`F-193` — Matérialisation des points procéduraux F-96 vers outils décisionnels + dashboard + pieces/délais + PDF

## Statut

`draft`

## Date de création

2026-05-06

## Branche Git

`feat/SF-193-02-frontend-procedure-checks-output`

## Pattern de référence

**SF-192-02-frontend.md** (F-192 mergée 2026-05-06, PR #860) — cette SF en est le **jumeau procédural**. Lire SF-192-02 pour le pattern complet (gating SSE, OnPush + markForCheck, fail-open silencieux).

## Contrat API importé de SF-193-01-backend

- `GET /api/v1/case-files/{id}/procedure-checks-alignment` → `ProcedureCheckAlignment[]`
- `ProcedureCheckAlignment { checkId, libelle, critereCode, statut, expectedValue?, raison?, toolIdCible?, matchStatus: 'ALIGNED' | 'DIVERGENT' | 'NON_COMPLIANT_FLAG' | 'TO_VERIFY_FLAG' | 'NOT_ANALYZED' | 'NO_TARGET_TOOL' }`
- `CaseFileDashboardResponse.tiles[]` inclut tile `{ toolId: 'F-193-procedure-checks-summary', theme: 'DELAIS', label, primaryValue, secondaryValue?, alertLevel? }`

---

## Objectif

Côté frontend, afficher l'alignement entre les checks F-96 (statuts avocat) et les outils décisionnels concernés via : (1) bloc dédié dans la sortie des outils impactés (« ✅ Vérifications procédurales confirmées » pour ALIGNED, alerte douce pour NON_COMPLIANT_FLAG/TO_VERIFY_FLAG) ; (2) badge sur card panel F-IA-04 ; (3) tile dashboard `F-193-procedure-checks-summary` thème DELAIS. Toutes ces lectures sont issues de la dernière `CaseAnalysis` DONE — refresh uniquement au run de Synthèse enrichie via SSE `ENRICHED_ANALYSIS DONE`.

---

## Comportement attendu

### Cas nominal

1. Au montage du dossier, `CaseFileDetailComponent` (ou le `<app-decisional-tools-panel>`) appelle `ProcedureCheckAlignmentService.getForCaseFile(caseFileId)` → `ProcedureCheckAlignment[]`. Signal cache.

2. **TOOL_REGISTRY étendu** — chaque entrée pour les outils mappés (F-DT-08, F-DT-09, F-DT-10, F-DT-12, F-IM-05, F-IM-06, F-IM-07, F-FA-05, F-FA-06, F-FA-07) reçoit `proceduresChecksAlignment: ProcedureCheckAlignment[]` filtrée sur `toolIdCible === <toolId courant>` dans `inputs(ctx)`.

3. **Sortie de chaque outil concerné** — nouveau bloc HTML après le résultat principal, structuré en 3 sous-sections selon ce qui s'applique :
   - **✅ Vérifications confirmées par votre avocat** (matchStatus = ALIGNED) : liste des checks VERIFIED qui pointent vers cet outil, signal positif (border-left or, icône `check_circle`). Aligné DESIGN_SYSTEM.md.
   - **⚠️ Points non conformes signalés** (matchStatus = NON_COMPLIANT_FLAG) : liste des checks NON_COMPLIANT, signal d'alerte (border-left rouge subtil, icône `warning`) avec libellé + raison.
   - **⏳ Points à vérifier** (matchStatus = TO_VERIFY_FLAG) : liste des checks TO_CHECK, signal d'incertitude (border-left gris, icône `help_outline`) avec libellé.
   
   Si `proceduresChecksAlignment` vide ou tous `NO_TARGET_TOOL` → bloc absent.

4. **Card du panel F-IA-04** : nouveau badge optionnel `🔍 Procédure (V/N/T)` avec compteurs si checks alignés sur l'outil. Icône `verified`. Static helper `getProcedureChecksBadge(input): { kind: 'verified' | 'non_compliant' | 'to_verify' | 'mixed' | 'none', counts: { verified, nonCompliant, toVerify } }` exposé par chaque composant outil concerné.
   - `verified` (3 verts) si tous ALIGNED
   - `non_compliant` (rouge) si ≥ 1 NON_COMPLIANT_FLAG
   - `to_verify` (gris) si 0 NON_COMPLIANT mais ≥ 1 TO_VERIFY_FLAG
   - `mixed` (mix) si combinaison
   - `none` si rien

5. **Tile dashboard** — `<app-dashboard-tile>` rend la tile `F-193-procedure-checks-summary` thème DELAIS comme les autres. Particularité : clic → scroll vers le bloc Checklist procédurale de la synthèse via `Router.navigate(['/case-files', id, 'synthesis'], { fragment: 'section-checklist' })` (ou équivalent du fragment existant).

6. **Cycle de rafraîchissement** identique F-192 SF-192-02 : pas de refresh post-PUT statut check (cohérence F-96 stricte côté frontend), refresh uniquement au run de Synthèse enrichie via SSE `ENRICHED_ANALYSIS DONE` ; `SynthesisComponent` strictement inchangé.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Endpoint 404/500/timeout | Fail-open silencieux, alignement = [], log console, blocs sortie vides, badges absents |
| `toolIdCible = null` (NO_TARGET_TOOL) | Le check n'apparaît dans aucune sortie d'outil mais est compté dans la tile dashboard |
| Aucun check matérialisé | Tile absente du dashboard, blocs sortie vides |

---

## Critères d'acceptation

- [ ] **CA-01** : `ProcedureCheckAlignmentService.getForCaseFile(id)` charge l'alignement au montage, signal exposé
- [ ] **CA-02 sortie outil ALIGNED** : sur un dossier avec ≥ 1 check VERIFIED `IM05_MOTIF` `expectedValue = "TRAVAIL"` aligné avec F-IM-05 → bloc `✅ Vérifications confirmées par votre avocat` apparaît dans la sortie de F-IM-05
- [ ] **CA-03 sortie outil NON_COMPLIANT** : check NON_COMPLIANT `LICENCIEMENT_NOTIFICATION` aligné avec F-DT-08 → bloc `⚠️ Points non conformes signalés` apparaît dans F-DT-08
- [ ] **CA-04 sortie outil TO_VERIFY** : check TO_CHECK aligné → bloc `⏳ Points à vérifier`
- [ ] **CA-05 mix** : combinaison de plusieurs statuts → 2-3 sous-sections affichées simultanément
- [ ] **CA-06 sans alignement** : `proceduresChecksAlignment` vide ou que NO_TARGET_TOOL → bloc absent (comportement actuel inchangé)
- [ ] **CA-07 badge card mixed** : 1 ALIGNED + 1 NON_COMPLIANT → kind `mixed`, counts corrects
- [ ] **CA-08 badge card non_compliant** : ≥ 1 NON_COMPLIANT, 0 ALIGNED, 0 TO_VERIFY → kind `non_compliant`
- [ ] **CA-09 tile dashboard** : tile `F-193-procedure-checks-summary` thème DELAIS rendue avec primary/secondary/alertLevel corrects
- [ ] **CA-10 tile clic** : clic → navigation vers `/case-files/{id}/synthesis#section-checklist`
- [ ] **CA-11 PUT statut sans refresh frontend** : après PUT statut check, **aucun appel** `triggerRefresh()` n'est déclenché. Test régression critique.
- [ ] **CA-12 refresh au run synthèse enrichie** : event SSE `ENRICHED_ANALYSIS DONE` → re-fetch alignement
- [ ] **CA-13 fail-open** : endpoint timeout → blocs vides, pas de spinner persistant, log console
- [ ] **CA-14 OnPush + markForCheck** : tous subscribe() qui mutent l'état affiché injectent ChangeDetectorRef + markForCheck() (cf. memory `feedback_onpush_subscribe_markforcheck.md`)
- [ ] **CA-15 visuel charte** : badges + blocs sortie palette navy/or DESIGN_SYSTEM.md (rouge réservé à NON_COMPLIANT subtil border-left, pas de rouge dominant)

---

## Périmètre

### Hors scope (explicite)

- (a) Refonte de la UI bloc Checklist procédurale dans `SynthesisComponent` (V1 : aucune modif)
- (b) Animation pulse au mount du badge
- (c) Personnalisation visuelle par check
- (d) Mode édition du libellé du check depuis la sortie outil

---

## Technique

### Composants Angular impactés

- `ProcedureCheckAlignmentService` (nouveau) — `core/services/procedure-check-alignment.service.ts`
- `procedure-check-alignment.model.ts` (nouveau) — interface miroir
- Outils mappés à étendre :
  - **Travail FR/BE** : `<app-validite-licenciement-section>`, `<app-comparateur-indemnites-section>`, `<app-rupture-conv-section>`, `<app-harcelement-licenciement-nul-section>` (FR seulement)
  - **Immigration FR/BE** : `<app-immigration-title-decision-section>` (déjà touché par F-192), `<app-immigration-recours-section>` (idem), `<app-immigration-work-right-section>`
  - **Famille FR/BE** : `<app-partage-immobilier-section>`, `<app-calendrier-garde-section>`, `<app-checklist-divorce-section>`
  - Pour chaque : `@Input() proceduresChecksAlignment?: ProcedureCheckAlignment[]`, computed signals `proceduresAligned/proceduresNonCompliant/proceduresToVerify`, template ajout 3 sous-sections, static `getProcedureChecksBadge()`
- `<app-decision-tool-card>` (étendu) — `@Input() proceduresChecksBadge?: { kind, counts }`, template ajout pill compteur
- `<app-decisional-tools-panel>` (`TOOL_REGISTRY` étendu) — `proceduresChecksAlignment: ctx.proceduresChecksAlignment?.filter(p => p.toolIdCible === '<toolId>')` dans `inputs(ctx)` pour ~10 outils
- `<app-case-dashboard>` — mapping toolId `F-193-procedure-checks-summary` → label "Vérifications procédurales" + handler clic → `Router.navigate`

### Migration

- [x] Aucune (couverte SF-193-01)

---

## Plan de test

### Tests Jest (~15-18 tests)

- `ProcedureCheckAlignmentServiceTest` (3 : success / 404 / 500 fail-open)
- Outils impactés (mosaïque) : 3 tests par outil clé (ALIGNED bloc / NON_COMPLIANT bloc / aucun check) × 5 outils représentatifs = ~15 tests
- `DecisionToolCardComponentTest` extension (3 : kind verified/non_compliant/none)
- `CaseDashboardComponentTest` (2 : tile présente / clic navigation)

### Isolation workspace

- [x] Non applicable côté frontend pur

---

## Dépendances

- F-96 ✅ Terminée
- F-192 SF-192-02 ✅ Terminée (pattern + DESIGN_SYSTEM badge réutilisé)
- F-167 ✅ Terminée
- F-IA-04 ✅ Terminée
- **SF-193-01 backend** — contrat API figé

---

## Notes et décisions

- **Décision 2026-05-06** : V1 transversal 3 domaines (cohérent SF-193-01 backend) — la palette d'outils impactés est plus large que F-192 (~10 outils vs 2). Charge frontend en conséquence (~1.5j vs 1j).
- **Décision 2026-05-06** : palette badge rouge (NON_COMPLIANT) **subtile** — border-left rouge fin, pas fond rouge plein. Le rouge dominant reste réservé aux alertes critiques système (cf. règle DESIGN_SYSTEM.md).
- **Décision 2026-05-06** : 3 sous-sections séparées en sortie outil (Vérifié / Non conforme / À vérifier) plutôt qu'une liste mixte — facilite la lecture, aligne avec les 3 statuts F-96.
- **Décision 2026-05-06** : pas de rollback frontend du PUT (cohérence F-96 stricte). Le refresh intervient au run de Synthèse enrichie via SSE.
