# Mini-spec — F-98 / SF-98-33 — Conclusions mesures provisoires (JAF) — défendeur FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section famille FR) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` (SF-98-02).

## Identifiant
`F-98 / SF-98-33`
## Feature parente
`F-98` — Génération de courrier / conclusions
## Statut
`done` — livrée 2026-05-18.
## Date de création
2026-05-18
## Branche Git
- `feat/SF-98-33-backend-jaf-mesures-defendeur` — **SF backend pure**.

## Objectif
Générer le projet de conclusions **en défense** sur mesures provisoires devant le juge aux affaires familiales, côté défendeur, droit de la famille FR.

## Comportement attendu
### Cas nominal
1. Dossier famille, workspace FR, stade procédural = JAF (`JAF`) / Mesures provisoires (`MESURES_PROVISOIRES`) / Défendeur (`DEFENDEUR`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `JafMesuresProvisoiresDefendeurPromptProvider` — conclusions **en défense** sur les mesures provisoires devant le juge aux affaires familiales. Ancrage : **code civil** (art. 254-255). Structure : en-tête (POUR [défendeur] / CONTRE [demandeur]) ; FAITS ET PROCÉDURE ; DISCUSSION — réfutation / contre-propositions mesure par mesure (résidence des enfants, contribution à l'entretien et à l'éducation, pension au titre du devoir de secours, jouissance du logement, droit de visite et d'hébergement) ; PAR CES MOTIFS (dispositif chiffré, en défense).
4. La version générée passe `DONE`.

### Cas d'erreur
| Combinaison hors registre | `409 COMBINATION_NOT_SUPPORTED` |
| Gardes `STAGE_NOT_SET` / `ANALYSIS_NOT_READY` / `ALREADY_GENERATING` | inchangées |

## Analyse de cohérence transversale
- [x] Outil **non décisionnel** — générateur de document.
- [x] Ajout additif : un `@Component ConclusionPromptProvider`, aucun fichier partagé modifié.
- [x] Préoccupations transversales : **aucune**.

## Conformité F-IA-04
- [x] **Non applicable** — générateur de document.

## Critères d'acceptation
- [ ] **CA1** — `JafMesuresProvisoiresDefendeurPromptProvider` enregistré pour `(DROIT_FAMILLE, FRANCE, JAF, MESURES_PROVISOIRES, DEFENDEUR)`.
- [ ] **CA2** — le prompt produit des conclusions sur mesures provisoires **en défense** (JAF, art. 254-255 code civil, contre-propositions).
- [ ] **CA3** — la garde `COMBINATION_NOT_SUPPORTED` accepte cette combinaison.
- [ ] **CA4** — le prompt cite les pièces par numéro et reprend les montants des calculs (invariants F-98 préservés).
- [ ] **CA5** — isolation workspace inchangée ; consigne de style SF-98-47 appliquée par-dessus.

## Périmètre
### Hors scope
- Les autres cellules de la matrice — SF distinctes. Tout changement frontend.

## Technique
### Contrat API / Tables / Migration
Inchangé / aucune / **non applicable**.
### Composants
- Backend (neuf) : `JafMesuresProvisoiresDefendeurPromptProvider` (`@Component`). Frontend : aucun.

## Plan de test
- [ ] `JafMesuresProvisoiresDefendeurPromptProviderTest` : le prompt cible le JAF, les mesures provisoires en défense, rôle défendeur.
- [x] `ConclusionPromptRegistryTest` (data-driven, SF-98-02) couvre automatiquement la cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

## Analyse d'impact
- [x] **Aucune préoccupation transversale**. Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre. F-243 (done) — codes `JAF` / `MESURES_PROVISOIRES` / `DEFENDEUR`.

## Notes et décisions
- Cellule miroir de SF-98-32 côté défense.
