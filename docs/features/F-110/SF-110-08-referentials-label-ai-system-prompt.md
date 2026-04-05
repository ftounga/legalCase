# Mini-spec — F-110 / SF-110-08 : Correction system prompt IA — validation du libellé

## Identifiant
`F-110 / SF-110-08`

## Feature parente
`F-110` — Guides & barèmes métier par domaine

## Statut
`in-progress`

## Date de création
`2026-04-05`

## Branche Git
`feat/SF-110-08-referentials-label-ai-system-prompt`

---

## Objectif

Corriger le system prompt de `ReferentialValidationService` pour que l'IA valide également le libellé proposé, pas uniquement la valeur JSON.

---

## Comportement attendu

### Cas nominal

Quand l'OWNER modifie un référentiel :
- Si le libellé proposé est inexact, trompeur ou incohérent avec la valeur → `WARNING: [message]`
- Si la valeur proposée diverge du droit en vigueur → `WARNING: [message]`
- Si libellé et valeur sont tous deux cohérents → `VALID`

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Anthropic indisponible | Fail-open — VALID (inchangé) |

---

## Critères d'acceptation

- [ ] Modifier uniquement le libellé par un terme inexact → l'IA retourne WARNING
- [ ] Modifier uniquement la valeur de façon incorrecte → comportement inchangé (WARNING)
- [ ] Libellé et valeur cohérents → VALID
- [ ] Fail-open conservé si Anthropic indisponible

---

## Périmètre

### Hors scope
- Modification de la logique frontend
- Modification du contrat API

---

## Technique

### Composants impactés

| Composant | Modification |
|-----------|-------------|
| `ReferentialValidationService` | System prompt mis à jour pour inclure la validation du libellé |

### Migration Liquibase
- Non applicable

---

## Plan de test

### Tests unitaires
- [ ] `ReferentialValidationServiceTest` — VAL-04 : changement libellé seul → WARNING si libellé incohérent
- [ ] Tests existants VAL-01/02/03 inchangés

### Isolation workspace
- Non applicable

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Aucune** — impact limité au system prompt IA

---

## Notes

SF-110-07 a ajouté `proposedLabel` au user message mais oublié de mettre à jour le system prompt.
Le system prompt instruisait l'IA de valider uniquement "la valeur proposée" → l'IA ignorait le libellé.
