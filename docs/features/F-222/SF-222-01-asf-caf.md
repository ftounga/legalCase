# Mini-spec — F-222 / SF-222-01 — Outil ASF (allocation de soutien familial)

## Identifiant
`F-222 / SF-222-01` — tool_id `F-FA-ASF-CAF` (Famille FR)

## Objectif (1 phrase)
Évaluer le droit à l'allocation de soutien familial (ASF) versée par la CAF quand l'autre parent ne paie pas (ou paie insuffisamment) la pension alimentaire, et estimer le montant.

## Périmètre / anti-doublon
ASF = **droit à une prestation sociale CAF** (art. L.523-1 CSS). **Distinct** de `F-FA-ARIPA-RECOUVREMENT` (recouvrement forcé de la pension). L'outil ASF **ne calcule pas** le recouvrement ; il renvoie vers ARIPA pour cela.

## Comportement (branches de calcul, branche `default`)
Entrées : `parentIsole` (bool), `pensionFixee` (bool), `montantPensionMensuel` (€, nullable), `pensionPayee` (enum: NON_PAYEE / PARTIELLE / PAYEE), `nbEnfantsACharge` (int), `autreParentDecedeOuInconnu` (bool).
Logique (montants ASF 2026 indicatifs, à paramétrer en référentiel) :
- **ASF taux plein** (~187 €/enfant/mois) si `autreParentDecedeOuInconnu` OU pension non fixée/non recouvrable.
- **ASF différentielle** si pension fixée < montant ASF et `pensionPayee=PARTIELLE` → ASF complète jusqu'au montant plancher.
- **ASF récupérable** si `pensionPayee=NON_PAYEE` ET pension fixée → CAF verse l'ASF + se subroge (ARIPA) → renvoi outil ARIPA.
- **Pas de droit** si `parentIsole=false` (ASF réservée au parent isolé sauf cas décès/inconnu).
Verdict 4 niveaux : `DROIT_ASF_PLEIN` / `DROIT_ASF_DIFFERENTIELLE` / `DROIT_AVEC_RECOUVREMENT` / `PAS_DE_DROIT`, + montant mensuel estimé + renvoi ARIPA si applicable.

## Cas d'erreur
| Situation | Comportement |
|---|---|
| `nbEnfantsACharge` ≤ 0 | 400 Bad Request (validation) |
| Aucun champ rempli | bouton calcul désactivé (front) |

## Contrat API (figé pour parallélisation back/front)
`POST /api/v1/case-files/{caseFileId}/asf-caf/analyze`
- Request `AsfCafAnalyzeRequest` : `{ parentIsole:bool, pensionFixee:bool, montantPensionMensuel:number|null, pensionPayee:"NON_PAYEE"|"PARTIELLE"|"PAYEE", nbEnfantsACharge:int, autreParentDecedeOuInconnu:bool }`
- Response `AsfCafAnalyzeResponse` : `{ verdict:string, montantMensuelEstime:number, recouvrementApplicable:bool, basesJuridiques:string[], messages:string[] }`
- 200 OK ; isolation workspace via pattern `getAnalysis` existant.

## Critères d'acceptation
- [ ] Les 4 verdicts couverts + montant estimé.
- [ ] `recouvrementApplicable=true` ⇒ message renvoyant vers `F-FA-ARIPA-RECOUVREMENT`.
- [ ] Pas de chevauchement fonctionnel avec ARIPA (l'outil ne calcule pas le recouvrement).
- [ ] Isolation workspace testée.
- [ ] Tous les champs saisissables pré-remplis par l'IA (sauf non factualisable).

## Plan de test
UT service (4 verdicts + différentielle), IT endpoint (200 + 400 + isolation workspace), Jest composant (rendu form + verdict + bouton désactivé si vide + flush jurisprudence-citations).

## Tables / endpoints / composants
- Backend : migration `asf_caf_analyses` (NNN), entité + repo + `AsfCafService` + `AsfCafController`.
- Frontend : `asf-caf-section.component` (+ .html/.scss/.spec + prefill-rules) + entrée `TOOL_REGISTRY` `F-FA-ASF-CAF` + `decision_tool_visibility_rules` + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- Champs IA (`FamilleExtractedData`) : `asfParentIsole`, `asfPensionFixee`, `asfMontantPension`, `asfPensionPayee`, `asfNbEnfants` — étendre record + prompt `LegalDomainPromptBuilder` Famille.

## Hors périmètre
Recouvrement (ARIPA), calcul détaillé du barème CAF complet (montants en référentiel paramétrable).
