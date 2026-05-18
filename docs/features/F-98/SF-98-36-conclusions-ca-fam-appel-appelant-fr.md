# Mini-spec — F-98 / SF-98-36 — Conclusions d'appel chambre de la famille — appelant FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section famille FR) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` (SF-98-02).

## Identifiant
`F-98 / SF-98-36`
## Feature parente
`F-98` — Génération de courrier / conclusions
## Statut
`done` — livrée 2026-05-18.
## Date de création
2026-05-18
## Branche Git
- `feat/SF-98-36-backend-ca-fam-appel-appelant` — **SF backend pure**.

## Objectif
Générer le projet de conclusions d'**appel** côté appelant devant la cour d'appel — chambre de la famille, droit de la famille FR.

## Comportement attendu
### Cas nominal
1. Dossier famille, workspace FR, stade procédural = Cour d'appel chambre de la famille (`CA_FAM`) / Appel (`APPEL`) / Appelant (`APPELANT`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `CaFamAppelAppelantPromptProvider` — conclusions d'appel devant la chambre de la famille de la cour d'appel contre un jugement du juge aux affaires familiales. Procédure avec représentation obligatoire, structure conforme à l'**art. 954 du code de procédure civile** : en-tête ; RAPPEL DES FAITS ET DE LA PROCÉDURE (incluant le jugement déféré du JAF) ; DISCUSSION (critique du jugement chef par chef — moyens d'infirmation, sur le divorce et/ou ses conséquences) ; DISPOSITIF récapitulatif (« Il est demandé à la Cour de : INFIRMER le jugement en ce qu'il… ; statuant à nouveau… »).
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
- [ ] **CA1** — `CaFamAppelAppelantPromptProvider` enregistré pour `(DROIT_FAMILLE, FRANCE, CA_FAM, APPEL, APPELANT)`.
- [ ] **CA2** — le prompt produit des conclusions d'appel familial (chambre de la famille, art. 954 CPC, critique du jugement du JAF, dispositif récapitulatif « INFIRMER »).
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
- Backend (neuf) : `CaFamAppelAppelantPromptProvider` (`@Component`). Frontend : aucun.

## Plan de test
- [ ] `CaFamAppelAppelantPromptProviderTest` : le prompt cible la chambre de la famille de la cour d'appel, le jugement du JAF, le dispositif récapitulatif « INFIRMER ».
- [x] `ConclusionPromptRegistryTest` (data-driven, SF-98-02) couvre automatiquement la cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

## Analyse d'impact
- [x] **Aucune préoccupation transversale**. Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre. F-243 (done) — codes `CA_FAM` / `APPEL` / `APPELANT`.

## Notes et décisions
- Structure d'appel art. 954 CPC — dispositif récapitulatif propre à l'appel.
