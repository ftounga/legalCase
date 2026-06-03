# Mini-spec — F-221 / SF-221-06 — Outil titre de séjour victime de traite des êtres humains (BE)

## Identifiant
`F-221 / SF-221-06` — tool_id `F-IM-52-victime-traite-be` (Immigration BE) — slug `victime-traite-be` — statut `ready` — 2026-06-03
## Branche Git
`feat/SF-221-06-victime-traite-be`

## Objectif (1 phrase)
Évaluer l'éligibilité au titre de séjour spécifique « victime de traite des êtres humains » (coopération judiciaire + rupture avec le réseau + accompagnement par un centre spécialisé) et situer l'étape de la procédure.

## Périmètre / anti-doublon
Titre de séjour **victime de traite** BE (art. 61/2 et s. loi 15/12/1980 ; circulaire 26/09/2008 ; partenariat PAG-ASA / Sürya / Payoke). **Distinct** du pendant FR `F-IM-35-victime-traite-l4251-fr` (régime FR, base juridique propre) : confirme la pertinence métier mais le **régime BE est propre** (procédure en 3 phases : délai de réflexion → titre temporaire → titre lié à la procédure pénale). Distinct des procédures humanitaires 9bis/9ter (`F-IM-14`) et du regroupement.

## Comportement (branches de verdict, branche `default`)
Entrées : `phaseProcedure` (enum: REFLEXION_45J / DECLARATION_FAITE / PROCEDURE_PENALE_EN_COURS / AUCUNE, requis), `ruptureAvecReseau` (bool), `cooperationJudiciaire` (bool), `accompagnementCentreSpecialise` (bool), `dateDebutAccompagnement` (date, nullable).
Logique (3 phases indicatives circulaire 26/09/2008 — **à vérifier par avocat**) :
- **DELAI_REFLEXION** si `phaseProcedure=REFLEXION_45J` : période de réflexion (~45 j) avant déclaration ; rappelle l'accompagnement obligatoire par un centre agréé.
- **ELIGIBLE_TITRE_TEMPORAIRE** si `ruptureAvecReseau=true` ET `accompagnementCentreSpecialise=true` ET `phaseProcedure` ∈ {DECLARATION_FAITE, PROCEDURE_PENALE_EN_COURS}.
- **ELIGIBLE_SOUS_PROCEDURE_PENALE** si `cooperationJudiciaire=true` ET `phaseProcedure=PROCEDURE_PENALE_EN_COURS` (titre lié à l'utilité de la déclaration).
- **CONDITIONS_NON_REUNIES** si `ruptureAvecReseau=false` OU `accompagnementCentreSpecialise=false`.
- **A_ORIENTER_CENTRE** (default) si `phaseProcedure=AUCUNE` → orienter vers un centre spécialisé (PAG-ASA / Sürya / Payoke) avant toute démarche.
Verdict enum : `DELAI_REFLEXION` / `ELIGIBLE_TITRE_TEMPORAIRE` / `ELIGIBLE_SOUS_PROCEDURE_PENALE` / `CONDITIONS_NON_REUNIES` / `A_ORIENTER_CENTRE`, + étape de procédure + bases juridiques annotées « (à vérifier par avocat) ».

## Cas d'erreur
| Situation | Comportement | Code |
|---|---|---|
| workspace.country ≠ BELGIQUE | 400 | 400 |
| legalDomain ≠ DROIT_IMMIGRATION | 400 | 400 |
| `phaseProcedure` absente / hors whitelist | 400 (validation) | 400 |
| caseFile hors workspace courant | 404 (isolation) | 404 |
| Aucun champ rempli | bouton calcul désactivé (front) | — |

## Contrat API (figé pour parallélisation back/front)
`POST /api/v1/case-files/{caseFileId}/victime-traite-be-analysis`
- Request : `{ phaseProcedure:"REFLEXION_45J"|"DECLARATION_FAITE"|"PROCEDURE_PENALE_EN_COURS"|"AUCUNE", ruptureAvecReseau:bool, cooperationJudiciaire:bool, accompagnementCentreSpecialise:bool, dateDebutAccompagnement:date|null }`
- Response : `{ verdict:string, etapeProcedure:string, basesJuridiques:string[], messages:string[] }`
- `GET` même chemin = dernière analyse. 200 OK ; isolation workspace.

## Champs IA (`ImmigrationExtractedData`) + flag pivot
| Champ outil | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|
| `phaseProcedure` | `victimeTraitePhase` | Nouveau — whitelist 4 valeurs |
| `ruptureAvecReseau` | `victimeTraiteRupture` | Nouveau |
| `accompagnementCentreSpecialise` | `victimeTraiteAccompagnement` | Nouveau |
| **flag pivot** | `victimeTraiteDetecte` (boolean, niveau 2 BE-only, `false` par défaut) | Nouveau |
`cooperationJudiciaire` / `dateDebutAccompagnement` = pré-fill si factualisable. Pré-fill IA F-246.

## Critères d'acceptation
- [ ] Les 5 verdicts couverts + `etapeProcedure`.
- [ ] Anti-doublon documenté vs `F-IM-35-victime-traite-l4251-fr` (régime BE propre, 3 phases).
- [ ] POST workspace FR → 400 ; legalDomain ≠ DROIT_IMMIGRATION → 400.
- [ ] Isolation workspace testée.
- [ ] Pré-fill IA des champs factualisables (F-246).
- [ ] Flag pivot `false` par défaut ; visibility CONTEXTUAL.
- [ ] `F-IM-52-victime-traite-be` dans `TOOL_REGISTRY`, `KNOWN_FRONTEND_TOOL_IDS`, `KNOWN_NO_DASHBOARD_TILE_IDS`.

## Plan de test
UT calculator (5 verdicts + 3 phases + conditions rupture/accompagnement/coopération), IT endpoint (200 + gates 400 FR/domaine + 400 whitelist + 404 isolation), Jest composant (form pré-rempli + verdict + bouton désactivé si vide + flush jurisprudence-citations).

## Tables / endpoints / composants
- Backend : migration `victime_traite_be_analyses` (à pré-assigner) + entité + repo + `VictimeTraiteBeCalculator` + service + controller.
- Frontend : `victime-traite-be-section.component` (+ .html/.scss/.spec + prefill-rules) + `TOOL_REGISTRY` `F-IM-52-victime-traite-be` + `decision_tool_visibility_rules` (CONTEXTUAL, trigger `victimeTraiteDetecte=true`) + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- Champs IA : étendre `ImmigrationExtractedData` (3 champs + flag pivot) + prompt `LegalDomainPromptBuilder` Immigration BE.

## Invariants
- CONTEXTUAL (jamais ALWAYS_ON) — flag pivot `victimeTraiteDetecte` niveau 2 BE-only `false` par défaut.
- Pré-fill IA F-246 ; visibility + KNOWN_NO_DASHBOARD ; 1 outil = 1 situation ; BE-only (gate country=BELGIQUE).

## Hors périmètre
Régime FR victime de traite (`F-IM-35`), procédures humanitaires 9bis/9ter (`F-IM-14`), pénal (procédure devant le juge répressif), génération de la demande de titre.
