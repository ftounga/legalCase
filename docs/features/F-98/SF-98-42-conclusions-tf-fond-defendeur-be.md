# Mini-spec — F-98 / SF-98-42 — Conclusions tribunal de la famille (fond) — défendeur BE

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section droit de la famille BE) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` livré par SF-98-02.

## Identifiant
`F-98 / SF-98-42`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`done` — livrée 2026-05-18.

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-42-backend-tf-fond-defendeur` — **SF backend pure**.

---

## Objectif
Générer le projet de conclusions au fond devant le **tribunal de la famille belge**, côté défendeur, droit de la famille BE.

---

## Comportement attendu

### Cas nominal
1. Dossier droit de la famille, workspace **BE**, stade procédural = Tribunal de la famille (`TF`) / Fond (`FOND`) / Défendeur (`DEFENDEUR`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `TfFondDefendeurPromptProvider` — **conclusions belges** : rôle « avocat du défendeur devant le tribunal de la famille » ; procédure du **Code judiciaire** (compétences du tribunal de la famille art. 572bis ; procédure art. 1253ter et s. ; conclusions de synthèse art. 748bis) ; droit applicable = **Code civil belge** (divorce pour désunion irrémédiable art. 229, autorité parentale et hébergement art. 373-374, contributions alimentaires art. 203 et 203bis, pension après divorce art. 301). Posture du défendeur : **réfutation chef par chef** des demandes du demandeur, contestation de la mesure d'hébergement ou du montant des contributions sollicitées, **demandes reconventionnelles** le cas échéant (hébergement principal, contribution alimentaire à charge du demandeur). Structure des conclusions belges : en-tête (POUR [défendeur] / CONTRE [demandeur]) ; EXPOSÉ DES FAITS (version du défendeur) ; RECEVABILITÉ ET COMPÉTENCE ; DISCUSSION (réfutation + moyens reconventionnels) ; **DISPOSITIF** « PAR CES MOTIFS, plaise au Tribunal de la famille de… » (débouter / reconventionnel chiffré) ; inventaire des pièces.
4. La version générée passe `DONE`.

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
- [ ] **CA1** — `TfFondDefendeurPromptProvider` enregistré pour `(DROIT_FAMILLE, BELGIQUE, TF, FOND, DEFENDEUR)`.
- [ ] **CA2** — le prompt produit des conclusions familiales **belges** côté défendeur (réfutation chef par chef, demandes reconventionnelles, tribunal de la famille, Code judiciaire) — **aucune** référence au droit français.
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
- Backend (neuf) : `TfFondDefendeurPromptProvider` (`@Component`).
- Frontend : aucun.

---

## Plan de test
### Backend (UT + IT)
- [ ] `TfFondDefendeurPromptProviderTest` : le prompt cible le tribunal de la famille belge côté défendeur (réfutation, reconventionnel, Code judiciaire, Code civil belge) ; aucune référence au droit français.
- [x] Le test data-driven `ConclusionPromptRegistryTest` (SF-98-02) couvre automatiquement la nouvelle cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

---

## Analyse d'impact
- [x] **Aucune préoccupation transversale** — ajout additif d'un provider.
- [x] Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre `ConclusionPromptProvider`.
- F-243 (done) — `ProcedureStageCatalog` fournit les codes BE `TF` / `FOND` / `DEFENDEUR`.

## Notes et décisions
- Cellule miroir de SF-98-41 côté défendeur. Couverture à partir des sources belges, pas un miroir du droit français.
