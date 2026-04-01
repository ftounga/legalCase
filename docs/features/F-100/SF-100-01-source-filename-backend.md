# Mini-spec — F-100 / SF-100-01 — Noms de documents dans les sources IA (backend)

## Identifiant
`F-100 / SF-100-01`

## Feature parente
`F-100` — Noms de documents dans les sources IA

## Statut
`ready`

## Date de création
2026-04-01

## Branche Git
`feat/SF-100-01-source-filename-backend`

---

## Objectif

Faire retourner par le LLM le nom de fichier comme référence source (au lieu de "Document N"), et exposer le snapshot des documents dans `CaseAnalysisResponse`.

---

## Comportement attendu

### Cas nominal

1. `CaseAnalysisService.buildAggregatedPrompt()` utilise le nom de fichier seul comme label : `"contrat.pdf : <résultat>"`. Fallback : `"document-<index>"` si le nom est null.
2. Le system prompt de `CaseAnalysisService` et `EnrichedAnalysisService` est mis à jour : `"source": "<nom exact du fichier>"` au lieu de `"source": "Document N"`.
3. `CaseAnalysisResponse` expose `List<AnalysisDocumentEntry> analysisDocuments` trié par `createdAt`, où `AnalysisDocumentEntry` est `{int index, String name}`.
4. `CaseAnalysisController.get()` charge les documents via `AnalysisDocumentRepository.findByAnalysisIdOrderByCreatedAt()` et les passe à la réponse.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `originalFilename` null | Fallback label `"document-<index>"` |
| `analysis_documents` vide (ancienne analyse sans snapshot) | `analysisDocuments: []` — pas d'erreur |
| LLM retourne quand même "Document N" | Le frontend résout rétroactivement via le snapshot (SF-100-02) |

---

## Critères d'acceptation

- [ ] `buildAggregatedPrompt()` envoie `"contrat.pdf : ..."` au lieu de `"Document 0 (contrat.pdf) : ..."`
- [ ] System prompt mis à jour dans `CaseAnalysisService` et `EnrichedAnalysisService`
- [ ] `CaseAnalysisResponse` contient `analysisDocuments: [{index: 0, name: "contrat.pdf"}, ...]`
- [ ] Analyses sans snapshot retournent `analysisDocuments: []` sans erreur
- [ ] Aucune migration Liquibase nécessaire

---

## Périmètre

### Hors scope

- Modification du frontend
- Enrichissement du snapshot (nouveaux champs)
- Résolution dans les exports PDF/DOCX

---

## Technique

### Endpoint(s)

| Méthode | URL | Changement |
|---------|-----|-----------|
| GET | `/api/v1/case-files/{id}/analyses/latest` | Nouveau champ `analysisDocuments` dans la réponse |
| GET | `/api/v1/case-files/{id}/analyses/{analysisId}` | Idem |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `analysis_documents` | SELECT | Lecture du snapshot existant |

### Migration Liquibase
- [x] Non applicable

### Composants impactés

- `CaseAnalysisService` — `buildAggregatedPrompt()` + system prompt
- `EnrichedAnalysisService` — system prompt
- `CaseAnalysisResponse` — nouveau champ + record interne `AnalysisDocumentEntry`
- `CaseAnalysisController` — passe les documents à `CaseAnalysisResponse`

---

## Plan de test

### Tests unitaires

- [ ] `CaseAnalysisResponseTest` — `analysisDocuments` correctement peuplé depuis liste de documents
- [ ] `CaseAnalysisResponseTest` — `analysisDocuments: []` si liste vide

### Tests d'intégration

- [ ] `GET /case-files/{id}/analyses/latest` → champ `analysisDocuments` présent et correctement ordonné
- [ ] `GET /case-files/{id}/analyses/latest` → `analysisDocuments: []` si aucun snapshot

### Isolation workspace

- [x] Non applicable — lecture seule sur données déjà isolées par l'analyse parente

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné

---

## Notes et décisions

- `AnalysisDocumentEntry` est un record interne à `CaseAnalysisResponse` : `record AnalysisDocumentEntry(int index, String name) {}`
- Le tri par `createdAt` dans `AnalysisDocumentRepository` garantit l'ordre cohérent avec `buildAggregatedPrompt()`
- L'`EnrichedAnalysisService` utilise la synthèse précédente comme prompt (pas `buildAggregatedPrompt`) — seul son system prompt est à modifier
