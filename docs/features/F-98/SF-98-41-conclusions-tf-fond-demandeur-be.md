# Mini-spec — F-98 / SF-98-41 — Conclusions tribunal de la famille (fond) — demandeur BE

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section droit de la famille BE) + `SF-98-00b-ux-coherence.md` (étape 0 bis — section conclusions déjà en place). **Cellule de matrice** : pas de nouveau cadrage (workflow et écran identiques à SF-98-01). Registre `ConclusionPromptProvider` livré par SF-98-02.

## Identifiant
`F-98 / SF-98-41`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`done` — livrée 2026-05-18.

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-41-backend-tf-fond-demandeur` — **SF backend pure** (frontend combinaison-agnostique).

---

## Objectif
Générer le projet de conclusions au fond devant le **tribunal de la famille belge**, côté demandeur, droit de la famille BE.

---

## Comportement attendu

### Cas nominal
1. Dossier droit de la famille, workspace **BE**, stade procédural = Tribunal de la famille (`TF`) / Fond (`FOND`) / Demandeur (`DEMANDEUR`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `TfFondDemandeurPromptProvider` — **conclusions belges** : rôle « avocat du demandeur devant le tribunal de la famille » ; procédure du **Code judiciaire** (tribunal de la famille créé par la loi du 30 juillet 2013, intégré au tribunal de première instance ; compétences art. 572bis ; procédure art. 1253ter et s. ; forme des conclusions art. 743-748, conclusions de synthèse art. 748bis) ; droit applicable = **Code civil belge** — divorce pour désunion irrémédiable (art. 229), autorité parentale conjointe et hébergement (art. 373-374, hébergement égalitaire privilégié — loi du 18 juillet 2006), contribution à l'entretien et à l'éducation des enfants (art. 203 et 203bis), pension alimentaire entre ex-époux (art. 301). Structure des conclusions belges : en-tête (POUR [demandeur] / CONTRE [défendeur]) ; EXPOSÉ DES FAITS ; RECEVABILITÉ ET COMPÉTENCE ; DISCUSSION (moyens en droit — un paragraphe argumenté par chef de demande : divorce, autorité parentale, hébergement, contributions alimentaires) ; **DISPOSITIF** introduit par « PAR CES MOTIFS, plaise au Tribunal de la famille de… » (dispositif chiffré pour les contributions alimentaires) ; inventaire des pièces.
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
- [ ] **CA1** — `TfFondDemandeurPromptProvider` enregistré pour `(DROIT_FAMILLE, BELGIQUE, TF, FOND, DEMANDEUR)`.
- [ ] **CA2** — le prompt produit des conclusions familiales **belges** (tribunal de la famille, Code judiciaire, Code civil belge) — **aucune** référence au droit français (ni JAF, ni code de procédure civile français).
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
- Backend (neuf) : `TfFondDemandeurPromptProvider` (`@Component`).
- Frontend : aucun.

---

## Plan de test
### Backend (UT + IT)
- [ ] `TfFondDemandeurPromptProviderTest` : le prompt cible le tribunal de la famille belge, le Code judiciaire et le Code civil belge, rôle demandeur, dispositif « plaise au Tribunal de la famille » ; aucune référence au droit français.
- [x] Le test data-driven `ConclusionPromptRegistryTest` (SF-98-02) couvre automatiquement la nouvelle cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

---

## Analyse d'impact
- [x] **Aucune préoccupation transversale** — ajout additif d'un provider.
- [x] Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre `ConclusionPromptProvider`.
- F-243 (done) — `ProcedureStageCatalog` fournit les codes BE `TF` / `FOND` / `DEMANDEUR`.

## Notes et décisions
- Couverture **à partir des sources de procédure belges** (Code judiciaire, Code civil belge), pas un miroir du droit français — exigence de couverture exhaustive du droit belge.
