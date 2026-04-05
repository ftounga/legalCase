# Mini-spec — F-110 / SF-110-07 : Correction validation IA du libellé dans la dialog d'édition

## Identifiant
`F-110 / SF-110-07`

## Feature parente
`F-110` — Guides & barèmes métier par domaine

## Statut
`in-progress`

## Date de création
`2026-04-05`

## Branche Git
`feat/SF-110-07-referentials-label-ai-validation`

---

## Objectif

Corriger la régression introduite par SF-110-06 : la validation IA doit détecter et avertir lorsque le libellé d'un référentiel est modifié, pas seulement lorsque la valeur JSON change.

---

## Comportement attendu

### Cas nominal

Quand l'OWNER modifie un référentiel (via le formulaire typé ou le textarea JSON) :
- Si le libellé **ou** la valeur change → l'IA compare `(libellé actuel, valueJson actuelle)` vs `(libellé proposé, valueJson proposée)`
- Si l'IA détecte une divergence → `WARNING: [message]` → dialog de confirmation côté frontend
- Si l'IA valide → `VALID` → sauvegarde directe

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Anthropic indisponible | Fail-open : validation passe, sauvegarde directe | 200 |
| JSON invalide | Rejeté avant validation IA | 400 |

---

## Critères d'acceptation

- [ ] Modifier uniquement le libellé d'un référentiel système → la validation IA se déclenche et peut retourner un WARNING
- [ ] Modifier uniquement la valeur → comportement inchangé (validation IA comme avant)
- [ ] Modifier libellé + valeur → validation IA prend en compte les deux changements
- [ ] Anthropic indisponible → fail-open, sauvegarde sans warning (comportement existant conservé)
- [ ] Aucun changement frontend requis

---

## Périmètre

### Hors scope

- Validation sémantique avancée du contenu (ex : vérifier que l'article de loi existe)
- Modification de la logique de confirmation côté frontend (le `confirm()` existant est conservé)

---

## Technique

### Endpoint impacté

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| PUT | `/api/v1/referentials/{id}` | Oui | OWNER/ADMIN |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `legal_referentials` | UPDATE | Inchangé — seule la logique de validation est modifiée |

### Migration Liquibase
- [x] Non applicable

### Composants impactés

| Composant | Modification |
|-----------|-------------|
| `ReferentialValidationService` | Signature `validate()` enrichie : ajouter `proposedLabel` |
| `LegalReferentialService` | Appel `validate()` passe `newLabel` en plus |

---

## Plan de test

### Tests unitaires

- [ ] `ReferentialValidationService` — prompt inclut libellé actuel ET libellé proposé
- [ ] `LegalReferentialService.updateReferential` — appel validate() avec newLabel

### Tests d'intégration
- Non applicable (pas de changement de contrat API)

### Isolation workspace
- Non applicable

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée, impact limité à `ReferentialValidationService` et `LegalReferentialService`

### Smoke tests E2E concernés
- [ ] Aucun smoke test concerné (pas de changement de routing/auth/workspace)

---

## Dépendances

### Subfeatures bloquantes
- SF-110-06 — statut : done

---

## Notes et décisions

Avant SF-110-06, l'utilisateur modifiait le libellé ET le JSON dans le même textarea → valueJson changeait presque toujours → l'IA se déclenchait implicitement.
Avec SF-110-06, libellé et valeurs sont des champs indépendants → un changement de libellé seul ne modifie pas la valueJson reconstruite → l'IA comparait deux valueJson identiques → toujours VALID.

Fix minimal : passer `newLabel` à `validate()` et l'inclure dans le prompt IA comme "Libellé proposé".
