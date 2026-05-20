# SF-212-16 — Frontend : section « télétravail — conformité »

> Feature F-212. Outil : `F-DT-82-teletravail-accord`. Contrat API : `SF-212-15` (figé).

## Objectif

Afficher la section permettant à l'avocat de vérifier la conformité du dispositif de télétravail et d'identifier les litiges courants.

## Comportement nominal

Composant standalone `TeletravailAccordSectionComponent`, sous `F-DT-82-teletravail-accord`. Affiché en `CONTEXTUAL` (flag `teletravail_litige_detecte`). **Gate `isFrance`** : bannière info si workspace ≠ FRANCE.

Formulaire (7 champs) : cadre juridique (select), double volontariat (toggle), indemnité versée (toggle), montant journalier (€), accident domicile (toggle), retour bureau unilatéral (toggle), refus télétravail incriminé (toggle). Bouton « Analyser » → verdict + alertes visuelles distinctes (accident travail domicile, refus licenciement) + points de conformité.

**Pré-remplissage IA** depuis `aiData.travailExtractedData.teletravailDetail`. **Cohérence F-IA-03** sur 4 `critereCode`.

## Cas d'erreur

- Backend 422 → message outil FR-only.
- 4xx/5xx → `MatSnackBar`.

## Critères d'acceptation

1. Section visible uniquement si `teletravail_litige_detecte = true`.
2. Alertes AT domicile et refus-licenciement affichées distinctement.
3. Verdict 3 états couleur.
4. Gate `isFrance` → bannière.
5. Pré-remplissage IA + `getPrefillCount()`.
6. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu, pré-remplissage, alertes distinctes, gate `isFrance`.

## Tables / endpoints / composants impactés

- Nouveaux fichiers composant, helper, service.
- Modifié : `TOOL_REGISTRY`.

## Hors périmètre

Backend (→ SF-212-15).
