# Mini-spec — F-98 / SF-98-16 — Conclusions d'appel cour du travail — intimé BE

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section droit du travail BE) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` livré par SF-98-02.

## Identifiant
`F-98 / SF-98-16`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`done` — livrée 2026-05-18.

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-16-backend-ct-appel-intime` — **SF backend pure**.

---

## Objectif
Générer le projet de conclusions d'**appel** côté **intimé** devant la **cour du travail belge**, droit du travail BE.

---

## Comportement attendu

### Cas nominal
1. Dossier droit du travail, workspace **BE**, stade procédural = Cour du travail (`CT`) / Appel (`APPEL`) / Intimé (`INTIME`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `CtAppelIntimePromptProvider` — **conclusions d'intimé belges** : rôle « avocat de l'intimé devant la cour du travail » ; procédure d'appel du **Code judiciaire** (art. 1050 et s.). Structure : en-tête (POUR [intimé] / CONTRE [appelant]) ; EXPOSÉ DES FAITS ET DE LA PROCÉDURE (incluant le jugement entrepris) ; DISCUSSION orientée **confirmation du jugement entrepris** (réfutation des griefs d'appel) et, le cas échéant, **appel incident** ; **DISPOSITIF** « PAR CES MOTIFS, plaise à la Cour du travail de **confirmer** le jugement entrepris… ; sur appel incident, de **mettre à néant / réformer** en ce qu'il… » ; inventaire des pièces.
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
- [ ] **CA1** — `CtAppelIntimePromptProvider` enregistré pour `(DROIT_DU_TRAVAIL, BELGIQUE, CT, APPEL, INTIME)`.
- [ ] **CA2** — le prompt produit des conclusions d'**intimé belges** (cour du travail, confirmation du jugement entrepris, appel incident) — **aucune** référence au droit français.
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
- Backend (neuf) : `CtAppelIntimePromptProvider` (`@Component`).
- Frontend : aucun.

---

## Plan de test
### Backend (UT + IT)
- [ ] `CtAppelIntimePromptProviderTest` : le prompt cible l'appel devant la cour du travail belge côté intimé (confirmation du jugement entrepris, appel incident).
- [x] Le test data-driven `ConclusionPromptRegistryTest` (SF-98-02) couvre automatiquement la nouvelle cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

---

## Analyse d'impact
- [x] **Aucune préoccupation transversale** — ajout additif d'un provider.
- [x] Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre `ConclusionPromptProvider`.
- F-243 (done) — `ProcedureStageCatalog` fournit les codes BE `CT` / `APPEL` / `INTIME`.

## Notes et décisions
- Cellule miroir de SF-98-15 côté intimé ; couverture à partir des sources de procédure belges (cour du travail, Code judiciaire).
