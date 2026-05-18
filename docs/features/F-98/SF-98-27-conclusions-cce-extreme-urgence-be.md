# Mini-spec — F-98 / SF-98-27 — Recours en suspension d'extrême urgence devant le CCE — requérant BE

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section immigration BE) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` (SF-98-02).

## Identifiant
`F-98 / SF-98-27`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-27-backend-cce-extreme-urgence` — **SF backend pure**.

## Objectif
Générer le projet de **recours en suspension d'extrême urgence** devant le Conseil du contentieux des étrangers (CCE), côté requérant, droit de l'immigration BE.

## Comportement attendu
### Cas nominal
1. Dossier immigration, workspace **BE**, stade procédural = Conseil du contentieux des étrangers (`CCE`) / Référé en extrême urgence (`REFERE_EXTREME_URGENCE`) / Requérant (`REQUERANT`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `CceRefereExtremeUrgenceRequerantPromptProvider` — projet de **demande de suspension en extrême urgence** devant le **Conseil du contentieux des étrangers** contre une mesure d'éloignement ou de refoulement imminente. Ancrage : **loi du 15 décembre 1980** (art. 39/82 — suspension ; art. 39/82 § 4 — procédure d'extrême urgence). Structure : identification de la décision attaquée et de l'imminence de son exécution ; **EXTRÊME URGENCE** (caractérisée — exécution imminente de l'éloignement) ; **MOYEN SÉRIEUX** d'annulation ; **PRÉJUDICE GRAVE difficilement réparable** ; PAR CES MOTIFS (suspendre en extrême urgence l'exécution de la décision attaquée).
4. La version générée passe `DONE`.

### Cas d'erreur
| Situation | Comportement |
|---|---|
| Combinaison hors registre | `409 COMBINATION_NOT_SUPPORTED` |
| Gardes `STAGE_NOT_SET` / `ANALYSIS_NOT_READY` / `ALREADY_GENERATING` | inchangées |

## Analyse de cohérence transversale
- [x] Outil **non décisionnel** — générateur de document.
- [x] Ajout additif : un `@Component ConclusionPromptProvider`, aucun fichier partagé modifié.
- [x] Préoccupations transversales : **aucune**.

## Conformité F-IA-04
- [x] **Non applicable** — générateur de document.

## Critères d'acceptation
- [ ] **CA1** — `CceRefereExtremeUrgenceRequerantPromptProvider` enregistré pour `(DROIT_IMMIGRATION, BELGIQUE, CCE, REFERE_EXTREME_URGENCE, REQUERANT)`.
- [ ] **CA2** — le prompt produit un recours en **suspension d'extrême urgence belge** (CCE, loi du 15 décembre 1980 art. 39/82, extrême urgence, moyen sérieux, préjudice grave difficilement réparable) — **aucune** référence au droit français.
- [ ] **CA3** — la garde `COMBINATION_NOT_SUPPORTED` accepte cette combinaison.
- [ ] **CA4** — le prompt cite les pièces par numéro (invariants F-98 préservés).
- [ ] **CA5** — isolation workspace inchangée ; consigne de style SF-98-47 appliquée par-dessus.

## Périmètre
### Hors scope
- Les autres cellules de la matrice — SF distinctes. Tout changement frontend.

## Technique
### Contrat API / Tables / Migration
Inchangé / aucune / **non applicable**.
### Composants
- Backend (neuf) : `CceRefereExtremeUrgenceRequerantPromptProvider` (`@Component`). Frontend : aucun.

## Plan de test
- [ ] `CceRefereExtremeUrgenceRequerantPromptProviderTest` : le prompt cible le CCE, la procédure d'extrême urgence (art. 39/82 loi du 15 décembre 1980), le moyen sérieux et le préjudice grave difficilement réparable.
- [x] `ConclusionPromptRegistryTest` (data-driven, SF-98-02) couvre automatiquement la cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

## Analyse d'impact
- [x] **Aucune préoccupation transversale**. Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre `ConclusionPromptProvider`. F-243 (done) — codes `CCE` / `REFERE_EXTREME_URGENCE` / `REQUERANT`.

## Notes et décisions
- La procédure d'extrême urgence du CCE a un triple standard propre (extrême urgence, moyen sérieux, préjudice grave difficilement réparable) — cellule distincte du recours en plein contentieux (SF-98-26).
