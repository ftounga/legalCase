# SF-225-01 — UX 5 types orphelins écran Référentiels (Guide & Barème)

## Objectif (1 phrase)

Brancher 5 nouveaux `referential_type` (`CONVENTION_PREAVIS`, `TRAVAIL_PROCEDURE_JALONS`, `FAMILLE_PROCEDURE_JALONS`, `MAJEURS_PROTEGES_REGIMES`, `IM21_VALIDITY_CRITERES`) dans l'écran "Guide & Barème" pour qu'ils s'affichent avec un titre humain, une valeur lisible, une icône adaptée et un formulaire d'édition typé.

## Contexte

Constat de l'audit 2026-05-06 (cf. F-225 dans `PRODUCT_SPEC.md`) : ces 5 types apparaissent côté UI avec :
- titre brut (code DB au lieu de label),
- valeur affichée en `JSON.stringify` (pas de `formatValue` dédié),
- icône `info` générique,
- édition en `textarea` JSON brut (pas de `buildForm` typé).

Les descriptions DB (`description` colonne) sont déjà seedées correctement (règle SF-140-03 respectée). Le pré-remplissage IA / validation F-IA-03 ne sont pas dans le scope (couverts par SF-225-02).

## Comportement nominal

- Pour chacun des 5 types : `SECTION_LABELS[type]` retourne un label français humain.
- `sectionIcon(type)` retourne une icône MatIcon métier (description / event_note / gavel / accessibility / verified).
- `formatValue(entry, type)` retourne une string lisible structurée (pas de JSON brut).
- Le edit dialog ouvre un formulaire typé adapté à la structure JSON, sérialise correctement à la soumission.

## Cas d'erreur

- Si la `valueJson` ne parse pas : fallback string brute (déjà en place, `formatValue` retourne `entry.valueJson`).
- Si la structure JSON ne correspond pas au schéma attendu (ex : pas de `fonctions` dans CONVENTION_PREAVIS) : fallback formulaire JSON brut (pattern déjà utilisé pour `INDEMNITE_BAREMES` et `IMMIGRATION_JALONS`).

## Critères d'acceptation

1. `SECTION_LABELS` contient les 5 nouvelles entrées avec les libellés exacts demandés.
2. `sectionIcon()` retourne respectivement `description`, `event_note`, `event_note`, `accessibility`, `verified` pour les 5 types (ou icônes équivalentes documentées).
3. `formatValue()` retourne une string non-JSON pour les 5 types sur des entrées valides.
4. Le edit dialog propose un formulaire structuré pour chacun des 5 types, sérialise via `JSON.stringify` une valeur reproduisant la structure d'origine.
5. Le build frontend passe (`npm run build`).
6. Les tests Jest passent (`npm test --testPathPattern referentials`).

## Plan de test

### Tests unitaires (`referentials.component.spec.ts`)

- `SECTION_LABELS[type]` retourne le bon label pour chacun des 5 types (5 assertions).
- `formatValue` produit une chaîne non-JSON pour chacun des 5 types sur un payload réel issu des migrations Liquibase (5 cas).
- `sectionIcon` retourne l'icône attendue (5 cas).

### Tests unitaires (`referential-edit-dialog.component.spec.ts`)

- CONVENTION_PREAVIS : pré-remplit l'article et le nombre de catégories de fonctions ; sérialise un objet `{fonctions, article}` sur submit.
- TRAVAIL_PROCEDURE_JALONS : pré-remplit la liste des jalons (label/offsetDays/articleRef) ; sérialise un tableau d'objets sur submit.
- FAMILLE_PROCEDURE_JALONS : idem TRAVAIL_PROCEDURE_JALONS.
- MAJEURS_PROTEGES_REGIMES : pré-remplit les champs `delaiProcedureMois`, `delaiInitialAnsMax`, `renouvelable`, articles/criteresEligibilite ; sérialise un objet conforme.
- IM21_VALIDITY_CRITERES : pré-remplit `binaire` et `description` ; sérialise `{binaire, description}`.

### Tests d'intégration

Hors scope — pas de nouvel endpoint backend.

### Isolation workspace

Pas concerné — l'écran référentiels lit la table système (workspace_id null).

## Tables / endpoints / composants impactés

