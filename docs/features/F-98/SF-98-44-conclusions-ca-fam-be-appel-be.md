# Mini-spec — F-98 / SF-98-44 — Conclusions d'appel chambre de la famille — appelant + intimé BE

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section droit de la famille BE) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` livré par SF-98-02.

## Identifiant
`F-98 / SF-98-44`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-44-backend-ca-fam-be-appel` — **SF backend pure**.

---

## Objectif
Générer le projet de conclusions d'**appel** devant la **cour d'appel — chambre de la famille belge** — côté appelant **et** côté intimé, droit de la famille BE.

---

## Décision de périmètre
La matrice `SF-98-00` prévoit une **SF unique** (SF-98-44) pour l'appel familial BE. Le catalogue procédural F-243 (`ProcedureStageCatalog`, entrée `DROIT_FAMILLE × BELGIQUE`) expose **deux positions** pour le stade `APPEL` : `APPELANT` et `INTIME`. Pour ne pas laisser de trou de couverture `COMBINATION_NOT_SUPPORTED`, **SF-98-44 livre les deux providers** — en une seule SF / une seule branche / une seule PR.

---

## Comportement attendu

### Cas nominal
1. Dossier droit de la famille, workspace **BE**, stade procédural = Cour d'appel chambre de la famille (`CA_FAM_BE`) / Appel (`APPEL`) / `APPELANT` **ou** `INTIME`.
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via le provider de la combinaison :
   - **`CaFamBeAppelAppelantPromptProvider`** (`APPELANT`) — conclusions d'appel : rôle « avocat de l'appelant devant la chambre de la famille de la cour d'appel » ; procédure du **Code judiciaire** (appel art. 1050 et s. ; délai d'un mois art. 1051 ; forme des conclusions et conclusions de synthèse art. 748bis ; effet dévolutif). Structure : EXPOSÉ DES FAITS ET DE LA PROCÉDURE (incluant le jugement entrepris du tribunal de la famille) ; RECEVABILITÉ DE L'APPEL ; DISCUSSION (critique du jugement chef par chef — griefs sur le divorce, l'autorité parentale, l'hébergement ou les contributions alimentaires) ; **DISPOSITIF** « PAR CES MOTIFS, plaise à la Cour de… mettre à néant le jugement entrepris en ce qu'il… ; statuant à nouveau… ».
   - **`CaFamBeAppelIntimePromptProvider`** (`INTIME`) — conclusions d'intimé : réfutation des griefs de l'appelant, orientation **confirmation du jugement entrepris**, et le cas échéant **appel incident**. Dispositif « PAR CES MOTIFS, plaise à la Cour de… confirmer le jugement entrepris ; sur appel incident, mettre à néant en ce qu'il… ».
   Droit applicable dans les deux cas : **Code civil belge** (divorce, autorité parentale et hébergement, contributions alimentaires). Inventaire des pièces.
4. La version générée passe `DONE`.

### Cas d'erreur / dégradation
| Situation | Comportement |
|---|---|
| Combinaison hors registre | `409 COMBINATION_NOT_SUPPORTED` |
| Gardes `STAGE_NOT_SET` / `ANALYSIS_NOT_READY` / `ALREADY_GENERATING` | inchangées |

---

## Analyse de cohérence transversale
- [x] Outil **non décisionnel** — générateur de document.
- [x] Ajout purement additif : deux `@Component ConclusionPromptProvider`, aucun fichier partagé modifié.
- [x] Préoccupations transversales : **aucune**.

## Conformité F-IA-04
- [x] **Non applicable** — générateur de document.

---

## Critères d'acceptation
- [ ] **CA1** — `CaFamBeAppelAppelantPromptProvider` enregistré pour `(DROIT_FAMILLE, BELGIQUE, CA_FAM_BE, APPEL, APPELANT)`.
- [ ] **CA2** — `CaFamBeAppelIntimePromptProvider` enregistré pour `(DROIT_FAMILLE, BELGIQUE, CA_FAM_BE, APPEL, INTIME)`.
- [ ] **CA3** — les prompts produisent des conclusions d'appel familial **belges** (chambre de la famille de la cour d'appel, Code judiciaire art. 1050 et s., dispositif « mettre à néant » / « confirmer le jugement entrepris ») — **aucune** référence au droit français (ni art. 954 CPC, ni « infirmer »).
- [ ] **CA4** — la garde `COMBINATION_NOT_SUPPORTED` accepte les deux combinaisons BE.
- [ ] **CA5** — les prompts citent les pièces par numéro et reprennent les montants des calculs (invariants F-98 préservés).
- [ ] **CA6** — isolation workspace inchangée ; consigne de style SF-98-47 appliquée par-dessus.

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
- Backend (neufs) : `CaFamBeAppelAppelantPromptProvider`, `CaFamBeAppelIntimePromptProvider` (`@Component`).
- Frontend : aucun.

---

## Plan de test
### Backend (UT + IT)
- [ ] `CaFamBeAppelAppelantPromptProviderTest` : conclusions d'appel côté appelant (chambre de la famille de la cour d'appel, Code judiciaire art. 1050 et s., « mettre à néant le jugement entrepris ») ; aucune référence au droit français.
- [ ] `CaFamBeAppelIntimePromptProviderTest` : conclusions d'intimé (confirmation du jugement entrepris, appel incident) ; aucune référence au droit français.
- [x] Le test data-driven `ConclusionPromptRegistryTest` (SF-98-02) couvre automatiquement les deux nouvelles cellules.
### Isolation workspace
- [x] Couverte par les contrôles existants.

---

## Analyse d'impact
- [x] **Aucune préoccupation transversale** — ajout additif de deux providers.
- [x] Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre `ConclusionPromptProvider`.
- F-243 (done) — `ProcedureStageCatalog` fournit les codes BE `CA_FAM_BE` / `APPEL` / `APPELANT` / `INTIME`.

## Notes et décisions
- Vocabulaire procédural belge — « jugement entrepris », « mettre à néant », « plaise à la Cour » — distinct du vocabulaire français. Couverture à partir des sources belges.
