# Mini-spec — F-12 / SF-12-01 — API REST restitution de l'analyse

## Identifiant
`F-12 / SF-12-01`

## Feature parente
`F-12` — Restitution de l'analyse

## Statut
`in-progress`

## Date de création
`2026-03-18`

## Branche Git
`feat/SF-12-01-api-case-analysis`

---

## Objectif

Exposer un endpoint REST permettant au frontend de consulter la synthèse globale d'un dossier une fois l'analyse terminée.

---

## Comportement attendu

### Cas nominal

`GET /api/v1/case-files/{caseFileId}/case-analysis`

Retourne la synthèse structurée parsée depuis `case_analyses.analysis_result`.

Réponse 200 :
```json
{
  "status": "DONE",
  "timeline": [
    {"date": "2024-01-15", "evenement": "Signature du contrat de travail"},
    {"date": "2024-09-15", "evenement": "Notification de licenciement"}
  ],
  "faits": ["Le salarié a été embauché le 15 janvier 2024.", "..."],
  "pointsJuridiques": ["Article L1232-1 du Code du travail — cause réelle et sérieuse", "..."],
  "risques": ["Risque de requalification du licenciement en licenciement sans cause réelle", "..."],
  "questionsOuvertes": ["Les délais de procédure ont-ils été respectés ?", "..."],
  "modelUsed": "claude-sonnet-4-6",
  "updatedAt": "2026-03-18T10:00:00Z"
}
```

Si `analysis_result` ne contient pas de champ (ex : analyse d'avant SF-10-03) → liste vide pour le champ manquant.

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| Aucune analyse DONE pour ce dossier | 404 Not Found | 404 |
| `caseFileId` inexistant | 404 Not Found | 404 |
| Dossier appartenant à un autre workspace | 404 (isolation) | 404 |
| Utilisateur non authentifié | 401 Unauthorized | 401 |

---

## Critères d'acceptation

- [x] `GET /api/v1/case-files/{caseFileId}/case-analysis` retourne 200 avec la synthèse parsée
- [x] `timeline`, `faits`, `pointsJuridiques`, `risques`, `questionsOuvertes` correctement extraits du JSON
- [x] Champ manquant dans le JSON → liste vide (pas d'erreur)
- [x] 404 si aucune analyse DONE
- [x] 404 si dossier inexistant
- [x] 404 si dossier appartient à un autre workspace (isolation)
- [x] 401 si non authentifié
- [x] Tests d'intégration couvrant les cas ci-dessus

---

## Périmètre

### Hors scope (explicite)

- Affichage frontend (SF-12-02)
- Restitution des analyses document par document
- Déclenchement ou relance de l'analyse

---

## Technique

### Endpoint

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{caseFileId}/case-analysis` | Oui | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `case_analyses` | SELECT | Filtre par case_file_id + status = DONE, ordre updatedAt DESC, LIMIT 1 |
| `case_files` | SELECT | Vérification existence + workspace |

### Nouveaux composants

| Composant | Rôle |
|-----------|------|
| `CaseAnalysisResponse` | Record de réponse (timeline, faits, pointsJuridiques, risques, questionsOuvertes, modelUsed, updatedAt) |
| `CaseAnalysisQueryService` | Résolution user→workspace, vérification isolation, parsing JSON |
| `CaseAnalysisController` | GET endpoint |

### Parsing JSON

`analysis_result` est un TEXT libre produit par le LLM. Le service parse avec `ObjectMapper`. Si un champ est absent → `List.of()`.

### Migration Liquibase
Non applicable.

---

## Plan de test

### Tests unitaires

- [x] `CaseAnalysisResponse.from()` — parsing nominal (tous les champs présents)
- [x] `CaseAnalysisResponse.from()` — champ manquant → liste vide
- [x] `CaseAnalysisResponse.from()` — `analysis_result` null → toutes les listes vides

### Tests d'intégration

- [x] `GET /{id}/case-analysis` → 200 avec synthèse parsée (timeline + faits)
- [x] `GET /{id}/case-analysis` → 404 si aucune analyse DONE
- [x] `GET /{unknown}/case-analysis` → 404
- [x] `GET /{autreWorkspace}/case-analysis` → 404 (isolation workspace)
- [x] `GET /{id}/case-analysis` sans auth → 401

---

## Dépendances

### Subfeatures bloquantes
- SF-10-03 — statut : done

### Débloque
- F-12 / SF-12-02 (frontend synthèse)

---

## Notes et décisions

- Pattern identique à `AnalysisJobQueryService` pour la résolution user→workspace
- Retourne uniquement la dernière analyse DONE (cas de relance future)
- Nommage camelCase dans le JSON de réponse (`pointsJuridiques`, `questionsOuvertes`) — cohérent avec les conventions Angular
