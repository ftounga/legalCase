# Mini-spec — F-98 / SF-98-28 — Recours en cassation administrative Conseil d'État — requérant BE

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section immigration BE) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` (SF-98-02).

## Identifiant
`F-98 / SF-98-28`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-28-backend-ce-be-cassation` — **SF backend pure**.

## Objectif
Générer le projet de **recours en cassation administrative** devant le Conseil d'État de Belgique, côté requérant, droit de l'immigration BE.

## Comportement attendu
### Cas nominal
1. Dossier immigration, workspace **BE**, stade procédural = Conseil d'État (`CE_BE`) / Cassation administrative (`CASSATION`) / Requérant (`REQUERANT`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `CeBeCassationRequerantPromptProvider` — projet de **requête en cassation administrative** devant le **Conseil d'État de Belgique** contre un arrêt du Conseil du contentieux des étrangers. Ancrage : **lois coordonnées du 12 janvier 1973 sur le Conseil d'État** ; le recours en cassation administrative est soumis à une procédure d'**admissibilité** préalable. Structure : identification de l'arrêt attaqué du CCE ; exposé des antécédents de la procédure ; **MOYENS DE CASSATION** — chaque moyen visant la violation de la loi ou des formes substantielles / prescrites à peine de nullité, l'excès ou le détournement de pouvoir ; PAR CES MOTIFS (casser l'arrêt attaqué et renvoyer). Le prompt précise que le Conseil d'État, juge de cassation administrative, **contrôle la légalité** et n'apprécie pas les faits — **pas de demandes chiffrées**.
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
- [ ] **CA1** — `CeBeCassationRequerantPromptProvider` enregistré pour `(DROIT_IMMIGRATION, BELGIQUE, CE_BE, CASSATION, REQUERANT)`.
- [ ] **CA2** — le prompt produit un recours en cassation administrative **belge** (Conseil d'État de Belgique, lois coordonnées du 12 janvier 1973, arrêt du CCE) — **aucune** référence au droit français.
- [ ] **CA3** — la garde `COMBINATION_NOT_SUPPORTED` accepte cette combinaison.
- [ ] **CA4** — le prompt cite les pièces par numéro ; il **n'invente pas de demandes chiffrées** (juge de cassation) — invariants F-98 préservés.
- [ ] **CA5** — isolation workspace inchangée ; consigne de style SF-98-47 appliquée par-dessus.

## Périmètre
### Hors scope
- Les autres cellules de la matrice — SF distinctes. Tout changement frontend.
- La procédure d'admissibilité du recours en cassation — hors F-98.

## Technique
### Contrat API / Tables / Migration
Inchangé / aucune / **non applicable**.
### Composants
- Backend (neuf) : `CeBeCassationRequerantPromptProvider` (`@Component`). Frontend : aucun.

## Plan de test
- [ ] `CeBeCassationRequerantPromptProviderTest` : le prompt cible le Conseil d'État de Belgique, les moyens de cassation administrative, l'arrêt du CCE, sans demandes chiffrées.
- [x] `ConclusionPromptRegistryTest` (data-driven, SF-98-02) couvre automatiquement la cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

## Analyse d'impact
- [x] **Aucune préoccupation transversale**. Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre `ConclusionPromptProvider`. F-243 (done) — codes `CE_BE` / `CASSATION` / `REQUERANT`.

## Notes et décisions
- Document de **pur droit** belge — la consigne transverse « demandes chiffrées » est neutralisée (seul « n'invente aucun chiffre » est conservé). Couverture à partir des sources belges.
