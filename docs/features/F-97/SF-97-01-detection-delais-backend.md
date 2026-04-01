# Mini-spec — F-97 / SF-97-01 Détection automatique des délais — Backend

## Identifiant

`F-97 / SF-97-01`

## Feature parente

`F-97` — Détection automatique des délais légaux

## Statut

`draft`

## Date de création

2026-04-01

## Branche Git

`feat/SF-97-01-detection-delais-backend`

---

## Objectif

Étendre le pipeline IA pour détecter automatiquement les délais légaux applicables depuis les documents du dossier, les persister comme délais `PENDING` sur `CaseDeadline`, et exposer un endpoint de validation (accepter / rejeter) à l'avocat.

---

## Comportement attendu

### Cas nominal

1. Lors d'une analyse (STANDARD ou ENRICHED), le LLM détecte des délais à partir des documents.
2. Le prompt système est enrichi d'une instruction de détection : extraire `delais_detectes` (label, date, source).
3. Après l'analyse, `CaseAnalysisService` appelle `CaseDeadlineService.createAiDetectedDeadlines(analysis, parsed)`.
4. Chaque délai détecté est persisté comme `CaseDeadline` avec `source = AI` et `aiStatus = PENDING`.
5. Les délais PENDING sont retournés dans `GET /api/v1/case-files/{id}/deadlines` avec les champs `source` et `aiStatus`.
6. L'avocat appelle `PATCH /api/v1/case-files/{id}/deadlines/{deadlineId}/validate` avec `{"action": "ACCEPT" | "REJECT"}`.
   - `ACCEPT` → `aiStatus = ACCEPTED` (le délai devient un délai normal, visible dans la section délais)
   - `REJECT` → suppression du délai de la base

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| LLM ne retourne pas `delais_detectes` | Fail-open : aucun délai créé, analyse non bloquée | — |
| `delais_detectes` mal formé (date invalide) | Entrée ignorée silencieusement, les autres sont créées | — |
| `deadlineId` inexistant | 404 | 404 |
| `deadlineId` appartient à un autre workspace | 403 | 403 |
| `action` invalide (ni ACCEPT ni REJECT) | 400 | 400 |
| Délai non PENDING (déjà accepté/rejeté) | 409 — "Ce délai a déjà été traité." | 409 |

---

## Critères d'acceptation

- [ ] `CaseDeadline` a deux nouveaux champs : `source` (MANUAL/AI, défaut MANUAL) et `ai_status` (PENDING/ACCEPTED/REJECTED, nullable — non null si source=AI)
- [ ] Migration Liquibase `042-add-ai-fields-to-case-deadlines.xml` ajoutant `source` et `ai_status`
- [ ] Le prompt système inclut l'instruction de détection des délais avec le format `delais_detectes`
- [ ] Après une analyse, les délais détectés sont persistés avec `source=AI`, `ai_status=PENDING`
- [ ] La détection est fail-open : une exception dans le parsing n'interrompt pas l'analyse
- [ ] `CaseDeadlineResponse` expose `source` et `aiStatus`
- [ ] `GET /deadlines` retourne les délais PENDING (inclus dans la liste avec les autres)
- [ ] `PATCH /deadlines/{id}/validate` avec `ACCEPT` → `ai_status = ACCEPTED`
- [ ] `PATCH /deadlines/{id}/validate` avec `REJECT` → suppression du délai
- [ ] Isolation workspace : 403 si le délai appartient à un autre workspace
- [ ] Tests unitaires sur le parsing fail-open
- [ ] Tests d'intégration sur PATCH validate (ACCEPT, REJECT, 404, 403, 409)

---

## Périmètre

### Hors scope (explicite)

- UI frontend (SF-97-02)
- Détection de délais sur les re-synthèses enrichies (même pipeline, couvert implicitement)
- Déduplication des délais entre analyses successives (V4)
- Délais issus d'une source externe (API jurisprudence, etc.)

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `source` | `MANUAL` | Pour tous les délais créés manuellement (rétrocompatibilité) |
| `ai_status` | `NULL` | Non null uniquement si `source = AI` |

Pour les délais AI créés par le pipeline :
- `source = AI`
- `ai_status = PENDING`
- `label` = label retourné par le LLM
- `dueDate` = date retournée par le LLM (parsée en `LocalDate`)

---

## Contraintes de validation

