# Mini-spec — F-98 / SF-98-24 — Mémoire d'admission au séjour (préfecture) — demandeur de titre FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section immigration FR) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` (SF-98-02).

## Identifiant
`F-98 / SF-98-24`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`done` — livrée 2026-05-18.

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-24-backend-prefecture-titre` — **SF backend pure**.

## Objectif
Générer le projet de **mémoire d'admission au séjour** adressé à la préfecture (hors contentieux), côté demandeur de titre, droit de l'immigration FR.

## Comportement attendu
### Cas nominal
1. Dossier immigration, workspace FR, stade procédural = Préfecture / OFII (`PREF`) / Demande de titre (`DEMANDE_TITRE`) / Demandeur de titre (`DEMANDEUR_TITRE`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `PrefDemandeTitrePromptProvider` — projet de **mémoire / note de soutien** adressé à la préfecture à l'appui d'une **demande de titre de séjour** (hors contentieux — l'administration n'a pas encore statué). Ancrage : **CESEDA** (catégories de titres : vie privée et familiale, salarié, étudiant, admission exceptionnelle au séjour…). Structure : identification du demandeur et de la demande ; exposé de la situation personnelle, familiale et professionnelle ; **DISCUSSION** — fondement légal du titre sollicité, conditions remplies, éléments d'intégration et d'ancrage en France ; **DEMANDE** (délivrance du titre sollicité). Le prompt précise que le document est une **demande gracieuse / un mémoire de soutien**, non une requête contentieuse — ton argumenté mais non procédural.
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
- [ ] **CA1** — `PrefDemandeTitrePromptProvider` enregistré pour `(DROIT_IMMIGRATION, FRANCE, PREF, DEMANDE_TITRE, DEMANDEUR_TITRE)`.
- [ ] **CA2** — le prompt produit un mémoire d'admission au séjour (préfecture, CESEDA, hors contentieux, demande de titre).
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
- Backend (neuf) : `PrefDemandeTitrePromptProvider` (`@Component`). Frontend : aucun.

## Plan de test
- [ ] `PrefDemandeTitrePromptProviderTest` : le prompt cible la préfecture, le CESEDA, la demande de titre hors contentieux.
- [x] `ConclusionPromptRegistryTest` (data-driven, SF-98-02) couvre automatiquement la cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

## Analyse d'impact
- [x] **Aucune préoccupation transversale**. Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre `ConclusionPromptProvider`. F-243 (done) — codes `PREF` / `DEMANDE_TITRE` / `DEMANDEUR_TITRE`.

## Notes et décisions
- Cellule **hors contentieux** : le document est un mémoire de soutien adressé à l'administration, pas une requête juridictionnelle — le prompt adapte le registre en conséquence.
