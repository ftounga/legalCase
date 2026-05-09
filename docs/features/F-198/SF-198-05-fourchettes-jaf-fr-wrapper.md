# SF-198-05 — Wrapper F-IA-04 pour Fourchettes JAF (F-153, FR)

## Objectif (1 phrase)

Restaurer dans le panneau F-IA-04 l'outil `F-153-fourchettes-jaf` (DELETE par migration 191) en livrant un composant Angular auto-suffisant qui agrège les fourchettes jurisprudentielles JAF (p25/p50/p75) actuellement disséminées dans `pensionAlimentaireEstimate.jurisprudenceRange` et `prestationCompensatoireEstimate.jurisprudenceRange`.

## Contexte

La migration 191 a DELETE `F-153-fourchettes-jaf` (audit F-191 §A.3 : composant inexistant). Le calcul backend F-153 enrichit `pensionAlimentaireEstimate.jurisprudenceRange` et `prestationCompensatoireEstimate.jurisprudenceRange` avec p25/p50/p75. Cette SF livre un composant qui rassemble les fourchettes accessibles dans le panneau F-IA-04 sous une forme "comparateur".

## Comportement nominal

- Le composant `<app-fourchettes-jaf-section>` reçoit `synthesis: CaseAnalysisResult` en `@Input()`.
- Lit `synthesis.pensionAlimentaireEstimate?.jurisprudenceRange` et `synthesis.prestationCompensatoireEstimate?.jurisprudenceRange`.
- Si aucune des deux n'est présente → état vide.
- Affiche un tableau condensé : pour chaque fourchette disponible, libellé + p25/médiane/p75 + sourceRef en tooltip.
- Présentationnel pur.
- Visible quand `legal_domain=DROIT_FAMILLE`, `country=FRANCE` (layer ALWAYS_ON comme le seed original).

## Cas d'erreur

- Aucune fourchette présente → état vide.

## Critères d'acceptation

1. Composant `fourchettes-jaf-section` créé dans `frontend/src/app/case-files/fourchettes-jaf-section/`.
2. Statics `TOOL_LABEL = 'FOURCHETTES JURISPRUDENTIELLES JAF'` + `TOOL_ICON = 'analytics'`.
3. Entrée `['F-153-fourchettes-jaf', { component, inputs }]` dans `TOOL_REGISTRY`.
4. INSERT dans migration 212.
5. `F-153-fourchettes-jaf` ajouté à `KNOWN_FRONTEND_TOOL_IDS`.
6. Build OK.

## Plan de test minimal

- **Jest** : état vide quand aucune fourchette ; rendu correct avec mock pension+prestation contenant `jurisprudenceRange`.

## Tables / endpoints / composants impactés

- **Composants** : nouveau `FourchettesJafSectionComponent`.
- **Tables** : `decision_tool_visibility_rules` (1 INSERT).
- **Endpoints** : aucun.
- **Tests** : 1 nouveau .spec.ts.

## Hors périmètre

- Filtrage par tribunal (les sourceRef sont déjà à granularité variable).
- Comparaison multi-paramètres (le composant rend ce que le backend remonte, ni plus ni moins).

## Analyse de cohérence transversale

- Pattern présentationnel miroir SF-198-01/02/03.
- Le contenu est superposé avec ce que `pension-alimentaire-section` et `prestation-compensatoire-section` affichent déjà (fourchette en bas du bloc) — c'est intentionnel : le panel F-IA-04 est un agrégateur, et l'outil "fourchettes JAF" matérialise le concept de manière indépendante.

## Impact par domaine métier

- **Sensibilité domaine** : OUI (Famille FR — JAF est une institution française). Pas d'équivalent BE direct (CGKR Belgique a son propre mode de calcul, déjà rendu par les outils pension/prestation).

## Parité des domaines métier

Niveau 6 (comparateur / fourchettes). Concept FR-spécifique. n/a Travail/Immigration/BE.

## Nouveau pattern UI ou service partagé

Aucun.
