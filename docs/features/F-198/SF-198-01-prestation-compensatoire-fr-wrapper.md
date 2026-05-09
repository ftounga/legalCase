# SF-198-01 — Wrapper F-IA-04 pour Prestation Compensatoire (F-FA-01, FR)

## Objectif (1 phrase)

Restaurer dans le panneau F-IA-04 l'outil `F-FA-01-prestation-compensatoire` (DELETE par migration 191) en livrant un composant Angular auto-suffisant qui rend le bloc "Prestation compensatoire indicative" précédemment hébergé inline dans `synthesis.component`.

## Contexte

La migration 191 a supprimé l'entrée `decision_tool_visibility_rules` pour `F-FA-01-prestation-compensatoire` au motif que le composant frontend auto-suffisant n'existait pas (voir audit F-191 §A.3). Le calcul backend (`PrestationCompensatoireCalculator`) reste inchangé : la fourchette `synthesis.prestationCompensatoireEstimate` est toujours produite par le pipeline IA. Cette SF expose cette donnée via le panneau décisionnel.

## Comportement nominal

- Le composant `<app-prestation-compensatoire-section>` reçoit `synthesis: CaseAnalysisResult` en `@Input()` et lit `synthesis.prestationCompensatoireEstimate`.
- Si l'estimate est `null/undefined` → message "Aucune donnée disponible" (cohérent avec les autres outils ALWAYS_ON présentationnels).
- Si l'estimate est présent → rendu identique au bloc `mat-expansion-panel` actuellement dans `synthesis.component.html` lignes 600-644 (fourchette de capital, écart de revenus, barème, jurisprudenceRange p25/p50/p75).
- Le composant n'envoie aucune requête HTTP (présentationnel pur).
- Visible côté panneau F-IA-04 quand `legal_domain=DROIT_FAMILLE` et `country=FRANCE` (layer ALWAYS_ON).

## Cas d'erreur

- `synthesis` non transmis → composant affiche état vide, pas d'erreur.
- `synthesis.prestationCompensatoireEstimate.donneesPartielles=true` → bandeau warning (idem synthesis.component.html actuel).

## Critères d'acceptation

1. Le composant `prestation-compensatoire-section` existe dans `frontend/src/app/case-files/prestation-compensatoire-section/`.
2. Il expose `static TOOL_LABEL = 'PRESTATION COMPENSATOIRE'` et `static TOOL_ICON = 'balance'`.
3. Une entrée `['F-FA-01-prestation-compensatoire', { component, inputs }]` est présente dans `TOOL_REGISTRY`.
4. Une entrée `INSERT INTO decision_tool_visibility_rules` est présente dans la migration 212.
5. `F-FA-01-prestation-compensatoire` est ajouté à `KNOWN_FRONTEND_TOOL_IDS` du test `DecisionToolVisibilityIntegrityIT`.
6. Build frontend + backend passent.

## Plan de test minimal

- **Jest unitaire** : composant rend l'état vide quand `synthesis = null` ; rend la fourchette quand `synthesis.prestationCompensatoireEstimate` est fourni avec valeurs typiques (montantMin=15k, montantMax=50k, ecartRevenus=2k, pays=FRANCE).
- **DecisionToolVisibilityIntegrityIT** : assertion sur la liste KNOWN_FRONTEND_TOOL_IDS (déjà couverte automatiquement).
- **Isolation workspace** : aucun appel API → non applicable.

## Tables / endpoints / composants impactés

- **Composants** : nouveau `PrestationCompensatoireSectionComponent` (frontend).
- **Tables** : `decision_tool_visibility_rules` (1 INSERT).
- **Endpoints** : aucun (pure présentation).
- **Tests** : 1 nouveau .spec.ts ; mise à jour de `DecisionToolVisibilityIntegrityIT.KNOWN_FRONTEND_TOOL_IDS`.

## Hors périmètre

- Refonte du calculateur backend.
- Ajout de logique de pré-fill avocat (le composant est pure lecture des estimations IA).
- Ajout de validation F-IA-03 (pas de saisie utilisateur, pas d'écart possible avec les sources IA).

## Analyse de cohérence transversale

- **Autres outils décisionnels** : pattern présentationnel pur, miroir de `RuptureAmiableInfoSectionComponent` (F-132-rupture-amiable-info) — aucune dette de convergence.
- **Autres pays/domaines** : prestation compensatoire BE est déjà dans le même `prestationCompensatoireEstimate.pays='BELGIQUE'` (le composant gère les deux pays dans le même rendu). Pas de SF jumelle nécessaire — la règle de visibilité est `country=FRANCE` au seed, mais le composant rend correctement les deux pays si l'avocat naviguait sur un dossier BE (cas non déclenché par le seed actuel).
- **UI patterns** : réutilise les classes CSS `compensation-block`, `compensation-grid`, `compensation-row`, `compensation-disclaimer` déjà partagées dans `synthesis.component.scss`. Une duplication minimale est acceptable — alternative serait d'extraire un composant partagé, mais l'effort dépasse le scope du rattrapage.

## Impact par domaine métier

- **Sensibilité domaine** : OUI (Famille FR uniquement). La SF est livrée pour Droit Famille FR. La prestation compensatoire BE existe également — elle est rendue par le même composant grâce au champ `pays` dans l'estimate.
- **Travail / Immigration** : non applicable.
- **Belgique** : la règle de visibilité est `country=FRANCE` ; un seed BE est hors scope du rattrapage F-198 (qui restaure exactement ce que migration 191 a DELETE).

## Parité des domaines métier

Niveau d'outil : 6 (comparateur / fourchettes). Le concept "prestation compensatoire" est spécifique au divorce (Famille). Travail et Immigration n'ont pas d'équivalent — n/a.

## Nouveau pattern UI ou service partagé

Aucun. Le composant réutilise les patterns existants.
