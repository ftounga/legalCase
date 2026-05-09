# SF-198-03 — Wrapper F-IA-04 pour Liquidation Communauté (F-FA-04, FR)

## Objectif (1 phrase)

Restaurer dans le panneau F-IA-04 l'outil `F-FA-04-liquidation-communaute` (DELETE par migration 191) en livrant un composant Angular auto-suffisant qui rend le bloc "Liquidation de communauté" précédemment hébergé inline dans `synthesis.component`.

## Contexte

La migration 191 a supprimé l'entrée `decision_tool_visibility_rules` pour `F-FA-04-liquidation-communaute` (audit F-191 §A.3). Le backend remplit toujours `synthesis.liquidationCommunaute` avec l'inventaire des biens (actifCommun, biensPropresEpouxA, biensPropresEpouxB, passifCommun) issu de l'analyse IA.

## Comportement nominal

- Le composant `<app-liquidation-communaute-section>` reçoit `synthesis: CaseAnalysisResult` en `@Input()` et lit `synthesis.liquidationCommunaute`.
- Si la donnée est `null` → message "Aucune donnée disponible".
- Si présente → rendu identique au bloc `mat-expansion-panel` actuellement dans `synthesis.component.html` lignes 646-682 (les 4 sections de biens avec valeur formatée).
- Présentationnel pur.
- Visible quand `legal_domain=DROIT_FAMILLE` et `country=FRANCE` (layer ALWAYS_ON).

## Cas d'erreur

- `synthesis` absent → état vide.
- Section `items.length === 0` → "Aucun bien détecté" (idem inline existant).

## Critères d'acceptation

1. Composant `liquidation-communaute-section` créé dans `frontend/src/app/case-files/liquidation-communaute-section/`.
2. Statics `TOOL_LABEL = 'LIQUIDATION DE COMMUNAUTÉ'` + `TOOL_ICON = 'account_balance'`.
3. Entrée `['F-FA-04-liquidation-communaute', { component, inputs }]` dans `TOOL_REGISTRY`.
4. INSERT dans migration 212.
5. `F-FA-04-liquidation-communaute` ajouté à `KNOWN_FRONTEND_TOOL_IDS`.
6. Build OK.

## Plan de test minimal

- **Jest** : état vide ; rendu correct avec un mock liquidationCommunaute (regimeMatrimonial='COMMUNAUTE_LEGALE', actifCommun=[{libelle:'Maison', valeur:300000}], etc.).
- **DecisionToolVisibilityIntegrityIT** : couvert.

## Tables / endpoints / composants impactés

- **Composants** : nouveau `LiquidationCommunauteSectionComponent`.
- **Tables** : `decision_tool_visibility_rules` (1 INSERT).
- **Endpoints** : aucun.
- **Tests** : 1 nouveau .spec.ts.

## Hors périmètre

- Calcul soulte / récompenses (déjà dans F-FA-05-partage-immobilier et F-FA-15-recompenses).
- Saisie manuelle des biens.

## Analyse de cohérence transversale

- Pattern présentationnel miroir SF-198-01/02.
- Distinct de F-FA-05 (partage immobilier — calcul soulte) et F-FA-15 (récompenses) qui sont des outils interactifs avec API.

## Impact par domaine métier

- **Sensibilité domaine** : OUI (Famille FR uniquement). Belgique : pas de seed dans cette SF (rattrapage strict de la migration 191).

## Parité des domaines métier

Niveau 1-2 (checklist/inventaire). Spécifique famille. n/a Travail/Immigration.

## Nouveau pattern UI ou service partagé

Aucun.
