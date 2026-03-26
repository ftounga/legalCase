# Mini-spec — F-54 / SF-54-01 — Backend diff analyses

> Ce document doit être validé AVANT de démarrer le dev.

---

## Identifiant

`F-54 / SF-54-01`

## Feature parente

`F-54` — Comparaison inter-analyses (diff)

## Statut

`draft`

## Date de création

2026-03-26

## Branche Git

`feat/SF-54-01-backend-diff-analyses`

---

## Objectif

Exposer un endpoint permettant de calculer le diff sémantique item par item entre deux versions de synthèse d'un dossier (tous types : STANDARD et ENRICHED), sur tous les champs (faits, points juridiques, risques, questions ouvertes, timeline).

---

## Comportement attendu

### Cas nominal

**Endpoint** : `GET /api/v1/case-files/{caseFileId}/case-analysis/diff?fromId={uuid}&toId={uuid}`

- Les deux analyses sont identifiées par leur `id` (UUID)
- Les deux doivent appartenir au même dossier, lui-même appartenant au workspace courant
- Les deux doivent avoir le statut `DONE`
- Le diff est calculé section par section :
  - **Listes** (`faits`, `pointsJuridiques`, `risques`, `questionsOuvertes`) : comparaison textuelle exacte — un item est `added` s'il est dans `to` mais pas dans `from`, `removed` s'il est dans `from` mais pas dans `to`, `unchanged` s'il est dans les deux
  - **Timeline** : même logique, égalité sur `{date + evenement}` combinés
- La réponse inclut les métadonnées des deux versions (`id`, `version`, `analysisType`, `updatedAt`)

**Structure de réponse** :
```json
{
  "from": { "id": "...", "version": 1, "analysisType": "STANDARD", "updatedAt": "..." },
  "to":   { "id": "...", "version": 3, "analysisType": "ENRICHED", "updatedAt": "..." },
  "faits":             { "added": [...], "removed": [...], "unchanged": [...] },
  "pointsJuridiques":  { "added": [...], "removed": [...], "unchanged": [...] },
  "risques":           { "added": [...], "removed": [...], "unchanged": [...] },
  "questionsOuvertes": { "added": [...], "removed": [...], "unchanged": [...] },
  "timeline": {
    "added":     [{ "date": "...", "evenement": "..." }],
    "removed":   [{ "date": "...", "evenement": "..." }],
    "unchanged": [{ "date": "...", "evenement": "..." }]
  }
}
```

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| Dossier inexistant ou autre workspace | Not found opaque | 404 |
| Dossier soft-deleted (`deleted_at` non null) | Not found opaque | 404 |
| `fromId` ou `toId` inconnu ou n'appartient pas au dossier | Not found | 404 |
| L'une des deux analyses n'a pas le statut `DONE` | Conflit | 409 |
| `fromId == toId` | Bad request | 400 |

---

## Critères d'acceptation

- [ ] `GET .../diff?fromId=X&toId=Y` → 200 avec diff complet sur les 5 sections
- [ ] Items présents uniquement dans `to` → dans `added`
- [ ] Items présents uniquement dans `from` → dans `removed`
- [ ] Items présents dans les deux → dans `unchanged`
- [ ] Timeline : égalité sur `date + evenement` combinés
- [ ] `fromId == toId` → 400
- [ ] Une analyse non DONE → 409
- [ ] Analyse d'un autre dossier → 404
- [ ] Isolation workspace : dossier d'un autre workspace → 404
- [ ] `fromId` et `toId` peuvent être de types différents (STANDARD vs ENRICHED)

---

## Périmètre

### Hors scope

- Diff textuel intra-item (comparer le contenu d'un même item qui aurait changé de formulation)
- Historisation des diffs
- Notifications lors d'un changement significatif
- Frontend (SF-54-02)

---

## Technique

### Endpoint

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{caseFileId}/case-analysis/diff` | Oui | MEMBER |

**Query params** : `fromId` (UUID, obligatoire), `toId` (UUID, obligatoire)

### Nouveaux composants

| Composant | Rôle |
|-----------|------|
| `AnalysisDiffService` (nouveau) | Logique de diff sémantique — résolution des deux analyses, calcul des sets added/removed/unchanged |
| `AnalysisDiffResponse` (nouveau) | Record de réponse — structure décrite ci-dessus |
| `AnalysisDiffResponse.SectionDiff<T>` | Record générique `{ added, removed, unchanged }` |
| `CaseAnalysisReadController` | Ajout du mapping `GET /diff` |
| `CaseAnalysisRepository` | Ajout de `findByIdAndCaseFileId(UUID id, UUID caseFileId)` |

### Tables impactées

Aucune — lecture seule sur `case_analyses`.

### Migration Liquibase

- Non applicable

---

## Plan de test

### Tests unitaires — `AnalysisDiffServiceTest`

- [ ] diff nominal — items strictement added/removed/unchanged sur `faits`
- [ ] diff nominal — items strictly added/removed/unchanged sur `timeline` (égalité date+evenement)
- [ ] diff — listes identiques → added=[], removed=[], unchanged=tout
- [ ] diff — `from` vide → tous items dans added
- [ ] diff — `to` vide → tous items dans removed
- [ ] fromId == toId → 400
- [ ] Une analyse non DONE → 409
- [ ] Analyse appartenant à un autre dossier → 404
- [ ] Isolation workspace → 404

### Tests d'intégration — `CaseAnalysisControllerIT`

- [ ] I-01 GET /diff → 200 avec diff correct entre v1 et v2
- [ ] I-02 GET /diff → 400 si fromId == toId
- [ ] I-03 GET /diff → 409 si analyse non DONE
- [ ] I-04 GET /diff → 404 si analyse d'un autre dossier
- [ ] I-05 GET /diff → 404 si dossier d'un autre workspace

### Isolation workspace

- [x] Applicable — I-05

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Workspace context** — résolution workspace via `WorkspaceMemberRepository.findByUserAndPrimaryTrue`
- [ ] **Auth / Principal** — pattern identique aux endpoints existants, pas de changement
- [ ] **Plans / limites** — lecture seule, pas de gate
- [ ] **Navigation / routing** — pas de frontend dans cette subfeature

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression |
|-----------|-----------------|----------------------|
| `CaseAnalysisReadController` | Ajout d'un mapping GET — risque nul sur les mappings existants | Tests IT existants non modifiés |
| `CaseAnalysisRepository` | Ajout d'une query dérivée — risque nul | Compilation |

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné

---

## Dépendances

### Subfeatures bloquantes

- Aucune

### Questions ouvertes impactées

- Aucune

---

## Notes et décisions

- Diff par **ID d'analyse** (pas par numéro de version) : plus précis, la version est un entier qui peut ne pas être unique à terme entre STANDARD et ENRICHED
- Égalité **textuelle exacte** sur les items : pas de normalisation (trim, lowercase) — les items sont produits par le LLM avec une casse cohérente
- Timeline : égalité sur `date + evenement` concaténés — un item dont seule la date change est considéré removed+added
- `fromId` et `toId` sont interchangeables fonctionnellement (le diff est directionnel : `from` = "avant", `to` = "après") — c'est au frontend de gérer l'ordre affiché
