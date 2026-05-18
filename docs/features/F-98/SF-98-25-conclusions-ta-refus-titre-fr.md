# Mini-spec — F-98 / SF-98-25 — Requête recours refus de titre / regroupement (TA) — requérant FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section immigration FR) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` (SF-98-02).

## Identifiant
`F-98 / SF-98-25`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`done` — livrée 2026-05-18.

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-25-backend-ta-refus-titre` — **SF backend pure**.

## Objectif
Générer le projet de **requête en annulation** d'un refus de titre de séjour ou de regroupement familial devant le tribunal administratif, côté requérant, droit de l'immigration FR.

## Comportement attendu
### Cas nominal
1. Dossier immigration, workspace FR, stade procédural = Tribunal administratif (`TA`) / Recours refus titre / regroupement (`RECOURS_TITRE`) / Requérant (`REQUERANT`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `TaRecoursTitreRequerantPromptProvider` — projet de **requête en annulation** devant le tribunal administratif contre une décision préfectorale de **refus de titre de séjour** ou de **refus de regroupement familial**. Ancrage : **CESEDA**, **code de justice administrative**. Structure : exposé des faits et de la procédure ; recevabilité (délai de recours) ; **DISCUSSION** — moyens de **légalité externe** (incompétence, vice de procédure, défaut de motivation) puis de **légalité interne** (erreur de droit sur la catégorie de titre, erreur manifeste d'appréciation, atteinte au droit au respect de la vie privée et familiale — art. 8 CEDH) ; PAR CES MOTIFS (annuler le refus et enjoindre le réexamen / la délivrance du titre).
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
- [ ] **CA1** — `TaRecoursTitreRequerantPromptProvider` enregistré pour `(DROIT_IMMIGRATION, FRANCE, TA, RECOURS_TITRE, REQUERANT)`.
- [ ] **CA2** — le prompt produit une requête contre un refus de titre / regroupement (TA, CESEDA, légalité externe / interne, injonction de réexamen).
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
- Backend (neuf) : `TaRecoursTitreRequerantPromptProvider` (`@Component`). Frontend : aucun.

## Plan de test
- [ ] `TaRecoursTitreRequerantPromptProviderTest` : le prompt cible le TA, le refus de titre / regroupement, les moyens de légalité externe/interne.
- [x] `ConclusionPromptRegistryTest` (data-driven, SF-98-02) couvre automatiquement la cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

## Analyse d'impact
- [x] **Aucune préoccupation transversale**. Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre `ConclusionPromptProvider`. F-243 (done) — codes `TA` / `RECOURS_TITRE` / `REQUERANT`.

## Notes et décisions
- Cellule distincte de SF-98-18 (OQTF) : objet du litige différent (refus de titre vs éloignement) — moyens et conclusions propres. **Solde l'immigration FR de la matrice** (SF-98-18 → SF-98-25).