- **Composants Angular** :
  - `frontend/src/app/referentials/referentials.component.ts` — `SECTION_LABELS`, `formatValue`, `sectionIcon`, `extractMetierDescription` (fallback).
  - `frontend/src/app/referentials/referential-edit-dialog/referential-edit-dialog.component.ts` — `buildForm`, `serializeValueJson`, FormArray getters/builders si nécessaire.
  - `frontend/src/app/referentials/referential-edit-dialog/referential-edit-dialog.component.html` — 5 nouveaux blocs `@case`.
- **Tests** : 2 fichiers `.spec.ts` correspondants.
- **Tables / endpoints backend** : aucun changement.

## Hors périmètre

- Validation IA "live" au changement (couvert par SF-225-02).
- Garde-fou CI sur l'intégrité `referential_type ↔ SECTION_LABELS` (couvert par SF-225-03).
- Refonte du dialog (laissée à V2 hypothétique).
- Ajout de nouveaux référentiels (couvert par F-200 à F-223 selon plan C×D).

## Analyse de cohérence transversale

- **Autres types `legal_referentials` non orphelins** : LITIGATION_TYPE, BAREME_MACRON, IMMIGRATION_*, PRESTATION_COEFF, CONVENTION_BAREMES, etc. — déjà intégrés UX. Non impactés.
- **Pays** : 4 types sont FR (CONVENTION_PREAVIS, FAMILLE_PROCEDURE_JALONS BE, IM21_VALIDITY_CRITERES FR), 1 multi (TRAVAIL_PROCEDURE_JALONS FR + BE). Le rendu UX ne dépend pas du pays — la colonne `country` est déjà affichée par le composant.
- **Domaines** : couvre Travail (CONVENTION_PREAVIS, TRAVAIL_PROCEDURE_JALONS), Famille (FAMILLE_PROCEDURE_JALONS, MAJEURS_PROTEGES_REGIMES), Immigration (IM21_VALIDITY_CRITERES) — les 3 domaines.
- **Pattern UI partagé** : la convention `SECTION_LABELS + formatValue + sectionIcon + buildForm switch case` est réutilisée à l'identique. Pas de nouveau pattern introduit. La SF s'inscrit dans l'extension du switch existant — pas de risque de divergence.

## Nouveau pattern UI ou service partagé

Aucun. La SF étend un pattern existant (switch sur `sectionType`).

## Impact par domaine métier

Cette feature **est sensible au domaine** dans son contenu (les 5 types couvrent Travail / Famille / Immigration), mais **transversale dans son comportement UI** : un seul composant Angular branche les 3 domaines. Les workspaces voient uniquement les types de leur domaine grâce au filtre côté backend (`WHERE legal_domain = :domain`). FR + BE déjà gérés par la colonne `country` affichée.

## Préoccupations transversales

- Auth / Principal : non concerné.
- Workspace context : non concerné (table système, déjà filtré côté backend).
- Plans / limites : non concerné.
- Navigation / routing : non concerné.
- Outil décisionnel métier : **non** — l'écran Guide & Barème n'est pas un outil décisionnel, c'est un viewer/editor de référentiels.

## Décision de design : 1 form table générique vs 5 forms custom

Choix : **5 forms custom** (suivre le pattern existant du dialog) — chaque type a une structure JSON différente, et le pattern actuel du dialog (switch case avec `buildForm` retournant un FormGroup typé) est plus lisible et plus testable. Pour CONVENTION_PREAVIS et MAJEURS_PROTEGES_REGIMES qui ont des structures imbriquées profondes, on combine champs scalaires + textarea pour les sous-arrays (lignes séparées par `\n`, pattern déjà utilisé pour `IMMIGRATION_PIECES`). Cela évite l'explosion de FormArrays imbriqués (la structure CONVENTION_PREAVIS = 4 fonctions × N tranches d'ancienneté = trop complexe pour un dialog rapide). Si l'admin a besoin d'éditer la matrice complète, il bascule sur le textarea JSON.

**Décision finale par type** :

| Type | Approche dialog |
|------|----------------|
| CONVENTION_PREAVIS | textarea JSON typed avec validation `jsonValidator` + champ `article` séparé |
| TRAVAIL_PROCEDURE_JALONS | FormArray `jalons` (label, offsetDays, articleRef) — extension du pattern IMMIGRATION_JALONS |
| FAMILLE_PROCEDURE_JALONS | FormArray identique (réutilise les builders) |
| MAJEURS_PROTEGES_REGIMES | champs scalaires (delaiProcedureMois, delaiInitialAnsMax, renouvelable) + 2 textareas (articles, criteresEligibilite) |
| IM21_VALIDITY_CRITERES | toggle `binaire` + textarea `description` |
