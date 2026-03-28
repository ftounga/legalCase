# Mini-spec — F-57 / SF-57-02 : Frontend bouton "Modifier" réponse Q&A

---

## Identifiant

`F-57 / SF-57-02`

## Feature parente

`F-57` — Modification des réponses Q&A

## Statut

`in-progress`

## Date de création

2026-03-28

## Branche Git

`feat/SF-57-02-update-qa-answer-frontend`

---

## Objectif

Afficher un bouton "Modifier" sur chaque réponse Q&A déjà soumise, permettant à l'avocat de corriger ou mettre à jour sa réponse via un formulaire inline.

---

## Comportement attendu

### Cas nominal

1. Question avec answerText → réponse affichée + bouton "Modifier"
2. Clic "Modifier" → textarea pré-rempli + "Enregistrer" + "Annuler"
3. "Enregistrer" → POST /api/v1/ai-questions/{id}/answer, réponse mise à jour localement
4. Une seule question en édition simultanément
5. "Annuler" → retour à l'affichage sans appel API

### Cas d'erreur

| Situation | Comportement |
|-----------|-------------|
| Textarea vide | Bouton "Enregistrer" désactivé |
| Erreur API | SnackBar "Erreur lors de la modification" |

---

## Critères d'acceptation

- [ ] Bouton "Modifier" visible sur toute réponse existante
- [ ] Clic → textarea pré-rempli avec la réponse actuelle
- [ ] Une seule question en édition simultanément
- [ ] "Enregistrer" désactivé si textarea vide
- [ ] Soumission réussie → réponse mise à jour, formulaire fermé
- [ ] "Annuler" → retour sans appel API
- [ ] Erreur API → snackbar d'erreur

---

## Périmètre

### Hors scope

- Historique des réponses
- Suppression d'une réponse

---

## Technique

### Composant impacté

SynthesisComponent — ajout signal `editingQuestionId`, méthodes `startEdit`, `cancelEdit`, `submitEdit`

### Service

AiQuestionAnswerService.submitAnswer() — réutilisé sans modification

### Migration Liquibase

- [x] Non applicable

---

## Plan de test

- [ ] `editingQuestionId` nul par défaut
- [ ] `startEdit(q)` → signal = id de la question
- [ ] `cancelEdit()` → signal = null
- [ ] `submitEdit()` texte vide → pas d'appel service
- [ ] `submitEdit()` nominal → service appelé, question mise à jour, signal null
- [ ] `submitEdit()` erreur → snackbar erreur

---

## Analyse d'impact

- [x] **Aucune préoccupation transversale** — modification locale à SynthesisComponent
