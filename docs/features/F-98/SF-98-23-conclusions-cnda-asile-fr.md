# Mini-spec — F-98 / SF-98-23 — Recours asile devant la CNDA — requérant FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section immigration FR) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` (SF-98-02).

## Identifiant
`F-98 / SF-98-23`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`done` — livrée 2026-05-18.

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-23-backend-cnda-asile` — **SF backend pure**.

## Objectif
Générer le projet de **recours** devant la Cour nationale du droit d'asile, côté requérant, droit de l'immigration FR.

## Comportement attendu
### Cas nominal
1. Dossier immigration, workspace FR, stade procédural = Cour nationale du droit d'asile (`CNDA`) / Recours asile (`RECOURS_ASILE`) / Requérant (`REQUERANT`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `CndaAsileRequerantPromptProvider` — projet de **recours devant la CNDA** contre une décision de l'OFPRA rejetant une demande de protection. Ancrage : **CESEDA** (livre V — asile), **convention de Genève du 28 juillet 1951**. La CNDA statue en **plein contentieux** : elle se prononce elle-même sur le droit à la protection. Structure : exposé du parcours et des craintes du demandeur ; recevabilité ; **DISCUSSION** — éligibilité au **statut de réfugié** (craintes de persécution pour un motif conventionnel) et, subsidiairement, à la **protection subsidiaire** ; crédibilité du récit, actualité et caractère personnel des craintes ; PAR CES MOTIFS (annuler la décision de l'OFPRA et reconnaître la qualité de réfugié / accorder la protection subsidiaire).
4. La version générée passe `DONE`.

### Cas d'erreur
| Situation | Comportement |
|---|---|
| Combinaison hors registre | `409 COMBINATION_NOT_SUPPORTED` |
| Gardes `STAGE_NOT_SET` / `ANALYSIS_NOT_READY` / `ALREADY_GENERATING` | inchangées |

## Analyse de cohérence transversale
- [x] Outil **non décisionnel** — générateur de document.
- [x] Ajout additif : un `@Component ConclusionPromptProvider`, aucun fichier partagé modifié.
- [x] Préoccupations transversales : **aucune**.

## Conformité F-IA-04
- [x] **Non applicable** — générateur de document.

## Critères d'acceptation
- [ ] **CA1** — `CndaAsileRequerantPromptProvider` enregistré pour `(DROIT_IMMIGRATION, FRANCE, CNDA, RECOURS_ASILE, REQUERANT)`.
- [ ] **CA2** — le prompt produit un recours asile (CNDA, plein contentieux, statut de réfugié / protection subsidiaire, convention de Genève).
- [ ] **CA3** — la garde `COMBINATION_NOT_SUPPORTED` accepte cette combinaison.
- [ ] **CA4** — le prompt cite les pièces par numéro (invariants F-98 préservés).
- [ ] **CA5** — isolation workspace inchangée ; consigne de style SF-98-47 appliquée par-dessus.

## Périmètre
### Hors scope
- Les autres cellules de la matrice — SF distinctes. Tout changement frontend.

## Technique
### Contrat API / Tables / Migration
Inchangé / aucune / **non applicable**.
### Composants
- Backend (neuf) : `CndaAsileRequerantPromptProvider` (`@Component`). Frontend : aucun.

## Plan de test
- [ ] `CndaAsileRequerantPromptProviderTest` : le prompt cible la CNDA, le plein contentieux, le statut de réfugié et la protection subsidiaire.
- [x] `ConclusionPromptRegistryTest` (data-driven, SF-98-02) couvre automatiquement la cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

## Analyse d'impact
- [x] **Aucune préoccupation transversale**. Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre `ConclusionPromptProvider`. F-243 (done) — codes `CNDA` / `RECOURS_ASILE` / `REQUERANT`.

## Notes et décisions
- La CNDA statue en **plein contentieux** (et non en simple annulation) — le prompt l'explicite : le juge accorde lui-même la protection.
