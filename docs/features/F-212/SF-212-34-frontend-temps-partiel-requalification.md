# SF-212-34 — Frontend : section « temps partiel — requalification »

> Feature F-212. Outil : `F-DT-49-temps-partiel-requalification`. Contrat API : `SF-212-33` (figé).

## Objectif

Afficher la section permettant à l'avocat de détecter les irrégularités d'un contrat TP et d'évaluer le rappel de salaire estimé.

## Comportement nominal

Composant standalone `TempsPartielRequalificationSectionComponent`, sous `F-DT-49-temps-partiel-requalification`. Affiché en `CONTEXTUAL` (flag `temps_partiel_requalification_envisagee`).

Formulaire (8 champs) : durée contractuelle (h/sem), toggles mentions durée / répartition / HC présentes, HC réelles moyennes (h/sem), modification répartition unilatérale (toggle), ancienneté, salaire contractuel. Bouton « Analyser » → verdict (PROBABLE rouge / POSSIBLE or / PAS_DE vert) + facteurs + rappel salaire estimé.

**Pré-remplissage IA** depuis `aiData.travailExtractedData.tempsPartielDetail`. **Cohérence F-IA-03** sur 4 `critereCode`.

## Cas d'erreur

- Backend 422 → message hors domaine.
- 4xx/5xx → `MatSnackBar`.

## Critères d'acceptation

1. Section visible uniquement si `temps_partiel_requalification_envisagee = true`.
2. Verdict 3 états couleur.
3. Rappel salaire affiché si applicable.
4. Pré-remplissage IA + `getPrefillCount()`.
5. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu, pré-remplissage, verdict, rappel salaire.

## Tables / endpoints / composants impactés

- Nouveaux fichiers composant, helper, service.
- Modifié : `TOOL_REGISTRY`.

## Hors périmètre

Backend (→ SF-212-33).
