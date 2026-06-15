# Bugfix — Clarté d'affichage des délais (section F-69 / Suivi)

> Bugfix issu d'un test utilisateur (2026-06-16). Exempt étape 0/0bis (correction d'affichage, aucun nouvel écran).

## Constat

Le testeur voit ~27 délais sur la Vue d'ensemble (le **fil** liste toutes les échéances) mais, en dépliant **« DÉLAIS LÉGAUX »** sur l'onglet Suivi, il ne « retrouve pas les 27 ». Causes :
1. **Titre trompeur** : la section s'appelait **« DÉLAIS LÉGAUX »** alors qu'elle regroupe TOUS les délais (légaux + manuels + IA + contradictoire). Le sous-groupe « Délais légaux applicables » ne contient que les STATUTORY (1-2).
2. **Pas de comptage par groupe** : impossible de voir d'un coup d'œil la répartition (combien de légaux / confirmés / suggérés / contradictoire) → l'avocat ne peut pas juger lui-même s'il y a du bruit.
3. Le chip replié comptait `case_deadlines` seul (sans les échéances contradictoires).

## Choix de conception (important — sécurité métier)

**On ne masque ni ne supprime aucun délai.** En droit, cacher une échéance = risque de forclusion. Le fix porte sur la **lisibilité/organisation**, pas sur le filtrage : l'avocat doit pouvoir TOUT voir et juger. (La réduction éventuelle du bruit à la SOURCE — délais IA non applicables — relève d'une décision produit séparée, informée par les données réelles que ce fix rend visibles.)

## Correctif

- **Renommage** : `TOOL_LABEL` + titre de section **« DÉLAIS LÉGAUX » → « DÉLAIS DU DOSSIER »** (carte du panneau + en-tête de section).
- **Comptage par groupe** dans les en-têtes : « Délais légaux applicables (N) », « Délais confirmés (N) » (nouvel en-tête), « Propositions suggérées (N) », « Échéances liées aux échanges (N) ».
- **Chip total** : `totalDeadlinesCount` = `case_deadlines` + échéances contradictoires (toutes sources).

## Critères d'acceptation

- [ ] La section/carte s'intitule **« DÉLAIS DU DOSSIER »**.
- [ ] Chaque groupe affiche son compteur ; le chip replié = total toutes sources.
- [ ] Aucun délai n'est masqué ni supprimé (tous les groupes restent rendus).

## Tests

- `case-deadlines-section.component.spec.ts` : `totalDeadlinesCount` agrège case_deadlines + contradictoire ; chip inchangé pour les cas existants.
- `decision-tool-instrumentation-travail.spec.ts` : libellé attendu mis à jour.

## Composants impactés

- `case-deadlines-section.component.{ts,html}` (titre, comptages, `totalDeadlinesCount`).
- `decision-tool-instrumentation-travail.spec.ts` (libellé).

**Aucun** : backend, migration, endpoint.

## Hors périmètre (suivi possible, décision produit)

- **Réduction du bruit à la source** (filtrer les délais IA non applicables au dossier précis, dédupliquer STATUTORY↔AI) — nécessite un flag `applicable` dans l'analyse + arbitrage. À cadrer une fois la répartition réelle observée via ce fix.
- **Cap/regroupement du fil** de la Vue d'ensemble — à évaluer sans jamais masquer une échéance.

## Analyse transversale

- **Outil décisionnel** : F-237 (wrapper délais) — affichage seul, aucun calcul. Self-check : spec section + instrumentation.
- **Auth/workspace/navigation** : aucun. **Smoke E2E** : N/A.
