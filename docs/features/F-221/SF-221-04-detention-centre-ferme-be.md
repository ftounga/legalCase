# Mini-spec — F-221 / SF-221-04 — Outil détention en centre fermé + requête mise en liberté (BE)

## Identifiant
`F-221 / SF-221-04` — tool_id `F-IM-50-detention-centre-ferme-be` (Immigration BE) — slug `detention-centre-ferme-be` — statut `ready` — 2026-06-03
## Branche Git
`feat/SF-221-04-detention-centre-ferme-be`

## Objectif (1 phrase)
Calculer la durée / la prolongation de la détention administrative en centre fermé et cadrer la requête de mise en liberté devant la chambre du conseil (délai et base juridique).

## Périmètre / anti-doublon
**Une seule situation = détention + son recours** (les deux entrées audit `detention-centre-ferme-be` + `detention-recours-chambre-conseil-be` sont **fusionnées** ici, conformément à l'invariant étape 0). Couvre le maintien en centre fermé (art. 7 al. 3, 27, 29, 74/5 loi 15/12/1980 ; AR 02/08/2002) **et** la requête de mise en liberté devant la **chambre du conseil** (art. 71+ ; délai indicatif 5 j). **Distinct** des recours CCE (`F-IM-31` annulation 30j, `F-IM-32` extrême urgence 5j, `F-IM-51` suspension) : la chambre du conseil est une **juridiction judiciaire** statuant sur la légalité de la détention, pas le CCE. Distinct de l'OQT (`F-IM-08`) et de l'IE (`F-IM-33`).

## Comportement (branches de verdict, branche `default`)
Entrées : `dateDebutDetention` (date, requis), `baseLegaleDetention` (enum: ART_7 / ART_27 / ART_29 / ART_74_5 / AUTRE, requis), `prolongationNotifiee` (bool), `dateProlongation` (date, nullable), `requeteMiseEnLiberteDeposee` (bool), `dateNotificationDecisionDetention` (date, nullable).
Logique (jours calendaires ; durées max et prolongations indicatives AR 02/08/2002 — **à vérifier par avocat**) :
- `dureeDetentionJours` = today − `dateDebutDetention`.
- `dateLimiteRequete` = `dateNotificationDecisionDetention` + 5 j (requête chambre du conseil — indicatif, **à vérifier**).
- **DETENTION_EN_COURS** (default) : affiche durée + base + rappel droit de requête.
- **REQUETE_OUVERTE** si `dateNotificationDecisionDetention` connue et `joursDepuisNotification ≤ 5` et `requeteMiseEnLiberteDeposee=false` → `joursRestantsRequete`.
- **REQUETE_TARDIVE** si `joursDepuisNotification > 5` et `requeteMiseEnLiberteDeposee=false`.
- **REQUETE_DEPOSEE** si `requeteMiseEnLiberteDeposee=true`.
- **PROLONGATION_A_CONTESTER** si `prolongationNotifiee=true` → recalcule la fenêtre de requête depuis `dateProlongation`.
Verdict enum : `DETENTION_EN_COURS` / `REQUETE_OUVERTE` / `REQUETE_TARDIVE` / `REQUETE_DEPOSEE` / `PROLONGATION_A_CONTESTER`, + `dureeDetentionJours` + `dateLimiteRequete` + `joursRestantsRequete` + bases juridiques annotées « (à vérifier par avocat) ».

## Cas d'erreur
| Situation | Comportement | Code |
|---|---|---|
| workspace.country ≠ BELGIQUE | 400 | 400 |
| legalDomain ≠ DROIT_IMMIGRATION | 400 | 400 |
| `dateDebutDetention` absente ou future | 400 (validation) | 400 |
| `prolongationNotifiee=true` sans `dateProlongation` | 400 (validation) | 400 |
| `requeteMiseEnLiberteDeposee=true` sans `dateNotificationDecisionDetention` | 400 (validation) | 400 |
| caseFile hors workspace courant | 404 (isolation) | 404 |
| Aucun champ rempli | bouton calcul désactivé (front) | — |

## Contrat API (figé pour parallélisation back/front)
`POST /api/v1/case-files/{caseFileId}/detention-centre-ferme-be-analysis`
- Request : `{ dateDebutDetention:date, baseLegaleDetention:"ART_7"|"ART_27"|"ART_29"|"ART_74_5"|"AUTRE", prolongationNotifiee:bool, dateProlongation:date|null, requeteMiseEnLiberteDeposee:bool, dateNotificationDecisionDetention:date|null }`
- Response : `{ verdict:string, dureeDetentionJours:int, dateLimiteRequete:date|null, joursRestantsRequete:int|null, basesJuridiques:string[], messages:string[] }`
- `GET` même chemin = dernière analyse. 200 OK ; isolation workspace.

## Champs IA (`ImmigrationExtractedData`) + flag pivot
| Champ outil | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|
| `dateDebutDetention` | `detentionDateDebut` | Nouveau |
| `baseLegaleDetention` | `detentionBaseLegale` | Nouveau — whitelist 5 valeurs |
| `dateNotificationDecisionDetention` | `detentionDateNotification` | Nouveau |
| **flag pivot** | `detentionCentreFermeDetecte` (boolean, niveau 2 BE-only, `false` par défaut) | Nouveau |
`prolongationNotifiee` / `dateProlongation` / `requeteMiseEnLiberteDeposee` = aspirationnels (actions procédurales). Pré-fill IA F-246.

## Critères d'acceptation
- [ ] Les 5 verdicts couverts + `dureeDetentionJours` + fenêtre requête 5 j.
- [ ] Fusion détention + requête mise en liberté = **un seul outil** (pas deux).
- [ ] Anti-doublon CCE (`F-IM-31/32/51`) documenté : juridiction = chambre du conseil ≠ CCE.
- [ ] POST workspace FR → 400 ; legalDomain ≠ DROIT_IMMIGRATION → 400.
- [ ] Isolation workspace testée.
- [ ] Pré-fill IA des champs factualisables (F-246).
- [ ] Flag pivot `false` par défaut ; visibility CONTEXTUAL.
- [ ] `F-IM-50-detention-centre-ferme-be` dans `TOOL_REGISTRY`, `KNOWN_FRONTEND_TOOL_IDS`, `KNOWN_NO_DASHBOARD_TILE_IDS`.

## Plan de test
UT calculator (5 verdicts + durée + fenêtre 5 j + requête tardive + prolongation), IT endpoint (200 + gates 400 FR/domaine + 400 validations conditionnelles + 404 isolation), Jest composant (form pré-rempli + verdict + bouton désactivé si vide + flush jurisprudence-citations).

## Tables / endpoints / composants
- Backend : migration `detention_centre_ferme_be_analyses` (à pré-assigner) + entité + repo + `DetentionCentreFermeBeCalculator` + service + controller.
- Frontend : `detention-centre-ferme-be-section.component` (+ .html/.scss/.spec + prefill-rules) + `TOOL_REGISTRY` `F-IM-50-detention-centre-ferme-be` + `decision_tool_visibility_rules` (CONTEXTUAL, trigger `detentionCentreFermeDetecte=true`) + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- Champs IA : étendre `ImmigrationExtractedData` (3 champs + flag pivot) + prompt `LegalDomainPromptBuilder` Immigration BE.

## Invariants
- CONTEXTUAL (jamais ALWAYS_ON) — flag pivot `detentionCentreFermeDetecte` niveau 2 BE-only `false` par défaut.
- 1 outil = 1 situation : détention **et** son recours chambre du conseil = une seule situation fusionnée.
- Pré-fill IA F-246 ; visibility + KNOWN_NO_DASHBOARD ; BE-only (gate country=BELGIQUE).

## Hors périmètre
Recours CCE (`F-IM-31/32/51`), OQT (`F-IM-08`), interdiction d'entrée (`F-IM-33`), génération de la requête de mise en liberté.
