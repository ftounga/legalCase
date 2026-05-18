# Mini-spec — F-98 / SF-98-34 — Conclusions de référé devant le JAF (demandeur + défendeur) FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section famille FR) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` (SF-98-02).

## Identifiant
`F-98 / SF-98-34`
## Feature parente
`F-98` — Génération de courrier / conclusions
## Statut
`ready`
## Date de création
2026-05-18
## Branche Git
- `feat/SF-98-34-backend-jaf-refere` — **SF backend pure**.

## Objectif
Générer le projet de conclusions de **référé** devant le juge aux affaires familiales, côté demandeur **et** côté défendeur, droit de la famille FR.

## Décision de périmètre
La matrice `SF-98-00` prévoit une SF unique (SF-98-34) pour le référé famille FR. Le catalogue F-243 expose **deux positions** pour le stade `REFERE` (`DEMANDEUR`, `DEFENDEUR`). Pour ne pas laisser de trou `COMBINATION_NOT_SUPPORTED`, **SF-98-34 livre les deux providers**.

## Comportement attendu
### Cas nominal
1. Dossier famille, workspace FR, stade procédural = JAF (`JAF`) / Référé (`REFERE`) / `DEMANDEUR` **ou** `DEFENDEUR`.
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via le provider de la combinaison :
   - **`JafRefereDemandeurPromptProvider`** (`DEMANDEUR`) — conclusions de référé devant le juge aux affaires familiales : mesures urgentes et provisoires relatives à l'autorité parentale, à la résidence des enfants, à la contribution à leur entretien. Ancrage : **code civil** + **code de procédure civile** (procédure de référé). Structure : en-tête, FAITS ET PROCÉDURE, URGENCE, DISCUSSION (mesures sollicitées, motivées), PAR CES MOTIFS.
   - **`JafRefereDefendeurPromptProvider`** (`DEFENDEUR`) — conclusions de référé **en défense** : contestation de l'urgence, contre-propositions sur les mesures sollicitées, PAR CES MOTIFS en défense.
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
- [ ] **CA1** — `JafRefereDemandeurPromptProvider` enregistré pour `(DROIT_FAMILLE, FRANCE, JAF, REFERE, DEMANDEUR)`.
- [ ] **CA2** — `JafRefereDefendeurPromptProvider` enregistré pour `(DROIT_FAMILLE, FRANCE, JAF, REFERE, DEFENDEUR)`.
- [ ] **CA3** — les prompts produisent des conclusions de référé familial (JAF, urgence, mesures provisoires) — demandeur / défendeur.
- [ ] **CA4** — la garde `COMBINATION_NOT_SUPPORTED` accepte les deux combinaisons.
- [ ] **CA5** — les prompts citent les pièces par numéro (invariants F-98 préservés) ; style SF-98-47 appliqué par-dessus ; isolation workspace inchangée.

## Périmètre
### Hors scope
- Les autres cellules de la matrice — SF distinctes. Tout changement frontend.

## Technique
### Contrat API / Tables / Migration
Inchangé / aucune / **non applicable**.
### Composants
- Backend (neufs) : `JafRefereDemandeurPromptProvider`, `JafRefereDefendeurPromptProvider` (`@Component`). Frontend : aucun.

## Plan de test
- [ ] `JafRefereDemandeurPromptProviderTest` / `JafRefereDefendeurPromptProviderTest` : le prompt cible le référé JAF, l'urgence, les mesures provisoires ; rôle correct.
- [x] `ConclusionPromptRegistryTest` (data-driven, SF-98-02) couvre automatiquement les deux cellules.
### Isolation workspace
- [x] Couverte par les contrôles existants.

## Analyse d'impact
- [x] **Aucune préoccupation transversale**. Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre. F-243 (done) — codes `JAF` / `REFERE` / `DEMANDEUR` / `DEFENDEUR`.

## Notes et décisions
- 2 providers pour ne pas laisser de trou de couverture sur le stade référé famille FR (même rationale que SF-98-17).
