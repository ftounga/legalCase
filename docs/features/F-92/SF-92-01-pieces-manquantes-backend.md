# Mini-spec — F-92 / SF-92-01 : Détection de pièces manquantes — Backend

> Ce document doit être validé AVANT de démarrer le dev.

---

## Identifiant

`F-92 / SF-92-01`

## Feature parente

`F-92` — Détection de pièces manquantes

## Statut

`draft`

## Date de création

`2026-03-31`

## Branche Git

`feat/SF-92-01-pieces-manquantes-backend`

---

## Objectif

Ajouter un champ `pieces_manquantes` au JSON produit par l'analyse de dossier, en enrichissant le system prompt de `CaseAnalysisService` pour que l'IA signale les pièces attendues qui sont absentes du dossier.

---

## Comportement attendu

### Cas nominal

Quand une case analysis se termine, le JSON `analysisResult` contient un champ supplémentaire :
```json
{
  "timeline": [...],
  "faits": [...],
  "points_juridiques": [...],
  "risques": [...],
  "questions_ouvertes": [...],
  "pieces_manquantes": ["Contrat de travail", "Bulletins de salaire des 3 derniers mois"]
}
```
Le champ est extrait dans `CaseAnalysisResponse` et retourné dans l'API.

### Cas vides / dégradés

| Situation | Comportement attendu |
|-----------|---------------------|
| Dossier complet selon le LLM | `pieces_manquantes: []` |
| LLM omet le champ `pieces_manquantes` | `pieces_manquantes: []` — fail-open, pas d'exception |
| JSON malformé | Comportement actuel inchangé — toutes les listes vides |
| Analyse ENRICHED | Même logique — champ inclus aussi dans le prompt enrichi |

---

## Critères d'acceptation

- [ ] `SYSTEM_PROMPT_TEMPLATE` dans `CaseAnalysisService` inclut `pieces_manquantes` dans le format JSON attendu avec contrainte de longueur configurable (via `AnalysisLimitsProperties`)
- [ ] `CaseAnalysisResponse` expose `List<String> piecesManquantes`
- [ ] `CaseAnalysisResponse.from()` extrait `pieces_manquantes` via `extractStringList(root, "pieces_manquantes")`
- [ ] Si le champ est absent du JSON LLM → liste vide retournée (pas d'exception)
- [ ] `GET /api/v1/case-files/{id}/analyses/{analysisId}` retourne `piecesManquantes` dans la réponse JSON
- [ ] Tests unitaires sur l'extraction (présent / absent / liste vide)
- [ ] Tests d'intégration : réponse API contient `piecesManquantes` non null après analyse

---

## Périmètre

### Hors scope (explicite)

- Affichage frontend — SF-92-02
- Nouveau champ en base (`case_analyses`) — stocké dans `analysis_result` JSON existant, pas de migration
- Enriched analysis prompt (`EnrichedCaseAnalysisService`) — à vérifier si le même prompt est réutilisé ou dupliqué

---

## Technique

### Fichiers modifiés

| Fichier | Modification |
|---------|-------------|
| `CaseAnalysisService.java` | `SYSTEM_PROMPT_TEMPLATE` : ajout `pieces_manquantes` dans le format JSON + contrainte longueur |
| `CaseAnalysisResponse.java` | Nouveau champ `List<String> piecesManquantes` dans le record + extraction dans `from()` |
| `AnalysisLimitsProperties.java` | Ajout `piecesManquantes` dans `LevelLimits` (max configurable, défaut : 5) |

### Endpoint impacté (existant, pas nouveau)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{id}/analyses/{analysisId}` | Oui | MEMBER |

### Migration Liquibase

- [x] Non applicable — `pieces_manquantes` est stocké dans la colonne `analysis_result` TEXT existante

---

## Plan de test

### Tests unitaires

- [ ] `CaseAnalysisResponseTest` — extraction `piecesManquantes` : JSON avec liste non vide → retourne la liste
- [ ] `CaseAnalysisResponseTest` — champ `pieces_manquantes` absent du JSON → liste vide
- [ ] `CaseAnalysisResponseTest` — champ `pieces_manquantes: []` → liste vide

### Tests d'intégration

- [ ] Analyse terminée (mockée) → `GET /api/v1/case-files/{id}/analyses/{analysisId}` retourne `piecesManquantes` non null

### Isolation workspace

- [ ] Non applicable — pas de nouvelle ressource, isolation déjà garantie par l'analyse parente

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — modification du prompt IA et ajout d'un champ dans la réponse existante, aucun changement auth/workspace/plans/routing

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — pas de changement de routing ni d'auth

---

## Dépendances

### Subfeatures bloquantes

- Aucune

### Questions ouvertes impactées

- Aucune

---

## Notes et décisions

- Option A validée : signal dans la synthèse après analyse, pas avant l'upload
- `pieces_manquantes` est inféré par le LLM depuis le contenu des documents + le domaine juridique du workspace (déjà disponible dans le prompt via `LegalDomainPromptBuilder`)
- Fail-open : si le LLM n'inclut pas le champ, la synthèse reste valide avec liste vide
- Aucune migration DB requise — le JSON `analysis_result` est une colonne TEXT libre
