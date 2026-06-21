# SF-283-05 — Phase « Clôture de l'instruction » (catalogue travail FR)

> Petit ajout de référentiel (suggestion de phase), issu du test 2026-06-22. Bugfix/référentiel — étapes 0/0bis exemptées.

## Objectif (une phrase)
Proposer la phase **« Clôture de l'instruction »** dans la frise des phases (F-283) pour un dossier **droit du travail / France**, entre « Mise en état » et le bureau de jugement.

## Justification métier
Depuis la réforme de 2016, la procédure devant le Conseil de prud'hommes peut être **écrite** avec une **mise en état** clôturée par une **ordonnance de clôture** avant l'audience de jugement. La phase était absente du catalogue (8 phases). C'est une **suggestion éditable** : l'avocat reste libre du libellé et du type.

## Comportement nominal
- Nouveau type d'enum `CasePhaseType.CLOTURE` (« CLOTURE » = 7 car. < varchar(30) → **0 migration**, rétro-compat).
- `CasePhaseSuggestionCatalog.TRAVAIL_FRANCE` insère `CLOTURE` (« Clôture de l'instruction ») entre `MISE_EN_ETAT` et `FOND` → 9 phases.
- Modèle frontend : `CasePhaseType` union + `CASE_PHASE_LABELS` + fallback `CASE_PHASE_OPTIONS` étendus.

## Cas d'erreur
- Anciennes phases persistées : inchangées (ajout additif). Le `displayLabel` générique couvre CLOTURE.

## Critères d'acceptation
- **CA1** : suggestions travail FR → 9 phases, CLOTURE en 4ᵉ position (après Mise en état).
- **CA2** : le sélecteur frontend propose « Clôture de l'instruction » (libellé éditable).
- **CA3** : autres domaines/pays inchangés (CLOTURE seulement dans travail FR + fallback civil FR).

## Plan de test
- `CasePhaseSuggestionCatalogTest` : ordre travail FR avec CLOTURE ; fallback 9 phases.
- `CasePhaseControllerIT` : `/phases/suggestions` travail FR = 9, CLOTURE en index 3.
- Frontend `case-phases-timeline.component.spec` : `phaseOptions().length === 9`.

## Tables / endpoints / composants
- Enum `CasePhaseType`, catalogue `CasePhaseSuggestionCatalog`, modèle `case-phase.model.ts`. **0 migration** (varchar(30) suffit).

## Hors périmètre
- Ajout de CLOTURE aux catalogues famille FR / immigration / BE (non demandé ; ajout ciblé travail FR).
