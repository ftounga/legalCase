# Mini-spec — F-98 / SF-98-15 — Conclusions d'appel cour du travail — appelant BE

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section droit du travail BE) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` livré par SF-98-02.

## Identifiant
`F-98 / SF-98-15`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-15-backend-ct-appel-appelant` — **SF backend pure**.

---

## Objectif
Générer le projet de conclusions d'**appel** côté **appelant** devant la **cour du travail belge**, droit du travail BE.

---

## Comportement attendu

### Cas nominal
1. Dossier droit du travail, workspace **BE**, stade procédural = Cour du travail (`CT`) / Appel (`APPEL`) / Appelant (`APPELANT`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `CtAppelAppelantPromptProvider` — **conclusions d'appel belges** : rôle « avocat de l'appelant devant la cour du travail » ; procédure d'appel du **Code judiciaire** (art. 1050 et s. — appel des jugements du tribunal du travail ; effet dévolutif). Structure : en-tête (POUR [appelant] / CONTRE [intimé]) ; EXPOSÉ DES FAITS ET DE LA PROCÉDURE (incluant le jugement entrepris du tribunal du travail) ; DISCUSSION = **critique du jugement entrepris** (griefs d'appel, moyens de réformation) ; **DISPOSITIF** « PAR CES MOTIFS, plaise à la Cour du travail de **mettre à néant / réformer** le jugement entrepris en ce qu'il… ; statuant à nouveau… » ; inventaire des pièces.
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
- [ ] **CA1** — `CtAppelAppelantPromptProvider` enregistré pour `(DROIT_DU_TRAVAIL, BELGIQUE, CT, APPEL, APPELANT)`.
- [ ] **CA2** — le prompt produit des conclusions d'**appel belges** (cour du travail, Code judiciaire art. 1050 et s., critique du jugement entrepris, réformation) — **aucune** référence au droit français (ni art. 954 CPC, ni cour d'appel chambre sociale).
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
- Backend (neuf) : `CtAppelAppelantPromptProvider` (`@Component`).
- Frontend : aucun.

---

## Plan de test
### Backend (UT + IT)
- [ ] `CtAppelAppelantPromptProviderTest` : le prompt cible l'appel devant la cour du travail belge (jugement entrepris, réformation), rôle appelant.
- [x] Le test data-driven `ConclusionPromptRegistryTest` (SF-98-02) couvre automatiquement la nouvelle cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

---

## Analyse d'impact
- [x] **Aucune préoccupation transversale** — ajout additif d'un provider.
- [x] Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre `ConclusionPromptProvider`.
- F-243 (done) — `ProcedureStageCatalog` fournit les codes BE `CT` / `APPEL` / `APPELANT`.

## Notes et décisions
- L'appel belge se porte devant la **cour du travail** (juridiction distincte) et obéit au Code judiciaire — couverture à partir des sources de procédure belges, pas un miroir de l'appel français.
