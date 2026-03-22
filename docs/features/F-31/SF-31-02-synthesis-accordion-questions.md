# Mini-spec — F-31 / SF-31-02 Accordéon synthèse + questions IA sur écran synthèse

---

## Identifiant

`F-31 / SF-31-02`

## Feature parente

`F-31` — Écran dédié synthèse

## Statut

`ready`

## Date de création

2026-03-22

## Branche Git

`feat/SF-31-02-synthesis-accordion-questions`

---

## Objectif

Améliorer la lisibilité de l'écran synthèse en rendant les sections pliables/dépliables (accordéon), et centraliser les questions IA et la réponse sur cet écran pour un flux de travail cohérent.

---

## Comportement attendu

### Cas nominal

**Page synthèse (SynthesisComponent)** :
- Chaque section (Chronologie, Faits, Points juridiques, Risques, Questions ouvertes) est rendue dans un `mat-expansion-panel`
- Par défaut, toutes les sections sont dépliées (`expanded`)
- L'utilisateur peut replier/déplier chaque section individuellement (pas de mode accordéon exclusif — multi-expand autorisé)
- Le titre du panel affiche le nom de la section et le nombre d'éléments
- Les sections vides ne sont pas affichées (comportement inchangé)
- **Nouveau bloc "Questions IA"** sous les sections de synthèse :
  - Affiché uniquement si `questions().length > 0`
  - Liste les questions avec leur statut (répondue / en attente)
  - Pour une question sans réponse : textarea + bouton "Répondre"
  - Pour une question avec réponse : affichage de la réponse (lecture seule)
  - Bouton "Re-analyser" en bas du bloc, visible uniquement si `hasAnsweredQuestions()`, déclenche re-analysis et navigue vers `/case-files/:id`

**Page dossier (CaseFileDetailComponent)** :
- Suppression du bloc "Questions IA" entier (section + carte)
- Si `questions().length > 0` : affichage d'un bandeau compact "X question(s) IA en attente" avec lien vers `/case-files/:id/synthesis`
- Suppression des imports/services liés aux questions qui ne sont plus utilisés : `AiQuestionService`, `AiQuestionAnswerService`, `AiQuestionAnswerService`, signals `questions`, `submittingAnswer`, méthodes `loadQuestions`, `submitAnswer`, `hasAnsweredQuestions`

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Soumission réponse vide | Ignorée (bouton Répondre inactif si textarea vide) |
| Soumission réponse → 402 | Ignorée silencieusement |
| Soumission réponse → autre erreur | SnackBar erreur |
| Re-analyser → 402 | Ignoré silencieusement |
| Re-analyser → autre erreur | SnackBar erreur |

---

## Critères d'acceptation

- [ ] Chaque section de synthèse est dans un `mat-expansion-panel`, dépliée par défaut
- [ ] Les sections restent pliables/dépliables indépendamment
- [ ] Les sections vides ne s'affichent pas
- [ ] Bloc "Questions IA" présent sur la page synthèse si questions disponibles
- [ ] Questions répondues affichées en lecture seule
- [ ] Questions sans réponse : formulaire de réponse fonctionnel
- [ ] Bouton "Re-analyser" visible uniquement si au moins une question est répondue
- [ ] Clic "Re-analyser" → navigue vers `/case-files/:id` après déclenchement
- [ ] Page dossier : bloc "Questions IA" supprimé
- [ ] Page dossier : bandeau "X question(s) en attente" affiché si questions disponibles, lien vers synthèse
- [ ] Conforme au DESIGN_SYSTEM

---

## Périmètre

### Hors scope (explicite)

- Pas de modification du backend
- Pas de pagination des questions
- Pas d'état "accordéon fermé par défaut" configurable
- Pas de comparaison entre réponses

---

## Technique

### Endpoint(s)

Aucun nouvel endpoint. Réutilise :
- `GET /api/v1/case-files/:id/ai-questions`
- `POST /api/v1/case-files/:id/ai-questions/:qid/answers`
- `POST /api/v1/case-files/:id/re-analysis`

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable

### Composants Angular

- Modifié : `SynthesisComponent` — sections en `mat-expansion-panel`, ajout bloc questions IA
- Modifié : `CaseFileDetailComponent` — suppression bloc questions, ajout bandeau compact

---

## Plan de test

### Tests unitaires

- [ ] U-01 : sections avec data → panels affichés dépliés
- [ ] U-02 : sections vides → panels absents
- [ ] U-03 : question sans réponse → formulaire affiché
- [ ] U-04 : question avec réponse → réponse affichée, pas de formulaire
- [ ] U-05 : bouton Re-analyser absent si aucune question répondue
- [ ] U-06 : bouton Re-analyser présent si au moins une question répondue

### Tests d'intégration

- [ ] Non applicable — pas de nouvel endpoint backend

### Isolation workspace

- [ ] Non applicable — utilise les services existants déjà isolés

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Navigation / routing frontend** — le flux de navigation questions→re-analyse change (dossier → synthèse)
- [ ] **Auth / Principal** — non touché
- [ ] **Workspace context** — non touché
- [ ] **Plans / limites** — non touché

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|-----------------|------------------------------|
| `CaseFileDetailComponent` | Suppression du bloc questions et des services associés | Smoke test happy-path (vérifier que la page dossier charge) |
| `SynthesisComponent` | Ajout de `AiQuestionService`, `AiQuestionAnswerService`, `ReAnalysisService` | Build + smoke tests |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/happy-path.spec.ts` — flux avocat complet — vérifier que la page dossier charge toujours correctement
- [ ] `e2e/smoke/navigation.spec.ts` — routes existantes non cassées

---

## Dépendances

### Subfeatures bloquantes

- SF-31-01 — statut : done ✓

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

**Multi-expand vs accordéon exclusif** : on utilise `mat-accordion` sans `multi=false` — l'utilisateur peut avoir plusieurs sections ouvertes simultanément. Un accordéon exclusif (une seule section ouverte) serait trop contraignant pour une synthèse longue.

**Déplié par défaut** : première lecture toujours complète. L'utilisateur replie ce dont il n'a plus besoin.

**Déplacement des questions** : le flux avocat devient linéaire sur un seul écran — lire la synthèse, répondre aux questions, re-analyser — sans aller-retour entre pages. La page dossier reste focalisée sur la gestion des documents et le suivi pipeline.

**Bandeau sur page dossier** : indicateur visuel non intrusif (pas une card entière) signalant que des questions attendent une réponse, avec lien direct vers la synthèse.
