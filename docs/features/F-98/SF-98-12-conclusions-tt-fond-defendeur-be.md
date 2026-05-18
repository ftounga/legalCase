# Mini-spec — F-98 / SF-98-12 — Conclusions tribunal du travail (fond) — défendeur BE

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section droit du travail BE) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` livré par SF-98-02.

## Identifiant
`F-98 / SF-98-12`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`done` — livrée 2026-05-18.

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-12-backend-tt-fond-defendeur` — **SF backend pure**.

---

## Objectif
Générer le projet de conclusions **en défense** au fond devant le **tribunal du travail belge**, côté défendeur (employeur), droit du travail BE.

---

## Comportement attendu

### Cas nominal
1. Dossier droit du travail, workspace **BE**, stade procédural = Tribunal du travail (`TT`) / Fond (`FOND`) / Défendeur (`DEFENDEUR`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `TtFondDefendeurPromptProvider` — **conclusions belges en défense** : rôle « avocat du défendeur (employeur) devant le tribunal du travail » ; procédure du **Code judiciaire** (art. 740 et s.) ; droit applicable = **loi du 3 juillet 1978 relative aux contrats de travail**, **CCT n° 109**, conventions collectives. Structure : en-tête (POUR [défendeur / employeur] / CONTRE [demandeur / travailleur]) ; EXPOSÉ DES FAITS ; RECEVABILITÉ ET COMPÉTENCE ; DISCUSSION (réfutation moyen par moyen des demandes du travailleur — régularité et motivation du congé, caractère non manifestement déraisonnable du licenciement, contestation du quantum) ; **DISPOSITIF** « PAR CES MOTIFS, plaise au Tribunal du travail de déclarer les demandes non fondées… ; subsidiairement, réduire les sommes » ; inventaire des pièces.
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
- [ ] **CA1** — `TtFondDefendeurPromptProvider` enregistré pour `(DROIT_DU_TRAVAIL, BELGIQUE, TT, FOND, DEFENDEUR)`.
- [ ] **CA2** — le prompt produit des conclusions **belges en défense** (tribunal du travail, Code judiciaire, loi du 3 juillet 1978) — **aucune** référence au droit français.
- [ ] **CA3** — la garde `COMBINATION_NOT_SUPPORTED` accepte cette combinaison BE.
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
- Backend (neuf) : `TtFondDefendeurPromptProvider` (`@Component`).
- Frontend : aucun.

---

## Plan de test
### Backend (UT + IT)
- [ ] `TtFondDefendeurPromptProviderTest` : le prompt cible le tribunal du travail belge, rôle défendeur (employeur), dispositif de défense « déclarer les demandes non fondées ».
- [x] Le test data-driven `ConclusionPromptRegistryTest` (SF-98-02) couvre automatiquement la nouvelle cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

---

## Analyse d'impact
- [x] **Aucune préoccupation transversale** — ajout additif d'un provider.
- [x] Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre `ConclusionPromptProvider`.
- F-243 (done) — `ProcedureStageCatalog` fournit les codes BE `TT` / `FOND` / `DEFENDEUR`.

## Notes et décisions
- Cellule miroir de SF-98-11 côté défense ; couverture à partir des sources de procédure belges, pas un miroir du droit français.
