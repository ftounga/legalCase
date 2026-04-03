# Mini-spec — F-96 / SF-96-04 Injection des points TO_CHECK dans la synthèse enrichie

## Identifiant

`F-96 / SF-96-04`

## Feature parente

`F-96` — Checklist procédurale interactive

## Statut

`ready`

## Date de création

2026-04-03

## Branche Git

`feat/SF-96-04-inject-to-check-enriched`

---

## Objectif

Injecter les points procéduraux au statut `TO_CHECK` dans le prompt de la synthèse enrichie (en plus des `NON_COMPLIANT` déjà injectés), afin que Claude puisse en tenir compte même si l'avocat n'a pas encore qualifié ces points.

---

## Comportement attendu

### Cas nominal

Au moment de construire le prompt de la synthèse enrichie (`buildEnrichedPrompt`) :

1. Les points `NON_COMPLIANT` sont injectés sous `[Points procéduraux non conformes]` — **inchangé**
2. Les points `TO_CHECK` (non encore qualifiés) sont désormais aussi injectés sous une nouvelle section `[Points procéduraux à vérifier]`
3. Claude reçoit les deux sections et en tient compte dans sa synthèse enrichie (redistribution dans `points_procedure`, `risques`, `questions_ouvertes`, `score_risque` à sa discrétion)
4. Si aucun point `TO_CHECK` n'existe : la section n'est pas ajoutée au prompt (comportement inchangé)

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `listToCheck` lève une exception | Fail-open : la section `TO_CHECK` est omise, la synthèse enrichie continue |
| Aucun point `TO_CHECK` | Section absente du prompt, pas d'impact |
| Aucun point `NON_COMPLIANT` non plus | Comportement identique à avant (aucune section procédurale) |

---

## Critères d'acceptation

- [ ] Si des points `TO_CHECK` existent pour la dernière analyse DONE du dossier, ils sont injectés dans le prompt sous `[Points procéduraux à vérifier]`
- [ ] Les points `VERIFIED` ne sont pas injectés
- [ ] Les points `NON_COMPLIANT` continuent d'être injectés sous `[Points procéduraux non conformes]` — comportement inchangé
- [ ] Si `listToCheck` échoue, la synthèse enrichie continue sans planter (fail-open)
- [ ] Si aucun `TO_CHECK` : section absente du prompt

---

## Périmètre

### Hors scope

- Modification du format JSON de sortie de la synthèse enrichie
- Modification du system prompt
- Modification du comportement des statuts `VERIFIED` et `NON_COMPLIANT`
- Frontend — aucun changement d'UI

---

## Technique

### Endpoint(s)

Aucun nouvel endpoint. Modification interne à `EnrichedAnalysisService` et `ProcedureCheckService`.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `procedure_checks` | SELECT | Ajout d'une requête pour statut `TO_CHECK` |

### Migration Liquibase

- [x] Non applicable — pas de changement de schéma

### Composants Angular

Aucun.

---

## Plan de test

### Tests unitaires

- [ ] `EnrichedAnalysisServiceTest` — `buildEnrichedPrompt` avec des TO_CHECK : la section `[Points procéduraux à vérifier]` est présente dans le prompt
- [ ] `EnrichedAnalysisServiceTest` — `buildEnrichedPrompt` sans TO_CHECK : la section est absente
- [ ] `EnrichedAnalysisServiceTest` — TO_CHECK + NON_COMPLIANT : les deux sections sont présentes et distinctes
- [ ] `ProcedureCheckService` — `listToCheck` retourne uniquement les `TO_CHECK` de la dernière analyse DONE

### Tests d'intégration

Pas de nouvel endpoint — les IT existants du pipeline `EnrichedAnalysisService` couvrent le flux.

### Isolation workspace

- [x] Non applicable — modification du prompt interne, pas d'accès croisé entre workspaces

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — modification interne du prompt IA uniquement

---

## Dépendances

### Subfeatures bloquantes

- SF-96-03 — statut : done (injection NON_COMPLIANT déjà en place)

---

## Notes et décisions

- `listToCheck` est une nouvelle méthode dans `ProcedureCheckService`, symétrique à `listNonCompliant`
- Fail-open identique à `listNonCompliant` : toute exception retourne une liste vide
- Claude décide lui-même où placer les TO_CHECK dans sa sortie — pas de contrainte sur le schéma JSON de sortie
