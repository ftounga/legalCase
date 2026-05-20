# SF-212-04 — Frontend : section « forfait jours — validité »

> Feature F-212. Outil : `F-DT-50-forfait-jours-validite`. Contrat API : `SF-212-03-backend-forfait-jours.md` (figé).

## Objectif

Afficher la section permettant à l'avocat de vérifier la validité d'une convention de forfait jours et de visualiser le rappel d'heures supplémentaires estimé si le forfait est nul.

## Comportement nominal

Composant standalone `ForfaitJoursValiditeSectionComponent`, enregistré dans `TOOL_REGISTRY` sous `F-DT-50-forfait-jours-validite`. Affiché en `CONTEXTUAL` (flag `forfait_jours_detecte`).

Formulaire (10 champs) : accord collectif (toggle), garantie suivi charge (toggle), entretien annuel (toggle), document de contrôle mensuel (toggle), catégorie autonome (toggle), nb jours forfait, ancienneté, salaire mensuel brut, HS estimées/semaine, semaines/an. Bouton « Analyser » → verdict + tableau rappel HS.

Affichage : badge validité (`VALIDE` vert / `PARTIELLEMENT_NULLE` or / `NULLE` rouge) + facteurs d'invalidité avec fondement + tableau rappel HS (si nul) avec montant estimé sur 3 ans.

**Pré-remplissage IA** depuis `aiData.travailExtractedData.forfaitJoursDetail`. **Cohérence F-IA-03** sur les 5 `critereCode`. **Gate `isFrance`** : bannière info si workspace ≠ FRANCE.

## Cas d'erreur

- Backend 422 → message outil FR-only, formulaire masqué.
- 4xx/5xx → `MatSnackBar`.

## Critères d'acceptation

1. Section visible **uniquement** si flag `forfait_jours_detecte = true`.
2. Badge validité couleur correcte.
3. Tableau rappel HS affiché si `validiteForfait ≠ VALIDE`.
4. Pré-remplissage IA opérationnel.
5. Gate `isFrance` → bannière info.
6. `getPrefillCount()` correct.
7. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu, pré-remplissage, badge 3 niveaux, tableau rappel HS, gate `isFrance`, `getPrefillCount()`.

## Tables / endpoints / composants impactés

- **Nouveaux fichiers** : composant, helper prefill-rules, service.
- **Modifié** : `TOOL_REGISTRY`.

## Hors périmètre

Backend (→ SF-212-03).
