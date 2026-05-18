# Mini-spec — F-98 / SF-98-26 — Recours en plein contentieux devant le CCE — requérant BE

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section immigration BE) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` (SF-98-02).

## Identifiant
`F-98 / SF-98-26`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-26-backend-cce-plein-contentieux` — **SF backend pure**.

## Objectif
Générer le projet de **recours en plein contentieux** devant le Conseil du contentieux des étrangers (CCE), côté requérant, droit de l'immigration BE.

## Comportement attendu
### Cas nominal
1. Dossier immigration, workspace **BE**, stade procédural = Conseil du contentieux des étrangers (`CCE`) / Recours en plein contentieux (`RECOURS_PLEIN_CONTENTIEUX`) / Requérant (`REQUERANT`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `CceRecoursRequerantPromptProvider` — projet de **requête en plein contentieux** devant le **Conseil du contentieux des étrangers** contre une décision du Commissariat général aux réfugiés et aux apatrides (CGRA) en matière de protection internationale. Ancrage : **loi du 15 décembre 1980** sur l'accès au territoire, le séjour, l'établissement et l'éloignement des étrangers (art. 39/2 § 1ᵉʳ — compétence de pleine juridiction du CCE en matière d'asile) ; **convention de Genève du 28 juillet 1951**. Structure : identification de la décision attaquée ; exposé des faits et du parcours du requérant ; recevabilité ; **MOYENS** — éligibilité au **statut de réfugié** et, subsidiairement, à la **protection subsidiaire** ; crédibilité et actualité des craintes ; PAR CES MOTIFS (réformer la décision du CGRA et reconnaître la protection internationale).
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
- [ ] **CA1** — `CceRecoursRequerantPromptProvider` enregistré pour `(DROIT_IMMIGRATION, BELGIQUE, CCE, RECOURS_PLEIN_CONTENTIEUX, REQUERANT)`.
- [ ] **CA2** — le prompt produit un recours **belge** (CCE, loi du 15 décembre 1980, plein contentieux, protection internationale) — **aucune** référence au droit français.
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
- Backend (neuf) : `CceRecoursRequerantPromptProvider` (`@Component`). Frontend : aucun.

## Plan de test
- [ ] `CceRecoursRequerantPromptProviderTest` : le prompt cible le CCE, la loi du 15 décembre 1980, le plein contentieux et la protection internationale.
- [x] `ConclusionPromptRegistryTest` (data-driven, SF-98-02) couvre automatiquement la cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

## Analyse d'impact
- [x] **Aucune préoccupation transversale**. Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre `ConclusionPromptProvider`. F-243 (done) — codes `CCE` / `RECOURS_PLEIN_CONTENTIEUX` / `REQUERANT`.

## Notes et décisions
- Couverture à partir des sources de procédure **belges** (loi du 15 décembre 1980, contentieux du CCE) — pas un miroir du droit français.
