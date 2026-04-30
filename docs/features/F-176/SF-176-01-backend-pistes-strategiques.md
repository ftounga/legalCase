# Mini-spec — F-176 / SF-176-01 Backend bloc transversal "Pistes stratégiques"

## Identifiant

`F-176 / SF-176-01`

## Feature parente

`F-176` — Bloc transversal "Pistes stratégiques" — séparer options stratégiques de la checklist procédurale F-96

## Statut

`ready`

## Date de création

2026-04-30

## Branche Git

`feat/SF-176-01-backend-pistes-strategiques`

---

## Objectif

Côté backend, introduire un nouveau champ `pistes_strategiques` dans la sortie JSON de la synthèse IA (standard + enrichie), créer la table `strategic_options` pour persister les pistes par analyse, exposer 2 endpoints (GET list + PATCH statut + raison écartée), et propager les pistes RETAINED et DISCARDED dans le prompt de la synthèse enrichie suivante (pattern miroir F-96 procedure_checks).

---

## Comportement attendu

### Cas nominal

1. **Génération par l'IA** : à la fin d'une analyse standard ou enrichie, Claude produit dans le JSON un nouveau champ `pistes_strategiques` qui liste 0 à N pistes stratégiques (options conditionnelles, opportunités futures, recommandations d'action) — c'est-à-dire le contenu que SF-96-06 vient d'exclure de `points_procedure`.
2. **Persistance** : `StrategicOptionService.persistFromAnalysis(analysis, jsonRoot)` est invoqué (fail-open) après extraction. Chaque piste devient une ligne `strategic_options` avec `statut = TO_STUDY` (par défaut), `case_analysis_id = analysis.id`, `workspace_id = analysis.workspace.id`, `ordre` correspondant à l'ordre dans le JSON IA.
3. **Lecture** : `GET /api/v1/case-files/{caseFileId}/analyses/{analysisId}/strategic-options` retourne la liste triée par `ordre` ascendant, isolation workspace stricte (403 si workspace différent).
4. **Mise à jour de statut** : `PATCH /api/v1/strategic-options/{optionId}` body `{statut: "TO_STUDY"|"RETAINED"|"DISCARDED", raisonDiscard?: string}` met à jour le statut + raison écartée, vérifie l'appartenance workspace (403 si KO), retourne le `StrategicOptionResponse` mis à jour.
5. **Propagation entre analyses (synthèse enrichie)** : avant de construire le prompt enrichi, `EnrichedAnalysisService` invoque `StrategicOptionService.collectForEnrichment(previousAnalysisId)` qui retourne les pistes RETAINED + DISCARDED de l'analyse précédente. Deux nouvelles sections sont injectées dans le prompt utilisateur :
   - `[Pistes stratégiques retenues à approfondir]` — listant les pistes RETAINED (texte + base juridique + conditions) — Claude doit les développer / leur donner suite dans la nouvelle synthèse
   - `[Pistes stratégiques écartées — NE PAS re-proposer]` — listant les pistes DISCARDED (texte + raison écartée) — Claude est instruit de ne pas les remettre dans `pistes_strategiques`
6. **Préservation post-enrichissement** : après la nouvelle analyse enrichie, `StrategicOptionService.persistFromAnalysis(newAnalysis, jsonRoot)` crée les pistes de la nouvelle analyse (depuis le JSON IA) ; SÉPARÉMENT, les pistes RETAINED et DISCARDED de l'ancienne analyse sont **clonées** sur la nouvelle analyse (avec leurs statuts conservés, ordre re-calculé à la suite des nouvelles TO_STUDY) — pour que l'avocat retrouve son tri à travers les itérations. Les TO_STUDY de l'ancienne analyse ne sont **pas** propagées (elles sont remplacées par les nouvelles propositions IA).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Champ `pistes_strategiques` absent ou null dans JSON IA | Aucune piste créée, fail-open log debug, analyse continue | — |
| Champ `pistes_strategiques` malformé (format invalide) | Fail-open : log warn, aucune piste créée, analyse continue (pattern `ProcedureCheckService.parsePointsProcedure`) | — |
| Piste avec `texte` vide ou null | Skip cette piste, pas de blocage analyse | — |
| `GET` analyses inexistante | Liste vide retournée (pas de 404 — comportement F-96) | 200 |
| `GET` workspace différent | Erreur 403 (isolation) | 403 |
| `PATCH` option inexistante | Erreur 404 | 404 |
| `PATCH` option workspace différent | Erreur 403 | 403 |
| `PATCH` `statut` invalide ou null | Erreur 400 "Champ 'statut' requis" / "Statut invalide" | 400 |
| `PATCH` `statut = DISCARDED` sans `raisonDiscard` | Accepté — `raisonDiscard` est optionnelle (l'avocat peut écarter sans expliquer V1) | 200 |
| Propagation : analyse précédente non trouvée | Fail-open : log debug, prompt enrichi sans sections de propagation | — |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** : F-96 (procedure_checks) — pattern miroir exact, je m'en inspire ; F-13/F-14 (questions IA) — différent (questions à l'avocat) ; F-90 (chat) — différent. Aucune duplication.
- [x] **Autres pays** : France + Belgique — la table et les endpoints sont neutres, le champ JSON est rempli par le prompt qui lui-même est dynamique (`LegalDomainPromptBuilder` neutre par pays sur ce point).
- [x] **Autres domaines** : transversal aux 3 domaines DROIT_DU_TRAVAIL / DROIT_FAMILLE / DROIT_IMMIGRATION — par construction.
- [x] **Autres UI patterns** : la SF backend ne touche pas l'UI. SF-176-02 frontend gère.
- [x] **Autres flows transversaux** : auth via `@AuthenticationPrincipal OidcUser` (pattern ProcedureCheckController), workspace context via `principal` (pattern existant), pas de plans/quotas (les pistes sont gratuites — pas de gate F-16/F-33), pas de routing.

### Niveaux de vérification

- [x] **Modèle TypeScript / API exposée** : `StrategicOption` interface (figée ci-dessous, consommée par SF-176-02).
- [x] **Record / DTO backend** : `StrategicOptionResponse` (figé ci-dessous).
- [x] **Service / logique métier** : `StrategicOptionService.persistFromAnalysis` + `list` + `updateStatus` + `collectForEnrichment` + `propagateRetainedAndDiscarded` (clonage post-enrichissement).
- [x] **Entité JPA + schéma DB** : nouvelle table `strategic_options` migration `197-create-strategic-options.xml` (à ajuster au numéro libre au moment du dev).
- [x] **Tests existants** : aucun à étendre, la SF crée tout.

### Nouveau pattern UI ou service partagé

- [x] **Nouveau service `StrategicOptionService`** — pas un service partagé transversal au sens "shared/", mais un service applicatif aligné sur le pattern `ProcedureCheckService`. Pas de risque de divergence (réutilise les conventions existantes).
- [x] **Nouveau DTO `StrategicOptionResponse`** — n'est pas réutilisable en dehors de F-176 (les autres outils ont leurs propres DTOs). Pas de mutualisation à prévoir.
- [x] **Nouveaux endpoints `/strategic-options`** — pattern miroir des endpoints `/procedure-checks`. Pas de divergence créée.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-96 (procedure_checks) | Oui — pattern de référence | Pattern miroir suivi (entity + repo + service + controller + status enum + IT isolation) |
| F-IA-03 (cohérence IA) | **Volontairement exclu** | Décision actée dans la backlog spec — les pistes sont par essence ouvertes, pas de cohérence à calculer |
| F-IA-02 (refresh dashboard) | Non | Les pistes ne génèrent pas de KPI dashboard ; le refresh n'est pas pertinent |
| F-IA-04 (panneau outils décisionnels) | Non | Pas un outil décisionnel (pas un calculateur, pas de verdict) |
| F-160 (historique paginé) | À étendre dans le futur | Hors scope de cette SF — la pagination par itération sera ajoutée si retour terrain |
| Frontend (SF-176-02) | Oui | SF-parallèle (contrat figé ci-dessous) |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (pattern miroir F-96 strict)
- [x] Subfeature parallèle SF-176-02 frontend créée (mini-spec écrite après celle-ci, contrat figé importé)

---

## Impact par domaine métier

| Domaine | Effet |
|---------|-------|
| **Droit du travail (FR + BE)** | Le prompt produit des pistes du type « Envisager prise d'acte si harcèlement avéré », « Demander la requalification CDD→CDI », « Transaction post-licenciement ». Stockées dans `strategic_options`, affichées dans le bloc dédié. |
| **Droit de la famille (FR + BE)** | Pistes du type « Divorce pour faute si preuve d'adultère », « Mode de garde alternatif si l'enfant change d'établissement », « Partage anticipé vs liquidation différée ». |
| **Droit de l'immigration (FR + BE)** | **Cible principale** — pistes du type « Demande Passeport talent — Chercheur si convention d'accueil INRIA », « Carte de résident à 3 ans de mariage », « Voie AES alternative », « Recours gracieux préfet avant contentieux ». |

Pas de différenciation par domaine côté backend (la table et les endpoints sont neutres). Le prompt s'adapte naturellement via `LegalDomainPromptBuilder` (déjà domaine-aware).

---

## Parité des domaines métier

(N/A — F-176 est transversale par construction, pas un outil décisionnel niveau ≥ 5. Les 3 domaines sont couverts symétriquement par la même infrastructure.)

---

## Critères d'acceptation

- [ ] Le prompt `CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` produit un nouveau champ `pistes_strategiques` dans le JSON, format figé `[{texte, base_juridique?, horizon_temporel?, conditions?, source?}]`.
- [ ] Idem pour `EnrichedAnalysisService.SYSTEM_PROMPT_TEMPLATE`.
- [ ] Le prompt instruit explicitement Claude que `pistes_strategiques` reçoit les options stratégiques / opportunités futures / recommandations d'action — c'est-à-dire ce que SF-96-06 vient d'exclure de `points_procedure`.
- [ ] Migration Liquibase crée la table `strategic_options` (schema décrit ci-dessous), avec FK + index, idempotente, réversible.
- [ ] Entité `StrategicOption` JPA correspondante avec `@ManyToOne CaseAnalysis caseAnalysis` + `@ManyToOne Workspace workspace`.
- [ ] Repository `StrategicOptionRepository` avec méthode `findByCaseAnalysisIdOrderByOrdreAsc(UUID)`.
- [ ] Service `StrategicOptionService` avec : `persistFromAnalysis(analysis, jsonRoot)` (fail-open extraction), `list(caseFileId, analysisId, oidcUser, principal)` (isolation workspace), `updateStatus(optionId, statut, raisonDiscard, oidcUser, principal)`, `collectForEnrichment(previousAnalysisId)` (returns RETAINED + DISCARDED), `propagateRetainedAndDiscarded(previousAnalysisId, newAnalysis)` (clone with statuts preserved).
- [ ] Controller `StrategicOptionController` avec endpoints :
  - `GET /api/v1/case-files/{caseFileId}/analyses/{analysisId}/strategic-options` → `List<StrategicOptionResponse>`
  - `PATCH /api/v1/strategic-options/{optionId}` body `{"statut": "TO_STUDY"|"RETAINED"|"DISCARDED", "raisonDiscard": "..."?}` → `StrategicOptionResponse`
- [ ] Enum `StrategicOptionStatus` : `TO_STUDY` (default), `RETAINED`, `DISCARDED`.
- [ ] Branchement dans `CaseAnalysisService.consumeCaseAnalysis()` après save de l'analyse : invoquer `strategicOptionService.persistFromAnalysis(savedAnalysis, jsonRoot)` en fail-open.
- [ ] Branchement dans `EnrichedAnalysisService.consumeEnrichedAnalysis()` AVANT `buildEnrichedPrompt` : invoquer `collectForEnrichment(previousAnalysisId)` et passer les listes RETAINED + DISCARDED dans le builder de prompt utilisateur. APRÈS save de la nouvelle analyse : invoquer `propagateRetainedAndDiscarded(previousAnalysisId, newAnalysis)` puis `persistFromAnalysis(newAnalysis, jsonRoot)`.
- [ ] Sections `[Pistes stratégiques retenues à approfondir]` et `[Pistes stratégiques écartées — NE PAS re-proposer]` injectées dans le prompt utilisateur enrichi (pas le system prompt — le system prompt a la règle générique).
- [ ] System prompt enrichi instruit de ne pas re-proposer les pistes DISCARDED dans `pistes_strategiques`.
- [ ] Tests unitaires : extraction fail-open, persistance, list, updateStatus, isolation workspace, propagation.
- [ ] Tests d'intégration : 2 IT (GET list happy path, PATCH update status, GET 403 cross-workspace, PATCH 403 cross-workspace, full pipeline avec extraction).
- [ ] `AnalysisJsonTruncator` supporte le champ `pistes_strategiques` avec une limite (ex. 10 pistes par analyse).
- [ ] Mini-spec SF-176-02 frontend (parallèle) référence ce contrat API.

---

## Périmètre

### Hors scope

- Frontend (couvert par SF-176-02 — bloc UI dans SynthesisComponent)
- F-IA-03 (cohérence IA) — volontairement exclu
- Export PDF/DOCX dédié — couvert par F-95/F-40 (export synthèse global) — au minimum, l'export devra mentionner les pistes RETAINED dans le DOCX/PDF dans une SF ultérieure (pas cette SF)
- Pagination par itération de synthèse — couvert par F-160 future
- Drag-and-drop de réorganisation — V2 si retour terrain
- Commentaires libres avocat sur chaque piste — V2 si retour terrain
- Requalification automatique IA des pistes RETAINED/DISCARDED entre analyses (pattern F-96 `checks_a_requalifier`) — V2

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `id` | UUID auto | Hibernate `@GeneratedValue` |
| `statut` | `TO_STUDY` | Imposé à la création depuis JSON IA |
| `raisonDiscard` | `null` | Optionnel — non rempli au début |
| `ordre` | Index dans JSON IA | Numérotation 0..N-1 selon ordre du JSON |
| `createdAt`, `updatedAt` | now() | `@PrePersist`, `@PreUpdate` |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées |
|-------|-------------|-------------|----------------------------|
| `texte` | Oui | 2000 (TEXT) | Non vide, trim() |
| `baseJuridique` | Non | 500 | Texte libre |
| `horizonTemporel` | Non | 255 | Texte libre |
| `conditions` | Non | — | Array de strings JSON, max 10 items, chaque item ≤ 500 char |
| `source` | Non | 500 | Texte libre |
| `statut` | Oui | 50 | TO_STUDY / RETAINED / DISCARDED |
| `raisonDiscard` | Non | 1000 (TEXT) | Texte libre, lecture seule sauf via PATCH |

---

## Technique

### Contrat API (figé pour SF-176-02)

#### Modèle TypeScript exposé (consommé par frontend)

```typescript
type StrategicOptionStatus = 'TO_STUDY' | 'RETAINED' | 'DISCARDED';

interface StrategicOption {
  id: string;                    // UUID
  texte: string;
  baseJuridique: string | null;
  horizonTemporel: string | null;
  conditions: string[];          // jamais null, [] si vide
  source: string | null;
  statut: StrategicOptionStatus;
  raisonDiscard: string | null;
  ordre: number;
  createdAt: string;             // ISO 8601
  updatedAt: string;             // ISO 8601
}
```

#### Endpoints

| Méthode | URL | Auth | Rôle min | Body | Réponse |
|---------|-----|------|---------|------|---------|
| GET | `/api/v1/case-files/{caseFileId}/analyses/{analysisId}/strategic-options` | Oui (OAuth2) | MEMBER | — | `200 List<StrategicOption>` (vide si pas de pistes) |
| PATCH | `/api/v1/strategic-options/{optionId}` | Oui (OAuth2) | MEMBER | `{"statut": "TO_STUDY"\|"RETAINED"\|"DISCARDED", "raisonDiscard": "..."?}` | `200 StrategicOption` |

Codes d'erreur :
- `400` body manquant / statut invalide / statut non listé
- `403` workspace différent (case-file ou option appartient à un autre workspace)
- `404` `case-file`, `analysis`, ou `option` inexistant

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `strategic_options` | CREATE (nouvelle) | Migration `197-create-strategic-options.xml` (numéro à confirmer au dev) |
| `case_analyses` | SELECT (FK) | Rien à modifier |
| `workspaces` | SELECT (FK) | Rien à modifier |

### Schéma `strategic_options`

```sql
CREATE TABLE strategic_options (
    id UUID PRIMARY KEY,
    case_analysis_id UUID NOT NULL REFERENCES case_analyses(id),
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    texte TEXT NOT NULL,
    base_juridique VARCHAR(500),
    horizon_temporel VARCHAR(255),
    conditions_json TEXT,            -- array of strings, JSON-encoded
    source VARCHAR(500),
    statut VARCHAR(50) NOT NULL DEFAULT 'TO_STUDY',
    raison_discard TEXT,
    ordre INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_strategic_options_case_analysis ON strategic_options(case_analysis_id, ordre);
CREATE INDEX idx_strategic_options_workspace ON strategic_options(workspace_id);
```

### Migration Liquibase

- [x] Oui — `db/changelog/migrations/197-create-strategic-options.xml` (à confirmer si 197 disponible au moment du dev — fallback 198, 199, etc.)

### Composants Angular

(N/A — couvert par SF-176-02 frontend)

---

## Plan de test

### Tests unitaires

- [ ] `StrategicOptionServiceTest.persistFromAnalysis_extractsStrategicOptionsFromJson()` — happy path
- [ ] `StrategicOptionServiceTest.persistFromAnalysis_failOpenOnInvalidJson()` — JSON malformé
- [ ] `StrategicOptionServiceTest.persistFromAnalysis_skipsEntriesWithEmptyText()` — guard
- [ ] `StrategicOptionServiceTest.persistFromAnalysis_setsDefaultStatusToToStudy()`
- [ ] `StrategicOptionServiceTest.list_returnsOrderedByOrdre()`
- [ ] `StrategicOptionServiceTest.list_throwsForbiddenIfWorkspaceMismatch()`
- [ ] `StrategicOptionServiceTest.updateStatus_persistsStatutAndRaisonDiscard()`
- [ ] `StrategicOptionServiceTest.updateStatus_throwsBadRequestIfStatutInvalid()`
- [ ] `StrategicOptionServiceTest.updateStatus_throwsForbiddenIfWorkspaceMismatch()`
- [ ] `StrategicOptionServiceTest.collectForEnrichment_returnsRetainedAndDiscarded()`
- [ ] `StrategicOptionServiceTest.propagateRetainedAndDiscarded_clonesWithStatutsPreserved()`
- [ ] `CaseAnalysisServiceTest.systemPrompt_containsPistesStrategiquesField()` — non-régression prompt
- [ ] `EnrichedAnalysisServiceTest.systemPrompt_containsPistesStrategiquesField()`
- [ ] `EnrichedAnalysisServiceTest.systemPrompt_instructsClaudeNotToReProposeDiscarded()` — règle prompt enrichi

### Tests d'intégration

- [ ] `StrategicOptionControllerIT.getList_returnsEmptyForAnalysisWithoutOptions()`
- [ ] `StrategicOptionControllerIT.getList_returnsOptionsOrdered()`
- [ ] `StrategicOptionControllerIT.getList_returns403ForCrossWorkspaceAccess()`
- [ ] `StrategicOptionControllerIT.patchStatus_persistsStatutAndRaison()`
- [ ] `StrategicOptionControllerIT.patchStatus_returns400ForInvalidStatut()`
- [ ] `StrategicOptionControllerIT.patchStatus_returns403ForCrossWorkspaceAccess()`
- [ ] `StrategicOptionControllerIT.patchStatus_returns404ForUnknownOption()`

### Isolation workspace

- [x] Applicable — tests `_returns403ForCrossWorkspaceAccess` couvrent le GET et le PATCH.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — la SF crée une nouvelle ressource isolée. Auth et workspace context sont consommés via les patterns existants (`@AuthenticationPrincipal OidcUser`, lookup workspace via Principal) sans modification.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné en SF-176-01 (pas de frontend ni de modification d'auth/workspace/navigation). SF-176-02 frontend gérera ses propres tests Jest.

---

## Dépendances

### Subfeatures bloquantes

- **SF-96-06** (durcissement prompt `points_procedure`) — Done (PR #708 mergée 2026-04-30, commit `7dcc9ac8`). Sans elle, le prompt mettrait les pistes dans 2 endroits (doublon).

### Subfeatures parallèles

- **SF-176-02** frontend — démarrable en parallèle (contrat API figé ci-dessus, mini-spec à écrire après).

### Subfeatures débloquées

- **F-IM-21 SF-IM-21-02** — extension prompt avec codes IM21_* binaires. Démarrable une fois F-176 stabilisée (le contenu stratégique est dans `pistes_strategiques`, le contenu binaire dans `points_procedure`).

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` n'est tranchée par cette SF.

---

## Notes et décisions

- **Pourquoi `case_analysis_id` (pas `case_file_id`) en FK ?** Pattern miroir F-96 `procedure_checks` — chaque analyse a ses pistes, propagation explicite entre analyses via `propagateRetainedAndDiscarded`. Permet à l'avocat d'avoir une trace par itération.
- **Pourquoi pas une colonne `previous_option_id` pour tracer la généalogie des pistes clonées ?** Inutile en V1 (l'avocat voit la liste de l'analyse en cours, pas l'historique). Si retour terrain, ajouter en V2 sans rétrocompat (`null` pour les pistes existantes).
- **Pourquoi `conditions` en JSON dans une colonne TEXT et pas une table N-N ?** Simplicité — les `conditions` ne sont jamais requêtées indépendamment, c'est un attribut display-only. Pattern miroir des champs `pieces_manquantes` / `points_procedure` qui restent en JSON dans `case_analyses.analysis_result`.
- **Pourquoi le DEFAULT statut = TO_STUDY ?** Cohérence UX — l'avocat reçoit les pistes "neutres", il décide ensuite ce qu'il garde / écarte / explore.
- **Pourquoi pas de F-IA-03 sur les pistes ?** Décision explicite dans la backlog spec F-176 : les pistes sont par essence ouvertes (l'avocat est libre de retenir une option même si elle contredit l'analyse IA brute). F-IA-03 produirait des fausses alertes.
- **Pourquoi 2 sections de propagation distinctes (RETAINED vs DISCARDED) ?** Sens opposé : RETAINED = "approfondis ça", DISCARDED = "ne re-propose plus ça". Mélanger les 2 dans une section confuserait Claude.
- **Compatibilité H2 (dev profile) ?** TEXT, VARCHAR, UUID, TIMESTAMP, INT, FK : tout supporté par H2 et PostgreSQL. JSON-as-TEXT compatible (pas de JSONB). OK.
