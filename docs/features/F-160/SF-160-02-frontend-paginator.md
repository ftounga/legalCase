# SF-160-02 — Frontend paginator par bloc (checklist + questions)

## Objectif

Permettre à l'avocat de naviguer dans l'historique des **checklists procédurales** (F-96) et des **questions IA** (F-13/F-14) **par itération de synthèse**, indépendamment l'un de l'autre, sur l'écran `SynthesisComponent`.

## Comportement nominal

1. Au chargement de la page, les blocs "Checklist procédurale" et "Questions complémentaires" affichent par défaut **la dernière itération** (correspondant à la version sélectionnée globalement).
2. Si `versions().length >= 2`, chaque bloc affiche dans son header une zone **paginator** :
   - Bouton ‹ (prev)
   - Badge texte « Itération N / Total »
   - Bouton › (next)
3. Cliquer ‹ ou › sur le bloc Checklist déclenche `loadChecksForVersion(caseFileId, analysisId)` pour la nouvelle itération **sans toucher** au bloc Questions ni à la synthèse.
4. Idem symétriquement sur le bloc Questions.
5. Quand l'avocat change la version globale via le dropdown existant, les deux paginators **se resynchronisent** sur la version sélectionnée.

## Cas d'erreur / edge cases

- Si `versions().length === 1` : aucun paginator affiché (1 seule itération).
- Si itération navigée a 0 checks ou 0 questions : afficher un message neutre dans le bloc (« Aucune entrée pour cette itération »).
- Si l'API `procedure-checks` ou `ai-questions` retourne 4xx/5xx : snackbar d'erreur, conserver l'itération précédente affichée.
- Boutons ‹ et › désactivés aux bornes (première / dernière itération).

## Critères d'acceptation

- [ ] Avec 2 versions DONE, le bloc Checklist montre `‹ Itération 2 / 2 ›` au chargement, ‹ désactivé sur la version la plus ancienne, › désactivé sur la plus récente.
- [ ] Cliquer ‹ sur Checklist quand on est en `Itération 2 / 2` → recharge les checks via `procedureCheckService.list(caseFileId, analysisIdV1)` et affiche `‹ Itération 1 / 2 ›`.
- [ ] Le bloc Questions reste sur sa propre itération quand le paginator Checklist est cliqué.
- [ ] Changer la version globale via le dropdown réaligne les deux paginators.
- [ ] Avec 1 seule version, aucun paginator affiché.

## Plan de test minimal

- **Jest unitaires** (synthesis.component.spec.ts) :
  - U1 : `setChecklistIteration(version)` met à jour `currentChecksVersion` + appelle `procedureCheckService.list` avec le bon `analysisId`.
  - U2 : `setQuestionsIteration(version)` met à jour `currentQuestionsVersion` + appelle `aiQuestionService.getQuestionsByAnalysisId` avec le bon `analysisId`.
  - U3 : `onVersionChange(v)` réinitialise les deux paginators.
  - U4 : `canChecksPrev`/`canChecksNext` cohérents avec la position dans `versions()`.
- **Smoke E2E** : non requis (UI interne d'écran existant, pas de chemin critique d'auth/routing).

## Tables / endpoints / composants impactés

- **Composant Angular** : `frontend/src/app/case-files/synthesis/synthesis.component.ts` + `.html`
- **Tests** : `frontend/src/app/case-files/synthesis/synthesis.component.spec.ts`
- **Endpoints (déjà existants — SF-160-01 a verrouillé leur contrat)** :
  - `GET /api/v1/case-files/{cf}/case-analysis/versions`
  - `GET /api/v1/case-files/{cf}/analyses/{analysisId}/procedure-checks`
  - `GET /api/v1/case-files/{cf}/ai-questions?analysisId={id}`
- **Aucune table / migration**.
- **Aucun nouveau service Angular**.

## Hors périmètre

- Pagination des autres blocs (faits, risques, timeline, points juridiques, pièces manquantes) → couvert par F-162.
- Déduplication sémantique des questions au moment de la regénération → SF-160-03 optionnelle.
- Refonte visuelle de l'écran synthèse (grille de badges) → F-162.

## Analyse de cohérence transversale

- **Préoccupations transversales** : aucune (auth/workspace/routing/plans inchangés).
- **Nouveau pattern UI** : un paginator inline « ‹ N / Total › » dans un header de panel `mat-expansion-panel`. Pas de directive partagée nécessaire — inline simple ; si plus tard d'autres blocs adoptent ce pattern (F-162), une factorisation pourra être faite. Pas de dette de convergence immédiate (pas d'autre paginator concurrent aujourd'hui dans le projet).
- **Impact par domaine métier** : transversal — affecte tous les domaines (Travail / Immigration / Famille) et les 2 pays (FR / BE) de manière identique, c'est de l'infra UI sur l'écran de synthèse partagé.

## Contrat API

Aucun nouvel endpoint. SF-160-02 frontend pure consommation des endpoints validés par SF-160-01.
