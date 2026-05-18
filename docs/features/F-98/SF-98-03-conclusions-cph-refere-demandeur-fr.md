# Mini-spec — F-98 / SF-98-03 — Conclusions CPH référé — demandeur FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage (workflow et écran identiques à SF-98-01). Dépend du refactor `ConclusionPromptProvider` livré par **SF-98-02**.

## Identifiant
`F-98 / SF-98-03`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`done` — livrée 2026-05-18.

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-03-backend-cph-refere-demandeur` — **SF backend pure**.

---

## Objectif
Générer le projet de conclusions de **référé prud'homal** côté demandeur, devant la formation de référé du Conseil de prud'hommes, droit du travail FR.

---

## Comportement attendu

### Cas nominal
1. Dossier droit du travail FR, stade procédural = CPH / **référé prud'homal** / demandeur.
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `CphRefereDemandeurPromptProvider` : conclusions de **référé** — en-tête `POUR [demandeur] / CONTRE [défendeur]`, `FAITS ET PROCÉDURE`, `DISCUSSION` articulée sur les fondements du référé prud'homal (art. R.1455-5 à R.1455-7 C. trav.) : **absence de contestation sérieuse**, **trouble manifestement illicite**, **mesures conservatoires / de remise en état**, **demande de provision** ; `PAR CES MOTIFS` (demandes typiques : remise sous astreinte des documents de fin de contrat — bulletins, certificat de travail, attestation France Travail —, provision sur rappels de salaire).
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
- [ ] **CA1** — `CphRefereDemandeurPromptProvider` enregistré pour `(DROIT_DU_TRAVAIL, FRANCE, CPH, REFERE, DEMANDEUR)`.
- [ ] **CA2** — un dossier CPH/REFERE/DEMANDEUR génère des conclusions de **référé** (fondements : contestation sérieuse, trouble manifestement illicite, provision).
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
- Backend (neuf) : `CphRefereDemandeurPromptProvider` (`@Component`).
- Frontend : aucun.

---

## Plan de test
### Backend (UT + IT)
- [ ] `CphRefereDemandeurPromptProviderTest` : le prompt cible le référé prud'homal (contestation sérieuse / trouble manifestement illicite / provision), rôle demandeur.
- [ ] `CaseConclusionControllerIT` : un dossier CPH/REFERE/DEMANDEUR n'est plus rejeté `COMBINATION_NOT_SUPPORTED`.
- [x] Le test data-driven `ConclusionPromptRegistryTest` (SF-98-02) couvre automatiquement la nouvelle cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

---

## Analyse d'impact
- [x] **Aucune préoccupation transversale** — ajout additif d'un provider.
- [x] Aucun smoke test E2E concerné.

## Dépendances
- **SF-98-02** (refactor `ConclusionPromptProvider`) — doit être mergée avant.
- F-243 (done) — codes `CPH` / `REFERE` / `DEMANDEUR` déjà au catalogue.

## Notes et décisions
- Le référé prud'homal a une logique procédurale propre (urgence, pas de jugement au fond) — le prompt système est distinct de celui du fond, d'où une cellule dédiée.
