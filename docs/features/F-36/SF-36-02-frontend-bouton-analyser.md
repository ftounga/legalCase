# SF-36-02 — Frontend : bouton "Analyser le dossier"

## Objectif
Ajouter un bouton "Analyser le dossier" dans `CaseFileDetailComponent`, visible quand au moins un document est analysé (DOCUMENT_ANALYSIS DONE) et qu'aucune analyse dossier n'est en cours.

## Comportement nominal
1. L'utilisateur uploade ses documents → document analysis se termine → statut DOCUMENT_ANALYSIS = DONE
2. Le bouton "Analyser le dossier" apparaît
3. L'utilisateur clique → appel `POST /api/v1/case-files/{id}/analyze`
4. Le polling démarre → l'UI suit la progression (CASE_ANALYSIS PROCESSING puis DONE)
5. Quand DONE : bandeau "Voir la synthèse" cliquable → `/case-files/:id/synthesis`

## Cas d'erreur
- `402` : snackbar "Limite d'analyses atteinte pour ce dossier. Passez au plan supérieur."
- `402` (budget tokens) : snackbar identique
- `409` : snackbar "Une analyse est déjà en cours"
- `422` : snackbar "Aucun document analysé disponible"
- Erreur réseau : snackbar générique

## Critères d'acceptation
- [ ] Bouton visible uniquement si DOCUMENT_ANALYSIS = DONE et CASE_ANALYSIS absent ou FAILED
- [ ] Bouton masqué si CASE_ANALYSIS = PENDING ou PROCESSING (spinner à la place)
- [ ] Clic → appel `POST /api/v1/case-files/{id}/analyze` via nouveau `CaseAnalysisCommandService`
- [ ] Chaque erreur 402/409/422 affiche un snackbar distinct
- [ ] `managePolling` : suppression de la condition `analysisStarting` (auto-trigger retiré)
- [ ] Tests : `CaseFileDetailComponent` couvre les états bouton visible/caché/spinner

## Plan de test
**Unitaires (Karma) :**
- Bouton visible si DOCUMENT_ANALYSIS=DONE et pas de CASE_ANALYSIS → oui
- Bouton masqué si CASE_ANALYSIS=PROCESSING → non, spinner affiché
- Bouton masqué si aucun document DONE → non
- Clic bouton → service appelé
- 402 → snackbar "Limite atteinte"
- 409 → snackbar "En cours"

**Manuel :**
- Upload 1 doc → attendre DONE → bouton apparaît → cliquer → polling → synthèse disponible

## Composants impactés
- `CaseFileDetailComponent` — bouton + logique analyze + polling simplifié
- Nouveau `CaseAnalysisCommandService` — `POST /api/v1/case-files/{id}/analyze`
- `CaseFileDetailComponent` spec — nouveaux cas de test

## Hors périmètre
- Compteur "N/5 analyses" (déféré — géré par 402)
- Modification de `SynthesisComponent`
- Modification du backend (SF-36-01)
