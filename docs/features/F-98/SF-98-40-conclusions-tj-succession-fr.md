# Mini-spec — F-98 / SF-98-40 — Conclusions succession / partage judiciaire (TJ) — demandeur + défendeur FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section famille FR) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` (SF-98-02).

## Identifiant
`F-98 / SF-98-40`
## Feature parente
`F-98` — Génération de courrier / conclusions
## Statut
`done` — livrée 2026-05-18.
## Date de création
2026-05-18
## Branche Git
- `feat/SF-98-40-backend-tj-succession` — **SF backend pure**.

## Objectif
Générer le projet de conclusions en matière de **succession / partage judiciaire** devant le tribunal judiciaire, côté demandeur **et** côté défendeur, droit de la famille FR.

## Décision de périmètre
La matrice `SF-98-00` prévoit une SF unique (SF-98-40) pour la succession. Le catalogue F-243 expose **deux positions** pour le stade `SUCCESSION` (`DEMANDEUR`, `DEFENDEUR`). **SF-98-40 livre les deux providers** — aucun trou `COMBINATION_NOT_SUPPORTED`.

## Comportement attendu
### Cas nominal
1. Dossier famille, workspace FR, stade = Tribunal judiciaire (`TJ`) / Succession (`SUCCESSION`) / `DEMANDEUR` **ou** `DEFENDEUR`.
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via le provider de la combinaison :
   - **`TjSuccessionDemandeurPromptProvider`** (`DEMANDEUR`) — conclusions en **partage judiciaire / sortie d'indivision** devant le tribunal judiciaire. Ancrage : **code civil** (livre III — successions et partage : indivision, rapport et réduction des libéralités, réserve héréditaire, ouverture des opérations de comptes-liquidation-partage). Structure : en-tête, FAITS ET PROCÉDURE, DISCUSSION (demande d'ouverture des opérations de partage, désignation d'un notaire, contestations sur la composition de la masse / les rapports / la réserve), PAR CES MOTIFS (dispositif chiffré).
   - **`TjSuccessionDefendeurPromptProvider`** (`DEFENDEUR`) — conclusions **en défense** : réfutation / contre-propositions sur la masse partageable, les rapports et la réduction, l'évaluation des biens, PAR CES MOTIFS en défense.
4. La version générée passe `DONE`.

### Cas d'erreur
| Combinaison hors registre | `409 COMBINATION_NOT_SUPPORTED` |
| Gardes `STAGE_NOT_SET` / `ANALYSIS_NOT_READY` / `ALREADY_GENERATING` | inchangées |

## Analyse de cohérence transversale
- [x] Outil **non décisionnel** — générateur de document.
- [x] Ajout additif : deux `@Component ConclusionPromptProvider`, aucun fichier partagé modifié.
- [x] Préoccupations transversales : **aucune**.

## Conformité F-IA-04
- [x] **Non applicable** — générateur de document.

## Critères d'acceptation
- [ ] **CA1** — `TjSuccessionDemandeurPromptProvider` enregistré pour `(DROIT_FAMILLE, FRANCE, TJ, SUCCESSION, DEMANDEUR)`.
- [ ] **CA2** — `TjSuccessionDefendeurPromptProvider` enregistré pour `(DROIT_FAMILLE, FRANCE, TJ, SUCCESSION, DEFENDEUR)`.
- [ ] **CA3** — les prompts produisent des conclusions de partage judiciaire (TJ, livre III du code civil, masse partageable, rapports, réserve) — demandeur / défendeur.
- [ ] **CA4** — les prompts citent les pièces par numéro et reprennent les montants des calculs (invariants F-98 préservés).
- [ ] **CA5** — la garde `COMBINATION_NOT_SUPPORTED` accepte les deux combinaisons ; isolation workspace inchangée ; style SF-98-47 appliqué par-dessus.

## Périmètre
### Hors scope
- Les autres cellules de la matrice — SF distinctes. Tout changement frontend.

## Technique
### Contrat API / Tables / Migration
Inchangé / aucune / **non applicable**.
### Composants
- Backend (neufs) : `TjSuccessionDemandeurPromptProvider`, `TjSuccessionDefendeurPromptProvider` (`@Component`). Frontend : aucun.

## Plan de test
- [ ] `TjSuccessionDemandeurPromptProviderTest` / `TjSuccessionDefendeurPromptProviderTest` : le prompt cible le TJ, le partage judiciaire (livre III code civil) ; rôle correct.
- [x] `ConclusionPromptRegistryTest` (data-driven, SF-98-02) couvre automatiquement les deux cellules.
### Isolation workspace
- [x] Couverte par les contrôles existants.

## Analyse d'impact
- [x] **Aucune préoccupation transversale**. Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre. F-243 (done) — codes `TJ` / `SUCCESSION` / `DEMANDEUR` / `DEFENDEUR`.

## Notes et décisions
- 2 providers pour le stade succession complet. **Solde la famille FR de la matrice** (SF-98-30 → SF-98-40).
