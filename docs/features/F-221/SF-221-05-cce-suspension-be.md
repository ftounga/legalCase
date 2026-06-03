# Mini-spec — F-221 / SF-221-05 — Outil recours CCE en suspension (référé administratif BE)

## Identifiant
`F-221 / SF-221-05` — tool_id `F-IM-51-cce-suspension-be` (Immigration BE) — slug `cce-suspension-be` — statut `ready` — 2026-06-03
## Branche Git
`feat/SF-221-05-cce-suspension-be`

## Objectif (1 phrase)
Évaluer les conditions du recours CCE en suspension (référé administratif : urgence + risque de préjudice grave difficilement réparable) et son délai, distinct de l'annulation 30j et de l'extrême urgence 5j.

## Périmètre / anti-doublon
Recours **en suspension** devant le CCE (art. 39/82 loi 15/12/1980 ; loi 15/09/2006). **3e recours CCE distinct** des deux déjà livrés en P2 (l'audit § 6.2 tranche explicitement : ne pas fusionner) :
- `F-IM-31-cce-annulation-30j-be` = annulation, 30 j calendaires (légalité de l'acte) ;
- `F-IM-32-cce-extreme-urgence-5j-be` = suspension en **extrême urgence**, 5 j ouvrables (mesure d'éloignement imminente) ;
- `F-IM-51-cce-suspension-be` (cet outil) = suspension **ordinaire** : urgence (non extrême) + risque de préjudice grave difficilement réparable, généralement greffée sur la requête en annulation.
L'outil cadre les conditions propres de la suspension ordinaire pour **ne pas chevaucher** l'extrême urgence. Distinct du générateur `F-IM-06` (qui produit le document).

## Comportement (branches de verdict, branche `default`)
Entrées : `dateNotificationDecision` (date, requis), `recoursAnnulationIntroduit` (bool), `urgenceInvocable` (bool), `risquePrejudiceGraveDifficilementReparable` (bool), `mesureEloignementImminente` (bool).
Logique (jours calendaires, greffage sur l'annulation 30j — **à vérifier par avocat**) :
- `joursDepuisNotification` = today − `dateNotificationDecision`.
- **REORIENTER_EXTREME_URGENCE** si `mesureEloignementImminente=true` → renvoi vers `F-IM-32` (5 j ouvrables) : l'extrême urgence prime quand l'éloignement est imminent.
- **CONDITIONS_REUNIES** si `urgenceInvocable=true` ET `risquePrejudiceGraveDifficilementReparable=true` ET (recours annulation introduit ou introductible dans les 30j) ET `mesureEloignementImminente=false`.
- **URGENCE_NON_DEMONTREE** si `urgenceInvocable=false`.
- **PREJUDICE_NON_DEMONTRE** si `risquePrejudiceGraveDifficilementReparable=false`.
- **HORS_DELAI_ANNULATION** si `joursDepuisNotification > 30` et `recoursAnnulationIntroduit=false` (la suspension se greffe sur l'annulation).
Verdict enum : `CONDITIONS_REUNIES` / `URGENCE_NON_DEMONTREE` / `PREJUDICE_NON_DEMONTRE` / `HORS_DELAI_ANNULATION` / `REORIENTER_EXTREME_URGENCE`, + `joursDepuisNotification` + renvoi cross-outil si applicable + bases juridiques annotées « (à vérifier par avocat) ».

## Cas d'erreur
| Situation | Comportement | Code |
|---|---|---|
| workspace.country ≠ BELGIQUE | 400 | 400 |
| legalDomain ≠ DROIT_IMMIGRATION | 400 | 400 |
| `dateNotificationDecision` absente ou future | 400 (validation) | 400 |
| caseFile hors workspace courant | 404 (isolation) | 404 |
| Aucun champ rempli | bouton calcul désactivé (front) | — |

## Contrat API (figé pour parallélisation back/front)
`POST /api/v1/case-files/{caseFileId}/cce-suspension-be-analysis`
- Request : `{ dateNotificationDecision:date, recoursAnnulationIntroduit:bool, urgenceInvocable:bool, risquePrejudiceGraveDifficilementReparable:bool, mesureEloignementImminente:bool }`
- Response : `{ verdict:string, joursDepuisNotification:int, renvoiOutil:string|null, basesJuridiques:string[], messages:string[] }`
- `GET` même chemin = dernière analyse. 200 OK ; isolation workspace.

## Champs IA (`ImmigrationExtractedData`) + flag pivot
| Champ outil | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|
| `dateNotificationDecision` | `cceSuspensionDateNotification` | Nouveau |
| `urgenceInvocable` | `cceSuspensionUrgence` | Nouveau |
| `risquePrejudiceGraveDifficilementReparable` | `cceSuspensionPrejudiceGrave` | Nouveau |
| **flag pivot** | `cceSuspensionDetecte` (boolean, niveau 2 BE-only, `false` par défaut) | Nouveau |
`recoursAnnulationIntroduit` / `mesureEloignementImminente` = aspirationnels / pré-fill si factualisable. Pré-fill IA F-246.

## Critères d'acceptation
- [ ] Les 5 verdicts couverts + `joursDepuisNotification`.
- [ ] `REORIENTER_EXTREME_URGENCE` ⇒ `renvoiOutil = "F-IM-32-cce-extreme-urgence-5j-be"`.
- [ ] Anti-doublon documenté vs `F-IM-31` (annulation 30j) ET `F-IM-32` (extrême urgence 5j ouvrables).
- [ ] POST workspace FR → 400 ; legalDomain ≠ DROIT_IMMIGRATION → 400.
- [ ] Isolation workspace testée.
- [ ] Pré-fill IA des champs factualisables (F-246).
- [ ] Flag pivot `false` par défaut ; visibility CONTEXTUAL.
- [ ] `F-IM-51-cce-suspension-be` dans `TOOL_REGISTRY`, `KNOWN_FRONTEND_TOOL_IDS`, `KNOWN_NO_DASHBOARD_TILE_IDS`.

## Plan de test
UT calculator (5 verdicts + renvoi extrême urgence + hors délai annulation + urgence/préjudice non démontrés), IT endpoint (200 + gates 400 FR/domaine + 400 validation + 404 isolation), Jest composant (form pré-rempli + verdict + renvoi + bouton désactivé si vide + flush jurisprudence-citations).

## Tables / endpoints / composants
- Backend : migration `cce_suspension_be_analyses` (à pré-assigner) + entité + repo + `CceSuspensionBeCalculator` + service + controller.
- Frontend : `cce-suspension-be-section.component` (+ .html/.scss/.spec + prefill-rules) + `TOOL_REGISTRY` `F-IM-51-cce-suspension-be` + `decision_tool_visibility_rules` (CONTEXTUAL, trigger `cceSuspensionDetecte=true`) + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- Champs IA : étendre `ImmigrationExtractedData` (3 champs + flag pivot) + prompt `LegalDomainPromptBuilder` Immigration BE.

## Invariants
- CONTEXTUAL (jamais ALWAYS_ON) — flag pivot `cceSuspensionDetecte` niveau 2 BE-only `false` par défaut.
- 1 outil = 1 situation : suspension ordinaire = recours CCE distinct des 2 autres (annulation, extrême urgence).
- Pré-fill IA F-246 ; visibility + KNOWN_NO_DASHBOARD ; BE-only (gate country=BELGIQUE).

## Hors périmètre
Annulation 30j (`F-IM-31`), extrême urgence 5j (`F-IM-32`), génération du document de recours (`F-IM-06`), détention chambre du conseil (`F-IM-50`).
