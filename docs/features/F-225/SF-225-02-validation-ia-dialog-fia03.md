# SF-225-02 — Alignement validation IA dialog référentiels sur F-IA-03

## Objectif

Aligner `ReferentialEditDialogComponent` (admin référentiels) sur le pattern F-IA-03
des outils décisionnels : afficher des **alertes de cohérence live** (computed
signal + popover non-bloquant) à côté des champs édités, en complément du
dialog blocking serveur (`ReferentialWarningDialogComponent`) qui reste en place
pour les violations critiques détectées côté backend après submit.

Avant : validation Haiku au submit uniquement, dialog blocking après réponse.
Après : retour visuel **live** au changement de chaque champ (popover hover),
puis pré-flight blocking si le serveur le déclenche.

## Comportement nominal

- L'admin ouvre le dialog d'édition d'une entrée référentiel.
- À chaque modification d'un champ contrôlé (délai jours, valeur monétaire,
  pourcentage, jours/an, code enum), un signal `coherenceAlerts` est recalculé.
- Si un seuil est dépassé (ex. délai > 999 j, ans > 50, pourcentage > 100,
  description > 1500 car., code non blanchi), un badge `<app-coherence-popover-trigger>`
  apparaît à côté du champ avec un tooltip détaillé.
- Le formulaire **reste soumissible** — l'alerte est informative (warning).
  Seules les violations bloquantes (côté backend ou Validators stricts) empêchent
  la soumission.
- Le dialog blocking serveur (`ReferentialWarningDialogComponent`) reste inchangé
  côté flow parent — pas de breaking change.

## Cas d'erreur

- Aucun (les alertes sont non-bloquantes par construction).
- Si `coherenceAlerts()` retourne `{}`, aucune badge n'apparaît — comportement
  équivalent au comportement antérieur.

## Critères d'acceptation

1. `coherenceAlerts` est un computed signal exposé sur le composant.
2. Pour les types couverts (`LITIGATION_TYPE`, `IMMIGRATION_TITLES`,
   `IMMIGRATION_RECOURS`, `MAJEURS_PROTEGES_REGIMES`, `IM21_VALIDITY_CRITERES`,
   `CONVENTION_BAREMES`, `LICENCIEMENT_CRITERES`, `DIVORCE_ETAPES`), au moins une
   règle d'alerte simple est implémentée.
3. Les alertes utilisent le helper partagé `CoherenceAlertBuilder`
   (`shared/coherence-popover/coherence-alert-builder.ts`) — pas d'interface
   `CoherenceAlert` ad-hoc redéfinie.
4. Les badges utilisent `<app-coherence-popover-trigger>` ou la directive
   `appCoherencePopover` cohérente avec le reste du projet (le
   `caseFileId=''` et `explanations=[]` car il n'y a pas de source IA dossier
   ici — seule la `reason` du tooltip et le `blocker=false` sont pertinents).
5. Au moins 1 test Jest par règle d'alerte vérifie que `coherenceAlerts()`
   produit l'alerte attendue lorsque le seuil est franchi, et `null` sinon.
6. `npm run build` passe.
7. Tests Jest référentiels passent.

## Plan de test minimal

- **Unitaires** :
  - `EDT-FIA03-01` : `LITIGATION_TYPE` `litigYears = 31` → alerte WARNING sur
    `LITIG_YEARS` (seuil > 30).
  - `EDT-FIA03-02` : `IMMIGRATION_TITLES` `titleDelai = 1000` → alerte sur
    `TITLE_DELAI` (seuil > 999).
  - `EDT-FIA03-03` : `IMMIGRATION_RECOURS` `recoursDelai = 0` → alerte sur
    `RECOURS_DELAI` (seuil < 1).
  - `EDT-FIA03-04` : `MAJEURS_PROTEGES_REGIMES` `mpDelaiProcedure = 60` →
    alerte sur `MP_DELAI_PROCEDURE` (à la limite haute).
  - `EDT-FIA03-05` : `IM21_VALIDITY_CRITERES` description > 1500 char →
    alerte `IM21_DESCRIPTION` (long contenu peu lisible).
  - `EDT-FIA03-06` : `CONVENTION_BAREMES` `convConges = 0` → alerte
    `CONV_CONGES` (suspect : aucune convention).
  - `EDT-FIA03-07` : `LICENCIEMENT_CRITERES` `critPoids = 50` → alerte
    `CRIT_POIDS` (poids extrême — vérifier intentionnel).
  - `EDT-FIA03-08` : `DIVORCE_ETAPES` `etapeOrdre = 20` → alerte
    `ETAPE_ORDRE` (ordre extrême).
  - `EDT-FIA03-NONE` : valeurs nominales → `coherenceAlerts()` retourne `{}`.
- **Intégration** : pas requis (composant pure-frontend, pas d'I/O réseau dans
  cette SF).
- **Isolation workspace** : non applicable (admin global, pas de filter workspace
  côté front pour l'édition référentiel).

## Tables / endpoints / composants impactés

- **Composant modifié** : `frontend/src/app/referentials/referential-edit-dialog/referential-edit-dialog.component.ts`
  + `.html`.
- **Helper réutilisé** : `frontend/src/app/shared/coherence-popover/coherence-alert-builder.ts`
  (sans modification — utilisation directe).
- **Directive réutilisée** : `frontend/src/app/shared/coherence-popover/coherence-popover-trigger.directive.ts`
  (pas de modification — utilisation directe).
- **Tests modifiés** : `referential-edit-dialog.component.spec.ts` (ajout des cas
  `EDT-FIA03-*`).
- **Aucune** modification backend, DB, endpoint.

## Hors périmètre

- Pas de remplacement du dialog blocking serveur (`ReferentialWarningDialogComponent`)
  — il reste en place pour les violations critiques pré-submit.
- Pas de validation Haiku enrichie — le scope est l'infrastructure, pas la
  richesse des règles métier (les règles ajoutées ici sont des seuils simples,
  raisonnables, complémentaires au backend).
- Pas d'extension `CoherenceAlertBuilder` — ses sources (`F96`/`QUESTION_IA`/
  `IA`/`PIECE_MANQUANTE`/`MULTI`) restent telles quelles. Les alertes ici
  utilisent `IA` comme source par défaut (l'analyse "IA" étant ici la règle
  de seuil locale, sémantiquement compatible).
- Pas de migration des autres dialogs admin (workspace, plan, etc.) — scope
  isolé à `ReferentialEditDialog`.

## Impact par domaine métier

Cette feature est **transversale** — infrastructure d'admin référentiels qui
sert tous les domaines (Travail / Immigration / Famille) et tous les pays
(FR / BE). Les règles de seuil ajoutées sont génériques (jours, ans,
pourcentages, longueurs) et ne hardcodent aucun domaine. Aucune adaptation
spécifique par domaine n'est requise.

## Parité des domaines métier

Non applicable — pas un outil décisionnel niveau ≥ 5. C'est de l'infrastructure
admin transversale.

## Analyse de cohérence transversale

- **Pattern partagé** : `CoherenceAlertBuilder` + `appCoherencePopover` réutilisés
  tels quels — pas de divergence introduite.
- **Composants similaires** (autres dialogs admin) : pas concernés par cette SF
  (scope explicitement isolé). À considérer en SF de suivi si demandé.
- **Pas de nouveau pattern UI** — réutilisation stricte du pattern F-IA-03.
