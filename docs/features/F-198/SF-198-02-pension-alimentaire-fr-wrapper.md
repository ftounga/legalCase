# SF-198-02 — Wrapper F-IA-04 pour Pension Alimentaire (F-FA-02, FR)

## Objectif (1 phrase)

Restaurer dans le panneau F-IA-04 l'outil `F-FA-02-pension-alimentaire` (DELETE par migration 191) en livrant un composant Angular auto-suffisant qui rend le bloc "Pension alimentaire indicative" précédemment hébergé inline dans `synthesis.component`.

## Contexte

La migration 191 a supprimé l'entrée `decision_tool_visibility_rules` pour `F-FA-02-pension-alimentaire` au motif que le composant frontend auto-suffisant n'existait pas (audit F-191 §A.3). Le calcul backend (`PensionAlimentaireCalculator`) reste inchangé : la fourchette mensuelle `synthesis.pensionAlimentaireEstimate` est toujours produite par le pipeline IA (incluant la fourchette jurisprudentielle JAF p25/p50/p75 livrée par F-153).

## Comportement nominal

- Le composant `<app-pension-alimentaire-section>` reçoit `synthesis: CaseAnalysisResult` en `@Input()` et lit `synthesis.pensionAlimentaireEstimate`.
- Si l'estimate est `null/undefined` → message "Aucune donnée disponible".
- Si l'estimate est présent → rendu identique au bloc `mat-expansion-panel` actuellement dans `synthesis.component.html` lignes 554-598 (fourchette mensuelle, revenus net débiteur, barème UNAF/CGKR, jurisprudenceRange p25/p50/p75).
- Présentationnel pur, pas de requête HTTP.
- Visible côté panneau F-IA-04 quand `legal_domain=DROIT_FAMILLE` et `country=FRANCE` (layer ALWAYS_ON).

## Cas d'erreur

- `synthesis` non transmis → état vide, pas d'erreur.
- `donneesPartielles=true` → bandeau warning identique à l'inline existant.

## Critères d'acceptation

1. Composant `pension-alimentaire-section` créé dans `frontend/src/app/case-files/pension-alimentaire-section/`.
2. Statics `TOOL_LABEL = 'PENSION ALIMENTAIRE'` + `TOOL_ICON = 'family_restroom'`.
3. Entrée `['F-FA-02-pension-alimentaire', { component, inputs }]` dans `TOOL_REGISTRY`.
4. INSERT dans migration 212.
5. `F-FA-02-pension-alimentaire` ajouté à `KNOWN_FRONTEND_TOOL_IDS`.
6. Build frontend + backend passent.

## Plan de test minimal

- **Jest unitaire** : état vide quand `synthesis=null` ; rendu correct quand `pensionAlimentaireEstimate` fourni (montantMin=350, montantMax=550, modeGarde=EXCLUSIVE, pays=FRANCE).
- **DecisionToolVisibilityIntegrityIT** : couvert par la mise à jour de la liste.
- **Isolation workspace** : non applicable.

## Tables / endpoints / composants impactés

- **Composants** : nouveau `PensionAlimentaireSectionComponent`.
- **Tables** : `decision_tool_visibility_rules` (1 INSERT).
- **Endpoints** : aucun.
- **Tests** : 1 nouveau .spec.ts ; mise à jour `DecisionToolVisibilityIntegrityIT`.

## Hors périmètre

- Outil de saisie/recalcul à la main (le composant lit l'estimation IA pré-calculée).
- Pré-fill F-IA-03 / validation (pas de saisie).

## Analyse de cohérence transversale

- Pattern miroir de SF-198-01 (prestation-compensatoire-section).
- Famille BE : déjà dans `pensionAlimentaireEstimate.pays='BELGIQUE'` (CGKR Belgique vs UNAF France) — un seed BE est hors scope F-198.

## Impact par domaine métier

- **Sensibilité domaine** : OUI (Famille FR). Le composant gère également BE via le champ `pays` mais la règle de visibilité reste `country=FRANCE`.
- **Travail / Immigration** : n/a.

## Parité des domaines métier

Niveau d'outil : 6 (fourchettes). Concept spécifique au droit de la famille. n/a Travail/Immigration.

## Nouveau pattern UI ou service partagé

Aucun.
