# Mini-spec — F-110 / SF-110-10 : Dialog Material pour la confirmation IA

## Identifiant
`F-110 / SF-110-10`

## Feature parente
`F-110` — Guides & barèmes métier par domaine

## Statut
`in-progress`

## Date de création
`2026-04-05`

## Branche Git
`feat/SF-110-10-referentials-ai-warning-dialog`

---

## Objectif

Remplacer le `confirm()` natif du navigateur par une dialog Angular Material pour afficher les avertissements IA lors de la modification d'un référentiel.

---

## Comportement attendu

### Cas nominal

Quand l'IA retourne un WARNING :
- Une dialog Material s'ouvre avec :
  - Titre : "Avertissement IA"
  - Icône warning (couleur orange/warn)
  - Message : texte du warning IA
  - Bouton "Annuler" (secondaire)
  - Bouton "Sauvegarder quand même" (primary)
- Si l'utilisateur clique "Sauvegarder quand même" → `force: true` → sauvegarde
- Si l'utilisateur clique "Annuler" ou ferme → rien ne se passe

---

## Critères d'acceptation

- [ ] `confirm()` natif remplacé par `MatDialog`
- [ ] Icône warning visible, couleur warn
- [ ] Message IA lisible (pas de `\n` bruts)
- [ ] Boutons "Annuler" / "Sauvegarder quand même" fonctionnels
- [ ] Aucun changement backend

---

## Périmètre

### Hors scope
- Modification du contrat API
- Changement de la logique de validation IA

---

## Technique

### Composants impactés

| Composant | Modification |
|-----------|-------------|
| `referentials.component.ts` | Remplacer `confirm()` par `MatDialog.open()` |
| `ReferentialWarningDialogComponent` (nouveau) | Dialog standalone avec icône + message + boutons |

---

## Plan de test

### Tests unitaires
- Non applicable (composant de présentation pur)

### Isolation workspace
- Non applicable

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Aucune**
