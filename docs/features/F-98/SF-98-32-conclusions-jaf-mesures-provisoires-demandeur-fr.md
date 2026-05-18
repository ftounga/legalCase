# Mini-spec — F-98 / SF-98-32 — Conclusions mesures provisoires (JAF) — demandeur FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section famille FR) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` (SF-98-02).

## Identifiant
`F-98 / SF-98-32`
## Feature parente
`F-98` — Génération de courrier / conclusions
## Statut
`done` — livrée 2026-05-18.
## Date de création
2026-05-18
## Branche Git
- `feat/SF-98-32-backend-jaf-mesures-demandeur` — **SF backend pure**.

## Objectif
Générer le projet de conclusions sur **mesures provisoires** devant le juge aux affaires familiales, côté demandeur, droit de la famille FR.

## Comportement attendu
### Cas nominal
1. Dossier famille, workspace FR, stade procédural = JAF (`JAF`) / Mesures provisoires (`MESURES_PROVISOIRES`) / Demandeur (`DEMANDEUR`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `JafMesuresProvisoiresDemandeurPromptProvider` — conclusions sur les **mesures provisoires** ordonnées par le juge aux affaires familiales pendant l'instance en divorce. Ancrage : **code civil** (art. 254-255 — mesures provisoires : résidence séparée, jouissance du logement et du mobilier, pension alimentaire au titre du devoir de secours, contribution à l'entretien et à l'éducation des enfants, exercice de l'autorité parentale, résidence des enfants, droit de visite et d'hébergement). Structure : en-tête (POUR / CONTRE) ; FAITS ET PROCÉDURE ; DISCUSSION (chaque mesure provisoire sollicitée, motivée) ; PAR CES MOTIFS (dispositif chiffré). Le prompt précise que les mesures sont **provisoires** et valent pour la durée de l'instance.
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
- [ ] **CA1** — `JafMesuresProvisoiresDemandeurPromptProvider` enregistré pour `(DROIT_FAMILLE, FRANCE, JAF, MESURES_PROVISOIRES, DEMANDEUR)`.
- [ ] **CA2** — le prompt produit des conclusions sur mesures provisoires (JAF, art. 254-255 code civil, mesures provisoires de l'instance), rôle demandeur.
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
- Backend (neuf) : `JafMesuresProvisoiresDemandeurPromptProvider` (`@Component`). Frontend : aucun.

## Plan de test
- [ ] `JafMesuresProvisoiresDemandeurPromptProviderTest` : le prompt cible le JAF, les mesures provisoires (art. 254-255 code civil), rôle demandeur.
- [x] `ConclusionPromptRegistryTest` (data-driven, SF-98-02) couvre automatiquement la cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

## Analyse d'impact
- [x] **Aucune préoccupation transversale**. Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre. F-243 (done) — codes `JAF` / `MESURES_PROVISOIRES` / `DEMANDEUR`.

## Notes et décisions
- Les mesures provisoires de l'instance en divorce sont un stade procédural distinct du fond — cellule dédiée.
