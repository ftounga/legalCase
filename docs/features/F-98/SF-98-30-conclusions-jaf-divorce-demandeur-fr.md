# Mini-spec — F-98 / SF-98-30 — Conclusions divorce au fond (JAF) — demandeur FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section famille FR) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` (SF-98-02).

## Identifiant
`F-98 / SF-98-30`
## Feature parente
`F-98` — Génération de courrier / conclusions
## Statut
`done` — livrée 2026-05-18.
## Date de création
2026-05-18
## Branche Git
- `feat/SF-98-30-backend-jaf-divorce-demandeur` — **SF backend pure**.

## Objectif
Générer le projet de conclusions de **divorce au fond** devant le juge aux affaires familiales, côté demandeur, droit de la famille FR.

## Comportement attendu
### Cas nominal
1. Dossier famille, workspace FR, stade procédural = JAF (`JAF`) / Divorce au fond (`DIVORCE_FOND`) / Demandeur (`DEMANDEUR`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `JafDivorceFondDemandeurPromptProvider` — conclusions de divorce contentieux devant le juge aux affaires familiales (tribunal judiciaire). Ancrage : **code civil** (art. 233 et s. — cas de divorce : acceptation du principe de la rupture, altération définitive du lien conjugal, faute) et **code de procédure civile**. Structure : en-tête (POUR [demandeur] / CONTRE [défendeur]) ; FAITS ET PROCÉDURE ; DISCUSSION — cas de divorce invoqué, puis demandes accessoires (prestation compensatoire, contribution à l'entretien et à l'éducation des enfants, exercice de l'autorité parentale, résidence des enfants, jouissance du logement, usage du nom) ; PAR CES MOTIFS (dispositif chiffré).
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
- [ ] **CA1** — `JafDivorceFondDemandeurPromptProvider` enregistré pour `(DROIT_FAMILLE, FRANCE, JAF, DIVORCE_FOND, DEMANDEUR)`.
- [ ] **CA2** — le prompt produit des conclusions de divorce au fond (JAF, cas de divorce du code civil, demandes accessoires), rôle demandeur.
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
- Backend (neuf) : `JafDivorceFondDemandeurPromptProvider` (`@Component`). Frontend : aucun.

## Plan de test
- [ ] `JafDivorceFondDemandeurPromptProviderTest` : le prompt cible le JAF, les cas de divorce du code civil, les demandes accessoires.
- [x] `ConclusionPromptRegistryTest` (data-driven, SF-98-02) couvre automatiquement la cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

## Analyse d'impact
- [x] **Aucune préoccupation transversale**. Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre. F-243 (done) — codes `JAF` / `DIVORCE_FOND` / `DEMANDEUR`.

## Notes et décisions
- Périmètre = divorce **contentieux** ; le divorce par consentement mutuel (déjudiciarisé) est hors objet d'un projet de conclusions.
