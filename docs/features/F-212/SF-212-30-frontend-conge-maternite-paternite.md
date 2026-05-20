# SF-212-30 — Frontend : section « congé maternité / paternité »

> Feature F-212. Outil : `F-DT-77-conge-paternite-maternite`. Contrat API : `SF-212-29` (figé).

## Objectif

Afficher la section permettant à l'avocat de calculer la durée du congé, les IJ CPAM et de vérifier la protection contre le licenciement.

## Comportement nominal

Composant standalone `CongeMaternitePaterniteSectionComponent`, sous `F-DT-77-conge-paternite-maternite`. Affiché en `CONTEXTUAL` (flag `conge_maternite_paternite_detecte`). **Gate `isFrance`**.

Formulaire (7 champs) : type de congé (select), rang enfant, naissance multiple (toggle), salaire mensuel brut, date de début, licenciement pendant congé (toggle), retour poste différent (toggle). Bouton « Analyser » → tableau synthèse : durée (jours), date fin théorique, IJ journalière + totale estimées, protection licenciement jusqu'au + alertes visuelles.

**Pré-remplissage IA** depuis `aiData.travailExtractedData.congeMaternitePaternitéDetail`. **Cohérence F-IA-03** sur 4 `critereCode`.

## Cas d'erreur

- Backend 422 → message outil FR-only.
- 4xx/5xx → `MatSnackBar`.

## Critères d'acceptation

1. Section visible uniquement si `conge_maternite_paternite_detecte = true`.
2. Durée et IJ calculées correctement selon rang et type.
3. Date de protection licenciement affichée.
4. Alertes licenciement illégal et retour poste illégal bien distinctes.
5. Gate `isFrance` → bannière.
6. Pré-remplissage IA + `getPrefillCount()`.
7. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu, pré-remplissage, tableau calculs, alertes distinctes, gate `isFrance`.

## Tables / endpoints / composants impactés

- Nouveaux fichiers composant, helper, service.
- Modifié : `TOOL_REGISTRY`.

## Hors périmètre

Backend (→ SF-212-29).
