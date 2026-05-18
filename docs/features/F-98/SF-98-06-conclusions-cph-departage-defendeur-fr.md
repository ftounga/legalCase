# Mini-spec — F-98 / SF-98-06 — Conclusions CPH audience de départage — défendeur FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Dépend du refactor `ConclusionPromptProvider` livré par **SF-98-02**.

## Identifiant
`F-98 / SF-98-06`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`done` — livrée 2026-05-18.

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-06-backend-cph-departage-defendeur` — **SF backend pure**.

---

## Objectif
Générer le projet de conclusions **en défense** devant l'audience de départage du Conseil de prud'hommes (juge départiteur), côté défendeur (employeur), droit du travail FR.

---

## Comportement attendu

### Cas nominal
1. Dossier droit du travail FR, stade procédural = CPH / **audience de départage** / défendeur.
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `CphDepartageDefendeurPromptProvider` : conclusions **en défense** au fond, devant le juge départiteur (art. L.1454-2 C. trav.). Structure : en-tête `POUR [employeur] / CONTRE [salarié]`, `FAITS ET PROCÉDURE` (mentionnant le renvoi en départage), `DISCUSSION` (réfutation moyen par moyen des demandes du salarié), `PAR CES MOTIFS` (débouter le demandeur ; subsidiairement réduire les sommes). Le prompt précise que l'audience est tenue en formation de départage.
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
- [ ] **CA1** — `CphDepartageDefendeurPromptProvider` enregistré pour `(DROIT_DU_TRAVAIL, FRANCE, CPH, DEPARTAGE, DEFENDEUR)`.
- [ ] **CA2** — un dossier CPH/DEPARTAGE/DEFENDEUR génère des conclusions **en défense** mentionnant la formation de départage.
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
- Backend (neuf) : `CphDepartageDefendeurPromptProvider` (`@Component`).
- Frontend : aucun.

---

## Plan de test
### Backend (UT + IT)
- [ ] `CphDepartageDefendeurPromptProviderTest` : le prompt mentionne la formation de départage, rôle défendeur, dispositif « débouter ».
- [ ] `CaseConclusionControllerIT` : un dossier CPH/DEPARTAGE/DEFENDEUR n'est plus rejeté `COMBINATION_NOT_SUPPORTED`.
- [x] Le test data-driven `ConclusionPromptRegistryTest` (SF-98-02) couvre automatiquement la nouvelle cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

---

## Analyse d'impact
- [x] **Aucune préoccupation transversale** — ajout additif d'un provider.
- [x] Aucun smoke test E2E concerné.

## Dépendances
- **SF-98-02** (refactor `ConclusionPromptProvider`) — doit être mergée avant.
- F-243 (done) — codes `CPH` / `DEPARTAGE` / `DEFENDEUR` déjà au catalogue.

## Notes et décisions
- Cellule miroir de SF-98-05 côté défense ; départage = type de document du fond, dans le contexte procédural de la formation de départage.
