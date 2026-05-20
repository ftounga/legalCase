# SF-212-20 — Frontend : section « mise à pied disciplinaire »

> Feature F-212. Outil : `F-DT-48-mise-a-pied-disciplinaire`. Contrat API : `SF-212-19` (figé).

## Objectif

Afficher la section permettant à l'avocat d'analyser la régularité d'une mise à pied et de distinguer mise à pied disciplinaire et conservatoire.

## Comportement nominal

Composant standalone `MiseAPiedDisciplinaireSectionComponent`, sous `F-DT-48-mise-a-pied-disciplinaire`. Affiché en `CONTEXTUAL` (flag `mise_a_pied_disciplinaire_detectee`).

Formulaire (8 champs) : nature (select : disciplinaire / conservatoire / inconnue), toggles procédure suivie / prescription / durée dans RI / salaire suspendu / double sanction, durée (jours), salaire mensuel brut. Bouton « Analyser » → verdict (REGULIERE vert / IRREGULIERE or-rouge / CONSERVATOIRE bleu) + points de régularité + salaire dû pendant la période.

**Pré-remplissage IA** depuis `aiData.travailExtractedData.miseAPiedDetail`. **Cohérence F-IA-03** sur 4 `critereCode`.

## Cas d'erreur

- Backend 422 → message hors domaine.
- 4xx/5xx → `MatSnackBar`.

## Critères d'acceptation

1. Section visible uniquement si `mise_a_pied_disciplinaire_detectee = true`.
2. Verdict 4 états avec couleurs distinctes.
3. Salaire dû période affiché si conservatoire.
4. Pré-remplissage IA + `getPrefillCount()`.
5. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu, pré-remplissage, verdict 4 états, salaire dû.

## Tables / endpoints / composants impactés

- Nouveaux fichiers composant, helper, service.
- Modifié : `TOOL_REGISTRY`.

## Hors périmètre

Backend (→ SF-212-19).
