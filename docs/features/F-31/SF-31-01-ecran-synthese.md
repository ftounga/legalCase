# Mini-spec — F-31 / SF-31-01 Écran dédié synthèse

---

## Identifiant

`F-31 / SF-31-01`

## Feature parente

`F-31` — Écran dédié synthèse

## Statut

`ready`

## Date de création

2026-03-22

## Branche Git

`feat/SF-31-01-ecran-synthese`

---

## Objectif

Remplacer l'affichage inline de la synthèse dans la page dossier par un écran dédié `/case-files/:id/synthesis` avec des sections visuellement distinctes et lisibles.

---

## Comportement attendu

### Cas nominal

**Page dossier (CaseFileDetailComponent)** :
- Le bloc synthèse inline est supprimé.
- Un bouton "Voir la synthèse" apparaît dès que l'analyse initiale est DONE (CASE_ANALYSIS ou ENRICHED_ANALYSIS). Il navigue vers `/case-files/:id/synthesis`.

**Page synthèse (SynthesisComponent)** :
- Route : `/case-files/:id/synthesis`
- En-tête : titre du dossier + lien "Retour au dossier"
- Date et heure de la dernière mise à jour de la synthèse
- Badge "Synthèse enrichie" si la dernière analyse est de type ENRICHED_ANALYSIS (updatedAt plus récent que le CASE_ANALYSIS initial), "Synthèse initiale" sinon
- 5 sections distinctes dans des cards séparées, dans cet ordre :
  1. **Chronologie** — liste d'entrées date / événement, affichée uniquement si `timeline.length > 0`
  2. **Faits** — liste numérotée, uniquement si `faits.length > 0`
  3. **Points juridiques** — liste numérotée, uniquement si `pointsJuridiques.length > 0`
  4. **Risques** — liste numérotée, uniquement si `risques.length > 0`
  5. **Questions ouvertes** — liste numérotée, uniquement si `questionsOuvertes.length > 0`
- Bouton "Re-analyser" (accentué) visible si des questions ont été répondues, déclenche re-analysis et navigue vers la page dossier pour suivre la progression
- Si aucune synthèse disponible : message "Synthèse non disponible" avec lien retour

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| API synthèse retourne 404 | Message "Synthèse non disponible" + lien retour dossier |
| API synthèse retourne erreur réseau | Même message, silencieux |
| Re-analyser échoue (402) | Ignoré silencieusement (gate billing déjà géré) |
| Re-analyser échoue (autre) | SnackBar erreur |
| Dossier introuvable (404 case-file) | Message "Dossier introuvable" |

---

## Critères d'acceptation

- [ ] Route `/case-files/:id/synthesis` accessible et protégée par AuthGuard
- [ ] Page dossier : bloc synthèse inline supprimé, bouton "Voir la synthèse" affiché si synthèse disponible
- [ ] Chaque section s'affiche dans une card distincte, dans le bon ordre
- [ ] La chronologie est affichée sous forme de liste date / événement
- [ ] Les autres sections sont affichées en liste numérotée
- [ ] Les sections vides (tableau length=0) ne s'affichent pas
- [ ] Badge "Synthèse enrichie" ou "Synthèse initiale" présent
- [ ] Date de mise à jour visible
- [ ] Bouton "Re-analyser" visible si questions répondues, navigue vers dossier après déclenchement
- [ ] Conforme au DESIGN_SYSTEM (couleurs, typographie, layout)

---

## Plan de test

### Tests unitaires

- [ ] U-01 : sections avec data → affichées
- [ ] U-02 : sections vides → masquées
- [ ] U-03 : API 404 → message "Synthèse non disponible"
- [ ] U-04 : bouton "Re-analyser" visible si hasAnsweredQuestions, absent sinon
- [ ] U-05 : clic "Re-analyser" → appel reAnalysisService + navigation vers dossier

### Tests d'intégration

- [ ] Non applicable — pas de nouvel endpoint backend

### Isolation workspace

- [ ] Non applicable — utilise le même endpoint case-analysis existant, isolation déjà assurée

---

## Périmètre

### Composants Angular

- Nouveau : `SynthesisComponent` (`/case-files/:id/synthesis`)
- Modifié : `CaseFileDetailComponent` — suppression du bloc synthèse inline, ajout du bouton "Voir la synthèse"
- Modifié : `app.routes.ts` — ajout de la route `/case-files/:id/synthesis`

### Endpoints

Aucun nouvel endpoint. Réutilise :
- `GET /api/v1/case-files/:id/case-analysis` (synthèse)
- `GET /api/v1/case-files/:id` (titre du dossier)
- `GET /api/v1/case-files/:id/ai-questions` (pour savoir si questions répondues)
- `POST /api/v1/case-files/:id/re-analysis` (re-analyser)

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable

---

## Hors scope (explicite)

- Pas d'export PDF de la synthèse
- Pas de comparaison synthèse initiale vs enrichie
- Pas de pagination ou de filtres
- Pas de modification du backend

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Navigation / routing** — nouvelle route `/case-files/:id/synthesis`
  - Routes existantes vérifiées : `/case-files`, `/case-files/:id`, `/case-files/:id/synthesis` (nouvelle) — pas de conflit
  - AuthGuard appliqué sur la nouvelle route comme sur les routes existantes
  - Smoke test `navigation.spec.ts` à vérifier (pas de changement de guard existant)

### Smoke tests E2E concernés

- [ ] `navigation.spec.ts` — vérifier que les routes existantes ne sont pas cassées

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

**Pourquoi supprimer l'inline plutôt que garder les deux ?**

Garder les deux crée de la duplication et dilue l'attention. La page dossier doit rester focalisée sur les documents et le statut pipeline. La synthèse est assez riche pour mériter sa propre page.

**Badge "Synthèse enrichie" / "Synthèse initiale"** : indiqué par la comparaison `updatedAt` — si l'analyse retournée est plus récente que la CASE_ANALYSIS initiale, c'est une synthèse enrichie. Simplifié : on affiche toujours "Synthèse enrichie" si `hasAnsweredQuestions()` est vrai au moment de l'affichage (les questions ont été répondues → une re-analyse a eu lieu).
