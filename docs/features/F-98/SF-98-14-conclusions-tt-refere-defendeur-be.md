# Mini-spec — F-98 / SF-98-14 — Conclusions référé tribunal du travail — défendeur BE

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section droit du travail BE) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` livré par SF-98-02.

## Identifiant
`F-98 / SF-98-14`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-14-backend-tt-refere-defendeur` — **SF backend pure**.

---

## Objectif
Générer le projet de conclusions **en défense** au référé devant le **président du tribunal du travail belge**, côté défendeur, droit du travail BE.

---

## Comportement attendu

### Cas nominal
1. Dossier droit du travail, workspace **BE**, stade procédural = Tribunal du travail (`TT`) / Référé — président (`REFERE`) / Défendeur (`DEFENDEUR`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `TtRefereDefendeurPromptProvider` — **conclusions de référé belges en défense** : rôle « avocat du défendeur devant le président du tribunal du travail siégeant en référé » ; fondement procédural = **art. 584 du Code judiciaire**. Structure : en-tête (POUR [défendeur] / CONTRE [demandeur]) ; EXPOSÉ DES FAITS ; DISCUSSION orientée défense — **contestation de l'urgence** (condition de la compétence du juge des référés), absence d'apparence de droit, le caractère sérieusement contestable de la demande relevant du juge du fond ; **DISPOSITIF** « PAR CES MOTIFS, plaise à Monsieur/Madame le Président du tribunal du travail, siégeant en référé, de déclarer la demande non fondée / d'en débouter le demandeur » ; inventaire des pièces.
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
- [ ] **CA1** — `TtRefereDefendeurPromptProvider` enregistré pour `(DROIT_DU_TRAVAIL, BELGIQUE, TT, REFERE, DEFENDEUR)`.
- [ ] **CA2** — le prompt produit des conclusions de **référé belge en défense** (président du tribunal du travail, art. 584 C. jud., contestation de l'urgence) — **aucune** référence au droit français.
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
- Backend (neuf) : `TtRefereDefendeurPromptProvider` (`@Component`).
- Frontend : aucun.

---

## Plan de test
### Backend (UT + IT)
- [ ] `TtRefereDefendeurPromptProviderTest` : le prompt cible la défense au référé devant le président du tribunal du travail (contestation de l'urgence), rôle défendeur.
- [x] Le test data-driven `ConclusionPromptRegistryTest` (SF-98-02) couvre automatiquement la nouvelle cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

---

## Analyse d'impact
- [x] **Aucune préoccupation transversale** — ajout additif d'un provider.
- [x] Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre `ConclusionPromptProvider`.
- F-243 (done) — `ProcedureStageCatalog` fournit les codes BE `TT` / `REFERE` / `DEFENDEUR`.

## Notes et décisions
- En défense au référé belge, l'axe central est la **contestation de l'urgence** — sans urgence, le juge des référés est sans pouvoir ; couverture à partir des sources de procédure belges.
