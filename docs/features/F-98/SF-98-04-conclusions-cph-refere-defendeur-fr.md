# Mini-spec — F-98 / SF-98-04 — Conclusions CPH référé — défendeur FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Dépend du refactor `ConclusionPromptProvider` livré par **SF-98-02**.

## Identifiant
`F-98 / SF-98-04`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-04-backend-cph-refere-defendeur` — **SF backend pure**.

---

## Objectif
Générer le projet de conclusions **en défense** au référé prud'homal, devant la formation de référé du Conseil de prud'hommes, droit du travail FR.

---

## Comportement attendu

### Cas nominal
1. Dossier droit du travail FR, stade procédural = CPH / **référé prud'homal** / défendeur.
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `CphRefereDefendeurPromptProvider` : conclusions **en défense au référé** — en-tête `POUR [défendeur] / CONTRE [demandeur]`, `FAITS ET PROCÉDURE`, `DISCUSSION` orientée défense : **existence d'une contestation sérieuse** (qui fait échec à la compétence du juge des référés), **absence de trouble manifestement illicite**, contestation de l'urgence et du quantum de la provision sollicitée ; `PAR CES MOTIFS` (dire n'y avoir lieu à référé, renvoyer les parties à se pourvoir au fond ; subsidiairement réduire la provision).
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
- [ ] **CA1** — `CphRefereDefendeurPromptProvider` enregistré pour `(DROIT_DU_TRAVAIL, FRANCE, CPH, REFERE, DEFENDEUR)`.
- [ ] **CA2** — un dossier CPH/REFERE/DEFENDEUR génère des conclusions **en défense au référé** (moyen central : contestation sérieuse).
- [ ] **CA3** — la garde `COMBINATION_NOT_SUPPORTED` accepte désormais cette combinaison.
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
- Backend (neuf) : `CphRefereDefendeurPromptProvider` (`@Component`).
- Frontend : aucun.

---

## Plan de test
### Backend (UT + IT)
- [ ] `CphRefereDefendeurPromptProviderTest` : le prompt cible la défense au référé (contestation sérieuse, n'y avoir lieu à référé), rôle défendeur.
- [ ] `CaseConclusionControllerIT` : un dossier CPH/REFERE/DEFENDEUR n'est plus rejeté `COMBINATION_NOT_SUPPORTED`.
- [x] Le test data-driven `ConclusionPromptRegistryTest` (SF-98-02) couvre automatiquement la nouvelle cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

---

## Analyse d'impact
- [x] **Aucune préoccupation transversale** — ajout additif d'un provider.
- [x] Aucun smoke test E2E concerné.

## Dépendances
- **SF-98-02** (refactor `ConclusionPromptProvider`) — doit être mergée avant.
- F-243 (done) — codes `CPH` / `REFERE` / `DEFENDEUR` déjà au catalogue.

## Notes et décisions
- En défense au référé, le moyen dominant est la **contestation sérieuse** : son existence prive le juge des référés de son pouvoir de trancher — le prompt l'érige en axe central.
