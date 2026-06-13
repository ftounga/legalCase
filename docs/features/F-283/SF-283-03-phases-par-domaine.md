# SF-283-03 — Suggestions de phases par domaine × pays

> Complétion de **F-283** (Terminée). La frise des phases (SF-283-01) existe mais
> son enum `CasePhaseType` est en vocabulaire civil-FR (prud'hommes / JAF),
> inadapté à l'immigration (procédure administrative) et à la Belgique.

## Objectif (1 phrase)

Proposer, dans le formulaire d'ajout/édition de phase, une **liste ordonnée de
phases pertinentes selon (domaine × pays) du dossier**, avec un **libellé par
défaut pré-rempli et éditable**, sans rien casser pour les phases existantes.

## Comportement nominal

1. À l'ouverture du formulaire d'ajout/édition de phase, le front appelle
   `GET /api/v1/case-files/{id}/phases/suggestions`.
2. Le backend résout `(legalDomain du dossier, country du workspace propriétaire)`
   et renvoie la **liste ordonnée** `{type, defaultLabel}` du catalogue
   autoritatif correspondant.
3. Le sélecteur de type est peuplé avec ces suggestions (libellés lisibles,
   ordonnés).
4. À la sélection d'un type, le champ `label` est **pré-rempli** avec le
   `defaultLabel` correspondant (éditable — l'avocat peut renommer).
5. L'avocat reste libre de garder le type pré-sélectionné ou de renommer le
   libellé. Le `type` stocké reste l'enum `CasePhaseType`.

## Cas d'erreur / limites

- **(domaine, pays) inconnu** → fallback : liste générique civile FR travail
  (les 8 valeurs historiques) → ne casse rien.
- **Dossier d'un autre workspace / inexistant** → 404 (isolation via le dossier,
  mirror du endpoint timeline existant).
- **Non authentifié** → 401.
- **Phases existantes** (anciens types persistés) → s'affichent toujours via
  `phaseLabel()` (displayLabel générique par type ajouté à l'enum).

## Catalogues autoritatifs (domaine × pays)

Format : `ENUM_TYPE → "Libellé par défaut"`. Listes **ordonnées**.

### DROIT_DU_TRAVAIL × FRANCE
SAISINE, CONCILIATION, MISE_EN_ETAT, FOND, JUGEMENT, APPEL, CASSATION, EXECUTION.

### DROIT_DU_TRAVAIL × BELGIQUE
INTRODUCTION, MISE_EN_ETAT, FOND, JUGEMENT, APPEL, CASSATION, EXECUTION.

### DROIT_IMMIGRATION × FRANCE
RECOURS_PREALABLE, TRIBUNAL_ADMINISTRATIF, CNDA, COUR_ADMINISTRATIVE_APPEL,
CONSEIL_ETAT, EXECUTION.

### DROIT_IMMIGRATION × BELGIQUE
RECOURS_PREALABLE, CCE, CONSEIL_ETAT, EXECUTION.

### DROIT_FAMILLE × FRANCE
SAISINE, CONCILIATION, MISE_EN_ETAT, FOND, JUGEMENT, APPEL, CASSATION, EXECUTION.

### DROIT_FAMILLE × BELGIQUE
INTRODUCTION, CONCILIATION, MISE_EN_ETAT, FOND, JUGEMENT, APPEL, CASSATION, EXECUTION.

(Libellés exacts dans `CasePhaseSuggestionCatalog`.)

## Contrat API figé

```
GET /api/v1/case-files/{caseFileId}/phases/suggestions
→ 200 OK
  [
    { "type": "SAISINE", "defaultLabel": "Saisine du Conseil de prud'hommes" },
    ...
  ]
→ 404 si dossier hors workspace / inexistant
→ 401 si non authentifié
```

Lecture seule. Pas de body. Ordre = ordre métier du catalogue.

## Enum `CasePhaseType` — valeurs ajoutées (rétro-compat)

Ajout SANS toucher aux valeurs existantes (toutes ≤ 30 car., varchar(30) — pas de
migration) :
`RECOURS_PREALABLE`, `INTRODUCTION`, `TRIBUNAL_ADMINISTRATIF`, `CNDA`,
`COUR_ADMINISTRATIVE_APPEL` (25 car.), `CONSEIL_ETAT`, `CCE`. Chaque type a un
`order` par défaut sensé + un `displayLabel` générique.

## Critères d'acceptation

- [ ] Endpoint suggestions renvoie le bon catalogue ordonné pour les 6
      combinaisons (DT/IM/FA × FR/BE).
- [ ] Combinaison inconnue → fallback liste civile FR travail (8 entrées).
- [ ] Isolation : 404 cross-workspace, 401 non auth.
- [ ] Enum étendu, varchar(30) respecté, aucune migration.
- [ ] Front : sélecteur peuplé par les suggestions ; sélection d'un type
      pré-remplit le champ `label` (éditable).
- [ ] Phases existantes (anciens types) s'affichent toujours.

## Plan de test minimal

- **Unitaire `CasePhaseSuggestionCatalogTest`** : 6 combinaisons (taille + ordre +
  1er/dernier type + defaultLabel) + fallback combinaison inconnue.
- **IT `CasePhaseControllerIT`** : `GET .../suggestions` 200 (DT×FR), 404
  cross-workspace, 401 non auth, + un cas BE/immigration via legalDomain/country.
- **Front spec** : sélection d'un type pré-remplit le `label` ; suggestions
  chargées à l'ouverture.

## Tables / endpoints / composants impactés

- Backend : `CasePhaseType` (enum étendu), `CasePhaseSuggestionCatalog` (new),
  `CasePhaseSuggestion` record (new), `CasePhaseService` (méthode `suggestions`),
  `CasePhaseController` (`GET .../suggestions`).
- Frontend : `case-phase.model.ts` (types + suggestion model), `case-phase.service.ts`
  (méthode `suggestions`), `case-phases-timeline.component.*` (peuplement + pré-remplissage).
- **Aucune table / migration** (varchar(30) accepte les nouveaux enums).

## Hors périmètre

- Pas de blocage du choix de type (tous restent choisissables côté back ; le front
  met en avant les pertinents).
- Pas de pré-remplissage IA de la date d'entrée.
- Pas de modification du tri de la frise (reste piloté par `entered_at`).
- Pas de nouveau plan / quota / route / guard.

## Préoccupations transversales

- Auth / Principal : inchangé (réutilise `resolveUser` / `resolveCaseFileForUser`).
- Workspace context : inchangé (résolution via le dossier, comme le timeline).
- Plans / limites : aucune.
- Navigation / routing : aucune nouvelle route front (même onglet Suivi).
- Outil décisionnel : non — suggestion de libellés, pas de calcul métier.

→ **Aucune préoccupation transversale déclenchée** (pas de smoke E2E requis).
