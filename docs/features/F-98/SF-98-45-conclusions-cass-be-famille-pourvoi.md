# Mini-spec — F-98 / SF-98-45 — Mémoires de pourvoi en cassation (BE, droit de la famille)

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section droit de la famille BE) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` livré par SF-98-02.

## Identifiant
`F-98 / SF-98-45`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`done` — livrée 2026-05-18.

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-45-backend-cass-be-famille-pourvoi` — **SF backend pure**.

---

## Objectif
Générer le projet de **mémoire de pourvoi en cassation** devant la **Cour de cassation belge** en matière de droit de la famille — côté demandeur au pourvoi **et** côté défendeur au pourvoi.

---

## Décision de périmètre
La matrice `SF-98-00` prévoit une **SF unique** (SF-98-45) pour le pourvoi en cassation familial BE. Le catalogue procédural F-243 (`ProcedureStageCatalog`, entrée `DROIT_FAMILLE × BELGIQUE`) expose **deux positions** pour le stade `POURVOI` : `DEMANDEUR_POURVOI` et `DEFENDEUR_POURVOI`. Pour ne pas laisser de trou de couverture `COMBINATION_NOT_SUPPORTED` (exigence de couverture exhaustive du droit belge), **SF-98-45 livre les deux providers** — c'est le **stade pourvoi familial BE complet**, en une seule SF / une seule branche / une seule PR.

---

## Comportement attendu

### Cas nominal
1. Dossier droit de la famille, workspace **BE**, stade procédural = Cour de cassation (`CASS_BE`) / Pourvoi en cassation (`POURVOI`) / `DEMANDEUR_POURVOI` **ou** `DEFENDEUR_POURVOI`.
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via le provider de la combinaison :
   - **`CassBeFamillePourvoiDemandeurPromptProvider`** (`DEMANDEUR_POURVOI`) — projet de **mémoire à l'appui du pourvoi** : rôle « avocat du demandeur en cassation devant la Cour de cassation de Belgique » ; procédure du **Code judiciaire** (art. 1073 et s. — pourvoi en cassation). Structure : EXPOSÉ DES FAITS ET DES ANTÉCÉDENTS DE LA PROCÉDURE (incluant l'arrêt attaqué de la cour d'appel — chambre de la famille) ; MOYEN(S) DE CASSATION — chaque moyen indiquant les **dispositions légales violées** (Code civil belge, Code judiciaire) et en quoi l'arrêt attaqué les méconnaît ; conclusion à la **cassation** de l'arrêt attaqué.
   - **`CassBeFamillePourvoiDefendeurPromptProvider`** (`DEFENDEUR_POURVOI`) — projet de **mémoire en réponse** : rôle « avocat du défendeur en cassation » ; réfutation moyen par moyen, possibilité d'opposer l'**irrecevabilité** d'un moyen (nouveau, imprécis, mélangé de fait et de droit) ; conclusion au **rejet du pourvoi**.
   Les deux précisent que la Cour de cassation **juge en droit** — pas de réappréciation des faits, pas de demandes chiffrées.
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
- [ ] **CA1** — `CassBeFamillePourvoiDemandeurPromptProvider` enregistré pour `(DROIT_FAMILLE, BELGIQUE, CASS_BE, POURVOI, DEMANDEUR_POURVOI)`.
- [ ] **CA2** — `CassBeFamillePourvoiDefendeurPromptProvider` enregistré pour `(DROIT_FAMILLE, BELGIQUE, CASS_BE, POURVOI, DEFENDEUR_POURVOI)`.
- [ ] **CA3** — les prompts produisent des mémoires de cassation **belges** (Cour de cassation de Belgique, Code judiciaire art. 1073 et s., moyens de cassation, dispositions légales violées) — **aucune** référence au droit français (ni Cour de cassation chambre civile, ni mémoire ampliatif).
- [ ] **CA4** — les prompts citent les pièces par numéro ; ils **n'inventent pas de demandes chiffrées** (la Cour juge en droit) — invariants F-98 préservés.
- [ ] **CA5** — la garde `COMBINATION_NOT_SUPPORTED` accepte les deux combinaisons BE de pourvoi.
- [ ] **CA6** — isolation workspace inchangée ; consigne de style SF-98-47 appliquée par-dessus.

---

## Périmètre
### Hors scope
- Les autres cellules de la matrice — SF distinctes.
- Tout changement frontend.
- Le ministère obligatoire d'un avocat à la Cour de cassation et la recevabilité formelle du pourvoi — hors F-98.

---

## Technique
### Contrat API
Inchangé.
### Tables impactées
Aucune.
### Migration Liquibase
- [ ] **Non applicable**.
### Composants
- Backend (neufs) : `CassBeFamillePourvoiDemandeurPromptProvider`, `CassBeFamillePourvoiDefendeurPromptProvider` (`@Component`).
- Frontend : aucun.

---

## Plan de test
### Backend (UT + IT)
- [ ] `CassBeFamillePourvoiDemandeurPromptProviderTest` : mémoire à l'appui du pourvoi (moyens de cassation, dispositions violées, cassation), pas de demandes chiffrées ; aucune référence au droit français.
- [ ] `CassBeFamillePourvoiDefendeurPromptProviderTest` : mémoire en réponse (réfutation, irrecevabilité, rejet du pourvoi), pas de demandes chiffrées ; aucune référence au droit français.
- [x] Le test data-driven `ConclusionPromptRegistryTest` (SF-98-02) couvre automatiquement les deux nouvelles cellules.
### Isolation workspace
- [x] Couverte par les contrôles existants.

---

## Analyse d'impact
- [x] **Aucune préoccupation transversale** — ajout additif de deux providers.
- [x] Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre `ConclusionPromptProvider`.
- F-243 (done) — `ProcedureStageCatalog` fournit les codes BE `CASS_BE` / `POURVOI` / `DEMANDEUR_POURVOI` / `DEFENDEUR_POURVOI`.

## Notes et décisions
- Le mémoire de cassation belge est un document de **pur droit** (moyens de cassation, dispositions légales violées) — la consigne transverse « demandes chiffrées » est neutralisée (seul « n'invente aucun chiffre » est conservé).
- **Solde le droit de la famille BE de la matrice F-98** (cellules SF-98-41 → SF-98-45) et, avec lui, **la matrice F-98 complète — 53/53 SF**.
