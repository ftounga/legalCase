# Mini-spec — F-98 / SF-98-02 — Conclusions CPH bureau de jugement (fond) — défendeur (employeur) FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice des ~53 cellules) + `SF-98-00b-ux-coherence.md` (étape 0 bis — section conclusions déjà en place). **Cellule de matrice** : pas de nouveau cadrage (workflow métier et écran identiques à SF-98-01, seule la combinaison change).

## Identifiant
`F-98 / SF-98-02`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-02-backend-cph-fond-defendeur` — **SF backend pure** (frontend combinaison-agnostique, aucun changement).

---

## Objectif
Générer le projet de conclusions **en défense** (côté employeur) devant le Conseil de prud'hommes en bureau de jugement (fond), droit du travail FR — cellule miroir de SF-98-01.

---

## Refactor d'habilitation porté par cette SF

SF-98-01 a figé la combinaison supportée dans 5 constantes `SUPPORTED_*` et un `SYSTEM_PROMPT` unique. Ajouter des cellules en parallèle sur ces fichiers partagés provoquerait des conflits de merge systématiques. SF-98-02, **première cellule de la vague DT FR**, porte le refactor :

- record `CombinationKey(domain, country, jurisdiction, stage, position)` ;
- interface `ConclusionPromptProvider` — `combination()` + `systemPrompt()` ; **une cellule = un `@Component`** ;
- `ConclusionPromptRegistry` — agrège les `ConclusionPromptProvider` injectés par Spring, expose `supports(key)` et `systemPrompt(key)` ;
- le prompt SF-98-01 devient `CphFondDemandeurPromptProvider` (comportement **strictement identique**) ;
- `CaseConclusionPromptBuilder.buildSystemPrompt(...)` et la garde `COMBINATION_NOT_SUPPORTED` de `CaseConclusionCommandService` consomment le registre ;
- la consigne de style SF-98-47 reste appliquée **par-dessus** le prompt de la cellule.

**Aucun changement de comportement pour SF-98-01.** Après ce refactor, SF-98-03→10 ajoutent chacune **un seul fichier** `@Component` — parallélisation sans conflit.

---

## Comportement attendu

### Cas nominal
1. Dossier droit du travail FR, stade procédural = CPH / bureau de jugement (fond) / **défendeur (employeur)**.
2. `POST .../conclusions/generate` → `202` ; génération asynchrone d'une nouvelle version (SF-98-52).
3. Le worker assemble le prompt via `CphFondDefendeurPromptProvider` : conclusions **en défense** — en-tête `POUR [employeur] / CONTRE [salarié]`, `FAITS ET PROCÉDURE`, `DISCUSSION` (réfutation moyen par moyen des demandes du salarié : régularité de la procédure, cause réelle et sérieuse du licenciement, contestation du quantum), `PAR CES MOTIFS` (débouter le demandeur ; subsidiairement réduire les sommes).
4. La version générée passe `DONE` — exploitable comme toute version (édition SF-98-49, export SF-98-50/51, cycle de vie SF-98-52).

### Cas d'erreur / dégradation
| Situation | Comportement |
|---|---|
| Combinaison hors registre | `409 COMBINATION_NOT_SUPPORTED` (inchangé) |
| Gardes `STAGE_NOT_SET` / `ANALYSIS_NOT_READY` / `ALREADY_GENERATING` | inchangées |

---

## Analyse de cohérence transversale
- [x] Outil **non décisionnel** — générateur de document (pas de `TOOL_REGISTRY`, pas de F-IA-04/03).
- [x] Le refactor préserve le contrat API (`ConclusionResponse`, endpoints) — extension interne.
- [x] Préoccupations transversales : **aucune** (pas d'auth, pas de workspace nouveau, pas de route, pas d'outil décisionnel).

## Conformité F-IA-04
- [x] **Non applicable** — générateur de document, pas un outil décisionnel.

---

## Critères d'acceptation
- [ ] **CA1** — `CombinationKey`, `ConclusionPromptProvider`, `ConclusionPromptRegistry` créés ; SF-98-01 migrée en `CphFondDemandeurPromptProvider`, comportement inchangé (tests SF-98-01 verts).
- [ ] **CA2** — `CphFondDefendeurPromptProvider` enregistré pour `(DROIT_DU_TRAVAIL, FRANCE, CPH, FOND, DEFENDEUR)`.
- [ ] **CA3** — un dossier CPH/FOND/DEFENDEUR génère des conclusions **en défense** (rôle employeur, dispositif « débouter »).
- [ ] **CA4** — la garde `COMBINATION_NOT_SUPPORTED` accepte DEMANDEUR **et** DEFENDEUR, rejette toute combinaison hors registre.
- [ ] **CA5** — la consigne de style SF-98-47 reste appliquée par-dessus le prompt de la cellule.
- [ ] **CA6** — isolation workspace inchangée.

---

## Périmètre
### Hors scope
- Les autres cellules (référé, départage, appel, cassation) — SF-98-03→10.
- Tout changement frontend — la section conclusions est combinaison-agnostique.

---

## Technique
### Contrat API
Inchangé — aucun nouvel endpoint, aucun champ.
### Tables impactées
Aucune — `case_conclusions` stocke déjà `jurisdiction_code` / `stage_code` / `position_code`.
### Migration Liquibase
- [ ] **Non applicable**.
### Composants
- Backend (neufs) : `CombinationKey`, `ConclusionPromptProvider`, `ConclusionPromptRegistry`, `CphFondDemandeurPromptProvider` (extrait de SF-98-01), `CphFondDefendeurPromptProvider`.
- Backend (modifiés) : `CaseConclusionPromptBuilder`, `CaseConclusionCommandService` (garde via registre), `CaseConclusionService` (worker — passe la `CombinationKey`), `CaseConclusionGuardCode` (message `COMBINATION_NOT_SUPPORTED` rendu générique).
- Frontend : aucun.

---

## Plan de test
### Backend (UT + IT)
- [ ] `ConclusionPromptRegistryTest` : `supports` / `systemPrompt` ; test **data-driven** — tout provider enregistré a un `systemPrompt` non vide et une `CombinationKey` dont les codes existent dans `ProcedureStageCatalog`.
- [ ] `CphFondDefendeurPromptProviderTest` : le prompt cible le rôle défendeur (employeur), dispositif « débouter ».
- [ ] `CphFondDemandeurPromptProviderTest` : prompt SF-98-01 inchangé.
- [ ] `CaseConclusionCommandServiceTest` / `CaseConclusionControllerIT` : combinaison DEFENDEUR acceptée ; combinaison hors registre → `409`.
### Isolation workspace
- [x] Couverte par les contrôles existants.

---

## Analyse d'impact
- [x] Refactor interne sans impact API — **documenté dans la PR**.
- [x] Aucun smoke test E2E concerné.

## Dépendances
- SF-98-01 (done) — cellule de référence migrée vers le registre.
- F-243 (done) — `ProcedureStageCatalog` fournit déjà les codes `CPH` / `FOND` / `DEFENDEUR`.

## Notes et décisions
- Le refactor `ConclusionPromptProvider` est le **socle de parallélisation** des cellules : SF-98-03→10 = 1 fichier `@Component` chacune, zéro conflit.
- `case_conclusions` fige les codes de combinaison au déclenchement (snapshot) — une version reste lisible même si le stade du dossier change ensuite.
