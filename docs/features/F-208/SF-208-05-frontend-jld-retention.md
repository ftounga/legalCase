# SF-208-05 — JLD rétention administrative frontend (Angular)

## Identifiant
`F-208 / SF-208-05`

## Statut
`draft` — 2026-05-11

## Branche Git
`feat/SF-208-frontend-immigration-fr-p1` (commune aux 4 SF frontend F-208 — TOOL_REGISTRY partagé)

## Pattern de référence
`frontend/src/app/case-files/oqtf-avec-delai-section/` (composant Immigration FR P1 OQTF mono-pays, déjà conforme F-IA-04 canonique).

## Objectif
Composant Angular `<app-jld-retention-section>` qui consomme l'endpoint `POST/GET /api/v1/case-files/{caseFileId}/jld-retention-analysis` (SF-208-01 backend mergée PR #915) et expose l'outil dans le panel F-IA-04 via TOOL_REGISTRY (tool_id `F-IM-21-jld-retention-fr`).

## Critères d'acceptation

### Service Angular
- [ ] **CA-01** : `JldRetentionService` (`frontend/src/app/core/services/jld-retention.service.ts`) avec `post(caseFileId, request): Observable<Response>` + `get(caseFileId): Observable<Response | null>` (fail-open 404 → null). Modèles dans `frontend/src/app/core/models/jld-retention.model.ts` (types miroirs DTO backend).

### Composant
- [ ] **CA-02** : standalone, OnPush, palette navy/or canonique (rouge réservé URGENT/EXPIRE conforme F-IA-04 alerts).
- [ ] **CA-03** : 4 inputs formulaire — `dateNotificationPlacement` (`<input type="date">`), `motifPlacement` (select enum), `recoursForme` (checkbox), `dateRecours` (`<input type="date">`, désactivé si `!recoursForme`).
- [ ] **CA-04** : bandeau verdict colorisé selon `statut` (DISPONIBLE navy, URGENT or, EXPIRE rouge `#C0392B`, RECOURS_FORME or souligné). Dates affichées JetBrains Mono.
- [ ] **CA-05** : gate `workspaceCountry` — bannière info si workspace BE (outil FR-only) + bouton submit désactivé.
- [ ] **CA-06** : pré-remplissage IA (`prefillFromAi()` + signal `provenanceDateNotification = signal<'IA'|null>(null)` + badge `auto_awesome` à côté du champ). Source canonique : `aiData.dateNotificationDecision` (ImmigrationExtractedData) si présent. **OBLIGATOIRE** (FAIL si absent).
- [ ] **CA-07** : validation F-IA-03 — `coherenceAlerts = computed<Partial<Record<FieldName, CoherenceAlert>>>()` sur `dateNotificationPlacement` (divergence si `procedureChecks.find(c => c.code === 'NOTIFICATION_DECISION')?.date` ≠ valeur saisie) + `<app-coherence-popover-trigger>` câblé via helper `CoherenceAlertBuilder`. **OBLIGATOIRE** (FAIL si absent).
- [ ] **CA-08** : Static `getPrefillCount(input)` — pattern miroir SF-177-12 / OQTF section.
- [ ] **CA-09** : entrée TOOL_REGISTRY `F-IM-21-jld-retention-fr` symétrique aux autres immigration FR (inputs : `caseFileId, workspaceCountry, aiData, procedureChecks, aiQuestions, piecesManquantes, retainedPistes, piecesAlignment, risquesAlignment, aiQuestionsAlignment`).
- [ ] **CA-10** : `KNOWN_FRONTEND_TOOL_IDS` du test d'intégrité backend `DecisionToolVisibilityIntegrityIT` est déjà à jour (mergée PR #915) — vérifier juste qu'il n'y a pas eu de régression.
- [ ] **CA-11** : tests Jest (≥ 15) couvrant : POST 200 nominal, GET 200 hydratation, GET 404 form vierge, gate country, validation client, pré-fill IA, getPrefillCount 0/M/N, coherenceAlerts, déduplication tile dashboard via dashboardRefreshService.

### Hors scope
- Pas de modification backend.
- Tests E2E inclus dans la suite SF-208 (non lancés ici).
