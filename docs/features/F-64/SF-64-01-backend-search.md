# SF-64-01 — Backend recherche full-text synthèses

**Feature parente :** F-64 — Recherche full-text dans les synthèses
**Branche :** feat/SF-64-01-backend-search
**Statut :** ready
**Date de création :** 2026-03-29

---

## Objectif

Exposer un endpoint `GET /api/v1/search?q=<terme>` qui recherche un mot-clé dans toutes les synthèses DONE du workspace de l'utilisateur connecté, et retourne une liste de résultats avec extraits contextuels.

---

## Comportement attendu

### Cas nominal

1. Utilisateur authentifié appelle `GET /api/v1/search?q=licenciement`.
2. Le backend résout son workspace primaire.
3. Il récupère toutes les `case_analyses` de statut `DONE` du workspace, version la plus récente par dossier.
4. Il filtre celles dont `analysis_result` contient le terme (insensible à la casse, ILIKE `%terme%`).
5. Pour chaque résultat, il extrait jusqu'à 3 extraits contextuels (faits / points_juridiques / risques / questionsOuvertes) contenant le terme.
6. Retourne la liste triée par `case_file.created_at DESC`.

### Réponse

```json
{
  "query": "licenciement",
  "totalResults": 2,
  "results": [
    {
      "caseFileId": "uuid",
      "caseFileTitle": "Dossier Dupont",
      "legalDomain": "DROIT_DU_TRAVAIL",
      "analysisType": "ENRICHED",
      "matchCount": 4,
      "excerpts": [
        "Le licenciement a été notifié le 15 janvier 2024.",
        "Risque de contestation du licenciement pour motif personnel."
      ]
    }
  ]
}
```

### Cas d'erreur

| Situation | Réponse |
|-----------|---------|
| `q` absent ou vide | 400 |
| `q` < 2 caractères | 400 |
| `q` > 200 caractères | 400 |
| Workspace non trouvé | 404 |
| Aucun résultat | 200 avec `results: []` |

---

## Critères d'acceptation

- [ ] `GET /api/v1/search?q=terme` → liste des dossiers matchants scoped au workspace
- [ ] Recherche insensible à la casse
- [ ] Seule la version la plus récente DONE par dossier est retournée
- [ ] Jusqu'à 3 extraits par résultat, contenant le terme
- [ ] `q` vide ou < 2 chars → 400
- [ ] Cross-workspace impossible (isolation garantie)
- [ ] Dossier sans synthèse DONE → absent des résultats
- [ ] Aucun résultat → 200 `results: []`

---

## Périmètre

### Hors scope

- Indexation PostgreSQL tsvector / GIN (V3+)
- Recherche dans le contenu des documents bruts (extracted_text)
- Pagination des résultats (max 50 résultats, V2 acceptable)
- Highlighting HTML des termes trouvés
- Recherche multi-termes / opérateurs booléens

---

## Technique

### Endpoint

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/search?q=<terme>` | Oui | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `case_analyses` | SELECT | Filtre ILIKE sur `analysis_result`, statut DONE |
| `case_files` | SELECT (JOIN) | Pour récupérer `title`, `legal_domain`, `workspace_id` |

### Migration Liquibase

Non requise — lecture seule, pas de changement de schéma.

### Composants nouveaux

- **`SynthesisSearchController`** — `GET /api/v1/search`
- **`SynthesisSearchService`** — logique : requête repo + extraction d'extraits
- **`SynthesisSearchRepository`** — native query ILIKE
- **`SynthesisSearchResult`** (record) — DTO de résultat par dossier
- **`SynthesisSearchResponse`** (record) — DTO de réponse globale

---

## Plan de test

### Tests unitaires

- [ ] `SynthesisSearchService` — terme trouvé dans faits → extrait retourné
- [ ] `SynthesisSearchService` — terme insensible à la casse
- [ ] `SynthesisSearchService` — max 3 extraits par résultat
- [ ] `SynthesisSearchService` — résultat vide si aucun match

### Tests d'intégration

- [ ] `GET /api/v1/search?q=terme` → 200 avec résultats
- [ ] `GET /api/v1/search?q=` → 400
- [ ] `GET /api/v1/search?q=x` → 400 (< 2 chars)
- [ ] `GET /api/v1/search` sans param → 400
- [ ] Isolation workspace — résultats du workspace A invisibles depuis workspace B

### Isolation workspace

- [ ] Applicable — test : utilisateur workspace B ne voit pas les synthèses workspace A

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Workspace context** — résolution du workspace courant pour scoper la recherche

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|-----------------|------------------------------|
| `CaseAnalysisReadController` | Aucun — nouveau controller indépendant | N/A |
| `CaseAnalysisQueryService` | Aucun — nouvelle couche service distincte | N/A |

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — nouvel endpoint sans impact sur les flux existants

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- Recherche ILIKE sur le champ `analysis_result TEXT` — pas d'index dédié pour V2. Performance acceptable jusqu'à ~1000 dossiers par workspace.
- L'extraction d'extraits est faite côté application (Java) : parsing JSON de `analysis_result`, scan des listes `faits`, `points_juridiques`, `risques`, `questionsOuvertes`. La timeline est exclue des extraits (format `{date, evenement}` moins lisible).
- Seule la version max DONE par `case_file_id` est incluse dans la recherche (sous-requête `MAX(version)`).
- Limite : 50 résultats max retournés (pas de pagination pour V2).
