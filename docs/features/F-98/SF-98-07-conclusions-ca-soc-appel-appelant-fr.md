# Mini-spec — F-98 / SF-98-07 — Conclusions d'appel chambre sociale — appelant FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Dépend du refactor `ConclusionPromptProvider` livré par **SF-98-02**.

## Identifiant
`F-98 / SF-98-07`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`done` — livrée 2026-05-18.

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-07-backend-ca-soc-appel-appelant` — **SF backend pure**.

---

## Objectif
Générer le projet de conclusions d'**appel** côté **appelant**, devant la chambre sociale de la cour d'appel, droit du travail FR.

---

## Comportement attendu

### Cas nominal
1. Dossier droit du travail FR, stade procédural = Cour d'appel chambre sociale / **appel** / appelant.
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `CaSocAppelAppelantPromptProvider` : conclusions d'appel — procédure avec représentation obligatoire, structure conforme à l'art. 954 CPC : en-tête, `RAPPEL DES FAITS ET DE LA PROCÉDURE` (incluant le jugement déféré), `DISCUSSION` (**critique du jugement chef par chef** — moyens d'infirmation), `DISPOSITIF récapitulatif` formulant expressément les prétentions (« Il est demandé à la Cour de : **INFIRMER** le jugement en ce qu'il… ; statuant à nouveau… »). Les pièces sont citées par numéro ; les montants des calculs sont repris à l'identique.
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
- [ ] **CA1** — `CaSocAppelAppelantPromptProvider` enregistré pour `(DROIT_DU_TRAVAIL, FRANCE, CA_SOC, APPEL, APPELANT)`.
- [ ] **CA2** — un dossier CA_SOC/APPEL/APPELANT génère des conclusions d'appel avec **dispositif récapitulatif** (mention expresse « INFIRMER »).
- [ ] **CA3** — la garde `COMBINATION_NOT_SUPPORTED` accepte désormais cette combinaison.
- [ ] **CA4** — le prompt cite les pièces par numéro et reprend les montants des calculs (invariants F-98 préservés).
- [ ] **CA5** — isolation workspace inchangée ; consigne de style SF-98-47 appliquée par-dessus.

---

## Périmètre
### Hors scope
- Les autres cellules de la matrice — SF distinctes.
- Tout changement frontend.
- La gestion procédurale de l'appel (délais art. 908 CPC, déclaration d'appel) — hors F-98.

---

## Technique
### Contrat API
Inchangé.
### Tables impactées
Aucune.
### Migration Liquibase
- [ ] **Non applicable**.
### Composants
- Backend (neuf) : `CaSocAppelAppelantPromptProvider` (`@Component`).
- Frontend : aucun.

---

## Plan de test
### Backend (UT + IT)
- [ ] `CaSocAppelAppelantPromptProviderTest` : le prompt produit une structure d'appel (rappel du jugement, critique chef par chef, dispositif récapitulatif « INFIRMER »), rôle appelant.
- [ ] `CaseConclusionControllerIT` : un dossier CA_SOC/APPEL/APPELANT n'est plus rejeté `COMBINATION_NOT_SUPPORTED`.
- [x] Le test data-driven `ConclusionPromptRegistryTest` (SF-98-02) couvre automatiquement la nouvelle cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

---

## Analyse d'impact
- [x] **Aucune préoccupation transversale** — ajout additif d'un provider.
- [x] Aucun smoke test E2E concerné.

## Dépendances
- **SF-98-02** (refactor `ConclusionPromptProvider`) — doit être mergée avant.
- F-243 (done) — codes `CA_SOC` / `APPEL` / `APPELANT` déjà au catalogue.

## Notes et décisions
- Les conclusions d'appel diffèrent structurellement des conclusions de 1ʳᵉ instance : critique du jugement et dispositif récapitulatif art. 954 CPC — d'où un prompt système dédié.
