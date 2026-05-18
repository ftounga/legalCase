# Mini-spec — F-98 / SF-98-29 — Demande de titre à l'Office des étrangers — demandeur de titre BE

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section immigration BE) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` (SF-98-02).

## Identifiant
`F-98 / SF-98-29`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`done` — livrée 2026-05-18.

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-29-backend-office-etrangers-titre` — **SF backend pure**.

## Objectif
Générer le projet de **mémoire de demande de titre** adressé à l'Office des étrangers (hors contentieux), côté demandeur de titre, droit de l'immigration BE.

## Comportement attendu
### Cas nominal
1. Dossier immigration, workspace **BE**, stade procédural = Office des étrangers (`OE`) / Demande de titre (`DEMANDE_TITRE`) / Demandeur de titre (`DEMANDEUR_TITRE`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `OeDemandeTitrePromptProvider` — projet de **mémoire / note de soutien** adressé à l'**Office des étrangers** à l'appui d'une **demande d'autorisation de séjour** (l'administration n'a pas encore statué). Ancrage : **loi du 15 décembre 1980** (notamment art. 9bis — autorisation de séjour pour circonstances exceptionnelles ; art. 9ter — séjour pour raisons médicales ; regroupement familial). Structure : identification du demandeur et de la demande ; exposé de la situation personnelle, familiale et d'ancrage durable en Belgique ; **DISCUSSION** — fondement légal de l'autorisation sollicitée, circonstances exceptionnelles ou conditions remplies, éléments d'intégration ; **DEMANDE** (octroi de l'autorisation de séjour sollicitée). Le prompt précise que le document est une **demande / un mémoire de soutien** adressé à l'administration, non une requête contentieuse — registre argumenté mais non procédural.
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
- [ ] **CA1** — `OeDemandeTitrePromptProvider` enregistré pour `(DROIT_IMMIGRATION, BELGIQUE, OE, DEMANDE_TITRE, DEMANDEUR_TITRE)`.
- [ ] **CA2** — le prompt produit un mémoire de demande de titre **belge** (Office des étrangers, loi du 15 décembre 1980 — art. 9bis / 9ter, hors contentieux) — **aucune** référence au droit français.
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
- Backend (neuf) : `OeDemandeTitrePromptProvider` (`@Component`). Frontend : aucun.

## Plan de test
- [ ] `OeDemandeTitrePromptProviderTest` : le prompt cible l'Office des étrangers, la loi du 15 décembre 1980 (art. 9bis / 9ter), la demande de titre hors contentieux.
- [x] `ConclusionPromptRegistryTest` (data-driven, SF-98-02) couvre automatiquement la cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

## Analyse d'impact
- [x] **Aucune préoccupation transversale**. Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre `ConclusionPromptProvider`. F-243 (done) — codes `OE` / `DEMANDE_TITRE` / `DEMANDEUR_TITRE`.

## Notes et décisions
- Cellule **hors contentieux** belge : mémoire de soutien adressé à l'Office des étrangers — registre adapté. **Solde l'immigration BE de la matrice** (SF-98-26 → SF-98-29).
