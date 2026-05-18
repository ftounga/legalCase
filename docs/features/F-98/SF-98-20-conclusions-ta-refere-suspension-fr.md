# Mini-spec — F-98 / SF-98-20 — Requête en référé-suspension (TA) — requérant FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section immigration FR) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` (SF-98-02).

## Identifiant
`F-98 / SF-98-20`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-20-backend-ta-refere-suspension` — **SF backend pure**.

## Objectif
Générer le projet de **requête en référé-suspension** (art. L.521-1 CJA) devant le tribunal administratif, côté requérant, droit de l'immigration FR.

## Comportement attendu
### Cas nominal
1. Dossier immigration, workspace FR, stade procédural = Tribunal administratif (`TA`) / Référé-suspension (`REFERE_SUSPENSION`) / Requérant (`REQUERANT`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `TaRefereSuspensionRequerantPromptProvider` — projet de **requête en référé-suspension** fondée sur l'**article L.521-1 du code de justice administrative** : suspension de l'exécution de la décision administrative attaquée lorsqu'il y a **urgence** et qu'un **moyen propre à créer un doute sérieux** sur la légalité de la décision est invoqué. Le prompt précise que le référé-suspension est l'**accessoire d'un recours en annulation au fond**. Structure : exposé des faits ; **URGENCE** ; **DOUTE SÉRIEUX** sur la légalité (moyens) ; rappel du recours au fond ; PAR CES MOTIFS (suspendre l'exécution de la décision).
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
- [ ] **CA1** — `TaRefereSuspensionRequerantPromptProvider` enregistré pour `(DROIT_IMMIGRATION, FRANCE, TA, REFERE_SUSPENSION, REQUERANT)`.
- [ ] **CA2** — le prompt produit une requête en référé-suspension (art. L.521-1 CJA, urgence, doute sérieux sur la légalité, accessoire au recours au fond).
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
- Backend (neuf) : `TaRefereSuspensionRequerantPromptProvider` (`@Component`). Frontend : aucun.

## Plan de test
- [ ] `TaRefereSuspensionRequerantPromptProviderTest` : le prompt cible l'art. L.521-1 CJA, l'urgence, le doute sérieux sur la légalité, l'accessoire au recours au fond.
- [x] `ConclusionPromptRegistryTest` (data-driven, SF-98-02) couvre automatiquement la cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

## Analyse d'impact
- [x] **Aucune préoccupation transversale**. Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre `ConclusionPromptProvider`. F-243 (done) — codes `TA` / `REFERE_SUSPENSION` / `REQUERANT`.

## Notes et décisions
- Le référé-suspension (doute sérieux) a un standard distinct du référé-liberté (atteinte grave et manifestement illégale) — cellule dédiée.
