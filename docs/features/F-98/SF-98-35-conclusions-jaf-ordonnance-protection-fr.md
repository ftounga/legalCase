# Mini-spec — F-98 / SF-98-35 — Requête en ordonnance de protection (JAF) — requérant FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section famille FR) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` (SF-98-02).

## Identifiant
`F-98 / SF-98-35`
## Feature parente
`F-98` — Génération de courrier / conclusions
## Statut
`done` — livrée 2026-05-18.
## Date de création
2026-05-18
## Branche Git
- `feat/SF-98-35-backend-jaf-ordonnance-protection` — **SF backend pure**.

## Objectif
Générer le projet de **requête en ordonnance de protection** (violences) devant le juge aux affaires familiales, côté requérant, droit de la famille FR.

## Comportement attendu
### Cas nominal
1. Dossier famille, workspace FR, stade procédural = JAF (`JAF`) / Ordonnance de protection (`ORDONNANCE_PROTECTION`) / Requérant (`REQUERANT`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `JafOrdonnanceProtectionRequerantPromptProvider` — projet de **requête / conclusions en ordonnance de protection** devant le juge aux affaires familiales, en cas de violences au sein du couple ou de la famille. Ancrage : **code civil** (art. 515-9 et s. — ordonnance de protection ; le juge la délivre s'il existe des **raisons sérieuses de considérer comme vraisemblables** les faits de violence allégués et le danger). Structure : en-tête ; FAITS (récit des violences) ; DISCUSSION — **vraisemblance des violences et du danger**, puis mesures sollicitées (interdiction d'entrer en contact, éviction du conjoint violent, dissimulation de l'adresse, jouissance du logement, modalités relatives aux enfants) ; PAR CES MOTIFS. Le prompt souligne le standard probatoire allégé propre à l'ordonnance de protection.
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
- [ ] **CA1** — `JafOrdonnanceProtectionRequerantPromptProvider` enregistré pour `(DROIT_FAMILLE, FRANCE, JAF, ORDONNANCE_PROTECTION, REQUERANT)`.
- [ ] **CA2** — le prompt produit une requête en ordonnance de protection (JAF, art. 515-9 et s. code civil, vraisemblance des violences et du danger, mesures de protection).
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
- Backend (neuf) : `JafOrdonnanceProtectionRequerantPromptProvider` (`@Component`). Frontend : aucun.

## Plan de test
- [ ] `JafOrdonnanceProtectionRequerantPromptProviderTest` : le prompt cible le JAF, l'ordonnance de protection (art. 515-9 et s. code civil), la vraisemblance des violences et du danger.
- [x] `ConclusionPromptRegistryTest` (data-driven, SF-98-02) couvre automatiquement la cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

## Analyse d'impact
- [x] **Aucune préoccupation transversale**. Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre. F-243 (done) — codes `JAF` / `ORDONNANCE_PROTECTION` / `REQUERANT`.

## Notes et décisions
- L'ordonnance de protection a un standard probatoire propre (vraisemblance, non preuve) — le prompt l'explicite.
