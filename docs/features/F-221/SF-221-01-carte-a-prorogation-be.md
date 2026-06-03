# Mini-spec — F-221 / SF-221-01 — Outil prorogation carte A (séjour temporaire BE)

## Identifiant
`F-221 / SF-221-01` — tool_id `F-IM-47-carte-a-prorogation-be` (Immigration BE) — slug `carte-a-prorogation-be` — statut `ready` — 2026-06-03
## Branche Git
`feat/SF-221-01-carte-a-prorogation-be`

## Objectif (1 phrase)
Calculer le délai de dépôt (30-45 j avant expiration) et vérifier les conditions de prorogation de la carte A (séjour temporaire), instruite par la commune.

## Périmètre / anti-doublon
Prorogation de la **carte A** (séjour temporaire / limité). **Distinct** de `F-IM-48-carte-b-sejour-illimite-be` (passage carte A → séjour illimité après 5 ans) et de la délivrance initiale (`F-IM-05` arbre titre / `F-IM-01` pièces). Le renouvellement single permit (`F-IM-25`) est une autre situation (travail). Cet outil ne traite que le **maintien temporaire** du même motif de séjour.

## Comportement (branches de verdict, branche `default`)
Entrées : `dateExpirationCarteA` (date, requis), `motifSejourPersiste` (bool), `conditionsInitialesToujoursReunies` (bool), `demandeDeposee` (bool), `dateDemande` (date, nullable).
Logique (jours calendaires ; fenêtre 30-45 j avant expiration — art. 13 loi 15/12/1980 + art. 33 AR 08/10/1981, **à vérifier par avocat**) :
- `dateOuvertureFenetre` = `dateExpirationCarteA` − 45 j ; `dateLimite` = `dateExpirationCarteA` − 30 j (recommandée) ; échéance dure = `dateExpirationCarteA`.
- `joursAvantExpiration` = `dateExpirationCarteA` − today.
- **PROROGEABLE** si `motifSejourPersiste=true` ET `conditionsInitialesToujoursReunies=true` ET dans la fenêtre.
- **A_DEPOSER_URGENT** si la fenêtre est ouverte / dépassée mais carte non encore expirée et `demandeDeposee=false`.
- **CONDITIONS_NON_REUNIES** si `motifSejourPersiste=false` OU `conditionsInitialesToujoursReunies=false`.
- **EXPIREE** si `joursAvantExpiration ≤ 0` et `demandeDeposee=false` (risque séjour irrégulier).
- **DEMANDE_DEPOSEE** si `demandeDeposee=true`.
Verdict enum : `PROROGEABLE` / `A_DEPOSER_URGENT` / `CONDITIONS_NON_REUNIES` / `EXPIREE` / `DEMANDE_DEPOSEE`, + `joursAvantExpiration` + fenêtre de dépôt + bases juridiques annotées « (à vérifier par avocat) ».

## Cas d'erreur
| Situation | Comportement | Code |
|---|---|---|
| workspace.country ≠ BELGIQUE | 400 Bad Request | 400 |
| legalDomain ≠ DROIT_IMMIGRATION | 400 Bad Request | 400 |
| `dateExpirationCarteA` absente | 400 (validation) | 400 |
| `demandeDeposee=true` sans `dateDemande` | 400 (validation) | 400 |
| caseFile hors workspace courant | 404 (isolation) | 404 |
| Aucun champ rempli | bouton calcul désactivé (front) | — |

## Contrat API (figé pour parallélisation back/front)
`POST /api/v1/case-files/{caseFileId}/carte-a-prorogation-be-analysis`
- Request : `{ dateExpirationCarteA:date, motifSejourPersiste:bool, conditionsInitialesToujoursReunies:bool, demandeDeposee:bool, dateDemande:date|null }`
- Response : `{ verdict:string, joursAvantExpiration:int, dateOuvertureFenetre:date, dateLimiteRecommandee:date, basesJuridiques:string[], messages:string[] }`
- `GET` même chemin = dernière analyse (pattern `getAnalysis`). 200 OK ; isolation workspace.

## Champs IA (`ImmigrationExtractedData`) + flag pivot
| Champ outil | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|
| `dateExpirationCarteA` | `carteAProrogationDateExpiration` | Nouveau |
| `motifSejourPersiste` | `carteAProrogationMotifPersiste` | Nouveau |
| `conditionsInitialesToujoursReunies` | `carteAProrogationConditionsReunies` | Nouveau |
| **flag pivot** | `carteAProrogationDetecte` (boolean, niveau 2 BE-only, `false` par défaut jamais `null`) | Nouveau |
`demandeDeposee` / `dateDemande` = aspirationnels (actions procédurales non extractibles). Pré-fill IA F-246 sur tous les champs factualisables.

## Critères d'acceptation
- [ ] Les 5 verdicts couverts + `joursAvantExpiration` + fenêtre 30-45 j calculée.
- [ ] POST workspace FR → 400 ; legalDomain ≠ DROIT_IMMIGRATION → 400.
- [ ] Isolation workspace testée (404 cross-workspace).
- [ ] Tous les champs factualisables pré-remplis par l'IA (F-246).
- [ ] Flag pivot `carteAProrogationDetecte` `false` par défaut ; visibility CONTEXTUAL (jamais ALWAYS_ON).
- [ ] `F-IM-47-carte-a-prorogation-be` dans `TOOL_REGISTRY`, `KNOWN_FRONTEND_TOOL_IDS`, `KNOWN_NO_DASHBOARD_TILE_IDS`.

## Plan de test
UT calculator (5 verdicts + fenêtre 30/45 + expirée), IT endpoint (200 + 400 gate FR + 400 gate domaine + 400 validation + 404 isolation), Jest composant (form pré-rempli + verdict + bouton désactivé si vide + flush jurisprudence-citations).

## Tables / endpoints / composants
- Backend : migration `carte_a_prorogation_be_analyses` (à pré-assigner) + entité + repo + `CarteAProrogationBeCalculator` + service + controller.
- Frontend : `carte-a-prorogation-be-section.component` (+ .html/.scss/.spec + prefill-rules) + `TOOL_REGISTRY` `F-IM-47-carte-a-prorogation-be` + `decision_tool_visibility_rules` (CONTEXTUAL, trigger `carteAProrogationDetecte=true`) + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- Champs IA : étendre `ImmigrationExtractedData` (3 champs + flag pivot) + prompt `LegalDomainPromptBuilder` Immigration BE.

## Invariants
- CONTEXTUAL (jamais ALWAYS_ON) — flag pivot `carteAProrogationDetecte` niveau 2 BE-only `false` par défaut.
- Pré-fill IA F-246 ; visibility + KNOWN_NO_DASHBOARD ; 1 outil = 1 situation ; BE-only (gate country=BELGIQUE).

## Hors périmètre
Passage carte A → carte B séjour illimité (`F-IM-48`), délivrance initiale du titre, renouvellement single permit (`F-IM-25`), génération du document de demande.
