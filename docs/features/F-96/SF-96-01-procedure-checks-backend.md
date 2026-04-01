# Mini-spec — F-96 / SF-96-01 : Checklist procédurale — Backend

> Ce document doit être validé AVANT de démarrer le dev.

---

## Identifiant

`F-96 / SF-96-01`

## Feature parente

`F-96` — Checklist procédurale interactive

## Statut

`draft`

## Date de création

`2026-04-01`

## Branche Git

`feat/SF-96-01-procedure-checks-backend`

---

## Objectif

Ajouter un champ `points_procedure` au JSON produit par la case analysis, créer automatiquement les lignes `procedure_checks` en base à la fin de chaque analyse, et exposer les endpoints pour lire et mettre à jour les statuts.

---

## Comportement attendu

### Cas nominal

1. L'analyse se termine → le JSON contient `"points_procedure": ["Entretien préalable tenu dans les délais", "Lettre motivée avec cause réelle et sérieuse", ...]`
2. `finalizeCaseAnalysis()` extrait les items et crée une ligne `procedure_checks` par item avec statut `TO_CHECK`
3. `GET /api/v1/case-files/{id}/analyses/{analysisId}/procedure-checks` → retourne la liste ordonnée
4. `PATCH /api/v1/procedure-checks/{checkId}` → `{"statut": "VERIFIED"}` met à jour le statut

**Statuts possibles :** `TO_CHECK` (défaut) / `VERIFIED` / `NON_COMPLIANT`

### Cas dégradés

| Situation | Comportement attendu |
|-----------|---------------------|
| LLM n'inclut pas `points_procedure` | Aucun check créé, fail-open, analyse sauvegardée normalement |
| JSON malformé | Aucun check créé, analyse sauvegardée normalement |
| `PATCH` workspace différent | 403 |
| `PATCH` statut invalide | 400 |
| Analyse ENRICHED | Mêmes règles — nouveaux checks créés, remplacent les anciens de la version précédente |

---

## Critères d'acceptation

- [ ] `SYSTEM_PROMPT_TEMPLATE` inclut `points_procedure` dans le format JSON (CaseAnalysisService + EnrichedAnalysisService)
- [ ] `CaseAnalysisResponse` expose `List<String> pointsProcedure` (extraction fail-open)
- [ ] `finalizeCaseAnalysis()` appelle `ProcedureCheckService.createChecks()` après persistance de l'analyse
- [ ] `finalizeEnrichedAnalysis()` idem
- [ ] `GET /case-files/{id}/analyses/{analysisId}/procedure-checks` retourne la liste ordonnée, isolée workspace
- [ ] `PATCH /procedure-checks/{checkId}` met à jour le statut, vérifie workspace
- [ ] Ancien JSON sans `points_procedure` → liste vide retournée, aucune erreur (rétrocompatibilité)
- [ ] Tests unitaires extraction + création des checks
- [ ] Tests IT sur les 2 endpoints

---

## Périmètre

### Hors scope (explicite)

- Affichage frontend — SF-96-02
- Injection des points NON_COMPLIANT dans le prompt enrichi — SF-96-03
- Modification du PDF export

---

## Technique

### Nouvelle table

```
procedure_checks
├── id            UUID          PK
├── case_analysis_id UUID       FK → case_analyses(id)
├── workspace_id  UUID          FK → workspaces(id)  -- isolation
├── ordre         INT           NOT NULL
├── description   TEXT          NOT NULL
├── statut        VARCHAR(20)   NOT NULL DEFAULT 'TO_CHECK'
├── created_at    TIMESTAMPTZ   NOT NULL
└── updated_at    TIMESTAMPTZ   NOT NULL
```

### Migration Liquibase

- [ ] Oui — `040-create-procedure-checks.xml`

### Nouveaux fichiers

| Fichier | Rôle |
|---------|------|
| `ProcedureCheck.java` | Entité JPA |
| `ProcedureCheckRepository.java` | Repository |
| `ProcedureCheckStatus.java` | Enum TO_CHECK / VERIFIED / NON_COMPLIANT |
| `ProcedureCheckService.java` | Création depuis JSON + update statut |
| `ProcedureCheckController.java` | GET + PATCH |
| `ProcedureCheckResponse.java` | DTO réponse |

### Fichiers modifiés

| Fichier | Modification |
|---------|-------------|
| `CaseAnalysisService.java` | `SYSTEM_PROMPT_TEMPLATE` + appel `ProcedureCheckService` dans `finalizeCaseAnalysis()` |
| `EnrichedAnalysisService.java` | Idem dans `finalizeEnrichedAnalysis()` |
| `CaseAnalysisResponse.java` | Champ `pointsProcedure: List<String>` |
| `AnalysisLimitsProperties.java` | `pointsProcedure` max (défaut 8) |
| `application.yml` | `points-procedure: 8` sur les 3 domaines |

### Endpoints

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{id}/analyses/{analysisId}/procedure-checks` | Oui | MEMBER |
| PATCH | `/api/v1/procedure-checks/{checkId}` | Oui | LAWYER |

---

## Plan de test

### Tests unitaires

- [ ] `ProcedureCheckService` — extraction depuis JSON avec liste → N checks créés avec ordre et description corrects
- [ ] `ProcedureCheckService` — champ `points_procedure` absent → 0 checks, pas d'exception
- [ ] `CaseAnalysisResponse` — extraction `pointsProcedure` présent / absent / vide

### Tests d'intégration

- [ ] Analyse finalisée (mock) → `GET /procedure-checks` retourne les checks en statut `TO_CHECK`
- [ ] `PATCH` statut `VERIFIED` → 200, statut mis à jour en base
- [ ] `PATCH` statut invalide → 400
- [ ] `PATCH` avec workspace différent → 403
- [ ] `GET` analyse appartenant à un autre workspace → 403

### Isolation workspace

- [ ] Applicable — `workspace_id` vérifié sur GET (via case_file) et PATCH (via procedure_check)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — nouveau composant isolé, aucun changement auth/routing/plans

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné

---

## Dépendances

### Subfeatures bloquantes

- Aucune

### Questions ouvertes

- Aucune

---

## Notes et décisions

- `workspace_id` dénormalisé sur `procedure_checks` pour simplifier les requêtes d'isolation (même pattern que `case_deadlines`)
- Les checks sont recréés à chaque nouvelle analyse (STANDARD ou ENRICHED) — pas de merge avec les checks précédents
- `ordre` correspond à l'index dans le tableau `points_procedure` du JSON LLM
