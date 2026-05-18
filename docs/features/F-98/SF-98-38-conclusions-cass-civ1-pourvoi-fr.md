# Mini-spec — F-98 / SF-98-38 — Mémoires de pourvoi Cass. 1ʳᵉ chambre civile (famille FR)

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section famille FR) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` (SF-98-02).

## Identifiant
`F-98 / SF-98-38`
## Feature parente
`F-98` — Génération de courrier / conclusions
## Statut
`ready`
## Date de création
2026-05-18
## Branche Git
- `feat/SF-98-38-backend-cass-civ1-pourvoi` — **SF backend pure**.

## Objectif
Générer le projet de **mémoire de pourvoi en cassation** devant la 1ʳᵉ chambre civile de la Cour de cassation en matière familiale — côté demandeur au pourvoi **et** côté défendeur au pourvoi.

## Décision de périmètre
La matrice `SF-98-00` prévoit une SF unique (SF-98-38) pour le pourvoi famille FR. Le catalogue F-243 expose **deux positions** (`DEMANDEUR_POURVOI`, `DEFENDEUR_POURVOI`). **SF-98-38 livre les deux providers** — stade pourvoi complet, aucun trou `COMBINATION_NOT_SUPPORTED`.

## Comportement attendu
### Cas nominal
1. Dossier famille, workspace FR, stade = Cour de cassation 1ʳᵉ chambre civile (`CASS_CIV1`) / Pourvoi (`POURVOI`) / `DEMANDEUR_POURVOI` **ou** `DEFENDEUR_POURVOI`.
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via le provider de la combinaison :
   - **`CassCiv1PourvoiDemandeurPromptProvider`** (`DEMANDEUR_POURVOI`) — projet de **mémoire ampliatif** : FAITS ET PROCÉDURE (incluant l'arrêt attaqué de la cour d'appel) ; MOYEN(S) DE CASSATION articulés en branches, énonçant le cas d'ouverture (violation de la loi, manque de base légale, défaut de motifs, dénaturation) ; PAR CES MOTIFS (casser et annuler).
   - **`CassCiv1PourvoiDefendeurPromptProvider`** (`DEFENDEUR_POURVOI`) — projet de **mémoire en défense** : réfutation moyen par moyen, irrecevabilité éventuelle d'un moyen, conclusion au rejet du pourvoi.
   Les deux précisent que la Cour de cassation **juge en droit** — pas de demandes chiffrées.
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
- [ ] **CA1** — `CassCiv1PourvoiDemandeurPromptProvider` enregistré pour `(DROIT_FAMILLE, FRANCE, CASS_CIV1, POURVOI, DEMANDEUR_POURVOI)`.
- [ ] **CA2** — `CassCiv1PourvoiDefendeurPromptProvider` enregistré pour `(DROIT_FAMILLE, FRANCE, CASS_CIV1, POURVOI, DEFENDEUR_POURVOI)`.
- [ ] **CA3** — les prompts produisent des mémoires de cassation (moyens / branches / cas d'ouverture ; mémoire en défense → rejet du pourvoi).
- [ ] **CA4** — les prompts citent les pièces par numéro ; **pas de demandes chiffrées** (juge en droit) — invariants F-98 préservés.
- [ ] **CA5** — la garde `COMBINATION_NOT_SUPPORTED` accepte les deux combinaisons ; isolation workspace inchangée ; style SF-98-47 appliqué par-dessus.

## Périmètre
### Hors scope
- Les autres cellules de la matrice — SF distinctes. Tout changement frontend.

## Technique
### Contrat API / Tables / Migration
Inchangé / aucune / **non applicable**.
### Composants
- Backend (neufs) : `CassCiv1PourvoiDemandeurPromptProvider`, `CassCiv1PourvoiDefendeurPromptProvider` (`@Component`). Frontend : aucun.

## Plan de test
- [ ] `CassCiv1PourvoiDemandeurPromptProviderTest` / `CassCiv1PourvoiDefendeurPromptProviderTest` : structure de mémoire ampliatif / en défense, pas de demandes chiffrées.
- [x] `ConclusionPromptRegistryTest` (data-driven, SF-98-02) couvre automatiquement les deux cellules.
### Isolation workspace
- [x] Couverte par les contrôles existants.

## Analyse d'impact
- [x] **Aucune préoccupation transversale**. Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre. F-243 (done) — codes `CASS_CIV1` / `POURVOI` / `DEMANDEUR_POURVOI` / `DEFENDEUR_POURVOI`.

## Notes et décisions
- Document de **pur droit** — la consigne transverse « demandes chiffrées » est neutralisée. 2 providers pour le stade pourvoi complet.
