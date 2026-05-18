# Mini-spec — F-98 / SF-98-21 — Requête d'appel cour administrative d'appel — appelant FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section immigration FR) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` (SF-98-02).

## Identifiant
`F-98 / SF-98-21`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-21-backend-caa-appel` — **SF backend pure**.

## Objectif
Générer le projet de **requête d'appel** devant la cour administrative d'appel, côté appelant, droit de l'immigration FR.

## Comportement attendu
### Cas nominal
1. Dossier immigration, workspace FR, stade procédural = Cour administrative d'appel (`CAA`) / Appel (`APPEL`) / Requérant (`REQUERANT`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `CaaAppelRequerantPromptProvider` — projet de **requête d'appel** devant la cour administrative d'appel contre un jugement du tribunal administratif. Ancrage : **code de justice administrative**. Structure : exposé des faits et de la procédure (incluant le jugement attaqué du TA) ; recevabilité de l'appel ; **DISCUSSION** — critique du jugement attaqué, moyens d'annulation / de réformation (la CAA statue à nouveau, effet dévolutif) ; PAR CES MOTIFS (annuler le jugement et faire droit aux conclusions de première instance).
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
- [ ] **CA1** — `CaaAppelRequerantPromptProvider` enregistré pour `(DROIT_IMMIGRATION, FRANCE, CAA, APPEL, REQUERANT)`.
- [ ] **CA2** — le prompt produit une requête d'appel administratif (CAA, jugement du TA attaqué, critique du jugement, effet dévolutif).
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
- Backend (neuf) : `CaaAppelRequerantPromptProvider` (`@Component`). Frontend : aucun.

## Plan de test
- [ ] `CaaAppelRequerantPromptProviderTest` : le prompt cible la CAA, le jugement du TA attaqué, la critique du jugement et les moyens de réformation.
- [x] `ConclusionPromptRegistryTest` (data-driven, SF-98-02) couvre automatiquement la cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

## Analyse d'impact
- [x] **Aucune préoccupation transversale**. Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre `ConclusionPromptProvider`. F-243 (done) — codes `CAA` / `APPEL` / `REQUERANT`.

## Notes et décisions
- Le catalogue F-243 rattache le stade `APPEL` immigration FR à la position `REQUERANT` (pas d'`APPELANT` distinct côté immigration) — la cellule suit le catalogue.
