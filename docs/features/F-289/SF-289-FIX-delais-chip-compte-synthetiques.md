# Bugfix — Le chip « DÉLAIS DU DOSSIER » comptait les non-délais

> Bugfix issu d'un test (captures `delai-1/2/3`, dossier Dupont-4). Exempt étape 0/0bis.

## Constat (captures)

Section **« DÉLAIS DU DOSSIER »** (F-69, onglet Suivi) : chip replié = **« 30 délais »**, mais une fois dépliée seulement **6** sont affichés (1 légal + 5 confirmés). L'échéancier (carte du haut) est, lui, propre (« +2 » = 6 vrais).

## Cause

`CaseDeadlineService.list()` renvoie **toutes** les lignes `case_deadlines`, y compris les ~24 lignes synthétiques injectées par F-194 (pièces à demander), F-193 (checks), F-192 (pistes). Le front F-69 ne **rend** que les sources MANUAL/STATUTORY/AI (groupes), mais le **chip** comptait `deadlines().length` = tout → 30. Le fix échéancier (PR #1694) avait nettoyé `EcheancierService` mais **pas** `CaseDeadlineService.list()`.

## Correctif

`CaseDeadlineService.list()` exclut les sources `PIECE_A_DEMANDER` / `PROCEDURE_CHECK_TO_CHECK` / `PISTE_RETENUE` (même ensemble que `EcheancierService`). → chip et contenu cohérents (6 = 6). Aucune donnée supprimée en base ; ces items restent dans leurs panneaux.

## Critères d'acceptation

- [ ] `GET /deadlines` ne renvoie pas les délais de source PIECE_A_DEMANDER / PROCEDURE_CHECK_TO_CHECK / PISTE_RETENUE.
- [ ] Les délais MANUAL/STATUTORY/AI restent renvoyés ; le chip = nombre réellement affiché.

## Tests
`CaseDeadlineControllerIT.list_excludesSyntheticSources_pieceCheckPiste` + 16 existants = 17 verts.

## Composants
- `CaseDeadlineService.java` (`NON_DEADLINE_SOURCES` + filtre `list`).

**Aucun** : migration, endpoint, frontend.

## Note
La liste affichait aussi quelques **doublons de vrais délais** (AI : « contestation » ≈ « prescription pour contester », « rappel de salaire » ×2) — dédup des délais AI = sujet distinct (suivi).
