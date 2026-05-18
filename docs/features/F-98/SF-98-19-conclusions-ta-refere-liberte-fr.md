# Mini-spec — F-98 / SF-98-19 — Requête en référé-liberté (TA) — requérant FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section immigration FR) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` (SF-98-02).

## Identifiant
`F-98 / SF-98-19`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-19-backend-ta-refere-liberte` — **SF backend pure**.

## Objectif
Générer le projet de **requête en référé-liberté** (art. L.521-2 CJA) devant le tribunal administratif, côté requérant, droit de l'immigration FR.

## Comportement attendu
### Cas nominal
1. Dossier immigration, workspace FR, stade procédural = Tribunal administratif (`TA`) / Référé-liberté (`REFERE_LIBERTE`) / Requérant (`REQUERANT`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `TaRefereLiberteRequerantPromptProvider` — projet de **requête en référé-liberté** fondée sur l'**article L.521-2 du code de justice administrative** : le juge des référés peut ordonner toute mesure nécessaire à la sauvegarde d'une **liberté fondamentale** à laquelle l'administration porte une **atteinte grave et manifestement illégale**, en cas d'**urgence**. Structure : exposé des faits ; **URGENCE** (caractérisée, particulière au référé-liberté) ; **LIBERTÉ FONDAMENTALE** en cause et **atteinte grave et manifestement illégale** ; mesures sollicitées ; PAR CES MOTIFS.
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
- [ ] **CA1** — `TaRefereLiberteRequerantPromptProvider` enregistré pour `(DROIT_IMMIGRATION, FRANCE, TA, REFERE_LIBERTE, REQUERANT)`.
- [ ] **CA2** — le prompt produit une requête en référé-liberté (art. L.521-2 CJA, urgence, liberté fondamentale, atteinte grave et manifestement illégale).
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
- Backend (neuf) : `TaRefereLiberteRequerantPromptProvider` (`@Component`). Frontend : aucun.

## Plan de test
- [ ] `TaRefereLiberteRequerantPromptProviderTest` : le prompt cible l'art. L.521-2 CJA, l'urgence, la liberté fondamentale et l'atteinte grave et manifestement illégale.
- [x] `ConclusionPromptRegistryTest` (data-driven, SF-98-02) couvre automatiquement la cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

## Analyse d'impact
- [x] **Aucune préoccupation transversale**. Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre `ConclusionPromptProvider`. F-243 (done) — codes `TA` / `REFERE_LIBERTE` / `REQUERANT`.

## Notes et décisions
- Le référé-liberté a un standard probatoire propre (atteinte **grave et manifestement illégale**) — prompt distinct du référé-suspension (SF-98-20).
