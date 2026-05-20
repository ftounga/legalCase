# SF-212-06 — Frontend : section « transfert d'entreprise — L. 1224-1 »

> Feature F-212. Outil : `F-DT-72-transfert-entreprise-l1224-1`. Contrat API : `SF-212-05` (figé).

## Objectif

Afficher la section permettant à l'avocat d'analyser l'applicabilité de L. 1224-1 lors d'un transfert d'entreprise et de détecter les alertes (licenciements frauduleux, défaut consultation CSE).

## Comportement nominal

Composant standalone `TransfertEntrepriseL12241SectionComponent`, enregistré dans `TOOL_REGISTRY` sous `F-DT-72-transfert-entreprise-l1224-1`. Affiché en `CONTEXTUAL` (flag `transfert_entreprise_detecte`).

Formulaire (9 champs) : type de transfert (select), toggles EEA identifiée / activité préservée / salariés transférés / contrats modifiés / licenciements pré-transfert / consultation CSE, nb licenciements, date transfert. Bouton « Analyser » → verdict applicabilité + alertes visuelles (licenciements frauduleux en rouge, défaut consultation CSE en orange) + points d'analyse avec fondement.

**Pré-remplissage IA** depuis `aiData.travailExtractedData.transfertEntrepriseDetail`. **Cohérence F-IA-03** sur les 4 `critereCode`. **Pas de gate `country`** (L. 1224-1 applicable à tous dossiers Travail FR sans condition de nationalité spécifique — l'outil est conditionné par le flag IA).

## Cas d'erreur

- Backend 422 → message outil hors domaine, formulaire masqué.
- 4xx/5xx → `MatSnackBar`.

## Critères d'acceptation

1. Section visible uniquement si `transfert_entreprise_detecte = true`.
2. Badge applicabilité couleur (APPLICABLE vert / INCERTAIN or / INAPPLICABLE rouge).
3. Alertes licenciements frauduleux et défaut consultation CSE bien distinctes visuellement.
4. Pré-remplissage IA opérationnel.
5. `getPrefillCount()` correct.
6. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu, pré-remplissage, alertes, badge 3 niveaux, `getPrefillCount()`.

## Tables / endpoints / composants impactés

- Nouveaux fichiers composant, helper, service.
- Modifié : `TOOL_REGISTRY`.

## Hors périmètre

Backend (→ SF-212-05).
