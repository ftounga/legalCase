# Mini-spec — F-98 / SF-98-22 — Pourvoi en cassation Conseil d'État — requérant FR

> Cadrages amont : `SF-98-00-coherence.md` (étape 0 — matrice, section immigration FR) + `SF-98-00b-ux-coherence.md` (étape 0 bis). **Cellule de matrice** : pas de nouveau cadrage. Registre `ConclusionPromptProvider` (SF-98-02).

## Identifiant
`F-98 / SF-98-22`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-22-backend-ce-cassation` — **SF backend pure**.

## Objectif
Générer le projet de **pourvoi en cassation** devant le Conseil d'État, côté requérant, droit de l'immigration FR.

## Comportement attendu
### Cas nominal
1. Dossier immigration, workspace FR, stade procédural = Conseil d'État (`CE`) / Cassation (`CASSATION`) / Requérant (`REQUERANT`).
2. `POST .../conclusions/generate` → `202`, génération asynchrone d'une nouvelle version.
3. Le worker assemble le prompt via `CeCassationRequerantPromptProvider` — projet de **requête en cassation** devant le Conseil d'État contre un arrêt de cour administrative d'appel (ou un jugement rendu en dernier ressort). Ancrage : **code de justice administrative**. Structure : exposé des faits et de la procédure (incluant la décision attaquée) ; **MOYENS DE CASSATION** — chaque moyen visant un cas d'ouverture (erreur de droit, dénaturation des pièces, insuffisance ou contradiction de motifs, inexacte qualification juridique des faits) ; PAR CES MOTIFS (annuler la décision attaquée). Le prompt précise que le Conseil d'État, juge de cassation, **contrôle le droit** et n'apprécie pas souverainement les faits ; pas de demandes chiffrées.
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
- [ ] **CA1** — `CeCassationRequerantPromptProvider` enregistré pour `(DROIT_IMMIGRATION, FRANCE, CE, CASSATION, REQUERANT)`.
- [ ] **CA2** — le prompt produit une requête en cassation administrative (Conseil d'État, cas d'ouverture à cassation, contrôle du droit).
- [ ] **CA3** — la garde `COMBINATION_NOT_SUPPORTED` accepte cette combinaison.
- [ ] **CA4** — le prompt cite les pièces par numéro ; il **n'invente pas de demandes chiffrées** (juge de cassation) — invariants F-98 préservés.
- [ ] **CA5** — isolation workspace inchangée ; consigne de style SF-98-47 appliquée par-dessus.

## Périmètre
### Hors scope
- Les autres cellules de la matrice — SF distinctes. Tout changement frontend.
- La procédure d'admission du pourvoi en cassation — hors F-98.

## Technique
### Contrat API / Tables / Migration
Inchangé / aucune / **non applicable**.
### Composants
- Backend (neuf) : `CeCassationRequerantPromptProvider` (`@Component`). Frontend : aucun.

## Plan de test
- [ ] `CeCassationRequerantPromptProviderTest` : le prompt cible le Conseil d'État, les cas d'ouverture à cassation, le contrôle du droit, sans demandes chiffrées.
- [x] `ConclusionPromptRegistryTest` (data-driven, SF-98-02) couvre automatiquement la cellule.
### Isolation workspace
- [x] Couverte par les contrôles existants.

## Analyse d'impact
- [x] **Aucune préoccupation transversale**. Aucun smoke test E2E concerné.

## Dépendances
- SF-98-02 (done) — registre `ConclusionPromptProvider`. F-243 (done) — codes `CE` / `CASSATION` / `REQUERANT`.

## Notes et décisions
- Document de **pur droit** — la consigne transverse « demandes chiffrées » est neutralisée (seul « n'invente aucun chiffre » est conservé).