| Champ | Obligatoire | Format | Notes |
|-------|-------------|--------|-------|
| `source` | Oui | MANUAL / AI | Défaut MANUAL pour les créations manuelles |
| `ai_status` | Conditionnel | PENDING / ACCEPTED / REJECTED | Null si source=MANUAL, non null si source=AI |
| `action` (PATCH validate) | Oui | ACCEPT / REJECT | 400 si autre valeur |

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{id}/deadlines` | Oui | MEMBER (existant, enrichi) |
| PATCH | `/api/v1/case-files/{id}/deadlines/{deadlineId}/validate` | Oui | LAWYER |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `case_deadlines` | ALTER | Ajout colonnes `source VARCHAR(10)`, `ai_status VARCHAR(20)` |

### Migration Liquibase

- [x] Oui — `042-add-ai-fields-to-case-deadlines.xml`
  - `ALTER TABLE case_deadlines ADD COLUMN source VARCHAR(10) NOT NULL DEFAULT 'MANUAL'`
  - `ALTER TABLE case_deadlines ADD COLUMN ai_status VARCHAR(20) NULL`

### Extension du prompt système (CaseAnalysisService)

Ajout dans `SYSTEM_PROMPT_TEMPLATE` :

```
Le champ "delais_detectes" liste les délais légaux détectés dans les documents (ex: délai de recours, délai de prescription). Format : [{"label": "Délai de recours prud'homal", "date_detectee": "YYYY-MM-DD", "source": "Document N"}]. Si aucun délai détectable, utilise "delais_detectes": [].
```

Format JSON enrichi : `{..., "delais_detectes": [{"label": "...", "date_detectee": "YYYY-MM-DD", "source": "..."}]}`

### Nouveaux composants / modifications Java

- `CaseDeadline` — champs `source` (String, défaut "MANUAL"), `aiStatus` (String, nullable)
- `CaseDeadlineResponse` — champs `source`, `aiStatus`
- `CaseDeadlineValidateRequest` — record `{String action}` avec `@Pattern(regexp = "ACCEPT|REJECT")`
- `CaseDeadlineService.createAiDetectedDeadlines(CaseAnalysis, AnalysisResult)` — persiste les délais PENDING
- `CaseDeadlineService.validate(caseFileId, deadlineId, action, user)` — ACCEPT ou DELETE
- `CaseDeadlineController` — ajouter `PATCH /{deadlineId}/validate`
- `CaseAnalysisService` — appel à `createAiDetectedDeadlines` après parsing (fail-open, dans un try/catch)

---

## Plan de test

### Tests unitaires

- [ ] `CaseDeadlineService.createAiDetectedDeadlines` — 2 délais valides → 2 persistés
- [ ] `CaseDeadlineService.createAiDetectedDeadlines` — date invalide → entrée ignorée, pas d'exception
- [ ] `CaseDeadlineService.createAiDetectedDeadlines` — `delais_detectes` absent → aucun délai créé
- [ ] `CaseDeadlineService.validate` — ACCEPT → `ai_status = ACCEPTED`
- [ ] `CaseDeadlineService.validate` — REJECT → délai supprimé
- [ ] `CaseDeadlineService.validate` — délai déjà ACCEPTED → 409

### Tests d'intégration

- [ ] `PATCH /validate` avec ACCEPT → 200, `aiStatus = ACCEPTED` dans réponse
- [ ] `PATCH /validate` avec REJECT → 204 (ou 200), délai absent de `GET /deadlines`
- [ ] `PATCH /validate` avec action invalide → 400
- [ ] `PATCH /validate` avec `deadlineId` inexistant → 404
- [ ] `PATCH /validate` avec délai d'un autre workspace → 403
- [ ] `PATCH /validate` sur délai déjà traité → 409
- [ ] `GET /deadlines` retourne les délais PENDING avec `source=AI` et `aiStatus=PENDING`

### Isolation workspace

- [x] Applicable — test : workspace A ne peut pas valider un délai du workspace B (403 attendu).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Workspace context** — `validate` résout le `caseFile` depuis le workspace de l'utilisateur connecté (pattern identique à `CaseDeadlineService.delete` existant).

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression |
|----------------------|-----------------|----------------------|
| `GET /deadlines` | Retourne maintenant `source` + `aiStatus` — rétrocompat OK (champs additionnels) | Test existant + nouveau test |
| `CaseAnalysisService` | Appel `createAiDetectedDeadlines` ajouté — fail-open → pas de régression | Test IT analyse complète |

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — pas de route ni guard modifié.

---

## Dépendances

### Subfeatures bloquantes

- SF-97-02 (frontend) est bloquée par SF-97-01.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- Fail-open obligatoire : si la détection plante (JSON mal formé, date non parseable), l'analyse n'est pas bloquée. Les délais non parsés sont silencieusement ignorés.
- Rétrocompatibilité : les `CaseDeadline` existants sans `source` reçoivent `MANUAL` via la valeur DEFAULT de la migration.
- `REJECT` = suppression physique (pas de soft-delete) — un délai rejeté n'a pas de valeur métier à conserver.
- On n'ajoute pas de déduplication entre analyses pour cette version (si l'avocat relance une analyse, de nouveaux délais PENDING peuvent être créés).
