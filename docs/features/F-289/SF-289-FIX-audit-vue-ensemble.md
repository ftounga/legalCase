# Bugfix — Audit Vue d'ensemble (F-289) & états visuels outils (F-292)

> **Nature** : bugfix coordonné issu d'un test utilisateur (2026-06-15). Exempt étape 0/0bis (corrections de comportement, aucun nouvel élément). Branche `fix/SF-289-292-audit-corrections`.

## Contexte

Test de la passe « nouvelles features » → 4 bugs réels constatés sur la Vue d'ensemble (F-289), le panneau décisionnel (F-292) et l'échéancier. Audit code (2 agents Explore) → causes racines confirmées.

## Correctifs

### Fix 1 — F-292 : l'état visuel des outils n'était pas réactif (le plus important)
- **Cause** : le panneau F-IA-04 (`decisional-tools-panel`) **ne passe jamais `[summary]`** à `decision-tool-card` (ses cartes sont des lanceurs). Or F-292 détectait « calculé » via `summary` → toujours `null` → la carte restait **bloquée en pointillé** même après calcul.
- **Fix** : nouvel input `[calculated]` sur la carte ; le panneau suit l'ensemble des **outils calculés** via les tuiles du dashboard (`CaseDashboardService.get`), rechargé sur `refresh$` (déjà déclenché par chaque calcul d'outil). `isCalculated = calculated === true || summary présent` (le dashboard garde le chemin `summary`). Bascule **pointillé → plein dynamique**, sans rechargement manuel.
- **Fichiers** : `decision-tool-card.component.ts`, `decisional-tools-panel.component.{ts,html}`.

### Fix 2 — F-289 : les questions IA inline disparaissaient du bloc « attention »
- **Cause** : `OverviewService.buildAttention` plafonne à 5 + trie par urgence ; une rafale d'échéances OVERDUE/CRITICAL + pièces (rang SOON, avant les questions) **remplissait les 5 slots** → la question IA (SOON, après les pièces) était **éjectée**.
- **Fix** : **sélection équilibrée** avant troncature — au moins le 1ᵉʳ item de chaque **type présent** (échéance / pièce / question / analyse obsolète) figure en tête (≤ 4 types ≤ cap), en préservant l'ordre d'urgence, puis complétion par urgence. La question IA n'est plus éjectée.
- **Fichiers** : `OverviewService.java`.

### Fix 3 — F-289 : libellé « Marquer fait » trompeur
- **Cause** : « Marquer fait » = item ÉCHÉANCE (`VALIDATE_DEADLINE`) → **route** vers l'onglet Suivi (aucune action inline) ; « Marquer obtenue » = PIÈCE → inline. Le libellé laissait croire à une action immédiate.
- **Fix** : `VALIDATE_DEADLINE` → libellé **« Voir le délai »** + icône `event` (honnête sur la navigation).
- **Fichiers** : `case-overview.component.ts`.

### Fix 4 — Échéancier : « voir tous les délais » n'affichait pas tout
- **Cause** : la carte Échéancier (F-284) agrège `case_deadlines` **+ échéances de réponse des rounds** (CONTRADICTOIRE) ; la liste détaillée (F-69) ne lisait que `case_deadlines` → l'avocat avait l'impression qu'« il en manque ».
- **Fix** : la section F-69 affiche désormais un bloc **lecture seule « Échéances liées aux échanges (contradictoire) »**, alimenté par l'échéancier (`EcheancierService`, filtre `kind === 'CONTRADICTOIRE'`), avec un hint « se gèrent depuis la frise Cycle contradictoire ».
- **Fichiers** : `case-deadlines-section.component.{ts,html}`.

## Clarification produit (réponse au testeur)
- **« Marquer obtenue »** = pièce manquante → marquée **sur place** (PUT statut OBTENUE + reload).
- **« Voir le délai »** (ex-« Marquer fait ») = échéance → **route** vers l'onglet Suivi (la validation se fait là-bas, pas d'action inline). Deux gestes distincts, désormais nommés sans ambiguïté.

## Tests
- **Backend** : `OverviewServiceTest` 11 verts, dont `balancedSelection_questionIaNotStarvedByOverdueDeadlines`.
- **Frontend** : `decision-tool-card` (+ 2 tests `calculated`), `decisional-tools-panel`, `case-deadlines-section`, `case-overview` — **167 verts**. Build prod OK.

## Hors périmètre
- Refonte du tri/priorité du bloc attention (au-delà de la garantie de représentation).
- Rendre les échéances de rounds éditables dans F-69 (elles restent gérées dans la frise contradictoire).
- Marquage inline d'une échéance (reste un routage vers Suivi).

## Analyse transversale
- **Composant décisionnel partagé** (`decision-tool-card`) : input optionnel ajouté, rétro-compatible (dashboard inchangé via `summary`). Self-check : specs carte + panel + dashboard verts.
- **Lecture seule** : aucune mutation d'outil, aucun calcul déclenché (invariant « 1 outil = 1 situation » intact). Aucune migration, aucun nouvel endpoint (réutilise `/dashboard` et `/echeancier` existants).
- **Smoke E2E** : aucun (pas d'impact auth/workspace/navigation applicative).
