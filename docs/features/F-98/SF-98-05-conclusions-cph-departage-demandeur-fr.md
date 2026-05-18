# Mini-spec — F-98 / SF-98-05 — Conclusions CPH audience de départage — demandeur FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Dépend du refactor `ConclusionPromptProvider` livré par **SF-98-02**.

## Identifiant
`F-98 / SF-98-05`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`done` — livrée 2026-05-18.

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-05-backend-cph-departage-demandeur` — **SF backend pure**.

---

## Objectif
Générer le projet de conclusions devant l'**audience de départage** du Conseil de prud'hommes (juge départiteur), côté demandeur, droit du travail FR.

---

## Comportement attendu

### Cas nominal
1. Dossier droit du travail FR, stade procédural = CPH / **audience de départage** / demandeur.
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `CphDepartageDemandeurPromptProvider` : conclusions au fond **devant le juge départiteur** (art. L.1454-2 C. trav. — audience tenue après partage de voix du bureau de jugement, présidée par un magistrat du tribunal judiciaire). Structure identique aux conclusions au fond : en-tête `POUR / CONTRE`, `FAITS ET PROCÉDURE` (mentionnant le renvoi en départage), `DISCUSSION` (moyens en droit, un paragraphe par moyen), `PAR CES MOTIFS` (dispositif avec demandes chiffrées). Le prompt précise que l'audience est tenue en formation de départage.
4. La version générée passe `DONE` — exploitable comme toute version.

### Cas d'erreur / dégradation
| Situation | Comportement |
|---|---|
| Combinaison hors registre | `409 COMBINATION_NOT_SUPPORTED` |
| Gardes `STAGE_NOT_SET` / `ANALYSIS_NOT_READY` / `ALREADY_GENERATING` | inchangées |

---

## Analyse de cohérence transversale
- [x] Outil **non décisionnel** — générateur de document.
- [x] Ajout purement additif : un `@Component ConclusionPromptProvider`, aucun fichier partagé modifié.
- [x] Préoccupations transversales : **aucune**.

## Conformité F-IA-04
- [x] **Non applicable** — générateur de document.

---

## Critères d'acceptation
- [ ] **CA1** — `CphDepartageDemandeurPromptProvider` enregistré pour `(DROIT_DU_TRAVAIL, FRANCE, CPH, DEPARTAGE, DEMANDEUR)`.
- [ ] **CA2** — un dossier CPH/DEPARTAGE/DEMANDEUR génère des conclusions au fond mentionnant la **formation de départage**.
- [ ] **CA3** — la garde `COMBINATION_NOT_SUPPORTED` accepte désormais cette combinaison.
- [ ] **CA4** — le prompt cite les pièces par numéro et reprend les montants des calculs (invariants F-98 préservés).
- [ ] **CA5** — isolation workspace inchangée ; consigne de style SF-98-47 appliquée par-dessus.

---

## Périmètre
### Hors scope
- Les autres cellules de la matrice — SF distinctes.
- Tout changement frontend.

---

## Technique
### Contrat API
Inchangé.
### Tables impactées
Aucune.
### Migration Liquibase
- [ ] **Non applicable**.
### Composants
- Backend (neuf) : `CphDepartageDemandeurPromptProvider` (`@Component`).
- Frontend : aucun.

---

## Plan de test
### Backend (UT + IT)
- [ ] `CphDepartageDemandeurPromptProviderTest` : le prompt mentionne la formation de départage, rôle demandeur, structure au fond.
- [ ] `CaseConclusionControllerIT` : un dossier CPH/DEPARTAGE/DEMANDEUR n'est plus rejeté `COMBINATION_NOT_SUPPORTED`.
- [x] Le test data-driven `ConclusionPromptRegistryTest` (SF-98-02) couvre automatiquement la nouvelle cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

---

## Analyse d'impact
- [x] **Aucune préoccupation transversale** — ajout additif d'un provider.
- [x] Aucun smoke test E2E concerné.

## Dépendances
- **SF-98-02** (refactor `ConclusionPromptProvider`) — doit être mergée avant.
- F-243 (done) — codes `CPH` / `DEPARTAGE` / `DEMANDEUR` déjà au catalogue.

## Notes et décisions
- Le départage = même type de document que le fond, devant une formation présidée par un magistrat ; cellule distincte car le prompt système doit mentionner ce contexte procédural particulier.
