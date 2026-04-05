# Mini-spec — F-110 / SF-110-09 : Verrouillage du libellé pour les entrées système

## Identifiant
`F-110 / SF-110-09`

## Feature parente
`F-110` — Guides & barèmes métier par domaine

## Statut
`in-progress`

## Date de création
`2026-04-05`

## Branche Git
`feat/SF-110-09-referentials-lock-system-label`

---

## Objectif

Interdire la modification du libellé pour les entrées système (`isSystem: true`), côté frontend (champ readonly) et backend (label ignoré → toujours celui de la source).

---

## Comportement attendu

### Cas nominal

- Entrée système (`isSystem: true`) : champ "Libellé" affiché en readonly dans la dialog, tooltip "Libellé officiel — non modifiable". Seule la valeur est modifiable.
- Entrée workspace (`isSystem: false`) : libellé et valeur éditables (comportement inchangé).
- Backend : pour un override workspace créé depuis une entrée système, le label est toujours celui de la source — `newLabel` est ignoré.
- Validation IA : ne porte que sur la valeur JSON (revert system prompt SF-110-08).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Tentative de modifier le label d'une entrée système via API | Label ignoré, valeur source conservée |

---

## Critères d'acceptation

- [ ] Dialog : champ libellé en `readonly` si `entry.isSystem === true`
- [ ] Dialog : tooltip ou hint "Libellé officiel — non modifiable" visible
- [ ] Backend : `updateReferential()` ignore `newLabel` pour les entrées système, utilise `source.getLabel()`
- [ ] Validation IA : system prompt revenu à la validation de la valeur uniquement (revert SF-110-08)
- [ ] Signature `validate()` revenue à 6 params (revert SF-110-07) : label n'est plus comparé
- [ ] Entrée workspace (`isSystem: false`) : libellé toujours éditable

---

## Périmètre

### Hors scope
- Validation sémantique du libellé pour les overrides workspace
- Modification du contrat API (`PUT /api/v1/referentials/{id}`)

---

## Technique

### Composants impactés

| Composant | Modification |
|-----------|-------------|
| `referential-edit-dialog.component.html` | `readonly` + tooltip sur le champ libellé si `isSystem` |
| `referential-edit-dialog.component.ts` | `buildForm()` : disable control label si `isSystem` |
| `LegalReferentialService` | `updateReferential()` : ignorer `newLabel` pour entrées système |
| `ReferentialValidationService` | Revert system prompt (SF-110-08) + signature 6 params (SF-110-07) |
| `ReferentialValidationServiceTest` | Mettre à jour les appels à 6 params |
| `LegalReferentialServiceTest` | Mettre à jour le mock à 6 params |

### Migration Liquibase
- Non applicable

---

## Plan de test

### Tests unitaires
- [ ] `LegalReferentialServiceTest` : update system entry → label dans l'override = source.label (pas newLabel)
- [ ] `ReferentialValidationServiceTest` : signature 6 params, tests VAL-01/02/03 inchangés

### Isolation workspace
- Non applicable

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Aucune** — impact limité aux composants listés ci-dessus
