# SF-198-04 — Wrapper F-IA-04 pour Divorce CM Scoring (F-152, FR)

## Objectif (1 phrase)

Restaurer dans le panneau F-IA-04 l'outil `F-152-divorce-consentement-scoring` (DELETE par migration 191) en livrant un composant wrapper auto-suffisant qui réutilise `DivorceConsentementScoringSectionComponent` (déjà existant, présentationnel pur).

## Contexte

La migration 191 a DELETE le seed `F-152-divorce-consentement-scoring` car le composant `DivorceConsentementScoringSectionComponent` (livré par F-152) est utilisé uniquement dans `synthesis.component` et n'est pas auto-suffisant pour le panneau F-IA-04 (signatures `[detection]` + `[scoring]`, pas `caseFileId` + `synthesis`). Cette SF livre un composant wrapper qui prend `synthesis` et délègue le rendu au composant existant.

## Comportement nominal

- Le composant `<app-divorce-cm-scoring-section>` reçoit `synthesis: CaseAnalysisResult` et lit `synthesis.divorceConsentementValidityDetection` + `synthesis.divorceConsentementScoring`.
- Si l'une des deux données est `null` → état vide.
- Si présentes → délègue à `<app-divorce-consentement-scoring-section [detection] [scoring] />` (composant existant inchangé).
- Visible quand `legal_domain=DROIT_FAMILLE`, `country=FRANCE`, `type_procedure_detectee=DIVORCE_CONSENTEMENT_MUTUEL` (layer CONTEXTUAL — comme le seed original).

## Cas d'erreur

- Données absentes → état vide.

## Critères d'acceptation

1. Composant wrapper `divorce-cm-scoring-section` créé dans `frontend/src/app/case-files/divorce-cm-scoring-section/`.
2. Statics `TOOL_LABEL = 'VALIDITÉ DIVORCE CM'` + `TOOL_ICON = 'verified'`.
3. Entrée `['F-152-divorce-consentement-scoring', { component, inputs }]` dans `TOOL_REGISTRY`.
4. INSERT dans migration 212 (CONTEXTUAL trigger `type_procedure_detectee=DIVORCE_CONSENTEMENT_MUTUEL`).
5. `F-152-divorce-consentement-scoring` ajouté à `KNOWN_FRONTEND_TOOL_IDS`.
6. Build OK.

## Plan de test minimal

- **Jest** : wrapper rend `<app-divorce-consentement-scoring-section>` avec les bons inputs si données présentes ; n'invoque pas le child si données absentes.

## Tables / endpoints / composants impactés

- **Composants** : nouveau `DivorceCmScoringSectionComponent` (wrapper).
- **Réutilise** : `DivorceConsentementScoringSectionComponent` (livré par F-152, inchangé).
- **Tables** : `decision_tool_visibility_rules` (1 INSERT CONTEXTUAL).
- **Tests** : 1 nouveau .spec.ts.

## Hors périmètre

- Modification du composant `DivorceConsentementScoringSectionComponent` existant.
- Modification de `synthesis.component` (le rendu inline reste actif).

## Analyse de cohérence transversale

- Pattern wrapper miroir des autres wrappers SF-198.
- L'outil reste affiché à 2 endroits (synthesis inline + panel F-IA-04) — c'est le comportement attendu (le panel F-IA-04 est un agrégateur indépendant de synthesis).

## Impact par domaine métier

- **Sensibilité domaine** : OUI (Famille FR uniquement, divorce par consentement mutuel).
- **Belgique** : équivalent BE (`désunion irrémédiable` art. 229 CC) déjà couvert par `F-FA-11-desunion-irremediable-be`. Aucune action nécessaire.

## Parité des domaines métier

Niveau 5 (scoring / analyse validité). Le concept est spécifique au divorce FR. Travail/Immigration n/a. La parité est tenue (F-FA-11 BE existe).

## Nouveau pattern UI ou service partagé

Aucun — réutilisation d'un composant existant.
