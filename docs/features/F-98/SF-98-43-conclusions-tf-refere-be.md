# Mini-spec — F-98 / SF-98-43 — Conclusions tribunal de la famille — mesures urgentes et provisoires BE

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section droit de la famille BE) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` livré par SF-98-02.

## Identifiant
`F-98 / SF-98-43`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`done` — livrée 2026-05-18.

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-43-backend-tf-refere` — **SF backend pure**.

---

## Objectif
Générer le projet de conclusions sur **mesures urgentes et provisoires** devant le **tribunal de la famille belge** — côté demandeur **et** côté défendeur, droit de la famille BE.

---

## Décision de périmètre
La matrice `SF-98-00` prévoit une **SF unique** (SF-98-43) pour le référé / mesures urgentes BE. Le catalogue procédural F-243 (`ProcedureStageCatalog`, entrée `DROIT_FAMILLE × BELGIQUE`) expose **deux positions** pour le stade `REFERE` : `DEMANDEUR` et `DEFENDEUR`. Pour ne pas laisser de trou de couverture `COMBINATION_NOT_SUPPORTED` (exigence de couverture exhaustive du droit belge), **SF-98-43 livre les deux providers** — en une seule SF / une seule branche / une seule PR.

---

## Comportement attendu

### Cas nominal
1. Dossier droit de la famille, workspace **BE**, stade procédural = Tribunal de la famille (`TF`) / Référé — mesures urgentes (`REFERE`) / `DEMANDEUR` **ou** `DEFENDEUR`.
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via le provider de la combinaison :
   - **`TfRefereDemandeurPromptProvider`** (`DEMANDEUR`) — conclusions sur mesures urgentes : rôle « avocat du demandeur devant le tribunal de la famille statuant sur les affaires réputées urgentes » ; procédure du **Code judiciaire** (art. 1253ter/4 — affaires réputées urgentes : résidence séparée, autorité parentale, hébergement des enfants, droit aux relations personnelles, obligations alimentaires ; saisine et procédure « comme en référé »). Droit applicable = **Code civil belge** (autorité parentale et hébergement art. 373-374, contributions alimentaires art. 203 et 203bis, devoir de secours entre époux art. 213). Le prompt précise que les mesures sollicitées sont **provisoires** et valent pour la durée de l'instance.
   - **`TfRefereDefendeurPromptProvider`** (`DEFENDEUR`) — conclusions en réponse : réfutation de l'urgence et/ou des mesures sollicitées, **demandes reconventionnelles** provisoires le cas échéant.
   Structure des conclusions belges dans les deux cas : en-tête (POUR / CONTRE) ; EXPOSÉ DES FAITS ; URGENCE ET COMPÉTENCE ; DISCUSSION (chaque mesure provisoire — sollicitée ou contestée — motivée) ; **DISPOSITIF** « PAR CES MOTIFS, plaise au Tribunal de la famille de… » (dispositif chiffré pour les contributions) ; inventaire des pièces.
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
- [ ] **CA1** — `TfRefereDemandeurPromptProvider` enregistré pour `(DROIT_FAMILLE, BELGIQUE, TF, REFERE, DEMANDEUR)`.
- [ ] **CA2** — `TfRefereDefendeurPromptProvider` enregistré pour `(DROIT_FAMILLE, BELGIQUE, TF, REFERE, DEFENDEUR)`.
- [ ] **CA3** — les prompts produisent des conclusions sur mesures urgentes **belges** (tribunal de la famille, Code judiciaire art. 1253ter/4 affaires réputées urgentes, mesures provisoires) — **aucune** référence au droit français (ni JAF, ni ordonnance de non-conciliation).
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
- Backend (neufs) : `TfRefereDemandeurPromptProvider`, `TfRefereDefendeurPromptProvider` (`@Component`).
- Frontend : aucun.

---

## Plan de test
### Backend (UT + IT)
- [ ] `TfRefereDemandeurPromptProviderTest` : conclusions sur mesures urgentes côté demandeur (Code judiciaire art. 1253ter/4, mesures provisoires) ; aucune référence au droit français.
- [ ] `TfRefereDefendeurPromptProviderTest` : conclusions en réponse côté défendeur (réfutation de l'urgence, reconventionnel provisoire) ; aucune référence au droit français.
- [x] Le test data-driven `ConclusionPromptRegistryTest` (SF-98-02) couvre automatiquement les deux nouvelles cellules.
### Isolation workspace
- [x] Couverte par les contrôles existants.

---

## Analyse d'impact
- [x] **Aucune préoccupation transversale** — ajout additif de deux providers.
- [x] Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre `ConclusionPromptProvider`.
- F-243 (done) — `ProcedureStageCatalog` fournit les codes BE `TF` / `REFERE` / `DEMANDEUR` / `DEFENDEUR`.

## Notes et décisions
- Le « référé » familial belge correspond aux **affaires réputées urgentes** du tribunal de la famille (art. 1253ter/4 du Code judiciaire), pas à un juge des référés distinct — couverture à partir des sources belges, pas un miroir du droit français.
