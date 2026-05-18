# Mini-spec — F-98 / SF-98-18 — Requête recours OQTF (TA) — requérant FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section immigration FR) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` (SF-98-02).

## Identifiant
`F-98 / SF-98-18`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`done` — livrée 2026-05-18.

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-18-backend-ta-oqtf` — **SF backend pure**.

## Objectif
Générer le projet de **requête en annulation** d'une OQTF devant le tribunal administratif, côté requérant, droit de l'immigration FR.

## Comportement attendu
### Cas nominal
1. Dossier immigration, workspace FR, stade procédural = Tribunal administratif (`TA`) / Recours OQTF (`RECOURS_OQTF`) / Requérant (`REQUERANT`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `TaOqtfRequerantPromptProvider` — projet de **requête en annulation** devant le tribunal administratif contre une obligation de quitter le territoire français (OQTF) et ses décisions accessoires (refus de séjour, délai de départ volontaire, interdiction de retour, pays de destination). Ancrage : **CESEDA** (L.611-1 et s.), **code de justice administrative**. Structure : exposé des faits et de la procédure ; recevabilité (délai de recours) ; **DISCUSSION** — moyens de **légalité externe** (incompétence, vice de procédure, défaut de motivation) puis de **légalité interne** (erreur de droit, erreur manifeste d'appréciation, atteinte disproportionnée au droit au respect de la vie privée et familiale — art. 8 CEDH) ; **PAR CES MOTIFS** (annuler l'OQTF et les décisions accessoires).
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
- [ ] **CA1** — `TaOqtfRequerantPromptProvider` enregistré pour `(DROIT_IMMIGRATION, FRANCE, TA, RECOURS_OQTF, REQUERANT)`.
- [ ] **CA2** — le prompt produit une requête de contentieux administratif (TA, CESEDA, légalité externe / interne, annulation de l'OQTF).
- [ ] **CA3** — la garde `COMBINATION_NOT_SUPPORTED` accepte cette combinaison.
- [ ] **CA4** — le prompt cite les pièces par numéro (invariants F-98 préservés).
- [ ] **CA5** — isolation workspace inchangée ; consigne de style SF-98-47 appliquée par-dessus.

## Périmètre
### Hors scope
- Les autres cellules de la matrice — SF distinctes. Tout changement frontend.
- La gestion des délais de recours OQTF (48 h / 15 j / 30 j) — hors F-98.

## Technique
### Contrat API / Tables / Migration
Inchangé / aucune / **non applicable**.
### Composants
- Backend (neuf) : `TaOqtfRequerantPromptProvider` (`@Component`). Frontend : aucun.

## Plan de test
- [ ] `TaOqtfRequerantPromptProviderTest` : le prompt cible le TA, le CESEDA, les moyens de légalité externe/interne, l'annulation de l'OQTF.
- [x] `ConclusionPromptRegistryTest` (data-driven, SF-98-02) couvre automatiquement la cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

## Analyse d'impact
- [x] **Aucune préoccupation transversale**. Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre `ConclusionPromptProvider`. F-243 (done) — codes `TA` / `RECOURS_OQTF` / `REQUERANT`.

## Notes et décisions
- Contentieux **administratif** : le document est une **requête** (et non des « conclusions » au sens judiciaire) — le prompt l'explicite.
