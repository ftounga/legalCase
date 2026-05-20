# SF-216-19 — Indignité successorale FR — backend

## Objectif

Outil décisionnel `F-FA-INDIGNITE-SUCCESSORALE` : détermine si un héritier peut être déclaré indigne de succéder (art. 726-727 Cciv) — indignité de plein droit (art. 726) vs indignité judiciaire (art. 727) — et évalue l'effet sur la dévolution successorale.

## Comportement nominal

- Endpoint `POST/GET /api/v1/case-files/{caseFileId}/indignite-successorale`.
- Body :
  - `motifIndignite` (MEURTRE_TENTE | MEURTRE | TEMOIGNAGE_FAUX | OBSTRUCTION_TESTAMENT | NON_SIGNALEMENT_HOMICIDE | VIOLENCES_GRAVES | AUTRE)
  - `condamnationPrononcee` (boolean) — condamnation pénale définitive
  - `indigniteJudiciaireDemandee` (boolean) — les autres héritiers demandent l'exclusion
  - `pardonTestamentaireDetecte` (boolean, optionnel) — le défunt avait-il explicitement pardonné dans son testament ?
  - `dateOuvertureSuccession` (LocalDate, requis)
  - `nbCoheritiersRestants` (int, optionnel) — après exclusion de l'indigne
- Calculator :
  - **Indignité de plein droit** (art. 726) : condamnation pour meurtre/tentative de meurtre + pas de pardon testamentaire → héritier indigne ipso jure.
  - **Indignité judiciaire** (art. 727) : autres cas (témoignage faux, obstruction testament, violences graves) → jugement du TJ requis.
  - **Pardon testamentaire** : si détecté → neutralise l'indignité de plein droit.
  - **Représentation** : les descendants de l'indigne peuvent-ils représenter leur parent ? (art. 729-1 — oui en cas d'indignité judiciaire, non si de plein droit).
  - **Délai d'action** : 5 ans depuis la succession ou depuis la condamnation (art. 729).
- Retourne : `typeIndignite`, `verdictIndignite`, `pardonNeutralisant`, `representationPossible`, `delaiAction`, `effetDévolution`, `baseLegale`, `messages`, `alertes`.
- Persiste 1:1 par dossier.

## Cas d'erreur

- `country ≠ FRANCE` → 400.
- `dateOuvertureSuccession` future → 400.

## Source juridique

- **art. 726 Cciv** — indignité de plein droit.
- **art. 727 Cciv** — indignité judiciaire (liste élargie loi 3/12/2001 + loi 5/12/2022).
- **art. 729 Cciv** — délai 5 ans pour demander l'indignité judiciaire.
- **art. 729-1 Cciv** — représentation des descendants de l'indigne.
- **Loi n°2022-1617 du 23/12/2022** — extension causes d'indignité (violences intrafamiliales).

## Champs IA à extraire (FamilleExtractedData)

**Réutilisés (F-246)** :
- `succession_detection_v2.dateOuvertureSuccessionDetectee`
- `succession_detection_v2.nombreCoheritiersDetecte`
- `succession_detection_v2.conjointSurvivantDetected`
- `succession_detection_v2.nbDescendantsDetecte`

**Nouveaux champs à ajouter** :
- `indigniteSuccessoraleEnvisagee` (boolean | null) — détecté si mention « indignité successorale », « art. 726 », « condamné meurtre + succession ».
- `condamnationPenaleSuccessionDetectee` (boolean | null) — condamnation pénale en lien avec le défunt détectée.
- `pardonTestamentaireDetecte` (boolean | null) — pardon explicite dans le testament détecté.

## Plan de test

- UT calculator : (a) meurtre + condamné + sans pardon → indignité plein droit, représentation impossible ; (b) meurtre + pardon testamentaire → pas d'indignité ; (c) témoignage faux → indignité judiciaire, délai 5 ans ; (d) violences loi 2022 → indignité judiciaire élargie.
- UT service : gates.
- IT : POST + GET.

## Composants impactés

- Migration Liquibase 289 : table `indignite_successorale_analyses`.
- Migration Liquibase 290 : INSERT `decision_tool_visibility_rules` CONTEXTUAL `indigniteSuccessoraleEnvisagee`, `DROIT_FAMILLE`, `FRANCE`, priority 107.
- Java : `IndigniteSuccessoraleCalculator`, result, analysis, repository, service, controller, `MotifIndigniteEnum`.
- `CaseAnalysisResponse.java` — ajout `indigniteSuccessoraleEnvisagee`, `condamnationPenaleSuccessionDetectee`, `pardonTestamentaireDetecte`.
- `LegalDomainPromptBuilder`.

## Critères d'acceptation

- AC1 : meurtre sans pardon → indignité plein droit.
- AC2 : pardon testamentaire → neutralisation.
- AC3 : loi 2022 violences → indignité judiciaire.
- AC4 : `country=BELGIQUE` → 400.

## Hors périmètre

- Frontend (SF-216-20).
- Recel de succession (SF-216-21/22 — situation distincte).
