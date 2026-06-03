# Mini-spec — F-223 / SF-223-02 — Outil recevabilité de l'adoption (Belgique)

## Identifiant
`F-223 / SF-223-02` — tool_id `adoption-be` (Famille BE) — statut `ready` — créé 2026-06-03 — branche `feat/SF-223-02-adoption-be`

## Objectif (1 phrase)
Évaluer la **recevabilité d'une adoption en Belgique** (loi 24/04/2003 ; CC art. 343-1 et s. — à vérifier par avocat belge, renumérotation CC post-réformes 2017-2019) selon le type (plénière / simple / co-parentale par cohabitant légal ou conjoint) : conditions d'âge, d'écart d'âge, d'agrément, et consentements requis.

## Périmètre / anti-doublon
**BE-only** (loi belge dédiée ≠ FR). Outil **multi-vues unique** : le type d'adoption (`PLENIERE` / `SIMPLE` / `CO_PARENTALE`) est une branche d'adoptant/d'effet de la même situation « recevabilité adoption » (1 outil = 1 situation — invariant cadrage, fusion explicite des items audit `adoption-be-pleniere` + `-simple` + `-co-parentale`). Ne recouvre pas la contestation de filiation (`contestation-filiation-be` F-217). L'**adoption internationale** (Convention La Haye + autorité centrale) est **différée P4 F-224**.

## Comportement (cas nominal)
- `POST /api/v1/case-files/{caseFileId}/adoption-be-analysis` (+ `GET`).
- Entrées : `typeAdoption` (enum `PLENIERE` / `SIMPLE` / `CO_PARENTALE`), `ageAdoptant` (int), `ageAdopte` (int), `ecartAgeAnnees` (int dérivé/saisi), `adoptantEnCoupleAvecParentBiologique` (bool — co-parentale), `lienCohabitationLegaleOuMariage` (bool nullable), `agrementObtenu` (bool), `consentementAdopteOuRepresentants` (bool), `consentementParentsBiologiques` (enum `OBTENU` / `REFUSE` / `SANS_OBJET`).
- Logique verdict : âge minimal de l'adoptant + écart d'âge minimal adoptant/adopté (CC 345 — à vérifier) ; agrément préalable du tribunal de la famille / enquête sociale ; consentements (adopté ≥ âge légal, parents biologiques, conjoint/cohabitant) ; co-parentale → vérifier le lien (mariage ou cohabitation légale avec le parent biologique).
- Verdict 4 niveaux : `RECEVABLE` / `RECEVABLE_SOUS_CONDITIONS` / `IRRECEVABLE` / `QUALIFICATION_INCOMPLETE` + motifs + actes à produire (requête TF, agrément, consentements à recueillir) + bases juridiques annotées « (à vérifier par avocat belge — renumérotation CC post-réformes 2017-2019) ».

## Cas d'erreur
| Situation | HTTP |
|---|---|
| Corps absent / `typeAdoption` absent ou invalide | 400 |
| `ageAdoptant` / `ageAdopte` ≤ 0 ou aberrant | 400 |
| Workspace ≠ BELGIQUE | 400 |
| `legalDomain` ≠ DROIT_FAMILLE | 400 |
| Autre workspace / inexistant / GET sans calcul | 404 |

## Champs IA (`FamilleExtractedData`)
- **Flag pivot CONTEXTUAL niveau 2 BE-only** : `adoptionBeDetectee` — **nouveau champ** à ajouter au record (boolean, default false, BE-only). ⚠️ Le record est partagé FR/BE — ajout d'un seul boolean (pas de sous-record `@JsonUnwrapped`, cf. plafond 255 params [[feedback_travailextracteddata_255_param_limit]]). Ne pas confondre avec `adoptionEnvisagee` (FR, ligne ~4203) ni `adoptionIntraEnvisagee` / `adoptionInternationaleEnvisagee` (FR, SF-216).
- Pré-fill (F-246) : âges et type d'adoption pré-remplis si factualisables ; sinon vides.

## Critères d'acceptation
- [ ] Les 3 types d'adoption couverts ; 4 verdicts produits.
- [ ] Écart d'âge insuffisant → `IRRECEVABLE` + motif.
- [ ] Co-parentale sans lien mariage/cohabitation légale → `IRRECEVABLE` + motif.
- [ ] Agrément absent → `RECEVABLE_SOUS_CONDITIONS` (agrément à obtenir).
- [ ] Aucune citation jurisprudentielle BE (F-JU-04 parké).
- [ ] Champs pré-remplis IA (sauf non factualisable) ; flag pivot BE-only niveau 2.
- [ ] Isolation workspace + gate BE-only/DROIT_FAMILLE testés.

## Plan de test
UT Calculator (3 types × verdicts + écart d'âge KO + co-parentale sans lien + agrément manquant), IT endpoint (200 + 400 gate + 404 isolation), Jest composant.

## Tables / endpoints / composants
- Backend : migration `adoption_be_analyses` (NNN — à pré-assigner) + Calculator/Service/Controller (pattern F-217). Migration record IA : addColumn flag (si stockage extracted data) — à pré-assigner.
- Frontend : `adoption-be-section.component` (+ artefacts) + `TOOL_REGISTRY` `adoption-be` + `THEME_BY_TOOL` + seed visibility (CONTEXTUAL, trigger `adoptionBeDetectee`) + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- IA : ajout flag `adoptionBeDetectee` à `FamilleExtractedData` + builder + prompt `LegalDomainPromptBuilder` Famille BE (impose false hors BE).

## Invariants
CONTEXTUAL jamais ALWAYS_ON ; pré-fill F-246 ; visibility + KNOWN_NO_DASHBOARD ; 1 outil = 1 situation ; BE-only ; pas de citation jurisprudence BE.

## Hors périmètre
Adoption internationale (P4 F-224) ; contestation de filiation (F-217) ; génération de la requête TF.
