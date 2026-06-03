# Mini-spec — F-221 / SF-221-03 — Outil résident longue durée UE (BE)

## Identifiant
`F-221 / SF-221-03` — tool_id `F-IM-49-residence-longue-duree-ue-be` (Immigration BE) — slug `residence-longue-duree-ue-be` — statut `ready` — 2026-06-03
## Branche Git
`feat/SF-221-03-residence-longue-duree-ue-be`

## Objectif (1 phrase)
Évaluer l'éligibilité au statut de résident longue durée UE (5 ans de séjour légal + ressources stables + assurance maladie + condition d'intégration), distinct de la carte B nationale.

## Périmètre / anti-doublon
Statut **résident longue durée UE** (art. 15bis loi 15/12/1980, transposant la directive 2003/109/CE). **Distinct** de `F-IM-48-carte-b-sejour-illimite-be` : la carte B est un séjour illimité **national** sans mobilité UE ; la résidence longue durée UE ajoute des **conditions propres** (ressources stables/régulières/suffisantes, assurance maladie, intégration) et confère la **mobilité intra-UE**. Les deux outils ont des entrées et verdicts différents — aucun chevauchement de formulaire.

## Comportement (branches de verdict, branche `default`)
Entrées : `dateDebutSejourLegal` (date, requis), `sejourLegalIninterrompu` (bool), `ressourcesStablesSuffisantes` (bool), `assuranceMaladie` (bool), `conditionIntegrationRemplie` (bool), `absencesHorsUeExcessives` (bool).
Logique (5 ans = 60 mois indicatif art. 15bis, **à vérifier par avocat**) :
- `dureeSejourMois` calculée depuis `dateDebutSejourLegal`.
- **ELIGIBLE** si `dureeSejourMois ≥ 60` ET `sejourLegalIninterrompu=true` ET `ressourcesStablesSuffisantes=true` ET `assuranceMaladie=true` ET `conditionIntegrationRemplie=true` ET `absencesHorsUeExcessives=false`.
- **DUREE_INSUFFISANTE** si `dureeSejourMois < 60` → `moisRestants`.
- **CONDITIONS_MATERIELLES_NON_REUNIES** si ressources OU assurance OU intégration manquante (liste les conditions non remplies).
- **CONTINUITE_ROMPUE** si `sejourLegalIninterrompu=false` OU `absencesHorsUeExcessives=true`.
- **A_EXAMINER** (default) si données partielles.
Verdict enum : `ELIGIBLE` / `DUREE_INSUFFISANTE` / `CONDITIONS_MATERIELLES_NON_REUNIES` / `CONTINUITE_ROMPUE` / `A_EXAMINER`, + `dureeSejourMois` + `moisRestants` + liste conditions manquantes + bases juridiques annotées « (à vérifier par avocat) ».

## Cas d'erreur
| Situation | Comportement | Code |
|---|---|---|
| workspace.country ≠ BELGIQUE | 400 | 400 |
| legalDomain ≠ DROIT_IMMIGRATION | 400 | 400 |
| `dateDebutSejourLegal` absente ou future | 400 (validation) | 400 |
| caseFile hors workspace courant | 404 (isolation) | 404 |
| Aucun champ rempli | bouton calcul désactivé (front) | — |

## Contrat API (figé pour parallélisation back/front)
`POST /api/v1/case-files/{caseFileId}/residence-longue-duree-ue-be-analysis`
- Request : `{ dateDebutSejourLegal:date, sejourLegalIninterrompu:bool, ressourcesStablesSuffisantes:bool, assuranceMaladie:bool, conditionIntegrationRemplie:bool, absencesHorsUeExcessives:bool }`
- Response : `{ verdict:string, dureeSejourMois:int, moisRestants:int, conditionsManquantes:string[], basesJuridiques:string[], messages:string[] }`
- `GET` même chemin = dernière analyse. 200 OK ; isolation workspace.

## Champs IA (`ImmigrationExtractedData`) + flag pivot
| Champ outil | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|
| `dateDebutSejourLegal` | `rlueDateDebutSejour` | Nouveau |
| `ressourcesStablesSuffisantes` | `rlueRessourcesSuffisantes` | Nouveau |
| `assuranceMaladie` | `rlueAssuranceMaladie` | Nouveau |
| `conditionIntegrationRemplie` | `rlueIntegrationRemplie` | Nouveau |
| **flag pivot** | `residenceLongueDureeUeDetecte` (boolean, niveau 2 BE-only, `false` par défaut) | Nouveau |
`sejourLegalIninterrompu` / `absencesHorsUeExcessives` = pré-fill si factualisable. Pré-fill IA F-246.

## Critères d'acceptation
- [ ] Les 5 verdicts couverts + `conditionsManquantes` listées.
- [ ] Seuil 60 mois + conditions matérielles (ressources/assurance/intégration) évaluées séparément.
- [ ] Anti-doublon `F-IM-48` documenté (mobilité UE = spécificité de cet outil).
- [ ] POST workspace FR → 400 ; legalDomain ≠ DROIT_IMMIGRATION → 400.
- [ ] Isolation workspace testée.
- [ ] Pré-fill IA des champs factualisables (F-246).
- [ ] Flag pivot `false` par défaut ; visibility CONTEXTUAL.
- [ ] `F-IM-49-residence-longue-duree-ue-be` dans `TOOL_REGISTRY`, `KNOWN_FRONTEND_TOOL_IDS`, `KNOWN_NO_DASHBOARD_TILE_IDS`.

## Plan de test
UT calculator (5 verdicts + seuil 60 mois + conditions manquantes énumérées + continuité rompue), IT endpoint (200 + gates 400 FR/domaine + 400 validation + 404 isolation), Jest composant (form pré-rempli + verdict + bouton désactivé si vide + flush jurisprudence-citations).

## Tables / endpoints / composants
- Backend : migration `residence_longue_duree_ue_be_analyses` (à pré-assigner) + entité + repo + `ResidenceLongueDureeUeBeCalculator` + service + controller.
- Frontend : `residence-longue-duree-ue-be-section.component` (+ .html/.scss/.spec + prefill-rules) + `TOOL_REGISTRY` `F-IM-49-residence-longue-duree-ue-be` + `decision_tool_visibility_rules` (CONTEXTUAL, trigger `residenceLongueDureeUeDetecte=true`) + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- Champs IA : étendre `ImmigrationExtractedData` (4 champs + flag pivot) + prompt `LegalDomainPromptBuilder` Immigration BE.

## Invariants
- CONTEXTUAL (jamais ALWAYS_ON) — flag pivot `residenceLongueDureeUeDetecte` niveau 2 BE-only `false` par défaut.
- Pré-fill IA F-246 ; visibility + KNOWN_NO_DASHBOARD ; 1 outil = 1 situation ; BE-only (gate country=BELGIQUE).

## Hors périmètre
Carte B nationale (`F-IM-48`), mobilité effective vers un autre État membre (procédure de l'État d'accueil), génération de la demande.
