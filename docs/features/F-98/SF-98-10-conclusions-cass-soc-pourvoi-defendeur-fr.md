# Mini-spec — F-98 / SF-98-10 — Mémoire en défense Cass. chambre sociale — défendeur au pourvoi FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Dépend du refactor `ConclusionPromptProvider` livré par **SF-98-02**.

## Identifiant
`F-98 / SF-98-10`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`done` — livrée 2026-05-18.

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-10-backend-cass-soc-pourvoi-defendeur` — **SF backend pure**.

---

## Objectif
Générer le projet de **mémoire en défense** devant la chambre sociale de la Cour de cassation, côté **défendeur au pourvoi**, droit du travail FR.

---

## Comportement attendu

### Cas nominal
1. Dossier droit du travail FR, stade procédural = Cour de cassation chambre sociale / **pourvoi** / défendeur au pourvoi.
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `CassSocPourvoiDefendeurPromptProvider` : projet de **mémoire en défense** — structure : `FAITS ET PROCÉDURE` (incluant l'arrêt attaqué), `DISCUSSION` — **réfutation moyen par moyen, branche par branche**, du mémoire ampliatif adverse ; peut soulever l'**irrecevabilité d'un moyen** (moyen nouveau, mélangé de fait et de droit, manquant en fait) ; conclut au **rejet du pourvoi**. `PAR CES MOTIFS` (rejeter le pourvoi). Le prompt précise que la Cour de cassation **juge en droit** : pas de demandes chiffrées.
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
- [ ] **CA1** — `CassSocPourvoiDefendeurPromptProvider` enregistré pour `(DROIT_DU_TRAVAIL, FRANCE, CASS_SOC, POURVOI, DEFENDEUR_POURVOI)`.
- [ ] **CA2** — un dossier CASS_SOC/POURVOI/DEFENDEUR_POURVOI génère un **mémoire en défense** (réfutation des moyens/branches, conclusion au rejet du pourvoi).
- [ ] **CA3** — la garde `COMBINATION_NOT_SUPPORTED` accepte désormais cette combinaison.
- [ ] **CA4** — le prompt cite les pièces par numéro ; il **n'invente pas de demandes chiffrées** (la Cour juge en droit) — invariants F-98 préservés.
- [ ] **CA5** — isolation workspace inchangée ; consigne de style SF-98-47 appliquée par-dessus.

---

## Périmètre
### Hors scope
- Les autres cellules de la matrice — SF distinctes.
- Tout changement frontend.
- L'appréciation de la recevabilité du pourvoi et le ministère d'avocat aux Conseils — hors F-98.

---

## Technique
### Contrat API
Inchangé.
### Tables impactées
Aucune.
### Migration Liquibase
- [ ] **Non applicable**.
### Composants
- Backend (neuf) : `CassSocPourvoiDefendeurPromptProvider` (`@Component`).
- Frontend : aucun.

---

## Plan de test
### Backend (UT + IT)
- [ ] `CassSocPourvoiDefendeurPromptProviderTest` : le prompt produit une structure de mémoire en défense (réfutation moyen/branche, irrecevabilité possible, « rejeter le pourvoi »), pas de demandes chiffrées.
- [ ] `CaseConclusionControllerIT` : un dossier CASS_SOC/POURVOI/DEFENDEUR_POURVOI n'est plus rejeté `COMBINATION_NOT_SUPPORTED`.
- [x] Le test data-driven `ConclusionPromptRegistryTest` (SF-98-02) couvre automatiquement la nouvelle cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

---

## Analyse d'impact
- [x] **Aucune préoccupation transversale** — ajout additif d'un provider.
- [x] Aucun smoke test E2E concerné.

## Dépendances
- **SF-98-02** (refactor `ConclusionPromptProvider`) — doit être mergée avant.
- F-243 (done) — codes `CASS_SOC` / `POURVOI` / `DEFENDEUR_POURVOI` déjà au catalogue.

## Notes et décisions
- Cellule miroir de SF-98-09 côté défense : réfutation des moyens de cassation + irrecevabilité éventuelle ; document de pur droit, pas de demandes chiffrées.
- **Solde le droit du travail FR de la matrice F-98** (cellules SF-98-01 → SF-98-10).
