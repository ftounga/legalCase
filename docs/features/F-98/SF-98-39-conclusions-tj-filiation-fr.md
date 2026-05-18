# Mini-spec — F-98 / SF-98-39 — Conclusions filiation (TJ) — demandeur + défendeur FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section famille FR) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` (SF-98-02).

## Identifiant
`F-98 / SF-98-39`
## Feature parente
`F-98` — Génération de courrier / conclusions
## Statut
`done` — livrée 2026-05-18.
## Date de création
2026-05-18
## Branche Git
- `feat/SF-98-39-backend-tj-filiation` — **SF backend pure**.

## Objectif
Générer le projet de conclusions en matière de **filiation** devant le tribunal judiciaire, côté demandeur **et** côté défendeur, droit de la famille FR.

## Décision de périmètre
La matrice `SF-98-00` prévoit une SF unique (SF-98-39) pour la filiation. Le catalogue F-243 expose **deux positions** pour le stade `FILIATION` (`DEMANDEUR`, `DEFENDEUR`). **SF-98-39 livre les deux providers** — aucun trou `COMBINATION_NOT_SUPPORTED`.

## Comportement attendu
### Cas nominal
1. Dossier famille, workspace FR, stade = Tribunal judiciaire (`TJ`) / Filiation (`FILIATION`) / `DEMANDEUR` **ou** `DEFENDEUR`.
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via le provider de la combinaison :
   - **`TjFiliationDemandeurPromptProvider`** (`DEMANDEUR`) — conclusions en **établissement ou contestation de filiation** devant le tribunal judiciaire. Ancrage : **code civil** (titre VII du livre Iᵉʳ — de la filiation : actions en recherche / contestation de paternité ou de maternité, présomptions, possession d'état, expertise biologique). Structure : en-tête, FAITS ET PROCÉDURE, DISCUSSION (fondement de l'action, modes de preuve de la filiation, demande d'expertise génétique), PAR CES MOTIFS.
   - **`TjFiliationDefendeurPromptProvider`** (`DEFENDEUR`) — conclusions **en défense** : contestation de la recevabilité (prescription, qualité), réfutation des éléments de preuve, position sur l'expertise, PAR CES MOTIFS en défense.
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
- [ ] **CA1** — `TjFiliationDemandeurPromptProvider` enregistré pour `(DROIT_FAMILLE, FRANCE, TJ, FILIATION, DEMANDEUR)`.
- [ ] **CA2** — `TjFiliationDefendeurPromptProvider` enregistré pour `(DROIT_FAMILLE, FRANCE, TJ, FILIATION, DEFENDEUR)`.
- [ ] **CA3** — les prompts produisent des conclusions de filiation (TJ, titre VII du code civil, modes de preuve, expertise) — demandeur / défendeur.
- [ ] **CA4** — les prompts citent les pièces par numéro (invariants F-98 préservés).
- [ ] **CA5** — la garde `COMBINATION_NOT_SUPPORTED` accepte les deux combinaisons ; isolation workspace inchangée ; style SF-98-47 appliqué par-dessus.

## Périmètre
### Hors scope
- Les autres cellules de la matrice — SF distinctes. Tout changement frontend.

## Technique
### Contrat API / Tables / Migration
Inchangé / aucune / **non applicable**.
### Composants
- Backend (neufs) : `TjFiliationDemandeurPromptProvider`, `TjFiliationDefendeurPromptProvider` (`@Component`). Frontend : aucun.

## Plan de test
- [ ] `TjFiliationDemandeurPromptProviderTest` / `TjFiliationDefendeurPromptProviderTest` : le prompt cible le TJ, la filiation (titre VII code civil), les modes de preuve ; rôle correct.
- [x] `ConclusionPromptRegistryTest` (data-driven, SF-98-02) couvre automatiquement les deux cellules.
### Isolation workspace
- [x] Couverte par les contrôles existants.

## Analyse d'impact
- [x] **Aucune préoccupation transversale**. Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre. F-243 (done) — codes `TJ` / `FILIATION` / `DEMANDEUR` / `DEFENDEUR`.

## Notes et décisions
- 2 providers pour ne pas laisser de trou de couverture sur le stade filiation (même rationale que SF-98-17).
