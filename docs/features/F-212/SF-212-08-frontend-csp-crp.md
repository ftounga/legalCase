# SF-212-08 — Frontend : section « CSP/CRP — conformité »

> Feature F-212. Outil : `F-DT-44-csp-crp-conformite`. Contrat API : `SF-212-07` (figé).

## Objectif

Afficher la section permettant à l'avocat de vérifier la conformité de la proposition de CSP et de visualiser l'ASP estimée.

## Comportement nominal

Composant standalone `CspCrpConformiteSectionComponent`, enregistré sous `F-DT-44-csp-crp-conformite`. Affiché en `CONTEXTUAL` (flag `csp_propose`).

Formulaire (9 champs) : effectif entreprise, toggles CSP proposé / document remis / délai mentionné, dates, adhésion (select tri-état), salaire mensuel brut, rémunération 12 mois. Bouton « Analyser » → verdict conformité + encadré ASP estimée (journalière + annuelle + durée 12 mois) + points de non-conformité. Si effectif ≥ 1 000 : message informatif « CSP non applicable, outil réservé aux entreprises < 1 000 salariés ».

**Pré-remplissage IA** depuis `aiData.travailExtractedData.cspDetail`. **Cohérence F-IA-03** sur 4 `critereCode`. **Gate `isFrance`** : bannière info.

## Cas d'erreur

- Backend 422 → message outil FR-only.
- 4xx/5xx → `MatSnackBar`.

## Critères d'acceptation

1. Section visible uniquement si `csp_propose = true`.
2. Message informatif si effectif ≥ 1 000.
3. Encadré ASP visible et complet.
4. Points de non-conformité listés avec fondement.
5. Gate `isFrance` → bannière info.
6. Pré-remplissage IA + `getPrefillCount()`.
7. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu, pré-remplissage, encadré ASP, message effectif > 1 000, gate `isFrance`.

## Tables / endpoints / composants impactés

- Nouveaux fichiers composant, helper, service.
- Modifié : `TOOL_REGISTRY`.

## Hors périmètre

Backend (→ SF-212-07).
