# Mini-spec — F-221 / SF-221-02 — Outil carte B (séjour illimité ressortissant tiers BE)

## Identifiant
`F-221 / SF-221-02` — tool_id `F-IM-48-carte-b-sejour-illimite-be` (Immigration BE) — slug `carte-b-sejour-illimite-be` — statut `ready` — 2026-06-03
## Branche Git
`feat/SF-221-02-carte-b-sejour-illimite-be`

## Objectif (1 phrase)
Évaluer l'éligibilité au passage carte A → carte B (séjour illimité) après 5 ans de séjour régulier ininterrompu et attache, pour un ressortissant tiers.

## Périmètre / anti-doublon
Carte B = **séjour illimité national BE** d'un ressortissant tiers (art. 14 loi 15/12/1980). **Distinct** de `F-IM-47-carte-a-prorogation-be` (maintien temporaire du même motif) et de `F-IM-49-residence-longue-duree-ue-be` (statut résident longue durée UE, directive 2003/109/CE, conditions propres ressources/assurance/intégration et portabilité UE). La carte B ne donne pas la mobilité intra-UE de la résidence longue durée — les deux outils ne chevauchent pas.

## Comportement (branches de verdict, branche `default`)
Entrées : `dateDebutSejourRegulier` (date, requis), `sejourIninterrompu` (bool), `absencesSuperieuresLimites` (bool), `motifSejourStable` (bool), `ordrePublicRisque` (bool).
Logique (5 ans = seuil indicatif art. 14, **à vérifier par avocat** — variantes selon motif) :
- `dureeSejourMois` calculée depuis `dateDebutSejourRegulier`.
- **ELIGIBLE** si `dureeSejourMois ≥ 60` ET `sejourIninterrompu=true` ET `absencesSuperieuresLimites=false` ET `motifSejourStable=true` ET `ordrePublicRisque=false`.
- **DUREE_INSUFFISANTE** si `dureeSejourMois < 60` → indiquer `moisRestants`.
- **CONTINUITE_ROMPUE** si `sejourIninterrompu=false` OU `absencesSuperieuresLimites=true`.
- **RISQUE_ORDRE_PUBLIC** si `ordrePublicRisque=true` (refus / examen renforcé).
- **A_EXAMINER** (default) si motif instable ou données partielles.
Verdict enum : `ELIGIBLE` / `DUREE_INSUFFISANTE` / `CONTINUITE_ROMPUE` / `RISQUE_ORDRE_PUBLIC` / `A_EXAMINER`, + `dureeSejourMois` + `moisRestants` + bases juridiques annotées « (à vérifier par avocat) ».

## Cas d'erreur
| Situation | Comportement | Code |
|---|---|---|
| workspace.country ≠ BELGIQUE | 400 | 400 |
| legalDomain ≠ DROIT_IMMIGRATION | 400 | 400 |
| `dateDebutSejourRegulier` absente ou future | 400 (validation) | 400 |
| caseFile hors workspace courant | 404 (isolation) | 404 |
| Aucun champ rempli | bouton calcul désactivé (front) | — |

## Contrat API (figé pour parallélisation back/front)
`POST /api/v1/case-files/{caseFileId}/carte-b-sejour-illimite-be-analysis`
- Request : `{ dateDebutSejourRegulier:date, sejourIninterrompu:bool, absencesSuperieuresLimites:bool, motifSejourStable:bool, ordrePublicRisque:bool }`
- Response : `{ verdict:string, dureeSejourMois:int, moisRestants:int, basesJuridiques:string[], messages:string[] }`
- `GET` même chemin = dernière analyse. 200 OK ; isolation workspace.

## Champs IA (`ImmigrationExtractedData`) + flag pivot
| Champ outil | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|
| `dateDebutSejourRegulier` | `carteBDateDebutSejour` | Nouveau |
| `sejourIninterrompu` | `carteBSejourIninterrompu` | Nouveau |
| `motifSejourStable` | `carteBMotifStable` | Nouveau |
| **flag pivot** | `carteBSejourIllimiteDetecte` (boolean, niveau 2 BE-only, `false` par défaut) | Nouveau |
`absencesSuperieuresLimites` / `ordrePublicRisque` = pré-fill si factualisable, sinon laissés à l'avocat. Pré-fill IA F-246.

## Critères d'acceptation
- [ ] Les 5 verdicts couverts + `dureeSejourMois` + `moisRestants`.
- [ ] Seuil 5 ans = 60 mois respecté ; `moisRestants` correct si insuffisant.
- [ ] POST workspace FR → 400 ; legalDomain ≠ DROIT_IMMIGRATION → 400.
- [ ] Anti-doublon `F-IM-49` documenté (pas de mobilité UE ici).
- [ ] Isolation workspace testée.
- [ ] Pré-fill IA des champs factualisables (F-246).
- [ ] Flag pivot `false` par défaut ; visibility CONTEXTUAL.
- [ ] `F-IM-48-carte-b-sejour-illimite-be` dans `TOOL_REGISTRY`, `KNOWN_FRONTEND_TOOL_IDS`, `KNOWN_NO_DASHBOARD_TILE_IDS`.

## Plan de test
UT calculator (5 verdicts + seuil 60 mois + continuité rompue + ordre public), IT endpoint (200 + gates 400 FR/domaine + 400 validation + 404 isolation), Jest composant (form pré-rempli + verdict + bouton désactivé si vide + flush jurisprudence-citations).

## Tables / endpoints / composants
- Backend : migration `carte_b_sejour_illimite_be_analyses` (à pré-assigner) + entité + repo + `CarteBSejourIllimiteBeCalculator` + service + controller.
- Frontend : `carte-b-sejour-illimite-be-section.component` (+ .html/.scss/.spec + prefill-rules) + `TOOL_REGISTRY` `F-IM-48-carte-b-sejour-illimite-be` + `decision_tool_visibility_rules` (CONTEXTUAL, trigger `carteBSejourIllimiteDetecte=true`) + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- Champs IA : étendre `ImmigrationExtractedData` (3 champs + flag pivot) + prompt `LegalDomainPromptBuilder` Immigration BE.

## Invariants
- CONTEXTUAL (jamais ALWAYS_ON) — flag pivot `carteBSejourIllimiteDetecte` niveau 2 BE-only `false` par défaut.
- Pré-fill IA F-246 ; visibility + KNOWN_NO_DASHBOARD ; 1 outil = 1 situation ; BE-only (gate country=BELGIQUE).

## Hors périmètre
Prorogation carte A (`F-IM-47`), résident longue durée UE (`F-IM-49`), établissement carte C (écarté étape 0 : absorbé), génération de la demande.
