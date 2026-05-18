# Mini-spec — F-98 / SF-98-11 — Conclusions tribunal du travail (fond) — demandeur BE

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section droit du travail BE) + `SF-98-00b-ux-coherence.md` (étape 0 bis — section conclusions déjà en place). **Cellule de matrice** : pas de nouveau cadrage (workflow et écran identiques à SF-98-01). Registre `ConclusionPromptProvider` livré par SF-98-02.

## Identifiant
`F-98 / SF-98-11`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-11-backend-tt-fond-demandeur` — **SF backend pure** (frontend combinaison-agnostique).

---

## Objectif
Générer le projet de conclusions au fond devant le **tribunal du travail belge**, côté demandeur (travailleur), droit du travail BE.

---

## Comportement attendu

### Cas nominal
1. Dossier droit du travail, workspace **BE**, stade procédural = Tribunal du travail (`TT`) / Fond (`FOND`) / Demandeur (`DEMANDEUR`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `TtFondDemandeurPromptProvider` — **conclusions belges** : rôle « avocat du demandeur (travailleur) devant le tribunal du travail » ; procédure du **Code judiciaire** (art. 578 compétence du tribunal du travail ; art. 740 et s. forme des conclusions) ; droit applicable = **loi du 3 juillet 1978 relative aux contrats de travail**, **CCT n° 109** (licenciement manifestement déraisonnable), conventions collectives de travail. Structure des conclusions belges : en-tête (POUR [demandeur] / CONTRE [défendeur]) ; EXPOSÉ DES FAITS ; RECEVABILITÉ ET COMPÉTENCE ; DISCUSSION (moyens en droit — un paragraphe argumenté par moyen) ; **DISPOSITIF** introduit par « PAR CES MOTIFS, plaise au Tribunal du travail de… » ; inventaire des pièces.
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
- [ ] **CA1** — `TtFondDemandeurPromptProvider` enregistré pour `(DROIT_DU_TRAVAIL, BELGIQUE, TT, FOND, DEMANDEUR)`.
- [ ] **CA2** — le prompt produit des conclusions **belges** (tribunal du travail, Code judiciaire, loi du 3 juillet 1978) — **aucune** référence au droit français (ni Conseil de prud'hommes, ni Code du travail français).
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
- Backend (neuf) : `TtFondDemandeurPromptProvider` (`@Component`).
- Frontend : aucun.

---

## Plan de test
### Backend (UT + IT)
- [ ] `TtFondDemandeurPromptProviderTest` : le prompt cible le tribunal du travail belge, le Code judiciaire et la loi du 3 juillet 1978, rôle demandeur (travailleur), dispositif « plaise au Tribunal du travail ».
- [x] Le test data-driven `ConclusionPromptRegistryTest` (SF-98-02) couvre automatiquement la nouvelle cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

---

## Analyse d'impact
- [x] **Aucune préoccupation transversale** — ajout additif d'un provider.
- [x] Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre `ConclusionPromptProvider`.
- F-243 (done) — `ProcedureStageCatalog` fournit les codes BE `TT` / `FOND` / `DEMANDEUR`.

## Notes et décisions
- Couverture **à partir des sources de procédure belges** (Code judiciaire, droit du travail belge), pas un miroir du droit français — exigence de couverture exhaustive du droit belge.
